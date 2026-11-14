package com.hr.dao;

import com.hr.model.Employee;
import com.hr.model.enums.EmployeeStatus;
import com.hr.model.enums.Gender;
import com.hr.model.enums.Role;
import com.hr.model.enums.SalaryType;
import com.hr.utils.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class EmployeeDAO {

    private static final String BASE_SELECT =
            "SELECT e.employee_id, e.full_name, e.gender, e.birth_day, e.phone_number, e.email, "
          + "e.citizen_id, e.address, e.position_id, e.salary_type, e.salary_rate, e.hire_date, e.status, "
          + "p.position_name "
          + "FROM Employee e JOIN Position p ON e.position_id = p.position_id ";

    /** Tất cả nhân viên (kể cả đã nghỉ), theo mã. */
    public List<Employee> findAll() throws SQLException {
        return search(null, null, null, null);
    }

    /**
     * Tìm / lọc phía DB. Tham số null = không lọc.
     * keyword: tìm theo tên; positionId; gender; status (ACTIVE/RESIGNED). Sắp xếp theo mã.
     */
    public List<Employee> search(String keyword, Integer positionId, Gender gender, EmployeeStatus status)
            throws SQLException {
        StringBuilder sql = new StringBuilder(BASE_SELECT).append("WHERE 1 = 1 ");
        List<Object> args = new ArrayList<>();
        if (keyword != null && !keyword.isBlank()) { sql.append("AND e.full_name LIKE ? "); args.add("%" + keyword.trim() + "%"); }
        if (positionId != null) { sql.append("AND e.position_id = ? "); args.add(positionId); }
        if (gender != null)     { sql.append("AND e.gender = ? ");      args.add(gender.name()); }
        if (status != null)     { sql.append("AND e.status = ? ");      args.add(status.name()); }
        sql.append("ORDER BY e.employee_id");

        List<Employee> list = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql.toString())) {
            for (int i = 0; i < args.size(); i++) ps.setObject(i + 1, args.get(i));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        }
        return list;
    }

    /** Tìm theo mã; không thấy trả về null. */
    public Employee findById(String employeeId) throws SQLException {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(BASE_SELECT + "WHERE e.employee_id = ?")) {
            ps.setString(1, employeeId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    /** Sinh mã tiếp theo dạng E26_018 dựa trên mã lớn nhất hiện có. */
    public String nextEmployeeId() throws SQLException {
        String sql = "SELECT TOP 1 employee_id FROM Employee WHERE employee_id LIKE 'E26[_]%' ORDER BY employee_id DESC";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            int next = 1;
            if (rs.next()) next = Integer.parseInt(rs.getString(1).substring(4)) + 1;
            return String.format("E26_%03d", next);
        }
    }

    /**
     * Thêm nhân viên + tài khoản (mật khẩu mặc định, must_change_password = 1) trong MỘT giao dịch.
     * salary_type / salary_rate được sao từ Position tại thời điểm thêm.
     */
    public void insert(Employee emp, Role role) throws SQLException {
        String sqlEmp = "INSERT INTO Employee (employee_id, full_name, gender, birth_day, phone_number, email, "
                      + "citizen_id, address, position_id, salary_type, salary_rate, hire_date, status) "
                      + "SELECT ?, ?, ?, ?, ?, ?, ?, ?, p.position_id, p.salary_type, p.salary_rate, ?, ? "
                      + "FROM Position p WHERE p.position_id = ?";
        String sqlAcc = "INSERT INTO Account (employee_id, role) VALUES (?, ?)";

        try (Connection c = DatabaseConnection.getConnection()) {
            c.setAutoCommit(false);
            try (PreparedStatement psEmp = c.prepareStatement(sqlEmp);
                 PreparedStatement psAcc = c.prepareStatement(sqlAcc)) {

                Gender g = emp.getGender() != null ? emp.getGender() : Gender.OTHER;
                LocalDate hire = emp.getHireDate() != null ? emp.getHireDate() : LocalDate.now();
                EmployeeStatus st = emp.getStatus() != null ? emp.getStatus() : EmployeeStatus.ACTIVE;

                psEmp.setString(1, emp.getEmployeeId());
                psEmp.setString(2, emp.getFullName());
                psEmp.setString(3, g.name());
                DaoUtil.setDate(psEmp, 4, emp.getBirthDay());
                DaoUtil.setStr(psEmp, 5, emp.getPhoneNumber());
                DaoUtil.setStr(psEmp, 6, emp.getEmail());
                DaoUtil.setStr(psEmp, 7, emp.getCitizenId());
                DaoUtil.setStr(psEmp, 8, emp.getAddress());
                DaoUtil.setDate(psEmp, 9, hire);
                psEmp.setString(10, st.name());
                psEmp.setInt(11, emp.getPositionId());
                if (psEmp.executeUpdate() == 0) throw new SQLException("Chức vụ không tồn tại: " + emp.getPositionId());

                psAcc.setString(1, emp.getEmployeeId());
                psAcc.setString(2, role.name());
                psAcc.executeUpdate();
                c.commit();
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        }
    }

    /** Quản lý sửa hồ sơ. Đổi chức vụ thì salary_type, salary_rate đổi theo chức vụ mới. */
    public boolean update(Employee emp) throws SQLException {
        String sql = "UPDATE e SET full_name = ?, gender = ?, birth_day = ?, phone_number = ?, email = ?, "
                   + "citizen_id = ?, address = ?, hire_date = ?, "
                   + "salary_type = CASE WHEN e.position_id = p.position_id THEN e.salary_type ELSE p.salary_type END, "
                   + "salary_rate = CASE WHEN e.position_id = p.position_id THEN e.salary_rate ELSE p.salary_rate END, "
                   + "position_id = p.position_id "
                   + "FROM Employee e CROSS JOIN Position p WHERE e.employee_id = ? AND p.position_id = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, emp.getFullName());
            ps.setString(2, (emp.getGender() != null ? emp.getGender() : Gender.OTHER).name());
            DaoUtil.setDate(ps, 3, emp.getBirthDay());
            DaoUtil.setStr(ps, 4, emp.getPhoneNumber());
            DaoUtil.setStr(ps, 5, emp.getEmail());
            DaoUtil.setStr(ps, 6, emp.getCitizenId());
            DaoUtil.setStr(ps, 7, emp.getAddress());
            DaoUtil.setDate(ps, 8, emp.getHireDate());
            ps.setString(9, emp.getEmployeeId());
            ps.setInt(10, emp.getPositionId());
            return ps.executeUpdate() > 0;
        }
    }

    /** Nhân viên tự cập nhật thông tin cá nhân (SĐT, địa chỉ, giới tính) - hộp thoại "Thông tin cá nhân". */
    public boolean updateProfile(String employeeId, String phone, String address, Gender gender) throws SQLException {
        String sql = "UPDATE Employee SET phone_number = ?, address = ?, gender = ? WHERE employee_id = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            DaoUtil.setStr(ps, 1, phone);
            DaoUtil.setStr(ps, 2, address);
            ps.setString(3, (gender != null ? gender : Gender.OTHER).name());
            ps.setString(4, employeeId);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Cho nghỉ việc (giữ dòng để còn lịch sử lương). Trigger tự khóa tài khoản;
     * các ca từ ngày mai trở đi của người này được hủy luôn, trong cùng giao dịch.
     */
    public boolean resign(String employeeId) throws SQLException {
        try (Connection c = DatabaseConnection.getConnection()) {
            c.setAutoCommit(false);
            try (PreparedStatement p1 = c.prepareStatement("UPDATE Employee SET status = 'RESIGNED' WHERE employee_id = ?");
                 PreparedStatement p2 = c.prepareStatement(
                         "UPDATE WorkSchedule SET status = 'CANCELLED' WHERE employee_id = ? "
                       + "AND work_date > CAST(GETDATE() AS DATE) AND status <> 'CANCELLED'")) {
                p1.setString(1, employeeId);
                boolean ok = p1.executeUpdate() > 0;
                p2.setString(1, employeeId);
                p2.executeUpdate();
                c.commit();
                return ok;
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        }
    }

    /** Cho đi làm lại (trigger tự mở tài khoản). */
    public boolean reactivate(String employeeId) throws SQLException {
        String sql = "UPDATE Employee SET status = 'ACTIVE' WHERE employee_id = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, employeeId);
            return ps.executeUpdate() > 0;
        }
    }

    /** Số nhân viên đang làm theo chức vụ (cho biểu đồ tròn ở Tổng quan). Giữ thứ tự position_id. */
    public Map<String, Integer> countActiveByPosition() throws SQLException {
        String sql = "SELECT p.position_name, COUNT(e.employee_id) AS n FROM Position p "
                   + "LEFT JOIN Employee e ON e.position_id = p.position_id AND e.status = 'ACTIVE' "
                   + "GROUP BY p.position_id, p.position_name ORDER BY p.position_id";
        Map<String, Integer> m = new LinkedHashMap<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) m.put(rs.getString(1), rs.getInt(2));
        }
        return m;
    }

    private Employee map(ResultSet rs) throws SQLException {
        Employee e = new Employee();
        e.setEmployeeId(rs.getString("employee_id"));
        e.setFullName(rs.getString("full_name"));
        e.setGender(Gender.valueOf(rs.getString("gender")));
        e.setBirthDay(DaoUtil.date(rs, "birth_day"));
        e.setPhoneNumber(rs.getString("phone_number"));
        e.setEmail(rs.getString("email"));
        e.setCitizenId(rs.getString("citizen_id"));
        e.setAddress(rs.getString("address"));
        e.setPositionId(rs.getInt("position_id"));
        e.setSalaryType(SalaryType.valueOf(rs.getString("salary_type")));
        e.setSalaryRate(rs.getBigDecimal("salary_rate"));
        e.setHireDate(DaoUtil.date(rs, "hire_date"));
        e.setStatus(EmployeeStatus.valueOf(rs.getString("status")));
        e.setPositionName(rs.getString("position_name"));
        return e;
    }
}
