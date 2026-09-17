"""
Standalone Runner for Offline Model Training & Scientific Evaluation
====================================================================
"""

import sys
from pathlib import Path

# Add ml-service to Python path
sys.path.insert(0, str(Path(__file__).resolve().parent))

from app.forecasting.evaluator import train_and_evaluate_models

if __name__ == "__main__":
    print("=================================================================")
    print(" PHC-NET AI: Training & Benchmarking Demand Forecaster (M5)")
    print("=================================================================")
    report = train_and_evaluate_models(sample_limit=100000)
    print("\n[SUCCESS] Final Evaluation Summary:")
    print(f"  - Baseline (7-Day Moving Avg) MAE : {report['baseline_moving_average']['mae']} units")
    print(f"  - ML Model (HistGradientBoost) MAE : {report['ml_gradient_boosting']['mae']} units")
    print(f"  - MAE Improvement Over Baseline   : +{report['mae_improvement_pct']}%")
    print(f"  - RMSE Improvement Over Baseline  : +{report['rmse_improvement_pct']}%")
    print("=================================================================")
