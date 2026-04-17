package ru.georgdeveloper.assistantcore.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.georgdeveloper.assistantcore.model.DiagnosticsScheduleEntry;
import ru.georgdeveloper.assistantcore.repository.DiagnosticsScheduleEntryRepository;
import ru.georgdeveloper.assistantcore.repository.DiagnosticsScheduleRepository;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/diagnostics-dynamics")
public class DiagnosticsDynamicsController {

    @Autowired
    private DiagnosticsScheduleEntryRepository entryRepository;

    @Autowired
    private DiagnosticsScheduleRepository scheduleRepository;
    
    @Autowired
    private ru.georgdeveloper.assistantcore.repository.DiagnosticsTypeRepository typeRepository;

    /**
     * Получить динамику диагностики по датам
     */
    @GetMapping("/data")
    public ResponseEntity<List<Map<String, Object>>> getDynamicsData(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) String area,
            @RequestParam(required = false) String equipment,
            @RequestParam(required = false) String diagnosticsType) {
        
        System.out.println("=== getDynamicsData ===");
        System.out.println("year: " + year);
        System.out.println("month: " + month);
        System.out.println("area: " + area);
        System.out.println("equipment: " + equipment);
        System.out.println("diagnosticsType: " + diagnosticsType);
        
        try {
            // Получаем все графики или по году
            List<Long> scheduleIds;
            if (year != null) {
                scheduleIds = scheduleRepository.findByYear(year)
                        .map(s -> List.of(s.getId()))
                        .orElse(Collections.emptyList());
            } else {
                scheduleIds = scheduleRepository.findAll().stream()
                        .map(s -> s.getId())
                        .collect(Collectors.toList());
            }
            
            if (scheduleIds.isEmpty()) {
                return ResponseEntity.ok(Collections.emptyList());
            }
            
            // Получаем все записи
            List<DiagnosticsScheduleEntry> allEntries = new ArrayList<>();
            for (Long scheduleId : scheduleIds) {
                List<DiagnosticsScheduleEntry> entries = entryRepository.findByScheduleId(scheduleId);
                allEntries.addAll(entries);
            }
            
            // Фильтруем по параметрам
            if (month != null && month >= 1 && month <= 12) {
                allEntries = allEntries.stream()
                        .filter(e -> e.getScheduledDate().getMonthValue() == month)
                        .collect(Collectors.toList());
            }
            
            if (area != null && !area.isEmpty()) {
                allEntries = allEntries.stream()
                        .filter(e -> area.equals(e.getArea()))
                        .collect(Collectors.toList());
            }
            
            if (equipment != null && !equipment.isEmpty()) {
                allEntries = allEntries.stream()
                        .filter(e -> equipment.equals(e.getEquipment()))
                        .collect(Collectors.toList());
            }
            
            if (diagnosticsType != null && !diagnosticsType.isEmpty()) {
                final String filterType = diagnosticsType.trim();
                System.out.println("Фильтрация по типу диагностики: '" + filterType + "'");
                System.out.println("Записей до фильтрации: " + allEntries.size());
                
                allEntries = allEntries.stream()
                        .filter(e -> {
                            String entryCode = e.getDiagnosticsType() != null ? e.getDiagnosticsType().getCode() : null;
                            boolean matches = filterType.equals(entryCode);
                            if (matches) {
                                System.out.println("Найдено совпадение: " + entryCode);
                            }
                            return matches;
                        })
                        .collect(Collectors.toList());
                
                System.out.println("Записей после фильтрации: " + allEntries.size());
            } else {
                System.out.println("Фильтр по типу диагностики не применен (null или empty)");
            }
            
            // Определяем, группировать по месяцам или по датам
            boolean groupByMonth = (month == null);
            
            // Группируем по датам или месяцам
            Map<String, Map<String, Object>> dataByPeriod = new TreeMap<>();
            
            for (DiagnosticsScheduleEntry entry : allEntries) {
                String periodKey;
                if (groupByMonth) {
                    // Группируем по месяцам: YYYY-MM
                    LocalDate entryDate = entry.getScheduledDate();
                    periodKey = String.format("%04d-%02d", entryDate.getYear(), entryDate.getMonthValue());
                } else {
                    // Группируем по датам: YYYY-MM-DD
                    periodKey = entry.getScheduledDate().toString();
                }
                
                Map<String, Object> periodData = dataByPeriod.computeIfAbsent(periodKey, k -> {
                    Map<String, Object> d = new HashMap<>();
                    d.put("date", periodKey);
                    d.put("groupByMonth", groupByMonth);
                    d.put("planned", 0L);
                    d.put("completed", 0L);
                    return d;
                });
                
                periodData.put("planned", ((Long) periodData.get("planned")) + 1);
                if (entry.getIsCompleted() != null && entry.getIsCompleted()) {
                    periodData.put("completed", ((Long) periodData.get("completed")) + 1);
                }
            }
            
            // Вычисляем процент выполнения для каждого периода
            List<Map<String, Object>> result = new ArrayList<>();
            for (Map<String, Object> periodData : dataByPeriod.values()) {
                long planned = (Long) periodData.get("planned");
                long completed = (Long) periodData.get("completed");
                double percentage = planned > 0 ? (completed * 100.0 / planned) : 0;
                periodData.put("percentage", percentage);
                result.add(periodData);
            }
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Collections.emptyList());
        }
    }

    /**
     * Получить общую статистику (для отображения в шапке)
     */
    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getSummary(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) String area,
            @RequestParam(required = false) String equipment,
            @RequestParam(required = false) String diagnosticsType) {
        
        System.out.println("=== getDynamicsData ===");
        System.out.println("year: " + year);
        System.out.println("month: " + month);
        System.out.println("area: " + area);
        System.out.println("equipment: " + equipment);
        System.out.println("diagnosticsType: " + diagnosticsType);
        
        try {
            // Получаем все графики или по году
            List<Long> scheduleIds;
            if (year != null) {
                scheduleIds = scheduleRepository.findByYear(year)
                        .map(s -> List.of(s.getId()))
                        .orElse(Collections.emptyList());
            } else {
                scheduleIds = scheduleRepository.findAll().stream()
                        .map(s -> s.getId())
                        .collect(Collectors.toList());
            }
            
            if (scheduleIds.isEmpty()) {
                Map<String, Object> empty = new HashMap<>();
                empty.put("planned", 0L);
                empty.put("completed", 0L);
                empty.put("percentage", 0.0);
                return ResponseEntity.ok(empty);
            }
            
            // Получаем все записи
            List<DiagnosticsScheduleEntry> allEntries = new ArrayList<>();
            for (Long scheduleId : scheduleIds) {
                List<DiagnosticsScheduleEntry> entries = entryRepository.findByScheduleId(scheduleId);
                allEntries.addAll(entries);
            }
            
            // Фильтруем по параметрам
            if (month != null && month >= 1 && month <= 12) {
                allEntries = allEntries.stream()
                        .filter(e -> e.getScheduledDate().getMonthValue() == month)
                        .collect(Collectors.toList());
            }
            
            if (area != null && !area.isEmpty()) {
                allEntries = allEntries.stream()
                        .filter(e -> area.equals(e.getArea()))
                        .collect(Collectors.toList());
            }
            
            if (equipment != null && !equipment.isEmpty()) {
                allEntries = allEntries.stream()
                        .filter(e -> equipment.equals(e.getEquipment()))
                        .collect(Collectors.toList());
            }
            
            if (diagnosticsType != null && !diagnosticsType.isEmpty()) {
                final String filterType = diagnosticsType.trim();
                allEntries = allEntries.stream()
                        .filter(e -> {
                            String entryCode = e.getDiagnosticsType() != null ? e.getDiagnosticsType().getCode() : null;
                            return filterType.equals(entryCode);
                        })
                        .collect(Collectors.toList());
            }
            
            long totalPlanned = allEntries.size();
            long totalCompleted = allEntries.stream()
                    .filter(e -> e.getIsCompleted() != null && e.getIsCompleted())
                    .count();
            
            double percentage = totalPlanned > 0 ? (totalCompleted * 100.0 / totalPlanned) : 0;
            
            Map<String, Object> summary = new HashMap<>();
            summary.put("planned", totalPlanned);
            summary.put("completed", totalCompleted);
            summary.put("percentage", percentage);
            
            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("planned", 0L);
            error.put("completed", 0L);
            error.put("percentage", 0.0);
            return ResponseEntity.status(500).body(error);
        }
    }

    /**
     * Получить список доступных типов диагностики
     */
    @GetMapping("/diagnostics-types")
    public ResponseEntity<List<Map<String, Object>>> getDiagnosticsTypes() {
        try {
            // Получаем все активные типы диагностики из репозитория
            List<ru.georgdeveloper.assistantcore.model.DiagnosticsType> allTypes = typeRepository.findByIsActiveTrue();
            
            List<Map<String, Object>> types = new ArrayList<>();
            for (ru.georgdeveloper.assistantcore.model.DiagnosticsType type : allTypes) {
                Map<String, Object> typeMap = new HashMap<>();
                typeMap.put("code", type.getCode());
                typeMap.put("name", type.getName());
                types.add(typeMap);
            }
            
            return ResponseEntity.ok(types);
        } catch (Exception e) {
            // Fallback: получаем из записей графика
            try {
                List<DiagnosticsScheduleEntry> allEntries = entryRepository.findAll();
                Set<String> typeCodes = allEntries.stream()
                        .filter(entry -> entry.getDiagnosticsType() != null)
                        .map(entry -> entry.getDiagnosticsType().getCode())
                        .collect(Collectors.toSet());
                
                List<Map<String, Object>> types = new ArrayList<>();
                for (String code : typeCodes) {
                    DiagnosticsScheduleEntry firstEntry = allEntries.stream()
                            .filter(entry -> entry.getDiagnosticsType() != null && code.equals(entry.getDiagnosticsType().getCode()))
                            .findFirst()
                            .orElse(null);
                    
                    if (firstEntry != null) {
                        Map<String, Object> type = new HashMap<>();
                        type.put("code", code);
                        type.put("name", firstEntry.getDiagnosticsType().getName());
                        types.add(type);
                    }
                }
                
                return ResponseEntity.ok(types);
            } catch (Exception e2) {
                return ResponseEntity.ok(Collections.emptyList());
            }
        }
    }
}

