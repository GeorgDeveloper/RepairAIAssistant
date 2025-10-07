package ru.georgdeveloper.assistantcore.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

/**
 * Эндпоинты для страницы "Динамика BD/AV".
 * Источник данных: таблицы report_* с полями production_day (dd.MM.yyyy), downtime_percentage, availability.
 */
@RestController
@RequestMapping("/bdav")
public class BdAvController {

    private static final Logger logger = LoggerFactory.getLogger(BdAvController.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final Map<String, String> AREA_TABLE = Map.of(
            "NewMixingArea", "report_new_mixing_area",
            "SemifinishingArea", "report_semifinishing_area",
            "BuildingArea", "report_building_area",
            "CuringArea", "report_curing_area",
            "FinishingArea", "report_finishig_area",
            "Modules", "report_modules",
            "Plant", "report_plant"
    );

    @GetMapping("/years")
    public List<Map<String, Object>> years() {
        logger.debug("Fetching available years from report_plant table");
        String sql = "SELECT DISTINCT YEAR(STR_TO_DATE(production_day, '%d.%m.%Y')) AS year FROM report_plant ORDER BY year DESC";
        List<Map<String, Object>> result = jdbcTemplate.queryForList(sql);
        logger.debug("Retrieved {} years", result.size());
        return result;
    }

    @GetMapping("/months")
    public List<Map<String, Object>> months(@RequestParam List<String> year) {
        logger.debug("Fetching available months for years: {}", year);
        if (year == null || year.isEmpty() || year.contains("all")) {
            String sql = "SELECT DISTINCT MONTH(STR_TO_DATE(production_day, '%d.%m.%Y')) AS month FROM report_plant ORDER BY month";
            List<Map<String, Object>> result = jdbcTemplate.queryForList(sql);
            logger.debug("Retrieved {} months for all years", result.size());
            return result;
        }
        String sql = "SELECT DISTINCT MONTH(STR_TO_DATE(production_day, '%d.%m.%Y')) AS month FROM report_plant " +
                "WHERE YEAR(STR_TO_DATE(production_day, '%d.%m.%Y')) IN (" + buildInClause(year.size()) + ") ORDER BY month";
        Object[] params = year.stream().map(Integer::parseInt).toArray();
        List<Map<String, Object>> result = jdbcTemplate.queryForList(sql, params);
        logger.debug("Retrieved {} months for years {}", result.size(), year);
        return result;
    }

    @GetMapping("/weeks")
    public List<Map<String, Object>> weeks(@RequestParam List<String> year, @RequestParam List<String> month) {
        logger.debug("Fetching available weeks for years: {} and months: {}", year, month);
        if (year == null || year.isEmpty() || year.contains("all") || 
            month == null || month.isEmpty() || month.contains("all")) {
            String sql = "SELECT DISTINCT WEEK(STR_TO_DATE(production_day, '%d.%m.%Y'), 3) AS week FROM report_plant ORDER BY week";
            List<Map<String, Object>> result = jdbcTemplate.queryForList(sql);
            logger.debug("Retrieved {} weeks for all years and months", result.size());
            return result;
        }
        String sql = "SELECT DISTINCT WEEK(STR_TO_DATE(production_day, '%d.%m.%Y'), 3) AS week " +
                "FROM report_plant WHERE YEAR(STR_TO_DATE(production_day, '%d.%m.%Y')) IN (" + buildInClause(year.size()) + ") " +
                "AND MONTH(STR_TO_DATE(production_day, '%d.%m.%Y')) IN (" + buildInClause(month.size()) + ") ORDER BY week";
        List<Object> params = new ArrayList<>();
        for (String y : year) params.add(Integer.parseInt(y));
        for (String m : month) params.add(Integer.parseInt(m));
        List<Map<String, Object>> result = jdbcTemplate.queryForList(sql, params.toArray());
        logger.debug("Retrieved {} weeks for years {} and months {}", result.size(), year, month);
        return result;
    }

    @GetMapping("/data")
    public List<Map<String, Object>> data(@RequestParam(required = false) List<String> year,
                                          @RequestParam(required = false) List<String> month,
                                          @RequestParam(required = false) List<String> week,
                                          @RequestParam(required = false) List<String> area,
                                          @RequestParam(defaultValue = "bd") String metric) {
        logger.debug("Fetching BD/AV data with parameters: year={}, month={}, week={}, area={}, metric={}", 
                    year, month, week, area, metric);
        String valueColumn = "bd".equalsIgnoreCase(metric) ? "downtime_percentage" : "availability";

        String periodSelect;
        String groupBy;
        List<Object> args = new ArrayList<>();

        // Определяем уровень детализации
        if (week != null && !week.isEmpty() && !week.contains("all") &&
            month != null && !month.isEmpty() && !month.contains("all") &&
            year != null && !year.isEmpty() && !year.contains("all")) {
            periodSelect = "DATE_FORMAT(STR_TO_DATE(production_day, '%d.%m.%Y'), '%d')";
            groupBy = periodSelect;
            for (String y : year) args.add(Integer.parseInt(y));
            for (String m : month) args.add(Integer.parseInt(m));
            for (String w : week) args.add(Integer.parseInt(w));
        } else if (month != null && !month.isEmpty() && !month.contains("all") &&
                   year != null && !year.isEmpty() && !year.contains("all")) {
            periodSelect = "WEEK(STR_TO_DATE(production_day, '%d.%m.%Y'), 3)";
            groupBy = periodSelect;
            for (String y : year) args.add(Integer.parseInt(y));
            for (String m : month) args.add(Integer.parseInt(m));
        } else if (year != null && !year.isEmpty() && !year.contains("all")) {
            periodSelect = "MONTH(STR_TO_DATE(production_day, '%d.%m.%Y'))";
            groupBy = periodSelect;
            for (String y : year) args.add(Integer.parseInt(y));
        } else {
            periodSelect = "YEAR(STR_TO_DATE(production_day, '%d.%m.%Y'))";
            groupBy = periodSelect;
        }

        String filter;
        if (week != null && !week.isEmpty() && !week.contains("all") &&
            month != null && !month.isEmpty() && !month.contains("all") &&
            year != null && !year.isEmpty() && !year.contains("all")) {
            filter = "WHERE YEAR(STR_TO_DATE(production_day, '%d.%m.%Y')) IN (" + buildInClause(year.size()) + ") " +
                    "AND MONTH(STR_TO_DATE(production_day, '%d.%m.%Y')) IN (" + buildInClause(month.size()) + ") " +
                    "AND WEEK(STR_TO_DATE(production_day, '%d.%m.%Y'), 3) IN (" + buildInClause(week.size()) + ")";
        } else if (month != null && !month.isEmpty() && !month.contains("all") &&
                   year != null && !year.isEmpty() && !year.contains("all")) {
            filter = "WHERE YEAR(STR_TO_DATE(production_day, '%d.%m.%Y')) IN (" + buildInClause(year.size()) + ") " +
                    "AND MONTH(STR_TO_DATE(production_day, '%d.%m.%Y')) IN (" + buildInClause(month.size()) + ")";
        } else if (year != null && !year.isEmpty() && !year.contains("all")) {
            filter = "WHERE YEAR(STR_TO_DATE(production_day, '%d.%m.%Y')) IN (" + buildInClause(year.size()) + ")";
        } else {
            filter = "";
        }

        List<Map<String, Object>> result = new ArrayList<>();

        if (area == null || area.isEmpty() || area.contains("all")) {
            logger.debug("Querying data for all areas");
            for (Map.Entry<String, String> entry : AREA_TABLE.entrySet()) {
                String areaKey = entry.getKey();
                String table = entry.getValue();
                String sql = "SELECT " + periodSelect + " AS period_label, AVG(" + valueColumn + ") AS value, '" + areaKey + "' AS area FROM " + table + " " + filter + " GROUP BY " + groupBy + " ORDER BY 1";
                result.addAll(jdbcTemplate.queryForList(sql, args.toArray()));
            }
        } else {
            for (String areaName : area) {
                String table = AREA_TABLE.getOrDefault(areaName, "report_plant");
                logger.debug("Querying data for area: {} using table: {}", areaName, table);
                String sql = "SELECT " + periodSelect + " AS period_label, AVG(" + valueColumn + ") AS value, '" + areaName + "' AS area FROM " + table + " " + filter + " GROUP BY " + groupBy + " ORDER BY 1";
                result.addAll(jdbcTemplate.queryForList(sql, args.toArray()));
            }
        }

        logger.debug("Retrieved {} data points", result.size());
        return result;
    }
    
    private String buildInClause(int size) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < size; i++) {
            if (i > 0) sb.append(",");
            sb.append("?");
        }
        return sb.toString();
    }
}


