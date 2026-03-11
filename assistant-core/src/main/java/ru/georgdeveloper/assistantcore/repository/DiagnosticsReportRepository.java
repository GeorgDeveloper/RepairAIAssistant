package ru.georgdeveloper.assistantcore.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.georgdeveloper.assistantcore.model.DiagnosticsReport;

@Repository
public interface DiagnosticsReportRepository extends JpaRepository<DiagnosticsReport, Long> {
}

