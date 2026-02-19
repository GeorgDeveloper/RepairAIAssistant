package ru.georgdeveloper.assistantcore.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Конфигурация базы данных и JPA репозиториев
 */
@Configuration
@EnableJpaRepositories(basePackages = "ru.georgdeveloper.assistantcore.repository")
@EnableTransactionManagement
public class DatabaseConfig {
}