# 🚀 Руководство по установке Repair AI Assistant v2.0 на Debian 12.6

## 📋 Обзор

Автоматический скрипт установки настраивает полную инфраструктуру для Repair AI Assistant v2.0 на сервере Debian 12.6.

## ⚡ Быстрая установка

### 1. Подготовка сервера

```bash
# Обновите систему
sudo apt update && sudo apt upgrade -y

# Скачайте скрипты установки
wget https://raw.githubusercontent.com/your-repo/repair-ai-assistant/main/install-server.sh
wget https://raw.githubusercontent.com/your-repo/repair-ai-assistant/main/check-installation.sh

# Или скопируйте файлы на сервер
scp install-server.sh check-installation.sh user@your-server:/tmp/
```

### 2. Запуск установки

```bash
# Сделайте скрипт исполняемым
chmod +x install-server.sh

# Запустите установку с правами root
sudo ./install-server.sh
```

### 3. Проверка установки

```bash
# Сделайте скрипт проверки исполняемым
chmod +x check-installation.sh

# Запустите проверку
./check-installation.sh
```

## 🔧 Что устанавливает скрипт

### Системные компоненты
- ✅ **Java 17** - OpenJDK для запуска приложения
- ✅ **Maven** - Сборка и управление зависимостями

### Базы данных и сервисы (native)
- ✅ **MySQL 8.0** - Основная реляционная БД
  - База: `monitoring_bd`
  - Пользователь: `dba` / `dbaPass`
  - Порт: `3306`
- ✅ **ChromaDB 0.4.15** - Векторная БД для семантического поиска
  - Порт: `8000`
  - Коллекция: `repair_knowledge`
- ✅ **Ollama** - Локальный LLM сервер (systemd)
  - Порт: `11434`
  - Модели: `deepseek-r1:latest`, `nomic-embed-text`

### Системная настройка
- ✅ **Пользователь приложения** - `repairai`
- ✅ **Директории** - `/opt/repair-ai-assistant/`
- ✅ **Systemd сервисы** - автозапуск приложения
- ✅ **Файрвол UFW** - настройка безопасности
- ✅ **Скрипты управления** - start.sh, stop.sh, status.sh

## 📁 Структура после установки

```
/opt/repair-ai-assistant/
├── application.yml          # Конфигурация приложения
├── chroma-env/              # Виртуальное окружение ChromaDB (venv)
├── start.sh                 # Запуск всех сервисов
├── stop.sh                  # Остановка всех сервисов
├── status.sh                # Проверка статуса
├── update.sh                # Обновление приложения
├── logs/                    # Логи приложения
├── uploads/                 # Загруженные файлы
├── manuals/                 # Руководства
├── analysis/                # Результаты анализа
└── data/                    # Данные приложения
```

## 🎯 Завершение установки

### 1. Копирование JAR файлов

После сборки проекта скопируйте JAR файлы:

```bash
# Локально (после mvn clean package)
scp assistant-core/target/assistant-core-*.jar user@server:/opt/repair-ai-assistant/assistant-core.jar
scp assistant-web/target/assistant-web-*.jar user@server:/opt/repair-ai-assistant/assistant-web.jar
scp assistant-telegram/target/assistant-telegram-*.jar user@server:/opt/repair-ai-assistant/assistant-telegram.jar

# На сервере
sudo chown repairai:repairai /opt/repair-ai-assistant/*.jar
```

### 2. Настройка Telegram бота

```bash
# Установите токен бота
sudo -u repairai bash -c 'echo "export TELEGRAM_BOT_TOKEN=\"YOUR_BOT_TOKEN\"" >> ~/.bashrc'

# Или глобально
echo 'export TELEGRAM_BOT_TOKEN="YOUR_BOT_TOKEN"' | sudo tee -a /etc/environment
```

### 3. Запуск приложения

```bash
# Переключитесь на пользователя приложения
sudo -u repairai -i

# Перейдите в директорию приложения
cd /opt/repair-ai-assistant

# Запустите все сервисы
./start.sh

# Проверьте статус
./status.sh
```

## 🔍 Проверка работы

### Автоматическая проверка

```bash
./check-installation.sh
```

### Ручная проверка

```bash
# Проверка API эндпоинтов
curl http://localhost:8080/api/v2/health    # Core API
curl http://localhost:8081/                 # Web Interface
curl http://localhost:8000/api/v1/heartbeat # ChromaDB
curl http://localhost:11434/api/tags        # Ollama

# Проверка сервисов
systemctl status repair-ai-core
systemctl status repair-ai-web
systemctl status repair-ai-telegram
systemctl status mysql
systemctl status chromadb
systemctl status ollama
```

## 🛠️ Управление сервисами

### Скрипты управления

```bash
cd /opt/repair-ai-assistant

# Запуск всех сервисов
sudo -u repairai ./start.sh

# Остановка всех сервисов
sudo -u repairai ./stop.sh

# Проверка статуса
sudo -u repairai ./status.sh

# Обновление приложения
sudo -u repairai ./update.sh
```

### Systemd команды

```bash
# Управление отдельными сервисами
sudo systemctl start repair-ai-core
sudo systemctl stop repair-ai-core
sudo systemctl restart repair-ai-core
sudo systemctl status repair-ai-core

# Включение автозапуска
sudo systemctl enable repair-ai-core
sudo systemctl enable repair-ai-web
sudo systemctl enable repair-ai-telegram
```

### Полезные systemd команды

```bash
sudo systemctl start|stop|restart mysql chromadb ollama
journalctl -u mysql -f
journalctl -u chromadb -f
journalctl -u ollama -f
```

## 📊 Мониторинг

### Логи системы

```bash
# Логи приложения
tail -f /opt/repair-ai-assistant/logs/repair-assistant.log

# Логи systemd сервисов
journalctl -u repair-ai-core -f
journalctl -u repair-ai-web -f
journalctl -u repair-ai-telegram -f

# Логи сервисов
journalctl -u mysql -f
journalctl -u chromadb -f
journalctl -u ollama -f
```

### Мониторинг ресурсов

```bash
# Системные ресурсы
htop
free -h
df -h
```

## 🚨 Устранение неполадок

### Частые проблемы

1. **Сервис не запускается**
   ```bash
   # Проверьте логи
   journalctl -u mysql -f
   journalctl -u chromadb -f
   journalctl -u ollama -f
   ```

2. **Приложение не подключается к БД**
   ```bash
   # Проверьте подключение к MySQL
   mysql -u dba -pdbaPass -e "SELECT 1;"
   
   # Проверьте настройки в application.yml
   ```

3. **Ollama модели не загружаются**
   ```bash
   # Загрузите модели вручную
   ollama pull phi3:mini
   ollama pull nomic-embed-text
   ```

4. **Порты заняты**
   ```bash
   # Найдите процессы, использующие порты
   netstat -tlnp | grep -E ':(3306|8000|8080|8081|8082|11434)'
   
   # Остановите конфликтующие сервисы
   ```

### Переустановка

```bash
# Полная очистка
sudo systemctl stop repair-ai-core repair-ai-web repair-ai-telegram
sudo systemctl stop mysql chromadb ollama
sudo rm -rf /opt/repair-ai-assistant

# Повторная установка
sudo ./install-server.sh
```

## 🔒 Безопасность

### Настройки файрвола

```bash
# Проверка правил UFW
sudo ufw status verbose

# Дополнительные правила (если нужно)
sudo ufw allow from TRUSTED_IP to any port 3306
sudo ufw allow from TRUSTED_IP to any port 8000
```

### Обновление паролей

```bash
# MySQL пароли в docker-compose.yml
# Telegram токен в переменных окружения
# Настройки в application.yml
```

## 📈 Производительность

### Рекомендуемые ресурсы

| Компонент | RAM | CPU | Диск |
|-----------|-----|-----|------|
| MySQL | 1GB | 1 ядро | 5GB |
| ChromaDB | 2GB | 1 ядро | 10GB |
| Ollama | 4GB | 2 ядра | 20GB |
| Приложение | 2GB | 2 ядра | 5GB |
| **Итого** | **8GB+** | **4+ ядра** | **40GB+** |

### Оптимизация

```bash
# Настройка JVM для приложения
export JAVA_OPTS="-Xmx4g -Xms2g -XX:+UseG1GC"

# Настройка Docker ресурсов
# В docker-compose.yml добавить:
# deploy:
#   resources:
#     limits:
#       memory: 4G
#       cpus: '2'
```

## 📞 Поддержка

### Полезные команды

```bash
# Генерация отчета о системе
./check-installation.sh > system-report.txt

# Создание резервной копии
tar -czf repair-ai-backup-$(date +%Y%m%d).tar.gz /opt/repair-ai-assistant

# Экспорт конфигурации Docker
docker-compose -f /opt/repair-ai-assistant/docker-compose.yml config
```

### Контакты

- Документация: README.md
- Логи установки: `/var/log/repair-ai-install.log`
- Отчет проверки: `/tmp/repair-ai-check-report.txt`

---

**Repair AI Assistant v2.0 готов к работе!** 🚀
