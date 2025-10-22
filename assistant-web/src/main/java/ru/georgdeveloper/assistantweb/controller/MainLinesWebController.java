package ru.georgdeveloper.assistantweb.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * Контроллер для проксирования запросов к API ключевых линий
 */
@RestController
@RequestMapping("/dashboard")
public class MainLinesWebController {

    @Value("${core.service.url:http://localhost:8080}")
    private String coreServiceUrl;

    private final RestTemplate restTemplate;

    public MainLinesWebController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @GetMapping("/main-lines/current")
    @ResponseBody
    public List<Map<String, Object>> getCurrentMainLinesMetrics() {
        return restTemplate.exchange(
            coreServiceUrl + "/dashboard/main-lines/current",
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<List<Map<String, Object>>>() {}
        ).getBody();
    }

    @GetMapping("/main-lines/bd")
    @ResponseBody
    public List<Map<String, Object>> getMainLinesBdMetrics() {
        return restTemplate.exchange(
            coreServiceUrl + "/dashboard/main-lines/bd",
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<List<Map<String, Object>>>() {}
        ).getBody();
    }

    @GetMapping("/main-lines/availability")
    @ResponseBody
    public List<Map<String, Object>> getMainLinesAvailabilityMetrics() {
        return restTemplate.exchange(
            coreServiceUrl + "/dashboard/main-lines/availability",
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<List<Map<String, Object>>>() {}
        ).getBody();
    }

    @GetMapping("/main-lines/by-area")
    @ResponseBody
    public List<Map<String, Object>> getMainLinesMetricsByArea(@RequestParam String area) {
        return restTemplate.exchange(
            coreServiceUrl + "/dashboard/main-lines/by-area?area=" + area,
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<List<Map<String, Object>>>() {}
        ).getBody();
    }
}
