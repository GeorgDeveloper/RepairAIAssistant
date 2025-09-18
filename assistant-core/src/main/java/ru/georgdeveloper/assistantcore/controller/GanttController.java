package ru.georgdeveloper.assistantcore.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import ru.georgdeveloper.assistantcore.repository.GanttRepository;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/gantt")
public class GanttController {

    @Autowired
    private GanttRepository ganttRepository;

    @GetMapping("/data")
    @ResponseBody
    public List<Map<String, Object>> getGanttData(
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo,
            @RequestParam(required = false) String area,
            @RequestParam(required = false) String equipment,
            @RequestParam(required = false) String failureType,
            @RequestParam(required = false) String status) {
        
        return ganttRepository.getGanttData(dateFrom, dateTo, area, equipment, failureType, status);
    }

    @GetMapping("/equipment")
    @ResponseBody
    public List<Map<String, Object>> getEquipment(@RequestParam(required = false) String area) {
        return ganttRepository.getEquipmentByArea(area);
    }

    @GetMapping("/areas")
    @ResponseBody
    public List<Map<String, Object>> getAreas() {
        return ganttRepository.getAreas();
    }

    @GetMapping("/failure-types")
    @ResponseBody
    public List<Map<String, Object>> getFailureTypes() {
        return ganttRepository.getFailureTypes();
    }

    @GetMapping("/statuses")
    @ResponseBody
    public List<Map<String, Object>> getStatuses() {
        return ganttRepository.getStatuses();
    }
}