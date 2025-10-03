package ru.georgdeveloper.assistantai.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

@Configuration
public class AssistantAiConfig {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}


