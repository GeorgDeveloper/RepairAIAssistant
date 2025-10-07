package ru.georgdeveloper.assistantcore.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import ru.georgdeveloper.assistantcore.repository.DynamicsRepository;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/dynamics")
public class DynamicsController {

    @Autowired
    private DynamicsRepository dynamicsRepository;

    @GetMapping("/data")
    @ResponseBody
    public List<Map<String, Object>> getDynamicsData(
            @RequestParam(required = false) List<String> year,
            @RequestParam(required = false) List<String> month,
            @RequestParam(required = false) List<String> week,
            @RequestParam(required = false) List<String> area,
            @RequestParam(required = false) List<String> equipment,
            @RequestParam(required = false) List<String> failureType) {
        
        return dynamicsRepository.getDynamicsData(year, month, week, area, equipment, failureType);
    }

    @GetMapping("/years")
    @ResponseBody
    public List<Map<String, Object>> getYears() {
        return dynamicsRepository.getYears();
    }

    @GetMapping("/months")
    @ResponseBody
    public List<Map<String, Object>> getMonths(@RequestParam(required = false) List<String> year) {
        return dynamicsRepository.getMonths(year);
    }

    @GetMapping("/weeks")
    @ResponseBody
    public List<Map<String, Object>> getWeeks(@RequestParam(required = false) List<String> year, @RequestParam(required = false) List<String> month) {
        return dynamicsRepository.getWeeks(year, month);
    }

    @GetMapping("/failure-types")
    @ResponseBody
    public List<Map<String, Object>> getFailureTypes() {
        return dynamicsRepository.getFailureTypes();
    }
}