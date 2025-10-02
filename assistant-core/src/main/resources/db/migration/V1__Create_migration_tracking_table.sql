-- Создание таблицы для отслеживания миграций в векторную БД
CREATE TABLE IF NOT EXISTS migration_tracking (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    table_name VARCHAR(100) NOT NULL UNIQUE,
    last_migrated_id BIGINT DEFAULT 0,
    last_migration_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    records_count BIGINT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_table_name (table_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Инициализация записей для отслеживания
INSERT IGNORE INTO migration_tracking (table_name, last_migrated_id, records_count) VALUES
('equipment_maintenance_record', 0, 0),
('summary_of_solutions', 0, 0),
('breakdown_report', 0, 0);
