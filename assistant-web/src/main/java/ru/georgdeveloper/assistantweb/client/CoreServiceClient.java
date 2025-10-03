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
            
            // Устанавливаем правильные заголовки (UTF-8 как в Telegram)
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(new org.springframework.http.MediaType("application", "json", java.nio.charset.StandardCharsets.UTF_8));
            headers.setAccept(java.util.List.of(org.springframework.http.MediaType.APPLICATION_JSON));
            
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
                    String extracted = response.substring(start, end)
                            .replace("\\\"", "\"")
                            .replace("\\n", "\n")
                            .replace("\\r", "\r")
                            .replace("\\t", "\t");
                    // Убираем случайные обрамляющие кавычки
                    if (extracted.startsWith("\"") && extracted.endsWith("\"") && extracted.length() >= 2) {
                        extracted = extracted.substring(1, extracted.length() - 1);
                    }
                    return extracted;
                }
            }
            return response;
            
        } catch (Exception e) {
            // Fallback на старый API если новый недоступен
            try {
                org.springframework.http.HttpHeaders h2 = new org.springframework.http.HttpHeaders();
                h2.setContentType(new org.springframework.http.MediaType("text", "plain", java.nio.charset.StandardCharsets.UTF_8));
                org.springframework.http.HttpEntity<String> e2 = new org.springframework.http.HttpEntity<>(request, h2);
                return restTemplate.postForObject(CORE_SERVICE_URL + "/analyze", e2, String.class);
            } catch (Exception fallbackException) {
                return "Ошибка соединения с сервисом: " + e.getMessage();
            }
        }
    }

    public String sendFeedback(Object feedback) {
        try {
            // Устанавливаем правильные заголовки
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
            
            org.springframework.http.HttpEntity<Object> entity = 
                new org.springframework.http.HttpEntity<>(feedback, headers);
            
            // Сначала пробуем новый API v2 для сохранения в векторную БД
            try {
                org.springframework.http.ResponseEntity<String> result = restTemplate.postForEntity(
                    CORE_SERVICE_URL + "/v2/feedback", entity, String.class);
                System.out.println("Обратная связь успешно сохранена в векторную БД: " + result.getStatusCode());
                return "Обратная связь сохранена в векторную базу данных для улучшения ответов";
            } catch (org.springframework.web.client.HttpClientErrorException e) {
                if (e.getStatusCode() == org.springframework.http.HttpStatus.NOT_FOUND) {
                    // Fallback на старый эндпоинт для совместимости
                    restTemplate.postForObject(CORE_SERVICE_URL + "/feedback", feedback, String.class);
                    return "Обратная связь сохранена через старый API";
                } else {
                    throw e;
                }
            }
        } catch (Exception e) {
            // В новой системе обратная связь критически важна для обучения
            System.err.println("Ошибка сохранения обратной связи: " + e.getMessage());
            return "Ошибка сохранения обратной связи: " + e.getMessage();
        }
    }

    @SuppressWarnings("unchecked")
    public java.util.List<java.util.Map<String, Object>> getEquipmentMaintenanceRecords(int limit) {
        return restTemplate.getForObject("http://localhost:8080/dashboard/equipment-maintenance-records?limit=" + limit, 
                java.util.List.class);
    }
}