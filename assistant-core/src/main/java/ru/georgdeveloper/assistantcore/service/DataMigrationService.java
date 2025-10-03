package ru.georgdeveloper.assistantcore.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

/**
 * Сервис для миграции данных из реляционной БД в векторное хранилище
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DataMigrationService {

    private final VectorStoreService vectorStoreService;
    private final EquipmentMaintenanceRepository equipmentMaintenanceRepository;
    private final SummaryOfSolutionsRepository summaryOfSolutionsRepository;
    private final BreakdownReportRepository breakdownReportRepository;
    private final MigrationTrackingRepository migrationTrackingRepository;
    
    @Value("${ai.migration.auto-migrate:true}")
    private boolean autoMigrate;
    
    @Value("${ai.migration.batch-size:1000}")
    private int batchSize;
    
    @Value("${ai.migration.parallel-threads:4}")
    private int parallelThreads;
    
    @Value("${ai.migration.fetch-size:5000}")
    private int fetchSize;

    /**
     * Умная миграция при запуске приложения
     */
    @EventListener(ApplicationReadyEvent.class)
    @Order(2) // Выполняется после DatabaseInitializationService
    public void smartMigrateOnStartup() {
        if (!autoMigrate) {
            log.info("Автоматическая миграция отключена (ai.migration.auto-migrate=false)");
            return;
        }
        
        log.info("Проверяем необходимость миграции данных...");
        
        CompletableFuture.runAsync(() -> {
            try {
                // Инициализируем отслеживание миграций если нужно
                initializeMigrationTracking();
                
                // Проверяем, нужна ли миграция
                boolean needsMigration = checkIfMigrationNeeded();
                
                if (needsMigration) {
                    log.info("Обнаружены новые данные, запускаем инкрементальную миграцию...");
                    migrateIncrementalData();
                    log.info("Инкрементальная миграция завершена успешно");
                } else {
                    log.info("Новых данных не обнаружено, миграция не требуется");
                }
            } catch (Exception e) {
                log.error("Ошибка при проверке/миграции данных", e);
            }
        });
    }

    /**
     * Полная миграция всех данных (только по явной команде)
     */
    @Transactional
    public void migrateAllData() {
        log.info("Начинаем полную миграцию данных");

        try {
            // Сбрасываем отслеживание миграций
            resetMigrationTracking();

            // Выполняем миграцию последовательно в одной транзакции
            migrateMaintenanceRecords(true);
            migrateSolutionRecords(true);
            migrateBreakdownRecords(true);

            log.info("Полная миграция данных завершена");
        } catch (Exception e) {
            log.error("Ошибка полной миграции данных", e);
            throw e;
        }
    }

    /**
     * Полная миграция с очисткой (только по явной команде администратора)
     */
    public void migrateAllDataWithClear() {
        log.info("Начинаем полную миграцию данных с очисткой векторного хранилища");

        // Очищаем векторное хранилище
        vectorStoreService.clearStore();
        
        // Выполняем полную миграцию
        migrateAllData();

        log.info("Полная миграция данных с очисткой завершена");
    }

    /**
     * Инициализация отслеживания миграций
     */
    @Transactional
    private void initializeMigrationTracking() {
        try {
            String[] tableNames = {"equipment_maintenance_record", "summary_of_solutions", "breakdown_report"};
            
            for (String tableName : tableNames) {
                Optional<MigrationTracking> existing = migrationTrackingRepository.findByTableName(tableName);
                if (existing.isEmpty()) {
                    MigrationTracking tracking = new MigrationTracking(tableName);
                    migrationTrackingRepository.save(tracking);
                    log.info("Инициализировано отслеживание для таблицы: {}", tableName);
                }
            }
        } catch (InvalidDataAccessResourceUsageException e) {
            log.error("Таблица migration_tracking не существует, но должна была быть создана при инициализации БД");
            log.error("Проверьте работу DatabaseInitializationService или создайте таблицу вручную");
            throw new RuntimeException("Таблица migration_tracking не найдена после инициализации БД", e);
        } catch (Exception e) {
            log.error("Ошибка инициализации отслеживания миграций", e);
            throw new RuntimeException("Ошибка инициализации отслеживания миграций", e);
        }
    }

    /**
     * Проверка необходимости миграции
     */
    private boolean checkIfMigrationNeeded() {
        boolean needsMigration = false;

        // Проверяем каждую таблицу
        needsMigration |= checkTableForNewData("equipment_maintenance_record", 
                                              equipmentMaintenanceRepository.count());
        needsMigration |= checkTableForNewData("summary_of_solutions", 
                                              summaryOfSolutionsRepository.count());
        needsMigration |= checkTableForNewData("breakdown_report", 
                                              breakdownReportRepository.count());

        return needsMigration;
    }

    /**
     * Проверка конкретной таблицы на новые данные
     */
    private boolean checkTableForNewData(String tableName, long currentCount) {
        Optional<MigrationTracking> trackingOpt = migrationTrackingRepository.findByTableName(tableName);
        
        if (trackingOpt.isEmpty()) {
            log.info("Таблица {} не отслеживается, требуется полная миграция", tableName);
            return true;
        }

        MigrationTracking tracking = trackingOpt.get();
        long lastCount = tracking.getRecordsCount() != null ? tracking.getRecordsCount() : 0;

        if (currentCount > lastCount) {
            log.info("Таблица {}: было {} записей, стало {} - требуется миграция", 
                    tableName, lastCount, currentCount);
            return true;
        } else if (currentCount < lastCount) {
            log.warn("Таблица {}: количество записей уменьшилось с {} до {} - возможно удаление данных", 
                    tableName, lastCount, currentCount);
            return true;
        }

        log.debug("Таблица {}: новых данных не обнаружено ({} записей)", tableName, currentCount);
        return false;
    }

    /**
     * Инкрементальная миграция новых данных
     */
    @Transactional
    private void migrateIncrementalData() {
        log.info("Начинаем инкрементальную миграцию данных");

        try {
            // Выполняем миграцию последовательно в одной транзакции
            migrateMaintenanceRecords(false);
            migrateSolutionRecords(false);
            migrateBreakdownRecords(false);
            
            log.info("Инкрементальная миграция данных завершена");
        } catch (Exception e) {
            log.error("Ошибка инкрементальной миграции данных", e);
            throw e;
        }
    }

    /**
     * Сброс отслеживания миграций
     */
    private void resetMigrationTracking() {
        LocalDateTime resetTime = LocalDateTime.now();
        migrationTrackingRepository.resetMigrationInfo("equipment_maintenance_record", resetTime);
        migrationTrackingRepository.resetMigrationInfo("summary_of_solutions", resetTime);
        migrationTrackingRepository.resetMigrationInfo("breakdown_report", resetTime);
        log.info("Сброшено отслеживание миграций");
    }

    /**
     * Миграция записей обслуживания оборудования
     */
    public void migrateMaintenanceRecords() {
        migrateMaintenanceRecords(false);
    }

    /**
     * Миграция записей обслуживания оборудования
     */
    public void migrateMaintenanceRecords(boolean fullMigration) {
        try {
            String tableName = "equipment_maintenance_record";
            log.info("Миграция записей обслуживания оборудования (полная: {})...", fullMigration);
            
            // Получаем информацию о последней миграции
            Optional<MigrationTracking> trackingOpt = migrationTrackingRepository.findByTableName(tableName);
            Long lastMigratedId = 0L;
            
            if (!fullMigration && trackingOpt.isPresent()) {
                lastMigratedId = trackingOpt.get().getLastMigratedId();
                log.info("Инкрементальная миграция с ID > {}", lastMigratedId);
            }

            int page = 0;
            int totalMigrated = 0;
            Long maxId = lastMigratedId;

            while (true) {
                List<EquipmentMaintenanceRecord> records;
                
                if (fullMigration) {
                    records = equipmentMaintenanceRepository
                            .findAll(PageRequest.of(page, batchSize))
                            .getContent();
                } else {
                    records = equipmentMaintenanceRepository
                            .findByIdGreaterThan(lastMigratedId, PageRequest.of(page, batchSize));
                }

                if (records.isEmpty()) {
                    break;
                }

                // Публичная обработка записей в батчах
                List<List<EquipmentMaintenanceRecord>> batches = partitionList(records, parallelThreads);
                
                for (List<EquipmentMaintenanceRecord> batch : batches) {
                    for (EquipmentMaintenanceRecord record : batch) {
                        if (isValidRecord(record)) {
                            vectorStoreService.addMaintenanceRecord(record);
                            totalMigrated++;
                            maxId = Math.max(maxId, record.getId());
                        }
                    }
                }

                page++;
                
                if (page % 10 == 0) {
                    log.info("Обработано {} страниц записей обслуживания", page);
                }
            }

            // Обновляем отслеживание миграции
            long totalCount = equipmentMaintenanceRepository.count();
            
            try {
                MigrationTracking tracking = migrationTrackingRepository.findByTableName(tableName).orElse(null);
                if (tracking == null) {
                    tracking = new MigrationTracking(tableName);
                    tracking.setLastMigratedId(maxId);
                    tracking.setLastMigrationTime(LocalDateTime.now());
                    tracking.setRecordsCount(totalCount);
                    migrationTrackingRepository.save(tracking);
                    log.info("Создана новая запись отслеживания для таблицы: {}", tableName);
                } else {
                    migrationTrackingRepository.updateMigrationInfo(tableName, maxId, LocalDateTime.now(), totalCount);
                    log.info("Обновлена запись отслеживания для таблицы: {}", tableName);
                }
            } catch (Exception e) {
                log.error("Ошибка обновления отслеживания миграции для таблицы: {}", tableName, e);
            }

            log.info("Мигрировано {} записей обслуживания (всего в БД: {})", totalMigrated, totalCount);

        } catch (Exception e) {
            log.error("Ошибка миграции записей обслуживания", e);
        }
    }

    /**
     * Миграция решений сложных ремонтов
     */
    public void migrateSolutionRecords() {
        migrateSolutionRecords(false);
    }

    /**
     * Миграция решений сложных ремонтов
     */
    public void migrateSolutionRecords(boolean fullMigration) {
        try {
            String tableName = "summary_of_solutions";
            log.info("Миграция решений сложных ремонтов (полная: {})...", fullMigration);
            
            // Получаем информацию о последней миграции
            Optional<MigrationTracking> trackingOpt = migrationTrackingRepository.findByTableName(tableName);
            Long lastMigratedId = 0L;
            
            if (!fullMigration && trackingOpt.isPresent()) {
                lastMigratedId = trackingOpt.get().getLastMigratedId();
                log.info("Инкрементальная миграция с ID > {}", lastMigratedId);
            }

            List<SummaryOfSolutions> solutions;
            if (fullMigration) {
                solutions = summaryOfSolutionsRepository.findAll();
            } else {
                solutions = summaryOfSolutionsRepository.findByIdGreaterThan(lastMigratedId);
            }
            
            int totalMigrated = 0;
            Long maxId = lastMigratedId;

            for (SummaryOfSolutions solution : solutions) {
                if (isValidSolution(solution)) {
                    vectorStoreService.addSolutionRecord(solution);
                    totalMigrated++;
                    maxId = Math.max(maxId, solution.getId());
                }
            }

            // Обновляем отслеживание миграции
            long totalCount = summaryOfSolutionsRepository.count();
            
            try {
                MigrationTracking tracking = migrationTrackingRepository.findByTableName(tableName).orElse(null);
                if (tracking == null) {
                    tracking = new MigrationTracking(tableName);
                    tracking.setLastMigratedId(maxId);
                    tracking.setLastMigrationTime(LocalDateTime.now());
                    tracking.setRecordsCount(totalCount);
                    migrationTrackingRepository.save(tracking);
                    log.info("Создана новая запись отслеживания для таблицы: {}", tableName);
                } else {
                    migrationTrackingRepository.updateMigrationInfo(tableName, maxId, LocalDateTime.now(), totalCount);
                    log.info("Обновлена запись отслеживания для таблицы: {}", tableName);
                }
            } catch (Exception e) {
                log.error("Ошибка обновления отслеживания миграции для таблицы: {}", tableName, e);
            }

            log.info("Мигрировано {} решений (всего в БД: {})", totalMigrated, totalCount);

        } catch (Exception e) {
            log.error("Ошибка миграции решений", e);
        }
    }

    /**
     * Миграция отчетов о поломках
     */
    public void migrateBreakdownRecords() {
        migrateBreakdownRecords(false);
    }

    /**
     * Миграция отчетов о поломках
     */
    public void migrateBreakdownRecords(boolean fullMigration) {
        try {
            String tableName = "breakdown_report";
            log.info("Миграция отчетов о поломках (полная: {})...", fullMigration);
            
            // Получаем информацию о последней миграции
            Optional<MigrationTracking> trackingOpt = migrationTrackingRepository.findByTableName(tableName);
            String lastMigratedId = "0";
            
            if (!fullMigration && trackingOpt.isPresent() && trackingOpt.get().getLastMigratedId() != null) {
                lastMigratedId = trackingOpt.get().getLastMigratedId().toString();
                log.info("Инкрементальная миграция с ID > {}", lastMigratedId);
            }

            int page = 0;
            int totalMigrated = 0;
            String maxId = lastMigratedId;

            while (true) {
                List<BreakdownReport> reports;
                
                if (fullMigration) {
                    reports = breakdownReportRepository
                            .findAll(PageRequest.of(page, batchSize))
                            .getContent();
                } else {
                    reports = breakdownReportRepository
                            .findByIdGreaterThan(lastMigratedId, PageRequest.of(page, batchSize));
                }

                if (reports.isEmpty()) {
                    break;
                }

                for (BreakdownReport report : reports) {
                    if (isValidBreakdown(report)) {
                        vectorStoreService.addBreakdownRecord(report);
                        totalMigrated++;
                        if (report.getIdCode() != null && report.getIdCode().compareTo(maxId) > 0) {
                            maxId = report.getIdCode();
                        }
                    }
                }

                page++;
                
                if (page % 10 == 0) {
                    log.info("Обработано {} страниц отчетов о поломках", page);
                }
            }

            // Обновляем отслеживание миграции
            long totalCount = breakdownReportRepository.count();
            Long maxIdLong = maxId.equals("0") ? 0L : Long.parseLong(maxId);
            
            try {
                MigrationTracking tracking = migrationTrackingRepository.findByTableName(tableName).orElse(null);
                if (tracking == null) {
                    tracking = new MigrationTracking(tableName);
                    tracking.setLastMigratedId(maxIdLong);
                    tracking.setLastMigrationTime(LocalDateTime.now());
                    tracking.setRecordsCount(totalCount);
                    migrationTrackingRepository.save(tracking);
                    log.info("Создана новая запись отслеживания для таблицы: {}", tableName);
                } else {
                    migrationTrackingRepository.updateMigrationInfo(tableName, maxIdLong, LocalDateTime.now(), totalCount);
                    log.info("Обновлена запись отслеживания для таблицы: {}", tableName);
                }
            } catch (Exception e) {
                log.error("Ошибка обновления отслеживания миграции для таблицы: {}", tableName, e);
            }

            log.info("Мигрировано {} отчетов о поломках (всего в БД: {})", totalMigrated, totalCount);

        } catch (Exception e) {
            log.error("Ошибка миграции отчетов о поломках", e);
        }
    }

    /**
     * Инкрементальная миграция новых записей
     */
    public void migrateNewRecords() {
        log.info("Инкрементальная миграция новых записей");
        
        // Получаем только недавние записи (например, за последние 24 часа)
        List<EquipmentMaintenanceRecord> recentRecords = equipmentMaintenanceRepository
                .findRecentRecords(PageRequest.of(0, 50));

        int migrated = 0;
        for (EquipmentMaintenanceRecord record : recentRecords) {
            if (isValidRecord(record)) {
                vectorStoreService.addMaintenanceRecord(record);
                migrated++;
            }
        }

        log.info("Инкрементально мигрировано {} новых записей", migrated);
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
     * Разделение списка на части для публичной обработки
     */
    private <T> List<List<T>> partitionList(List<T> list, int partitions) {
        List<List<T>> result = new ArrayList<>();
        if (list.isEmpty()) {
            return result;
        }
        
        int size = list.size();
        int partitionSize = (int) Math.ceil((double) size / partitions);
        
        for (int i = 0; i < size; i += partitionSize) {
            int end = Math.min(i + partitionSize, size);
            result.add(new ArrayList<>(list.subList(i, end)));
        }
        
        return result;
    }
    
    /**
     * Получение статистики миграции
     */
    public Map<String, Object> getMigrationStats() {
        Map<String, Object> stats = new HashMap<>();
        
        try {
            List<MigrationTracking> tracking = migrationTrackingRepository.findAll();
            stats.put("trackingRecords", tracking);
            stats.put("totalTables", tracking.size());
            
            // Статистика по таблицам
            Map<String, Object> tableStats = new HashMap<>();
            tableStats.put("equipmentMaintenanceRecord", equipmentMaintenanceRepository.count());
            tableStats.put("summaryOfSolutions", summaryOfSolutionsRepository.count());
            tableStats.put("breakdownReport", breakdownReportRepository.count());
            stats.put("tableStats", tableStats);
            
            stats.put("batchSize", batchSize);
            stats.put("parallelThreads", parallelThreads);
            stats.put("autoMigrate", autoMigrate);
            
        } catch (Exception e) {
            log.error("Ошибка получения статистики миграции", e);
            stats.put("error", e.getMessage());
        }
        
        return stats;
    }
}
