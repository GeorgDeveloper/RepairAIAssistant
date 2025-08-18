package ru.georgdeveloper.assistantcore.service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Тесты для RepairInstructionsService
 */
@SpringBootTest
@SpringJUnitConfig
public class RepairInstructionsServiceTest {
    
    @Autowired
    private RepairInstructionsService repairInstructionsService;
    
    @Test
    public void testLoadRepairInstructions() {
        // Проверяем, что инструкции загружаются
        List<RepairInstructionsService.RepairInstruction> instructions = 
            repairInstructionsService.getAllInstructions();
        
        assertNotNull(instructions);
        assertTrue(instructions.size() > 0, "Должны быть загружены инструкции из repair_instructions.json");
        
        // Проверяем структуру первой инструкции
        RepairInstructionsService.RepairInstruction firstInstruction = instructions.get(0);
        assertNotNull(firstInstruction.getProblem());
        assertNotNull(firstInstruction.getSolution());
        
        System.out.println("Загружено инструкций: " + instructions.size());
        System.out.println("Пример инструкции: " + firstInstruction.getProblem());
    }
    
    @Test
    public void testFindRelevantInstructions() {
        // Тестируем поиск по утечкам
        List<RepairInstructionsService.RepairInstruction> leakInstructions = 
            repairInstructionsService.findRelevantInstructions("утечка азота");
        
        assertNotNull(leakInstructions);
        System.out.println("Найдено инструкций по утечке азота: " + leakInstructions.size());
        
        // Тестируем поиск по неисправностям
        List<RepairInstructionsService.RepairInstruction> malfunctionInstructions = 
            repairInstructionsService.findRelevantInstructions("не работает датчик");
        
        assertNotNull(malfunctionInstructions);
        System.out.println("Найдено инструкций по неисправности датчика: " + malfunctionInstructions.size());
    }
    
    @Test
    public void testGetInstructionsForEquipment() {
        // Тестируем поиск по оборудованию
        List<RepairInstructionsService.RepairInstruction> hfvInstructions = 
            repairInstructionsService.getInstructionsForEquipment("HFV2");
        
        assertNotNull(hfvInstructions);
        System.out.println("Найдено инструкций для HFV2: " + hfvInstructions.size());
        
        if (!hfvInstructions.isEmpty()) {
            RepairInstructionsService.RepairInstruction example = hfvInstructions.get(0);
            System.out.println("Пример для HFV2: " + example.getProblem());
        }
    }
    
    @Test
    public void testGetProblemStatistics() {
        Map<String, Long> statistics = repairInstructionsService.getProblemStatistics();
        
        assertNotNull(statistics);
        assertTrue(statistics.size() > 0, "Должна быть статистика по типам проблем");
        
        System.out.println("Статистика по типам проблем:");
        statistics.forEach((type, count) -> 
            System.out.println(type + ": " + count + " случаев"));
    }
    
    @Test
    public void testGenerateRepairInstruction() {
        // Тестируем генерацию инструкции
        String instruction = repairInstructionsService.generateRepairInstruction("утечка пара");
        
        assertNotNull(instruction);
        assertFalse(instruction.trim().isEmpty());
        
        System.out.println("Сгенерированная инструкция для 'утечка пара':");
        System.out.println(instruction);
    }
}