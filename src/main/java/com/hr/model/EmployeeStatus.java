package com.hr.model;

public enum EmployeeStatus {
    ACTIVE("Còn làm"), 
    RESIGNED("Đã nghỉ"); 

    private final String displayName;

    EmployeeStatus(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
