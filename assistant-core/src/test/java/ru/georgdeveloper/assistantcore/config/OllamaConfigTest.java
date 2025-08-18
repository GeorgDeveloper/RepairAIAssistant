package ru.georgdeveloper.assistantcore.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class OllamaConfigTest {
    
    @Autowired
    private AssistantProperties properties;
    
    @Test
    public void testOllamaConfiguration() {
        assertNotNull(properties, "AssistantProperties должны быть загружены");
        assertNotNull(properties.getOllama(), "Ollama конфигурация должна быть загружена");
        
        String url = properties.getOllama().getUrl();
        String model = properties.getOllama().getModel();
        
        System.out.println("Ollama URL: " + url);
        System.out.println("Ollama Model: " + model);
        
        assertNotNull(url, "URL Ollama не должен быть null");
        assertFalse(url.trim().isEmpty(), "URL Ollama не должен быть пустым");
        assertTrue(url.startsWith("http"), "URL должен начинаться с http");
    }
}