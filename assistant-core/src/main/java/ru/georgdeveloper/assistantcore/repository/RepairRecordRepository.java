package ru.georgdeveloper.assistantcore.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import ru.georgdeveloper.assistantcore.model.RepairRecord;
import java.util.List;

@Repository
public interface RepairRecordRepository extends JpaRepository<RepairRecord, Long> {
    
    @Query("SELECT r FROM RepairRecord r WHERE r.durationHours IS NOT NULL ORDER BY r.durationHours DESC")
    List<RepairRecord> findLongestRepairs();
    
    @Query("SELECT r FROM RepairRecord r WHERE r.status = 'COMPLETED' ORDER BY r.durationHours DESC")
    List<RepairRecord> findCompletedRepairsByDuration();
}