package ru.georgdeveloper.assistantcore.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.georgdeveloper.assistantcore.model.EnergySteamHourlyValue;

import java.time.LocalDateTime;
import java.util.List;

public interface EnergySteamHourlyValueRepository extends JpaRepository<EnergySteamHourlyValue, Long> {

    List<EnergySteamHourlyValue> findByFactTimeBetweenOrderByFactTimeAscConsumerCodeAsc(
            LocalDateTime fromInclusive, LocalDateTime toInclusive);

    void deleteByFactTimeBetween(LocalDateTime fromInclusive, LocalDateTime toInclusive);

    @Query("select min(e.factTime), max(e.factTime) from EnergySteamHourlyValue e")
    Object[] findMinMaxFactTime();
}
