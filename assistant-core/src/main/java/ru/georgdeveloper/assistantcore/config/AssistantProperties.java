package ru.georgdeveloper.assistantcore.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

/**
 * Конфигурационные свойства AI ассистента
 */
@Data
@ConfigurationProperties(prefix = "ai")
public class AssistantProperties {
    
    /** Имя ассистента */
    private String name;
    /** Личность ассистента */
    private String personality;
    /** Настройки Ollama */
    private Ollama ollama = new Ollama();
    /** Настройки Telegram бота */
    private Telegram telegram = new Telegram();
    /** Настройки веб-интерфейса */
    private Web web = new Web();
    
    /**
     * Настройки подключения к Ollama
     */
    @Data
    public static class Ollama {
        /** URL сервера Ollama */
        private String url;
        /** Модель для использования */
        private String model;
    }
    
    /**
     * Настройки Telegram бота
     */
    @Data
    public static class Telegram {
        /** Включен ли Telegram бот */
        private boolean enabled;
        /** Имя бота */
        private String botName;
        /** Токен бота */
        private String token;
    }
    
    /**
     * Настройки веб-интерфейса
     */
    @Data
    public static class Web {
        /** Директория для загрузки файлов */
        private String uploadDir;
    }
}