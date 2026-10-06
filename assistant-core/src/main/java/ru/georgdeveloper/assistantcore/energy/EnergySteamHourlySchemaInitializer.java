package ru.georgdeveloper.assistantcore.energy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class EnergySteamHourlySchemaInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(EnergySteamHourlySchemaInitializer.class);

    private static final String DDL = """
            CREATE TABLE IF NOT EXISTS energy_steam_hourly_value (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                fact_time DATETIME NOT NULL,
                consumer_code VARCHAR(64) NOT NULL,
                consumer_label VARCHAR(128) NOT NULL,
                flow_value DECIMAL(24, 8) NULL,
                temp_c DECIMAL(24, 8) NULL,
                pressure_mpa DECIMAL(24, 8) NULL,
                mass_t DECIMAL(24, 8) NULL,
                source_file VARCHAR(512) NULL,
                created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                UNIQUE KEY uk_energy_steam_time_consumer (fact_time, consumer_code),
                KEY idx_energy_steam_time (fact_time),
                KEY idx_energy_steam_consumer_time (consumer_code, fact_time)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """;

    private final JdbcTemplate jdbcTemplate;

    public EnergySteamHourlySchemaInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        jdbcTemplate.execute(DDL);
        log.info("Таблица energy_steam_hourly_value проверена/создана");
    }
}
