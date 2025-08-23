package ru.georgdeveloper.assistantcore.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import ru.georgdeveloper.assistantcore.model.SummaryOfSolutions;
import ru.georgdeveloper.assistantcore.repository.SummaryOfSolutionsRepository;
import org.springframework.http.ResponseEntity;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/summary-of-solutions")
public class SummaryOfSolutionsController {
    @Autowired
    private SummaryOfSolutionsRepository repository;

    @GetMapping
    public List<SummaryOfSolutions> getAll() {
        return repository.findAll();
    }

    @PostMapping
    public SummaryOfSolutions add(@RequestBody SummaryOfSolutions solution) {
        return repository.save(solution);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SummaryOfSolutions> getById(@PathVariable Long id) {
        return repository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLongReport(@PathVariable int id) {
        repository.deleteById((long) id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> updateLongReport(@PathVariable int id, @RequestBody Map<String, Object> report) {
        repository.updateByParametr(
                (long) id,
                (String) report.get("date"),
                (String) report.get("executor"),
                (String) report.get("region"),
                (String) report.get("equipment"),
                (String) report.get("node"),
                (String) report.get("notes_on_the_operation_of_the_equipment"),
                (String) report.get("measures_taken"),
                (String) report.get("comments"));

        return ResponseEntity.noContent().build();
    }
}
