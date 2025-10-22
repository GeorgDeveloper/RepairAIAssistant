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
    
    @Column(name = "User_CODE_T5")
    private String userCodeT5;
    
    @Column(name = "Date_T5")
    private LocalDateTime dateT5;
    
    @Column(name = "SDate_T5")
    private String sDateT5;
    
    @Column(name = "User_CODE_T6")
    private String userCodeT6;
    
    @Column(name = "Date_T6")
    private LocalDateTime dateT6;
    
    @Column(name = "SDate_T6")
    private String sDateT6;
    
    @Column(name = "User_CODE_T7")
    private String userCodeT7;
    
    @Column(name = "Date_T7")
    private LocalDateTime dateT7;
    
    @Column(name = "SDate_T7")
    private String sDateT7;
    
    @Column(name = "User_CODE_T8")
    private String userCodeT8;
    
    @Column(name = "Date_T8")
    private LocalDateTime dateT8;
    
    @Column(name = "SDate_T8")
    private String sDateT8;
    
    @Column(name = "User_CODE_T9")
    private String userCodeT9;
    
    @Column(name = "Date_T9")
    private LocalDateTime dateT9;
    
    @Column(name = "SDate_T9")
    private String sDateT9;
    
    @Column(name = "User_CODE_T10")
    private String userCodeT10;
    
    @Column(name = "Date_T10")
    private LocalDateTime dateT10;
    
    @Column(name = "SDate_T10")
    private String sDateT10;
    
    @Column(name = "User_CODE_T11")
    private String userCodeT11;
    
    @Column(name = "Date_T11")
    private LocalDateTime dateT11;
    
    @Column(name = "SDate_T11")
    private String sDateT11;
    
    @Column(name = "User_CODE_T12")
    private String userCodeT12;
    
    @Column(name = "Date_T12")
    private LocalDateTime dateT12;
    
    @Column(name = "SDate_T12")
    private String sDateT12;
    
    @Column(name = "User_CODE_T13")
    private String userCodeT13;
    
    @Column(name = "Date_T13")
    private LocalDateTime dateT13;
    
    @Column(name = "SDate_T13")
    private String sDateT13;
    
    @Column(name = "User_CODE_T14")
    private String userCodeT14;
    
    @Column(name = "Date_T14")
    private LocalDateTime dateT14;
    
    @Column(name = "SDate_T14")
    private String sDateT14;
    
    @Column(name = "User_CODE_T15")
    private String userCodeT15;
    
    @Column(name = "Date_T15")
    private LocalDateTime dateT15;
    
    @Column(name = "SDate_T15")
    private String sDateT15;
    
    @Column(name = "User_CODE_T16")
    private String userCodeT16;
    
    @Column(name = "Date_T16")
    private LocalDateTime dateT16;
    
    @Column(name = "SDate_T16")
    private String sDateT16;
    
    @Column(name = "User_CODE_T17")
    private String userCodeT17;
    
    @Column(name = "Date_T17")
    private LocalDateTime dateT17;
    
    @Column(name = "SDate_T17")
    private String sDateT17;
    
    @Column(name = "User_CODE_T18")
    private String userCodeT18;
    
    @Column(name = "Date_T18")
    private LocalDateTime dateT18;
    
    @Column(name = "SDate_T18")
    private String sDateT18;
    
    @Column(name = "User_CODE_T19")
    private String userCodeT19;
    
    @Column(name = "Date_T19")
    private LocalDateTime dateT19;
    
    @Column(name = "SDate_T19")
    private String sDateT19;
    
    @Column(name = "User_CODE_T20")
    private String userCodeT20;
    
    @Column(name = "Date_T20")
    private LocalDateTime dateT20;
    
    @Column(name = "SDate_T20")
    private String sDateT20;
}
