package ru.georgdeveloper.assistantcore.controller;

import org.springframework.dao.DataAccessException;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import ru.georgdeveloper.assistantcore.energy.EnergySteamHourlyExcelImportService;
import ru.georgdeveloper.assistantcore.energy.EnergySteamHourlyExcelImportService.ImportSummary;
import ru.georgdeveloper.assistantcore.model.EnergySteamHourlyValue;
import ru.georgdeveloper.assistantcore.repository.EnergySteamHourlyValueRepository;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/energy/steam-hourly")
public class EnergySteamHourlyController {

    private final EnergySteamHourlyExcelImportService importService;
    private final EnergySteamHourlyValueRepository repository;

    public EnergySteamHourlyController(
            EnergySteamHourlyExcelImportService importService,
            EnergySteamHourlyValueRepository repository) {
        this.importService = importService;
        this.repository = repository;
    }

    @GetMapping("/hourly-values")
    public ResponseEntity<?> listHourlyValues(
            @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        if (to.isBefore(from)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "to before from");
        }
        try {
            List<EnergySteamHourlyValue> rows =
                    repository.findByFactTimeBetweenOrderByFactTimeAscConsumerCodeAsc(
                            from.atStartOfDay(), to.atTime(LocalTime.of(23, 59, 59)));
            return ResponseEntity.ok(rows);
        } catch (DataAccessException ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Ошибка БД: " + rootMessage(ex)));
        }
    }

    @GetMapping("/range")
    public ResponseEntity<Map<String, Object>> dataRange() {
        Object[] minMax = repository.findMinMaxFactTime();
        Map<String, Object> body = new LinkedHashMap<>();
        if (minMax == null || minMax.length < 2 || minMax[0] == null || minMax[1] == null) {
            body.put("from", null);
            body.put("to", null);
            body.put("empty", true);
            return ResponseEntity.ok(body);
        }
        body.put("from", ((LocalDateTime) minMax[0]).toLocalDate().toString());
        body.put("to", ((LocalDateTime) minMax[1]).toLocalDate().toString());
        body.put("empty", false);
        return ResponseEntity.ok(body);
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> importExcel(@RequestParam("file") MultipartFile file)
            throws IOException {
        if (file == null || file.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Файл не передан"));
        }
        String name = file.getOriginalFilename();
        if (name == null || !name.toLowerCase().endsWith(".xlsx")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Ожидается файл .xlsx"));
        }
        try {
            ImportSummary summary = importService.importWorkbook(file);
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("rowsScanned", summary.rowsScanned());
            body.put("rowsAccepted", summary.rowsAccepted());
            body.put("valuesWritten", summary.valuesWritten());
            body.put("warnings", summary.warnings());
            return ResponseEntity.ok(body);
        } catch (DataAccessException ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Ошибка БД при импорте пара: " + rootMessage(ex)));
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Ошибка импорта пара: " + rootMessage(ex)));
        }
    }

    private static String rootMessage(Throwable ex) {
        Throwable cur = ex;
        while (cur.getCause() != null && cur.getCause() != cur) {
            cur = cur.getCause();
        }
        String msg = cur.getMessage();
        return msg != null && !msg.isBlank() ? msg : ex.getClass().getSimpleName();
    }
}
