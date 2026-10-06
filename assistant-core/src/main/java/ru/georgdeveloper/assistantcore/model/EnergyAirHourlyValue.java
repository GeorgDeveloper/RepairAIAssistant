package ru.georgdeveloper.assistantcore.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "energy_air_hourly_value",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_energy_air_time_building",
                columnNames = {"fact_time", "building_code"}))
@Getter
@Setter
public class EnergyAirHourlyValue {

    protected EnergyAirHourlyValue() {}

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fact_time", nullable = false)
    private LocalDateTime factTime;

    @Column(name = "building_code", nullable = false, length = 32)
    private String buildingCode;

    @Column(name = "building_label", nullable = false, length = 64)
    private String buildingLabel;

    @Column(name = "pressure_mpa", precision = 24, scale = 8)
    private BigDecimal pressureMpa;

    @Column(name = "volume_m3", precision = 24, scale = 8)
    private BigDecimal volumeM3;

    @Column(name = "source_file", length = 512)
    private String sourceFile;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public EnergyAirHourlyValue(
            LocalDateTime factTime,
            String buildingCode,
            String buildingLabel,
            BigDecimal pressureMpa,
            BigDecimal volumeM3,
            String sourceFile) {
        this.factTime = factTime;
        this.buildingCode = buildingCode;
        this.buildingLabel = buildingLabel;
        this.pressureMpa = pressureMpa;
        this.volumeM3 = volumeM3;
        this.sourceFile = sourceFile;
        this.createdAt = Instant.now();
    }
}
