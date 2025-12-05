package ru.georgdeveloper.assistantweb.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/diagnostics-dynamics")
public class DiagnosticsDynamicsWebController {

    @Value("${core.service.url:http://localhost:8080}")
    private String coreServiceUrl;

    private final RestTemplate restTemplate;

    public DiagnosticsDynamicsWebController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @SuppressWarnings("unchecked")
    @GetMapping("/data")
    public ResponseEntity<List<Map<String, Object>>> getDynamicsData(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) String area,
            @RequestParam(required = false) String equipment,
            @RequestParam(required = false) String diagnosticsType) {
        try {
            StringBuilder url = new StringBuilder(coreServiceUrl + "/api/diagnostics-dynamics/data?");
            if (year != null) url.append("year=").append(year).append("&");
            if (month != null) url.append("month=").append(month).append("&");
            if (area != null && !area.isEmpty()) url.append("area=").append(java.net.URLEncoder.encode(area, java.nio.charset.StandardCharsets.UTF_8)).append("&");
            if (equipment != null && !equipment.isEmpty()) url.append("equipment=").append(java.net.URLEncoder.encode(equipment, java.nio.charset.StandardCharsets.UTF_8)).append("&");
            if (diagnosticsType != null && !diagnosticsType.isEmpty()) {
                url.append("diagnosticsType=").append(java.net.URLEncoder.encode(diagnosticsType, java.nio.charset.StandardCharsets.UTF_8)).append("&");
            }
            
            List<Map<String, Object>> data = (List<Map<String, Object>>) (List<?>) 
                    restTemplate.getForObject(url.toString(), List.class);
            return ResponseEntity.ok(data != null ? data : List.of());
        } catch (Exception e) {
            return ResponseEntity.status(500).body(List.of());
        }
    }

    @SuppressWarnings("unchecked")
    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getSummary(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) String area,
            @RequestParam(required = false) String equipment,
            @RequestParam(required = false) String diagnosticsType) {
        try {
            StringBuilder url = new StringBuilder(coreServiceUrl + "/api/diagnostics-dynamics/summary?");
            if (year != null) url.append("year=").append(year).append("&");
            if (month != null) url.append("month=").append(month).append("&");
            if (area != null && !area.isEmpty()) url.append("area=").append(java.net.URLEncoder.encode(area, java.nio.charset.StandardCharsets.UTF_8)).append("&");
            if (equipment != null && !equipment.isEmpty()) url.append("equipment=").append(java.net.URLEncoder.encode(equipment, java.nio.charset.StandardCharsets.UTF_8)).append("&");
            if (diagnosticsType != null && !diagnosticsType.isEmpty()) {
                url.append("diagnosticsType=").append(java.net.URLEncoder.encode(diagnosticsType, java.nio.charset.StandardCharsets.UTF_8)).append("&");
            }
            
            Map<String, Object> summary = restTemplate.getForObject(url.toString(), Map.class);
            return ResponseEntity.ok(summary != null ? summary : new HashMap<>());
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("planned", 0L);
            error.put("completed", 0L);
            error.put("percentage", 0.0);
            return ResponseEntity.status(500).body(error);
        }
    }

    @SuppressWarnings("unchecked")
    @GetMapping("/diagnostics-types")
    public ResponseEntity<List<Map<String, Object>>> getDiagnosticsTypes() {
        try {
            List<Map<String, Object>> types = (List<Map<String, Object>>) (List<?>) 
                    restTemplate.getForObject(coreServiceUrl + "/api/diagnostics-dynamics/diagnostics-types", List.class);
            return ResponseEntity.ok(types != null ? types : List.of());
        } catch (Exception e) {
            return ResponseEntity.ok(List.of());
        }
    }
}

