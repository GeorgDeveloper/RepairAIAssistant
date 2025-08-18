package ru.georgdeveloper.assistantcore.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import ru.georgdeveloper.assistantcore.service.RepairAssistantService;

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
}