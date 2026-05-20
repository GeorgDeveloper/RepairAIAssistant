package ru.georgdeveloper.assistantbaseupdate.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.georgdeveloper.assistantbaseupdate.service.CmmsReconciliationService;
import ru.georgdeveloper.assistantbaseupdate.service.CmmsReconciliationService.RecordKind;
import ru.georgdeveloper.assistantbaseupdate.service.CmmsReconciliationService.ReconciliationResult;

import java.time.Year;

@RestController
@RequestMapping("/api/reconciliation")
public class CmmsReconciliationController {

    @Autowired
    private CmmsReconciliationService reconciliationService;

    /**
     * Наряды BD/HP из CMMS с начала года, которых нет в {@code equipment_maintenance_records}
     * (сопоставление: code + Date_T1 + Date_T4, как при переносе).
     */
    @GetMapping(value = "/bd/missing.csv", produces = "text/csv; charset=UTF-8")
    public ResponseEntity<byte[]> bdMissingCsv(
            @RequestParam(value = "year", required = false) Integer year) {
        return csvResponse(RecordKind.BD, year, "bd-missing-in-mysql");
    }

    /**
     * Наряды Tag из CMMS с начала года, которых нет в {@code tag_maintenance_records}.
     */
    @GetMapping(value = "/tag/missing.csv", produces = "text/csv; charset=UTF-8")
    public ResponseEntity<byte[]> tagMissingCsv(
            @RequestParam(value = "year", required = false) Integer year) {
        return csvResponse(RecordKind.TAG, year, "tag-missing-in-mysql");
    }

    private ResponseEntity<byte[]> csvResponse(RecordKind kind, Integer year, String filePrefix) {
        int y = year != null ? year : Year.now().getValue();
        if (y < 2000 || y > 2100) {
            return ResponseEntity.badRequest().build();
        }

        ReconciliationResult result = reconciliationService.buildMissingCsv(kind, y);
        String filename = filePrefix + "-" + y + ".csv";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .header("X-Cmms-Total", String.valueOf(result.cmmsTotal()))
                .header("X-Mysql-Keys-Loaded", String.valueOf(result.mysqlKeysLoaded()))
                .header("X-Missing-Count", String.valueOf(result.missingCount()))
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(result.csvBytes());
    }
}
