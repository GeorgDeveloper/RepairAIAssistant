package ru.georgdeveloper.assistantcore.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Универсальный анализатор запросов с улучшенным пониманием намерений
 */
@Service
public class UniversalQueryAnalyzer {
    
    @Autowired
    private OllamaService ollamaService;
    
    /**
     * Определяет тип запроса и намерение пользователя
     */
    public QueryIntent analyzeIntent(String userRequest) {
        String prompt = String.format("""
            Проанализируй запрос пользователя и определи его намерение.
            Ответь одним словом из списка: REPAIR_INSTRUCTION, STATISTICS, GENERAL
            
            REPAIR_INSTRUCTION - если пользователь описывает проблему с оборудованием или просит инструкцию
            STATISTICS - если пользователь хочет получить данные, статистику, найти записи
            GENERAL - если это общий вопрос о системе
            
            Примеры:
            "Не накладывается протектор на сборочном станке VMI" → REPAIR_INSTRUCTION
            "Утечка масла что делать" → REPAIR_INSTRUCTION  
            "Топ 5 ремонтов" → STATISTICS
            "Кто ты" → GENERAL
            
            Запрос: %s
            """, userRequest);
        
        String response = ollamaService.generateResponse(prompt).trim().toUpperCase();
        
        try {
            return QueryIntent.valueOf(response);
        } catch (IllegalArgumentException e) {
            // Fallback анализ
            return fallbackAnalysis(userRequest);
        }
    }
    
    private QueryIntent fallbackAnalysis(String request) {
        String lower = request.toLowerCase();
        
        // Признаки запроса на инструкцию
        if (lower.matches(".*(не работает|неисправность|поломка|что делать|как устранить|утечка|не накладывается|не крутится).*") ||
            (request.endsWith("?") && lower.contains("станок"))) {
            return QueryIntent.REPAIR_INSTRUCTION;
        }
        
        // Признаки статистического запроса
        if (lower.contains("топ") || lower.contains("статистика") || lower.contains("найди") || 
            lower.contains("сколько") || lower.contains("частые")) {
            return QueryIntent.STATISTICS;
        }
        
        return QueryIntent.GENERAL;
    }
    
    /**
     * Извлекает ключевые термины из описания проблемы
     */
    public String[] extractProblemKeywords(String problemDescription) {
        String prompt = String.format("""
            Извлеки ключевые технические термины из описания проблемы.
            Верни только важные слова через запятую (оборудование, узлы, действия).
            
            Пример: "Не накладывается протектор на сборочном станке VMI"
            Ответ: протектор, накладывается, сборочный, станок, VMI
            
            Описание: %s
            """, problemDescription);
        
        String response = ollamaService.generateResponse(prompt);
        return response.split(",\\s*");
    }
    
    public enum QueryIntent {
        REPAIR_INSTRUCTION,
        STATISTICS, 
        GENERAL
    }
}