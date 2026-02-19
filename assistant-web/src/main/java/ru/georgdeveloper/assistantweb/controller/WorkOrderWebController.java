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
 * Контроллер для проксирования запросов к API нарядов на работы
 */
@RestController
@RequestMapping("/api/work-orders")
public class WorkOrderWebController {

    private static final Logger logger = LoggerFactory.getLogger(WorkOrderWebController.class);

    @Value("${base-update.service.url:http://localhost:8084}")
    private String baseUpdateServiceUrl;

    @Value("${core.service.url:http://localhost:8080}")
    private String coreServiceUrl;

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
        try {
            logger.debug("Запрос нарядов на работы к сервису: {}", baseUpdateServiceUrl + "/api/work-orders/dashboard");
            return restTemplate.exchange(
                baseUpdateServiceUrl + "/api/work-orders/dashboard",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<Map<String, Object>>>() {}
            ).getBody();
        } catch (RestClientException e) {
            logger.error("Ошибка подключения к сервису нарядов на работы ({}): {}", 
                        baseUpdateServiceUrl, e.getMessage());
            // Возвращаем пустой список вместо ошибки
            return new ArrayList<>();
        } catch (Exception e) {
            logger.error("Неожиданная ошибка при получении нарядов на работы: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * Получение активных нарядов (не закрытых)
     */
    @GetMapping("/active")
    @ResponseBody
    public List<Map<String, Object>> getActiveWorkOrders() {
        try {
            logger.debug("Запрос активных нарядов к сервису: {}", baseUpdateServiceUrl + "/api/work-orders/active");
            return restTemplate.exchange(
                baseUpdateServiceUrl + "/api/work-orders/active",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<Map<String, Object>>>() {}
            ).getBody();
        } catch (RestClientException e) {
            logger.error("Ошибка подключения к сервису нарядов на работы ({}): {}", 
                        baseUpdateServiceUrl, e.getMessage());
            return new ArrayList<>();
        } catch (Exception e) {
            logger.error("Неожиданная ошибка при получении активных нарядов: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * Получение детализации нарядов для конкретной даты и области
     */
    @GetMapping("/breakdown-details")
    @ResponseBody
    public List<Map<String, Object>> getBreakdownDetails(@RequestParam("date") String date,
                                                         @RequestParam("area") String area) {
        try {
            logger.debug("Запрос детализации нарядов к core сервису для даты: {}, области: {}", date, area);
            return restTemplate.exchange(
                coreServiceUrl + "/top-equipment/breakdown-details?date=" + date + "&area=" + area,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<Map<String, Object>>>() {}
            ).getBody();
        } catch (RestClientException e) {
            logger.error("Ошибка подключения к core сервису ({}): {}", 
                        coreServiceUrl, e.getMessage());
            return new ArrayList<>();
        } catch (Exception e) {
            logger.error("Неожиданная ошибка при получении детализации нарядов: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * Поиск нарядов по ключевому слову
     */
    @GetMapping("/search")
    @ResponseBody
    public List<Map<String, Object>> searchWorkOrders(@RequestParam("keyword") String keyword) {
        try {
            logger.debug("Поиск нарядов по ключевому слову '{}' к сервису: {}", keyword, baseUpdateServiceUrl + "/api/work-orders/search");
            return restTemplate.exchange(
                baseUpdateServiceUrl + "/api/work-orders/search?keyword=" + keyword,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<Map<String, Object>>>() {}
            ).getBody();
        } catch (RestClientException e) {
            logger.error("Ошибка подключения к сервису нарядов на работы ({}): {}", 
                        baseUpdateServiceUrl, e.getMessage());
            return new ArrayList<>();
        } catch (Exception e) {
            logger.error("Неожиданная ошибка при поиске нарядов: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * Получение последних 15 нарядов (совместимость с существующим API)
     */
    @GetMapping("/last-15")
    @ResponseBody
    public List<Map<String, Object>> getLast15WorkOrders() {
        try {
            logger.debug("Запрос последних 15 нарядов к сервису: {}", baseUpdateServiceUrl + "/api/work-orders/last-15");
            return restTemplate.exchange(
                baseUpdateServiceUrl + "/api/work-orders/last-15",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<Map<String, Object>>>() {}
            ).getBody();
        } catch (RestClientException e) {
            logger.error("Ошибка подключения к сервису нарядов на работы ({}): {}", 
                        baseUpdateServiceUrl, e.getMessage());
            return new ArrayList<>();
        } catch (Exception e) {
            logger.error("Неожиданная ошибка при получении последних 15 нарядов: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }
}
