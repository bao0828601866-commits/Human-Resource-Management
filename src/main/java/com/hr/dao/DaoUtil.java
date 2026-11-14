package com.hr.dao;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/** Hàm tiện ích dùng chung cho các DAO (đọc/ghi giá trị có thể NULL). */
final class DaoUtil {
    private DaoUtil() {}

    @FunctionalInterface
    interface Binder { void bind(PreparedStatement ps) throws SQLException; }

    static LocalDate date(ResultSet rs, String col) throws SQLException {
        Date d = rs.getDate(col);
        return d == null ? null : d.toLocalDate();
    }

    static LocalTime time(ResultSet rs, String col) throws SQLException {
        Time t = rs.getTime(col);
        return t == null ? null : t.toLocalTime();
    }

    static LocalDateTime ts(ResultSet rs, String col) throws SQLException {
        Timestamp t = rs.getTimestamp(col);
        return t == null ? null : t.toLocalDateTime();
    }

    static void setDate(PreparedStatement ps, int i, LocalDate v) throws SQLException {
        if (v == null) ps.setNull(i, Types.DATE); else ps.setDate(i, Date.valueOf(v));
    }

    static void setTime(PreparedStatement ps, int i, LocalTime v) throws SQLException {
        if (v == null) ps.setNull(i, Types.TIME); else ps.setTime(i, Time.valueOf(v));
    }

    static void setTs(PreparedStatement ps, int i, LocalDateTime v) throws SQLException {
        if (v == null) ps.setNull(i, Types.TIMESTAMP); else ps.setTimestamp(i, Timestamp.valueOf(v));
    }

    /** Chuỗi rỗng/chỉ có khoảng trắng được ghi thành NULL (vd citizen_id UNIQUE). */
    static void setStr(PreparedStatement ps, int i, String v) throws SQLException {
        if (v == null || v.isBlank()) ps.setNull(i, Types.NVARCHAR); else ps.setString(i, v.trim());
    }
}
