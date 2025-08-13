package ru.georgdeveloper.assistantcore.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "repair_records")
@Data
public class RepairRecord {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String deviceType;
    
    @Column(nullable = false)
    private String problemDescription;
    
    @Column
    private String solution;
    
    @Column(nullable = false)
    private LocalDateTime startDate;
    
    @Column
    private LocalDateTime endDate;
    
    @Column
    private Integer durationHours;
    
    @Column
    private String status;
}