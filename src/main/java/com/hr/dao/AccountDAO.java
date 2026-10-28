package com.hr.dao;

import com.hr.model.Account;
import com.hr.model.Role;
import com.hr.utils.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class AccountDAO {

    /** Đăng nhập đúng trả về Account, sai hoặc đã nghỉ việc trả về null */
    public Account login(String username, String password) {
        // Bước 1: SQL, chỉ cho nhân viên còn làm đăng nhập
        String sql = "SELECT a.username, a.role, a.employee_id "
                   + "FROM Account a JOIN Employee e ON a.employee_id = e.employee_id "
                   + "WHERE a.username = ? AND a.password = ? AND e.status = 'ACTIVE'";

        // Bước 2: mở kết nối (try-with-resources tự đóng khi xong)
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            // Bước 3: gán giá trị cho 2 dấu ?
            ps.setString(1, username);
            ps.setString(2, password);

            // Bước 4 + 5: chạy và đọc kết quả
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Account(
                        rs.getString("username"),
                        Role.valueOf(rs.getString("role")),
                        rs.getString("employee_id")
                    );
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
}