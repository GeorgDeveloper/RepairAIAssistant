package ru.georgdeveloper.assistantweb.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class CoreServiceClient {
    /** HTTP клиент для взаимодействия с ядром (assistant-core) */
    private final RestTemplate restTemplate;

    /**
     * Конструктор клиента для core-сервиса
     * @param restTemplate HTTP клиент Spring
     */
    public CoreServiceClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Value("${core.service.url:http://localhost:8080}")
    private String coreServiceUrl;

    public String analyzeRepairRequest(String request) {
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.setContentType(new org.springframework.http.MediaType("application", "json", java.nio.charset.StandardCharsets.UTF_8));
        org.springframework.http.HttpEntity<String> entity = new org.springframework.http.HttpEntity<>(request, headers);
        return restTemplate.postForObject(coreServiceUrl + "/api/analyze", entity, String.class);
    }

    public String sendFeedback(Object feedback) {
        return restTemplate.postForObject(coreServiceUrl + "/api/feedback", feedback, String.class);
    }

    @SuppressWarnings("unchecked")
    public java.util.List<java.util.Map<String, Object>> getEquipmentMaintenanceRecords(int limit) {
        return restTemplate.getForObject(coreServiceUrl + "/dashboard/equipment-maintenance-records?limit=" + limit, 
                java.util.List.class);
    }
}