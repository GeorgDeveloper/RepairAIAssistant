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
@RequestMapping("/final")
public class FinalWebController {

    @Value("${core.service.url:http://localhost:8080}")
    private String coreServiceUrl;

    private final RestTemplate restTemplate;

    public FinalWebController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @SuppressWarnings("unchecked")
    @GetMapping("/data")
    public List<Map<String, Object>> data(@RequestParam(required = false) List<Integer> year,
                                          @RequestParam(required = false) List<Integer> month,
                                          @RequestParam(required = false, defaultValue = "${app.final.months-limit:12}") Integer limit) {
        StringBuilder url = new StringBuilder(coreServiceUrl + "/final/data?");
        if (year != null && !year.isEmpty()) url.append("year=").append(join(year)).append('&');
        if (month != null && !month.isEmpty()) url.append("month=").append(join(month)).append('&');
        if (limit != null) url.append("limit=").append(limit);
        return (List<Map<String, Object>>) (List<?>) restTemplate.getForObject(url.toString(), List.class);
    }

    @SuppressWarnings("unchecked")
    @GetMapping("/years")
    public List<Map<String, Object>> years() {
        return (List<Map<String, Object>>) (List<?>) restTemplate.getForObject(coreServiceUrl + "/final/years", List.class);
    }

    @SuppressWarnings("unchecked")
    @GetMapping("/months")
    public List<Map<String, Object>> months(@RequestParam(required = false) List<Integer> year) {
        StringBuilder url = new StringBuilder(coreServiceUrl + "/final/months");
        if (year != null && !year.isEmpty()) {
            url.append("?year=").append(join(year));
        }
        return (List<Map<String, Object>>) (List<?>) restTemplate.getForObject(url.toString(), List.class);
    }

    @SuppressWarnings("unchecked")
    @GetMapping("/config")
    public Map<String, Object> config() {
        return (Map<String, Object>) restTemplate.getForObject(coreServiceUrl + "/final/config", Map.class);
    }

    private String join(List<Integer> list) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(',');
            sb.append(list.get(i));
        }
        return sb.toString();
    }
}


