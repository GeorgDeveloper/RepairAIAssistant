package ru.georgdeveloper.assistantcore.service;

/**
 * Централизованный билдер системных промптов для Kvant AI.
 * Адаптирован под использование контекста из векторной БД и домен ремонта.
 */
public final class SmartPromptBuilder {

    private SmartPromptBuilder() {}

    private static final String SYSTEM_PROMPT = """
            СИСТЕМА: Ты — Kvant AI, эксперт по ремонту и обслуживанию промышленного оборудования.
            Твоя специализация: анализ данных о ремонтах, диагностика неисправностей, пошаговые инструкции по устранению проблем.

            КЛЮЧЕВЫЕ ПРИНЦИПЫ:
            • Используй ТОЛЬКО предоставленные данные - не домысливай!
            • Отвечай конкретно и по делу - без лишних слов
            • Структурируй ответ с помощью нумерации и эмодзи
            • Указывай конкретные названия оборудования из контекста
            • При недостатке данных - честно об этом сообщай
            """;

    public static String buildRepair(String context, String query) {
        String safeContext = (context == null || context.isBlank())
                ? "По вашему запросу не найдено специфичных данных в базе ремонтов."
                : context;

        return SYSTEM_PROMPT + "\n\n" +
                "📁 ДАННЫЕ ИЗ БАЗЫ РЕМОНТОВ:\n" + safeContext + "\n\n" +
                "❓ ВОПРОС ПОЛЬЗОВАТЕЛЯ: " + query + "\n\n" +
                "🎯 ЗАДАНИЕ: Проанализируй проблему и дай пошаговую инструкцию (5-8 пунктов).\n" +
                "Обязательно укажи конкретное оборудование, если оно упоминается в данных.\n\n" +
                "🔧 ОТВЕТ:";
    }

    public static String buildStatistics(String context, String query) {
        String safeContext = (context == null || context.isBlank())
                ? "Недостаточно данных для полноценного статистического анализа."
                : context;

        return SYSTEM_PROMPT + "\n\n" +
                "📈 ДАННЫЕ ДЛЯ АНАЛИЗА:\n" + safeContext + "\n\n" +
                "❓ ЗАПРОС НА АНАЛИЗ: " + query + "\n\n" +
                "🎯 ЗАДАНИЕ: Проведи статистический анализ и предоставь:\n" +
                "1️⃣ Ключевые показатели и тренды\n" +
                "2️⃣ Частоту и причины проблем\n" +
                "3️⃣ Практические рекомендации (3-4 пункта)\n\n" +
                "📊 ОТВЕТ:";
    }

    public static String buildGeneral(String context, String query, boolean useContext) {
        String contextBlock = useContext && context != null && !context.isBlank()
                ? "📁 ДОСТУПНАЯ ИНФОРМАЦИЯ:\n" + context + "\n\n"
                : "";

        return SYSTEM_PROMPT + "\n\n" +
                contextBlock +
                "❓ ВОПРОС ПОЛЬЗОВАТЕЛЯ: " + query + "\n\n" +
                "🎯 ЗАДАНИЕ: Ответь конкретно и по делу (до 5-6 предложений).\n" +
                "Если в доступной информации нет ответа - честно об этом сообщи.\n\n" +
                "🤖 ОТВЕТ:";
    }
}


