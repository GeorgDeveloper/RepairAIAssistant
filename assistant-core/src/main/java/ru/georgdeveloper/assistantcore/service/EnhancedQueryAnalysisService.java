package ru.georgdeveloper.assistantcore.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Расширенный сервис анализа запросов с использованием AI
 */
@Service
public class EnhancedQueryAnalysisService {
    
    @Autowired
    private OllamaService ollamaService;
    
    @Autowired
    private QueryAnalysisService queryAnalysisService;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * Анализирует запрос с использованием AI для извлечения параметров
     */
    public QueryAnalysisService.AnalysisResult analyzeWithAI(String userRequest) {
        String normalized = normalizeRequest(userRequest);
        
        // Определяем тип запроса и параметры через AI
        String prompt = buildAnalysisPrompt(normalized);
        String aiResponse = ollamaService.generateResponse(prompt);
        
        return parseAiResponse(aiResponse, normalized);
    }
    
    private String normalizeRequest(String request) {
        return request.toLowerCase()
                .replace("сколько раз", "количество")
                .replace("как починить", "инструкция по ремонту")
                .replace("как отремонтировать", "инструкция по ремонту")
                .replace("сколько времени", "продолжительность")
                .replaceAll("\\b(пожалуйста|мне|нужно|можно|хочу)\\b", "")
                .trim();
    }
    
    private String buildAnalysisPrompt(String request) {
        return String.format("""
            Проанализируй запрос и верни JSON с параметрами:
            {
                "type": "statistics|instruction|general",
                "needsDatabase": true/false,
                "dateRange": {
                    "isRange": true/false,
                    "start": "дата начала или месяц",
                    "end": "дата окончания или месяц"
                },
                "machine": "название оборудования или null",
                "status": "статус ремонта или null",
                "orderBy": "date|downtime|ttr|null",
                "limit": число или null,
                "simpleAnswer": "ответ если не требует данных"
            }
            
            Примеры:
            - "статистика за январь-март" → type: "statistics", dateRange: {"isRange": true, "start": "январь", "end": "март"}
            - "топ 5 продолжительных ремонтов" → type: "statistics", orderBy: "downtime", limit: 5
            - "инструкция по ремонту насоса" → type: "instruction", needsDatabase: false
            - "привет" → type: "general", needsDatabase: false, simpleAnswer: "Привет! Готов помочь."
            
            Запрос: %s
            """, request);
    }
    
    private QueryAnalysisService.AnalysisResult parseAiResponse(String aiResponse, String originalRequest) {
        try {
            JsonNode json = objectMapper.readTree(aiResponse);
            
            String type = json.get("type").asText();
            boolean needsDatabase = json.get("needsDatabase").asBoolean();
            
            if (!needsDatabase) {
                String simpleAnswer = json.has("simpleAnswer") ? 
                    json.get("simpleAnswer").asText() : 
                    getDefaultAnswer(type, originalRequest);
                return new QueryAnalysisService.AnalysisResult(false, simpleAnswer, null);
            }
            
            // Формируем параметры для поиска в БД
            String dataNeeded = buildDataNeeded(json);
            return new QueryAnalysisService.AnalysisResult(true, null, dataNeeded);
            
        } catch (Exception e) {
            // Если AI не смог распарсить, используем стандартный анализ
            return queryAnalysisService.analyzeRequest(originalRequest);
        }
    }
    
    private String getDefaultAnswer(String type, String request) {
        return switch (type) {
            case "instruction" -> "Для получения инструкций по ремонту обратитесь к техническому руководству или специалисту.";
            case "general" -> {
                if (request.contains("привет")) yield "Привет! Готов помочь с анализом данных о ремонте оборудования.";
                if (request.contains("кто ты")) yield "Я Kvant AI - AI-ассистент для анализа данных о ремонте промышленного оборудования.";
                yield "Я готов помочь с анализом данных о ремонте оборудования.";
            }
            default -> "Требуется анализ данных о ремонтах";
        };
    }
    
    private String buildDataNeeded(JsonNode json) {
        StringBuilder sb = new StringBuilder();
        
        if (json.has("status") && !json.get("status").isNull()) {
            sb.append("STATUS=").append(json.get("status").asText()).append("|");
        }
        
        if (json.has("machine") && !json.get("machine").isNull()) {
            sb.append("MACHINE=").append(json.get("machine").asText()).append("|");
        }
        
        if (json.has("limit") && !json.get("limit").isNull()) {
            sb.append("LIMIT=").append(json.get("limit").asInt()).append("|");
        }
        
        if (json.has("orderBy") && !json.get("orderBy").isNull()) {
            String orderBy = json.get("orderBy").asText();
            switch (orderBy) {
                case "downtime" -> sb.append("ORDER_BY_DOWNTIME=true|");
                case "ttr" -> sb.append("ORDER_BY_TTR=true|");
                case "date" -> sb.append("ORDER_BY_DATE=true|");
            }
        }
        
        if (json.has("dateRange")) {
            JsonNode dateRange = json.get("dateRange");
            if (dateRange.get("isRange").asBoolean()) {
                sb.append("DATE_RANGE=")
                  .append(dateRange.get("start").asText())
                  .append("-")
                  .append(dateRange.get("end").asText())
                  .append("|");
            } else if (dateRange.has("start") && !dateRange.get("start").isNull()) {
                sb.append("DATE=").append(dateRange.get("start").asText()).append("|");
            }
        }
        
        return sb.toString();
    }
    
    /**
     * Генерирует улучшенные параметры запроса с использованием AI
     */
    public QueryAnalysisService.QueryParams generateEnhancedParams(String request, String dataNeeded) {
        QueryAnalysisService.QueryParams params = queryAnalysisService.generateQueryParams(request, dataNeeded);
        
        // Дополнительная обработка через AI если стандартный парсинг не дал результата
        if (params.getMonth() == null && params.getStartDate() == null) {
            enhanceParamsWithAI(request, params);
        }
        
        return params;
    }
    
    private void enhanceParamsWithAI(String request, QueryAnalysisService.QueryParams params) {
        String prompt = String.format("""
            Извлеки временные параметры из запроса. Ответь JSON:
            {
                "month": "название месяца или null",
                "startDate": "дата начала или null", 
                "endDate": "дата окончания или null",
                "isRange": true/false
            }
            
            Запрос: %s
            """, request);
        
        try {
            String response = ollamaService.generateResponse(prompt);
            JsonNode json = objectMapper.readTree(response);
            
            if (json.has("month") && !json.get("month").isNull()) {
                params.setMonth(json.get("month").asText());
            }
            
            if (json.has("startDate") && !json.get("startDate").isNull()) {
                params.setStartDate(json.get("startDate").asText());
            }
            
            if (json.has("endDate") && !json.get("endDate").isNull()) {
                params.setEndDate(json.get("endDate").asText());
            }
            
        } catch (Exception e) {
            // Игнорируем ошибки AI-анализа
        }
    }
}