#!/bin/bash

echo "Starting Repair AI Assistant..."

# Check if Java is available
if ! command -v java &> /dev/null; then
    echo "Java not found! Please install Java 17 or higher."
    exit 1
fi

# Check if Maven is available
if ! command -v mvn &> /dev/null; then
    echo "Maven not found! Please install Maven."
    exit 1
fi

# Check if Ollama is running
echo "Checking Ollama service..."
if ! curl -s http://localhost:11434/api/tags > /dev/null 2>&1; then
    echo "Ollama is not running! Please start Ollama first."
    exit 1
fi

# Build all modules
echo "Building modules..."
mvn clean install -q

# Start assistant-core
echo "Starting assistant-core..."
cd assistant-core
mvn spring-boot:run > ../logs/core.log 2>&1 &
CORE_PID=$!
cd ..

# Wait for core service
echo "Waiting for core service to start..."
while ! curl -s http://localhost:8080/actuator/health > /dev/null 2>&1; do
    sleep 2
done

# Start assistant-web
echo "Starting assistant-web..."
cd assistant-web
mvn spring-boot:run > ../logs/web.log 2>&1 &
WEB_PID=$!
cd ..

# Wait for web service
echo "Waiting for web service to start..."
while ! curl -s http://localhost:8081 > /dev/null 2>&1; do
    sleep 2
done

# Start assistant-telegram
echo "Starting assistant-telegram..."
cd assistant-telegram
mvn spring-boot:run > ../logs/telegram.log 2>&1 &
TELEGRAM_PID=$!
cd ..

echo "All services started successfully!"
echo "Web interface: http://localhost:8081"
echo "Core API: http://localhost:8080"
echo "Telegram bot: Running on port 8082"
echo ""
echo "PIDs: Core=$CORE_PID, Web=$WEB_PID, Telegram=$TELEGRAM_PID"
echo "Logs are in ./logs/ directory"
echo "Press Ctrl+C to stop all services"

# Create logs directory
mkdir -p logs

# Wait for interrupt
trap 'kill $CORE_PID $WEB_PID $TELEGRAM_PID; exit' INT
wait