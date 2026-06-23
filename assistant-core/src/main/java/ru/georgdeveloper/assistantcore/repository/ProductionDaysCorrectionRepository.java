package ru.georgdeveloper.assistantcore.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import ru.georgdeveloper.assistantcore.model.ProductionDaysCorrection;
import ru.georgdeveloper.assistantcore.model.ProductionDaysCorrectionRange;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class ProductionDaysCorrectionRepository {

    private final JdbcTemplate jdbcTemplate;

    private static final RowMapper<ProductionDaysCorrection> ROW_MAPPER = new RowMapper<ProductionDaysCorrection>() {
        @Override
        public ProductionDaysCorrection mapRow(ResultSet rs, int rowNum) throws SQLException {
            ProductionDaysCorrection c = new ProductionDaysCorrection();
            c.setId(rs.getObject("id", Integer.class));
            c.setYear(rs.getInt("year"));
            c.setMonth(rs.getInt("month"));
            c.setComment(rs.getString("comment"));
            return c;
        }
    };

    private static final RowMapper<ProductionDaysCorrectionRange> RANGE_ROW_MAPPER = new RowMapper<ProductionDaysCorrectionRange>() {
        @Override
        public ProductionDaysCorrectionRange mapRow(ResultSet rs, int rowNum) throws SQLException {
            ProductionDaysCorrectionRange r = new ProductionDaysCorrectionRange();
            r.setId(rs.getObject("id", Integer.class));
            r.setFirstProductionDay(rs.getInt("first_production_day"));
            r.setLastProductionDay(rs.getInt("last_production_day"));
            return r;
        }
    };

    @Autowired
    public ProductionDaysCorrectionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Возвращает корректировку для года/месяца (без диапазонов).
     */
    public Optional<ProductionDaysCorrection> findByYearAndMonth(int year, int month) {
        String sql = "SELECT id, year, month, comment FROM production_days_correction WHERE year = ? AND month = ?";
        List<ProductionDaysCorrection> list = jdbcTemplate.query(sql, ROW_MAPPER, year, month);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    /**
     * Список диапазонов [first_day, last_day] для данного года/месяца (для фильтра в запросах).
     */
    public List<int[]> findRangesByYearAndMonth(int year, int month) {
        String sql = "SELECT r.first_production_day, r.last_production_day " +
                "FROM production_days_correction c " +
                "JOIN production_days_correction_range r ON r.correction_id = c.id " +
                "WHERE c.year = ? AND c.month = ? ORDER BY r.first_production_day";
        return jdbcTemplate.query(sql, (rs, rowNum) -> new int[]{rs.getInt(1), rs.getInt(2)}, year, month);
    }

    public List<ProductionDaysCorrection> findAll() {
        String sql = "SELECT id, year, month, comment FROM production_days_correction ORDER BY year DESC, month DESC";
        List<ProductionDaysCorrection> list = jdbcTemplate.query(sql, ROW_MAPPER);
        for (ProductionDaysCorrection c : list) {
            c.setRanges(findRangesByCorrectionId(c.getId()));
        }
        return list;
    }

    public List<ProductionDaysCorrectionRange> findRangesByCorrectionId(Integer correctionId) {
        if (correctionId == null) return new ArrayList<>();
        String sql = "SELECT id, first_production_day, last_production_day FROM production_days_correction_range WHERE correction_id = ? ORDER BY first_production_day";
        return jdbcTemplate.query(sql, RANGE_ROW_MAPPER, correctionId);
    }

    /**
     * Сохраняет корректировку и её диапазоны. Если запись для года/месяца есть — обновляет и перезаписывает диапазоны.
     */
    public ProductionDaysCorrection save(ProductionDaysCorrection correction) {
        Optional<ProductionDaysCorrection> existing = findByYearAndMonth(correction.getYear(), correction.getMonth());
        if (existing.isPresent()) {
            correction.setId(existing.get().getId());
        }
        if (correction.getId() != null) {
            jdbcTemplate.update("UPDATE production_days_correction SET comment = ? WHERE id = ?",
                    correction.getComment(), correction.getId());
            jdbcTemplate.update("DELETE FROM production_days_correction_range WHERE correction_id = ?", correction.getId());
        } else {
            jdbcTemplate.update("INSERT INTO production_days_correction (year, month, comment) VALUES (?, ?, ?)",
                    correction.getYear(), correction.getMonth(), correction.getComment());
            Integer newId = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Integer.class);
            correction.setId(newId);
        }
        for (ProductionDaysCorrectionRange range : correction.getRanges()) {
            if (range.getFirstProductionDay() >= 1 && range.getFirstProductionDay() <= 31
                    && range.getLastProductionDay() >= 1 && range.getLastProductionDay() <= 31
                    && range.getFirstProductionDay() <= range.getLastProductionDay()) {
                jdbcTemplate.update("INSERT INTO production_days_correction_range (correction_id, first_production_day, last_production_day) VALUES (?, ?, ?)",
                        correction.getId(), range.getFirstProductionDay(), range.getLastProductionDay());
            }
        }
        return correction;
    }

    public int deleteByYearAndMonth(int year, int month) {
        return jdbcTemplate.update("DELETE FROM production_days_correction WHERE year = ? AND month = ?", year, month);
    }

    public Optional<ProductionDaysCorrection> findById(Integer id) {
        String sql = "SELECT id, year, month, comment FROM production_days_correction WHERE id = ?";
        List<ProductionDaysCorrection> list = jdbcTemplate.query(sql, ROW_MAPPER, id);
        if (list.isEmpty()) return Optional.empty();
        ProductionDaysCorrection c = list.get(0);
        c.setRanges(findRangesByCorrectionId(c.getId()));
        return Optional.of(c);
    }
}
