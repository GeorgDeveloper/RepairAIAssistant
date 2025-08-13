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
    
    @Column(nullable = false)
    private String deviceType;
    
    @Column(columnDefinition = "TEXT")
    private String content;
    
    @Column
    private String fileName;
}