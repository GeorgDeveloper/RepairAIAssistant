package ru.georgdeveloper.assistantweb.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.StringHttpMessageConverter;

import java.nio.charset.StandardCharsets;

@Configuration
public class WebHttpConfig {

    @Bean
    public StringHttpMessageConverter utf8StringHttpMessageConverter() {
        // Регистрируем отдельный конвертер, чтобы не конфликтовать с существующим RestTemplate
        return new StringHttpMessageConverter(StandardCharsets.UTF_8);
    }
}


