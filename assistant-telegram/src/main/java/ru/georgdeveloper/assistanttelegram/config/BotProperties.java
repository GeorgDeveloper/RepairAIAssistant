package ru.georgdeveloper.assistanttelegram.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import lombok.Data;

@Data
@Component
@ConfigurationProperties(prefix = "telegram.bot")
public class BotProperties {
    
    private String token;
    private String username;
}