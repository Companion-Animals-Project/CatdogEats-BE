package com.team5.catdogeats.batch.handler;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.*;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public class ZonedDateTimeTypeHandler extends BaseTypeHandler<ZonedDateTime> {
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, ZonedDateTime parameter, JdbcType jdbcType) throws SQLException {
        ps.setTimestamp(i, Timestamp.valueOf(parameter.withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime()));
    }

    @Override
    public ZonedDateTime getNullableResult(ResultSet rs, String columnName) throws SQLException {
        Timestamp ts = rs.getTimestamp(columnName);
        if (ts != null) {
            return ts.toInstant().atZone(ZoneId.systemDefault());
        }
        return null;
    }

    @Override
    public ZonedDateTime getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        Timestamp ts = rs.getTimestamp(columnIndex);
        if (ts != null) {
            return ts.toInstant().atZone(ZoneId.systemDefault());
        }
        return null;
    }

    @Override
    public ZonedDateTime getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        Timestamp ts = cs.getTimestamp(columnIndex);
        if (ts != null) {
            return ts.toInstant().atZone(ZoneId.systemDefault());
        }
        return null;
    }
}