package ru.georgdeveloper.assistantcore.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.georgdeveloper.assistantcore.model.SummaryOfSolutions;


public interface SummaryOfSolutionsRepository extends JpaRepository<SummaryOfSolutions, String> {
}
