package com.hr.model;

public enum SalaryType {
    FIXED("Cố định"), 
    HOURLY("Theo giờ");
    
    private final String displayName;

    SalaryType(String displayType) {
        this.displayName = displayType;
    }

    @Override 
    public String toString() {
        return displayName;
    }
}
