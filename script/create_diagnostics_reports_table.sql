-- SQL скрипт для создания таблицы diagnostics_reports
-- Таблица для хранения отчетов диагностики

CREATE TABLE IF NOT EXISTS `diagnostics_reports` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `detection_date` DATE NULL COMMENT 'Дата обнаружения',
  `diagnostics_type` VARCHAR(255) NULL COMMENT 'Тип диагностики',
  `equipment` VARCHAR(255) NULL COMMENT 'Оборудование',
  `node` VARCHAR(255) NULL COMMENT 'Узел',
  `area` VARCHAR(255) NULL COMMENT 'Участок',
  `malfunction` TEXT NULL COMMENT 'Неисправность',
  `additional_kit` VARCHAR(255) NULL COMMENT 'Доп. комплект',
  `causes` TEXT NULL COMMENT 'Причины',
  `report` TEXT NULL COMMENT 'Отчет',
  `elimination_date` DATE NULL COMMENT 'Дата устранения',
  `condition_after_elimination` TEXT NULL COMMENT 'Состояние после устранения',
  `responsible` VARCHAR(255) NULL COMMENT 'Ответственный',
  `status` VARCHAR(100) NULL DEFAULT 'ОТКРЫТО' COMMENT 'Статус (ОТКРЫТО, В РАБОТЕ, ЗАКРЫТО)',
  `non_elimination_reason` TEXT NULL COMMENT 'Причина неустранения',
  `measures` TEXT NULL COMMENT 'Мероприятия',
  `comments` TEXT NULL COMMENT 'Комментарии',
  `photo_path` TEXT NULL COMMENT 'Путь к фото',
  `document_path` TEXT NULL COMMENT 'Путь к документу',
  `created_at` DATETIME NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Дата создания',
  `updated_at` DATETIME NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Дата обновления',
  PRIMARY KEY (`id`),
  INDEX `idx_detection_date` (`detection_date`),
  INDEX `idx_equipment` (`equipment`),
  INDEX `idx_area` (`area`),
  INDEX `idx_status` (`status`),
  INDEX `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Отчеты диагностики';

