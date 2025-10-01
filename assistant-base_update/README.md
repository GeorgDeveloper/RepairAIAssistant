# Assistant Base Update Module

Модуль для ежедневного переноса данных из SQL Server в MySQL.

## Функциональность

### Ежедневный перенос данных
- **Расписание**: Каждый день в 8:00 утра
- **Источник**: SQL Server (CMMS база)
- **Назначение**: MySQL (monitor_bd база)
- **Таблица**: `equipment_maintenance_records`

### Обработка данных

#### 1. Перенос основных данных
- Перенос записей из `REP_BreakdownReport` (SQL Server) в `equipment_maintenance_records` (MySQL)
- Фильтрация по датам: `Date_T1 >= 8:00` ИЛИ `Date_T4 >= 8:00`
- Преобразование временных полей из строкового формата в TIME

#### 2. Обработка дополнительных полей
- **cause**: Извлечение из поля `comments` (паттерн "Cause:")
- **staff**: Извлечение из поля `comments` (паттерн "[(имя)]")
- **date**: Форматирование из `start_bd_t1` в формат DD.MM.YYYY
- **week_number**: Расчет номера недели из даты
- **month_name**: Название месяца на русском языке
- **shift**: Определение смены по времени (1-я: 8:00-19:59, 2-я: 20:00-7:59)
- **failure_type**: Сопоставление с таблицей `staff_technical`
- **crew_de_facto**: Сопоставление с таблицей `staff_technical`
- **crew**: Сопоставление с таблицей `график_работы_104`
- **production_day**: Расчет производственного дня с учетом смен 8:00-8:00

#### 3. Очистка данных
Удаление отфильтрованных записей:
- Ошибочные запросы и ложные вызовы
- Записи с определенной длиной комментариев
- Tag-записи
- Записи "В исполнении"

## Конфигурация

### application.yml
```yaml
data-sync:
  enabled: true
  schedule: "0 0 8 * * *"  # Каждый день в 8:00
  sql_server:
    url: jdbc:sqlserver://pindapp466-39ru:1433;databaseName=CMMS
    username: sa.CMMSreader001
    password: JDFklm512@
    driver: com.microsoft.sqlserver.jdbc.SQLServerDriver
  mysql:
    url: jdbc:mysql://10.130.142.142:3306/monitor_bd
    username: test_user
    password: Password%123
    driver: com.mysql.cj.jdbc.Driver
```

## Запуск

### Автоматический запуск
Модуль автоматически запускается по расписанию (8:00 каждый день).

### Ручной запуск
```bash
# Запуск модуля
mvn spring-boot:run

# Или через JAR
java -jar assistant-base-update-0.0.1-SNAPSHOT.jar
```

### Тестирование
```bash
# Запуск тестов
mvn test

# Запуск с отключенным автоматическим переносом
mvn test -Ddata-sync.enabled=false
```

## Логирование

Все операции логируются с уровнем INFO:
- Количество найденных записей
- Количество успешно перенесенных записей
- Количество ошибок
- Количество обработанных дополнительных полей
- Количество удаленных отфильтрованных записей

## Зависимости

- Spring Boot 3.2.5
- Spring Data JPA
- MySQL Connector/J 8.0.33
- Microsoft SQL Server JDBC Driver 12.4.2
- HikariCP (пул соединений)

## Структура проекта

```
assistant-base_update/
├── src/main/java/ru/georgdeveloper/assistantbaseupdate/
│   ├── AssistantBaseUpdateApplication.java
│   ├── config/
│   │   ├── DatabaseConfig.java
│   │   └── DataSyncProperties.java
│   ├── entity/
│   │   └── ProductionMetricsOnline.java
│   ├── repository/
│   │   └── ProductionMetricsOnlineRepository.java
│   └── service/
│       ├── DataSyncService.java
│       └── DataTransferService.java
└── src/test/java/...
```

## Мониторинг

Модуль предоставляет следующие эндпоинты для мониторинга:
- `/actuator/health` - Статус здоровья
- `/actuator/info` - Информация о приложении
- `/actuator/metrics` - Метрики производительности
