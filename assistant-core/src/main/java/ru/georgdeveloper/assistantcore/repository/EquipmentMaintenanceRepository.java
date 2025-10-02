package ru.georgdeveloper.assistantcore.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.georgdeveloper.assistantcore.model.EquipmentMaintenanceRecord;
import org.springframework.data.domain.Pageable;
import java.util.List;

@Repository
public interface EquipmentMaintenanceRepository extends JpaRepository<EquipmentMaintenanceRecord, Long> {
    
    List<EquipmentMaintenanceRecord> findByStatus(String status);
    
    @Query("SELECT e FROM EquipmentMaintenanceRecord e ORDER BY e.startBdT1 DESC")
    List<EquipmentMaintenanceRecord> findAllOrderByStartDateDesc();
    
    @Query("SELECT e FROM EquipmentMaintenanceRecord e WHERE e.machineName LIKE %?1%")
    List<EquipmentMaintenanceRecord> findByMachineNameContaining(String machineName);
    
    /** Умная выборка по статусу с лимитом */
    @Query("SELECT e FROM EquipmentMaintenanceRecord e WHERE e.status = :status ORDER BY e.startBdT1 DESC")
    List<EquipmentMaintenanceRecord> findByStatusWithLimit(@Param("status") String status, Pageable pageable);
    
    /** Умная выборка по машине и статусу с лимитом */
    @Query("SELECT e FROM EquipmentMaintenanceRecord e WHERE e.machineName LIKE %:machine% AND e.status = :status ORDER BY e.startBdT1 DESC")
    List<EquipmentMaintenanceRecord> findByMachineAndStatusWithLimit(@Param("machine") String machine, @Param("status") String status, Pageable pageable);
    
    /** Умная выборка только по машине с лимитом */
    @Query("SELECT e FROM EquipmentMaintenanceRecord e WHERE e.machineName LIKE %:machine% ORDER BY e.startBdT1 DESC")
    List<EquipmentMaintenanceRecord> findByMachineWithLimit(@Param("machine") String machine, Pageable pageable);
    
    /** Последние записи с лимитом */
    @Query("SELECT e FROM EquipmentMaintenanceRecord e ORDER BY e.startBdT1 DESC")
    List<EquipmentMaintenanceRecord> findRecentRecords(Pageable pageable);
    
    /** Топ записей по времени ремонта (TTR) */
    @Query("SELECT e FROM EquipmentMaintenanceRecord e WHERE e.ttr IS NOT NULL ORDER BY e.ttr DESC")
    List<EquipmentMaintenanceRecord> findTopByTtr(Pageable pageable);
    
    /** Топ записей по времени простоя (machine_downtime) */
    @Query("SELECT e FROM EquipmentMaintenanceRecord e WHERE e.machineDowntime IS NOT NULL ORDER BY e.machineDowntime DESC")
    List<EquipmentMaintenanceRecord> findTopByDowntime(Pageable pageable);
    
    /** Поиск по месяцу с сортировкой по времени простоя */
    @Query("SELECT e FROM EquipmentMaintenanceRecord e WHERE e.monthName = :month AND e.machineDowntime IS NOT NULL ORDER BY e.machineDowntime DESC")
    List<EquipmentMaintenanceRecord> findByMonthOrderByDowntime(@Param("month") String month, Pageable pageable);
    
    /** Поиск по месяцу с сортировкой по TTR */
    @Query("SELECT e FROM EquipmentMaintenanceRecord e WHERE e.monthName = :month AND e.ttr IS NOT NULL ORDER BY e.ttr DESC")
    List<EquipmentMaintenanceRecord> findByMonth(@Param("month") String month, Pageable pageable);
    
    /** Поиск по комментариям */
    @Query("SELECT e FROM EquipmentMaintenanceRecord e WHERE e.comments LIKE %:searchTerm% ORDER BY e.startBdT1 DESC")
    List<EquipmentMaintenanceRecord> findByCommentsContaining(@Param("searchTerm") String searchTerm, Pageable pageable);
    
    /** Подсчет записей по статусу */
    @Query("SELECT COUNT(e) FROM EquipmentMaintenanceRecord e WHERE e.status = :status")
    Long countByStatus(@Param("status") String status);
    
    /** Поиск похожих случаев по описанию проблемы */
    @Query("SELECT e FROM EquipmentMaintenanceRecord e WHERE e.description LIKE %:problem% ORDER BY e.startBdT1 DESC")
    List<EquipmentMaintenanceRecord> findSimilarCasesByProblem(@Param("problem") String problem, Pageable pageable);
    
    /** Поиск похожих случаев по типу оборудования и проблеме */
    @Query("SELECT e FROM EquipmentMaintenanceRecord e WHERE e.machineName LIKE %:machineType% AND e.description LIKE %:problem% ORDER BY e.startBdT1 DESC")
    List<EquipmentMaintenanceRecord> findSimilarCases(@Param("machineType") String machineType, @Param("problem") String problem, Pageable pageable);
    
    /** Поиск по датам (диапазон) */
    @Query("SELECT e FROM EquipmentMaintenanceRecord e WHERE e.date BETWEEN :startDate AND :endDate ORDER BY e.startBdT1 DESC")
    List<EquipmentMaintenanceRecord> findByDateRange(@Param("startDate") String startDate, @Param("endDate") String endDate, Pageable pageable);
    
    /** Получение новых записей для переобучения */
    @Query("SELECT e FROM EquipmentMaintenanceRecord e WHERE e.createdAt > :since ORDER BY e.createdAt DESC")
    List<EquipmentMaintenanceRecord> findNewRecordsSince(@Param("since") java.time.LocalDateTime since, Pageable pageable);
    
    /** Поиск по типу неисправности */
    @Query("SELECT e FROM EquipmentMaintenanceRecord e WHERE e.failureType = :failureType ORDER BY e.startBdT1 DESC")
    List<EquipmentMaintenanceRecord> findByFailureType(@Param("failureType") String failureType, Pageable pageable);
    
    /** Универсальный поиск по ключевому слову */
    @Query("SELECT e FROM EquipmentMaintenanceRecord e WHERE " +
           "LOWER(e.description) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(e.comments) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(e.machineName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(e.cause) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "ORDER BY e.startBdT1 DESC")
    List<EquipmentMaintenanceRecord> findByKeyword(@Param("keyword") String keyword, Pageable pageable);
    
    /** Поиск записей с ID больше указанного (для инкрементальной миграции) */
    @Query("SELECT e FROM EquipmentMaintenanceRecord e WHERE e.id > :lastId ORDER BY e.id ASC")
    List<EquipmentMaintenanceRecord> findByIdGreaterThan(@Param("lastId") Long lastId, Pageable pageable);
}