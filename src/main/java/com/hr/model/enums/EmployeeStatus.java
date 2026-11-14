package com.hr.model.enums;

public enum EmployeeStatus implements Displayable {
    ACTIVE("Còn làm"),
    RESIGNED("Đã nghỉ");

    private final String displayName;

    EmployeeStatus(String displayName) {
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
