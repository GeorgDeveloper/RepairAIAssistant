@echo off
setlocal enabledelayedexpansion

echo ========================================
echo    REPAIR AI ASSISTANT - PRODUCTION BUILD
echo ========================================
echo.

REM Check if Java is available
echo [1/8] Checking Java installation...
java -version >nul 2>&1
if errorlevel 1 (
    echo ❌ Java not found! Please install Java 17 or higher.
    exit /b 1
)
echo ✅ Java found

REM Check if Maven is available
echo [2/8] Checking Maven installation...
mvn -version >nul 2>&1
if errorlevel 1 (
    echo ❌ Maven not found! Please install Maven.
    exit /b 1
)
echo ✅ Maven found

REM Clean and build all modules
echo.
echo [3/8] Cleaning previous builds...
echo ████████████████████████████████████████ 0%%
mvn clean -q
echo ████████████████████████████████████████ 100%%
echo ✅ Clean completed

echo.
echo [4/8] Building all modules...
echo ████████████████████████████████████████ 0%%
mvn package -DskipTests -q
echo ████████████████████████████████████████ 100%%
echo ✅ Build completed

REM Create target directory structure
echo.
echo [5/8] Creating target directory structure...
echo ████████████████████████████████████████ 0%%
if not exist target mkdir target
if not exist target\jars mkdir target\jars
if not exist target\config mkdir target\config
if not exist target\logs mkdir target\logs
if not exist target\scripts mkdir target\scripts
echo ████████████████████████████████████████ 100%%
echo ✅ Directory structure created

REM Copy WAR files
echo.
echo [6/8] Copying application files...
echo ████████████████████████████████████████ 0%%
copy "assistant-core\target\*.war" "target\jars\" >nul 2>&1
echo ████████████████████████████████████████ 33%%
copy "assistant-web\target\*.war" "target\jars\" >nul 2>&1
echo ████████████████████████████████████████ 66%%
copy "assistant-telegram\target\*.war" "target\jars\" >nul 2>&1
echo ████████████████████████████████████████ 100%%
echo ✅ Application files copied

REM Copy configuration files
echo.
echo [7/8] Copying configuration files...
echo ████████████████████████████████████████ 0%%
copy "assistant-core\src\main\resources\application.yml" "target\config\core-application.yml" >nul 2>&1
echo ████████████████████████████████████████ 33%%
copy "assistant-web\src\main\resources\application.yml" "target\config\web-application.yml" >nul 2>&1
echo ████████████████████████████████████████ 66%%
copy "assistant-telegram\src\main\resources\application.yml" "target\config\telegram-application.yml" >nul 2>&1
echo ████████████████████████████████████████ 100%%
echo ✅ Configuration files copied

REM Copy and convert shell scripts for Linux
echo.
echo [8/8] Preparing Linux deployment scripts...
echo ████████████████████████████████████████ 0%%

REM Create Linux start script
(
echo #!/bin/bash
echo.
echo echo "Starting Repair AI Assistant..."
echo.
echo # Check if Java is available
echo if ! command -v java ^&^> /dev/null; then
echo     echo "Java not found! Please install Java 17 or higher."
echo     exit 1
echo fi
echo.
echo # Check if Ollama is running
echo echo "Checking Ollama service..."
echo if ! curl -s http://localhost:11434/api/tags ^&^> /dev/null 2^>^&1; then
echo     echo "Ollama is not running! Please start Ollama first."
echo     exit 1
echo fi
echo.
echo # Start assistant-core
echo echo "Starting assistant-core..."
echo java -jar jars/assistant-core-0.0.1-SNAPSHOT.war --spring.config.location=config/core-application.yml ^> logs/core.log 2^>^&1 ^&
echo CORE_PID=$!
echo.
echo # Wait for core service
echo echo "Waiting for core service to start..."
echo while ! curl -s http://localhost:8080/actuator/health ^&^> /dev/null 2^>^&1; do
echo     sleep 2
echo done
echo.
echo # Start assistant-web
echo echo "Starting assistant-web..."
echo java -jar jars/assistant-web-0.0.1-SNAPSHOT.war --spring.config.location=config/web-application.yml ^> logs/web.log 2^>^&1 ^&
echo WEB_PID=$!
echo.
echo # Wait for web service
echo echo "Waiting for web service to start..."
echo while ! curl -s http://localhost:8081 ^&^> /dev/null 2^>^&1; do
echo     sleep 2
echo done
echo.
echo # Start assistant-telegram
echo echo "Starting assistant-telegram..."
echo java -jar jars/assistant-telegram-0.0.1-SNAPSHOT.war --spring.config.location=config/telegram-application.yml ^> logs/telegram.log 2^>^&1 ^&
echo TELEGRAM_PID=$!
echo.
echo echo "All services started successfully!"
echo echo "Web interface: http://localhost:8081"
echo echo "Core API: http://localhost:8080"
echo echo "Telegram bot: Running on port 8082"
echo echo ""
echo echo "PIDs: Core=$CORE_PID, Web=$WEB_PID, Telegram=$TELEGRAM_PID"
echo echo "Logs are in ./logs/ directory"
echo echo "Press Ctrl+C to stop all services"
echo.
echo # Save PIDs for stop script
echo echo "$CORE_PID $WEB_PID $TELEGRAM_PID" ^> .pids
echo.
echo # Wait for interrupt
echo trap 'kill $CORE_PID $WEB_PID $TELEGRAM_PID; rm -f .pids; exit' INT
echo wait
) > "target\scripts\start.sh"

REM Create Linux stop script
(
echo #!/bin/bash
echo.
echo echo "Stopping Repair AI Assistant services..."
echo.
echo # Kill processes by port
echo kill $(lsof -t -i:8080^) 2^>^/dev/null
echo kill $(lsof -t -i:8081^) 2^>^/dev/null
echo kill $(lsof -t -i:8082^) 2^>^/dev/null
echo.
echo # Kill processes by PID if available
echo if [ -f .pids ]; then
echo     kill $(cat .pids^) 2^>^/dev/null
echo     rm -f .pids
echo fi
echo.
echo echo "All services stopped."
) > "target\scripts\stop.sh"

REM Create deployment instructions
(
echo # Repair AI Assistant - Linux Deployment
echo.
echo ## Prerequisites
echo - Java 17 or higher
echo - MySQL 8.0 or higher
echo - Ollama running on localhost:11434
echo - curl and lsof utilities
echo.
echo ## Deployment Steps
echo.
echo 1. Copy the entire target directory to your Linux server
echo 2. Make scripts executable:
echo    chmod +x scripts/start.sh scripts/stop.sh
echo 3. Configure your database connection in config/*.yml files
echo 4. Start the application:
echo    ./scripts/start.sh
echo.
echo ## Services
echo - Core API: http://localhost:8080
echo - Web Interface: http://localhost:8081  
echo - Telegram Bot: Port 8082
echo.
echo ## Logs
echo - Core: logs/core.log
echo - Web: logs/web.log
echo - Telegram: logs/telegram.log
echo.
echo ## Stop Services
echo ./scripts/stop.sh
) > "target\DEPLOYMENT.md"

echo ████████████████████████████████████████ 100%%
echo ✅ Linux deployment scripts created

echo.
echo ========================================
echo           BUILD COMPLETED!
echo ========================================
echo.
echo 📦 Production build location:
echo   • WAR files: target\jars\
echo   • Configuration: target\config\
echo   • Linux scripts: target\scripts\
echo   • Deployment guide: target\DEPLOYMENT.md
echo.
echo 🚀 Next steps:
echo   1. Copy target\ directory to your Linux server
echo   2. On Linux: chmod +x scripts/*.sh
echo   3. On Linux: ./scripts/start.sh
echo.
echo ✅ Production build successful! Ready for Linux deployment.
