package com.hr.dao;

import com.hr.utils.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class OverviewDao {
    private Connection connect() throws SQLException {
        return DatabaseConnection.getConnection();
    }

    // ---------- DTO ----------
    public static final class StaffStats {
        public int active, fullTime, partTime, resigned, inShift;
        /** Chức vụ -> số nhân viên đang làm (cho biểu đồ tròn). */
        public final Map<String, Integer> byPosition = new LinkedHashMap<>();
    }

    public static final class NextShift {
        public LocalDate date;
        public String shift;
        public LocalTime start, end;
    }

    public static final class Leave {
        public int id;
        public String empId, empName, type, shift, newShift, reason, status;
        public LocalDate date;
        public LocalTime newStart, newEnd;
    }

    // ---------- Nhân sự ----------
    public StaffStats staffStats(LocalDateTime now) throws SQLException {
        StaffStats s = new StaffStats();
        try (Connection c = connect()) {
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT e.status, p.salary_type, COUNT(*) FROM Employee e "
                            + "JOIN Position p ON p.position_id = e.position_id "
                            + "GROUP BY e.status, p.salary_type");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int n = rs.getInt(3);
                    if ("ACTIVE".equals(rs.getString(1))) {
                        s.active += n;
                        if ("FIXED".equals(rs.getString(2))) s.fullTime += n;
                        else s.partTime += n;
                    } else {
                        s.resigned += n;
                    }
                }
            }
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT p.position_name, COUNT(*) FROM Employee e "
                            + "JOIN Position p ON p.position_id = e.position_id "
                            + "WHERE e.status = 'ACTIVE' "
                            + "GROUP BY p.position_name ORDER BY COUNT(*) DESC, p.position_name");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) s.byPosition.put(rs.getString(1), rs.getInt(2));
            }
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT COUNT(DISTINCT w.employee_id) FROM WorkSchedule w "
                            + "JOIN Employee e ON e.employee_id = w.employee_id AND e.status = 'ACTIVE' "
                            + "WHERE w.work_date = ? AND w.status = 'CONFIRMED' "
                            + "AND w.start_time <= CAST(? AS TIME) AND w.end_time > CAST(? AS TIME)")) {
                Time t = Time.valueOf(now.toLocalTime());
                ps.setDate(1, Date.valueOf(now.toLocalDate()));
                ps.setTime(2, t);
                ps.setTime(3, t);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) s.inShift = rs.getInt(1);
                }
            }
        }
        return s;
    }

    // ---------- Ca làm tiếp theo của riêng một nhân viên ----------
    public List<NextShift> nextShifts(String empId, LocalDateTime now, int limit) throws SQLException {
        List<NextShift> out = new ArrayList<>();
        try (Connection c = connect();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT TOP (?) work_date, shift_name, start_time, end_time FROM WorkSchedule "
                             + "WHERE employee_id = ? AND status = 'CONFIRMED' "
                             + "AND (work_date > ? OR (work_date = ? AND start_time > CAST(? AS TIME))) "
                             + "ORDER BY work_date, start_time")) {
            Date d = Date.valueOf(now.toLocalDate());
            ps.setInt(1, limit);
            ps.setString(2, empId);
            ps.setDate(3, d);
            ps.setDate(4, d);
            ps.setTime(5, Time.valueOf(now.toLocalTime()));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    NextShift n = new NextShift();
                    n.date = rs.getDate(1).toLocalDate();
                    n.shift = rs.getString(2);
                    n.start = rs.getTime(3).toLocalTime();
                    n.end = rs.getTime(4).toLocalTime();
                    out.add(n);
                }
            }
        }
        return out;
    }

    // ---------- Doanh thu (triệu đồng, mảng 12 tháng, tháng chưa có = 0) ----------
    public double[] revenueByMonth(int year) throws SQLException {
        double[] v = new double[12];
        try (Connection c = connect();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT month, total_revenue FROM MonthlyRevenue WHERE year = ?")) {
            ps.setInt(1, year);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) v[rs.getInt(1) - 1] = rs.getDouble(2) / 1_000_000.0;
            }
        }
        return v;
    }

    // ---------- Đơn nghỉ / đổi ca ----------
    /** Đơn chờ duyệt lên đầu, sau đó theo ngày. */
    public List<Leave> leaves() throws SQLException {
        List<Leave> out = new ArrayList<>();
        try (Connection c = connect();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT TOP 50 r.request_id, r.employee_id, e.full_name, r.request_type, r.target_date, "
                             + "r.shift_name, r.new_shift_name, r.new_start_time, r.new_end_time, r.reason, r.status "
                             + "FROM LeaveRequest r JOIN Employee e ON e.employee_id = r.employee_id "
                             + "ORDER BY CASE WHEN r.status = 'PENDING' THEN 0 ELSE 1 END, r.target_date DESC, r.request_id DESC");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Leave l = new Leave();
                l.id = rs.getInt(1);
                l.empId = rs.getString(2);
                l.empName = rs.getString(3);
                l.type = rs.getString(4);
                l.date = rs.getDate(5).toLocalDate();
                l.shift = rs.getString(6);
                l.newShift = rs.getString(7);
                Time ns = rs.getTime(8), ne = rs.getTime(9);
                l.newStart = ns == null ? null : ns.toLocalTime();
                l.newEnd = ne == null ? null : ne.toLocalTime();
                l.reason = rs.getString(10);
                l.status = rs.getString(11);
                out.add(l);
            }
        }
        return out;
    }

    /**
     * Duyệt / từ chối đơn trong 1 transaction.
     * Duyệt: LEAVE -> hủy ca; SHIFT_CHANGE -> chuyển sang ca/giờ mới (trigger của WorkSchedule vẫn kiểm tra hợp lệ).
     * @throws IllegalStateException đơn đã được xử lý trước đó
     * @throws SQLException          trigger/CSDL từ chối (thông báo lỗi nằm trong getMessage())
     */
    public void decideLeave(int id, boolean approve, String reviewerId) throws SQLException {
        try (Connection c = connect()) {
            c.setAutoCommit(false);
            try {
                int n;
                try (PreparedStatement ps = c.prepareStatement(
                        "UPDATE LeaveRequest SET status = ?, reviewed_by = ?, reviewed_at = GETDATE() "
                                + "WHERE request_id = ? AND status = 'PENDING'")) {
                    ps.setString(1, approve ? "APPROVED" : "REJECTED");
                    ps.setString(2, reviewerId);
                    ps.setInt(3, id);
                    n = ps.executeUpdate();
                }
                if (n == 0) throw new IllegalStateException("Đơn này đã được xử lý.");
                if (approve) applyToSchedule(c, id);
                c.commit();
            } catch (SQLException | RuntimeException ex) {
                c.rollback();
                throw ex;
            }
        }
    }

    private void applyToSchedule(Connection c, int id) throws SQLException {
        String empId, type, shift, newShift;
        Date date;
        Time ns, ne;
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT employee_id, request_type, target_date, shift_name, new_shift_name, "
                        + "new_start_time, new_end_time FROM LeaveRequest WHERE request_id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return;
                empId = rs.getString(1);
                type = rs.getString(2);
                date = rs.getDate(3);
                shift = rs.getString(4);
                newShift = rs.getString(5);
                ns = rs.getTime(6);
                ne = rs.getTime(7);
            }
        }

        if ("LEAVE".equals(type)) {
            String sql = "UPDATE WorkSchedule SET status = 'CANCELLED' "
                    + "WHERE employee_id = ? AND work_date = ? AND status <> 'CANCELLED'"
                    + (shift == null ? "" : " AND shift_name = ?");
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, empId);
                ps.setDate(2, date);
                if (shift != null) ps.setString(3, shift);
                ps.executeUpdate();
            }
        } else {
            try (PreparedStatement ps = c.prepareStatement(
                    "UPDATE WorkSchedule SET shift_name = ?, start_time = ?, end_time = ? "
                            + "WHERE employee_id = ? AND work_date = ? AND shift_name = ? AND status <> 'CANCELLED'")) {
                ps.setString(1, newShift);
                ps.setTime(2, ns);
                ps.setTime(3, ne);
                ps.setString(4, empId);
                ps.setDate(5, date);
                ps.setString(6, shift);
                ps.executeUpdate();
            }
        }
    }
}