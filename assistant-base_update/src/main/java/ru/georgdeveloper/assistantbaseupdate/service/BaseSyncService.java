package ru.georgdeveloper.assistantbaseupdate.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.georgdeveloper.assistantbaseupdate.config.DataSyncProperties;
import ru.georgdeveloper.assistantbaseupdate.util.MetricsCalculator;
import ru.georgdeveloper.assistantbaseupdate.util.RetryExecutor;
import ru.georgdeveloper.assistantbaseupdate.util.ShiftCalculator;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Supplier;

/**
 * Базовый абстрактный класс для сервисов синхронизации данных.
 * Содержит общую логику для работы с базами данных и расчета метрик.
 */
public abstract class BaseSyncService {
    
    protected final Logger logger = LoggerFactory.getLogger(getClass());
    
    @Autowired
    protected DataSyncProperties dataSyncProperties;
    
    @Autowired
    @Qualifier("sqlServerJdbcTemplate")
    protected JdbcTemplate sqlServerJdbcTemplate;
    
    @Autowired
    @Qualifier("mysqlJdbcTemplate")
    protected JdbcTemplate mysqlJdbcTemplate;
    
    /**
     * Определяет диапазон дат для запроса (текущая смена)
     * @return массив из двух элементов: [начало_смены, конец_смены]
     */
    protected LocalDateTime[] calculateDateRange() {
        LocalDateTime now = LocalDateTime.now();
        return ShiftCalculator.getCurrentShiftRange(now);
    }
    
    /**
     * Получает рабочее время из MySQL для указанной области и даты
     * @param area область
     * @param searchDate дата поиска в формате dd.MM.yyyy
     * @return рабочее время в минутах или null если не найдено
     */
    protected Double getWorkingTime(DataSyncProperties.Area area, String searchDate) {
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
     * Рассчитывает новое рабочее время по формуле
     * @param workingTime исходное рабочее время
     * @return пересчитанное рабочее время
     */
    protected Double calculateNewWorkingTime(Double workingTime) {
        return MetricsCalculator.calculateNewWorkingTime(workingTime);
    }
    
    /**
     * Рассчитывает инкрементальное рабочее время
     * @param fullWorkingTime полное рабочее время
     * @param startDate время начала периода
     * @return инкрементальное рабочее время
     */
    protected Double calculateIncrementalWorkingTime(Double fullWorkingTime, LocalDateTime startDate) {
        return MetricsCalculator.calculateIncrementalWorkingTime(fullWorkingTime, startDate);
    }
    
    /**
     * Рассчитывает процент простоя
     * @param downtime время простоя
     * @param workingTime рабочее время
     * @return процент простоя
     */
    protected Double calculateDowntimePercentage(Double downtime, Double workingTime) {
        return MetricsCalculator.calculateDowntimePercentage(downtime, workingTime);
    }
    
    /**
     * Рассчитывает доступность оборудования
     * @param downtimePercentage процент простоя
     * @return доступность в процентах
     */
    protected Double calculateAvailability(Double downtimePercentage) {
        return MetricsCalculator.calculateAvailability(downtimePercentage);
    }
    
    /**
     * Выполняет операцию с повторными попытками при deadlock
     * @param operation операция для выполнения
     * @param operationName название операции
     * @return результат выполнения
     */
    protected <T> T executeWithRetry(Supplier<T> operation, String operationName) {
        return RetryExecutor.executeWithRetry(operation, operationName, logger);
    }
    
    /**
     * Форматирует дату для поиска в базе данных
     * @param dateTime дата и время
     * @return отформатированная дата в формате dd.MM.yyyy
     */
    protected String formatDateForSearch(LocalDateTime dateTime) {
        return dateTime.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
    }
    
    /**
     * Проверяет, включена ли синхронизация в конфигурации
     * @return true если синхронизация включена
     */
    protected boolean isSyncEnabled() {
        return dataSyncProperties.isEnabled();
    }
}
