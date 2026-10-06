package ru.georgdeveloper.assistantcore.energy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Создаёт таблицу воздуха при старте, если её ещё нет (ddl-auto=none).
 */
@Component
public class EnergyAirSchemaInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(EnergyAirSchemaInitializer.class);

    private static final String DDL = """
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
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """;

    private final JdbcTemplate jdbcTemplate;

    public EnergyAirSchemaInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        jdbcTemplate.execute(DDL);
        log.info("Таблица energy_air_hourly_value проверена/создана");
    }
}
