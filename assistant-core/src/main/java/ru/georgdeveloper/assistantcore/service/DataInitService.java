package ru.georgdeveloper.assistantcore.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

import ru.georgdeveloper.assistantcore.repository.EquipmentMaintenanceRepository;
import ru.georgdeveloper.assistantcore.repository.BreakdownReportRepository;
import ru.georgdeveloper.assistantcore.repository.SummaryOfSolutionsRepository;

@Service
public class DataInitService implements CommandLineRunner {
    
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

            System.out.println("✅ Подключение к базе данных успешно!");
            System.out.println("🔧 Записей обслуживания: " + maintenanceCount);
            System.out.println("⚠️ Отчетов о поломках: " + breakdownCount);
            System.out.println("⚠️ Отчетов о сложных ремонтах: " + summaryOfSolutionsCount);

        } catch (Exception e) {
            System.err.println("❌ Ошибка подключения к базе данных: " + e.getMessage());
            e.printStackTrace();
        }
    }
}