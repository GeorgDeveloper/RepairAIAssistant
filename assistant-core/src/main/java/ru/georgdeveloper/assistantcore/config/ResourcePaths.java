package ru.georgdeveloper.assistantcore.config;

/**
 * Константы для путей к ресурсам приложения
 */
public final class ResourcePaths {
    
    private ResourcePaths() {
        // Утилитный класс
    }
    
    /**
     * Путь к файлу с инструкциями по ремонту
     */
    public static final String REPAIR_INSTRUCTIONS_JSON = "training/repair_instructions.json";
    
    /**
     * Путь к файлу с обучающими данными для запросов
     */
    public static final String QUERY_TRAINING_DATA_JSONL = "training/query_training_data.jsonl";
    
    /**
     * Базовый путь к папке с обучающими данными
     */
    public static final String TRAINING_BASE_PATH = "training/";
}