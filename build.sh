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

# Copy shell scripts
echo
echo "[7.5/8] Copying shell scripts..."
echo "████████████████████████████████████████ 0%"
cp start.sh target/ 2>/dev/null
echo "████████████████████████████████████████ 50%"
cp stop.sh target/ 2>/dev/null
echo "████████████████████████████████████████ 100%"
echo "✅ Shell scripts copied"

# Make copied scripts executable
chmod +x target/start.sh target/stop.sh
echo "████████████████████████████████████████ 100%"
echo "✅ Scripts made executable"

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