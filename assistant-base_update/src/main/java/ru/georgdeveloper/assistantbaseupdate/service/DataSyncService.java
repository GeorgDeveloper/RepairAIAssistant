package ru.georgdeveloper.assistantbaseupdate.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ru.georgdeveloper.assistantbaseupdate.config.DataSyncProperties;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Основной сервис синхронизации данных между SQL Server и MySQL.
 * 
 * Выполняет синхронизацию каждые 3 минуты согласно расписанию.
 * Логика синхронизации:
 * 1. Получение данных о времени простоя из SQL Server (REP_BreakdownReport)
 * 2. Получение рабочего времени из MySQL (working_time_of_*)
 * 3. Расчет метрик (процент простоя, доступность)
 * 4. Сохранение результатов в MySQL (production_metrics_online)
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
        }

        // 3. Получаем время простоя из SQL Server
        Double downtime = getDowntimeFromSqlServer(area, dateRange[0], dateRange[1]);

        // 4. Рассчитываем инкрементальное рабочее время на основе количества прошедших 3-минутных интервалов
        Double incrementalWorkingTime = calculateIncrementalWorkingTime(workingTime, dateRange[0]);
        
        // 6. Рассчитываем метрики
        Double downtimePercentage = calculateDowntimePercentage(downtime, incrementalWorkingTime);
        Double availability = calculateAvailability(downtimePercentage);

        // 7. Сохраняем в MySQL
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
     * Определяет диапазон дат для запроса (с 08:00 текущего дня до 08:00 следующего дня)
     */
    private LocalDateTime[] calculateDateRange() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startDate;

        if (now.getHour() >= 8) {
            // Если сейчас 08:00 или позже, начало диапазона - сегодня 08:00
            startDate = now.withHour(8).withMinute(0).withSecond(0).withNano(0);
        } else {
            // Если сейчас раньше 08:00, начало диапазона - вчера 08:00
            startDate = now.minusDays(1).withHour(8).withMinute(0).withSecond(0).withNano(0);
        }

        LocalDateTime endDate = startDate.plusDays(1);
        return new LocalDateTime[]{startDate, endDate};
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
     * Получает время простоя из SQL Server для указанной области и диапазона дат
     */
    private Double getDowntimeFromSqlServer(DataSyncProperties.Area area, LocalDateTime startDate, LocalDateTime endDate) {
        StringBuilder sql = new StringBuilder();
        List<Object> params = new ArrayList<>();
        
        sql.append("SELECT SUM(Duration) as total_duration ");
        sql.append("FROM REP_BreakdownReport ");
        sql.append("WHERE ");
        sql.append("((SDate_T1 >= ? AND SDate_T1 < ?) OR (SDate_T4 >= ? AND SDate_T4 < ?)) ");
        sql.append("AND (Comment NOT LIKE '%Cause:%Ошибочный запрос%' AND Comment NOT LIKE '%Cause:%Ложный вызов%') ");
        sql.append("AND NOT (LEN(Comment) BETWEEN 15 AND 19 AND WOStatusLocalDescr LIKE '%Закрыто%') ");
        sql.append("AND TYPEWO NOT LIKE '%Tag%' ");
        
        // Добавляем параметры дат
        params.add(startDate);
        params.add(endDate);
        params.add(startDate);
        params.add(endDate);

        // Специальная логика для Modules - фильтрация по участку FinishigArea и модулям A-1, A-2, A-3
        if ("Modules".equals(area.getName())) {
            sql.append("AND PlantDepartmentGeographicalCodeName = 'FinishigArea' ");
            sql.append("AND MachineName IN ('Module A-1', 'Module A-2', 'Module A-3') ");
        } 
        // Для остальных областей используем стандартную фильтрацию
        else if (area.getFilterColumn() != null && area.getFilterValue() != null) {
            sql.append("AND ").append(area.getFilterColumn()).append(" = ? ");
            params.add(area.getFilterValue());
        }

        try {
            Double result = sqlServerJdbcTemplate.queryForObject(sql.toString(), params.toArray(), Double.class);
            return result != null ? result : 0.0;
        } catch (Exception e) {
            logger.error("Ошибка получения данных простоя из SQL Server для области {}: {}", 
                        area.getName(), e.getMessage());
            logger.error("SQL запрос: {}", sql.toString());
            logger.error("Параметры: {}", params);
            return 0.0;
        }
    }

    /**
     * Рассчитывает инкрементальное рабочее время на основе количества прошедших 3-минутных интервалов с начала смены
     * 
     * Логика:
     * 1. Берем исходное working_time из БД
     * 2. Делим на 480 интервалов (24 часа * 60 минут / 3 минуты = 480)
     * 3. Умножаем на количество прошедших интервалов с начала смены
     */
    private Double calculateIncrementalWorkingTime(Double workingTime, LocalDateTime startDate) {
        if (workingTime == null || workingTime == 0) {
            return workingTime;
        }
        
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime currentStart = startDate;
        LocalDateTime currentEnd = startDate.plusDays(1);
        
        // Если текущее время вне диапазона 08:00-08:00, возвращаем полное значение
        if (now.isBefore(currentStart) || now.isAfter(currentEnd)) {
            return workingTime;
        }
        
        // Вычисляем количество прошедших 3-минутных интервалов с 08:00
        long minutesFromStart = java.time.Duration.between(currentStart, now).toMinutes();
        long intervalsPassed = minutesFromStart / 3; // Количество прошедших интервалов
        
        // Общее количество интервалов в сутках (24 часа * 60 минут / 3 минуты = 480)
        long totalIntervals = 480;
        
        // Рассчитываем значение на один интервал
        double valuePerInterval = workingTime / totalIntervals;
        
        // Рассчитываем текущее рабочее время: значение_на_интервал * количество_прошедших_интервалов
        double currentWorkingTime = valuePerInterval * intervalsPassed;
        
        logger.debug("Расчет рабочего времени: исходное={} мин, интервалов прошло={}, значение на интервал={}, текущее={}", 
                    workingTime, intervalsPassed, valuePerInterval, currentWorkingTime);
        
        return Math.round(currentWorkingTime * 100.0) / 100.0; // Округление до 2 знаков
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