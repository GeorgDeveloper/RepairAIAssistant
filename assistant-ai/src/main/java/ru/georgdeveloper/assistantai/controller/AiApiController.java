package ru.georgdeveloper.assistantai.controller;

import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.georgdeveloper.assistantai.service.OllamaService;
import ru.georgdeveloper.assistantai.config.TrainingPaths;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

@RestController
@RequestMapping("/api")
public class AiApiController {

    private static final Logger logger = LoggerFactory.getLogger(AiApiController.class);

    private final OllamaService ollamaService;

    public AiApiController(OllamaService ollamaService) {
        this.ollamaService = ollamaService;
    }

    @PostMapping(value = "/analyze", consumes = "application/json;charset=UTF-8", produces = "application/json;charset=UTF-8")
    public String analyzeRepairRequest(@RequestBody String request) {
        String normalized = request;
        if (normalized != null && normalized.length() >= 2 && normalized.startsWith("\"") && normalized.endsWith("\"")) {
            // Убираем обертку JSON-строки и экранирование
            normalized = normalized.substring(1, normalized.length() - 1)
                    .replace("\\\"", "\"")
                    .replace("\\n", "\n")
                    .replace("\\r", "\r")
                    .replace("\\t", "\t")
                    .replace("\\\\", "\\");
        }
        logger.info("Получен AI-запрос: {}", normalized);
        String response = ollamaService.generateResponse(normalized);
        logger.info("Ответ AI: {}", response);
        return response;
    }

    @PostMapping("/feedback")
    public String saveFeedback(@RequestBody FeedbackDto feedback) {
        try {
            saveToQueryTrainingData(feedback);
            if (feedback.response != null && (feedback.response.contains("SIMPLE_ANSWER") || feedback.response.toLowerCase().contains("инструкция"))) {
                saveToRepairInstructions(feedback);
            }
            return "OK";
        } catch (Exception e) {
            logger.error("Ошибка сохранения обратной связи", e);
            return "ERROR: " + e.getMessage();
        }
    }

    private void saveToRepairInstructions(FeedbackDto feedback) throws IOException {
        String problem = feedback.request;
        String solution = feedback.response.trim();
        Map<String, String> entry = new LinkedHashMap<>();
        entry.put("Участок", "-");
        entry.put("Группа оборудования", "-");
        entry.put("Узел", "-");
        entry.put("Проблема", problem);
        entry.put("Решение", solution);

        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        File file = new File(TrainingPaths.REPAIR_INSTRUCTIONS_JSON);
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        java.util.List<java.util.Map<String, String>> all;
        if (file.exists()) {
            java.util.List<java.util.Map<String, String>> tempList = mapper.readValue(file, new com.fasterxml.jackson.core.type.TypeReference<java.util.List<java.util.Map<String, String>>>() {});
            all = new java.util.ArrayList<>();
            for (java.util.Map<String, String> rawMap : tempList) {
                all.add(new java.util.LinkedHashMap<>(rawMap));
            }
        } else {
            all = new java.util.ArrayList<>();
        }
        all.add(entry);
        mapper.writerWithDefaultPrettyPrinter().writeValue(file, all);
    }

    private void saveToQueryTrainingData(FeedbackDto feedback) throws IOException {
        String line = String.format("{\"input\": \"%s\", \"output\": \"%s\"}\n",
                feedback.request.replace("\"", "\\\""),
                feedback.response.replace("\"", "\\\""));
        java.nio.file.Path target = Paths.get(TrainingPaths.QUERY_TRAINING_DATA_JSONL);
        if (target.getParent() != null && !Files.exists(target.getParent())) {
            Files.createDirectories(target.getParent());
        }
        Files.write(target,
                line.getBytes(StandardCharsets.UTF_8),
                Files.exists(target) ? java.nio.file.StandardOpenOption.APPEND : java.nio.file.StandardOpenOption.CREATE);
    }

    public static class FeedbackDto {
        public String request;
        public String response;
    }
}


