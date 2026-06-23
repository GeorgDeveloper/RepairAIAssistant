package ru.georgdeveloper.assistantcore.controller;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import ru.georgdeveloper.assistantcore.energy.EnergyResource;
import ru.georgdeveloper.assistantcore.model.EnergyDailyValue;
import ru.georgdeveloper.assistantcore.repository.EnergyDailyValueRepository;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/energy")
public class EnergyDailyValuesController {

    private final EnergyDailyValueRepository energyDailyValueRepository;

    public EnergyDailyValuesController(EnergyDailyValueRepository energyDailyValueRepository) {
        this.energyDailyValueRepository = energyDailyValueRepository;
    }

    @GetMapping("/daily-values")
    public ResponseEntity<List<EnergyDailyValue>> listDailyValues(
            @RequestParam("resource") String resource,
            @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        EnergyResource key;
        try {
            key = EnergyResource.valueOf(resource.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.notFound().build();
        }
        if (to.isBefore(from)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "to before from");
        }
        List<EnergyDailyValue> rows =
                energyDailyValueRepository.findByResourceCodeAndFactDateBetweenOrderByFactDateAscMetricIdAsc(
                        key.name(), from, to);
        return ResponseEntity.ok(rows);
    }
}
