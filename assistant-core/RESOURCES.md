# Управление ресурсами

## Структура ресурсов

```
src/main/resources/
└── training/
    ├── repair_instructions.json    # Инструкции по ремонту
    └── query_training_data.jsonl   # Обучающие данные для запросов
```

## Использование путей к ресурсам

Все пути к ресурсам централизованы в классе `ResourcePaths`:

```java
import ru.georgdeveloper.assistantcore.config.ResourcePaths;

// Использование констант
ClassPathResource resource = new ClassPathResource(ResourcePaths.REPAIR_INSTRUCTIONS_JSON);
```

## Константы путей

- `REPAIR_INSTRUCTIONS_JSON` - путь к файлу с инструкциями по ремонту
- `QUERY_TRAINING_DATA_JSONL` - путь к файлу с обучающими данными
- `TRAINING_BASE_PATH` - базовый путь к папке training

## Валидация ресурсов

Сервис `ResourceValidationService` автоматически проверяет наличие всех необходимых ресурсов при запуске приложения.

## Сервисы, использующие ресурсы

1. **RepairInstructionsService** - загружает инструкции из `repair_instructions.json`
2. **ModelTrainingService** - использует оба файла для обучения модели
3. **ResourceValidationService** - проверяет доступность ресурсов

## Тестирование

Класс `ResourcePathsTest` содержит тесты для проверки:
- Существования файлов ресурсов
- Корректности констант путей
- Доступности ресурсов через ClassPathResource

## Рекомендации

1. Всегда используйте константы из `ResourcePaths` вместо хардкода путей
2. Проверяйте доступность ресурсов через `ResourceValidationService`
3. Добавляйте новые пути в `ResourcePaths` при добавлении ресурсов
4. Обновляйте тесты при изменении структуры ресурсов