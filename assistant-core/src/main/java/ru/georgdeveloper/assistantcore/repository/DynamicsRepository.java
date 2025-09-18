package ru.georgdeveloper.assistantcore.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Repository
public class DynamicsRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public List<Map<String, Object>> getDynamicsData(String year, String month, String week, String area, String equipment, String failureType) {
        StringBuilder sql = new StringBuilder();
        List<Object> params = new ArrayList<>();
        
        // Определяем уровень детализации
        if (week != null && !"all".equals(week)) {
            // По дням недели
            sql.append("SELECT DAY(start_bd_t1) as period_label, cause as failure_type, ")
               .append("SUM(TIME_TO_SEC(machine_downtime)/3600) as total_downtime, COUNT(*) as failure_count ")
               .append("FROM equipment_maintenance_records ")
               .append("WHERE YEAR(start_bd_t1) = ? AND MONTH(start_bd_t1) = ? AND WEEK(start_bd_t1, 1) = ? ");
            
            params.add(Integer.parseInt(year));
            params.add(Integer.parseInt(month));
            params.add(Integer.parseInt(week));
            
        } else if (month != null && !"all".equals(month)) {
            // По неделям месяца
            sql.append("SELECT WEEK(start_bd_t1, 1) as period_label, cause as failure_type, ")
               .append("SUM(TIME_TO_SEC(machine_downtime)/3600) as total_downtime, COUNT(*) as failure_count ")
               .append("FROM equipment_maintenance_records ")
               .append("WHERE YEAR(start_bd_t1) = ? AND MONTH(start_bd_t1) = ? ");
            
            params.add(Integer.parseInt(year));
            params.add(Integer.parseInt(month));
            
        } else if (year != null && !"all".equals(year)) {
            // По месяцам года
            sql.append("SELECT MONTH(start_bd_t1) as period_label, cause as failure_type, ")
               .append("SUM(TIME_TO_SEC(machine_downtime)/3600) as total_downtime, COUNT(*) as failure_count ")
               .append("FROM equipment_maintenance_records ")
               .append("WHERE YEAR(start_bd_t1) = ? ");
            
            params.add(Integer.parseInt(year));
            
        } else {
            // По годам
            sql.append("SELECT YEAR(start_bd_t1) as period_label, cause as failure_type, ")
               .append("SUM(TIME_TO_SEC(machine_downtime)/3600) as total_downtime, COUNT(*) as failure_count ")
               .append("FROM equipment_maintenance_records ")
               .append("WHERE 1=1 ");
        }
        
        if (area != null && !"all".equals(area)) {
            sql.append("AND area = ? ");
            params.add(area);
        }
        
        if (equipment != null && !"all".equals(equipment)) {
            sql.append("AND machine_name = ? ");
            params.add(equipment);
        }
        
        if (failureType != null && !"all".equals(failureType)) {
            sql.append("AND cause = ? ");
            params.add(failureType);
        }
        
        sql.append("GROUP BY period_label, cause ORDER BY period_label, cause");
        
        return jdbcTemplate.queryForList(sql.toString(), params.toArray());
    }

    public List<Map<String, Object>> getYears() {
        String sql = "SELECT DISTINCT YEAR(start_bd_t1) as year FROM equipment_maintenance_records ORDER BY year DESC";
        return jdbcTemplate.queryForList(sql);
    }

    public List<Map<String, Object>> getMonths(String year) {
        String sql = "SELECT DISTINCT MONTH(start_bd_t1) as month FROM equipment_maintenance_records WHERE YEAR(start_bd_t1) = ? ORDER BY month";
        return jdbcTemplate.queryForList(sql, Integer.parseInt(year));
    }

    public List<Map<String, Object>> getWeeks(String year, String month) {
        String sql = "SELECT DISTINCT WEEK(start_bd_t1, 1) as week FROM equipment_maintenance_records WHERE YEAR(start_bd_t1) = ? AND MONTH(start_bd_t1) = ? ORDER BY week";
        return jdbcTemplate.queryForList(sql, Integer.parseInt(year), Integer.parseInt(month));
    }

    public List<Map<String, Object>> getFailureTypes() {
        String sql = "SELECT DISTINCT TRIM(cause) as cause FROM equipment_maintenance_records WHERE cause IS NOT NULL AND TRIM(cause) != '' ORDER BY cause";
        return jdbcTemplate.queryForList(sql);
    }
}