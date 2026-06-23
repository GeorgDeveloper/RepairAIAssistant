-- run_UpdateAvailabilityStatsForPeriod: обёртка с двумя параметрами (месяц, год).
-- UpdateAvailabilityStatsForPeriod: перерасчёт за указанный месяц; диапазон дней из production_days_correction;
-- при обновлении строки в availability_stats created_at не меняется (UPDATE).

DELIMITER $$

DROP PROCEDURE IF EXISTS monitoring_bd.UpdateAvailabilityStatsForPeriod$$
DROP PROCEDURE IF EXISTS monitoring_bd.run_UpdateAvailabilityStatsForPeriod$$

CREATE DEFINER=`dba`@`%` PROCEDURE `monitoring_bd`.`UpdateAvailabilityStatsForPeriod`(
    IN target_month INT,
    IN target_year INT
)
BEGIN
    DECLARE v_month_name VARCHAR(20);
    DECLARE v_availability_percent DECIMAL(10,2);
    DECLARE v_bd_percent DECIMAL(10,2);
    DECLARE v_breakdowns_count INT;
    DECLARE v_planned_repairs_percent DECIMAL(10,2);
    DECLARE v_planned_repairs_count INT;
    DECLARE v_pm_repairs_percent DECIMAL(10,2);
    DECLARE v_total_repairs_count INT;
    DECLARE v_month_ym VARCHAR(7);

    SET v_month_ym = CONCAT(target_year, '-', LPAD(target_month, 2, '0'));
    SET v_month_name = CASE target_month
        WHEN 1 THEN 'Январь' WHEN 2 THEN 'Февраль' WHEN 3 THEN 'Март' WHEN 4 THEN 'Апрель'
        WHEN 5 THEN 'Май' WHEN 6 THEN 'Июнь' WHEN 7 THEN 'Июль' WHEN 8 THEN 'Август'
        WHEN 9 THEN 'Сентябрь' WHEN 10 THEN 'Октябрь' WHEN 11 THEN 'Ноябрь' WHEN 12 THEN 'Декабрь'
    END;

    SELECT
        ROUND(COALESCE((
            SELECT AVG(rp.availability)
            FROM report_plant rp
            WHERE DATE_FORMAT(STR_TO_DATE(rp.production_day, '%d.%m.%Y'), '%Y-%m') = v_month_ym
              AND (rp.availability IS NOT NULL OR rp.downtime_percentage IS NOT NULL)
              AND (
                  NOT EXISTS (SELECT 1 FROM production_days_correction c WHERE c.year = target_year AND c.month = target_month)
                  OR EXISTS (
                      SELECT 1 FROM production_days_correction c
                      JOIN production_days_correction_range r ON r.correction_id = c.id
                      WHERE c.year = target_year AND c.month = target_month
                        AND DAY(STR_TO_DATE(rp.production_day, '%d.%m.%Y')) BETWEEN r.first_production_day AND r.last_production_day
                  )
              )
        ), 0), 2),
        ROUND(COALESCE((
            SELECT AVG(rp.downtime_percentage)
            FROM report_plant rp
            WHERE DATE_FORMAT(STR_TO_DATE(rp.production_day, '%d.%m.%Y'), '%Y-%m') = v_month_ym
              AND (rp.availability IS NOT NULL OR rp.downtime_percentage IS NOT NULL)
              AND (
                  NOT EXISTS (SELECT 1 FROM production_days_correction c WHERE c.year = target_year AND c.month = target_month)
                  OR EXISTS (
                      SELECT 1 FROM production_days_correction c
                      JOIN production_days_correction_range r ON r.correction_id = c.id
                      WHERE c.year = target_year AND c.month = target_month
                        AND DAY(STR_TO_DATE(rp.production_day, '%d.%m.%Y')) BETWEEN r.first_production_day AND r.last_production_day
                  )
              )
        ), 0), 2),
        (SELECT COUNT(*)
         FROM equipment_maintenance_records emr
         WHERE DATE_FORMAT(STR_TO_DATE(emr.production_day, '%d.%m.%Y'), '%Y-%m') = v_month_ym
           AND (emr.failure_type != 'Другие' OR emr.failure_type IS NULL)
           AND (
               NOT EXISTS (SELECT 1 FROM production_days_correction c WHERE c.year = target_year AND c.month = target_month)
               OR EXISTS (
                   SELECT 1 FROM production_days_correction c
                   JOIN production_days_correction_range r ON r.correction_id = c.id
                   WHERE c.year = target_year AND c.month = target_month
                     AND DAY(STR_TO_DATE(emr.production_day, '%d.%m.%Y')) BETWEEN r.first_production_day AND r.last_production_day
               )
           )),
        ROUND(COALESCE((
            SELECT AVG(rp.pm_time_percentage)
            FROM report_plant rp
            WHERE DATE_FORMAT(STR_TO_DATE(rp.production_day, '%d.%m.%Y'), '%Y-%m') = v_month_ym AND rp.pm_time_percentage IS NOT NULL
        ), 0), 2),
        COALESCE((
            SELECT SUM(rp.quantity_pm_close)
            FROM report_plant rp
            WHERE DATE_FORMAT(STR_TO_DATE(rp.production_day, '%d.%m.%Y'), '%Y-%m') = v_month_ym AND rp.quantity_pm_close IS NOT NULL
        ), 0),
        COALESCE(ROUND(
            (SELECT SUM(rp.quantity_pm_close) FROM report_plant rp
             WHERE DATE_FORMAT(STR_TO_DATE(rp.production_day, '%d.%m.%Y'), '%Y-%m') = v_month_ym AND rp.quantity_pm_close IS NOT NULL
            ) * 100.0 / NULLIF(
                (SELECT SUM(rp.quantity_pm_planned) FROM report_plant rp
                 WHERE DATE_FORMAT(STR_TO_DATE(rp.production_day, '%d.%m.%Y'), '%Y-%m') = v_month_ym AND rp.quantity_pm_planned IS NOT NULL
                ), 0)
        ), 0)
    INTO
        v_availability_percent,
        v_bd_percent,
        v_breakdowns_count,
        v_planned_repairs_percent,
        v_planned_repairs_count,
        v_pm_repairs_percent;

    SET v_total_repairs_count = v_breakdowns_count + v_planned_repairs_count;

    UPDATE availability_stats
    SET
        availability_percent = v_availability_percent,
        bd_percent = v_bd_percent,
        breakdowns_count = v_breakdowns_count,
        planned_repairs_percent = v_planned_repairs_percent,
        planned_repairs_count = v_planned_repairs_count,
        pm_repairs_percent = v_pm_repairs_percent,
        total_repairs_count = v_total_repairs_count
    WHERE month = v_month_name AND year = target_year;

    IF ROW_COUNT() = 0 THEN
        INSERT INTO availability_stats (
            month, year, availability_percent, bd_percent, breakdowns_count,
            planned_repairs_percent, planned_repairs_count, pm_repairs_percent, total_repairs_count, created_at
        ) VALUES (
            v_month_name, target_year, v_availability_percent, v_bd_percent, v_breakdowns_count,
            v_planned_repairs_percent, v_planned_repairs_count, v_pm_repairs_percent, v_total_repairs_count, NOW()
        );
    END IF;

    SELECT CONCAT('Статистика доступности обновлена за ', v_month_name, ' ', target_year) AS result;
END$$

CREATE DEFINER=`dba`@`%` PROCEDURE `monitoring_bd`.`run_UpdateAvailabilityStatsForPeriod`(
    IN target_month INT,
    IN target_year INT
)
BEGIN
    DECLARE start_time DATETIME DEFAULT NOW();
    DECLARE end_time DATETIME;
    DECLARE execution_status VARCHAR(20) DEFAULT 'success';
    DECLARE error_message TEXT DEFAULT NULL;

    BEGIN
        DECLARE EXIT HANDLER FOR SQLEXCEPTION
        BEGIN
            GET DIAGNOSTICS CONDITION 1 error_message = MESSAGE_TEXT;
            SET execution_status = 'error';
            SET end_time = NOW();

            INSERT INTO monitoring_bd.execution_log (
                procedure_name,
                start_time,
                end_time,
                execution_status,
                error_message
            ) VALUES (
                'run_UpdateAvailabilityStatsForPeriod',
                start_time,
                end_time,
                execution_status,
                error_message
            );

            RESIGNAL;
        END;


        CALL monitoring_bd.UpdateAvailabilityStatsForPeriod(target_month, target_year);

        SET end_time = NOW();
        SET execution_status = 'success';

    END;

    INSERT INTO monitoring_bd.execution_log (
        procedure_name,
        start_time,
        end_time,
        execution_status,
        error_message
    ) VALUES (
        'run_UpdateAvailabilityStatsForPeriod',
        start_time,
        end_time,
        execution_status,
        error_message
    );

END$$

DELIMITER ;
