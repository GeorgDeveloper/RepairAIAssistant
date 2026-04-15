package ru.georgdeveloper.assistantcore.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.georgdeveloper.assistantcore.model.EnergyDailyValue;

import java.time.LocalDate;
import java.util.List;

public interface EnergyDailyValueRepository extends JpaRepository<EnergyDailyValue, Long> {

    void deleteByFactDateBetweenAndResourceCode(LocalDate startInclusive, LocalDate endInclusive, String resourceCode);

    List<EnergyDailyValue> findByResourceCodeAndFactDateBetweenOrderByFactDateAscMetricIdAsc(
            String resourceCode, LocalDate fromInclusive, LocalDate toInclusive);
}
