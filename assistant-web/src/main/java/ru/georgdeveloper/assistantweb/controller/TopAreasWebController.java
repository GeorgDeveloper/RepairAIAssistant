package ru.georgdeveloper.assistantweb.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/top-areas")
public class TopAreasWebController {

    @Value("${core.service.url:http://localhost:8080}")
    private String coreServiceUrl;

    private final RestTemplate restTemplate;

    /**
     * Конструктор контроллера для проксирования запросов к core-сервису
     */
    public TopAreasWebController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @SuppressWarnings("unchecked")
    @GetMapping("/data")
    public List<Map<String, Object>> data(@RequestParam(required = false) String dateFrom,
                                          @RequestParam(required = false) String dateTo,
                                          @RequestParam(required = false) String week,
                                          @RequestParam(required = false) String failureType,
                                          @RequestParam(required = false) Integer limit) {
        StringBuilder url = new StringBuilder(coreServiceUrl + "/top-areas/data?");
        if (dateFrom != null) url.append("dateFrom=").append(dateFrom).append('&');
        if (dateTo != null) url.append("dateTo=").append(dateTo).append('&');
        if (week != null) url.append("week=").append(week).append('&');
        if (failureType != null) url.append("failureType=").append(failureType).append('&');
        if (limit != null) url.append("limit=").append(limit).append('&');
        return (List<Map<String, Object>>) (List<?>) restTemplate.getForObject(url.toString(), List.class);
    }

    @SuppressWarnings("unchecked")
    @GetMapping("/weeks")
    public List<Map<String, Object>> weeks() {
        return (List<Map<String, Object>>) (List<?>) restTemplate.getForObject(coreServiceUrl + "/top-areas/weeks", List.class);
    }

    @SuppressWarnings("unchecked")
    @GetMapping("/failure-types")
    public List<Map<String, Object>> failureTypes() {
        return (List<Map<String, Object>>) (List<?>) restTemplate.getForObject(coreServiceUrl + "/top-areas/failure-types", List.class);
    }

    @SuppressWarnings("unchecked")
    @GetMapping("/drilldown/categories")
    public List<Map<String, Object>> categories(@RequestParam String area,
                                                @RequestParam(required = false) String dateFrom,
                                                @RequestParam(required = false) String dateTo,
                                                @RequestParam(required = false) String week,
                                                @RequestParam(required = false) String failureType) {
        StringBuilder url = new StringBuilder(coreServiceUrl + "/top-areas/drilldown/categories?area=" + area);
        if (dateFrom != null) url.append("&dateFrom=").append(dateFrom);
        if (dateTo != null) url.append("&dateTo=").append(dateTo);
        if (week != null) url.append("&week=").append(week);
        if (failureType != null) url.append("&failureType=").append(failureType);
        return (List<Map<String, Object>>) (List<?>) restTemplate.getForObject(url.toString(), List.class);
    }

    @SuppressWarnings("unchecked")
    @GetMapping("/drilldown/causes")
    public List<Map<String, Object>> causes(@RequestParam String area,
                                            @RequestParam String category,
                                            @RequestParam(required = false) String dateFrom,
                                            @RequestParam(required = false) String dateTo,
                                            @RequestParam(required = false) String week,
                                            @RequestParam(required = false) String failureType) {
        StringBuilder url = new StringBuilder(coreServiceUrl + "/top-areas/drilldown/causes?area=" + area + "&category=" + category);
        if (dateFrom != null) url.append("&dateFrom=").append(dateFrom);
        if (dateTo != null) url.append("&dateTo=").append(dateTo);
        if (week != null) url.append("&week=").append(week);
        if (failureType != null) url.append("&failureType=").append(failureType);
        return (List<Map<String, Object>>) (List<?>) restTemplate.getForObject(url.toString(), List.class);
    }

    @SuppressWarnings("unchecked")
    @GetMapping("/drilldown/events")
    public List<Map<String, Object>> events(@RequestParam String area,
                                            @RequestParam String category,
                                            @RequestParam String cause,
                                            @RequestParam(required = false) String dateFrom,
                                            @RequestParam(required = false) String dateTo,
                                            @RequestParam(required = false) String week,
                                            @RequestParam(required = false) String failureType) {
        StringBuilder url = new StringBuilder(coreServiceUrl + "/top-areas/drilldown/events?area=" + area + "&category=" + category + "&cause=" + cause);
        if (dateFrom != null) url.append("&dateFrom=").append(dateFrom);
        if (dateTo != null) url.append("&dateTo=").append(dateTo);
        if (week != null) url.append("&week=").append(week);
        if (failureType != null) url.append("&failureType=").append(failureType);
        return (List<Map<String, Object>>) (List<?>) restTemplate.getForObject(url.toString(), List.class);
    }
}
