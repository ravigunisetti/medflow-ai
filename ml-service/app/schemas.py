from typing import List, Optional, Dict
from pydantic import BaseModel, Field

class DailyForecast(BaseModel):
    day: int = Field(..., description="Forecast day index (1 to 7)")
    forecast_date: str = Field(..., description="Target date in YYYY-MM-DD")
    predicted_demand: float = Field(..., description="Predicted medicine units needed")
    lower_bound_95: float = Field(..., description="95% confidence interval lower bound")
    upper_bound_95: float = Field(..., description="95% confidence interval upper bound")

class ForecastRequest(BaseModel):
    phc_id: int
    medicine_id: int
    recent_daily_demands: Optional[List[int]] = Field(default=None, description="Recent 14 to 30 days of actual demand")
    recent_patient_footfalls: Optional[List[int]] = Field(default=None, description="Recent 14 to 30 days of OPD footfall")

class ForecastResponse(BaseModel):
    phc_id: int
    medicine_id: int
    model_version: str
    baseline_7day_total: float
    predicted_7day_total: float
    daily_forecasts: List[DailyForecast]
    confidence_level: float = 0.95
    seasonality_factor_detected: str

class ModelMetrics(BaseModel):
    mae: float
    rmse: float
    mape: float
    sample_count: int

class EvaluationComparison(BaseModel):
    baseline_moving_average: ModelMetrics
    ml_gradient_boosting: ModelMetrics
    mae_improvement_pct: float
    rmse_improvement_pct: float
    training_samples: int
    testing_samples: int
    evaluation_date: str
