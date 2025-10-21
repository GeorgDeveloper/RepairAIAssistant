package ru.georgdeveloper.assistantcore.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.georgdeveloper.assistantcore.model.BreakdownReport;
import org.springframework.data.domain.Pageable;
import java.util.List;

@Repository
public interface BreakdownReportRepository extends JpaRepository<BreakdownReport, String> {
    /** Универсальный поиск по ключевому слову */
    @Query("SELECT b FROM BreakdownReport b WHERE " +
        "LOWER(b.machineName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
        "LOWER(b.assembly) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
        "LOWER(b.comment) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
        "LOWER(b.woStatusLocalDescr) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
        "ORDER BY b.duration DESC")
    List<BreakdownReport> findByKeyword(@Param("keyword") String keyword, Pageable pageable);
    
    List<BreakdownReport> findByWoStatusLocalDescr(String status);
    
    @Query("SELECT b FROM BreakdownReport b ORDER BY b.duration DESC")
    List<BreakdownReport> findLongestDowntimes();
    
    @Query("SELECT b FROM BreakdownReport b WHERE b.machineName LIKE %?1%")
    List<BreakdownReport> findByMachineNameContaining(String machineName);
    
    /** Умная выборка по статусу с лимитом */
    @Query("SELECT b FROM BreakdownReport b WHERE b.woStatusLocalDescr = :status ORDER BY b.duration DESC")
    List<BreakdownReport> findByStatusWithLimit(@Param("status") String status, Pageable pageable);
    
    /** Умная выборка по машине с лимитом */
    @Query("SELECT b FROM BreakdownReport b WHERE b.machineName LIKE %:machine% ORDER BY b.duration DESC")
    List<BreakdownReport> findByMachineWithLimit(@Param("machine") String machine, Pageable pageable);
    
    /** Последние отчеты с лимитом */
    @Query("SELECT b FROM BreakdownReport b ORDER BY b.duration DESC")
    List<BreakdownReport> findRecentReports(Pageable pageable);
    
}