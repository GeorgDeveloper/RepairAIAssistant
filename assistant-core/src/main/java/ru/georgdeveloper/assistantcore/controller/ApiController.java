package ru.georgdeveloper.assistantcore.controller;

import org.springframework.web.bind.annotation.*;
import ru.georgdeveloper.assistantcore.service.RepairAssistantService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import ru.georgdeveloper.assistantcore.config.ResourcePaths;
import ru.georgdeveloper.assistantcore.repository.MonitoringRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    
    private static final Logger logger = LoggerFactory.getLogger(ApiController.class);
    
    // Основной сервис для обработки запросов с интеграцией AI и БД
    private final RepairAssistantService repairAssistantService;

    // Используем MonitoringRepository вместо прямых SQL-запросов
    private final MonitoringRepository monitoringRepository;

    /**
     * Конструктор контроллера
     * @param repairAssistantService сервис бизнес-логики с AI
     * @param monitoringRepository репозиторий для справочных данных
     */
    public ApiController(RepairAssistantService repairAssistantService, MonitoringRepository monitoringRepository) {
        this.repairAssistantService = repairAssistantService;
        this.monitoringRepository = monitoringRepository;
    }
    
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
    // AI endpoints перенесены в модуль assistant-ai

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
        // Гарантируем существование директории перед записью файла
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
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
        java.nio.file.Path target = Paths.get(ResourcePaths.QUERY_TRAINING_DATA_JSONL_ABS);
        // Создаём директорию training при отсутствии
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

    // Добавлены эндпоинты для получения данных из таблиц region, equipment и node

    @GetMapping("/regions")
    public List<Map<String, Object>> getRegions() {
        return monitoringRepository.getRegions();
    }

    @GetMapping("/equipment")
    public org.springframework.http.ResponseEntity<List<Map<String, Object>>> getEquipment(@RequestParam String regionId) {
        try {
            int id = Integer.parseInt(regionId);
            return org.springframework.http.ResponseEntity.ok(monitoringRepository.getEquipment(id));
        } catch (NumberFormatException ex) {
            return org.springframework.http.ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/nodes")
    public org.springframework.http.ResponseEntity<List<Map<String, Object>>> getNodes(@RequestParam String equipmentId) {
        try {
            int id = Integer.parseInt(equipmentId);
            return org.springframework.http.ResponseEntity.ok(monitoringRepository.getNodes(id));
        } catch (NumberFormatException ex) {
            return org.springframework.http.ResponseEntity.badRequest().build();
        }
    }

}