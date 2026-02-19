package ru.georgdeveloper.assistantcore.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import ru.georgdeveloper.assistantcore.repository.TopAreasRepository;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/top-areas")
public class TopAreasController {

    @Autowired
    private TopAreasRepository repository;

    @GetMapping("/data")
    public List<Map<String, Object>> getTopAreas(@RequestParam(required = false) String dateFrom,
                                                 @RequestParam(required = false) String dateTo,
                                                 @RequestParam(required = false) String week,
                                                 @RequestParam(required = false) String failureType,
                                                 @RequestParam(required = false) Integer limit) {
        return repository.getTopAreas(dateFrom, dateTo, week, failureType, limit);
    }

    @GetMapping("/weeks")
    public List<Map<String, Object>> getWeeks() { return repository.getWeeks(); }

    @GetMapping("/failure-types")
    public List<Map<String, Object>> getFailureTypes() { return repository.getFailureTypes(); }

    @GetMapping("/drilldown/categories")
    public List<Map<String, Object>> getCategories(@RequestParam String area,
                                                   @RequestParam(required = false) String dateFrom,
                                                   @RequestParam(required = false) String dateTo,
                                                   @RequestParam(required = false) String week,
                                                   @RequestParam(required = false) String failureType) {
        return repository.getAreaTypeCategories(area, dateFrom, dateTo, week, failureType);
    }

    @GetMapping("/drilldown/causes")
    public List<Map<String, Object>> getCauses(@RequestParam String area,
                                               @RequestParam String category,
                                               @RequestParam(required = false) String dateFrom,
                                               @RequestParam(required = false) String dateTo,
                                               @RequestParam(required = false) String week,
                                               @RequestParam(required = false) String failureType) {
        return repository.getAreaCategoryCauses(area, category, dateFrom, dateTo, week, failureType);
    }

    @GetMapping("/drilldown/events")
    public List<Map<String, Object>> getEvents(@RequestParam String area,
                                               @RequestParam String category,
                                               @RequestParam String cause,
                                               @RequestParam(required = false) String dateFrom,
                                               @RequestParam(required = false) String dateTo,
                                               @RequestParam(required = false) String week,
                                               @RequestParam(required = false) String failureType) {
        return repository.getAreaCategoryEvents(area, category, cause, dateFrom, dateTo, week, failureType);
    }
}
