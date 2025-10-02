package ru.georgdeveloper.assistantcore.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import ru.georgdeveloper.assistantcore.model.SummaryOfSolutions;

public interface SummaryOfSolutionsRepository extends JpaRepository<SummaryOfSolutions, Long> {
    @Query("SELECT s FROM SummaryOfSolutions s WHERE " +
	    "LOWER(s.notes_on_the_operation_of_the_equipment) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
	    "LOWER(s.measures_taken) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
	    "LOWER(s.comments) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<SummaryOfSolutions> searchByKeyword(@Param("keyword") String keyword);

    @Modifying
    @Transactional
    @Query("UPDATE SummaryOfSolutions SET date = :date, executor = :executor, region = :region, equipment = :equipment, node = :node, notes_on_the_operation_of_the_equipment = :notes_on_the_operation_of_the_equipment, measures_taken = :measures_taken, comments = :comments WHERE id = :id")
    void updateByParametr(@Param("id") Long id,
			    @Param("date") String date,
			    @Param("executor") String executor,
			    @Param("region") String region,
			    @Param("equipment") String equipment,
			    @Param("node") String node,
			    @Param("notes_on_the_operation_of_the_equipment") String notes_on_the_operation_of_the_equipment,
			    @Param("measures_taken") String measures_taken,
			    @Param("comments") String comments);
    
    /** Поиск записей с ID больше указанного (для инкрементальной миграции) */
    @Query("SELECT s FROM SummaryOfSolutions s WHERE s.id > :lastId ORDER BY s.id ASC")
    List<SummaryOfSolutions> findByIdGreaterThan(@Param("lastId") Long lastId);
}
