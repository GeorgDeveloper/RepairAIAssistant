package ru.georgdeveloper.assistantcore.service;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import ru.georgdeveloper.assistantcore.config.ResourcePaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.annotation.PostConstruct;

/**
 * Сервис для валидации наличия необходимых ресурсов
 */
@Service
public class ResourceValidationService {
    
    private static final Logger logger = LoggerFactory.getLogger(ResourceValidationService.class);
    
    @PostConstruct
    public void validateResources() {
        validateResource(ResourcePaths.REPAIR_INSTRUCTIONS_JSON, "Файл с инструкциями по ремонту");
        validateResource(ResourcePaths.QUERY_TRAINING_DATA_JSONL, "Файл с обучающими данными");
    }
    
    private void validateResource(String resourcePath, String description) {
        try {
            ClassPathResource resource = new ClassPathResource(resourcePath);
            if (resource.exists()) {
                logger.info("✓ {} найден: {}", description, resourcePath);
            } else {
                logger.warn("⚠ {} не найден: {}", description, resourcePath);
            }
        } catch (Exception e) {
            logger.error("✗ Ошибка проверки {}: {}", description, e.getMessage());
        }
    }
    
    /**
     * Проверяет доступность ресурса
     */
    public boolean isResourceAvailable(String resourcePath) {
        try {
            ClassPathResource resource = new ClassPathResource(resourcePath);
            return resource.exists();
        } catch (Exception e) {
            logger.debug("Ресурс {} недоступен: {}", resourcePath, e.getMessage());
            return false;
        }
    }
}