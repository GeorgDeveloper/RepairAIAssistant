package ru.georgdeveloper.assistantweb.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/dynamics")
public class DynamicsWebController {

    @Value("${core.service.url:http://localhost:8080}")
    private String coreServiceUrl;

    private final RestTemplate restTemplate;

    public DynamicsWebController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @SuppressWarnings("unchecked")
    @GetMapping("/years")
    public List<Map<String, Object>> years() {
        return (List<Map<String, Object>>) (List<?>) restTemplate.getForObject(coreServiceUrl + "/dynamics/years", List.class);
    }

    @SuppressWarnings("unchecked")
    @GetMapping("/months")
    public List<Map<String, Object>> months(@RequestParam(required = false) List<String> year) {
        if (year == null || year.isEmpty()) {
            return (List<Map<String, Object>>) (List<?>) restTemplate.getForObject(coreServiceUrl + "/dynamics/months", List.class);
        }
        String yearParam = String.join(",", year);
        return (List<Map<String, Object>>) (List<?>) restTemplate.getForObject(coreServiceUrl + "/dynamics/months?year=" + yearParam, List.class);
    }

    @SuppressWarnings("unchecked")
    @GetMapping("/weeks")
    public List<Map<String, Object>> weeks(@RequestParam(required = false) List<String> year, @RequestParam(required = false) List<String> month) {
        if (year == null || year.isEmpty() || month == null || month.isEmpty()) {
            return (List<Map<String, Object>>) (List<?>) restTemplate.getForObject(coreServiceUrl + "/dynamics/weeks", List.class);
        }
        String yearParam = String.join(",", year);
        String monthParam = String.join(",", month);
        return (List<Map<String, Object>>) (List<?>) restTemplate.getForObject(coreServiceUrl + "/dynamics/weeks?year=" + yearParam + "&month=" + monthParam, List.class);
    }

    @SuppressWarnings("unchecked")
    @GetMapping("/failure-types")
    public List<Map<String, Object>> failureTypes() {
        return (List<Map<String, Object>>) (List<?>) restTemplate.getForObject(coreServiceUrl + "/dynamics/failure-types", List.class);
    }

    @SuppressWarnings("unchecked")
    @GetMapping("/data")
    public List<Map<String, Object>> data(@RequestParam(required = false) List<String> year,
                                          @RequestParam(required = false) List<String> month,
                                          @RequestParam(required = false) List<String> week,
                                          @RequestParam(required = false) List<String> area,
                                          @RequestParam(required = false) List<String> equipment,
                                          @RequestParam(required = false, name = "failureType") List<String> failureType) {
        StringBuilder url = new StringBuilder(coreServiceUrl + "/dynamics/data?");
        if (year != null && !year.isEmpty()) url.append("year=").append(String.join(",", year)).append('&');
        if (month != null && !month.isEmpty()) url.append("month=").append(String.join(",", month)).append('&');
        if (week != null && !week.isEmpty()) url.append("week=").append(String.join(",", week)).append('&');
        if (area != null && !area.isEmpty()) url.append("area=").append(String.join(",", area)).append('&');
        if (equipment != null && !equipment.isEmpty()) url.append("equipment=").append(String.join(",", equipment)).append('&');
        if (failureType != null && !failureType.isEmpty()) url.append("failureType=").append(String.join(",", failureType)).append('&');
        return (List<Map<String, Object>>) (List<?>) restTemplate.getForObject(url.toString(), List.class);
    }

    @SuppressWarnings("unchecked")
    @GetMapping("/type/data")
    public List<Map<String, Object>> typeData(@RequestParam(required = false) List<String> year,
                                               @RequestParam(required = false) List<String> month,
                                               @RequestParam(required = false) List<String> week,
                                               @RequestParam(required = false) List<String> area,
                                               @RequestParam(required = false) List<String> equipment) {
        StringBuilder url = new StringBuilder(coreServiceUrl + "/dynamics/type/data?");
        if (year != null && !year.isEmpty()) url.append("year=").append(String.join(",", year)).append('&');
        if (month != null && !month.isEmpty()) url.append("month=").append(String.join(",", month)).append('&');
        if (week != null && !week.isEmpty()) url.append("week=").append(String.join(",", week)).append('&');
        if (area != null && !area.isEmpty()) url.append("area=").append(String.join(",", area)).append('&');
        if (equipment != null && !equipment.isEmpty()) url.append("equipment=").append(String.join(",", equipment)).append('&');
        return (List<Map<String, Object>>) (List<?>) restTemplate.getForObject(url.toString(), List.class);
    }

    @SuppressWarnings("unchecked")
    @GetMapping("/type/failure-types")
    public List<Map<String, Object>> typeFailureTypes() {
        return (List<Map<String, Object>>) (List<?>) restTemplate.getForObject(coreServiceUrl + "/dynamics/type/failure-types", List.class);
    }
}


