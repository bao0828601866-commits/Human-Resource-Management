package com.hr.model;

import java.math.BigDecimal;

public class Employee {
    private String employeeId;
    private String fullName;
    private String position;
    private SalaryType salaryType;  // Loại lương part hay fulltime
    private BigDecimal salaryRate;  // Mức lương
    private String phoneNumber;
    private EmployeeStatus status;  // Còn làm hay Đã nghỉ

    public Employee() {}

    public Employee(String employeeId, String fullName, String position, SalaryType salaryType, BigDecimal salaryRate,
            String phoneNumber, EmployeeStatus status) {
        this.employeeId = employeeId;
        this.fullName = fullName;
        this.position = position;
        this.salaryType = salaryType;
        this.salaryRate = salaryRate;
        this.phoneNumber = phoneNumber;
        this.status = status;
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public SalaryType getSalaryType() {
        return salaryType;
    }

    public void setSalaryType(SalaryType salaryType) {
        this.salaryType = salaryType;
    }

    public BigDecimal getSalaryRate() {
        return salaryRate;
    }

    public void setSalaryRate(BigDecimal salaryRate) {
        this.salaryRate = salaryRate;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public EmployeeStatus getStatus() {
        return status;
    }

    public void setStatus(EmployeeStatus status) {
        this.status = status;
    }
}
