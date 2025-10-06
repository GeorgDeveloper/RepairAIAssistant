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
    public List<Map<String, Object>> months(@RequestParam int year) {
        logger.debug("Fetching available months for year: {}", year);
        String sql = "SELECT DISTINCT MONTH(STR_TO_DATE(production_day, '%d.%m.%Y')) AS month FROM report_plant " +
                "WHERE YEAR(STR_TO_DATE(production_day, '%d.%m.%Y')) = ? ORDER BY month";
        List<Map<String, Object>> result = jdbcTemplate.queryForList(sql, year);
        logger.debug("Retrieved {} months for year {}", result.size(), year);
        return result;
    }

    @GetMapping("/weeks")
    public List<Map<String, Object>> weeks(@RequestParam int year, @RequestParam int month) {
        logger.debug("Fetching available weeks for year: {} and month: {}", year, month);
        String sql = "SELECT DISTINCT WEEK(STR_TO_DATE(production_day, '%d.%m.%Y'), 3) AS week " +
                "FROM report_plant WHERE YEAR(STR_TO_DATE(production_day, '%d.%m.%Y')) = ? AND MONTH(STR_TO_DATE(production_day, '%d.%m.%Y')) = ? ORDER BY week";
        List<Map<String, Object>> result = jdbcTemplate.queryForList(sql, year, month);
        logger.debug("Retrieved {} weeks for year {} and month {}", result.size(), year, month);
        return result;
    }

    @GetMapping("/data")
    public List<Map<String, Object>> data(@RequestParam(required = false) Integer year,
                                          @RequestParam(required = false) Integer month,
                                          @RequestParam(required = false) Integer week,
                                          @RequestParam(defaultValue = "Plant") String area,
                                          @RequestParam(defaultValue = "bd") String metric) {
        logger.debug("Fetching BD/AV data with parameters: year={}, month={}, week={}, area={}, metric={}", 
                    year, month, week, area, metric);
        String valueColumn = "bd".equalsIgnoreCase(metric) ? "downtime_percentage" : "availability";

        String periodSelect;
        String groupBy;
        List<Object> args = new ArrayList<>();

        if (week != null && month != null && year != null) {
            periodSelect = "DATE_FORMAT(STR_TO_DATE(production_day, '%d.%m.%Y'), '%d')";
            groupBy = periodSelect;
            args.add(year); args.add(month); args.add(week);
        } else if (month != null && year != null) {
            periodSelect = "WEEK(STR_TO_DATE(production_day, '%d.%m.%Y'), 3)";
            groupBy = periodSelect;
            args.add(year); args.add(month);
        } else if (year != null) {
            periodSelect = "MONTH(STR_TO_DATE(production_day, '%d.%m.%Y'))";
            groupBy = periodSelect;
            args.add(year);
        } else {
            periodSelect = "YEAR(STR_TO_DATE(production_day, '%d.%m.%Y'))";
            groupBy = periodSelect;
        }

        String filter;
        if (week != null && month != null && year != null) {
            filter = "WHERE YEAR(STR_TO_DATE(production_day, '%d.%m.%Y')) = ? AND MONTH(STR_TO_DATE(production_day, '%d.%m.%Y')) = ? AND WEEK(STR_TO_DATE(production_day, '%d.%m.%Y'), 3) = ?";
        } else if (month != null && year != null) {
            filter = "WHERE YEAR(STR_TO_DATE(production_day, '%d.%m.%Y')) = ? AND MONTH(STR_TO_DATE(production_day, '%d.%m.%Y')) = ?";
        } else if (year != null) {
            filter = "WHERE YEAR(STR_TO_DATE(production_day, '%d.%m.%Y')) = ?";
        } else {
            filter = "";
        }

        List<Map<String, Object>> result = new ArrayList<>();

        if ("all".equalsIgnoreCase(area)) {
            logger.debug("Querying data for all areas");
            for (Map.Entry<String, String> entry : AREA_TABLE.entrySet()) {
                String areaKey = entry.getKey();
                String table = entry.getValue();
                String sql = "SELECT " + periodSelect + " AS period_label, AVG(" + valueColumn + ") AS value, '" + areaKey + "' AS area FROM " + table + " " + filter + " GROUP BY " + groupBy + " ORDER BY 1";
                result.addAll(jdbcTemplate.queryForList(sql, args.toArray()));
            }
        } else {
            String table = AREA_TABLE.getOrDefault(area, "report_plant");
            logger.debug("Querying data for area: {} using table: {}", area, table);
            String sql = "SELECT " + periodSelect + " AS period_label, AVG(" + valueColumn + ") AS value, '" + area + "' AS area FROM " + table + " " + filter + " GROUP BY " + groupBy + " ORDER BY 1";
            result.addAll(jdbcTemplate.queryForList(sql, args.toArray()));
        }

        logger.debug("Retrieved {} data points", result.size());
        return result;
    }
}


