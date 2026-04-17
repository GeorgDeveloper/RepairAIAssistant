-- Разовая очистка удаленных метрик энергоресурсов из БД.
-- Выполните в схеме monitoring_bd (или своей БД).

DELETE FROM energy_daily_value
WHERE
    (resource_code = 'WATER' AND metric_id IN (
        'tech_water_fact_meter_1',
        'tech_water_fact_meter_2',
        'tech_water_fact_meter_3'
    ))
    OR (resource_code = 'GAS' AND metric_id IN (
        'gas_fact_kshz_m3',
        'gas_line_323_m3'
    ))
    OR (resource_code = 'ELECTRICITY' AND metric_id IN (
        'electricity_kirov_substation_kwh'
    ))
    OR (resource_code = 'STEAM' AND metric_id IN (
        'steam9_plan_mr_gcal',
        'steam9_R',
        'steam9_fact_kt_t',
        'steam9_fact_kshz_t',
        'steam_16ata_fact_t',
        'steam_kshz_mass_fact_t',
        'steam_9ata_mass_fact_t',
        'steam_9ata_queue1_fact_t',
        'steam_kshz_mass_fact_t_paren',
        'condensate_return_t',
        'condensate_return_kshz_t'
    ));
