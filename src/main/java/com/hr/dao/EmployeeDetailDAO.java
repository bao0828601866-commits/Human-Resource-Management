package com.hr.dao;

import com.hr.model.Employee;
import com.hr.utils.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class EmployeeDetailDAO {

    private static final String SELECT =
            "SELECT pr.hometown, pr.ethnicity, pr.education_level, pr.school, pr.permanent_address, "
          + "       b.bank_name, b.bank_account "
          + "FROM Employee e "
          + "LEFT JOIN PersonalInfo pr ON pr.employee_id = e.employee_id "
          + "LEFT JOIN BankAccount b     ON b.employee_id  = e.employee_id "
          + "WHERE e.employee_id = ?";

    private static final String MERGE_PROFILE =
            "MERGE PersonalInfo AS t "
          + "USING (SELECT ? AS employee_id, ? AS hometown, ? AS ethnicity, ? AS education_level, "
          + "              ? AS school, ? AS permanent_address) AS s "
          + "ON t.employee_id = s.employee_id "
          + "WHEN MATCHED THEN UPDATE SET hometown = s.hometown, ethnicity = s.ethnicity, "
          + "     education_level = s.education_level, school = s.school, permanent_address = s.permanent_address "
          + "WHEN NOT MATCHED THEN INSERT (employee_id, hometown, ethnicity, education_level, school, permanent_address) "
          + "     VALUES (s.employee_id, s.hometown, s.ethnicity, s.education_level, s.school, s.permanent_address);";

    private static final String MERGE_BANK =
            "MERGE BankAccount AS t "
          + "USING (SELECT ? AS employee_id, ? AS bank_name, ? AS bank_account) AS s "
          + "ON t.employee_id = s.employee_id "
          + "WHEN MATCHED THEN UPDATE SET bank_name = s.bank_name, bank_account = s.bank_account "
          + "WHEN NOT MATCHED THEN INSERT (employee_id, bank_name, bank_account) "
          + "     VALUES (s.employee_id, s.bank_name, s.bank_account);";

    private static final String DELETE_BANK = "DELETE FROM BankAccount WHERE employee_id = ?";

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
                    e.setPermanentAddress(rs.getString("permanent_address"));
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
            ps.setString(6, nul(e.getPermanentAddress()));
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