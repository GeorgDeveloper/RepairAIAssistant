-- SQL скрипт для создания таблиц графика диагностики оборудования

-- Таблица типов диагностики
CREATE TABLE IF NOT EXISTS `diagnostics_types` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `code` VARCHAR(10) NOT NULL UNIQUE COMMENT 'Код типа диагностики (B, K, y и т.д.)',
  `name` VARCHAR(255) NOT NULL COMMENT 'Название типа диагностики',
  `duration_minutes` INT NOT NULL COMMENT 'Длительность диагностики в минутах',
  `color_code` VARCHAR(20) NULL COMMENT 'Цвет для отображения (hex)',
  `is_active` BOOLEAN DEFAULT TRUE COMMENT 'Активен ли тип',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`),
  INDEX `idx_active` (`is_active`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Типы диагностики';

-- Таблица графиков диагностики (на год)
CREATE TABLE IF NOT EXISTS `diagnostics_schedules` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `year` INT NOT NULL UNIQUE COMMENT 'Год графика',
  `shift_duration_hours` INT NOT NULL DEFAULT 7 COMMENT 'Длительность смены в часах',
  `workers_count` INT NOT NULL COMMENT 'Количество человек для диагностики',
  `created_at` DATETIME NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Дата создания',
  `updated_at` DATETIME NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Дата обновления',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_year` (`year`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Графики диагностики на год';

-- Таблица записей графика диагностики
CREATE TABLE IF NOT EXISTS `diagnostics_schedule_entries` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `schedule_id` BIGINT NOT NULL COMMENT 'ID графика',
  `equipment` VARCHAR(255) NOT NULL COMMENT 'Оборудование',
  `area` VARCHAR(255) NULL COMMENT 'Участок',
  `diagnostics_type_id` BIGINT NOT NULL COMMENT 'ID типа диагностики',
  `scheduled_date` DATE NOT NULL COMMENT 'Запланированная дата',
  `is_completed` BOOLEAN DEFAULT FALSE COMMENT 'Выполнена ли диагностика',
  `completed_date` DATE NULL COMMENT 'Дата выполнения',
  `notes` TEXT NULL COMMENT 'Заметки',
  `duration_minutes` INT NULL COMMENT 'Продолжительность диагностики в минутах (сохраняется при создании наряда)',
  PRIMARY KEY (`id`),
  FOREIGN KEY (`schedule_id`) REFERENCES `diagnostics_schedules` (`id`) ON DELETE CASCADE,
  FOREIGN KEY (`diagnostics_type_id`) REFERENCES `diagnostics_types` (`id`),
  INDEX `idx_schedule_date` (`schedule_id`, `scheduled_date`),
  INDEX `idx_equipment` (`equipment`),
  INDEX `idx_scheduled_date` (`scheduled_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Записи графика диагностики';

-- Вставка стандартных типов диагностики
INSERT INTO `diagnostics_types` (`code`, `name`, `duration_minutes`, `color_code`, `is_active`) VALUES
('B', 'Вибродиагностика', 60, '#FFD700', TRUE),
('K', 'Диагностика конденсатоотводчиков', 30, '#90EE90', TRUE),
('y', 'Диагностики утечек воздуха', 45, '#87CEEB', TRUE),
('T', 'Тепловизионная диагностика', 30, '#FFA500', TRUE)
ON DUPLICATE KEY UPDATE `name` = VALUES(`name`), `color_code` = VALUES(`color_code`);

