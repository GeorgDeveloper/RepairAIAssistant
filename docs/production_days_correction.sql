-- Корректировка производственных дней по месяцам.
-- Показатели BD и доступность за месяц считаются только по производственным дням (диапазонам).
-- ППР (PM) из перерасчёта исключены — останов завода на них не влияет.
-- Поддержка нескольких диапазонов в месяце (например простой 15–20 число: диапазоны 1–14 и 21–31).

-- Основная таблица (год, месяц, комментарий)
CREATE TABLE IF NOT EXISTS monitoring_bd.production_days_correction (
    id INT AUTO_INCREMENT PRIMARY KEY,
    year INT NOT NULL,
    month INT NOT NULL,
    comment VARCHAR(255) DEFAULT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_year_month (year, month)
);

-- Диапазоны производственных дней (несколько на один месяц)
CREATE TABLE IF NOT EXISTS monitoring_bd.production_days_correction_range (
    id INT AUTO_INCREMENT PRIMARY KEY,
    correction_id INT NOT NULL,
    first_production_day TINYINT NOT NULL COMMENT 'Первый день учёта (1-31)',
    last_production_day TINYINT NOT NULL COMMENT 'Последний день учёта (1-31)',
    CONSTRAINT chk_pdcr_first CHECK (first_production_day BETWEEN 1 AND 31),
    CONSTRAINT chk_pdcr_last CHECK (last_production_day BETWEEN 1 AND 31),
    CONSTRAINT chk_pdcr_order CHECK (first_production_day <= last_production_day),
    CONSTRAINT fk_correction FOREIGN KEY (correction_id) REFERENCES monitoring_bd.production_days_correction(id) ON DELETE CASCADE
);

-- Миграция: если уже есть таблица со старыми колонками first_production_day, last_production_day
-- (одна запись на месяц с одним диапазоном), выполните перед переходом на новую схему:
--
-- INSERT INTO monitoring_bd.production_days_correction_range (correction_id, first_production_day, last_production_day)
-- SELECT id, first_production_day, last_production_day FROM monitoring_bd.production_days_correction
-- WHERE first_production_day IS NOT NULL AND last_production_day IS NOT NULL;
-- ALTER TABLE monitoring_bd.production_days_correction DROP COLUMN first_production_day, DROP COLUMN last_production_day;
