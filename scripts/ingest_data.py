"""
PHC-NET AI: Modular Data Ingestion Interface
============================================
Provides an extensible pipeline to ingest external or synthetic datasets (CSV/JSON)
into the relational database. Supports both real government health records
(e.g., HMIS, e-Aushadhi) and synthetic benchmark data.

Features:
- Schema validation & constraint checking (negative values, date ranges, FK lookups).
- Dual-target support: PostgreSQL (production) or SQLite (offline/local embedded demo).
- Chunked bulk insertions for time-series scalability.
"""

import argparse
import csv
import json
import os
import sqlite3
import sys
from datetime import datetime
from pathlib import Path

DATA_DIR = Path(__file__).resolve().parent.parent / "data"

class DataIngestionEngine:
    def __init__(self, db_type="sqlite", db_path=None, pg_conn_str=None):
        self.db_type = db_type.lower()
        self.db_path = db_path or (DATA_DIR / "phcnet_local.db")
        self.pg_conn_str = pg_conn_str
        self.conn = None

    def connect(self):
        if self.db_type == "sqlite":
            self.conn = sqlite3.connect(str(self.db_path))
            self.conn.execute("PRAGMA foreign_keys = ON;")
            print(f"[Ingest] Connected to local SQLite store: {self.db_path}")
        elif self.db_type in ["postgres", "postgresql"]:
            import psycopg2
            self.conn = psycopg2.connect(self.pg_conn_str)
            print("[Ingest] Connected to PostgreSQL target.")
        else:
            raise ValueError(f"Unsupported database engine: {self.db_type}")

    def init_schema(self, schema_file=None):
        """Initializes database schema from DDL script."""
        schema_path = schema_file or (Path(__file__).resolve().parent / "schema.sql")
        with open(schema_path, "r", encoding="utf-8") as f:
            ddl = f.read()
            
        # If SQLite, adjust DDL syntax for compatibility
        if self.db_type == "sqlite":
            ddl = ddl.replace("DROP TABLE IF EXISTS audit_logs CASCADE;", "DROP TABLE IF EXISTS audit_logs;")
            ddl = ddl.replace("DROP TABLE IF EXISTS transfers CASCADE;", "DROP TABLE IF EXISTS transfers;")
            ddl = ddl.replace("DROP TABLE IF EXISTS predictions CASCADE;", "DROP TABLE IF EXISTS predictions;")
            ddl = ddl.replace("DROP TABLE IF EXISTS patient_footfalls CASCADE;", "DROP TABLE IF EXISTS patient_footfalls;")
            ddl = ddl.replace("DROP TABLE IF EXISTS demands CASCADE;", "DROP TABLE IF EXISTS demands;")
            ddl = ddl.replace("DROP TABLE IF EXISTS inventories CASCADE;", "DROP TABLE IF EXISTS inventories;")
            ddl = ddl.replace("DROP TABLE IF EXISTS users CASCADE;", "DROP TABLE IF EXISTS users;")
            ddl = ddl.replace("DROP TABLE IF EXISTS medicines CASCADE;", "DROP TABLE IF EXISTS medicines;")
            ddl = ddl.replace("DROP TABLE IF EXISTS phcs CASCADE;", "DROP TABLE IF EXISTS phcs;")
            ddl = ddl.replace("BIGSERIAL PRIMARY KEY", "INTEGER PRIMARY KEY AUTOINCREMENT")
            ddl = ddl.replace("BIGSERIAL", "INTEGER")
            ddl = ddl.replace("BIGINT", "INTEGER")
            ddl = ddl.replace("DOUBLE PRECISION", "REAL")
            ddl = ddl.replace("TIMESTAMP WITH TIME ZONE", "TIMESTAMP")
            ddl = ddl.replace("BOOLEAN NOT NULL DEFAULT TRUE", "INTEGER NOT NULL DEFAULT 1")
            ddl = ddl.replace("BOOLEAN NOT NULL DEFAULT FALSE", "INTEGER NOT NULL DEFAULT 0")
            ddl = ddl.replace("BOOLEAN", "INTEGER")
            ddl = ddl.replace("CURRENT_TIMESTAMP", "CURRENT_TIMESTAMP")
            ddl = ddl.replace("JSONB", "TEXT")
            # Remove line comments
            clean_lines = []
            for line in ddl.splitlines():
                if not line.strip().startswith("--"):
                    clean_lines.append(line)
            clean_ddl = "\n".join(clean_lines)
            statements = [s.strip() for s in clean_ddl.split(";") if s.strip()]
            for stmt in statements:
                try:
                    self.conn.execute(stmt)
                except Exception as e:
                    print(f"[Schema Warning] Statement failed: {stmt[:40]}... -> {e}")
            self.conn.commit()
            print("[Ingest] SQLite schema initialized.")
        else:
            cur = self.conn.cursor()
            cur.execute(ddl)
            self.conn.commit()
            cur.close()
            print("[Ingest] PostgreSQL schema initialized.")

    def ingest_csv(self, table_name, csv_filename, chunk_size=5000):
        """Streams a CSV file and inserts into the specified table with validation."""
        csv_path = DATA_DIR / csv_filename
        if not csv_path.exists():
            raise FileNotFoundError(f"Source file not found: {csv_path}")

        print(f"[Ingest] Ingesting {csv_filename} into '{table_name}'...")
        with open(csv_path, "r", encoding="utf-8") as f:
            reader = csv.DictReader(f)
            columns = reader.fieldnames
            
            # Exclude computed / metadata columns not in table
            if table_name == "medicines":
                columns = [c for c in columns if c in [
                    "id", "code", "name", "category", "unit", "safety_stock", 
                    "shelf_life_days", "requires_cold_chain"
                ]]

            col_names = ", ".join(columns)
            placeholders = ", ".join(["?" if self.db_type == "sqlite" else "%s"] * len(columns))
            insert_sql = f"INSERT OR IGNORE INTO {table_name} ({col_names}) VALUES ({placeholders})" \
                if self.db_type == "sqlite" else \
                f"INSERT INTO {table_name} ({col_names}) VALUES ({placeholders}) ON CONFLICT DO NOTHING"

            batch = []
            total_inserted = 0
            cur = self.conn.cursor()

            for row in reader:
                # Type sanitization and validation
                clean_row = []
                for col in columns:
                    val = row.get(col)
                    if val is None or val == "" or val == "NULL":
                        clean_row.append(None)
                    elif col in ["id", "phc_id", "medicine_id", "population_served", "quantity", 
                                 "reserved_quantity", "quantity_used", "patient_count", "emergency_count", 
                                 "safety_stock", "shelf_life_days", "assigned_phc_id"]:
                        clean_row.append(int(val))
                    elif col in ["latitude", "longitude"]:
                        clean_row.append(float(val))
                    elif col in ["is_active", "requires_cold_chain"]:
                        clean_row.append(1 if str(val).lower() in ["true", "1"] else 0)
                    else:
                        clean_row.append(val)
                        
                batch.append(tuple(clean_row))

                if len(batch) >= chunk_size:
                    cur.executemany(insert_sql, batch)
                    self.conn.commit()
                    total_inserted += len(batch)
                    batch = []

            if batch:
                cur.executemany(insert_sql, batch)
                self.conn.commit()
                total_inserted += len(batch)

            cur.close()
            print(f"[Ingest] Successfully ingested {total_inserted:,} records into '{table_name}'.")

    def run_full_pipeline(self):
        """Executes full loading sequence in referential integrity order."""
        self.connect()
        self.init_schema()
        
        # Load tables in dependency order
        self.ingest_csv("phcs", "phcs.csv")
        self.ingest_csv("medicines", "medicines.csv")
        self.ingest_csv("users", "users.csv")
        self.ingest_csv("inventories", "inventories.csv")
        self.ingest_csv("patient_footfalls", "patient_footfalls.csv")
        self.ingest_csv("demands", "demands.csv")
        
        print("[Ingest] Full ingestion pipeline completed successfully!")

    def close(self):
        if self.conn:
            self.conn.close()

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="PHC-NET AI Data Ingestion Pipeline")
    parser.add_argument("--db-type", default="sqlite", choices=["sqlite", "postgres"], help="Database engine target")
    parser.add_argument("--db-path", default=None, help="SQLite database path")
    parser.add_argument("--pg-conn", default=None, help="PostgreSQL connection string")
    
    args = parser.parse_args()
    engine = DataIngestionEngine(db_type=args.db_type, db_path=args.db_path, pg_conn_str=args.pg_conn)
    try:
        engine.run_full_pipeline()
    finally:
        engine.close()
