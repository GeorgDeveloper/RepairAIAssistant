package ru.georgdeveloper.assistantcore.service;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Новый AI ассистент на основе LangChain4j
 * Заменяет сложную логику старого RepairAssistantService
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LangChainAssistantService {

    private final ChatLanguageModel chatModel;
    private final VectorStoreService vectorStoreService;

    // Шаблоны промптов
    private static final PromptTemplate REPAIR_INSTRUCTION_TEMPLATE = PromptTemplate.from("""
            Ты Kvant AI - эксперт по ремонту промышленного оборудования.
            
            КОНТЕКСТ ИЗ БАЗЫ ЗНАНИЙ:
            {{context}}
            
            ЗАПРОС ПОЛЬЗОВАТЕЛЯ: {{query}}
            
            ИНСТРУКЦИИ:
            1. Используй ТОЛЬКО информацию из контекста выше
            2. Давай конкретные технические советы
            3. Если в контексте нет релевантной информации, честно скажи об этом
            4. Отвечай на русском языке
            5. Структурируй ответ пошагово
            
            ОТВЕТ:
            """);

    private static final PromptTemplate STATISTICS_TEMPLATE = PromptTemplate.from("""
            Ты Kvant AI - аналитик данных по ремонту оборудования.
            
            ДАННЫЕ ИЗ БАЗЫ:
            {{context}}
            
            ЗАПРОС: {{query}}
            
            ЗАДАЧА:
            1. Проанализируй предоставленные данные
            2. Дай статистический анализ
            3. Выдели ключевые тенденции
            4. Предложи рекомендации
            5. Отвечай на русском языке
            
            АНАЛИЗ:
            """);

    private static final PromptTemplate GENERAL_TEMPLATE = PromptTemplate.from("""
            Ты Kvant AI - помощник по ремонту промышленного оборудования.
            
            НАЙДЕННАЯ ИНФОРМАЦИЯ:
            {{context}}
            
            ВОПРОС: {{query}}
            
            Отвечай как эксперт по ремонту оборудования. Используй найденную информацию для ответа.
            Если информации недостаточно, дай общие рекомендации по теме ремонта оборудования.
            
            ОТВЕТ:
            """);

    /**
     * Главный метод обработки запросов пользователей
     */
    public String processQuery(String userQuery) {
        try {
            log.info("Обработка запроса: {}", userQuery);

            // Определяем тип запроса
            QueryType queryType = classifyQuery(userQuery);
            log.debug("Тип запроса: {}", queryType);

            // Выполняем семантический поиск
            List<TextSegment> relevantSegments = vectorStoreService.searchSimilar(userQuery, 10);
            String context = buildContext(relevantSegments);

            // Генерируем ответ в зависимости от типа запроса
            String response = switch (queryType) {
                case REPAIR_INSTRUCTION -> generateRepairInstruction(userQuery, context);
                case STATISTICS -> generateStatisticsAnalysis(userQuery, context);
                case GENERAL -> generateGeneralResponse(userQuery, context);
            };

            log.info("Ответ сгенерирован, длина: {} символов", response.length());
            return response;

        } catch (Exception e) {
            log.error("Ошибка обработки запроса: {}", userQuery, e);
            return "Извините, произошла ошибка при обработке вашего запроса. Попробуйте переформулировать вопрос.";
        }
    }

    /**
     * Обработка запроса с фильтрацией по типу данных
     */
    public String processQueryWithFilter(String userQuery, String dataType) {
        try {
            List<TextSegment> relevantSegments = vectorStoreService.searchByType(userQuery, dataType, 10);
            String context = buildContext(relevantSegments);

            QueryType queryType = classifyQuery(userQuery);
            return switch (queryType) {
                case REPAIR_INSTRUCTION -> generateRepairInstruction(userQuery, context);
                case STATISTICS -> generateStatisticsAnalysis(userQuery, context);
                case GENERAL -> generateGeneralResponse(userQuery, context);
            };

        } catch (Exception e) {
            log.error("Ошибка обработки запроса с фильтром: {}", userQuery, e);
            return "Ошибка обработки запроса.";
        }
    }

    /**
     * Классификация типа запроса
     */
    private QueryType classifyQuery(String query) {
        String lowerQuery = query.toLowerCase();

        // Запросы на инструкции по ремонту
        if (lowerQuery.contains("как починить") || 
            lowerQuery.contains("как устранить") ||
            lowerQuery.contains("инструкция") ||
            lowerQuery.contains("что делать") ||
            lowerQuery.contains("утечка") ||
            lowerQuery.contains("не работает") ||
            lowerQuery.contains("поломка")) {
            return QueryType.REPAIR_INSTRUCTION;
        }

        // Статистические запросы
        if (lowerQuery.contains("статистика") ||
            lowerQuery.contains("сколько") ||
            lowerQuery.contains("топ") ||
            lowerQuery.contains("самые") ||
            lowerQuery.contains("частые") ||
            lowerQuery.contains("анализ") ||
            lowerQuery.contains("тенденции")) {
            return QueryType.STATISTICS;
        }

        // Общие запросы
        return QueryType.GENERAL;
    }

    /**
     * Генерация инструкции по ремонту
     */
    private String generateRepairInstruction(String query, String context) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("query", query);
        variables.put("context", context.isEmpty() ? "Релевантная информация не найдена в базе знаний." : context);

        Prompt prompt = REPAIR_INSTRUCTION_TEMPLATE.apply(variables);
        return chatModel.generate(prompt.text());
    }

    /**
     * Генерация статистического анализа
     */
    private String generateStatisticsAnalysis(String query, String context) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("query", query);
        variables.put("context", context.isEmpty() ? "Данные для анализа не найдены." : context);

        Prompt prompt = STATISTICS_TEMPLATE.apply(variables);
        return chatModel.generate(prompt.text());
    }

    /**
     * Генерация общего ответа
     */
    private String generateGeneralResponse(String query, String context) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("query", query);
        variables.put("context", context.isEmpty() ? "Дополнительная информация не найдена." : context);

        Prompt prompt = GENERAL_TEMPLATE.apply(variables);
        return chatModel.generate(prompt.text());
    }

    /**
     * Формирование контекста из найденных сегментов
     */
    private String buildContext(List<TextSegment> segments) {
        if (segments.isEmpty()) {
            return "";
        }

        return segments.stream()
                .map(segment -> {
                    String type = segment.metadata().getString("type");
                    String content = segment.text();
                    return String.format("[%s] %s", type.toUpperCase(), content);
                })
                .collect(Collectors.joining("\n\n---\n\n"));
    }

    /**
     * Типы запросов для классификации
     */
    private enum QueryType {
        REPAIR_INSTRUCTION,  // Инструкции по ремонту
        STATISTICS,          // Статистические запросы
        GENERAL             // Общие вопросы
    }
}
