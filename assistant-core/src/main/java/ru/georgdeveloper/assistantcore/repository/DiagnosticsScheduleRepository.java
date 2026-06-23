package ru.georgdeveloper.assistantcore.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.georgdeveloper.assistantcore.model.DiagnosticsSchedule;

import java.util.Optional;

@Repository
public interface DiagnosticsScheduleRepository extends JpaRepository<DiagnosticsSchedule, Long> {
    Optional<DiagnosticsSchedule> findByYear(Integer year);
}

