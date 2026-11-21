package com.hr.dao;

import com.hr.dao.DaoUtil.Binder;
import com.hr.model.WorkSchedule;
import com.hr.model.enums.SalaryType;
import com.hr.model.enums.ScheduleStatus;
import com.hr.utils.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Lịch làm (WorkSchedule). Đọc qua view vw_ScheduleDetail (có tên + loại nhân viên).
 * Mọi lệnh ghi đều qua trigger trg_WorkSchedule_Validate: vi phạm quy tắc ca sẽ ném SQLException
 * (nội dung RAISERROR nằm trong getMessage() hoặc getNextException()).
 */
public class WorkScheduleDAO {

    private static final String SELECT =
            "SELECT schedule_id, employee_id, full_name, salary_type, work_date, shift_name, start_time, end_time, status "
          + "FROM vw_ScheduleDetail ";

    // ---------------- Đọc ----------------

    /** Mọi lịch (mọi trạng thái) trong khoảng ngày [from, to]: dựng lưới tuần + danh sách đăng ký chờ chốt. */
    public List<WorkSchedule> findByRange(LocalDate from, LocalDate to) throws SQLException {
        return query(SELECT + "WHERE work_date BETWEEN ? AND ? ORDER BY work_date, start_time, employee_id",
                ps -> { DaoUtil.setDate(ps, 1, from); DaoUtil.setDate(ps, 2, to); });
    }

    /** Lịch của một nhân viên trong khoảng ngày (tuần này + tuần sau). */
    public List<WorkSchedule> findByEmployee(String employeeId, LocalDate from, LocalDate to) throws SQLException {
        return query(SELECT + "WHERE employee_id = ? AND work_date BETWEEN ? AND ? ORDER BY work_date, start_time",
                ps -> { ps.setString(1, employeeId); DaoUtil.setDate(ps, 2, from); DaoUtil.setDate(ps, 3, to); });
    }

    /** Ca ĐÃ CHỐT của nhân viên trong ngày, ca sớm nhất trước (dùng cho nút VÀO CA). */
    public List<WorkSchedule> findConfirmedOn(String employeeId, LocalDate date) throws SQLException {
        return query(SELECT + "WHERE employee_id = ? AND work_date = ? AND status = 'CONFIRMED' ORDER BY start_time",
                ps -> { ps.setString(1, employeeId); DaoUtil.setDate(ps, 2, date); });
    }

    /** Nhân viên đang trong ca tại thời điểm `now` ("Nhân viên đang trong ca"). */
    public List<WorkSchedule> findInShift(LocalDateTime now) throws SQLException {
        return query(SELECT + "WHERE status = 'CONFIRMED' AND work_date = ? AND start_time <= CAST(? AS TIME) AND end_time > CAST(? AS TIME) "
                           + "ORDER BY start_time, employee_id",
                ps -> { DaoUtil.setDate(ps, 1, now.toLocalDate()); DaoUtil.setTime(ps, 2, now.toLocalTime());
                        DaoUtil.setTime(ps, 3, now.toLocalTime()); });
    }

    /** `limit` ca đã chốt sắp bắt đầu ("Ca làm tiếp theo"). */
    public List<WorkSchedule> findUpcoming(LocalDateTime now, int limit) throws SQLException {
        return query(SELECT.replace("SELECT ", "SELECT TOP (" + Math.max(1, limit) + ") ")
                + "WHERE status = 'CONFIRMED' AND (work_date > ? OR (work_date = ? AND start_time > CAST(? AS TIME))) "
                + "ORDER BY work_date, start_time, employee_id",
                ps -> { DaoUtil.setDate(ps, 1, now.toLocalDate()); DaoUtil.setDate(ps, 2, now.toLocalDate());
                        DaoUtil.setTime(ps, 3, now.toLocalTime()); });
    }

    /** Ca đã chốt kế tiếp (chưa kết thúc) của một nhân viên; null nếu không có. */
    public WorkSchedule findNextForEmployee(String employeeId, LocalDateTime now) throws SQLException {
        List<WorkSchedule> l = query(SELECT.replace("SELECT ", "SELECT TOP (1) ")
                + "WHERE employee_id = ? AND status = 'CONFIRMED' AND (work_date > ? OR (work_date = ? AND end_time > CAST(? AS TIME))) "
                + "ORDER BY work_date, start_time",
                ps -> { ps.setString(1, employeeId); DaoUtil.setDate(ps, 2, now.toLocalDate());
                        DaoUtil.setDate(ps, 3, now.toLocalDate()); DaoUtil.setTime(ps, 4, now.toLocalTime()); });
        return l.isEmpty() ? null : l.get(0);
    }

    /** Nhân viên có ca (chưa hủy) vào ngày đó không? shiftName null = bất kỳ ca nào (kiểm tra khi gửi đơn). */
    public boolean hasSchedule(String employeeId, LocalDate date, String shiftName) throws SQLException {
        String sql = "SELECT TOP 1 1 FROM WorkSchedule WHERE employee_id = ? AND work_date = ? AND status <> 'CANCELLED'"
                   + (shiftName != null ? " AND shift_name = ?" : "");
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, employeeId);
            DaoUtil.setDate(ps, 2, date);
            if (shiftName != null) ps.setString(3, shiftName);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * Số người ĐÃ CHỐT làm cùng lúc nhiều nhất trong [from, to) của ngày đó (bỏ qua lịch `ignoreId`).
     * So với DemoData.MAX_CONCURRENT trước khi chốt ca.
     */
    public int peakConcurrent(LocalDate date, LocalTime from, LocalTime to, int ignoreId) throws SQLException {
        String sql = "SELECT start_time, end_time FROM WorkSchedule WHERE work_date = ? AND status = 'CONFIRMED' "
                   + "AND schedule_id <> ? AND start_time < CAST(? AS TIME) AND end_time > CAST(? AS TIME)";
        List<LocalTime[]> rows = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            DaoUtil.setDate(ps, 1, date);
            ps.setInt(2, ignoreId);
            DaoUtil.setTime(ps, 3, to);
            DaoUtil.setTime(ps, 4, from);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) rows.add(new LocalTime[]{DaoUtil.time(rs, "start_time"), DaoUtil.time(rs, "end_time")});
            }
        }
        List<LocalTime> pts = new ArrayList<>();
        pts.add(from);
        for (LocalTime[] r : rows) if (r[0].isAfter(from)) pts.add(r[0]);
        int max = 0;
        for (LocalTime t : pts) {
            int n = 0;
            for (LocalTime[] r : rows) if (!r[0].isAfter(t) && t.isBefore(r[1])) n++;
            max = Math.max(max, n);
        }
        return max;
    }

    // ---------------- Ghi ----------------

    /** Nhân viên đăng ký ca (trạng thái REGISTERED). Trả về schedule_id mới. */
    public int register(String employeeId, LocalDate date, String shiftName, LocalTime from, LocalTime to)
            throws SQLException {
        String sql = "INSERT INTO WorkSchedule (employee_id, work_date, shift_name, start_time, end_time, status) "
                   + "VALUES (?, ?, ?, ?, ?, 'REGISTERED')";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, employeeId);
            DaoUtil.setDate(ps, 2, date);
            ps.setString(3, shiftName);
            DaoUtil.setTime(ps, 4, from);
            DaoUtil.setTime(ps, 5, to);
            ps.executeUpdate();
            try (ResultSet k = ps.getGeneratedKeys()) {
                return k.next() ? k.getInt(1) : 0;
            }
        }
    }

    /** Cửa hàng trưởng chốt ca (có thể chỉnh giờ part-time). Chỉ chốt được ca đang REGISTERED. */
    public boolean confirm(int scheduleId, LocalTime from, LocalTime to) throws SQLException {
        String sql = "UPDATE WorkSchedule SET start_time = ?, end_time = ?, status = 'CONFIRMED' "
                   + "WHERE schedule_id = ? AND status = 'REGISTERED'";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            DaoUtil.setTime(ps, 1, from);
            DaoUtil.setTime(ps, 2, to);
            ps.setInt(3, scheduleId);
            return ps.executeUpdate() > 0;
        }
    }

    /** Cửa hàng trưởng từ chối đăng ký (REGISTERED -> CANCELLED). */
    public boolean reject(int scheduleId) throws SQLException {
        return cancelRegistered(scheduleId, null);
    }

    /** Nhân viên tự hủy đăng ký của mình khi còn chờ chốt; ca đã chốt phải gửi đơn nghỉ/đổi ca. */
    public boolean cancelRegistration(int scheduleId, String employeeId) throws SQLException {
        return cancelRegistered(scheduleId, employeeId);
    }

    private boolean cancelRegistered(int scheduleId, String employeeId) throws SQLException {
        String sql = "UPDATE WorkSchedule SET status = 'CANCELLED' WHERE schedule_id = ? AND status = 'REGISTERED'"
                   + (employeeId != null ? " AND employee_id = ?" : "");
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, scheduleId);
            if (employeeId != null) ps.setString(2, employeeId);
            return ps.executeUpdate() > 0;
        }
    }

    // ---------------- Map ----------------
    private List<WorkSchedule> query(String sql, Binder b) throws SQLException {
        List<WorkSchedule> list = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            b.bind(ps);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        }
        return list;
    }

    private WorkSchedule map(ResultSet rs) throws SQLException {
        WorkSchedule w = new WorkSchedule();
        w.setScheduleId(rs.getInt("schedule_id"));
        w.setEmployeeId(rs.getString("employee_id"));
        w.setFullName(rs.getString("full_name"));
        w.setSalaryType(SalaryType.valueOf(rs.getString("salary_type")));
        w.setWorkDate(DaoUtil.date(rs, "work_date"));
        w.setShiftName(rs.getString("shift_name"));
        w.setStartTime(DaoUtil.time(rs, "start_time"));
        w.setEndTime(DaoUtil.time(rs, "end_time"));
        w.setStatus(ScheduleStatus.valueOf(rs.getString("status")));
        return w;
    }
}