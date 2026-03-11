-- Миграция с одной пары (first_production_day, last_production_day) на таблицу диапазонов.
-- Выполнять только если уже использовалась старая схема production_days_correction с колонками first_production_day, last_production_day.

-- 1) Создать таблицу диапазонов, если ещё нет
CREATE TABLE IF NOT EXISTS monitoring_bd.production_days_correction_range (
    id INT AUTO_INCREMENT PRIMARY KEY,
    correction_id INT NOT NULL,
    first_production_day TINYINT NOT NULL,
    last_production_day TINYINT NOT NULL,
    CONSTRAINT chk_pdcr_first CHECK (first_production_day BETWEEN 1 AND 31),
    CONSTRAINT chk_pdcr_last CHECK (last_production_day BETWEEN 1 AND 31),
    CONSTRAINT chk_pdcr_order CHECK (first_production_day <= last_production_day),
    CONSTRAINT fk_correction FOREIGN KEY (correction_id) REFERENCES monitoring_bd.production_days_correction(id) ON DELETE CASCADE
);

-- 2) Перенести существующие диапазоны (если колонки есть)
INSERT INTO monitoring_bd.production_days_correction_range (correction_id, first_production_day, last_production_day)
SELECT id, first_production_day, last_production_day
FROM monitoring_bd.production_days_correction
WHERE first_production_day IS NOT NULL AND last_production_day IS NOT NULL;

-- 3) Удалить ограничения и старые колонки (обязательно для работы приложения с новой схемой).
--    Либо выполните отдельный скрипт: production_days_correction_alter_drop_columns.sql
ALTER TABLE monitoring_bd.production_days_correction DROP CONSTRAINT chk_order;
ALTER TABLE monitoring_bd.production_days_correction DROP CONSTRAINT chk_first_day;
ALTER TABLE monitoring_bd.production_days_correction DROP CONSTRAINT chk_last_day;

ALTER TABLE monitoring_bd.production_days_correction
    DROP COLUMN first_production_day,
    DROP COLUMN last_production_day;
