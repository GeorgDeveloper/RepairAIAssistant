package ru.georgdeveloper.assistantcore.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import ru.georgdeveloper.assistantcore.model.MigrationTracking;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Repository для работы с отслеживанием миграций
 */
@Repository
public interface MigrationTrackingRepository extends JpaRepository<MigrationTracking, Long> {

    /**
     * Найти запись отслеживания по имени таблицы
     */
    Optional<MigrationTracking> findByTableName(String tableName);

    /**
     * Обновить информацию о последней миграции
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("UPDATE MigrationTracking m SET m.lastMigratedId = :lastId, " +
           "m.lastMigrationTime = :migrationTime, m.recordsCount = :recordsCount " +
           "WHERE m.tableName = :tableName")
    void updateMigrationInfo(@Param("tableName") String tableName,
                           @Param("lastId") Long lastId,
                           @Param("migrationTime") LocalDateTime migrationTime,
                           @Param("recordsCount") Long recordsCount);

    /**
     * Сбросить информацию о миграции (для полной очистки)
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("UPDATE MigrationTracking m SET m.lastMigratedId = 0, " +
           "m.recordsCount = 0, m.lastMigrationTime = :resetTime " +
           "WHERE m.tableName = :tableName")
    void resetMigrationInfo(@Param("tableName") String tableName,
                          @Param("resetTime") LocalDateTime resetTime);
}
