package ru.georgdeveloper.assistantbaseupdate.entity.mysql;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Сущность для таблицы main_lines_online в MySQL.
 * 
 * Содержит онлайн метрики производства по ключевым линиям:
 * - Время простоя оборудования
 * - Процент простоя
 * - Доступность оборудования
 * - Время профилактического обслуживания
 */
@Entity
@Table(name = "main_lines_online")
public class MainLinesOnline {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "line_name", nullable = false)
    private String lineName;

    @Column(name = "area", nullable = false)
    private String area;

    @Column(name = "last_update", nullable = false)
    private LocalDateTime lastUpdate;

    @Column(name = "machine_downtime")
    private Double machineDowntime;

    @Column(name = "wt_min")
    private Double wtMin;

    @Column(name = "downtime_percentage")
    private Double downtimePercentage;

    @Column(name = "preventive_maintenance_duration_min")
    private Double preventiveMaintenanceDurationMin;

    @Column(name = "availability")
    private Double availability;

    // Конструкторы
    public MainLinesOnline() {}

    public MainLinesOnline(String lineName, String area, Double machineDowntime, Double wtMin, 
                          Double downtimePercentage, Double availability) {
        this.lineName = lineName;
        this.area = area;
        this.lastUpdate = LocalDateTime.now();
        this.machineDowntime = machineDowntime;
        this.wtMin = wtMin;
        this.downtimePercentage = downtimePercentage;
        this.preventiveMaintenanceDurationMin = 0.0;
        this.availability = availability;
    }

    // Геттеры и сеттеры
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getLineName() {
        return lineName;
    }

    public void setLineName(String lineName) {
        this.lineName = lineName;
    }

    public String getArea() {
        return area;
    }

    public void setArea(String area) {
        this.area = area;
    }

    public LocalDateTime getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(LocalDateTime lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    public Double getMachineDowntime() {
        return machineDowntime;
    }

    public void setMachineDowntime(Double machineDowntime) {
        this.machineDowntime = machineDowntime;
    }

    public Double getWtMin() {
        return wtMin;
    }

    public void setWtMin(Double wtMin) {
        this.wtMin = wtMin;
    }

    public Double getDowntimePercentage() {
        return downtimePercentage;
    }

    public void setDowntimePercentage(Double downtimePercentage) {
        this.downtimePercentage = downtimePercentage;
    }

    public Double getPreventiveMaintenanceDurationMin() {
        return preventiveMaintenanceDurationMin;
    }

    public void setPreventiveMaintenanceDurationMin(Double preventiveMaintenanceDurationMin) {
        this.preventiveMaintenanceDurationMin = preventiveMaintenanceDurationMin;
    }

    public Double getAvailability() {
        return availability;
    }

    public void setAvailability(Double availability) {
        this.availability = availability;
    }

    @Override
    public String toString() {
        return "MainLinesOnline{" +
                "id=" + id +
                ", lineName='" + lineName + '\'' +
                ", area='" + area + '\'' +
                ", lastUpdate=" + lastUpdate +
                ", machineDowntime=" + machineDowntime +
                ", wtMin=" + wtMin +
                ", downtimePercentage=" + downtimePercentage +
                ", preventiveMaintenanceDurationMin=" + preventiveMaintenanceDurationMin +
                ", availability=" + availability +
                '}';
    }
}
