package ru.georgdeveloper.assistantcore.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import ru.georgdeveloper.assistantcore.repository.TopCausesRepository;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/top-causes")
public class TopCausesController {

    @Autowired
    private TopCausesRepository repository;

    @GetMapping("/data")
    public List<Map<String, Object>> getTopCauses(
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo,
            @RequestParam(required = false) String week,
            @RequestParam(required = false) String area,
            @RequestParam(required = false) String failureType,
            @RequestParam(required = false) String machine,
            @RequestParam(required = false) Integer limit
    ) {
        return repository.getTopCauses(dateFrom, dateTo, week, area, failureType, machine, limit);
    }

    @GetMapping("/production-days")
    public List<Map<String, Object>> getProductionDays(@RequestParam(required = false) String monthsBack) {
        return repository.getProductionDays(monthsBack);
    }

    @GetMapping("/weeks")
    public List<Map<String, Object>> getWeeks() {
        return repository.getWeeks();
    }

    @GetMapping("/areas")
    public List<Map<String, Object>> getAreas() {
        return repository.getAreas();
    }

    @GetMapping("/failure-types")
    public List<Map<String, Object>> getFailureTypes() {
        return repository.getFailureTypes();
    }

    @GetMapping("/machines")
    public List<Map<String, Object>> getMachines(@RequestParam(required = false) String area) {
        return repository.getMachines(area);
    }

    @GetMapping("/drilldown/machines")
    public List<Map<String, Object>> getCauseMachines(@RequestParam String cause,
                                                      @RequestParam(required = false) String dateFrom,
                                                      @RequestParam(required = false) String dateTo,
                                                      @RequestParam(required = false) String week,
                                                      @RequestParam(required = false) String area) {
        return repository.getCauseMachines(cause, dateFrom, dateTo, week, area);
    }

    @GetMapping("/drilldown/mechanisms")
    public List<Map<String, Object>> getCauseMechanisms(@RequestParam String cause,
                                                        @RequestParam String machine,
                                                        @RequestParam(required = false) String dateFrom,
                                                        @RequestParam(required = false) String dateTo,
                                                        @RequestParam(required = false) String week,
                                                        @RequestParam(required = false) String area) {
        return repository.getCauseMechanisms(cause, machine, dateFrom, dateTo, week, area);
    }
}


