package ru.georgdeveloper.assistantcore.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.georgdeveloper.assistantcore.repository.EquipmentMaintenanceRepository;
import ru.georgdeveloper.assistantcore.model.EquipmentMaintenanceRecord;
import org.springframework.data.domain.PageRequest;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * Сервис для предоставления инструкций по устранению неисправностей
 */
@Service
public class TroubleshootingService {
    
    @Autowired
    private EquipmentMaintenanceRepository equipmentMaintenanceRepository;
    
    @Autowired
    private OllamaService ollamaService;
    
    private static final Map<String, String> TROUBLESHOOTING_INSTRUCTIONS = new HashMap<>();
    
    static {
        TROUBLESHOOTING_INSTRUCTIONS.put("утечка азота", 
            "## Инструкция по устранению утечки азота\n\n" +
            "### 1. Идентификация проблемы:\n" +
            "• Провести тестовую варку для подтверждения утечки\n" +
            "• Визуально осмотреть оборудование на наличие явных повреждений\n\n" +
            "### 2. Локализация утечки:\n" +
            "• Проверить соединения РВД (рукавов высокого давления)\n" +
            "• Осмотреть датчики формовки и высокого давления\n" +
            "• Проверить латунные переходники\n\n" +
            "### 3. Демонтаж поврежденных компонентов:\n" +
            "• Отключить подачу азота\n" +
            "• Демонтировать поврежденный датчик или РВД\n" +
            "• Осмотреть уплотнители\n\n" +
            "### 4. Замена компонентов:\n" +
            "• Заменить поврежденные детали (РВД, датчики, переходники)\n" +
            "• Убедиться в наличии и качестве уплотнителей\n" +
            "• Использовать только оригинальные запчасти\n\n" +
            "### 5. Сборка и проверка:\n" +
            "• Установить новые компоненты\n" +
            "• Провести тестовую варку для проверки герметичности\n" +
            "• Проверить рабочее давление\n\n" +
            "### 6. Фиксация результатов:\n" +
            "• Задокументировать проведенные работы\n" +
            "• Убедиться, что утечка устранена\n" +
            "• Внести запись в журнал ремонтов\n\n" +
            "⚠️ **ВАЖНО**: При работе с азотом соблюдайте меры безопасности - используйте СИЗ, убедитесь в хорошей вентиляции помещения.");
        
        TROUBLESHOOTING_INSTRUCTIONS.put("падение давления", 
            "## Инструкция по устранению падения давления\n\n" +
            "### 1. Диагностика:\n" +
            "• Проверить показания манометров\n" +
            "• Осмотреть пневматические соединения\n" +
            "• Проверить состояние фильтров\n\n" +
            "### 2. Поиск утечек:\n" +
            "• Использовать мыльный раствор для обнаружения утечек\n" +
            "• Проверить все соединения и фитинги\n" +
            "• Осмотреть пневматические цилиндры\n\n" +
            "### 3. Устранение:\n" +
            "• Подтянуть ослабленные соединения\n" +
            "• Заменить поврежденные уплотнители\n" +
            "• Очистить или заменить фильтры\n\n" +
            "### 4. Проверка:\n" +
            "• Восстановить рабочее давление\n" +
            "• Провести тест на герметичность\n" +
            "• Проверить стабильность давления");
            
        TROUBLESHOOTING_INSTRUCTIONS.put("неисправность датчика", 
            "## Инструкция по замене датчика\n\n" +
            "### 1. Диагностика:\n" +
            "• Проверить показания датчика\n" +
            "• Измерить сопротивление мультиметром\n" +
            "• Проверить целостность проводки\n\n" +
            "### 2. Демонтаж:\n" +
            "• Отключить питание\n" +
            "• Отсоединить разъемы\n" +
            "• Выкрутить датчик из посадочного места\n\n" +
            "### 3. Установка:\n" +
            "• Установить новый датчик\n" +
            "• Подключить разъемы\n" +
            "• Проверить надежность крепления\n\n" +
            "### 4. Калибровка:\n" +
            "• Выполнить калибровку датчика\n" +
            "• Проверить точность показаний\n" +
            "• Провести тестовый цикл");
    }
    
    /**
     * Получает инструкцию по устранению неисправности
     */
    public String getTroubleshootingInstruction(String problem) {
        String lowerProblem = problem.toLowerCase();
        
        // Поиск готовых инструкций
        for (Map.Entry<String, String> entry : TROUBLESHOOTING_INSTRUCTIONS.entrySet()) {
            if (lowerProblem.contains(entry.getKey())) {
                return addRealExamples(entry.getValue(), entry.getKey());
            }
        }
        
        // Если готовой инструкции нет, генерируем через AI
        return generateInstructionWithAI(problem);
    }
    
    /**
     * Добавляет реальные примеры из базы данных к инструкции
     */
    private String addRealExamples(String instruction, String problemType) {
        try {
            List<EquipmentMaintenanceRecord> examples = equipmentMaintenanceRepository
                .findByCommentsContaining(problemType, PageRequest.of(0, 3));
            
            if (!examples.isEmpty()) {
                StringBuilder result = new StringBuilder(instruction);
                result.append("\n\n### Примеры из практики:\n\n");
                
                for (EquipmentMaintenanceRecord record : examples) {
                    result.append(String.format("**%s** (%s):\n", 
                        record.getMachineName(), record.getDate()));
                    result.append(String.format("Проблема: %s\n", record.getDescription()));
                    if (record.getComments() != null && record.getComments().length() > 0) {
                        String comment = record.getComments().length() > 200 ? 
                            record.getComments().substring(0, 200) + "..." : 
                            record.getComments();
                        result.append(String.format("Решение: %s\n\n", comment));
                    }
                }
                
                return result.toString();
            }
        } catch (Exception e) {
            System.err.println("Ошибка получения примеров: " + e.getMessage());
        }
        
        return instruction;
    }
    
    /**
     * Генерирует инструкцию через AI на основе данных из БД
     */
    private String generateInstructionWithAI(String problem) {
        try {
            // Ищем похожие случаи в базе данных
            List<EquipmentMaintenanceRecord> similarCases = equipmentMaintenanceRepository
                .findByCommentsContaining(problem, PageRequest.of(0, 5));
            
            if (similarCases.isEmpty()) {
                return "К сожалению, в базе данных не найдено информации по данной проблеме. " +
                       "Рекомендуется обратиться к техническому специалисту или руководству по эксплуатации оборудования.";
            }
            
            StringBuilder context = new StringBuilder();
            context.append("Найденные случаи решения похожих проблем:\n\n");
            
            for (EquipmentMaintenanceRecord record : similarCases) {
                context.append(String.format("Машина: %s\n", record.getMachineName()));
                context.append(String.format("Проблема: %s\n", record.getDescription()));
                context.append(String.format("Статус: %s\n", record.getStatus()));
                if (record.getComments() != null) {
                    context.append(String.format("Действия: %s\n\n", record.getComments()));
                }
            }
            
            String prompt = "На основе предоставленных реальных случаев ремонта создай пошаговую инструкцию " +
                           "по устранению проблемы: " + problem + "\n\n" +
                           "Реальные случаи из базы данных:\n" + context.toString() + "\n\n" +
                           "Создай структурированную инструкцию с разделами: Диагностика, Устранение, Проверка. " +
                           "Используй только информацию из предоставленных случаев.";
            
            return ollamaService.generateResponse(prompt);
            
        } catch (Exception e) {
            return "Ошибка генерации инструкции: " + e.getMessage();
        }
    }
}