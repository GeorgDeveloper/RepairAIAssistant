package ru.georgdeveloper.assistantcore.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.georgdeveloper.assistantcore.service.FinalService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/final")
public class FinalController {

    @Autowired
    private FinalService finalService;

    @Value("${app.final.months-limit:12}")
    private int defaultMonthsLimit;

    @GetMapping("/data")
    public List<Map<String, Object>> data(
            @RequestParam(required = false) List<Integer> year,
            @RequestParam(required = false) List<Integer> month,
            @RequestParam(required = false, defaultValue = "${app.final.months-limit:12}") Integer limit
    ) {
        return finalService.getSummaries(year, month, limit);
    }

    @GetMapping("/years")
    public List<Map<String, Object>> years() {
        return finalService.getYears();
    }

    @GetMapping("/months")
    public List<Map<String, Object>> months(@RequestParam(required = false) List<Integer> year) {
        return finalService.getMonths(year);
    }

    @GetMapping("/config")
    public Map<String, Object> config() {
        Map<String, Object> config = new java.util.HashMap<>();
        config.put("monthsLimit", defaultMonthsLimit);
        return config;
    }
}


