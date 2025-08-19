
package ru.georgdeveloper.assistantcore.service;
import ru.georgdeveloper.assistantcore.repository.EquipmentMaintenanceRepository;
import ru.georgdeveloper.assistantcore.model.EquipmentMaintenanceRecord;
import java.util.List;

/**
 * Утилитный класс для форматирования промптов
 */
public class SmartPromptBuilder {

    /**
     * Универсальный поиск по ключевым словам, если не найдено по основным фильтрам
     */
    public static String buildKeywordFallbackPrompt(String request, EquipmentMaintenanceRepository equipmentMaintenanceRepository) {
        String[] keywords = request.split("\\s+");
        StringBuilder context = new StringBuilder();
        boolean found = false;
        for (String keyword : keywords) {
            List<EquipmentMaintenanceRecord> records = equipmentMaintenanceRepository.findByKeyword(keyword, org.springframework.data.domain.PageRequest.of(0, 5));
            if (records != null && !records.isEmpty()) {
                found = true;
                context.append("Ключевое слово: ").append(keyword).append("\n");
                for (EquipmentMaintenanceRecord record : records) {
                    context.append(String.format("• Оборудование: %s\n  Проблема: %s\n  Решение: %s\n  Статус: %s\n\n",
                        record.getMachineName(), record.getDescription(), record.getComments(), record.getStatus()));
                }
            }
        }
        if (!found) {
            context.append("По вашему запросу не найдено данных в базе даже по ключевым словам.\n");
        }
        return String.format(SYSTEM_PROMPT, context) + "\n\nВопрос пользователя: " + request + "\n\nДай подробный ответ как эксперт по ремонту оборудования:";
    }
    private static final String SYSTEM_PROMPT = """
    Ты — Kvant AI, эксперт по ремонту промышленного оборудования.
    Твоя специализация: анализ данных о ремонтах, диагностика неисправностей, инструкции по устранению проблем.
    
    ВАЖНО: Ты работаешь ТОЛЬКО с промышленным оборудованием и ремонтами. Это твоя основная задача.
    
    Данные из базы ремонтов:
    %s
    
    Правила работы:
    1. Всегда помогай с вопросами по ремонту оборудования
        2. Давай конкретные технические советы
        3. Используй данные из базы ремонтов для примеров
        4. Если нет точных данных — давай общие рекомендации по ремонту
        5. Форматируй ответы четко и понятно
        
        Никогда не отказывайся от вопросов по ремонту оборудования!
        """;

    /**
     * Формирует промпт с контекстом для статистических запросов
     */
    public static String buildStatisticsPrompt(String request, QueryAnalysisService.QueryParams params, DatabaseSearchService databaseSearchService) {
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
        if (context.length() == 0) {
            context.append("По вашему запросу не найдено данных в базе.\n");
        }
        return String.format(SYSTEM_PROMPT, context) + "\n\nВопрос пользователя: " + request + "\n\nДай подробный ответ как эксперт по ремонту оборудования:";
    }

    /**
     * Формирует промпт с похожими случаями ремонта
     */
    public static String buildSimilarCasesPrompt(String request, String machineType, String problemDescription, DatabaseSearchService databaseSearchService) {
        String searchQuery = (machineType != null ? machineType + " " : "") + (problemDescription != null ? problemDescription : "");
        DatabaseSearchService.SearchResult result = databaseSearchService.searchAll(searchQuery.trim(), 10);
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
        if (context.length() == 0) {
            context.append("По вашему запросу не найдено данных в базе.\n");
        }
        return String.format(SYSTEM_PROMPT, context) + "\n\nВопрос пользователя: " + request + "\n\nДай подробную инструкцию по ремонту:";
    }

    /**
     * Формирует промпт для инструкций по ремонту
     */
    public static String buildRepairInstructionPrompt(String request) {
        String instructionPrompt = """
            Ты — Kvant AI, эксперт по ремонту промышленного оборудования.
            Твоя задача: давать подробные инструкции по устранению неисправностей.
            
            Особенно хорошо ты разбираешься в:
            - Утечках (азота, пара, воздуха, масла)
            - Неисправностях датчиков
            - Проблемах с пневматикой
            - Механических поломках
            - Электрических неисправностях
            
            Формат ответа:
            🔧 ИНСТРУКЦИЯ ПО УСТРАНЕНИЮ
            
            1. Диагностика:
            2. Подготовка:
            3. Устранение:
            4. Проверка:
            
            ⚠️ Меры безопасности:
            
            Вопрос пользователя: %s
            
            Дай подробную инструкцию по устранению этой проблемы:
            """;
        return String.format(instructionPrompt, request);
    }
}