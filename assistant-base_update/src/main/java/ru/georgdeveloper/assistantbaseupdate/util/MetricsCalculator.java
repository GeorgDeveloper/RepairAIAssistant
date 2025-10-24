package ru.georgdeveloper.assistantbaseupdate.util;

import java.time.LocalDateTime;

/**
 * Утилитарный класс для расчета производственных метрик.
 */
public final class MetricsCalculator {
    
    private static final double MINUTES_IN_DAY = 24 * 60; // 1440 минут в сутках
    private static final double INTERVAL_MINUTES = 3.0; // 3-минутные интервалы
    private static final double TOTAL_INTERVALS_PER_DAY = MINUTES_IN_DAY / INTERVAL_MINUTES; // 480 интервалов
    
    private MetricsCalculator() {
        // Utility class
    }
    
    /**
     * Рассчитывает новое рабочее время по формуле: new_working_time = (working_time/(24*60))*3
     * @param workingTime исходное рабочее время в минутах
     * @return пересчитанное рабочее время
     */
    public static double calculateNewWorkingTime(Double workingTime) {
        if (workingTime == null || workingTime == 0) {
            return workingTime != null ? workingTime : 0.0;
        }
        
        // new_working_time = (working_time/(24*60))*3
        double newWorkingTime = (workingTime / MINUTES_IN_DAY) * INTERVAL_MINUTES;
        
        return Math.round(newWorkingTime * 100.0) / 100.0; // Округление до 2 знаков
    }
    
    /**
     * Рассчитывает инкрементальное рабочее время на основе текущего времени
     * @param fullWorkingTime полное рабочее время
     * @param startDate время начала периода
     * @return инкрементальное рабочее время
     */
    public static double calculateIncrementalWorkingTime(Double fullWorkingTime, LocalDateTime startDate) {
        if (fullWorkingTime == null || fullWorkingTime == 0) {
            return fullWorkingTime != null ? fullWorkingTime : 0.0;
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
        long intervals = minutesFromStart / (long) INTERVAL_MINUTES + 1; // +1 чтобы начинать с 1
        
        // Рассчитываем инкремент на интервал
        double increment = fullWorkingTime / TOTAL_INTERVALS_PER_DAY;
        
        return Math.round(increment * intervals * 100.0) / 100.0;
    }
    
    /**
     * Рассчитывает процент простоя
     * @param downtime время простоя в минутах
     * @param workingTime рабочее время в минутах
     * @return процент простоя или null если данные некорректны
     */
    public static Double calculateDowntimePercentage(Double downtime, Double workingTime) {
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
     * @param downtimePercentage процент простоя
     * @return доступность в процентах или null если данные некорректны
     */
    public static Double calculateAvailability(Double downtimePercentage) {
        if (downtimePercentage == null) {
            return null; // NULL для MySQL
        }
        return Math.round((100 - downtimePercentage) * 100.0) / 100.0; // Округление до 2 знаков
    }
    
    /**
     * Рассчитывает скорректированное рабочее время с учетом количества единиц оборудования
     * @param baseWorkingTime базовое рабочее время
     * @param recordCount количество записей/единиц оборудования
     * @return скорректированное рабочее время
     */
    public static double calculateAdjustedWorkingTime(Double baseWorkingTime, Double recordCount) {
        if (baseWorkingTime == null || baseWorkingTime == 0) {
            return baseWorkingTime != null ? baseWorkingTime : 0.0;
        }
        
        // Если нет записей простоя, возвращаем базовое рабочее время
        if (recordCount == null || recordCount == 0) {
            return baseWorkingTime;
        }
        
        // Умножаем рабочее время на количество единиц оборудования
        return baseWorkingTime * recordCount;
    }
}
