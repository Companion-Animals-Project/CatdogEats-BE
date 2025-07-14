package com.team5.catdogeats.global.config.mybatis;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * PostgreSQL timestamp/timestamptz와 ZonedDateTime 간의 커스텀 TypeHandler
 * PostgreSQL JDBC 드라이버가 ZonedDateTime을 직접 지원하지 않아서
 * LocalDateTime을 거쳐서 변환하는 방식을 사용
 */
@MappedTypes(ZonedDateTime.class)
public class ZonedDateTimeTypeHandler extends BaseTypeHandler<ZonedDateTime> {

    // 기본 타임존 (시스템 타임존 또는 UTC)
    private static final ZoneId DEFAULT_ZONE = ZoneId.systemDefault();

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, ZonedDateTime parameter, JdbcType jdbcType) throws SQLException {
        // ZonedDateTime을 Timestamp로 변환해서 설정
        ps.setTimestamp(i, Timestamp.valueOf(parameter.toLocalDateTime()));
    }

    @Override
    public ZonedDateTime getNullableResult(ResultSet rs, String columnName) throws SQLException {
        Timestamp timestamp = rs.getTimestamp(columnName);
        return timestamp != null ?
                timestamp.toLocalDateTime().atZone(DEFAULT_ZONE) : null;
    }

    @Override
    public ZonedDateTime getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        Timestamp timestamp = rs.getTimestamp(columnIndex);
        return timestamp != null ?
                timestamp.toLocalDateTime().atZone(DEFAULT_ZONE) : null;
    }

    @Override
    public ZonedDateTime getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        Timestamp timestamp = cs.getTimestamp(columnIndex);
        return timestamp != null ?
                timestamp.toLocalDateTime().atZone(DEFAULT_ZONE) : null;
    }
}