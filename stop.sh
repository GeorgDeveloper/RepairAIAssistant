#!/bin/bash

echo "Stopping Repair AI Assistant services..."

# Kill processes by port
kill $(lsof -t -i:8080) 2>/dev/null
kill $(lsof -t -i:8081) 2>/dev/null
kill $(lsof -t -i:8082) 2>/dev/null

# Kill Spring Boot processes
pkill -f "spring-boot:run" 2>/dev/null

echo "All services stopped."