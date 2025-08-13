package ru.georgdeveloper.assistantcore.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import ru.georgdeveloper.assistantcore.model.EquipmentMaintenanceRecord;
import java.util.List;

@Repository
public interface EquipmentMaintenanceRepository extends JpaRepository<EquipmentMaintenanceRecord, Long> {
    
    List<EquipmentMaintenanceRecord> findByStatus(String status);
    
    @Query("SELECT e FROM EquipmentMaintenanceRecord e ORDER BY e.startBdT1 DESC")
    List<EquipmentMaintenanceRecord> findAllOrderByStartDateDesc();
    
    @Query("SELECT e FROM EquipmentMaintenanceRecord e WHERE e.machineName LIKE %?1%")
    List<EquipmentMaintenanceRecord> findByMachineNameContaining(String machineName);
}