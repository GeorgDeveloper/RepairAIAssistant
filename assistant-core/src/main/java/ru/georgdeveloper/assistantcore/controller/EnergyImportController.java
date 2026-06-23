package ru.georgdeveloper.assistantcore.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ru.georgdeveloper.assistantcore.energy.EnergyExcelImportService;
import ru.georgdeveloper.assistantcore.energy.EnergyExcelImportService.ImportSummary;
import ru.georgdeveloper.assistantcore.energy.EnergyResource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/api/energy")
public class EnergyImportController {

    private final EnergyExcelImportService energyExcelImportService;

    public EnergyImportController(EnergyExcelImportService energyExcelImportService) {
        this.energyExcelImportService = energyExcelImportService;
    }

    /**
     * Импорт .xlsx по YAML-картам колонок. Перед вставкой строки за диапазон дат в файле для данного resource удаляются.
     *
     * @param year календарный год для колонки даты вида dd.MM (если в ячейке только день.месяц)
     * @param resource необязательно: один {@link EnergyResource} или несколько через запятую; если пусто — все виды
     */
    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> importExcel(
            @RequestParam("file") MultipartFile file,
            @RequestParam("year") int year,
            @RequestParam(value = "resource", required = false) String resource)
            throws IOException {
        if (file == null || file.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Файл не передан"));
        }
        String name = file.getOriginalFilename();
        if (name == null || !name.toLowerCase().endsWith(".xlsx")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Ожидается файл .xlsx"));
        }
        if (year < 1990 || year > 2100) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Некорректный year"));
        }
        List<EnergyResource> resources = parseResources(resource);
        if (resources.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Некорректный resource"));
        }
        ImportSummary summary = energyExcelImportService.importWorkbook(file, year, resources);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("rowsScanned", summary.rowsScanned());
        body.put("rowsAccepted", summary.rowsAccepted());
        body.put("valuesWritten", summary.valuesWritten());
        body.put("warnings", summary.warnings());
        return ResponseEntity.ok(body);
    }

    private static List<EnergyResource> parseResources(String resource) {
        if (resource == null || resource.isBlank()) {
            return List.of(EnergyResource.values());
        }
        List<EnergyResource> out = new ArrayList<>();
        for (String part : resource.split(",")) {
            String p = part.trim();
            if (p.isEmpty()) {
                continue;
            }
            try {
                out.add(EnergyResource.valueOf(p.toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException ex) {
                return List.of();
            }
        }
        return out.isEmpty() ? List.of(EnergyResource.values()) : out;
    }
}
