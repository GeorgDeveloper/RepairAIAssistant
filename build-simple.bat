@echo off
echo Building Repair AI Assistant...

echo [1/3] Checking Java...
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo ERROR: Java not found!
    pause
    exit /b 1
)
echo OK: Java found

echo [2/3] Checking Maven...
call mvn -version >nul 2>&1
if %errorlevel% neq 0 (
    echo ERROR: Maven not found!
    pause
    exit /b 1
)
echo OK: Maven found

echo [3/3] Building project...
call mvn clean package -DskipTests -q
if %errorlevel% neq 0 (
    echo ERROR: Build failed!
    pause
    exit /b 1
)
echo OK: Build completed

echo.
echo SUCCESS: Project built successfully!
echo WAR files created in target directories.
pause