# Разовая загрузка нарядов из CSV в MySQL

Скрипт для файлов, полученных с endpoint'ов сверки:

- `GET /api/reconciliation/bd/missing.csv`
- `GET /api/reconciliation/tag/missing.csv`

## Подготовка

```powershell
cd scripts\oneoff_import_missing_cmms
python -m venv .venv
.\.venv\Scripts\activate
pip install -r requirements.txt
copy config.example.env config.env
# отредактируйте MYSQL_* и CMMS_* в config.env
```

### SQL Server (CMMS) на Windows

Используется **pyodbc**, не pymssql. Нужен ODBC-драйвер:

- [Microsoft ODBC Driver 17 for SQL Server](https://learn.microsoft.com/sql/connect/odbc/download-odbc-driver-for-sql-server)

Проверка подключения:

```powershell
python import_from_csv.py --test-cmms
```

Если драйвер не найден — в `config.env` укажите имя из списка:

```powershell
python -c "import pyodbc; print(pyodbc.drivers())"
```

## Проверка без записи в БД

```powershell
python import_from_csv.py --csv C:\path\bd-missing-in-mysql-2026.csv --kind bd --dry-run
```

## Загрузка BD

```powershell
python import_from_csv.py --csv C:\path\bd-missing-in-mysql-2026.csv --kind bd
```

## Загрузка Tag

```powershell
python import_from_csv.py --csv C:\path\tag-missing-in-mysql-2026.csv --kind tag
```

## Исправление уже загруженных строк (пустые production_day, cause, …)

Если записи уже в MySQL с высокими `id`, но без `production_day` / `date` / `cause`:

```powershell
pip install pymssql
# CMMS_* в config.env обязательны для mechanism_node, maintainers

python import_from_csv.py --repair-id-min 186698 --repair-id-max 186705 --repair-table bd
```

Если INSERT прошёл, а постобработка упала на последнем шаге (ошибка `%` в LIKE):

```powershell
python import_from_csv.py --repair-id-min 186979 --repair-id-max 187241 --repair-table bd --post-process-only
```

## Опции

| Параметр | Описание |
|----------|----------|
| `--kind bd\|tag\|auto` | Таблица назначения (`auto` — Tag если в `type_wo` есть Tag) |
| `--dry-run` | Только план вставок, без SQL INSERT |
| `--no-split-long` | Не дробить наряды дольше 24 ч по сменам 08:00 |
| `--no-enrich-cmms` | Не ходить в SQL Server (по умолчанию догрузка **включена**) |
| `--skip-post-process` | Не заполнять `date`, `shift`, `production_day` и др. |
| `--repair-id-min` / `--repair-id-max` | Починить существующие строки по диапазону `id` |

## Что делает скрипт

1. Читает CSV (UTF-8 с BOM).
2. Вставляет в `equipment_maintenance_records` или `tag_maintenance_records`.
3. Ключ дубликата: `code` + `start_bd_t1` + `stop_bd_t4` (как в Java-переносе).
4. Длинные наряды (>1439 мин) по умолчанию режет по границам **08:00** (как `DataTransferService`).
5. Догружает из CMMS (`Assembly`, `Maintainers`, `STTR`, …) если задан `CMMS_HOST`.
6. Заполняет **`production_day`**, `date`, `shift`, `cause`, `staff` в Python (надёжнее SQL SUBSTRING) + JOIN `staff_technical` / `график_работы_104`.

**Cleanup (удаление «отфильтрованных») не вызывается** — наряды с коротким `comments` останутся в БД.

## После загрузки

При необходимости пересчитайте агрегаты доступности за затронутые производственные дни (процедуры из `docs/`, если используете их в эксплуатации).

## Пример полного цикла

```powershell
# 1. Скачать отчёт
Invoke-WebRequest "http://localhost:8084/api/reconciliation/bd/missing.csv?year=2026" -OutFile bd-missing-2026.csv

# 2. Проверить
python import_from_csv.py --csv bd-missing-2026.csv --kind bd --dry-run

# 3. Загрузить
python import_from_csv.py --csv bd-missing-2026.csv --kind bd --enrich-from-cmms
```
