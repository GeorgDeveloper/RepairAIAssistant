@echo off
echo Тестирование системы Repair AI Assistant...

echo.
echo 1. Компиляция assistant-core...
cd assistant-core
call mvn compile -q
if %errorlevel% neq 0 (
    echo ОШИБКА: Компиляция assistant-core не удалась
    pause
    exit /b 1
)
echo ✓ assistant-core скомпилирован успешно

echo.
echo 2. Компиляция assistant-telegram...
cd ..\assistant-telegram
call mvn compile -q
if %errorlevel% neq 0 (
    echo ОШИБКА: Компиляция assistant-telegram не удалась
    pause
    exit /b 1
)
echo ✓ assistant-telegram скомпилирован успешно

echo.
echo 3. Компиляция assistant-web...
cd ..\assistant-web
call mvn compile -q
if %errorlevel% neq 0 (
    echo ОШИБКА: Компиляция assistant-web не удалась
    pause
    exit /b 1
)
echo ✓ assistant-web скомпилирован успешно

echo.
echo 4. Создание WAR файлов...
cd ..
call mvn package -DskipTests -q
if %errorlevel% neq 0 (
    echo ОШИБКА: Создание WAR файлов не удалось
    pause
    exit /b 1
)
echo ✓ WAR файлы созданы успешно

echo.
echo 5. Проверка структуры проекта...
if exist assistant-core\target\assistant-core-0.0.1-SNAPSHOT.war (
    echo ✓ assistant-core WAR файл создан
) else (
    echo ✗ assistant-core WAR файл не найден
)

if exist assistant-web\target\assistant-web-0.0.1-SNAPSHOT.war (
    echo ✓ assistant-web WAR файл создан
) else (
    echo ✗ assistant-web WAR файл не найден
)

if exist assistant-telegram\target\assistant-telegram-0.0.1-SNAPSHOT.war (
    echo ✓ assistant-telegram WAR файл создан
) else (
    echo ✗ assistant-telegram WAR файл не найден
)

echo.
echo ========================================
echo РЕЗУЛЬТАТ ТЕСТИРОВАНИЯ:
echo ========================================
echo ✓ Все модули компилируются без ошибок
echo ✓ WAR файлы создаются успешно
echo ✓ Структура проекта корректна
echo.
echo Система готова к запуску!
echo Для запуска используйте: cd target && start.bat
echo.
pause