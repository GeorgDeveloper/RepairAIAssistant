package ru.georgdeveloper.assistantcore.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "equipment_maintenance_records")
@Data
public class EquipmentMaintenanceRecord {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "machine_name")
    private String machineName;
    
    @Column(name = "mechanism_node")
    private String mechanismNode;
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "status")
    private String status;
    
    @Column(name = "start_bd_t1")
    private LocalDateTime startBdT1;
    
    @Column(name = "start_maint_t2")
    private LocalDateTime startMaintT2;
    
    @Column(name = "stop_maint_t3")
    private LocalDateTime stopMaintT3;
    
    @Column(name = "stop_bd_t4")
    private LocalDateTime stopBdT4;
    
    @Column(name = "machine_downtime")
    private LocalTime machineDowntime;
    
    @Column(name = "maintainers", columnDefinition = "TEXT")
    private String maintainers;
    
    @Column(name = "comments", columnDefinition = "TEXT")
    private String comments;
    
    @Column(name = "failure_type")
    private String failureType;
    
    @Column(name = "area")
    private String area;
}