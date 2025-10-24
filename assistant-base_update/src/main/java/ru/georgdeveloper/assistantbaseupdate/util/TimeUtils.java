package ru.georgdeveloper.assistantbaseupdate.util;

import java.sql.Time;
import java.time.LocalTime;

/**
 * Утилитарный класс для работы с временем и его форматированием.
 */
public final class TimeUtils {
    
    private TimeUtils() {
        // Utility class
    }
    
    /**
     * Парсинг времени из SQL Server формата
     * @param timeStr строка времени в различных форматах
     * @return объект Time или null если не удалось распарсить
     */
    public static Time parseSqlTime(String timeStr) {
        if (timeStr == null || timeStr.trim().isEmpty()) {
            return null;
        }
        
        try {
            timeStr = timeStr.trim().replace(" ", "");
            
            if (timeStr.contains(":")) {
                return parseTimeWithColons(timeStr);
            } else if (timeStr.matches("\\d+")) {
                return parseTimeFromMinutes(timeStr);
            }
            
        } catch (Exception e) {
            // Логирование ошибки можно добавить при необходимости
        }
        
        return null;
    }
    
    /**
     * Парсинг времени в формате с двоеточиями
     */
    private static Time parseTimeWithColons(String timeStr) {
        String[] parts = timeStr.split(":");
        
        if (parts.length == 2) {
            // Формат "часы:минуты"
            int hours = Integer.parseInt(parts[0]);
            int minutes = Integer.parseInt(parts[1]);
            
            if (isValidTime(hours, minutes, 0)) {
                return Time.valueOf(LocalTime.of(hours, minutes, 0));
            }
        } else if (parts.length == 3) {
            // Формат "часы:минуты:секунды"
            int hours = Integer.parseInt(parts[0]);
            int minutes = Integer.parseInt(parts[1]);
            String secondsPart = parts[2];
            int seconds = secondsPart.contains(".") ? 
                (int) Double.parseDouble(secondsPart) : 
                Integer.parseInt(secondsPart);
            
            if (isValidTime(hours, minutes, seconds)) {
                return Time.valueOf(LocalTime.of(hours, minutes, seconds));
            }
        }
        
        return null;
    }
    
    /**
     * Парсинг времени из минут
     */
    private static Time parseTimeFromMinutes(String timeStr) {
        int totalMinutes = Integer.parseInt(timeStr);
        int hours = totalMinutes / 60;
        int minutes = totalMinutes % 60;
        
        if (isValidTime(hours, minutes, 0)) {
            return Time.valueOf(LocalTime.of(hours, minutes, 0));
        }
        
        return null;
    }
    
    /**
     * Проверка валидности времени
     */
    private static boolean isValidTime(int hours, int minutes, int seconds) {
        return hours >= 0 && minutes >= 0 && minutes <= 59 && seconds >= 0 && seconds <= 59;
    }
    
    /**
     * Форматирование длительности в читаемый вид
     * @param duration длительность в секундах
     * @return отформатированная строка в формате "H.mm:ss"
     */
    public static String formatDuration(Integer duration) {
        if (duration == null) {
            return "0.00:00";
        }
        
        int hours = duration / 3600;
        int minutes = (duration % 3600) / 60;
        int seconds = duration % 60;
        
        return String.format("%d.%02d:%02d", hours, minutes, seconds);
    }
    
    /**
     * Преобразование минут в формат времени
     * @param totalMinutes общее количество минут
     * @return строка в формате "HH:mm:ss"
     */
    public static String minutesToTime(double totalMinutes) {
        int hours = (int) Math.floor(totalMinutes / 60.0);
        int minutes = (int) Math.floor(totalMinutes % 60.0);
        int seconds = (int) Math.floor((totalMinutes * 60.0) % 60.0);
        
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }
    
    /**
     * Безопасное получение строкового значения
     * @param value значение для преобразования
     * @return строковое представление или пустая строка
     */
    public static String getStringValue(Object value) {
        return value != null ? value.toString() : "";
    }
}
