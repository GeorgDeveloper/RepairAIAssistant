package ru.georgdeveloper.assistantcore.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import ru.georgdeveloper.assistantcore.model.BreakdownReport;
import java.util.List;

@Repository
public interface BreakdownReportRepository extends JpaRepository<BreakdownReport, String> {
    
    List<BreakdownReport> findByWoStatusLocalDescr(String status);
    
    @Query("SELECT b FROM BreakdownReport b ORDER BY b.duration DESC")
    List<BreakdownReport> findLongestDowntimes();
    
    @Query("SELECT b FROM BreakdownReport b WHERE b.machineName LIKE %?1%")
    List<BreakdownReport> findByMachineNameContaining(String machineName);
}