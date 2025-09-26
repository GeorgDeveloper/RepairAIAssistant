package ru.georgdeveloper.assistantcore.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public class MonitoringRepository {
    // Топ-5 поломок за неделю (общее)
    public List<Map<String, Object>> getTopBreakdownsPerWeek() {
        String sql = "SELECT machine_name, SEC_TO_TIME(SUM(TIME_TO_SEC(machine_downtime))) as machine_downtime " +
                "FROM equipment_maintenance_records " +
                "WHERE WEEK(CURDATE(), 1) = WEEK(start_bd_t1, 1) AND failure_type <> 'Другие' " +
                "GROUP BY machine_name " +
                "ORDER BY SUM(TIME_TO_SEC(machine_downtime)) DESC " +
                "LIMIT 5";
        return jdbcTemplate.queryForList(sql);
    }

    // Топ-5 поломок за неделю по ключевым линиям (используем шаблоны по именам)
    public List<Map<String, Object>> getTopBreakdownsPerWeekKeyLines() {
        String sql = "SELECT machine_name, SEC_TO_TIME(SUM(TIME_TO_SEC(machine_downtime))) as machine_downtime " +
                "FROM equipment_maintenance_records " +
                "WHERE WEEK(CURDATE(), 1) = WEEK(start_bd_t1, 1) AND failure_type <> 'Другие' " +
                "AND (" +
                " machine_name LIKE 'Mixer GK 270%' OR machine_name LIKE 'Mixer GK 320%' OR" +
                " machine_name LIKE 'Calender %' OR machine_name LIKE 'Calender Complex %' OR" +
                " machine_name LIKE 'Bandina%' OR machine_name LIKE 'Duplex%' OR" +
                " machine_name LIKE 'Calender Comerio Ercole%' OR" +
                " machine_name LIKE 'VMI APEX%' OR machine_name LIKE 'CMP APEX%' OR" +
                " machine_name LIKE 'Trafila Quadruplex%' OR machine_name LIKE 'Bartell Bead Machine%' OR" +
                " machine_name LIKE 'TTM fisher belt cutting%' OR machine_name LIKE 'VMI TPCS%' " +
                ") " +
                "GROUP BY machine_name " +
                "ORDER BY SUM(TIME_TO_SEC(machine_downtime)) DESC " +
                "LIMIT 5";
        return jdbcTemplate.queryForList(sql);
    }
    // Топ-5 поломок за сутки (последние 24 часа)
    public List<Map<String, Object>> getTopBreakdownsPerDay() {
        String sql = "SELECT code, machine_name, machine_downtime, cause " +
                "FROM equipment_maintenance_records " +
                "WHERE start_bd_t1 >= DATE_SUB(NOW(), INTERVAL 24 HOUR) " +
                "AND failure_type <> 'Другие' " +
                "ORDER BY TIME_TO_SEC(machine_downtime) DESC " +
                "LIMIT 5";
        return jdbcTemplate.queryForList(sql);
    }

    // Топ-5 поломок по ключевым линиям за сутки (последние 24 часа)
    public List<Map<String, Object>> getTopBreakdownsPerDayKeyLines() {
        // String sql = "SELECT code, machine_name, machine_downtime, cause " +
        //         "FROM equipment_maintenance_records " +
        //         "WHERE (" +
        //         " machine_name LIKE 'Mixer GK 270%' OR machine_name LIKE 'Mixer GK 320%' OR" +
        //         " machine_name LIKE 'Calender %' OR machine_name LIKE 'Calender Complex %' OR" +
        //         " machine_name LIKE 'Bandina%' OR machine_name LIKE 'Duplex%' OR" +
        //         " machine_name LIKE 'Calender Comerio Ercole%' OR" +
        //         " machine_name LIKE 'VMI APEX%' OR machine_name LIKE 'CMP APEX%' OR" +
        //         " machine_name LIKE 'Trafila Quadruplex%' OR machine_name LIKE 'Bartell Bead Machine%' OR" +
        //         " machine_name LIKE 'TTM fisher belt cutting%' OR machine_name LIKE 'VMI TPCS%' " +
        //         ") " +
        //         "AND start_bd_t1 >= DATE_SUB(NOW(), INTERVAL 24 HOUR) " +
        //         "AND failure_type <> 'Другие' " +
        //         "ORDER BY TIME_TO_SEC(machine_downtime) DESC " +
        //         "LIMIT 5";
        String sql = "SELECT FROM top_breakdowns_per_day_key_lines (code, machine_name, machine_downtime, cause) " +
                "SELECT code, machine_name, machine_downtime, cause " +
                "FROM monitor_bd.equipment_maintenance_records " +
                "WHERE machine_name IN ('Mixer GK 270 T-C 2.1', 'Mixer GK 320 E 1.1', " +
                "'Calender Complex Berstorf - 01', 'Bandina - 01', 'Duplex - 01', " +
                "'Calender Comerio Ercole - 01', 'VMI APEX - 01', 'VMI APEX - 02', " +
                "'Trafila Quadruplex - 01', 'Bartell Bead Machine - 01', " +
                "'TTM fisher belt cutting - 01', 'VMI TPCS 1600-1000') " +
                "AND start_bd_t1 >= CONCAT(DATE_SUB(CURRENT_DATE(), INTERVAL 1 DAY), ' 06:00:00') " +
                "AND stop_bd_t4 <= CONCAT(CURRENT_DATE(), ' 08:00:00') " +
                "AND NOT failure_type = 'Другие' " +
                "ORDER BY TIME_TO_SEC(machine_downtime) DESC " +
                "LIMIT 5";
        return jdbcTemplate.queryForList(sql);
    }

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
                + "FROM report_plant WHERE YEAR(STR_TO_DATE(production_day, '%d.%m.%Y')) = YEAR(CURDATE()) "
                + "AND MONTH(STR_TO_DATE(production_day, '%d.%m.%Y')) = MONTH(CURDATE()) ORDER BY STR_TO_DATE(production_day, '%d.%m.%Y')";
        return jdbcTemplate.queryForList(sql);
    }

    public List<Map<String, Object>> searchAvailability() {
        String sql = "SELECT production_day, availability "
                + "FROM report_plant WHERE YEAR(STR_TO_DATE(production_day, '%d.%m.%Y')) = YEAR(CURDATE()) "
                + "AND MONTH(STR_TO_DATE(production_day, '%d.%m.%Y')) = MONTH(CURDATE()) ORDER BY STR_TO_DATE(production_day, '%d.%m.%Y')";
        return jdbcTemplate.queryForList(sql);
    }

    // Данные для графика PM: план/факт/tag за текущий месяц
    public List<Map<String, Object>> getPmPlanFactTagPerMonth() {
        String sql = "SELECT production_day, quantity_pm_planned AS plan, quantity_pm_close AS fact, quantity_tag AS tag " +
                "FROM report_plant WHERE YEAR(STR_TO_DATE(production_day, '%d.%m.%Y')) = YEAR(CURDATE()) " +
                "AND MONTH(STR_TO_DATE(production_day, '%d.%m.%Y')) = MONTH(CURDATE()) " +
                "ORDER BY STR_TO_DATE(production_day, '%d.%m.%Y')";
        return jdbcTemplate.queryForList(sql);
    }

    // Все заявки на поломки (equipment_maintenance_records) с ограничением по количеству
    public List<Map<String, Object>> getEquipmentMaintenanceRecords(int limit) {
        String sql = "SELECT * " +
                "FROM equipment_maintenance_records LIMIT ?";
        return jdbcTemplate.queryForList(sql, limit);
    }


    public Map<String, Object> getCurrentMetrics() {
        Map<String, Object> result = new java.util.HashMap<>();
        String[][] tables = {
            {"report_new_mixing_area", "report_new_mixing_area"},
            {"report_semifinishing_area", "report_semifinishing_area"},
            {"report_building_area", "report_building_area"},
            {"report_curing_area", "report_curing_area"},
            {"report_finishig_area", "report_finishig_area"},
            {"report_modules", "report_modules"},
            {"report_plant", "report_plant"}
        };
        for (String[] t : tables) {
            String table = t[0];
            String prefix = t[1];

            String sqlToday = "SELECT AVG(downtime_percentage) as bd FROM " + table + " WHERE production_day = DATE_FORMAT(DATE_SUB(CURDATE(), INTERVAL 1 DAY), '%d.%m.%Y')";
            Double bdToday = jdbcTemplate.queryForObject(sqlToday, Double.class);
            result.put(prefix + "_bd_today", bdToday != null ? bdToday : 0.0);

            String sqlMonth = "SELECT AVG(downtime_percentage) as bd FROM " + table + " WHERE YEAR(STR_TO_DATE(production_day, '%d.%m.%Y')) = YEAR(CURDATE()) AND MONTH(STR_TO_DATE(production_day, '%d.%m.%Y')) = MONTH(CURDATE())";
            Double bdMonth = jdbcTemplate.queryForObject(sqlMonth, Double.class);
            result.put(prefix + "_bd_month", bdMonth != null ? bdMonth : 0.0);

            String sqlTodayA = "SELECT AVG(availability) FROM " + table + " WHERE production_day = DATE_FORMAT(DATE_SUB(CURDATE(), INTERVAL 1 DAY), '%d.%m.%Y')";
            Double aToday = jdbcTemplate.queryForObject(sqlTodayA, Double.class);
            result.put(prefix + "_availability_today", aToday != null ? aToday : 0.0);

            String sqlMonthA = "SELECT AVG(availability) FROM " + table + " WHERE YEAR(STR_TO_DATE(production_day, '%d.%m.%Y')) = YEAR(CURDATE()) AND MONTH(STR_TO_DATE(production_day, '%d.%m.%Y')) = MONTH(CURDATE())";
            Double aMonth = jdbcTemplate.queryForObject(sqlMonthA, Double.class);
            result.put(prefix + "_availability_month", aMonth != null ? aMonth : 0.0);
        }
        return result;
    }
}
