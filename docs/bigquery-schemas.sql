-- ==============================================================================
-- PHC-NET AI: Google BigQuery Analytical Warehouse Schemas
-- Dataset: phc_analytics
-- Target Engine: Google BigQuery (Partitioned & Clustered)
-- ==============================================================================

-- 1. Daily Dispensation Fact Table (Partitioned by date, Clustered by district and medicine)
CREATE OR REPLACE TABLE `phc_analytics.fact_daily_dispensation` (
    record_date DATE NOT NULL,
    phc_id INT64 NOT NULL,
    phc_name STRING,
    district STRING,
    medicine_id INT64 NOT NULL,
    medicine_code STRING,
    medicine_name STRING,
    category STRING,
    quantity_dispensed INT64,
    patient_opd_count INT64,
    emergency_count INT64,
    ingestion_timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP()
)
PARTITION BY record_date
CLUSTER BY district, category, medicine_id
OPTIONS(
    description="Partitioned time-series of daily PHC medicine consumption and OPD footfall"
);

-- 2. Stock-Out Incidents and Prevention Fact Table
CREATE OR REPLACE TABLE `phc_analytics.fact_stockout_events` (
    incident_date DATE NOT NULL,
    phc_id INT64 NOT NULL,
    district STRING,
    medicine_id INT64 NOT NULL,
    stock_on_hand INT64,
    daily_consumption_rate FLOAT64,
    days_to_stockout FLOAT64,
    risk_level STRING,
    prevented_by_transfer BOOL,
    patients_impacted INT64
)
PARTITION BY incident_date
CLUSTER BY district, risk_level;

-- 3. Redistribution Logistics & Efficiency Fact Table
CREATE OR REPLACE TABLE `phc_analytics.fact_redistribution_efficiency` (
    transfer_id INT64 NOT NULL,
    approval_date DATE NOT NULL,
    source_phc_id INT64 NOT NULL,
    source_district STRING,
    destination_phc_id INT64 NOT NULL,
    destination_district STRING,
    medicine_id INT64 NOT NULL,
    quantity_transferred INT64,
    distance_km FLOAT64,
    transport_cost_inr FLOAT64,
    days_gained_at_destination FLOAT64,
    status STRING
)
PARTITION BY approval_date
CLUSTER BY destination_district, medicine_id;
