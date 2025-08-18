package ru.georgdeveloper.assistantcore.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.PageRequest;
import ru.georgdeveloper.assistantcore.model.EquipmentMaintenanceRecord;
import ru.georgdeveloper.assistantcore.repository.EquipmentMaintenanceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Оптимизированный сервис обучения модели
 */
@Service
public class OptimizedModelTrainingService {
    
    private static final Logger logger = LoggerFactory.getLogger(OptimizedModelTrainingService.class);
    
    @Autowired
    private EquipmentMaintenanceRepository equipmentRepository;
    
    @Autowired
    private OllamaService ollamaService;
    
    @Value("${ai.training.batch-size:200}")
    private int batchSize;
    
    @PostConstruct
    public void init() {
        logger.info("Инициализация оптимизированного сервиса обучения");
        // Отключено для быстрого запуска
        // trainOnHistoricalData();
    }
    
    public void trainOnHistoricalData() {
        try {
            List<EquipmentMaintenanceRecord> records = equipmentRepository
                .findRecentRecords(PageRequest.of(0, batchSize));
            
            logger.info("Обучение на {} записях из БД", records.size());
            
            // Создание обучающих примеров
            List<String> examples = records.stream()
                .map(this::createTrainingPrompt)
                .collect(Collectors.toList());
            
            // Обучение модели (упрощенная версия)
            String trainingPrompt = String.join("\n", examples);
            ollamaService.generateResponse("Изучи эти примеры ремонтов:\n" + trainingPrompt);
            
        } catch (Exception e) {
            logger.error("Ошибка обучения: {}", e.getMessage());
        }
    }
    
    private String createTrainingPrompt(EquipmentMaintenanceRecord record) {
        return String.format("Оборудование: %s, Проблема: %s, Решение: %s",
            record.getMachineName(),
            record.getDescription(),
            record.getComments());
    }
}