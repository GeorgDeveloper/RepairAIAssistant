# База данных Repair AI Assistant

## Настройка базы данных

### 1. Установка MySQL
Убедитесь, что MySQL Server установлен и запущен.

### 2. Создание базы данных
Выполните SQL скрипт:
```bash
mysql -u dba -p < create_tables.sql
```

Или подключитесь к MySQL и выполните команды из `create_tables.sql`

### 3. Настройка подключения
В `assistant-core/src/main/resources/application.yml`:
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/monitoring_bd?useUnicode=true&characterEncoding=UTF-8
    username: dba
    password: dbaPass
```

## Структура базы данных

### Таблица `repair_records`
- `id` - уникальный идентификатор
- `device_type` - тип устройства (Телевизор, Холодильник и т.д.)
- `problem_description` - описание проблемы
- `solution` - решение проблемы
- `start_date` - дата начала ремонта
- `end_date` - дата окончания ремонта
- `duration_hours` - длительность в часах
- `status` - статус ремонта (COMPLETED, временно закрыто и т.д.)

### Таблица `manuals`
- `id` - уникальный идентификатор
- `device_type` - тип устройства
- `file_name` - имя файла руководства
- `content` - содержимое документа

## Использование

Система автоматически:
1. Подключается к базе данных при запуске
2. Создает таблицы если их нет (через JPA)
3. Загружает данные для AI анализа
4. Предоставляет контекст для deepseek-r1

## Добавление данных

Данные можно добавлять:
- Через SQL команды
- Через веб-интерфейс (планируется)
- Через REST API
- Импорт из файлов