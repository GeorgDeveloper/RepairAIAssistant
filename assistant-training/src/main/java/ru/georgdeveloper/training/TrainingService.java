package ru.georgdeveloper.training;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TrainingService {
    
    private static final Logger logger = LoggerFactory.getLogger(TrainingService.class);
    
    @Value("${ai.training.enabled:true}")
    private boolean trainingEnabled;
    
    @Value("${ai.training.batch-size:200}")
    private int batchSize;
    
    @Value("${ai.training.max-examples:1000}")
    private int maxExamples;
    

    private List<TrainingExample> trainingData = new ArrayList<>();
    
    @PostConstruct
    public void init() {
        if (trainingEnabled) {
            loadTrainingData();
        }
    }
    
    public void loadTrainingData() {
        try {
            trainingData.clear();
            // Загрузка из файлов обучения
            logger.info("Загружено {} примеров для обучения", trainingData.size());
        } catch (Exception e) {
            logger.error("Ошибка загрузки обучающих данных: {}", e.getMessage());
        }
    }
    
    @Scheduled(cron = "${ai.training.schedule:0 0 3 * * ?}")
    public void scheduledTraining() {
        if (trainingEnabled) {
            logger.info("Запуск планового переобучения модели");
            loadTrainingData();
        }
    }
    
    public void addExample(String input, String output) {
        if (trainingData.size() >= maxExamples) {
            trainingData.remove(0);
        }
        trainingData.add(new TrainingExample(input, output));
    }
    
    public List<TrainingExample> getRelevantExamples(String query, int limit) {
        return trainingData.stream()
            .filter(example -> isRelevant(example, query))
            .limit(limit)
            .collect(Collectors.toList());
    }
    
    private boolean isRelevant(TrainingExample example, String query) {
        String[] queryWords = query.toLowerCase().split("\\s+");
        String exampleText = example.getInput().toLowerCase();
        
        for (String word : queryWords) {
            if (exampleText.contains(word)) {
                return true;
            }
        }
        return false;
    }
    
    public static class TrainingExample {
        private final String input;
        private final String output;
        
        public TrainingExample(String input, String output) {
            this.input = input;
            this.output = output;
        }
        
        public String getInput() { return input; }
        public String getOutput() { return output; }
    }
}