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
@RequestMapping("/top-causes")
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
        return getList(url.toString());
    }

    @GetMapping("/weeks")
    public List<Map<String, Object>> weeks() {
        return getList(coreServiceUrl + "/top-causes/weeks");
    }

    @GetMapping("/areas")
    public List<Map<String, Object>> areas() {
        return getList(coreServiceUrl + "/top-causes/areas");
    }

    @GetMapping("/failure-types")
    public List<Map<String, Object>> failureTypes() {
        return getList(coreServiceUrl + "/top-causes/failure-types");
    }

    @GetMapping("/machines")
    public List<Map<String, Object>> machines(@RequestParam(required = false) String area) {
        String url = coreServiceUrl + "/top-causes/machines" + (area != null ? ("?area=" + area) : "");
        return getList(url);
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
        return getList(url.toString());
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
        return getList(url.toString());
    }

    @GetMapping("/drilldown/events")
    public List<Map<String, Object>> drillEvents(@RequestParam String cause,
                                                 @RequestParam String machine,
                                                 @RequestParam String mechanism,
                                                 @RequestParam(required = false) String dateFrom,
                                                 @RequestParam(required = false) String dateTo,
                                                 @RequestParam(required = false) String week,
                                                 @RequestParam(required = false) String area) {
        StringBuilder url = new StringBuilder(coreServiceUrl + "/top-causes/drilldown/events?cause=" + cause + "&machine=" + machine + "&mechanism=" + mechanism);
        if (dateFrom != null) url.append("&dateFrom=").append(dateFrom);
        if (dateTo != null) url.append("&dateTo=").append(dateTo);
        if (week != null) url.append("&week=").append(week);
        if (area != null) url.append("&area=").append(area);
        return getList(url.toString());
    }

    private List<Map<String, Object>> getList(String url) {
        return restTemplate
                .exchange(url, HttpMethod.GET, null,
                        new ParameterizedTypeReference<List<Map<String, Object>>>() {})
                .getBody();
    }
}


