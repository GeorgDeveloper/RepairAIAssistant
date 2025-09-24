package ru.georgdeveloper.assistantcore.controller;

import org.springframework.web.bind.annotation.*;
import ru.georgdeveloper.assistantcore.service.RepairInstructionsService;

import java.util.List;
import java.util.Map;

/**
 * Контроллер для работы с инструкциями по ремонту.
 * 
 * Переведён на конструкторную инъекцию зависимостей для лучшей тестируемости
 * и предсказуемости жизненного цикла бинов.
 */
@RestController
@RequestMapping("/api/repair-instructions")
public class RepairInstructionsController {

    private final RepairInstructionsService repairInstructionsService;

    /**
     * Конструктор контроллера
     * @param repairInstructionsService сервис работы с инструкциями
     */
    public RepairInstructionsController(RepairInstructionsService repairInstructionsService) {
        this.repairInstructionsService = repairInstructionsService;
    }

    /**
     * Получить инструкцию по описанию проблемы
     */
    @PostMapping("/generate")
    public String generateInstruction(@RequestParam String problem) {
        return repairInstructionsService.generateRepairInstruction(problem);
    }

    /**
     * Найти релевантные инструкции
     */
    @PostMapping("/search")
    public List<RepairInstructionsService.RepairInstruction> searchInstructions(@RequestParam String problem) {
        return repairInstructionsService.findRelevantInstructions(problem);
    }

    /**
     * Получить инструкции для конкретного оборудования
     */
    @GetMapping("/equipment/{equipmentName}")
    public List<RepairInstructionsService.RepairInstruction> getInstructionsForEquipment(@PathVariable String equipmentName) {
        return repairInstructionsService.getInstructionsForEquipment(equipmentName);
    }

    /**
     * Получить статистику по типам проблем
     */
    @GetMapping("/statistics")
    public Map<String, Long> getProblemStatistics() {
        return repairInstructionsService.getProblemStatistics();
    }

    /**
     * Перезагрузить инструкции из файла
     */
    @PostMapping("/reload")
    public String reloadInstructions() {
        repairInstructionsService.loadRepairInstructions();
        return "Инструкции перезагружены из repair_instructions.json";
    }

    /**
     * Получить количество загруженных инструкций
     */
    @GetMapping("/count")
    public int getInstructionsCount() {
        return repairInstructionsService.getAllInstructions().size();
    }
}