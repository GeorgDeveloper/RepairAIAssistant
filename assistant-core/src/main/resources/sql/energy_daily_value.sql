-- Таблица для импорта суточных показателей энергоресурсов из Excel.
-- Выполните один раз в схеме monitoring_bd (или своей БД), если ddl-auto=none.

CREATE TABLE IF NOT EXISTS energy_daily_value (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    fact_date DATE NOT NULL,
    resource_code VARCHAR(32) NOT NULL,
    metric_id VARCHAR(128) NOT NULL,
    value_numeric DECIMAL(24, 8) NULL,
    source_file VARCHAR(512) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_energy_day_metric (fact_date, resource_code, metric_id),
    KEY idx_energy_resource_date (resource_code, fact_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
