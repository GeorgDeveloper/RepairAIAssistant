package ru.georgdeveloper.assistantbaseupdate.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Сверка записей CMMS (SQL Server) с MySQL: наряды, которые не найдены в целевой таблице
 * по ключу переноса (code + start_bd_t1 + stop_bd_t4).
 */
@Service
public class CmmsReconciliationService {

    private static final Logger logger = LoggerFactory.getLogger(CmmsReconciliationService.class);
    private static final ZoneId PRODUCTION_ZONE = ZoneId.of("Europe/Moscow");

    private static final String CMMS_SELECT = """
            SELECT
                MachineName, Assembly, SubAssembly, InitialComment, WOCodeName,
                TYPEWO, Date_T1, Date_T2, Date_T3, Date_T4, SDuration, STTR,
                SLogisticTimeMin, WOStatusLocalDescr, Maintainers, comment,
                PlantDepartmentGeographicalCodeName
            FROM REP_BreakdownReport
            WHERE
                ((Date_T1 >= ? AND Date_T1 < ?)
                OR (Date_T4 >= ? AND Date_T4 < ?))
                AND %s
            """;

    private static final String[] CSV_HEADER = {
            "code", "type_wo", "machine_name", "status", "area",
            "date_t1", "date_t2", "date_t3", "date_t4", "sduration", "duration_minutes",
            "comment_length", "comment", "note"
    };

    public enum RecordKind {
        BD("equipment_maintenance_records", "TYPEWO IS NULL OR TYPEWO NOT LIKE '%Tag%'"),
        TAG("tag_maintenance_records", "TYPEWO LIKE '%Tag%'");

        private final String mysqlTable;
        private final String typeWoFilter;

        RecordKind(String mysqlTable, String typeWoFilter) {
            this.mysqlTable = mysqlTable;
            this.typeWoFilter = typeWoFilter;
        }
    }

    public record ReconciliationResult(byte[] csvBytes, int cmmsTotal, int mysqlKeysLoaded, int missingCount) {}

    @Autowired
    private JdbcTemplate sqlServerJdbcTemplate;

    @Autowired
    private JdbcTemplate mysqlJdbcTemplate;

    public ReconciliationResult buildMissingCsv(RecordKind kind, int year) {
        LocalDateTime rangeStart = LocalDate.of(year, 1, 1).atStartOfDay();
        LocalDateTime rangeEnd = LocalDateTime.now(PRODUCTION_ZONE).plusSeconds(1);

        String cmmsSql = CMMS_SELECT.formatted(kind.typeWoFilter);
        List<Map<String, Object>> cmmsRows = sqlServerJdbcTemplate.queryForList(
                cmmsSql, rangeStart, rangeEnd, rangeStart, rangeEnd);

        Set<String> mysqlKeys = loadMysqlKeys(kind.mysqlTable, rangeStart);
        List<Map<String, Object>> missing = new ArrayList<>();

        for (Map<String, Object> row : cmmsRows) {
            String code = stringValue(row.get("WOCodeName"));
            LocalDateTime t1 = parseSqlServerDateTime(row.get("Date_T1"));
            LocalDateTime t4 = parseSqlServerDateTime(row.get("Date_T4"));
            if (!mysqlKeys.contains(recordKey(code, t1, t4))) {
                missing.add(row);
            }
        }

        byte[] csv = toCsv(missing);
        logger.info(
                "Сверка {} за {}: CMMS={}, ключей в MySQL={}, отсутствует в MySQL={}",
                kind.name(), year, cmmsRows.size(), mysqlKeys.size(), missing.size());

        return new ReconciliationResult(csv, cmmsRows.size(), mysqlKeys.size(), missing.size());
    }

    private Set<String> loadMysqlKeys(String table, LocalDateTime rangeStart) {
        String sql = """
                SELECT code, start_bd_t1, stop_bd_t4
                FROM %s
                WHERE start_bd_t1 >= ? OR stop_bd_t4 >= ? OR start_bd_t1 IS NULL OR stop_bd_t4 IS NULL
                """.formatted(table);

        List<Map<String, Object>> rows = mysqlJdbcTemplate.queryForList(sql, rangeStart, rangeStart);
        Set<String> keys = new HashSet<>(rows.size() * 2);
        for (Map<String, Object> row : rows) {
            keys.add(recordKey(
                    stringValue(row.get("code")),
                    toLocalDateTime(row.get("start_bd_t1")),
                    toLocalDateTime(row.get("stop_bd_t4"))));
        }
        return keys;
    }

    static String recordKey(String code, LocalDateTime t1, LocalDateTime t4) {
        return (code != null ? code : "") + '\u0001'
                + (t1 != null ? t1 : "") + '\u0001'
                + (t4 != null ? t4 : "");
    }

    private byte[] toCsv(List<Map<String, Object>> rows) {
        StringBuilder sb = new StringBuilder();
        sb.append('\uFEFF');
        sb.append(String.join(",", CSV_HEADER)).append('\n');

        for (Map<String, Object> row : rows) {
            String comment = stringValue(row.get("comment"));
            int durationMin = parseDurationToMinutes(stringValue(row.get("SDuration")));
            String note = durationMin > 1439 ? "long_order_possible_split_in_mysql" : "";

            sb.append(csvCell(stringValue(row.get("WOCodeName")))).append(',');
            sb.append(csvCell(stringValue(row.get("TYPEWO")))).append(',');
            sb.append(csvCell(stringValue(row.get("MachineName")))).append(',');
            sb.append(csvCell(stringValue(row.get("WOStatusLocalDescr")))).append(',');
            sb.append(csvCell(stringValue(row.get("PlantDepartmentGeographicalCodeName")))).append(',');
            sb.append(csvCell(formatDateTime(parseSqlServerDateTime(row.get("Date_T1"))))).append(',');
            sb.append(csvCell(formatDateTime(parseSqlServerDateTime(row.get("Date_T2"))))).append(',');
            sb.append(csvCell(formatDateTime(parseSqlServerDateTime(row.get("Date_T3"))))).append(',');
            sb.append(csvCell(formatDateTime(parseSqlServerDateTime(row.get("Date_T4"))))).append(',');
            sb.append(csvCell(stringValue(row.get("SDuration")))).append(',');
            sb.append(csvCell(String.valueOf(durationMin))).append(',');
            sb.append(csvCell(String.valueOf(comment.length()))).append(',');
            sb.append(csvCell(comment)).append(',');
            sb.append(csvCell(note)).append('\n');
        }

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static String csvCell(String value) {
        if (value == null) {
            return "";
        }
        String escaped = value.replace("\"", "\"\"");
        if (escaped.contains(",") || escaped.contains("\"") || escaped.contains("\n") || escaped.contains("\r")) {
            return "\"" + escaped + "\"";
        }
        return escaped;
    }

    private static String stringValue(Object value) {
        return value != null ? value.toString().trim() : "";
    }

    private static String formatDateTime(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.toString() : "";
    }

    private static LocalDateTime toLocalDateTime(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof LocalDateTime ldt) {
            return ldt;
        }
        if (value instanceof java.sql.Timestamp ts) {
            return ts.toLocalDateTime();
        }
        return parseSqlServerDateTime(value);
    }

    private static LocalDateTime parseSqlServerDateTime(Object dateValue) {
        if (dateValue == null) {
            return null;
        }
        try {
            if (dateValue instanceof LocalDateTime ldt) {
                return ldt;
            }
            if (dateValue instanceof java.sql.Timestamp ts) {
                return ts.toLocalDateTime();
            }
            if (dateValue instanceof java.sql.Date d) {
                return d.toLocalDate().atStartOfDay();
            }
            String dateStr = dateValue.toString().trim();
            return LocalDateTime.parse(dateStr.replace(' ', 'T'));
        } catch (Exception e) {
            return null;
        }
    }

    private static int parseDurationToMinutes(String durationStr) {
        if (durationStr == null || durationStr.isBlank()) {
            return 0;
        }
        try {
            if (durationStr.contains(":")) {
                String[] parts = durationStr.split(":");
                if (parts.length == 2) {
                    return Integer.parseInt(parts[0].trim()) * 60 + Integer.parseInt(parts[1].trim());
                }
            }
        } catch (Exception ignored) {
            // ignore
        }
        return 0;
    }
}
