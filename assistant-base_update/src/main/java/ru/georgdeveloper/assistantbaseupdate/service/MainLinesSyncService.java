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
 * Сервис синхронизации данных ключевых линий между SQL Server и MySQL.
 * 
 * Выполняет синхронизацию каждые 3 минуты согласно расписанию.
 * Логика синхронизации:
 * 1. Получение данных о времени простоя из SQL Server (REP_BreakdownReport) по конкретным машинам
 * 2. Получение рабочего времени из MySQL (working_time_of_*)
 * 3. Расчет метрик (процент простоя, доступность) для каждой ключевой линии
 * 4. Сохранение результатов в MySQL (main_lines_online)
 */
@Service
public class MainLinesSyncService {

    private static final Logger logger = LoggerFactory.getLogger(MainLinesSyncService.class);

    @Autowired
    private DataSyncProperties dataSyncProperties;

    @Autowired
    @Qualifier("sqlServerJdbcTemplate")
    private JdbcTemplate sqlServerJdbcTemplate;

    @Autowired
    @Qualifier("mysqlJdbcTemplate")
    private JdbcTemplate mysqlJdbcTemplate;


    /**
     * Планируемая задача синхронизации данных ключевых линий каждые 3 минуты
     */
    @Scheduled(cron = "${data-sync.schedule:0 */3 * * * ?}")
    public void syncMainLinesData() {
        if (!dataSyncProperties.isEnabled()) {
            logger.debug("Синхронизация данных ключевых линий отключена в конфигурации");
            return;
        }

        List<DataSyncProperties.MainLine> mainLines = dataSyncProperties.getMainLines();
        if (mainLines == null || mainLines.isEmpty()) {
            logger.warn("Список ключевых линий для синхронизации пуст");
            return;
        }

        logger.info("Начало синхронизации данных ключевых линий");
        long startTime = System.currentTimeMillis();

        try {
            int successCount = 0;
            int errorCount = 0;

            for (DataSyncProperties.MainLine mainLine : mainLines) {
                try {
                    syncMainLineData(mainLine);
                    successCount++;
                } catch (Exception e) {
                    logger.error("Ошибка синхронизации для ключевой линии {}: {}", mainLine.getName(), e.getMessage(), e);
                    errorCount++;
                }
            }

            long duration = System.currentTimeMillis() - startTime;
            logger.info("Синхронизация ключевых линий завершена. Успешно: {}, Ошибок: {}, Время: {} мс", 
                       successCount, errorCount, duration);

        } catch (Exception e) {
            logger.error("Критическая ошибка при синхронизации данных ключевых линий: {}", e.getMessage(), e);
        }
    }

    /**
     * Синхронизация данных для конкретной ключевой линии
     */
    private void syncMainLineData(DataSyncProperties.MainLine mainLine) {
        logger.debug("Синхронизация данных для ключевой линии: {}", mainLine.getName());

        // 1. Определяем диапазон дат (с 08:00 текущего дня до 08:00 следующего дня)
        LocalDateTime[] dateRange = calculateDateRange();
        String searchDate = dateRange[0].format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));

        // 2. Получаем рабочее время из MySQL для области этой линии
        Double workingTime = getWorkingTimeForArea(mainLine.getArea(), searchDate);
        if (workingTime == null) {
            // Используем значение по умолчанию для области
            workingTime = getDefaultWorkingTimeForArea(mainLine.getArea());
            logger.debug("Используется значение по умолчанию для области {}: {} мин", mainLine.getArea(), workingTime);
        }

        // 3. Получаем время простоя из SQL Server для конкретной машины
        Double downtime = getDowntimeFromSqlServerForMachine(mainLine, dateRange[0], dateRange[1]);

        // 4. Рассчитываем инкрементальное рабочее время
        Double incrementalWorkingTime = calculateIncrementalWorkingTime(workingTime, dateRange[0]);
        
        // 5. Рассчитываем метрики
        Double downtimePercentage = calculateDowntimePercentage(downtime, incrementalWorkingTime);
        Double availability = calculateAvailability(downtimePercentage);

        // 6. Сохраняем в MySQL
        saveMainLineMetricsToMysql(mainLine.getName(), mainLine.getArea(), downtime, incrementalWorkingTime, downtimePercentage, availability);

        logger.debug("Ключевая линия {} синхронизирована: downtime={}, wt={}, incremental_wt={}, percentage={}, availability={}", 
                   mainLine.getName(), downtime, workingTime, incrementalWorkingTime, downtimePercentage, availability);
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
    private Double getWorkingTimeForArea(String area, String searchDate) {
        // Находим конфигурацию области
        DataSyncProperties.Area areaConfig = dataSyncProperties.getAreas().stream()
                .filter(a -> a.getName().equals(area))
                .findFirst()
                .orElse(null);

        if (areaConfig == null) {
            logger.warn("Конфигурация для области {} не найдена", area);
            return null;
        }

        String sql = String.format("SELECT %s FROM %s WHERE date = ?", 
                                  areaConfig.getWtColumn(), areaConfig.getWorkingTimeTable());
        
        try {
            Double result = mysqlJdbcTemplate.queryForObject(sql, Double.class, searchDate);
            logger.debug("Найдено рабочее время для области {} на дату {}: {} мин", 
                        area, searchDate, result);
            return result;
        } catch (Exception e) {
            logger.debug("Данные для области {} на дату {} не найдены", area, searchDate);
            return null;
        }
    }

    /**
     * Получает значение по умолчанию для рабочего времени области
     */
    private Double getDefaultWorkingTimeForArea(String area) {
        DataSyncProperties.Area areaConfig = dataSyncProperties.getAreas().stream()
                .filter(a -> a.getName().equals(area))
                .findFirst()
                .orElse(null);

        return areaConfig != null ? areaConfig.getDefaultWt() : 23040.0; // Значение по умолчанию
    }

    /**
     * Получает время простоя из SQL Server для конкретной машины
     */
    private Double getDowntimeFromSqlServerForMachine(DataSyncProperties.MainLine mainLine, 
                                                     LocalDateTime startDate, LocalDateTime endDate) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT SUM(Duration) as total_duration ");
        sql.append("FROM REP_BreakdownReport ");
        sql.append("WHERE ");
        sql.append("((SDate_T1 >= ? AND SDate_T1 < ?) OR (SDate_T4 >= ? AND SDate_T4 < ?)) ");
        sql.append("AND (Comment NOT LIKE '%Cause:%Ошибочный запрос%' AND Comment NOT LIKE '%Cause:%Ложный вызов%') ");
        sql.append("AND NOT (LEN(Comment) BETWEEN 15 AND 19 AND WOStatusLocalDescr LIKE '%Закрыто%') ");
        sql.append("AND TYPEWO NOT LIKE '%Tag%' ");
        sql.append("AND PlantDepartmentGeographicalCodeName = ? ");
        sql.append("AND MachineName LIKE ? ");

        try {
            String machineFilter = "%" + mainLine.getMachineFilter() + "%";
            Double result = sqlServerJdbcTemplate.queryForObject(sql.toString(), Double.class, 
                                                               startDate, endDate, startDate, endDate, 
                                                               mainLine.getArea(), machineFilter);
            
            return result != null ? result : 0.0;
        } catch (Exception e) {
            logger.error("Ошибка получения данных простоя из SQL Server для ключевой линии {}: {}", 
                        mainLine.getName(), e.getMessage());
            return 0.0;
        }
    }

    /**
     * Рассчитывает инкрементальное рабочее время на основе текущего времени
     */
    private Double calculateIncrementalWorkingTime(Double fullWorkingTime, LocalDateTime startDate) {
        if (fullWorkingTime == null || fullWorkingTime == 0) {
            return fullWorkingTime;
        }
        
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime currentStart = startDate;
        LocalDateTime currentEnd = startDate.plusDays(1);
        
        // Если текущее время вне диапазона 08:00-08:00, возвращаем полное значение
        if (now.isBefore(currentStart) || now.isAfter(currentEnd)) {
            return fullWorkingTime;
        }
        
        // Вычисляем количество прошедших 3-минутных интервалов с 08:00
        long minutesFromStart = java.time.Duration.between(currentStart, now).toMinutes();
        long intervals = minutesFromStart / 3 + 1; // +1 чтобы начинать с 1
        
        // Общее количество интервалов в сутках (24 часа * 60 минут / 3 минуты = 480)
        long totalIntervals = 480;
        
        // Рассчитываем инкремент на интервал
        double increment = fullWorkingTime / totalIntervals;
        
        return Math.round(increment * intervals * 100.0) / 100.0;
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
     * Сохраняет метрики ключевой линии в MySQL
     */
    @org.springframework.transaction.annotation.Transactional
    private void saveMainLineMetricsToMysql(String lineName, String area, Double downtime, Double workingTime,
                                          Double downtimePercentage, Double availability) {
        // Удаляем только записи старше 24 часов для данной линии
        mysqlJdbcTemplate.update(
            "DELETE FROM main_lines_online WHERE line_name = ? AND last_update < DATE_SUB(NOW(), INTERVAL 24 HOUR)",
            lineName
        );
        
        // Вставляем новую запись
        mysqlJdbcTemplate.update(
            "INSERT INTO main_lines_online " +
            "(line_name, area, last_update, machine_downtime, wt_min, downtime_percentage, preventive_maintenance_duration_min, availability) " +
            "VALUES (?, ?, NOW(), ?, ?, ?, ?, ?)",
            lineName,
            area,
            downtime,
            workingTime,
            downtimePercentage,
            0.0,
            availability
        );
        logger.debug("Метрики для ключевой линии {} сохранены в MySQL", lineName);
    }
}
