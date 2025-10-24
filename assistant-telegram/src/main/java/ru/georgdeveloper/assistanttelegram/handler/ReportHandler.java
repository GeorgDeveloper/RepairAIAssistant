package ru.georgdeveloper.assistanttelegram.handler;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Component
public class ReportHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(ReportHandler.class);
    
    @Value("${web.service.url:http://localhost:8082}")
    private String webServiceUrl;
    
    private final RestTemplate restTemplate;
    
    public ReportHandler(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }
    
    public String getReportMenuMessage() {
        return "📊 Выберите тип отчета:\n\n" +
               "• Отчет за сутки - показатели со страницы dashboard\n" +
               "• Текущий отчет - показатели онлайн по участкам и текущие работы";
    }
    
    public InlineKeyboardMarkup getReportMenuKeyboard() {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        
        InlineKeyboardButton dailyReportButton = new InlineKeyboardButton("📅 Отчет за сутки");
        dailyReportButton.setCallbackData("daily_report");
        
        InlineKeyboardButton currentReportButton = new InlineKeyboardButton("⚡ Текущий отчет");
        currentReportButton.setCallbackData("current_report");
        
        InlineKeyboardButton backButton = new InlineKeyboardButton("⬅️ Назад");
        backButton.setCallbackData("back_to_main");
        
        List<List<InlineKeyboardButton>> keyboard = Arrays.asList(
            Arrays.asList(dailyReportButton),
            Arrays.asList(currentReportButton),
            Arrays.asList(backButton)
        );
        
        markup.setKeyboard(keyboard);
        return markup;
    }
    
    public String generateDailyReport() {
        try {
            logger.info("Генерация отчета за сутки");
            
            // Получаем данные для отчета за сутки (как на index.html)
            Map<String, Object> reportData = new HashMap<>();
            
            // Получаем данные breakdown и availability
            List<Map<String, Object>> breakDownData = fetchData("/dashboard/breakDown");
            List<Map<String, Object>> availabilityData = fetchData("/dashboard/availability");
            List<Map<String, Object>> currentMetrics = fetchData("/dashboard/current-metrics");
            List<Map<String, Object>> topBreakdownsWeek = fetchData("/dashboard/top-breakdowns-week");
            List<Map<String, Object>> topBreakdownsWeekKeyLines = fetchData("/dashboard/top-breakdowns-week-key-lines");
            List<Map<String, Object>> pmData = fetchData("/dashboard/pm-plan-fact-tag");
            
            reportData.put("breakDown", breakDownData);
            reportData.put("availability", availabilityData);
            reportData.put("currentMetrics", currentMetrics);
            reportData.put("topBreakdownsWeek", topBreakdownsWeek);
            reportData.put("topBreakdownsWeekKeyLines", topBreakdownsWeekKeyLines);
            reportData.put("pmData", pmData);
            
            return formatDailyReport(reportData);
            
        } catch (Exception e) {
            logger.error("Ошибка при генерации отчета за сутки: {}", e.getMessage(), e);
            return "❌ Ошибка при формировании отчета за сутки. Попробуйте позже.";
        }
    }
    
    public String generateCurrentReport() {
        try {
            logger.info("Генерация текущего отчета");
            
            // Получаем онлайн данные по участкам
            List<Map<String, Object>> bdMetrics = fetchData("/dashboard/online/bd");
            List<Map<String, Object>> availabilityMetrics = fetchData("/dashboard/online/availability");
            List<Map<String, Object>> currentMainLines = fetchData("/dashboard/main-lines/current");
            List<Map<String, Object>> activeWorkOrders = fetchData("/api/work-orders/active");
            
            return formatCurrentReport(bdMetrics, availabilityMetrics, currentMainLines, activeWorkOrders);
            
        } catch (Exception e) {
            logger.error("Ошибка при генерации текущего отчета: {}", e.getMessage(), e);
            return "❌ Ошибка при формировании текущего отчета. Попробуйте позже.";
        }
    }
    
    private List<Map<String, Object>> fetchData(String endpoint) {
        try {
            String url = webServiceUrl + endpoint;
            logger.debug("Запрос данных: {}", url);
            
            return restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<Map<String, Object>>>() {}
            ).getBody();
            
        } catch (Exception e) {
            logger.warn("Ошибка при получении данных с {}: {}", endpoint, e.getMessage());
            return new ArrayList<>();
        }
    }
    
    private String formatDailyReport(Map<String, Object> data) {
        StringBuilder report = new StringBuilder();
        report.append("📊 ОТЧЕТ ЗА СУТКИ\n");
        report.append("📅 Дата: ").append(LocalDate.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))).append("\n\n");
        
        // Показатели Breakdown
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> breakDownData = (List<Map<String, Object>>) data.get("breakDown");
        if (breakDownData != null && !breakDownData.isEmpty()) {
            report.append("📉 ПОКАЗАТЕЛИ BREAKDOWN:\n");
            for (Map<String, Object> item : breakDownData) {
                String day = (String) item.get("production_day");
                Object downtime = item.get("downtime_percentage");
                report.append("• ").append(day).append(": ").append(downtime).append("%\n");
            }
            report.append("\n");
        }
        
        // Показатели Availability
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> availabilityData = (List<Map<String, Object>>) data.get("availability");
        if (availabilityData != null && !availabilityData.isEmpty()) {
            report.append("📈 ПОКАЗАТЕЛИ ДОСТУПНОСТИ:\n");
            for (Map<String, Object> item : availabilityData) {
                String day = (String) item.get("production_day");
                Object availability = item.get("availability");
                report.append("• ").append(day).append(": ").append(availability).append("%\n");
            }
            report.append("\n");
        }
        
        // Топ поломок за неделю
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> topBreakdowns = (List<Map<String, Object>>) data.get("topBreakdownsWeek");
        if (topBreakdowns != null && !topBreakdowns.isEmpty()) {
            report.append("🔧 ТОП ПОЛОМОК ЗА НЕДЕЛЮ:\n");
            int count = 0;
            for (Map<String, Object> item : topBreakdowns) {
                if (count >= 5) break; // Показываем только топ-5
                String machine = (String) item.get("machine_name");
                Object downtime = item.get("machine_downtime");
                report.append("• ").append(machine).append(": ").append(downtime).append("\n");
                count++;
            }
            report.append("\n");
        }
        
        // Ключевые линии
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> keyLines = (List<Map<String, Object>>) data.get("topBreakdownsWeekKeyLines");
        if (keyLines != null && !keyLines.isEmpty()) {
            report.append("🏭 КЛЮЧЕВЫЕ ЛИНИИ (НЕДЕЛЯ):\n");
            int count = 0;
            for (Map<String, Object> item : keyLines) {
                if (count >= 5) break; // Показываем только топ-5
                String machine = (String) item.get("machine_name");
                Object downtime = item.get("machine_downtime");
                report.append("• ").append(machine).append(": ").append(downtime).append("\n");
                count++;
            }
        }
        
        return report.toString();
    }
    
    private String formatCurrentReport(List<Map<String, Object>> bdMetrics, 
                                     List<Map<String, Object>> availabilityMetrics,
                                     List<Map<String, Object>> currentMainLines,
                                     List<Map<String, Object>> activeWorkOrders) {
        StringBuilder report = new StringBuilder();
        report.append("⚡ ТЕКУЩИЙ ОТЧЕТ\n");
        report.append("🕐 Время: ").append(LocalDate.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))).append("\n\n");
        
        // Текущие показатели по участкам
        if (bdMetrics != null && !bdMetrics.isEmpty()) {
            report.append("📊 ТЕКУЩИЕ ПОКАЗАТЕЛИ ПО УЧАСТКАМ:\n");
            for (Map<String, Object> item : bdMetrics) {
                String area = (String) item.get("area");
                String timestamp = (String) item.get("timestamp");
                Object value = item.get("value");
                report.append("• ").append(area).append(" (").append(timestamp).append("): ").append(value).append("%\n");
            }
            report.append("\n");
        }
        
        // Доступность по участкам
        if (availabilityMetrics != null && !availabilityMetrics.isEmpty()) {
            report.append("📈 ДОСТУПНОСТЬ ПО УЧАСТКАМ:\n");
            for (Map<String, Object> item : availabilityMetrics) {
                String area = (String) item.get("area");
                String timestamp = (String) item.get("timestamp");
                Object value = item.get("value");
                report.append("• ").append(area).append(" (").append(timestamp).append("): ").append(value).append("%\n");
            }
            report.append("\n");
        }
        
        // Ключевые линии
        if (currentMainLines != null && !currentMainLines.isEmpty()) {
            report.append("🏭 КЛЮЧЕВЫЕ ЛИНИИ:\n");
            for (Map<String, Object> item : currentMainLines) {
                String line = (String) item.get("line_name");
                Object status = item.get("status");
                report.append("• ").append(line).append(": ").append(status).append("\n");
            }
            report.append("\n");
        }
        
        // Активные работы
        if (activeWorkOrders != null && !activeWorkOrders.isEmpty()) {
            report.append("🔧 АКТИВНЫЕ РАБОТЫ:\n");
            int count = 0;
            for (Map<String, Object> item : activeWorkOrders) {
                if (count >= 10) break; // Показываем только первые 10
                String machine = (String) item.get("machineName");
                String type = (String) item.get("type");
                String status = (String) item.get("status");
                report.append("• ").append(machine).append(" - ").append(type).append(" (").append(status).append(")\n");
                count++;
            }
        }
        
        return report.toString();
    }
}
