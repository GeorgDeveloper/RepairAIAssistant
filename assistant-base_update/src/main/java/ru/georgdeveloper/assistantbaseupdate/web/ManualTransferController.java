package ru.georgdeveloper.assistantbaseupdate.web;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Autowired;
import ru.georgdeveloper.assistantbaseupdate.service.DataTransferService;
import ru.georgdeveloper.assistantbaseupdate.service.TagTransferService;
import ru.georgdeveloper.assistantbaseupdate.service.PreventiveMaintenanceTransferService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/transfer")
public class ManualTransferController {

    @Autowired
    private DataTransferService dataTransferService;

    @Autowired
    private TagTransferService tagTransferService;

    @Autowired
    private PreventiveMaintenanceTransferService preventiveMaintenanceTransferService;

    @PostMapping("/bd")
    public ResponseEntity<String> runBdTransfer(
            @RequestParam(value = "start", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam(value = "end", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {

        ZoneId zone = ZoneId.of("Europe/Moscow");
        if (start == null || end == null) {
            LocalDate today = LocalDate.now(zone);
            start = today.minusDays(1).atTime(8, 0);
            end = today.atTime(8, 0);
        }

        dataTransferService.runTransfer(start, end);
        return ResponseEntity.ok("BD transfer started for interval [" + start + " .. " + end + "]");
    }

    /**
     * Ручной догруз BD за несколько производственных суток подряд.
     * Каждый день — интервал [fromDay 08:00, fromDay+1 08:00) в Europe/Moscow (как ежедневный cron {@code transferDataDaily}).
     *
     * @param from первый день (включительно), граница интервала начинается в 08:00 этого дня
     * @param to   последний день (включительно), для него выполняется перенос до 08:00 следующего дня
     */
    @PostMapping("/bd/backfill")
    public ResponseEntity<String> runBdBackfill(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        ZoneId zone = ZoneId.of("Europe/Moscow");
        if (to.isBefore(from)) {
            return ResponseEntity.badRequest().body("Parameter \"to\" must be >= \"from\"");
        }

        List<String> intervals = new ArrayList<>();
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            LocalDateTime start = LocalDateTime.of(d, LocalTime.of(8, 0));
            LocalDateTime end = d.plusDays(1).atTime(8, 0);
            dataTransferService.runTransfer(start, end);
            intervals.add("[" + start + " .. " + end + "] (" + zone + ")");
        }

        return ResponseEntity.ok(
                "BD backfill completed: " + intervals.size() + " production day(s) from " + from + " to " + to + ":\n"
                        + String.join("\n", intervals));
    }

    @PostMapping("/tag")
    public ResponseEntity<String> runTagTransfer(
            @RequestParam(value = "start", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam(value = "end", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {

        ZoneId zone = ZoneId.of("Europe/Moscow");
        if (start == null || end == null) {
            LocalDate today = LocalDate.now(zone);
            start = today.minusDays(1).atTime(8, 0);
            end = today.atTime(8, 0);
        }

        tagTransferService.runTagTransfer(start, end);
        return ResponseEntity.ok("Tag transfer started for interval [" + start + " .. " + end + "]");
    }

    /**
     * Ручная догруз Tag за несколько производственных суток (те же окна 08:00–08:00, Europe/Moscow).
     */
    @PostMapping("/tag/backfill")
    public ResponseEntity<String> runTagBackfill(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        ZoneId zone = ZoneId.of("Europe/Moscow");
        if (to.isBefore(from)) {
            return ResponseEntity.badRequest().body("Parameter \"to\" must be >= \"from\"");
        }

        List<String> intervals = new ArrayList<>();
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            LocalDateTime start = LocalDateTime.of(d, LocalTime.of(8, 0));
            LocalDateTime end = d.plusDays(1).atTime(8, 0);
            tagTransferService.runTagTransfer(start, end);
            intervals.add("[" + start + " .. " + end + "] (" + zone + ")");
        }

        return ResponseEntity.ok(
                "Tag backfill completed: " + intervals.size() + " production day(s) from " + from + " to " + to + ":\n"
                        + String.join("\n", intervals));
    }

    @PostMapping("/pm")
    public ResponseEntity<String> runPreventiveMaintenanceTransfer() {
        preventiveMaintenanceTransferService.runTransfer();
        return ResponseEntity.ok("Preventive maintenance transfer started");
    }
}


