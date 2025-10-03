package ru.georgdeveloper.assistantcore.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import ru.georgdeveloper.assistantcore.model.MigrationTracking;
import ru.georgdeveloper.assistantcore.repository.MigrationTrackingRepository;
import ru.georgdeveloper.assistantcore.service.DataMigrationService;

import java.util.*;

/**
 * Сервис для диагностики проблем с миграцией данных
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MigrationDiagnosticService {

    private final JdbcTemplate jdbcTemplate;
    private final MigrationTrackingRepository migrationTrackingRepository;
    private final DataMigrationService dataMigrationService;

    /**
     * Получить полную диагностику состояния миграции
     */
    public Map<String, Object> getFullDiagnostic() {
        Map<String, Object> diagnostic = new HashMap<>();
        
        try {
            // Проверяем существование таблицы migration_tracking
            boolean tableExists = checkMigrationTrackingTableExists();
            diagnostic.put("migrationTrackingTableExists", tableExists);
            
            if (tableExists) {
                // Получаем все записи из migration_tracking
                List<MigrationTracking> trackingRecords = migrationTrackingRepository.findAll();
                diagnostic.put("trackingRecords", trackingRecords);
                diagnostic.put("trackingRecordsCount", trackingRecords.size());
                
                // Проверяем каждую таблицу
                Map<String, Object> tableStatus = new HashMap<>();
                String[] tables = {"equipment_maintenance_record", "summary_of_solutions", "breakdown_report"};
                
                for (String tableName : tables) {
                    Map<String, Object> status = checkTableStatus(tableName);
                    tableStatus.put(tableName, status);
                }
                
                diagnostic.put("tableStatus", tableStatus);
            } else {
                diagnostic.put("error", "Таблица migration_tracking не существует");
            }
            
            diagnostic.put("timestamp", new Date());
            diagnostic.put("status", "success");
            
        } catch (Exception e) {
            log.error("Ошибка диагностики миграции", e);
            diagnostic.put("error", e.getMessage());
            diagnostic.put("status", "error");
            diagnostic.put("timestamp", new Date());
        }
        
        return diagnostic;
    }

    /**
     * Проверить существование таблицы migration_tracking
     */
    private boolean checkMigrationTrackingTableExists() {
        try {
            Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'migration_tracking'",
                Integer.class
            );
            return count != null && count > 0;
        } catch (Exception e) {
            log.error("Ошибка проверки существования таблицы migration_tracking", e);
            return false;
        }
    }

    /**
     * Проверить статус конкретной таблицы
     */
    private Map<String, Object> checkTableStatus(String tableName) {
        Map<String, Object> status = new HashMap<>();
        
        try {
            // Проверяем существование таблицы
            Integer tableExists = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = ?",
                Integer.class,
                tableName
            );
            
            status.put("tableExists", tableExists != null && tableExists > 0);
            
            if (tableExists != null && tableExists > 0) {
                // Получаем количество записей в таблице
                Long recordCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM " + tableName,
                    Long.class
                );
                status.put("recordCount", recordCount);
                
                // Получаем максимальный ID
                Long maxId = jdbcTemplate.queryForObject(
                    "SELECT COALESCE(MAX(id), 0) FROM " + tableName,
                    Long.class
                );
                status.put("maxId", maxId);
                
                // Получаем информацию о миграции
                Optional<MigrationTracking> tracking = migrationTrackingRepository.findByTableName(tableName);
                if (tracking.isPresent()) {
                    MigrationTracking t = tracking.get();
                    status.put("lastMigratedId", t.getLastMigratedId());
                    status.put("lastMigrationTime", t.getLastMigrationTime());
                    status.put("trackedRecordCount", t.getRecordsCount());
                    
                    // Проверяем, нужна ли миграция
                    Long trackedCount = t.getRecordsCount();
                    long trackedCountValue = trackedCount != null ? trackedCount : 0L;
                    boolean needsMigration = recordCount > trackedCountValue;
                    status.put("needsMigration", needsMigration);
                } else {
                    status.put("needsMigration", true);
                    status.put("trackingRecord", "not_found");
                }
            } else {
                status.put("error", "Таблица не существует");
            }
            
        } catch (Exception e) {
            log.error("Ошибка проверки статуса таблицы {}", tableName, e);
            status.put("error", e.getMessage());
        }
        
        return status;
    }

    /**
     * Принудительно инициализировать записи отслеживания миграции
     */
    public Map<String, Object> forceInitializeMigrationTracking() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // Создаем записи для всех таблиц
            String[] tables = {"equipment_maintenance_record", "summary_of_solutions", "breakdown_report"};
            int created = 0;
            
            for (String tableName : tables) {
                Optional<MigrationTracking> existing = migrationTrackingRepository.findByTableName(tableName);
                if (existing.isEmpty()) {
                    MigrationTracking tracking = new MigrationTracking(tableName);
                    migrationTrackingRepository.save(tracking);
                    created++;
                    log.info("Создана запись отслеживания для таблицы: {}", tableName);
                }
            }
            
            result.put("created", created);
            result.put("status", "success");
            result.put("message", "Инициализация завершена успешно");
            
        } catch (Exception e) {
            log.error("Ошибка принудительной инициализации отслеживания миграции", e);
            result.put("status", "error");
            result.put("message", "Ошибка: " + e.getMessage());
        }
        
        return result;
    }

    /**
     * Сбросить все записи отслеживания миграции
     */
    public Map<String, Object> resetMigrationTracking() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            migrationTrackingRepository.deleteAll();
            
            // Пересоздаем записи
            String[] tables = {"equipment_maintenance_record", "summary_of_solutions", "breakdown_report"};
            for (String tableName : tables) {
                MigrationTracking tracking = new MigrationTracking(tableName);
                migrationTrackingRepository.save(tracking);
            }
            
            result.put("status", "success");
            result.put("message", "Отслеживание миграции сброшено и пересоздано");
            
        } catch (Exception e) {
            log.error("Ошибка сброса отслеживания миграции", e);
            result.put("status", "error");
            result.put("message", "Ошибка: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * Получить расширенную статистику миграции
     */
    public Map<String, Object> getExtendedMigrationStats() {
        Map<String, Object> fullDiagnostic = getFullDiagnostic();
        Map<String, Object> migrationStats = dataMigrationService.getMigrationStats();
        
        Map<String, Object> extended = new HashMap<>();
        extended.putAll(fullDiagnostic);
        extended.put("migrationSettings", migrationStats);
        
        return extended;
    }
}
