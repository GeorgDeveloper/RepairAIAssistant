package ru.georgdeveloper.analyzer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;

@Service
public class AnalyzerService {
    
    private static final Logger logger = LoggerFactory.getLogger(AnalyzerService.class);
    
    @Autowired
    private EquipmentAnalyzer equipmentAnalyzer;
    
    @Value("${files.upload-dir:./uploads}")
    private String uploadDir;
    
    public List<EquipmentAnalyzer.RepairInstruction> analyzeEquipmentFile(String fileName) {
        String filePath = uploadDir + File.separator + fileName;
        
        if (!new File(filePath).exists()) {
            logger.error("Файл не найден: {}", filePath);
            throw new RuntimeException("Файл не найден: " + fileName);
        }
        
        return equipmentAnalyzer.analyzeEquipmentData(filePath);
    }
    
    public void saveAnalysisResults(List<EquipmentAnalyzer.RepairInstruction> instructions, String outputFileName) {
        String outputPath = uploadDir + File.separator + outputFileName;
        equipmentAnalyzer.saveToJson(instructions, outputPath);
    }
    
    public String processEquipmentFile(String csvFileName) {
        try {
            List<EquipmentAnalyzer.RepairInstruction> instructions = analyzeEquipmentFile(csvFileName);
            
            String outputFileName = csvFileName.replace(".csv", "_analysis.json");
            saveAnalysisResults(instructions, outputFileName);
            
            return String.format("Анализ завершен. Создано %d инструкций. Результат: %s", 
                               instructions.size(), outputFileName);
                               
        } catch (Exception e) {
            logger.error("Ошибка обработки файла {}: {}", csvFileName, e.getMessage());
            return "Ошибка обработки файла: " + e.getMessage();
        }
    }
}