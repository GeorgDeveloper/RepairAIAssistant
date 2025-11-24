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
import java.time.ZoneId;

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

    @PostMapping("/pm")
    public ResponseEntity<String> runPreventiveMaintenanceTransfer() {
        preventiveMaintenanceTransferService.runTransfer();
        return ResponseEntity.ok("Preventive maintenance transfer started");
    }
}


