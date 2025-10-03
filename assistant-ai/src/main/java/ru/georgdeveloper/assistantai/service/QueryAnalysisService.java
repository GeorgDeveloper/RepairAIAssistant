package ru.georgdeveloper.assistantai.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class QueryAnalysisService {
    @Autowired
    private OllamaService ollamaService;

    @Autowired
    private ModelTrainingService modelTrainingService;

    public AnalysisResult analyzeRequest(String request) {
        String normalized = normalizeRequest(request);
        String aiResponse = modelTrainingService.analyzeWithTraining(normalized);
        if (aiResponse.startsWith("SIMPLE_ANSWER:")) {
            String answer = aiResponse.substring("SIMPLE_ANSWER:".length()).trim();
            return new AnalysisResult(false, answer, null);
        } else if (aiResponse.startsWith("NEED_DATABASE:")) {
            String dataNeeded = aiResponse.substring("NEED_DATABASE:".length()).trim();
            return new AnalysisResult(true, null, dataNeeded);
        }
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
                if (!"NONE".equals(status)) { params.setStatus(status); }
            } else if (part.startsWith("MACHINE=")) {
                String machine = part.substring(8);
                if (!"NONE".equals(machine)) { params.setMachineKeyword(machine); }
            } else if (part.startsWith("LIMIT=")) {
                try { params.setLimit(Integer.parseInt(part.substring(6))); } catch (NumberFormatException e) { params.setLimit(100); }
            } else if (part.contains("ORDER_BY_TTR=true")) {
                params.setOrderByTtr(true);
            } else if (part.contains("ORDER_BY_DOWNTIME=true")) {
                params.setOrderByDowntime(true);
            } else if (part.contains("ORDER_BY_DATE=true")) {
                params.setOrderByDate(true);
            } else if (part.startsWith("FAILURE_TYPE=")) {
                String failureType = part.substring(13);
                if (!"NONE".equals(failureType)) { params.setFailureType(failureType); }
            }
        }
        return params;
    }

    private QueryParams generateWithRules(String request) {
        QueryParams params = new QueryParams();
        String lowerRequest = request.toLowerCase();
        if (lowerRequest.contains("временно закрыт")) {
            params.setStatus("Временно закрыто");
        } else if (lowerRequest.contains("закрыт")) {
            params.setStatus("Закрыто");
        }
        return params;
    }

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

    public static class QueryParams {
        private String status;
        private String machineKeyword;
        private int limit = 100;
        private boolean orderByDate = false;
        private boolean orderByDowntime = false;
        private boolean orderByTtr = false;
        private String searchComments;
        private String month;
        private String startDate;
        private String endDate;
        private String failureType;
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getMachineKeyword() { return machineKeyword; }
        public void setMachineKeyword(String machineKeyword) { this.machineKeyword = machineKeyword; }
        public int getLimit() { return limit; }
        public void setLimit(int limit) { this.limit = limit; }
        public boolean isOrderByDate() { return orderByDate; }
        public void setOrderByDate(boolean orderByDate) { this.orderByDate = orderByDate; }
        public boolean isOrderByDowntime() { return orderByDowntime; }
        public void setOrderByDowntime(boolean orderByDowntime) { this.orderByDowntime = orderByDowntime; }
        public boolean isOrderByTtr() { return orderByTtr; }
        public void setOrderByTtr(boolean orderByTtr) { this.orderByTtr = orderByTtr; }
        public String getSearchComments() { return searchComments; }
        public void setSearchComments(String searchComments) { this.searchComments = searchComments; }
        public String getMonth() { return month; }
        public void setMonth(String month) { this.month = month; }
        public String getStartDate() { return startDate; }
        public void setStartDate(String startDate) { this.startDate = startDate; }
        public String getEndDate() { return endDate; }
        public void setEndDate(String endDate) { this.endDate = endDate; }
        public String getFailureType() { return failureType; }
        public void setFailureType(String failureType) { this.failureType = failureType; }
    }
}


