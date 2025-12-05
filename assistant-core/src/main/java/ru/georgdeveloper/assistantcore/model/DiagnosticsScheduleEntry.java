package ru.georgdeveloper.assistantcore.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Entity
@Table(name = "diagnostics_schedule_entries", indexes = {
    @Index(name = "idx_schedule_date", columnList = "schedule_id, scheduled_date"),
    @Index(name = "idx_equipment", columnList = "equipment"),
    @Index(name = "idx_scheduled_date", columnList = "scheduled_date")
})
@Data
public class DiagnosticsScheduleEntry {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id", nullable = false)
    private DiagnosticsSchedule schedule;
    
    @Column(name = "equipment", nullable = false)
    private String equipment; // Название оборудования
    
    @Column(name = "area")
    private String area; // Участок
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "diagnostics_type_id", nullable = false)
    private DiagnosticsType diagnosticsType;
    
    @Column(name = "scheduled_date", nullable = false)
    private LocalDate scheduledDate; // Запланированная дата
    
    @Column(name = "is_completed")
    private Boolean isCompleted = false; // Выполнена ли диагностика
    
    @Column(name = "completed_date")
    private LocalDate completedDate; // Дата выполнения
    
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes; // Заметки
}

