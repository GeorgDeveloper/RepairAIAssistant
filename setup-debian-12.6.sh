#!/usr/bin/env bash
set -euo pipefail

# =========================
# Repair AI Assistant server bootstrap (Debian 12.6)
# - Installs Java 17, MySQL 8
# - Configures MySQL (UTF8MB4, external access)
# - Creates DB/user as per app config
# - Opens firewall for 3306
# - Does NOT start the application
# =========================

# ---- Config (matches application.yml) ----
DB_NAME="monitoring_bd"
DB_USER="dba"
DB_PASS="dbaPass"
MYSQL_PORT="3306"
BIND_ADDRESS="0.0.0.0"

# ---- Helpers ----
log() { echo "[setup] $*"; }
require_root() { [ "$(id -u)" -eq 0 ] || { echo "Run as root (sudo)"; exit 1; }; }

require_root

# ---- Validate distro ----
if ! grep -q "Debian GNU/Linux 12" /etc/os-release; then
  log "Warning: this script is tailored for Debian 12.x. Continuing anyway..."
fi

export DEBIAN_FRONTEND=noninteractive

# ---- System update ----
log "Updating system packages..."
apt-get update -y
apt-get upgrade -y

# ---- Install dependencies ----
log "Installing required packages (Java 17, MySQL, UFW, Maven, utilities)..."
apt-get install -y \
  ca-certificates curl gnupg lsb-release software-properties-common \
  openjdk-17-jre-headless \
  mysql-server \
  ufw \
  maven

# ---- Java env (optional convenience) ----
if [ -d /usr/lib/jvm/java-17-openjdk-amd64 ]; then
  echo 'export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64' >/etc/profile.d/java.sh
  echo 'export PATH=$JAVA_HOME/bin:$PATH' >>/etc/profile.d/java.sh
  chmod 0644 /etc/profile.d/java.sh
fi

# ---- MySQL configuration ----
log "Configuring MySQL for UTF8MB4 and external access..."

MY_CNF_DIR="/etc/mysql/conf.d"
mkdir -p "$MY_CNF_DIR"

# Charset/collation for server
cat >/etc/mysql/conf.d/charset.cnf <<EOF
[mysqld]
character-set-server = utf8mb4
collation-server = utf8mb4_unicode_ci
skip-character-set-client-handshake
EOF

# Bind on all interfaces
MYSQLD_MAIN="/etc/mysql/mysql.conf.d/mysqld.cnf"
if [ -f "$MYSQLD_MAIN" ]; then
  sed -i 's/^\s*bind-address\s*=.*$/bind-address = 0.0.0.0/' "$MYSQLD_MAIN" || true
  if ! grep -qE '^\s*bind-address\s*=\s*0\.0\.0\.0' "$MYSQLD_MAIN"; then
    printf "\n[mysqld]\nbind-address = %s\n" "$BIND_ADDRESS" >> "$MYSQLD_MAIN"
  fi
else
  # Fallback if file layout differs
  cat > /etc/mysql/conf.d/network.cnf <<EOF
[mysqld]
bind-address = ${BIND_ADDRESS}
EOF
fi

# Ensure default port (optional explicitness)
if ! grep -qE '^\s*port\s*=' "$MYSQLD_MAIN" 2>/dev/null; then
  printf "\n[mysqld]\nport = %s\n" "$MYSQL_PORT" >> "$MYSQLD_MAIN" || true
fi

# Restart MySQL to apply config
systemctl enable mysql
systemctl restart mysql

# ---- Secure and provision database ----
log "Creating database and user aligned with application.yml..."
# Use socket auth for root on Debian; run as root without password
mysql --protocol=socket -uroot <<SQL
CREATE DATABASE IF NOT EXISTS \`${DB_NAME}\` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Create/alter user for any host
CREATE USER IF NOT EXISTS '${DB_USER}'@'%' IDENTIFIED BY '${DB_PASS}';
ALTER USER '${DB_USER}'@'%' IDENTIFIED BY '${DB_PASS}';

-- Also ensure localhost variant exists (optional but helpful)
CREATE USER IF NOT EXISTS '${DB_USER}'@'localhost' IDENTIFIED BY '${DB_PASS}';
ALTER USER '${DB_USER}'@'localhost' IDENTIFIED BY '${DB_PASS}';

GRANT ALL PRIVILEGES ON \`${DB_NAME}\`.* TO '${DB_USER}'@'%';
GRANT ALL PRIVILEGES ON \`${DB_NAME}\`.* TO '${DB_USER}'@'localhost';
FLUSH PRIVILEGES;
SQL

# ---- Firewall ----
log "Configuring firewall to allow external MySQL (${MYSQL_PORT}/tcp)..."
if command -v ufw >/dev/null 2>&1; then
  # Enable UFW if inactive
  if ufw status | grep -qi inactive; then
    ufw --force enable
  fi
  ufw allow "${MYSQL_PORT}/tcp"
else
  log "UFW not available; skipping. Ensure port ${MYSQL_PORT} is allowed in your firewall."
fi

# ---- Verification (non-fatal) ----
log "Verifying MySQL connectivity and user privileges..."
set +e
mysql -h 127.0.0.1 -P "${MYSQL_PORT}" -u "${DB_USER}" -p"${DB_PASS}" -e "SHOW DATABASES LIKE '${DB_NAME}';" 2>/dev/null
if [ $? -eq 0 ]; then
  log "MySQL user '${DB_USER}' can access database '${DB_NAME}'."
else
  log "Warning: verification failed. Re-check credentials and MySQL logs."
fi
set -e

# ---- Final notes ----
cat <<'EONOTES'

============================================================
Server bootstrap complete.

Installed:
- OpenJDK 17 (headless)
- MySQL Server 8.x
- UFW (firewall)
- Maven (for builds, optional)

MySQL configured:
- bind-address: 0.0.0.0 (external access enabled)
- Port: 3306
- Charset/Collation: utf8mb4/utf8mb4_unicode_ci
- Database: monitoring_bd
- User: dba
- Password: dbaPass
- Grants: ALL on monitoring_bd.* for dba@'%' and dba@'localhost'

Next steps (manual):
- Deploy and start your application services separately (this script does not start them).
- If a cloud provider firewall is used, allow inbound TCP/3306 there as well.

Security tip:
- Restrict 3306 to your trusted IPs instead of open internet (e.g., ufw allow from <YOUR_IP> to any port 3306).
============================================================
EONOTES