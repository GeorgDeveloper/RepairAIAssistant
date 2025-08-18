package ru.georgdeveloper.assistantcore.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.georgdeveloper.assistantcore.config.ResourcePaths;

import jakarta.annotation.PostConstruct;
import java.io.InputStream;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Сервис для работы с инструкциями по ремонту из repair_instructions.json
 */
@Service
public class RepairInstructionsService {
    
    private static final Logger logger = LoggerFactory.getLogger(RepairInstructionsService.class);
    
    @Autowired
    private OllamaService ollamaService;
    
    @Autowired
    private ResourceValidationService resourceValidationService;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    private List<RepairInstruction> repairInstructions = new ArrayList<>();
    
    @PostConstruct
    public void init() {
        loadRepairInstructions();
    }
    
    /**
     * Загружает инструкции по ремонту из JSON файла
     */
    public void loadRepairInstructions() {
        try {
            if (!resourceValidationService.isResourceAvailable(ResourcePaths.REPAIR_INSTRUCTIONS_JSON)) {
                logger.error("Файл {} не найден в classpath", ResourcePaths.REPAIR_INSTRUCTIONS_JSON);
                repairInstructions = new ArrayList<>();
                return;
            }
            
            ClassPathResource resource = new ClassPathResource(ResourcePaths.REPAIR_INSTRUCTIONS_JSON);
            
            try (InputStream inputStream = resource.getInputStream()) {
                JsonNode rootNode = objectMapper.readTree(inputStream);
                
                if (rootNode.isArray()) {
                    for (JsonNode node : rootNode) {
                        RepairInstruction instruction = parseInstruction(node);
                        if (instruction != null) {
                            repairInstructions.add(instruction);
                        }
                    }
                }
                
                logger.info("Загружено {} инструкций по ремонту", repairInstructions.size());
            }
            
        } catch (Exception e) {
            logger.error("Ошибка загрузки инструкций по ремонту: {}", e.getMessage(), e);
            repairInstructions = new ArrayList<>();
        }
    }
    
    private RepairInstruction parseInstruction(JsonNode node) {
        try {
            String area = getNodeText(node, "Участок");
            String equipmentGroup = getNodeText(node, "Группа оборудования");
            String component = getNodeText(node, "Узел");
            String problem = getNodeText(node, "Проблема");
            String solution = getNodeText(node, "Решение");
            
            if (problem != null && solution != null) {
                return new RepairInstruction(area, equipmentGroup, component, problem, solution);
            }
        } catch (Exception e) {
            logger.warn("Ошибка парсинга инструкции: {}", e.getMessage());
        }
        return null;
    }
    
    private String getNodeText(JsonNode node, String fieldName) {
        JsonNode field = node.get(fieldName);
        return field != null && !field.isNull() ? field.asText().trim() : null;
    }
    
    /**
     * Находит подходящие инструкции по описанию проблемы
     */
    public List<RepairInstruction> findRelevantInstructions(String problemDescription) {
        if (problemDescription == null || problemDescription.trim().isEmpty()) {
            return Collections.emptyList();
        }
        
        String normalizedProblem = problemDescription.toLowerCase(Locale.ROOT);
        String[] keywords = extractKeywords(normalizedProblem);
        
        return repairInstructions.stream()
            .filter(instruction -> matchesKeywords(instruction, keywords))
            .sorted((a, b) -> calculateRelevance(b, keywords) - calculateRelevance(a, keywords))
            .limit(5)
            .collect(Collectors.toList());
    }
    
    private String[] extractKeywords(String text) {
        return text.replaceAll("[^а-яё\\s]", " ")
                  .replaceAll("\\b(что|как|где|когда|не|на|в|и|или|а|но|для|по|с|из|к|от)\\b", "")
                  .trim()
                  .split("\\s+");
    }
    
    private boolean matchesKeywords(RepairInstruction instruction, String[] keywords) {
        String searchText = (instruction.getProblem() + " " + instruction.getSolution()).toLowerCase(Locale.ROOT);
        
        return Arrays.stream(keywords)
            .filter(keyword -> keyword.length() > 2)
            .anyMatch(searchText::contains);
    }
    
    private int calculateRelevance(RepairInstruction instruction, String[] keywords) {
        String searchText = (instruction.getProblem() + " " + instruction.getSolution()).toLowerCase(Locale.ROOT);
        
        return (int) Arrays.stream(keywords)
            .filter(keyword -> keyword.length() > 2)
            .mapToLong(keyword -> searchText.split(keyword, -1).length - 1)
            .sum();
    }
    
    /**
     * Генерирует инструкцию по ремонту с использованием AI и базы знаний
     */
    public String generateRepairInstruction(String problemDescription) {
        List<RepairInstruction> relevantInstructions = findRelevantInstructions(problemDescription);
        
        if (relevantInstructions.isEmpty()) {
            return "К сожалению, в базе знаний не найдено подходящих инструкций для данной проблемы. " +
                   "Рекомендуется обратиться к техническому специалисту.";
        }
        
        StringBuilder context = new StringBuilder();
        context.append("Похожие случаи из базы знаний:\n\n");
        
        for (RepairInstruction instruction : relevantInstructions) {
            context.append("Оборудование: ").append(instruction.getEquipmentGroup())
                   .append(" (").append(instruction.getComponent()).append(")\n");
            context.append("Проблема: ").append(instruction.getProblem()).append("\n");
            context.append("Решение: ").append(instruction.getSolution()).append("\n\n");
        }
        
        String prompt = String.format("""
            Ты - эксперт по ремонту промышленного оборудования.
            На основе похожих случаев создай пошаговую инструкцию.
            
            %s
            
            Проблема для решения: %s
            
            Создай структурированную инструкцию:
            🔧 ИНСТРУКЦИЯ ПО УСТРАНЕНИЮ
            
            1. Диагностика:
            2. Подготовка:
            3. Устранение:
            4. Проверка:
            
            ⚠️ Меры безопасности:
            
            Используй опыт из похожих случаев, но адаптируй под конкретную проблему.
            """, context.toString(), problemDescription);
        
        return ollamaService.generateResponse(prompt);
    }
    
    /**
     * Получает все инструкции для конкретного оборудования
     */
    public List<RepairInstruction> getInstructionsForEquipment(String equipmentName) {
        if (equipmentName == null) return Collections.emptyList();
        
        String normalizedName = equipmentName.toLowerCase(Locale.ROOT);
        
        return repairInstructions.stream()
            .filter(instruction -> 
                instruction.getEquipmentGroup() != null && 
                instruction.getEquipmentGroup().toLowerCase(Locale.ROOT).contains(normalizedName))
            .collect(Collectors.toList());
    }
    
    /**
     * Получает статистику по типам проблем
     */
    public Map<String, Long> getProblemStatistics() {
        return repairInstructions.stream()
            .map(RepairInstruction::getProblem)
            .filter(Objects::nonNull)
            .collect(Collectors.groupingBy(
                problem -> extractProblemType(problem),
                Collectors.counting()
            ));
    }
    
    private String extractProblemType(String problem) {
        String lowerProblem = problem.toLowerCase(Locale.ROOT);
        
        if (lowerProblem.contains("утечка")) return "Утечки";
        if (lowerProblem.contains("не работает") || lowerProblem.contains("неисправ")) return "Неисправности";
        if (lowerProblem.contains("замена") || lowerProblem.contains("износ")) return "Замена деталей";
        if (lowerProblem.contains("калибровка") || lowerProblem.contains("настройка")) return "Настройка";
        if (lowerProblem.contains("датчик")) return "Датчики";
        if (lowerProblem.contains("безопасность")) return "Безопасность";
        
        return "Прочие";
    }
    
    public List<RepairInstruction> getAllInstructions() {
        return new ArrayList<>(repairInstructions);
    }
    
    /**
     * Класс для хранения инструкции по ремонту
     */
    public static class RepairInstruction {
        private final String area;
        private final String equipmentGroup;
        private final String component;
        private final String problem;
        private final String solution;
        
        public RepairInstruction(String area, String equipmentGroup, String component, 
                               String problem, String solution) {
            this.area = area;
            this.equipmentGroup = equipmentGroup;
            this.component = component;
            this.problem = problem;
            this.solution = solution;
        }
        
        public String getArea() { return area; }
        public String getEquipmentGroup() { return equipmentGroup; }
        public String getComponent() { return component; }
        public String getProblem() { return problem; }
        public String getSolution() { return solution; }
        
        @Override
        public String toString() {
            return String.format("RepairInstruction{equipment='%s', component='%s', problem='%s'}", 
                               equipmentGroup, component, problem);
        }
    }
}