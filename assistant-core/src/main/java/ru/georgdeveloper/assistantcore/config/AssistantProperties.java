package ru.georgdeveloper.assistantcore.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import lombok.Data;

@Data
@Component
@ConfigurationProperties(prefix = "techno.assistant")
public class AssistantProperties {
    
    private String name;
    private String personality;
    private Ollama ollama = new Ollama();
    private Telegram telegram = new Telegram();
    private Web web = new Web();
    
    @Data
    public static class Ollama {
        private String url;
        private String model;
    }
    
    @Data
    public static class Telegram {
        private boolean enabled;
        private String botName;
        private String token;
    }
    
    @Data
    public static class Web {
        private String uploadDir;
    }
}