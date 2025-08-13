package ru.georgdeveloper.assistantcore.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.math.BigDecimal;

@Entity
@Table(name = "pm_monitor")
@Data
public class PmMonitor {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "Machine Name")
    private String machineName;
    
    @Column(name = "Work Order Name")
    private String workOrderName;
    
    @Column(name = "Work Order Frequency")
    private BigDecimal workOrderFrequency;
    
    @Column(name = "Typology")
    private String typology;
    
    @Column(name = "Scheduled Date")
    private LocalDateTime scheduledDate;
    
    @Column(name = "Date Start Work Order (T1mp)")
    private LocalDateTime dateStartWorkOrder;
    
    @Column(name = "Date Stop Work Order (T2mp)")
    private LocalDateTime dateStopWorkOrder;
    
    @Column(name = "Status")
    private String status;
    
    @Column(name = "Maintainers")
    private String maintainers;
    
    @Column(name = "Comment")
    private String comment;
}