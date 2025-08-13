package ru.georgdeveloper.assistantcore.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import ru.georgdeveloper.assistantcore.model.PmMonitor;
import java.util.List;

@Repository
public interface PmMonitorRepository extends JpaRepository<PmMonitor, Long> {
    
    List<PmMonitor> findByStatus(String status);
    
    @Query("SELECT p FROM PmMonitor p ORDER BY p.scheduledDate DESC")
    List<PmMonitor> findAllOrderByScheduledDateDesc();
    
    @Query("SELECT p FROM PmMonitor p WHERE p.machineName LIKE %?1%")
    List<PmMonitor> findByMachineNameContaining(String machineName);
}