package ru.georgdeveloper.assistantcore.service;

/**
 * Централизованный билдер системных промптов для Kvant AI.
 * Адаптирован под использование контекста из векторной БД и домен ремонта.
 */
public final class SmartPromptBuilder {

    private SmartPromptBuilder() {}

    private static final String SYSTEM_PROMPT = """
            СИСТЕМА: Ты — Kvant AI, помощник по ремонту промышленного оборудования.
            Твоя специализация: анализ данных о ремонтах, диагностика неисправностей, инструкции по устранению проблем.

            ПРАВИЛА:
            - Используй ТОЛЬКО предоставленный контекст; если его недостаточно — скажи об этом.
            - Не выдумывай детали, придерживайся терминологии из контекста.
            - Отвечай кратко и структурировано на русском языке.
            - Дай чёткие, прикладные рекомендации.
            """;

    public static String buildRepair(String context, String query) {
        String safeContext = (context == null || context.isBlank())
                ? "Релевантные данные из базы ремонтов отсутствуют."
                : context;

        return SYSTEM_PROMPT + "\n\n" +
                "КОНТЕКСТ (из базы ремонтов):\n" + safeContext + "\n\n" +
                "ЗАПРОС: " + query + "\n\n" +
                "ЗАДАНИЕ: Дай пошаговые действия (5–8 пунктов), опираясь на контекст.\n" +
                "ОТВЕТ:";
    }

    public static String buildStatistics(String context, String query) {
        String safeContext = (context == null || context.isBlank())
                ? "Данные для анализа отсутствуют."
                : context;

        return SYSTEM_PROMPT + "\n\n" +
                "ДАННЫЕ ДЛЯ АНАЛИЗА: \n" + safeContext + "\n\n" +
                "ЗАПРОС: " + query + "\n\n" +
                "ЗАДАНИЕ: Дай короткий анализ (тренды/частоты/топ-причины) и 3–4 рекомендации.\n" +
                "ОТВЕТ:";
    }

    public static String buildGeneral(String context, String query, boolean useContext) {
        String contextBlock = useContext && context != null && !context.isBlank()
                ? "КОНТЕКСТ (для ответа):\n" + context + "\n\n"
                : "";

        return SYSTEM_PROMPT + "\n\n" +
                contextBlock +
                "ВОПРОС: " + query + "\n\n" +
                "ЗАДАНИЕ: Ответь кратко (до 5–6 предложений). Если данных недостаточно — скажи об этом.\n" +
                "ОТВЕТ:";
    }
}


