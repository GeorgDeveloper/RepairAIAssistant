package ru.georgdeveloper.assistantcore.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Сервис для работы с данными ключевых линий
 */
@Service
public class MainLinesService {

    @Autowired
    @Qualifier("sqlServerJdbcTemplate")
    private JdbcTemplate jdbcTemplate;

    /**
     * Возвращает метрики BD (downtime_percentage) за последние 24 часа по всем ключевым линиям
     */
    public List<Map<String, Object>> getMainLinesBdMetrics() {
        try {
            String sql = "SELECT line_name as lineName, area, FORMAT(last_update, 'HH:mm') as timestamp, downtime_percentage as value " +
                         "FROM main_lines_online " +
                         "WHERE last_update >= DATEADD(HOUR, -24, GETDATE()) " +
                         "ORDER BY last_update";
            return jdbcTemplate.queryForList(sql);
        } catch (Exception e) {
            System.err.println("Ошибка при получении метрик BD ключевых линий: " + e.getMessage());
            return new java.util.ArrayList<>();
        }
    }

    /**
     * Возвращает метрики Availability за последние 24 часа по всем ключевым линиям
     */
    public List<Map<String, Object>> getMainLinesAvailabilityMetrics() {
        try {
            String sql = "SELECT line_name as lineName, area, FORMAT(last_update, 'HH:mm') as timestamp, availability as value " +
                         "FROM main_lines_online " +
                         "WHERE last_update >= DATEADD(HOUR, -24, GETDATE()) " +
                         "ORDER BY last_update";
            return jdbcTemplate.queryForList(sql);
        } catch (Exception e) {
            System.err.println("Ошибка при получении метрик Availability ключевых линий: " + e.getMessage());
            return new java.util.ArrayList<>();
        }
    }

    /**
     * Возвращает текущие метрики всех ключевых линий (последние записи)
     */
    public List<Map<String, Object>> getCurrentMainLinesMetrics() {
        try {
            String sql = "SELECT m1.* FROM main_lines_online m1 " +
                         "WHERE m1.id IN (SELECT MAX(m2.id) FROM main_lines_online m2 GROUP BY m2.line_name) " +
                         "ORDER BY m1.line_name";
            return jdbcTemplate.queryForList(sql);
        } catch (Exception e) {
            // Если таблица не существует, возвращаем пустой список
            System.err.println("Таблица main_lines_online не найдена: " + e.getMessage());
            return new java.util.ArrayList<>();
        }
    }

    /**
     * Возвращает метрики ключевых линий по области
     */
    public List<Map<String, Object>> getMainLinesMetricsByArea(String area) {
        String sql = "SELECT m1.* FROM main_lines_online m1 " +
                     "WHERE m1.area = ? AND m1.id IN (SELECT MAX(m2.id) FROM main_lines_online m2 WHERE m2.area = ? GROUP BY m2.line_name) " +
                     "ORDER BY m1.line_name";
        return jdbcTemplate.queryForList(sql, area, area);
    }
}
