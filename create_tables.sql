-- Создание базы данных
CREATE DATABASE IF NOT EXISTS monitoring_bd 
CHARACTER SET utf8mb4 
COLLATE utf8mb4_unicode_ci;

USE monitoring_bd;

-- Таблица записей о ремонте
CREATE TABLE IF NOT EXISTS repair_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    device_type VARCHAR(255) NOT NULL COMMENT 'Тип устройства',
    problem_description TEXT NOT NULL COMMENT 'Описание проблемы',
    solution TEXT COMMENT 'Решение проблемы',
    start_date DATETIME NOT NULL COMMENT 'Дата начала ремонта',
    end_date DATETIME COMMENT 'Дата окончания ремонта',
    duration_hours INT COMMENT 'Длительность в часах',
    status VARCHAR(100) NOT NULL COMMENT 'Статус ремонта',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Таблица руководств и инструкций
CREATE TABLE IF NOT EXISTS manuals (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    device_type VARCHAR(255) NOT NULL COMMENT 'Тип устройства',
    file_name VARCHAR(255) NOT NULL COMMENT 'Имя файла',
    content TEXT NOT NULL COMMENT 'Содержимое документа',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Индексы для оптимизации поиска
CREATE INDEX idx_repair_status ON repair_records(status);
CREATE INDEX idx_repair_device ON repair_records(device_type);
CREATE INDEX idx_repair_duration ON repair_records(duration_hours);
CREATE INDEX idx_manual_device ON manuals(device_type);
CREATE INDEX idx_manual_content ON manuals(content(100));

-- Пример данных для тестирования (можно удалить в продакшене)
INSERT INTO repair_records (device_type, problem_description, solution, start_date, end_date, duration_hours, status) VALUES
('Телевизор Samsung', 'Не включается, мигает индикатор', 'Замена блока питания', '2025-08-03 10:00:00', '2025-08-05 10:00:00', 48, 'COMPLETED'),
('Холодильник LG', 'Не морозит, компрессор не работает', 'Замена компрессора и заправка фреоном', '2025-07-29 09:00:00', '2025-08-01 09:00:00', 72, 'COMPLETED'),
('Стиральная машина Bosch', 'Не сливает воду, ошибка E18', 'Чистка фильтра и замена помпы', '2025-08-08 14:00:00', '2025-08-09 14:00:00', 24, 'COMPLETED'),
('Микроволновка Samsung', 'Не греет, свет горит', 'Ожидаем запчасти', '2025-08-10 11:00:00', NULL, NULL, 'временно закрыто'),
('Плита Electrolux', 'Не работает одна конфорка', 'Ожидаем мастера', '2025-08-12 16:00:00', NULL, NULL, 'временно закрыто');

INSERT INTO manuals (device_type, file_name, content) VALUES
('Телевизор', 'samsung_tv_manual.pdf', 'Руководство по ремонту телевизоров Samsung. Основные неисправности: проблемы с питанием, матрицей, звуком.'),
('Холодильник', 'lg_fridge_manual.pdf', 'Инструкция по ремонту холодильников LG. Диагностика компрессора, системы охлаждения, электроники.'),
('Стиральная машина', 'bosch_washer_manual.pdf', 'Руководство по ремонту стиральных машин Bosch. Коды ошибок, замена деталей, профилактика.');

COMMIT;