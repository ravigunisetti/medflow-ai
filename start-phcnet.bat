@echo off
title PHC-NET AI Launcher
echo ========================================================
echo   Launching PHC-NET AI (Local Development Environment)
echo ========================================================
echo.

set ROOT=%~dp0

echo [1/3] Starting Spring Boot 3 Backend on port 8080...
start "PHC-NET AI - Backend (:8080)" cmd /k "cd /d %ROOT%backend && mvnw.cmd spring-boot:run"

echo [2/3] Starting Python ML & Gemini Agent Service on port 8000...
start "PHC-NET AI - ML Service (:8000)" cmd /k "cd /d %ROOT%ml-service && python -m uvicorn app.main:app --port 8000 --reload"

echo [3/3] Starting React 19 Frontend Dashboard on port 5173...
start "PHC-NET AI - Frontend (:5173)" cmd /k "cd /d %ROOT%frontend && npm run dev"

echo.
echo ========================================================
echo   All 3 services are launching in separate windows!
echo   Dashboard URL: http://localhost:5173
echo   Backend API:   http://localhost:8080/swagger-ui.html
echo   ML Service:    http://localhost:8000/docs
echo ========================================================
echo.
pause
