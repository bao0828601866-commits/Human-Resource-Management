package com.hr.model.enums;

public enum RequestStatus implements Displayable {
    PENDING("Chờ duyệt"),
    APPROVED("Đã duyệt"),
    REJECTED("Từ chối");

    private final String displayName;

    RequestStatus(String displayName) {
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
