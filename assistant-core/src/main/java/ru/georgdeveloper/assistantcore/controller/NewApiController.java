package ru.georgdeveloper.assistantcore.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.georgdeveloper.assistantcore.service.DataMigrationService;
import ru.georgdeveloper.assistantcore.service.FallbackAssistantService;
import ru.georgdeveloper.assistantcore.service.LangChainAssistantService;
import ru.georgdeveloper.assistantcore.service.VectorStoreService;

import java.util.Map;

/**
 * Новый API контроллер на основе LangChain
 * Заменяет сложную логику старого ApiController
 */
@RestController
@RequestMapping("/api/v2")
@RequiredArgsConstructor
@Slf4j
public class NewApiController {

    private final DataMigrationService migrationService;
    
    @Autowired(required = false)
    private LangChainAssistantService assistantService;
    
    @Autowired(required = false) 
    private VectorStoreService vectorStoreService;
    
    @Autowired(required = false)
    private FallbackAssistantService fallbackService;
    
    @Value("${ai.enabled:true}")
    private boolean aiEnabled;

    /**
     * Основной эндпоинт для обработки запросов пользователей
     * Использует семантический поиск и LangChain для генерации ответов
     */
    @PostMapping(value = "/query", produces = "application/json;charset=UTF-8")
    public ResponseEntity<Map<String, Object>> processQuery(@RequestBody Map<String, String> request) {
        try {
            String userQuery = request.get("query");
            if (userQuery == null || userQuery.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Запрос не может быть пустым"));
            }

            log.info("Получен запрос: {}", userQuery);

            // Обрабатываем запрос через соответствующий сервис
            String response;
            if (!aiEnabled || assistantService == null) {
                if (fallbackService != null) {
                    response = fallbackService.processQuery(userQuery);
                } else {
                    response = "🚫 AI функции отключены. Обратитесь к администратору.";
                }
            } else {
                response = assistantService.processQuery(userQuery);
            }

            log.info("Ответ сгенерирован, длина: {} символов", response.length());

            return ResponseEntity.ok(Map.of(
                    "response", response,
                    "query", userQuery,
                    "timestamp", System.currentTimeMillis(),
                    "version", "v2-langchain"
            ));

        } catch (Exception e) {
            log.error("Ошибка обработки запроса", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Внутренняя ошибка сервера: " + e.getMessage()));
        }
    }

    /**
     * Эндпоинт для обработки запросов с фильтрацией по типу данных
     */
    @PostMapping(value = "/query/filtered", produces = "application/json;charset=UTF-8")
    public ResponseEntity<Map<String, Object>> processFilteredQuery(
            @RequestBody Map<String, String> request,
            @RequestParam(defaultValue = "all") String dataType) {
        
        try {
            String userQuery = request.get("query");
            if (userQuery == null || userQuery.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Запрос не может быть пустым"));
            }

            log.info("Получен фильтрованный запрос: {}, тип: {}", userQuery, dataType);

            String response;
            if (!aiEnabled || assistantService == null) {
                if (fallbackService != null) {
                    response = fallbackService.processQuery(userQuery);
                } else {
                    response = "🚫 AI функции отключены. Обратитесь к администратору.";
                }
            } else {
                if ("all".equals(dataType)) {
                    response = assistantService.processQuery(userQuery);
                } else {
                    response = assistantService.processQueryWithFilter(userQuery, dataType);
                }
            }

            return ResponseEntity.ok(Map.of(
                    "response", response,
                    "query", userQuery,
                    "dataType", dataType,
                    "timestamp", System.currentTimeMillis(),
                    "version", "v2-langchain"
            ));

        } catch (Exception e) {
            log.error("Ошибка обработки фильтрованного запроса", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Внутренняя ошибка сервера: " + e.getMessage()));
        }
    }

    /**
     * Эндпоинт для сохранения обратной связи пользователей в векторную БД
     */
    @PostMapping(value = "/feedback", produces = "application/json;charset=UTF-8")
    public ResponseEntity<Map<String, Object>> saveFeedback(@RequestBody Map<String, String> request) {
        try {
            String userQuery = request.get("request");
            String assistantResponse = request.get("response");
            
            if (userQuery == null || userQuery.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Запрос пользователя не может быть пустым"));
            }
            
            if (assistantResponse == null || assistantResponse.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Ответ ассистента не может быть пустым"));
            }

            log.info("Сохранение обратной связи: запрос длиной {} символов", userQuery.length());

            // Сохраняем в векторную базу данных (если AI включен)
            if (aiEnabled && vectorStoreService != null) {
                vectorStoreService.addUserFeedback(userQuery, assistantResponse);
            } else {
                log.info("Обратная связь получена, но AI отключен - сохранение пропущено");
            }

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Обратная связь сохранена в векторную базу данных",
                    "timestamp", System.currentTimeMillis(),
                    "version", "v2-langchain"
            ));

        } catch (Exception e) {
            log.error("Ошибка сохранения обратной связи", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Ошибка сохранения обратной связи: " + e.getMessage()));
        }
    }

    /**
     * Эндпоинт для проверки здоровья системы
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        try {
            return ResponseEntity.ok(Map.of(
                    "status", "healthy",
                    "version", "v2-langchain",
                    "timestamp", System.currentTimeMillis(),
                    "components", Map.of(
                            "ai", aiEnabled ? "enabled" : "disabled",
                            "langchain", (aiEnabled && assistantService != null) ? "active" : "inactive",
                            "vectorStore", (aiEnabled && vectorStoreService != null) ? "active" : "inactive",
                            "fallback", (!aiEnabled && fallbackService != null) ? "active" : "inactive"
                    )
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of(
                            "status", "unhealthy",
                            "error", e.getMessage()
                    ));
        }
    }

    /**
     * Эндпоинт для запуска миграции данных
     */
    @PostMapping("/admin/migrate")
    public ResponseEntity<Map<String, Object>> migrateData() {
        try {
            log.info("Запуск миграции данных по запросу администратора");
            
            // Запускаем миграцию в отдельном потоке
            new Thread(() -> {
                try {
                    migrationService.migrateAllData();
                } catch (Exception e) {
                    log.error("Ошибка миграции данных", e);
                }
            }).start();

            return ResponseEntity.ok(Map.of(
                    "message", "Миграция данных запущена",
                    "status", "started",
                    "timestamp", System.currentTimeMillis()
            ));

        } catch (Exception e) {
            log.error("Ошибка запуска миграции", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Ошибка запуска миграции: " + e.getMessage()));
        }
    }

    /**
     * Эндпоинт для инкрементальной миграции новых данных
     */
    @PostMapping("/admin/migrate/incremental")
    public ResponseEntity<Map<String, Object>> migrateNewData() {
        try {
            log.info("Запуск инкрементальной миграции");
            
            new Thread(() -> {
                try {
                    migrationService.migrateNewRecords();
                } catch (Exception e) {
                    log.error("Ошибка инкрементальной миграции", e);
                }
            }).start();

            return ResponseEntity.ok(Map.of(
                    "message", "Инкрементальная миграция запущена",
                    "status", "started",
                    "timestamp", System.currentTimeMillis()
            ));

        } catch (Exception e) {
            log.error("Ошибка инкрементальной миграции", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Ошибка миграции: " + e.getMessage()));
        }
    }

    /**
     * Полная миграция данных с очисткой векторного хранилища (только для администраторов)
     */
    @PostMapping("/admin/migrate/clear")
    public ResponseEntity<Map<String, Object>> migrateDataWithClear() {
        try {
            log.info("Запуск полной миграции данных с очисткой через API");
            
            new Thread(() -> {
                try {
                    migrationService.migrateAllDataWithClear();
                } catch (Exception e) {
                    log.error("Ошибка миграции данных с очисткой", e);
                }
            }).start();
            
            return ResponseEntity.ok(Map.of(
                "status", "started",
                "message", "Полная миграция данных с очисткой запущена",
                "timestamp", System.currentTimeMillis()
            ));
        } catch (Exception e) {
            log.error("Ошибка при запуске миграции с очисткой", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "status", "error",
                "message", "Ошибка при запуске миграции с очисткой: " + e.getMessage(),
                "timestamp", System.currentTimeMillis()
            ));
        }
    }

    /**
     * Очистка векторного хранилища (только для администраторов)
     */
    @PostMapping("/admin/clear")
    public ResponseEntity<Map<String, Object>> clearVectorStore() {
        try {
            log.info("Запуск очистки векторного хранилища через API");
            vectorStoreService.clearStore();
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Векторное хранилище очищено",
                "timestamp", System.currentTimeMillis()
            ));
        } catch (Exception e) {
            log.error("Ошибка при очистке векторного хранилища", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "status", "error",
                "message", "Ошибка при очистке: " + e.getMessage(),
                "timestamp", System.currentTimeMillis()
            ));
        }
    }

    /**
     * Эндпоинт для получения информации о системе
     */
    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> getSystemInfo() {
        return ResponseEntity.ok(Map.of(
                "name", "Repair AI Assistant v2",
                "version", "2.0.0-langchain",
                "description", "AI ассистент на основе LangChain4j и ChromaDB",
                "features", Map.of(
                        "semanticSearch", true,
                        "vectorDatabase", true,
                        "langchain", true,
                        "embeddings", "nomic-embed-text",
                        "llm", "deepseek-r1:latest"
                ),
                "endpoints", Map.of(
                        "query", "/api/v2/query",
                        "filteredQuery", "/api/v2/query/filtered",
                        "feedback", "/api/v2/feedback",
                        "health", "/api/v2/health",
                        "migrate", "/api/v2/admin/migrate",
                        "migrateIncremental", "/api/v2/admin/migrate/incremental",
                        "migrateClear", "/api/v2/admin/migrate/clear",
                        "clearVectorStore", "/api/v2/admin/clear"
                )
        ));
    }
}
