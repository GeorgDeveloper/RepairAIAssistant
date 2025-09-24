package ru.georgdeveloper.assistantcore.client;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import ru.georgdeveloper.assistantcore.config.AssistantProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Клиент для взаимодействия с Ollama AI API.
 * 
 * Отвечает за:
 * - Отправку HTTP запросов к Ollama серверу (localhost:11434)
 * - Форматирование JSON запросов для deepseek-r1:latest модели
 * - Парсинг JSON ответов и фильтрацию технических размышлений
 * - Обработку ошибок подключения
 * 
 * Особенности работы с deepseek-r1:latest:
 * - Модель может включать размышления в тегах <think>
 * - Необходима фильтрация этих тегов для получения чистого ответа
 * - Поддержка различных форматов экранирования Unicode
 */
@Component
public class OllamaClient {
    
    private static final Logger logger = LoggerFactory.getLogger(OllamaClient.class);
    
    // HTTP клиент для отправки запросов к Ollama API
    @Autowired
    private RestTemplate restTemplate;
    
    // Конфигурация приложения (адрес Ollama, модель AI)
    @Autowired
    private AssistantProperties properties;
    
    /**
     * Отправляет запрос к Ollama AI и обрабатывает ответ.
     * 
     * Последовательность обработки:
     * 1. Экранирование спецсимволов для JSON
     * 2. Формирование JSON запроса с параметрами
     * 3. Отправка HTTP POST запроса
     * 4. Парсинг JSON ответа
     * 5. Фильтрация технических размышлений deepseek-r1:latest
     * 6. Обратное экранирование спецсимволов
     * 
     * @param prompt Промпт для AI с контекстом и инструкциями
     * @return Очищенный ответ AI без технических размышлений
     */
    public String generateResponse(String prompt) {
        try {
            // Получаем конфигурацию Ollama сервера и модели с fallback значениями
            String baseUrl = "http://localhost:11434/api";
            String model = "mistral:latest";
            
            if (properties != null && properties.getOllama() != null) {
                if (properties.getOllama().getUrl() != null && !properties.getOllama().getUrl().trim().isEmpty()) {
                    baseUrl = properties.getOllama().getUrl();
                }
                if (properties.getOllama().getModel() != null && !properties.getOllama().getModel().trim().isEmpty()) {
                    model = properties.getOllama().getModel();
                }
            }
            
            String url = baseUrl + "/generate";
            
            /*
             * Экранирование спецсимволов для корректного JSON
             * Особенно важно для кириллицы и многострочных текстов
             */
            String escapedPrompt = prompt.replace("\\", "\\\\")
                                        .replace("\"", "\\\"")
                                        .replace("\n", "\\n")
                                        .replace("\r", "\\r")
                                        .replace("\t", "\\t");
            
            /*
             * Формирование JSON запроса для Ollama API
             * Параметры:
             * - model: имя модели (deepseek-r1:latest)
             * - prompt: обрабатываемый текст
             * - stream: false - получаем полный ответ сразу
             * - temperature: 0.7 - баланс между точностью и креативностью
             */
            String requestBody = "{" +
                "\"model\": \"" + model + "\"," +
                "\"prompt\": \"" + escapedPrompt + "\"," +
                "\"stream\": false," +
                "\"options\": {\"temperature\": 0.7}" +
                "}";
            
            logger.debug("Ollama request: {}", requestBody); // Отладочный вывод
            
            // Настройка HTTP заголовков для JSON API
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
            headers.set("Accept", "application/json");
            
            org.springframework.http.HttpEntity<String> entity = new org.springframework.http.HttpEntity<>(requestBody, headers);
            
            // Отправка запроса к Ollama серверу
            String response = restTemplate.postForObject(url, entity, String.class);
            logger.debug("Ollama response: {}", response); // Отладочный вывод
            
            /*
             * Парсинг JSON ответа от Ollama
             * Ожидаемый формат: {"response": "текст ответа", ...}
             */
            if (response != null && response.contains("\"response\":")) {
                // Находим поле "response" в JSON
                int responseStart = response.indexOf("\"response\":");
                if (responseStart != -1) {
                    responseStart += 11; // Пропускаем '"response":'
                    // Пропускаем пробелы и открывающую кавычку
                    while (responseStart < response.length() && 
                           (response.charAt(responseStart) == ' ' || response.charAt(responseStart) == '"')) {
                        responseStart++;
                    }
                    
                    // Находим закрывающую кавычку перед следующим полем
                    int responseEnd = response.indexOf("\",\"", responseStart);
                    if (responseEnd == -1) {
                        responseEnd = response.lastIndexOf('"');
                    }
                    
                    if (responseEnd > responseStart) {
                        // Извлекаем текст ответа и обратно экранируем
                        String result = response.substring(responseStart, responseEnd)
                                              .replace("\\\"", "\"")
                                              .replace("\\n", "\n")
                                              .replace("\\r", "\r")
                                              .replace("\\t", "\t")
                                              .replace("\\\\", "\\");
                        
                        /*
                         * Фильтрация технических размышлений deepseek-r1:latest
                         * Модель может включать свои рассуждения в теги <think>
                         * Нужно удалять их для получения чистого ответа
                         */
                        result = result.replaceAll("<think>.*?</think>", "") // Обычные теги
                                      .replaceAll("u003cthink\\u003e.*?u003c/think\\u003e", "") // Экранированные теги
                                      .replaceAll("\\u003cthink\\u003e.*?\\u003c/think\\u003e", "") // Unicode экранирование
                                      .trim(); // Удаляем лишние пробелы
                        
                        return result;
                    }
                }
            }
            
            // Если не удалось распарсить ответ
            return "Ошибка обработки ответа от AI: " + response;
            
        } catch (Exception e) {
            // Логирование ошибок для отладки
            logger.error("Ollama API error: {}", e.getMessage(), e);
            return "Ошибка подключения к Ollama: " + e.getMessage();
        }
    }
}