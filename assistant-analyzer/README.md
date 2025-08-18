# Equipment Analyzer Module

Модуль для анализа CSV файлов с данными оборудования и создания инструкций по ремонту.

## Функциональность

- Анализ CSV файлов с записями о ремонтах
- Извлечение проблем и решений из комментариев
- Группировка данных по участкам, оборудованию и узлам
- Создание JSON файлов с инструкциями по ремонту

## API Endpoints

- `POST /api/analyzer/upload` - Загрузка и анализ CSV файла
- `POST /api/analyzer/analyze/{fileName}` - Анализ существующего файла
- `GET /api/analyzer/health` - Проверка состояния модуля

## Использование

### Java API
```java
@Autowired
private AnalyzerService analyzerService;

List<RepairInstruction> instructions = analyzerService.analyzeEquipmentFile("data.csv");
```

### Python Script
```bash
python assistant-analyzer/equipment_analyzer.py
```

### REST API
```bash
curl -X POST -F "file=@equipment_data.csv" http://localhost:8080/api/analyzer/upload
```

## Формат входных данных

CSV файл должен содержать колонки:
- `area` - Участок
- `machine_name` - Название оборудования
- `mechanism_node` - Узел механизма
- `comments` - Комментарии с проблемами и решениями

## Формат комментариев

```
Что Произошло: Описание проблемы; Что ты сделал: Описание решения
```

## Выходные данные

JSON файл с массивом инструкций:
```json
[
  {
    "area": "Участок 1",
    "equipment_group": "Пресс П-1258",
    "component": "Гидроцилиндр",
    "problem": "Утечка масла",
    "solution": "Заменить уплотнения; Проверить давление"
  }
]
```