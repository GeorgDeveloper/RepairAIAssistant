package ru.georgdeveloper.assistantbaseupdate.repository.sqlserver;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.georgdeveloper.assistantbaseupdate.entity.sqlserver.WOM_WorkOrder;

import java.util.List;

@Repository
public interface WOM_WorkOrderRepository extends JpaRepository<WOM_WorkOrder, String> {
    
    /**
     * Поиск причины простоя по коду наряда
     */
    @Query("SELECT w.pcsDftDesc FROM WOM_WorkOrder w WHERE w.woCodeName = :woCodeName")
    String findPcsDftDescByWoCodeName(@Param("woCodeName") String woCodeName);
    
    /**
     * Поиск всех записей по коду наряда
     */
    @Query("SELECT w FROM WOM_WorkOrder w WHERE w.woCodeName = :woCodeName")
    List<WOM_WorkOrder> findByWoCodeName(@Param("woCodeName") String woCodeName);
}
