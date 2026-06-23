package ru.georgdeveloper.assistantcore.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.georgdeveloper.assistantcore.model.ProductionDaysCorrection;
import ru.georgdeveloper.assistantcore.repository.ProductionDaysCorrectionRepository;

import java.util.List;

/**
 * REST API для управления корректировкой производственных дней по месяцам.
 * Поддерживается несколько диапазонов в месяце (простой в середине месяца).
 * Показатели BD и доступность считаются только по производственным дням; ППР (PM) не пересчитываются.
 */
@RestController
@RequestMapping("/api/production-days-correction")
public class ProductionDaysCorrectionController {

    private final ProductionDaysCorrectionRepository repository;

    @Autowired
    public ProductionDaysCorrectionController(ProductionDaysCorrectionRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<ProductionDaysCorrection> list() {
        return repository.findAll();
    }

    @GetMapping("/{year}/{month}")
    public ResponseEntity<ProductionDaysCorrection> get(@PathVariable int year, @PathVariable int month) {
        return repository.findByYearAndMonth(year, month)
                .flatMap(c -> repository.findById(c.getId()))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<ProductionDaysCorrection> save(@RequestBody ProductionDaysCorrection correction) {
        if (correction.getRanges() != null) {
            for (ru.georgdeveloper.assistantcore.model.ProductionDaysCorrectionRange r : correction.getRanges()) {
                if (r.getFirstProductionDay() < 1 || r.getFirstProductionDay() > 31
                        || r.getLastProductionDay() < 1 || r.getLastProductionDay() > 31
                        || r.getFirstProductionDay() > r.getLastProductionDay()) {
                    return ResponseEntity.badRequest().build();
                }
            }
        }
        repository.save(correction);
        return ResponseEntity.status(HttpStatus.CREATED).body(correction);
    }

    @DeleteMapping("/{year}/{month}")
    public ResponseEntity<Void> delete(@PathVariable int year, @PathVariable int month) {
        int deleted = repository.deleteByYearAndMonth(year, month);
        return deleted > 0 ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }
}
