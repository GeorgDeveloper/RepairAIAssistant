@echo off
setlocal enabledelayedexpansion
echo Starting Repair AI Assistant...

@REM REM Check if Java is available
@REM java -version >nul 2>&1
@REM if %errorlevel% neq 0 (
@REM     echo Java not found! Please install Java 17 or higher.
@REM     pause
@REM     exit /b 1
@REM )
@REM echo Java is available.
@REM echo Proceeding to Maven check...

@REM REM Check if Maven is available
@REM echo Checking Maven...
@REM mvn -version
@REM if %errorlevel% neq 0 (
@REM     echo Maven not found or not working! Please check your Maven installation.
@REM     pause
@REM     exit /b 1
@REM )
@REM echo Maven is available.
@REM echo Proceeding to Ollama check...

@REM REM Check if Ollama is running
@REM echo Checking Ollama service...
@REM curl -s http://localhost:11434/api/tags >nul 2>&1
@REM if %errorlevel% neq 0 (
@REM     echo Ollama is not running! Please start Ollama first.
@REM     pause
@REM     exit /b 1
@REM )
@REM echo Ollama service is running.
@REM echo Proceeding to build modules...

@REM REM Build all modules
@REM echo Building modules...
@REM call mvn clean install -q
@REM if %errorlevel% neq 0 (
@REM     echo Build failed! Please check the Maven logs.
@REM     pause
@REM     exit /b 1
@REM )
@REM echo Build successful.
@REM echo Proceeding to start assistant-core...

REM Start assistant-core
echo Starting assistant-core...
start "Assistant Core" cmd /k "cd assistant-core && mvn spring-boot:run"

REM Wait for core service to start
echo Waiting for core service to start...
:wait_core
timeout /t 5 /nobreak >nul
curl -s http://localhost:8080/actuator/health >nul 2>&1
if %errorlevel% neq 0 goto wait_core
echo Core service started.
echo Proceeding to start assistant-web...

REM Start assistant-web
echo Starting assistant-web...
start "Assistant Web" cmd /k "cd assistant-web && mvn spring-boot:run"

REM Wait for web service to start
echo Waiting for web service to start...
:wait_web
timeout /t 3 /nobreak >nul
curl -s http://localhost:8081 >nul 2>&1
if %errorlevel% neq 0 goto wait_web
echo Web service started.
echo Proceeding to start assistant-telegram...

REM Start assistant-telegram
echo Starting assistant-telegram...
start "Assistant Telegram" cmd /k "cd assistant-telegram && mvn spring-boot:run"

REM Final message
echo All services started successfully!
echo Web interface: http://localhost:8081
echo Core API: http://localhost:8080
echo Telegram bot: Running on port 8082
pause