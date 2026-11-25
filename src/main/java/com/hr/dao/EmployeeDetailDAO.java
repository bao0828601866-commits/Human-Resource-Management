package com.hr.dao;

import com.hr.model.Employee;
import com.hr.utils.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Đọc / ghi 2 bảng phụ của nhân viên: EmployeeProfile (hồ sơ) và EmployeeBank (ngân hàng).
 * Cả hai là quan hệ 1-1 với Employee và có thể chưa có dòng -> đọc bằng LEFT JOIN, ghi bằng MERGE.
 * Kết nối lấy từ pool HikariCP (DatabaseConnection): close() chỉ trả kết nối về pool.
 */
public class EmployeeDetailDAO {

    private static final String SELECT =
            "SELECT pr.hometown, pr.ethnicity, pr.education_level, pr.school, pr.temp_address, "
          + "       b.bank_name, b.bank_account "
          + "FROM Employee e "
          + "LEFT JOIN EmployeeProfile pr ON pr.employee_id = e.employee_id "
          + "LEFT JOIN EmployeeBank b     ON b.employee_id  = e.employee_id "
          + "WHERE e.employee_id = ?";

    private static final String MERGE_PROFILE =
            "MERGE EmployeeProfile AS t "
          + "USING (SELECT ? AS employee_id, ? AS hometown, ? AS ethnicity, ? AS education_level, "
          + "              ? AS school, ? AS temp_address) AS s "
          + "ON t.employee_id = s.employee_id "
          + "WHEN MATCHED THEN UPDATE SET hometown = s.hometown, ethnicity = s.ethnicity, "
          + "     education_level = s.education_level, school = s.school, temp_address = s.temp_address "
          + "WHEN NOT MATCHED THEN INSERT (employee_id, hometown, ethnicity, education_level, school, temp_address) "
          + "     VALUES (s.employee_id, s.hometown, s.ethnicity, s.education_level, s.school, s.temp_address);";

    private static final String MERGE_BANK =
            "MERGE EmployeeBank AS t "
          + "USING (SELECT ? AS employee_id, ? AS bank_name, ? AS bank_account) AS s "
          + "ON t.employee_id = s.employee_id "
          + "WHEN MATCHED THEN UPDATE SET bank_name = s.bank_name, bank_account = s.bank_account "
          + "WHEN NOT MATCHED THEN INSERT (employee_id, bank_name, bank_account) "
          + "     VALUES (s.employee_id, s.bank_name, s.bank_account);";

    private static final String DELETE_BANK = "DELETE FROM EmployeeBank WHERE employee_id = ?";

    /** Điền 7 trường chi tiết vào Employee (dòng chưa tồn tại -> các trường để null). */
    public void loadInto(Employee e) throws SQLException {
        try (Connection c = DatabaseConnection.getConnection(); PreparedStatement ps = c.prepareStatement(SELECT)) {
            ps.setString(1, e.getEmployeeId());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    e.setHometown(rs.getString("hometown"));
                    e.setEthnicity(rs.getString("ethnicity"));
                    e.setEducationLevel(rs.getString("education_level"));
                    e.setSchool(rs.getString("school"));
                    e.setTempAddress(rs.getString("temp_address"));
                    e.setBankName(rs.getString("bank_name"));
                    e.setBankAccount(rs.getString("bank_account"));
                }
            }
        }
    }

    /** Ghi hồ sơ + ngân hàng trong một transaction (có thì sửa, chưa có thì thêm). */
    public void save(Employee e) throws SQLException {
        try (Connection c = DatabaseConnection.getConnection()) {
            boolean oldAuto = c.getAutoCommit();
            c.setAutoCommit(false);
            try {
                saveProfile(c, e);
                saveBank(c, e);
                c.commit();
            } catch (SQLException ex) {
                c.rollback();
                throw ex;
            } finally {
                c.setAutoCommit(oldAuto);
            }
        }
    }

    private void saveProfile(Connection c, Employee e) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(MERGE_PROFILE)) {
            ps.setString(1, e.getEmployeeId());
            ps.setString(2, nul(e.getHometown()));
            ps.setString(3, nul(e.getEthnicity()));
            ps.setString(4, nul(e.getEducationLevel()));
            ps.setString(5, nul(e.getSchool()));
            ps.setString(6, nul(e.getTempAddress()));
            ps.executeUpdate();
        }
    }

    /** Không nhập ngân hàng + số tài khoản -> xóa dòng (cột bank_name, bank_account là NOT NULL). */
    private void saveBank(Connection c, Employee e) throws SQLException {
        String bank = nul(e.getBankName());
        String acc = nul(e.getBankAccount());
        if (bank == null && acc == null) {
            try (PreparedStatement ps = c.prepareStatement(DELETE_BANK)) {
                ps.setString(1, e.getEmployeeId());
                ps.executeUpdate();
            }
            return;
        }
        try (PreparedStatement ps = c.prepareStatement(MERGE_BANK)) {
            ps.setString(1, e.getEmployeeId());
            ps.setString(2, bank);
            ps.setString(3, acc);
            ps.executeUpdate();
        }
    }

    /** Chuỗi rỗng / toàn khoảng trắng -> null để lưu NULL vào DB. */
    private static String nul(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}