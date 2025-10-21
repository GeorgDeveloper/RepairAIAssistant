package ru.georgdeveloper.assistantcore.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
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
    
    @Autowired
    @Qualifier("sqlServerJdbcTemplate")
    private JdbcTemplate sqlServerJdbcTemplate;
    
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
            System.out.println("Starting getWorkOrders...");
            
            // Прямой запрос к SQL Server
            String sql = "SELECT TOP 15 MachineName, TYPEWO, WOStatusLocalDescr, Duration " +
                        "FROM REP_BreakdownReport " +
                        "ORDER BY Date_T1 DESC";
            
            List<Map<String, Object>> rows = sqlServerJdbcTemplate.queryForList(sql);
            System.out.println("Found " + rows.size() + " breakdown reports from SQL Server");
            
            List<Map<String, Object>> result = new ArrayList<>();
            
            for (Map<String, Object> row : rows) {
                Map<String, Object> workOrder = new HashMap<>();
                workOrder.put("machineName", row.get("MachineName"));
                workOrder.put("type", row.get("TYPEWO"));
                workOrder.put("status", row.get("WOStatusLocalDescr"));
                
                // Форматируем продолжительность
                Object duration = row.get("Duration");
                String formattedDuration = "0.00:00";
                if (duration != null) {
                    if (duration instanceof Integer) {
                        formattedDuration = formatDuration((Integer) duration);
                    } else if (duration instanceof String) {
                        formattedDuration = (String) duration;
                    }
                }
                workOrder.put("duration", formattedDuration);
                
                result.add(workOrder);
                System.out.println("Added work order: " + row.get("MachineName") + " - " + row.get("WOStatusLocalDescr"));
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
