package ru.georgdeveloper.assistantcore.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Сущность для отслеживания состояния миграций в векторную БД
 */
@Entity
@Table(name = "migration_tracking")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MigrationTracking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "table_name", nullable = false, length = 100)
    private String tableName;

    @Column(name = "last_migrated_id")
    private Long lastMigratedId;

    @Column(name = "last_migration_time")
    private LocalDateTime lastMigrationTime;

    @Column(name = "records_count")
    private Long recordsCount;

    public MigrationTracking(String tableName) {
        this.tableName = tableName;
        this.lastMigratedId = 0L;
        this.recordsCount = 0L;
        this.lastMigrationTime = LocalDateTime.now();
    }
}
