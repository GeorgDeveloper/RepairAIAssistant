package ru.georgdeveloper.assistantbaseupdate;

import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

/**
 * Инициализатор сервлета для развертывания в WAR-архиве.
 * 
 * Позволяет развертывать модуль обновления базы данных
 * как отдельное веб-приложение в контейнере сервлетов.
 */
public class ServletInitializer extends SpringBootServletInitializer {

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(AssistantBaseUpdateApplication.class);
    }
}
