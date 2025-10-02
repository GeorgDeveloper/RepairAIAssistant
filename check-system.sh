#!/bin/bash

# Скрипт проверки состояния Repair AI Assistant v2.0

echo "🔍 Проверка состояния Repair AI Assistant v2.0..."
echo "=================================================="

# Цвета для вывода
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Функция для проверки HTTP эндпоинта
check_http() {
    local url=$1
    local name=$2
    local expected=$3
    
    echo -n "Проверка $name... "
    
    if response=$(curl -s --max-time 10 "$url" 2>/dev/null); then
        if [[ -z "$expected" ]] || echo "$response" | grep -q "$expected"; then
            echo -e "${GREEN}✓ OK${NC}"
            return 0
        else
            echo -e "${RED}✗ Неожиданный ответ${NC}"
            echo "  Ответ: $response"
            return 1
        fi
    else
        echo -e "${RED}✗ Недоступен${NC}"
        return 1
    fi
}

# Функция для проверки Docker контейнера
check_docker_container() {
    local container_name=$1
    local service_name=$2
    
    echo -n "Проверка контейнера $service_name... "
    
    if docker ps --format "table {{.Names}}" | grep -q "$container_name"; then
        status=$(docker ps --format "table {{.Names}}\t{{.Status}}" | grep "$container_name" | awk '{print $2}')
        if [[ "$status" == "Up" ]]; then
            echo -e "${GREEN}✓ Запущен${NC}"
            return 0
        else
            echo -e "${YELLOW}⚠ Статус: $status${NC}"
            return 1
        fi
    else
        echo -e "${RED}✗ Не найден${NC}"
        return 1
    fi
}

# Функция для проверки порта
check_port() {
    local port=$1
    local service_name=$2
    
    echo -n "Проверка порта $port ($service_name)... "
    
    if netstat -an 2>/dev/null | grep -q ":$port.*LISTEN" || ss -tulpn 2>/dev/null | grep -q ":$port"; then
        echo -e "${GREEN}✓ Открыт${NC}"
        return 0
    else
        echo -e "${RED}✗ Закрыт${NC}"
        return 1
    fi
}

# Проверка системных требований
echo -e "\n${BLUE}1. Системные требования${NC}"
echo "------------------------"

# Проверка Docker
echo -n "Docker установлен... "
if command -v docker &> /dev/null; then
    docker_version=$(docker --version | cut -d' ' -f3 | cut -d',' -f1)
    echo -e "${GREEN}✓ Версия: $docker_version${NC}"
else
    echo -e "${RED}✗ Не установлен${NC}"
fi

# Проверка Docker Compose
echo -n "Docker Compose установлен... "
if command -v docker-compose &> /dev/null; then
    compose_version=$(docker-compose --version | cut -d' ' -f3 | cut -d',' -f1)
    echo -e "${GREEN}✓ Версия: $compose_version${NC}"
else
    echo -e "${RED}✗ Не установлен${NC}"
fi

# Проверка Java
echo -n "Java установлена... "
if command -v java &> /dev/null; then
    java_version=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2)
    echo -e "${GREEN}✓ Версия: $java_version${NC}"
else
    echo -e "${RED}✗ Не установлена${NC}"
fi

# Проверка Maven
echo -n "Maven установлен... "
if command -v mvn &> /dev/null; then
    mvn_version=$(mvn -version 2>/dev/null | head -n 1 | cut -d' ' -f3)
    echo -e "${GREEN}✓ Версия: $mvn_version${NC}"
else
    echo -e "${RED}✗ Не установлен${NC}"
fi

# Проверка свободного места
echo -n "Свободное место на диске... "
free_space=$(df -h . | awk 'NR==2 {print $4}')
echo -e "${GREEN}✓ Доступно: $free_space${NC}"

# Проверка памяти
echo -n "Доступная память... "
if command -v free &> /dev/null; then
    available_ram=$(free -h | awk 'NR==2{print $7}')
    echo -e "${GREEN}✓ Доступно: $available_ram${NC}"
else
    echo -e "${YELLOW}⚠ Не удалось определить${NC}"
fi

# Проверка Docker контейнеров
echo -e "\n${BLUE}2. Docker контейнеры${NC}"
echo "---------------------"

check_docker_container "chromadb" "ChromaDB"
check_docker_container "ollama" "Ollama"
check_docker_container "mysql" "MySQL"

# Проверка портов
echo -e "\n${BLUE}3. Сетевые порты${NC}"
echo "-----------------"

check_port 8000 "ChromaDB"
check_port 11434 "Ollama"
check_port 3306 "MySQL"
check_port 8080 "Приложение"

# Проверка сервисов
echo -e "\n${BLUE}4. HTTP сервисы${NC}"
echo "----------------"

check_http "http://localhost:8000/api/v1/heartbeat" "ChromaDB API" "heartbeat"
check_http "http://localhost:11434/api/tags" "Ollama API" "models"
check_http "http://localhost:8080/api/v2/health" "Приложение API" "healthy"

# Проверка моделей Ollama
echo -e "\n${BLUE}5. Модели Ollama${NC}"
echo "-----------------"

echo -n "Проверка моделей... "
if models_response=$(curl -s http://localhost:11434/api/tags 2>/dev/null); then
    if echo "$models_response" | grep -q "deepseek-r1"; then
        echo -e "${GREEN}✓ deepseek-r1 найдена${NC}"
    else
        echo -e "${RED}✗ deepseek-r1 не найдена${NC}"
    fi
    
    if echo "$models_response" | grep -q "nomic-embed-text"; then
        echo -e "  ${GREEN}✓ nomic-embed-text найдена${NC}"
    else
        echo -e "  ${RED}✗ nomic-embed-text не найдена${NC}"
    fi
else
    echo -e "${RED}✗ Не удалось получить список моделей${NC}"
fi

# Проверка коллекций ChromaDB
echo -e "\n${BLUE}6. ChromaDB коллекции${NC}"
echo "----------------------"

echo -n "Проверка коллекции repair_knowledge... "
if collections_response=$(curl -s http://localhost:8000/api/v1/collections 2>/dev/null); then
    if echo "$collections_response" | grep -q "repair_knowledge"; then
        echo -e "${GREEN}✓ Найдена${NC}"
    else
        echo -e "${YELLOW}⚠ Не найдена (будет создана автоматически)${NC}"
    fi
else
    echo -e "${RED}✗ Ошибка получения коллекций${NC}"
fi

# Тестовый запрос к приложению
echo -e "\n${BLUE}7. Тестирование функциональности${NC}"
echo "--------------------------------"

echo -n "Тестовый запрос к AI... "
test_query='{"query": "Привет, как дела?"}'
if test_response=$(curl -s -X POST http://localhost:8080/api/v2/query \
    -H "Content-Type: application/json" \
    -d "$test_query" 2>/dev/null); then
    
    if echo "$test_response" | grep -q "response"; then
        echo -e "${GREEN}✓ Работает${NC}"
        # Показываем краткий ответ
        response_text=$(echo "$test_response" | grep -o '"response":"[^"]*"' | cut -d'"' -f4 | cut -c1-50)
        echo "  Ответ: $response_text..."
    else
        echo -e "${RED}✗ Неожиданный ответ${NC}"
        echo "  Ответ: $test_response"
    fi
else
    echo -e "${RED}✗ Ошибка запроса${NC}"
fi

# Итоговая сводка
echo -e "\n${BLUE}📊 Итоговая сводка${NC}"
echo "=================="

# Подсчет успешных проверок
total_checks=15
passed_checks=0

# Здесь можно добавить логику подсчета, но для простоты покажем статус
echo "Система проверена. Детали см. выше."

echo -e "\n${BLUE}💡 Полезные команды${NC}"
echo "==================="
echo "Перезапуск всех сервисов:    docker-compose restart"
echo "Просмотр логов ChromaDB:     docker logs chromadb -f"
echo "Просмотр логов Ollama:       docker logs ollama -f"
echo "Просмотр логов приложения:   tail -f assistant-core/logs/repair-assistant.log"
echo "Проверка здоровья:           curl http://localhost:8080/api/v2/health"

echo -e "\n${GREEN}✅ Проверка завершена!${NC}"
