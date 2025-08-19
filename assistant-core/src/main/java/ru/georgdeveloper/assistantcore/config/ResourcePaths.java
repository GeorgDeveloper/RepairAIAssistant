package ru.georgdeveloper.assistantcore.config;

/**
 * Константы для путей к ресурсам приложения
 */


public final class ResourcePaths {

    private ResourcePaths() {
        // Утилитный класс
    }

    // Для чтения (через classpath)
    public static final String REPAIR_INSTRUCTIONS_JSON = "training/repair_instructions.json";
    public static final String QUERY_TRAINING_DATA_JSONL = "training/query_training_data.jsonl";

    // Для записи (абсолютный путь)
    public static final String REPAIR_INSTRUCTIONS_JSON_ABS = "training/repair_instructions.json";
    public static final String QUERY_TRAINING_DATA_JSONL_ABS ="training/query_training_data.jsonl";
}