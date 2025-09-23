package ru.georgdeveloper.assistantweb.controller;

import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    private RestTemplate restTemplate;

    @GetMapping("/breakDown")
    public List<Map<String, Object>> breakDown() {
        return restTemplate.getForObject(coreServiceUrl + "/dashboard/breakDown", List.class);
    }

    @GetMapping("/availability")
    public List<Map<String, Object>> availability() {
        return restTemplate.getForObject(coreServiceUrl + "/dashboard/availability", List.class);
    }

    @GetMapping("/current-metrics")
    public Map<String, Object> currentMetrics() {
        return restTemplate.getForObject(coreServiceUrl + "/dashboard/current-metrics", Map.class);
    }

    @GetMapping("/top-breakdowns-week")
    public List<Map<String, Object>> topBreakdownsWeek() {
        return restTemplate.getForObject(coreServiceUrl + "/dashboard/top-breakdowns-week", List.class);
    }

    @GetMapping("/top-breakdowns-week-key-lines")
    public List<Map<String, Object>> topBreakdownsWeekKeyLines() {
        return restTemplate.getForObject(coreServiceUrl + "/dashboard/top-breakdowns-week-key-lines", List.class);
    }

    @GetMapping("/top-breakdowns")
    public List<Map<String, Object>> topBreakdownsDay() {
        return restTemplate.getForObject(coreServiceUrl + "/dashboard/top-breakdowns", List.class);
    }

    @GetMapping("/top-breakdowns-key-lines")
    public List<Map<String, Object>> topBreakdownsDayKeyLines() {
        return restTemplate.getForObject(coreServiceUrl + "/dashboard/top-breakdowns-key-lines", List.class);
    }

    @GetMapping("/pm-plan-fact-tag")
    public List<Map<String, Object>> pmPlanFactTag() {
        return restTemplate.getForObject(coreServiceUrl + "/dashboard/pm-plan-fact-tag", List.class);
    }
}
