package com.hr.controller;

import com.hr.dao.EmployeeDAO;
import com.hr.model.enums.Gender;

import java.sql.SQLException;

public class EmployeeController {

    private final EmployeeDAO employeeDAO = new EmployeeDAO();

    /**
     * Nhân viên tự cập nhật SĐT, địa chỉ, giới tính.
     * @return true nếu cập nhật được, false nếu không tìm thấy nhân viên
     * @throws IllegalArgumentException nếu dữ liệu không hợp lệ
     * @throws IllegalStateException    nếu lỗi kết nối database
     */
    public boolean updateProfile(String employeeId, String phone, String address, Gender gender) {
        String p = phone == null ? "" : phone.trim();
        String a = address == null ? "" : address.trim();
        if (!p.matches("0\\d{9}")) throw new IllegalArgumentException("SĐT phải gồm 10 số, bắt đầu bằng 0.");
        if (a.length() > 255) throw new IllegalArgumentException("Địa chỉ tối đa 255 ký tự.");
        try {
            return employeeDAO.updateProfile(employeeId, p, a.isEmpty() ? null : a, gender);
        } catch (SQLException e) {
            e.printStackTrace();
            throw new IllegalStateException("Không kết nối được cơ sở dữ liệu.", e);
        }
    }
}