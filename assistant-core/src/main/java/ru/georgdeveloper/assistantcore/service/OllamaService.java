package ru.georgdeveloper.assistantcore.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.georgdeveloper.assistantcore.client.OllamaClient;

@Service
public class OllamaService {
    
    @Autowired
    private OllamaClient ollamaClient;
    
    public String generateResponse(String prompt) {
        return ollamaClient.generateResponse(prompt);
    }
}