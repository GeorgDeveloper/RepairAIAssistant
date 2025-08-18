package ru.georgdeveloper.assistantcore.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Тесты для проверки путей к ресурсам
 */
@SpringBootTest
public class ResourcePathsTest {
    
    @Test
    public void testRepairInstructionsJsonExists() {
        ClassPathResource resource = new ClassPathResource(ResourcePaths.REPAIR_INSTRUCTIONS_JSON);
        assertTrue(resource.exists(), 
            "Файл repair_instructions.json должен существовать по пути: " + ResourcePaths.REPAIR_INSTRUCTIONS_JSON);
    }
    
    @Test
    public void testQueryTrainingDataJsonlExists() {
        ClassPathResource resource = new ClassPathResource(ResourcePaths.QUERY_TRAINING_DATA_JSONL);
        assertTrue(resource.exists(), 
            "Файл query_training_data.jsonl должен существовать по пути: " + ResourcePaths.QUERY_TRAINING_DATA_JSONL);
    }
    
    @Test
    public void testResourcePathsConstants() {
        // Проверяем, что константы имеют правильные значения
        assertEquals("training/repair_instructions.json", ResourcePaths.REPAIR_INSTRUCTIONS_JSON);
        assertEquals("training/query_training_data.jsonl", ResourcePaths.QUERY_TRAINING_DATA_JSONL);
        assertEquals("training/", ResourcePaths.TRAINING_BASE_PATH);
    }
}