package ru.georgdeveloper.assistantcore.service;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class OnlineMetricsService {

    private final String[] areas = {"Резиносмешение", "Сборка 1", "Сборка 2", "Вулканизация", "УЗО", "Модули", "Завод"};
    private final Random random = new Random();

    public List<Map<String, Object>> getBdMetrics() {
        List<Map<String, Object>> metrics = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        
        for (int i = 0; i < 24; i++) {
            LocalDateTime time = now.minusHours(23 - i);
            for (String area : areas) {
                Map<String, Object> metric = new HashMap<>();
                metric.put("area", area);
                metric.put("timestamp", time.format(DateTimeFormatter.ofPattern("HH:mm")));
                metric.put("value", Math.round((random.nextDouble() * 5 + 0.5) * 100.0) / 100.0);
                metrics.add(metric);
            }
        }
        return metrics;
    }

    public List<Map<String, Object>> getAvailabilityMetrics() {
        List<Map<String, Object>> metrics = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        
        for (int i = 0; i < 24; i++) {
            LocalDateTime time = now.minusHours(23 - i);
            for (String area : areas) {
                Map<String, Object> metric = new HashMap<>();
                metric.put("area", area);
                metric.put("timestamp", time.format(DateTimeFormatter.ofPattern("HH:mm")));
                metric.put("value", Math.round((random.nextDouble() * 10 + 90) * 100.0) / 100.0);
                metrics.add(metric);
            }
        }
        return metrics;
    }
}