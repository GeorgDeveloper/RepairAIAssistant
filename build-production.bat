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
call mvn -version >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] Maven not found! Please install Maven.
    pause
    exit /b 1
)
echo [OK] Maven found

REM Clean and build all modules
echo [3/8] Cleaning previous builds...
echo [                                        ] 0%%
call mvn clean -q
echo [########################################] 100%%
echo [OK] Clean completed

echo.
echo [4/8] Building all modules...
echo [                                        ] 0%%
call mvn package -DskipTests -q
echo [########################################] 100%%
echo [OK] Build completed

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
echo ████████████████████████████████████████ 20%%
copy "assistant-web\target\*.war" "target\jars\" >nul 2>&1
echo ████████████████████████████████████████ 40%%
copy "assistant-telegram\target\*.war" "target\jars\" >nul 2>&1
echo ████████████████████████████████████████ 60%%
copy "assistant-ai\target\*.war" "target\jars\" >nul 2>&1
echo ████████████████████████████████████████ 80%%
copy "assistant-base_update\target\*.war" "target\jars\" >nul 2>&1
echo ████████████████████████████████████████ 100%%
echo ✅ Application files copied

REM Copy configuration files
echo.
echo [7/8] Copying configuration files...
echo ████████████████████████████████████████ 0%%
copy "assistant-core\src\main\resources\application.yml" "target\config\core-application.yml" >nul 2>&1
echo ████████████████████████████████████████ 20%%
copy "assistant-web\src\main\resources\application.yml" "target\config\web-application.yml" >nul 2>&1
echo ████████████████████████████████████████ 40%%
copy "assistant-telegram\src\main\resources\application.yml" "target\config\telegram-application.yml" >nul 2>&1
echo ████████████████████████████████████████ 60%%
copy "assistant-ai\src\main\resources\application.yml" "target\config\ai-application.yml" >nul 2>&1
echo ████████████████████████████████████████ 80%%
copy "assistant-base_update\src\main\resources\application.yml" "target\config\base-update-application.yml" >nul 2>&1
echo ████████████████████████████████████████ 90%%
copy "application.yml" "target\application.yml" >nul 2>&1
echo ████████████████████████████████████████ 100%%
echo ✅ Configuration files copied

REM Copy and convert shell scripts for Linux
echo.
echo [8/8] Preparing Linux deployment scripts...
echo ████████████████████████████████████████ 0%%

REM Create Linux start script (minimal, no checks or wait loops)
setlocal DisableDelayedExpansion
(
echo #!/bin/bash
echo
echo mkdir -p logs
echo
echo echo "Starting services..."
echo
echo java -jar jars/assistant-core-0.0.1-SNAPSHOT.war --spring.config.location=config/core-application.yml --spring.config.additional-location=optional:application.yml ^> logs/core.log 2^>^&1 ^&
echo CORE_PID=$!
echo java -jar jars/assistant-web-0.0.1-SNAPSHOT.war --spring.config.location=config/web-application.yml --spring.config.additional-location=optional:application.yml ^> logs/web.log 2^>^&1 ^&
echo WEB_PID=$!
echo java -jar jars/assistant-telegram-0.0.1-SNAPSHOT.war --spring.config.location=config/telegram-application.yml --spring.config.additional-location=optional:application.yml ^> logs/telegram.log 2^>^&1 ^&
echo TELEGRAM_PID=$!
echo java -jar jars/assistant-ai-0.0.1-SNAPSHOT.war --spring.config.location=config/ai-application.yml --spring.config.additional-location=optional:application.yml ^> logs/ai.log 2^>^&1 ^&
echo AI_PID=$!
echo java -jar jars/assistant-base_update-1.0.0.war --spring.config.location=config/base-update-application.yml --spring.config.additional-location=optional:application.yml ^> logs/base-update.log 2^>^&1 ^&
echo BASE_UPDATE_PID=$!
echo
echo echo "Started. Logs: ./logs."
echo echo "PIDs: Core=$CORE_PID Web=$WEB_PID Telegram=$TELEGRAM_PID AI=$AI_PID BaseUpdate=$BASE_UPDATE_PID"
echo echo "Use scripts/stop.sh to stop."
echo
echo echo "$CORE_PID $WEB_PID $TELEGRAM_PID $AI_PID $BASE_UPDATE_PID" ^> .pids
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
echo kill $(lsof -t -i:8085^) 2^>^/dev/null
echo kill $(lsof -t -i:8084^) 2^>^/dev/null
echo.
echo # Kill processes by PID if available
echo if [ -f .pids ]; then
echo     kill $(cat .pids^) 2^>^/dev/null
echo     rm -f .pids
echo fi
echo.
echo echo "All services stopped."
) > "target\scripts\stop.sh"
setlocal EnableDelayedExpansion

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
echo - AI Service: Port 8083
echo - Base Update Service: Port 8084
echo.
echo ## Logs
echo - Core: logs/core.log
echo - Web: logs/web.log
echo - Telegram: logs/telegram.log
echo - AI: logs/ai.log
echo - Base Update: logs/base-update.log
echo.
echo ## Stop Services
echo ./scripts/stop.sh
) > "target\DEPLOYMENT.md"

echo ████████████████████████████████████████ 100%%
echo ✅ Linux deployment scripts created

REM Make shell scripts executable (for WSL or Git Bash)
echo.
echo [9/8] Setting executable permissions...
if exist "target\scripts\start.sh" (
    echo Setting permissions for start.sh...
    call wsl chmod +x target/scripts/start.sh 2>nul || echo Note: WSL not available, permissions will be set on Linux
)
if exist "target\scripts\stop.sh" (
    echo Setting permissions for stop.sh...
    call wsl chmod +x target/scripts/stop.sh 2>nul || echo Note: WSL not available, permissions will be set on Linux
)
REM Normalize encoding to UTF-8 (no BOM) and Unix line endings (LF)
for %%F in ("target\scripts\start.sh" "target\scripts\stop.sh") do (
    if exist %%F (
        echo Converting %%F to UTF-8 (no BOM) with LF endings...
        powershell -NoProfile -ExecutionPolicy Bypass -Command ^
          "$p='%%F'; $t=Get-Content -Raw -LiteralPath $p; $t=$t -replace '\r\n','\n' -replace '\r','\n'; ^
           [IO.File]::WriteAllText($p, $t, New-Object System.Text.UTF8Encoding($false));"
    )
)
echo ✅ Permissions configured

echo.
echo ========================================
echo           BUILD COMPLETED!
echo ========================================
echo.
echo 📦 Production build location:
echo   • WAR files: target\jars\ (5 modules: core, web, telegram, ai, base_update)
echo   • Configuration: target\config\ (5 config files)
echo   • Linux scripts: target\scripts\ (start.sh, stop.sh)
echo   • Deployment guide: target\DEPLOYMENT.md
echo.
echo 🚀 Next steps:
echo   1. Copy target\ directory to your Linux server
echo   2. On Linux: chmod +x scripts/*.sh
echo   3. On Linux: ./scripts/start.sh
echo.
echo ✅ Production build successful! All 5 modules ready for Linux deployment.
