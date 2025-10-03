package ru.georgdeveloper.assistantcore.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import ru.georgdeveloper.assistantcore.model.BreakdownReport;
import ru.georgdeveloper.assistantcore.model.EquipmentMaintenanceRecord;
import ru.georgdeveloper.assistantcore.model.MigrationTracking;
import ru.georgdeveloper.assistantcore.model.SummaryOfSolutions;
import ru.georgdeveloper.assistantcore.repository.BreakdownReportRepository;
import ru.georgdeveloper.assistantcore.repository.EquipmentMaintenanceRepository;
import ru.georgdeveloper.assistantcore.repository.MigrationTrackingRepository;
import ru.georgdeveloper.assistantcore.repository.SummaryOfSolutionsRepository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Сервис для быстрой миграции больших объемов данных
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FastMigrationService {

    private final VectorStoreService vectorStoreService;
    private final EquipmentMaintenanceRepository equipmentMaintenanceRepository;
    private final SummaryOfSolutionsRepository summaryOfSolutionsRepository;
    private final BreakdownReportRepository breakdownReportRepository;
    private final MigrationTrackingRepository migrationTrackingRepository;
    
    private final Executor migrationExecutor = Executors.newFixedThreadPool(8);

    /**
     * Быстрая миграция всех данных с использованием параллельной обработки
     */
    public Map<String, Object> fastMigrateAllData() {
        log.info("Запуск быстрой миграции всех данных");
        
        Map<String, Object> result = new HashMap<>();
        long startTime = System.currentTimeMillis();
        
        try {
            // Параллельная миграция всех таблиц
            CompletableFuture<MigrationResult> maintenanceFuture = CompletableFuture.supplyAsync(() -> 
                migrateMaintenanceRecordsFast(), migrationExecutor);
            CompletableFuture<MigrationResult> solutionsFuture = CompletableFuture.supplyAsync(() -> 
                migrateSolutionRecordsFast(), migrationExecutor);
            CompletableFuture<MigrationResult> breakdownFuture = CompletableFuture.supplyAsync(() -> 
                migrateBreakdownRecordsFast(), migrationExecutor);
            
            // Ждем завершения всех миграций
            MigrationResult maintenanceResult = maintenanceFuture.get();
            MigrationResult solutionsResult = solutionsFuture.get();
            MigrationResult breakdownResult = breakdownFuture.get();
            
            long endTime = System.currentTimeMillis();
            long totalTime = endTime - startTime;
            
            result.put("maintenanceRecords", maintenanceResult);
            result.put("solutionsRecords", solutionsResult);
            result.put("breakdownRecords", breakdownResult);
            result.put("totalTimeMs", totalTime);
            result.put("totalTimeMinutes", totalTime / 60000.0);
            result.put("status", "success");
            
            log.info("Быстрая миграция завершена за {} минут", totalTime / 60000.0);
            
        } catch (Exception e) {
            log.error("Ошибка быстрой миграции", e);
            result.put("status", "error");
            result.put("error", e.getMessage());
        }
        
        return result;
    }

    /**
     * Быстрая миграция записей обслуживания
     */
    private MigrationResult migrateMaintenanceRecordsFast() {
        try {
            log.info("Быстрая миграция записей обслуживания...");
            
            String tableName = "equipment_maintenance_record";
            AtomicLong totalMigrated = new AtomicLong(0);
            AtomicLong maxId = new AtomicLong(0);
            long totalCount = equipmentMaintenanceRepository.count();
            
            int pageSize = 2000; // Большой размер страницы
            int page = 0;
            
            while (true) {
                List<EquipmentMaintenanceRecord> records = equipmentMaintenanceRepository
                        .findAll(PageRequest.of(page, pageSize))
                        .getContent();
                
                if (records.isEmpty()) break;
                
                // Фильтрация и обработка
                List<EquipmentMaintenanceRecord> validRecords = records.stream()
                        .filter(this::isValidRecord)
                        .toList();
                
                // Массовая обработка
                for (EquipmentMaintenanceRecord record : validRecords) {
                    vectorStoreService.addMaintenanceRecord(record);
                    totalMigrated.incrementAndGet();
                    maxId.updateAndGet(current -> Math.max(current, record.getId()));
                }
                
                page++;
                
                if (page % 5 == 0) {
                    log.info("Обработано {} страниц записей обслуживания, мигрировано: {}", page, totalMigrated.get());
                }
            }
            
            // Обновляем отслеживание миграции
            updateMigrationTracking(tableName, maxId.get(), totalCount);
            
            log.info("Быстрая миграция записей обслуживания завершена: {} записей", totalMigrated.get());
            
            return new MigrationResult(tableName, totalMigrated.get(), totalCount, null);
            
        } catch (Exception e) {
            log.error("Ошибка быстрой миграции записей обслуживания", e);
            return new MigrationResult("equipment_maintenance_record", 0L, 0L, e.getMessage());
        }
    }

    /**
     * Быстрая миграция решений
     */
    private MigrationResult migrateSolutionRecordsFast() {
        try {
            log.info("Быстрая миграция решений...");
            
            String tableName = "summary_of_solutions";
            AtomicLong totalMigrated = new AtomicLong(0);
            AtomicLong maxId = new AtomicLong(0);
            long totalCount = summaryOfSolutionsRepository.count();
            
            int pageSize = 1000;
            int page = 0;
            
            while (true) {
                List<SummaryOfSolutions> records = summaryOfSolutionsRepository
                        .findAll(PageRequest.of(page, pageSize))
                        .getContent();
                
                if (records.isEmpty()) break;
                
                // Фильтрация и обработка
                List<SummaryOfSolutions> validRecords = records.stream()
                        .filter(this::isValidSolution)
                        .toList();
                
                // Массовая обработка
                for (SummaryOfSolutions record : validRecords) {
                    vectorStoreService.addSolutionRecord(record);
                    totalMigrated.incrementAndGet();
                    maxId.updateAndGet(current -> Math.max(current, record.getId()));
                }
                
                page++;
                
                if (page % 10 == 0) {
                    log.info("Обработано {} страниц решений, мигрировано: {}", page, totalMigrated.get());
                }
            }
            
            // Обновляем отслеживание миграции
            updateMigrationTracking(tableName, maxId.get(), totalCount);
            
            log.info("Быстрая миграция решений завершена: {} записей", totalMigrated.get());
            
            return new MigrationResult(tableName, totalMigrated.get(), totalCount, null);
            
        } catch (Exception e) {
            log.error("Ошибка быстрой миграции решений", e);
            return new MigrationResult("summary_of_solutions", 0L, 0L, e.getMessage());
        }
    }

    /**
     * Быстрая миграция отчетов о поломках
     */
    private MigrationResult migrateBreakdownRecordsFast() {
        try {
            log.info("Быстрая миграция отчетов о поломках...");
            
            String tableName = "breakdown_report";
            AtomicLong totalMigrated = new AtomicLong(0);
            AtomicLong maxId = new AtomicLong(0);
            long totalCount = breakdownReportRepository.count();
            
            int pageSize = 1000;
            int page = 0;
            
            while (true) {
                List<BreakdownReport> records = breakdownReportRepository
                        .findAll(PageRequest.of(page, pageSize))
                        .getContent();
                
                if (records.isEmpty()) break;
                
                // Фильтрация и обработка
                List<BreakdownReport> validRecords = records.stream()
                        .filter(this::isValidBreakdown)
                        .toList();
                
                // Массовая обработка
                for (BreakdownReport record : validRecords) {
                    vectorStoreService.addBreakdownRecord(record);
                    totalMigrated.incrementAndGet();
                    if (record.getIdCode() != null) {
                        try {
                            long recordId = Long.parseLong(record.getIdCode());
                            maxId.updateAndGet(current -> Math.max(current, recordId));
                        } catch (NumberFormatException ignored) {
                            // Игнорируем неверные ID
                        }
                    }
                }
                
                page++;
                
                if (page % 10 == 0) {
                    log.info("Обработано {} страниц отчетов о поломках, мигрировано: {}", page, totalMigrated.get());
                }
            }
            
            // Обновляем отслеживание миграции
            updateMigrationTracking(tableName, maxId.get(), totalCount);
            
            log.info("Быстрая миграция отчетов о поломках завершена: {} записей", totalMigrated.get());
            
            return new MigrationResult(tableName, totalMigrated.get(), totalCount, null);
            
        } catch (Exception e) {
            log.error("Ошибка быстрой миграции отчетов о поломках", e);
            return new MigrationResult("breakdown_report", 0L, 0L, e.getMessage());
        }
    }

    /**
     * Обновление отслеживания миграции
     */
    private void updateMigrationTracking(String tableName, Long maxId, Long totalCount) {
        try {
            MigrationTracking tracking = migrationTrackingRepository.findByTableName(tableName).orElse(null);
            if (tracking == null) {
                tracking = new MigrationTracking(tableName);
                tracking.setLastMigratedId(maxId);
                tracking.setLastMigrationTime(LocalDateTime.now());
                tracking.setRecordsCount(totalCount);
                migrationTrackingRepository.save(tracking);
                log.info("Создана новая запись отслеживания миграции: {}", tableName);
            } else {
                migrationTrackingRepository.updateMigrationInfo(tableName, maxId, LocalDateTime.now(), totalCount);
                log.info("Обновлена запись отслеживания миграции: {}", tableName);
            }
        } catch (Exception e) {
            log.error("Ошибка обновления отслеживания миграции: {}", tableName, e);
        }
    }

    /**
     * Проверка валидности записи обслуживания
     */
    private boolean isValidRecord(EquipmentMaintenanceRecord record) {
        return record != null &&
               record.getMachineName() != null && !record.getMachineName().trim().isEmpty() &&
               record.getDescription() != null && !record.getDescription().trim().isEmpty();
    }

    /**
     * Проверка валидности решения
     */
    private boolean isValidSolution(SummaryOfSolutions solution) {
        return solution != null &&
               solution.getEquipment() != null && !solution.getEquipment().trim().isEmpty() &&
               solution.getMeasures_taken() != null && !solution.getMeasures_taken().trim().isEmpty();
    }

    /**
     * Проверка валидности отчета о поломке
     */
    private boolean isValidBreakdown(BreakdownReport breakdown) {
        return breakdown != null &&
               breakdown.getMachineName() != null && !breakdown.getMachineName().trim().isEmpty() &&
               breakdown.getComment() != null && !breakdown.getComment().trim().isEmpty();
    }

    /**
     * Результат миграции
     */
    private static class MigrationResult {
        private final String tableName;
        private final Long migratedCount;
        private final Long totalCount;
        private final String error;

        public MigrationResult(String tableName, Long migratedCount, Long totalCount, String error) {
            this.tableName = tableName;
            this.migratedCount = migratedCount;
            this.totalCount = totalCount;
            this.error = error;
        }

        // Getters
        public String getTableName() { return tableName; }
        public Long getMigratedCount() { return migratedCount; }
        public Long getTotalCount() { return totalCount; }
        public String getError() { return error; }
    }
}
