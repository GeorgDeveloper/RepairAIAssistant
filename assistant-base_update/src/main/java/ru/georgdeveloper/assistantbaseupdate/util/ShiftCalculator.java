package ru.georgdeveloper.assistantbaseupdate.util;

import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Утилитарный класс для расчета смен и времени с начала смены.
 * 
 * Смена:
 * - 24-часовая смена: 08:00 - 08:00 (следующего дня)
 */
public class ShiftCalculator {
    
    private static final LocalTime SHIFT_START = LocalTime.of(8, 0);
    
    /**
     * Определяет, активна ли текущая смена
     * @param currentTime текущее время
     * @return true если текущая смена активна (08:00-08:00)
     */
    public static boolean isCurrentShift(LocalDateTime currentTime) {
        // Смена всегда активна, так как она 24-часовая
        return true;
    }
    
    /**
     * Получает время начала текущей смены
     * @param currentTime текущее время
     * @return время начала текущей смены
     */
    public static LocalDateTime getCurrentShiftStart(LocalDateTime currentTime) {
        LocalTime time = currentTime.toLocalTime();
        
        if (time.isBefore(SHIFT_START)) {
            // Если сейчас раньше 08:00, смена началась вчера в 08:00
            return currentTime.minusDays(1).withHour(8).withMinute(0).withSecond(0).withNano(0);
        } else {
            // Если сейчас 08:00 или позже, смена началась сегодня в 08:00
            return currentTime.withHour(8).withMinute(0).withSecond(0).withNano(0);
        }
    }
    
    /**
     * Получает время окончания текущей смены
     * @param currentTime текущее время
     * @return время окончания текущей смены
     */
    public static LocalDateTime getCurrentShiftEnd(LocalDateTime currentTime) {
        LocalTime time = currentTime.toLocalTime();
        
        if (time.isBefore(SHIFT_START)) {
            // Если сейчас раньше 08:00, смена закончится сегодня в 08:00
            return currentTime.withHour(8).withMinute(0).withSecond(0).withNano(0);
        } else {
            // Если сейчас 08:00 или позже, смена закончится завтра в 08:00
            return currentTime.plusDays(1).withHour(8).withMinute(0).withSecond(0).withNano(0);
        }
    }
    
    /**
     * Рассчитывает время, прошедшее с начала текущей смены в минутах
     * @param currentTime текущее время
     * @return количество минут с начала смены
     */
    public static long getMinutesFromShiftStart(LocalDateTime currentTime) {
        LocalDateTime shiftStart = getCurrentShiftStart(currentTime);
        return java.time.Duration.between(shiftStart, currentTime).toMinutes();
    }
    
    /**
     * Рассчитывает общую продолжительность текущей смены в минутах
     * @param currentTime текущее время
     * @return общая продолжительность смены в минутах
     */
    public static long getTotalShiftMinutes(LocalDateTime currentTime) {
        LocalDateTime shiftStart = getCurrentShiftStart(currentTime);
        LocalDateTime shiftEnd = getCurrentShiftEnd(currentTime);
        return java.time.Duration.between(shiftStart, shiftEnd).toMinutes();
    }
    
    /**
     * Рассчитывает инкрементальное рабочее время на основе времени с начала смены
     * @param fullWorkingTime полное рабочее время за смену в минутах
     * @param currentTime текущее время
     * @return инкрементальное рабочее время в минутах
     */
    public static double calculateIncrementalWorkingTime(double fullWorkingTime, LocalDateTime currentTime) {
        if (fullWorkingTime <= 0) {
            return 0.0;
        }
        
        long minutesFromStart = getMinutesFromShiftStart(currentTime);
        long totalShiftMinutes = getTotalShiftMinutes(currentTime);
        
        // Если время с начала смены больше или равно общей продолжительности смены,
        // возвращаем полное рабочее время
        if (minutesFromStart >= totalShiftMinutes) {
            return fullWorkingTime;
        }
        
        // Рассчитываем инкрементальное рабочее время пропорционально прошедшему времени
        double incrementalWorkingTime = (fullWorkingTime * minutesFromStart) / totalShiftMinutes;
        
        // Округляем до 2 знаков после запятой
        return Math.round(incrementalWorkingTime * 100.0) / 100.0;
    }
    
    /**
     * Получает диапазон дат для текущей смены
     * @param currentTime текущее время
     * @return массив из двух элементов: [начало_смены, конец_смены]
     */
    public static LocalDateTime[] getCurrentShiftRange(LocalDateTime currentTime) {
        return new LocalDateTime[]{
            getCurrentShiftStart(currentTime),
            getCurrentShiftEnd(currentTime)
        };
    }
}
