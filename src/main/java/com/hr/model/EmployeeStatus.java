package com.hr.model;

public enum EmployeeStatus {
    CON_LAM("Còn làm"), 
    DA_NGHI("Đã nghỉ"); 

    private final String displayName;

    EmployeeStatus(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
