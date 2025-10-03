@echo off
setlocal enabledelayedexpansion
REM Скрипт запуска Repair AI Assistant
REM Этот скрипт запускает все компоненты системы: core, web, telegram и base_update
echo Starting Repair AI Assistant...

REM ========== БЛОК ПРОВЕРКИ JAVA (ЗАКОММЕНТИРОВАН) ==========
REM Проверка доступности Java в системе
@REM REM Check if Java is available
@REM java -version >nul 2>&1
@REM if %errorlevel% neq 0 (
@REM     echo Java not found! Please install Java 17 or higher.
@REM     pause
@REM     exit /b 1
@REM )
@REM echo Java is available.
@REM echo Proceeding to Maven check...

REM ========== БЛОК ПРОВЕРКИ MAVEN (ЗАКОММЕНТИРОВАН) ==========
REM Проверка доступности Maven для сборки проекта
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

REM ========== БЛОК ПРОВЕРКИ OLLAMA (ЗАКОММЕНТИРОВАН) ==========
REM Проверка работы сервиса Ollama для ИИ-функций
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

REM ========== БЛОК СБОРКИ МОДУЛЕЙ (ЗАКОММЕНТИРОВАН) ==========
REM Сборка всех модулей проекта через Maven
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

REM ========== ЗАПУСК ОСНОВНОГО СЕРВИСА (ASSISTANT-CORE) ==========
REM Запуск основного сервиса на порту 8080 - содержит API и бизнес-логику
REM Start assistant-core
echo Starting assistant-core...
start "Assistant Core" cmd /k "cd assistant-core && mvnw.cmd -DskipTests spring-boot:run"

REM Ожидание запуска основного сервиса
REM Проверяем доступность health endpoint каждые 5 секунд
REM Wait for core service to start
echo Waiting for core service to start...
:wait_core
timeout /t 5 /nobreak >nul
curl -s http://localhost:8080/actuator/health >nul 2>&1
if %errorlevel% neq 0 goto wait_core
echo Core service started.
echo Proceeding to start assistant-web...

REM ========== ЗАПУСК ВЕБ-ИНТЕРФЕЙСА (ASSISTANT-WEB) ==========
REM Запуск веб-интерфейса на порту 8081 - пользовательский интерфейс
REM Start assistant-web
echo Starting assistant-web...
start "Assistant Web" cmd /k "cd assistant-web && mvnw.cmd -f ..\pom.xml -pl assistant-web -am -DskipTests spring-boot:run"

REM Ожидание запуска веб-сервиса
REM Проверяем доступность веб-интерфейса каждые 3 секунды
REM Wait for web service to start
echo Waiting for web service to start...
:wait_web
timeout /t 3 /nobreak >nul
curl -s http://localhost:8081 >nul 2>&1
if %errorlevel% neq 0 goto wait_web
echo Web service started.
echo Proceeding to start assistant-telegram...

REM ========== ЗАПУСК TELEGRAM-БОТА (ASSISTANT-TELEGRAM) ==========
REM Запуск Telegram-бота на порту 8082 - интеграция с мессенджером
REM Start assistant-telegram
echo Starting assistant-telegram...
start "Assistant Telegram" cmd /k "cd assistant-telegram && mvnw.cmd -DskipTests spring-boot:run"

REM ========== ЗАПУСК МОДУЛЯ ОБНОВЛЕНИЯ БАЗЫ (ASSISTANT-BASE_UPDATE) ==========
REM Модуль синхронизации данных между SQL Server и MySQL, запускается на 8084
REM Start assistant-base_update
echo Starting assistant-base_update...
start "Assistant Base Update" cmd /k "cd assistant-base_update && mvnw.cmd -DskipTests spring-boot:run"

REM ========== ЗАВЕРШЕНИЕ ЗАПУСКА ==========
REM Информация о запущенных сервисах и их адресах
REM Final message
echo All services started successfully!
echo Web interface: http://localhost:8081
echo Core API: http://localhost:8080
echo Telegram bot: Running on port 8082
echo Base Update: http://localhost:8084
pause