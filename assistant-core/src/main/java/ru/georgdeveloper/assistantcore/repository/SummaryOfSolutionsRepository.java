package ru.georgdeveloper.assistantcore.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import ru.georgdeveloper.assistantcore.model.SummaryOfSolutions;


public interface SummaryOfSolutionsRepository extends JpaRepository<SummaryOfSolutions, Long> {
	@Query("SELECT s FROM SummaryOfSolutions s WHERE " +
		   "LOWER(s.notes_on_the_operation_of_the_equipment) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
		   "LOWER(s.measures_taken) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
		   "LOWER(s.comments) LIKE LOWER(CONCAT('%', :keyword, '%'))")
	List<SummaryOfSolutions> searchByKeyword(@Param("keyword") String keyword);
}
