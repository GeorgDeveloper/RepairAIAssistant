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

    @GetMapping("/dinamic_pm")
    public String dinamicPm() {
        return "dinamic_pm";
    }

    @GetMapping("/pm_tag_bd")
    public String pmTagBd() {
        return "pm_tag_bd";
    }

    @GetMapping("/tag")
    public String tag() {
        return "tag";
    }

    @GetMapping("/dinamic_type")
    public String dinamicType() {
        return "dinamic_type";
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

    @GetMapping("/pm")
    public String pm() {
        return "pm";
    }

    @GetMapping("/diagnostics-report")
    public String diagnosticsReport() {
        return "diagnostics_report";
    }

    @GetMapping("/create-diagnostics-report")
    public String createDiagnosticsReport() {
        return "create_diagnostics_report";
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

    @GetMapping("/dashboard/pm-maintenance-records")
    @ResponseBody
    public java.util.List<java.util.Map<String, Object>> getPmMaintenanceRecords(
            @RequestParam(name = "limit", defaultValue = "500") int limit) {
        return coreServiceClient.getPmMaintenanceRecords(limit);
    }

    @GetMapping("/dashboard/diagnostics-reports")
    @ResponseBody
    public java.util.List<java.util.Map<String, Object>> getDiagnosticsReports(
            @RequestParam(name = "limit", defaultValue = "500") int limit) {
        return coreServiceClient.getDiagnosticsReports(limit);
    }

    public static class FeedbackDto {
        public String request;
        public String response;
    }
}