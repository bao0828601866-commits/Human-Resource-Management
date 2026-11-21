package com.hr.utils;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;

public final class DatabaseConnection {
    // 127.0.0.1 thay cho localhost: tránh bị phân giải sang IPv6 (::1) rồi mới quay lại IPv4.
    private static final String URL = "jdbc:sqlserver://127.0.0.1:1433;databaseName=QuanLyNhanSu;" 
                                        + "encrypt=true;trustServerCertificate=true;";
    private static final String USER = "sa";
    private static final String PASSWORD = "123456789";

    private static final HikariDataSource DS;

    static {
        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl(URL);
        cfg.setUsername(USER);
        cfg.setPassword(PASSWORD);
        cfg.setPoolName("hrm-pool");

        cfg.setMaximumPoolSize(4);         // app desktop 1 người dùng: 4 là dư
        cfg.setMinimumIdle(1);             // luôn giữ sẵn ít nhất 1 kết nối
        cfg.setConnectionTimeout(8_000);   // chờ lấy kết nối tối đa 8 giây rồi báo lỗi
        cfg.setIdleTimeout(120_000);       // kết nối rảnh quá 2 phút thì đóng bớt
        cfg.setMaxLifetime(1_500_000);     // làm mới kết nối sau 25 phút

        
        cfg.setInitializationFailTimeout(-1);

        DS = new HikariDataSource(cfg);
        Runtime.getRuntime().addShutdownHook(new Thread(DS::close));
    }

    private DatabaseConnection() {}

    // close() trên Connection chỉ TRẢ kết nối về pool, nên các DAO giữ nguyên try-with-resources
    public static Connection getConnection() throws SQLException {
        return DS.getConnection();
    }

    // Gọi ở luồng nền ngay khi mở màn hình đăng nhập để pool mở sẵn kết nối
    public static void warmUp() {
        try (Connection c = DS.getConnection()) {
            // chỉ để khởi tạo pool
        } catch (Exception ignored) {
        }
    }
}