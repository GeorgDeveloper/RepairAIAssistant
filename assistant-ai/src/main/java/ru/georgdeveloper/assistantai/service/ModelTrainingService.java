package ru.georgdeveloper.assistantai.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.annotation.Scheduled;
import ru.georgdeveloper.assistantai.config.TrainingPaths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

import jakarta.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ModelTrainingService {
    private static final Logger logger = LoggerFactory.getLogger(ModelTrainingService.class);

    // No DB repositories; training is file-driven in assistant-ai

    @Autowired
    private OllamaService ollamaService;

    @Value("${ai.training.batch-size:200}")
    private int batchSize;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private List<TrainingExample> trainingData;

    @PostConstruct
    public void init() {
        loadTrainingData();
        loadRepairInstructionsData();
        trainOnHistoricalData();
    }

    public void loadTrainingData() {
        trainingData = new ArrayList<>();
        try {
            ClassPathResource resource = new ClassPathResource(TrainingPaths.QUERY_TRAINING_DATA_JSONL);
            if (!resource.exists()) {
                logger.warn("Файл обучающих данных не найден: {}", TrainingPaths.QUERY_TRAINING_DATA_JSONL);
                return;
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                int skipped = 0;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty()) { continue; }
                    try {
                        JsonNode node = objectMapper.readTree(line);
                        if (node.hasNonNull("input") && node.hasNonNull("output")) {
                            String input = node.get("input").asText("");
                            String output = node.get("output").asText("");
                            if (!input.isBlank() && !output.isBlank()) {
                                trainingData.add(new TrainingExample(input, output));
                            } else {
                                skipped++;
                            }
                        } else {
                            skipped++;
                        }
                    } catch (Exception perLine) {
                        skipped++;
                    }
                }
                if (skipped > 0) {
                    logger.warn("Пропущено {} некорректных строк обучающих данных", skipped);
                }
            }
            logger.info("Загружено {} примеров для обучения", trainingData.size());
        } catch (Exception e) {
            logger.error("Ошибка загрузки обучающих данных: {}", e.getMessage(), e);
            trainingData = new ArrayList<>();
        }
    }

    public void loadRepairInstructionsData() {
        try {
            java.util.List<com.fasterxml.jackson.databind.JsonNode> instructions = new java.util.ArrayList<>();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(new ClassPathResource(TrainingPaths.REPAIR_INSTRUCTIONS_JSON).getInputStream(), StandardCharsets.UTF_8))) {
                com.fasterxml.jackson.databind.JsonNode arr = objectMapper.readTree(reader);
                if (arr.isArray()) {
                    for (com.fasterxml.jackson.databind.JsonNode n : arr) instructions.add(n);
                }
            }

            if (trainingData == null) trainingData = new ArrayList<>();

            for (com.fasterxml.jackson.databind.JsonNode instruction : instructions) {
                String input = "Как устранить: " + instruction.path("Проблема").asText("");
                String output = "SIMPLE_ANSWER: " + instruction.path("Решение").asText("");
                trainingData.add(new TrainingExample(input, output));

                if (instruction.path("Проблема").asText("").toLowerCase(Locale.ROOT).contains("утечка")) {
                    String leakInput = "Утечка на " + instruction.path("Группа оборудования").asText("");
                    trainingData.add(new TrainingExample(leakInput, output));
                }

                if (instruction.path("Проблема").asText("").toLowerCase(Locale.ROOT).contains("не работает")) {
                    String notWorkingInput = "Не работает " + instruction.path("Узел").asText("");
                    trainingData.add(new TrainingExample(notWorkingInput, output));
                }
            }

            logger.info("Добавлено {} примеров из repair_instructions.json", 
                       instructions.size() * 2);

        } catch (Exception e) {
            logger.error("Ошибка загрузки данных из repair_instructions.json: {}", e.getMessage(), e);
        }
    }

    public String analyzeWithTraining(String request) {
        if (trainingData == null || trainingData.isEmpty()) {
            loadTrainingData();
            trainOnHistoricalData();
        }

        List<TrainingExample> relevantExamples = getRelevantExamples(request, 10);

        StringBuilder prompt = new StringBuilder();
        prompt.append("Ты - анализатор запросов для системы ремонта оборудования.\n");
        prompt.append("Изучи примеры и отвечай в том же формате:\n\n");

        for (TrainingExample example : relevantExamples) {
            prompt.append("Запрос: ").append(example.getInput()).append("\n");
            prompt.append("Ответ: ").append(example.getOutput()).append("\n\n");
        }

        prompt.append("Теперь проанализируй этот запрос:\n");
        prompt.append("Запрос: ").append(request).append("\n");
        prompt.append("Ответ: ");

        return ollamaService.generateResponse(prompt.toString());
    }

    private List<TrainingExample> getRelevantExamples(String request, int limit) {
        if (trainingData.size() <= limit) {
            return trainingData;
        }
        String[] keywords = request.toLowerCase(Locale.ROOT).split("\\s+");
        return trainingData.stream()
            .filter(example -> {
                String input = example.getInput().toLowerCase(Locale.ROOT);
                for (String keyword : keywords) {
                    if (input.contains(keyword)) {
                        return true;
                    }
                }
                return false;
            })
            .limit(limit)
            .collect(Collectors.toList());
    }

    public void trainOnHistoricalData() { /* no-op without DB */ }

    // DB-derived example generation removed

    @Scheduled(cron = "0 0 3 * * ?")
    public void retrainModel() {
        logger.info("Начинаем переобучение модели...");
        trainOnHistoricalData();
        loadRepairInstructionsData();
    }

    public void addTrainingExample(String input, String output) {
        if (trainingData == null) {
            loadTrainingData();
        }
        trainingData.add(new TrainingExample(input, output));
        if (input != null && !input.trim().isEmpty()) {
            logger.info("Добавлен новый пример обучения: {}", input.length() > 100 ? input.substring(0, 100) + "..." : input);
        }
    }

    public static class TrainingExample {
        private final String input;
        private final String output;
        public TrainingExample(String input, String output) {
            this.input = input;
            this.output = output;
        }
        public String getInput() { return input; }
        public String getOutput() { return output; }
        @Override
        public String toString() {
            return "TrainingExample{input='" + input + "', output='" + output + "'}";
        }
    }
}


