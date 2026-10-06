package ru.georgdeveloper.assistantcore.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.georgdeveloper.assistantcore.model.EnergyAirHourlyValue;

import java.time.LocalDateTime;
import java.util.List;

public interface EnergyAirHourlyValueRepository extends JpaRepository<EnergyAirHourlyValue, Long> {

    List<EnergyAirHourlyValue> findByFactTimeBetweenOrderByFactTimeAscBuildingCodeAsc(
            LocalDateTime fromInclusive, LocalDateTime toInclusive);

    void deleteByFactTimeBetweenAndBuildingCode(
            LocalDateTime fromInclusive, LocalDateTime toInclusive, String buildingCode);

    @org.springframework.data.jpa.repository.Query("select min(e.factTime), max(e.factTime) from EnergyAirHourlyValue e")
    Object[] findMinMaxFactTime();
}
