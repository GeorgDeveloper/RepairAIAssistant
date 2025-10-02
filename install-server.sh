#!/bin/bash

# =============================================================================
# Автоматическая установка Repair AI Assistant v2.0 на Debian 12.6
# =============================================================================

set -e  # Остановка при любой ошибке

# Цвета для вывода
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Логирование
LOG_FILE="/var/log/repair-ai-install.log"
exec > >(tee -a ${LOG_FILE})
exec 2>&1

# Функции для вывода
print_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

print_header() {
    echo -e "\n${BLUE}============================================${NC}"
    echo -e "${BLUE} $1${NC}"
    echo -e "${BLUE}============================================${NC}\n"
}

# Проверка прав root
check_root() {
    if [[ $EUID -ne 0 ]]; then
        print_error "Этот скрипт должен быть запущен с правами root"
        print_info "Используйте: sudo $0"
        exit 1
    fi
}

# Проверка версии ОС
check_os() {
    print_header "Проверка операционной системы"
    
    if [[ ! -f /etc/debian_version ]]; then
        print_error "Этот скрипт предназначен для Debian"
        exit 1
    fi
    
    DEBIAN_VERSION=$(cat /etc/debian_version)
    print_info "Обнаружена версия Debian: $DEBIAN_VERSION"
    
    # Проверяем, что это Debian 12.x
    if [[ ! $DEBIAN_VERSION =~ ^12\. ]]; then
        print_warning "Скрипт тестировался на Debian 12.6. Текущая версия: $DEBIAN_VERSION"
        read -p "Продолжить установку? (y/N): " -n 1 -r
        echo
        if [[ ! $REPLY =~ ^[Yy]$ ]]; then
            exit 1
        fi
    fi
    
    print_success "Операционная система совместима"
}

# Обновление системы
update_system() {
    print_header "Обновление системы"
    
    print_info "Обновление списка пакетов..."
    apt-get update -y
    
    print_info "Обновление установленных пакетов..."
    apt-get upgrade -y
    
    print_info "Установка базовых пакетов..."
    apt-get install -y \
        curl \
        wget \
        gnupg \
        lsb-release \
        ca-certificates \
        software-properties-common \
        apt-transport-https \
        unzip \
        git \
        htop \
        nano \
        net-tools \
        ufw
    
    print_success "Система обновлена"
}

# Установка Java 17
install_java() {
    print_header "Установка Java 17"
    
    if command -v java &> /dev/null; then
        JAVA_VERSION=$(java -version 2>&1 | head -n1 | awk -F '"' '{print $2}')
        print_info "Java уже установлена: $JAVA_VERSION"
        
        if [[ $JAVA_VERSION =~ ^17\. ]]; then
            print_success "Java 17 уже установлена"
            return
        else
            print_warning "Установлена неподходящая версия Java: $JAVA_VERSION"
        fi
    fi
    
    print_info "Установка OpenJDK 17..."
    apt-get install -y openjdk-17-jdk openjdk-17-jre
    
    # Настройка JAVA_HOME
    JAVA_HOME_PATH=$(readlink -f /usr/bin/java | sed "s:bin/java::")
    echo "export JAVA_HOME=$JAVA_HOME_PATH" >> /etc/environment
    echo "export PATH=\$PATH:\$JAVA_HOME/bin" >> /etc/environment
    
    # Применяем переменные окружения
    source /etc/environment
    
    # Проверка установки
    java -version
    print_success "Java 17 установлена"
}

# Установка Maven
install_maven() {
    print_header "Установка Apache Maven"
    
    if command -v mvn &> /dev/null; then
        MVN_VERSION=$(mvn -version | head -n1 | awk '{print $3}')
        print_info "Maven уже установлен: $MVN_VERSION"
        return
    fi
    
    print_info "Установка Maven..."
    apt-get install -y maven
    
    # Проверка установки
    mvn -version
    print_success "Maven установлен"
}

# Установка Docker
install_docker() {
    print_header "Установка Docker"
    
    if command -v docker &> /dev/null; then
        DOCKER_VERSION=$(docker --version | awk '{print $3}' | sed 's/,//')
        print_info "Docker уже установлен: $DOCKER_VERSION"
    else
        print_info "Добавление официального GPG ключа Docker..."
        curl -fsSL https://download.docker.com/linux/debian/gpg | gpg --dearmor -o /usr/share/keyrings/docker-archive-keyring.gpg
        
        print_info "Добавление репозитория Docker..."
        echo "deb [arch=$(dpkg --print-architecture) signed-by=/usr/share/keyrings/docker-archive-keyring.gpg] https://download.docker.com/linux/debian $(lsb_release -cs) stable" | tee /etc/apt/sources.list.d/docker.list > /dev/null
        
        print_info "Обновление списка пакетов..."
        apt-get update -y
        
        print_info "Установка Docker..."
        apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin
        
        print_success "Docker установлен"
    fi
    
    # Запуск и автозагрузка Docker
    systemctl start docker
    systemctl enable docker
    
    # Добавление пользователя в группу docker (если не root)
    if [[ -n "$SUDO_USER" ]]; then
        usermod -aG docker $SUDO_USER
        print_info "Пользователь $SUDO_USER добавлен в группу docker"
    fi
    
    # Проверка установки
    docker --version
    print_success "Docker настроен и запущен"
}

# Установка Docker Compose
install_docker_compose() {
    print_header "Установка Docker Compose"
    
    if command -v docker-compose &> /dev/null; then
        COMPOSE_VERSION=$(docker-compose --version | awk '{print $4}' | sed 's/,//')
        print_info "Docker Compose уже установлен: $COMPOSE_VERSION"
        return
    fi
    
    print_info "Установка Docker Compose..."
    apt-get install -y docker-compose-plugin
    
    # Создание символической ссылки для совместимости
    if [[ ! -f /usr/local/bin/docker-compose ]]; then
        ln -s /usr/libexec/docker/cli-plugins/docker-compose /usr/local/bin/docker-compose
    fi
    
    # Проверка установки
    docker-compose --version
    print_success "Docker Compose установлен"
}

# Создание пользователя для приложения
create_app_user() {
    print_header "Создание пользователя приложения"
    
    APP_USER="repairai"
    APP_HOME="/opt/repair-ai-assistant"
    
    if id "$APP_USER" &>/dev/null; then
        print_info "Пользователь $APP_USER уже существует"
    else
        print_info "Создание пользователя $APP_USER..."
        useradd -r -m -d $APP_HOME -s /bin/bash $APP_USER
        usermod -aG docker $APP_USER
        print_success "Пользователь $APP_USER создан"
    fi
    
    # Создание директорий
    mkdir -p $APP_HOME/{logs,uploads,manuals,analysis,data}
    chown -R $APP_USER:$APP_USER $APP_HOME
    
    print_success "Директории приложения созданы"
}

# Настройка MySQL
setup_mysql() {
    print_header "Настройка MySQL"
    
    print_info "Запуск MySQL контейнера..."
    
    # Создаем docker-compose.yml для MySQL
    cat > /tmp/mysql-compose.yml << 'EOF'
version: '3.8'
services:
  mysql:
    image: mysql:8.0
    container_name: repair_mysql
    ports:
      - "3306:3306"
    volumes:
      - mysql_data:/var/lib/mysql
      - ./mysql-init:/docker-entrypoint-initdb.d
    environment:
      - MYSQL_ROOT_PASSWORD=rootPass
      - MYSQL_DATABASE=monitoring_bd
      - MYSQL_USER=dba
      - MYSQL_PASSWORD=dbaPass
    restart: unless-stopped
    command: --default-authentication-plugin=mysql_native_password --character-set-server=utf8mb4 --collation-server=utf8mb4_unicode_ci --bind-address=0.0.0.0

volumes:
  mysql_data:
    driver: local
EOF
    
    # Создаем скрипт инициализации базы данных
    mkdir -p /tmp/mysql-init
    cat > /tmp/mysql-init/01-init.sql << 'EOF'
-- Создание базы данных с правильной кодировкой
CREATE DATABASE IF NOT EXISTS monitoring_bd CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Создание пользователей и предоставление прав
CREATE USER IF NOT EXISTS 'dba'@'%' IDENTIFIED BY 'dbaPass';
GRANT ALL PRIVILEGES ON monitoring_bd.* TO 'dba'@'%';

-- Создание пользователя для внешних подключений (DBeaver)
CREATE USER IF NOT EXISTS 'admin'@'%' IDENTIFIED BY 'AdminPass123!';
GRANT ALL PRIVILEGES ON *.* TO 'admin'@'%' WITH GRANT OPTION;

-- Создание пользователя только для чтения (для безопасности)
CREATE USER IF NOT EXISTS 'readonly'@'%' IDENTIFIED BY 'ReadOnlyPass123!';
GRANT SELECT ON monitoring_bd.* TO 'readonly'@'%';

-- Создание основных таблиц (базовая структура)
USE monitoring_bd;

-- Таблица записей обслуживания оборудования
CREATE TABLE IF NOT EXISTS equipment_maintenance_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    machine_name VARCHAR(255) NOT NULL,
    description TEXT,
    failure_type VARCHAR(255),
    machine_downtime DECIMAL(10,2),
    comments TEXT,
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_machine_name (machine_name),
    INDEX idx_failure_type (failure_type),
    INDEX idx_created_date (created_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Таблица сводки решений
CREATE TABLE IF NOT EXISTS summary_of_solutions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    problem_description TEXT NOT NULL,
    solution_description TEXT NOT NULL,
    equipment_type VARCHAR(255),
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_equipment_type (equipment_type),
    INDEX idx_created_date (created_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Таблица отчетов о поломках
CREATE TABLE IF NOT EXISTS breakdown_reports (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    id_code VARCHAR(100) UNIQUE NOT NULL,
    equipment_name VARCHAR(255) NOT NULL,
    breakdown_description TEXT,
    repair_actions TEXT,
    downtime_hours DECIMAL(10,2),
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_id_code (id_code),
    INDEX idx_equipment_name (equipment_name),
    INDEX idx_created_date (created_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Таблица руководств
CREATE TABLE IF NOT EXISTS manuals (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    content LONGTEXT,
    equipment_type VARCHAR(255),
    file_path VARCHAR(500),
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_title (title),
    INDEX idx_equipment_type (equipment_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Вставка тестовых данных
INSERT IGNORE INTO equipment_maintenance_records (machine_name, description, failure_type, machine_downtime, comments) VALUES
('Пресс GK270', 'Утечка масла в гидравлической системе', 'Гидравлическая неисправность', 4.5, 'Заменены уплотнения, проверена система'),
('Форматор F150', 'Не работает система подачи азота', 'Пневматическая неисправность', 2.0, 'Очищены фильтры, заменен клапан'),
('Насос P200', 'Повышенная вибрация и шум', 'Механическая неисправность', 6.0, 'Заменены подшипники, отбалансирован ротор');

INSERT IGNORE INTO summary_of_solutions (problem_description, solution_description, equipment_type) VALUES
('Утечка масла в гидросистеме', 'Проверить состояние уплотнений, заменить изношенные детали, проверить давление в системе', 'Гидравлическое оборудование'),
('Не работает подача азота', 'Очистить фильтры, проверить клапаны, убедиться в правильности настроек давления', 'Пневматическое оборудование'),
('Повышенная вибрация насоса', 'Проверить крепления, состояние подшипников, отбалансировать ротор', 'Насосное оборудование');

INSERT IGNORE INTO breakdown_reports (id_code, equipment_name, breakdown_description, repair_actions, downtime_hours) VALUES
('BR001', 'Пресс GK270', 'Критическая утечка масла', 'Полная замена гидравлических уплотнений', 8.0),
('BR002', 'Форматор F150', 'Отказ системы азота', 'Замена пневматических клапанов', 4.0),
('BR003', 'Насос P200', 'Механический износ', 'Замена подшипникового узла', 12.0);

FLUSH PRIVILEGES;
EOF
    
    # Запуск MySQL
    cd /tmp
    docker-compose -f mysql-compose.yml up -d
    
    # Ожидание запуска MySQL
    print_info "Ожидание запуска MySQL..."
    sleep 30
    
    # Проверка подключения
    for i in {1..10}; do
        if docker exec repair_mysql mysql -u dba -pdbaPass -e "SELECT 1;" &>/dev/null; then
            print_success "MySQL успешно настроен и запущен"
            break
        else
            print_info "Попытка подключения к MySQL ($i/10)..."
            sleep 10
        fi
    done
    
    # Очистка временных файлов
    rm -rf /tmp/mysql-compose.yml /tmp/mysql-init
}

# Установка и настройка ChromaDB
setup_chromadb() {
    print_header "Настройка ChromaDB"
    
    print_info "Запуск ChromaDB контейнера..."
    
    docker run -d \
        --name repair_chromadb \
        --restart unless-stopped \
        -p 8000:8000 \
        -v chroma_data:/chroma/chroma \
        -e CHROMA_HOST=0.0.0.0 \
        -e CHROMA_PORT=8000 \
        -e CHROMA_LOG_LEVEL=INFO \
        chromadb/chroma:0.4.15
    
    # Ожидание запуска ChromaDB
    print_info "Ожидание запуска ChromaDB..."
    sleep 20
    
    # Проверка работы ChromaDB
    for i in {1..5}; do
        if curl -s http://localhost:8000/api/v1/heartbeat &>/dev/null; then
            print_success "ChromaDB успешно запущен"
            break
        else
            print_info "Ожидание запуска ChromaDB ($i/5)..."
            sleep 10
        fi
    done
}

# Установка и настройка Ollama
setup_ollama() {
    print_header "Установка и настройка Ollama"
    
    print_info "Запуск Ollama контейнера..."
    
    docker run -d \
        --name repair_ollama \
        --restart unless-stopped \
        -p 11434:11434 \
        -v ollama_data:/root/.ollama \
        -e OLLAMA_HOST=0.0.0.0 \
        ollama/ollama:latest
    
    # Ожидание запуска Ollama
    print_info "Ожидание запуска Ollama..."
    sleep 30
    
    # Проверка работы Ollama
    for i in {1..5}; do
        if curl -s http://localhost:11434/api/tags &>/dev/null; then
            print_success "Ollama успешно запущен"
            break
        else
            print_info "Ожидание запуска Ollama ($i/5)..."
            sleep 10
        fi
    done
    
    # Установка моделей
    print_info "Установка модели deepseek-r1:latest..."
    docker exec repair_ollama ollama pull deepseek-r1:latest
    
    print_info "Установка модели nomic-embed-text..."
    docker exec repair_ollama ollama pull nomic-embed-text
    
    # Проверка установленных моделей
    print_info "Проверка установленных моделей..."
    docker exec repair_ollama ollama list
    
    print_success "Ollama и модели установлены"
}

# Настройка файрвола
setup_firewall() {
    print_header "Настройка файрвола"
    
    print_info "Настройка UFW..."
    
    # Включение UFW
    ufw --force enable
    
    # Разрешение SSH
    ufw allow ssh
    
    # Разрешение портов приложения
    ufw allow 8080/tcp comment 'Repair AI Core'
    ufw allow 8081/tcp comment 'Repair AI Web'
    ufw allow 8082/tcp comment 'Repair AI Telegram'
    
    # Разрешение портов баз данных
    ufw allow 3306/tcp comment 'MySQL external access'
    ufw allow from 127.0.0.1 to any port 8000 comment 'ChromaDB local'
    ufw allow from 127.0.0.1 to any port 11434 comment 'Ollama local'
    
    # Показать статус
    ufw status verbose
    
    print_success "Файрвол настроен"
}

# Создание systemd сервисов
create_systemd_services() {
    print_header "Создание systemd сервисов"
    
    APP_HOME="/opt/repair-ai-assistant"
    
    # Сервис для Core модуля
    cat > /etc/systemd/system/repair-ai-core.service << EOF
[Unit]
Description=Repair AI Assistant Core Service
After=network.target docker.service
Requires=docker.service

[Service]
Type=simple
User=repairai
Group=repairai
WorkingDirectory=$APP_HOME
ExecStart=/usr/bin/java -Xmx4g -Xms2g -jar $APP_HOME/assistant-core.jar
ExecStop=/bin/kill -TERM \$MAINPID
Restart=always
RestartSec=10
StandardOutput=journal
StandardError=journal
Environment=JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64

[Install]
WantedBy=multi-user.target
EOF

    # Сервис для Web модуля
    cat > /etc/systemd/system/repair-ai-web.service << EOF
[Unit]
Description=Repair AI Assistant Web Service
After=network.target repair-ai-core.service
Requires=repair-ai-core.service

[Service]
Type=simple
User=repairai
Group=repairai
WorkingDirectory=$APP_HOME
ExecStart=/usr/bin/java -Xmx2g -Xms1g -jar $APP_HOME/assistant-web.jar
ExecStop=/bin/kill -TERM \$MAINPID
Restart=always
RestartSec=10
StandardOutput=journal
StandardError=journal
Environment=JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64

[Install]
WantedBy=multi-user.target
EOF

    # Сервис для Telegram модуля
    cat > /etc/systemd/system/repair-ai-telegram.service << EOF
[Unit]
Description=Repair AI Assistant Telegram Service
After=network.target repair-ai-core.service
Requires=repair-ai-core.service

[Service]
Type=simple
User=repairai
Group=repairai
WorkingDirectory=$APP_HOME
ExecStart=/usr/bin/java -Xmx1g -Xms512m -jar $APP_HOME/assistant-telegram.jar
ExecStop=/bin/kill -TERM \$MAINPID
Restart=always
RestartSec=10
StandardOutput=journal
StandardError=journal
Environment=JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64

[Install]
WantedBy=multi-user.target
EOF

    # Перезагрузка systemd
    systemctl daemon-reload
    
    print_success "Systemd сервисы созданы"
}

# Создание скриптов управления
create_management_scripts() {
    print_header "Создание скриптов управления"
    
    APP_HOME="/opt/repair-ai-assistant"
    
    # Скрипт запуска
    cat > $APP_HOME/start.sh << 'EOF'
#!/bin/bash

echo "Запуск Repair AI Assistant..."

# Запуск Docker контейнеров
echo "Запуск баз данных..."
docker start repair_mysql repair_chromadb repair_ollama

# Ожидание запуска баз данных
sleep 30

# Запуск сервисов приложения
echo "Запуск сервисов приложения..."
sudo systemctl start repair-ai-core
sleep 10
sudo systemctl start repair-ai-web
sudo systemctl start repair-ai-telegram

echo "Проверка статуса сервисов..."
sudo systemctl status repair-ai-core --no-pager -l
sudo systemctl status repair-ai-web --no-pager -l
sudo systemctl status repair-ai-telegram --no-pager -l

echo "Repair AI Assistant запущен!"
echo "Core API: http://localhost:8080"
echo "Web Interface: http://localhost:8081"
echo "Telegram Bot: активен"
EOF

    # Скрипт остановки
    cat > $APP_HOME/stop.sh << 'EOF'
#!/bin/bash

echo "Остановка Repair AI Assistant..."

# Остановка сервисов приложения
echo "Остановка сервисов приложения..."
sudo systemctl stop repair-ai-telegram
sudo systemctl stop repair-ai-web
sudo systemctl stop repair-ai-core

# Остановка Docker контейнеров
echo "Остановка баз данных..."
docker stop repair_mysql repair_chromadb repair_ollama

echo "Repair AI Assistant остановлен!"
EOF

    # Скрипт проверки статуса
    cat > $APP_HOME/status.sh << 'EOF'
#!/bin/bash

echo "=== Статус Repair AI Assistant ==="
echo

echo "Docker контейнеры:"
docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}" --filter name=repair_

echo
echo "Systemd сервисы:"
sudo systemctl status repair-ai-core --no-pager -l | head -3
sudo systemctl status repair-ai-web --no-pager -l | head -3
sudo systemctl status repair-ai-telegram --no-pager -l | head -3

echo
echo "Проверка портов:"
netstat -tlnp | grep -E ':(3306|8000|8080|8081|8082|11434)'

echo
echo "Тест API:"
curl -s http://localhost:8080/api/v2/health || echo "Core API недоступен"
curl -s http://localhost:8000/api/v1/heartbeat || echo "ChromaDB недоступен"
curl -s http://localhost:11434/api/tags || echo "Ollama недоступен"
EOF

    # Скрипт обновления
    cat > $APP_HOME/update.sh << 'EOF'
#!/bin/bash

echo "Обновление Repair AI Assistant..."

# Остановка сервисов
./stop.sh

# Резервное копирование
echo "Создание резервной копии..."
mkdir -p backups
cp *.jar backups/ 2>/dev/null || true

# Здесь должна быть логика обновления JAR файлов
echo "Поместите новые JAR файлы в директорию $PWD"
echo "Затем запустите ./start.sh"
EOF

    # Делаем скрипты исполняемыми
    chmod +x $APP_HOME/*.sh
    chown repairai:repairai $APP_HOME/*.sh
    
    print_success "Скрипты управления созданы"
}

# Создание конфигурационных файлов
create_config_files() {
    print_header "Создание конфигурационных файлов"
    
    APP_HOME="/opt/repair-ai-assistant"
    
    # Копируем основной конфигурационный файл
    cp application.yml $APP_HOME/ 2>/dev/null || {
        # Создаем конфигурационный файл, если его нет
        cat > $APP_HOME/application.yml << 'EOF'
# Repair AI Assistant Configuration
app:
  name: Repair AI Assistant
  version: 2.0.0

server:
  core:
    port: 8080
  web:
    port: 8081
  telegram:
    port: 8082

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/monitoring_bd?useUnicode=true&characterEncoding=UTF-8
    username: dba
    password: dbaPass
    driver-class-name: com.mysql.cj.jdbc.Driver

ai:
  enabled: true
  ollama:
    url: http://localhost:11434
    chat-model: deepseek-r1:latest
    embedding-model: nomic-embed-text
    temperature: 0.7
    timeout: 30000
  chroma:
    url: http://localhost:8000
    collection-name: repair_knowledge
  migration:
    auto-migrate: true
    batch-size: 100

telegram:
  bot:
    enabled: true
    token: ${TELEGRAM_BOT_TOKEN:}
    username: repair_ai_bot

logging:
  level:
    ru.georgdeveloper: DEBUG
  file:
    name: logs/repair-assistant.log
EOF
    }
    
    # Docker Compose файл
    cat > $APP_HOME/docker-compose.yml << 'EOF'
version: '3.8'

services:
  mysql:
    image: mysql:8.0
    container_name: repair_mysql
    ports:
      - "3306:3306"
    volumes:
      - mysql_data:/var/lib/mysql
    environment:
      - MYSQL_ROOT_PASSWORD=rootPass
      - MYSQL_DATABASE=monitoring_bd
      - MYSQL_USER=dba
      - MYSQL_PASSWORD=dbaPass
    restart: unless-stopped
    command: --default-authentication-plugin=mysql_native_password --bind-address=0.0.0.0

  chromadb:
    image: chromadb/chroma:0.4.15
    container_name: repair_chromadb
    ports:
      - "8000:8000"
    volumes:
      - chroma_data:/chroma/chroma
    environment:
      - CHROMA_HOST=0.0.0.0
      - CHROMA_PORT=8000
    restart: unless-stopped

  ollama:
    image: ollama/ollama:latest
    container_name: repair_ollama
    ports:
      - "11434:11434"
    volumes:
      - ollama_data:/root/.ollama
    environment:
      - OLLAMA_HOST=0.0.0.0
    restart: unless-stopped

volumes:
  mysql_data:
  chroma_data:
  ollama_data:
EOF
    
    chown -R repairai:repairai $APP_HOME
    
    print_success "Конфигурационные файлы созданы"
}

# Финальная проверка
final_check() {
    print_header "Финальная проверка установки"
    
    print_info "Проверка установленных компонентов..."
    
    # Java
    if java -version &>/dev/null; then
        print_success "✓ Java установлена"
    else
        print_error "✗ Java не установлена"
    fi
    
    # Maven
    if mvn -version &>/dev/null; then
        print_success "✓ Maven установлен"
    else
        print_error "✗ Maven не установлен"
    fi
    
    # Docker
    if docker --version &>/dev/null; then
        print_success "✓ Docker установлен"
    else
        print_error "✗ Docker не установлен"
    fi
    
    # Docker Compose
    if docker-compose --version &>/dev/null; then
        print_success "✓ Docker Compose установлен"
    else
        print_error "✗ Docker Compose не установлен"
    fi
    
    # Проверка контейнеров
    print_info "Проверка Docker контейнеров..."
    docker ps --format "table {{.Names}}\t{{.Status}}"
    
    # Проверка портов
    print_info "Проверка открытых портов..."
    netstat -tlnp | grep -E ':(3306|8000|8080|8081|8082|11434)' || print_warning "Некоторые порты могут быть недоступны"
    
    print_success "Базовая установка завершена"
}

# Вывод инструкций
print_instructions() {
    print_header "Инструкции по завершению установки"
    
    echo -e "${GREEN}Установка базовых компонентов завершена!${NC}"
    echo
    echo -e "${YELLOW}Следующие шаги:${NC}"
    echo
    echo "1. Скопируйте JAR файлы приложения в /opt/repair-ai-assistant/"
    echo "   - assistant-core.jar"
    echo "   - assistant-web.jar"
    echo "   - assistant-telegram.jar"
    echo
    echo "2. Настройте Telegram бот токен:"
    echo "   export TELEGRAM_BOT_TOKEN='your_bot_token'"
    echo "   echo 'export TELEGRAM_BOT_TOKEN=\"your_bot_token\"' >> /etc/environment"
    echo
    echo "3. Запустите приложение:"
    echo "   cd /opt/repair-ai-assistant"
    echo "   sudo -u repairai ./start.sh"
    echo
    echo "4. Проверьте статус:"
    echo "   sudo -u repairai ./status.sh"
    echo
    echo -e "${BLUE}Полезные команды:${NC}"
    echo "   ./start.sh    - Запуск всех сервисов"
    echo "   ./stop.sh     - Остановка всех сервисов"
    echo "   ./status.sh   - Проверка статуса"
    echo "   ./update.sh   - Обновление приложения"
    echo
    echo -e "${BLUE}Доступ к сервисам:${NC}"
    echo "   Core API: http://localhost:8080"
    echo "   Web Interface: http://localhost:8081"
    echo "   MySQL: localhost:3306"
    echo "   ChromaDB: http://localhost:8000"
    echo "   Ollama: http://localhost:11434"
    echo
    echo -e "${BLUE}Подключение к MySQL через DBeaver:${NC}"
    echo "   Хост: IP_СЕРВЕРА"
    echo "   Порт: 3306"
    echo "   База данных: monitoring_bd"
    echo "   Пользователи:"
    echo "     - admin / AdminPass123! (полные права)"
    echo "     - dba / dbaPass (права на monitoring_bd)"
    echo "     - readonly / ReadOnlyPass123! (только чтение)"
    echo
    echo -e "${BLUE}Логи:${NC}"
    echo "   Установка: $LOG_FILE"
    echo "   Приложение: /opt/repair-ai-assistant/logs/"
    echo "   Systemd: journalctl -u repair-ai-core -f"
    echo
    echo -e "${GREEN}Установка завершена успешно!${NC}"
}

# Основная функция
main() {
    print_header "Repair AI Assistant v2.0 - Автоматическая установка"
    print_info "Начало установки на Debian 12.6"
    print_info "Логи сохраняются в: $LOG_FILE"
    
    check_root
    check_os
    update_system
    install_java
    install_maven
    install_docker
    install_docker_compose
    create_app_user
    setup_mysql
    setup_chromadb
    setup_ollama
    setup_firewall
    create_systemd_services
    create_management_scripts
    create_config_files
    final_check
    print_instructions
    
    print_success "Автоматическая установка завершена!"
}

# Обработка сигналов
trap 'print_error "Установка прервана пользователем"; exit 1' INT TERM

# Запуск основной функции
main "$@"
