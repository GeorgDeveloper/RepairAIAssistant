package ru.georgdeveloper.assistantweb.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Контроллер для проксирования запросов к API ключевых линий
 */
@RestController
@RequestMapping("/dashboard")
public class MainLinesWebController {

    private static final Logger logger = LoggerFactory.getLogger(MainLinesWebController.class);

    @Value("${core.service.url:http://localhost:8080}")
    private String coreServiceUrl;

    private final RestTemplate restTemplate;

    public MainLinesWebController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @GetMapping("/main-lines/current")
    @ResponseBody
    public List<Map<String, Object>> getCurrentMainLinesMetrics() {
        try {
            logger.debug("Запрос текущих метрик ключевых линий к сервису: {}", coreServiceUrl + "/dashboard/main-lines/current");
            return restTemplate.exchange(
                coreServiceUrl + "/dashboard/main-lines/current",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<Map<String, Object>>>() {}
            ).getBody();
        } catch (RestClientException e) {
            logger.error("Ошибка подключения к сервису ключевых линий ({}): {}", 
                        coreServiceUrl, e.getMessage());
            return new ArrayList<>();
        } catch (Exception e) {
            logger.error("Неожиданная ошибка при получении метрик ключевых линий: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    @GetMapping("/main-lines/bd")
    @ResponseBody
    public List<Map<String, Object>> getMainLinesBdMetrics() {
        try {
            logger.debug("Запрос BD метрик ключевых линий к сервису: {}", coreServiceUrl + "/dashboard/main-lines/bd");
            return restTemplate.exchange(
                coreServiceUrl + "/dashboard/main-lines/bd",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<Map<String, Object>>>() {}
            ).getBody();
        } catch (RestClientException e) {
            logger.error("Ошибка подключения к сервису ключевых линий ({}): {}", 
                        coreServiceUrl, e.getMessage());
            return new ArrayList<>();
        } catch (Exception e) {
            logger.error("Неожиданная ошибка при получении BD метрик ключевых линий: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    @GetMapping("/main-lines/availability")
    @ResponseBody
    public List<Map<String, Object>> getMainLinesAvailabilityMetrics() {
        try {
            logger.debug("Запрос метрик доступности ключевых линий к сервису: {}", coreServiceUrl + "/dashboard/main-lines/availability");
            return restTemplate.exchange(
                coreServiceUrl + "/dashboard/main-lines/availability",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<Map<String, Object>>>() {}
            ).getBody();
        } catch (RestClientException e) {
            logger.error("Ошибка подключения к сервису ключевых линий ({}): {}", 
                        coreServiceUrl, e.getMessage());
            return new ArrayList<>();
        } catch (Exception e) {
            logger.error("Неожиданная ошибка при получении метрик доступности ключевых линий: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    @GetMapping("/main-lines/by-area")
    @ResponseBody
    public List<Map<String, Object>> getMainLinesMetricsByArea(@RequestParam String area) {
        try {
            logger.debug("Запрос метрик ключевых линий по области '{}' к сервису: {}", area, coreServiceUrl + "/dashboard/main-lines/by-area");
            return restTemplate.exchange(
                coreServiceUrl + "/dashboard/main-lines/by-area?area=" + area,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<Map<String, Object>>>() {}
            ).getBody();
        } catch (RestClientException e) {
            logger.error("Ошибка подключения к сервису ключевых линий ({}): {}", 
                        coreServiceUrl, e.getMessage());
            return new ArrayList<>();
        } catch (Exception e) {
            logger.error("Неожиданная ошибка при получении метрик ключевых линий по области: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }
}
