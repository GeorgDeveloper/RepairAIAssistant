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
        
        HttpEntity<String> entity = new HttpEntity<>(request, headers);
        return restTemplate.postForObject(CORE_SERVICE_URL + "/analyze", entity, String.class);
    }
    /**
     * Отправляет пару запрос-ответ на сохранение для дообучения модели
     */
    public void saveFeedback(String request, String response) {
        FeedbackDto dto = new FeedbackDto(request, response);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<FeedbackDto> entity = new HttpEntity<>(dto, headers);
        restTemplate.postForObject(CORE_SERVICE_URL + "/feedback", entity, String.class);
    }
}