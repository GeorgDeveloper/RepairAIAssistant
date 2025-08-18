package ru.georgdeveloper.assistantcore.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.PageRequest;
import ru.georgdeveloper.assistantcore.model.EquipmentMaintenanceRecord;
import ru.georgdeveloper.assistantcore.repository.EquipmentMaintenanceRepository;

import java.util.List;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Универсальный сервис для получения инструкций по любым неисправностям
 */
@Service
public class EnhancedTroubleshootingService {
    
    @Autowired
    private EquipmentMaintenanceRepository equipmentRepo;
    
    @Autowired
    private OllamaService ollamaService;
    
    @Autowired
    private RepairInstructionsService repairInstructionsService;
    
    /**
     * Универсальный метод для получения инструкций по любым неисправностям
     */
    public String getRepairInstruction(String problemDescription) {
        // Сначала ищем в базе знаний repair_instructions.json
        String knowledgeBaseInstruction = repairInstructionsService.generateRepairInstruction(problemDescription);
        
        if (knowledgeBaseInstruction != null && 
            !knowledgeBaseInstruction.contains("не найдено подходящих инструкций")) {
            return knowledgeBaseInstruction;
        }
        
        // Если в базе знаний нет подходящих инструкций, ищем в БД
        List<EquipmentMaintenanceRecord> similarCases = findSimilarCases(problemDescription);
        String context = buildInstructionContext(similarCases, problemDescription);
        return generateRepairInstruction(context, problemDescription);
    }
    
    private List<EquipmentMaintenanceRecord> findSimilarCases(String problemDescription) {
        String[] keywords = extractKeywords(problemDescription);
        
        // Ищем по каждому ключевому слову
        return Arrays.stream(keywords)
            .filter(keyword -> keyword.length() > 2)
            .flatMap(keyword -> equipmentRepo.findByKeyword(keyword, PageRequest.of(0, 3)).stream())
            .distinct()
            .limit(5)
            .collect(Collectors.toList());
    }
    
    private String[] extractKeywords(String text) {
        return text.toLowerCase()
            .replaceAll("\\b(что|как|почему|делать|устранить|починить|не|на|в|и|или|а|но)\\b", "")
            .replaceAll("[^а-яё\\s]", " ")
            .trim()
            .split("\\s+");
    }
    
    private String buildInstructionContext(List<EquipmentMaintenanceRecord> cases, String problem) {
        StringBuilder sb = new StringBuilder();
        sb.append("Проблема: ").append(problem).append("\n\n");
        
        if (!cases.isEmpty()) {
            sb.append("Похожие случаи из базы ремонтов:\n");
            cases.forEach(record -> {
                sb.append("• Оборудование: ").append(record.getMachineName()).append("\n");
                sb.append("  Проблема: ").append(record.getDescription()).append("\n");
                sb.append("  Решение: ").append(extractSolution(record.getComments())).append("\n\n");
            });
        }
        
        return sb.toString();
    }
    
    private String extractSolution(String comments) {
        if (comments == null || comments.trim().isEmpty()) {
            return "решение не указано";
        }
        
        // Извлекаем часть после "Что ты сделал:"
        String[] parts = comments.split("Что ты сделал:");
        if (parts.length > 1) {
            return parts[1].split("Cause:")[0].trim();
        }
        
        return comments.length() > 200 ? comments.substring(0, 200) + "..." : comments;
    }
    
    private String generateRepairInstruction(String context, String problem) {
        String prompt = String.format("""
            Ты - технический эксперт по ремонту промышленного оборудования.
            На основе данных создай инструкцию по устранению проблемы.
            
            %s
            
            Создай краткую пошаговую инструкцию для: %s
            
            Формат ответа:
            🔧 ИНСТРУКЦИЯ ПО УСТРАНЕНИЮ
            
            1. Диагностика: [как выявить проблему]
            2. Подготовка: [инструменты/материалы]
            3. Устранение: [пошаговые действия]
            4. Проверка: [как убедиться что исправлено]
            
            ⚠️ Безопасность: [важные меры предосторожности]
            
            Используй опыт из похожих случаев, но адаптируй под конкретную проблему.
            """, context, problem);
        
        return ollamaService.generateResponse(prompt);
    }
}