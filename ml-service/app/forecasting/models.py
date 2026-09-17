"""
Demand Forecasting Models: Explainable Baseline vs. ML Regressor
===============================================================
Compares a simple Moving Average Baseline against an ML HistGradientBoostingRegressor
trained on historical features. Computes 7-day demand trajectories with confidence intervals.
"""

from datetime import datetime, timedelta
import joblib
import numpy as np
import pandas as pd
from sklearn.ensemble import HistGradientBoostingRegressor
from app.config import settings
from app.forecasting.feature_pipeline import FEATURE_COLUMNS, extract_temporal_features

MODEL_FILE = settings.models_dir / "demand_forecaster.pkl"

class MovingAverageBaseline:
    """Simple, explainable 7-day trailing average benchmark."""
    def predict(self, recent_demands):
        if not recent_demands:
            return 15.0
        return float(np.mean(recent_demands[-7:]))

class DemandForecastingModel:
    def __init__(self):
        self.model = HistGradientBoostingRegressor(
            max_iter=150,
            learning_rate=0.08,
            max_leaf_nodes=31,
            random_state=42
        )
        self.residual_std = 5.0
        self.is_trained = False
        self.version = "ml-histgradientboost-v1"

    def fit(self, X: pd.DataFrame, y: pd.Series):
        self.model.fit(X[FEATURE_COLUMNS], y)
        preds = self.model.predict(X[FEATURE_COLUMNS])
        residuals = y - preds
        self.residual_std = float(np.std(residuals))
        self.is_trained = True
        # Save model
        joblib.dump({"model": self.model, "std": self.residual_std, "version": self.version}, MODEL_FILE)

    def load(self):
        if MODEL_FILE.exists():
            data = joblib.load(MODEL_FILE)
            self.model = data["model"]
            self.residual_std = data.get("std", 5.0)
            self.version = data.get("version", "ml-histgradientboost-v1")
            self.is_trained = True
            return True
        return False

    def predict_7_days(self, phc_id: int, medicine_id: int, recent_demands: list, recent_footfalls: list):
        """
        Rolls forward day-by-day for the next 7 days, recursively updating lags.
        """
        # Fallback values if empty
        demands = list(recent_demands) if recent_demands else [25] * 14
        footfalls = list(recent_footfalls) if recent_footfalls else [140] * 14
        avg_footfall = float(np.mean(footfalls[-7:]))

        daily_forecasts = []
        today = datetime.now()

        for day_offset in range(1, 8):
            target_date = today + timedelta(days=day_offset)
            temp = extract_temporal_features(target_date)

            lag1 = demands[-1]
            lag7 = demands[-7] if len(demands) >= 7 else demands[-1]
            roll7 = float(np.mean(demands[-7:]))

            row = {
                "day_of_week": temp["day_of_week"],
                "is_weekend": temp["is_weekend"],
                "month": temp["month"],
                "is_monsoon": temp["is_monsoon"],
                "is_winter": temp["is_winter"],
                "patient_footfall": avg_footfall,
                "lag_1_demand": lag1,
                "lag_7_demand": lag7,
                "rolling_7_demand_mean": roll7,
            }

            if self.is_trained:
                pred = float(self.model.predict(pd.DataFrame([row])[FEATURE_COLUMNS])[0])
            else:
                # Fallback to rolling average if model file not loaded yet
                pred = roll7 * (1.2 if temp["day_of_week"] == 0 else (0.7 if temp["is_weekend"] else 1.0))

            pred = max(0.0, round(pred, 1))
            lower_bound = max(0.0, round(pred - (1.96 * self.residual_std), 1))
            upper_bound = round(pred + (1.96 * self.residual_std), 1)

            daily_forecasts.append({
                "day": day_offset,
                "forecast_date": target_date.strftime("%Y-%m-%d"),
                "predicted_demand": pred,
                "lower_bound_95": lower_bound,
                "upper_bound_95": upper_bound,
            })

            # Append prediction for rolling recursive forecasting
            demands.append(int(round(pred)))

        return daily_forecasts
