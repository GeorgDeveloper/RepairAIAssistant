# Repair AI Assistant

Система технической поддержки с искусственным интеллектом для анализа и решения проблем ремонта оборудования.

## Архитектура

- **assistant-core** (8080) - Основной API и бизнес-логика
- **assistant-web** (8081) - Веб-интерфейс
- **assistant-telegram** (8082) - Telegram бот
- **assistant-training** - Модуль обучения ИИ
- **assistant-analyzer** (8083) - Модуль анализа CSV файлов

## Быстрый старт

### Требования
- Java 17+
- Maven 3.6+
- MySQL 8.0+
- Ollama с моделью deepseek-r1:latest

### Установка
```bash
# Windows
start.bat

# Linux/Mac
chmod +x start.sh && ./start.sh
```

### Остановка
```bash
# Windows
stop.bat

# Linux/Mac  
./stop.sh
```

## Конфигурация

Единый файл конфигурации: `application.yml`

### База данных
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/monitoring_bd
    username: dba
    password: dbaPass
```

### ИИ модель
```yaml
ai:
  ollama:
    url: http://localhost:11434/api
    model: deepseek-r1:latest
```

### Telegram бот
```yaml
telegram:
  bot:
    token: ${TELEGRAM_BOT_TOKEN}
    username: js_mai_bot
```

## Функциональность

### Основные возможности
- Анализ запросов на естественном языке
- Поиск по базе данных ремонтов
- Генерация инструкций по ремонту
- Статистика и аналитика
- Обучение на исторических данных

### API Endpoints
- `GET /api/health` - Статус системы
- `POST /api/query` - Обработка запросов
- `GET /api/stats` - Статистика
- `POST /api/manual/upload` - Загрузка руководств
- `POST /api/equipment/analyze` - Анализ CSV файлов оборудования

### Веб-интерфейс
- Чат с ИИ ассистентом
- Загрузка документов
- Просмотр статистики

### Telegram бот
- Обработка текстовых запросов
- Загрузка документов
- Получение инструкций

## База данных

### Основные таблицы
- `equipment_maintenance_records` - Записи о ремонтах
- `manuals` - Руководства по эксплуатации
- `repair_records` - История ремонтов

### Поля для ИИ анализа
- `machine_name` - Название оборудования
- `description` - Описание проблемы  
- `failure_type` - Тип неисправности
- `machine_downtime` - Время простоя
- `comments` - Комментарии и решения

## Обучение ИИ

### Автоматическое обучение
- Загрузка при старте системы
- Ежедневное переобучение в 3:00
- Обучение на новых данных из БД

### Источники данных
- `training/query_training_data.jsonl` - Примеры запросов
- `training/repair_instructions.json` - Инструкции по ремонту
- База данных ремонтов - Исторические данные

### Типы запросов
- Поиск по оборудованию
- Статистика поломок
- Инструкции по ремонту
- Анализ времени простоя

## Мониторинг

### Логи
- Файл: `logs/repair-assistant.log`
- Уровень: DEBUG для приложения
- Ротация: ежедневная

### Метрики
- `/actuator/health` - Состояние системы
- `/actuator/metrics` - Метрики производительности

## Разработка

### Структура проекта
```
repair-ai-assistant/
├── application.yml          # Единая конфигурация
├── assistant-core/          # Основной модуль
├── assistant-web/           # Веб-интерфейс  
├── assistant-telegram/      # Telegram бот
├── assistant-training/      # Модуль обучения
├── assistant-analyzer/      # Модуль анализа CSV
└── target/                  # Сборка
```

### Сборка
```bash
mvn clean package
```

### Тестирование
```bash
mvn test
```

## Примеры использования

### Поиск ремонтов
```
Запрос: "Найди ремонты пресса за последний месяц"
Ответ: Список ремонтов с деталями
```

### Инструкции
```
Запрос: "Как устранить утечку масла?"
Ответ: Пошаговая инструкция
```

### Статистика
```
Запрос: "Топ 5 самых долгих ремонтов"
Ответ: Рейтинг с временем простоя
```

## Поддержка

- Логи: `logs/repair-assistant.log`
- Конфигурация: `application.yml`
- База данных: MySQL на порту 3306
- ИИ модель: Ollama на порту 11434