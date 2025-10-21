package ru.georgdeveloper.assistantbaseupdate.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.georgdeveloper.assistantbaseupdate.entity.WorkOrder;
import org.springframework.data.domain.Pageable;
import java.util.List;

@Repository
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name = "workorder.enabled", havingValue = "true", matchIfMissing = false)
public interface WorkOrderRepository extends JpaRepository<WorkOrder, String> {
    
    /** Универсальный поиск по ключевому слову */
    @Query("SELECT w FROM WorkOrder w WHERE " +
        "LOWER(w.machineName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
        "LOWER(w.assembly) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
        "LOWER(w.comment) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
        "LOWER(w.woStatusLocalDescr) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
        "ORDER BY w.duration DESC")
    List<WorkOrder> findByKeyword(@Param("keyword") String keyword, Pageable pageable);
    
    List<WorkOrder> findByWoStatusLocalDescr(String status);
    
    @Query("SELECT w FROM WorkOrder w ORDER BY w.duration DESC")
    List<WorkOrder> findLongestDowntimes();
    
    @Query("SELECT w FROM WorkOrder w WHERE w.machineName LIKE %?1%")
    List<WorkOrder> findByMachineNameContaining(String machineName);
    
    /** Умная выборка по статусу с лимитом */
    @Query("SELECT w FROM WorkOrder w WHERE w.woStatusLocalDescr = :status ORDER BY w.duration DESC")
    List<WorkOrder> findByStatusWithLimit(@Param("status") String status, Pageable pageable);
    
    /** Умная выборка по машине с лимитом */
    @Query("SELECT w FROM WorkOrder w WHERE w.machineName LIKE %:machine% ORDER BY w.duration DESC")
    List<WorkOrder> findByMachineWithLimit(@Param("machine") String machine, Pageable pageable);
    
    /** Последние отчеты с лимитом */
    @Query("SELECT w FROM WorkOrder w ORDER BY w.duration DESC")
    List<WorkOrder> findRecentReports(Pageable pageable);
    
    /** Последние 15 записей для отображения нарядов на работы */
    @Query("SELECT w FROM WorkOrder w WHERE w.isDeleted = 0 ORDER BY w.dateT1 DESC")
    List<WorkOrder> findLast15WorkOrders(Pageable pageable);
    
    /** Получение активных нарядов (не удаленных и не закрытых) */
    @Query("SELECT w FROM WorkOrder w WHERE w.isDeleted = 0 AND w.woStatusLocalDescr NOT IN ('Закрыто', 'Выполнено') ORDER BY w.dateT1 DESC")
    List<WorkOrder> findActiveWorkOrders(Pageable pageable);
    
    /** Получение нарядов по типу */
    @Query("SELECT w FROM WorkOrder w WHERE w.typeWo = :typeWo AND w.isDeleted = 0 ORDER BY w.dateT1 DESC")
    List<WorkOrder> findByTypeWo(@Param("typeWo") String typeWo, Pageable pageable);
    
    /** Получение нарядов по зоне */
    @Query("SELECT w FROM WorkOrder w WHERE w.plantDepartmentGeographicalCodeName = :area AND w.isDeleted = 0 ORDER BY w.dateT1 DESC")
    List<WorkOrder> findByArea(@Param("area") String area, Pageable pageable);
}
