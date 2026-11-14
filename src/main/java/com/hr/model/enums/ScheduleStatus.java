package com.hr.model.enums;

public enum ScheduleStatus implements Displayable {
    REGISTERED("Đã đăng ký"),
    CONFIRMED("Đã chốt"),
    CANCELLED("Đã hủy");

    private final String displayName;

    ScheduleStatus(String displayName) {
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
