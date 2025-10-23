package ru.georgdeveloper.assistantbaseupdate.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ru.georgdeveloper.assistantbaseupdate.config.DataSyncProperties;
import ru.georgdeveloper.assistantbaseupdate.util.ShiftCalculator;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Основной сервис синхронизации данных между SQL Server и MySQL.
 * 
 * Выполняет синхронизацию каждые 3 минуты согласно расписанию.
 * Логика синхронизации:
 * 1. Определение текущей смены (дневная: 08:00-20:00, ночная: 20:00-08:00)
 * 2. Получение данных о времени простоя из SQL Server за текущую смену
 * 3. Получение рабочего времени из MySQL (working_time_of_*)
 * 4. Расчет инкрементального рабочего времени на основе времени с начала смены
 * 5. Расчет метрик (процент простоя, доступность) относительно времени смены
 * 6. Сохранение результатов в MySQL (production_metrics_online)
 * 
 * Важно: Простой учитывается с начала текущей смены, включая перенос из предыдущей смены.
 * Это обеспечивает корректный расчет показателей в реальном времени.
 */
@Service
public class DataSyncService {

    private static final Logger logger = LoggerFactory.getLogger(DataSyncService.class);

    @Autowired
    private DataSyncProperties dataSyncProperties;

    @Autowired
    @Qualifier("sqlServerJdbcTemplate")
    private JdbcTemplate sqlServerJdbcTemplate;

    @Autowired
    @Qualifier("mysqlJdbcTemplate")
    private JdbcTemplate mysqlJdbcTemplate;


    /**
     * Планируемая задача синхронизации данных каждые 3 минуты
     */
    @Scheduled(cron = "${data-sync.schedule:0 */3 * * * ?}")
    public void syncData() {
        if (!dataSyncProperties.isEnabled()) {
            logger.debug("Синхронизация данных отключена в конфигурации");
            return;
        }

        logger.info("Начало синхронизации данных между базами");
        long startTime = System.currentTimeMillis();

        try {
            List<DataSyncProperties.Area> areas = dataSyncProperties.getAreas();
            if (areas == null || areas.isEmpty()) {
                logger.warn("Список областей для синхронизации пуст");
                return;
            }

            int successCount = 0;
            int errorCount = 0;

            for (DataSyncProperties.Area area : areas) {
                try {
                    syncAreaData(area);
                    successCount++;
                } catch (Exception e) {
                    logger.error("Ошибка синхронизации для области {}: {}", area.getName(), e.getMessage(), e);
                    errorCount++;
                }
            }

            // Дополнительно синхронизируем текущий ТОП поломок по участкам (онлайн)
            try {
                LocalDateTime[] dateRange = calculateDateRange();
                syncCurrentTopBreakdowns(dateRange[0], dateRange[1]);
            } catch (Exception e) {
                logger.error("Ошибка синхронизации топ текущих поломок: {}", e.getMessage(), e);
            }

            long duration = System.currentTimeMillis() - startTime;
            logger.info("Синхронизация завершена. Успешно: {}, Ошибок: {}, Время: {} мс", 
                       successCount, errorCount, duration);

        } catch (Exception e) {
            logger.error("Критическая ошибка при синхронизации данных: {}", e.getMessage(), e);
        }
    }

    /**
     * Синхронизация данных для конкретной области
     */
    private void syncAreaData(DataSyncProperties.Area area) {
        logger.debug("Синхронизация данных для области: {}", area.getName());

        // 1. Определяем диапазон дат (с 08:00 текущего дня до 08:00 следующего дня)
        LocalDateTime[] dateRange = calculateDateRange();
        String searchDate = dateRange[0].format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));

        // 2. Получаем рабочее время из MySQL
        Double workingTime = getWorkingTime(area, searchDate);
        if (workingTime == null) {
            workingTime = area.getDefaultWt();
            logger.debug("Используется значение по умолчанию для {}: {} мин", area.getName(), workingTime);
        } else {
            logger.debug("Получено рабочее время для {} из БД: {} мин", area.getName(), workingTime);
        }

        // 3. Получаем время простоя из SQL Server
        Double downtime = getDowntimeFromSqlServer(area, dateRange[0], dateRange[1]);
        logger.debug("Получено время простоя для области {}: {} мин", area.getName(), downtime);

        // 4. Рассчитываем инкрементальное рабочее время
        Double incrementalWorkingTime = calculateIncrementalWorkingTime(workingTime, dateRange[0]);
        
        // 5. Рассчитываем метрики
        Double downtimePercentage = calculateDowntimePercentage(downtime, incrementalWorkingTime);
        Double availability = calculateAvailability(downtimePercentage);

        // 6. Сохраняем в MySQL
        saveMetricsToMysql(area.getName(), downtime, incrementalWorkingTime, downtimePercentage, availability);

        logger.debug("Область {} синхронизирована: downtime={}, wt={}, incremental_wt={}, percentage={}, availability={}", 
                   area.getName(), downtime, workingTime, incrementalWorkingTime, downtimePercentage, availability);
    }

    /**
     * Синхронизирует таблицу top_breakdowns_current_status_online по данным из SQL Server за текущие сутки (с 08:00 до 08:00)
     */
    @org.springframework.transaction.annotation.Transactional
    protected void syncCurrentTopBreakdowns(LocalDateTime startDate, LocalDateTime endDate) {
        logger.info("Синхронизация top_breakdowns_current_status_online за период {} - {}", startDate, endDate);

        // Запрос к SQL Server: берем записи с Comment, содержащим 'Cause:'
        String sql = "SELECT PlantDepartmentGeographicalCodeName AS area, MachineName AS machine_name, Duration AS downtime, Comment " +
                     "FROM REP_BreakdownReport " +
                     "WHERE ((SDate_T1 >= ? AND SDate_T1 < ?) OR (SDate_T4 >= ? AND SDate_T4 < ?)) " +
                     "AND Comment LIKE '%Cause:%' " +
                     "ORDER BY PlantDepartmentGeographicalCodeName, MachineName";

        List<Map<String, Object>> rows = sqlServerJdbcTemplate.queryForList(sql, startDate, endDate, startDate, endDate);

        // Агрегируем по участку и машине
        Map<String, Map<String, AggregatedMachine>> areaToMachines = new HashMap<>();
        for (Map<String, Object> row : rows) {
            String area = Objects.toString(row.get("area"), null);
            String machineName = Objects.toString(row.get("machine_name"), null);
            Double duration = row.get("downtime") == null ? 0.0 : ((Number) row.get("downtime")).doubleValue();
            String comment = Objects.toString(row.get("Comment"), "");

            if (area == null || machineName == null) {
                continue;
            }

            areaToMachines.computeIfAbsent(area, a -> new HashMap<>());
            Map<String, AggregatedMachine> machines = areaToMachines.get(area);
            AggregatedMachine agg = machines.computeIfAbsent(machineName, m -> new AggregatedMachine());
            agg.totalDowntimeMinutes += duration;
            String cause = extractCause(comment);
            if (cause != null && !cause.isBlank()) {
                agg.causes.add(cause);
            }
        }

        // Определяем топ-машину по каждому участку
        List<Object[]> rowsToInsert = new ArrayList<>();
        for (Map.Entry<String, Map<String, AggregatedMachine>> areaEntry : areaToMachines.entrySet()) {
            String area = areaEntry.getKey();
            String topMachine = null;
            double maxMinutes = 0.0;
            Set<String> causes = Collections.emptySet();
            for (Map.Entry<String, AggregatedMachine> mEntry : areaEntry.getValue().entrySet()) {
                AggregatedMachine agg = mEntry.getValue();
                if (agg.totalDowntimeMinutes > maxMinutes) {
                    maxMinutes = agg.totalDowntimeMinutes;
                    topMachine = mEntry.getKey();
                    causes = agg.causes;
                }
            }
            if (topMachine != null) {
                String downtimeTime = minutesToTime(maxMinutes);
                String causesJoined = causes.isEmpty() ? "Причина не указана" : String.join(", ", causes);
                rowsToInsert.add(new Object[]{area, topMachine, downtimeTime, causesJoined});
            }
        }

        // Перезаписываем таблицу в одной транзакции
        mysqlJdbcTemplate.update("TRUNCATE TABLE top_breakdowns_current_status_online");
        if (!rowsToInsert.isEmpty()) {
            mysqlJdbcTemplate.batchUpdate(
                "INSERT INTO top_breakdowns_current_status_online (area, machine_name, machine_downtime, cause) VALUES (?, ?, ?, ?)",
                rowsToInsert
            );
        }

        logger.info("Синхронизация top_breakdowns_current_status_online завершена. Строк вставлено: {}", rowsToInsert.size());
    }

    private static class AggregatedMachine {
        double totalDowntimeMinutes = 0.0;
        Set<String> causes = new LinkedHashSet<>();
    }

    private String extractCause(String comment) {
        if (comment == null || !comment.contains("Cause:")) {
            return null;
        }
        try {
            String[] parts = comment.split("Cause:", 2);
            String causePart = parts.length > 1 ? parts[1] : "";
            int dot = causePart.indexOf('.');
            if (dot >= 0) {
                causePart = causePart.substring(0, dot);
            }
            causePart = causePart.replace("pithon", "java").trim();
            return causePart.isEmpty() ? null : causePart;
        } catch (Exception e) {
            return null;
        }
    }

    private String minutesToTime(double totalMinutes) {
        int hours = (int) Math.floor(totalMinutes / 60.0);
        int minutes = (int) Math.floor(totalMinutes % 60.0);
        int seconds = (int) Math.floor((totalMinutes * 60.0) % 60.0);
        return String.format(java.util.Locale.ROOT, "%02d:%02d:%02d", hours, minutes, seconds);
    }

    /**
     * Определяет диапазон дат для запроса (текущая смена)
     */
    private LocalDateTime[] calculateDateRange() {
        LocalDateTime now = LocalDateTime.now();
        return ShiftCalculator.getCurrentShiftRange(now);
    }

    /**
     * Получает рабочее время из MySQL для указанной области и даты
     */
    private Double getWorkingTime(DataSyncProperties.Area area, String searchDate) {
        String sql = String.format("SELECT %s FROM %s WHERE date = ?", 
                                  area.getWtColumn(), area.getWorkingTimeTable());
        
        try {
            Double result = mysqlJdbcTemplate.queryForObject(sql, Double.class, searchDate);
            logger.debug("Найдено рабочее время для {} на дату {}: {} мин", 
                        area.getName(), searchDate, result);
            return result;
        } catch (Exception e) {
            logger.debug("Данные для области {} на дату {} не найдены", area.getName(), searchDate);
            return null;
        }
    }

    /**
     * Получает время простоя из SQL Server для указанной области и диапазона дат.
     * Учитывает простой с начала текущей смены, включая перенос из предыдущей смены.
     */
    private Double getDowntimeFromSqlServer(DataSyncProperties.Area area, LocalDateTime startDate, LocalDateTime endDate) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT SUM(Duration) as total_duration ");
        sql.append("FROM REP_BreakdownReport ");
        sql.append("WHERE ");
        sql.append("((SDate_T1 >= ? AND SDate_T1 < ?) OR (SDate_T4 >= ? AND SDate_T4 < ?)) ");
        sql.append("AND (Comment NOT LIKE '%Cause:%Ошибочный запрос%' AND Comment NOT LIKE '%Cause:%Ложный вызов%') ");
        sql.append("AND NOT (LEN(Comment) BETWEEN 15 AND 19 AND WOStatusLocalDescr LIKE '%Закрыто%') ");
        sql.append("AND TYPEWO NOT LIKE '%Tag%' ");

        // Специальная обработка для области Modules
        if ("Modules".equals(area.getName())) {
            sql.append("AND MachineName IN ('Module A-1', 'Module A-2', 'Module A-3') ");
        } else if (area.getFilterColumn() != null && area.getFilterValue() != null) {
            // Добавляем фильтр по области, если он задан
            sql.append("AND ").append(area.getFilterColumn()).append(" = ? ");
        }

        try {
            logger.debug("SQL запрос для области {} за период {} - {}: {}", area.getName(), startDate, endDate, sql.toString());
            Double result;
            if ("Modules".equals(area.getName())) {
                result = sqlServerJdbcTemplate.queryForObject(sql.toString(), Double.class, 
                                                             startDate, endDate, startDate, endDate);
                // Дополнительная проверка для модулей - посмотрим, есть ли вообще записи
                String countSql = sql.toString().replace("SELECT SUM(Duration) as total_duration", "SELECT COUNT(*)");
                try {
                    Integer count = sqlServerJdbcTemplate.queryForObject(countSql, Integer.class, 
                                                                        startDate, endDate, startDate, endDate);
                    logger.debug("Количество записей для модулей в период {} - {}: {}", startDate, endDate, count);
                } catch (Exception e) {
                    logger.debug("Не удалось получить количество записей для модулей: {}", e.getMessage());
                }
            } else if (area.getFilterColumn() != null && area.getFilterValue() != null) {
                result = sqlServerJdbcTemplate.queryForObject(sql.toString(), Double.class, 
                                                             startDate, endDate, startDate, endDate, area.getFilterValue());
            } else {
                result = sqlServerJdbcTemplate.queryForObject(sql.toString(), Double.class, 
                                                             startDate, endDate, startDate, endDate);
            }
            
            logger.debug("Результат SQL запроса для области {} за период {} - {}: {} мин", 
                        area.getName(), startDate, endDate, result);
            return result != null ? result : 0.0;
        } catch (Exception e) {
            logger.error("Ошибка получения данных простоя из SQL Server для области {}: {}", 
                        area.getName(), e.getMessage());
            return 0.0;
        }
    }

    /**
     * Рассчитывает инкрементальное рабочее время на основе времени с начала текущей смены
     */
    private Double calculateIncrementalWorkingTime(Double fullWorkingTime, LocalDateTime startDate) {
        if (fullWorkingTime == null || fullWorkingTime == 0) {
            return fullWorkingTime;
        }
        
        LocalDateTime now = LocalDateTime.now();
        return ShiftCalculator.calculateIncrementalWorkingTime(fullWorkingTime, now);
    }
    
    /**
     * Рассчитывает процент простоя
     */
    private Double calculateDowntimePercentage(Double downtime, Double workingTime) {
        if (downtime == null || downtime == 0) {
            return 0.0;
        }
        if (workingTime == null || workingTime == 0) {
            return null; // NULL для MySQL
        }
        return Math.round((downtime / workingTime) * 100 * 100.0) / 100.0; // Округление до 2 знаков
    }

    /**
     * Рассчитывает доступность оборудования
     */
    private Double calculateAvailability(Double downtimePercentage) {
        if (downtimePercentage == null) {
            return null; // NULL для MySQL
        }
        return Math.round((100 - downtimePercentage) * 100.0) / 100.0; // Округление до 2 знаков
    }

    /**
     * Сохраняет метрики в MySQL
     */
    @org.springframework.transaction.annotation.Transactional
    private void saveMetricsToMysql(String areaName, Double downtime, Double workingTime,
                                    Double downtimePercentage, Double availability) {
        // Удаляем только записи старше 24 часов для данной области
        mysqlJdbcTemplate.update(
            "DELETE FROM production_metrics_online WHERE area = ? AND last_update < DATE_SUB(NOW(), INTERVAL 24 HOUR)",
            areaName
        );
        // Вставляем новую запись (без возврата identity)
        mysqlJdbcTemplate.update(
            "INSERT INTO production_metrics_online " +
            "(area, last_update, machine_downtime, wt_min, downtime_percentage, preventive_maintenance_duration_min, availability) " +
            "VALUES (?, NOW(), ?, ?, ?, ?, ?)",
            areaName,
            downtime,
            workingTime,
            downtimePercentage,
            0.0,
            availability
        );
        logger.debug("Метрики для области {} сохранены в MySQL (JdbcTemplate)", areaName);
    }
}
