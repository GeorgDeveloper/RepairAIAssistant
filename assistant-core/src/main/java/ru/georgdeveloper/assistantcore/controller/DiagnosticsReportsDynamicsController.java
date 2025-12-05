package ru.georgdeveloper.assistantcore.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.georgdeveloper.assistantcore.model.DiagnosticsReport;
import ru.georgdeveloper.assistantcore.repository.DiagnosticsReportRepository;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/diagnostics-reports-dynamics")
public class DiagnosticsReportsDynamicsController {

    @Autowired
    private DiagnosticsReportRepository reportRepository;

    /**
     * Получить динамику отчетов по датам/месяцам
     */
    @GetMapping("/data")
    public ResponseEntity<List<Map<String, Object>>> getDynamicsData(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) String area,
            @RequestParam(required = false) String equipment,
            @RequestParam(required = false) String diagnosticsType) {
        
        try {
            // Получаем все отчеты
            List<DiagnosticsReport> allReports = reportRepository.findAll();
            
            // Фильтруем по году
            if (year != null) {
                allReports = allReports.stream()
                        .filter(r -> r.getDetectionDate() != null && r.getDetectionDate().getYear() == year)
                        .collect(Collectors.toList());
            }
            
            // Фильтруем по месяцу
            if (month != null && month >= 1 && month <= 12) {
                allReports = allReports.stream()
                        .filter(r -> r.getDetectionDate() != null && r.getDetectionDate().getMonthValue() == month)
                        .collect(Collectors.toList());
            }
            
            // Фильтруем по участку
            if (area != null && !area.isEmpty()) {
                allReports = allReports.stream()
                        .filter(r -> area.equals(r.getArea()))
                        .collect(Collectors.toList());
            }
            
            // Фильтруем по оборудованию
            if (equipment != null && !equipment.isEmpty()) {
                allReports = allReports.stream()
                        .filter(r -> equipment.equals(r.getEquipment()))
                        .collect(Collectors.toList());
            }
            
            // Фильтруем по типу диагностики
            if (diagnosticsType != null && !diagnosticsType.isEmpty()) {
                final String filterType = diagnosticsType.trim();
                allReports = allReports.stream()
                        .filter(r -> {
                            String reportType = r.getDiagnosticsType();
                            return filterType.equals(reportType);
                        })
                        .collect(Collectors.toList());
            }
            
            // Определяем, группировать по месяцам или по датам
            boolean groupByMonth = (month == null);
            
            // Группируем по датам или месяцам
            Map<String, Map<String, Object>> dataByPeriod = new TreeMap<>();
            
            for (DiagnosticsReport report : allReports) {
                if (report.getDetectionDate() == null) {
                    continue; // Пропускаем отчеты без даты обнаружения
                }
                
                String periodKey;
                if (groupByMonth) {
                    // Группируем по месяцам: YYYY-MM
                    LocalDate reportDate = report.getDetectionDate();
                    periodKey = String.format("%04d-%02d", reportDate.getYear(), reportDate.getMonthValue());
                } else {
                    // Группируем по датам: YYYY-MM-DD
                    periodKey = report.getDetectionDate().toString();
                }
                
                Map<String, Object> periodData = dataByPeriod.computeIfAbsent(periodKey, k -> {
                    Map<String, Object> d = new HashMap<>();
                    d.put("date", periodKey);
                    d.put("groupByMonth", groupByMonth);
                    d.put("detected", 0L);      // Обнаружено дефектов (все отчеты)
                    d.put("opened", 0L);        // Открыто
                    d.put("inWork", 0L);        // В работе
                    d.put("closed", 0L);        // Закрыто
                    return d;
                });
                
                // Увеличиваем счетчик обнаруженных дефектов (все отчеты)
                periodData.put("detected", ((Long) periodData.get("detected")) + 1);
                
                // Увеличиваем счетчик по статусу
                String status = report.getStatus();
                if (status != null) {
                    if ("ОТКРЫТО".equals(status)) {
                        periodData.put("opened", ((Long) periodData.get("opened")) + 1);
                    } else if ("В РАБОТЕ".equals(status)) {
                        periodData.put("inWork", ((Long) periodData.get("inWork")) + 1);
                    } else if ("ЗАКРЫТО".equals(status)) {
                        periodData.put("closed", ((Long) periodData.get("closed")) + 1);
                    }
                }
            }
            
            // Преобразуем в список
            List<Map<String, Object>> result = new ArrayList<>(dataByPeriod.values());
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Collections.emptyList());
        }
    }

    /**
     * Получить общую статистику
     */
    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getSummary(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) String area,
            @RequestParam(required = false) String equipment,
            @RequestParam(required = false) String diagnosticsType) {
        
        try {
            // Получаем все отчеты
            List<DiagnosticsReport> allReports = reportRepository.findAll();
            
            // Применяем те же фильтры, что и в getDynamicsData
            if (year != null) {
                allReports = allReports.stream()
                        .filter(r -> r.getDetectionDate() != null && r.getDetectionDate().getYear() == year)
                        .collect(Collectors.toList());
            }
            
            if (month != null && month >= 1 && month <= 12) {
                allReports = allReports.stream()
                        .filter(r -> r.getDetectionDate() != null && r.getDetectionDate().getMonthValue() == month)
                        .collect(Collectors.toList());
            }
            
            if (area != null && !area.isEmpty()) {
                allReports = allReports.stream()
                        .filter(r -> area.equals(r.getArea()))
                        .collect(Collectors.toList());
            }
            
            if (equipment != null && !equipment.isEmpty()) {
                allReports = allReports.stream()
                        .filter(r -> equipment.equals(r.getEquipment()))
                        .collect(Collectors.toList());
            }
            
            if (diagnosticsType != null && !diagnosticsType.isEmpty()) {
                final String filterType = diagnosticsType.trim();
                allReports = allReports.stream()
                        .filter(r -> filterType.equals(r.getDiagnosticsType()))
                        .collect(Collectors.toList());
            }
            
            long detected = allReports.size();
            long opened = allReports.stream()
                    .filter(r -> "ОТКРЫТО".equals(r.getStatus()))
                    .count();
            long inWork = allReports.stream()
                    .filter(r -> "В РАБОТЕ".equals(r.getStatus()))
                    .count();
            long closed = allReports.stream()
                    .filter(r -> "ЗАКРЫТО".equals(r.getStatus()))
                    .count();
            
            Map<String, Object> summary = new HashMap<>();
            summary.put("detected", detected);
            summary.put("opened", opened);
            summary.put("inWork", inWork);
            summary.put("closed", closed);
            
            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    /**
     * Получить список доступных типов диагностики из отчетов
     */
    @GetMapping("/diagnostics-types")
    public ResponseEntity<List<String>> getDiagnosticsTypes() {
        try {
            List<DiagnosticsReport> allReports = reportRepository.findAll();
            Set<String> types = allReports.stream()
                    .map(DiagnosticsReport::getDiagnosticsType)
                    .filter(type -> type != null && !type.isEmpty())
                    .collect(Collectors.toSet());
            
            List<String> sortedTypes = new ArrayList<>(types);
            Collections.sort(sortedTypes);
            
            return ResponseEntity.ok(sortedTypes);
        } catch (Exception e) {
            return ResponseEntity.ok(Collections.emptyList());
        }
    }
}

