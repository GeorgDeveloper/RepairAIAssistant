package ru.georgdeveloper.assistantcore.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.PageRequest;
import java.util.*;
import ru.georgdeveloper.assistantcore.repository.MonitoringRepository;
import ru.georgdeveloper.assistantcore.repository.BreakdownReportRepository;
import ru.georgdeveloper.assistantcore.model.BreakdownReport;

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
    
    @Autowired
    private BreakdownReportRepository breakdownReportRepository;
    
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
    
    @GetMapping("/work-orders")
    @ResponseBody
    public List<Map<String, Object>> getWorkOrders() {
        try {
            List<BreakdownReport> reports = breakdownReportRepository.findLast15WorkOrders(PageRequest.of(0, 15));
            System.out.println("Found " + reports.size() + " breakdown reports");
            
            List<Map<String, Object>> result = new ArrayList<>();
            
            for (BreakdownReport report : reports) {
                Map<String, Object> workOrder = new HashMap<>();
                workOrder.put("machineName", report.getMachineName());
                workOrder.put("type", report.getTypeWo());
                workOrder.put("status", report.getWoStatusLocalDescr());
                workOrder.put("duration", formatDuration(report.getDuration()));
                result.add(workOrder);
                System.out.println("Added work order: " + report.getMachineName() + " - " + report.getWoStatusLocalDescr());
            }
            
            System.out.println("Returning " + result.size() + " work orders");
            return result;
        } catch (Exception e) {
            System.err.println("Error in getWorkOrders: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
    
    private String formatDuration(Integer duration) {
        if (duration == null) return "0.00:00";
        int hours = duration / 3600;
        int minutes = (duration % 3600) / 60;
        int seconds = duration % 60;
        return String.format("%d.%02d:%02d", hours, minutes, seconds);
    }
}
