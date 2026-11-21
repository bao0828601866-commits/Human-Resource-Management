package com.hr.dao;

import com.hr.utils.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Hộp thư của một nhân viên + trạng thái đã đọc (bảng Notification, NotificationRead). */
public class NotificationReadDAO {

    public static final class Item {
        public final int id;
        public final String title, content;
        public final LocalDateTime createdAt;
        public final boolean read;

        Item(int id, String title, String content, LocalDateTime createdAt, boolean read) {
            this.id = id;
            this.title = title;
            this.content = content;
            this.createdAt = createdAt;
            this.read = read;
        }
    }

    /** Thông báo gửi riêng cho người này + thông báo gửi tất cả. Chưa đọc lên trước, mới nhất lên trước. */
    public List<Item> findInbox(String employeeId) throws SQLException {
        String sql = "SELECT n.notification_id, n.title, n.content, n.created_at, "
                   + "CASE WHEN r.employee_id IS NULL THEN 0 ELSE 1 END AS is_read "
                   + "FROM Notification n "
                   + "LEFT JOIN NotificationRead r ON r.notification_id = n.notification_id AND r.employee_id = ? "
                   + "WHERE n.receiver_id IS NULL OR n.receiver_id = ? "
                   + "ORDER BY is_read ASC, n.created_at DESC";
        List<Item> list = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, employeeId);
            ps.setString(2, employeeId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new Item(rs.getInt("notification_id"), rs.getString("title"),
                            rs.getString("content"), rs.getTimestamp("created_at").toLocalDateTime(),
                            rs.getInt("is_read") == 1));
                }
            }
        }
        return list;
    }

    /** Ghi NotificationRead cho mọi thông báo chưa đọc của người này. Trả về số dòng vừa ghi. */
    public int markAllRead(String employeeId) throws SQLException {
        String sql = "INSERT INTO NotificationRead (notification_id, employee_id) "
                   + "SELECT n.notification_id, ? FROM Notification n "
                   + "WHERE (n.receiver_id IS NULL OR n.receiver_id = ?) "
                   + "AND NOT EXISTS (SELECT 1 FROM NotificationRead r "
                   + "                WHERE r.notification_id = n.notification_id AND r.employee_id = ?)";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, employeeId);
            ps.setString(2, employeeId);
            ps.setString(3, employeeId);
            return ps.executeUpdate();
        }
    }
}