package ru.georgdeveloper.assistantcore.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "diagnostics_schedules")
@Data
public class DiagnosticsSchedule {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "year", nullable = false, unique = true)
    private Integer year; // Год графика
    
    @Column(name = "shift_duration_hours", nullable = false)
    private Integer shiftDurationHours = 7; // Длительность смены в часах
    
    @Column(name = "workers_count", nullable = false)
    private Integer workersCount; // Количество человек для диагностики
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

