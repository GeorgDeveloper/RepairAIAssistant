package ru.georgdeveloper.assistantcore.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.ResourceAccessException;

import java.util.Map;
import java.util.HashMap;

/**
 * Сервис для проверки состояния Ollama
 */
@Service
@Slf4j
public class OllamaHealthService {
    
    private final RestTemplate restTemplate;
    private final String ollamaUrl;
    
    @Value("${ai.ollama.fallback-models:tinyllama:latest,phi3:mini,mistral:latest}")
    private String fallbackModelsStr;
    
    public OllamaHealthService(RestTemplate restTemplate, 
                              @Value("${ai.ollama.url:http://localhost:11434}") String ollamaUrl) {
        this.restTemplate = restTemplate;
        this.ollamaUrl = ollamaUrl;
    }
    
    /**
     * Проверяет доступность Ollama сервера
     */
    public boolean isOllamaAvailable() {
        try {
            String url = ollamaUrl + "/api/tags";
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            log.debug("Ollama доступен, получен ответ: {}", response != null ? "OK" : "NULL");
            return response != null;
        } catch (ResourceAccessException e) {
            log.warn("Ollama недоступен: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("Ошибка проверки Ollama: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Проверяет доступность конкретной модели
     */
    public boolean isModelAvailable(String modelName) {
        try {
            String url = ollamaUrl + "/api/tags";
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            
            if (response != null && response.containsKey("models")) {
                @SuppressWarnings("unchecked")
                java.util.List<Map<String, Object>> models = (java.util.List<Map<String, Object>>) response.get("models");
                
                return models.stream()
                        .anyMatch(model -> modelName.equals(model.get("name")));
            }
            return false;
        } catch (Exception e) {
            log.error("Ошибка проверки модели {}: {}", modelName, e.getMessage());
            return false;
        }
    }
    
    /**
     * Получает статус Ollama с подробной информацией
     */
    public String getOllamaStatus() {
        if (!isOllamaAvailable()) {
            return "❌ Ollama сервер недоступен";
        }
        
        try {
            String url = ollamaUrl + "/api/tags";
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            
            if (response != null && response.containsKey("models")) {
                @SuppressWarnings("unchecked")
                java.util.List<Map<String, Object>> models = (java.util.List<Map<String, Object>>) response.get("models");
                
                StringBuilder status = new StringBuilder("✅ Ollama сервер доступен\n");
                status.append("📋 Доступные модели:\n");
                
                for (Map<String, Object> model : models) {
                    String name = (String) model.get("name");
                    String size = model.get("size") != null ? model.get("size").toString() : "неизвестно";
                    status.append("  - ").append(name).append(" (размер: ").append(size).append(")\n");
                }
                
                return status.toString();
            }
            
            return "✅ Ollama сервер доступен, но модели не найдены";
        } catch (Exception e) {
            log.error("Ошибка получения статуса Ollama: {}", e.getMessage());
            return "⚠️ Ollama сервер доступен, но произошла ошибка при получении информации о моделях";
        }
    }
    
    /**
     * Тестирует доступность модели и получает рабочую модель
     */
    public String getWorkingChatModel() {
        // Получаем список доступных моделей
        String[] fallbackModels = fallbackModelsStr.split(",");
        
        for (String modelName : fallbackModels) {
            modelName = modelName.trim();
            
            if (isModelAvailable(modelName)) {
                log.info("Пробуем модель: {}", modelName);
                
                if (testModelGeneration(modelName)) {
                    log.info("Модель {} работает корректно", modelName);
                    return modelName;
                } else {
                    log.warn("Модель {} недоступна из-за нехватки памяти", modelName);
                }
            }
        }
        
        log.error("Ни одна модель не доступна");
        return null;
    }
    
    /**
     * Тестирует генерацию ответа для модели
     */
    public boolean testModelGeneration(String modelName) {
        try {
            String testPrompt = "{\"model\":\"" + modelName + 
                              "\",\"prompt\":\"Привет\",\"stream\":false}";
            
            log.debug("Тестируем модель {} с запросом: {}", modelName, testPrompt);
            
            RestTemplate testTemplate = new RestTemplate();
            String response = testTemplate.postForObject(
                ollamaUrl + "/api/generate", 
                testPrompt, 
                String.class
            );
            
            return response != null && !response.isEmpty();
            
        } catch (Exception e) {
            log.debug("Ошибка тестирования модели {}: {}", modelName, e.getMessage());
            return false;
        }
    }
    
    /**
     * Получает рекомендацию модели на основе доступной памяти
     */
    public Map<String, Object> getModelRecommendation() {
        Map<String, Object> recommendation = new HashMap<>();
        
        try {
            String workingModel = getWorkingChatModel();
            
            if (workingModel != null) {
                recommendation.put("recommendedModel", workingModel);
                recommendation.put("status", "working");
                recommendation.put("message", "Найдена работающая модель: " + workingModel);
            } else {
                recommendation.put("recommendedModel", null);
                recommendation.put("status", "error");
                recommendation.put("message", "Не удалось найти доступную модель. Проверьте память системы.");
            }
            
        } catch (Exception e) {
            recommendation.put("recommendedModel", null);
            recommendation.put("status", "error");
            recommendation.put("message", "Ошибка рекомендации модели: " + e.getMessage());
        }
        
        return recommendation;
    }
}
