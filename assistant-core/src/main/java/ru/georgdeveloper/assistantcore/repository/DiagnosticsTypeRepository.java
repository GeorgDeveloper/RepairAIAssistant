package ru.georgdeveloper.assistantcore.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.georgdeveloper.assistantcore.model.DiagnosticsType;

import java.util.List;
import java.util.Optional;

@Repository
public interface DiagnosticsTypeRepository extends JpaRepository<DiagnosticsType, Long> {
    Optional<DiagnosticsType> findByCode(String code);
    List<DiagnosticsType> findByIsActiveTrue();
}

