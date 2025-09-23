package ru.georgdeveloper.assistantweb.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/top-equipment")
@CrossOrigin(origins = "*")
public class TopEquipmentWebController {

    @Value("${core.service.url:http://localhost:8080}")
    private String coreServiceUrl;

    @Autowired
    private RestTemplate restTemplate;

    @GetMapping("/data")
    public List<Map<String, Object>> data(@RequestParam(required = false) String dateFrom,
                                          @RequestParam(required = false) String dateTo,
                                          @RequestParam(required = false) String week,
                                          @RequestParam(required = false) String area,
                                          @RequestParam(required = false) String failureType,
                                          @RequestParam(required = false) Integer limit) {
        StringBuilder url = new StringBuilder(coreServiceUrl + "/top-equipment/data?");
        if (dateFrom != null) url.append("dateFrom=").append(dateFrom).append('&');
        if (dateTo != null) url.append("dateTo=").append(dateTo).append('&');
        if (week != null) url.append("week=").append(week).append('&');
        if (area != null) url.append("area=").append(area).append('&');
        if (failureType != null) url.append("failureType=").append(failureType).append('&');
        if (limit != null) url.append("limit=").append(limit).append('&');
        return restTemplate.getForObject(url.toString(), List.class);
    }

    @GetMapping("/weeks")
    public List<Map<String, Object>> weeks() {
        return restTemplate.getForObject(coreServiceUrl + "/top-equipment/weeks", List.class);
    }

    @GetMapping("/areas")
    public List<Map<String, Object>> areas() {
        return restTemplate.getForObject(coreServiceUrl + "/top-equipment/areas", List.class);
    }

    @GetMapping("/failure-types")
    public List<Map<String, Object>> failureTypes() {
        return restTemplate.getForObject(coreServiceUrl + "/top-equipment/failure-types", List.class);
    }

    @GetMapping("/drilldown/causes")
    public List<Map<String, Object>> drillCauses(@RequestParam String machine,
                                                 @RequestParam(required = false) String dateFrom,
                                                 @RequestParam(required = false) String dateTo,
                                                 @RequestParam(required = false) String week,
                                                 @RequestParam(required = false) String area) {
        StringBuilder url = new StringBuilder(coreServiceUrl + "/top-equipment/drilldown/causes?machine=" + machine);
        if (dateFrom != null) url.append("&dateFrom=").append(dateFrom);
        if (dateTo != null) url.append("&dateTo=").append(dateTo);
        if (week != null) url.append("&week=").append(week);
        if (area != null) url.append("&area=").append(area);
        return restTemplate.getForObject(url.toString(), List.class);
    }

    @GetMapping("/drilldown/mechanisms")
    public List<Map<String, Object>> drillMechanisms(@RequestParam String machine,
                                                     @RequestParam String cause,
                                                     @RequestParam(required = false) String dateFrom,
                                                     @RequestParam(required = false) String dateTo,
                                                     @RequestParam(required = false) String week,
                                                     @RequestParam(required = false) String area) {
        StringBuilder url = new StringBuilder(coreServiceUrl + "/top-equipment/drilldown/mechanisms?machine=" + machine + "&cause=" + cause);
        if (dateFrom != null) url.append("&dateFrom=").append(dateFrom);
        if (dateTo != null) url.append("&dateTo=").append(dateTo);
        if (week != null) url.append("&week=").append(week);
        if (area != null) url.append("&area=").append(area);
        return restTemplate.getForObject(url.toString(), List.class);
    }

    @GetMapping("/drilldown/events")
    public List<Map<String, Object>> drillEvents(@RequestParam String machine,
                                                 @RequestParam String cause,
                                                 @RequestParam String mechanism,
                                                 @RequestParam(required = false) String dateFrom,
                                                 @RequestParam(required = false) String dateTo,
                                                 @RequestParam(required = false) String week,
                                                 @RequestParam(required = false) String area) {
        StringBuilder url = new StringBuilder(coreServiceUrl + "/top-equipment/drilldown/events?machine=" + machine + "&cause=" + cause + "&mechanism=" + mechanism);
        if (dateFrom != null) url.append("&dateFrom=").append(dateFrom);
        if (dateTo != null) url.append("&dateTo=").append(dateTo);
        if (week != null) url.append("&week=").append(week);
        if (area != null) url.append("&area=").append(area);
        return restTemplate.getForObject(url.toString(), List.class);
    }
}
