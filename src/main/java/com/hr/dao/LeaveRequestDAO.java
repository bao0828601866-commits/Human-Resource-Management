package com.hr.dao;

import com.hr.model.LeaveRequest;
import com.hr.model.enums.RequestStatus;
import com.hr.model.enums.RequestType;
import com.hr.utils.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/** Đơn xin nghỉ / đổi ca. */
public class LeaveRequestDAO {

    private static final String SELECT =
            "SELECT r.request_id, r.employee_id, e.full_name, r.request_type, r.target_date, r.shift_name, "
          + "r.new_shift_name, r.new_start_time, r.new_end_time, r.reason, r.status, r.created_at, "
          + "r.reviewed_by, r.reviewed_at FROM LeaveRequest r JOIN Employee e ON e.employee_id = r.employee_id ";

    /** Mọi đơn, mới nhất trước (bảng đơn của cửa hàng trưởng). */
    public List<LeaveRequest> findAll() throws SQLException {
        return query(SELECT + "ORDER BY r.created_at DESC, r.request_id DESC", null);
    }

    public List<LeaveRequest> findPending() throws SQLException {
        return query(SELECT + "WHERE r.status = 'PENDING' ORDER BY r.created_at DESC, r.request_id DESC", null);
    }

    /** Đơn của một nhân viên ("Đơn xin nghỉ / đổi ca của tôi"). */
    public List<LeaveRequest> findByEmployee(String employeeId) throws SQLException {
        return query(SELECT + "WHERE r.employee_id = ? ORDER BY r.created_at DESC, r.request_id DESC", employeeId);
    }

    public int countPending() throws SQLException {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM LeaveRequest WHERE status = 'PENDING'");
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        }
    }

    /**
     * Gửi đơn. LEAVE: bỏ qua newShiftName/newStart/newEnd. shiftName null = nghỉ cả ngày.
     * SHIFT_CHANGE bắt buộc có ca + giờ mới (CHECK ở DB). Trả về request_id.
     */
    public int insert(LeaveRequest r) throws SQLException {
        String sql = "INSERT INTO LeaveRequest (employee_id, request_type, target_date, shift_name, "
                   + "new_shift_name, new_start_time, new_end_time, reason) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        boolean change = r.getRequestType() == RequestType.SHIFT_CHANGE;
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, r.getEmployeeId());
            ps.setString(2, r.getRequestType().name());
            DaoUtil.setDate(ps, 3, r.getTargetDate());
            if (r.getShiftName() == null) ps.setNull(4, Types.NVARCHAR); else ps.setString(4, r.getShiftName());
            if (change) ps.setString(5, r.getNewShiftName()); else ps.setNull(5, Types.NVARCHAR);
            DaoUtil.setTime(ps, 6, change ? r.getNewStartTime() : null);
            DaoUtil.setTime(ps, 7, change ? r.getNewEndTime() : null);
            ps.setString(8, r.getReason());
            ps.executeUpdate();
            try (ResultSet k = ps.getGeneratedKeys()) {
                return k.next() ? k.getInt(1) : 0;
            }
        }
    }

    /**
     * Duyệt / từ chối đơn đang PENDING, MỘT giao dịch.
     * Duyệt nghỉ -> hủy ca tương ứng; duyệt đổi ca -> chuyển sang ca/giờ mới.
     * Trả về false nếu đơn đã được xử lý trước đó. Không còn ca tương ứng hoặc trigger từ chối -> rollback + ném lỗi.
     */
    public boolean review(int requestId, boolean approve, String reviewerId) throws SQLException {
        try (Connection c = DatabaseConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                LeaveRequest r = lockPending(c, requestId);
                if (r == null) { c.rollback(); return false; }

                try (PreparedStatement ps = c.prepareStatement(
                        "UPDATE LeaveRequest SET status = ?, reviewed_by = ?, reviewed_at = GETDATE() WHERE request_id = ?")) {
                    ps.setString(1, approve ? "APPROVED" : "REJECTED");
                    ps.setString(2, reviewerId);
                    ps.setInt(3, requestId);
                    ps.executeUpdate();
                }
                if (approve) applyToSchedule(c, r);
                c.commit();
                return true;
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            }
        }
    }

    private LeaveRequest lockPending(Connection c, int id) throws SQLException {
        String sql = SELECT.replace("FROM LeaveRequest r", "FROM LeaveRequest r WITH (UPDLOCK)")
                   + "WHERE r.request_id = ? AND r.status = 'PENDING'";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    private void applyToSchedule(Connection c, LeaveRequest r) throws SQLException {
        boolean leave = r.getRequestType() == RequestType.LEAVE;
        String where = " WHERE employee_id = ? AND work_date = ? AND status <> 'CANCELLED'"
                     + (r.getShiftName() != null ? " AND shift_name = ?" : "");
        String sql = leave
                ? "UPDATE WorkSchedule SET status = 'CANCELLED'" + where
                : "UPDATE WorkSchedule SET shift_name = ?, start_time = ?, end_time = ?" + where;

        try (PreparedStatement ps = c.prepareStatement(sql)) {
            int i = 1;
            if (!leave) {
                ps.setString(i++, r.getNewShiftName());
                DaoUtil.setTime(ps, i++, r.getNewStartTime());
                DaoUtil.setTime(ps, i++, r.getNewEndTime());
            }
            ps.setString(i++, r.getEmployeeId());
            DaoUtil.setDate(ps, i++, r.getTargetDate());
            if (r.getShiftName() != null) ps.setString(i, r.getShiftName());
            if (ps.executeUpdate() == 0) throw new IllegalStateException("Không còn ca làm tương ứng để áp dụng đơn này.");
        }
    }

    private List<LeaveRequest> query(String sql, String arg) throws SQLException {
        List<LeaveRequest> list = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            if (arg != null) ps.setString(1, arg);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        }
        return list;
    }

    private LeaveRequest map(ResultSet rs) throws SQLException {
        LeaveRequest r = new LeaveRequest();
        r.setRequestId(rs.getInt("request_id"));
        r.setEmployeeId(rs.getString("employee_id"));
        r.setFullName(rs.getString("full_name"));
        r.setRequestType(RequestType.valueOf(rs.getString("request_type")));
        r.setTargetDate(DaoUtil.date(rs, "target_date"));
        r.setShiftName(rs.getString("shift_name"));
        r.setNewShiftName(rs.getString("new_shift_name"));
        r.setNewStartTime(DaoUtil.time(rs, "new_start_time"));
        r.setNewEndTime(DaoUtil.time(rs, "new_end_time"));
        r.setReason(rs.getString("reason"));
        r.setStatus(RequestStatus.valueOf(rs.getString("status")));
        r.setCreatedAt(DaoUtil.ts(rs, "created_at"));
        r.setReviewedBy(rs.getString("reviewed_by"));
        r.setReviewedAt(DaoUtil.ts(rs, "reviewed_at"));
        return r;
    }
}
