package ru.georgdeveloper.assistantcore.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.georgdeveloper.assistantcore.model.Manual;
import java.util.List;

@Repository
public interface ManualRepository extends JpaRepository<Manual, Long> {
    
    List<Manual> findByDeviceType(String deviceType);
    
    @Query("SELECT m FROM Manual m WHERE m.content LIKE %:keyword%")
    List<Manual> findByContentContaining(@Param("keyword") String keyword);
}