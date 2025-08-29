#!/bin/bash

# Остановка сервисов Repair AI Assistant

# Выводим сообщение о начале остановки
echo "Stopping Repair AI Assistant services..."

# Убиваем процессы, работающие на определенных портах
# Порты: 8080, 8081, 8082
kill $(lsof -t -i:8080) 2>/dev/null
kill $(lsof -t -i:8081) 2>/dev/null
kill $(lsof -t -i:8082) 2>/dev/null

# Убиваем процессы Spring Boot, ищя строку "spring-boot:run" в процессах
# Это позволяет остановить все процессы, связанные с запуском Spring Boot
pkill -f "spring-boot:run" 2>/dev/null

# Выводим сообщение о полной остановке
echo "All services stopped."
