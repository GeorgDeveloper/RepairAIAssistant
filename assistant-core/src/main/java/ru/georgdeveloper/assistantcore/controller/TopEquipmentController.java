package ru.georgdeveloper.assistantcore.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import ru.georgdeveloper.assistantcore.repository.TopEquipmentRepository;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/top-equipment")
public class TopEquipmentController {

    @Autowired
    private TopEquipmentRepository repository;

    @GetMapping("/data")
    public List<Map<String, Object>> getTopEquipment(
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo,
            @RequestParam(required = false) String week,
            @RequestParam(required = false) String area,
            @RequestParam(required = false) String failureType,
            @RequestParam(required = false) Integer limit
    ) {
        return repository.getTopEquipment(dateFrom, dateTo, week, area, failureType, limit);
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

    @GetMapping("/drilldown/causes")
    public List<Map<String, Object>> getEquipmentCauses(@RequestParam String machine,
                                                        @RequestParam(required = false) String dateFrom,
                                                        @RequestParam(required = false) String dateTo,
                                                        @RequestParam(required = false) String week,
                                                        @RequestParam(required = false) String area,
                                                        @RequestParam(required = false) String failureType) {
        return repository.getEquipmentCauses(machine, dateFrom, dateTo, week, area, failureType);
    }

    @GetMapping("/drilldown/mechanisms")
    public List<Map<String, Object>> getEquipmentMechanisms(@RequestParam String machine,
                                                            @RequestParam String cause,
                                                            @RequestParam(required = false) String dateFrom,
                                                            @RequestParam(required = false) String dateTo,
                                                            @RequestParam(required = false) String week,
                                                            @RequestParam(required = false) String area,
                                                            @RequestParam(required = false) String failureType) {
        return repository.getEquipmentMechanisms(machine, cause, dateFrom, dateTo, week, area, failureType);
    }

    @GetMapping("/drilldown/events")
    public List<Map<String, Object>> getEquipmentEvents(@RequestParam String machine,
                                                        @RequestParam String cause,
                                                        @RequestParam String mechanism,
                                                        @RequestParam(required = false) String dateFrom,
                                                        @RequestParam(required = false) String dateTo,
                                                        @RequestParam(required = false) String week,
                                                        @RequestParam(required = false) String area,
                                                        @RequestParam(required = false) String failureType) {
        return repository.getEquipmentEvents(machine, cause, mechanism, dateFrom, dateTo, week, area, failureType);
    }

    @GetMapping("/breakdown-details")
    public List<Map<String, Object>> getBreakdownDetailsForDateAndArea(@RequestParam("date") String date,
                                                                       @RequestParam("area") String area) {
        return repository.getBreakdownDetailsForDateAndArea(date, area);
    }

    @GetMapping("/drilldown/all-events")
    public List<Map<String, Object>> getEquipmentAllEvents(@RequestParam String machine,
                                                            @RequestParam(required = false) String dateFrom,
                                                            @RequestParam(required = false) String dateTo,
                                                            @RequestParam(required = false) String week,
                                                            @RequestParam(required = false) String area,
                                                            @RequestParam(required = false) String failureType) {
        return repository.getEquipmentAllEvents(machine, dateFrom, dateTo, week, area, failureType);
    }
}
