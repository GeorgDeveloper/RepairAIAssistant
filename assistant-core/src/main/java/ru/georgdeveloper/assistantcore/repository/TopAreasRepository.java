package ru.georgdeveloper.assistantcore.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Repository
public class TopAreasRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String categoryCaseExpr() {
        return "CASE " +
                "WHEN LOWER(failure_type) LIKE '%электрон%' THEN 'Электроника' " +
                "WHEN LOWER(failure_type) LIKE '%электрик%' OR LOWER(failure_type) LIKE '%эл.%' THEN 'Электрика' " +
                "WHEN LOWER(failure_type) LIKE '%мех%' THEN 'Механика' " +
                "ELSE 'Другие' END";
    }

    public List<Map<String, Object>> getTopAreas(String dateFrom, String dateTo, String week, String failureType, Integer limit) {
        StringBuilder sql = new StringBuilder();
        List<Object> params = new ArrayList<>();
        sql.append("SELECT area, ")
           .append("SUM(TIME_TO_SEC(machine_downtime))/3600 AS total_downtime_hours, ")
           .append("COUNT(*) AS failure_count ")
           .append("FROM equipment_maintenance_records WHERE 1=1 ");
        if (dateFrom != null && !dateFrom.isEmpty()) { sql.append("AND start_bd_t1 >= STR_TO_DATE(?, '%Y-%m-%d') "); params.add(dateFrom); }
        if (dateTo != null && !dateTo.isEmpty()) { sql.append("AND start_bd_t1 < DATE_ADD(STR_TO_DATE(?, '%Y-%m-%d'), INTERVAL 1 DAY) "); params.add(dateTo); }
        if (week != null && !week.equals("all") && !week.isEmpty()) { sql.append("AND WEEK(start_bd_t1, 1) = ? "); params.add(Integer.parseInt(week)); }
        if (failureType != null && !failureType.equals("all") && !failureType.isEmpty()) { sql.append("AND failure_type = ? "); params.add(failureType); }
        sql.append("GROUP BY area HAVING area IS NOT NULL AND TRIM(area) <> '' ORDER BY total_downtime_hours DESC ");
        if (limit == null || limit <= 0) limit = 30;
        sql.append("LIMIT ?"); params.add(limit);
        return jdbcTemplate.queryForList(sql.toString(), params.toArray());
    }

    public List<Map<String, Object>> getAreaTypeCategories(String area, String dateFrom, String dateTo, String week) {
        String expr = categoryCaseExpr();
        StringBuilder sql = new StringBuilder();
        List<Object> params = new ArrayList<>();
        sql.append("SELECT ").append(expr).append(" AS category, ")
           .append("SUM(TIME_TO_SEC(machine_downtime))/3600 AS total_downtime_hours, COUNT(*) AS failure_count ")
           .append("FROM equipment_maintenance_records WHERE area = ? ");
        params.add(area);
        if (dateFrom != null && !dateFrom.isEmpty()) { sql.append("AND start_bd_t1 >= STR_TO_DATE(?, '%Y-%m-%d') "); params.add(dateFrom); }
        if (dateTo != null && !dateTo.isEmpty()) { sql.append("AND start_bd_t1 < DATE_ADD(STR_TO_DATE(?, '%Y-%m-%d'), INTERVAL 1 DAY) "); params.add(dateTo); }
        if (week != null && !week.equals("all") && !week.isEmpty()) { sql.append("AND WEEK(start_bd_t1, 1) = ? "); params.add(Integer.parseInt(week)); }
        sql.append("GROUP BY ").append(expr).append(" ORDER BY total_downtime_hours DESC");
        return jdbcTemplate.queryForList(sql.toString(), params.toArray());
    }

    public List<Map<String, Object>> getAreaCategoryCauses(String area, String category, String dateFrom, String dateTo, String week) {
        String expr = categoryCaseExpr();
        StringBuilder sql = new StringBuilder();
        List<Object> params = new ArrayList<>();
        sql.append("SELECT TRIM(cause) AS cause, ")
           .append("SUM(TIME_TO_SEC(machine_downtime))/3600 AS total_downtime_hours, COUNT(*) AS failure_count ")
           .append("FROM equipment_maintenance_records WHERE area = ? AND ").append(expr).append(" = ? ");
        params.add(area); params.add(category);
        if (dateFrom != null && !dateFrom.isEmpty()) { sql.append("AND start_bd_t1 >= STR_TO_DATE(?, '%Y-%m-%d') "); params.add(dateFrom); }
        if (dateTo != null && !dateTo.isEmpty()) { sql.append("AND start_bd_t1 < DATE_ADD(STR_TO_DATE(?, '%Y-%m-%d'), INTERVAL 1 DAY) "); params.add(dateTo); }
        if (week != null && !week.equals("all") && !week.isEmpty()) { sql.append("AND WEEK(start_bd_t1, 1) = ? "); params.add(Integer.parseInt(week)); }
        sql.append("GROUP BY TRIM(cause) HAVING TRIM(cause) IS NOT NULL AND TRIM(cause) <> '' ORDER BY total_downtime_hours DESC");
        return jdbcTemplate.queryForList(sql.toString(), params.toArray());
    }

    public List<Map<String, Object>> getAreaCategoryEvents(String area, String category, String cause, String dateFrom, String dateTo, String week) {
        String expr = categoryCaseExpr();
        StringBuilder sql = new StringBuilder();
        List<Object> params = new ArrayList<>();
        sql.append("SELECT code, machine_downtime, comments, cause, start_bd_t1 ")
           .append("FROM equipment_maintenance_records WHERE area = ? AND ").append(expr).append(" = ? AND TRIM(cause) = ? ");
        params.add(area); params.add(category); params.add(cause);
        if (dateFrom != null && !dateFrom.isEmpty()) { sql.append("AND start_bd_t1 >= STR_TO_DATE(?, '%Y-%m-%d') "); params.add(dateFrom); }
        if (dateTo != null && !dateTo.isEmpty()) { sql.append("AND start_bd_t1 < DATE_ADD(STR_TO_DATE(?, '%Y-%m-%d'), INTERVAL 1 DAY) "); params.add(dateTo); }
        if (week != null && !week.equals("all") && !week.isEmpty()) { sql.append("AND WEEK(start_bd_t1, 1) = ? "); params.add(Integer.parseInt(week)); }
        sql.append("ORDER BY start_bd_t1 DESC LIMIT 500");
        return jdbcTemplate.queryForList(sql.toString(), params.toArray());
    }

    public List<Map<String, Object>> getWeeks() {
        String sql = "SELECT DISTINCT WEEK(start_bd_t1, 1) AS week_number FROM equipment_maintenance_records WHERE start_bd_t1 IS NOT NULL ORDER BY week_number DESC";
        return jdbcTemplate.queryForList(sql);
    }

    public List<Map<String, Object>> getFailureTypes() {
        String sql = "SELECT DISTINCT failure_type FROM equipment_maintenance_records WHERE failure_type IS NOT NULL AND TRIM(failure_type) <> '' ORDER BY failure_type";
        return jdbcTemplate.queryForList(sql);
    }
}
