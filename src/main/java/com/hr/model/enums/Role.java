package com.hr.model.enums;

public enum Role implements Displayable {
    MANAGER("Quản lí"),
    EMPLOYEE("Nhân viên");

    private final String displayName;

    Role(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    /** Hiển thị tiếng Việt trên JComboBox/JTable. Giá trị lưu DB dùng name(). */
    @Override
    public String toString() {
        return displayName;
    }
}
