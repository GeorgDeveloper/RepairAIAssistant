package ru.georgdeveloper.assistantcore.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class OnlineMetricsService {

    @Autowired
    @Qualifier("sqlServerJdbcTemplate")
    private JdbcTemplate jdbcTemplate;

    /**
     * Возвращает метрики BD (downtime_percentage) за последние 24 часа по всем областям
     */
    public List<Map<String, Object>> getBdMetrics() {
        try {
            String sql = "SELECT area, FORMAT(last_update, 'HH:mm') as timestamp, downtime_percentage as value " +
                         "FROM production_metrics_online " +
                         "WHERE last_update >= DATEADD(HOUR, -24, GETDATE()) " +
                         "ORDER BY last_update";
            return jdbcTemplate.queryForList(sql);
        } catch (Exception e) {
            System.err.println("Ошибка при получении метрик BD: " + e.getMessage());
            return new java.util.ArrayList<>();
        }
    }

    /**
     * Возвращает метрики Availability за последние 24 часа по всем областям
     */
    public List<Map<String, Object>> getAvailabilityMetrics() {
        try {
            String sql = "SELECT area, FORMAT(last_update, 'HH:mm') as timestamp, availability as value " +
                         "FROM production_metrics_online " +
                         "WHERE last_update >= DATEADD(HOUR, -24, GETDATE()) " +
                         "ORDER BY last_update";
            return jdbcTemplate.queryForList(sql);
        } catch (Exception e) {
            System.err.println("Ошибка при получении метрик Availability: " + e.getMessage());
            return new java.util.ArrayList<>();
        }
    }
}