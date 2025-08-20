package ru.georgdeveloper.assistantcore.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import ru.georgdeveloper.assistantcore.model.SummaryOfSolutions;
import ru.georgdeveloper.assistantcore.repository.SummaryOfSolutionsRepository;
import java.util.List;

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
}
