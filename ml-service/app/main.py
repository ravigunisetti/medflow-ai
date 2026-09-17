from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from app.config import settings
from app.api.forecast_routes import router as forecast_router
from app.api.agent_routes import router as agent_router

app = FastAPI(
    title="PHC-NET AI — Demand Forecasting & AI Reasoning Service",
    description="Microservice providing ML time-series demand forecasting and Gemini AI tool-calling agents for PHC healthcare network",
    version="1.0.0"
)

# CORS Middleware
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Include Routers
app.include_router(forecast_router)
app.include_router(agent_router)

@app.get("/healthz", tags=["System Health"])
def health_check():
    return {
        "status": "HEALTHY",
        "service": "ml-forecasting-service",
        "version": "1.0.0",
        "environment": settings.app_env
    }

if __name__ == "__main__":
    import uvicorn
    uvicorn.run("app.main:app", host=settings.host, port=settings.port, reload=True)
