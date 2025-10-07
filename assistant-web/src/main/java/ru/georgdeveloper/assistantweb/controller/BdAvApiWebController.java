package ru.georgdeveloper.assistantweb.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/bdav")
public class BdAvApiWebController {

    @Value("${core.service.url:http://localhost:8080}")
    private String coreServiceUrl;

    private final RestTemplate restTemplate;

    public BdAvApiWebController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @SuppressWarnings("unchecked")
    @GetMapping("/years")
    public List<Map<String, Object>> years() {
        return (List<Map<String, Object>>) (List<?>) restTemplate.getForObject(coreServiceUrl + "/bdav/years", List.class);
    }

    @SuppressWarnings("unchecked")
    @GetMapping("/months")
    public List<Map<String, Object>> months(@RequestParam(required = false) List<String> year) {
        if (year == null || year.isEmpty()) {
            return (List<Map<String, Object>>) (List<?>) restTemplate.getForObject(coreServiceUrl + "/bdav/months", List.class);
        }
        String yearParam = String.join(",", year);
        return (List<Map<String, Object>>) (List<?>) restTemplate.getForObject(coreServiceUrl + "/bdav/months?year=" + yearParam, List.class);
    }

    @SuppressWarnings("unchecked")
    @GetMapping("/weeks")
    public List<Map<String, Object>> weeks(@RequestParam(required = false) List<String> year, @RequestParam(required = false) List<String> month) {
        if (year == null || year.isEmpty() || month == null || month.isEmpty()) {
            return (List<Map<String, Object>>) (List<?>) restTemplate.getForObject(coreServiceUrl + "/bdav/weeks", List.class);
        }
        String yearParam = String.join(",", year);
        String monthParam = String.join(",", month);
        return (List<Map<String, Object>>) (List<?>) restTemplate.getForObject(coreServiceUrl + 
                "/bdav/weeks?year=" + yearParam + "&month=" + monthParam, List.class);
    }

    @SuppressWarnings("unchecked")
    @GetMapping("/data")
    public List<Map<String, Object>> data(@RequestParam(required = false) List<String> year,
                                          @RequestParam(required = false) List<String> month,
                                          @RequestParam(required = false) List<String> week,
                                          @RequestParam(required = false) List<String> area,
                                          @RequestParam String metric) {
        StringBuilder url = new StringBuilder(coreServiceUrl + "/bdav/data?");
        if (year != null && !year.isEmpty()) {
            year.forEach(y -> url.append("year=").append(y).append('&'));
        }
        if (month != null && !month.isEmpty()) {
            month.forEach(m -> url.append("month=").append(m).append('&'));
        }
        if (week != null && !week.isEmpty()) {
            week.forEach(w -> url.append("week=").append(w).append('&'));
        }
        if (area != null && !area.isEmpty()) {
            area.forEach(a -> url.append("area=").append(a).append('&'));
        } else {
            // Если area не указан, используем "all" по умолчанию
            url.append("area=all&");
        }
        url.append("metric=").append(metric).append('&');
        return (List<Map<String, Object>>) (List<?>) restTemplate.getForObject(url.toString(), List.class);
    }
}


