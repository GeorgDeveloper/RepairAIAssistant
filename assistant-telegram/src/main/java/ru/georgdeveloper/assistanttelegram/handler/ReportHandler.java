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
import java.util.Arrays;

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
            
             // Получаем данные для отчета за предыдущие сутки
             LocalDate yesterday = LocalDate.now().minusDays(1);
            
            Map<String, Object> reportData = new HashMap<>();
            
             // Получаем данные по участкам из current-metrics (как на веб-странице)
             List<Map<String, Object>> areaData = null; // Будем использовать currentMetrics
             logger.info("Данные по участкам будут взяты из current-metrics");
             
             // Получаем данные breakdown и availability за предыдущие сутки
             List<Map<String, Object>> breakDownData = fetchData("/dashboard/breakDown");
             logger.info("Получено данных breakdown: {}", breakDownData != null ? breakDownData.size() : 0);
             
             List<Map<String, Object>> availabilityData = fetchData("/dashboard/availability");
             logger.info("Получено данных availability: {}", availabilityData != null ? availabilityData.size() : 0);
             
             List<Map<String, Object>> currentMetrics = fetchData("/dashboard/current-metrics");
             logger.info("Получено данных current-metrics: {}", currentMetrics != null ? currentMetrics.size() : 0);
             
             List<Map<String, Object>> topBreakdownsWeek = fetchData("/dashboard/top-breakdowns-week");
             logger.info("Получено данных top-breakdowns-week: {}", topBreakdownsWeek != null ? topBreakdownsWeek.size() : 0);
             
             List<Map<String, Object>> topBreakdownsWeekKeyLines = fetchData("/dashboard/top-breakdowns-week-key-lines");
             logger.info("Получено данных top-breakdowns-week-key-lines: {}", topBreakdownsWeekKeyLines != null ? topBreakdownsWeekKeyLines.size() : 0);
             
             List<Map<String, Object>> pmData = fetchData("/dashboard/pm-plan-fact-tag");
             logger.info("Получено данных pm-plan-fact-tag: {}", pmData != null ? pmData.size() : 0);
            
            reportData.put("areaData", areaData);
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
    
    public InlineKeyboardMarkup getBackToReportsKeyboard() {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();

        InlineKeyboardButton backButton = new InlineKeyboardButton("⬅️ Назад к отчетам");
        backButton.setCallbackData("request_report");

        List<List<InlineKeyboardButton>> keyboard = Arrays.asList(
            Arrays.asList(backButton)
        );

        markup.setKeyboard(keyboard);
        return markup;
    }
    
    private List<Map<String, Object>> fetchData(String endpoint) {
        try {
            String url = webServiceUrl + endpoint;
            logger.debug("Запрос данных: {}", url);
            
            // Специальная обработка для current-metrics, который может возвращать Map вместо List
            if (endpoint.equals("/dashboard/current-metrics")) {
                try {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> response = restTemplate.getForObject(url, Map.class);
                    if (response != null) {
                        // Преобразуем Map в List с одним элементом
                        return Arrays.asList(response);
                    }
                } catch (Exception e) {
                    logger.warn("Ошибка при получении current-metrics как Map: {}", e.getMessage());
                }
            }
            
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
    
    private List<Map<String, Object>> fetchDataWithParams(String endpoint, String... params) {
        try {
            StringBuilder url = new StringBuilder(webServiceUrl + endpoint);
            if (params.length > 0) {
                url.append("?");
                for (int i = 0; i < params.length; i += 2) {
                    if (i > 0) url.append("&");
                    url.append(params[i]).append("=").append(params[i + 1]);
                }
            }
            
            logger.debug("Запрос данных с параметрами: {}", url);
            
            return restTemplate.exchange(
                url.toString(),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<Map<String, Object>>>() {}
            ).getBody();
            
        } catch (Exception e) {
            logger.warn("Ошибка при получении данных с параметрами {}: {}", endpoint, e.getMessage());
            return new ArrayList<>();
        }
    }
    
    private String formatDailyReport(Map<String, Object> data) {
        StringBuilder report = new StringBuilder();
        LocalDate yesterday = LocalDate.now().minusDays(1);
        report.append("📊 ОТЧЕТ ЗА СУТКИ\n");
        report.append("📅 Дата: ").append(yesterday.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))).append("\n\n");
        
         // Показатели Breakdown по участкам за предыдущие сутки (из current-metrics)
         @SuppressWarnings("unchecked")
         List<Map<String, Object>> currentMetrics = (List<Map<String, Object>>) data.get("currentMetrics");
         logger.debug("Current metrics: {} записей", currentMetrics != null ? currentMetrics.size() : 0);
         
         if (currentMetrics != null && !currentMetrics.isEmpty()) {
             // Берем первую запись из current-metrics (как на веб-странице)
             Map<String, Object> metrics = currentMetrics.get(0);
             logger.debug("Current metrics data: {}", metrics);
             
             report.append("📉 ПОКАЗАТЕЛИ BREAKDOWN ПО УЧАСТКАМ:\n");
             
             // Маппинг участков как на веб-странице
             String[] areaPrefixes = {
                 "report_new_mixing_area", "report_semifinishing_area", "report_building_area",
                 "report_curing_area", "report_finishig_area", "report_modules", "report_plant"
             };
             String[] areaNames = {
                 "NewMixingArea", "SemifinishingArea", "BuildingArea",
                 "CuringArea", "FinishigArea", "Modules", "Plant"
             };
             
             Map<String, Double> areaBreakdown = new HashMap<>();
             for (int i = 0; i < areaPrefixes.length; i++) {
                 String prefix = areaPrefixes[i];
                 String areaName = areaNames[i];
                 
                 Object bdToday = metrics.get(prefix + "_bd_today");
                 if (bdToday != null) {
                     try {
                         double value = Double.parseDouble(bdToday.toString());
                         areaBreakdown.put(areaName, value);
                         logger.debug("Участок {}: BD = {}", areaName, value);
                     } catch (NumberFormatException e) {
                         logger.warn("Некорректное значение BD для участка {}: {}", areaName, bdToday);
                     }
                 }
             }
             
             // Сортируем участки по убыванию BD% и показываем только топ-5
             areaBreakdown.entrySet().stream()
                 .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                 .limit(5)
                 .forEach(entry -> {
                     report.append("• ").append(entry.getKey()).append(" - ").append(String.format("%.2f", entry.getValue())).append("%\n");
                 });
             
             report.append("\n");
         } else {
            logger.debug("Нет данных current-metrics, используем общие показатели");
            // Если нет данных по участкам, показываем общие показатели
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> breakDownData = (List<Map<String, Object>>) data.get("breakDown");
            if (breakDownData != null && !breakDownData.isEmpty()) {
                report.append("📉 ПОКАЗАТЕЛИ BREAKDOWN:\n");
                String yesterdayStr = yesterday.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
                
                // Ищем данные за вчерашний день
                for (Map<String, Object> item : breakDownData) {
                    String day = (String) item.get("production_day");
                    if (yesterdayStr.equals(day)) {
                        Object downtime = item.get("downtime_percentage");
                        if (downtime != null) {
                            report.append("• ").append(day).append(": ").append(downtime).append("%\n");
                        }
                        break;
                    }
                }
                report.append("\n");
            }
        }
        
        // Показатели Availability за предыдущие сутки
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> availabilityData = (List<Map<String, Object>>) data.get("availability");
        if (availabilityData != null && !availabilityData.isEmpty()) {
            report.append("📈 ПОКАЗАТЕЛИ ДОСТУПНОСТИ:\n");
            String yesterdayStr = yesterday.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
            
            // Ищем данные за вчерашний день
            for (Map<String, Object> item : availabilityData) {
                String day = (String) item.get("production_day");
                if (yesterdayStr.equals(day)) {
                    Object availability = item.get("availability");
                    if (availability != null) {
                        report.append("• ").append(day).append(": ").append(availability).append("%\n");
                    }
                    break;
                }
            }
            report.append("\n");
        }
        
         // Топ поломок за неделю
         @SuppressWarnings("unchecked")
         List<Map<String, Object>> topBreakdowns = (List<Map<String, Object>>) data.get("topBreakdownsWeek");
         logger.debug("Топ поломок за неделю: {} записей", topBreakdowns != null ? topBreakdowns.size() : 0);
         if (topBreakdowns != null && !topBreakdowns.isEmpty()) {
             report.append("🔧 ТОП ПОЛОМОК ЗА НЕДЕЛЮ:\n");
             int count = 0;
             for (Map<String, Object> item : topBreakdowns) {
                 if (count >= 5) break; // Показываем только топ-5
                 String machine = (String) item.get("machine_name");
                 Object downtime = item.get("machine_downtime");
                 logger.debug("Поломка: machine={}, downtime={}", machine, downtime);
                 if (machine != null && downtime != null) {
                     report.append("• ").append(machine).append(": ").append(downtime).append("\n");
                     count++;
                 }
             }
             report.append("\n");
         } else {
             logger.debug("Нет данных о топ поломках за неделю");
         }
        
         // Ключевые линии
         @SuppressWarnings("unchecked")
         List<Map<String, Object>> keyLines = (List<Map<String, Object>>) data.get("topBreakdownsWeekKeyLines");
         logger.debug("Ключевые линии: {} записей", keyLines != null ? keyLines.size() : 0);
         if (keyLines != null && !keyLines.isEmpty()) {
             report.append("🏭 КЛЮЧЕВЫЕ ЛИНИИ (НЕДЕЛЯ):\n");
             int count = 0;
             for (Map<String, Object> item : keyLines) {
                 if (count >= 5) break; // Показываем только топ-5
                 String machine = (String) item.get("machine_name");
                 Object downtime = item.get("machine_downtime");
                 logger.debug("Ключевая линия: machine={}, downtime={}", machine, downtime);
                 if (machine != null && downtime != null) {
                     report.append("• ").append(machine).append(": ").append(downtime).append("\n");
                     count++;
                 }
             }
             report.append("\n");
         } else {
             logger.debug("Нет данных о ключевых линиях");
         }
        
         // Показатели PM (планово-предупредительного обслуживания)
         @SuppressWarnings("unchecked")
         List<Map<String, Object>> pmData = (List<Map<String, Object>>) data.get("pmData");
         logger.debug("PM данные: {} записей", pmData != null ? pmData.size() : 0);
         if (pmData != null && !pmData.isEmpty()) {
             report.append("🔧 ВЫПОЛНЕНИЕ PM:\n");
             String yesterdayStr = yesterday.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
             logger.debug("Ищем PM данные за дату: {}", yesterdayStr);
             
             // Ищем данные за вчерашний день
             boolean found = false;
             for (Map<String, Object> item : pmData) {
                 String day = (String) item.get("production_day");
                 logger.debug("PM запись: day={}, plan={}, fact={}, tag={}", 
                     day, item.get("plan"), item.get("fact"), item.get("tag"));
                 if (yesterdayStr.equals(day)) {
                     Object plan = item.get("plan");
                     Object fact = item.get("fact");
                     Object tag = item.get("tag");
                     
                     if (plan != null && fact != null) {
                         report.append("• План: ").append(plan).append("\n");
                         report.append("• Факт: ").append(fact).append("\n");
                         if (tag != null) {
                             report.append("• Tag: ").append(tag).append("\n");
                         }
                         // Вычисляем процент выполнения
                         try {
                             double planValue = Double.parseDouble(plan.toString());
                             double factValue = Double.parseDouble(fact.toString());
                             if (planValue > 0) {
                                 double percentage = (factValue / planValue) * 100;
                                 report.append("• Выполнение: ").append(String.format("%.1f", percentage)).append("%\n");
                             }
                         } catch (NumberFormatException e) {
                             logger.warn("Ошибка при вычислении процента PM: {}", e.getMessage());
                         }
                         found = true;
                     }
                     break;
                 }
             }
             if (!found) {
                 logger.debug("Не найдены PM данные за дату {}", yesterdayStr);
             }
         } else {
             logger.debug("Нет данных о PM");
         }
        
        String reportText = report.toString();
        
        // Ограничиваем длину сообщения (Telegram лимит 4096 символов)
        if (reportText.length() > 4000) {
            reportText = reportText.substring(0, 4000) + "\n\n... (сообщение обрезано)";
        }
        
        return reportText;
    }
    
    private String formatCurrentReport(List<Map<String, Object>> bdMetrics, 
                                     List<Map<String, Object>> availabilityMetrics,
                                     List<Map<String, Object>> currentMainLines,
                                     List<Map<String, Object>> activeWorkOrders) {
        StringBuilder report = new StringBuilder();
        report.append("⚡ ТЕКУЩИЙ ОТЧЕТ\n");
        report.append("🕐 Время: ").append(java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))).append("\n\n");
        
         // Текущие показатели по участкам (только последние записи)
         if (bdMetrics != null && !bdMetrics.isEmpty()) {
             report.append("📊 ТЕКУЩИЕ ПОКАЗАТЕЛИ ПО УЧАСТКАМ:\n");
             
             // Группируем по участкам и берем только последние записи
             Map<String, Map<String, Object>> latestByArea = new HashMap<>();
             for (Map<String, Object> item : bdMetrics) {
                 String area = (String) item.get("area");
                 String timestamp = (String) item.get("timestamp");
                 Object value = item.get("value");
                 
                 if (area != null && timestamp != null && value != null) {
                     // Если это первая запись для участка или более новая
                     if (!latestByArea.containsKey(area) || 
                         timestamp.compareTo((String) latestByArea.get(area).get("timestamp")) > 0) {
                         latestByArea.put(area, item);
                     }
                 }
             }
             
             // Выводим только последние записи по каждому участку
             latestByArea.values().stream()
                 .sorted((a, b) -> ((String) a.get("area")).compareTo((String) b.get("area")))
                 .forEach(item -> {
                     String area = (String) item.get("area");
                     Object value = item.get("value");
                     report.append("• ").append(area).append(": ").append(value).append("%\n");
                 });
             
             report.append("\n");
         }
        
         // Доступность по участкам (только последние записи)
         if (availabilityMetrics != null && !availabilityMetrics.isEmpty()) {
             report.append("📈 ДОСТУПНОСТЬ ПО УЧАСТКАМ:\n");
             
             // Группируем по участкам и берем только последние записи
             Map<String, Map<String, Object>> latestAvailabilityByArea = new HashMap<>();
             for (Map<String, Object> item : availabilityMetrics) {
                 String area = (String) item.get("area");
                 String timestamp = (String) item.get("timestamp");
                 Object value = item.get("value");
                 
                 if (area != null && timestamp != null && value != null) {
                     // Если это первая запись для участка или более новая
                     if (!latestAvailabilityByArea.containsKey(area) || 
                         timestamp.compareTo((String) latestAvailabilityByArea.get(area).get("timestamp")) > 0) {
                         latestAvailabilityByArea.put(area, item);
                     }
                 }
             }
             
             // Выводим только последние записи по каждому участку
             latestAvailabilityByArea.values().stream()
                 .sorted((a, b) -> ((String) a.get("area")).compareTo((String) b.get("area")))
                 .forEach(item -> {
                     String area = (String) item.get("area");
                     Object value = item.get("value");
                     report.append("• ").append(area).append(": ").append(value).append("%\n");
                 });
             
             report.append("\n");
         }
        
         // Ключевые линии убраны по запросу пользователя
        
         // Текущие наряды на работы
         if (activeWorkOrders != null && !activeWorkOrders.isEmpty()) {
             report.append("🔧 ТЕКУЩИЕ НАРЯДЫ НА РАБОТЫ:\n");
             int count = 0;
             for (Map<String, Object> item : activeWorkOrders) {
                 if (count >= 10) break; // Показываем только первые 10
                 String machine = (String) item.get("machineName");
                 String type = (String) item.get("type");
                 String status = (String) item.get("status");
                 Object duration = item.get("duration");
                 
                 if (machine != null && type != null && status != null) {
                     report.append("• ").append(machine).append(" - ").append(type).append(" (").append(status).append(")");
                     if (duration != null) {
                         report.append(" - ").append(duration);
                     }
                     report.append("\n");
                     count++;
                 }
             }
             
             if (count == 0) {
                 report.append("Нет активных нарядов на работы\n");
             }
         } else {
             report.append("🔧 ТЕКУЩИЕ НАРЯДЫ НА РАБОТЫ:\n");
             report.append("Нет активных нарядов на работы\n");
         }
        
        String reportText = report.toString();
        
        // Ограничиваем длину сообщения (Telegram лимит 4096 символов)
        if (reportText.length() > 4000) {
            reportText = reportText.substring(0, 4000) + "\n\n... (сообщение обрезано)";
        }
        
        return reportText;
    }
}
