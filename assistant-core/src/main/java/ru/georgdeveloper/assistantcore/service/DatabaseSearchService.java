package ru.georgdeveloper.assistantcore.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.georgdeveloper.assistantcore.model.EquipmentMaintenanceRecord;
import ru.georgdeveloper.assistantcore.model.SummaryOfSolutions;
import ru.georgdeveloper.assistantcore.model.BreakdownReport;
import ru.georgdeveloper.assistantcore.repository.EquipmentMaintenanceRepository;
import ru.georgdeveloper.assistantcore.repository.SummaryOfSolutionsRepository;
import ru.georgdeveloper.assistantcore.repository.BreakdownReportRepository;

import java.util.*;
import java.util.stream.Collectors;
import ru.georgdeveloper.assistantcore.nlp.TfidfKeywordExtractorService;

@Service
public class DatabaseSearchService {

    @Autowired
    private TfidfKeywordExtractorService tfidfKeywordExtractorService;
    @Autowired(required = false)
    private EquipmentMaintenanceRepository equipmentRepo;
    @Autowired(required = false)
    private SummaryOfSolutionsRepository summaryRepo;
    @Autowired(required = false)
    private BreakdownReportRepository breakdownRepo;

    /**
     * Универсальный поиск по всем ключевым таблицам на основе динамических ключевых слов из запроса пользователя
     */
    public SearchResult searchAll(String userQuery, int limit) {
        // 1. Извлекаем ключевые слова и фразы через TF-IDF
        List<String> keywords = tfidfKeywordExtractorService.extractKeywords(userQuery, 7);
        List<SummaryOfSolutions> summaryResults = new ArrayList<>();
        List<EquipmentMaintenanceRecord> equipmentResults = new ArrayList<>();
        List<BreakdownReport> breakdownResults = new ArrayList<>();

        // 2. Поиск по всем трем таблицам параллельно
        if (summaryRepo != null && !keywords.isEmpty()) {
            for (String keyword : keywords) {
                List<SummaryOfSolutions> found = summaryRepo.searchByKeyword(keyword);
                if (found != null) summaryResults.addAll(found);
            }
            // Убираем дубли и ограничиваем лимитом
            summaryResults = summaryResults.stream().distinct().limit(limit).collect(Collectors.toList());
        }
        if (equipmentRepo != null && !keywords.isEmpty()) {
            for (String keyword : keywords) {
                List<EquipmentMaintenanceRecord> found = equipmentRepo.findByKeyword(keyword, org.springframework.data.domain.PageRequest.of(0, limit));
                if (found != null) equipmentResults.addAll(found);
            }
            equipmentResults = equipmentResults.stream().distinct().limit(limit).collect(Collectors.toList());
        }
        if (breakdownRepo != null && !keywords.isEmpty()) {
            for (String keyword : keywords) {
                List<BreakdownReport> found = breakdownRepo.findByKeyword(keyword, org.springframework.data.domain.PageRequest.of(0, limit));
                if (found != null) breakdownResults.addAll(found);
            }
            breakdownResults = breakdownResults.stream().distinct().limit(limit).collect(Collectors.toList());
        }
        // 3. Группировка и возврат результатов
        return new SearchResult(summaryResults, equipmentResults, breakdownResults);
    }


    /**
     * Извлекает ключевые слова из пользовательского запроса (минимальная длина 3 символа)
     */
    public List<String> extractKeywords(String query) {
        if (query == null) return Collections.emptyList();
        return Arrays.stream(query.toLowerCase().split("\\W+"))
                .filter(s -> s.length() > 2)
                .distinct()
                .collect(Collectors.toList());
    }

    public static class SearchResult {
        public final List<SummaryOfSolutions> summary;
        public final List<EquipmentMaintenanceRecord> equipment;
        public final List<BreakdownReport> breakdowns;
        public SearchResult(List<SummaryOfSolutions> summary, List<EquipmentMaintenanceRecord> equipment, List<BreakdownReport> breakdowns) {
            this.summary = summary;
            this.equipment = equipment;
            this.breakdowns = breakdowns;
        }
    }
}
