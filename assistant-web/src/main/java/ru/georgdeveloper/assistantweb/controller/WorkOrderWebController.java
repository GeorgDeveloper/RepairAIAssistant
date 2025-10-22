package ru.georgdeveloper.assistantweb.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * Контроллер для проксирования запросов к API нарядов на работы
 */
@RestController
@RequestMapping("/api/work-orders")
public class WorkOrderWebController {

    @Value("${base-update.service.url:http://localhost:8084}")
    private String baseUpdateServiceUrl;

    private final RestTemplate restTemplate;

    public WorkOrderWebController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Получение последних 15 нарядов на работы для отображения в таблице
     */
    @GetMapping("/dashboard")
    @ResponseBody
    public List<Map<String, Object>> getWorkOrdersForDashboard() {
        return restTemplate.exchange(
            baseUpdateServiceUrl + "/api/work-orders/dashboard",
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<List<Map<String, Object>>>() {}
        ).getBody();
    }

    /**
     * Получение активных нарядов (не закрытых)
     */
    @GetMapping("/active")
    @ResponseBody
    public List<Map<String, Object>> getActiveWorkOrders() {
        return restTemplate.exchange(
            baseUpdateServiceUrl + "/api/work-orders/active",
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<List<Map<String, Object>>>() {}
        ).getBody();
    }

    /**
     * Поиск нарядов по ключевому слову
     */
    @GetMapping("/search")
    @ResponseBody
    public List<Map<String, Object>> searchWorkOrders(@RequestParam("keyword") String keyword) {
        return restTemplate.exchange(
            baseUpdateServiceUrl + "/api/work-orders/search?keyword=" + keyword,
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<List<Map<String, Object>>>() {}
        ).getBody();
    }

    /**
     * Получение последних 15 нарядов (совместимость с существующим API)
     */
    @GetMapping("/last-15")
    @ResponseBody
    public List<Map<String, Object>> getLast15WorkOrders() {
        return restTemplate.exchange(
            baseUpdateServiceUrl + "/api/work-orders/last-15",
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<List<Map<String, Object>>>() {}
        ).getBody();
    }
}
