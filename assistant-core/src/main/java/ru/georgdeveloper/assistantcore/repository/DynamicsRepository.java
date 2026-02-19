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

    public List<Map<String, Object>> getDynamicsData(List<String> year, List<String> month, List<String> week, List<String> area, List<String> equipment, List<String> failureType) {
        StringBuilder sql = new StringBuilder();
        List<Object> params = new ArrayList<>();
        
        // Определяем уровень детализации
        if (week != null && !week.isEmpty() && !week.contains("all") && 
            month != null && !month.isEmpty() && !month.contains("all") &&
            year != null && !year.isEmpty() && !year.contains("all")) {
            // По дням недели
            sql.append("SELECT DAY(start_bd_t1) as period_label, cause as failure_type, ")
               .append("SUM(TIME_TO_SEC(machine_downtime)/3600) as total_downtime, COUNT(*) as failure_count ")
               .append("FROM equipment_maintenance_records ")
               .append("WHERE YEAR(start_bd_t1) IN (").append(buildInClause(year.size())).append(") ")
               .append("AND MONTH(start_bd_t1) IN (").append(buildInClause(month.size())).append(") ")
               .append("AND WEEK(start_bd_t1, 1) IN (").append(buildInClause(week.size())).append(") ");
            
            for (String y : year) params.add(Integer.parseInt(y));
            for (String m : month) params.add(Integer.parseInt(m));
            for (String w : week) params.add(Integer.parseInt(w));
            
        } else if (month != null && !month.isEmpty() && !month.contains("all") &&
                   year != null && !year.isEmpty() && !year.contains("all")) {
            // По неделям месяца
            sql.append("SELECT WEEK(start_bd_t1, 1) as period_label, cause as failure_type, ")
               .append("SUM(TIME_TO_SEC(machine_downtime)/3600) as total_downtime, COUNT(*) as failure_count ")
               .append("FROM equipment_maintenance_records ")
               .append("WHERE YEAR(start_bd_t1) IN (").append(buildInClause(year.size())).append(") ")
               .append("AND MONTH(start_bd_t1) IN (").append(buildInClause(month.size())).append(") ");
            
            for (String y : year) params.add(Integer.parseInt(y));
            for (String m : month) params.add(Integer.parseInt(m));
            
        } else if (year != null && !year.isEmpty() && !year.contains("all")) {
            // По месяцам года
            sql.append("SELECT MONTH(start_bd_t1) as period_label, cause as failure_type, ")
               .append("SUM(TIME_TO_SEC(machine_downtime)/3600) as total_downtime, COUNT(*) as failure_count ")
               .append("FROM equipment_maintenance_records ")
               .append("WHERE YEAR(start_bd_t1) IN (").append(buildInClause(year.size())).append(") ");
            
            for (String y : year) params.add(Integer.parseInt(y));
            
        } else {
            // По годам
            sql.append("SELECT YEAR(start_bd_t1) as period_label, cause as failure_type, ")
               .append("SUM(TIME_TO_SEC(machine_downtime)/3600) as total_downtime, COUNT(*) as failure_count ")
               .append("FROM equipment_maintenance_records ")
               .append("WHERE 1=1 ");
        }
        
        if (area != null && !area.isEmpty() && !area.contains("all")) {
            sql.append("AND area IN (").append(buildInClause(area.size())).append(") ");
            params.addAll(area);
        }
        
        if (equipment != null && !equipment.isEmpty() && !equipment.contains("all")) {
            sql.append("AND machine_name IN (").append(buildInClause(equipment.size())).append(") ");
            params.addAll(equipment);
        }
        
        if (failureType != null && !failureType.isEmpty() && !failureType.contains("all")) {
            sql.append("AND cause IN (").append(buildInClause(failureType.size())).append(") ");
            params.addAll(failureType);
        }
        
        sql.append("GROUP BY period_label, cause ORDER BY period_label, cause");
        
        return jdbcTemplate.queryForList(sql.toString(), params.toArray());
    }

    public List<Map<String, Object>> getYears() {
        String sql = "SELECT DISTINCT YEAR(start_bd_t1) as year FROM equipment_maintenance_records ORDER BY year DESC";
        return jdbcTemplate.queryForList(sql);
    }

    public List<Map<String, Object>> getMonths(List<String> year) {
        if (year == null || year.isEmpty() || year.contains("all")) {
            String sql = "SELECT DISTINCT MONTH(start_bd_t1) as month FROM equipment_maintenance_records ORDER BY month";
            return jdbcTemplate.queryForList(sql);
        }
        String sql = "SELECT DISTINCT MONTH(start_bd_t1) as month FROM equipment_maintenance_records WHERE YEAR(start_bd_t1) IN (" + buildInClause(year.size()) + ") ORDER BY month";
        Object[] params = year.stream().map(Integer::parseInt).toArray();
        return jdbcTemplate.queryForList(sql, params);
    }

    public List<Map<String, Object>> getWeeks(List<String> year, List<String> month) {
        if (year == null || year.isEmpty() || year.contains("all") || 
            month == null || month.isEmpty() || month.contains("all")) {
            String sql = "SELECT DISTINCT WEEK(start_bd_t1, 1) as week FROM equipment_maintenance_records ORDER BY week";
            return jdbcTemplate.queryForList(sql);
        }
        String sql = "SELECT DISTINCT WEEK(start_bd_t1, 1) as week FROM equipment_maintenance_records WHERE YEAR(start_bd_t1) IN (" + buildInClause(year.size()) + ") AND MONTH(start_bd_t1) IN (" + buildInClause(month.size()) + ") ORDER BY week";
        List<Object> params = new ArrayList<>();
        for (String y : year) params.add(Integer.parseInt(y));
        for (String m : month) params.add(Integer.parseInt(m));
        return jdbcTemplate.queryForList(sql, params.toArray());
    }
    
    private String buildInClause(int size) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < size; i++) {
            if (i > 0) sb.append(",");
            sb.append("?");
        }
        return sb.toString();
    }

    public List<Map<String, Object>> getFailureTypes() {
        String sql = "SELECT DISTINCT TRIM(cause) as cause FROM equipment_maintenance_records WHERE cause IS NOT NULL AND TRIM(cause) != '' ORDER BY cause";
        return jdbcTemplate.queryForList(sql);
    }

    /**
     * Получает данные динамики по типам поломок (failure_type) с группировкой по месяцам
     * Возвращает время простоя в секундах и количество случаев
     */
    public List<Map<String, Object>> getDynamicsByFailureType(List<String> year, List<String> month, List<String> week, List<String> area, List<String> equipment) {
        StringBuilder sql = new StringBuilder();
        List<Object> params = new ArrayList<>();
        
        // Определяем уровень детализации
        if (week != null && !week.isEmpty() && !week.contains("all") && 
            month != null && !month.isEmpty() && !month.contains("all") &&
            year != null && !year.isEmpty() && !year.contains("all")) {
            // По дням недели
            // Если выбрано несколько месяцев или годов, возвращаем их для сравнения
            if (year.size() > 1 || month.size() > 1) {
                sql.append("SELECT DAY(start_bd_t1) as period_label, ")
                   .append("YEAR(start_bd_t1) as year, ")
                   .append("MONTH(start_bd_t1) as month, ")
                   .append("TRIM(failure_type) as failure_type, ")
                   .append("SUM(TIME_TO_SEC(machine_downtime)) as total_downtime_seconds, ")
                   .append("COUNT(*) as failure_count ")
                   .append("FROM equipment_maintenance_records ")
                   .append("WHERE YEAR(start_bd_t1) IN (").append(buildInClause(year.size())).append(") ")
                   .append("AND MONTH(start_bd_t1) IN (").append(buildInClause(month.size())).append(") ")
                   .append("AND WEEK(start_bd_t1, 1) IN (").append(buildInClause(week.size())).append(") ")
                   .append("AND failure_type IS NOT NULL AND TRIM(failure_type) != '' ");
            } else {
                sql.append("SELECT DAY(start_bd_t1) as period_label, ")
                   .append("TRIM(failure_type) as failure_type, ")
                   .append("SUM(TIME_TO_SEC(machine_downtime)) as total_downtime_seconds, ")
                   .append("COUNT(*) as failure_count ")
                   .append("FROM equipment_maintenance_records ")
                   .append("WHERE YEAR(start_bd_t1) IN (").append(buildInClause(year.size())).append(") ")
                   .append("AND MONTH(start_bd_t1) IN (").append(buildInClause(month.size())).append(") ")
                   .append("AND WEEK(start_bd_t1, 1) IN (").append(buildInClause(week.size())).append(") ")
                   .append("AND failure_type IS NOT NULL AND TRIM(failure_type) != '' ");
            }
            
            for (String y : year) params.add(Integer.parseInt(y));
            for (String m : month) params.add(Integer.parseInt(m));
            for (String w : week) params.add(Integer.parseInt(w));
            
        } else if (month != null && !month.isEmpty() && !month.contains("all") &&
                   year != null && !year.isEmpty() && !year.contains("all")) {
            // По неделям месяца
            // Если выбрано несколько месяцев или годов, возвращаем их для сравнения
            if (year.size() > 1 || month.size() > 1) {
                sql.append("SELECT WEEK(start_bd_t1, 1) as period_label, ")
                   .append("YEAR(start_bd_t1) as year, ")
                   .append("MONTH(start_bd_t1) as month, ")
                   .append("TRIM(failure_type) as failure_type, ")
                   .append("SUM(TIME_TO_SEC(machine_downtime)) as total_downtime_seconds, ")
                   .append("COUNT(*) as failure_count ")
                   .append("FROM equipment_maintenance_records ")
                   .append("WHERE YEAR(start_bd_t1) IN (").append(buildInClause(year.size())).append(") ")
                   .append("AND MONTH(start_bd_t1) IN (").append(buildInClause(month.size())).append(") ")
                   .append("AND failure_type IS NOT NULL AND TRIM(failure_type) != '' ");
            } else {
                sql.append("SELECT WEEK(start_bd_t1, 1) as period_label, ")
                   .append("TRIM(failure_type) as failure_type, ")
                   .append("SUM(TIME_TO_SEC(machine_downtime)) as total_downtime_seconds, ")
                   .append("COUNT(*) as failure_count ")
                   .append("FROM equipment_maintenance_records ")
                   .append("WHERE YEAR(start_bd_t1) IN (").append(buildInClause(year.size())).append(") ")
                   .append("AND MONTH(start_bd_t1) IN (").append(buildInClause(month.size())).append(") ")
                   .append("AND failure_type IS NOT NULL AND TRIM(failure_type) != '' ");
            }
            
            for (String y : year) params.add(Integer.parseInt(y));
            for (String m : month) params.add(Integer.parseInt(m));
            
        } else if (year != null && !year.isEmpty() && !year.contains("all")) {
            // По месяцам года (основной режим для отображения как на скриншоте)
            // Если выбрано несколько годов, возвращаем год для сравнения
            if (year.size() > 1) {
                sql.append("SELECT MONTH(start_bd_t1) as period_label, ")
                   .append("YEAR(start_bd_t1) as year, ")
                   .append("TRIM(failure_type) as failure_type, ")
                   .append("SUM(TIME_TO_SEC(machine_downtime)) as total_downtime_seconds, ")
                   .append("COUNT(*) as failure_count ")
                   .append("FROM equipment_maintenance_records ")
                   .append("WHERE YEAR(start_bd_t1) IN (").append(buildInClause(year.size())).append(") ")
                   .append("AND failure_type IS NOT NULL AND TRIM(failure_type) != '' ");
            } else {
                sql.append("SELECT MONTH(start_bd_t1) as period_label, ")
                   .append("TRIM(failure_type) as failure_type, ")
                   .append("SUM(TIME_TO_SEC(machine_downtime)) as total_downtime_seconds, ")
                   .append("COUNT(*) as failure_count ")
                   .append("FROM equipment_maintenance_records ")
                   .append("WHERE YEAR(start_bd_t1) IN (").append(buildInClause(year.size())).append(") ")
                   .append("AND failure_type IS NOT NULL AND TRIM(failure_type) != '' ");
            }
            
            for (String y : year) params.add(Integer.parseInt(y));
        } else {
            // По годам, если год не выбран
            sql.append("SELECT YEAR(start_bd_t1) as period_label, ")
               .append("TRIM(failure_type) as failure_type, ")
               .append("SUM(TIME_TO_SEC(machine_downtime)) as total_downtime_seconds, ")
               .append("COUNT(*) as failure_count ")
               .append("FROM equipment_maintenance_records ")
               .append("WHERE failure_type IS NOT NULL AND TRIM(failure_type) != '' ");
        }
        
        if (area != null && !area.isEmpty() && !area.contains("all")) {
            sql.append("AND area IN (").append(buildInClause(area.size())).append(") ");
            params.addAll(area);
        }
        
        if (equipment != null && !equipment.isEmpty() && !equipment.contains("all")) {
            sql.append("AND machine_name IN (").append(buildInClause(equipment.size())).append(") ");
            params.addAll(equipment);
        }
        
        // Группировка зависит от того, выбраны ли несколько годов/месяцев и какие поля в SELECT
        // Проверяем, включен ли month в SELECT (когда выбраны месяц и год для сравнения)
        boolean hasMonthInSelect = month != null && !month.isEmpty() && !month.contains("all") &&
                                   year != null && !year.isEmpty() && !year.contains("all") &&
                                   (year.size() > 1 || month.size() > 1);
        boolean hasYearInSelect = year != null && !year.isEmpty() && !year.contains("all") && year.size() > 1;
        
        if (hasMonthInSelect) {
            // Если month в SELECT, он должен быть в GROUP BY
            sql.append("GROUP BY period_label, year, month, failure_type ORDER BY year, month, period_label, failure_type");
        } else if (hasYearInSelect) {
            // Если только year в SELECT (несколько годов, но не месяц)
            sql.append("GROUP BY period_label, year, failure_type ORDER BY year, period_label, failure_type");
        } else {
            // Обычная группировка без сравнения
            sql.append("GROUP BY period_label, failure_type ORDER BY period_label, failure_type");
        }
        
        return jdbcTemplate.queryForList(sql.toString(), params.toArray());
    }

    /**
     * Получает список типов поломок из колонки failure_type
     */
    public List<Map<String, Object>> getFailureTypesFromColumn() {
        String sql = "SELECT DISTINCT TRIM(failure_type) as failure_type FROM equipment_maintenance_records WHERE failure_type IS NOT NULL AND TRIM(failure_type) != '' ORDER BY failure_type";
        return jdbcTemplate.queryForList(sql);
    }
}