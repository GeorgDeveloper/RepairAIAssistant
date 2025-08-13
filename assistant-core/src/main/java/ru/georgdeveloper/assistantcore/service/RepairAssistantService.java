package ru.georgdeveloper.assistantcore.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.georgdeveloper.assistantcore.repository.RepairRecordRepository;
import ru.georgdeveloper.assistantcore.repository.EquipmentMaintenanceRepository;
import ru.georgdeveloper.assistantcore.repository.BreakdownReportRepository;
import ru.georgdeveloper.assistantcore.model.RepairRecord;
import ru.georgdeveloper.assistantcore.model.EquipmentMaintenanceRecord;
import ru.georgdeveloper.assistantcore.model.BreakdownReport;
import ru.georgdeveloper.assistantcore.model.PmMonitor;
import ru.georgdeveloper.assistantcore.repository.PmMonitorRepository;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Основной сервис для обработки запросов по ремонту оборудования.
 * Интегрирует данные из базы данных с AI-анализом через Ollama.
 * 
 * Функциональность:
 * - Получение данных о ремонтах из различных таблиц БД
 * - Формирование контекста для AI на основе реальных данных
 * - Обработка запросов пользователей через deepseek-r1 модель
 * - Поиск релевантной информации в руководствах и инструкциях
 */
@Service
public class RepairAssistantService {
    
    // Сервис для взаимодействия с Ollama AI (deepseek-r1)
    @Autowired
    private OllamaService ollamaService;
    
    // Сервис для анализа PDF документов с инструкциями
    @Autowired
    private PdfAnalysisService pdfAnalysisService;
    
    // Сервис для поиска в руководствах по ремонту
    @Autowired
    private ManualStorageService manualStorageService;
    
    // Репозиторий для тестовых записей о ремонте
    @Autowired
    private RepairRecordRepository repairRecordRepository;
    
    // Репозиторий для записей обслуживания оборудования (12,888+ записей)
    @Autowired(required = false)
    private EquipmentMaintenanceRepository equipmentMaintenanceRepository;
    
    // Репозиторий для отчетов о поломках (122MB данных)
    @Autowired(required = false)
    private BreakdownReportRepository breakdownReportRepository;
    
    // Репозиторий для планового обслуживания (1,224 записи)
    @Autowired(required = false)
    private PmMonitorRepository pmMonitorRepository;
    
    /**
     * Главный метод обработки запросов пользователей.
     * 
     * Алгоритм работы:
     * 1. Извлекает релевантные данные из всех доступных таблиц БД
     * 2. Формирует контекст с реальными данными о ремонтах
     * 3. Добавляет инструкции для AI модели
     * 4. Отправляет запрос в Ollama (deepseek-r1)
     * 5. Возвращает обработанный ответ без технических размышлений
     * 
     * @param request Запрос пользователя (например: "Посчитай ремонты со статусом временно закрыто")
     * @return Ответ AI на основе реальных данных из БД
     */
    public String processRepairRequest(String request) {
        // Получаем все доступные данные из базы данных
        String dbContext = buildDatabaseContext(request);
        
        // Формируем системный промпт для AI-ассистента по работе с БД ремонтов
        String prompt = buildSystemPrompt(dbContext, request);
        
        // Отправляем запрос в Ollama и получаем ответ
        return ollamaService.generateResponse(prompt);
    }
    
    /**
     * Формирует контекст для AI на основе данных из базы данных.
     * 
     * Метод объединяет информацию из нескольких источников:
     * 1. equipment_maintenance_records - записи о ремонтах (12,888+ записей)
     * 2. rep_breakdownreport - отчеты о поломках (122MB данных) 
     * 3. manuals - руководства и инструкции по ремонту
     * 
     * Ограничения производительности:
     * - Загружает максимум 20 записей из каждой таблицы
     * - Использует безопасные проверки на null
     * - Обрабатывает ошибки подключения к БД
     * 
     * @param request Запрос пользователя для поиска релевантных руководств
     * @return Форматированный контекст с данными для AI
     */
    private String buildDatabaseContext(String request) {
        StringBuilder context = new StringBuilder();
        
        try {
            /*
             * БЛОК 1: Записи обслуживания оборудования
             * Таблица: equipment_maintenance_records
             * Содержит: название машины, узел, описание проблемы, статус, время простоя
             */
            if (equipmentMaintenanceRepository != null) {
                List<EquipmentMaintenanceRecord> maintenanceRecords = equipmentMaintenanceRepository.findAll();
                if (!maintenanceRecords.isEmpty()) {
                    context.append("Данные о ремонтах оборудования:\n");
                    // Ограничиваем выборку для производительности
                    for (int i = 0; i < Math.min(maintenanceRecords.size(), 20); i++) {
                        EquipmentMaintenanceRecord record = maintenanceRecords.get(i);
                        // Безопасное извлечение данных с проверкой на null
                        context.append(String.format("- %s (%s): %s (Статус: %s, Время простоя: %s)\n", 
                            record.getMachineName() != null ? record.getMachineName() : "Не указано",
                            record.getMechanismNode() != null ? record.getMechanismNode() : "",
                            record.getDescription() != null ? record.getDescription() : "Описание отсутствует",
                            record.getStatus() != null ? record.getStatus() : "Не указан",
                            record.getMachineDowntime() != null ? record.getMachineDowntime().toString() : "не указано"));
                    }
                    context.append("\n");
                }
            }
            
            /*
             * БЛОК 2: Отчеты о поломках
             * Таблица: rep_breakdownreport  
             * Содержит: машина, сборка, комментарии, статус заявки, длительность ремонта
             */
            if (breakdownReportRepository != null) {
                List<BreakdownReport> breakdownReports = breakdownReportRepository.findAll();
                if (!breakdownReports.isEmpty()) {
                    context.append("Отчеты о поломках:\n");
                    // Ограичиваем выборку из большой таблицы (122MB)
                    for (int i = 0; i < Math.min(breakdownReports.size(), 20); i++) {
                        BreakdownReport report = breakdownReports.get(i);
                        context.append(String.format("- %s (%s): %s (Статус: %s, Длительность: %s мин)\n", 
                            report.getMachineName() != null ? report.getMachineName() : "Не указан",
                            report.getAssembly() != null ? report.getAssembly() : "",
                            report.getComment() != null ? report.getComment() : "Комментарий отсутствует",
                            report.getWoStatusLocalDescr() != null ? report.getWoStatusLocalDescr() : "Не указан",
                            report.getDuration() != null ? report.getDuration().toString() : "не указано"));
                    }
                    context.append("\n");
                }
            }
            
            /*
             * БЛОК 3: Поиск релевантных руководств
             * Ищет в базе инструкций информацию, связанную с запросом пользователя
             */
            List<String> manuals = manualStorageService.searchManuals(request);
            if (!manuals.isEmpty()) {
                context.append("Инструкции и руководства:\n");
                context.append(String.join("\n", manuals));
                context.append("\n");
            }
            
        } catch (Exception e) {
            // Логируем ошибки подключения к БД, но не прерываем работу
            context.append("Ошибка загрузки данных из базы: ").append(e.getMessage()).append("\n");
        }
        
        return context.toString();
    }
    
    /**
     * Формирует системный промпт для AI-ассистента по работе с БД ремонтов.
     * Адаптирован под специфику производственных данных и задач технического обслуживания.
     */
    private String buildSystemPrompt(String dbContext, String request) {
        return "Системный промпт для AI-ассистента по работе с БД ремонтов\n\n" +
               "Роль:\n" +
               "Ты — интеллектуальный анализатор базы данных ремонтов и технического обслуживания. " +
               "Твоя задача — точно интерпретировать запросы о ремонтах, разбивать их на логические подзадачи, " +
               "находить релевантные данные в БД, анализировать информацию и предоставлять четкий, структурированный ответ.\n\n" +
               
               "Инструкции:\n\n" +
               
               "1. Анализ запроса:\n" +
               "- Определи цель запроса (поиск информации, статистика по ремонтам, сравнение данных и т.д.)\n" +
               "- Выдели ключевые сущности (названия оборудования, статусы ремонтов, даты, типы неисправностей)\n" +
               "- Разбей сложный запрос на подзадачи (например: 'Найди поломки машины X и время ремонта' → 1) Поиск поломок, 2) Анализ времени)\n\n" +
               
               "2. Поиск в БД:\n" +
               "- Используй точные совпадения по статусам (например, 'временно закрыто' ≠ 'закрыто')\n" +
               "- Если точных данных нет, предложи альтернативы (похожие статусы, оборудование) или уточни критерии\n" +
               "- Для числовых данных (время простоя, длительность) учитывай единицы измерения (часы, минуты)\n" +
               "- Различай таблицы: equipment_maintenance_records (ремонты), rep_breakdownreport (поломки), pm_monitor (плановое ТО)\n\n" +
               
               "3. Анализ данных:\n" +
               "- Сравнивай данные из разных источников, проверяй актуальность\n" +
               "- Выявляй зависимости, тренды (например, 'Какое оборудование чаще ломается?')\n" +
               "- При работе с большими объемами агрегируй информацию (суммы, средние значения, категории)\n" +
               "- Учитывай производственную специфику (смены, бригады, типы оборудования)\n\n" +
               
               "4. Формирование ответа:\n" +
               "- Структурируй ответ:\n" +
               "  • Краткий вывод (основная информация)\n" +
               "  • Детали (таблицы, списки, цифры при необходимости)\n" +
               "  • Источники (если данные взяты из конкретных таблиц)\n" +
               "- Если данных нет, сообщи об этом и предложи варианты\n" +
               "- НЕ включай технические размышления в тегах <think>\n" +
               "- Отвечай только на русском языке\n" +
               "- Давай конкретные цифры и факты\n\n" +
               
               "Контекст из БД:\n" + dbContext + "\n\n" +
               "Запрос пользователя: " + request + "\n\n" +
               "Проанализируй запрос и предоставь структурированный ответ на основе данных из БД.";
    }
}