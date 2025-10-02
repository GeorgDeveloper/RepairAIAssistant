@echo off
setlocal enabledelayedexpansion

REM Скрипт проверки состояния Repair AI Assistant v2.0 для Windows

echo 🔍 Проверка состояния Repair AI Assistant v2.0...
echo ==================================================

REM Функция для проверки HTTP эндпоинта
:check_http
set url=%1
set name=%2
set expected=%3

echo Проверка %name%...
curl -s --max-time 10 "%url%" >nul 2>&1
if %errorlevel% equ 0 (
    echo   ✓ OK
) else (
    echo   ✗ Недоступен
)
goto :eof

REM Функция для проверки Docker контейнера
:check_docker_container
set container_name=%1
set service_name=%2

echo Проверка контейнера %service_name%...
docker ps --format "table {{.Names}}" | findstr /C:"%container_name%" >nul 2>&1
if %errorlevel% equ 0 (
    echo   ✓ Запущен
) else (
    echo   ✗ Не найден
)
goto :eof

REM Функция для проверки порта
:check_port
set port=%1
set service_name=%2

echo Проверка порта %port% (%service_name%)...
netstat -an | findstr ":%port%" | findstr "LISTENING" >nul 2>&1
if %errorlevel% equ 0 (
    echo   ✓ Открыт
) else (
    echo   ✗ Закрыт
)
goto :eof

REM Проверка системных требований
echo.
echo 1. Системные требования
echo ------------------------

REM Проверка Docker
echo Проверка Docker...
docker --version >nul 2>&1
if %errorlevel% equ 0 (
    for /f "tokens=3" %%i in ('docker --version') do set docker_version=%%i
    echo   ✓ Версия: !docker_version!
) else (
    echo   ✗ Не установлен
)

REM Проверка Docker Compose
echo Проверка Docker Compose...
docker-compose --version >nul 2>&1
if %errorlevel% equ 0 (
    for /f "tokens=3" %%i in ('docker-compose --version') do set compose_version=%%i
    echo   ✓ Версия: !compose_version!
) else (
    echo   ✗ Не установлен
)

REM Проверка Java
echo Проверка Java...
java -version >nul 2>&1
if %errorlevel% equ 0 (
    for /f "tokens=3" %%i in ('java -version 2^>^&1 ^| findstr "version"') do set java_version=%%i
    echo   ✓ Java установлена
) else (
    echo   ✗ Не установлена
)

REM Проверка Maven
echo Проверка Maven...
mvn -version >nul 2>&1
if %errorlevel% equ 0 (
    echo   ✓ Maven установлен
) else (
    echo   ✗ Не установлен
)

REM Проверка свободного места
echo Проверка свободного места...
for /f "tokens=3" %%i in ('dir /-c ^| findstr "bytes free"') do set free_space=%%i
echo   ✓ Свободное место проверено

REM Проверка Docker контейнеров
echo.
echo 2. Docker контейнеры
echo ---------------------

call :check_docker_container "chromadb" "ChromaDB"
call :check_docker_container "ollama" "Ollama"  
call :check_docker_container "mysql" "MySQL"

REM Проверка портов
echo.
echo 3. Сетевые порты
echo -----------------

call :check_port 8000 "ChromaDB"
call :check_port 11434 "Ollama"
call :check_port 3306 "MySQL"
call :check_port 8080 "Приложение"

REM Проверка сервисов
echo.
echo 4. HTTP сервисы
echo ----------------

call :check_http "http://localhost:8000/api/v1/heartbeat" "ChromaDB API"
call :check_http "http://localhost:11434/api/tags" "Ollama API"
call :check_http "http://localhost:8080/api/v2/health" "Приложение API"

REM Проверка моделей Ollama
echo.
echo 5. Модели Ollama
echo -----------------

echo Проверка моделей...
curl -s http://localhost:11434/api/tags 2>nul | findstr "deepseek-r1" >nul 2>&1
if %errorlevel% equ 0 (
    echo   ✓ deepseek-r1 найдена
) else (
    echo   ✗ deepseek-r1 не найдена
)

curl -s http://localhost:11434/api/tags 2>nul | findstr "nomic-embed-text" >nul 2>&1
if %errorlevel% equ 0 (
    echo   ✓ nomic-embed-text найдена
) else (
    echo   ✗ nomic-embed-text не найдена
)

REM Проверка коллекций ChromaDB
echo.
echo 6. ChromaDB коллекции
echo ----------------------

echo Проверка коллекции repair_knowledge...
curl -s http://localhost:8000/api/v1/collections 2>nul | findstr "repair_knowledge" >nul 2>&1
if %errorlevel% equ 0 (
    echo   ✓ Найдена
) else (
    echo   ⚠ Не найдена (будет создана автоматически)
)

REM Тестовый запрос к приложению
echo.
echo 7. Тестирование функциональности
echo --------------------------------

echo Тестовый запрос к AI...
curl -s -X POST http://localhost:8080/api/v2/query -H "Content-Type: application/json" -d "{\"query\": \"Привет, как дела?\"}" 2>nul | findstr "response" >nul 2>&1
if %errorlevel% equ 0 (
    echo   ✓ Работает
) else (
    echo   ✗ Ошибка запроса
)

REM Итоговая сводка
echo.
echo 📊 Итоговая сводка
echo ==================
echo Система проверена. Детали см. выше.

echo.
echo 💡 Полезные команды
echo ===================
echo Перезапуск всех сервисов:    docker-compose restart
echo Просмотр логов ChromaDB:     docker logs chromadb -f
echo Просмотр логов Ollama:       docker logs ollama -f
echo Проверка здоровья:           curl http://localhost:8080/api/v2/health

echo.
echo ✅ Проверка завершена!

pause
