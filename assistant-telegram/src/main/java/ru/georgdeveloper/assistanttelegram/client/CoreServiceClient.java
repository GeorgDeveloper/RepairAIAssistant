package ru.georgdeveloper.assistanttelegram.client;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import java.nio.charset.StandardCharsets;

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
                    return response.substring(start, end).replace("\\\"", "\"");
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
     * Отправляет пару запрос-ответ на сохранение для дообучения модели
     * В новой архитектуре v2 обратная связь обрабатывается автоматически через векторную БД
     */
    public void saveFeedback(String request, String response) {
        try {
            FeedbackDto dto = new FeedbackDto(request, response);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<FeedbackDto> entity = new HttpEntity<>(dto, headers);
            
            // Пробуем отправить в старый эндпоинт для совместимости
            restTemplate.postForObject(CORE_SERVICE_URL + "/feedback", entity, String.class);
        } catch (Exception e) {
            // В новой системе обратная связь может не требоваться
            // так как система использует семантический поиск
            System.out.println("Feedback endpoint not available in v2 architecture: " + e.getMessage());
        }
    }
}