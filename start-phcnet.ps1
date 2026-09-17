Write-Host "========================================================" -ForegroundColor Cyan
Write-Host "  Launching PHC-NET AI (Local Development Environment)" -ForegroundColor Yellow
Write-Host "========================================================" -ForegroundColor Cyan

$root = $PSScriptRoot

Write-Host "[1/3] Starting Spring Boot 3 Backend on port 8080..." -ForegroundColor Green
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$root\backend'; .\mvnw.cmd spring-boot:run"

Write-Host "[2/3] Starting Python ML & Gemini Agent Service on port 8000..." -ForegroundColor Green
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$root\ml-service'; python -m uvicorn app.main:app --port 8000 --reload"

Write-Host "[3/3] Starting React 19 Frontend Dashboard on port 5173..." -ForegroundColor Green
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$root\frontend'; npm run dev"

Write-Host "`nAll 3 services are launching in separate windows!" -ForegroundColor Cyan
Write-Host "• Dashboard URL: http://localhost:5173" -ForegroundColor White
Write-Host "• Swagger Docs:  http://localhost:8080/swagger-ui.html" -ForegroundColor White
Write-Host "• FastAPI Docs:  http://localhost:8000/docs" -ForegroundColor White
