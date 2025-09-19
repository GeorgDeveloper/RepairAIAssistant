package ru.georgdeveloper.assistantcore.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import ru.georgdeveloper.assistantcore.service.OnlineMetricsService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/online-metrics")
@CrossOrigin(origins = "*")
public class OnlineMetricsController {

    @Autowired
    private OnlineMetricsService onlineMetricsService;

    @GetMapping("/bd")
    public List<Map<String, Object>> getBdMetrics() {
        return onlineMetricsService.getBdMetrics();
    }

    @GetMapping("/availability")
    public List<Map<String, Object>> getAvailabilityMetrics() {
        return onlineMetricsService.getAvailabilityMetrics();
    }
}