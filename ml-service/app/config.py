import os
from pathlib import Path
from pydantic_settings import BaseSettings

class Settings(BaseSettings):
    app_name: str = "PHC-NET AI Forecasting & Agent Service"
    app_env: str = "development"
    port: int = 8000
    host: str = "0.0.0.0"

    # Backend Connection
    backend_url: str = os.getenv("BACKEND_SERVICE_URL", "http://localhost:8080")

    # Gemini API
    gemini_api_key: str = os.getenv("GEMINI_API_KEY", "")
    gemini_model: str = os.getenv("GEMINI_MODEL", "gemini-2.5-flash")

    # Data & Models paths
    base_dir: Path = Path(__file__).resolve().parent.parent
    data_dir: Path = base_dir.parent / "data"
    models_dir: Path = base_dir / "models"

    class Config:
        env_file = ".env"
        extra = "ignore"

settings = Settings()
settings.models_dir.mkdir(parents=True, exist_ok=True)
