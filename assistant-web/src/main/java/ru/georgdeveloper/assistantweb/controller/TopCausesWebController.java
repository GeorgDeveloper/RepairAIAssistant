package ru.georgdeveloper.assistantweb.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/top-causes")
@CrossOrigin(origins = "*")
public class TopCausesWebController {

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
                                          @RequestParam(required = false) String machine,
                                          @RequestParam(required = false) Integer limit) {
        StringBuilder url = new StringBuilder(coreServiceUrl + "/top-causes/data?");
        if (dateFrom != null) url.append("dateFrom=").append(dateFrom).append('&');
        if (dateTo != null) url.append("dateTo=").append(dateTo).append('&');
        if (week != null) url.append("week=").append(week).append('&');
        if (area != null) url.append("area=").append(area).append('&');
        if (failureType != null) url.append("failureType=").append(failureType).append('&');
        if (machine != null) url.append("machine=").append(machine).append('&');
        if (limit != null) url.append("limit=").append(limit).append('&');
        return restTemplate.getForObject(url.toString(), List.class);
    }

    @GetMapping("/weeks")
    public List<Map<String, Object>> weeks() {
        return restTemplate.getForObject(coreServiceUrl + "/top-causes/weeks", List.class);
    }

    @GetMapping("/areas")
    public List<Map<String, Object>> areas() {
        return restTemplate.getForObject(coreServiceUrl + "/top-causes/areas", List.class);
    }

    @GetMapping("/failure-types")
    public List<Map<String, Object>> failureTypes() {
        return restTemplate.getForObject(coreServiceUrl + "/top-causes/failure-types", List.class);
    }

    @GetMapping("/machines")
    public List<Map<String, Object>> machines(@RequestParam(required = false) String area) {
        String url = coreServiceUrl + "/top-causes/machines" + (area != null ? ("?area=" + area) : "");
        return restTemplate.getForObject(url, List.class);
    }

    @GetMapping("/drilldown/machines")
    public List<Map<String, Object>> drillMachines(@RequestParam String cause,
                                                   @RequestParam(required = false) String dateFrom,
                                                   @RequestParam(required = false) String dateTo,
                                                   @RequestParam(required = false) String week,
                                                   @RequestParam(required = false) String area) {
        StringBuilder url = new StringBuilder(coreServiceUrl + "/top-causes/drilldown/machines?cause=" + cause);
        if (dateFrom != null) url.append("&dateFrom=").append(dateFrom);
        if (dateTo != null) url.append("&dateTo=").append(dateTo);
        if (week != null) url.append("&week=").append(week);
        if (area != null) url.append("&area=").append(area);
        return restTemplate.getForObject(url.toString(), List.class);
    }

    @GetMapping("/drilldown/mechanisms")
    public List<Map<String, Object>> drillMechanisms(@RequestParam String cause,
                                                     @RequestParam String machine,
                                                     @RequestParam(required = false) String dateFrom,
                                                     @RequestParam(required = false) String dateTo,
                                                     @RequestParam(required = false) String week,
                                                     @RequestParam(required = false) String area) {
        StringBuilder url = new StringBuilder(coreServiceUrl + "/top-causes/drilldown/mechanisms?cause=" + cause + "&machine=" + machine);
        if (dateFrom != null) url.append("&dateFrom=").append(dateFrom);
        if (dateTo != null) url.append("&dateTo=").append(dateTo);
        if (week != null) url.append("&week=").append(week);
        if (area != null) url.append("&area=").append(area);
        return restTemplate.getForObject(url.toString(), List.class);
    }
}


