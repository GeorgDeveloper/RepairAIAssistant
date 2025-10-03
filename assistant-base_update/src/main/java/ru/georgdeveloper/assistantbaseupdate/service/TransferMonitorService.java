package ru.georgdeveloper.assistantbaseupdate.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.ZoneId;

@Service
public class TransferMonitorService {

    private static final Logger logger = LoggerFactory.getLogger(TransferMonitorService.class);

    private final TransferRunTracker tracker;

    @Value("${transfer.enabled:true}")
    private boolean transferEnabled;

    public TransferMonitorService(TransferRunTracker tracker) {
        this.tracker = tracker;
    }

    // Проверяем ежедневно в 08:10, что запуск состоялся. Зона та же, что и у расписания
    @Scheduled(cron = "0 10 8 * * *", zone = "${app.timezone:Europe/Moscow}")
    public void verifyMorningTransfers() {
        if (!transferEnabled) {
            return;
        }

        // Data
        if (!tracker.didRunToday("data")) {
            String msg = String.format(
                "Перенос данных не запустился к 08:10. TZ=%s. Последний запланированный день=%s, последний фактический=%s",
                ZoneId.systemDefault(), tracker.getLastPlannedRunDate("data"), tracker.getLastActualRunDate("data"));
            System.out.println(msg);
            logger.warn(msg);
        }

        // Tag
        if (!tracker.didRunToday("tag")) {
            String msg = String.format(
                "Перенос Tag данных не запустился к 08:10. TZ=%s. Последний запланированный день=%s, последний фактический=%s",
                ZoneId.systemDefault(), tracker.getLastPlannedRunDate("tag"), tracker.getLastActualRunDate("tag"));
            System.out.println(msg);
            logger.warn(msg);
        }
    }
}


