package ru.georgdeveloper.assistantcore.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Repository
public class TopCausesRepository {

    @Autowired
    @Qualifier("sqlServerJdbcTemplate")
    private JdbcTemplate jdbcTemplate;

    public List<Map<String, Object>> getTopCauses(String dateFrom,
                                                  String dateTo,
                                                  String week,
                                                  String area,
                                                  String failureType,
                                                  String machine,
                                                  Integer limit) {
        StringBuilder sql = new StringBuilder();
        List<Object> params = new ArrayList<>();

        sql.append("SELECT TRIM(cause) AS cause, ")
           .append("SUM(TIME_TO_SEC(machine_downtime))/3600 AS total_downtime_hours, ")
           .append("COUNT(*) AS failure_count ")
           .append("FROM equipment_maintenance_records WHERE 1=1 ");

        if (dateFrom != null && !dateFrom.isEmpty()) {
            sql.append("AND start_bd_t1 >= STR_TO_DATE(?, '%Y-%m-%d') ");
            params.add(dateFrom);
        }
        if (dateTo != null && !dateTo.isEmpty()) {
            sql.append("AND start_bd_t1 < DATE_ADD(STR_TO_DATE(?, '%Y-%m-%d'), INTERVAL 1 DAY) ");
            params.add(dateTo);
        }
        if (week != null && !week.equals("all") && !week.isEmpty()) {
            sql.append("AND WEEK(start_bd_t1, 1) = ? ");
            params.add(Integer.parseInt(week));
        }
        if (area != null && !area.equals("all") && !area.isEmpty()) {
            sql.append("AND area = ? ");
            params.add(area);
        }
        if (failureType != null && !failureType.equals("all") && !failureType.isEmpty()) {
            sql.append("AND failure_type = ? ");
            params.add(failureType);
        }
        if (machine != null && !machine.equals("all") && !machine.isEmpty()) {
            sql.append("AND machine_name = ? ");
            params.add(machine);
        }

        sql.append("GROUP BY TRIM(cause) ")
           .append("HAVING TRIM(cause) IS NOT NULL AND TRIM(cause) <> '' ")
           .append("ORDER BY total_downtime_hours DESC ");

        if (limit == null || limit <= 0) {
            limit = 30;
        }
        sql.append("LIMIT ?");
        params.add(limit);

        return jdbcTemplate.queryForList(sql.toString(), params.toArray());
    }

    public List<Map<String, Object>> getProductionDays(String monthsBack) {
        // Return distinct dates for the recent period (default 2 months)
        int back = 2;
        try { if (monthsBack != null) back = Integer.parseInt(monthsBack); } catch (Exception ignored) {}
        String sql = "SELECT DISTINCT DATE(start_bd_t1) AS production_day " +
                "FROM equipment_maintenance_records " +
                "WHERE start_bd_t1 >= DATE_SUB(CURDATE(), INTERVAL ? MONTH) " +
                "ORDER BY production_day DESC";
        return jdbcTemplate.queryForList(sql, back);
    }

    public List<Map<String, Object>> getWeeks() {
        String sql = "SELECT DISTINCT WEEK(start_bd_t1, 1) AS week_number " +
                "FROM equipment_maintenance_records WHERE start_bd_t1 IS NOT NULL " +
                "ORDER BY week_number DESC";
        return jdbcTemplate.queryForList(sql);
    }

    public List<Map<String, Object>> getAreas() {
        String sql = "SELECT DISTINCT area FROM equipment_maintenance_records " +
                "WHERE area IS NOT NULL AND TRIM(area) <> '' ORDER BY area";
        return jdbcTemplate.queryForList(sql);
    }

    public List<Map<String, Object>> getFailureTypes() {
        String sql = "SELECT DISTINCT failure_type FROM equipment_maintenance_records " +
                "WHERE failure_type IS NOT NULL AND TRIM(failure_type) <> '' ORDER BY failure_type";
        return jdbcTemplate.queryForList(sql);
    }

    public List<Map<String, Object>> getMachines(String area) {
        StringBuilder sql = new StringBuilder("SELECT DISTINCT machine_name FROM equipment_maintenance_records WHERE machine_name IS NOT NULL AND TRIM(machine_name) <> '' ");
        List<Object> params = new ArrayList<>();
        if (area != null && !area.equals("all") && !area.isEmpty()) {
            sql.append("AND area = ? ");
            params.add(area);
        }
        sql.append("ORDER BY machine_name");
        return jdbcTemplate.queryForList(sql.toString(), params.toArray());
    }

    public List<Map<String, Object>> getCauseMachines(String cause,
                                                      String dateFrom,
                                                      String dateTo,
                                                      String week,
                                                      String area) {
        StringBuilder sql = new StringBuilder();
        List<Object> params = new ArrayList<>();
        sql.append("SELECT machine_name, ")
           .append("SUM(TIME_TO_SEC(machine_downtime))/3600 AS total_downtime_hours, ")
           .append("COUNT(*) AS failure_count ")
           .append("FROM equipment_maintenance_records WHERE TRIM(cause) = ? ");
        params.add(cause);
        if (dateFrom != null && !dateFrom.isEmpty()) {
            sql.append("AND start_bd_t1 >= STR_TO_DATE(?, '%Y-%m-%d') ");
            params.add(dateFrom);
        }
        if (dateTo != null && !dateTo.isEmpty()) {
            sql.append("AND start_bd_t1 < DATE_ADD(STR_TO_DATE(?, '%Y-%m-%d'), INTERVAL 1 DAY) ");
            params.add(dateTo);
        }
        if (week != null && !week.equals("all") && !week.isEmpty()) {
            sql.append("AND WEEK(start_bd_t1, 1) = ? ");
            params.add(Integer.parseInt(week));
        }
        if (area != null && !area.equals("all") && !area.isEmpty()) {
            sql.append("AND area = ? ");
            params.add(area);
        }
        sql.append("GROUP BY machine_name ORDER BY total_downtime_hours DESC");
        return jdbcTemplate.queryForList(sql.toString(), params.toArray());
    }

    public List<Map<String, Object>> getCauseMechanisms(String cause,
                                                        String machine,
                                                        String dateFrom,
                                                        String dateTo,
                                                        String week,
                                                        String area) {
        StringBuilder sql = new StringBuilder();
        List<Object> params = new ArrayList<>();
        sql.append("SELECT COALESCE(TRIM(mechanism_node), 'Не указан') AS mechanism_node, ")
           .append("SUM(TIME_TO_SEC(machine_downtime))/3600 AS total_downtime_hours, ")
           .append("COUNT(*) AS failure_count ")
           .append("FROM equipment_maintenance_records WHERE TRIM(cause) = ? AND machine_name = ? ");
        params.add(cause);
        params.add(machine);
        if (dateFrom != null && !dateFrom.isEmpty()) {
            sql.append("AND start_bd_t1 >= STR_TO_DATE(?, '%Y-%m-%d') ");
            params.add(dateFrom);
        }
        if (dateTo != null && !dateTo.isEmpty()) {
            sql.append("AND start_bd_t1 < DATE_ADD(STR_TO_DATE(?, '%Y-%m-%d'), INTERVAL 1 DAY) ");
            params.add(dateTo);
        }
        if (week != null && !week.equals("all") && !week.isEmpty()) {
            sql.append("AND WEEK(start_bd_t1, 1) = ? ");
            params.add(Integer.parseInt(week));
        }
        if (area != null && !area.equals("all") && !area.isEmpty()) {
            sql.append("AND area = ? ");
            params.add(area);
        }
        sql.append("GROUP BY mechanism_node ORDER BY total_downtime_hours DESC");
        return jdbcTemplate.queryForList(sql.toString(), params.toArray());
    }

    public List<Map<String, Object>> getCauseEvents(String cause,
                                                    String machine,
                                                    String mechanism,
                                                    String dateFrom,
                                                    String dateTo,
                                                    String week,
                                                    String area) {
        StringBuilder sql = new StringBuilder();
        List<Object> params = new ArrayList<>();
        sql.append("SELECT code, machine_downtime, comments, cause, start_bd_t1 ")
           .append("FROM equipment_maintenance_records WHERE TRIM(cause) = ? AND machine_name = ? ");
        params.add(cause);
        params.add(machine);
        if (mechanism != null && !mechanism.isEmpty()) {
            sql.append("AND COALESCE(TRIM(mechanism_node), 'Не указан') = ? ");
            params.add(mechanism);
        }
        if (dateFrom != null && !dateFrom.isEmpty()) {
            sql.append("AND start_bd_t1 >= STR_TO_DATE(?, '%Y-%m-%d') ");
            params.add(dateFrom);
        }
        if (dateTo != null && !dateTo.isEmpty()) {
            sql.append("AND start_bd_t1 < DATE_ADD(STR_TO_DATE(?, '%Y-%m-%d'), INTERVAL 1 DAY) ");
            params.add(dateTo);
        }
        if (week != null && !week.equals("all") && !week.isEmpty()) {
            sql.append("AND WEEK(start_bd_t1, 1) = ? ");
            params.add(Integer.parseInt(week));
        }
        if (area != null && !area.equals("all") && !area.isEmpty()) {
            sql.append("AND area = ? ");
            params.add(area);
        }
        sql.append("ORDER BY start_bd_t1 DESC LIMIT 500");
        return jdbcTemplate.queryForList(sql.toString(), params.toArray());
    }
}


