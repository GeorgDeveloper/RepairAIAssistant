# 🚀 Руководство по развертыванию Repair AI Assistant на Linux сервере

## 📋 Предварительные требования

### На сервере Linux должны быть установлены:
- **Java 17 или выше**
- **MySQL 8.0 или выше** 
- **Ollama** (локальный LLM-сервис)
- **curl** и **lsof** утилиты
- **Maven** (для разработки, опционально)

## 🔧 Подготовка сервера

### 1. Установка Java 17
```bash
# Ubuntu/Debian
sudo apt update
sudo apt install openjdk-17-jre-headless

# CentOS/RHEL
sudo yum install java-17-openjdk-headless
```

### 2. Установка MySQL 8.0
```bash
# Ubuntu/Debian
sudo apt install mysql-server

# CentOS/RHEL
sudo yum install mysql-server
```

### 3. Установка Ollama
```bash
curl -fsSL https://ollama.ai/install.sh | sh
ollama serve &
ollama pull llama2  # или другую модель
```

### 4. Установка дополнительных утилит
```bash
sudo apt install curl lsof  # Ubuntu/Debian
# или
sudo yum install curl lsof  # CentOS/RHEL
```

## 📦 Развертывание приложения

### 1. Копирование файлов на сервер
```bash
# Скопируйте папку target/ на сервер
scp -r target/ user@your-server:/opt/repair-ai/
# или
rsync -av target/ user@your-server:/opt/repair-ai/
```

### 2. Настройка прав доступа
```bash
cd /opt/repair-ai
chmod +x start.sh stop.sh
chmod 755 jars/ config/ logs/
```

### 3. Настройка базы данных MySQL
```bash
# Подключитесь к MySQL
sudo mysql -u root -p

# Создайте базу данных и пользователя
CREATE DATABASE monitoring_bd CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'dba'@'localhost' IDENTIFIED BY 'dbaPass';
GRANT ALL PRIVILEGES ON monitoring_bd.* TO 'dba'@'localhost';
FLUSH PRIVILEGES;
EXIT;
```

### 4. Настройка конфигурации
Отредактируйте файлы в папке `config/`:
- `core-application.yml` - настройки основного сервиса
- `web-application.yml` - настройки веб-интерфейса  
- `telegram-application.yml` - настройки Telegram бота

## 🚀 Запуск приложения

### Вариант 1: Прямой запуск
```bash
cd /opt/repair-ai
./start.sh
```

### Вариант 2: Запуск в фоновом режиме
```bash
cd /opt/repair-ai
nohup ./start.sh > startup.log 2>&1 &
```

### Вариант 3: Создание systemd сервиса
```bash
# Создайте файл сервиса
sudo nano /etc/systemd/system/repair-ai.service
```

Содержимое файла `/etc/systemd/system/repair-ai.service`:
```ini
[Unit]
Description=Repair AI Assistant
After=network.target mysql.service

[Service]
Type=forking
User=repair-ai
Group=repair-ai
WorkingDirectory=/opt/repair-ai
ExecStart=/opt/repair-ai/start.sh
ExecStop=/opt/repair-ai/stop.sh
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
```

Активация сервиса:
```bash
# Создайте пользователя для приложения
sudo useradd -r -s /bin/false repair-ai
sudo chown -R repair-ai:repair-ai /opt/repair-ai

# Активируйте сервис
sudo systemctl daemon-reload
sudo systemctl enable repair-ai
sudo systemctl start repair-ai
```

## 🔍 Проверка работы

### Проверка статуса сервисов
```bash
# Проверка портов
netstat -tlnp | grep -E ':(8080|8081|8082)'

# Проверка логов
tail -f logs/core.log
tail -f logs/web.log  
tail -f logs/telegram.log

# Проверка через curl
curl http://localhost:8080/actuator/health
curl http://localhost:8081
```

### Веб-интерфейсы
- **Основное приложение**: http://your-server:8081
- **API Core**: http://your-server:8080
- **Telegram Bot**: работает на порту 8082

## 🛑 Остановка приложения

### Остановка через скрипт
```bash
cd /opt/repair-ai
./stop.sh
```

### Остановка systemd сервиса
```bash
sudo systemctl stop repair-ai
```

## 📊 Мониторинг и логи

### Просмотр логов
```bash
# Все логи
tail -f logs/*.log

# Конкретный сервис
tail -f logs/core.log
tail -f logs/web.log
tail -f logs/telegram.log
```

### Мониторинг процессов
```bash
# Поиск процессов Java
ps aux | grep java

# Использование портов
lsof -i :8080
lsof -i :8081  
lsof -i :8082
```

## 🔧 Устранение неполадок

### Проблема: "Java not found"
```bash
# Проверьте установку Java
java -version
which java

# Установите Java 17
sudo apt install openjdk-17-jre-headless
```

### Проблема: "Ollama is not running"
```bash
# Запустите Ollama
ollama serve &
ollama pull llama2
```

### Проблема: "Port already in use"
```bash
# Найдите процесс, использующий порт
lsof -i :8080
# Убейте процесс
kill -9 <PID>
```

### Проблема: "Database connection failed"
```bash
# Проверьте MySQL
sudo systemctl status mysql
sudo mysql -u dba -p -e "SHOW DATABASES;"
```

## 🔄 Обновление приложения

```bash
# Остановите приложение
./stop.sh

# Скопируйте новые файлы
# (замените старые WAR файлы в jars/)

# Запустите заново
./start.sh
```

## 📝 Дополнительные настройки

### Настройка firewall
```bash
# Откройте необходимые порты
sudo ufw allow 8080
sudo ufw allow 8081
sudo ufw allow 8082
sudo ufw allow 3306  # MySQL
```

### Настройка nginx (опционально)
```nginx
server {
    listen 80;
    server_name your-domain.com;
    
    location / {
        proxy_pass http://localhost:8081;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

---

## ✅ Быстрый старт

1. **Скопируйте папку `target/` на сервер**
2. **Установите Java 17, MySQL, Ollama**
3. **Настройте базу данных**
4. **Запустите**: `cd /opt/repair-ai && ./start.sh`
5. **Проверьте**: http://your-server:8081

🎉 **Готово! Ваше приложение запущено на сервере.**
