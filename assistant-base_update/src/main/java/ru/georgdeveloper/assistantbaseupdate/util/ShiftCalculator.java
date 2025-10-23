package ru.georgdeveloper.assistantbaseupdate.util;

import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Утилитарный класс для расчета смен и времени с начала смены.
 * 
 * Смены:
 * - Дневная смена: 08:00 - 20:00
 * - Ночная смена: 20:00 - 08:00 (следующего дня)
 */
public class ShiftCalculator {
    
    private static final LocalTime DAY_SHIFT_START = LocalTime.of(8, 0);
    private static final LocalTime NIGHT_SHIFT_START = LocalTime.of(20, 0);
    
    /**
     * Определяет, какая смена сейчас активна
     * @param currentTime текущее время
     * @return true если дневная смена (08:00-20:00), false если ночная смена (20:00-08:00)
     */
    public static boolean isDayShift(LocalDateTime currentTime) {
        LocalTime time = currentTime.toLocalTime();
        return !time.isBefore(DAY_SHIFT_START) && time.isBefore(NIGHT_SHIFT_START);
    }
    
    /**
     * Получает время начала текущей смены
     * @param currentTime текущее время
     * @return время начала текущей смены
     */
    public static LocalDateTime getCurrentShiftStart(LocalDateTime currentTime) {
        if (isDayShift(currentTime)) {
            // Дневная смена: начало в 08:00 сегодня
            return currentTime.withHour(8).withMinute(0).withSecond(0).withNano(0);
        } else {
            // Ночная смена: начало в 20:00 вчера (если сейчас до 08:00) или сегодня (если сейчас после 20:00)
            if (currentTime.toLocalTime().isBefore(DAY_SHIFT_START)) {
                // Сейчас ночь, смена началась вчера в 20:00
                return currentTime.minusDays(1).withHour(20).withMinute(0).withSecond(0).withNano(0);
            } else {
                // Сейчас вечер, смена началась сегодня в 20:00
                return currentTime.withHour(20).withMinute(0).withSecond(0).withNano(0);
            }
        }
    }
    
    /**
     * Получает время окончания текущей смены
     * @param currentTime текущее время
     * @return время окончания текущей смены
     */
    public static LocalDateTime getCurrentShiftEnd(LocalDateTime currentTime) {
        if (isDayShift(currentTime)) {
            // Дневная смена: окончание в 20:00 сегодня
            return currentTime.withHour(20).withMinute(0).withSecond(0).withNano(0);
        } else {
            // Ночная смена: окончание в 08:00 завтра
            if (currentTime.toLocalTime().isBefore(DAY_SHIFT_START)) {
                // Сейчас ночь, смена закончится сегодня в 08:00
                return currentTime.withHour(8).withMinute(0).withSecond(0).withNano(0);
            } else {
                // Сейчас вечер, смена закончится завтра в 08:00
                return currentTime.plusDays(1).withHour(8).withMinute(0).withSecond(0).withNano(0);
            }
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
        
        
        // Округляем до 2 знаков после запятой
        return minutesFromStart;
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
