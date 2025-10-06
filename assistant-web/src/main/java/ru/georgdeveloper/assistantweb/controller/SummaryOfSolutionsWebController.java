package ru.georgdeveloper.assistantweb.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/summary-of-solutions")
public class SummaryOfSolutionsWebController {

    @Value("${core.service.url:http://localhost:8080}")
    private String coreServiceUrl;

    private final RestTemplate restTemplate;

    public SummaryOfSolutionsWebController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @SuppressWarnings("unchecked")
    @GetMapping
    public List<Map<String, Object>> list() {
        return (List<Map<String, Object>>) (List<?>) restTemplate.getForObject(coreServiceUrl + "/api/summary-of-solutions", List.class);
    }

    @PostMapping
    public String create(@RequestBody Map<String, Object> body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        return restTemplate.postForObject(coreServiceUrl + "/api/summary-of-solutions", entity, String.class);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable("id") String id) {
        restTemplate.delete(coreServiceUrl + "/api/summary-of-solutions/" + id);
    }

    @SuppressWarnings("unchecked")
    @GetMapping("/{id}")
    public Map<String, Object> get(@PathVariable("id") String id) {
        return (Map<String, Object>) restTemplate.getForObject(coreServiceUrl + "/api/summary-of-solutions/" + id, Map.class);
    }

    @PutMapping("/{id}")
    public void update(@PathVariable("id") String id, @RequestBody Map<String, Object> body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        restTemplate.exchange(coreServiceUrl + "/api/summary-of-solutions/" + id, HttpMethod.PUT, entity, Void.class);
    }
}


