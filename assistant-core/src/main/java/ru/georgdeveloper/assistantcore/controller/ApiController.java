package ru.georgdeveloper.assistantcore.controller;

import org.springframework.web.bind.annotation.*;
import java.util.*;
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
    
    // Используем MonitoringRepository вместо прямых SQL-запросов
    private final MonitoringRepository monitoringRepository;

    /**
     * Конструктор контроллера
     * @param monitoringRepository репозиторий для справочных данных
     */
    public ApiController(MonitoringRepository monitoringRepository) {
        this.monitoringRepository = monitoringRepository;
    }
    

    // Добавлены эндпоинты для получения данных из таблиц region, equipment и node

    @GetMapping("/regions")
    public List<Map<String, Object>> getRegions() {
        logger.info("Getting all regions");
        List<Map<String, Object>> regions = monitoringRepository.getRegions();
        logger.debug("Retrieved {} regions", regions.size());
        return regions;
    }

    @GetMapping("/equipment")
    public org.springframework.http.ResponseEntity<List<Map<String, Object>>> getEquipment(@RequestParam String regionId) {
        logger.info("Getting equipment for regionId: {}", regionId);
        try {
            int id = Integer.parseInt(regionId);
            List<Map<String, Object>> equipment = monitoringRepository.getEquipment(id);
            logger.debug("Retrieved {} equipment items for region {}", equipment.size(), id);
            return org.springframework.http.ResponseEntity.ok(equipment);
        } catch (NumberFormatException ex) {
            logger.warn("Invalid regionId format: {}", regionId, ex);
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