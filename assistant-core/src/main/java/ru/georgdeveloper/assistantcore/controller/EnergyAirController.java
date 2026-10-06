package ru.georgdeveloper.assistantcore.controller;

import org.springframework.dao.DataAccessException;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import ru.georgdeveloper.assistantcore.energy.EnergyAirExcelImportService;
import ru.georgdeveloper.assistantcore.energy.EnergyAirExcelImportService.ImportSummary;
import ru.georgdeveloper.assistantcore.model.EnergyAirHourlyValue;
import ru.georgdeveloper.assistantcore.repository.EnergyAirHourlyValueRepository;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/energy/air")
public class EnergyAirController {

    private final EnergyAirExcelImportService airExcelImportService;
    private final EnergyAirHourlyValueRepository airHourlyValueRepository;

    public EnergyAirController(
            EnergyAirExcelImportService airExcelImportService,
            EnergyAirHourlyValueRepository airHourlyValueRepository) {
        this.airExcelImportService = airExcelImportService;
        this.airHourlyValueRepository = airHourlyValueRepository;
    }

    @GetMapping("/hourly-values")
    public ResponseEntity<?> listHourlyValues(
            @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        if (to.isBefore(from)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "to before from");
        }
        LocalDateTime fromDt = from.atStartOfDay();
        LocalDateTime toDt = to.atTime(LocalTime.of(23, 59, 59));
        try {
            List<EnergyAirHourlyValue> rows =
                    airHourlyValueRepository.findByFactTimeBetweenOrderByFactTimeAscBuildingCodeAsc(fromDt, toDt);
            return ResponseEntity.ok(rows);
        } catch (DataAccessException ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "error",
                            "Ошибка БД при чтении воздуха: " + rootMessage(ex)
                                    + ". Проверьте, что таблица energy_air_hourly_value создана, и перезапустите assistant-core."));
        }
    }

    @GetMapping("/range")
    public ResponseEntity<Map<String, Object>> dataRange() {
        Object[] minMax = airHourlyValueRepository.findMinMaxFactTime();
        Map<String, Object> body = new LinkedHashMap<>();
        if (minMax == null || minMax.length < 2 || minMax[0] == null || minMax[1] == null) {
            body.put("from", null);
            body.put("to", null);
            body.put("empty", true);
            return ResponseEntity.ok(body);
        }
        LocalDateTime min = (LocalDateTime) minMax[0];
        LocalDateTime max = (LocalDateTime) minMax[1];
        body.put("from", min.toLocalDate().toString());
        body.put("to", max.toLocalDate().toString());
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
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Ожидается файл .xlsx"));
        }
        try {
            ImportSummary summary = airExcelImportService.importWorkbook(file);
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("rowsScanned", summary.rowsScanned());
            body.put("rowsAccepted", summary.rowsAccepted());
            body.put("valuesWritten", summary.valuesWritten());
            body.put("warnings", summary.warnings());
            return ResponseEntity.ok(body);
        } catch (DataAccessException ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "error",
                            "Ошибка БД при импорте воздуха: " + rootMessage(ex)
                                    + ". Проверьте таблицу energy_air_hourly_value и перезапустите assistant-core."));
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Ошибка импорта воздуха: " + rootMessage(ex)));
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
