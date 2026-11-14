package com.hr.dao;

import com.hr.dao.DaoUtil.Binder;
import com.hr.model.Attendance;
import com.hr.model.AttendanceLog;
import com.hr.model.WorkSchedule;
import com.hr.model.enums.AttendanceStatus;
import com.hr.utils.DatabaseConnection;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Chấm công (Attendance) + nhật ký sửa (AttendanceLog). 1 lịch làm = 1 lượt chấm công. */
public class AttendanceDAO {

    private static final String SELECT =
            "SELECT a.attendance_id, a.schedule_id, a.employee_id, a.work_date, a.check_in_time, a.check_out_time, "
          + "a.status, a.edit_reason, a.edited_by, a.edited_at, e.full_name, w.start_time, w.end_time "
          + "FROM Attendance a JOIN Employee e ON e.employee_id = a.employee_id "
          + "JOIN WorkSchedule w ON w.schedule_id = a.schedule_id ";

    // ---------------- Đọc ----------------

    /** Chấm công cả cửa hàng trong khoảng ngày, mới nhất trước (tab "Chấm công" của quản lý). */
    public List<Attendance> findByRange(LocalDate from, LocalDate to) throws SQLException {
        return query(SELECT + "WHERE a.work_date BETWEEN ? AND ? ORDER BY a.work_date DESC, a.employee_id",
                ps -> { DaoUtil.setDate(ps, 1, from); DaoUtil.setDate(ps, 2, to); });
    }

    /** Chấm công của một nhân viên, mới nhất trước (nhân viên tự xem). */
    public List<Attendance> findByEmployee(String employeeId, LocalDate from, LocalDate to) throws SQLException {
        return query(SELECT + "WHERE a.employee_id = ? AND a.work_date BETWEEN ? AND ? ORDER BY a.work_date DESC, w.start_time DESC",
                ps -> { ps.setString(1, employeeId); DaoUtil.setDate(ps, 2, from); DaoUtil.setDate(ps, 3, to); });
    }

    public Attendance findByScheduleId(int scheduleId) throws SQLException {
        List<Attendance> l = query(SELECT + "WHERE a.schedule_id = ?", ps -> ps.setInt(1, scheduleId));
        return l.isEmpty() ? null : l.get(0);
    }

    /** Ai quên chấm công ra: đã vào ca nhưng chưa ra, ngày < `before` (mục "phê duyệt nhanh" ở Tổng quan). */
    public List<Attendance> findForgottenCheckout(LocalDate before) throws SQLException {
        return query(SELECT + "WHERE a.check_in_time IS NOT NULL AND a.check_out_time IS NULL AND a.work_date < ? "
                           + "ORDER BY a.work_date DESC, a.employee_id",
                ps -> DaoUtil.setDate(ps, 1, before));
    }

    /** Tổng giờ làm thực tế trong tháng (chỉ tính lượt có đủ giờ vào/ra). */
    public BigDecimal workedHours(String employeeId, int month, int year) throws SQLException {
        String sql = "SELECT ISNULL(CAST(SUM(DATEDIFF(MINUTE, check_in_time, check_out_time)) / 60.0 AS DECIMAL(6,1)), 0) "
                   + "FROM Attendance WHERE employee_id = ? AND MONTH(work_date) = ? AND YEAR(work_date) = ? "
                   + "AND check_in_time IS NOT NULL AND check_out_time IS NOT NULL";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, employeeId);
            ps.setInt(2, month);
            ps.setInt(3, year);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getBigDecimal(1);
            }
        }
    }

    public int lateCount(String employeeId, int month, int year) throws SQLException {
        String sql = "SELECT COUNT(*) FROM Attendance WHERE employee_id = ? AND status = 'LATE' "
                   + "AND MONTH(work_date) = ? AND YEAR(work_date) = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, employeeId);
            ps.setInt(2, month);
            ps.setInt(3, year);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    // ---------------- Nhân viên chấm công ----------------

    /**
     * VÀO CA cho một ca đã chốt. Trễ nếu vào muộn hơn giờ đăng ký quá graceMinutes
     * (DemoData.LATE_GRACE_MIN), ngược lại ON_TIME. Trả về trạng thái đã ghi.
     * Chấm công 2 lần cho cùng một ca -> SQLException (UNIQUE schedule_id).
     */
    public AttendanceStatus clockIn(WorkSchedule s, LocalDateTime now, int graceMinutes) throws SQLException {
        AttendanceStatus st = now.isAfter(LocalDateTime.of(s.getWorkDate(), s.getStartTime()).plusMinutes(graceMinutes))
                ? AttendanceStatus.LATE : AttendanceStatus.ON_TIME;
        String sql = "INSERT INTO Attendance (schedule_id, employee_id, work_date, check_in_time, status) "
                   + "SELECT schedule_id, employee_id, work_date, ?, ? FROM WorkSchedule "
                   + "WHERE schedule_id = ? AND status = 'CONFIRMED'";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            DaoUtil.setTs(ps, 1, now);
            ps.setString(2, st.name());
            ps.setInt(3, s.getScheduleId());
            if (ps.executeUpdate() == 0) throw new IllegalStateException("Ca này chưa được chốt hoặc không tồn tại.");
        }
        return st;
    }

    /**
     * RA CA. Đã LATE thì giữ LATE; ra trước giờ kết thúc đã đăng ký -> EARLY_LEAVE; còn lại ON_TIME.
     * false nếu chưa vào ca hoặc đã ra rồi.
     */
    public boolean clockOut(int scheduleId, LocalDateTime out) throws SQLException {
        String sql = "UPDATE a SET check_out_time = ?, status = CASE WHEN a.status = 'LATE' THEN 'LATE' "
                   + "WHEN ? < CAST(a.work_date AS DATETIME) + CAST(w.end_time AS DATETIME) THEN 'EARLY_LEAVE' "
                   + "ELSE 'ON_TIME' END "
                   + "FROM Attendance a JOIN WorkSchedule w ON w.schedule_id = a.schedule_id "
                   + "WHERE a.schedule_id = ? AND a.check_in_time IS NOT NULL AND a.check_out_time IS NULL";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            DaoUtil.setTs(ps, 1, out);
            DaoUtil.setTs(ps, 2, out);
            ps.setInt(3, scheduleId);
            return ps.executeUpdate() > 0;
        }
    }

    // ---------------- Cửa hàng trưởng sửa (có lưu vết) ----------------

    /**
     * Sửa giờ vào/ra/trạng thái. Bắt buộc có lý do. Cập nhật Attendance (edit_reason, edited_by, edited_at)
     * và ghi AttendanceLog (trước/sau) trong cùng một giao dịch.
     */
    public void edit(int attendanceId, LocalDateTime newIn, LocalDateTime newOut, AttendanceStatus newStatus,
                     String reason, String editorId) throws SQLException {
        requireReason(reason);
        inTx(c -> apply(c, load(c, attendanceId), newIn, newOut, newStatus, reason.trim(), editorId));
    }

    /** Ghi nhận nhanh: đi trễ / về sớm / vắng. VẮNG xóa giờ vào-ra; trạng thái khác giữ nguyên giờ. */
    public void quickMark(int attendanceId, AttendanceStatus status, String reason, String editorId) throws SQLException {
        requireReason(reason);
        inTx(c -> {
            Attendance old = load(c, attendanceId);
            boolean absent = status == AttendanceStatus.ABSENT;
            apply(c, old, absent ? null : old.getCheckInTime(), absent ? null : old.getCheckOutTime(),
                    status, reason.trim(), editorId);
        });
    }

    /** Nhật ký chỉnh sửa, mới nhất trước (tab "Lịch sử chỉnh sửa"). */
    public List<AttendanceLog> findLogs() throws SQLException {
        String sql = "SELECT l.log_id, l.attendance_id, l.edited_by, l.edited_at, l.old_in, l.old_out, l.old_status, "
                   + "l.new_in, l.new_out, l.new_status, l.reason, a.employee_id, a.work_date "
                   + "FROM AttendanceLog l JOIN Attendance a ON a.attendance_id = l.attendance_id "
                   + "ORDER BY l.edited_at DESC, l.log_id DESC";
        List<AttendanceLog> list = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                AttendanceLog g = new AttendanceLog();
                g.setLogId(rs.getInt("log_id"));
                g.setAttendanceId(rs.getInt("attendance_id"));
                g.setEditedBy(rs.getString("edited_by"));
                g.setEditedAt(DaoUtil.ts(rs, "edited_at"));
                g.setOldIn(DaoUtil.ts(rs, "old_in"));
                g.setOldOut(DaoUtil.ts(rs, "old_out"));
                g.setOldStatus(status(rs.getString("old_status")));
                g.setNewIn(DaoUtil.ts(rs, "new_in"));
                g.setNewOut(DaoUtil.ts(rs, "new_out"));
                g.setNewStatus(status(rs.getString("new_status")));
                g.setReason(rs.getString("reason"));
                g.setEmployeeId(rs.getString("employee_id"));
                g.setWorkDate(DaoUtil.date(rs, "work_date"));
                list.add(g);
            }
        }
        return list;
    }

    // ---------------- Nội bộ ----------------
    @FunctionalInterface
    private interface TxWork { void run(Connection c) throws SQLException; }

    private void inTx(TxWork work) throws SQLException {
        try (Connection c = DatabaseConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                work.run(c);
                c.commit();
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            }
        }
    }

    private static void requireReason(String reason) {
        if (reason == null || reason.isBlank()) throw new IllegalArgumentException("Bắt buộc nhập lý do sửa chấm công.");
    }

    /** Đọc bản ghi hiện tại và khóa dòng để không sửa chồng nhau. */
    private Attendance load(Connection c, int id) throws SQLException {
        String sql = "SELECT attendance_id, check_in_time, check_out_time, status FROM Attendance WITH (UPDLOCK) WHERE attendance_id = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new IllegalStateException("Không tìm thấy bản ghi chấm công #" + id);
                Attendance a = new Attendance();
                a.setAttendanceId(rs.getInt("attendance_id"));
                a.setCheckInTime(DaoUtil.ts(rs, "check_in_time"));
                a.setCheckOutTime(DaoUtil.ts(rs, "check_out_time"));
                a.setStatus(status(rs.getString("status")));
                return a;
            }
        }
    }

    private void apply(Connection c, Attendance old, LocalDateTime in, LocalDateTime out, AttendanceStatus st,
                       String reason, String editorId) throws SQLException {
        if (in != null && out != null && !out.isAfter(in)) throw new IllegalArgumentException("Giờ ra phải sau giờ vào.");

        String upd = "UPDATE Attendance SET check_in_time = ?, check_out_time = ?, status = ?, edit_reason = ?, "
                   + "edited_by = ?, edited_at = GETDATE() WHERE attendance_id = ?";
        String log = "INSERT INTO AttendanceLog (attendance_id, edited_by, old_in, old_out, old_status, "
                   + "new_in, new_out, new_status, reason) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pu = c.prepareStatement(upd); PreparedStatement pl = c.prepareStatement(log)) {
            DaoUtil.setTs(pu, 1, in);
            DaoUtil.setTs(pu, 2, out);
            pu.setString(3, st.name());
            pu.setString(4, reason);
            pu.setString(5, editorId);
            pu.setInt(6, old.getAttendanceId());
            pu.executeUpdate();

            pl.setInt(1, old.getAttendanceId());
            pl.setString(2, editorId);
            DaoUtil.setTs(pl, 3, old.getCheckInTime());
            DaoUtil.setTs(pl, 4, old.getCheckOutTime());
            pl.setString(5, old.getStatus() == null ? null : old.getStatus().name());
            DaoUtil.setTs(pl, 6, in);
            DaoUtil.setTs(pl, 7, out);
            pl.setString(8, st.name());
            pl.setString(9, reason);
            pl.executeUpdate();
        }
    }

    private List<Attendance> query(String sql, Binder b) throws SQLException {
        List<Attendance> list = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            b.bind(ps);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Attendance a = new Attendance();
                    a.setAttendanceId(rs.getInt("attendance_id"));
                    a.setScheduleId(rs.getInt("schedule_id"));
                    a.setEmployeeId(rs.getString("employee_id"));
                    a.setWorkDate(DaoUtil.date(rs, "work_date"));
                    a.setCheckInTime(DaoUtil.ts(rs, "check_in_time"));
                    a.setCheckOutTime(DaoUtil.ts(rs, "check_out_time"));
                    a.setStatus(status(rs.getString("status")));
                    a.setEditReason(rs.getString("edit_reason"));
                    a.setEditedBy(rs.getString("edited_by"));
                    a.setEditedAt(DaoUtil.ts(rs, "edited_at"));
                    a.setFullName(rs.getString("full_name"));
                    a.setScheduledStart(DaoUtil.time(rs, "start_time"));
                    a.setScheduledEnd(DaoUtil.time(rs, "end_time"));
                    list.add(a);
                }
            }
        }
        return list;
    }

    private static AttendanceStatus status(String s) { return s == null ? null : AttendanceStatus.valueOf(s); }
}
