package ru.georgdeveloper.assistantcore.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class OnlineMetricsService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * Возвращает метрики BD (downtime_percentage) за последние 24 часа по всем областям
     */
    public List<Map<String, Object>> getBdMetrics() {
        String sql = "SELECT area, last_update as timestamp, downtime_percentage as value " +
                     "FROM production_metrics_online " +
                     "WHERE last_update >= DATE_SUB(NOW(), INTERVAL 24 HOUR) " +
                     "ORDER BY last_update";
        return jdbcTemplate.queryForList(sql);
    }

    /**
     * Возвращает метрики Availability за последние 24 часа по всем областям
     */
    public List<Map<String, Object>> getAvailabilityMetrics() {
        String sql = "SELECT area, last_update as timestamp, availability as value " +
                     "FROM production_metrics_online " +
                     "WHERE last_update >= DATE_SUB(NOW(), INTERVAL 24 HOUR) " +
                     "ORDER BY last_update";
        return jdbcTemplate.queryForList(sql);
    }
}