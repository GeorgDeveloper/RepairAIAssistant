package ru.georgdeveloper.assistantcore.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Repository
public class TopEquipmentRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public List<Map<String, Object>> getTopEquipment(String dateFrom,
                                                     String dateTo,
                                                     String week,
                                                     String area,
                                                     String failureType,
                                                     Integer limit) {
        StringBuilder sql = new StringBuilder();
        List<Object> params = new ArrayList<>();

        sql.append("SELECT machine_name, ")
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

        sql.append("GROUP BY machine_name ")
           .append("HAVING machine_name IS NOT NULL AND TRIM(machine_name) <> '' ")
           .append("ORDER BY total_downtime_hours DESC ");

        if (limit == null || limit <= 0) {
            limit = 30;
        }
        sql.append("LIMIT ?");
        params.add(limit);

        return jdbcTemplate.queryForList(sql.toString(), params.toArray());
    }

    public List<Map<String, Object>> getEquipmentCauses(String machine,
                                                        String dateFrom,
                                                        String dateTo,
                                                        String week,
                                                        String area,
                                                        String failureType) {
        StringBuilder sql = new StringBuilder();
        List<Object> params = new ArrayList<>();
        sql.append("SELECT TRIM(cause) AS cause, ")
           .append("SUM(TIME_TO_SEC(machine_downtime))/3600 AS total_downtime_hours, ")
           .append("COUNT(*) AS failure_count ")
           .append("FROM equipment_maintenance_records WHERE machine_name = ? ");
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
        if (failureType != null && !failureType.equals("all") && !failureType.isEmpty()) {
            sql.append("AND TRIM(failure_type) = ? ");
            params.add(failureType);
        }
        sql.append("GROUP BY TRIM(cause) ")
           .append("HAVING TRIM(cause) IS NOT NULL AND TRIM(cause) <> '' ")
           .append("ORDER BY total_downtime_hours DESC");
        return jdbcTemplate.queryForList(sql.toString(), params.toArray());
    }

    public List<Map<String, Object>> getEquipmentMechanisms(String machine,
                                                            String cause,
                                                            String dateFrom,
                                                            String dateTo,
                                                            String week,
                                                            String area,
                                                            String failureType) {
        StringBuilder sql = new StringBuilder();
        List<Object> params = new ArrayList<>();
        sql.append("SELECT COALESCE(TRIM(mechanism_node), 'Не указан') AS mechanism_node, ")
           .append("SUM(TIME_TO_SEC(machine_downtime))/3600 AS total_downtime_hours, ")
           .append("COUNT(*) AS failure_count ")
           .append("FROM equipment_maintenance_records WHERE machine_name = ? AND TRIM(cause) = ? ");
        params.add(machine);
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
        if (failureType != null && !failureType.equals("all") && !failureType.isEmpty()) {
            sql.append("AND TRIM(failure_type) = ? ");
            params.add(failureType);
        }
        sql.append("GROUP BY mechanism_node ORDER BY total_downtime_hours DESC");
        return jdbcTemplate.queryForList(sql.toString(), params.toArray());
    }

    public List<Map<String, Object>> getEquipmentEvents(String machine,
                                                        String cause,
                                                        String mechanism,
                                                        String dateFrom,
                                                        String dateTo,
                                                        String week,
                                                        String area,
                                                        String failureType) {
        StringBuilder sql = new StringBuilder();
        List<Object> params = new ArrayList<>();
        sql.append("SELECT code, machine_downtime, comments, cause, start_bd_t1 ")
           .append("FROM equipment_maintenance_records WHERE machine_name = ? AND TRIM(cause) = ? ");
        params.add(machine);
        params.add(cause);
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
        if (failureType != null && !failureType.equals("all") && !failureType.isEmpty()) {
            sql.append("AND TRIM(failure_type) = ? ");
            params.add(failureType);
        }
        sql.append("ORDER BY start_bd_t1 DESC LIMIT 500");
        return jdbcTemplate.queryForList(sql.toString(), params.toArray());
    }

    /**
     * Получение всех событий для оборудования за период с опциональным фильтром по типу поломки
     * Без фильтрации по cause и mechanism - возвращает все события
     */
    public List<Map<String, Object>> getEquipmentAllEvents(String machine,
                                                           String dateFrom,
                                                           String dateTo,
                                                           String week,
                                                           String area,
                                                           String failureType) {
        StringBuilder sql = new StringBuilder();
        List<Object> params = new ArrayList<>();
        sql.append("SELECT code, machine_downtime, comments, cause, start_bd_t1, failure_type ")
           .append("FROM equipment_maintenance_records WHERE machine_name = ? ");
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
        if (failureType != null && !failureType.equals("all") && !failureType.isEmpty()) {
            sql.append("AND TRIM(failure_type) = ? ");
            params.add(failureType);
        }
        
        sql.append("ORDER BY start_bd_t1 DESC LIMIT 500");
        return jdbcTemplate.queryForList(sql.toString(), params.toArray());
    }

    /**
     * Получение детализации нарядов для конкретной даты и области
     */
    public List<Map<String, Object>> getBreakdownDetailsForDateAndArea(String date, String area) {
        StringBuilder sql = new StringBuilder();
        List<Object> params = new ArrayList<>();
        
        sql.append("SELECT code, machine_name, mechanism_node, failure_type, status, ")
           .append("machine_downtime, start_bd_t1, stop_bd_t4, cause, comments ")
           .append("FROM equipment_maintenance_records WHERE 1=1 ");
        
        // Фильтр по дате - используем production_day, если он заполнен, иначе fallback на start_bd_t1
        if (date != null && !date.isEmpty()) {
            sql.append("AND (production_day = ? OR (production_day IS NULL OR production_day = '') AND DATE(start_bd_t1) = STR_TO_DATE(?, '%d.%m.%Y')) ");
            params.add(date);
            params.add(date);
        }
        
        // Фильтр по области
        if (area != null && !area.isEmpty() && !area.equals("all")) {
            if ("Modules".equals(area)) {
                // Для области "Модули" ищем по маске "Module" в machine_name
                sql.append("AND machine_name LIKE '%Module%' ");
            } else {
                // Для остальных областей используем поле area
                sql.append("AND area = ? ");
                params.add(area);
            }
        }
        
        sql.append("ORDER BY start_bd_t1 DESC");
        
        return jdbcTemplate.queryForList(sql.toString(), params.toArray());
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
}
