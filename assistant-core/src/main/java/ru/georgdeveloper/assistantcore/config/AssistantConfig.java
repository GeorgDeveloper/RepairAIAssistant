package ru.georgdeveloper.assistantcore.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@Configuration
@EnableConfigurationProperties(AssistantProperties.class)
public class AssistantConfig {
    
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}