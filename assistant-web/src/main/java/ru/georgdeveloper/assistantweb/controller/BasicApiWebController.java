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
@RequestMapping("/api")
public class BasicApiWebController {

    @Value("${core.service.url:http://localhost:8080}")
    private String coreServiceUrl;

    private final RestTemplate restTemplate;

    public BasicApiWebController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @SuppressWarnings("unchecked")
    @GetMapping("/regions")
    public List<Map<String, Object>> regions() {
        return (List<Map<String, Object>>) (List<?>) restTemplate.getForObject(coreServiceUrl + "/api/regions", List.class);
    }

    @SuppressWarnings("unchecked")
    @GetMapping("/equipment")
    public List<Map<String, Object>> equipment(@RequestParam String regionId) {
        return (List<Map<String, Object>>) (List<?>) restTemplate.getForObject(coreServiceUrl + "/api/equipment?regionId=" + regionId, List.class);
    }

    @SuppressWarnings("unchecked")
    @GetMapping("/nodes")
    public List<Map<String, Object>> nodes(@RequestParam String equipmentId) {
        return (List<Map<String, Object>>) (List<?>) restTemplate.getForObject(coreServiceUrl + "/api/nodes?equipmentId=" + equipmentId, List.class);
    }
}


