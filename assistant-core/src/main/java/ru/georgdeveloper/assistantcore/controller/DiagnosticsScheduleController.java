package ru.georgdeveloper.assistantcore.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.georgdeveloper.assistantcore.model.*;
import ru.georgdeveloper.assistantcore.repository.*;
import ru.georgdeveloper.assistantcore.service.DiagnosticsScheduleService;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/diagnostics-schedule")
public class DiagnosticsScheduleController {

    @Autowired
    private DiagnosticsScheduleService scheduleService;

    @Autowired
    private DiagnosticsScheduleRepository scheduleRepository;

    @Autowired
    private DiagnosticsScheduleEntryRepository entryRepository;

    @Autowired
    private DiagnosticsTypeRepository typeRepository;

    /**
     * Получить все графики
     */
    @GetMapping
    public ResponseEntity<List<DiagnosticsSchedule>> getAllSchedules() {
        return ResponseEntity.ok(scheduleRepository.findAll());
    }

    /**
     * Получить график по году
     */
    @GetMapping("/year/{year}")
    public ResponseEntity<DiagnosticsSchedule> getScheduleByYear(@PathVariable Integer year) {
        return scheduleRepository.findByYear(year)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Получить график на месяц
     */
    @GetMapping("/{scheduleId}/month/{month}")
    public ResponseEntity<Map<String, Object>> getMonthSchedule(
            @PathVariable Long scheduleId,
            @PathVariable int month) {
        try {
            Map<String, Object> result = scheduleService.getMonthSchedule(scheduleId, month);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    /**
     * Создать график на год
     */
    @PostMapping("/create")
    public ResponseEntity<Map<String, Object>> createSchedule(
            @RequestBody Map<String, Object> request) {
        try {
            Integer year = (Integer) request.get("year");
            Integer workersCount = (Integer) request.get("workersCount");
            Integer shiftDurationHours = request.containsKey("shiftDurationHours") 
                    ? (Integer) request.get("shiftDurationHours") : 7;
            
            // Получаем дату старта (если указана)
            LocalDate startDate = null;
            if (request.containsKey("startDate") && request.get("startDate") != null) {
                String startDateStr = (String) request.get("startDate");
                if (startDateStr != null && !startDateStr.isEmpty()) {
                    startDate = LocalDate.parse(startDateStr);
                }
            }
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> equipmentList = (List<Map<String, Object>>) request.get("equipmentList");
            
            List<DiagnosticsScheduleService.EquipmentDiagnosticsRequest> requests = equipmentList.stream()
                    .map(eq -> {
                        DiagnosticsScheduleService.EquipmentDiagnosticsRequest req = 
                                new DiagnosticsScheduleService.EquipmentDiagnosticsRequest();
                        req.setEquipment((String) eq.get("equipment"));
                        req.setArea((String) eq.get("area"));
                        
                        @SuppressWarnings("unchecked")
                        Map<String, Object> countsMap = (Map<String, Object>) eq.get("diagnosticsCounts");
                        // Преобразуем в Map<String, Number> для поддержки дробных значений периода
                        Map<String, Number> counts = new HashMap<>();
                        for (Map.Entry<String, Object> countEntry : countsMap.entrySet()) {
                            Object value = countEntry.getValue();
                            if (value instanceof Number) {
                                counts.put(countEntry.getKey(), (Number) value);
                            } else if (value instanceof String) {
                                try {
                                    counts.put(countEntry.getKey(), Double.parseDouble((String) value));
                                } catch (NumberFormatException e) {
                                    throw new RuntimeException("Неверный формат периода для типа '" + countEntry.getKey() + "'");
                                }
                            }
                        }
                        req.setDiagnosticsCounts(counts);
                        
                        @SuppressWarnings("unchecked")
                        Map<String, Integer> durations = (Map<String, Integer>) eq.get("diagnosticsDurations");
                        if (durations != null && !durations.isEmpty()) {
                            req.setDiagnosticsDurations(durations);
                        }
                        return req;
                    })
                    .collect(Collectors.toList());

            DiagnosticsSchedule schedule = scheduleService.createYearlySchedule(
                    year, workersCount, shiftDurationHours, requests, startDate);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("schedule", schedule);
            response.put("message", "График успешно создан");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Ошибка при создании графика: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Обновить график на месяц
     */
    @PutMapping("/{scheduleId}/month/{month}")
    public ResponseEntity<Map<String, Object>> updateMonthSchedule(
            @PathVariable Long scheduleId,
            @PathVariable int month,
            @RequestBody List<Map<String, Object>> entriesData) {
        try {
            List<DiagnosticsScheduleEntry> entries = entriesData.stream()
                    .map(data -> {
                        DiagnosticsScheduleEntry entry = new DiagnosticsScheduleEntry();
                        entry.setId(data.containsKey("id") && data.get("id") != null 
                                ? Long.valueOf(data.get("id").toString()) : null);
                        entry.setEquipment((String) data.get("equipment"));
                        entry.setArea((String) data.get("area"));
                        
                        Long typeId = Long.valueOf(data.get("diagnosticsTypeId").toString());
                        DiagnosticsType type = typeRepository.findById(typeId)
                                .orElseThrow(() -> new RuntimeException("Тип диагностики не найден"));
                        entry.setDiagnosticsType(type);
                        
                        entry.setScheduledDate(java.time.LocalDate.parse((String) data.get("scheduledDate")));
                        entry.setIsCompleted(data.containsKey("isCompleted") 
                                ? (Boolean) data.get("isCompleted") : false);
                        entry.setNotes((String) data.get("notes"));
                        
                        return entry;
                    })
                    .collect(Collectors.toList());

            scheduleService.updateMonthSchedule(scheduleId, month, entries);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "График успешно обновлен");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Ошибка при обновлении графика: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Получить все типы диагностики
     */
    @GetMapping("/types")
    public ResponseEntity<List<DiagnosticsType>> getDiagnosticsTypes() {
        return ResponseEntity.ok(typeRepository.findByIsActiveTrue());
    }

    /**
     * Получить статистику по графику
     * @param scheduleId ID графика
     * @param month месяц (опционально, если не указан - за весь год)
     */
    @GetMapping("/{scheduleId}/stats")
    public ResponseEntity<Map<String, Object>> getScheduleStats(
            @PathVariable Long scheduleId,
            @RequestParam(required = false) Integer month) {
        try {
            // Проверяем существование графика
            DiagnosticsSchedule schedule = scheduleRepository.findById(scheduleId)
                    .orElseThrow(() -> new RuntimeException("График не найден"));

            List<DiagnosticsScheduleEntry> allEntries;
            if (month != null && month >= 1 && month <= 12) {
                // Статистика за конкретный месяц
                LocalDate startDate = LocalDate.of(schedule.getYear(), month, 1);
                LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());
                allEntries = entryRepository.findByScheduleIdAndScheduledDateBetween(scheduleId, startDate, endDate);
            } else {
                // Статистика за весь год
                allEntries = entryRepository.findByScheduleId(scheduleId);
            }
            
            long totalPlanned = allEntries.size();
            long totalCompleted = allEntries.stream()
                    .filter(DiagnosticsScheduleEntry::getIsCompleted)
                    .count();
            
            Map<String, Object> stats = new HashMap<>();
            stats.put("totalPlanned", totalPlanned);
            stats.put("totalCompleted", totalCompleted);
            stats.put("completionPercentage", totalPlanned > 0 
                    ? (totalCompleted * 100.0 / totalPlanned) : 0);
            
            // Статистика по типам
            Map<String, Map<String, Object>> byType = new HashMap<>();
            for (DiagnosticsScheduleEntry entry : allEntries) {
                String typeCode = entry.getDiagnosticsType().getCode();
                Map<String, Object> typeStats = byType.computeIfAbsent(typeCode, k -> {
                    Map<String, Object> s = new HashMap<>();
                    s.put("planned", 0L);
                    s.put("completed", 0L);
                    return s;
                });
                typeStats.put("planned", ((Long) typeStats.get("planned")) + 1);
                if (entry.getIsCompleted()) {
                    typeStats.put("completed", ((Long) typeStats.get("completed")) + 1);
                }
            }
            
            for (Map<String, Object> typeStats : byType.values()) {
                long planned = (Long) typeStats.get("planned");
                long completed = (Long) typeStats.get("completed");
                typeStats.put("completionPercentage", planned > 0 
                        ? (completed * 100.0 / planned) : 0);
            }
            
            stats.put("byType", byType);

            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Удалить график
     */
    @DeleteMapping("/{scheduleId}")
    public ResponseEntity<Map<String, Object>> deleteSchedule(@PathVariable Long scheduleId) {
        try {
            DiagnosticsSchedule schedule = scheduleRepository.findById(scheduleId)
                    .orElseThrow(() -> new RuntimeException("График не найден"));

            // Удаляем график (записи удалятся каскадно благодаря CASCADE в БД)
            scheduleRepository.delete(schedule);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "График успешно удален");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Ошибка при удалении графика: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Добавить оборудование в существующий график
     */
    @PostMapping("/{scheduleId}/add-equipment")
    public ResponseEntity<Map<String, Object>> addEquipmentToSchedule(
            @PathVariable Long scheduleId,
            @RequestBody Map<String, Object> request) {
        try {
            // Получаем дату старта (если указана)
            LocalDate startDate = null;
            if (request.containsKey("startDate") && request.get("startDate") != null) {
                String startDateStr = (String) request.get("startDate");
                if (startDateStr != null && !startDateStr.isEmpty()) {
                    startDate = LocalDate.parse(startDateStr);
                }
            }
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> equipmentList = (List<Map<String, Object>>) request.get("equipmentList");
            
            List<DiagnosticsScheduleService.EquipmentDiagnosticsRequest> requests = equipmentList.stream()
                    .map(eq -> {
                        DiagnosticsScheduleService.EquipmentDiagnosticsRequest req = 
                                new DiagnosticsScheduleService.EquipmentDiagnosticsRequest();
                        req.setEquipment((String) eq.get("equipment"));
                        req.setArea((String) eq.get("area"));
                        
                        @SuppressWarnings("unchecked")
                        Map<String, Object> countsMap = (Map<String, Object>) eq.get("diagnosticsCounts");
                        // Преобразуем в Map<String, Number> для поддержки дробных значений периода
                        Map<String, Number> counts = new HashMap<>();
                        for (Map.Entry<String, Object> countEntry : countsMap.entrySet()) {
                            Object value = countEntry.getValue();
                            if (value instanceof Number) {
                                counts.put(countEntry.getKey(), (Number) value);
                            } else if (value instanceof String) {
                                try {
                                    counts.put(countEntry.getKey(), Double.parseDouble((String) value));
                                } catch (NumberFormatException e) {
                                    throw new RuntimeException("Неверный формат периода для типа '" + countEntry.getKey() + "'");
                                }
                            }
                        }
                        req.setDiagnosticsCounts(counts);
                        
                        @SuppressWarnings("unchecked")
                        Map<String, Integer> durations = (Map<String, Integer>) eq.get("diagnosticsDurations");
                        if (durations != null && !durations.isEmpty()) {
                            req.setDiagnosticsDurations(durations);
                        }
                        return req;
                    })
                    .collect(Collectors.toList());

            DiagnosticsSchedule schedule = scheduleService.addEquipmentToSchedule(scheduleId, requests, startDate);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("schedule", schedule);
            response.put("message", "Оборудование успешно добавлено в график");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Ошибка при добавлении оборудования: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Обновить дату диагностической записи
     */
    @PutMapping("/entry/{entryId}/date")
    public ResponseEntity<Map<String, Object>> updateEntryDate(
            @PathVariable Long entryId,
            @RequestBody Map<String, Object> request) {
        try {
            String newDateStr = (String) request.get("newDate");
            if (newDateStr == null || newDateStr.isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Не указана новая дата");
                return ResponseEntity.badRequest().body(response);
            }
            
            LocalDate newDate = LocalDate.parse(newDateStr);
            DiagnosticsScheduleService.UpdateDateResult result = scheduleService.updateEntryDate(entryId, newDate);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", result.isSuccess());
            response.put("message", result.getMessage());
            
            if (result.isSuccess() && result.getEntry() != null) {
                // Создаем Map с данными записи, избегая проблем с сериализацией Hibernate прокси
                DiagnosticsScheduleEntry entry = result.getEntry();
                Map<String, Object> entryData = new HashMap<>();
                entryData.put("id", entry.getId());
                entryData.put("equipment", entry.getEquipment());
                entryData.put("area", entry.getArea());
                entryData.put("scheduledDate", entry.getScheduledDate().toString());
                entryData.put("isCompleted", entry.getIsCompleted());
                entryData.put("completedDate", entry.getCompletedDate() != null ? entry.getCompletedDate().toString() : null);
                entryData.put("notes", entry.getNotes());
                
                if (entry.getDiagnosticsType() != null) {
                    Map<String, Object> typeData = new HashMap<>();
                    typeData.put("id", entry.getDiagnosticsType().getId());
                    typeData.put("code", entry.getDiagnosticsType().getCode());
                    typeData.put("name", entry.getDiagnosticsType().getName());
                    typeData.put("durationMinutes", entry.getDiagnosticsType().getDurationMinutes());
                    typeData.put("colorCode", entry.getDiagnosticsType().getColorCode());
                    entryData.put("diagnosticsType", typeData);
                }
                
                response.put("entry", entryData);
            }
            
            // Возвращаем 200 OK даже при неудаче, так как это нормальная ситуация (недостаток времени)
            // Клиент обработает success: false
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Ошибка при обновлении даты: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
}

