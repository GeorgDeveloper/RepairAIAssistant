package ru.georgdeveloper.assistantcore;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Тест для проверки подключения к Ollama
 */
public class OllamaConnectionTest {
    
    private static final Logger logger = LoggerFactory.getLogger(OllamaConnectionTest.class);
    private final RestTemplate restTemplate = new RestTemplate();
    
    @Test
    public void testOllamaConnection() {
        String baseUrl = "http://localhost:11434";
        
        try {
            // Проверка доступности сервера
            logger.info("Проверка доступности Ollama на {}", baseUrl);
            String healthResponse = restTemplate.getForObject(baseUrl, String.class);
            logger.info("Ollama доступен: {}", healthResponse != null ? "ДА" : "НЕТ");
            
        } catch (Exception e) {
            logger.error("Ollama недоступен: {}", e.getMessage());
        }
        
        try {
            // Проверка API /api/tags
            logger.info("Проверка /api/tags");
            String tagsUrl = baseUrl + "/api/tags";
            String tagsResponse = restTemplate.getForObject(tagsUrl, String.class);
            logger.info("Ответ /api/tags: {}", tagsResponse);
            
        } catch (Exception e) {
            logger.error("Ошибка /api/tags: {}", e.getMessage());
        }
        
        try {
            // Проверка API /api/version
            logger.info("Проверка /api/version");
            String versionUrl = baseUrl + "/api/version";
            String versionResponse = restTemplate.getForObject(versionUrl, String.class);
            logger.info("Версия Ollama: {}", versionResponse);
            
        } catch (Exception e) {
            logger.error("Ошибка /api/version: {}", e.getMessage());
        }
        
        try {
            // Тест API /api/generate
            logger.info("Тестирование /api/generate");
            String generateUrl = baseUrl + "/api/generate";
            
            // Формируем тестовый запрос
            String requestBody = "{"
                + "\"model\": \"mistral:latest\","
                + "\"prompt\": \"Привет! Как дела?\","
                + "\"stream\": false,"
                + "\"options\": {\"temperature\": 0.7}"
                + "}";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);
            
            logger.info("Отправка запроса: {}", requestBody);
            String generateResponse = restTemplate.postForObject(generateUrl, entity, String.class);
            logger.info("Ответ /api/generate: {}", generateResponse);
            
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            logger.error("HTTP ошибка /api/generate: {} - {}", e.getStatusCode(), e.getMessage());
            if (e.getStatusCode().value() == 404) {
                logger.error("Ошибка 404: API эндпоинт не найден. Проверьте версию Ollama.");
            }
        } catch (Exception e) {
            logger.error("Ошибка /api/generate: {}", e.getMessage());
        }
    }
    
    @Test
    public void testOllamaWithDifferentModels() {
        String baseUrl = "http://localhost:11434";
        String[] modelsToTest = {"mistral:latest", "deepseek-r1:latest", "llama2:latest"};
        
        for (String model : modelsToTest) {
            try {
                logger.info("Тестирование модели: {}", model);
                String generateUrl = baseUrl + "/api/generate";
                
                String requestBody = "{"
                    + "\"model\": \"" + model + "\","
                    + "\"prompt\": \"Тест\","
                    + "\"stream\": false"
                    + "}";
                
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);
                
                String response = restTemplate.postForObject(generateUrl, entity, String.class);
                logger.info("Модель {} работает: {}", model, response != null && response.contains("response"));
                
            } catch (Exception e) {
                logger.warn("Модель {} недоступна: {}", model, e.getMessage());
            }
        }
    }
}
