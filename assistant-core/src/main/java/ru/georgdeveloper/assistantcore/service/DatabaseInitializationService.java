package ru.georgdeveloper.assistantcore.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * Сервис для инициализации базы данных
 * Создает необходимые таблицы если они отсутствуют
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DatabaseInitializationService {

    private final JdbcTemplate jdbcTemplate;

    /**
     * Инициализация БД при запуске приложения (выполняется до миграции)
     */
    @EventListener(ApplicationReadyEvent.class)
    @Order(1) // Выполняется перед DataMigrationService
    public void initializeDatabase() {
        log.info("Проверяем и создаем необходимые таблицы БД...");
        createMigrationTrackingTable();
    }

    /**
     * Создание таблицы отслеживания миграций
     */
    private void createMigrationTrackingTable() {
        try {
            // Проверяем существование таблицы
            jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'migration_tracking'",
                Integer.class
            );
            
            Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'migration_tracking'",
                Integer.class
            );
            
            if (count == null || count == 0) {
                log.info("Таблица migration_tracking не найдена, создаем...");
                createTable();
            } else {
                log.debug("Таблица migration_tracking уже существует");
            }
            
        } catch (Exception e) {
            log.warn("Ошибка при проверке таблицы migration_tracking, пытаемся создать: {}", e.getMessage());
            createTable();
        }
    }

    /**
     * Создание таблицы migration_tracking
     */
    private void createTable() {
        try {
            String createTableSql = """
                CREATE TABLE IF NOT EXISTS migration_tracking (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    table_name VARCHAR(100) NOT NULL UNIQUE,
                    last_migrated_id BIGINT DEFAULT 0,
                    last_migration_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    records_count BIGINT DEFAULT 0,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    INDEX idx_table_name (table_name)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """;

            jdbcTemplate.execute(createTableSql);
            log.info("Таблица migration_tracking успешно создана");

            // Инициализируем записи для отслеживания
            initializeTrackingRecords();

        } catch (Exception e) {
            log.error("Ошибка создания таблицы migration_tracking", e);
            throw new RuntimeException("Не удалось создать таблицу migration_tracking", e);
        }
    }

    /**
     * Инициализация записей отслеживания
     */
    private void initializeTrackingRecords() {
        try {
            String insertSql = """
                INSERT IGNORE INTO migration_tracking (table_name, last_migrated_id, records_count) VALUES
                ('equipment_maintenance_record', 0, 0),
                ('summary_of_solutions', 0, 0),
                ('breakdown_report', 0, 0)
                """;

            int inserted = jdbcTemplate.update(insertSql);
            if (inserted > 0) {
                log.info("Инициализировано {} записей отслеживания миграций", inserted);
            } else {
                log.debug("Записи отслеживания миграций уже существуют");
            }

        } catch (Exception e) {
            log.error("Ошибка инициализации записей отслеживания", e);
        }
    }
}
