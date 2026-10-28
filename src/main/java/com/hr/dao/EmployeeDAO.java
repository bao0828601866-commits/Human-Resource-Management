package com.hr.dao;

import com.hr.model.Employee;
import com.hr.model.EmployeeStatus;
import com.hr.model.SalaryType;
import com.hr.utils.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class EmployeeDAO {

    /** Chuyển 1 dòng ResultSet thành đối tượng Employee */
    private Employee mapRow(ResultSet rs) throws SQLException {
        return new Employee(
            rs.getString("employee_id"),
            rs.getString("full_name"),
            rs.getString("position"),
            SalaryType.valueOf(rs.getString("salary_type")),
            rs.getBigDecimal("salary_rate"),
            rs.getString("phone_number"),
            EmployeeStatus.valueOf(rs.getString("status"))
        );
    }

    /** Lấy toàn bộ nhân viên */
    public List<Employee> getAll() {
        List<Employee> list = new ArrayList<>();
        String sql = "SELECT * FROM Employee ORDER BY employee_id";

        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    /** Tìm 1 nhân viên theo mã, không có thì trả về null */
    public Employee findById(String employeeId) {
        String sql = "SELECT * FROM Employee WHERE employee_id = ?";

        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, employeeId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
}