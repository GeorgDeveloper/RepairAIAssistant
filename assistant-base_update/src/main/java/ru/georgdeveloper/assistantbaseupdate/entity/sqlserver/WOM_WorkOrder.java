package ru.georgdeveloper.assistantbaseupdate.entity.sqlserver;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "WOM_WorkOrder")
@Data
public class WOM_WorkOrder {
    
    @Id
    @Column(name = "IDCode")
    private String idCode;
    
    @Column(name = "WOCodeName")
    private String woCodeName;
    
    @Column(name = "PCS_DFT_DESC")
    private String pcsDftDesc;
    
    // Другие поля таблицы можно добавить по необходимости
}
