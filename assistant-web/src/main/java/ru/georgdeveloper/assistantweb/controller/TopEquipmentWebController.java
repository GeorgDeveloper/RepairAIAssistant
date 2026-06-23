package ru.georgdeveloper.assistantweb.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/top-equipment")
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
        return getList(url.toString());
    }

    @GetMapping("/weeks")
    public List<Map<String, Object>> weeks() {
        return getList(coreServiceUrl + "/top-equipment/weeks");
    }

    @GetMapping("/areas")
    public List<Map<String, Object>> areas() {
        return getList(coreServiceUrl + "/top-equipment/areas");
    }

    @GetMapping("/failure-types")
    public List<Map<String, Object>> failureTypes() {
        return getList(coreServiceUrl + "/top-equipment/failure-types");
    }

    @GetMapping("/drilldown/causes")
    public List<Map<String, Object>> drillCauses(@RequestParam String machine,
                                                 @RequestParam(required = false) String dateFrom,
                                                 @RequestParam(required = false) String dateTo,
                                                 @RequestParam(required = false) String week,
                                                 @RequestParam(required = false) String area,
                                                 @RequestParam(required = false) String failureType) {
        StringBuilder url = new StringBuilder(coreServiceUrl + "/top-equipment/drilldown/causes?machine=" + machine);
        if (dateFrom != null) url.append("&dateFrom=").append(dateFrom);
        if (dateTo != null) url.append("&dateTo=").append(dateTo);
        if (week != null) url.append("&week=").append(week);
        if (area != null) url.append("&area=").append(area);
        if (failureType != null) url.append("&failureType=").append(failureType);
        return getList(url.toString());
    }

    @GetMapping("/drilldown/mechanisms")
    public List<Map<String, Object>> drillMechanisms(@RequestParam String machine,
                                                     @RequestParam String cause,
                                                     @RequestParam(required = false) String dateFrom,
                                                     @RequestParam(required = false) String dateTo,
                                                     @RequestParam(required = false) String week,
                                                     @RequestParam(required = false) String area,
                                                     @RequestParam(required = false) String failureType) {
        StringBuilder url = new StringBuilder(coreServiceUrl + "/top-equipment/drilldown/mechanisms?machine=" + machine + "&cause=" + cause);
        if (dateFrom != null) url.append("&dateFrom=").append(dateFrom);
        if (dateTo != null) url.append("&dateTo=").append(dateTo);
        if (week != null) url.append("&week=").append(week);
        if (area != null) url.append("&area=").append(area);
        if (failureType != null) url.append("&failureType=").append(failureType);
        return getList(url.toString());
    }

    @GetMapping("/drilldown/events")
    public List<Map<String, Object>> drillEvents(@RequestParam String machine,
                                                 @RequestParam String cause,
                                                 @RequestParam String mechanism,
                                                 @RequestParam(required = false) String dateFrom,
                                                 @RequestParam(required = false) String dateTo,
                                                 @RequestParam(required = false) String week,
                                                 @RequestParam(required = false) String area,
                                                 @RequestParam(required = false) String failureType) {
        StringBuilder url = new StringBuilder(coreServiceUrl + "/top-equipment/drilldown/events?machine=" + machine + "&cause=" + cause + "&mechanism=" + mechanism);
        if (dateFrom != null) url.append("&dateFrom=").append(dateFrom);
        if (dateTo != null) url.append("&dateTo=").append(dateTo);
        if (week != null) url.append("&week=").append(week);
        if (area != null) url.append("&area=").append(area);
        if (failureType != null) url.append("&failureType=").append(failureType);
        return getList(url.toString());
    }

    @GetMapping("/drilldown/all-events")
    public List<Map<String, Object>> drillAllEvents(@RequestParam String machine,
                                                     @RequestParam(required = false) String dateFrom,
                                                     @RequestParam(required = false) String dateTo,
                                                     @RequestParam(required = false) String week,
                                                     @RequestParam(required = false) String area,
                                                     @RequestParam(required = false) String failureType) {
        StringBuilder url = new StringBuilder(coreServiceUrl + "/top-equipment/drilldown/all-events?machine=" + machine);
        if (dateFrom != null) url.append("&dateFrom=").append(dateFrom);
        if (dateTo != null) url.append("&dateTo=").append(dateTo);
        if (week != null) url.append("&week=").append(week);
        if (area != null) url.append("&area=").append(area);
        if (failureType != null) url.append("&failureType=").append(failureType);
        return getList(url.toString());
    }

    private List<Map<String, Object>> getList(String url) {
        return restTemplate
                .exchange(url, HttpMethod.GET, null,
                        new ParameterizedTypeReference<List<Map<String, Object>>>() {})
                .getBody();
    }
}
