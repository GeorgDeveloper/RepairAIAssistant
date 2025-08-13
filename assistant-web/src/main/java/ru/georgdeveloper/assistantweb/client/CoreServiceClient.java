package ru.georgdeveloper.assistantweb.client;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class CoreServiceClient {
    
    @Autowired
    private RestTemplate restTemplate;
    
    private static final String CORE_SERVICE_URL = "http://localhost:8080/api";
    
    public String analyzeRepairRequest(String request) {
        return restTemplate.postForObject(CORE_SERVICE_URL + "/analyze", request, String.class);
    }
}