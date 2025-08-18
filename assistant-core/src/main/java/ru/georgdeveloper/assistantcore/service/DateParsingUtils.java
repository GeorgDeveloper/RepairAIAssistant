package ru.georgdeveloper.assistantcore.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Утилиты для парсинга дат в различных форматах
 */
public class DateParsingUtils {
    
    private static final Map<String, String> MONTH_MAPPING = new HashMap<>();
    
    static {
        MONTH_MAPPING.put("январ", "Январь");
        MONTH_MAPPING.put("феврал", "Февраль");
        MONTH_MAPPING.put("март", "Март");
        MONTH_MAPPING.put("апрел", "Апрель");
        MONTH_MAPPING.put("май", "Май");
        MONTH_MAPPING.put("июн", "Июнь");
        MONTH_MAPPING.put("июл", "Июль");
        MONTH_MAPPING.put("август", "Август");
        MONTH_MAPPING.put("сентябр", "Сентябрь");
        MONTH_MAPPING.put("октябр", "Октябрь");
        MONTH_MAPPING.put("ноябр", "Ноябрь");
        MONTH_MAPPING.put("декабр", "Декабрь");
    }
    
    /**
     * Парсит дату в различных форматах
     */
    public static String parseDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }
        
        String normalized = dateStr.toLowerCase().trim();
        
        // Формат dd.MM.yyyy
        if (normalized.matches("\\d{1,2}\\.\\d{1,2}\\.\\d{4}")) {
            return validateAndFormatDate(normalized, "dd.MM.yyyy");
        }
        
        // Формат yyyy-MM-dd
        if (normalized.matches("\\d{4}-\\d{1,2}-\\d{1,2}")) {
            return validateAndFormatDate(normalized, "yyyy-MM-dd");
        }
        
        // Формат dd/MM/yyyy
        if (normalized.matches("\\d{1,2}/\\d{1,2}/\\d{4}")) {
            return validateAndFormatDate(normalized.replace("/", "."), "dd.MM.yyyy");
        }
        
        // Месяцы
        for (Map.Entry<String, String> entry : MONTH_MAPPING.entrySet()) {
            if (normalized.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        
        return dateStr;
    }
    
    /**
     * Извлекает период дат из строки
     */
    public static DateRange extractDateRange(String text) {
        if (text == null) return null;
        
        String normalized = text.toLowerCase();
        
        // Паттерн для периодов: "январь-март", "01.01-15.01", "2024-01-01 - 2024-03-31"
        Pattern rangePattern = Pattern.compile("([\\w\\d\\.\\-]+)\\s*[-–—]\\s*([\\w\\d\\.\\-]+)");
        Matcher matcher = rangePattern.matcher(normalized);
        
        if (matcher.find()) {
            String start = matcher.group(1).trim();
            String end = matcher.group(2).trim();
            
            String parsedStart = parseDate(start);
            String parsedEnd = parseDate(end);
            
            if (parsedStart != null && parsedEnd != null) {
                return new DateRange(parsedStart, parsedEnd, true);
            }
        }
        
        // Одиночная дата
        String singleDate = parseDate(normalized);
        if (singleDate != null && !singleDate.equals(normalized)) {
            return new DateRange(singleDate, null, false);
        }
        
        return null;
    }
    
    private static String validateAndFormatDate(String dateStr, String pattern) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
            LocalDate.parse(dateStr, formatter);
            return dateStr; // Возвращаем оригинальный формат если валидация прошла
        } catch (DateTimeParseException e) {
            return null;
        }
    }
    
    /**
     * Класс для хранения диапазона дат
     */
    public static class DateRange {
        private final String startDate;
        private final String endDate;
        private final boolean isRange;
        
        public DateRange(String startDate, String endDate, boolean isRange) {
            this.startDate = startDate;
            this.endDate = endDate;
            this.isRange = isRange;
        }
        
        public String getStartDate() { return startDate; }
        public String getEndDate() { return endDate; }
        public boolean isRange() { return isRange; }
    }
}