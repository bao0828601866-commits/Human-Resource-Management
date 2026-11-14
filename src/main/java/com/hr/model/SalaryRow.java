package com.hr.model;

import com.hr.model.enums.SalaryType;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/**
 * Một dòng bảng lương. Nguồn: Payroll (đã chốt) JOIN Employee + Position,
 * hoặc view vw_MonthlySalary (chưa chốt: allowance, revenueBonus = 0).
 * Không ứng với bảng nào.
 */
public class SalaryRow {
    private final String employeeId;
    private final String fullName;
    private final String positionName;
    private final int month;
    private final int year;
    private final LocalDate periodStart;
    private final LocalDate periodEnd;
    private final SalaryType salaryType;
    private final BigDecimal salaryRate;
    private final BigDecimal hours;
    private final BigDecimal baseSalary;
    private final BigDecimal allowance;
    private final BigDecimal revenueBonus;
    private final BigDecimal totalSalary;

    public SalaryRow(String employeeId, String fullName, String positionName,
                     int month, int year, LocalDate periodStart, LocalDate periodEnd,
                     SalaryType salaryType, BigDecimal salaryRate, BigDecimal hours,
                     BigDecimal baseSalary, BigDecimal allowance, BigDecimal revenueBonus,
                     BigDecimal totalSalary) {
        this.employeeId = employeeId;
        this.fullName = fullName;
        this.positionName = positionName;
        this.month = month;
        this.year = year;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.salaryType = salaryType;
        this.salaryRate = salaryRate;
        this.hours = hours;
        this.baseSalary = baseSalary;
        this.allowance = allowance;
        this.revenueBonus = revenueBonus;
        this.totalSalary = totalSalary;
    }

    /** Ngày công: 1 công = 9 giờ (khớp cột work_days của MonthlyHours). */
    public BigDecimal getWorkDays() {
        return hours.divide(BigDecimal.valueOf(9), 1, RoundingMode.HALF_UP);
    }

    public String getEmployeeId()        { return employeeId; }
    public String getFullName()          { return fullName; }
    public String getPositionName()      { return positionName; }
    public int getMonth()                { return month; }
    public int getYear()                 { return year; }
    public LocalDate getPeriodStart()    { return periodStart; }
    public LocalDate getPeriodEnd()      { return periodEnd; }
    public SalaryType getSalaryType()    { return salaryType; }
    public BigDecimal getSalaryRate()    { return salaryRate; }
    public BigDecimal getHours()         { return hours; }
    public BigDecimal getBaseSalary()    { return baseSalary; }
    public BigDecimal getAllowance()     { return allowance; }
    public BigDecimal getRevenueBonus()  { return revenueBonus; }
    public BigDecimal getTotalSalary()   { return totalSalary; }
}
