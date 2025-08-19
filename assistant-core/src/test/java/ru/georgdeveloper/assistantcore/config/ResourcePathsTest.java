package ru.georgdeveloper.assistantcore.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Тесты для проверки путей к ресурсам
 */
@SpringBootTest
public class ResourcePathsTest {
    
    @Test
    public void testRepairInstructionsJsonExists() {
        File file = new File(ResourcePaths.REPAIR_INSTRUCTIONS_JSON);
        assertTrue(file.exists(),
            "Файл repair_instructions.json должен существовать по пути: " + ResourcePaths.REPAIR_INSTRUCTIONS_JSON);
    }

    @Test
    public void testQueryTrainingDataJsonlExists() {
        File file = new File(ResourcePaths.QUERY_TRAINING_DATA_JSONL);
        assertTrue(file.exists(),
            "Файл query_training_data.jsonl должен существовать по пути: " + ResourcePaths.QUERY_TRAINING_DATA_JSONL);
    }

    @Test
    public void testResourcePathsConstants() {
    // Проверяем, что константы оканчиваются на нужные имена файлов
    assertTrue(ResourcePaths.REPAIR_INSTRUCTIONS_JSON.endsWith("training/repair_instructions.json"));
    assertTrue(ResourcePaths.QUERY_TRAINING_DATA_JSONL.endsWith("training/query_training_data.jsonl"));
    }
}