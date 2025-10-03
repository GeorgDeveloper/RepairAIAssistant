package ru.georgdeveloper.assistantai.controller;

import org.springframework.web.bind.annotation.*;
import ru.georgdeveloper.assistantai.service.QueryAnalysisService;
import ru.georgdeveloper.assistantai.service.ModelTrainingService;

@RestController
@RequestMapping("/api/training")
public class TrainingController {

    private final QueryAnalysisService queryAnalysisService;
    private final ModelTrainingService modelTrainingService;

    public TrainingController(QueryAnalysisService queryAnalysisService, ModelTrainingService modelTrainingService) {
        this.queryAnalysisService = queryAnalysisService;
        this.modelTrainingService = modelTrainingService;
    }

    @PostMapping("/add-example")
    public String addExample(@RequestParam String input, @RequestParam String output) {
        modelTrainingService.addTrainingExample(input, output);
        return "Пример добавлен: " + input;
    }

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


