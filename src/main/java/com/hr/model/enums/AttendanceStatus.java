package com.hr.model.enums;

public enum AttendanceStatus implements Displayable {
    ON_TIME("Đúng giờ"),
    LATE("Đi trễ"),
    EARLY_LEAVE("Về sớm"),
    ABSENT("Vắng");

    private final String displayName;

    AttendanceStatus(String displayName) {
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
