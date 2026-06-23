-- Обновление цвета для типа диагностики "y - Диагностики утечек воздуха"
-- Светлоголубой цвет: #87CEEB (SkyBlue)

UPDATE `diagnostics_types` 
SET `color_code` = '#87CEEB' 
WHERE `code` = 'y';

