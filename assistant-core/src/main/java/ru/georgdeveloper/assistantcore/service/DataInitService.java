package ru.georgdeveloper.assistantcore.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.georgdeveloper.assistantcore.repository.EquipmentMaintenanceRepository;
import ru.georgdeveloper.assistantcore.repository.BreakdownReportRepository;
import ru.georgdeveloper.assistantcore.repository.SummaryOfSolutionsRepository;

@Service
public class DataInitService implements CommandLineRunner {
    
    private static final Logger logger = LoggerFactory.getLogger(DataInitService.class);
    
    @Autowired
    private EquipmentMaintenanceRepository equipmentMaintenanceRepository;
    
    @Autowired
    private BreakdownReportRepository breakdownReportRepository;

    @Autowired
    private SummaryOfSolutionsRepository summaryOfSolutionsRepository;
    
    @Override
    public void run(String... args) {
        try {
            long maintenanceCount = equipmentMaintenanceRepository.count();
            long breakdownCount = breakdownReportRepository.count();
            long summaryOfSolutionsCount = summaryOfSolutionsRepository.count();

            logger.info("✅ Подключение к базе данных успешно!");
            logger.info("🔧 Записей обслуживания: {}", maintenanceCount);
            logger.info("⚠️ Отчетов о поломках: {}", breakdownCount);
            logger.info("⚠️ Отчетов о сложных ремонтах: {}", summaryOfSolutionsCount);

        } catch (Exception e) {
            logger.error("❌ Ошибка подключения к базе данных: {}", e.getMessage(), e);
        }
    }
}