package ru.georgdeveloper.assistantcore.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "manuals")
@Data
public class Manual {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "region")
    private String region;

    @Column(name = "equipment")
    private String equipment;

    @Column(name = "node")
    private String node;
    
    @Column(name = "deviceType")
    private String deviceType;
    
    @Column(columnDefinition = "TEXT")
    private String content;
    
    @Column
    private String fileName;

    @Lob
    @Column(name = "files")
    private byte[] files;
}