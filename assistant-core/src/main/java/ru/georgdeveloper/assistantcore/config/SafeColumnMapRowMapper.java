package ru.georgdeveloper.assistantcore.config;

import org.springframework.jdbc.core.ColumnMapRowMapper;
import org.springframework.lang.Nullable;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;

/**
 * Читает MySQL TIME как строку, чтобы значения вроде 24:00:00 не ломали JDBC.
 */
public class SafeColumnMapRowMapper extends ColumnMapRowMapper {

    @Override
    @Nullable
    protected Object getColumnValue(ResultSet rs, int index) throws SQLException {
        ResultSetMetaData metaData = rs.getMetaData();
        int sqlType = metaData.getColumnType(index);
        if (sqlType == Types.TIME) {
            return rs.getString(index);
        }
        return super.getColumnValue(rs, index);
    }
}
