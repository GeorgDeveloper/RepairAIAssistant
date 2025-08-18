package ru.georgdeveloper.assistantcore.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import ru.georgdeveloper.assistantcore.service.QueryAnalysisService;

/**
 * Контроллер для управления обучением модели
 */
@RestController
@RequestMapping("/api/training")
public class TrainingController {
    
    @Autowired
    private QueryAnalysisService queryAnalysisService;
    
    /**
     * Добавляет новый пример для обучения
     */
    @PostMapping("/add-example")
    public String addExample(@RequestParam String input, @RequestParam String output) {
        queryAnalysisService.addTrainingExample(input, output);
        return "Пример добавлен: " + input;
    }
    
    /**
     * Тестирует анализ запроса
     */
    @PostMapping("/test")
    public String testAnalysis(@RequestParam String request) {
        var result = queryAnalysisService.analyzeRequest(request);
        if (result.needsDatabase()) {
            return "NEED_DATABASE: " + result.getDataNeeded();
        } else {
            return "SIMPLE_ANSWER: " + result.getSimpleAnswer();
        }
    }
}