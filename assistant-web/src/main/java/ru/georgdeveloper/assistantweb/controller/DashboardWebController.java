package ru.georgdeveloper.assistantweb.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/dashboard")
public class DashboardWebController {

    @Value("${core.service.url:http://localhost:8080}")
    private String coreServiceUrl;

    private final RestTemplate restTemplate;

    /**
     * Конструктор контроллера дашборда
     */
    public DashboardWebController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @SuppressWarnings("unchecked")
    @GetMapping("/breakDown")
    public List<Map<String, Object>> breakDown(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        StringBuilder url = new StringBuilder(coreServiceUrl + "/dashboard/breakDown");
        boolean hasParams = false;
        
        if (year != null) {
            url.append("?year=").append(year);
            hasParams = true;
        }
        
        if (month != null) {
            url.append(hasParams ? "&" : "?").append("month=").append(month);
        }
        
        return (List<Map<String, Object>>) (List<?>) restTemplate.getForObject(url.toString(), List.class);
    }

    @SuppressWarnings("unchecked")
    @GetMapping("/availability")
    public List<Map<String, Object>> availability(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        StringBuilder url = new StringBuilder(coreServiceUrl + "/dashboard/availability");
        boolean hasParams = false;
        
        if (year != null) {
            url.append("?year=").append(year);
            hasParams = true;
        }
        
        if (month != null) {
            url.append(hasParams ? "&" : "?").append("month=").append(month);
        }
        
        return (List<Map<String, Object>>) (List<?>) restTemplate.getForObject(url.toString(), List.class);
    }

    @SuppressWarnings("unchecked")
    @GetMapping("/current-metrics")
    public Map<String, Object> currentMetrics(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        StringBuilder url = new StringBuilder(coreServiceUrl + "/dashboard/current-metrics");
        boolean hasParams = false;
        
        if (year != null) {
            url.append("?year=").append(year);
            hasParams = true;
        }
        
        if (month != null) {
            url.append(hasParams ? "&" : "?").append("month=").append(month);
        }
        
        return (Map<String, Object>) restTemplate.getForObject(url.toString(), Map.class);
    }

    @SuppressWarnings("unchecked")
    @GetMapping("/top-breakdowns-week")
    public List<Map<String, Object>> topBreakdownsWeek() {
        return (List<Map<String, Object>>) (List<?>) restTemplate.getForObject(coreServiceUrl + "/dashboard/top-breakdowns-week", List.class);
    }

    @SuppressWarnings("unchecked")
    @GetMapping("/top-breakdowns-week-key-lines")
    public List<Map<String, Object>> topBreakdownsWeekKeyLines() {
        return (List<Map<String, Object>>) (List<?>) restTemplate.getForObject(coreServiceUrl + "/dashboard/top-breakdowns-week-key-lines", List.class);
    }

    @SuppressWarnings("unchecked")
    @GetMapping("/top-breakdowns")
    public List<Map<String, Object>> topBreakdownsDay() {
        return (List<Map<String, Object>>) (List<?>) restTemplate.getForObject(coreServiceUrl + "/dashboard/top-breakdowns", List.class);
    }

    @SuppressWarnings("unchecked")
    @GetMapping("/top-breakdowns-key-lines")
    public List<Map<String, Object>> topBreakdownsDayKeyLines() {
        return (List<Map<String, Object>>) (List<?>) restTemplate.getForObject(coreServiceUrl + "/dashboard/top-breakdowns-key-lines", List.class);
    }

    @SuppressWarnings("unchecked")
    @GetMapping("/top-breakdowns-current")
    public List<Map<String, Object>> topBreakdownsCurrent() {
        return (List<Map<String, Object>>) (List<?>) restTemplate.getForObject(coreServiceUrl + "/dashboard/top-breakdowns-current", List.class);
    }

    @SuppressWarnings("unchecked")
    @GetMapping("/pm-plan-fact-tag")
    public List<Map<String, Object>> pmPlanFactTag(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        StringBuilder url = new StringBuilder(coreServiceUrl + "/dashboard/pm-plan-fact-tag");
        boolean hasParams = false;
        
        if (year != null) {
            url.append("?year=").append(year);
            hasParams = true;
        }
        
        if (month != null) {
            url.append(hasParams ? "&" : "?").append("month=").append(month);
        }
        
        return (List<Map<String, Object>>) (List<?>) restTemplate.getForObject(url.toString(), List.class);
    }

    @SuppressWarnings("unchecked")
    @GetMapping("/pm-plan-fact-tag-all")
    public List<Map<String, Object>> pmPlanFactTagAll(
            @RequestParam(required = false) String area,
            @RequestParam(required = false) String machineName) {
        StringBuilder url = new StringBuilder(coreServiceUrl + "/dashboard/pm-plan-fact-tag-all");
        boolean hasParams = false;
        
        if (area != null && !area.isEmpty()) {
            url.append(hasParams ? "&" : "?").append("area=").append(area);
            hasParams = true;
        }
        
        if (machineName != null && !machineName.isEmpty()) {
            url.append(hasParams ? "&" : "?").append("machineName=").append(machineName);
        }
        
        return (List<Map<String, Object>>) (List<?>) restTemplate.getForObject(url.toString(), List.class);
    }

    @SuppressWarnings("unchecked")
    @GetMapping("/pm/areas")
    public List<Map<String, Object>> pmAreas() {
        return (List<Map<String, Object>>) (List<?>) restTemplate.getForObject(coreServiceUrl + "/dashboard/pm/areas", List.class);
    }

    @SuppressWarnings("unchecked")
    @GetMapping("/pm/equipment")
    public List<Map<String, Object>> pmEquipment(@RequestParam(required = false) String area) {
        String url = coreServiceUrl + "/dashboard/pm/equipment";
        if (area != null && !area.isEmpty()) {
            url += "?area=" + area;
        }
        return (List<Map<String, Object>>) (List<?>) restTemplate.getForObject(url, List.class);
    }

    @SuppressWarnings("unchecked")
    @GetMapping("/tag/areas")
    public List<Map<String, Object>> tagAreas() {
        return (List<Map<String, Object>>) (List<?>) restTemplate.getForObject(coreServiceUrl + "/dashboard/tag/areas", List.class);
    }

    @SuppressWarnings("unchecked")
    @GetMapping("/tag/equipment")
    public List<Map<String, Object>> tagEquipment(@RequestParam(required = false) String area) {
        String url = coreServiceUrl + "/dashboard/tag/equipment";
        if (area != null && !area.isEmpty()) {
            url += "?area=" + area;
        }
        return (List<Map<String, Object>>) (List<?>) restTemplate.getForObject(url, List.class);
    }

    @SuppressWarnings("unchecked")
    @GetMapping("/bd/areas")
    public List<Map<String, Object>> bdAreas() {
        return (List<Map<String, Object>>) (List<?>) restTemplate.getForObject(coreServiceUrl + "/dashboard/bd/areas", List.class);
    }

    @SuppressWarnings("unchecked")
    @GetMapping("/bd/equipment")
    public List<Map<String, Object>> bdEquipment(@RequestParam(required = false) String area) {
        String url = coreServiceUrl + "/dashboard/bd/equipment";
        if (area != null && !area.isEmpty()) {
            url += "?area=" + area;
        }
        return (List<Map<String, Object>>) (List<?>) restTemplate.getForObject(url, List.class);
    }

    @SuppressWarnings("unchecked")
    @GetMapping("/pm-tag-bd-completed")
    public List<Map<String, Object>> pmTagBdCompleted(
            @RequestParam(required = false) String area,
            @RequestParam(required = false) String machineName) {
        StringBuilder url = new StringBuilder(coreServiceUrl + "/dashboard/pm-tag-bd-completed");
        boolean hasParams = false;
        
        if (area != null && !area.isEmpty()) {
            url.append(hasParams ? "&" : "?").append("area=").append(area);
            hasParams = true;
        }
        
        if (machineName != null && !machineName.isEmpty()) {
            url.append(hasParams ? "&" : "?").append("machineName=").append(machineName);
        }
        
        return (List<Map<String, Object>>) (List<?>) restTemplate.getForObject(url.toString(), List.class);
    }

    @SuppressWarnings("unchecked")
    @GetMapping("/metrics-for-date")
    public Map<String, Object> getMetricsForDate(@RequestParam("date") String date) {
        return (Map<String, Object>) restTemplate.getForObject(coreServiceUrl + "/dashboard/metrics-for-date?date=" + date, Map.class);
    }

    @SuppressWarnings("unchecked")
    @GetMapping("/tag-maintenance-records")
    public List<Map<String, Object>> tagMaintenanceRecords() {
        return (List<Map<String, Object>>) (List<?>) restTemplate.getForObject(coreServiceUrl + "/dashboard/tag-maintenance-records", List.class);
    }

    @SuppressWarnings("unchecked")
    @GetMapping("/diagnostics/areas")
    public List<Map<String, Object>> diagnosticsAreas() {
        return (List<Map<String, Object>>) (List<?>) restTemplate.getForObject(coreServiceUrl + "/dashboard/diagnostics/areas", List.class);
    }

    @SuppressWarnings("unchecked")
    @GetMapping("/diagnostics/equipment")
    public List<Map<String, Object>> diagnosticsEquipment(@RequestParam(required = false) String area) {
        String url = coreServiceUrl + "/dashboard/diagnostics/equipment";
        if (area != null && !area.isEmpty()) {
            url += "?area=" + area;
        }
        return (List<Map<String, Object>>) (List<?>) restTemplate.getForObject(url, List.class);
    }

}
