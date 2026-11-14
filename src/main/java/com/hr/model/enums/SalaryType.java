package com.hr.model.enums;

public enum SalaryType implements Displayable {
    FIXED("Cố định"),
    HOURLY("Theo giờ");

    private final String displayName;

    SalaryType(String displayName) {
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
