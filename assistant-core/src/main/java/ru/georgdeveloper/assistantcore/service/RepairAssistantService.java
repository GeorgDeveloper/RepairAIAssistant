package ru.georgdeveloper.assistantcore.service;

import org.springframework.stereotype.Service;

import ru.georgdeveloper.assistantcore.repository.EquipmentMaintenanceRepository;
import ru.georgdeveloper.assistantcore.repository.BreakdownReportRepository;
import ru.georgdeveloper.assistantcore.model.EquipmentMaintenanceRecord;
import ru.georgdeveloper.assistantcore.model.BreakdownReport;
import ru.georgdeveloper.assistantcore.model.SummaryOfSolutions;
import ru.georgdeveloper.assistantcore.repository.SummaryOfSolutionsRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import java.util.List;

/**
 * Основной сервис для обработки запросов по ремонту оборудования.
 * Интегрирует данные из базы данных с AI-анализом через Ollama.
 * Функциональность:
 * - Получение данных о ремонтах из различных таблиц БД
 * - Формирование контекста для AI на основе реальных данных
 * - Обработка запросов пользователей через deepseek-coder:6.7b модель
 * - Поиск релевантной информации в руководствах и инструкциях
 */
@Service
public class RepairAssistantService {
    
    // Сервис для взаимодействия с Ollama AI (deepseek-coder:6.7b)
    private final OllamaService ollamaService;
    
    // Репозитории БД (могут отсутствовать в профиле без БД)
    private final EquipmentMaintenanceRepository equipmentMaintenanceRepository;
    private final BreakdownReportRepository breakdownReportRepository;
    private final SummaryOfSolutionsRepository summaryOfSolutionsRepository;
    
    // Сервис анализа запросов
    private final QueryAnalysisService queryAnalysisService;
    
    // Сервис поиска по БД
    private final DatabaseSearchService databaseSearchService;

    /**
     * Конструктор сервиса
     */
    public RepairAssistantService(
            OllamaService ollamaService,
            EquipmentMaintenanceRepository equipmentMaintenanceRepository,
            BreakdownReportRepository breakdownReportRepository,
            SummaryOfSolutionsRepository summaryOfSolutionsRepository,
            QueryAnalysisService queryAnalysisService,
            DatabaseSearchService databaseSearchService
    ) {
        this.ollamaService = ollamaService;
        this.equipmentMaintenanceRepository = equipmentMaintenanceRepository;
        this.breakdownReportRepository = breakdownReportRepository;
        this.summaryOfSolutionsRepository = summaryOfSolutionsRepository;
        this.queryAnalysisService = queryAnalysisService;
        this.databaseSearchService = databaseSearchService;
    }
    
    /**
     * Главный метод обработки запросов пользователей.
     * 
     * Алгоритм работы:
     * 1. Извлекает релевантные данные из всех доступных таблиц БД
     * 2. Формирует контекст с реальными данными о ремонтах
     * 3. Добавляет инструкции для AI модели
     * 4. Отправляет запрос в Ollama (deepseek-coder:6.7b)
     * 5. Возвращает обработанный ответ без технических размышлений
     * 
     * @param request Запрос пользователя (например: "Посчитай ремонты со статусом временно закрыто")
     * @return Ответ AI на основе реальных данных из БД
     */
    public String processRepairRequest(String request) {
        // Универсальный анализ запроса и поиск по всем таблицам
        DatabaseSearchService.SearchResult result = databaseSearchService.searchAll(request, 10);
        StringBuilder context = new StringBuilder();
        if (!result.summary.isEmpty()) {
            context.append("НАЙДЕННЫЕ РЕШЕНИЯ ИЗ БАЗЫ СЛОЖНЫХ РЕМОНТОВ:\n");
            for (var s : result.summary) {
                context.append(String.format("• Оборудование: %s\n  Узел: %s\n  Описание: %s\n  Меры: %s\n  Комментарии: %s\n\n",
                    s.getEquipment(), s.getNode(), s.getNotes_on_the_operation_of_the_equipment(), s.getMeasures_taken(), s.getComments()));
            }
        }
        if (!result.equipment.isEmpty()) {
            context.append("Реальные случаи ремонта из базы EquipmentMaintenanceRecord:\n");
            for (var r : result.equipment) {
                context.append(String.format("• Оборудование: %s\n  Узел: %s\n  Проблема: %s\n  Решение: %s\n  Статус: %s\n\n",
                    r.getMachineName(), r.getMechanismNode(), r.getDescription(), r.getComments(), r.getStatus()));
            }
        }
        if (!result.breakdowns.isEmpty()) {
            context.append("Отчеты о поломках из базы BreakdownReport:\n");
            for (var b : result.breakdowns) {
                context.append(String.format("• Машина: %s\n  Узел: %s\n  Комментарий: %s\n  Статус: %s\n\n",
                    b.getMachineName(), b.getAssembly(), b.getComment(), b.getWoStatusLocalDescr()));
            }
        }
        String prompt;
        if (context.length() == 0) {
            // fallback: поиск по ключевым словам
            prompt = SmartPromptBuilder.buildKeywordFallbackPrompt(request, equipmentMaintenanceRepository);
        } else {
            prompt = String.format("""
                Ты — Kvant AI, эксперт по ремонту промышленного оборудования.
                Используй только данные из базы ниже для ответа. Не придумывай информацию.
                ДАННЫЕ ИЗ БАЗЫ:
                %s
                Запрос пользователя: %s
                Дай подробный ответ, строго основываясь на найденных данных. Если данных нет — сообщи об этом.
                """, context, request);
        }
        return ollamaService.generateResponse(prompt);
    }
    
    /**
     * Обработка запроса с поддержкой индикатора прогресса
     */
    public String processRepairRequest(String request, Runnable progressCallback) {
        // Проверяем общие запросы
        if (isGeneralQuery(request)) {
            return handleGeneralQuery(request);
        }
        
        // Проверяем, является ли это запросом на инструкцию по ремонту
        if (isRepairInstructionRequest(request)) {
            // Получаем данные из БД для контекста
            String dbContext = buildRepairInstructionContext(request);
            
            // Используем промпт с данными из БД
            String prompt = buildRepairPromptWithData(request, dbContext);
            String response = ollamaService.generateResponse(prompt);
            return removeThinkTags(response);
        }
        
        // Этап 1: AI анализирует запрос и решает, нужны ли данные из БД
        QueryAnalysisService.AnalysisResult analysis = queryAnalysisService.analyzeRequest(request);
        
        if (!analysis.needsDatabase()) {
            // AI может ответить самостоятельно
            return analysis.getSimpleAnswer();
        }
        
        // Этап 2: AI определяет параметры для поиска в БД
        QueryAnalysisService.QueryParams params = queryAnalysisService.generateQueryParams(request, analysis.getDataNeeded());
        
        // Обновляем индикатор прогресса
        if (progressCallback != null) {
            progressCallback.run();
        }
        
        // Этап 3: Получаем данные из БД по AI-генерированным параметрам
        buildDatabaseContextWithParams(params);
        
        // Обновляем индикатор прогресса
        if (progressCallback != null) {
            progressCallback.run();
        }
        
        // Этап 4: AI анализирует полученные данные и дает ответ с умным промптом
    String prompt = SmartPromptBuilder.buildStatisticsPrompt(request, params, databaseSearchService);
        String response = ollamaService.generateResponse(prompt);
        
        return removeThinkTags(response);
    }
    
    /**
     * Формирует контекст для AI на основе умных запросов к базе данных.
     * Использует QueryAnalysisService для анализа запроса пользователя
     * и генерации оптимальных SQL-запросов с лимитами.
     * @return Форматированный контекст с релевантными данными
     */
    private String buildDatabaseContextWithParams(QueryAnalysisService.QueryParams params) {
        StringBuilder context = new StringBuilder();
        
        try {
            // Используем AI-генерированные параметры
            Pageable pageable = PageRequest.of(0, params.getLimit());
            
            /*
             * БЛОК 1: Умная выборка записей обслуживания оборудования
             */
            if (equipmentMaintenanceRepository != null) {
                List<EquipmentMaintenanceRecord> maintenanceRecords = getMaintenanceRecords(params, pageable);
                if (!maintenanceRecords.isEmpty()) {
                    context.append("Данные о ремонтах оборудования (найдено ").append(maintenanceRecords.size()).append(" записей):\n");
                    for (EquipmentMaintenanceRecord record : maintenanceRecords) {
                        context.append(String.format("ID:%d | Code:%s | Machine:%s | Node:%s | Problem:%s | Status:%s | Downtime:%s | TTR:%s | FailureType:%s | Area:%s | Date:%s | Maintainers:%s | Comments:%s\n", 
                            record.getId() != null ? record.getId() : 0,
                            record.getCode() != null ? record.getCode() : "BD" + String.format("%010d", record.getId() != null ? record.getId() : 0),
                            record.getMachineName() != null ? record.getMachineName() : "Не указано",
                            record.getMechanismNode() != null ? record.getMechanismNode() : "Не указан",
                            record.getDescription() != null ? record.getDescription() : "Описание отсутствует",
                            record.getStatus() != null ? record.getStatus() : "Не указан",
                            record.getMachineDowntime() != null ? record.getMachineDowntime() : "не указано",
                            record.getTtr() != null ? record.getTtr() : "не указано",
                            record.getFailureType() != null ? record.getFailureType() : "Не указан",
                            record.getArea() != null ? record.getArea() : "Не указана",
                            record.getDate() != null ? record.getDate() : "не указана",
                            record.getMaintainers() != null ? record.getMaintainers() : "Не указаны",
                            record.getComments() != null ? record.getComments().substring(0, Math.min(100, record.getComments().length())) + "..." : "Нет комментариев"));
                    }
                    context.append("\n");
                }
            }
            
            /*
             * БЛОК 2: Умная выборка отчетов о поломках
             */
            if (breakdownReportRepository != null) {
                List<BreakdownReport> breakdownReports = getBreakdownReports(params, pageable);
                if (!breakdownReports.isEmpty()) {
                    context.append("Отчеты о поломках (найдено ").append(breakdownReports.size()).append(" записей):\n");
                    for (BreakdownReport report : breakdownReports) {
                        context.append(String.format("Code:%s | Machine:%s | Assembly:%s | Comment:%s | Status:%s | Duration:%s min\n", 
                            report.getIdCode() != null ? report.getIdCode() : "Не указан",
                            report.getMachineName() != null ? report.getMachineName() : "Не указан",
                            report.getAssembly() != null ? report.getAssembly() : "Не указана",
                            report.getComment() != null ? report.getComment() : "Комментарий отсутствует",
                            report.getWoStatusLocalDescr() != null ? report.getWoStatusLocalDescr() : "Не указан",
                            report.getDuration() != null ? report.getDuration().toString() : "не указано"));
                    }
                    context.append("\n");
                }
            }
            
            // Поиск руководств пока отключен для упрощения
            
        } catch (Exception e) {
            // Логируем ошибки подключения к БД, но не прерываем работу
            context.append("Ошибка загрузки данных из базы: ").append(e.getMessage()).append("\n");
        }
        
        return context.toString();
    }
    
    /**
     * Получает записи обслуживания на основе параметров запроса
     */
    private List<EquipmentMaintenanceRecord> getMaintenanceRecords(QueryAnalysisService.QueryParams params, Pageable pageable) {
        // Поиск по типу неисправности
        if (params.getFailureType() != null) {
            return equipmentMaintenanceRepository.findByFailureType(params.getFailureType(), pageable);
        }
        
        // Поиск по комментариям
        if (params.getSearchComments() != null) {
            return equipmentMaintenanceRepository.findByCommentsContaining(params.getSearchComments(), pageable);
        }
        
        // Топ по времени простоя (machine_downtime) с фильтром по месяцу
        if (params.isOrderByDowntime() && params.getMonth() != null) {
            return equipmentMaintenanceRepository.findByMonthOrderByDowntime(params.getMonth(), pageable);
        } else if (params.isOrderByDowntime()) {
            return equipmentMaintenanceRepository.findTopByDowntime(pageable);
        }
        
        // Топ по TTR с фильтром по месяцу
        if (params.isOrderByTtr() && params.getMonth() != null) {
            return equipmentMaintenanceRepository.findByMonth(params.getMonth(), pageable);
        } else if (params.isOrderByTtr()) {
            return equipmentMaintenanceRepository.findTopByTtr(pageable);
        }
        
        if (params.getStatus() != null && params.getMachineKeyword() != null) {
            return equipmentMaintenanceRepository.findByMachineAndStatusWithLimit(params.getMachineKeyword(), params.getStatus(), pageable);
        } else if (params.getStatus() != null) {
            return equipmentMaintenanceRepository.findByStatusWithLimit(params.getStatus(), pageable);
        } else if (params.getMachineKeyword() != null) {
            return equipmentMaintenanceRepository.findByMachineWithLimit(params.getMachineKeyword(), pageable);
        } else {
            return equipmentMaintenanceRepository.findRecentRecords(pageable);
        }
    }
    
    /**
     * Получает отчеты о поломках на основе параметров запроса
     */
    private List<BreakdownReport> getBreakdownReports(QueryAnalysisService.QueryParams params, Pageable pageable) {
        if (params.getStatus() != null) {
            return breakdownReportRepository.findByStatusWithLimit(params.getStatus(), pageable);
        } else if (params.getMachineKeyword() != null) {
            return breakdownReportRepository.findByMachineWithLimit(params.getMachineKeyword(), pageable);
        } else {
            return breakdownReportRepository.findRecentReports(pageable);
        }
    }
    

    
    /**
     * Обработка запроса с поиском похожих случаев
     */
    public String processWithSimilarCases(String request, String machineType) {
    String prompt = SmartPromptBuilder.buildSimilarCasesPrompt(request, machineType, null, databaseSearchService);
        String response = ollamaService.generateResponse(prompt);
        return removeThinkTags(response);
    }
    
    /**
     * Проверяет, является ли запрос общим
     */
    private boolean isGeneralQuery(String request) {
        String lower = request.toLowerCase();
        return lower.contains("кто ты") || lower.contains("что умеешь") || 
               lower.contains("что можешь") || lower.contains("привет");
    }
    
    /**
     * Универсальное определение запросов на инструкции по ремонту
     */
    private boolean isRepairInstructionRequest(String request) {
        String lower = request.toLowerCase();
        
        // Прямые указания на проблемы
        if (lower.matches(".*(\u043d\u0435 \u0440\u0430\u0431\u043e\u0442\u0430\u0435\u0442|\u043d\u0435\u0438\u0441\u043f\u0440\u0430\u0432\u043d\u043e\u0441\u0442\u044c|\u043f\u043e\u043b\u043e\u043c\u043a\u0430|\u0447\u0442\u043e \u0434\u0435\u043b\u0430\u0442\u044c|\u043a\u0430\u043a \u0443\u0441\u0442\u0440\u0430\u043d\u0438\u0442\u044c|\u043a\u0430\u043a \u043f\u043e\u0447\u0438\u043d\u0438\u0442\u044c|\u0443\u0442\u0435\u0447\u043a\u0430|\u043d\u0435 \u043d\u0430\u043a\u043b\u0430\u0434\u044b\u0432\u0430\u0435\u0442\u0441\u044f|\u043d\u0435 \u043a\u0440\u0443\u0442\u0438\u0442\u0441\u044f|\u043d\u0435 \u0437\u0430\u043f\u0443\u0441\u043a\u0430\u0435\u0442\u0441\u044f).*")) {
            return true;
        }
        
        // В\u043e\u043f\u0440\u043e\u0441\u044b \u0441 \u043e\u043f\u0438\u0441\u0430\u043d\u0438\u0435\u043c \u043f\u0440\u043e\u0431\u043b\u0435\u043c\u044b
        if (request.endsWith("?") && (lower.contains("о\u0431\u043e\u0440\u0443\u0434\u043e\u0432\u0430\u043d") || lower.contains("с\u0442\u0430\u043d\u043e\u043a") || lower.contains("м\u0430\u0448\u0438\u043d"))) {
            return true;
        }
        
        // \u0417\u0430\u043f\u0440\u043e\u0441\u044b \u043d\u0430 \u0438\u043d\u0441\u0442\u0440\u0443\u043a\u0446\u0438\u0438
        return lower.contains("и\u043d\u0441\u0442\u0440\u0443\u043a\u0446\u0438\u0430") || lower.contains("п\u0440\u043e\u0431\u043b\u0435\u043c\u0430");
    }
    
    private String handleGeneralQuery(String request) {
        String lower = request.toLowerCase();
        if (lower.contains("кто ты")) {
            return "Я Kvant AI - эксперт по ремонту промышленного оборудования. Моя специализация - анализ данных о ремонтах и диагностика неисправностей.";
        }
        if (lower.contains("что умеешь") || lower.contains("что можешь")) {
            return "🔧 Я умею:\n• Анализировать данные о ремонтах\n• Давать инструкции по устранению неисправностей\n• Находить статистику по оборудованию\n• Помогать с диагностикой проблем";
        }
        if (lower.contains("привет")) {
            return "👋 Привет! Я Kvant AI - ваш помощник по ремонту оборудования. Опишите проблему с оборудованием или задайте вопрос о статистике ремонтов.";
        }
        return "🔧 Я Kvant AI - эксперт по ремонту оборудования. Опишите проблему с оборудованием, и я помогу её устранить!";
    }
    
    /**
     * Получает контекст из БД для инструкций по ремонту
     */
    private String buildRepairInstructionContext(String request) {
        StringBuilder context = new StringBuilder();
        try {
            String keyword = extractKeyword(request);
            if (keyword != null && summaryOfSolutionsRepository != null) {
                List<SummaryOfSolutions> summaryMatches = summaryOfSolutionsRepository.searchByKeyword(keyword);
                if (summaryMatches != null && !summaryMatches.isEmpty()) {
                    context.append("НАЙДЕННЫЕ РЕШЕНИЯ ИЗ БАЗЫ СЛОЖНЫХ РЕМОНТОВ (цитируй эти меры и комментарии в ответе):\n");
                    summaryMatches.stream().limit(5).forEach(s -> {
                        context.append(String.format("• Оборудование: %s\n", s.getEquipment()));
                        context.append(String.format("  Узел: %s\n", s.getNode()));
                        context.append(String.format("  Описание: %s\n", s.getNotes_on_the_operation_of_the_equipment()));
                        context.append(String.format("  Меры (цитировать!): %s\n", s.getMeasures_taken()));
                        context.append(String.format("  Комментарии (цитировать!): %s\n\n", s.getComments()));
                    });
                    context.append("\nИспользуй найденные меры и комментарии из базы выше в ответе для пользователя. Не придумывай общих советов, а только цитируй найденные решения.\n");
                    return context.toString();
                }
            }
            // Если не найдено — ищем в EquipmentMaintenanceRecord
            if (keyword != null && equipmentMaintenanceRepository != null) {
                List<EquipmentMaintenanceRecord> records = equipmentMaintenanceRepository
                    .findByKeyword(keyword, PageRequest.of(0, 10));
                if (!records.isEmpty()) {
                    context.append("Реальные случаи ремонта из базы данных:\n");
                    for (EquipmentMaintenanceRecord record : records) {
                        context.append(String.format("• Оборудование: %s\n",
                            record.getMachineName() != null ? record.getMachineName() : "Неуказано"));
                        context.append(String.format("  Проблема: %s\n",
                            record.getDescription() != null ? record.getDescription() : "Не указана"));
                        context.append(String.format("  Решение: %s\n",
                            record.getComments() != null ? record.getComments() : "Не указано"));
                        context.append(String.format("  Статус: %s\n",
                            record.getStatus() != null ? record.getStatus() : "Не указан"));
                        context.append("\n");
                    }
                } else {
                    context.append("По ключевому слову '").append(keyword).append("' не найдено случаев ремонта в базе данных.\n");
                }
            } else {
                context.append("Не удалось определить ключевое слово для поиска в базе данных.\n");
            }
        } catch (Exception e) {
            context.append("Ошибка подключения к базе данных: ").append(e.getMessage()).append("\n");
        }
        return context.toString();
    }
    
    /**
     * Создает промпт для инструкций по ремонту с данными из БД
     */
    private String buildRepairPromptWithData(String request, String dbContext) {
        if (dbContext == null || dbContext.trim().isEmpty()) {
            return "По данному запросу нет данных в базе. Обратитесь к специалисту по ремонту.";
        }
        
        return String.format("""
            Ты Kvant AI - эксперт по ремонту промышленного оборудования.
            Отвечай ТОЛЬКО на русском языке!
            
            КРИТИЧНО ВАЖНО:
            - Используй ТОЛЬКО данные из базы ниже
            - НЕ придумывай информацию
            - Основывайся на реальных случаях ремонта
            
            ДАННЫЕ ИЗ БАЗЫ:
            %s
            
            Запрос: %s
            
            Ответь на основе данных выше. Если нет данных - скажи об этом.
            """, dbContext, request);
    }
    
    /**
     * Извлекает ключевое слово для поиска в БД
     */
    private String extractKeyword(String request) {
        String lower = request.toLowerCase();
        
        // Приоритет комбинациям слов
        if (lower.contains("утечка") && lower.contains("азот")) return "азот";
        if (lower.contains("утечка") && lower.contains("масл")) return "масл";
        if (lower.contains("форматор")) return "форматор";
        if (lower.contains("vmi")) return "vmi";
        if (lower.contains("протектор")) return "протектор";
        if (lower.contains("температур")) return "температур";
        if (lower.contains("конус")) return "конус";
        
        // Отдельные ключевые слова
        if (lower.contains("утечка")) return "утечка";
        if (lower.contains("азот")) return "азот";
        if (lower.contains("масл")) return "масл";
        if (lower.contains("пар")) return "пар";
        if (lower.contains("воздух")) return "воздух";
        if (lower.contains("насос")) return "насос";
        if (lower.contains("двигатель")) return "двигатель";
        
        return null;
    }
    
    /**
     * Удаляет теги размышлений из ответа AI
     */
    private String removeThinkTags(String response) {
        if (response == null) return null;
        
        // Удаляем все содержимое между <think> и </think>
        return response.replaceAll("(?s)<think>.*?</think>", "").trim();
    }
}