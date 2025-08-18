package ru.georgdeveloper.analyzer;

import com.opencsv.CSVReader;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
public class EquipmentAnalyzer {
    
    private static final Logger logger = LoggerFactory.getLogger(EquipmentAnalyzer.class);
    private static final Pattern PROBLEM_PATTERN = Pattern.compile("Что Произошло:\\s*([^;]+)");
    private static final Pattern SOLUTION_PATTERN = Pattern.compile("Что ты сделал:\\s*([^;]+)");
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    public List<RepairInstruction> analyzeEquipmentData(String csvFilePath) {
        logger.info("Начинаем анализ файла: {}", csvFilePath);
        
        Map<String, Map<String, Map<String, List<ProblemSolution>>>> groupedData = new HashMap<>();
        
        try (CSVReader reader = new CSVReader(new FileReader(csvFilePath))) {
            String[] headers = reader.readNext();
            Map<String, Integer> headerMap = createHeaderMap(headers);
            
            String[] row;
            while ((row = reader.readNext()) != null) {
                processRow(row, headerMap, groupedData);
            }
            
        } catch (Exception e) {
            logger.error("Ошибка чтения CSV файла: {}", e.getMessage());
            return Collections.emptyList();
        }
        
        return createRepairInstructions(groupedData);
    }
    
    private Map<String, Integer> createHeaderMap(String[] headers) {
        Map<String, Integer> map = new HashMap<>();
        for (int i = 0; i < headers.length; i++) {
            map.put(headers[i], i);
        }
        return map;
    }
    
    private void processRow(String[] row, Map<String, Integer> headerMap, 
                          Map<String, Map<String, Map<String, List<ProblemSolution>>>> groupedData) {
        
        String area = getValue(row, headerMap, "area", "Unknown");
        String machineName = getValue(row, headerMap, "machine_name", "Unknown");
        String mechanismNode = getValue(row, headerMap, "mechanism_node", "Unknown");
        String comments = getValue(row, headerMap, "comments", "");
        
        ProblemSolution ps = extractProblemAndSolution(comments);
        if (ps.isValid()) {
            groupedData.computeIfAbsent(area, k -> new HashMap<>())
                      .computeIfAbsent(machineName, k -> new HashMap<>())
                      .computeIfAbsent(mechanismNode, k -> new ArrayList<>())
                      .add(ps);
        }
    }
    
    private String getValue(String[] row, Map<String, Integer> headerMap, String column, String defaultValue) {
        Integer index = headerMap.get(column);
        return (index != null && index < row.length) ? row[index] : defaultValue;
    }
    
    private ProblemSolution extractProblemAndSolution(String comments) {
        if (comments == null || comments.trim().isEmpty()) {
            return new ProblemSolution("", "");
        }
        
        String problem = extractMatch(PROBLEM_PATTERN, comments);
        String solution = extractMatch(SOLUTION_PATTERN, comments);
        
        return new ProblemSolution(problem, solution);
    }
    
    private String extractMatch(Pattern pattern, String text) {
        Matcher matcher = pattern.matcher(text);
        return matcher.find() ? matcher.group(1).trim() : "";
    }
    
    private List<RepairInstruction> createRepairInstructions(
            Map<String, Map<String, Map<String, List<ProblemSolution>>>> groupedData) {
        
        List<RepairInstruction> instructions = new ArrayList<>();
        
        groupedData.forEach((area, machines) ->
            machines.forEach((machine, nodes) ->
                nodes.forEach((node, repairs) -> {
                    Map<String, Set<String>> problemGroups = repairs.stream()
                        .collect(Collectors.groupingBy(
                            ProblemSolution::getProblem,
                            Collectors.mapping(ProblemSolution::getSolution, Collectors.toSet())
                        ));
                    
                    problemGroups.forEach((problem, solutions) -> {
                        String combinedSolution = String.join("; ", solutions);
                        instructions.add(new RepairInstruction(area, machine, node, problem, combinedSolution));
                    });
                })
            )
        );
        
        logger.info("Создано {} инструкций по ремонту", instructions.size());
        return instructions;
    }
    
    public void saveToJson(List<RepairInstruction> instructions, String outputPath) {
        try {
            objectMapper.writerWithDefaultPrettyPrinter()
                       .writeValue(new File(outputPath), instructions);
            logger.info("Результат сохранен в файл: {}", outputPath);
        } catch (IOException e) {
            logger.error("Ошибка сохранения в JSON: {}", e.getMessage());
        }
    }
    
    private static class ProblemSolution {
        private final String problem;
        private final String solution;
        
        public ProblemSolution(String problem, String solution) {
            this.problem = problem;
            this.solution = solution;
        }
        
        public String getProblem() { return problem; }
        public String getSolution() { return solution; }
        public boolean isValid() { return !problem.isEmpty() && !solution.isEmpty(); }
    }
    
    public static class RepairInstruction {
        private String area;
        private String equipmentGroup;
        private String component;
        private String problem;
        private String solution;
        
        public RepairInstruction() {}
        
        public RepairInstruction(String area, String equipmentGroup, String component, 
                               String problem, String solution) {
            this.area = area;
            this.equipmentGroup = equipmentGroup;
            this.component = component;
            this.problem = problem;
            this.solution = solution;
        }
        
        // Getters and setters
        public String getArea() { return area; }
        public void setArea(String area) { this.area = area; }
        
        public String getEquipmentGroup() { return equipmentGroup; }
        public void setEquipmentGroup(String equipmentGroup) { this.equipmentGroup = equipmentGroup; }
        
        public String getComponent() { return component; }
        public void setComponent(String component) { this.component = component; }
        
        public String getProblem() { return problem; }
        public void setProblem(String problem) { this.problem = problem; }
        
        public String getSolution() { return solution; }
        public void setSolution(String solution) { this.solution = solution; }
    }
}