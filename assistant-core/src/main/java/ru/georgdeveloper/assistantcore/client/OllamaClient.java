package ru.georgdeveloper.assistantcore.client;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import ru.georgdeveloper.assistantcore.config.AssistantProperties;

@Component
public class OllamaClient {
    
    @Autowired
    private RestTemplate restTemplate;
    
    @Autowired
    @Qualifier("assistantProperties")
    private AssistantProperties properties;
    
    public String generateResponse(String prompt) {
        try {
            String url = properties.getOllama().getUrl() + "/generate";
            String model = properties.getOllama().getModel();
            
            // Escape JSON properly
            String escapedPrompt = prompt.replace("\\", "\\\\")
                                        .replace("\"", "\\\"")
                                        .replace("\n", "\\n")
                                        .replace("\r", "\\r")
                                        .replace("\t", "\\t");
            
            // Create proper JSON request
            String requestBody = "{" +
                "\"model\": \"" + model + "\"," +
                "\"prompt\": \"" + escapedPrompt + "\"," +
                "\"stream\": false," +
                "\"options\": {\"temperature\": 0.7}" +
                "}";
            
            System.out.println("Ollama request: " + requestBody);
            
            // Set headers
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
            headers.set("Accept", "application/json");
            
            org.springframework.http.HttpEntity<String> entity = new org.springframework.http.HttpEntity<>(requestBody, headers);
            
            // Make API call
            String response = restTemplate.postForObject(url, entity, String.class);
            System.out.println("Ollama response: " + response);
            
            // Parse JSON response
            if (response != null && response.contains("\"response\":")) {
                // Find the response field
                int responseStart = response.indexOf("\"response\":");
                if (responseStart != -1) {
                    responseStart += 11; // Skip '"response":'
                    // Skip whitespace and opening quote
                    while (responseStart < response.length() && 
                           (response.charAt(responseStart) == ' ' || response.charAt(responseStart) == '"')) {
                        responseStart++;
                    }
                    
                    // Find closing quote before next field or end
                    int responseEnd = response.indexOf("\",\"", responseStart);
                    if (responseEnd == -1) {
                        responseEnd = response.lastIndexOf('"');
                    }
                    
                    if (responseEnd > responseStart) {
                        String result = response.substring(responseStart, responseEnd)
                                              .replace("\\\"", "\"")
                                              .replace("\\n", "\n")
                                              .replace("\\r", "\r")
                                              .replace("\\t", "\t")
                                              .replace("\\\\", "\\");
                        
                        // Remove thinking tags from deepseek-r1
                        result = result.replaceAll("<think>.*?</think>", "")
                                      .replaceAll("u003cthink\\u003e.*?u003c/think\\u003e", "")
                                      .replaceAll("\\u003cthink\\u003e.*?\\u003c/think\\u003e", "")
                                      .trim();
                        
                        return result;
                    }
                }
            }
            
            return "Ошибка обработки ответа от AI: " + response;
            
        } catch (Exception e) {
            System.err.println("Ollama API error: " + e.getMessage());
            e.printStackTrace();
            return "Ошибка подключения к Ollama: " + e.getMessage();
        }
    }
}