package ru.georgdeveloper.assistantweb.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@RestController
@RequestMapping("/dashboard/online")
@CrossOrigin(origins = "*")
public class OnlineMetricsWebController {

    @Value("${core.service.url:http://localhost:8080}")
    private String coreServiceUrl;

    @Autowired
    private RestTemplate restTemplate;

    @GetMapping("/bd")
    public List<Map<String, Object>> getBdMetrics() {
        try {
            return restTemplate.getForObject(coreServiceUrl + "/api/online-metrics/bd", List.class);
        } catch (Exception e) {
            return generateMockBdData();
        }
    }

    @GetMapping("/availability")
    public List<Map<String, Object>> getAvailabilityMetrics() {
        try {
            return restTemplate.getForObject(coreServiceUrl + "/api/online-metrics/availability", List.class);
        } catch (Exception e) {
            return generateMockAvailabilityData();
        }
    }

    private List<Map<String, Object>> generateMockBdData() {
        List<Map<String, Object>> data = new ArrayList<>();
        String[] areas = {"Резиносмешение", "Сборка 1", "Сборка 2", "Вулканизация", "УЗО", "Модули", "Завод"};
        Random random = new Random();
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        
        for (int i = 0; i < 24; i++) {
            java.time.LocalDateTime time = now.minusHours(23 - i);
            for (String area : areas) {
                Map<String, Object> point = new HashMap<>();
                point.put("area", area);
                point.put("timestamp", time.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")));
                point.put("value", Math.round((random.nextDouble() * 5 + 0.5) * 100.0) / 100.0);
                data.add(point);
            }
        }
        return data;
    }

    private List<Map<String, Object>> generateMockAvailabilityData() {
        List<Map<String, Object>> data = new ArrayList<>();
        String[] areas = {"Резиносмешение", "Сборка 1", "Сборка 2", "Вулканизация", "УЗО", "Модули", "Завод"};
        Random random = new Random();
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        
        for (int i = 0; i < 24; i++) {
            java.time.LocalDateTime time = now.minusHours(23 - i);
            for (String area : areas) {
                Map<String, Object> point = new HashMap<>();
                point.put("area", area);
                point.put("timestamp", time.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")));
                point.put("value", Math.round((random.nextDouble() * 10 + 90) * 100.0) / 100.0);
                data.add(point);
            }
        }
        return data;
    }
}