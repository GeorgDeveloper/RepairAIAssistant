-- SQL скрипт для обновления таблицы diagnostics_reports
-- Добавление полей для фото и документов, если они еще не существуют

ALTER TABLE `diagnostics_reports` 
ADD COLUMN IF NOT EXISTS `photo_path` TEXT NULL COMMENT 'Путь к фото' AFTER `comments`,
ADD COLUMN IF NOT EXISTS `document_path` TEXT NULL COMMENT 'Путь к документу' AFTER `photo_path`;

-- Обновление значения по умолчанию для статуса, если оно еще не установлено
ALTER TABLE `diagnostics_reports` 
MODIFY COLUMN `status` VARCHAR(100) NULL DEFAULT 'ОТКРЫТО' COMMENT 'Статус (ОТКРЫТО, В РАБОТЕ, ЗАКРЫТО)';

