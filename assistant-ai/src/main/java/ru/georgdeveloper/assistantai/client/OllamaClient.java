package ru.georgdeveloper.assistantai.client;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class OllamaClient {
    private static final Logger logger = LoggerFactory.getLogger(OllamaClient.class);

    @Autowired
    private RestTemplate restTemplate;

    @Value("${ai.ollama.url:http://localhost:11434/api}")
    private String ollamaBaseUrl;

    @Value("${ai.ollama.model:mistral:latest}")
    private String ollamaModel;

    public String generateResponse(String prompt) {
        try {
            String baseUrl = ollamaBaseUrl;
            String model = ollamaModel;

            String url = baseUrl + "/generate";

            String escapedPrompt = prompt.replace("\\", "\\\\")
                                         .replace("\"", "\\\"")
                                         .replace("\n", "\\n")
                                         .replace("\r", "\\r")
                                         .replace("\t", "\\t");

            String requestBody = "{" +
                "\"model\": \"" + model + "\"," +
                "\"prompt\": \"" + escapedPrompt + "\"," +
                "\"stream\": false," +
                "\"options\": {\"temperature\": 0.7}" +
                "}";

            logger.debug("Ollama request: {}", requestBody);

            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
            headers.set("Accept", "application/json");

            org.springframework.http.HttpEntity<String> entity = new org.springframework.http.HttpEntity<>(requestBody, headers);

            String response = restTemplate.postForObject(url, entity, String.class);
            logger.debug("Ollama response: {}", response);

            if (response != null && response.contains("\"response\":")) {
                int responseStart = response.indexOf("\"response\":");
                if (responseStart != -1) {
                    responseStart += 11;
                    while (responseStart < response.length() && 
                           (response.charAt(responseStart) == ' ' || response.charAt(responseStart) == '"')) {
                        responseStart++;
                    }
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
            logger.error("Ollama API error: {}", e.getMessage(), e);
            return "Ошибка подключения к Ollama: " + e.getMessage();
        }
    }
}


