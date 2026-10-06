-- Почасовые показатели «Воздух» (два корпуса из листов СПГ742 / СПГ742_2).
-- Выполните один раз в схеме monitoring_bd (или своей БД), если ddl-auto=none.

CREATE TABLE IF NOT EXISTS energy_air_hourly_value (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    fact_time DATETIME NOT NULL,
    building_code VARCHAR(32) NOT NULL,
    building_label VARCHAR(64) NOT NULL,
    pressure_mpa DECIMAL(24, 8) NULL,
    volume_m3 DECIMAL(24, 8) NULL,
    source_file VARCHAR(512) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_energy_air_time_building (fact_time, building_code),
    KEY idx_energy_air_time (fact_time),
    KEY idx_energy_air_building_time (building_code, fact_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
