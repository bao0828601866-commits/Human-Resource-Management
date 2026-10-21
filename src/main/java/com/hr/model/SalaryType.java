package com.hr.model;

public enum SalaryType {
    FULL_TIME("Cố định"), 
    PART_TIME("Theo giờ");
    
    private final String displayType;

    SalaryType(String displayType) {
        this.displayType = displayType;
    }

    @Override 
    public String toString() {
        return displayType;
    }
}
