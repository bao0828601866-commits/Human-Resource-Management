package com.hr.dao;

import com.hr.model.ShiftTime;
import com.hr.utils.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Khung giờ của ca (Ca sáng / Ca chiều). Thay cho DemoData.SHIFTS. */
public class ShiftTimeDAO {

    public List<ShiftTime> findAll() throws SQLException {
        String sql = "SELECT shift_name, start_time, end_time FROM ShiftTime ORDER BY start_time";
        List<ShiftTime> list = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new ShiftTime(rs.getString("shift_name"),
                        DaoUtil.time(rs, "start_time"), DaoUtil.time(rs, "end_time")));
            }
        }
        return list;
    }
}
