@echo off
echo Stopping Repair AI Assistant services...

REM Kill Java processes running Spring Boot applications
for /f "tokens=2" %%i in ('tasklist /fi "imagename eq java.exe" /fo csv ^| findstr "spring-boot"') do (
    taskkill /pid %%i /f >nul 2>&1
)

REM Kill processes by port
for /f "tokens=5" %%a in ('netstat -aon ^| findstr ":8080"') do taskkill /f /pid %%a >nul 2>&1
for /f "tokens=5" %%a in ('netstat -aon ^| findstr ":8081"') do taskkill /f /pid %%a >nul 2>&1
for /f "tokens=5" %%a in ('netstat -aon ^| findstr ":8082"') do taskkill /f /pid %%a >nul 2>&1

echo All services stopped.
pause