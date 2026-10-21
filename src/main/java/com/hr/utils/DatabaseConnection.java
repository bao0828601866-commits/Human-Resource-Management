package com.hr.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {
    private static final String URL = "jdbc:sqlserver://localhost:1433;databaseName=QuanLyNhanSu;encrypt=true;trustServerCertificate=true;";
    private static final String USER = "sa";
    private static final String PASSWORD = "123456789";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    public static boolean isConnected() {
        try (Connection conn = getConnection()) {
            // isValid(timeout giây) gửi một lệnh kiểm tra tới database
            return conn != null && conn.isValid(3);
        } catch (SQLException e) {
            System.out.println("Lỗi kết nối: " + e.getMessage());
            return false;
        }
    }   
}