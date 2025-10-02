-- Таблица для отслеживания состояния миграций в векторную БД
CREATE TABLE IF NOT EXISTS migration_tracking (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    table_name VARCHAR(100) NOT NULL,
    last_migrated_id BIGINT,
    last_migration_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    records_count BIGINT DEFAULT 0,
    INDEX idx_table_name (table_name)
);

-- Инициализация записей для отслеживания
INSERT IGNORE INTO migration_tracking (table_name, last_migrated_id, records_count) VALUES
('equipment_maintenance_record', 0, 0),
('summary_of_solutions', 0, 0),
('breakdown_report', 0, 0);
