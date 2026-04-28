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
                LocalDate detectionDate = report.getDetectionDate();
                if (detectionDate != null && matchesPeriodFilter(detectionDate, year, month)) {
                    String detectionPeriodKey = toPeriodKey(detectionDate, groupByMonth);
                    Map<String, Object> periodData = dataByPeriod.computeIfAbsent(detectionPeriodKey, k -> createPeriodData(k, groupByMonth));
                    periodData.put("detected", ((Long) periodData.get("detected")) + 1);

                    String status = report.getStatus();
                    if ("ОТКРЫТО".equals(status)) {
                        periodData.put("opened", ((Long) periodData.get("opened")) + 1);
                    } else if ("В РАБОТЕ".equals(status)) {
                        periodData.put("inWork", ((Long) periodData.get("inWork")) + 1);
                    }
                }

                // Закрытые считаем по дате закрытия, а не по дате обнаружения
                String status = report.getStatus();
                LocalDate eliminationDate = report.getEliminationDate();
                if ("ЗАКРЫТО".equals(status) && eliminationDate != null && matchesPeriodFilter(eliminationDate, year, month)) {
                    String closePeriodKey = toPeriodKey(eliminationDate, groupByMonth);
                    Map<String, Object> closedPeriodData = dataByPeriod.computeIfAbsent(closePeriodKey, k -> createPeriodData(k, groupByMonth));
                    closedPeriodData.put("closed", ((Long) closedPeriodData.get("closed")) + 1);
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

    private boolean matchesPeriodFilter(LocalDate date, Integer year, Integer month) {
        if (date == null) {
            return false;
        }
        if (year != null && date.getYear() != year) {
            return false;
        }
        if (month != null && month >= 1 && month <= 12 && date.getMonthValue() != month) {
            return false;
        }
        return true;
    }

    private String toPeriodKey(LocalDate date, boolean groupByMonth) {
        if (groupByMonth) {
            return String.format("%04d-%02d", date.getYear(), date.getMonthValue());
        }
        return date.toString();
    }

    private Map<String, Object> createPeriodData(String periodKey, boolean groupByMonth) {
        Map<String, Object> d = new HashMap<>();
        d.put("date", periodKey);
        d.put("groupByMonth", groupByMonth);
        d.put("detected", 0L);
        d.put("opened", 0L);
        d.put("inWork", 0L);
        d.put("closed", 0L);
        return d;
    }
}

