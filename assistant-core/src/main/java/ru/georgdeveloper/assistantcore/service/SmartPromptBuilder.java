package ru.georgdeveloper.assistantcore.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.PageRequest;
import ru.georgdeveloper.assistantcore.model.EquipmentMaintenanceRecord;
import ru.georgdeveloper.assistantcore.repository.EquipmentMaintenanceRepository;

import java.util.List;


/**
 * Сервис для создания умных промптов с контекстом из БД
 */
@Service
public class SmartPromptBuilder {
    
    @Autowired
    private EquipmentMaintenanceRepository equipmentMaintenanceRepository;
    
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
     * Создает промпт с контекстом для статистических запросов
     */
    public String buildStatisticsPrompt(String request, QueryAnalysisService.QueryParams params) {
        String context = buildStatisticsContext(params);
        return String.format(SYSTEM_PROMPT, context) + "\n\nВопрос пользователя: " + request + "\n\nДай подробный ответ как эксперт по ремонту оборудования:";
    }
    
    /**
     * Создает промпт с похожими случаями ремонта
     */
    public String buildSimilarCasesPrompt(String request, String machineType, String problemDescription) {
        String context = buildSimilarCasesContext(machineType, problemDescription);
        return String.format(SYSTEM_PROMPT, context) + "\n\nВопрос пользователя: " + request + "\n\nДай подробную инструкцию по ремонту:";
    }
    
    /**
     * Создает промпт для инструкций по ремонту
     */
    public String buildRepairInstructionPrompt(String request) {
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
    
    private String buildStatisticsContext(QueryAnalysisService.QueryParams params) {
        StringBuilder context = new StringBuilder("БАЗА ДАННЫХ РЕМОНТОВ ПРОМЫШЛЕННОГО ОБОРУДОВАНИЯ:\n\n");
        
        try {
            List<EquipmentMaintenanceRecord> records = getRecordsByParams(params);
            
            if (records.isEmpty()) {
                return "В базе данных ремонтов нет записей по указанным критериям, но ты можешь дать общие рекомендации по ремонту оборудования.";
            }
            
            // Топ по времени простоя
            if (params.isOrderByDowntime()) {
                context.append("ТОП ПО ВРЕМЕНИ ПРОСТОЯ:\n");
                records.stream()
                    .filter(r -> r.getMachineDowntime() != null)
                    .limit(params.getLimit())
                    .forEach(r -> context.append(formatRecord(r)).append("\n"));
            }
            
            // Статистика по статусам
            if (params.getStatus() != null) {
                long count = equipmentMaintenanceRepository.countByStatus(params.getStatus());
                context.append(String.format("Количество записей со статусом '%s': %d\n", params.getStatus(), count));
            }
            
            // Общая статистика
            context.append(String.format("\nВсего записей в выборке: %d\n", records.size()));
            
            // Добавляем примеры ремонтов для контекста
            context.append("\nПРИМЕРЫ РЕМОНТОВ:\n");
            records.stream().limit(3).forEach(r -> {
                context.append(String.format("- %s: %s (TTR: %s)\n", 
                    r.getMachineName() != null ? r.getMachineName() : "Неизвестно",
                    r.getDescription() != null ? r.getDescription() : "Описание отсутствует",
                    r.getTtr() != null ? r.getTtr().toString() : "Неизвестно"
                ));
            });
            
        } catch (Exception e) {
            context.append("База данных временно недоступна, но ты можешь дать рекомендации на основе общих знаний по ремонту оборудования.");
        }
        
        return context.toString();
    }
    
    private String buildSimilarCasesContext(String machineType, String problemDescription) {
        StringBuilder context = new StringBuilder("ПРИМЕРЫ РЕМОНТОВ ИЗ БАЗЫ ДАННЫХ:\n\n");
        
        try {
            List<EquipmentMaintenanceRecord> similarCases;
            
            if (machineType != null && !machineType.isEmpty()) {
                similarCases = equipmentMaintenanceRepository
                    .findByMachineWithLimit(machineType, PageRequest.of(0, 5));
            } else {
                similarCases = equipmentMaintenanceRepository
                    .findRecentRecords(PageRequest.of(0, 5));
            }
            
            if (similarCases.isEmpty()) {
                return "В базе нет точно похожих случаев, но ты можешь дать общие инструкции по ремонту на основе своих знаний.";
            }
            
            similarCases.forEach(record -> {
                context.append(formatDetailedRecord(record)).append("\n\n");
            });
            
            // Добавляем общие рекомендации
            context.append("ОБЩИЕ ПРИНЦИПЫ РЕМОНТА ОБОРУДОВАНИЯ:\n");
            context.append("- Всегда обесточь оборудование перед ремонтом\n");
            context.append("- Провести визуальный осмотр\n");
            context.append("- Использовать соответствующие СИЗ\n");
            context.append("- Проверить работу после ремонта\n\n");
            
        } catch (Exception e) {
            context.append("База данных временно недоступна, но ты можешь дать инструкции на основе общих знаний по ремонту оборудования.");
        }
        
        return context.toString();
    }
    
    private List<EquipmentMaintenanceRecord> getRecordsByParams(QueryAnalysisService.QueryParams params) {
        PageRequest pageRequest = PageRequest.of(0, params.getLimit());
        
        // Приоритет параметров
        if (params.getStatus() != null && params.getMachineKeyword() != null) {
            return equipmentMaintenanceRepository
                .findByMachineAndStatusWithLimit(params.getMachineKeyword(), params.getStatus(), pageRequest);
        }
        
        if (params.getStatus() != null) {
            return equipmentMaintenanceRepository
                .findByStatusWithLimit(params.getStatus(), pageRequest);
        }
        
        if (params.getMachineKeyword() != null) {
            return equipmentMaintenanceRepository
                .findByMachineWithLimit(params.getMachineKeyword(), pageRequest);
        }
        
        if (params.getMonth() != null) {
            if (params.isOrderByDowntime()) {
                return equipmentMaintenanceRepository
                    .findByMonthOrderByDowntime(params.getMonth(), pageRequest);
            } else {
                return equipmentMaintenanceRepository
                    .findByMonth(params.getMonth(), pageRequest);
            }
        }
        
        if (params.isOrderByDowntime()) {
            return equipmentMaintenanceRepository.findTopByDowntime(pageRequest);
        }
        
        if (params.isOrderByTtr()) {
            return equipmentMaintenanceRepository.findTopByTtr(pageRequest);
        }
        
        return equipmentMaintenanceRepository.findRecentRecords(pageRequest);
    }
    
    private String formatRecord(EquipmentMaintenanceRecord record) {
        return String.format("🔧 %s - %s (✅ %s, ⏱️ %s)",
            record.getMachineName() != null ? record.getMachineName() : "Неизвестно",
            record.getDescription() != null ? record.getDescription() : "Описание отсутствует",
            record.getStatus() != null ? record.getStatus() : "Неизвестно",
            record.getMachineDowntime() != null ? record.getMachineDowntime().toString() : "Неизвестно"
        );
    }
    
    private String formatDetailedRecord(EquipmentMaintenanceRecord record) {
        return String.format("""
            🔧 Машина: %s
            🔴 Проблема: %s
            ✅ Статус: %s
            ⏱️ Время простоя: %s
            🔧 TTR: %s
            📅 Дата: %s
            📝 Комментарии: %s
            """,
            record.getMachineName() != null ? record.getMachineName() : "Неизвестно",
            record.getDescription() != null ? record.getDescription() : "Описание отсутствует",
            record.getStatus() != null ? record.getStatus() : "Неизвестно",
            record.getMachineDowntime() != null ? record.getMachineDowntime().toString() : "Неизвестно",
            record.getTtr() != null ? record.getTtr().toString() : "Неизвестно",
            record.getDate() != null ? record.getDate() : "Неизвестно",
            record.getComments() != null ? record.getComments() : "Комментарии отсутствуют"
        );
    }
}