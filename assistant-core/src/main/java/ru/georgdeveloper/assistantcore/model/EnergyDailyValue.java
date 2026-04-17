package ru.georgdeveloper.assistantcore.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(
        name = "energy_daily_value",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_energy_day_metric",
                columnNames = {"fact_date", "resource_code", "metric_id"}))
@Getter
@Setter
public class EnergyDailyValue {

    protected EnergyDailyValue() {}

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fact_date", nullable = false)
    private LocalDate factDate;

    @Column(name = "resource_code", nullable = false, length = 32)
    private String resourceCode;

    @Column(name = "metric_id", nullable = false, length = 128)
    private String metricId;

    @Column(name = "value_numeric", precision = 24, scale = 8)
    private BigDecimal valueNumeric;

    @Column(name = "source_file", length = 512)
    private String sourceFile;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public EnergyDailyValue(
            LocalDate factDate,
            String resourceCode,
            String metricId,
            BigDecimal valueNumeric,
            String sourceFile) {
        this.factDate = factDate;
        this.resourceCode = resourceCode;
        this.metricId = metricId;
        this.valueNumeric = valueNumeric;
        this.sourceFile = sourceFile;
        this.createdAt = Instant.now();
    }
}
