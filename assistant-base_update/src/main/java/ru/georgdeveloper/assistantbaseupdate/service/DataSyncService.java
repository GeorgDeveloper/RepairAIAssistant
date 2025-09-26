package ru.georgdeveloper.assistantbaseupdate.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ru.georgdeveloper.assistantbaseupdate.config.DataSyncProperties;
import ru.georgdeveloper.assistantbaseupdate.entity.ProductionMetricsOnline;
import ru.georgdeveloper.assistantbaseupdate.repository.ProductionMetricsOnlineRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

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

    @Autowired
    private ProductionMetricsOnlineRepository repository;

    /**
     * Планируемая задача синхронизации данных каждые 3 минуты
     */
    @Scheduled(cron = "${data_sync.schedule:0 */3 * * * ?}")
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

        // 4. Рассчитываем метрики
        Double downtimePercentage = calculateDowntimePercentage(downtime, workingTime);
        Double availability = calculateAvailability(downtimePercentage);

        // 5. Сохраняем в MySQL
        saveMetricsToMysql(area.getName(), downtime, workingTime, downtimePercentage, availability);

        logger.debug("Область {} синхронизирована: downtime={}, wt={}, percentage={}, availability={}", 
                   area.getName(), downtime, workingTime, downtimePercentage, availability);
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
        sql.append("SELECT SUM(Duration) as total_duration ");
        sql.append("FROM REP_BreakdownReport ");
        sql.append("WHERE ");
        sql.append("((SDate_T1 >= ? AND SDate_T1 < ?) OR (SDate_T4 >= ? AND SDate_T4 < ?)) ");
        sql.append("AND (Comment NOT LIKE '%Cause:%Ошибочный запрос%' AND Comment NOT LIKE '%Cause:%Ложный вызов%') ");
        sql.append("AND NOT (LEN(Comment) BETWEEN 15 AND 19 AND WOStatusLocalDescr LIKE '%Закрыто%') ");
        sql.append("AND TYPEWO NOT LIKE '%Tag%' ");

        // Добавляем фильтр по области, если он задан
        if (area.getFilterColumn() != null && area.getFilterValue() != null) {
            sql.append("AND ").append(area.getFilterColumn()).append(" = ? ");
        }

        try {
            Double result;
            if (area.getFilterColumn() != null && area.getFilterValue() != null) {
                result = sqlServerJdbcTemplate.queryForObject(sql.toString(), Double.class, 
                                                             startDate, endDate, startDate, endDate, area.getFilterValue());
            } else {
                result = sqlServerJdbcTemplate.queryForObject(sql.toString(), Double.class, 
                                                             startDate, endDate, startDate, endDate);
            }
            
            return result != null ? result : 0.0;
        } catch (Exception e) {
            logger.error("Ошибка получения данных простоя из SQL Server для области {}: {}", 
                        area.getName(), e.getMessage());
            return 0.0;
        }
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
    private void saveMetricsToMysql(String areaName, Double downtime, Double workingTime, 
                                   Double downtimePercentage, Double availability) {
        // Удаляем старые записи для области
        repository.deleteByArea(areaName);

        // Создаем новую запись
        ProductionMetricsOnline metrics = new ProductionMetricsOnline(
            areaName, downtime, workingTime, downtimePercentage, availability
        );

        repository.save(metrics);
        logger.debug("Метрики для области {} сохранены в MySQL", areaName);
    }
}
