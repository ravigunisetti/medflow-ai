"""
PHC-NET AI: Verification & Evaluation Test Suite for Synthetic Data
===================================================================
Executes automated mathematical and relational tests on the generated data:
1. Table record counts and primary/foreign key integrity.
2. Value constraint assertions (non-negative quantities, dates).
3. Pearson correlation between patient footfall and acute medicine consumption.
4. Day-of-week and seasonal surge statistical tests.
5. Verification of deficit vs. surplus inventory clusters.
"""

import sqlite3
import unittest
from pathlib import Path

DATA_DIR = Path(__file__).resolve().parent.parent / "data"
DB_PATH = DATA_DIR / "phcnet_local.db"

class TestSyntheticDataIntegrity(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        if not DB_PATH.exists():
            raise FileNotFoundError(f"Database not found at {DB_PATH}. Run ingest_data.py first.")
        cls.conn = sqlite3.connect(str(DB_PATH))
        cls.cur = cls.conn.cursor()

    @classmethod
    def tearDownClass(cls):
        cls.conn.close()

    def test_01_record_counts(self):
        """Validates that all tables contain exact expected record counts."""
        expected_counts = {
            "phcs": 100,
            "medicines": 20,
            "users": 5,
            "inventories": 2000,
            "patient_footfalls": 36500,
            "demands": 730000
        }
        for table, expected in expected_counts.items():
            self.cur.execute(f"SELECT COUNT(*) FROM {table}")
            actual = self.cur.fetchone()[0]
            self.assertEqual(actual, expected, f"Table {table} count mismatch: got {actual}, expected {expected}")

    def test_02_no_negative_values(self):
        """Verifies strict absence of negative quantities in footfall, demand, and inventory."""
        self.cur.execute("SELECT COUNT(*) FROM patient_footfalls WHERE patient_count < 0 OR emergency_count < 0")
        self.assertEqual(self.cur.fetchone()[0], 0, "Found negative values in patient_footfalls")

        self.cur.execute("SELECT COUNT(*) FROM demands WHERE quantity_used < 0")
        self.assertEqual(self.cur.fetchone()[0], 0, "Found negative values in demands")

        self.cur.execute("SELECT COUNT(*) FROM inventories WHERE quantity < 0 OR reserved_quantity < 0")
        self.assertEqual(self.cur.fetchone()[0], 0, "Found negative values in inventories")

    def test_03_referential_integrity(self):
        """Verifies all foreign keys point to valid PHCs and medicines."""
        self.cur.execute("""
            SELECT COUNT(*) FROM inventories i
            LEFT JOIN phcs p ON i.phc_id = p.id
            LEFT JOIN medicines m ON i.medicine_id = m.id
            WHERE p.id IS NULL OR m.id IS NULL
        """)
        self.assertEqual(self.cur.fetchone()[0], 0, "Orphan foreign keys found in inventories")

        self.cur.execute("""
            SELECT COUNT(*) FROM demands d
            LEFT JOIN phcs p ON d.phc_id = p.id
            LEFT JOIN medicines m ON d.medicine_id = m.id
            WHERE p.id IS NULL OR m.id IS NULL
        """)
        self.assertEqual(self.cur.fetchone()[0], 0, "Orphan foreign keys found in demands")

    def test_04_footfall_demand_correlation(self):
        """
        Validates empirical correlation between patient footfall and Paracetamol/ORS consumption.
        Expects strong positive correlation (Pearson r > 0.60).
        """
        self.cur.execute("""
            SELECT f.patient_count, d.quantity_used
            FROM patient_footfalls f
            JOIN demands d ON f.phc_id = d.phc_id AND f.record_date = d.record_date
            WHERE d.medicine_id = 5  -- Paracetamol 500mg
            LIMIT 5000
        """)
        rows = self.cur.fetchall()
        x = [r[0] for r in rows]
        y = [r[1] for r in rows]
        
        n = len(x)
        mean_x = sum(x) / n
        mean_y = sum(y) / n
        
        cov = sum((x[i] - mean_x) * (y[i] - mean_y) for i in range(n))
        var_x = sum((x[i] - mean_x) ** 2 for i in range(n))
        var_y = sum((y[i] - mean_y) ** 2 for i in range(n))
        
        r = cov / ((var_x * var_y) ** 0.5)
        print(f"\n[Test Metric] Pearson correlation (Footfall vs. Paracetamol): r = {r:.4f}")
        self.assertGreater(r, 0.60, f"Expected correlation > 0.60, got {r:.4f}")

    def test_05_day_of_week_pattern(self):
        """Verifies that Monday footfall is significantly higher than Sunday footfall."""
        # SQLite strftime('%w', record_date): 0 = Sunday, 1 = Monday
        self.cur.execute("SELECT AVG(patient_count) FROM patient_footfalls WHERE strftime('%w', record_date) = '1'")
        monday_avg = self.cur.fetchone()[0]
        
        self.cur.execute("SELECT AVG(patient_count) FROM patient_footfalls WHERE strftime('%w', record_date) = '0'")
        sunday_avg = self.cur.fetchone()[0]
        
        ratio = monday_avg / sunday_avg
        print(f"[Test Metric] Monday-to-Sunday Footfall Ratio: {ratio:.2f}x (Mon: {monday_avg:.1f}, Sun: {sunday_avg:.1f})")
        self.assertGreater(ratio, 1.5, f"Expected Monday/Sunday footfall ratio > 1.5x, got {ratio:.2f}x")

    def test_06_deficit_and_surplus_clusters(self):
        """Verifies engineered clusters for redistribution: PHC 1-15 (Critical) vs PHC 16-25 (Surplus)."""
        # Average stock for Paracetamol in deficit cluster (PHC 1-15)
        self.cur.execute("SELECT AVG(quantity) FROM inventories WHERE medicine_id = 5 AND phc_id BETWEEN 1 AND 15")
        deficit_avg = self.cur.fetchone()[0]

        # Average stock for Paracetamol in surplus cluster (PHC 16-25)
        self.cur.execute("SELECT AVG(quantity) FROM inventories WHERE medicine_id = 5 AND phc_id BETWEEN 16 AND 25")
        surplus_avg = self.cur.fetchone()[0]

        ratio = surplus_avg / deficit_avg
        print(f"[Test Metric] Surplus-to-Deficit Inventory Ratio: {ratio:.1f}x (Surplus: {surplus_avg:.1f}, Deficit: {deficit_avg:.1f})")
        self.assertGreater(ratio, 5.0, f"Expected Surplus/Deficit stock ratio > 5x, got {ratio:.1f}x")

if __name__ == "__main__":
    unittest.main(verbosity=2)
