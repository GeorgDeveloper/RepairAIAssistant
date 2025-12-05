package ru.georgdeveloper.assistantweb.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/diagnostics-schedule")
public class DiagnosticsScheduleWebController {

    @Value("${core.service.url:http://localhost:8080}")
    private String coreServiceUrl;

    private final RestTemplate restTemplate;

    public DiagnosticsScheduleWebController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAllSchedules() {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> schedules = (List<Map<String, Object>>) (List<?>) 
                restTemplate.getForObject(coreServiceUrl + "/api/diagnostics-schedule", List.class);
        return ResponseEntity.ok(schedules);
    }

    @GetMapping("/year/{year}")
    public ResponseEntity<Map<String, Object>> getScheduleByYear(@PathVariable Integer year) {
        Map<String, Object> schedule = restTemplate.getForObject(
                coreServiceUrl + "/api/diagnostics-schedule/year/" + year, Map.class);
        return schedule != null ? ResponseEntity.ok(schedule) : ResponseEntity.notFound().build();
    }

    @GetMapping("/{scheduleId}/month/{month}")
    public ResponseEntity<Map<String, Object>> getMonthSchedule(
            @PathVariable Long scheduleId,
            @PathVariable int month) {
        try {
            Map<String, Object> result = restTemplate.getForObject(
                    coreServiceUrl + "/api/diagnostics-schedule/" + scheduleId + "/month/" + month, 
                    Map.class);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    @PostMapping("/create")
    public ResponseEntity<Map<String, Object>> createSchedule(@RequestBody Map<String, Object> request) {
        try {
            Map<String, Object> response = restTemplate.postForObject(
                    coreServiceUrl + "/api/diagnostics-schedule/create",
                    request,
                    Map.class);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Ошибка при создании графика: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    @PutMapping("/{scheduleId}/month/{month}")
    public ResponseEntity<Map<String, Object>> updateMonthSchedule(
            @PathVariable Long scheduleId,
            @PathVariable int month,
            @RequestBody List<Map<String, Object>> entriesData) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<List<Map<String, Object>>> requestEntity = new HttpEntity<>(entriesData, headers);

            ResponseEntity<Map> responseEntity = restTemplate.exchange(
                    coreServiceUrl + "/api/diagnostics-schedule/" + scheduleId + "/month/" + month,
                    HttpMethod.PUT,
                    requestEntity,
                    Map.class);

            @SuppressWarnings("unchecked")
            Map<String, Object> response = (Map<String, Object>) responseEntity.getBody();
            return ResponseEntity.status(responseEntity.getStatusCode()).body(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Ошибка при обновлении графика: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    @GetMapping("/types")
    public ResponseEntity<List<Map<String, Object>>> getDiagnosticsTypes() {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> types = (List<Map<String, Object>>) (List<?>) 
                restTemplate.getForObject(coreServiceUrl + "/api/diagnostics-schedule/types", List.class);
        return ResponseEntity.ok(types);
    }

    @GetMapping("/{scheduleId}/stats")
    public ResponseEntity<Map<String, Object>> getScheduleStats(
            @PathVariable Long scheduleId,
            @RequestParam(required = false) Integer month) {
        try {
            String url = coreServiceUrl + "/api/diagnostics-schedule/" + scheduleId + "/stats";
            if (month != null && month >= 1 && month <= 12) {
                url += "?month=" + month;
            }
            Map<String, Object> stats = restTemplate.getForObject(url, Map.class);
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    @DeleteMapping("/{scheduleId}")
    public ResponseEntity<Map<String, Object>> deleteSchedule(@PathVariable Long scheduleId) {
        try {
            HttpHeaders headers = new HttpHeaders();
            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

            ResponseEntity<Map> responseEntity = restTemplate.exchange(
                    coreServiceUrl + "/api/diagnostics-schedule/" + scheduleId,
                    HttpMethod.DELETE,
                    requestEntity,
                    Map.class);

            @SuppressWarnings("unchecked")
            Map<String, Object> response = (Map<String, Object>) responseEntity.getBody();
            return ResponseEntity.status(responseEntity.getStatusCode()).body(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Ошибка при удалении графика: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    @PostMapping("/{scheduleId}/add-equipment")
    public ResponseEntity<Map<String, Object>> addEquipmentToSchedule(
            @PathVariable Long scheduleId,
            @RequestBody Map<String, Object> request) {
        try {
            Map<String, Object> response = restTemplate.postForObject(
                    coreServiceUrl + "/api/diagnostics-schedule/" + scheduleId + "/add-equipment",
                    request,
                    Map.class);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Ошибка при добавлении оборудования: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    @PutMapping("/entry/{entryId}/date")
    public ResponseEntity<Map<String, Object>> updateEntryDate(
            @PathVariable Long entryId,
            @RequestBody Map<String, Object> request) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(request, headers);

            ResponseEntity<Map> responseEntity = restTemplate.exchange(
                    coreServiceUrl + "/api/diagnostics-schedule/entry/" + entryId + "/date",
                    HttpMethod.PUT,
                    requestEntity,
                    Map.class);

            @SuppressWarnings("unchecked")
            Map<String, Object> response = (Map<String, Object>) responseEntity.getBody();
            return ResponseEntity.status(responseEntity.getStatusCode()).body(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Ошибка при обновлении даты: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    @PutMapping("/entry/{entryId}/status")
    public ResponseEntity<Map<String, Object>> updateEntryStatus(
            @PathVariable Long entryId,
            @RequestBody Map<String, Object> request) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(request, headers);

            ResponseEntity<Map> responseEntity = restTemplate.exchange(
                    coreServiceUrl + "/api/diagnostics-schedule/entry/" + entryId + "/status",
                    HttpMethod.PUT,
                    requestEntity,
                    Map.class);

            @SuppressWarnings("unchecked")
            Map<String, Object> response = (Map<String, Object>) responseEntity.getBody();
            return ResponseEntity.status(responseEntity.getStatusCode()).body(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Ошибка при обновлении статуса: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }
}

