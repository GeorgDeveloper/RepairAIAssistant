package ru.georgdeveloper.assistantcore.service;

import org.springframework.stereotype.Service;
import ru.georgdeveloper.assistantcore.repository.FinalRepository;

import java.util.List;
import java.util.Map;

@Service
public class FinalService {

    private final FinalRepository repository;

    public FinalService(FinalRepository repository) {
        this.repository = repository;
    }

    public List<Map<String, Object>> getSummaries(List<Integer> years, List<Integer> months, Integer limit) {
        return repository.getSummaries(years, months, limit);
    }

    public List<Map<String, Object>> getYears() {
        return repository.getYears();
    }

    public List<Map<String, Object>> getMonths(List<Integer> years) {
        return repository.getMonths(years);
    }
}


