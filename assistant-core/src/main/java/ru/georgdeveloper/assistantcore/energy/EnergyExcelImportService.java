package ru.georgdeveloper.assistantcore.energy;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import ru.georgdeveloper.assistantcore.model.EnergyDailyValue;
import ru.georgdeveloper.assistantcore.repository.EnergyDailyValueRepository;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
public class EnergyExcelImportService {

    private static final DateTimeFormatter DAY_MONTH_YEAR = DateTimeFormatter.ofPattern("d.M.yyyy", Locale.ROOT);

    private final EnergyColumnMapService columnMapService;
    private final EnergyDailyValueRepository energyDailyValueRepository;

    public EnergyExcelImportService(
            EnergyColumnMapService columnMapService,
            EnergyDailyValueRepository energyDailyValueRepository) {
        this.columnMapService = columnMapService;
        this.energyDailyValueRepository = energyDailyValueRepository;
    }

    public record ImportSummary(
            int rowsScanned,
            int rowsAccepted,
            int valuesWritten,
            List<String> warnings) {}

    @Transactional
    public ImportSummary importWorkbook(MultipartFile file, int year, List<EnergyResource> resources)
            throws IOException {
        Objects.requireNonNull(file, "file");
        if (resources == null || resources.isEmpty()) {
            resources = List.of(EnergyResource.values());
        }
        String sourceName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "upload.xlsx";
        int rowsScanned = 0;
        int rowsAccepted = 0;
        int valuesWritten = 0;
        List<String> warnings = new ArrayList<>();

        try (InputStream in = file.getInputStream(); Workbook workbook = new XSSFWorkbook(in)) {
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
            for (EnergyResource resource : resources) {
                ImportSummary part =
                        importResource(workbook, evaluator, resource, year, sourceName, warnings);
                rowsScanned += part.rowsScanned();
                rowsAccepted += part.rowsAccepted();
                valuesWritten += part.valuesWritten();
            }
        }
        return new ImportSummary(rowsScanned, rowsAccepted, valuesWritten, warnings);
    }

    private ImportSummary importResource(
            Workbook workbook,
            FormulaEvaluator evaluator,
            EnergyResource resource,
            int year,
            String sourceName,
            List<String> warnings) {
        Map<String, Object> map = columnMapService.getColumnMap(resource);
        if (map.isEmpty()) {
            warnings.add("Нет YAML-карты для " + resource.name());
            return new ImportSummary(0, 0, 0, warnings);
        }
        String sheetName = stringVal(map.get("sheet"));
        if (sheetName == null) {
            warnings.add(resource.name() + ": не указан sheet в YAML");
            return new ImportSummary(0, 0, 0, warnings);
        }
        Sheet sheet = workbook.getSheet(sheetName);
        if (sheet == null) {
            warnings.add(resource.name() + ": лист «" + sheetName + "» не найден в файле");
            return new ImportSummary(0, 0, 0, warnings);
        }
        int firstRow0 = excelRowToZeroBased(intVal(map.get("data_first_excel_row"), 3));
        @SuppressWarnings("unchecked")
        Map<String, Object> dateCol = (Map<String, Object>) map.get("date_column");
        int dateColIndex = intVal(dateCol != null ? dateCol.get("zero_based_index") : null, 0);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> metrics = (List<Map<String, Object>>) map.get("metrics");
        if (metrics == null || metrics.isEmpty()) {
            warnings.add(resource.name() + ": пустой список metrics в YAML");
            return new ImportSummary(0, 0, 0, warnings);
        }

        List<EnergyDailyValue> batch = new ArrayList<>();
        Set<String> seenKeys = new HashSet<>();
        LocalDate minDate = null;
        LocalDate maxDate = null;
        int rowsScanned = 0;
        int rowsAccepted = 0;

        for (int r = firstRow0; r <= sheet.getLastRowNum(); r++) {
            Row row = sheet.getRow(r);
            if (row == null) {
                continue;
            }
            rowsScanned++;
            Cell dateCell = row.getCell(dateColIndex, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
            LocalDate factDate = parseFactDate(dateCell, year);
            if (factDate == null) {
                continue;
            }
            rowsAccepted++;
            if (minDate == null || factDate.isBefore(minDate)) {
                minDate = factDate;
            }
            if (maxDate == null || factDate.isAfter(maxDate)) {
                maxDate = factDate;
            }
            for (Map<String, Object> metricDef : metrics) {
                String metricId = stringVal(metricDef.get("id"));
                if (metricId == null) {
                    continue;
                }
                int col = intVal(metricDef.get("zero_based_index"), -1);
                if (col < 0) {
                    continue;
                }
                Cell cell = row.getCell(col, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                BigDecimal num = readNumeric(cell, evaluator);
                String dedupKey = factDate + "|" + metricId;
                if (!seenKeys.add(dedupKey)) {
                    // В исходном файле иногда встречаются дубли строк по дате; второй раз не пишем.
                    continue;
                }
                batch.add(new EnergyDailyValue(factDate, resource.name(), metricId, num, sourceName));
            }
        }

        if (batch.isEmpty()) {
            warnings.add(resource.name() + ": нет строк с датами для импорта");
            return new ImportSummary(rowsScanned, rowsAccepted, 0, warnings);
        }
        energyDailyValueRepository.deleteByFactDateBetweenAndResourceCode(
                Objects.requireNonNull(minDate), Objects.requireNonNull(maxDate), resource.name());
        // Обеспечиваем порядок SQL: DELETE должен уйти в БД до INSERT, иначе срабатывает unique key.
        energyDailyValueRepository.flush();
        energyDailyValueRepository.saveAll(batch);
        return new ImportSummary(rowsScanned, rowsAccepted, batch.size(), warnings);
    }

    private static int excelRowToZeroBased(int excel1BasedRow) {
        return Math.max(0, excel1BasedRow - 1);
    }

    /**
     * Первая колонка: дата суток в формате dd.MM или число Excel-даты; строки «Итого…» пропускаются.
     */
    private static LocalDate parseFactDate(Cell cell, int year) {
        if (cell == null || cell.getCellType() == CellType.BLANK) {
            return null;
        }
        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            return cell.getLocalDateTimeCellValue().toLocalDate();
        }
        if (cell.getCellType() == CellType.NUMERIC) {
            return DateUtil.getLocalDateTime(cell.getNumericCellValue()).toLocalDate();
        }
        String raw = new DataFormatter().formatCellValue(cell).trim();
        if (raw.isEmpty()) {
            return null;
        }
        if (raw.toLowerCase(Locale.ROOT).contains("итого")) {
            return null;
        }
        if (raw.length() > 40) {
            return null;
        }
        String withYear = raw.contains(".") && raw.chars().filter(ch -> ch == '.').count() == 1
                ? raw + "." + year
                : raw;
        try {
            return LocalDate.parse(withYear, DAY_MONTH_YEAR);
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    private static BigDecimal readNumeric(Cell cell, FormulaEvaluator evaluator) {
        if (cell == null || cell.getCellType() == CellType.BLANK) {
            return null;
        }
        CellType type = cell.getCellType();
        if (type == CellType.FORMULA) {
            CellValue evaluated = evaluator.evaluate(cell);
            if (evaluated == null) {
                return null;
            }
            switch (evaluated.getCellType()) {
                case NUMERIC:
                    return BigDecimal.valueOf(evaluated.getNumberValue());
                case STRING:
                    return parseBigDecimalString(evaluated.getStringValue());
                case BLANK:
                default:
                    return null;
            }
        }
        if (type == CellType.NUMERIC) {
            return BigDecimal.valueOf(cell.getNumericCellValue());
        }
        if (type == CellType.STRING) {
            return parseBigDecimalString(cell.getStringCellValue());
        }
        return null;
    }

    private static BigDecimal parseBigDecimalString(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        if (t.isEmpty()
                || t.equalsIgnoreCase("#DIV/0!")
                || t.equalsIgnoreCase("#N/A")
                || t.equalsIgnoreCase("#VALUE!")
                || t.equalsIgnoreCase("#REF!")) {
            return null;
        }
        try {
            String normalized = t.replace(" ", "").replace(',', '.');
            return new BigDecimal(normalized);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static String stringVal(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    private static int intVal(Object o, int defaultVal) {
        if (o instanceof Number n) {
            return n.intValue();
        }
        if (o instanceof String s) {
            try {
                return Integer.parseInt(s.trim());
            } catch (NumberFormatException e) {
                return defaultVal;
            }
        }
        return defaultVal;
    }
}
