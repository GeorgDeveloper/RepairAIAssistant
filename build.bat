@echo off
echo ========================================
echo    REPAIR AI ASSISTANT - BUILD
echo ========================================
echo.

echo [1/8] Checking Java installation...
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] Java not found! Please install Java 17 or higher.
    pause
    exit /b 1
)
echo [OK] Java found

echo [2/8] Checking Maven installation...
call mvn -version >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] Maven not found! Please install Maven.
    pause
    exit /b 1
)
echo [OK] Maven found

echo.
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

echo.
echo [5/8] Creating target directory structure...
if not exist target mkdir target
if not exist target\jars mkdir target\jars
if not exist target\config mkdir target\config
if not exist target\logs mkdir target\logs
echo [OK] Directory structure created

echo.
echo [6/8] Copying application files...
copy assistant-core\target\*.war target\jars\ >nul 2>&1
copy assistant-web\target\*.war target\jars\ >nul 2>&1
copy assistant-telegram\target\*.war target\jars\ >nul 2>&1
echo [OK] Application files copied

echo.
echo [7/8] Copying configuration files...
copy assistant-core\src\main\resources\application.yml target\config\core-application.yml >nul 2>&1
copy assistant-web\src\main\resources\application.yml target\config\web-application.yml >nul 2>&1
copy assistant-telegram\src\main\resources\application.yml target\config\telegram-application.yml >nul 2>&1
echo [OK] Configuration files copied

echo.
echo [8/8] Creating startup scripts...
echo @echo off > target\start.bat
echo echo Starting Repair AI Assistant... >> target\start.bat
echo start "Core" java -jar jars\assistant-core-0.0.1-SNAPSHOT.war >> target\start.bat
echo timeout /t 10 /nobreak ^>nul >> target\start.bat
echo start "Web" java -jar jars\assistant-web-0.0.1-SNAPSHOT.war >> target\start.bat
echo timeout /t 5 /nobreak ^>nul >> target\start.bat
echo start "Telegram" java -jar jars\assistant-telegram-0.0.1-SNAPSHOT.war >> target\start.bat
echo echo All services started! >> target\start.bat
echo pause >> target\start.bat

echo @echo off > target\stop.bat
echo echo Stopping services... >> target\stop.bat
echo taskkill /f /im java.exe /fi "WINDOWTITLE eq Core*" ^>nul 2^>^&1 >> target\stop.bat
echo taskkill /f /im java.exe /fi "WINDOWTITLE eq Web*" ^>nul 2^>^&1 >> target\stop.bat
echo taskkill /f /im java.exe /fi "WINDOWTITLE eq Telegram*" ^>nul 2^>^&1 >> target\stop.bat
echo echo Services stopped. >> target\stop.bat
echo pause >> target\stop.bat

echo [OK] Scripts created

echo.
echo ========================================
echo           BUILD COMPLETED!
echo ========================================
echo.
echo Built files location:
echo   - WAR files: target\jars\
echo   - Configuration: target\config\
echo   - Startup script: target\start.bat
echo   - Stop script: target\stop.bat
echo.
echo To run: cd target ^&^& start.bat
echo.
echo [SUCCESS] Build completed! System ready to launch.
pause