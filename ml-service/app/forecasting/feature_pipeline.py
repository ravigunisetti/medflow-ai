"""
Feature Engineering Pipeline for PHC Medicine Demand Forecasting
================================================================
Transforms raw time-series records of daily medicine consumption and patient
OPD footfall into clean feature arrays for scikit-learn regressor models.
"""

from datetime import datetime, timedelta
import numpy as np
import pandas as pd

FEATURE_COLUMNS = [
    "day_of_week",
    "is_weekend",
    "month",
    "is_monsoon",
    "is_winter",
    "patient_footfall",
    "lag_1_demand",
    "lag_7_demand",
    "rolling_7_demand_mean",
]

def extract_temporal_features(record_date: datetime):
    dow = record_date.weekday()
    month = record_date.month
    return {
        "day_of_week": dow,
        "is_weekend": 1 if dow in [5, 6] else 0,
        "month": month,
        "is_monsoon": 1 if 6 <= month <= 9 else 0,
        "is_winter": 1 if month in [11, 12, 1] else 0,
    }

def build_training_matrix(demands_df: pd.DataFrame, footfalls_df: pd.DataFrame):
    """
    Merges historical demand and footfall tables and computes rolling lag features.
    """
    demands_df["record_date"] = pd.to_datetime(demands_df["record_date"])
    footfalls_df["record_date"] = pd.to_datetime(footfalls_df["record_date"])

    merged = pd.merge(
        demands_df,
        footfalls_df[["phc_id", "record_date", "patient_count"]],
        on=["phc_id", "record_date"],
        how="left"
    ).sort_values(["phc_id", "medicine_id", "record_date"])

    merged["patient_count"] = merged["patient_count"].fillna(120)

    # Compute lags per (phc_id, medicine_id) group
    grouped = merged.groupby(["phc_id", "medicine_id"])
    merged["lag_1_demand"] = grouped["quantity_used"].shift(1)
    merged["lag_7_demand"] = grouped["quantity_used"].shift(7)
    merged["rolling_7_demand_mean"] = grouped["quantity_used"].transform(lambda x: x.shift(1).rolling(7).mean())

    # Temporal columns
    merged["day_of_week"] = merged["record_date"].dt.dayofweek
    merged["is_weekend"] = merged["day_of_week"].isin([5, 6]).astype(int)
    merged["month"] = merged["record_date"].dt.month
    merged["is_monsoon"] = merged["month"].isin([6, 7, 8, 9]).astype(int)
    merged["is_winter"] = merged["month"].isin([11, 12, 1]).astype(int)
    merged["patient_footfall"] = merged["patient_count"]

    # Drop rows with NaN caused by shift/rolling
    clean_df = merged.dropna(subset=["lag_7_demand", "rolling_7_demand_mean"]).copy()
    return clean_df
