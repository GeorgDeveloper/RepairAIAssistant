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

    @SuppressWarnings("unchecked")
    public java.util.List<java.util.Map<String, Object>> getEquipmentMaintenanceRecords(String dateFrom, String dateTo, String area) {
        StringBuilder url = new StringBuilder(coreServiceUrl + "/dashboard/equipment-maintenance-records");
        boolean firstParam = true;
        
        if (dateFrom != null && !dateFrom.isEmpty()) {
            url.append(firstParam ? "?" : "&").append("dateFrom=").append(java.net.URLEncoder.encode(dateFrom, java.nio.charset.StandardCharsets.UTF_8));
            firstParam = false;
        }
        if (dateTo != null && !dateTo.isEmpty()) {
            url.append(firstParam ? "?" : "&").append("dateTo=").append(java.net.URLEncoder.encode(dateTo, java.nio.charset.StandardCharsets.UTF_8));
            firstParam = false;
        }
        if (area != null && !area.isEmpty() && !area.equals("all")) {
            url.append(firstParam ? "?" : "&").append("area=").append(java.net.URLEncoder.encode(area, java.nio.charset.StandardCharsets.UTF_8));
        }
        
        return restTemplate.getForObject(url.toString(), java.util.List.class);
    }

    @SuppressWarnings("unchecked")
    public java.util.List<java.util.Map<String, Object>> getPmMaintenanceRecords(int limit) {
        return restTemplate.getForObject(coreServiceUrl + "/dashboard/pm-maintenance-records?limit=" + limit, 
                java.util.List.class);
    }

    @SuppressWarnings("unchecked")
    public java.util.List<java.util.Map<String, Object>> getDiagnosticsReports(int limit) {
        return restTemplate.getForObject(coreServiceUrl + "/dashboard/diagnostics-reports?limit=" + limit, 
                java.util.List.class);
    }
}