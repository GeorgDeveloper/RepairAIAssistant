package ru.georgdeveloper.assistantweb.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@RestController
@RequestMapping("/dashboard/online")
public class OnlineMetricsWebController {

    @Value("${core.service.url:http://localhost:8080}")
    private String coreServiceUrl;

    private final RestTemplate restTemplate;

    /**
     * Конструктор контроллера онлайн-метрик
     */
    public OnlineMetricsWebController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @SuppressWarnings("unchecked")
    @GetMapping("/bd")
    public List<Map<String, Object>> getBdMetrics() {
        try {
            return (List<Map<String, Object>>) (List<?>) restTemplate.getForObject(coreServiceUrl + "/api/online-metrics/bd", List.class);
        } catch (Exception e) {
            return generateMockBdData();
        }
    }

    @SuppressWarnings("unchecked")
    @GetMapping("/availability")
    public List<Map<String, Object>> getAvailabilityMetrics() {
        try {
            return (List<Map<String, Object>>) (List<?>) restTemplate.getForObject(coreServiceUrl + "/api/online-metrics/availability", List.class);
        } catch (Exception e) {
            return generateMockAvailabilityData();
        }
    }

    private List<Map<String, Object>> generateMockBdData() {
        List<Map<String, Object>> list = new ArrayList<>();
        list.add(Map.of("production_day", "01.09.2025", "downtime_percentage", 1.2));
        return list;
    }

    private List<Map<String, Object>> generateMockAvailabilityData() {
        List<Map<String, Object>> list = new ArrayList<>();
        list.add(Map.of("production_day", "01.09.2025", "availability", 98.5));
        return list;
    }
}