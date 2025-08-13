@echo off
echo Starting Repair AI Assistant...

REM Check if Java is available
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo Java not found! Please install Java 17 or higher.
    pause
    exit /b 1
)

REM Check if Maven is available
mvn -version >nul 2>&1
if %errorlevel% neq 0 (
    echo Maven not found! Please install Maven.
    pause
    exit /b 1
)

REM Check if Ollama is running
echo Checking Ollama service...
curl -s http://localhost:11434/api/tags >nul 2>&1
if %errorlevel% neq 0 (
    echo Ollama is not running! Please start Ollama first.
    pause
    exit /b 1
)

REM Build all modules
echo Building modules...
call mvn clean install -q

REM Start assistant-core
echo Starting assistant-core...
start "Assistant Core" cmd /c "cd assistant-core && mvn spring-boot:run"

REM Wait for core service
echo Waiting for core service to start...
:wait_core
timeout /t 5 /nobreak >nul
curl -s http://localhost:8080/actuator/health >nul 2>&1
if %errorlevel% neq 0 goto wait_core

REM Start assistant-web
echo Starting assistant-web...
start "Assistant Web" cmd /c "cd assistant-web && mvn spring-boot:run"

REM Wait for web service
echo Waiting for web service to start...
:wait_web
timeout /t 3 /nobreak >nul
curl -s http://localhost:8081 >nul 2>&1
if %errorlevel% neq 0 goto wait_web

REM Start assistant-telegram
echo Starting assistant-telegram...
start "Assistant Telegram" cmd /c "cd assistant-telegram && mvn spring-boot:run"

echo All services started successfully!
echo Web interface: http://localhost:8081
echo Core API: http://localhost:8080
echo Telegram bot: Running on port 8082
pause