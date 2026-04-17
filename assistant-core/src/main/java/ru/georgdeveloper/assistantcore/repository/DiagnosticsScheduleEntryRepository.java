package ru.georgdeveloper.assistantcore.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.georgdeveloper.assistantcore.model.DiagnosticsScheduleEntry;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface DiagnosticsScheduleEntryRepository extends JpaRepository<DiagnosticsScheduleEntry, Long> {
    List<DiagnosticsScheduleEntry> findByScheduleId(Long scheduleId);
    
    List<DiagnosticsScheduleEntry> findByScheduleIdAndScheduledDateBetween(
            Long scheduleId, LocalDate startDate, LocalDate endDate);
    
    List<DiagnosticsScheduleEntry> findByScheduleIdAndScheduledDate(
            Long scheduleId, LocalDate scheduledDate);
    
    @Query("SELECT e FROM DiagnosticsScheduleEntry e WHERE e.schedule.id = :scheduleId " +
           "AND e.scheduledDate >= :startDate AND e.scheduledDate <= :endDate " +
           "ORDER BY e.scheduledDate, e.equipment")
    List<DiagnosticsScheduleEntry> findByScheduleAndDateRange(
            @Param("scheduleId") Long scheduleId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
    
    @Query("SELECT e FROM DiagnosticsScheduleEntry e WHERE e.schedule.id = :scheduleId " +
           "AND e.scheduledDate = :date")
    List<DiagnosticsScheduleEntry> findByScheduleAndDate(
            @Param("scheduleId") Long scheduleId,
            @Param("date") LocalDate date);
}

