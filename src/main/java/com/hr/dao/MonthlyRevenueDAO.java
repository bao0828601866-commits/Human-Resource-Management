package com.hr.dao;

import com.hr.model.MonthlyRevenue;
import com.hr.utils.DatabaseConnection;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Doanh thu tháng (biểu đồ cột ở Tổng quan). */
public class MonthlyRevenueDAO {

    public List<MonthlyRevenue> findByYear(int year) throws SQLException {
        String sql = "SELECT month, year, total_revenue FROM MonthlyRevenue WHERE year = ? ORDER BY month";
        List<MonthlyRevenue> list = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, year);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new MonthlyRevenue(rs.getInt("month"), rs.getInt("year"), rs.getBigDecimal("total_revenue")));
                }
            }
        }
        return list;
    }

    /** Mảng 12 phần tử (T1..T12), đơn vị TRIỆU đồng, tháng chưa có = 0: đưa thẳng vào BarChartPanel. */
    public double[] millionsByMonth(int year) throws SQLException {
        double[] r = new double[12];
        for (MonthlyRevenue m : findByYear(year)) {
            r[m.getMonth() - 1] = m.getTotalRevenue().divide(BigDecimal.valueOf(1_000_000)).doubleValue();
        }
        return r;
    }

    /** Thêm hoặc cập nhật doanh thu của một tháng. */
    public void upsert(int month, int year, BigDecimal revenue) throws SQLException {
        try (Connection c = DatabaseConnection.getConnection()) {
            c.setAutoCommit(false);
            try (PreparedStatement up = c.prepareStatement(
                         "UPDATE MonthlyRevenue SET total_revenue = ? WHERE month = ? AND year = ?");
                 PreparedStatement in = c.prepareStatement(
                         "INSERT INTO MonthlyRevenue (month, year, total_revenue) VALUES (?, ?, ?)")) {
                up.setBigDecimal(1, revenue);
                up.setInt(2, month);
                up.setInt(3, year);
                if (up.executeUpdate() == 0) {
                    in.setInt(1, month);
                    in.setInt(2, year);
                    in.setBigDecimal(3, revenue);
                    in.executeUpdate();
                }
                c.commit();
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        }
    }
}
