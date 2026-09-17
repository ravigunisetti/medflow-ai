-- ==============================================================================
-- PHC-NET AI: PostgreSQL Relational Database Schema
-- Version: 1.0.0
-- Target Engine: PostgreSQL 14+
-- ==============================================================================

-- Drop tables in reverse dependency order
DROP TABLE IF EXISTS audit_logs CASCADE;
DROP TABLE IF EXISTS transfers CASCADE;
DROP TABLE IF EXISTS predictions CASCADE;
DROP TABLE IF EXISTS patient_footfalls CASCADE;
DROP TABLE IF EXISTS demands CASCADE;
DROP TABLE IF EXISTS inventories CASCADE;
DROP TABLE IF EXISTS users CASCADE;
DROP TABLE IF EXISTS medicines CASCADE;
DROP TABLE IF EXISTS phcs CASCADE;

-- ------------------------------------------------------------------------------
-- 1. Primary Health Centres (PHCs)
-- ------------------------------------------------------------------------------
CREATE TABLE phcs (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    district VARCHAR(100) NOT NULL,
    state VARCHAR(100) NOT NULL DEFAULT 'Maharashtra',
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    population_served INTEGER NOT NULL CHECK (population_served > 0),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_phcs_district ON phcs(district);
CREATE INDEX idx_phcs_geo ON phcs(latitude, longitude);

-- ------------------------------------------------------------------------------
-- 2. Essential Medicines Catalog
-- ------------------------------------------------------------------------------
CREATE TABLE medicines (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(150) NOT NULL,
    category VARCHAR(100) NOT NULL,
    unit VARCHAR(50) NOT NULL,
    safety_stock INTEGER NOT NULL CHECK (safety_stock >= 0),
    shelf_life_days INTEGER NOT NULL DEFAULT 730,
    requires_cold_chain BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_medicines_category ON medicines(category);

-- ------------------------------------------------------------------------------
-- 3. Inventory Stock Balances
-- ------------------------------------------------------------------------------
CREATE TABLE inventories (
    id BIGSERIAL PRIMARY KEY,
    phc_id BIGINT NOT NULL REFERENCES phcs(id) ON DELETE RESTRICT,
    medicine_id BIGINT NOT NULL REFERENCES medicines(id) ON DELETE RESTRICT,
    quantity INTEGER NOT NULL CHECK (quantity >= 0),
    reserved_quantity INTEGER NOT NULL DEFAULT 0 CHECK (reserved_quantity >= 0),
    batch_number VARCHAR(100) NOT NULL DEFAULT 'BATCH-2026-A1',
    expiry_date DATE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_phc_medicine_batch UNIQUE (phc_id, medicine_id, batch_number)
);

CREATE INDEX idx_inventories_phc_med ON inventories(phc_id, medicine_id);
CREATE INDEX idx_inventories_quantity ON inventories(quantity);

-- ------------------------------------------------------------------------------
-- 4. Historical Medicine Demand (Time-Series Dispensing)
-- ------------------------------------------------------------------------------
CREATE TABLE demands (
    id BIGSERIAL PRIMARY KEY,
    phc_id BIGINT NOT NULL REFERENCES phcs(id) ON DELETE CASCADE,
    medicine_id BIGINT NOT NULL REFERENCES medicines(id) ON DELETE CASCADE,
    record_date DATE NOT NULL,
    quantity_used INTEGER NOT NULL CHECK (quantity_used >= 0),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_phc_med_date UNIQUE (phc_id, medicine_id, record_date)
);

CREATE INDEX idx_demands_lookup ON demands(phc_id, medicine_id, record_date DESC);
CREATE INDEX idx_demands_date ON demands(record_date);

-- ------------------------------------------------------------------------------
-- 5. Historical Patient Footfall (OPD & Emergency)
-- ------------------------------------------------------------------------------
CREATE TABLE patient_footfalls (
    id BIGSERIAL PRIMARY KEY,
    phc_id BIGINT NOT NULL REFERENCES phcs(id) ON DELETE CASCADE,
    record_date DATE NOT NULL,
    patient_count INTEGER NOT NULL CHECK (patient_count >= 0),
    emergency_count INTEGER NOT NULL DEFAULT 0 CHECK (emergency_count >= 0),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_phc_footfall_date UNIQUE (phc_id, record_date)
);

CREATE INDEX idx_footfall_phc_date ON patient_footfalls(phc_id, record_date DESC);

-- ------------------------------------------------------------------------------
-- 6. Demand & Stock-Out Predictions
-- ------------------------------------------------------------------------------
CREATE TABLE predictions (
    id BIGSERIAL PRIMARY KEY,
    phc_id BIGINT NOT NULL REFERENCES phcs(id) ON DELETE CASCADE,
    medicine_id BIGINT NOT NULL REFERENCES medicines(id) ON DELETE CASCADE,
    predicted_daily_demand DOUBLE PRECISION NOT NULL CHECK (predicted_daily_demand >= 0),
    predicted_days_to_stockout DOUBLE PRECISION NOT NULL,
    risk_level VARCHAR(20) NOT NULL CHECK (risk_level IN ('CRITICAL', 'HIGH', 'MEDIUM', 'LOW')),
    model_version VARCHAR(50) NOT NULL,
    confidence_score DOUBLE PRECISION NOT NULL DEFAULT 1.0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_predictions_risk ON predictions(risk_level, created_at DESC);
CREATE INDEX idx_predictions_phc_med ON predictions(phc_id, medicine_id, created_at DESC);

-- ------------------------------------------------------------------------------
-- 7. Inter-PHC Stock Transfers
-- ------------------------------------------------------------------------------
CREATE TABLE transfers (
    id BIGSERIAL PRIMARY KEY,
    source_phc_id BIGINT NOT NULL REFERENCES phcs(id) ON DELETE RESTRICT,
    destination_phc_id BIGINT NOT NULL REFERENCES phcs(id) ON DELETE RESTRICT,
    medicine_id BIGINT NOT NULL REFERENCES medicines(id) ON DELETE RESTRICT,
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    distance_km DOUBLE PRECISION NOT NULL,
    estimated_cost DOUBLE PRECISION NOT NULL CHECK (estimated_cost >= 0),
    status VARCHAR(30) NOT NULL CHECK (status IN ('PENDING_APPROVAL', 'APPROVED', 'IN_TRANSIT', 'COMPLETED', 'REJECTED')),
    recommendation_reason TEXT,
    approved_by_user_id BIGINT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_transfers_distinct_phcs CHECK (source_phc_id <> destination_phc_id)
);

CREATE INDEX idx_transfers_status ON transfers(status);
CREATE INDEX idx_transfers_src_dst ON transfers(source_phc_id, destination_phc_id);

-- ------------------------------------------------------------------------------
-- 8. Users & Authentication (RBAC)
-- ------------------------------------------------------------------------------
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(80) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(150) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    role VARCHAR(50) NOT NULL CHECK (role IN ('ROLE_ADMIN', 'ROLE_DISTRICT_OFFICER', 'ROLE_PHC_MANAGER')),
    assigned_phc_id BIGINT REFERENCES phcs(id) ON DELETE SET NULL,
    assigned_district VARCHAR(100),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ------------------------------------------------------------------------------
-- 9. Audit Logs (Tamper-Evident History)
-- ------------------------------------------------------------------------------
CREATE TABLE audit_logs (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(50) NOT NULL,
    entity_id BIGINT,
    details JSONB,
    ip_address VARCHAR(50),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_audit_created ON audit_logs(created_at DESC);
