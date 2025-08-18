package ru.georgdeveloper.assistantcore.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.cache.annotation.Cacheable;
import ru.georgdeveloper.assistantcore.client.OllamaClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class OllamaService {
    
    private static final Logger logger = LoggerFactory.getLogger(OllamaService.class);
    
    @Autowired
    private OllamaClient ollamaClient;
    
    @Value("${ai.ollama.temperature:0.7}")
    private double temperature;
    
    @Cacheable(value = "ollama-responses", condition = "#prompt.length() < 500")
    public String generateResponse(String prompt) {
        try {
            logger.debug("Генерация ответа для промпта длиной: {} символов", prompt.length());
            String response = ollamaClient.generateResponse(prompt);
            logger.debug("Получен ответ длиной: {} символов", response != null ? response.length() : 0);
            return response;
        } catch (Exception e) {
            logger.error("Ошибка генерации ответа: {}", e.getMessage());
            return "Извините, произошла ошибка при обработке запроса.";
        }
    }
    
    public String generateResponseWithContext(String prompt, String context) {
        String fullPrompt = context + "\n\nЗапрос: " + prompt + "\nОтвет:";
        return generateResponse(fullPrompt);
    }
}