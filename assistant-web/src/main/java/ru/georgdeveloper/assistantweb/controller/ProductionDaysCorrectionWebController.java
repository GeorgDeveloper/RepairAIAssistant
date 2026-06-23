package ru.georgdeveloper.assistantweb.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * Прокси к API корректировки производственных дней (core).
 */
@RestController
@RequestMapping("/api/production-days-correction")
public class ProductionDaysCorrectionWebController {

    @Value("${core.service.url:http://localhost:8080}")
    private String coreServiceUrl;

    private final RestTemplate restTemplate;

    public ProductionDaysCorrectionWebController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    private String baseUrl() {
        return coreServiceUrl + "/api/production-days-correction";
    }

    @GetMapping
    public List<Map<String, Object>> list() {
        return restTemplate.exchange(baseUrl(), HttpMethod.GET, null,
                new ParameterizedTypeReference<List<Map<String, Object>>>() {}).getBody();
    }

    @GetMapping("/{year}/{month}")
    public ResponseEntity<Map<String, Object>> get(@PathVariable int year, @PathVariable int month) {
        try {
            ResponseEntity<Map<String, Object>> resp = restTemplate.exchange(baseUrl() + "/" + year + "/" + month,
                    HttpMethod.GET, null, new ParameterizedTypeReference<Map<String, Object>>() {});
            return resp.getBody() != null ? ResponseEntity.ok(resp.getBody()) : ResponseEntity.notFound().build();
        } catch (org.springframework.web.client.HttpClientErrorException.NotFound e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> save(@RequestBody Map<String, Object> body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<Map<String, Object>> resp = restTemplate.exchange(baseUrl(), HttpMethod.POST,
                new HttpEntity<>(body, headers), new ParameterizedTypeReference<Map<String, Object>>() {});
        return new ResponseEntity<>(resp.getBody(), resp.getStatusCode());
    }

    @DeleteMapping("/{year}/{month}")
    public ResponseEntity<Void> delete(@PathVariable int year, @PathVariable int month) {
        try {
            restTemplate.delete(baseUrl() + "/" + year + "/" + month);
            return ResponseEntity.noContent().build();
        } catch (org.springframework.web.client.HttpClientErrorException.NotFound e) {
            return ResponseEntity.notFound().build();
        }
    }
}
