package com.hr.model;

import java.math.BigDecimal;

/** work_days là cột tính sẵn của DB (hours / 9): chỉ đọc, KHÔNG INSERT/UPDATE. */
public class MonthlyHours {
    private int recordId;
    private String employeeId;
    private int month;
    private int year;
    private BigDecimal hours;
    private BigDecimal workDays;   // chỉ đọc

    public MonthlyHours() { }

    public int getRecordId()               { return recordId; }
    public void setRecordId(int v)         { this.recordId = v; }

    public String getEmployeeId()          { return employeeId; }
    public void setEmployeeId(String v)    { this.employeeId = v; }

    public int getMonth()                  { return month; }
    public void setMonth(int v)            { this.month = v; }

    public int getYear()                   { return year; }
    public void setYear(int v)             { this.year = v; }

    public BigDecimal getHours()           { return hours; }
    public void setHours(BigDecimal v)     { this.hours = v; }

    public BigDecimal getWorkDays()        { return workDays; }
    public void setWorkDays(BigDecimal v)  { this.workDays = v; }
}
