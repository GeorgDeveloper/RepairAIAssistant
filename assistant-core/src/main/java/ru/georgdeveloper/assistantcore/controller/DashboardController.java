package ru.georgdeveloper.assistantcore.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import ru.georgdeveloper.assistantcore.repository.MonitoringRepository;

@RestController
@RequestMapping("/dashboard")
public class DashboardController {

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


    @GetMapping("/current-metrics")
    @ResponseBody
    public Map<String, Object> getCurrentMetrics() {
        return monitoringRepository.getCurrentMetrics();
    }
}
