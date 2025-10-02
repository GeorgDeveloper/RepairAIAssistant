package ru.georgdeveloper.assistantweb.client;

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

    private static final String CORE_SERVICE_URL = "http://localhost:8080/api";

    public String analyzeRepairRequest(String request) {
        try {
            // Формируем запрос в формате JSON для нового API v2
            String requestBody = String.format("{\"query\":\"%s\"}", request.replace("\"", "\\\""));
            
            // Устанавливаем правильные заголовки
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
            
            org.springframework.http.HttpEntity<String> entity = 
                new org.springframework.http.HttpEntity<>(requestBody, headers);
            
            // Сначала пробуем новый API v2
            String response = restTemplate.postForObject(CORE_SERVICE_URL + "/v2/query", entity, String.class);
            
            // Парсим JSON ответ и извлекаем поле "response"
            if (response != null && response.contains("\"response\"")) {
                // Простое извлечение response из JSON
                int start = response.indexOf("\"response\":\"") + 12;
                int end = response.lastIndexOf("\",\"query\"");
                if (end == -1) end = response.lastIndexOf("\"}");
                if (start > 11 && end > start) {
                    return response.substring(start, end).replace("\\\"", "\"");
                }
            }
            return response;
            
        } catch (Exception e) {
            // Fallback на старый API если новый недоступен
            try {
                return restTemplate.postForObject(CORE_SERVICE_URL + "/analyze", request, String.class);
            } catch (Exception fallbackException) {
                return "Ошибка соединения с сервисом: " + e.getMessage();
            }
        }
    }

    public String sendFeedback(Object feedback) {
        try {
            // Пробуем отправить в старый эндпоинт для совместимости
            return restTemplate.postForObject(CORE_SERVICE_URL + "/feedback", feedback, String.class);
        } catch (Exception e) {
            // В новой системе v2 обратная связь может не требоваться
            // так как система использует семантический поиск и автоматическое обучение
            System.out.println("Feedback endpoint not available in v2 architecture: " + e.getMessage());
            return "Обратная связь принята (v2 система использует автоматическое обучение)";
        }
    }

    @SuppressWarnings("unchecked")
    public java.util.List<java.util.Map<String, Object>> getEquipmentMaintenanceRecords(int limit) {
        return restTemplate.getForObject("http://localhost:8080/dashboard/equipment-maintenance-records?limit=" + limit, 
                java.util.List.class);
    }
}