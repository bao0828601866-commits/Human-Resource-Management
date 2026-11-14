package com.hr.dao;

import com.hr.model.Position;
import com.hr.model.enums.SalaryType;
import com.hr.utils.DatabaseConnection;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class PositionDAO {

    private static final String SELECT =
            "SELECT position_id, position_name, salary_type, salary_rate FROM Position ";

    /** Toàn bộ chức vụ (nạp JComboBox khi thêm/sửa nhân viên). */
    public List<Position> findAll() throws SQLException {
        List<Position> list = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(SELECT + "ORDER BY position_id");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    public Position findById(int positionId) throws SQLException {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(SELECT + "WHERE position_id = ?")) {
            ps.setInt(1, positionId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    /**
     * Đổi mức lương của chức vụ và áp luôn cho nhân viên ĐANG LÀM thuộc chức vụ đó
     * (Employee.salary_rate là bản sao). Nhân viên đã nghỉ giữ nguyên; Payroll đã chốt không bị đổi.
     */
    public boolean updateSalaryRate(int positionId, BigDecimal newRate) throws SQLException {
        try (Connection c = DatabaseConnection.getConnection()) {
            c.setAutoCommit(false);
            try (PreparedStatement p1 = c.prepareStatement("UPDATE Position SET salary_rate = ? WHERE position_id = ?");
                 PreparedStatement p2 = c.prepareStatement(
                         "UPDATE Employee SET salary_rate = ? WHERE position_id = ? AND status = 'ACTIVE'")) {
                p1.setBigDecimal(1, newRate);
                p1.setInt(2, positionId);
                boolean ok = p1.executeUpdate() > 0;
                p2.setBigDecimal(1, newRate);
                p2.setInt(2, positionId);
                p2.executeUpdate();
                c.commit();
                return ok;
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        }
    }

    private Position map(ResultSet rs) throws SQLException {
        return new Position(rs.getInt("position_id"), rs.getString("position_name"),
                SalaryType.valueOf(rs.getString("salary_type")), rs.getBigDecimal("salary_rate"));
    }
}
