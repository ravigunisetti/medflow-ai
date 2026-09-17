"""
Tests for Gemini AI Reasoning Agent and Tool Execution
======================================================
"""

import unittest
from fastapi.testclient import TestClient
from app.main import app

class TestGeminiAgent(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.client = TestClient(app)

    def test_01_agent_query_critical_facilities(self):
        payload = {"query": "Which PHCs require immediate attention and restocking?"}
        res = self.client.post("/api/v1/agent/query", json=payload)
        self.assertEqual(res.status_code, 200)
        data = res.json()
        self.assertTrue(data["grounded"])
        self.assertIn("tool_calls_executed", data)
        self.assertGreater(len(data["reasons"]), 0)

    def test_02_agent_explain_risk_endpoint(self):
        payload = {"phc_id": 1, "medicine_id": 5}
        res = self.client.post("/api/v1/agent/explain-risk", json=payload)
        self.assertEqual(res.status_code, 200)
        data = res.json()
        self.assertTrue(data["grounded"])
        self.assertEqual(data["phcId"], 1)
        self.assertEqual(data["medicineId"], 5)

if __name__ == "__main__":
    unittest.main(verbosity=2)
