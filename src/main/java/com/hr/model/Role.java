package com.hr.model;

public enum Role {
    MANAGER("Quản lí"),
    EMPLOYEE("Nhân viên");

    private final String displayName;

    Role(String displayName) {
        this.displayName = displayName;
    }

    @Override 
    public String toString() {
        return displayName;
    }
}
