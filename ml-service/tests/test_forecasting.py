"""
Unit and Integration Tests for Demand Forecasting Service
=========================================================
"""

import unittest
from fastapi.testclient import TestClient
from app.main import app
from app.forecasting.models import DemandForecastingModel, MovingAverageBaseline

class TestForecastingService(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.client = TestClient(app)

    def test_01_health_check(self):
        res = self.client.get("/healthz")
        self.assertEqual(res.status_code, 200)
        self.assertEqual(res.json()["status"], "HEALTHY")

    def test_02_baseline_model(self):
        baseline = MovingAverageBaseline()
        demands = [20, 22, 25, 24, 28, 30, 35]
        pred = baseline.predict(demands)
        expected = sum(demands) / len(demands)
        self.assertAlmostEqual(pred, expected, places=2)

    def test_03_7day_forecast_endpoint(self):
        payload = {
            "phc_id": 1,
            "medicine_id": 5,
            "recent_daily_demands": [50, 55, 60, 62, 58, 65, 70, 52, 60, 63, 61, 59, 68, 72],
            "recent_patient_footfalls": [150, 160, 155, 145, 162, 170, 180, 152, 158, 164, 160, 159, 168, 175]
        }
        res = self.client.post("/api/v1/forecast/predict-7day", json=payload)
        self.assertEqual(res.status_code, 200)
        data = res.json()
        self.assertEqual(data["phc_id"], 1)
        self.assertEqual(data["medicine_id"], 5)
        self.assertEqual(len(data["daily_forecasts"]), 7)
        self.assertGreater(data["predicted_7day_total"], 0)

        # Check confidence intervals
        for f in data["daily_forecasts"]:
            self.assertLessEqual(f["lower_bound_95"], f["predicted_demand"])
            self.assertGreaterEqual(f["upper_bound_95"], f["predicted_demand"])

if __name__ == "__main__":
    unittest.main(verbosity=2)
