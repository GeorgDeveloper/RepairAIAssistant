package ru.georgdeveloper.assistantcore.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.time.LocalDate;

@Entity
@Table(name = "diagnostics_reports")
@Data
public class DiagnosticsReport {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "detection_date")
    private LocalDate detectionDate;
    
    @Column(name = "diagnostics_type")
    private String diagnosticsType;
    
    @Column(name = "equipment")
    private String equipment;
    
    @Column(name = "node")
    private String node;
    
    @Column(name = "area")
    private String area;
    
    @Column(name = "malfunction", columnDefinition = "TEXT")
    private String malfunction;
    
    @Column(name = "additional_kit")
    private String additionalKit;
    
    @Column(name = "causes", columnDefinition = "TEXT")
    private String causes;
    
    @Column(name = "report", columnDefinition = "TEXT")
    private String report;
    
    @Column(name = "elimination_date")
    private LocalDate eliminationDate;
    
    @Column(name = "condition_after_elimination", columnDefinition = "TEXT")
    private String conditionAfterElimination;
    
    @Column(name = "responsible")
    private String responsible;
    
    @Column(name = "status")
    private String status;
    
    @Column(name = "non_elimination_reason", columnDefinition = "TEXT")
    private String nonEliminationReason;
    
    @Column(name = "measures", columnDefinition = "TEXT")
    private String measures;
    
    @Column(name = "comments", columnDefinition = "TEXT")
    private String comments;
    
    @Column(name = "photo_path", columnDefinition = "TEXT")
    private String photoPath;
    
    @Column(name = "document_path", columnDefinition = "TEXT")
    private String documentPath;
    
    @Column(name = "photo_result_path", columnDefinition = "TEXT")
    private String photoResultPath;
    
    @Column(name = "document_result_path", columnDefinition = "TEXT")
    private String documentResultPath;
    
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

