package com.hr.dao;

import com.hr.model.Notification;
import com.hr.utils.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/** Notification + NotificationRead. Trạng thái "đã đọc" tính theo từng người xem. */
public class NotificationDAO {

    private static final String SELECT =
            "SELECT n.notification_id, n.sender_id, n.receiver_id, n.title, n.content, n.created_at, "
          + "CASE WHEN EXISTS (SELECT 1 FROM NotificationRead r WHERE r.notification_id = n.notification_id "
          + "AND r.employee_id = ?) THEN 1 ELSE 0 END AS is_read FROM Notification n ";

    /** Thông báo gửi cho tôi (gửi tất cả hoặc gửi riêng), không gồm thông báo tôi tự gửi. Mới nhất trước. */
    public List<Notification> findForEmployee(String employeeId) throws SQLException {
        String sql = SELECT + "WHERE (n.receiver_id IS NULL OR n.receiver_id = ?) AND n.sender_id <> ? "
                   + "ORDER BY n.created_at DESC, n.notification_id DESC";
        return query(sql, employeeId, employeeId, employeeId);
    }

    /** Thông báo do tôi gửi (tab "Đã gửi" của cửa hàng trưởng). */
    public List<Notification> findSentBy(String senderId) throws SQLException {
        String sql = SELECT + "WHERE n.sender_id = ? ORDER BY n.created_at DESC, n.notification_id DESC";
        return query(sql, senderId, senderId);
    }

    public int countUnread(String employeeId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM Notification n WHERE (n.receiver_id IS NULL OR n.receiver_id = ?) "
                   + "AND n.sender_id <> ? AND NOT EXISTS (SELECT 1 FROM NotificationRead r "
                   + "WHERE r.notification_id = n.notification_id AND r.employee_id = ?)";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, employeeId);
            ps.setString(2, employeeId);
            ps.setString(3, employeeId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    /** Gửi thông báo. receiverId null = gửi tất cả. Trả về mã thông báo mới. */
    public int send(String senderId, String receiverId, String title, String content) throws SQLException {
        String sql = "INSERT INTO Notification (sender_id, receiver_id, title, content) VALUES (?, ?, ?, ?)";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, senderId);
            if (receiverId == null || receiverId.isBlank()) ps.setNull(2, Types.VARCHAR); else ps.setString(2, receiverId);
            ps.setString(3, title);
            ps.setString(4, content);
            ps.executeUpdate();
            try (ResultSet k = ps.getGeneratedKeys()) {
                return k.next() ? k.getInt(1) : 0;
            }
        }
    }

    public void markRead(int notificationId, String employeeId) throws SQLException {
        String sql = "INSERT INTO NotificationRead (notification_id, employee_id) SELECT ?, ? "
                   + "WHERE NOT EXISTS (SELECT 1 FROM NotificationRead WHERE notification_id = ? AND employee_id = ?)";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, notificationId);
            ps.setString(2, employeeId);
            ps.setInt(3, notificationId);
            ps.setString(4, employeeId);
            ps.executeUpdate();
        }
    }

    /** Nút "Đánh dấu tất cả đã đọc". */
    public int markAllRead(String employeeId) throws SQLException {
        String sql = "INSERT INTO NotificationRead (notification_id, employee_id) "
                   + "SELECT n.notification_id, ? FROM Notification n "
                   + "WHERE (n.receiver_id IS NULL OR n.receiver_id = ?) AND n.sender_id <> ? "
                   + "AND NOT EXISTS (SELECT 1 FROM NotificationRead r "
                   + "WHERE r.notification_id = n.notification_id AND r.employee_id = ?)";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, employeeId);
            ps.setString(2, employeeId);
            ps.setString(3, employeeId);
            ps.setString(4, employeeId);
            return ps.executeUpdate();
        }
    }

    private List<Notification> query(String sql, String... args) throws SQLException {
        List<Notification> list = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < args.length; i++) ps.setString(i + 1, args[i]);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Notification n = new Notification();
                    n.setNotificationId(rs.getInt("notification_id"));
                    n.setSenderId(rs.getString("sender_id"));
                    n.setReceiverId(rs.getString("receiver_id"));
                    n.setTitle(rs.getString("title"));
                    n.setContent(rs.getString("content"));
                    n.setCreatedAt(DaoUtil.ts(rs, "created_at"));
                    n.setRead(rs.getInt("is_read") == 1);
                    list.add(n);
                }
            }
        }
        return list;
    }
}
