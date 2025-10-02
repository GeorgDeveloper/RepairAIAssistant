#!/bin/bash

# =============================================================================
# Скрипт проверки установки Repair AI Assistant v2.0
# =============================================================================

# Цвета для вывода
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

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

# Проверка системных требований
check_system_requirements() {
    print_header "Проверка системных требований"
    
    # Проверка ОС
    if [[ -f /etc/debian_version ]]; then
        DEBIAN_VERSION=$(cat /etc/debian_version)
        print_success "✓ Debian $DEBIAN_VERSION"
    else
        print_error "✗ Не обнаружена ОС Debian"
    fi
    
    # Проверка памяти
    TOTAL_RAM=$(free -g | awk '/^Mem:/{print $2}')
    if [[ $TOTAL_RAM -ge 8 ]]; then
        print_success "✓ RAM: ${TOTAL_RAM}GB (достаточно)"
    elif [[ $TOTAL_RAM -ge 4 ]]; then
        print_warning "⚠ RAM: ${TOTAL_RAM}GB (минимально)"
    else
        print_error "✗ RAM: ${TOTAL_RAM}GB (недостаточно, требуется минимум 4GB)"
    fi
    
    # Проверка дискового пространства
    AVAILABLE_SPACE=$(df -BG / | awk 'NR==2 {print $4}' | sed 's/G//')
    if [[ $AVAILABLE_SPACE -ge 20 ]]; then
        print_success "✓ Свободное место: ${AVAILABLE_SPACE}GB"
    else
        print_warning "⚠ Свободное место: ${AVAILABLE_SPACE}GB (рекомендуется минимум 20GB)"
    fi
    
    # Проверка CPU
    CPU_CORES=$(nproc)
    if [[ $CPU_CORES -ge 4 ]]; then
        print_success "✓ CPU ядер: $CPU_CORES"
    else
        print_warning "⚠ CPU ядер: $CPU_CORES (рекомендуется минимум 4)"
    fi
}

# Проверка установленного ПО
check_software() {
    print_header "Проверка установленного ПО"
    
    # Java
    if command -v java &> /dev/null; then
        JAVA_VERSION=$(java -version 2>&1 | head -n1 | awk -F '"' '{print $2}')
        if [[ $JAVA_VERSION =~ ^17\. ]]; then
            print_success "✓ Java $JAVA_VERSION"
        else
            print_warning "⚠ Java $JAVA_VERSION (рекомендуется Java 17)"
        fi
    else
        print_error "✗ Java не установлена"
    fi
    
    # Maven
    if command -v mvn &> /dev/null; then
        MVN_VERSION=$(mvn -version 2>/dev/null | head -n1 | awk '{print $3}')
        print_success "✓ Maven $MVN_VERSION"
    else
        print_error "✗ Maven не установлен"
    fi
    
    # Docker
    if command -v docker &> /dev/null; then
        DOCKER_VERSION=$(docker --version | awk '{print $3}' | sed 's/,//')
        print_success "✓ Docker $DOCKER_VERSION"
        
        # Проверка запуска Docker
        if systemctl is-active --quiet docker; then
            print_success "✓ Docker сервис запущен"
        else
            print_error "✗ Docker сервис не запущен"
        fi
    else
        print_error "✗ Docker не установлен"
    fi
    
    # Docker Compose
    if command -v docker-compose &> /dev/null; then
        COMPOSE_VERSION=$(docker-compose --version | awk '{print $4}' | sed 's/,//')
        print_success "✓ Docker Compose $COMPOSE_VERSION"
    else
        print_error "✗ Docker Compose не установлен"
    fi
}

# Проверка Docker контейнеров
check_containers() {
    print_header "Проверка Docker контейнеров"
    
    # Список ожидаемых контейнеров
    EXPECTED_CONTAINERS=("repair_mysql" "repair_chromadb" "repair_ollama")
    
    for container in "${EXPECTED_CONTAINERS[@]}"; do
        if docker ps --format "{{.Names}}" | grep -q "^${container}$"; then
            STATUS=$(docker ps --format "{{.Names}}\t{{.Status}}" | grep "^${container}" | awk '{print $2}')
            print_success "✓ $container ($STATUS)"
        elif docker ps -a --format "{{.Names}}" | grep -q "^${container}$"; then
            print_warning "⚠ $container (остановлен)"
        else
            print_error "✗ $container (не найден)"
        fi
    done
    
    # Показать все контейнеры с repair в имени
    print_info "Все контейнеры Repair AI:"
    docker ps -a --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}" --filter name=repair_ || print_warning "Контейнеры не найдены"
}

# Проверка сетевых портов
check_ports() {
    print_header "Проверка сетевых портов"
    
    # Список ожидаемых портов
    EXPECTED_PORTS=(3306 8000 8080 8081 8082 11434)
    
    for port in "${EXPECTED_PORTS[@]}"; do
        if netstat -tlnp 2>/dev/null | grep -q ":${port} "; then
            SERVICE=$(netstat -tlnp 2>/dev/null | grep ":${port} " | awk '{print $7}' | head -1)
            print_success "✓ Порт $port открыт ($SERVICE)"
        else
            print_warning "⚠ Порт $port закрыт"
        fi
    done
}

# Проверка API эндпоинтов
check_apis() {
    print_header "Проверка API эндпоинтов"
    
    # ChromaDB
    if curl -s --connect-timeout 5 http://localhost:8000/api/v1/heartbeat &>/dev/null; then
        print_success "✓ ChromaDB API доступен"
    else
        print_error "✗ ChromaDB API недоступен"
    fi
    
    # Ollama
    if curl -s --connect-timeout 5 http://localhost:11434/api/tags &>/dev/null; then
        print_success "✓ Ollama API доступен"
        
        # Проверка моделей
        MODELS=$(curl -s http://localhost:11434/api/tags | grep -o '"name":"[^"]*"' | cut -d'"' -f4)
        if echo "$MODELS" | grep -q "deepseek-r1"; then
            print_success "✓ Модель deepseek-r1 установлена"
        else
            print_warning "⚠ Модель deepseek-r1 не найдена"
        fi
        
        if echo "$MODELS" | grep -q "nomic-embed-text"; then
            print_success "✓ Модель nomic-embed-text установлена"
        else
            print_warning "⚠ Модель nomic-embed-text не найдена"
        fi
    else
        print_error "✗ Ollama API недоступен"
    fi
    
    # MySQL
    if command -v mysql &> /dev/null; then
        if mysql -h localhost -u dba -pdbaPass -e "SELECT 1;" &>/dev/null; then
            print_success "✓ MySQL доступен"
        else
            print_error "✗ MySQL недоступен или неверные учетные данные"
        fi
    else
        # Проверка через Docker
        if docker exec repair_mysql mysql -u dba -pdbaPass -e "SELECT 1;" &>/dev/null; then
            print_success "✓ MySQL доступен (через Docker)"
        else
            print_error "✗ MySQL недоступен"
        fi
    fi
    
    # Core API (если запущен)
    if curl -s --connect-timeout 5 http://localhost:8080/api/v2/health &>/dev/null; then
        print_success "✓ Core API доступен"
    else
        print_warning "⚠ Core API недоступен (возможно, приложение не запущено)"
    fi
    
    # Web Interface (если запущен)
    if curl -s --connect-timeout 5 http://localhost:8081/ &>/dev/null; then
        print_success "✓ Web Interface доступен"
    else
        print_warning "⚠ Web Interface недоступен (возможно, приложение не запущено)"
    fi
}

# Проверка systemd сервисов
check_services() {
    print_header "Проверка systemd сервисов"
    
    SERVICES=("repair-ai-core" "repair-ai-web" "repair-ai-telegram")
    
    for service in "${SERVICES[@]}"; do
        if systemctl list-unit-files | grep -q "${service}.service"; then
            if systemctl is-active --quiet $service; then
                print_success "✓ $service (активен)"
            elif systemctl is-enabled --quiet $service; then
                print_warning "⚠ $service (включен, но не активен)"
            else
                print_warning "⚠ $service (отключен)"
            fi
        else
            print_error "✗ $service (сервис не найден)"
        fi
    done
}

# Проверка файлов и директорий
check_files() {
    print_header "Проверка файлов и директорий"
    
    APP_HOME="/opt/repair-ai-assistant"
    
    # Основная директория
    if [[ -d "$APP_HOME" ]]; then
        print_success "✓ Директория приложения: $APP_HOME"
        
        # Проверка владельца
        OWNER=$(stat -c '%U' "$APP_HOME")
        if [[ "$OWNER" == "repairai" ]]; then
            print_success "✓ Владелец директории: $OWNER"
        else
            print_warning "⚠ Владелец директории: $OWNER (ожидается: repairai)"
        fi
    else
        print_error "✗ Директория приложения не найдена: $APP_HOME"
        return
    fi
    
    # JAR файлы
    JAR_FILES=("assistant-core.jar" "assistant-web.jar" "assistant-telegram.jar")
    for jar in "${JAR_FILES[@]}"; do
        if [[ -f "$APP_HOME/$jar" ]]; then
            SIZE=$(du -h "$APP_HOME/$jar" | cut -f1)
            print_success "✓ $jar ($SIZE)"
        else
            print_warning "⚠ $jar не найден"
        fi
    done
    
    # Конфигурационные файлы
    CONFIG_FILES=("application.yml" "docker-compose.yml")
    for config in "${CONFIG_FILES[@]}"; do
        if [[ -f "$APP_HOME/$config" ]]; then
            print_success "✓ $config"
        else
            print_error "✗ $config не найден"
        fi
    done
    
    # Скрипты управления
    SCRIPTS=("start.sh" "stop.sh" "status.sh" "update.sh")
    for script in "${SCRIPTS[@]}"; do
        if [[ -f "$APP_HOME/$script" ]]; then
            if [[ -x "$APP_HOME/$script" ]]; then
                print_success "✓ $script (исполняемый)"
            else
                print_warning "⚠ $script (не исполняемый)"
            fi
        else
            print_error "✗ $script не найден"
        fi
    done
    
    # Директории логов
    if [[ -d "$APP_HOME/logs" ]]; then
        print_success "✓ Директория логов"
    else
        print_warning "⚠ Директория логов не найдена"
    fi
}

# Проверка пользователя приложения
check_user() {
    print_header "Проверка пользователя приложения"
    
    if id "repairai" &>/dev/null; then
        print_success "✓ Пользователь repairai существует"
        
        # Проверка групп
        GROUPS=$(groups repairai)
        if echo "$GROUPS" | grep -q "docker"; then
            print_success "✓ Пользователь repairai в группе docker"
        else
            print_warning "⚠ Пользователь repairai не в группе docker"
        fi
    else
        print_error "✗ Пользователь repairai не найден"
    fi
}

# Проверка файрвола
check_firewall() {
    print_header "Проверка файрвола"
    
    if command -v ufw &> /dev/null; then
        UFW_STATUS=$(ufw status | head -1)
        if echo "$UFW_STATUS" | grep -q "active"; then
            print_success "✓ UFW активен"
            
            # Проверка правил для портов приложения
            PORTS=(8080 8081 8082)
            for port in "${PORTS[@]}"; do
                if ufw status | grep -q "$port"; then
                    print_success "✓ Порт $port разрешен в UFW"
                else
                    print_warning "⚠ Порт $port не найден в правилах UFW"
                fi
            done
        else
            print_warning "⚠ UFW неактивен"
        fi
    else
        print_warning "⚠ UFW не установлен"
    fi
}

# Генерация отчета
generate_report() {
    print_header "Сводный отчет"
    
    REPORT_FILE="/tmp/repair-ai-check-report.txt"
    
    {
        echo "Repair AI Assistant v2.0 - Отчет о проверке системы"
        echo "Дата: $(date)"
        echo "Хост: $(hostname)"
        echo "ОС: $(cat /etc/debian_version 2>/dev/null || echo 'Unknown')"
        echo "Пользователь: $(whoami)"
        echo
        echo "=== Системные ресурсы ==="
        echo "RAM: $(free -h | grep '^Mem:' | awk '{print $2}')"
        echo "CPU: $(nproc) ядер"
        echo "Диск: $(df -h / | awk 'NR==2 {print $4}') свободно"
        echo
        echo "=== Docker контейнеры ==="
        docker ps -a --format "{{.Names}}\t{{.Status}}" --filter name=repair_ 2>/dev/null || echo "Нет контейнеров"
        echo
        echo "=== Сетевые порты ==="
        netstat -tlnp 2>/dev/null | grep -E ':(3306|8000|8080|8081|8082|11434)' || echo "Порты не открыты"
        echo
        echo "=== Systemd сервисы ==="
        for service in repair-ai-core repair-ai-web repair-ai-telegram; do
            if systemctl list-unit-files | grep -q "${service}.service"; then
                echo "$service: $(systemctl is-active $service 2>/dev/null)"
            else
                echo "$service: не найден"
            fi
        done
    } > "$REPORT_FILE"
    
    print_info "Отчет сохранен в: $REPORT_FILE"
}

# Рекомендации по устранению проблем
print_recommendations() {
    print_header "Рекомендации"
    
    echo -e "${YELLOW}Если обнаружены проблемы:${NC}"
    echo
    echo "1. Для запуска контейнеров:"
    echo "   cd /opt/repair-ai-assistant"
    echo "   docker-compose up -d"
    echo
    echo "2. Для установки моделей Ollama:"
    echo "   docker exec repair_ollama ollama pull deepseek-r1:latest"
    echo "   docker exec repair_ollama ollama pull nomic-embed-text"
    echo
    echo "3. Для запуска приложения:"
    echo "   sudo -u repairai /opt/repair-ai-assistant/start.sh"
    echo
    echo "4. Для проверки логов:"
    echo "   journalctl -u repair-ai-core -f"
    echo "   docker logs repair_mysql"
    echo
    echo "5. Для переустановки:"
    echo "   sudo bash install-server.sh"
    echo
    echo -e "${BLUE}Документация: README.md${NC}"
}

# Основная функция
main() {
    print_header "Repair AI Assistant v2.0 - Проверка установки"
    
    check_system_requirements
    check_software
    check_user
    check_files
    check_containers
    check_ports
    check_apis
    check_services
    check_firewall
    generate_report
    print_recommendations
    
    print_success "Проверка завершена!"
}

# Запуск
main "$@"
