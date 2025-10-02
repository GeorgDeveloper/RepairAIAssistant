package ru.georgdeveloper.assistantbaseupdate.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Сервис для ежедневного переноса данных из SQL Server в MySQL
 * Выполняется каждый день в 8:00 утра
 */
@Service
public class DataTransferService {

    private static final Logger logger = LoggerFactory.getLogger(DataTransferService.class);

    @Autowired
    private JdbcTemplate sqlServerJdbcTemplate;

    @Autowired
    private JdbcTemplate mysqlJdbcTemplate;

    /**
     * Ежедневный перенос данных в 8:00 утра
     */
    @Scheduled(cron = "0 0 8 * * *")
    @Transactional
    public void transferDataDaily() {
        try {
            logger.info("=== Начало ежедневного переноса данных...");
            
            // Получаем текущую дату и время для фильтрации
            LocalDate today = LocalDate.now();
            LocalDateTime startDateTime = today.minusDays(1).atTime(8, 0);
            LocalDateTime endDateTime = today.atTime(8, 0);
            
            logger.info("Фильтрация данных: Date_T1 >= {} OR Date_T4 >= {}", startDateTime, startDateTime);
            logger.info("И Date_T1 < {} OR Date_T4 < {}", endDateTime, endDateTime);
            
            // Выполняем перенос данных
            int transferredCount = transferDataFromSqlServer(startDateTime, endDateTime);
            
            if (transferredCount > 0) {
                // Обрабатываем дополнительные поля
                processAdditionalFields();
                
                // Удаляем отфильтрованные записи
                int deletedCount = cleanupFilteredRecords();
                
                logger.info("Итог: перенесено {} записей, удалено {} отфильтрованных записей", 
                    transferredCount, deletedCount);
            } else {
                logger.warn("Нет данных для обработки");
            }
            
        } catch (Exception e) {
            logger.error("Критическая ошибка при переносе данных: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Перенос данных из SQL Server в MySQL
     */
    private int transferDataFromSqlServer(LocalDateTime startDateTime, LocalDateTime endDateTime) {
        String sql = """
            SELECT 
                MachineName, Assembly, SubAssembly, InitialComment, WOCodeName,
                TYPEWO, Date_T1, Date_T2, Date_T3, Date_T4, SDuration, STTR,
                SLogisticTimeMin, WOStatusLocalDescr, Maintainers, comment,
                PlantDepartmentGeographicalCodeName
            FROM REP_BreakdownReport
            WHERE 
                ((Date_T1 >= ? AND Date_T1 < ?)
                OR 
                (Date_T4 >= ? AND Date_T4 < ?))
            """;

        List<Map<String, Object>> rows = sqlServerJdbcTemplate.queryForList(sql, 
            startDateTime, endDateTime, startDateTime, endDateTime);
        
        logger.info("Найдено {} записей для переноса", rows.size());
        
        String insertSql = """
            INSERT INTO equipment_maintenance_records (
                machine_name, mechanism_node, additional_kit, description, code, 
                hp_bd, start_bd_t1, start_maint_t2, stop_maint_t3, stop_bd_t4,
                machine_downtime, ttr, t2_minus_t1, status, maintainers, comments, area,
                created_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

        int successCount = 0;
        int errorCount = 0;

        for (int i = 0; i < rows.size(); i++) {
            try {
                Map<String, Object> row = rows.get(i);
                
                // Преобразование времени из строкового формата
                Time machineDowntime = parseSqlTime((String) row.get("SDuration"));
                Time ttr = parseSqlTime((String) row.get("STTR"));
                Time t2MinusT1 = parseSqlTime((String) row.get("SLogisticTimeMin"));
                
                // Подготовка данных для вставки
                Object[] data = {
                    getStringValue(row.get("MachineName")),
                    getStringValue(row.get("Assembly")),
                    getStringValue(row.get("SubAssembly")),
                    getStringValue(row.get("InitialComment")),
                    getStringValue(row.get("WOCodeName")),
                    getStringValue(row.get("TYPEWO")),
                    row.get("Date_T1"),
                    row.get("Date_T2"),
                    row.get("Date_T3"),
                    row.get("Date_T4"),
                    machineDowntime,
                    ttr,
                    t2MinusT1,
                    getStringValue(row.get("WOStatusLocalDescr")),
                    getStringValue(row.get("Maintainers")),
                    getStringValue(row.get("comment")),
                    getStringValue(row.get("PlantDepartmentGeographicalCodeName")),
                    LocalDateTime.now()
                };
                
                mysqlJdbcTemplate.update(insertSql, data);
                successCount++;
                
                if ((i + 1) % 50 == 0) {
                    logger.info("Обработано {} записей", i + 1);
                }
                
            } catch (Exception e) {
                errorCount++;
                logger.error("Ошибка при обработке записи {}: {}", i + 1, e.getMessage());
            }
        }
        
        logger.info("Перенос данных завершен. Успешно: {}, Ошибок: {}", successCount, errorCount);
        return successCount;
    }

    /**
     * Парсинг времени из SQL Server формата
     */
    private Time parseSqlTime(String timeStr) {
        if (timeStr == null || timeStr.trim().isEmpty()) {
            return null;
        }
        
        try {
            timeStr = timeStr.trim();
            
            if (timeStr.contains(":")) {
                String[] parts = timeStr.split(":");
                
                if (parts.length == 2) {
                    // Формат "часы:минуты"
                    int hours = Integer.parseInt(parts[0]);
                    int minutes = Integer.parseInt(parts[1]);
                    return Time.valueOf(LocalTime.of(hours, minutes, 0));
                } else if (parts.length == 3) {
                    // Формат "часы:минуты:секунды"
                    int hours = Integer.parseInt(parts[0]);
                    int minutes = Integer.parseInt(parts[1]);
                    String secondsPart = parts[2];
                    int seconds = secondsPart.contains(".") ? 
                        (int) Double.parseDouble(secondsPart) : 
                        Integer.parseInt(secondsPart);
                    return Time.valueOf(LocalTime.of(hours, minutes, seconds));
                }
            } else if (timeStr.matches("\\d+")) {
                // Время в минутах
                int totalMinutes = Integer.parseInt(timeStr);
                int hours = totalMinutes / 60;
                int minutes = totalMinutes % 60;
                return Time.valueOf(LocalTime.of(hours, minutes, 0));
            }
            
        } catch (Exception e) {
            logger.warn("Ошибка преобразования времени '{}': {}", timeStr, e.getMessage());
        }
        
        return null;
    }

    private int executeWithRetry(java.util.function.Supplier<Integer> operation, String operationName) {
        int maxRetries = 3;
        int retryDelay = 1000; // 1 секунда
        
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                return operation.get();
            } catch (org.springframework.dao.CannotAcquireLockException e) {
                if (attempt == maxRetries) {
                    logger.error("Критическая ошибка при {}: исчерпаны все попытки ({})", operationName, maxRetries);
                    throw e;
                }
                logger.warn("Deadlock при {} (попытка {}/{}), повтор через {} мс", operationName, attempt, maxRetries, retryDelay);
                try {
                    Thread.sleep(retryDelay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Прервано во время ожидания повтора", ie);
                }
                retryDelay *= 2; // Экспоненциальная задержка
            }
        }
        return 0; // Никогда не достигается
    }

    /**
     * Обработка всех дополнительных полей
     */
    @Transactional
    public void processAdditionalFields() {
        try {
            logger.info("Начало обработки дополнительных полей...");
            
            // 1. Извлекаем причину из comments (только для пустого cause)
            String updateCause = """
                UPDATE equipment_maintenance_records
                SET cause = TRIM(
                    SUBSTRING(
                        comments,
                        LOCATE('Cause:', comments) + CHAR_LENGTH('Cause:'),
                        COALESCE(
                            NULLIF(
                                LEAST(
                                    LOCATE('[', comments, LOCATE('Cause:', comments)),
                                    LOCATE(']', comments, LOCATE('Cause:', comments))
                                ),
                                0
                            ),
                            CHAR_LENGTH(comments) + 1
                        ) - (LOCATE('Cause:', comments) + CHAR_LENGTH('Cause:'))
                    )
                )
                WHERE (cause IS NULL OR cause = '') 
                  AND comments LIKE '%Cause:%'
                """;
            
            int causeCount = executeWithRetry(() -> mysqlJdbcTemplate.update(updateCause), "извлечение cause");
            logger.info("Извлечение cause из comments: обработано {} записей", causeCount);
            
            // 2. Очистка символов в cause
            String cleanCause = """
                UPDATE equipment_maintenance_records
                SET cause = REPLACE(REPLACE(REPLACE(REPLACE(cause, '#', ''), 'Cause:', ''), ';', ''), '---', '')
                WHERE cause IS NOT NULL AND cause != ''
                """;
            
            int cleanCount = executeWithRetry(() -> mysqlJdbcTemplate.update(cleanCause), "очистка символов в cause");
            logger.info("Очистка символов в cause: обработано {} записей", cleanCount);
            
            // 3. Удаление пробелов в cause
            String trimCause = """
                UPDATE equipment_maintenance_records
                SET cause = TRIM(cause)
                WHERE cause IS NOT NULL AND cause != ''
                """;
            
            int trimCount = executeWithRetry(() -> mysqlJdbcTemplate.update(trimCause), "удаление пробелов в cause");
            logger.info("Удаление пробелов в cause: обработано {} записей", trimCount);
            
            // 4. Заполнение поля staff
            String updateStaff = """
                UPDATE equipment_maintenance_records
                SET staff = TRIM(
                    SUBSTRING_INDEX(
                        SUBSTRING_INDEX(
                            SUBSTRING_INDEX(comments, ']', 1),
                            '(',
                            -1
                        ),
                        ')',
                        1
                    )
                )
                WHERE (staff IS NULL OR staff = '')
                  AND comments LIKE '%(%' 
                  AND comments LIKE '%)%'
                  AND comments LIKE '%[%]%'
                """;
            
            int staffCount = executeWithRetry(() -> mysqlJdbcTemplate.update(updateStaff), "заполнение поля staff");
            logger.info("Заполнение поля staff: обработано {} записей", staffCount);
            
            // 5. Заполнение поля date
            String updateDate = """
                UPDATE equipment_maintenance_records
                SET date = DATE_FORMAT(start_bd_t1, '%d.%m.%Y')
                WHERE (date IS NULL OR date = '') 
                  AND start_bd_t1 IS NOT NULL
                """;
            
            int dateCount = executeWithRetry(() -> mysqlJdbcTemplate.update(updateDate), "заполнение поля date");
            logger.info("Заполнение поля date: обработано {} записей", dateCount);
            
            // 6. Заполнение поля week_number
            String updateWeek = """
                UPDATE equipment_maintenance_records
                SET week_number = WEEK(STR_TO_DATE(date, '%d.%m.%Y'), 3)
                WHERE (week_number IS NULL OR week_number = '') 
                  AND date IS NOT NULL AND date != ''
                """;
            
            int weekCount = executeWithRetry(() -> mysqlJdbcTemplate.update(updateWeek), "заполнение поля week_number");
            logger.info("Заполнение поля week_number: обработано {} записей", weekCount);
            
            // 7. Заполнение поля month_name
            String updateMonth = """
                UPDATE equipment_maintenance_records
                SET month_name = 
                    CASE MONTH(STR_TO_DATE(date, '%d.%m.%Y'))
                        WHEN 1 THEN 'Январь'
                        WHEN 2 THEN 'Февраль'
                        WHEN 3 THEN 'Март'
                        WHEN 4 THEN 'Апрель'
                        WHEN 5 THEN 'Май'
                        WHEN 6 THEN 'Июнь'
                        WHEN 7 THEN 'Июль'
                        WHEN 8 THEN 'Август'
                        WHEN 9 THEN 'Сентябрь'
                        WHEN 10 THEN 'Октябрь'
                        WHEN 11 THEN 'Ноябрь'
                        WHEN 12 THEN 'Декабрь'
                    END
                WHERE (month_name IS NULL OR month_name = '') 
                  AND date IS NOT NULL AND date != ''
                """;
            
            int monthCount = executeWithRetry(() -> mysqlJdbcTemplate.update(updateMonth), "заполнение поля month_name");
            logger.info("Заполнение поля month_name: обработано {} записей", monthCount);
            
            // 8. Заполнение поля shift
            String updateShift = """
                UPDATE equipment_maintenance_records
                SET shift = 
                    CASE 
                        WHEN HOUR(start_bd_t1) BETWEEN 8 AND 19 THEN '1'
                        WHEN HOUR(start_bd_t1) BETWEEN 20 AND 23 
                              OR HOUR(start_bd_t1) BETWEEN 0 AND 7 THEN '2'
                        ELSE 'Не определено'
                    END
                WHERE (shift IS NULL OR shift = '') 
                  AND start_bd_t1 IS NOT NULL
                """;
            
            int shiftCount = executeWithRetry(() -> mysqlJdbcTemplate.update(updateShift), "заполнение поля shift");
            logger.info("Заполнение поля shift: обработано {} записей", shiftCount);
            
            // 9. Обновление failure_type из staff_technical
            String updateFailureType = """
                UPDATE equipment_maintenance_records rp
                JOIN staff_technical rpp ON rp.staff = rpp.staff
                SET rp.failure_type = rpp.directivity
                WHERE (rp.failure_type IS NULL OR rp.failure_type = '')
                """;
            
            int failureTypeCount = executeWithRetry(() -> mysqlJdbcTemplate.update(updateFailureType), "заполнение поля failure_type");
            logger.info("Заполнение поля failure_type: обработано {} записей", failureTypeCount);
            
            // 10. Обновление crew_de_facto из staff_technical
            String updateCrewDeFacto = """
                UPDATE equipment_maintenance_records rp
                JOIN staff_technical rpp ON rp.staff = rpp.staff
                SET rp.crew_de_facto = rpp.shift
                WHERE (rp.crew_de_facto IS NULL OR rp.crew_de_facto = '')
                """;
            
            int crewDeFactoCount = executeWithRetry(() -> mysqlJdbcTemplate.update(updateCrewDeFacto), "заполнение поля crew_de_facto");
            logger.info("Заполнение поля crew_de_facto: обработано {} записей", crewDeFactoCount);
            
            // 11. Обновление crew из график_работы_104
            String updateCrew = """
                UPDATE equipment_maintenance_records AS emr
                JOIN график_работы_104 AS g 
                    ON g.Дата = emr.date 
                    AND CAST(g.Смена AS CHAR) = emr.shift
                SET emr.crew = g.Бригада
                WHERE (emr.crew IS NULL OR emr.crew = '') 
                  AND emr.date IS NOT NULL 
                  AND emr.shift IS NOT NULL
                """;
            
            int crewCount = executeWithRetry(() -> mysqlJdbcTemplate.update(updateCrew), "заполнение поля crew");
            logger.info("Заполнение поля crew: обработано {} записей", crewCount);
            
            // 12. Заполнение production_day
            String updateProductionDay = """
                UPDATE equipment_maintenance_records
                SET production_day = 
                    CASE 
                        WHEN start_bd_t1 IS NULL THEN NULL
                        WHEN stop_bd_t4 IS NULL THEN 
                            DATE_FORMAT(
                                CASE 
                                    WHEN TIME(start_bd_t1) >= '08:00:00' THEN start_bd_t1
                                    ELSE DATE_SUB(start_bd_t1, INTERVAL 1 DAY)
                                END, 
                                '%d.%m.%Y'
                            )
                        ELSE 
                            DATE_FORMAT(
                                CASE 
                                    WHEN 
                                        CASE 
                                            WHEN TIME(start_bd_t1) >= '08:00:00' THEN DATE(start_bd_t1)
                                            ELSE DATE(DATE_SUB(start_bd_t1, INTERVAL 1 DAY))
                                        END < 
                                        CASE 
                                            WHEN TIME(stop_bd_t4) >= '08:00:00' THEN DATE(stop_bd_t4)
                                            ELSE DATE(DATE_SUB(stop_bd_t4, INTERVAL 1 DAY))
                                        END
                                    THEN 
                                        CASE 
                                            WHEN TIME(stop_bd_t4) >= '08:00:00' THEN DATE(stop_bd_t4)
                                            ELSE DATE(DATE_SUB(stop_bd_t4, INTERVAL 1 DAY))
                                        END
                                    ELSE 
                                        CASE 
                                            WHEN TIME(start_bd_t1) >= '08:00:00' THEN DATE(start_bd_t1)
                                            ELSE DATE(DATE_SUB(start_bd_t1, INTERVAL 1 DAY))
                                        END
                                END,
                                '%d.%m.%Y'
                            )
                    END
                WHERE (production_day IS NULL OR production_day = '')
                """;
            
            int productionDayCount = executeWithRetry(() -> mysqlJdbcTemplate.update(updateProductionDay), "заполнение поля production_day");
            logger.info("Заполнение поля production_day: обработано {} записей", productionDayCount);
            
            // 13. Обновление failure_type для специфичных причин
            String updateSpecificFailureType = """
                UPDATE equipment_maintenance_records
                SET failure_type = 'Другие'
                WHERE cause LIKE '%Наладка%' 
                   OR cause LIKE '%Простой по вине производства%' 
                   OR cause LIKE '%Простой по вине с. качества%'
                """;
            
            int specificFailureTypeCount = executeWithRetry(() -> mysqlJdbcTemplate.update(updateSpecificFailureType), "заполнение поля failure_type (specific)");
            logger.info("Обновление failure_type для специфичных причин: обработано {} записей", specificFailureTypeCount);
            
            logger.info("Обработка всех дополнительных полей завершена успешно");
            
        } catch (Exception e) {
            logger.error("Ошибка при обработке дополнительных полей: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Удаление отфильтрованных записей
     */
    @Transactional
    public int cleanupFilteredRecords() {
        try {
            logger.info("Начало удаления отфильтрованных записей...");
            
            String deleteQuery = """
                DELETE FROM equipment_maintenance_records
                WHERE 
                    (comments LIKE '%Cause:%Ошибочный запрос%' OR comments LIKE '%Cause:%Ложный вызов%')
                    OR (
                        LENGTH(comments) BETWEEN 15 AND 19 
                        AND (status LIKE '%Закрыто%'
                        OR status LIKE '%Выполнено%')
                    )
                    OR hp_bd LIKE '%Tag%'
                    OR status LIKE '%В исполнении%'
                """;
            
            int deletedCount = executeWithRetry(() -> mysqlJdbcTemplate.update(deleteQuery), "удаление отфильтрованных записей");
            logger.info("Удалено отфильтрованных записей: {}", deletedCount);
            
            return deletedCount;
            
        } catch (Exception e) {
            logger.error("Ошибка при удалении отфильтрованных записей: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Получение строкового значения с проверкой на null
     */
    private String getStringValue(Object value) {
        return value != null ? value.toString() : "";
    }
}
