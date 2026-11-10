package com.rxs.library.infrastructure.typehandler;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Calendar;
import java.util.TimeZone;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;

/** 将 DATETIME(6) 明确按 UTC 读写，不受 JVM 默认时区影响。 */
@MappedTypes(Instant.class)
public class UtcInstantTypeHandler extends BaseTypeHandler<Instant> {
    private Calendar utc() {
        return Calendar.getInstance(TimeZone.getTimeZone("UTC"));
    }

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, Instant value, JdbcType type)
            throws SQLException {
        ps.setTimestamp(i, Timestamp.from(value), utc());
    }

    @Override
    public Instant getNullableResult(ResultSet rs, String column) throws SQLException {
        return instant(rs.getTimestamp(column, utc()));
    }

    @Override
    public Instant getNullableResult(ResultSet rs, int column) throws SQLException {
        return instant(rs.getTimestamp(column, utc()));
    }

    @Override
    public Instant getNullableResult(CallableStatement cs, int column) throws SQLException {
        return instant(cs.getTimestamp(column, utc()));
    }

    private Instant instant(Timestamp value) {
        return value == null ? null : value.toInstant();
    }
}
