package com.hr.controller;

import com.hr.dao.NotificationReadDAO;

import java.sql.SQLException;
import java.util.List;

public class NotificationController {

    private final NotificationReadDAO dao = new NotificationReadDAO();

    /** @throws IllegalStateException nếu lỗi kết nối database */
    public List<NotificationReadDAO.Item> inbox(String employeeId) {
        try {
            return dao.findInbox(employeeId);
        } catch (SQLException e) {
            e.printStackTrace();
            throw new IllegalStateException("Không kết nối được cơ sở dữ liệu.", e);
        }
    }

    /** @throws IllegalStateException nếu lỗi kết nối database */
    public void markAllRead(String employeeId) {
        try {
            dao.markAllRead(employeeId);
        } catch (SQLException e) {
            e.printStackTrace();
            throw new IllegalStateException("Không kết nối được cơ sở dữ liệu.", e);
        }
    }
}