package ru.georgdeveloper.assistanttelegram.client;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Component
public class CoreServiceClient {
    
    @Autowired
    private RestTemplate restTemplate;
    
    private static final String CORE_SERVICE_URL = "http://localhost:8080/api";
    
    public String analyzeRepairRequest(String request) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("application", "json", StandardCharsets.UTF_8));
        
        // Формируем запрос в формате, ожидаемом новым API v2
        String requestBody = String.format("{\"query\":\"%s\"}", request.replace("\"", "\\\""));
        
        HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);
        
        try {
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
                    if (extracted.startsWith("\"") && extracted.endsWith("\"") && extracted.length() >= 2) {
                        extracted = extracted.substring(1, extracted.length() - 1);
                    }
                    return extracted;
                }
            }
            return response;
            
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            // Обработка HTTP ошибок (404, 500, etc.)
            if (e.getStatusCode().value() == 404) {
                // Fallback на старый API если новый API недоступен
                try {
                    return restTemplate.postForObject(CORE_SERVICE_URL + "/analyze", entity, String.class);
                } catch (Exception fallbackException) {
                    return "❌ Сервис временно недоступен. API эндпоинты не найдены.";
                }
            } else {
                return "❌ Ошибка сервиса: " + e.getStatusCode() + " - " + e.getMessage();
            }
        } catch (org.springframework.web.client.ResourceAccessException e) {
            // Ошибки подключения (сервис недоступен)
            return "❌ Сервис недоступен. Убедитесь, что Core API запущен на http://localhost:8080";
        } catch (Exception e) {
            // Другие ошибки
            return "❌ Произошла ошибка при обработке запроса: " + e.getMessage();
        }
    }
    /**
     * Отправляет пару запрос-ответ на сохранение в векторную базу данных
     * В новой архитектуре v2 обратная связь сохраняется в ChromaDB для семантического поиска
     */
    public void saveFeedback(String request, String response) {
        try {
            // Создаем JSON объект для нового API v2
            Map<String, String> feedbackData = new HashMap<>();
            feedbackData.put("request", request);
            feedbackData.put("response", response);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, String>> entity = new HttpEntity<>(feedbackData, headers);
            
            // Пробуем сначала новый эндпоинт v2
            try {
                ResponseEntity<String> result = restTemplate.postForEntity(
                    CORE_SERVICE_URL + "/api/v2/feedback", entity, String.class);
                System.out.println("Обратная связь успешно сохранена в векторную БД: " + result.getStatusCode());
            } catch (HttpClientErrorException e) {
                if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                    // Fallback на старый эндпоинт для совместимости
                    FeedbackDto dto = new FeedbackDto(request, response);
                    HttpEntity<FeedbackDto> oldEntity = new HttpEntity<>(dto, headers);
                    restTemplate.postForObject(CORE_SERVICE_URL + "/feedback", oldEntity, String.class);
                    System.out.println("Обратная связь сохранена через старый API");
                } else {
                    throw e;
                }
            }
        } catch (Exception e) {
            // В новой системе обратная связь критически важна для обучения
            System.err.println("Ошибка сохранения обратной связи: " + e.getMessage());
            e.printStackTrace();
        }
    }
}