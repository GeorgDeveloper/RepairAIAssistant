package ru.georgdeveloper.assistantcore.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public class MonitoringRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public List<Map<String, Object>> getRegions() {
        return jdbcTemplate.queryForList("SELECT id, name_region FROM monitoring_bd.region");
    }

    public List<Map<String, Object>> getEquipment(int regionId) {
        return jdbcTemplate.queryForList("SELECT id, name_equipment FROM monitoring_bd.equipment WHERE region_id = ?",
                regionId);
    }

    public List<Map<String, Object>> getNodes(int equipmentId) {
        return jdbcTemplate.queryForList("SELECT id, name_node FROM monitoring_bd.node WHERE equipment_id = ?",
                equipmentId);
    }

    public List<Map<String, Object>> searchBreakDown() {
        String sql = "SELECT production_day, downtime_percentage " 
        + "FROM report_plant WHERE YEAR(STR_TO_DATE(production_day, '%d.%m.%Y')) = YEAR(CURDATE()) " + 
        "AND MONTH(STR_TO_DATE(production_day, '%d.%m.%Y')) = MONTH(CURDATE()) ORDER BY STR_TO_DATE(production_day, '%d.%m.%Y')";
        return jdbcTemplate.queryForList(sql);
    }

    public List<Map<String, Object>> searchAvailability() {
        String sql = "SELECT production_day, availability " 
        + "FROM report_plant WHERE YEAR(STR_TO_DATE(production_day, '%d.%m.%Y')) = YEAR(CURDATE()) " + 
        "AND MONTH(STR_TO_DATE(production_day, '%d.%m.%Y')) = MONTH(CURDATE()) ORDER BY STR_TO_DATE(production_day, '%d.%m.%Y')";
        return jdbcTemplate.queryForList(sql);
    }


    public Map<String, Object> getCurrentMetrics() {
        Map<String, Object> result = new java.util.HashMap<>();
        // --- Список таблиц и их префиксы ---
        String[][] tables = {
            {"report_new_mixing_area", "report_new_mixing_area", null},
            {"report_semifinishing_area", "report_semifinishing_area", null},
            {"report_building_area", "report_building_area", null},
            {"report_curing_area", "report_curing_area", null},
            {"report_finishig_area", "report_finishig_area", null},
            {"report_modules", "report_modules", null},
            {"report_plant", "report_plant", "availability"}
        };
        for (String[] t : tables) {
            String table = t[0];
            String prefix = t[1];
            // String hasAvailability = t[2];
            // BD за предыдущие сутки
            String sqlToday = "SELECT AVG(downtime_percentage) as bd FROM " + table + " WHERE production_day = DATE_FORMAT(DATE_SUB(CURDATE(), INTERVAL 1 DAY), '%d.%m.%Y')";
            Double bdToday = jdbcTemplate.queryForObject(sqlToday, Double.class);
            result.put(prefix + "_bd_today", bdToday != null ? bdToday : 0.0);
            // BD за месяц
            String sqlMonth = "SELECT AVG(downtime_percentage) as bd FROM " + table + " WHERE YEAR(STR_TO_DATE(production_day, '%d.%m.%Y')) = YEAR(CURDATE()) AND MONTH(STR_TO_DATE(production_day, '%d.%m.%Y')) = MONTH(CURDATE())";
            Double bdMonth = jdbcTemplate.queryForObject(sqlMonth, Double.class);
            result.put(prefix + "_bd_month", bdMonth != null ? bdMonth : 0.0);
            // Availability
            if ("report_plant".equals(table)) {
                String sqlTodayA = "SELECT AVG(availability) FROM report_plant WHERE production_day = DATE_FORMAT(DATE_SUB(CURDATE(), INTERVAL 1 DAY), '%d.%m.%Y')";
                Double aToday = jdbcTemplate.queryForObject(sqlTodayA, Double.class);
                result.put(prefix + "_availability_today", aToday != null ? aToday : 0.0);
                String sqlMonthA = "SELECT AVG(availability) FROM report_plant WHERE YEAR(STR_TO_DATE(production_day, '%d.%m.%Y')) = YEAR(CURDATE()) AND MONTH(STR_TO_DATE(production_day, '%d.%m.%Y')) = MONTH(CURDATE())";
                Double aMonth = jdbcTemplate.queryForObject(sqlMonthA, Double.class);
                result.put(prefix + "_availability_month", aMonth != null ? aMonth : 0.0);
            } else {
                // Для остальных таблиц availability - заглушка
                result.put(prefix + "_availability_today", 0.0);
                result.put(prefix + "_availability_month", 0.0);
            }
        }
        return result;
    }
}
