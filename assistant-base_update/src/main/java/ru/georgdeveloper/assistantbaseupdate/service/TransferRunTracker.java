package ru.georgdeveloper.assistantbaseupdate.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class TransferRunTracker {

    private static final Logger logger = LoggerFactory.getLogger(TransferRunTracker.class);

    private final ConcurrentMap<String, LocalDate> lastPlannedRunDateByType = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, LocalDate> lastActualRunDateByType = new ConcurrentHashMap<>();

    public void markPlannedRun(String type) {
        lastPlannedRunDateByType.put(type, LocalDate.now());
    }

    public void markActualRun(String type) {
        lastActualRunDateByType.put(type, LocalDate.now());
    }

    public boolean didRunToday(String type) {
        LocalDate last = lastActualRunDateByType.get(type);
        return last != null && last.equals(LocalDate.now());
    }

    public LocalDate getLastPlannedRunDate(String type) {
        return lastPlannedRunDateByType.get(type);
    }

    public LocalDate getLastActualRunDate(String type) {
        return lastActualRunDateByType.get(type);
    }
}


