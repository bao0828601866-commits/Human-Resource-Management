package com.hr;

import com.hr.utils.DatabaseConnection;

public class Main {
    public static void main(String[] args) {
        if (DatabaseConnection.isConnected()) {
            System.out.println("✅ Kết nối JDBC thành công!");
        } else {
            System.out.println("❌ Kết nối JDBC thất bại!");
        }
    } 
}