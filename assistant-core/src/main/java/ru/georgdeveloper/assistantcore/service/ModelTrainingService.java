package ru.georgdeveloper.assistantcore.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.PageRequest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.annotation.Scheduled;
import ru.georgdeveloper.assistantcore.model.EquipmentMaintenanceRecord;
import ru.georgdeveloper.assistantcore.repository.EquipmentMaintenanceRepository;
import ru.georgdeveloper.assistantcore.config.ResourcePaths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

import jakarta.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Упрощенный сервис для обучения модели
 */
@Service
public class ModelTrainingService {
    
    private static final Logger logger = LoggerFactory.getLogger(ModelTrainingService.class);
    

    
    @Autowired
    private EquipmentMaintenanceRepository equipmentMaintenanceRepository;
    
    @Autowired
    private RepairInstructionsService repairInstructionsService;
    
    @Autowired
    private OllamaService ollamaService;
    
    @Value("${ai.training.batch-size:200}")
    private int batchSize;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    private List<TrainingExample> trainingData;
    
    @PostConstruct
    public void init() {
        loadTrainingData();
        loadRepairInstructionsData();
        trainOnHistoricalData();
    }
    
    /**
     * Загружает обучающие данные из файла
     */
    public void loadTrainingData() {
        try {
            trainingData = new ArrayList<>();
            ClassPathResource resource = new ClassPathResource(ResourcePaths.QUERY_TRAINING_DATA_JSONL);
            
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    JsonNode node = objectMapper.readTree(line);
                    String input = node.get("input").asText();
                    String output = node.get("output").asText();
                    trainingData.add(new TrainingExample(input, output));
                }
            }
            
            logger.info("Загружено {} примеров для обучения", trainingData.size());
        } catch (Exception e) {
            logger.error("Ошибка загрузки обучающих данных: {}", e.getMessage(), e);
            trainingData = new ArrayList<>();
        }
    }
    
    /**
     * Загружает данные из repair_instructions.json для обучения
     */
    public void loadRepairInstructionsData() {
        try {
            List<RepairInstructionsService.RepairInstruction> instructions = 
                repairInstructionsService.getAllInstructions();
            
            if (trainingData == null) trainingData = new ArrayList<>();
            
            for (RepairInstructionsService.RepairInstruction instruction : instructions) {
                // Создаем примеры для обучения на основе инструкций
                String input = "Как устранить: " + instruction.getProblem();
                String output = "SIMPLE_ANSWER: " + instruction.getSolution();
                trainingData.add(new TrainingExample(input, output));
                
                // Добавляем вариации запросов
                if (instruction.getProblem().toLowerCase(Locale.ROOT).contains("утечка")) {
                    String leakInput = "Утечка на " + instruction.getEquipmentGroup();
                    trainingData.add(new TrainingExample(leakInput, output));
                }
                
                if (instruction.getProblem().toLowerCase(Locale.ROOT).contains("не работает")) {
                    String notWorkingInput = "Не работает " + instruction.getComponent();
                    trainingData.add(new TrainingExample(notWorkingInput, output));
                }
            }
            
            logger.info("Добавлено {} примеров из repair_instructions.json", 
                       instructions.size() * 2); // примерно 2 примера на инструкцию
            
        } catch (Exception e) {
            logger.error("Ошибка загрузки данных из repair_instructions.json: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Анализирует запрос с использованием обученной модели
     */
    public String analyzeWithTraining(String request) {
        if (trainingData == null || trainingData.isEmpty()) {
            loadTrainingData();
            trainOnHistoricalData();
        }
        
        // Ограничиваем количество примеров для промпта
        List<TrainingExample> relevantExamples = getRelevantExamples(request, 10);
        
        StringBuilder prompt = new StringBuilder();
        prompt.append("Ты - анализатор запросов для системы ремонта оборудования.\n");
        prompt.append("Изучи примеры и отвечай в том же формате:\n\n");
        
        for (TrainingExample example : relevantExamples) {
            prompt.append("Запрос: ").append(example.getInput()).append("\n");
            prompt.append("Ответ: ").append(example.getOutput()).append("\n\n");
        }
        
        prompt.append("Теперь проанализируй этот запрос:\n");
        prompt.append("Запрос: ").append(request).append("\n");
        prompt.append("Ответ: ");
        
        return ollamaService.generateResponse(prompt.toString());
    }
    
    private List<TrainingExample> getRelevantExamples(String request, int limit) {
        if (trainingData.size() <= limit) {
            return trainingData;
        }
        
        // Простая релевантность по ключевым словам
        String[] keywords = request.toLowerCase(Locale.ROOT).split("\\s+");
        
        return trainingData.stream()
            .filter(example -> {
                String input = example.getInput().toLowerCase(Locale.ROOT);
                for (String keyword : keywords) {
                    if (input.contains(keyword)) {
                        return true;
                    }
                }
                return false;
            })
            .limit(limit)
            .collect(Collectors.toList());
    }
    
    /**
     * Обучение на исторических данных из БД
     */
    public void trainOnHistoricalData() {
        try {
            // Получаем разнообразные данные для обучения
            List<EquipmentMaintenanceRecord> recentRecords = equipmentMaintenanceRepository
                .findRecentRecords(PageRequest.of(0, 200));
            
            List<EquipmentMaintenanceRecord> mechanicsRecords = equipmentMaintenanceRepository
                .findAll().stream()
                .filter(r -> r.getFailureType() != null && r.getFailureType().contains("Механика"))
                .limit(100)
                .collect(Collectors.toList());
            
            List<EquipmentMaintenanceRecord> leakRecords = equipmentMaintenanceRepository
                .findAll().stream()
                .filter(r -> (r.getDescription() != null && r.getDescription().toLowerCase(Locale.ROOT).contains("утечка")) ||
                           (r.getCause() != null && r.getCause().toLowerCase(Locale.ROOT).contains("утечка")) ||
                           (r.getComments() != null && r.getComments().toLowerCase(Locale.ROOT).contains("утечка")))
                .limit(50)
                .collect(Collectors.toList());
            
            // Объединяем все записи
            List<EquipmentMaintenanceRecord> allRecords = new ArrayList<>();
            allRecords.addAll(recentRecords);
            allRecords.addAll(mechanicsRecords);
            allRecords.addAll(leakRecords);
            
            List<TrainingExample> examples = allRecords.stream()
                .map(this::createTrainingExample)
                .collect(Collectors.toList());
            
            if (trainingData == null) trainingData = new ArrayList<>();
            trainingData.addAll(examples);
            
            logger.info("Добавлено {} примеров из БД (в т.ч. {} по утечкам)", examples.size(), leakRecords.size());
        } catch (Exception e) {
            logger.error("Ошибка обучения на исторических данных: {}", e.getMessage(), e);
        }
    }
    
    private TrainingExample createTrainingExample(EquipmentMaintenanceRecord record) {
        // Создаем разные типы обучающих примеров
        String description = record.getDescription() != null ? record.getDescription() : "не указана";
        String cause = record.getCause() != null ? record.getCause() : "не указана";
        String comments = record.getComments() != null ? record.getComments() : "нет комментариев";
        
        // Для инструкций по ремонту
        if (description.toLowerCase(Locale.ROOT).contains("утечка") || 
            cause.toLowerCase(Locale.ROOT).contains("утечка") ||
            comments.toLowerCase(Locale.ROOT).contains("утечка")) {
            
            String input = "Утечка " + extractLeakType(description, cause, comments) + ". Напиши инструкцию по устранению.";
            String solution = extractSolution(comments);
            
            String output = String.format(
                "SIMPLE_ANSWER: Инструкция по устранению утечки:\n1. Осмотрите оборудование %s\n2. Проверьте: %s\n3. Выполните: %s\n4. Проверьте работу оборудования",
                record.getMachineName() != null ? record.getMachineName() : "оборудование",
                cause,
                solution
            );
            
            return new TrainingExample(input, output);
        }
        
        // Для частых поломок
        if (record.getFailureType() != null) {
            String input = "Какие самые частые поломки по " + record.getFailureType().toLowerCase(Locale.ROOT) + " на оборудовании?";
            String output = String.format(
                "NEED_DATABASE:STATUS=NONE|MACHINE=NONE|LIMIT=100|FAILURE_TYPE=%s",
                record.getFailureType()
            );
            return new TrainingExample(input, output);
        }
        
        // Обычные запросы
        String input = String.format(
            "Какие ремонты были для %s?",
            record.getMachineName() != null ? record.getMachineName() : "оборудования"
        );
        
        String output = String.format(
            "NEED_DATABASE:STATUS=NONE|MACHINE=%s|LIMIT=50|ORDER_BY_DATE=true",
            record.getMachineName() != null ? record.getMachineName() : "NONE"
        );
        
        return new TrainingExample(input, output);
    }
    
    private String extractLeakType(String description, String cause, String comments) {
        String text = (description + " " + cause + " " + comments).toLowerCase(Locale.ROOT);
        if (text.contains("воздух")) return "воздуха";
        if (text.contains("масл")) return "масла";
        if (text.contains("пар")) return "пара";
        if (text.contains("вод")) return "воды";
        if (text.contains("азот")) return "азота";
        return "жидкости";
    }
    
    private String extractSolution(String comments) {
        if (comments == null) return "обратитесь к специалисту";
        
        String text = comments.toLowerCase(Locale.ROOT);
        if (text.contains("замен")) return "замените поврежденные детали";
        if (text.contains("подтян")) return "подтяните соединения";
        if (text.contains("очист")) return "очистите поверхности";
        if (text.contains("регулир")) return "отрегулируйте параметры";
        
        return "проверьте состояние узлов";
    }
    
    @Scheduled(cron = "0 0 3 * * ?") // Каждый день в 3 ночи
    public void retrainModel() {
        logger.info("Начинаем переобучение модели...");
        trainOnHistoricalData();
        loadRepairInstructionsData();
    }
    
    /**
     * Добавляет новый пример для обучения
     */
    public void addTrainingExample(String input, String output) {
        if (trainingData == null) {
            loadTrainingData();
        }
        trainingData.add(new TrainingExample(input, output));
        if (input != null && !input.trim().isEmpty()) {
            logger.info("Добавлен новый пример обучения: {}", input.length() > 100 ? input.substring(0, 100) + "..." : input);
        }
    }
    
    /**
     * Класс для хранения примера обучения
     */
    public static class TrainingExample {
        private final String input;
        private final String output;
        
        public TrainingExample(String input, String output) {
            this.input = input;
            this.output = output;
        }
        
        public String getInput() { return input; }
        public String getOutput() { return output; }
        
        @Override
        public String toString() {
            return "TrainingExample{input='" + input + "', output='" + output + "'}";
        }
    }
}