package com.hr.dao;

import com.hr.model.Account;
import com.hr.model.enums.Role;
import com.hr.utils.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class AccountDAO {
    public static final String DEFAULT_PASSWORD = "123456";

    /**
     * Đăng nhập. Đúng mật khẩu + tài khoản đang mở + nhân viên còn làm -> Account
     * (kèm cờ must_change_password); ngược lại null. Lỗi DB được ném ra ngoài.
     */
    public Account login(String employeeId, String password) throws SQLException {
        String sql = "SELECT a.employee_id, a.role, a.must_change_password "
                   + "FROM Account a JOIN Employee e ON a.employee_id = e.employee_id "
                   + "WHERE a.employee_id = ? AND a.password = ? AND a.is_active = 1 AND e.status = 'ACTIVE'";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, employeeId);
            ps.setString(2, password);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Account(rs.getString("employee_id"),
                            Role.valueOf(rs.getString("role")),
                            rs.getBoolean("must_change_password"));
                }
            }
        }
        return null;
    }

    /** Đổi mật khẩu (tắt cờ must_change_password). false nếu mật khẩu cũ sai. */
    public boolean changePassword(String employeeId, String oldPass, String newPass) throws SQLException {
        String sql = "UPDATE Account SET password = ?, must_change_password = 0 "
                   + "WHERE employee_id = ? AND password = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, newPass);
            ps.setString(2, employeeId);
            ps.setString(3, oldPass);
            return ps.executeUpdate() > 0;
        }
    }

    /** Đặt lại mật khẩu mặc định, bắt đổi ở lần đăng nhập sau. */
    public boolean resetPassword(String employeeId) throws SQLException {
        String sql = "UPDATE Account SET password = ?, must_change_password = 1 WHERE employee_id = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, DEFAULT_PASSWORD);
            ps.setString(2, employeeId);
            return ps.executeUpdate() > 0;
        }
    }

    /** Khóa / mở tài khoản thủ công (nghỉ việc đã được trigger xử lý tự động). */
    public boolean setActive(String employeeId, boolean active) throws SQLException {
        String sql = "UPDATE Account SET is_active = ? WHERE employee_id = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setBoolean(1, active);
            ps.setString(2, employeeId);
            return ps.executeUpdate() > 0;
        }
    }

    /** Tạo tài khoản cho nhân viên đã có trong Employee (mật khẩu mặc định nếu password null). */
    public void createAccount(String employeeId, String password, Role role) throws SQLException {
        String sql = "INSERT INTO Account (employee_id, password, role) VALUES (?, ?, ?)";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, employeeId);
            ps.setString(2, password == null ? DEFAULT_PASSWORD : password);
            ps.setString(3, role.name());
            ps.executeUpdate();
        }
    }
}
