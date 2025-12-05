package ru.georgdeveloper.assistantcore.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "diagnostics_types")
@Data
public class DiagnosticsType {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "code", unique = true, nullable = false)
    private String code; // 'B', 'K', 'y' и т.д.
    
    @Column(name = "name", nullable = false)
    private String name; // "Вибродиагностика", "Диагностика конденсатоотводчиков" и т.д.
    
    @Column(name = "duration_minutes", nullable = false)
    private Integer durationMinutes; // Длительность диагностики в минутах
    
    @Column(name = "color_code")
    private String colorCode; // Цвет для отображения (например, "#FFD700" для желтого, "#90EE90" для зеленого)
    
    @Column(name = "is_active")
    private Boolean isActive = true;
}

