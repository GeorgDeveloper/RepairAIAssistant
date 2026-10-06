package ru.georgdeveloper.assistantcore.energy;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import ru.georgdeveloper.assistantcore.model.EnergyAirHourlyValue;
import ru.georgdeveloper.assistantcore.repository.EnergyAirHourlyValueRepository;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Импорт почасовых показателей воздуха из файла с листами СПГ742 / СПГ742_2
 * (колонки «Время», «P1 МПа», «V1 м³»).
 */
@Service
public class EnergyAirExcelImportService {

    private static final DateTimeFormatter DT =
            DateTimeFormatter.ofPattern("d.M.yyyy H:mm", Locale.ROOT);
    private static final DateTimeFormatter DT_SEC =
            DateTimeFormatter.ofPattern("d.M.yyyy H:mm:ss", Locale.ROOT);
    private static final DateTimeFormatter DT_SLASH =
            DateTimeFormatter.ofPattern("d/M/yyyy H:mm", Locale.ROOT);
    private static final Pattern CORPUS_PATTERN =
            Pattern.compile("кр\\.?\\s*(\\d+)", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern SHEET_742 = Pattern.compile(".*(сп[гт]|sp[gt]).*742.*", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    private final EnergyAirHourlyValueRepository repository;

    public EnergyAirExcelImportService(EnergyAirHourlyValueRepository repository) {
        this.repository = repository;
    }

    public record ImportSummary(
            int rowsScanned, int rowsAccepted, int valuesWritten, List<String> warnings) {}

    @Transactional
    public ImportSummary importWorkbook(MultipartFile file) throws IOException {
        Objects.requireNonNull(file, "file");
        String sourceName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "upload.xlsx";
        List<String> warnings = new ArrayList<>();
        int rowsScanned = 0;
        int rowsAccepted = 0;
        List<EnergyAirHourlyValue> batch = new ArrayList<>();

        try (InputStream in = file.getInputStream(); Workbook workbook = new XSSFWorkbook(in)) {
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
            DataFormatter formatter = new DataFormatter(Locale.ROOT);

            List<String> allSheetNames = new ArrayList<>();
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                allSheetNames.add(workbook.getSheetName(i));
            }
            warnings.add("Листы в файле: " + String.join(", ", allSheetNames));

            List<Sheet> sheets = findAirSheets(workbook);
            if (sheets.isEmpty()) {
                warnings.add("Не найдены листы СПГ742 / СПГ742_2 (или СПТ742). Импорт воздуха не выполнен.");
                return new ImportSummary(0, 0, 0, warnings);
            }

            for (Sheet sheet : sheets) {
                String sheetName = sheet.getSheetName();
                SheetImport part = importSheet(sheet, sheetName, sourceName, evaluator, formatter, warnings);
                rowsScanned += part.rowsScanned;
                rowsAccepted += part.rowsAccepted;
                batch.addAll(part.rows);
                warnings.add(sheetName + ": принято строк " + part.rowsAccepted + ", записано уникальных " + part.rows.size());
            }
        }

        if (batch.isEmpty()) {
            warnings.add("Воздух: нет строк с распознанным временем — проверьте колонку «Время».");
            return new ImportSummary(rowsScanned, rowsAccepted, 0, warnings);
        }

        LocalDateTime min = null;
        LocalDateTime max = null;
        Set<String> buildings = new HashSet<>();
        for (EnergyAirHourlyValue row : batch) {
            buildings.add(row.getBuildingCode());
            if (min == null || row.getFactTime().isBefore(min)) {
                min = row.getFactTime();
            }
            if (max == null || row.getFactTime().isAfter(max)) {
                max = row.getFactTime();
            }
        }
        for (String building : buildings) {
            repository.deleteByFactTimeBetweenAndBuildingCode(
                    Objects.requireNonNull(min), Objects.requireNonNull(max), building);
        }
        repository.flush();
        repository.saveAll(batch);
        warnings.add("Период данных: " + min + " … " + max);
        return new ImportSummary(rowsScanned, rowsAccepted, batch.size(), warnings);
    }

    private static List<Sheet> findAirSheets(Workbook workbook) {
        List<Sheet> exact = new ArrayList<>();
        List<Sheet> fuzzy = new ArrayList<>();
        for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
            Sheet sheet = workbook.getSheetAt(i);
            String name = sheet.getSheetName() != null ? sheet.getSheetName().trim() : "";
            String compact = name.replace(" ", "");
            if (compact.equalsIgnoreCase("СПГ742")
                    || compact.equalsIgnoreCase("СПГ742_2")
                    || compact.equalsIgnoreCase("СПТ742")
                    || compact.equalsIgnoreCase("СПТ742_2")) {
                exact.add(sheet);
            } else if (SHEET_742.matcher(compact).matches() || compact.toUpperCase(Locale.ROOT).contains("742")) {
                // только если похоже на приборный архив
                String upper = compact.toUpperCase(Locale.ROOT);
                if (upper.contains("СПГ") || upper.contains("СПТ") || upper.contains("SPG") || upper.contains("SPT")) {
                    fuzzy.add(sheet);
                }
            }
        }
        return exact.isEmpty() ? fuzzy : exact;
    }

    private record SheetImport(int rowsScanned, int rowsAccepted, List<EnergyAirHourlyValue> rows) {}

    private record ColumnLayout(int timeCol, int pressureCol, int volumeCol, int headerRow) {}

    private SheetImport importSheet(
            Sheet sheet,
            String sheetName,
            String sourceName,
            FormulaEvaluator evaluator,
            DataFormatter formatter,
            List<String> warnings) {
        ColumnLayout layout = detectColumns(sheet, formatter);
        if (layout == null) {
            warnings.add(sheetName + ": не найдены колонки «Время» / «P1» / «V1». "
                    + describeHeaderProbe(sheet, formatter));
            return new SheetImport(0, 0, List.of());
        }
        warnings.add(sheetName + ": колонки время=" + colLetter(layout.timeCol())
                + ", P1=" + colLetter(layout.pressureCol())
                + ", V1=" + colLetter(layout.volumeCol())
                + " (строка заголовка " + (layout.headerRow() + 1) + ")");

        String buildingCode = sheetName.trim();
        String buildingLabel = detectBuildingLabel(sheet, formatter, sheetName);

        List<EnergyAirHourlyValue> rows = new ArrayList<>();
        Set<LocalDateTime> seen = new HashSet<>();
        int scanned = 0;
        int accepted = 0;
        String sampleUnparsed = null;
        for (int r = layout.headerRow() + 1; r <= sheet.getLastRowNum(); r++) {
            Row row = sheet.getRow(r);
            if (row == null) {
                continue;
            }
            scanned++;
            Cell timeCell = row.getCell(layout.timeCol());
            LocalDateTime factTime = parseDateTime(timeCell, formatter);
            if (factTime == null) {
                if (sampleUnparsed == null) {
                    sampleUnparsed = cellText(timeCell, formatter);
                }
                continue;
            }
            accepted++;
            if (!seen.add(factTime)) {
                continue;
            }
            BigDecimal pressure = readNumeric(row.getCell(layout.pressureCol()), evaluator, formatter);
            BigDecimal volume = readNumeric(row.getCell(layout.volumeCol()), evaluator, formatter);
            rows.add(new EnergyAirHourlyValue(
                    factTime, buildingCode, buildingLabel, pressure, volume, sourceName));
        }
        if (accepted == 0 && sampleUnparsed != null && !sampleUnparsed.isBlank()) {
            warnings.add(sheetName + ": пример нераспознанного времени: «" + sampleUnparsed + "»");
        }
        return new SheetImport(scanned, accepted, rows);
    }

    private static String describeHeaderProbe(Sheet sheet, DataFormatter formatter) {
        int maxProbe = Math.min(sheet.getLastRowNum(), 10);
        StringBuilder sb = new StringBuilder("Заголовки: ");
        for (int r = 0; r <= maxProbe; r++) {
            Row row = sheet.getRow(r);
            if (row == null) {
                continue;
            }
            List<String> cells = new ArrayList<>();
            short last = row.getLastCellNum();
            for (int c = 0; c < Math.min(last, 12); c++) {
                String t = cellText(row.getCell(c), formatter);
                if (!t.isEmpty()) {
                    cells.add(colLetter(c) + "=" + t);
                }
            }
            if (!cells.isEmpty()) {
                sb.append("[row").append(r + 1).append(": ").append(String.join("; ", cells)).append("] ");
            }
        }
        return sb.toString();
    }

    private static ColumnLayout detectColumns(Sheet sheet, DataFormatter formatter) {
        int maxProbe = Math.min(sheet.getLastRowNum(), 20);
        for (int r = 0; r <= maxProbe; r++) {
            Row row = sheet.getRow(r);
            if (row == null) {
                continue;
            }
            int timeCol = -1;
            int pressureCol = -1;
            int volumeCol = -1;
            int last = Math.max(row.getLastCellNum(), 0);
            for (int c = 0; c < last; c++) {
                String text = normalizeHeader(cellText(row.getCell(c), formatter));
                if (text.isEmpty()) {
                    continue;
                }
                // отдельные if — одна ячейка = одна роль, без else-if цепочки
                if (timeCol < 0 && isTimeHeader(text)) {
                    timeCol = c;
                }
                if (pressureCol < 0 && isPressureHeader(text)) {
                    pressureCol = c;
                }
                if (volumeCol < 0 && isVolumeHeader(text)) {
                    volumeCol = c;
                }
            }
            if (timeCol >= 0 && pressureCol >= 0 && volumeCol >= 0) {
                return new ColumnLayout(timeCol, pressureCol, volumeCol, r);
            }
            // Известная раскладка архива СПТ742: Интервал | Время | … | P1 | t1 | V1
            if (timeCol >= 0 && looksLikeSpt742HeaderRow(row, formatter)) {
                int p = indexOfHeader(row, formatter, EnergyAirExcelImportService::isPressureHeader);
                int v = indexOfHeader(row, formatter, EnergyAirExcelImportService::isVolumeHeader);
                if (p < 0 || v < 0) {
                    // B=Время(1), E=P1(4), G=V1(6) — как в файле прибора
                    p = 4;
                    v = 6;
                }
                return new ColumnLayout(timeCol, p, v, r);
            }
        }
        return detectColumnsFallback(sheet, formatter);
    }

    private static boolean looksLikeSpt742HeaderRow(Row row, DataFormatter formatter) {
        boolean hasInterval = false;
        boolean hasTime = false;
        short last = row.getLastCellNum();
        for (int c = 0; c < last; c++) {
            String text = normalizeHeader(cellText(row.getCell(c), formatter));
            if (text.contains("интервал")) {
                hasInterval = true;
            }
            if (isTimeHeader(text)) {
                hasTime = true;
            }
        }
        return hasInterval && hasTime;
    }

    private static int indexOfHeader(Row row, DataFormatter formatter, java.util.function.Predicate<String> match) {
        short last = row.getLastCellNum();
        for (int c = 0; c < last; c++) {
            String text = normalizeHeader(cellText(row.getCell(c), formatter));
            if (match.test(text)) {
                return c;
            }
        }
        return -1;
    }

    private static ColumnLayout detectColumnsFallback(Sheet sheet, DataFormatter formatter) {
        int maxProbe = Math.min(sheet.getLastRowNum(), 20);
        for (int r = 0; r <= maxProbe; r++) {
            Row row = sheet.getRow(r);
            if (row == null) {
                continue;
            }
            if (!looksLikeSpt742HeaderRow(row, formatter)) {
                continue;
            }
            int timeCol = indexOfHeader(row, formatter, EnergyAirExcelImportService::isTimeHeader);
            if (timeCol < 0) {
                timeCol = 1;
            }
            int pressureCol = indexOfHeader(row, formatter, EnergyAirExcelImportService::isPressureHeader);
            int volumeCol = indexOfHeader(row, formatter, EnergyAirExcelImportService::isVolumeHeader);
            if (pressureCol < 0) {
                pressureCol = 4;
            }
            if (volumeCol < 0) {
                volumeCol = 6;
            }
            return new ColumnLayout(timeCol, pressureCol, volumeCol, r);
        }
        return null;
    }

    private static boolean isTimeHeader(String text) {
        return text.contains("время") || text.equals("time");
    }

    private static boolean isPressureHeader(String text) {
        // p1мпа / p1 / р1мпа после нормализации омоглифов
        return text.startsWith("p1") || text.contains("p1мпа") || text.contains("p1mpa");
    }

    private static boolean isVolumeHeader(String text) {
        return text.startsWith("v1") || text.startsWith("в1")
                || text.contains("v1м3") || text.contains("v1m3") || text.contains("в1м3");
    }

    /** Латиница/кириллица-омоглифы в заголовках прибора. */
    private static String normalizeHeader(String raw) {
        if (raw == null) {
            return "";
        }
        // Убираем пробелы/переносы заранее
        String cleaned = raw
                .replace('\u00A0', ' ')
                .replace('\u202F', ' ')
                .replace("\r", "")
                .replace("\n", "")
                .replace("\t", "")
                .replace(" ", "");
        StringBuilder sb = new StringBuilder(cleaned.length());
        for (int i = 0; i < cleaned.length(); i++) {
            char ch = cleaned.charAt(i);
            char next = i + 1 < cleaned.length() ? cleaned.charAt(i + 1) : '\0';
            // Только «Р1»/«р1» (и греческая Ρ1) → p1; НЕ трогаем «р» в «Время»/«Интервал»
            if ((ch == 'р' || ch == 'Р' || ch == 'ρ' || ch == 'Ρ') && Character.isDigit(next)) {
                ch = 'p';
            }
            sb.append(Character.toLowerCase(ch));
        }
        String t = sb.toString();
        t = t.replace("м³", "м3")
                .replace("мᶾ", "м3")
                .replace("m³", "m3")
                .replace("^3", "3");
        t = t.replaceAll("[^a-zа-яё0-9]", "");
        return t;
    }

    private static String detectBuildingLabel(Sheet sheet, DataFormatter formatter, String sheetName) {
        int maxProbe = Math.min(sheet.getLastRowNum(), 5);
        for (int r = 0; r <= maxProbe; r++) {
            Row row = sheet.getRow(r);
            if (row == null) {
                continue;
            }
            short last = row.getLastCellNum();
            for (int c = 0; c < last; c++) {
                String text = cellText(row.getCell(c), formatter);
                Matcher m = CORPUS_PATTERN.matcher(text);
                if (m.find()) {
                    return "Корпус " + m.group(1);
                }
            }
        }
        if (sheetName.contains("_2")) {
            return "Корпус 16";
        }
        return "Корпус 12";
    }

    private static LocalDateTime parseDateTime(Cell cell, DataFormatter formatter) {
        if (cell == null || cell.getCellType() == CellType.BLANK) {
            return null;
        }
        CellType type = cell.getCellType();
        if (type == CellType.FORMULA) {
            type = cell.getCachedFormulaResultType();
        }
        if (type == CellType.NUMERIC) {
            double n = cell.getNumericCellValue();
            if (DateUtil.isCellDateFormatted(cell) || DateUtil.isValidExcelDate(n)) {
                try {
                    return DateUtil.getLocalDateTime(n);
                } catch (Exception ignored) {
                    return null;
                }
            }
            return null;
        }
        String raw = cellText(cell, formatter)
                .replace('\u00A0', ' ')
                .replace('\u202F', ' ')
                .trim();
        if (raw.isEmpty()) {
            return null;
        }
        // "19.01.2026 12:00" / "19.01.2026 12:00:00"
        String normalized = raw.replace(',', '.');
        for (DateTimeFormatter fmt : List.of(DT, DT_SEC, DT_SLASH)) {
            try {
                return LocalDateTime.parse(normalized, fmt);
            } catch (DateTimeParseException ignored) {
                // next
            }
        }
        // "19.01.2026 12.00" (точка вместо двоеточия во времени)
        String alt = normalized.replaceAll("(\\d{1,2})\\.(\\d{2})$", "$1:$2");
        try {
            return LocalDateTime.parse(alt, DT);
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    private static BigDecimal readNumeric(Cell cell, FormulaEvaluator evaluator, DataFormatter formatter) {
        if (cell == null || cell.getCellType() == CellType.BLANK) {
            return null;
        }
        CellType type = cell.getCellType();
        if (type == CellType.FORMULA) {
            CellValue evaluated = evaluator.evaluate(cell);
            if (evaluated == null) {
                return null;
            }
            return switch (evaluated.getCellType()) {
                case NUMERIC -> BigDecimal.valueOf(evaluated.getNumberValue());
                case STRING -> parseBigDecimalString(evaluated.getStringValue());
                default -> null;
            };
        }
        if (type == CellType.NUMERIC) {
            return BigDecimal.valueOf(cell.getNumericCellValue());
        }
        return parseBigDecimalString(cellText(cell, formatter));
    }

    private static BigDecimal parseBigDecimalString(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        if (t.isEmpty() || t.equals("-") || t.startsWith("#")) {
            return null;
        }
        try {
            return new BigDecimal(t.replace(" ", "").replace(',', '.'));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static String cellText(Cell cell, DataFormatter formatter) {
        if (cell == null) {
            return "";
        }
        return formatter.formatCellValue(cell).trim();
    }

    private static String colLetter(int zeroBased) {
        int n = zeroBased;
        StringBuilder sb = new StringBuilder();
        do {
            sb.insert(0, (char) ('A' + (n % 26)));
            n = n / 26 - 1;
        } while (n >= 0);
        return sb.toString();
    }
}
