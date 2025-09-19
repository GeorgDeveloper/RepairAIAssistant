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


    @GetMapping("/current-metrics")
    @ResponseBody
    public Map<String, Object> getCurrentMetrics() {
        return monitoringRepository.getCurrentMetrics();
    }
}
