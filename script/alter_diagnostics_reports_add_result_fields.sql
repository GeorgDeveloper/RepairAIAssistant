-- Добавление полей для итоговых файлов и исполнителя
-- Если колонка уже существует, будет ошибка, которую можно игнорировать

ALTER TABLE `diagnostics_reports`
ADD COLUMN `photo_result_path` TEXT NULL COMMENT 'Путь к итоговому фото' AFTER `document_path`;

ALTER TABLE `diagnostics_reports`
ADD COLUMN `document_result_path` TEXT NULL COMMENT 'Путь к итоговому документу' AFTER `photo_result_path`;

