package ru.georgdeveloper.assistantcore.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import ru.georgdeveloper.assistantcore.repository.MonitoringRepository;

@RestController
@RequestMapping("/dashboard")
public class DashboardController {

    @GetMapping("/top-breakdowns-week")
    @ResponseBody
    public List<Map<String, Object>> getTopBreakdownsPerWeek() {
        return monitoringRepository.getTopBreakdownsPerWeek();
    }

    @GetMapping("/top-breakdowns-week-key-lines")
    @ResponseBody
    public List<Map<String, Object>> getTopBreakdownsPerWeekKeyLines() {
        return monitoringRepository.getTopBreakdownsPerWeekKeyLines();
    }

    @GetMapping("/top-breakdowns")
    @ResponseBody
    public List<Map<String, Object>> getTopBreakdownsPerDay() {
        return monitoringRepository.getTopBreakdownsPerDay();
    }

    @GetMapping("/top-breakdowns-key-lines")
    @ResponseBody
    public List<Map<String, Object>> getTopBreakdownsPerDayKeyLines() {
        return monitoringRepository.getTopBreakdownsPerDayKeyLines();
    }

    @GetMapping("/top-breakdowns-current")
    @ResponseBody
    public List<Map<String, Object>> getTopBreakdownsCurrent() {
        return monitoringRepository.getTopBreakdownsCurrentStatusOnline();
    }

    @Autowired
    private MonitoringRepository monitoringRepository;
    
    @GetMapping("/breakDown")
    @ResponseBody
    public List<Map<String, Object>> searchBreakDown() {
        return monitoringRepository.searchBreakDown();
    }

    @GetMapping("/availability")
    @ResponseBody
    public List<Map<String, Object>> searchAvailability() {
        return monitoringRepository.searchAvailability();
    }

    @GetMapping("/pm-plan-fact-tag")
    @ResponseBody
    public List<Map<String, Object>> getPmPlanFactTagPerMonth() {
        return monitoringRepository.getPmPlanFactTagPerMonth();
    }

    @GetMapping("/pm-plan-fact-tag-all")
    @ResponseBody
    public List<Map<String, Object>> getPmPlanFactTagAll(
            @RequestParam(required = false) String area,
            @RequestParam(required = false) String machineName) {
        if ((area != null && !area.isEmpty() && !area.equals("all")) || 
            (machineName != null && !machineName.isEmpty() && !machineName.equals("all"))) {
            return monitoringRepository.getPmPlanFactTagFiltered(area, machineName);
        }
        return monitoringRepository.getPmPlanFactTagAll();
    }

    @GetMapping("/pm/areas")
    @ResponseBody
    public List<Map<String, Object>> getPmAreas() {
        return monitoringRepository.getPmAreas();
    }

    @GetMapping("/pm/equipment")
    @ResponseBody
    public List<Map<String, Object>> getPmEquipment(@RequestParam(required = false) String area) {
        return monitoringRepository.getPmEquipmentByArea(area);
    }


    @GetMapping("/current-metrics")
    @ResponseBody
    public Map<String, Object> getCurrentMetrics() {
        return monitoringRepository.getCurrentMetrics();
    }

    @GetMapping("/metrics-for-date")
    @ResponseBody
    public Map<String, Object> getMetricsForDate(@RequestParam("date") String date) {
        return monitoringRepository.getMetricsForDate(date);
    }

    @GetMapping("/equipment-maintenance-records")
    @ResponseBody
    public java.util.List<java.util.Map<String, Object>> getEquipmentMaintenanceRecords() {
        return monitoringRepository.getEquipmentMaintenanceRecords();
    }

    @GetMapping("/pm-maintenance-records")
    @ResponseBody
    public java.util.List<java.util.Map<String, Object>> getPmMaintenanceRecords() {
        return monitoringRepository.getPmMaintenanceRecords();
    }
}
