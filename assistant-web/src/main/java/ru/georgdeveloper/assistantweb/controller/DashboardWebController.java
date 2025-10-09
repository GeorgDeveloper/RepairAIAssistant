package ru.georgdeveloper.assistantweb.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/dashboard")
public class DashboardWebController {

    @Value("${core.service.url:http://localhost:8080}")
    private String coreServiceUrl;

    private final RestTemplate restTemplate;

    /**
     * Конструктор контроллера дашборда
     */
    public DashboardWebController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @SuppressWarnings("unchecked")
    @GetMapping("/breakDown")
    public List<Map<String, Object>> breakDown() {
        return (List<Map<String, Object>>) (List<?>) restTemplate.getForObject(coreServiceUrl + "/dashboard/breakDown", List.class);
    }

    @SuppressWarnings("unchecked")
    @GetMapping("/availability")
    public List<Map<String, Object>> availability() {
        return (List<Map<String, Object>>) (List<?>) restTemplate.getForObject(coreServiceUrl + "/dashboard/availability", List.class);
    }

    @SuppressWarnings("unchecked")
    @GetMapping("/current-metrics")
    public Map<String, Object> currentMetrics() {
        return (Map<String, Object>) restTemplate.getForObject(coreServiceUrl + "/dashboard/current-metrics", Map.class);
    }

    @SuppressWarnings("unchecked")
    @GetMapping("/top-breakdowns-week")
    public List<Map<String, Object>> topBreakdownsWeek() {
        return (List<Map<String, Object>>) (List<?>) restTemplate.getForObject(coreServiceUrl + "/dashboard/top-breakdowns-week", List.class);
    }

    @SuppressWarnings("unchecked")
    @GetMapping("/top-breakdowns-week-key-lines")
    public List<Map<String, Object>> topBreakdownsWeekKeyLines() {
        return (List<Map<String, Object>>) (List<?>) restTemplate.getForObject(coreServiceUrl + "/dashboard/top-breakdowns-week-key-lines", List.class);
    }

    @SuppressWarnings("unchecked")
    @GetMapping("/top-breakdowns")
    public List<Map<String, Object>> topBreakdownsDay() {
        return (List<Map<String, Object>>) (List<?>) restTemplate.getForObject(coreServiceUrl + "/dashboard/top-breakdowns", List.class);
    }

    @SuppressWarnings("unchecked")
    @GetMapping("/top-breakdowns-key-lines")
    public List<Map<String, Object>> topBreakdownsDayKeyLines() {
        return (List<Map<String, Object>>) (List<?>) restTemplate.getForObject(coreServiceUrl + "/dashboard/top-breakdowns-key-lines", List.class);
    }

    @SuppressWarnings("unchecked")
    @GetMapping("/top-breakdowns-current")
    public List<Map<String, Object>> topBreakdownsCurrent() {
        return (List<Map<String, Object>>) (List<?>) restTemplate.getForObject(coreServiceUrl + "/dashboard/top-breakdowns-current", List.class);
    }

    @SuppressWarnings("unchecked")
    @GetMapping("/pm-plan-fact-tag")
    public List<Map<String, Object>> pmPlanFactTag() {
        return (List<Map<String, Object>>) (List<?>) restTemplate.getForObject(coreServiceUrl + "/dashboard/pm-plan-fact-tag", List.class);
    }
}
