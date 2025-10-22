package ru.georgdeveloper.assistantbaseupdate.entity.sqlserver;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "REP_BreakdownReport")
@Data
public class WorkOrder {
    
    @Id
    @Column(name = "IDCode")
    private String idCode;
    
    @Column(name = "IsDeleted")
    private Integer isDeleted;
    
    @Column(name = "DateInsert")
    private LocalDateTime dateInsert;
    
    @Column(name = "DateLastUpdate")
    private LocalDateTime dateLastUpdate;
    
    @Column(name = "PlantDepartmentGeographicalCodeName")
    private String plantDepartmentGeographicalCodeName;
    
    @Column(name = "WOCodeName")
    private String woCodeName;
    
    @Column(name = "MachineName")
    private String machineName;
    
    @Column(name = "Assembly")
    private String assembly;
    
    @Column(name = "SubAssembly")
    private String subAssembly;
    
    @Column(name = "User_CODE_T1")
    private String userCodeT1;
    
    @Column(name = "Date_T1")
    private LocalDateTime dateT1;
    
    @Column(name = "SDate_T1")
    private String sDateT1;
    
    @Column(name = "IsHP")
    private Integer isHp;
    
    @Column(name = "Maintainer")
    private String maintainer;
    
    @Column(name = "WOStatusLocalDescr")
    private String woStatusLocalDescr;
    
    @Column(name = "TYPEWO")
    private String typeWo;
    
    @Column(name = "Date_T3")
    private LocalDateTime dateT3;
    
    @Column(name = "SDate_T3")
    private String sDateT3;
    
    @Column(name = "User_CODE_T4")
    private String userCodeT4;
    
    @Column(name = "Date_T4")
    private LocalDateTime dateT4;
    
    @Column(name = "SDate_T4")
    private String sDateT4;
    
    @Column(name = "Duration")
    private Integer duration;
    
    @Column(name = "SDuration")
    private String sDuration;
    
    @Column(name = "WOBreakDownTime")
    private Double woBreakDownTime;
    
    @Column(name = "SWOBreakDownTime")
    private String sWoBreakDownTime;
    
    @Column(name = "LogisticTime")
    private Double logisticTime;
    
    @Column(name = "SLogisticTime")
    private String sLogisticTime;
    
    @Column(name = "comment", columnDefinition = "TEXT")
    private String comment;
    
    @Column(name = "iindex")
    private Integer iindex;
    
    @Column(name = "Date_T2")
    private LocalDateTime dateT2;
    
    @Column(name = "SDate_T2")
    private String sDateT2;
    
    @Column(name = "TTR")
    private Integer ttr;
    
    @Column(name = "STTR")
    private String sTtr;
    
    @Column(name = "Date_T1Bis")
    private LocalDateTime dateT1Bis;
    
    @Column(name = "SDate_T1Bis")
    private String sDateT1Bis;
    
    @Column(name = "LogisticTimeMin")
    private Integer logisticTimeMin;
    
    @Column(name = "SLogisticTimeMin")
    private String sLogisticTimeMin;
    
    @Column(name = "WorkCenter")
    private String workCenter;
    
    @Column(name = "InitialComment")
    private String initialComment;
    
    @Column(name = "Maintainers", columnDefinition = "TEXT")
    private String maintainers;
    
    @Column(name = "CustomField01")
    private String customField01;
    
    @Column(name = "CustomField02")
    private String customField02;
    
    @Column(name = "CustomField03")
    private String customField03;
    
    @Column(name = "CustomField04")
    private String customField04;
    
    @Column(name = "CustomField05")
    private String customField05;
    
}
