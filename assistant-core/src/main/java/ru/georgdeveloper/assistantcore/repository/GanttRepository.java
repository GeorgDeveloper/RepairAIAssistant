package ru.georgdeveloper.assistantcore.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public class GanttRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public List<Map<String, Object>> getGanttData(String dateFrom, String dateTo, String area, 
                                                  String equipment, String failureType, String status) {
        
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ");
        sql.append("machine_name, ");
        sql.append("CASE ");
        sql.append("  WHEN machine_name LIKE 'ATP%' OR machine_name LIKE 'VMI -%' THEN 'BuildingArea' ");
        sql.append("  WHEN machine_name LIKE 'HFV2%' THEN 'CuringArea' ");
        sql.append("  WHEN machine_name IN ('Bandina - 01', 'Bartell Bead Machine - 01', 'Calemard 1st stage - 01', ");
        sql.append("                        'Calender Complex Berstorf - 01', 'CMP APEX - 01', 'CMP APEX - 02', ");
        sql.append("                        'CMP APEX - 03', 'Duplex - 01', 'Low Angle - 01', 'Trafila Quadruplex - 01', ");
        sql.append("                        'TTM fisher belt cutting - 01', 'VMI APEX - 01', 'VMI APEX - 02', ");
        sql.append("                        'VMI TPCS 1600-1000') THEN 'SemifinishingArea' ");
        sql.append("  WHEN machine_name LIKE '%visual control%' OR machine_name LIKE 'A-TEC%' OR ");
        sql.append("       machine_name IN ('Matteuzzi RRM 50T', 'Module A-1', 'Module A-2', 'Module A-3', 'Saivatori') THEN 'FinishigArea' ");
        sql.append("  WHEN machine_name IN ('Automatic Chemistry Dosing System', 'Calender Comerio Ercole - 01', ");
        sql.append("                        'Grinding manual', 'Intake raw materials', 'Isolation solution', ");
        sql.append("                        'Manual dosing', 'Mixer GK 270 T-C 2.1', 'Mixer GK 320 E 1.1') THEN 'NewMixingArea' ");
        sql.append("  ELSE 'Other' ");
        sql.append("END as area, ");
        sql.append("failure_type, ");
        sql.append("start_bd_t1, ");
        sql.append("stop_bd_t4, ");
        sql.append("machine_downtime, ");
        sql.append("status, ");
        sql.append("comments ");
        sql.append("FROM equipment_maintenance_records ");
        sql.append("WHERE 1=1 ");

        // Добавляем фильтры
        if (dateFrom != null && !dateFrom.isEmpty()) {
            sql.append("AND start_bd_t1 >= ? ");
        }
        if (dateTo != null && !dateTo.isEmpty()) {
            sql.append("AND start_bd_t1 <= ? ");
        }
        if (equipment != null && !equipment.isEmpty() && !equipment.equals("all")) {
            sql.append("AND machine_name = ? ");
        }
        if (failureType != null && !failureType.isEmpty() && !failureType.equals("all")) {
            sql.append("AND failure_type = ? ");
        }
        if (status != null && !status.isEmpty() && !status.equals("all")) {
            sql.append("AND status = ? ");
        }

        sql.append("ORDER BY start_bd_t1 DESC ");
        // sql.append("LIMIT 1000");

        // Подготавливаем параметры
        Object[] params = buildParams(dateFrom, dateTo, area, equipment, failureType, status);
        
        return jdbcTemplate.queryForList(sql.toString(), params);
    }

    private Object[] buildParams(String dateFrom, String dateTo, String area, 
                                String equipment, String failureType, String status) {
        java.util.List<Object> params = new java.util.ArrayList<>();
        
        if (dateFrom != null && !dateFrom.isEmpty()) {
            params.add(dateFrom);
        }
        if (dateTo != null && !dateTo.isEmpty()) {
            params.add(dateTo);
        }
        if (equipment != null && !equipment.isEmpty() && !equipment.equals("all")) {
            params.add(equipment);
        }
        if (failureType != null && !failureType.isEmpty() && !failureType.equals("all")) {
            params.add(failureType);
        }
        if (status != null && !status.isEmpty() && !status.equals("all")) {
            params.add(status);
        }
        
        return params.toArray();
    }

    public List<Map<String, Object>> getEquipmentByArea(String area) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT DISTINCT machine_name FROM equipment_maintenance_records ");
        sql.append("WHERE machine_name IS NOT NULL AND machine_name != '' ");
        
        if (area != null && !area.equals("all")) {
            sql.append("AND (");
            switch (area) {
                case "BuildingArea":
                    sql.append("machine_name LIKE 'ATP%' OR machine_name LIKE 'VMI -%'");
                    break;
                case "CuringArea":
                    sql.append("machine_name LIKE 'HFV2%'");
                    break;
                case "SemifinishingArea":
                    sql.append("machine_name IN ('Bandina - 01', 'Bartell Bead Machine - 01', 'Calemard 1st stage - 01', ");
                    sql.append("'Calender Complex Berstorf - 01', 'CMP APEX - 01', 'CMP APEX - 02', 'CMP APEX - 03', ");
                    sql.append("'Duplex - 01', 'Low Angle - 01', 'Trafila Quadruplex - 01', 'TTM fisher belt cutting - 01', ");
                    sql.append("'VMI APEX - 01', 'VMI APEX - 02', 'VMI TPCS 1600-1000')");
                    break;
                case "FinishigArea":
                    sql.append("machine_name LIKE '%visual control%' OR machine_name LIKE 'A-TEC%' OR ");
                    sql.append("machine_name IN ('Matteuzzi RRM 50T', 'Module A-1', 'Module A-2', 'Module A-3', 'Saivatori')");
                    break;
                case "NewMixingArea":
                    sql.append("machine_name IN ('Automatic Chemistry Dosing System', 'Calender Comerio Ercole - 01', ");
                    sql.append("'Grinding manual', 'Intake raw materials', 'Isolation solution', 'Manual dosing', ");
                    sql.append("'Mixer GK 270 T-C 2.1', 'Mixer GK 320 E 1.1')");
                    break;
            }
            sql.append(") ");
        }
        
        sql.append("ORDER BY machine_name");
        return jdbcTemplate.queryForList(sql.toString());
    }

    public List<Map<String, Object>> getAreas() {
        String sql = "SELECT DISTINCT " +
                    "CASE " +
                    "  WHEN machine_name LIKE 'ATP%' OR machine_name LIKE 'VMI -%' THEN 'BuildingArea' " +
                    "  WHEN machine_name LIKE 'HFV2%' THEN 'CuringArea' " +
                    "  WHEN machine_name IN ('Bandina - 01', 'Bartell Bead Machine - 01', 'Calemard 1st stage - 01', " +
                    "                        'Calender Complex Berstorf - 01', 'CMP APEX - 01', 'CMP APEX - 02', " +
                    "                        'CMP APEX - 03', 'Duplex - 01', 'Low Angle - 01', 'Trafila Quadruplex - 01', " +
                    "                        'TTM fisher belt cutting - 01', 'VMI APEX - 01', 'VMI APEX - 02', " +
                    "                        'VMI TPCS 1600-1000') THEN 'SemifinishingArea' " +
                    "  WHEN machine_name LIKE '%visual control%' OR machine_name LIKE 'A-TEC%' OR " +
                    "       machine_name IN ('Matteuzzi RRM 50T', 'Module A-1', 'Module A-2', 'Module A-3', 'Saivatori') THEN 'FinishigArea' " +
                    "  WHEN machine_name IN ('Automatic Chemistry Dosing System', 'Calender Comerio Ercole - 01', " +
                    "                        'Grinding manual', 'Intake raw materials', 'Isolation solution', " +
                    "                        'Manual dosing', 'Mixer GK 270 T-C 2.1', 'Mixer GK 320 E 1.1') THEN 'NewMixingArea' " +
                    "  ELSE 'Other' " +
                    "END as area " +
                    "FROM equipment_maintenance_records " +
                    "WHERE machine_name IS NOT NULL " +
                    "ORDER BY area";
        
        return jdbcTemplate.queryForList(sql);
    }

    public List<Map<String, Object>> getFailureTypes() {
        String sql = "SELECT DISTINCT failure_type FROM equipment_maintenance_records " +
                    "WHERE failure_type IS NOT NULL AND failure_type != '' " +
                    "ORDER BY failure_type";
        return jdbcTemplate.queryForList(sql);
    }

    public List<Map<String, Object>> getStatuses() {
        String sql = "SELECT DISTINCT status FROM equipment_maintenance_records " +
                    "WHERE status IS NOT NULL AND status != '' " +
                    "ORDER BY status";
        return jdbcTemplate.queryForList(sql);
    }
}