package ru.georgdeveloper.assistantcore.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


/**
 * Сервис для анализа запросов через AI и определения необходимости поиска в БД
 */
@Service
public class QueryAnalysisService {
    
    @Autowired
    private OllamaService ollamaService;
    
    @Autowired
    private ModelTrainingService modelTrainingService;
    
    /**
     * Определяет тип запроса с использованием AI и улучшенного анализа
     */
    public AnalysisResult analyzeRequest(String request) {
        String normalized = normalizeRequest(request);
        
        // Сначала пробуем обученную модель
        String aiResponse = modelTrainingService.analyzeWithTraining(normalized);
        
        if (aiResponse.startsWith("SIMPLE_ANSWER:")) {
            String answer = aiResponse.substring("SIMPLE_ANSWER:".length()).trim();
            return new AnalysisResult(false, answer, null);
        } else if (aiResponse.startsWith("NEED_DATABASE:")) {
            String dataNeeded = aiResponse.substring("NEED_DATABASE:".length()).trim();
            return new AnalysisResult(true, null, dataNeeded);
        }
        
        // Если модель не дала результат, используем AI-классификацию
        return classifyWithAI(normalized);
    }
    
    private String normalizeRequest(String request) {
        return request.toLowerCase()
                .replace("сколько раз", "количество")
                .replace("как починить", "инструкция по ремонту")
                .replace("как отремонтировать", "инструкция по ремонту")
                .replaceAll("\\b(пожалуйста|мне|нужно)\\b", "")
                .trim();
    }
    
    private AnalysisResult classifyWithAI(String request) {
        String prompt = String.format(
            "Классифицируй запрос. Ответь одним словом: statistics (статистика/данные), instruction (инструкция), general (общий вопрос).\n" +
            "Примеры:\n" +
            "- 'статистика за январь' → statistics\n" +
            "- 'инструкция по ремонту насоса' → instruction\n" +
            "- 'кто ты' → general\n" +
            "Запрос: %s", request);
        
        String classification = ollamaService.generateResponse(prompt).toLowerCase().trim();
        
        if (classification.contains("general")) {
            return handleGeneralQuery(request);
        } else if (classification.contains("instruction")) {
            return new AnalysisResult(false, "Для получения инструкций по ремонту обратитесь к техническому руководству или специалисту.", null);
        }
        
        return new AnalysisResult(true, null, "Требуется анализ данных о ремонтах");
    }
    
    private AnalysisResult handleGeneralQuery(String request) {
        if (request.contains("кто ты")) {
            return new AnalysisResult(false, "Я Kvant AI - AI-ассистент для анализа данных о ремонте промышленного оборудования.", null);
        }
        if (request.contains("что ты") || request.contains("что умеешь") || request.contains("что можешь")) {
            return new AnalysisResult(false, "Я умею:\n• Анализировать данные о ремонтах\n• Находить топ самых продолжительных ремонтов\n• Подсчитывать статистику по статусам\n• Искать информацию по конкретным машинам", null);
        }
        if (request.contains("привет") || request.contains("как дела")) {
            return new AnalysisResult(false, "Привет! Я готов помочь с анализом данных о ремонте оборудования.", null);
        }
        return new AnalysisResult(true, null, "Требуется анализ данных о ремонтах");
    }
    
    public QueryParams generateQueryParams(String request, String dataNeeded) {
        QueryParams params = parseTrainingParams(dataNeeded);
        if (params != null) {
            return params;
        }
        return generateWithRules(request);
    }
    
    private QueryParams parseTrainingParams(String dataNeeded) {
        if (dataNeeded == null || !dataNeeded.contains("|")) {
            return null;
        }
        
        QueryParams params = new QueryParams();
        String[] parts = dataNeeded.split("\\|");
        
        for (String part : parts) {
            if (part.startsWith("STATUS=")) {
                String status = part.substring(7);
                if (!"NONE".equals(status)) {
                    params.setStatus(status);
                }
            } else if (part.startsWith("MACHINE=")) {
                String machine = part.substring(8);
                if (!"NONE".equals(machine)) {
                    params.setMachineKeyword(machine);
                }
            } else if (part.startsWith("LIMIT=")) {
                try {
                    int limit = Integer.parseInt(part.substring(6));
                    params.setLimit(limit);
                } catch (NumberFormatException e) {
                    params.setLimit(100);
                }
            } else if (part.contains("ORDER_BY_TTR=true")) {
                params.setOrderByTtr(true);
            } else if (part.contains("ORDER_BY_DOWNTIME=true")) {
                params.setOrderByDowntime(true);
            } else if (part.contains("ORDER_BY_DATE=true")) {
                params.setOrderByDate(true);
            } else if (part.startsWith("FAILURE_TYPE=")) {
                String failureType = part.substring(13);
                if (!"NONE".equals(failureType)) {
                    params.setFailureType(failureType);
                }
            }
        }
        
        return params;
    }
    
    private QueryParams generateWithRules(String request) {
        QueryParams params = new QueryParams();
        String lowerRequest = request.toLowerCase();
        
        // Определяем статус
        if (lowerRequest.contains("временно закрыт")) {
            params.setStatus("Временно закрыто");
        } else if (lowerRequest.contains("закрыт")) {
            params.setStatus("Закрыто");
        }
        
        // Улучшенный анализ дат и периодов
        extractDateParams(lowerRequest, params);
        
        // Определяем тип запроса
        if (lowerRequest.contains("самые продолжительные ремонты") || 
            (lowerRequest.contains("продолжительн") && lowerRequest.contains("ремонт"))) {
            params.setLimit(5);
            params.setOrderByDowntime(true);
        } else if (lowerRequest.contains("топ") && (lowerRequest.contains("продолжительн") || lowerRequest.contains("долгие"))) {
            params.setLimit(lowerRequest.contains("5") ? 5 : 10);
            params.setOrderByDowntime(true);
        } else if (lowerRequest.contains("топ 5")) {
            params.setLimit(5);
            params.setOrderByDate(true);
        } else {
            params.setLimit(100);
        }
        
        return params;
    }
    
    private void extractDateParams(String request, QueryParams params) {
        DateParsingUtils.DateRange dateRange = DateParsingUtils.extractDateRange(request);
        
        if (dateRange != null) {
            if (dateRange.isRange()) {
                params.setStartDate(dateRange.getStartDate());
                params.setEndDate(dateRange.getEndDate());
            } else {
                String date = dateRange.getStartDate();
                // Если это месяц, устанавливаем как месяц
                if (isMonth(date)) {
                    params.setMonth(date);
                } else {
                    params.setStartDate(date);
                }
            }
        }
    }
    
    private boolean isMonth(String dateStr) {
        String[] months = {"Январь", "Февраль", "Март", "Апрель", "Май", "Июнь", 
                          "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь"};
        for (String month : months) {
            if (month.equals(dateStr)) {
                return true;
            }
        }
        return false;
    }
    

    
    public void addTrainingExample(String input, String output) {
        modelTrainingService.addTrainingExample(input, output);
    }
    

    
    /**
     * Результат анализа запроса
     */
    public static class AnalysisResult {
        private final boolean needsDatabase;
        private final String simpleAnswer;
        private final String dataNeeded;
        
        public AnalysisResult(boolean needsDatabase, String simpleAnswer, String dataNeeded) {
            this.needsDatabase = needsDatabase;
            this.simpleAnswer = simpleAnswer;
            this.dataNeeded = dataNeeded;
        }
        
        public boolean needsDatabase() { return needsDatabase; }
        public String getSimpleAnswer() { return simpleAnswer; }
        public String getDataNeeded() { return dataNeeded; }
    }
    
    /**
     * Класс для хранения параметров запроса
     */
    public static class QueryParams {
        private String status;
        private String machineKeyword;
        private int limit = 100;
        private boolean orderByDate = false;
        
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        
        public String getMachineKeyword() { return machineKeyword; }
        public void setMachineKeyword(String machineKeyword) { this.machineKeyword = machineKeyword; }
        
        public int getLimit() { return limit; }
        public void setLimit(int limit) { this.limit = limit; }
        
        public boolean isOrderByDate() { return orderByDate; }
        public void setOrderByDate(boolean orderByDate) { this.orderByDate = orderByDate; }
        
        private boolean orderByDowntime = false;
        public boolean isOrderByDowntime() { return orderByDowntime; }
        public void setOrderByDowntime(boolean orderByDowntime) { this.orderByDowntime = orderByDowntime; }
        
        private boolean orderByTtr = false;
        public boolean isOrderByTtr() { return orderByTtr; }
        public void setOrderByTtr(boolean orderByTtr) { this.orderByTtr = orderByTtr; }
        
        private String searchComments;
        public String getSearchComments() { return searchComments; }
        public void setSearchComments(String searchComments) { this.searchComments = searchComments; }
        
        private String month;
        public String getMonth() { return month; }
        public void setMonth(String month) { this.month = month; }
        
        private String startDate;
        private String endDate;
        public String getStartDate() { return startDate; }
        public void setStartDate(String startDate) { this.startDate = startDate; }
        public String getEndDate() { return endDate; }
        public void setEndDate(String endDate) { this.endDate = endDate; }
        
        private String failureType;
        public String getFailureType() { return failureType; }
        public void setFailureType(String failureType) { this.failureType = failureType; }
    }
}