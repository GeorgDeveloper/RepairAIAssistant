-- Рабочее время оборудования на 2026 год по участкам.
-- Норма суток взята из application.yml, блок data-sync.areas (default_wt), минуты:
--   Plant              156960  working_time_of_plant.wt_p_min
--   BuildingArea        23040  working_time_of_building_area.wt_ba_min
--   CuringArea          79200  working_time_of_curing_area.wt_ca_min
--   FinishigArea        27360  working_time_of_finishig_area.wt_fa_min
--   SemifinishingArea   23040  working_time_of_semifinishing_area.wt_sa_min
--   NewMixingArea       10080  working_time_of_new_mixing_area.wt_nma_min
--   Modules              4320  working_time_of_modules.wt_ma_min
--
-- Дата в таблицах — строка dd.MM.yyyy, как её читает DataSyncService.
-- 01.01.2026–11.01.2026 остаются 0 (простой уже записан в report_*).
-- 12.01.2026 — половина суточной нормы (в report_* этот день уже был половинным).
-- У Modules дополнительно 0 на 01.05.2026 и 09.05.2026.
-- Остальные дни 2026 года — полная норма из yml.
--
-- Plant в yml = 156960. Это прежняя сумма участков без Modules
-- (23040 + 74880 + 25920 + 23040 + 10080). С новыми CuringArea и FinishigArea
-- сумма участков без Modules = 162720. Скрипт пишет в Plant значение из yml.
--
-- Повторный запуск безопасен: строки 2026 в working_time_of_* сначала удаляются.

USE monitoring_bd;

START TRANSACTION;

DROP TEMPORARY TABLE IF EXISTS tmp_days_2026;
CREATE TEMPORARY TABLE tmp_days_2026 (
    dt DATE NOT NULL PRIMARY KEY,
    date_str VARCHAR(10) NOT NULL
);

INSERT INTO tmp_days_2026 (dt, date_str)
WITH RECURSIVE d AS (
    SELECT DATE('2026-01-01') AS dt
    UNION ALL
    SELECT dt + INTERVAL 1 DAY FROM d WHERE dt < DATE('2026-12-31')
)
SELECT dt, DATE_FORMAT(dt, '%d.%m.%Y') FROM d;

DELETE FROM working_time_of_plant WHERE `date` LIKE '%.2026';
INSERT INTO working_time_of_plant (`date`, wt_p_min)
SELECT date_str,
       CASE
           WHEN dt BETWEEN '2026-01-01' AND '2026-01-11' THEN 0
           WHEN dt = '2026-01-12' THEN 78480
           ELSE 156960
       END
FROM tmp_days_2026;

DELETE FROM working_time_of_building_area WHERE `date` LIKE '%.2026';
INSERT INTO working_time_of_building_area (`date`, wt_ba_min)
SELECT date_str,
       CASE
           WHEN dt BETWEEN '2026-01-01' AND '2026-01-11' THEN 0
           WHEN dt = '2026-01-12' THEN 11520
           ELSE 23040
       END
FROM tmp_days_2026;

DELETE FROM working_time_of_curing_area WHERE `date` LIKE '%.2026';
INSERT INTO working_time_of_curing_area (`date`, wt_ca_min)
SELECT date_str,
       CASE
           WHEN dt BETWEEN '2026-01-01' AND '2026-01-11' THEN 0
           WHEN dt = '2026-01-12' THEN 39600
           ELSE 79200
       END
FROM tmp_days_2026;

DELETE FROM working_time_of_finishig_area WHERE `date` LIKE '%.2026';
INSERT INTO working_time_of_finishig_area (`date`, wt_fa_min)
SELECT date_str,
       CASE
           WHEN dt BETWEEN '2026-01-01' AND '2026-01-11' THEN 0
           WHEN dt = '2026-01-12' THEN 13680
           ELSE 27360
       END
FROM tmp_days_2026;

DELETE FROM working_time_of_semifinishing_area WHERE `date` LIKE '%.2026';
INSERT INTO working_time_of_semifinishing_area (`date`, wt_sa_min)
SELECT date_str,
       CASE
           WHEN dt BETWEEN '2026-01-01' AND '2026-01-11' THEN 0
           WHEN dt = '2026-01-12' THEN 11520
           ELSE 23040
       END
FROM tmp_days_2026;

DELETE FROM working_time_of_new_mixing_area WHERE `date` LIKE '%.2026';
INSERT INTO working_time_of_new_mixing_area (`date`, wt_nma_min)
SELECT date_str,
       CASE
           WHEN dt BETWEEN '2026-01-01' AND '2026-01-11' THEN 0
           WHEN dt = '2026-01-12' THEN 5040
           ELSE 10080
       END
FROM tmp_days_2026;

DELETE FROM working_time_of_modules WHERE `date` LIKE '%.2026';
INSERT INTO working_time_of_modules (`date`, wt_ma_min)
SELECT date_str,
       CASE
           WHEN dt BETWEEN '2026-01-01' AND '2026-01-11' THEN 0
           WHEN dt = '2026-01-12' THEN 2160
           WHEN dt IN ('2026-05-01', '2026-05-09') THEN 0
           ELSE 4320
       END
FROM tmp_days_2026;

-- В report_* за 2026 норма уже стоит, но CuringArea (74880) и FinishigArea (25920)
-- отстают от yml. Обновляем только эти две таблицы и пересчитываем BD и доступность
-- теми же формулами, что в update_curing_area_report / update_finishig_area_report.
-- Дни с 0 не трогаем. 12.01.2026 ставим половину новой нормы.

UPDATE report_curing_area
SET wt_ca_min = 79200
WHERE production_day LIKE '%.2026'
  AND wt_ca_min = 74880;

UPDATE report_curing_area
SET wt_ca_min = 39600
WHERE production_day = '12.01.2026';

UPDATE report_curing_area
SET downtime_percentage = ROUND((machine_downtime / wt_ca_min) * 100, 2),
    availability = ROUND(100 - (((IFNULL(preventive_maintenance_duration_min, 0) + machine_downtime) / wt_ca_min) * 100), 2)
WHERE production_day LIKE '%.2026'
  AND wt_ca_min > 0
  AND machine_downtime IS NOT NULL;

UPDATE report_finishig_area
SET wt_fa_min = 27360
WHERE production_day LIKE '%.2026'
  AND wt_fa_min = 25920;

UPDATE report_finishig_area
SET wt_fa_min = 13680
WHERE production_day = '12.01.2026';

UPDATE report_finishig_area
SET downtime_percentage = ROUND((machine_downtime / wt_fa_min) * 100, 2),
    availability = ROUND(100 - (((IFNULL(preventive_maintenance_duration_min, 0) + machine_downtime) / wt_fa_min) * 100), 2)
WHERE production_day LIKE '%.2026'
  AND wt_fa_min > 0
  AND machine_downtime IS NOT NULL;

COMMIT;

-- Проверка: по каждой таблице 365 строк, норма из yml на обычный день.
SELECT 'working_time_of_plant' AS tbl, COUNT(*) AS rows_2026, SUM(`date` = '02.10.2026' AND wt_p_min = 156960) AS sample_ok FROM working_time_of_plant WHERE `date` LIKE '%.2026'
UNION ALL
SELECT 'working_time_of_building_area', COUNT(*), SUM(`date` = '02.10.2026' AND wt_ba_min = 23040) FROM working_time_of_building_area WHERE `date` LIKE '%.2026'
UNION ALL
SELECT 'working_time_of_curing_area', COUNT(*), SUM(`date` = '02.10.2026' AND wt_ca_min = 79200) FROM working_time_of_curing_area WHERE `date` LIKE '%.2026'
UNION ALL
SELECT 'working_time_of_finishig_area', COUNT(*), SUM(`date` = '02.10.2026' AND wt_fa_min = 27360) FROM working_time_of_finishig_area WHERE `date` LIKE '%.2026'
UNION ALL
SELECT 'working_time_of_semifinishing_area', COUNT(*), SUM(`date` = '02.10.2026' AND wt_sa_min = 23040) FROM working_time_of_semifinishing_area WHERE `date` LIKE '%.2026'
UNION ALL
SELECT 'working_time_of_new_mixing_area', COUNT(*), SUM(`date` = '02.10.2026' AND wt_nma_min = 10080) FROM working_time_of_new_mixing_area WHERE `date` LIKE '%.2026'
UNION ALL
SELECT 'working_time_of_modules', COUNT(*), SUM(`date` = '02.10.2026' AND wt_ma_min = 4320) FROM working_time_of_modules WHERE `date` LIKE '%.2026';
