package ru.georgdeveloper.assistantcore.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ru.georgdeveloper.assistantcore.repository.FinalRepository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class FinalService {

    private static final Logger log = LoggerFactory.getLogger(FinalService.class);

    private final FinalRepository repository;
    private final DiagnosticsScheduleService diagnosticsScheduleService;

    public FinalService(FinalRepository repository, DiagnosticsScheduleService diagnosticsScheduleService) {
        this.repository = repository;
        this.diagnosticsScheduleService = diagnosticsScheduleService;
    }

    public List<Map<String, Object>> getSummaries(List<Integer> years, List<Integer> months, Integer limit) {
        List<Map<String, Object>> rows = repository.getSummaries(years, months, limit);
        List<Map<String, Object>> orderedYearMonths = repository.getOrderedYearMonths(years, months, limit);
        if (orderedYearMonths.isEmpty()) return rows;

        List<Double> completionPercentages = diagnosticsScheduleService.getCompletionPercentagesForOrderedYearMonths(orderedYearMonths);
        log.debug("FinalService: orderedYearMonths={}, diagnostics %={}", orderedYearMonths, completionPercentages);
        Map<String, Object> diagnosticsRow = new LinkedHashMap<>();
        diagnosticsRow.put("metric", "Диагностика %");
        for (int i = 0; i < completionPercentages.size(); i++) {
            diagnosticsRow.put("m" + (i + 1), completionPercentages.get(i));
        }
        // Вставляем строку «Диагностика %» после «Факт выполнения ппр, %»
        int insertIndex = -1;
        for (int i = 0; i < rows.size(); i++) {
            if ("Факт выполнения ппр, %".equals(rows.get(i).get("metric"))) {
                insertIndex = i + 1;
                break;
            }
        }
        if (insertIndex < 0) insertIndex = rows.size();
        rows.add(insertIndex, diagnosticsRow);
        return rows;
    }

    public List<Map<String, Object>> getYears() {
        return repository.getYears();
    }

    public List<Map<String, Object>> getMonths(List<Integer> years) {
        return repository.getMonths(years);
    }
}


