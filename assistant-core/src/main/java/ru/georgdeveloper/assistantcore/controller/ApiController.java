package ru.georgdeveloper.assistantcore.controller;

import org.springframework.web.bind.annotation.*;
import ru.georgdeveloper.assistantcore.service.LangChainAssistantService;
import ru.georgdeveloper.assistantcore.service.OllamaHealthService;
import ru.georgdeveloper.assistantcore.service.MigrationDiagnosticService;
import ru.georgdeveloper.assistantcore.service.FastMigrationService;
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
    private final LangChainAssistantService assistantService;

    // Используем MonitoringRepository вместо прямых SQL-запросов
    private final MonitoringRepository monitoringRepository;
    
    // Сервис для проверки состояния Ollama
    private final OllamaHealthService ollamaHealthService;
    
    // Сервис для диагностики миграции
    private final MigrationDiagnosticService migrationDiagnosticService;
    
    // Сервис для быстрой миграции
    private final FastMigrationService fastMigrationService;

    /**
     * Конструктор контроллера
     * @param assistantService сервис бизнес-логики с AI на основе LangChain
     * @param monitoringRepository репозиторий для справочных данных
     * @param ollamaHealthService сервис проверки состояния Ollama
     * @param migrationDiagnosticService сервис диагностики миграции
     * @param fastMigrationService сервис быстрой миграции
     */
    public ApiController(LangChainAssistantService assistantService, 
                        MonitoringRepository monitoringRepository,
                        OllamaHealthService ollamaHealthService,
                        MigrationDiagnosticService migrationDiagnosticService,
                        FastMigrationService fastMigrationService) {
        this.assistantService = assistantService;
        this.monitoringRepository = monitoringRepository;
        this.ollamaHealthService = ollamaHealthService;
        this.migrationDiagnosticService = migrationDiagnosticService;
        this.fastMigrationService = fastMigrationService;
    }
    
    /**
     * Основной эндпоинт для анализа запросов пользователей.
     * 
     * Процесс обработки:
     * 1. Получение запроса от клиента (Telegram бот или веб-интерфейс)
     * 2. Логирование входящего запроса для отладки
     * 3. Передача в LangChainAssistantService для обработки
     * 4. LangChainAssistantService выполняет семантический поиск в ChromaDB
     * 5. Формирование контекста из найденных релевантных сегментов
     * 6. Отправка контекста + запроса в Ollama (deepseek-r1) через LangChain
     * 7. Возврат обработанного ответа клиенту
     * 
     * @param request Текстовый запрос пользователя (например: "Посчитай ремонты со статусом временно закрыто")
     * @return Ответ AI на основе реальных данных из БД
     */
    @PostMapping(value = "/analyze", produces = "application/json;charset=UTF-8")
    public String analyzeRepairRequest(@RequestBody String request) {
        // Логирование для мониторинга и отладки
        logger.info("Получен запрос: {}", request);
        
        // Основная обработка через новый сервисный слой на основе LangChain
        String response = assistantService.processQuery(request);
        
        // Логирование ответа для контроля качества
        logger.info("Отправляем ответ: {}", response);
        
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

    /**
     * Эндпоинт для проверки состояния Ollama AI сервера
     * @return статус Ollama сервера и доступных моделей
     */
    @GetMapping("/health/ollama")
    public Map<String, Object> getOllamaHealth() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            boolean isAvailable = ollamaHealthService.isOllamaAvailable();
            String status = ollamaHealthService.getOllamaStatus();
            
            response.put("available", isAvailable);
            response.put("status", status);
            response.put("timestamp", new java.util.Date());
            
            if (isAvailable) {
                response.put("chatModelAvailable", ollamaHealthService.isModelAvailable("phi3:mini"));
                response.put("embeddingModelAvailable", ollamaHealthService.isModelAvailable("nomic-embed-text:latest"));
            }
            
        } catch (Exception e) {
            logger.error("Ошибка проверки состояния Ollama: {}", e.getMessage());
            response.put("available", false);
            response.put("status", "❌ Ошибка проверки состояния: " + e.getMessage());
            response.put("timestamp", new java.util.Date());
        }
        
        return response;
    }

    /**
     * Эндпоинт для проверки состояния миграции данных
     * @return статус миграции и информация о таблице migration_tracking
     */
    @GetMapping("/health/migration")
    public Map<String, Object> getMigrationHealth() {
        return migrationDiagnosticService.getFullDiagnostic();
    }

    /**
     * Эндпоинт для принудительной инициализации отслеживания миграции
     * @return результат инициализации
     */
    @PostMapping("/migration/initialize")
    public Map<String, Object> initializeMigrationTracking() {
        return migrationDiagnosticService.forceInitializeMigrationTracking();
    }

    /**
     * Эндпоинт для сброса отслеживания миграции
     * @return результат сброса
     */
    @PostMapping("/migration/reset")
    public Map<String, Object> resetMigrationTracking() {
        return migrationDiagnosticService.resetMigrationTracking();
    }

    /**
     * Эндпоинт для получения расширенной статистики миграции
     * @return подробная статистика миграции и настроек
     */
    @GetMapping("/migration/stats")
    public Map<String, Object> getMigrationStats() {
        return migrationDiagnosticService.getExtendedMigrationStats();
    }

    /**
     * Эндпоинт для быстрой миграции всех данных
     * @return результат быстрой миграции с временными метриками
     */
    @PostMapping("/migration/fast")
    public Map<String, Object> fastMigrateAllData() {
        return fastMigrationService.fastMigrateAllData();
    }

    /**
     * Эндпоинт для получения рекомендаций по модели на основе доступной памяти
     * @return рекомендации по AI модели
     */
    @GetMapping("/ai/model-recommendation")
    public Map<String, Object> getModelRecommendation() {
        return ollamaHealthService.getModelRecommendation();
    }

}