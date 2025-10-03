package ru.georgdeveloper.assistantweb.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import org.springframework.web.bind.annotation.*;
import ru.georgdeveloper.assistantweb.client.CoreServiceClient;
import ru.georgdeveloper.assistantcore.service.FallbackAssistantService;

@Controller
public class AssistantController {

    @GetMapping("/dashboard")
    public String dashboard() {
        return "dashboard";
    }
    
    private final CoreServiceClient coreServiceClient;

    @Value("${ai.enabled:true}")
    private boolean aiEnabled;
    
    @Autowired(required = false)
    private FallbackAssistantService fallbackService;
    
    /**
     * Конструктор контроллера веб-интерфейса ассистента
     * @param coreServiceClient HTTP клиент для взаимодействия с ядром
     */
    public AssistantController(CoreServiceClient coreServiceClient) {
        this.coreServiceClient = coreServiceClient;
    }
    
    @GetMapping("/")
    public String index() {
        return "index";
    }
    
    @GetMapping("/chat")
    public String chat() {
        if (!aiEnabled) {
            return "index";
        }
        return "chat";
    }

    @GetMapping("/long_report")
    public String longReport() {
        return "long_report";
    }

    @GetMapping("/manuals")
    public String manuals() {
        return "manuals";
    }

    @GetMapping("/gantt")
    public String gantt() {
        return "gantt";
    }

    @GetMapping("/dinamic_bd")
    public String dinamicBd() {
        return "dinamic_bd";
    }

    @GetMapping("/top_causes")
    public String topCauses() {
        return "top_causes";
    }

    @GetMapping("/top-causes")
    public String topCausesAlt() {
        return "top_causes";
    }

    @GetMapping("/top_equipment")
    public String topEquipment() {
        return "top_equipment";
    }

    @GetMapping("/top-equipment")
    public String topEquipmentAlt() {
        return "top_equipment";
    }

    @GetMapping("/top_areas")
    public String topAreas() {
        return "top_areas";
    }

    @GetMapping("/top-areas")
    public String topAreasAlt() {
        return "top_areas";
    }

    @GetMapping("/failures")
    public String failures() {
        return "failures";
    }

    @PostMapping("/api/chat")
    @ResponseBody
    public String processChat(@RequestBody String message) {
        if (!aiEnabled) {
            return "🚫 AI отключен в конфигурации (ai.enabled=false). Обратитесь к администратору для включения.";
        }
        
        // Очищаем сообщение от лишних кавычек JSON
        String cleanMessage = message;
        if (message.startsWith("\"") && message.endsWith("\"")) {
            cleanMessage = message.substring(1, message.length() - 1);
        }
        
        // Используем fallback сервис если AI отключен
        if (!aiEnabled && fallbackService != null) {
            return fallbackService.processQuery(cleanMessage);
        }
        
        try {
            // Логируем запрос для отладки
            System.out.println("Web запрос: " + cleanMessage);
            
            return coreServiceClient.analyzeRepairRequest(cleanMessage);
        } catch (Exception e) {
            System.err.println("Ошибка обработки веб-запроса: " + e.getMessage());
            return "❌ Ошибка обработки запроса. Попробуйте позже.";
        }
    }

    @PostMapping("/api/chat/feedback")
    @ResponseBody
    public String processFeedback(@RequestBody FeedbackDto feedback) {
        if (!aiEnabled) {
            return "AI is disabled";
        }
        return coreServiceClient.sendFeedback(feedback);
    }

    @GetMapping("/dashboard/equipment-maintenance-records")
    @ResponseBody
    public java.util.List<java.util.Map<String, Object>> getEquipmentMaintenanceRecords(
            @RequestParam(name = "limit", defaultValue = "500") int limit) {
        return coreServiceClient.getEquipmentMaintenanceRecords(limit);
    }

    public static class FeedbackDto {
        public String request;
        public String response;
    }
}