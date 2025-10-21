package ru.georgdeveloper.assistantcore.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "REP_BreakdownReport")
@Data
public class BreakdownReport {
    
    @Id
    @Column(name = "IDCode")
    private String idCode;
    
    @Column(name = "MachineName")
    private String machineName;
    
    @Column(name = "Assembly")
    private String assembly;
    
    @Column(name = "SubAssembly")
    private String subAssembly;
    
    @Column(name = "Date_T1")
    private String dateT1;
    
    @Column(name = "Date_T4")
    private String dateT4;
    
    @Column(name = "Duration")
    private Integer duration;
    
    @Column(name = "WOBreakDownTime")
    private Double woBreakDownTime;
    
    @Column(name = "LogisticTime")
    private Double logisticTime;
    
    @Column(name = "comment", columnDefinition = "TEXT")
    private String comment;
    
    @Column(name = "InitialComment")
    private String initialComment;
    
    @Column(name = "Maintainers", columnDefinition = "TEXT")
    private String maintainers;
    
    @Column(name = "WOStatusLocalDescr")
    private String woStatusLocalDescr;
    
    @Column(name = "TYPEWO")
    private String typeWo;
}