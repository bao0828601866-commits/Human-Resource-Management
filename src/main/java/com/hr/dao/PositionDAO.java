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
     * Đổi mức lương của chức vụ. Lương chỉ lưu ở Position nên mọi nhân viên thuộc chức vụ này
     * (kể cả đã nghỉ) đều lấy mức mới; bảng Payroll đã chốt giữ nguyên số liệu cũ.
     */
    public boolean updateSalaryRate(int positionId, BigDecimal newRate) throws SQLException {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("UPDATE Position SET salary_rate = ? WHERE position_id = ?")) {
            ps.setBigDecimal(1, newRate);
            ps.setInt(2, positionId);
            return ps.executeUpdate() > 0;
        }
    }

    private Position map(ResultSet rs) throws SQLException {
        return new Position(rs.getInt("position_id"), rs.getString("position_name"),
                SalaryType.valueOf(rs.getString("salary_type")), rs.getBigDecimal("salary_rate"));
    }
}