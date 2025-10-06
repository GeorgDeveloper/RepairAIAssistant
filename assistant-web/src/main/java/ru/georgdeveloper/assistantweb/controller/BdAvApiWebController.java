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
@RequestMapping("/bdav")
public class BdAvApiWebController {

    @Value("${core.service.url:http://localhost:8080}")
    private String coreServiceUrl;

    private final RestTemplate restTemplate;

    public BdAvApiWebController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @SuppressWarnings("unchecked")
    @GetMapping("/years")
    public List<Map<String, Object>> years() {
        return (List<Map<String, Object>>) (List<?>) restTemplate.getForObject(coreServiceUrl + "/bdav/years", List.class);
    }

    @SuppressWarnings("unchecked")
    @GetMapping("/months")
    public List<Map<String, Object>> months(@RequestParam String year) {
        return (List<Map<String, Object>>) (List<?>) restTemplate.getForObject(coreServiceUrl + "/bdav/months?year=" + year, List.class);
    }

    @SuppressWarnings("unchecked")
    @GetMapping("/weeks")
    public List<Map<String, Object>> weeks(@RequestParam String year, @RequestParam String month) {
        return (List<Map<String, Object>>) (List<?>) restTemplate.getForObject(coreServiceUrl + 
                "/bdav/weeks?year=" + year + "&month=" + month, List.class);
    }

    @SuppressWarnings("unchecked")
    @GetMapping("/data")
    public List<Map<String, Object>> data(@RequestParam(required = false) String year,
                                          @RequestParam(required = false) String month,
                                          @RequestParam(required = false) String week,
                                          @RequestParam String area,
                                          @RequestParam String metric) {
        StringBuilder url = new StringBuilder(coreServiceUrl + "/bdav/data?");
        if (year != null) url.append("year=").append(year).append('&');
        if (month != null) url.append("month=").append(month).append('&');
        if (week != null) url.append("week=").append(week).append('&');
        url.append("area=").append(area).append('&');
        url.append("metric=").append(metric).append('&');
        return (List<Map<String, Object>>) (List<?>) restTemplate.getForObject(url.toString(), List.class);
    }
}


