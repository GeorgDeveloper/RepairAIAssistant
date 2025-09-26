package ru.georgdeveloper.assistantbaseupdate;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Главный класс приложения модуля обновления базы данных.
 * 
 * Модуль предназначен для:
 * - Обновления схемы базы данных
 * - Миграции данных между версиями
 * - Синхронизации данных между системами
 * - Резервного копирования и восстановления
 * 
 * Особенности:
 * - Автоматическое планирование задач обновления
 * - Безопасное выполнение миграций
 * - Логирование всех операций обновления
 * - Интеграция с основной системой Repair AI Assistant
 */
@SpringBootApplication
@EnableScheduling
public class AssistantBaseUpdateApplication {

    public static void main(String[] args) {
        SpringApplication.run(AssistantBaseUpdateApplication.class, args);
    }
}
