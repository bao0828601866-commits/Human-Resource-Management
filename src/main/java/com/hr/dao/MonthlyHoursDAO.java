package com.hr.dao;

import com.hr.model.MonthlyHours;
import com.hr.utils.DatabaseConnection;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Bảng MonthlyHours. work_days là cột tính sẵn: chỉ đọc. */
public class MonthlyHoursDAO {

    private static final String SELECT =
            "SELECT record_id, employee_id, month, year, hours, work_days FROM MonthlyHours ";

    public List<MonthlyHours> findByMonth(int month, int year) throws SQLException {
        List<MonthlyHours> list = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(SELECT + "WHERE month = ? AND year = ? ORDER BY employee_id")) {
            ps.setInt(1, month);
            ps.setInt(2, year);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        }
        return list;
    }

    public MonthlyHours findOne(String employeeId, int month, int year) throws SQLException {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(SELECT + "WHERE employee_id = ? AND month = ? AND year = ?")) {
            ps.setString(1, employeeId);
            ps.setInt(2, month);
            ps.setInt(3, year);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    /** Gọi sp_RefreshMonthlyHours: tổng hợp giờ làm của tháng từ bảng Attendance. */
    public void refresh(int month, int year) throws SQLException {
        try (Connection c = DatabaseConnection.getConnection();
             CallableStatement cs = c.prepareCall("{call sp_RefreshMonthlyHours(?, ?)}")) {
            cs.setInt(1, month);
            cs.setInt(2, year);
            cs.execute();
        }
    }

    private MonthlyHours map(ResultSet rs) throws SQLException {
        MonthlyHours m = new MonthlyHours();
        m.setRecordId(rs.getInt("record_id"));
        m.setEmployeeId(rs.getString("employee_id"));
        m.setMonth(rs.getInt("month"));
        m.setYear(rs.getInt("year"));
        m.setHours(rs.getBigDecimal("hours"));
        m.setWorkDays(rs.getBigDecimal("work_days"));
        return m;
    }
}
