@echo off
echo Testing Repair AI Assistant with Mistral...

echo.
echo 1. Testing Ollama connection...
curl -s http://localhost:11434/api/tags | findstr mistral
if %errorlevel% neq 0 (
    echo ERROR: Mistral model not found in Ollama
    exit /b 1
)

echo.
echo 2. Testing database connection...
curl -s -X POST -H "Content-Type: application/json" -d "SELECT COUNT(*) FROM equipment_maintenance_records" http://localhost:8080/api/health
if %errorlevel% neq 0 (
    echo ERROR: Cannot connect to API
    exit /b 1
)

echo.
echo 3. Testing AI training system...
curl -s -X POST -H "Content-Type: application/json" -d "Кто ты?" http://localhost:8080/api/analyze
if %errorlevel% neq 0 (
    echo ERROR: AI system not responding
    exit /b 1
)

echo.
echo 4. Testing database search...
curl -s -X POST -H "Content-Type: application/json" -d "Найди ремонты со статусом закрыто" http://localhost:8080/api/analyze

echo.
echo 5. Testing repair instructions...
curl -s -X POST -H "Content-Type: application/json" -d "Что делать при утечке масла?" http://localhost:8080/api/analyze

echo.
echo System test completed!
pause