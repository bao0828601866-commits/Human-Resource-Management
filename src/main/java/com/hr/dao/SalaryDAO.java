package com.hr.dao;

import com.hr.model.SalaryRow;
import com.hr.model.enums.SalaryType;
import com.hr.utils.DatabaseConnection;

import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Lương. Hai nguồn, giống màn hình:
 *  - Đã chốt: bảng Payroll (lưu mức lương + phụ cấp + thưởng tại thời điểm chốt).
 *  - Chưa chốt (tạm tính): giờ lấy từ Attendance (nếu chưa có thì MonthlyHours), thưởng doanh thu = 0.
 * Phụ cấp theo chức vụ đặt ở ALLOWANCE_EXPR cho khớp database.sql (chưa có cột phụ cấp trong Position).
 */
public class SalaryDAO {

    private static final String ALLOWANCE_EXPR =
            "CAST(CASE p.position_name WHEN N'Quản lý' THEN 1000000 WHEN N'Master Barista Leader' THEN 500000 "
          + "WHEN N'Thu ngân' THEN 300000 WHEN N'Security' THEN 300000 ELSE 0 END AS DECIMAL(12,0))";
    private static final String BONUS_EXPR =
            "CAST(CASE WHEN p.salary_type = 'FIXED' THEN 200000 ELSE 100000 END AS DECIMAL(12,0))";
    private static final String BASE_EXPR =
            "CAST(CASE WHEN p.salary_type = 'FIXED' THEN p.salary_rate ELSE p.salary_rate * h.hours END AS DECIMAL(14,0))";

    /** Dòng nguồn chung cho cả tạm tính lẫn chốt: giờ = chấm công, không có thì MonthlyHours. */
    private static final String SOURCE =
            "FROM Employee e JOIN Position p ON p.position_id = e.position_id "
          + "LEFT JOIN MonthlyHours mh ON mh.employee_id = e.employee_id AND mh.month = ? AND mh.year = ? "
          + "OUTER APPLY (SELECT CAST(SUM(DATEDIFF(MINUTE, a.check_in_time, a.check_out_time)) / 60.0 AS DECIMAL(6,1)) AS hours "
          + "  FROM Attendance a WHERE a.employee_id = e.employee_id AND MONTH(a.work_date) = ? AND YEAR(a.work_date) = ? "
          + "  AND a.check_in_time IS NOT NULL AND a.check_out_time IS NOT NULL) att "
          + "CROSS APPLY (SELECT CAST(COALESCE(att.hours, mh.hours, 0) AS DECIMAL(6,1)) AS hours) h "
          + "WHERE (e.status = 'ACTIVE' OR h.hours > 0) AND e.hire_date <= ? ";

    /** Bảng lương cả cửa hàng một tháng: đã chốt thì đọc Payroll, chưa thì tạm tính. */
    public List<SalaryRow> findByMonth(int month, int year) throws SQLException {
        return isFinalized(month, year) ? finalized(month, year, null) : preview(month, year, null);
    }

    /** Lương của một nhân viên trong một tháng (nhân viên tự xem). Không có trả về null. */
    public SalaryRow findOne(String employeeId, int month, int year) throws SQLException {
        List<SalaryRow> l = isFinalized(month, year) ? finalized(month, year, employeeId) : preview(month, year, employeeId);
        return l.isEmpty() ? null : l.get(0);
    }

    /** `count` tháng gần nhất tính từ `from` (mới -> cũ) của một nhân viên, bỏ qua tháng không có dữ liệu. */
    public List<SalaryRow> findRecent(String employeeId, YearMonth from, int count) throws SQLException {
        return findRecent(employeeId, from, count, finalizedMonths(from.minusMonths(count - 1L), from));
    }

    /** Bản dùng lại tập tháng đã chốt (không phải truy vấn lại isFinalized cho từng tháng). */
    public List<SalaryRow> findRecent(String employeeId, YearMonth from, int count, Set<YearMonth> done)
            throws SQLException {
        List<SalaryRow> out = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            YearMonth ym = from.minusMonths(i);
            List<SalaryRow> l = done.contains(ym)
                    ? finalized(ym.getMonthValue(), ym.getYear(), employeeId)
                    : preview(ym.getMonthValue(), ym.getYear(), employeeId);
            if (!l.isEmpty()) out.add(l.get(0));
        }
        return out;
    }

    /** Những tháng đã chốt lương trong khoảng [from, to]: 1 truy vấn thay cho nhiều lần isFinalized. */
    public Set<YearMonth> finalizedMonths(YearMonth from, YearMonth to) throws SQLException {
        String sql = "SELECT DISTINCT month, year FROM Payroll WHERE year * 12 + month BETWEEN ? AND ?";
        Set<YearMonth> set = new HashSet<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, from.getYear() * 12 + from.getMonthValue());
            ps.setInt(2, to.getYear() * 12 + to.getMonthValue());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) set.add(YearMonth.of(rs.getInt("year"), rs.getInt("month")));
            }
        }
        return set;
    }

    /** Tháng này đã chốt lương chưa (có dòng trong Payroll). */
    public boolean isFinalized(int month, int year) throws SQLException {
        String sql = "SELECT TOP 1 1 FROM Payroll WHERE month = ? AND year = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, month);
            ps.setInt(2, year);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * Chốt lương: tổng hợp MonthlyHours (sp_RefreshMonthlyHours) rồi ghi Payroll, MỘT giao dịch.
     * Chốt lại cùng tháng sẽ ghi đè. Trả về số nhân viên đã chốt.
     */
    public int finalizePayroll(int month, int year) throws SQLException {
        YearMonth ym = YearMonth.of(year, month);
        String ins = "INSERT INTO Payroll (employee_id, month, year, period_start, period_end, salary_type, salary_rate, "
                   + "hours, base_salary, allowance, revenue_bonus, total_salary) "
                   + "SELECT x.employee_id, CAST(? AS TINYINT), CAST(? AS SMALLINT), ?, ?, x.salary_type, x.salary_rate, "
                   + "x.hours, x.base, x.allow, x.bonus, x.base + x.allow + x.bonus FROM ("
                   + "SELECT e.employee_id, p.salary_type, p.salary_rate, h.hours, "
                   + BASE_EXPR + " AS base, " + ALLOWANCE_EXPR + " AS allow, " + BONUS_EXPR + " AS bonus "
                   + SOURCE + ") x";

        try (Connection c = DatabaseConnection.getConnection()) {
            c.setAutoCommit(false);
            try (CallableStatement cs = c.prepareCall("{call sp_RefreshMonthlyHours(?, ?)}");
                 PreparedStatement del = c.prepareStatement("DELETE FROM Payroll WHERE month = ? AND year = ?");
                 PreparedStatement ps = c.prepareStatement(ins)) {
                cs.setInt(1, month);
                cs.setInt(2, year);
                cs.execute();

                del.setInt(1, month);
                del.setInt(2, year);
                del.executeUpdate();

                int i = 1;
                ps.setInt(i++, month);
                ps.setInt(i++, year);
                DaoUtil.setDate(ps, i++, ym.atDay(1));
                DaoUtil.setDate(ps, i++, ym.atEndOfMonth());
                ps.setInt(i++, month);          // SOURCE: MonthlyHours
                ps.setInt(i++, year);
                ps.setInt(i++, month);          // SOURCE: Attendance
                ps.setInt(i++, year);
                DaoUtil.setDate(ps, i, ym.atEndOfMonth());
                int rows = ps.executeUpdate();
                c.commit();
                return rows;
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        }
    }

    // ---------------------------------------------------------------------
    private List<SalaryRow> preview(int month, int year, String employeeId) throws SQLException {
        YearMonth ym = YearMonth.of(year, month);
        String sql = "SELECT e.employee_id, e.full_name, p.position_name, p.salary_type, p.salary_rate, h.hours, "
                   + BASE_EXPR + " AS base_salary, " + ALLOWANCE_EXPR + " AS allowance "
                   + SOURCE + (employeeId != null ? "AND e.employee_id = ? " : "") + "ORDER BY e.employee_id";
        List<SalaryRow> list = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            int i = 1;
            ps.setInt(i++, month);
            ps.setInt(i++, year);
            ps.setInt(i++, month);
            ps.setInt(i++, year);
            DaoUtil.setDate(ps, i++, ym.atEndOfMonth());
            if (employeeId != null) ps.setString(i, employeeId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    BigDecimal base = rs.getBigDecimal("base_salary");
                    BigDecimal allow = rs.getBigDecimal("allowance");
                    list.add(new SalaryRow(rs.getString("employee_id"), rs.getString("full_name"),
                            rs.getString("position_name"), month, year, ym.atDay(1), ym.atEndOfMonth(),
                            SalaryType.valueOf(rs.getString("salary_type")), rs.getBigDecimal("salary_rate"),
                            rs.getBigDecimal("hours"), base, allow, BigDecimal.ZERO, base.add(allow)));
                }
            }
        }
        return list;
    }

    private List<SalaryRow> finalized(int month, int year, String employeeId) throws SQLException {
        String sql = "SELECT pr.employee_id, e.full_name, p.position_name, pr.month, pr.year, pr.period_start, "
                   + "pr.period_end, pr.salary_type, pr.salary_rate, pr.hours, pr.base_salary, pr.allowance, "
                   + "pr.revenue_bonus, pr.total_salary "
                   + "FROM Payroll pr JOIN Employee e ON e.employee_id = pr.employee_id "
                   + "JOIN Position p ON p.position_id = e.position_id "
                   + "WHERE pr.month = ? AND pr.year = ? " + (employeeId != null ? "AND pr.employee_id = ? " : "")
                   + "ORDER BY pr.employee_id";
        List<SalaryRow> list = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, month);
            ps.setInt(2, year);
            if (employeeId != null) ps.setString(3, employeeId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    LocalDate s = DaoUtil.date(rs, "period_start"), e = DaoUtil.date(rs, "period_end");
                    list.add(new SalaryRow(rs.getString("employee_id"), rs.getString("full_name"),
                            rs.getString("position_name"), rs.getInt("month"), rs.getInt("year"), s, e,
                            SalaryType.valueOf(rs.getString("salary_type")), rs.getBigDecimal("salary_rate"),
                            rs.getBigDecimal("hours"), rs.getBigDecimal("base_salary"), rs.getBigDecimal("allowance"),
                            rs.getBigDecimal("revenue_bonus"), rs.getBigDecimal("total_salary")));
                }
            }
        }
        return list;
    }
}