package ru.georgdeveloper.assistantcore.service;

import org.springframework.beans.factory.annotation.Autowired;
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
    private JdbcTemplate jdbcTemplate;

    /**
     * Возвращает метрики BD (downtime_percentage) за последние 24 часа по всем ключевым линиям
     */
    public List<Map<String, Object>> getMainLinesBdMetrics() {
        String sql = "SELECT line_name as lineName, area, DATE_FORMAT(last_update, '%H:%i') as timestamp, downtime_percentage as value " +
                     "FROM main_lines_online " +
                     "WHERE last_update >= DATE_SUB(NOW(), INTERVAL 24 HOUR) " +
                     "ORDER BY last_update";
        return jdbcTemplate.queryForList(sql);
    }

    /**
     * Возвращает метрики Availability за последние 24 часа по всем ключевым линиям
     */
    public List<Map<String, Object>> getMainLinesAvailabilityMetrics() {
        String sql = "SELECT line_name as lineName, area, DATE_FORMAT(last_update, '%H:%i') as timestamp, availability as value " +
                     "FROM main_lines_online " +
                     "WHERE last_update >= DATE_SUB(NOW(), INTERVAL 24 HOUR) " +
                     "ORDER BY last_update";
        return jdbcTemplate.queryForList(sql);
    }

    /**
     * Возвращает текущие метрики всех ключевых линий (последние записи)
     */
    public List<Map<String, Object>> getCurrentMainLinesMetrics() {
        String sql = "SELECT m1.* FROM main_lines_online m1 " +
                     "WHERE m1.id IN (SELECT MAX(m2.id) FROM main_lines_online m2 GROUP BY m2.line_name) " +
                     "ORDER BY m1.line_name";
        return jdbcTemplate.queryForList(sql);
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
