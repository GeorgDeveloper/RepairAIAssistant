package ru.georgdeveloper.assistantai.config;

public final class TrainingPaths {
    private TrainingPaths() {}

    public static final String TRAINING_DIR = "training/";
    public static final String QUERY_TRAINING_DATA_JSONL = TRAINING_DIR + "query_training_data.jsonl";
    public static final String REPAIR_INSTRUCTIONS_JSON = TRAINING_DIR + "repair_instructions.json";
}


