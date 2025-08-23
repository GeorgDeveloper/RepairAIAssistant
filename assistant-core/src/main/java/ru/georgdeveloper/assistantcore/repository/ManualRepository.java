package ru.georgdeveloper.assistantcore.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import ru.georgdeveloper.assistantcore.model.Manual;
import java.util.List;

@Repository
public interface ManualRepository extends JpaRepository<Manual, Long> {
    List<Manual> findByDeviceType(String deviceType);

    @Query("SELECT m FROM Manual m WHERE m.content LIKE %:keyword%")
    List<Manual> findByContentContaining(@Param("keyword") String keyword);

    @Modifying
    @Transactional
    @Query("UPDATE Manual m SET m.region = :region, m.equipment = :equipment, m.node = :node, m.deviceType = :deviceType, m.content = :content WHERE m.id = :id")
    void updateByParametr(@Param("id") Long id,
                         @Param("region") String region,
                         @Param("equipment") String equipment,
                         @Param("node") String node,
                         @Param("deviceType") String deviceType,
                         @Param("content") String content);
}