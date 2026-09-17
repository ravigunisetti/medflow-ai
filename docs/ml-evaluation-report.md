# Demand Forecasting Model Evaluation Report

**Date**: 2026-09-16 17:18:44

## 1. Experimental Setup

- **Training Samples**: 68,800
- **Testing Samples (Unseen Test Split)**: 17,200
- **Features Used**: `day_of_week`, `is_weekend`, `month`, `is_monsoon`, `is_winter`, `patient_footfall`, `lag_1_demand`, `lag_7_demand`, `rolling_7_demand_mean`

## 2. Benchmark Comparison Results

| Metric | 7-Day Moving Average Baseline | HistGradientBoosting ML Regressor | Improvement (%)
|---|---|---|---|
| **MAE (Mean Absolute Error)** | 3.49 units | **2.63 units** | **+24.63%** |
| **RMSE (Root Mean Squared Error)** | 6.4 units | **4.97 units** | **+22.45%** |
| **MAPE (%)** | 33.7% | **24.97%** | - |

## 3. Scientific Analysis

- The ML Regressor significantly outperforms simple moving averages during acute Monday surges and seasonal monsoon transitions because it learns explicit non-linear couplings between OPD footfall spikes and acute drug dispensing.
- Model weights and residual variance are saved to `ml-service/models/demand_forecaster.pkl` for low-latency inference.
