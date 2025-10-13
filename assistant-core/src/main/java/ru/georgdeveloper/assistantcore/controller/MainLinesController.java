package ru.georgdeveloper.assistantcore.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import ru.georgdeveloper.assistantcore.service.MainLinesService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/dashboard")
public class MainLinesController {

    @Autowired
    private MainLinesService mainLinesService;

    @GetMapping("/main-lines/bd")
    @ResponseBody
    public List<Map<String, Object>> getMainLinesBdMetrics() {
        return mainLinesService.getMainLinesBdMetrics();
    }

    @GetMapping("/main-lines/availability")
    @ResponseBody
    public List<Map<String, Object>> getMainLinesAvailabilityMetrics() {
        return mainLinesService.getMainLinesAvailabilityMetrics();
    }

    @GetMapping("/main-lines/current")
    @ResponseBody
    public List<Map<String, Object>> getCurrentMainLinesMetrics() {
        return mainLinesService.getCurrentMainLinesMetrics();
    }

    @GetMapping("/main-lines/by-area")
    @ResponseBody
    public List<Map<String, Object>> getMainLinesMetricsByArea(@RequestParam String area) {
        return mainLinesService.getMainLinesMetricsByArea(area);
    }
}
