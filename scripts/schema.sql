-- ==============================================================================
-- PHC-NET AI: PostgreSQL Relational Database Schema
-- Version: 1.0.0
-- Target Engine: PostgreSQL 14+
-- ==============================================================================

-- Drop tables in reverse dependency order
DROP TABLE IF EXISTS audit_logs CASCADE;
DROP TABLE IF EXISTS blood_transfers CASCADE;
DROP TABLE IF EXISTS emergency_blood_requests CASCADE;
DROP TABLE IF EXISTS blood_inventories CASCADE;
DROP TABLE IF EXISTS blood_banks CASCADE;
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

-- ==============================================================================
-- EMERGENCY BLOOD NETWORK MODULE
-- ==============================================================================

-- ------------------------------------------------------------------------------
-- 10. Certified Blood Banks & Resource Centers
-- ------------------------------------------------------------------------------
CREATE TABLE blood_banks (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    district VARCHAR(100) NOT NULL,
    state VARCHAR(100) NOT NULL DEFAULT 'Maharashtra',
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    verification_status VARCHAR(30) NOT NULL CHECK (verification_status IN ('VERIFIED', 'PROVISIONAL', 'PENDING_AUDIT')),
    contact_phone VARCHAR(50) NOT NULL,
    contact_email VARCHAR(100),
    operating_hours VARCHAR(100) NOT NULL DEFAULT '24x7 Emergency',
    storage_capacity_units INTEGER NOT NULL DEFAULT 1000 CHECK (storage_capacity_units > 0),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_blood_banks_district ON blood_banks(district);
CREATE INDEX idx_blood_banks_geo ON blood_banks(latitude, longitude);
CREATE INDEX idx_blood_banks_status ON blood_banks(verification_status, is_active);

-- ------------------------------------------------------------------------------
-- 11. Blood Bank Real-Time Inventory Balances
-- ------------------------------------------------------------------------------
CREATE TABLE blood_inventories (
    id BIGSERIAL PRIMARY KEY,
    blood_bank_id BIGINT NOT NULL REFERENCES blood_banks(id) ON DELETE CASCADE,
    blood_group VARCHAR(10) NOT NULL CHECK (blood_group IN ('A+', 'A-', 'B+', 'B-', 'AB+', 'AB-', 'O+', 'O-')),
    component_type VARCHAR(30) NOT NULL DEFAULT 'WHOLE_BLOOD' CHECK (component_type IN ('WHOLE_BLOOD', 'PRBC', 'FFP', 'PLATELETS')),
    units_available INTEGER NOT NULL CHECK (units_available >= 0),
    reserved_units INTEGER NOT NULL DEFAULT 0 CHECK (reserved_units >= 0),
    last_updated TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_blood_bank_group_component UNIQUE (blood_bank_id, blood_group, component_type)
);

CREATE INDEX idx_blood_inv_bank_group ON blood_inventories(blood_bank_id, blood_group);
CREATE INDEX idx_blood_inv_available ON blood_inventories(blood_group, units_available);

-- ------------------------------------------------------------------------------
-- 12. Urgent & Emergency Blood Requests
-- ------------------------------------------------------------------------------
CREATE TABLE emergency_blood_requests (
    id BIGSERIAL PRIMARY KEY,
    hospital_id BIGINT NOT NULL REFERENCES phcs(id) ON DELETE RESTRICT,
    blood_group VARCHAR(10) NOT NULL CHECK (blood_group IN ('A+', 'A-', 'B+', 'B-', 'AB+', 'AB-', 'O+', 'O-')),
    component_type VARCHAR(30) NOT NULL DEFAULT 'WHOLE_BLOOD',
    units_required INTEGER NOT NULL CHECK (units_required > 0),
    priority VARCHAR(20) NOT NULL CHECK (priority IN ('CRITICAL', 'HIGH', 'MEDIUM', 'LOW')),
    required_by TIMESTAMP WITH TIME ZONE NOT NULL,
    status VARCHAR(30) NOT NULL CHECK (status IN ('CREATED', 'MATCHING', 'MATCH_FOUND', 'CONFIRMED', 'IN_TRANSIT', 'FULFILLED', 'CANCELLED', 'EXPIRED')),
    clinical_notes TEXT,
    created_by_user_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_blood_req_status ON emergency_blood_requests(status, priority);
CREATE INDEX idx_blood_req_hospital ON emergency_blood_requests(hospital_id, created_at DESC);
CREATE INDEX idx_blood_req_deadline ON emergency_blood_requests(required_by);

-- ------------------------------------------------------------------------------
-- 13. Emergency Blood Transport & Delivery Tracking
-- ------------------------------------------------------------------------------
CREATE TABLE blood_transfers (
    id BIGSERIAL PRIMARY KEY,
    request_id BIGINT NOT NULL REFERENCES emergency_blood_requests(id) ON DELETE RESTRICT,
    source_blood_bank_id BIGINT NOT NULL REFERENCES blood_banks(id) ON DELETE RESTRICT,
    destination_hospital_id BIGINT NOT NULL REFERENCES phcs(id) ON DELETE RESTRICT,
    units INTEGER NOT NULL CHECK (units > 0),
    estimated_distance_km DOUBLE PRECISION NOT NULL,
    estimated_eta_minutes INTEGER NOT NULL,
    status VARCHAR(30) NOT NULL CHECK (status IN ('DISPATCH_PENDING', 'IN_TRANSIT', 'DELIVERED', 'CANCELLED')),
    cold_chain_verified BOOLEAN NOT NULL DEFAULT TRUE,
    dispatched_at TIMESTAMP WITH TIME ZONE,
    delivered_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_blood_transfer_req ON blood_transfers(request_id);
CREATE INDEX idx_blood_transfer_status ON blood_transfers(status);
