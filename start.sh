#!/bin/bash

echo "Starting Repair AI Assistant..."

# Проверка наличия Java (требуется версия 17+)
if ! command -v java &> /dev/null; then
    echo "Java не найдена! Установите Java 17 или выше."
    exit 1
fi

# Проверка наличия Maven
if ! command -v mvn &> /dev/null; then
    echo "Maven не найден! Установите Maven."
    exit 1
fi

# Проверка запущенного Ollama (локальный LLM-сервис)
echo "Проверка Ollama..."
if ! curl -s http://localhost:11434/api/tags > /dev/null 2>&1; then
    echo "Ollama не запущен! Запустите Ollama перед началом."
    exit 1
fi

# Сборка всех модулей проекта (тихо, без вывода)
echo "Сборка модулей (реактор, без установки)..."
mvn -q -f ./pom.xml -DskipTests package

# Запуск core-сервиса
echo "Запуск assistant-core..."
mkdir -p logs
cd assistant-core
./mvnw spring-boot:run > ../logs/core.log 2>&1 &
CORE_PID=$!
cd ..

# Ожидание готовности core-сервиса (без таймаута!)
echo "Ожидание запуска core-сервиса..."
while ! curl -s http://localhost:8080/actuator/health > /dev/null 2>&1; do
    sleep 2
done

# Запуск веб-интерфейса
echo "Запуск assistant-web..."
cd assistant-web
./mvnw -f ../pom.xml -pl assistant-web -am -DskipTests spring-boot:run > ../logs/web.log 2>&1 &
WEB_PID=$!
cd ..

# Ожидание готовности веб-сервиса
echo "Ожидание запуска веб-сервиса..."
while ! curl -s http://localhost:8081 > /dev/null 2>&1; do
    sleep 2
done

# Запуск Telegram-бота
echo "Запуск assistant-telegram..."
cd assistant-telegram
./mvnw spring-boot:run > ../logs/telegram.log 2>&1 &
TELEGRAM_PID=$!
cd ..

# Сообщение о успешном старте
echo "Все сервисы запущены!"
echo "Веб-интерфейс: http://localhost:8081"
echo "API Core: http://localhost:8080"
echo "Telegram-бот: порт 8082"
echo ""
echo "PID: Core=$CORE_PID, Web=$WEB_PID, Telegram=$TELEGRAM_PID"
echo "Логи находятся в ./logs/"

# Обработка Ctrl+C для корректного завершения
trap 'kill $CORE_PID $WEB_PID $TELEGRAM_PID; exit' INT
wait
