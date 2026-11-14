package com.hr.controller;

import com.hr.dao.AccountDAO;
import com.hr.model.Account;

import java.sql.SQLException;

public class AuthController {

    private final AccountDAO accountDAO = new AccountDAO();

    /**
     * @return Account nếu đăng nhập đúng (kiểm tra account.isMustChangePassword() để bắt đổi mật khẩu lần đầu),
     *         null nếu sai thông tin / tài khoản bị khóa / nhân viên đã nghỉ
     * @throws IllegalArgumentException nếu để trống mã nhân viên hoặc mật khẩu
     * @throws IllegalStateException    nếu lỗi kết nối database
     */
    public Account login(String employeeId, String password) {
        if (employeeId == null || employeeId.trim().isEmpty() || password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Vui lòng nhập đầy đủ mã nhân viên và mật khẩu.");
        }
        try {
            return accountDAO.login(employeeId.trim().toUpperCase(), password);
        } catch (SQLException e) {
            e.printStackTrace();
            throw new IllegalStateException("Không kết nối được cơ sở dữ liệu.", e);
        }
    }

    /**
     * Đổi mật khẩu (dùng cho hộp thoại "Đổi mật khẩu" ở BaseFrame).
     * @return true nếu thành công, false nếu mật khẩu cũ sai
     * @throws IllegalArgumentException nếu mật khẩu mới không hợp lệ
     */
    public boolean changePassword(String employeeId, String oldPass, String newPass) {
        if (oldPass == null || oldPass.isEmpty()) throw new IllegalArgumentException("Vui lòng nhập mật khẩu cũ.");
        if (newPass == null || newPass.length() < 6) throw new IllegalArgumentException("Mật khẩu mới tối thiểu 6 ký tự.");
        if (newPass.equals(oldPass)) throw new IllegalArgumentException("Mật khẩu mới phải khác mật khẩu cũ.");
        try {
            return accountDAO.changePassword(employeeId, oldPass, newPass);
        } catch (SQLException e) {
            e.printStackTrace();
            throw new IllegalStateException("Không kết nối được cơ sở dữ liệu.", e);
        }
    }
}
