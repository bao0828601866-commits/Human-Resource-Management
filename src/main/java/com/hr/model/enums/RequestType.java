package com.hr.model.enums;

public enum RequestType implements Displayable {
    LEAVE("Nghỉ phép"),
    SHIFT_CHANGE("Đổi ca");

    private final String displayName;

    RequestType(String displayName) {
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
