package ru.georgdeveloper.assistantweb.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;

import org.springframework.web.bind.annotation.*;
import ru.georgdeveloper.assistantweb.client.CoreServiceClient;

@Controller
public class AssistantController {

    @GetMapping("/dashboard")
    public String dashboard() {
        return "dashboard";
    }
    
    @GetMapping("/dashboard_graf")
    public String dashboardGraf() {
        return "dashboard_graf";
    }
    
    private final CoreServiceClient coreServiceClient;

    @Value("${ai.enabled:true}")
    private boolean aiEnabled;
    
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

    @GetMapping("/final")
    public String finalPage() {
        return "final";
    }

    @PostMapping("/api/chat")
    @ResponseBody
    public String processChat(@RequestBody String message) {
        if (!aiEnabled) {
            return "AI is disabled";
        }
        return coreServiceClient.analyzeRepairRequest(message);
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