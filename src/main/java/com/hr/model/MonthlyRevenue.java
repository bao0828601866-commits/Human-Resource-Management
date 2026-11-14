package com.hr.model;

import java.math.BigDecimal;

public class MonthlyRevenue {
    private final int month;
    private final int year;
    private final BigDecimal totalRevenue;

    public MonthlyRevenue(int month, int year, BigDecimal totalRevenue) {
        this.month = month;
        this.year = year;
        this.totalRevenue = totalRevenue;
    }

    public int getMonth()                { return month; }
    public int getYear()                 { return year; }
    public BigDecimal getTotalRevenue()  { return totalRevenue; }
}
