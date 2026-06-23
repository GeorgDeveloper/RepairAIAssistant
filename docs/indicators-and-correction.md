# Показатели главной страницы и корректировка по дням производства

## Где отображаются показатели

- **Главная (index.html)**  
  Таблица показателей участков (BD %, Доступность % за месяц и за «сегодня»), графики BD и Availability, график ППР план/факт, топ поломок.
- **Источник данных:** API `/dashboard/current-metrics?year=&month=` → `MonitoringRepository.getCurrentMetrics(year, month)`.

## Как рассчитываются показатели

### 1. Таблица показателей участков (BD и Доступность)

- **Метод:** `assistant-core` → `MonitoringRepository.getCurrentMetrics(year, month)`.
- **Таблицы БД:** по одной на участок:
  - `report_new_mixing_area`, `report_semifinishing_area`, `report_building_area`, `report_curing_area`, `report_finishig_area`, `report_modules`, `report_plant`.
- **Расчёт:**
  - **BD за месяц:** `AVG(downtime_percentage)` по строкам выбранного года/месяца в соответствующей `report_*`.
  - **BD за «сегодня»:** `AVG(downtime_percentage)` по строке с `production_day = последний день месяца` (или в текущем месяце — предыдущий день).
  - **Доступность за месяц / за сегодня:** то же по полю `availability`.
- **Важно:** сейчас в расчёт месяца попадают **все** дни месяца, для которых есть строка в `report_*`. Исключение дней простоя делается инструментом корректировки (см. ниже).

### 2. Итоговая таблица (availability_stats)

- **Процедура БД:** `monitoring_bd.UpdateAvailabilityStats()` (вызывается в т.ч. через `run_UpdateAvailabilityStats`).
- **Логика:** для **текущего** месяца:
  - удаляет старую запись за этот месяц в `availability_stats`;
  - считает по `report_plant` за этот месяц:
    - `availability_percent` = `AVG(availability)`;
    - `bd_percent` = `AVG(downtime_percentage)`;
    - `breakdowns_count` из `equipment_maintenance_records` за месяц;
    - плановые ремонты и т.д. из `report_plant`.
  - вставляет одну строку в `availability_stats`.
- **Учёт простоя:** по умолчанию в расчёт попадают все дни месяца. Чтобы учитывать только «производственные» дни, процедуру нужно доработать: фильтровать строки `report_plant` по таблице корректировки (см. раздел «Корректировка»).

### 3. Откуда берутся данные в report_*

- **Процедуры по участкам** (например `update_semifinishing_area_report`, `update_plant_reports`, `update_new_mixing_area_report`, и т.д.):
  - заполняют/обновляют таблицы вида `breakdown_of_*` из `equipment_maintenance_records` и `pm_maintenance_records`;
  - обновляют в `report_*` поля: `ttr`, `t2_minus_t1`, `machine_downtime`, `downtime_percentage`, `preventive_maintenance_duration_min`, `quantity_pm_planned`, `quantity_pm_close`, `quantity_tag`, `availability`, при необходимости `pm_time_percentage`.
- **Формулы в процедурах:**
  - `downtime_percentage = (machine_downtime / wt_*_min) * 100`
  - `availability = 100 - ((preventive_maintenance_duration_min + machine_downtime) / wt_*_min) * 100`
- **Рабочее время:** в `report_*` у каждой строки есть столбец рабочего времени (`wt_sa_min`, `wt_p_min`, `wt_nma_min`, `wt_fa_min`, `wt_ca_min`, `wt_ba_min`, `wt_ma_min`). Эти значения должны быть заполнены для каждой даты (например из таблиц `working_time_of_*` или из того же источника, что и календарь производственных дней). Строки в `report_*` по сути «один производственный день — одна строка» с полем `production_day` в формате `dd.MM.yyyy`.

### 4. Рабочее время (working_time_of_*)

- В `application.yml` для каждого участка заданы:
  - `working_time_table`: например `working_time_of_plant`, `working_time_of_semifinishing_area`, …
  - `wt_column`: например `wt_p_min`, `wt_sa_min`, …
- Эти таблицы используются сервисом синхронизации в `assistant-base_update` (например получение рабочего времени на дату). В них хранится «норма» рабочего времени на дату (в минутах). От них зависят корректные значения в `report_*` и, как следствие, BD и доступность.

## Корректировка данных (дни простоя)

Задача: в текущем месяце 9 дней простоя — считать показатели только с 10 по 31 число; в прошлом месяце 3 дня простоя — считать с 1 по 25 число.

### Реализация в приложении

- **Таблицы:** `docs/production_days_correction.sql` — основная таблица и `production_days_correction_range` (несколько диапазонов в месяце). При переходе со старой схемы выполнить `docs/production_days_correction_migrate.sql`.
- **Страница:** Инфопанель → «Корректировка производственных дней» (`/production-days-correction`): год, месяц, один или несколько диапазонов дней (с/по), комментарий. Поддержка простоя в середине месяца (например диапазоны 1–14 и 21–31).
- **ППР (PM) исключены из перерасчёта** — останов завода на плановые ремонты не влияет; график ППР и показатели плановых ремонтов в итогах считаются за весь месяц.

1. **Таблицы БД**
   - `production_days_correction`: `id`, `year`, `month`, `comment`.
   - `production_days_correction_range`: `id`, `correction_id`, `first_production_day`, `last_production_day` (несколько строк на один месяц).

2. **При расчёте показателей в приложении**
   - BD и доступность за месяц: фильтр по производственным дням (все заданные диапазоны). ППР (график план/факт/tag, количество ППР) — без фильтра, за весь месяц.

3. **Итоговая таблица (availability_stats)**
   - В БД нужно подменить процедуру `UpdateAvailabilityStats` на версию из `docs/update_availability_stats_with_production_days.sql`: доступность и BD по производственным дням, поломки — по производственным дням, плановые ремонты и % ППР — за весь месяц.

### Примеры

- Текущий месяц: 9 дней простоя в начале → один диапазон: первый день 10, последний 31. Показатели BD/доступность только за 10–31.
- Простой в середине месяца (например 15–20) → два диапазона: 1–14 и 21–31.
- Прошлый месяц: 3 дня простоя в конце → один диапазон: 1–25.

После внедрения инструмента корректировки (таблица + API + UI) пользователь задаёт для нужного года/месяца первый и последний производственный день; все запросы метрик за этот месяц в приложении автоматически используют только этот диапазон дней.
