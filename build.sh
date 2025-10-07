#!/bin/bash

echo "========================================"
echo "    REPAIR AI ASSISTANT - BUILD"
echo "========================================"
echo

# Check if Java is available
echo "[1/8] Checking Java installation..."
if ! command -v java &> /dev/null; then
    echo "❌ Java not found! Please install Java 17 or higher."
    exit 1
fi
echo "✅ Java found"

# Check if Maven is available
echo "[2/8] Checking Maven installation..."
if ! command -v mvn &> /dev/null; then
    echo "❌ Maven not found! Please install Maven."
    exit 1
fi
echo "✅ Maven found"

# Clean and build all modules
echo
echo "[3/8] Cleaning previous builds..."
echo "████████████████████████████████████████ 0%"
mvn clean -q
echo "████████████████████████████████████████ 100%"
echo "✅ Clean completed"

echo
echo "[4/8] Building all modules..."
echo "████████████████████████████████████████ 0%"
mvn package -DskipTests -q
echo "████████████████████████████████████████ 100%"
echo "✅ Build completed"

# Create target directory structure
echo
echo "[5/8] Creating target directory structure..."
echo "████████████████████████████████████████ 0%"
mkdir -p target/{jars,config,logs}
echo "████████████████████████████████████████ 100%"
echo "✅ Directory structure created"

# Copy WAR files
echo
echo "[6/8] Copying application files..."
echo "████████████████████████████████████████ 0%"
cp assistant-core/target/*.war target/jars/ 2>/dev/null
echo "████████████████████████████████████████ 33%"
cp assistant-web/target/*.war target/jars/ 2>/dev/null
echo "████████████████████████████████████████ 66%"
cp assistant-telegram/target/*.war target/jars/ 2>/dev/null
echo "████████████████████████████████████████ 100%"
echo "✅ Application files copied"

# Copy configuration files
echo
echo "[7/8] Copying configuration files..."
echo "████████████████████████████████████████ 0%"
cp assistant-core/src/main/resources/application.yml target/config/core-application.yml 2>/dev/null
echo "████████████████████████████████████████ 33%"
cp assistant-web/src/main/resources/application.yml target/config/web-application.yml 2>/dev/null
echo "████████████████████████████████████████ 66%"
cp assistant-telegram/src/main/resources/application.yml target/config/telegram-application.yml 2>/dev/null
echo "████████████████████████████████████████ 100%"
echo "✅ Configuration files copied"

# Create startup script
echo
echo "[8/8] Creating startup and stop scripts..."
echo "████████████████████████████████████████ 0%"
cat > target/start.sh << 'EOF'
#!/bin/bash

echo "Starting Repair AI Assistant..."

# Check if Ollama is running
echo "Checking Ollama service..."
if ! curl -s http://localhost:11434/api/tags > /dev/null 2>&1; then
    echo "Ollama is not running! Please start Ollama first."
    exit 1
fi

# Start assistant-core
echo "Starting assistant-core..."
java -jar jars/assistant-core-0.0.1-SNAPSHOT.war --spring.config.location=config/core-application.yml > logs/core.log 2>&1 &
CORE_PID=$!

# Wait for core service
echo "Waiting for core service to start..."
while ! curl -s http://localhost:8080/actuator/health > /dev/null 2>&1; do
    sleep 2
done

# Start assistant-web
echo "Starting assistant-web..."
java -jar jars/assistant-web-0.0.1-SNAPSHOT.war --spring.config.location=config/web-application.yml > logs/web.log 2>&1 &
WEB_PID=$!

# Wait for web service
echo "Waiting for web service to start..."
while ! curl -s http://localhost:8081 > /dev/null 2>&1; do
    sleep 2
done

# Start assistant-telegram
echo "Starting assistant-telegram..."
java -jar jars/assistant-telegram-0.0.1-SNAPSHOT.war --spring.config.location=config/telegram-application.yml > logs/telegram.log 2>&1 &
TELEGRAM_PID=$!

echo "All services started successfully!"
echo "Web interface: http://localhost:8081"
echo "Core API: http://localhost:8080"
echo "Telegram bot: Running on port 8082"
echo ""
echo "PIDs: Core=$CORE_PID, Web=$WEB_PID, Telegram=$TELEGRAM_PID"
echo "Logs are in ./logs/ directory"
echo "Press Ctrl+C to stop all services"

# Save PIDs for stop script
echo "$CORE_PID $WEB_PID $TELEGRAM_PID" > .pids

# Wait for interrupt
trap 'kill $CORE_PID $WEB_PID $TELEGRAM_PID; rm -f .pids; exit' INT
wait
EOF

# Create stop script
echo "Creating stop script..."
cat > target/stop.sh << 'EOF'
#!/bin/bash

echo "Stopping Repair AI Assistant services..."

# Kill processes by port
kill $(lsof -t -i:8080) 2>/dev/null
kill $(lsof -t -i:8081) 2>/dev/null
kill $(lsof -t -i:8082) 2>/dev/null

# Kill processes by PID if available
if [ -f .pids ]; then
    kill $(cat .pids) 2>/dev/null
    rm -f .pids
fi

echo "All services stopped."
EOF

# Make scripts executable
chmod +x target/start.sh target/stop.sh
echo "████████████████████████████████████████ 100%"
echo "✅ Scripts created"

echo
echo "========================================"
echo "           BUILD COMPLETED!"
echo "========================================"
echo
echo "📦 Built files location:"
echo "  • WAR files: target/jars/"
echo "  • Configuration: target/config/"
echo "  • Startup script: target/start.sh"
echo "  • Stop script: target/stop.sh"
echo
echo "🚀 To run the application:"
echo "  cd target"
echo "  ./start.sh"
echo
echo "✅ Build successful! System ready to launch."