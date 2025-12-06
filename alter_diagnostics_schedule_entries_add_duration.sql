-- Миграция: добавление колонки duration_minutes в таблицу diagnostics_schedule_entries
-- Это позволяет сохранять индивидуальную продолжительность для каждого наряда

ALTER TABLE `diagnostics_schedule_entries` 
ADD COLUMN `duration_minutes` INT NULL COMMENT 'Продолжительность диагностики в минутах (сохраняется при создании наряда)' 
AFTER `notes`;

-- Обновляем существующие записи: устанавливаем продолжительность из типа диагностики
UPDATE `diagnostics_schedule_entries` dse
INNER JOIN `diagnostics_types` dt ON dse.diagnostics_type_id = dt.id
SET dse.duration_minutes = dt.duration_minutes
WHERE dse.duration_minutes IS NULL;
