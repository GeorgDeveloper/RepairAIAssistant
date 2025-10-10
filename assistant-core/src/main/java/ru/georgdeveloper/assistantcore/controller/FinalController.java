package ru.georgdeveloper.assistantcore.controller;

import org.springframework.beans.factory.annotation.Autowired;
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

    @GetMapping("/data")
    public List<Map<String, Object>> data(
            @RequestParam(required = false) List<Integer> year,
            @RequestParam(required = false) List<Integer> month,
            @RequestParam(required = false, defaultValue = "6") Integer limit
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
}


