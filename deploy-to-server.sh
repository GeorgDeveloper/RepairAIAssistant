#!/bin/bash

# =========================
# Repair AI Assistant - Server Deployment Script
# Быстрый скрипт развертывания на Linux сервере
# =========================

set -euo pipefail

# ---- Config ----
DEPLOY_DIR="/opt/repair-ai"
SERVICE_USER="repair-ai"
DB_NAME="monitoring_bd"
DB_USER="dba"
DB_PASS="dbaPass"

# ---- Helpers ----
log() { echo "[DEPLOY] $*"; }
require_root() { [ "$(id -u)" -eq 0 ] || { echo "Run as root (sudo)"; exit 1; }; }

require_root

log "Starting Repair AI Assistant deployment..."

# ---- System update and dependencies ----
log "Installing system dependencies..."
apt-get update -y
apt-get install -y openjdk-17-jre-headless mysql-server curl lsof

# ---- Create service user ----
log "Creating service user..."
if ! id "$SERVICE_USER" &>/dev/null; then
    useradd -r -s /bin/false -d "$DEPLOY_DIR" "$SERVICE_USER"
fi

# ---- Deploy application ----
log "Deploying application to $DEPLOY_DIR..."
mkdir -p "$DEPLOY_DIR"
cp -r target/* "$DEPLOY_DIR/"
chown -R "$SERVICE_USER:$SERVICE_USER" "$DEPLOY_DIR"
chmod +x "$DEPLOY_DIR"/*.sh

# ---- Configure MySQL ----
log "Configuring MySQL database..."
systemctl enable mysql
systemctl start mysql

# Create database and user
mysql <<SQL
CREATE DATABASE IF NOT EXISTS \`${DB_NAME}\` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS '${DB_USER}'@'localhost' IDENTIFIED BY '${DB_PASS}';
GRANT ALL PRIVILEGES ON \`${DB_NAME}\`.* TO '${DB_USER}'@'localhost';
FLUSH PRIVILEGES;
SQL

# ---- Install Ollama (if not present) ----
if ! command -v ollama &> /dev/null; then
    log "Installing Ollama..."
    curl -fsSL https://ollama.ai/install.sh | sh
fi

# Start Ollama service
log "Starting Ollama service..."
sudo -u "$SERVICE_USER" ollama serve &
sleep 5

# Pull a model (optional)
log "Pulling Llama2 model (this may take a while)..."
sudo -u "$SERVICE_USER" ollama pull llama2 || log "Model pull failed, continuing..."

# ---- Create systemd service ----
log "Creating systemd service..."
cat > /etc/systemd/system/repair-ai.service <<EOF
[Unit]
Description=Repair AI Assistant
After=network.target mysql.service

[Service]
Type=forking
User=$SERVICE_USER
Group=$SERVICE_USER
WorkingDirectory=$DEPLOY_DIR
ExecStart=$DEPLOY_DIR/start.sh
ExecStop=$DEPLOY_DIR/stop.sh
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
EOF

# ---- Enable and start service ----
log "Enabling and starting service..."
systemctl daemon-reload
systemctl enable repair-ai
systemctl start repair-ai

# ---- Configure firewall ----
log "Configuring firewall..."
if command -v ufw >/dev/null 2>&1; then
    ufw --force enable
    ufw allow 8080/tcp
    ufw allow 8081/tcp
    ufw allow 8082/tcp
    ufw allow 3306/tcp
fi

# ---- Wait for services to start ----
log "Waiting for services to start..."
sleep 10

# ---- Verification ----
log "Verifying deployment..."
if systemctl is-active --quiet repair-ai; then
    log "✅ Service is running"
else
    log "❌ Service failed to start"
    systemctl status repair-ai
fi

# Check ports
for port in 8080 8081 8082; do
    if netstat -tlnp | grep -q ":$port "; then
        log "✅ Port $port is listening"
    else
        log "❌ Port $port is not listening"
    fi
done

# ---- Final instructions ----
cat <<'EOF'

============================================================
🎉 DEPLOYMENT COMPLETED!

Your Repair AI Assistant is now running on:
- Web Interface: http://localhost:8081
- Core API: http://localhost:8080  
- Telegram Bot: Port 8082

Management commands:
- Check status: systemctl status repair-ai
- View logs: journalctl -u repair-ai -f
- Stop service: systemctl stop repair-ai
- Start service: systemctl start repair-ai
- Restart service: systemctl restart repair-ai

Application logs: /opt/repair-ai/logs/
============================================================
EOF

log "Deployment completed successfully!"
