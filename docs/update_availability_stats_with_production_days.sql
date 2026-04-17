-- Процедура UpdateAvailabilityStats с учётом корректировки производственных дней.
-- BD и доступность считаются только по производственным дням (таблица production_days_correction + production_days_correction_range).
-- ППР (плановые ремонты, PM) из перерасчёта исключены — считаются за весь месяц.

DELIMITER $$

DROP PROCEDURE IF EXISTS monitoring_bd.UpdateAvailabilityStats$$

CREATE DEFINER=`dba`@`%` PROCEDURE `monitoring_bd`.`UpdateAvailabilityStats`()
BEGIN
    DECLARE current_month_num INT;
    DECLARE current_month_name VARCHAR(20);
    DECLARE current_year INT;

    SET current_month_num = MONTH(CURRENT_DATE);
    SET current_year = YEAR(CURRENT_DATE);

    SET current_month_name = CASE current_month_num
        WHEN 1 THEN 'Январь'
        WHEN 2 THEN 'Февраль'
        WHEN 3 THEN 'Март'
        WHEN 4 THEN 'Апрель'
        WHEN 5 THEN 'Май'
        WHEN 6 THEN 'Июнь'
        WHEN 7 THEN 'Июль'
        WHEN 8 THEN 'Август'
        WHEN 9 THEN 'Сентябрь'
        WHEN 10 THEN 'Октябрь'
        WHEN 11 THEN 'Ноябрь'
        WHEN 12 THEN 'Декабрь'
    END;

    DELETE FROM availability_stats
    WHERE month = current_month_name
      AND year = current_year;

    -- Вставка: availability и bd только по производственным дням (если задана корректировка);
    -- плановые ремонты (PM) — за весь месяц без фильтра по дням.
    INSERT INTO availability_stats (
        month,
        year,
        availability_percent,
        bd_percent,
        breakdowns_count,
        planned_repairs_percent,
        planned_repairs_count,
        pm_repairs_percent,
        total_repairs_count,
        created_at
    )
    SELECT
        current_month_name AS month,
        current_year AS year,
        availability_percent,
        bd_percent,
        breakdowns_count,
        planned_repairs_percent,
        planned_repairs_count,
        pm_repairs_percent,
        (breakdowns_count + planned_repairs_count) AS total_repairs_count,
        NOW() AS created_at
    FROM (
        SELECT
            -- Доступность и BD только по производственным дням (если есть корректировка)
            ROUND(COALESCE((
                SELECT AVG(rp.availability)
                FROM report_plant rp
                WHERE DATE_FORMAT(STR_TO_DATE(rp.production_day, '%d.%m.%Y'), '%Y-%m') = DATE_FORMAT(CURRENT_DATE, '%Y-%m')
                  AND (availability IS NOT NULL OR rp.downtime_percentage IS NOT NULL)
                  AND (
                      NOT EXISTS (
                          SELECT 1 FROM production_days_correction c
                          WHERE c.year = current_year AND c.month = current_month_num
                      )
                      OR EXISTS (
                          SELECT 1 FROM production_days_correction c
                          JOIN production_days_correction_range r ON r.correction_id = c.id
                          WHERE c.year = current_year AND c.month = current_month_num
                            AND DAY(STR_TO_DATE(rp.production_day, '%d.%m.%Y')) BETWEEN r.first_production_day AND r.last_production_day
                      )
                  )
            ), 0), 2) AS availability_percent,

            ROUND(COALESCE((
                SELECT AVG(rp.downtime_percentage)
                FROM report_plant rp
                WHERE DATE_FORMAT(STR_TO_DATE(rp.production_day, '%d.%m.%Y'), '%Y-%m') = DATE_FORMAT(CURRENT_DATE, '%Y-%m')
                  AND (rp.availability IS NOT NULL OR rp.downtime_percentage IS NOT NULL)
                  AND (
                      NOT EXISTS (SELECT 1 FROM production_days_correction c WHERE c.year = current_year AND c.month = current_month_num)
                      OR EXISTS (
                          SELECT 1 FROM production_days_correction c
                          JOIN production_days_correction_range r ON r.correction_id = c.id
                          WHERE c.year = current_year AND c.month = current_month_num
                            AND DAY(STR_TO_DATE(rp.production_day, '%d.%m.%Y')) BETWEEN r.first_production_day AND r.last_production_day
                      )
                  )
            ), 0), 2) AS bd_percent,

            -- Поломки: только по производственным дням (если есть корректировка)
            (SELECT COUNT(*)
             FROM equipment_maintenance_records emr
             WHERE DATE_FORMAT(STR_TO_DATE(emr.production_day, '%d.%m.%Y'), '%Y-%m') = DATE_FORMAT(CURRENT_DATE, '%Y-%m')
               AND (emr.failure_type != 'Другие' OR emr.failure_type IS NULL)
               AND (
                   NOT EXISTS (SELECT 1 FROM production_days_correction c WHERE c.year = current_year AND c.month = current_month_num)
                   OR EXISTS (
                       SELECT 1 FROM production_days_correction c
                       JOIN production_days_correction_range r ON r.correction_id = c.id
                       WHERE c.year = current_year AND c.month = current_month_num
                         AND DAY(STR_TO_DATE(emr.production_day, '%d.%m.%Y')) BETWEEN r.first_production_day AND r.last_production_day
                   )
               )
            ) AS breakdowns_count,

            -- ППР: за весь месяц, без фильтра по производственным дням
            ROUND(COALESCE((
                SELECT AVG(rp.pm_time_percentage)
                FROM report_plant rp
                WHERE DATE_FORMAT(STR_TO_DATE(rp.production_day, '%d.%m.%Y'), '%Y-%m') = DATE_FORMAT(CURRENT_DATE, '%Y-%m')
                  AND rp.pm_time_percentage IS NOT NULL
            ), 0), 2) AS planned_repairs_percent,

            COALESCE((
                SELECT SUM(rp.quantity_pm_close)
                FROM report_plant rp
                WHERE DATE_FORMAT(STR_TO_DATE(rp.production_day, '%d.%m.%Y'), '%Y-%m') = DATE_FORMAT(CURRENT_DATE, '%Y-%m')
                  AND rp.quantity_pm_close IS NOT NULL
            ), 0) AS planned_repairs_count,

            COALESCE(ROUND(
                (SELECT SUM(rp.quantity_pm_close)
                 FROM report_plant rp
                 WHERE DATE_FORMAT(STR_TO_DATE(rp.production_day, '%d.%m.%Y'), '%Y-%m') = DATE_FORMAT(CURRENT_DATE, '%Y-%m')
                   AND rp.quantity_pm_close IS NOT NULL
                ) * 100.0 / NULLIF(
                    (SELECT SUM(rp.quantity_pm_planned)
                     FROM report_plant rp
                     WHERE DATE_FORMAT(STR_TO_DATE(rp.production_day, '%d.%m.%Y'), '%Y-%m') = DATE_FORMAT(CURRENT_DATE, '%Y-%m')
                       AND rp.quantity_pm_planned IS NOT NULL
                    ), 0
                )
            ), 0) AS pm_repairs_percent

    ) AS all_data;

    SELECT CONCAT('Статистика доступности обновлена за ', current_month_name, ' ', current_year) AS result;

END$$

DELIMITER ;
