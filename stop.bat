@echo off
REM ========================================
REM Скрипт остановки сервисов Repair AI Assistant
REM ========================================

echo Остановка сервисов Repair AI Assistant...

REM ========================================
REM Завершение Java процессов Spring Boot приложений
REM Ищем процессы java.exe, которые содержат "spring-boot" в командной строке
REM ========================================
for /f "tokens=2 delims=,\"" %%i in ('wmic process where "name='java.exe' and commandline like '%%spring-boot:run%%'" get ProcessId /format:csv ^| findstr /r "^[^,]*,[0-9][0-9]*$"') do taskkill /f /pid %%i >nul 2>&1

REM ========================================
REM Завершение процессов по портам
REM Останавливаем процессы, использующие порты 8080, 8081, 8082, 8084
REM ========================================
REM Остановка процесса на порту 8080 (основной сервис)
for /f "tokens=5" %%a in ('netstat -aon ^| findstr ":8080"') do taskkill /f /pid %%a >nul 2>&1
REM Остановка процесса на порту 8081 (веб-интерфейс)
for /f "tokens=5" %%a in ('netstat -aon ^| findstr ":8081"') do taskkill /f /pid %%a >nul 2>&1
REM Остановка процесса на порту 8082 (телеграм бот)
for /f "tokens=5" %%a in ('netstat -aon ^| findstr ":8082"') do taskkill /f /pid %%a >nul 2>&1

REM Остановка процесса на порту 8084 (assistant-base_update)
for /f "tokens=5" %%a in ('netstat -aon ^| findstr ":8084"') do taskkill /f /pid %%a >nul 2>&1

echo Все сервисы остановлены.
pause