package ru.georgdeveloper.assistantcore.service;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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
    // (removed legacy template in favor of SmartPromptBuilder)

    // (removed legacy template in favor of SmartPromptBuilder)

    // (removed legacy template in favor of SmartPromptBuilder)

    /**
     * Главный метод обработки запросов пользователей
     */
    public String processQuery(String userQuery) {
        try {
            log.info("Обработка запроса: {}", userQuery);

            // Определяем тип запроса
            QueryType queryType = classifyQuery(userQuery);
            log.debug("Тип запроса: {}", queryType);

            // Классификация: общий/ремонтный/статистика уже выполнена выше (эвристики)
            // Смарт-поиск: гибрид + метаданные (пока без фильтров)
            List<TextSegment> relevantSegments = vectorStoreService.searchSmart(userQuery, 10);
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
            List<TextSegment> relevantSegments = vectorStoreService.searchSmart(userQuery, 10, Map.of("type", dataType));
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
        try {
            String promptText = SmartPromptBuilder.buildRepair(
                    context.isEmpty() ? "" : context,
                    query
            );
            return chatModel.generate(promptText);
        } catch (Exception e) {
            log.error("Ошибка генерации инструкции по ремонту: {}", e.getMessage());
            return generateFallbackRepairResponse(query, context);
        }
    }

    /**
     * Генерация статистического анализа
     */
    private String generateStatisticsAnalysis(String query, String context) {
        try {
            String promptText = SmartPromptBuilder.buildStatistics(
                    context.isEmpty() ? "" : context,
                    query
            );
            return chatModel.generate(promptText);
        } catch (Exception e) {
            log.error("Ошибка генерации статистического анализа: {}", e.getMessage());
            return generateFallbackStatisticsResponse(query, context);
        }
    }

    /**
     * Генерация общего ответа
     */
    private String generateGeneralResponse(String query, String context) {
        try {
            String lowerQuery = query == null ? "" : query.toLowerCase();
            if (lowerQuery.contains("кто ты") || lowerQuery.contains("что ты") || lowerQuery.contains("who are you") || lowerQuery.matches(".*\\b(привет|здравствуй|hello|hi)\\b.*")) {
                return "Привет! Я Kvant AI — ваш помощник по ремонту промышленного оборудования.\n\n" +
                        "Помогаю с: \n" +
                        "• диагностикой неисправностей\n" +
                        "• инструкциями по ремонту\n" +
                        "• анализом данных оборудования\n" +
                        "• техническими рекомендациями";
            }

            boolean isRepairIntent = lowerQuery.contains("утечка") || lowerQuery.contains("ремонт") || lowerQuery.contains("не работает") || lowerQuery.contains("поломка") || lowerQuery.contains("как починить") || lowerQuery.contains("как устранить") || lowerQuery.contains("инструкция") || lowerQuery.contains("что делать");
            String promptText = SmartPromptBuilder.buildGeneral(
                    context,
                    query,
                    isRepairIntent
            );
            return chatModel.generate(promptText);
        } catch (Exception e) {
            log.error("Ошибка генерации общего ответа: {}", e.getMessage());
            return generateFallbackGeneralResponse(query, context);
        }
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
     * Fallback ответ для инструкций по ремонту
     */
    private String generateFallbackRepairResponse(String query, String context) {
        StringBuilder response = new StringBuilder();
        response.append("🔧 **Помощь с ремонтом**\n\n");
        
        if (!context.isEmpty()) {
            response.append("Найдена похожая информация из базы знаний:\n\n");
            // Показываем только первые 2-3 записи для краткости
            String[] contextParts = context.split("---");
            int maxParts = Math.min(2, contextParts.length);
            for (int i = 0; i < maxParts; i++) {
                response.append(contextParts[i].trim()).append("\n\n");
            }
        }
        
        response.append("**Рекомендации по ремонту:**\n");
        response.append("1. Проверьте основные компоненты системы\n");
        response.append("2. Убедитесь в правильности подключения\n");
        response.append("3. Проверьте состояние расходных материалов\n");
        response.append("4. При необходимости обратитесь к техническому специалисту\n\n");
        
        response.append("⚠️ *Для получения более точных инструкций обратитесь к эксперту по ремонту.*");
        
        return response.toString();
    }
    
    /**
     * Fallback ответ для статистического анализа
     */
    private String generateFallbackStatisticsResponse(String query, String context) {
        StringBuilder response = new StringBuilder();
        response.append("📊 **Статистический анализ**\n\n");
        
        if (!context.isEmpty()) {
            response.append("**Доступные данные:**\n");
            response.append(context).append("\n\n");
        }
        
        response.append("**Общие рекомендации по анализу:**\n");
        response.append("1. Изучите предоставленные данные\n");
        response.append("2. Выделите ключевые показатели\n");
        response.append("3. Обратите внимание на тренды и аномалии\n");
        response.append("4. Сформулируйте выводы на основе данных\n\n");
        
        response.append("⚠️ *AI-модель временно недоступна. Рекомендуется ручной анализ данных.*");
        
        return response.toString();
    }
    
    /**
     * Fallback ответ для общих вопросов
     */
    private String generateFallbackGeneralResponse(String query, String context) {
        StringBuilder response = new StringBuilder();
        
        // Простой ответ без технических деталей
        if (query.toLowerCase().contains("кто ты") || query.toLowerCase().contains("что ты")) {
            response.append("Привет! Я Kvant AI - ваш помощник по ремонту промышленного оборудования.\n\n");
            response.append("Я могу помочь с:\n");
            response.append("• Диагностикой неисправностей\n");
            response.append("• Инструкциями по ремонту\n");
            response.append("• Анализом данных оборудования\n");
            response.append("• Техническими рекомендациями\n\n");
            response.append("К сожалению, сейчас у меня ограниченные возможности. ");
            response.append("Попробуйте переформулировать вопрос или обратитесь к техническому специалисту.");
            return response.toString();
        }
        
        // Для вопросов о ремонте
        if (query.toLowerCase().contains("утечка") || query.toLowerCase().contains("ремонт") || 
            query.toLowerCase().contains("не работает") || query.toLowerCase().contains("поломка")) {
            response.append("🔧 **Помощь с ремонтом**\n\n");
            
            if (!context.isEmpty()) {
                response.append("Найдена похожая информация из базы знаний:\n\n");
                // Показываем только первые 2-3 записи для краткости
                String[] contextParts = context.split("---");
                int maxParts = Math.min(3, contextParts.length);
                for (int i = 0; i < maxParts; i++) {
                    response.append(contextParts[i].trim()).append("\n\n");
                }
            }
            
            response.append("**Рекомендации:**\n");
            response.append("1. Проверьте основные компоненты системы\n");
            response.append("2. Убедитесь в правильности подключения\n");
            response.append("3. Проверьте состояние расходных материалов\n");
            response.append("4. При необходимости обратитесь к техническому специалисту\n\n");
            
            response.append("⚠️ *Для получения более точных рекомендаций обратитесь к эксперту по ремонту.*");
            return response.toString();
        }
        
        // Общий ответ
        response.append("🤖 **Kvant AI**\n\n");
        response.append("Я ваш помощник по ремонту промышленного оборудования.\n\n");
        
        if (!context.isEmpty()) {
            response.append("Найдена релевантная информация:\n");
            response.append(context.substring(0, Math.min(500, context.length())));
            if (context.length() > 500) {
                response.append("...");
            }
            response.append("\n\n");
        }
        
        response.append("К сожалению, сейчас у меня ограниченные возможности. ");
        response.append("Попробуйте переформулировать вопрос или обратитесь к техническому специалисту.");
        
        return response.toString();
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
