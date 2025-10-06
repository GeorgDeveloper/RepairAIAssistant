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
@RequestMapping("/gantt")
public class GanttWebController {

    @Value("${core.service.url:http://localhost:8080}")
    private String coreServiceUrl;

    private final RestTemplate restTemplate;

    public GanttWebController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @SuppressWarnings("unchecked")
    @GetMapping("/equipment")
    public List<Map<String, Object>> equipment(@RequestParam(required = false) String area) {
        String url = coreServiceUrl + "/gantt/equipment" + (area != null ? ("?area=" + area) : "");
        return (List<Map<String, Object>>) (List<?>) restTemplate.getForObject(url, List.class);
    }

    @SuppressWarnings("unchecked")
    @GetMapping("/areas")
    public List<Map<String, Object>> areas() {
        return (List<Map<String, Object>>) (List<?>) restTemplate.getForObject(coreServiceUrl + "/gantt/areas", List.class);
    }

    @SuppressWarnings("unchecked")
    @GetMapping("/failure-types")
    public List<Map<String, Object>> failureTypes() {
        return (List<Map<String, Object>>) (List<?>) restTemplate.getForObject(coreServiceUrl + "/gantt/failure-types", List.class);
    }

    @SuppressWarnings("unchecked")
    @GetMapping("/statuses")
    public List<Map<String, Object>> statuses() {
        return (List<Map<String, Object>>) (List<?>) restTemplate.getForObject(coreServiceUrl + "/gantt/statuses", List.class);
    }

    @SuppressWarnings("unchecked")
    @GetMapping("/data")
    public List<Map<String, Object>> data(@RequestParam(required = false) String dateFrom,
                                          @RequestParam(required = false) String dateTo,
                                          @RequestParam(required = false) String area,
                                          @RequestParam(required = false) String equipment,
                                          @RequestParam(required = false) String failureType,
                                          @RequestParam(required = false) String status) {
        StringBuilder url = new StringBuilder(coreServiceUrl + "/gantt/data?");
        if (dateFrom != null) url.append("dateFrom=").append(dateFrom).append('&');
        if (dateTo != null) url.append("dateTo=").append(dateTo).append('&');
        if (area != null) url.append("area=").append(area).append('&');
        if (equipment != null) url.append("equipment=").append(equipment).append('&');
        if (failureType != null) url.append("failureType=").append(failureType).append('&');
        if (status != null) url.append("status=").append(status).append('&');
        return (List<Map<String, Object>>) (List<?>) restTemplate.getForObject(url.toString(), List.class);
    }
}


