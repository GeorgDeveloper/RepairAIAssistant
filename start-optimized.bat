@echo off
echo Starting Repair AI Assistant...

REM Проверка Java
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo ERROR: Java not found. Please install Java 17+
    pause
    exit /b 1
)

REM Проверка Maven
mvn -version >nul 2>&1
if %errorlevel% neq 0 (
    echo ERROR: Maven not found. Please install Maven
    pause
    exit /b 1
)

REM Сборка проекта
echo Building project...
call mvn clean package -DskipTests -q

if %errorlevel% neq 0 (
    echo ERROR: Build failed
    pause
    exit /b 1
)

REM Создание директорий
if not exist "logs" mkdir logs
if not exist "uploads" mkdir uploads

REM Запуск сервисов
echo Starting services...

start "Core Service" java -jar assistant-core/target/assistant-core-0.0.1-SNAPSHOT.war --spring.config.location=file:application.yml
timeout /t 10 /nobreak >nul

start "Web Service" java -jar assistant-web/target/assistant-web-0.0.1-SNAPSHOT.war --spring.config.location=file:application.yml
timeout /t 5 /nobreak >nul

start "Telegram Service" java -jar assistant-telegram/target/assistant-telegram-0.0.1-SNAPSHOT.war --spring.config.location=file:application.yml

echo.
echo Services started successfully!
echo.
echo Web Interface: http://localhost:8081
echo API: http://localhost:8080
echo Telegram Bot: Active
echo.
echo Press any key to stop all services...
pause >nul

REM Остановка сервисов
taskkill /f /im java.exe >nul 2>&1
echo Services stopped.