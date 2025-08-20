package ru.georgdeveloper.assistantcore.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import ru.georgdeveloper.assistantcore.service.RepairAssistantService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import ru.georgdeveloper.assistantcore.config.ResourcePaths;

/**
 * REST API контроллер для взаимодействия между модулями системы.
 * Архитектура взаимодействия модулей:
 * assistant-telegram -> HTTP запрос -> assistant-core -> БД + AI -> ответ
 * assistant-web -> HTTP запрос -> assistant-core -> БД + AI -> ответ
 * Основные эндпоинты:
 * POST /api/analyze - анализ запросов пользователей через AI
 * Особенности:
 * - Поддержка UTF-8 кодировки для корректной работы с кириллицей
 * - Логирование входящих запросов и исходящих ответов
 * - Интеграция с реальными данными из производственной БД
 * - Обработка запросов от Telegram бота и веб-интерфейса
 */
@RestController
@RequestMapping("/api")
public class ApiController {
    
    // Основной сервис для обработки запросов с интеграцией AI и БД
    @Autowired
    private RepairAssistantService repairAssistantService;
    
    /**
     * Основной эндпоинт для анализа запросов пользователей.
     * 
     * Процесс обработки:
     * 1. Получение запроса от клиента (Telegram бот или веб-интерфейс)
     * 2. Логирование входящего запроса для отладки
     * 3. Передача в RepairAssistantService для обработки
     * 4. RepairAssistantService формирует контекст из БД
     * 5. Отправка контекста + запроса в Ollama (deepseek-r1)
     * 6. Получение и фильтрация ответа от AI
     * 7. Возврат обработанного ответа клиенту
     * 
     * @param request Текстовый запрос пользователя (например: "Посчитай ремонты со статусом временно закрыто")
     * @return Ответ AI на основе реальных данных из БД
     */
    @PostMapping(value = "/analyze", produces = "application/json;charset=UTF-8")
    public String analyzeRepairRequest(@RequestBody String request) {
        // Логирование для мониторинга и отладки
        System.out.println("Получен запрос: " + request);
        
        // Основная обработка через сервисный слой
        String response = repairAssistantService.processRepairRequest(request);
        
        // Логирование ответа для контроля качества
        System.out.println("Отправляем ответ: " + response);
        
        return response;
    }
    /**
     * Эндпоинт для обратной связи: сохраняет пару запрос-ответ для дообучения
     */
    @PostMapping("/feedback")
    public String saveFeedback(@RequestBody FeedbackDto feedback) {
        try {
            // Всегда сохраняем пару в query_training_data.jsonl
            saveToQueryTrainingData(feedback);
            // Если ответ похож на инструкцию — дополнительно сохраняем в repair_instructions.json
            if (feedback.response != null && (feedback.response.contains("SIMPLE_ANSWER") || feedback.response.toLowerCase().contains("инструкция"))) {
                saveToRepairInstructions(feedback);
            }
            return "OK";
        } catch (Exception e) {
            e.printStackTrace();
            return "ERROR: " + e.getMessage();
        }
    }

    private void saveToRepairInstructions(FeedbackDto feedback) throws IOException {
        // Сохраняем полный текст запроса и ответа
        String problem = feedback.request;
        String solution = feedback.response.trim();
        Map<String, String> entry = new LinkedHashMap<>();
        entry.put("Участок", "-");
        entry.put("Группа оборудования", "-");
        entry.put("Узел", "-");
        entry.put("Проблема", problem);
        entry.put("Решение", solution);

        ObjectMapper mapper = new ObjectMapper();
        File file = new File(ResourcePaths.REPAIR_INSTRUCTIONS_JSON_ABS);
        List<Map<String, String>> all;
        if (file.exists()) {
            List<Map<String, String>> tempList = mapper.readValue(file, new com.fasterxml.jackson.core.type.TypeReference<List<Map<String, String>>>() {});
            all = new ArrayList<>();
            for (Map<String, String> rawMap : tempList) {
                all.add(new LinkedHashMap<>(rawMap));
            }
        } else {
            all = new ArrayList<>();
        }
        all.add(entry);
        mapper.writerWithDefaultPrettyPrinter().writeValue(file, all);
    }

    private void saveToQueryTrainingData(FeedbackDto feedback) throws IOException {
        String line = String.format("{\"input\": \"%s\", \"output\": \"%s\"}\n",
                feedback.request.replace("\"", "\\\""),
                feedback.response.replace("\"", "\\\""));
        Files.write(Paths.get(ResourcePaths.QUERY_TRAINING_DATA_JSONL_ABS),
                line.getBytes(StandardCharsets.UTF_8),
                Files.exists(Paths.get(ResourcePaths.QUERY_TRAINING_DATA_JSONL_ABS)) ? java.nio.file.StandardOpenOption.APPEND : java.nio.file.StandardOpenOption.CREATE);
    }

    public static class FeedbackDto {
        public String request;
        public String response;
    }
}