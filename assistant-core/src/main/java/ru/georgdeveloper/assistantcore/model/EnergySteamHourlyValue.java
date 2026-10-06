package ru.georgdeveloper.assistantcore.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "energy_steam_hourly_value",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_energy_steam_time_consumer",
                columnNames = {"fact_time", "consumer_code"}))
@Getter
@Setter
public class EnergySteamHourlyValue {

    protected EnergySteamHourlyValue() {}

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fact_time", nullable = false)
    private LocalDateTime factTime;

    @Column(name = "consumer_code", nullable = false, length = 64)
    private String consumerCode;

    @Column(name = "consumer_label", nullable = false, length = 128)
    private String consumerLabel;

    /** Расход: Qo / Gв / Gм */
    @Column(name = "flow_value", precision = 24, scale = 8)
    private BigDecimal flowValue;

    @Column(name = "temp_c", precision = 24, scale = 8)
    private BigDecimal tempC;

    @Column(name = "pressure_mpa", precision = 24, scale = 8)
    private BigDecimal pressureMpa;

    /** Масса: M / Mг / Mм, т */
    @Column(name = "mass_t", precision = 24, scale = 8)
    private BigDecimal massT;

    @Column(name = "source_file", length = 512)
    private String sourceFile;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public EnergySteamHourlyValue(
            LocalDateTime factTime,
            String consumerCode,
            String consumerLabel,
            BigDecimal flowValue,
            BigDecimal tempC,
            BigDecimal pressureMpa,
            BigDecimal massT,
            String sourceFile) {
        this.factTime = factTime;
        this.consumerCode = consumerCode;
        this.consumerLabel = consumerLabel;
        this.flowValue = flowValue;
        this.tempC = tempC;
        this.pressureMpa = pressureMpa;
        this.massT = massT;
        this.sourceFile = sourceFile;
        this.createdAt = Instant.now();
    }
}
