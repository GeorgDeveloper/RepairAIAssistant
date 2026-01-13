package ru.georgdeveloper.assistantcore.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public class FinalRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Value("${app.final.months-limit:12}")
    private int defaultMonthsLimit;

    public List<Map<String, Object>> getSummaries(List<Integer> years, List<Integer> months, Integer limit) {
        // build filter and fetch up to limit months sorted from left-to-right as requested: from older to newer
        StringBuilder sql = new StringBuilder("SELECT month, availability_percent, bd_percent, breakdowns_count, " +
                "planned_repairs_percent,pm_repairs_percent, planned_repairs_count, total_repairs_count, created_at, " +
                "YEAR(created_at) AS year, CONCAT(month, ' ', YEAR(created_at)) AS month_label " +
                "FROM availability_stats");

        List<Object> params = new ArrayList<>();
        List<String> where = new ArrayList<>();

        if (years != null && !years.isEmpty()) {
            String in = String.join(",", java.util.Collections.nCopies(years.size(), "?"));
            where.add("YEAR(created_at) IN (" + in + ")");
            params.addAll(years);
        }
        if (months != null && !months.isEmpty()) {
            String in = String.join(",", java.util.Collections.nCopies(months.size(), "?"));
            where.add("MONTH(created_at) IN (" + in + ")");
            params.addAll(months);
        }
        if (!where.isEmpty()) {
            sql.append(" WHERE ").append(String.join(" AND ", where));
        }
        // Сортируем по году и месяцу для правильного порядка отображения
        // Сначала по году DESC, затем по месяцу DESC (чтобы декабрь был первым, январь последним)
        sql.append(" ORDER BY YEAR(created_at) DESC, MONTH(created_at) DESC, id DESC");
        
        // Применяем LIMIT только если не выбраны конкретные месяцы
        // Если выбраны конкретные месяцы, возвращаем все выбранные месяцы без ограничения
        if (months == null || months.isEmpty()) {
            sql.append(" LIMIT ").append(limit != null && limit > 0 ? Math.min(limit, defaultMonthsLimit) : defaultMonthsLimit);
        }
        // Если выбраны конкретные месяцы, не применяем LIMIT - возвращаем все выбранные месяцы

        List<Map<String, Object>> records = jdbcTemplate.queryForList(sql.toString(), params.toArray());
        // Keep DESC order: newest -> oldest (right to left in table)

        // Transform to rows (metrics as rows, months as columns)
        List<Map<String, Object>> rows = new ArrayList<>();
        rows.add(metricRow("Месяц", records, "month_label"));
        rows.add(metricRow("Доступность, %", records, "availability_percent"));
        rows.add(metricRow("BD, %", records, "bd_percent"));
        rows.add(metricRow("Поломки, шт", records, "breakdowns_count"));
        rows.add(metricRow("Плановые ремонты, %", records, "planned_repairs_percent"));
        rows.add(metricRow("Факт выполнения ппр, %", records, "pm_repairs_percent"));
        rows.add(metricRow("Плановые ремонты, шт", records, "planned_repairs_count"));
        rows.add(metricRow("Итого ремонтов, шт", records, "total_repairs_count"));
        return rows;
    }

    public List<Map<String, Object>> getYears() {
        String sql = "SELECT DISTINCT YEAR(created_at) AS year FROM availability_stats ORDER BY year DESC";
        List<Map<String, Object>> result = jdbcTemplate.queryForList(sql);
        return result;
    }

    public List<Map<String, Object>> getMonths(List<Integer> years) {
        if (years == null || years.isEmpty()) {
            return jdbcTemplate.queryForList("SELECT DISTINCT MONTH(created_at) AS month FROM availability_stats ORDER BY month");
        }
        String in = String.join(",", java.util.Collections.nCopies(years.size(), "?"));
        String sql = "SELECT DISTINCT MONTH(created_at) AS month FROM availability_stats WHERE YEAR(created_at) IN (" + in + ") ORDER BY month";
        return jdbcTemplate.queryForList(sql, years.toArray());
    }

    // helpers from previous version removed as unused

    private Map<String, Object> metricRow(String metricName, List<Map<String, Object>> records, String field) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("metric", metricName);
        for (int i = 0; i < records.size(); i++) {
            map.put("m" + (i + 1), records.get(i).get(field));
        }
        return map;
    }
}


