package ru.georgdeveloper.assistantcore.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Repository
public class MonitoringRepository {
    // Топ-5 поломок за неделю (общее)
    public List<Map<String, Object>> getTopBreakdownsPerWeek() {
        // Используем режим 3 для WEEK, чтобы соответствовать расчету week_number в БД
        // Режим 3: неделя начинается с понедельника, неделя 1 - первая неделя с 4+ днями в новом году
        // Используем прямое сравнение week_number с расчетом недели из start_bd_t1 для надежности
        String sql = "SELECT machine_name, SUM(TIME_TO_SEC(machine_downtime)) AS machine_downtime_seconds " +
                "FROM equipment_maintenance_records " +
                "WHERE week_number = WEEK(CURDATE(), 3) " +
                "AND YEAR(start_bd_t1) = YEAR(CURDATE()) " +
                "AND failure_type <> 'Другие' " +
                "GROUP BY machine_name " +
                "ORDER BY SUM(TIME_TO_SEC(machine_downtime)) DESC " +
                "LIMIT 4";
        return jdbcTemplate.queryForList(sql);
    }

    // Топ-5 поломок за неделю по ключевым линиям (используем шаблоны по именам)
    public List<Map<String, Object>> getTopBreakdownsPerWeekKeyLines() {
        // Используем режим 3 для WEEK, чтобы соответствовать расчету week_number в БД
        // Режим 3: неделя начинается с понедельника, неделя 1 - первая неделя с 4+ днями в новом году
        // Используем прямое сравнение week_number с расчетом недели из start_bd_t1 для надежности
        String sql = "SELECT machine_name, SUM(TIME_TO_SEC(machine_downtime)) AS machine_downtime_seconds " +
                "FROM equipment_maintenance_records " +
                "WHERE week_number = WEEK(CURDATE(), 3) " +
                "AND YEAR(start_bd_t1) = YEAR(CURDATE()) " +
                "AND failure_type <> 'Другие' " +
                "AND machine_name IN (" +
                "'Mixer GK 270 T-C 2.1', 'Mixer GK 320 E 1.1', " +
                "'Calender Complex Berstorf - 01', 'Bandina - 01', 'Duplex - 01', " +
                "'Calender Comerio Ercole - 01', 'VMI APEX - 01', 'VMI APEX - 02', " +
                "'Trafila Quadruplex - 01', 'Bartell Bead Machine - 01', " +
                "'TTM fisher belt cutting - 01', 'VMI TPCS 1600-1000'" +
                ") " +
                "GROUP BY machine_name " +
                "ORDER BY SUM(TIME_TO_SEC(machine_downtime)) DESC " +
                "LIMIT 4";
        return jdbcTemplate.queryForList(sql);
    }

    // Топ-5 поломок за месяц (общее)
    public List<Map<String, Object>> getTopBreakdownsPerMonth(Integer year, Integer month) {
        String sql;
        if (year != null && month != null) {
            sql = "SELECT machine_name, SUM(TIME_TO_SEC(machine_downtime)) AS machine_downtime_seconds " +
                    "FROM equipment_maintenance_records " +
                    "WHERE YEAR(start_bd_t1) = ? " +
                    "AND MONTH(start_bd_t1) = ? " +
                    "AND failure_type <> 'Другие' " +
                    "GROUP BY machine_name " +
                    "ORDER BY SUM(TIME_TO_SEC(machine_downtime)) DESC " +
                    "LIMIT 4";
            return jdbcTemplate.queryForList(sql, year, month);
        } else {
            // По умолчанию используем предыдущий месяц
            sql = "SELECT machine_name, SUM(TIME_TO_SEC(machine_downtime)) AS machine_downtime_seconds " +
                    "FROM equipment_maintenance_records " +
                    "WHERE YEAR(start_bd_t1) = YEAR(DATE_SUB(CURDATE(), INTERVAL 1 MONTH)) " +
                    "AND MONTH(start_bd_t1) = MONTH(DATE_SUB(CURDATE(), INTERVAL 1 MONTH)) " +
                    "AND failure_type <> 'Другие' " +
                    "GROUP BY machine_name " +
                    "ORDER BY SUM(TIME_TO_SEC(machine_downtime)) DESC " +
                    "LIMIT 4";
            return jdbcTemplate.queryForList(sql);
        }
    }

    // Топ-5 поломок за месяц по ключевым линиям
    public List<Map<String, Object>> getTopBreakdownsPerMonthKeyLines(Integer year, Integer month) {
        String sql;
        if (year != null && month != null) {
            sql = "SELECT machine_name, SUM(TIME_TO_SEC(machine_downtime)) AS machine_downtime_seconds " +
                    "FROM equipment_maintenance_records " +
                    "WHERE YEAR(start_bd_t1) = ? " +
                    "AND MONTH(start_bd_t1) = ? " +
                    "AND failure_type <> 'Другие' " +
                    "AND machine_name IN (" +
                    "'Mixer GK 270 T-C 2.1', 'Mixer GK 320 E 1.1', " +
                    "'Calender Complex Berstorf - 01', 'Bandina - 01', 'Duplex - 01', " +
                    "'Calender Comerio Ercole - 01', 'VMI APEX - 01', 'VMI APEX - 02', " +
                    "'Trafila Quadruplex - 01', 'Bartell Bead Machine - 01', " +
                    "'TTM fisher belt cutting - 01', 'VMI TPCS 1600-1000'" +
                    ") " +
                    "GROUP BY machine_name " +
                    "ORDER BY SUM(TIME_TO_SEC(machine_downtime)) DESC " +
                    "LIMIT 4";
            return jdbcTemplate.queryForList(sql, year, month);
        } else {
            // По умолчанию используем предыдущий месяц
            sql = "SELECT machine_name, SUM(TIME_TO_SEC(machine_downtime)) AS machine_downtime_seconds " +
                    "FROM equipment_maintenance_records " +
                    "WHERE YEAR(start_bd_t1) = YEAR(DATE_SUB(CURDATE(), INTERVAL 1 MONTH)) " +
                    "AND MONTH(start_bd_t1) = MONTH(DATE_SUB(CURDATE(), INTERVAL 1 MONTH)) " +
                    "AND failure_type <> 'Другие' " +
                    "AND machine_name IN (" +
                    "'Mixer GK 270 T-C 2.1', 'Mixer GK 320 E 1.1', " +
                    "'Calender Complex Berstorf - 01', 'Bandina - 01', 'Duplex - 01', " +
                    "'Calender Comerio Ercole - 01', 'VMI APEX - 01', 'VMI APEX - 02', " +
                    "'Trafila Quadruplex - 01', 'Bartell Bead Machine - 01', " +
                    "'TTM fisher belt cutting - 01', 'VMI TPCS 1600-1000'" +
                    ") " +
                    "GROUP BY machine_name " +
                    "ORDER BY SUM(TIME_TO_SEC(machine_downtime)) DESC " +
                    "LIMIT 4";
            return jdbcTemplate.queryForList(sql);
        }
    }
    // Топ-5 поломок за сутки (последние 24 часа)
    public List<Map<String, Object>> getTopBreakdownsPerDay() {
        String sql = "SELECT code, machine_name, machine_downtime, cause " +
                "FROM equipment_maintenance_records " +
                "WHERE start_bd_t1 >= DATE_SUB(NOW(), INTERVAL 24 HOUR) " +
                "AND failure_type <> 'Другие' " +
                "AND machine_downtime IS NOT NULL " +
                "ORDER BY TIME_TO_SEC(machine_downtime) DESC " +
                "LIMIT 3";
        return jdbcTemplate.queryForList(sql);
    }

    // Топ-5 поломок по ключевым линиям за сутки (последние 24 часа)
    public List<Map<String, Object>> getTopBreakdownsPerDayKeyLines() {
        String sql = "SELECT code, machine_name, machine_downtime, cause " +
        "FROM equipment_maintenance_records " +
        "WHERE (" +
        " machine_name LIKE 'Mixer GK 270%' OR machine_name LIKE 'Mixer GK 320%' OR" +
        " machine_name LIKE 'Calender %' OR machine_name LIKE 'Calender Complex %' OR" +
        " machine_name LIKE 'Bandina%' OR machine_name LIKE 'Duplex%' OR" +
        " machine_name LIKE 'Calender Comerio Ercole%' OR" +
        " machine_name LIKE 'VMI APEX%' OR machine_name LIKE 'VMI APEX%' OR" +
        " machine_name LIKE 'Trafila Quadruplex%' OR machine_name LIKE 'Bartell Bead Machine%' OR" +
        " machine_name LIKE 'TTM fisher belt cutting%' OR machine_name LIKE 'VMI TPCS%' " +
        ") " +
        "AND start_bd_t1 >= CONCAT(DATE_SUB(CURRENT_DATE(), INTERVAL 1 DAY), ' 06:00:00') " +
        "AND stop_bd_t4 <= CONCAT(CURRENT_DATE(), ' 08:00:00') " +
        "AND failure_type <> 'Другие' " +
        "AND machine_downtime IS NOT NULL " +
        "ORDER BY TIME_TO_SEC(machine_downtime) DESC " +
        "LIMIT 3";
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
        return jdbcTemplate.queryForList(sql);
    }

    // Текущий ТОП (онлайн) по таблице top_breakdowns_current_status_online
    // Возвращаем длительность в секундах, чтобы избежать JDBC-мэппинга TIME -> java.sql.Time
    public List<Map<String, Object>> getTopBreakdownsCurrentStatusOnline() {
        String sql = "SELECT area, machine_name, TIME_TO_SEC(machine_downtime) AS machine_downtime_seconds, cause " +
                "FROM top_breakdowns_current_status_online " +
                "ORDER BY TIME_TO_SEC(machine_downtime) DESC";
        return jdbcTemplate.queryForList(sql);
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ProductionDaysCorrectionRepository productionDaysCorrectionRepository;

    /**
     * Список диапазонов производственных дней [firstDay, lastDay] для учёта в расчётах за месяц.
     * Пустой список — фильтр не применяется. Несколько диапазонов поддерживают простой в середине месяца.
     */
    private List<int[]> getProductionDayRanges(int year, int month) {
        return productionDaysCorrectionRepository.findRangesByYearAndMonth(year, month);
    }

    /** Строит фрагмент SQL и массив параметров для фильтра по производственным дням (несколько диапазонов). */
    private Object[] buildDayFilterAndParams(List<int[]> ranges) {
        if (ranges == null || ranges.isEmpty()) {
            return new Object[]{ "", new Object[0] };
        }
        String dayExpr = "DAY(STR_TO_DATE(production_day, '%d.%m.%Y'))";
        StringBuilder sb = new StringBuilder(" AND (");
        List<Object> params = new ArrayList<>();
        for (int i = 0; i < ranges.size(); i++) {
            if (i > 0) sb.append(" OR ");
            sb.append("(").append(dayExpr).append(" >= ? AND ").append(dayExpr).append(" <= ?)");
            params.add(ranges.get(i)[0]);
            params.add(ranges.get(i)[1]);
        }
        sb.append(")");
        return new Object[]{ sb.toString(), params.toArray() };
    }

    public List<Map<String, Object>> getRegions() {
        return jdbcTemplate.queryForList("SELECT id, name_region FROM region");
    }

    public List<Map<String, Object>> getEquipment(int regionId) {
        return jdbcTemplate.queryForList("SELECT id, name_equipment FROM equipment WHERE region_id = ?",
                regionId);
    }

    public List<Map<String, Object>> getNodes(int equipmentId) {
        return jdbcTemplate.queryForList("SELECT id, name_node FROM node WHERE equipment_id = ?",
                equipmentId);
    }

    public List<Map<String, Object>> searchBreakDown(Integer year, Integer month) {
        int y, m;
        if (year != null && month != null) {
            y = year;
            m = month;
        } else {
            java.util.Calendar cal = java.util.Calendar.getInstance();
            cal.add(java.util.Calendar.DAY_OF_MONTH, -1);
            y = cal.get(java.util.Calendar.YEAR);
            m = cal.get(java.util.Calendar.MONTH) + 1;
        }
        List<int[]> ranges = getProductionDayRanges(y, m);
        Object[] filterAndParams = buildDayFilterAndParams(ranges);
        String dayFilter = (String) filterAndParams[0];
        Object[] dayParams = (Object[]) filterAndParams[1];
        String sql = "SELECT production_day, downtime_percentage "
                + "FROM report_plant WHERE YEAR(STR_TO_DATE(production_day, '%d.%m.%Y')) = ? "
                + "AND MONTH(STR_TO_DATE(production_day, '%d.%m.%Y')) = ?" + dayFilter + " ORDER BY STR_TO_DATE(production_day, '%d.%m.%Y')";
        if (dayParams.length > 0) {
            Object[] params = new Object[2 + dayParams.length];
            params[0] = y;
            params[1] = m;
            System.arraycopy(dayParams, 0, params, 2, dayParams.length);
            return jdbcTemplate.queryForList(sql, params);
        }
        return jdbcTemplate.queryForList(sql, y, m);
    }
    
    public List<Map<String, Object>> searchAvailability(Integer year, Integer month) {
        int y, m;
        if (year != null && month != null) {
            y = year;
            m = month;
        } else {
            java.util.Calendar cal = java.util.Calendar.getInstance();
            cal.add(java.util.Calendar.DAY_OF_MONTH, -1);
            y = cal.get(java.util.Calendar.YEAR);
            m = cal.get(java.util.Calendar.MONTH) + 1;
        }
        List<int[]> ranges = getProductionDayRanges(y, m);
        Object[] filterAndParams = buildDayFilterAndParams(ranges);
        String dayFilter = (String) filterAndParams[0];
        Object[] dayParams = (Object[]) filterAndParams[1];
        String sql = "SELECT production_day, availability "
                + "FROM report_plant WHERE YEAR(STR_TO_DATE(production_day, '%d.%m.%Y')) = ? "
                + "AND MONTH(STR_TO_DATE(production_day, '%d.%m.%Y')) = ?" + dayFilter + " ORDER BY STR_TO_DATE(production_day, '%d.%m.%Y')";
        if (dayParams.length > 0) {
            Object[] params = new Object[2 + dayParams.length];
            params[0] = y;
            params[1] = m;
            System.arraycopy(dayParams, 0, params, 2, dayParams.length);
            return jdbcTemplate.queryForList(sql, params);
        }
        return jdbcTemplate.queryForList(sql, y, m);
    }
    
    // Данные для графика PM: план/факт/tag за указанный месяц.
    // Фильтр по производственным дням не применяется — останов завода на ППР не влияет.
    public List<Map<String, Object>> getPmPlanFactTagPerMonth(Integer year, Integer month) {
        String sql;
        if (year != null && month != null) {
            sql = "SELECT production_day, quantity_pm_planned AS plan, quantity_pm_close AS fact, quantity_tag AS tag " +
                    "FROM report_plant WHERE YEAR(STR_TO_DATE(production_day, '%d.%m.%Y')) = ? " +
                    "AND MONTH(STR_TO_DATE(production_day, '%d.%m.%Y')) = ? " +
                    "ORDER BY STR_TO_DATE(production_day, '%d.%m.%Y')";
            return jdbcTemplate.queryForList(sql, year, month);
        }
        sql = "SELECT production_day, quantity_pm_planned AS plan, quantity_pm_close AS fact, quantity_tag AS tag " +
                "FROM report_plant WHERE YEAR(STR_TO_DATE(production_day, '%d.%m.%Y')) = YEAR(DATE_SUB(CURDATE(), INTERVAL 1 DAY)) " +
                "AND MONTH(STR_TO_DATE(production_day, '%d.%m.%Y')) = MONTH(DATE_SUB(CURDATE(), INTERVAL 1 DAY)) " +
                "ORDER BY STR_TO_DATE(production_day, '%d.%m.%Y')";
        return jdbcTemplate.queryForList(sql);
    }

    // Данные для графика PM: план/факт/tag за все периоды
    // Использует логику из триггера update_plant_reports (без фильтров)
    public List<Map<String, Object>> getPmPlanFactTagAll() {
        // Используем ту же логику что и в getPmPlanFactTagFiltered, но без фильтров
        return getPmPlanFactTagFiltered(null, null);
    }

    // Данные для графика PM с фильтрацией по участку и оборудованию
    // Использует логику из триггера update_plant_reports:
    // - Plan: COUNT(*) из pm_maintenance_records по scheduled_proposed_date
    // - Fact: COUNT(*) из pm_maintenance_records по scheduled_date где status IN ('Закрыто','Выполнено')
    // - Tag: COUNT(*) из tag_maintenance_records по production_day
    public List<Map<String, Object>> getPmPlanFactTagFiltered(String area, String machineName) {
        StringBuilder sql = new StringBuilder();
        List<Object> params = new ArrayList<>();
        
        // Собираем все уникальные даты из всех трех источников
        sql.append("SELECT ");
        sql.append("    all_dates.production_day, ");
        sql.append("    COALESCE(pm_plan.quantity, 0) AS plan, ");
        sql.append("    COALESCE(pm_fact.quantity, 0) AS fact, ");
        sql.append("    COALESCE(tag.quantity, 0) AS tag ");
        sql.append("FROM ( ");
        sql.append("    SELECT DATE_FORMAT(scheduled_proposed_date, '%d.%m.%Y') AS production_day ");
        sql.append("    FROM pm_maintenance_records ");
        sql.append("    WHERE scheduled_proposed_date IS NOT NULL ");
        if (area != null && !area.isEmpty() && !area.equals("all")) {
            sql.append("    AND area = ? ");
            params.add(area);
        }
        if (machineName != null && !machineName.isEmpty() && !machineName.equals("all")) {
            sql.append("    AND machine_name = ? ");
            params.add(machineName);
        }
        sql.append("    UNION ");
        sql.append("    SELECT DATE_FORMAT(scheduled_date, '%d.%m.%Y') AS production_day ");
        sql.append("    FROM pm_maintenance_records ");
        sql.append("    WHERE scheduled_date IS NOT NULL AND status IN ('Закрыто', 'Выполнено') ");
        if (area != null && !area.isEmpty() && !area.equals("all")) {
            sql.append("    AND area = ? ");
            params.add(area);
        }
        if (machineName != null && !machineName.isEmpty() && !machineName.equals("all")) {
            sql.append("    AND machine_name = ? ");
            params.add(machineName);
        }
        sql.append("    UNION ");
        sql.append("    SELECT production_day ");
        sql.append("    FROM tag_maintenance_records ");
        sql.append("    WHERE production_day IS NOT NULL AND production_day != '' ");
        if (area != null && !area.isEmpty() && !area.equals("all")) {
            sql.append("    AND area = ? ");
            params.add(area);
        }
        if (machineName != null && !machineName.isEmpty() && !machineName.equals("all")) {
            sql.append("    AND machine_name = ? ");
            params.add(machineName);
        }
        sql.append(") AS all_dates ");
        sql.append("LEFT JOIN ( ");
        sql.append("    SELECT DATE_FORMAT(scheduled_proposed_date, '%d.%m.%Y') AS production_day, COUNT(*) AS quantity ");
        sql.append("    FROM pm_maintenance_records ");
        sql.append("    WHERE scheduled_proposed_date IS NOT NULL ");
        if (area != null && !area.isEmpty() && !area.equals("all")) {
            sql.append("    AND area = ? ");
            params.add(area);
        }
        if (machineName != null && !machineName.isEmpty() && !machineName.equals("all")) {
            sql.append("    AND machine_name = ? ");
            params.add(machineName);
        }
        sql.append("    GROUP BY DATE_FORMAT(scheduled_proposed_date, '%d.%m.%Y') ");
        sql.append(") AS pm_plan ON all_dates.production_day = pm_plan.production_day ");
        sql.append("LEFT JOIN ( ");
        sql.append("    SELECT DATE_FORMAT(scheduled_date, '%d.%m.%Y') AS production_day, COUNT(*) AS quantity ");
        sql.append("    FROM pm_maintenance_records ");
        sql.append("    WHERE scheduled_date IS NOT NULL AND status IN ('Закрыто', 'Выполнено') ");
        if (area != null && !area.isEmpty() && !area.equals("all")) {
            sql.append("    AND area = ? ");
            params.add(area);
        }
        if (machineName != null && !machineName.isEmpty() && !machineName.equals("all")) {
            sql.append("    AND machine_name = ? ");
            params.add(machineName);
        }
        sql.append("    GROUP BY DATE_FORMAT(scheduled_date, '%d.%m.%Y') ");
        sql.append(") AS pm_fact ON all_dates.production_day = pm_fact.production_day ");
        sql.append("LEFT JOIN ( ");
        sql.append("    SELECT production_day, COUNT(*) AS quantity ");
        sql.append("    FROM tag_maintenance_records ");
        sql.append("    WHERE production_day IS NOT NULL AND production_day != '' ");
        if (area != null && !area.isEmpty() && !area.equals("all")) {
            sql.append("    AND area = ? ");
            params.add(area);
        }
        if (machineName != null && !machineName.isEmpty() && !machineName.equals("all")) {
            sql.append("    AND machine_name = ? ");
            params.add(machineName);
        }
        sql.append("    GROUP BY production_day ");
        sql.append(") AS tag ON all_dates.production_day = tag.production_day ");
        sql.append("ORDER BY STR_TO_DATE(all_dates.production_day, '%d.%m.%Y')");
        
        return jdbcTemplate.queryForList(sql.toString(), params.toArray());
    }

    // public List<Map<String, Object>> searchBreakDown() {
    //     String sql = "SELECT production_day, downtime_percentage "
    //             + "FROM report_plant WHERE YEAR(STR_TO_DATE(production_day, '%d.%m.%Y')) = YEAR(CURDATE()) "
    //             + "AND MONTH(STR_TO_DATE(production_day, '%d.%m.%Y')) = MONTH(CURDATE()) ORDER BY STR_TO_DATE(production_day, '%d.%m.%Y')";
    //     return jdbcTemplate.queryForList(sql);
    // }

    // public List<Map<String, Object>> searchAvailability() {
    //     String sql = "SELECT production_day, availability "
    //             + "FROM report_plant WHERE YEAR(STR_TO_DATE(production_day, '%d.%m.%Y')) = YEAR(CURDATE()) "
    //             + "AND MONTH(STR_TO_DATE(production_day, '%d.%m.%Y')) = MONTH(CURDATE()) ORDER BY STR_TO_DATE(production_day, '%d.%m.%Y')";
    //     return jdbcTemplate.queryForList(sql);
    // }

    // // Данные для графика PM: план/факт/tag за текущий месяц
    // public List<Map<String, Object>> getPmPlanFactTagPerMonth() {
    //     String sql = "SELECT production_day, quantity_pm_planned AS plan, quantity_pm_close AS fact, quantity_tag AS tag " +
    //             "FROM report_plant WHERE YEAR(STR_TO_DATE(production_day, '%d.%m.%Y')) = YEAR(CURDATE()) " +
    //             "AND MONTH(STR_TO_DATE(production_day, '%d.%m.%Y')) = MONTH(CURDATE()) " +
    //             "ORDER BY STR_TO_DATE(production_day, '%d.%m.%Y')";
    //     return jdbcTemplate.queryForList(sql);
    // }

    // Все заявки на поломки (equipment_maintenance_records) с ограничением по количеству
    public List<Map<String, Object>> getEquipmentMaintenanceRecords() {
        String sql = "SELECT * FROM equipment_maintenance_records ORDER BY id DESC";
        return jdbcTemplate.queryForList(sql);
    }

    // Все заявки на поломки с фильтрацией по датам и участку
    public List<Map<String, Object>> getEquipmentMaintenanceRecords(String dateFrom, String dateTo, String area) {
        StringBuilder sql = new StringBuilder();
        List<Object> params = new ArrayList<>();
        
        sql.append("SELECT * FROM equipment_maintenance_records WHERE 1=1 ");
        
        if (dateFrom != null && !dateFrom.isEmpty()) {
            sql.append("AND start_bd_t1 >= STR_TO_DATE(?, '%Y-%m-%d') ");
            params.add(dateFrom);
        }
        if (dateTo != null && !dateTo.isEmpty()) {
            sql.append("AND start_bd_t1 < DATE_ADD(STR_TO_DATE(?, '%Y-%m-%d'), INTERVAL 1 DAY) ");
            params.add(dateTo);
        }
        if (area != null && !area.isEmpty() && !area.equals("all")) {
            sql.append("AND area = ? ");
            params.add(area);
        }
        
        sql.append("ORDER BY id DESC");
        
        if (params.isEmpty()) {
            return jdbcTemplate.queryForList(sql.toString());
        } else {
            return jdbcTemplate.queryForList(sql.toString(), params.toArray());
        }
    }

    // Все записи ППР (pm_maintenance_records)
    public List<Map<String, Object>> getPmMaintenanceRecords() {
        String sql = "SELECT * FROM pm_maintenance_records ORDER BY id DESC";
        return jdbcTemplate.queryForList(sql);
    }

    // Все записи Tag (tag_maintenance_records)
    public List<Map<String, Object>> getTagMaintenanceRecords() {
        String sql = "SELECT * FROM tag_maintenance_records ORDER BY id DESC";
        return jdbcTemplate.queryForList(sql);
    }

    // Все записи диагностики (diagnostics_reports)
    public List<Map<String, Object>> getDiagnosticsReports() {
        String sql = "SELECT * FROM diagnostics_reports ORDER BY id DESC";
        return jdbcTemplate.queryForList(sql);
    }

    // Получение оборудования из equipment_list
    public List<Map<String, Object>> getEquipmentList() {
        String sql = "SELECT id, machine_name, area FROM equipment_list ORDER BY machine_name";
        return jdbcTemplate.queryForList(sql);
    }

    // Получение оборудования из equipment_list с фильтром по участку
    public List<Map<String, Object>> getEquipmentListByArea(String area) {
        if (area != null && !area.isEmpty() && !area.equals("all")) {
            String sql = "SELECT id, machine_name, area FROM equipment_list WHERE area = ? ORDER BY machine_name";
            return jdbcTemplate.queryForList(sql, area);
        }
        return getEquipmentList();
    }

    // Получение участков из equipment_list
    public List<Map<String, Object>> getDiagnosticsAreas() {
        String sql = "SELECT DISTINCT area FROM equipment_list " +
                "WHERE area IS NOT NULL AND TRIM(area) <> '' ORDER BY area";
        return jdbcTemplate.queryForList(sql);
    }

    // Получение участков из PM записей
    public List<Map<String, Object>> getPmAreas() {
        String sql = "SELECT DISTINCT area FROM pm_maintenance_records " +
                "WHERE area IS NOT NULL AND TRIM(area) <> '' ORDER BY area";
        return jdbcTemplate.queryForList(sql);
    }

    // Получение участков из Tag записей
    public List<Map<String, Object>> getTagAreas() {
        String sql = "SELECT DISTINCT area FROM tag_maintenance_records " +
                "WHERE area IS NOT NULL AND TRIM(area) <> '' ORDER BY area";
        return jdbcTemplate.queryForList(sql);
    }

    // Получение участков из BD записей (equipment_maintenance_records)
    public List<Map<String, Object>> getBdAreas() {
        String sql = "SELECT DISTINCT area FROM equipment_maintenance_records " +
                "WHERE area IS NOT NULL AND TRIM(area) <> '' ORDER BY area";
        return jdbcTemplate.queryForList(sql);
    }

    // Получение оборудования из PM записей с фильтром по участку
    public List<Map<String, Object>> getPmEquipmentByArea(String area) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT DISTINCT machine_name FROM pm_maintenance_records ");
        sql.append("WHERE machine_name IS NOT NULL AND TRIM(machine_name) <> '' ");
        
        if (area != null && !area.isEmpty() && !area.equals("all")) {
            sql.append("AND area = ? ");
        }
        
        sql.append("ORDER BY machine_name");
        
        if (area != null && !area.isEmpty() && !area.equals("all")) {
            return jdbcTemplate.queryForList(sql.toString(), area);
        }
        return jdbcTemplate.queryForList(sql.toString());
    }

    // Получение оборудования из Tag записей с фильтром по участку
    public List<Map<String, Object>> getTagEquipmentByArea(String area) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT DISTINCT machine_name FROM tag_maintenance_records ");
        sql.append("WHERE machine_name IS NOT NULL AND TRIM(machine_name) <> '' ");
        
        if (area != null && !area.isEmpty() && !area.equals("all")) {
            sql.append("AND area = ? ");
        }
        
        sql.append("ORDER BY machine_name");
        
        if (area != null && !area.isEmpty() && !area.equals("all")) {
            return jdbcTemplate.queryForList(sql.toString(), area);
        }
        return jdbcTemplate.queryForList(sql.toString());
    }

    // Получение оборудования из BD записей с фильтром по участку
    public List<Map<String, Object>> getBdEquipmentByArea(String area) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT DISTINCT machine_name FROM equipment_maintenance_records ");
        sql.append("WHERE machine_name IS NOT NULL AND TRIM(machine_name) <> '' ");
        
        if (area != null && !area.isEmpty() && !area.equals("all")) {
            sql.append("AND area = ? ");
        }
        
        sql.append("ORDER BY machine_name");
        
        if (area != null && !area.isEmpty() && !area.equals("all")) {
            return jdbcTemplate.queryForList(sql.toString(), area);
        }
        return jdbcTemplate.queryForList(sql.toString());
    }

    // Данные для графика PM/Tag/BD: количество выполненных по датам
    // - PM: COUNT(*) из pm_maintenance_records где status IN ('Закрыто', 'Выполнено') по scheduled_date
    // - Tag: COUNT(*) из tag_maintenance_records где status IN ('Закрыто', 'Выполнено') по production_day
    // - BD: COUNT(*) из equipment_maintenance_records где status IN ('Закрыто', 'Выполнено') 
    //      и failure_type NOT IN ('Другие', 'ППР') по date
    public List<Map<String, Object>> getPmTagBdCompleted(String area, String machineName) {
        StringBuilder sql = new StringBuilder();
        List<Object> params = new ArrayList<>();
        
        // Собираем все уникальные даты из всех трех источников
        sql.append("SELECT ");
        sql.append("    all_dates.production_day, ");
        sql.append("    COALESCE(pm.quantity, 0) AS pm_count, ");
        sql.append("    COALESCE(tag.quantity, 0) AS tag_count, ");
        sql.append("    COALESCE(bd.quantity, 0) AS bd_count ");
        sql.append("FROM ( ");
        sql.append("    SELECT DATE_FORMAT(scheduled_date, '%d.%m.%Y') AS production_day ");
        sql.append("    FROM pm_maintenance_records ");
        sql.append("    WHERE scheduled_date IS NOT NULL AND status IN ('Закрыто', 'Выполнено') ");
        if (area != null && !area.isEmpty() && !area.equals("all")) {
            sql.append("    AND area = ? ");
            params.add(area);
        }
        if (machineName != null && !machineName.isEmpty() && !machineName.equals("all")) {
            sql.append("    AND machine_name = ? ");
            params.add(machineName);
        }
        sql.append("    UNION ");
        sql.append("    SELECT production_day ");
        sql.append("    FROM tag_maintenance_records ");
        sql.append("    WHERE production_day IS NOT NULL AND production_day != '' AND status IN ('Закрыто', 'Выполнено') ");
        if (area != null && !area.isEmpty() && !area.equals("all")) {
            sql.append("    AND area = ? ");
            params.add(area);
        }
        if (machineName != null && !machineName.isEmpty() && !machineName.equals("all")) {
            sql.append("    AND machine_name = ? ");
            params.add(machineName);
        }
        sql.append("    UNION ");
        sql.append("    SELECT date AS production_day ");
        sql.append("    FROM equipment_maintenance_records ");
        sql.append("    WHERE date IS NOT NULL AND date != '' AND status IN ('Закрыто', 'Выполнено') ");
        sql.append("    AND (failure_type IS NULL OR failure_type NOT IN ('Другие', 'ППР')) ");
        if (area != null && !area.isEmpty() && !area.equals("all")) {
            sql.append("    AND area = ? ");
            params.add(area);
        }
        if (machineName != null && !machineName.isEmpty() && !machineName.equals("all")) {
            sql.append("    AND machine_name = ? ");
            params.add(machineName);
        }
        sql.append(") AS all_dates ");
        sql.append("LEFT JOIN ( ");
        sql.append("    SELECT DATE_FORMAT(scheduled_date, '%d.%m.%Y') AS production_day, COUNT(*) AS quantity ");
        sql.append("    FROM pm_maintenance_records ");
        sql.append("    WHERE scheduled_date IS NOT NULL AND status IN ('Закрыто', 'Выполнено') ");
        if (area != null && !area.isEmpty() && !area.equals("all")) {
            sql.append("    AND area = ? ");
            params.add(area);
        }
        if (machineName != null && !machineName.isEmpty() && !machineName.equals("all")) {
            sql.append("    AND machine_name = ? ");
            params.add(machineName);
        }
        sql.append("    GROUP BY DATE_FORMAT(scheduled_date, '%d.%m.%Y') ");
        sql.append(") AS pm ON all_dates.production_day = pm.production_day ");
        sql.append("LEFT JOIN ( ");
        sql.append("    SELECT production_day, COUNT(*) AS quantity ");
        sql.append("    FROM tag_maintenance_records ");
        sql.append("    WHERE production_day IS NOT NULL AND production_day != '' AND status IN ('Закрыто', 'Выполнено') ");
        if (area != null && !area.isEmpty() && !area.equals("all")) {
            sql.append("    AND area = ? ");
            params.add(area);
        }
        if (machineName != null && !machineName.isEmpty() && !machineName.equals("all")) {
            sql.append("    AND machine_name = ? ");
            params.add(machineName);
        }
        sql.append("    GROUP BY production_day ");
        sql.append(") AS tag ON all_dates.production_day = tag.production_day ");
        sql.append("LEFT JOIN ( ");
        sql.append("    SELECT date AS production_day, COUNT(*) AS quantity ");
        sql.append("    FROM equipment_maintenance_records ");
        sql.append("    WHERE date IS NOT NULL AND date != '' AND status IN ('Закрыто', 'Выполнено') ");
        sql.append("    AND (failure_type IS NULL OR failure_type NOT IN ('Другие', 'ППР')) ");
        if (area != null && !area.isEmpty() && !area.equals("all")) {
            sql.append("    AND area = ? ");
            params.add(area);
        }
        if (machineName != null && !machineName.isEmpty() && !machineName.equals("all")) {
            sql.append("    AND machine_name = ? ");
            params.add(machineName);
        }
        sql.append("    GROUP BY date ");
        sql.append(") AS bd ON all_dates.production_day = bd.production_day ");
        sql.append("ORDER BY STR_TO_DATE(all_dates.production_day, '%d.%m.%Y')");
        
        return jdbcTemplate.queryForList(sql.toString(), params.toArray());
    }


    public Map<String, Object> getCurrentMetrics(Integer year, Integer month) {
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
        
        // Определяем год и месяц для запросов
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.add(java.util.Calendar.DAY_OF_MONTH, -1);
        int currentYear = cal.get(java.util.Calendar.YEAR);
        int currentMonth = cal.get(java.util.Calendar.MONTH) + 1; // Calendar месяцы начинаются с 0
        
        int targetYear, targetMonth;
        boolean isCurrentMonth = false;
        
        if (year != null && month != null) {
            targetYear = year;
            targetMonth = month;
            // Проверяем, является ли выбранный месяц текущим (предыдущим)
            isCurrentMonth = (targetYear == currentYear && targetMonth == currentMonth);
        } else {
            // По умолчанию используем предыдущий день
            targetYear = currentYear;
            targetMonth = currentMonth;
            isCurrentMonth = true;
        }
        
        // Для текущего месяца используем старую логику - предыдущий день
        // Для других месяцев - последний день выбранного месяца
        String lastDayStr;
        if (isCurrentMonth) {
            // Старая логика - предыдущий день
            java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("dd.MM.yyyy");
            lastDayStr = dateFormat.format(cal.getTime());
        } else {
            // Новая логика - последний день выбранного месяца
            java.util.Calendar lastDayCal = java.util.Calendar.getInstance();
            lastDayCal.set(targetYear, targetMonth - 1, 1); // Устанавливаем первый день месяца
            lastDayCal.set(java.util.Calendar.DAY_OF_MONTH, lastDayCal.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)); // Устанавливаем последний день
            java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("dd.MM.yyyy");
            lastDayStr = dateFormat.format(lastDayCal.getTime());
        }
        
        List<int[]> dayRanges = getProductionDayRanges(targetYear, targetMonth);
        Object[] filterAndParams = buildDayFilterAndParams(dayRanges);
        String monthDayFilter = (String) filterAndParams[0];
        Object[] dayParams = (Object[]) filterAndParams[1];

        for (String[] t : tables) {
            String table = t[0];
            String prefix = t[1];

            // BD для последнего дня месяца
            String sqlToday = "SELECT AVG(downtime_percentage) as bd FROM " + table + " WHERE production_day = ?";
            Double bdToday = null;
            try {
                bdToday = jdbcTemplate.queryForObject(sqlToday, Double.class, lastDayStr);
            } catch (Exception e) {
                // Игнорируем ошибки, если данных нет
            }
            result.put(prefix + "_bd_today", bdToday != null ? bdToday : 0.0);

            // BD за весь месяц (с учётом корректировки производственных дней; ППР не пересчитывается)
            String sqlMonth = "SELECT AVG(downtime_percentage) as bd FROM " + table + " WHERE YEAR(STR_TO_DATE(production_day, '%d.%m.%Y')) = ? AND MONTH(STR_TO_DATE(production_day, '%d.%m.%Y')) = ?" + monthDayFilter;
            Double bdMonth = null;
            try {
                if (dayParams.length > 0) {
                    Object[] params = new Object[2 + dayParams.length];
                    params[0] = targetYear;
                    params[1] = targetMonth;
                    System.arraycopy(dayParams, 0, params, 2, dayParams.length);
                    bdMonth = jdbcTemplate.queryForObject(sqlMonth, Double.class, params);
                } else {
                    bdMonth = jdbcTemplate.queryForObject(sqlMonth, Double.class, targetYear, targetMonth);
                }
            } catch (Exception e) {
                // Игнорируем ошибки, если данных нет
            }
            result.put(prefix + "_bd_month", bdMonth != null ? bdMonth : 0.0);

            // Availability для последнего дня месяца
            String sqlTodayA = "SELECT AVG(availability) FROM " + table + " WHERE production_day = ?";
            Double aToday = null;
            try {
                aToday = jdbcTemplate.queryForObject(sqlTodayA, Double.class, lastDayStr);
            } catch (Exception e) {
                // Игнорируем ошибки, если данных нет
            }
            result.put(prefix + "_availability_today", aToday != null ? aToday : 0.0);

            // Availability за весь месяц (с учётом корректировки производственных дней; ППР не пересчитывается)
            String sqlMonthA = "SELECT AVG(availability) FROM " + table + " WHERE YEAR(STR_TO_DATE(production_day, '%d.%m.%Y')) = ? AND MONTH(STR_TO_DATE(production_day, '%d.%m.%Y')) = ?" + monthDayFilter;
            Double aMonth = null;
            try {
                if (dayParams.length > 0) {
                    Object[] params = new Object[2 + dayParams.length];
                    params[0] = targetYear;
                    params[1] = targetMonth;
                    System.arraycopy(dayParams, 0, params, 2, dayParams.length);
                    aMonth = jdbcTemplate.queryForObject(sqlMonthA, Double.class, params);
                } else {
                    aMonth = jdbcTemplate.queryForObject(sqlMonthA, Double.class, targetYear, targetMonth);
                }
            } catch (Exception e) {
                // Игнорируем ошибки, если данных нет
            }
            result.put(prefix + "_availability_month", aMonth != null ? aMonth : 0.0);
        }
        return result;
    }

    public Map<String, Object> getMetricsForDate(String date) {
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
        int yearMonth = parseYearMonthFromProductionDay(date);
        int targetYear = yearMonth / 100;
        int targetMonth = yearMonth % 100;
        List<int[]> dayRanges = (targetYear > 0 && targetMonth >= 1 && targetMonth <= 12)
                ? getProductionDayRanges(targetYear, targetMonth) : new ArrayList<>();
        Object[] filterAndParams = buildDayFilterAndParams(dayRanges);
        String monthDayFilter = (String) filterAndParams[0];
        Object[] dayParams = (Object[]) filterAndParams[1];

        for (String[] t : tables) {
            String table = t[0];
            String prefix = t[1];

            String sqlToday = "SELECT AVG(downtime_percentage) as bd FROM " + table + " WHERE production_day = ?";
            Double bdToday = null;
            try {
                bdToday = jdbcTemplate.queryForObject(sqlToday, Double.class, date);
            } catch (Exception e) {
                // If no data found, bdToday will remain null
            }
            result.put(prefix + "_bd_today", bdToday != null ? bdToday : 0.0);

            String sqlMonth = "SELECT AVG(downtime_percentage) as bd FROM " + table + " WHERE YEAR(STR_TO_DATE(production_day, '%d.%m.%Y')) = YEAR(STR_TO_DATE(?, '%d.%m.%Y')) AND MONTH(STR_TO_DATE(production_day, '%d.%m.%Y')) = MONTH(STR_TO_DATE(?, '%d.%m.%Y'))" + monthDayFilter;
            Double bdMonth = null;
            try {
                if (dayParams.length > 0) {
                    Object[] params = new Object[2 + dayParams.length];
                    params[0] = date;
                    params[1] = date;
                    System.arraycopy(dayParams, 0, params, 2, dayParams.length);
                    bdMonth = jdbcTemplate.queryForObject(sqlMonth, Double.class, params);
                } else {
                    bdMonth = jdbcTemplate.queryForObject(sqlMonth, Double.class, date, date);
                }
            } catch (Exception e) {
                // If no data found, bdMonth will remain null
            }
            result.put(prefix + "_bd_month", bdMonth != null ? bdMonth : 0.0);

            String sqlTodayA = "SELECT AVG(availability) FROM " + table + " WHERE production_day = ?";
            Double aToday = null;
            try {
                aToday = jdbcTemplate.queryForObject(sqlTodayA, Double.class, date);
            } catch (Exception e) {
                // If no data found, aToday will remain null
            }
            result.put(prefix + "_availability_today", aToday != null ? aToday : 0.0);

            String sqlMonthA = "SELECT AVG(availability) FROM " + table + " WHERE YEAR(STR_TO_DATE(production_day, '%d.%m.%Y')) = YEAR(STR_TO_DATE(?, '%d.%m.%Y')) AND MONTH(STR_TO_DATE(production_day, '%d.%m.%Y')) = MONTH(STR_TO_DATE(?, '%d.%m.%Y'))" + monthDayFilter;
            Double aMonth = null;
            try {
                if (dayParams.length > 0) {
                    Object[] params = new Object[2 + dayParams.length];
                    params[0] = date;
                    params[1] = date;
                    System.arraycopy(dayParams, 0, params, 2, dayParams.length);
                    aMonth = jdbcTemplate.queryForObject(sqlMonthA, Double.class, params);
                } else {
                    aMonth = jdbcTemplate.queryForObject(sqlMonthA, Double.class, date, date);
                }
            } catch (Exception e) {
                // If no data found, aMonth will remain null
            }
            result.put(prefix + "_availability_month", aMonth != null ? aMonth : 0.0);
        }
        return result;
    }

    /** Парсит дату в формате dd.MM.yyyy и возвращает year*100+month (например 202603 для марта 2026). При ошибке — 0. */
    private int parseYearMonthFromProductionDay(String productionDay) {
        if (productionDay == null || productionDay.length() < 10) return 0;
        try {
            String[] parts = productionDay.split("\\.");
            if (parts.length != 3) return 0;
            int month = Integer.parseInt(parts[1]);
            int year = Integer.parseInt(parts[2]);
            if (month >= 1 && month <= 12 && year >= 1900 && year <= 2100) return year * 100 + month;
        } catch (NumberFormatException ignored) { }
        return 0;
    }
}
