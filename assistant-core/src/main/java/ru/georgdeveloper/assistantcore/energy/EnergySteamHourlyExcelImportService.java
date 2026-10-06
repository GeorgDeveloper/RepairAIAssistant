package ru.georgdeveloper.assistantcore.energy;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import ru.georgdeveloper.assistantcore.model.EnergySteamHourlyValue;
import ru.georgdeveloper.assistantcore.repository.EnergySteamHourlyValueRepository;

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
 * Импорт почасового архива пара по потребителям.
 * Берём «Время» и для каждого потребителя: расход (Qo/Gв/Gм), T, P, массу (M/Mг/Mм).
 */
@Service
public class EnergySteamHourlyExcelImportService {

    private static final DateTimeFormatter DT =
            DateTimeFormatter.ofPattern("d.M.yyyy H:mm", Locale.ROOT);
    private static final DateTimeFormatter DT_SEC =
            DateTimeFormatter.ofPattern("d.M.yyyy H:mm:ss", Locale.ROOT);
    private static final Pattern CONSUMER_CODE = Pattern.compile("т0*(\\d+)", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern UZ_CODE = Pattern.compile("уз\\.?\\s*№?\\s*([\\w\\-]+)", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    private final EnergySteamHourlyValueRepository repository;

    public EnergySteamHourlyExcelImportService(EnergySteamHourlyValueRepository repository) {
        this.repository = repository;
    }

    public record ImportSummary(
            int rowsScanned, int rowsAccepted, int valuesWritten, List<String> warnings) {}

    private record ConsumerBlock(
            String code, String label, int flowCol, int tempCol, int pressureCol, int massCol) {}

    private record Layout(int timeCol, int headerRow, int metricRow, List<ConsumerBlock> consumers) {}

    @Transactional
    public ImportSummary importWorkbook(MultipartFile file) throws IOException {
        Objects.requireNonNull(file, "file");
        String sourceName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "upload.xlsx";
        List<String> warnings = new ArrayList<>();
        int rowsScanned = 0;
        int rowsAccepted = 0;
        List<EnergySteamHourlyValue> batch = new ArrayList<>();

        try (InputStream in = file.getInputStream(); Workbook workbook = new XSSFWorkbook(in)) {
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
            DataFormatter formatter = new DataFormatter(Locale.ROOT);
            Sheet sheet = findSteamSheet(workbook, warnings);
            if (sheet == null) {
                return new ImportSummary(0, 0, 0, warnings);
            }
            Layout layout = detectLayout(sheet, formatter, warnings);
            if (layout == null || layout.consumers().isEmpty()) {
                warnings.add("Не удалось определить потребителей / колонки Qo·T·P·M");
                return new ImportSummary(0, 0, 0, warnings);
            }
            warnings.add("Потребителей: " + layout.consumers().size()
                    + "; колонка времени=" + colLetter(layout.timeCol()));

            Set<String> seen = new HashSet<>();
            for (int r = layout.metricRow() + 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) {
                    continue;
                }
                rowsScanned++;
                LocalDateTime factTime = parseDateTime(row.getCell(layout.timeCol()), formatter);
                if (factTime == null) {
                    continue;
                }
                rowsAccepted++;
                for (ConsumerBlock c : layout.consumers()) {
                    String dedup = factTime + "|" + c.code();
                    if (!seen.add(dedup)) {
                        continue;
                    }
                    batch.add(new EnergySteamHourlyValue(
                            factTime,
                            c.code(),
                            c.label(),
                            readNumeric(row.getCell(c.flowCol()), evaluator, formatter),
                            readNumeric(row.getCell(c.tempCol()), evaluator, formatter),
                            readNumeric(row.getCell(c.pressureCol()), evaluator, formatter),
                            readNumeric(row.getCell(c.massCol()), evaluator, formatter),
                            sourceName));
                }
            }
        }

        if (batch.isEmpty()) {
            warnings.add("Пар (почасовой): нет строк с распознанным временем");
            return new ImportSummary(rowsScanned, rowsAccepted, 0, warnings);
        }

        LocalDateTime min = null;
        LocalDateTime max = null;
        for (EnergySteamHourlyValue row : batch) {
            if (min == null || row.getFactTime().isBefore(min)) {
                min = row.getFactTime();
            }
            if (max == null || row.getFactTime().isAfter(max)) {
                max = row.getFactTime();
            }
        }
        repository.deleteByFactTimeBetween(Objects.requireNonNull(min), Objects.requireNonNull(max));
        repository.flush();
        repository.saveAll(batch);
        warnings.add("Период данных: " + min + " … " + max);
        return new ImportSummary(rowsScanned, rowsAccepted, batch.size(), warnings);
    }

    private static Sheet findSteamSheet(Workbook workbook, List<String> warnings) {
        List<String> names = new ArrayList<>();
        for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
            names.add(workbook.getSheetName(i));
        }
        warnings.add("Листы в файле: " + String.join(", ", names));
        for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
            Sheet sheet = workbook.getSheetAt(i);
            String n = sheet.getSheetName() != null ? sheet.getSheetName().trim().toLowerCase(Locale.ROOT) : "";
            if (n.contains("пар") || n.contains("steam")) {
                return sheet;
            }
        }
        if (workbook.getNumberOfSheets() > 0) {
            warnings.add("Лист «пар» не найден — используем первый лист «" + workbook.getSheetName(0) + "»");
            return workbook.getSheetAt(0);
        }
        warnings.add("В файле нет листов");
        return null;
    }

    private static Layout detectLayout(Sheet sheet, DataFormatter formatter, List<String> warnings) {
        int maxProbe = Math.min(sheet.getLastRowNum(), 15);
        for (int r = 0; r <= maxProbe; r++) {
            Row metricRow = sheet.getRow(r);
            if (metricRow == null) {
                continue;
            }
            int timeCol = -1;
            int last = Math.max(metricRow.getLastCellNum(), 0);
            for (int c = 0; c < last; c++) {
                if (isTimeHeader(normalizeHeader(cellText(metricRow.getCell(c), formatter)))) {
                    timeCol = c;
                    break;
                }
            }
            if (timeCol < 0) {
                continue;
            }
            // строка потребителей — обычно над строкой метрик
            int consumerRowIdx = Math.max(0, r - 1);
            Row consumerRow = sheet.getRow(consumerRowIdx);
            List<ConsumerBlock> consumers = buildConsumers(consumerRow, metricRow, formatter, last, warnings);
            if (!consumers.isEmpty()) {
                return new Layout(timeCol, consumerRowIdx, r, consumers);
            }
        }
        return null;
    }

    private static List<ConsumerBlock> buildConsumers(
            Row consumerRow, Row metricRow, DataFormatter formatter, int lastCol, List<String> warnings) {
        List<int[]> ranges = new ArrayList<>(); // start, endInclusive
        if (consumerRow != null) {
            List<Integer> starts = new ArrayList<>();
            List<String> labels = new ArrayList<>();
            for (int c = 0; c < lastCol; c++) {
                String raw = cellText(consumerRow.getCell(c), formatter).trim();
                if (!raw.isEmpty()) {
                    starts.add(c);
                    labels.add(raw.replace('\n', ' ').trim());
                }
            }
            for (int i = 0; i < starts.size(); i++) {
                int start = starts.get(i);
                int end = (i + 1 < starts.size() ? starts.get(i + 1) : lastCol) - 1;
                if (end >= start) {
                    ranges.add(new int[] {start, end, i});
                }
            }
            List<ConsumerBlock> out = new ArrayList<>();
            for (int i = 0; i < ranges.size(); i++) {
                int start = ranges.get(i)[0];
                int end = ranges.get(i)[1];
                String label = labels.get(i);
                ConsumerBlock block = metricsInRange(metricRow, formatter, start, end, label, i + 1);
                if (block != null) {
                    out.add(block);
                    warnings.add("Потребитель «" + block.label() + "» (" + block.code() + "): "
                            + "Qo/G=" + colLetter(block.flowCol())
                            + ", T=" + colLetter(block.tempCol())
                            + ", P=" + colLetter(block.pressureCol())
                            + ", M=" + colLetter(block.massCol()));
                } else {
                    warnings.add("Потребитель «" + label + "»: не найден полный набор Qo/T/P/M в колонках "
                            + colLetter(start) + "…" + colLetter(end));
                }
            }
            if (!out.isEmpty()) {
                return out;
            }
        }
        // Fallback: группы по суффиксу т01/т02 в заголовках метрик
        return buildConsumersByMetricSuffix(metricRow, formatter, lastCol, warnings);
    }

    private static List<ConsumerBlock> buildConsumersByMetricSuffix(
            Row metricRow, DataFormatter formatter, int lastCol, List<String> warnings) {
        // collect unique тNN codes in order of first appearance
        List<String> codes = new ArrayList<>();
        for (int c = 0; c < lastCol; c++) {
            String h = normalizeHeader(cellText(metricRow.getCell(c), formatter));
            Matcher m = CONSUMER_CODE.matcher(h);
            if (m.find()) {
                String code = "t" + m.group(1);
                if (!codes.contains(code)) {
                    codes.add(code);
                }
            }
        }
        List<ConsumerBlock> out = new ArrayList<>();
        for (String code : codes) {
            int flow = -1, temp = -1, press = -1, mass = -1;
            for (int c = 0; c < lastCol; c++) {
                String h = normalizeHeader(cellText(metricRow.getCell(c), formatter));
                if (!h.contains(code.replace("t", "т")) && !h.contains(code)) {
                    // also match т01 without zero pad variants
                    String num = code.substring(1);
                    if (!h.contains("т" + num) && !h.contains("т0" + num) && !h.contains("т00" + num)) {
                        continue;
                    }
                }
                if (flow < 0 && isFlowHeader(h)) {
                    flow = c;
                } else if (temp < 0 && isTempHeader(h)) {
                    temp = c;
                } else if (press < 0 && isPressureHeader(h)) {
                    press = c;
                } else if (mass < 0 && isMassHeader(h)) {
                    mass = c;
                }
            }
            if (flow >= 0 && temp >= 0 && press >= 0 && mass >= 0) {
                out.add(new ConsumerBlock(code, code, flow, temp, press, mass));
            }
        }
        if (!out.isEmpty()) {
            warnings.add("Потребители определены по суффиксам метрик: " + out.size());
        }
        return out;
    }

    private static ConsumerBlock metricsInRange(
            Row metricRow, DataFormatter formatter, int start, int end, String label, int index1) {
        int flow = -1, temp = -1, press = -1, mass = -1;
        String codeFromMetric = null;
        for (int c = start; c <= end; c++) {
            String h = normalizeHeader(cellText(metricRow.getCell(c), formatter));
            if (h.isEmpty()) {
                continue;
            }
            if (codeFromMetric == null) {
                Matcher m = CONSUMER_CODE.matcher(h);
                if (m.find()) {
                    codeFromMetric = "t" + m.group(1);
                }
            }
            if (flow < 0 && isFlowHeader(h)) {
                flow = c;
            } else if (temp < 0 && isTempHeader(h)) {
                temp = c;
            } else if (press < 0 && isPressureHeader(h)) {
                press = c;
            } else if (mass < 0 && isMassHeader(h)) {
                mass = c;
            }
        }
        // Позиционный fallback внутри блока: 1-я, 2-я, 3-я и колонка M (4-я по смыслу / последняя «M»)
        if (flow < 0 || temp < 0 || press < 0 || mass < 0) {
            List<Integer> metricCols = new ArrayList<>();
            for (int c = start; c <= end; c++) {
                String h = normalizeHeader(cellText(metricRow.getCell(c), formatter));
                if (h.isEmpty() || isSkipHeader(h)) {
                    continue;
                }
                metricCols.add(c);
            }
            if (metricCols.size() >= 4) {
                if (flow < 0) {
                    flow = metricCols.get(0);
                }
                if (temp < 0) {
                    temp = metricCols.get(1);
                }
                if (press < 0) {
                    press = metricCols.get(2);
                }
                if (mass < 0) {
                    // предпочитаем колонку с M, иначе 4-я
                    mass = metricCols.get(Math.min(3, metricCols.size() - 1));
                    for (int c : metricCols) {
                        String h = normalizeHeader(cellText(metricRow.getCell(c), formatter));
                        if (isMassHeader(h)) {
                            mass = c;
                            break;
                        }
                    }
                }
            }
        }
        if (flow < 0 || temp < 0 || press < 0 || mass < 0) {
            return null;
        }
        String code = codeFromMetric;
        if (code == null) {
            Matcher uz = UZ_CODE.matcher(label);
            if (uz.find()) {
                code = "uz_" + uz.group(1);
            } else {
                code = "c" + index1;
            }
        }
        return new ConsumerBlock(code, label, flow, temp, press, mass);
    }

    private static boolean isTimeHeader(String text) {
        return text.contains("время") || text.equals("time");
    }

    private static boolean isFlowHeader(String text) {
        // Qo_т01, Gв.уз1, Gм_106, Gв_уз1
        return text.startsWith("qo")
                || text.startsWith("gв")
                || text.startsWith("gм")
                || text.startsWith("gm")
                || text.startsWith("gv")
                || (text.startsWith("g") && (text.contains("кгч") || text.contains("м3ч") || text.contains("тч")));
    }

    private static boolean isTempHeader(String text) {
        if (text.startsWith("to") || text.startsWith("time")) {
            return false;
        }
        return text.startsWith("t_")
                || text.startsWith("t.")
                || text.startsWith("tт")
                || text.startsWith("tуз")
                || text.matches("t[._]?уз?\\d+.*")
                || text.matches("t[._]?т\\d+.*")
                || (text.startsWith("t") && text.length() > 1 && Character.isDigit(text.charAt(1)));
    }

    private static boolean isPressureHeader(String text) {
        return text.startsWith("p_")
                || text.startsWith("p.")
                || text.startsWith("pт")
                || text.startsWith("pуз")
                || (text.startsWith("p") && (text.contains("мпа") || text.contains("mpa") || text.matches("p\\d+.*")));
    }

    private static boolean isMassHeader(String text) {
        // M_т01, Mг.уз1, Mм_106 — не путать с to_
        if (text.startsWith("to") || text.startsWith("hc") || text.startsWith("w")) {
            return false;
        }
        return text.startsWith("m_")
                || text.startsWith("m.")
                || text.startsWith("mг")
                || text.startsWith("mм")
                || text.startsWith("мг")
                || text.startsWith("мм")
                || text.startsWith("mт")
                || (text.startsWith("m") && text.contains("т") && !text.startsWith("mp"));
    }

    private static boolean isSkipHeader(String text) {
        return text.startsWith("to")
                || text.startsWith("нс")
                || text.startsWith("hc")
                || text.startsWith("w_")
                || text.startsWith("wт");
    }

    private static String normalizeHeader(String raw) {
        if (raw == null) {
            return "";
        }
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
            if ((ch == 'р' || ch == 'Р' || ch == 'ρ' || ch == 'Ρ') && Character.isDigit(next)) {
                ch = 'p';
            }
            sb.append(Character.toLowerCase(ch));
        }
        String t = sb.toString();
        t = t.replace("м³", "м3").replace("мᶾ", "м3").replace("m³", "m3").replace("^3", "3");
        t = t.replace("°", "").replace("/", "");
        t = t.replaceAll("[^a-zа-яё0-9._]", "");
        return t;
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
        String raw = cellText(cell, formatter).replace('\u00A0', ' ').trim();
        if (raw.isEmpty()) {
            return null;
        }
        String normalized = raw.replace(',', '.');
        for (DateTimeFormatter fmt : List.of(DT, DT_SEC)) {
            try {
                return LocalDateTime.parse(normalized, fmt);
            } catch (DateTimeParseException ignored) {
                // next
            }
        }
        return null;
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
