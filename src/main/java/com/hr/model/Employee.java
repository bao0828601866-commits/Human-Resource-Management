package com.hr.model;

import com.hr.exception.InvalidEmployeeDataException;
import java.time.LocalDate;
import java.time.Period;

public abstract class Employee {
    private String id;
    private String fullName;
    private String department;
    private double baseSalary;
    private LocalDate hireDate;

    public Employee(String id, String fullName, String department, double baseSalary, LocalDate hireDate)
            throws InvalidEmployeeDataException {
        this.id = id;
        this.fullName = fullName;
        this.department = department;
        this.baseSalary = baseSalary;
        this.hireDate = hireDate;
    }

    public abstract double calculateSalary(); // trừu tượng tính lương

    public final int getSeniorityYears() {
        return Period.between(hireDate, LocalDate.now()).getYears(); // tính số năm vào làm
    }

    public String getId() {
        return this.id;
    }

    public String getFullName() {
        return this.fullName;
    }

    public String getDepartment() {
        return this.department;
    }

    public double getBaseaSalary() {
        return this.baseSalary;
    }

    public LocalDate getHireDate() {
        return hireDate;
    }

    public void setId(String id) throws InvalidEmployeeDataException {
        if (id == null || id.trim().isEmpty()) {
            throw new InvalidEmployeeDataException("Mã nhân viên không được để trống");
        }
        this.id = id.trim();
    }

    public void setFullName(String fullName) throws InvalidEmployeeDataException {
        if (fullName == null || fullName.trim().isEmpty()) {
            throw new InvalidEmployeeDataException("Họ và tên không được để trống");
        }
        this.fullName = fullName.trim();
    }

    public void setDepartment(String department) throws InvalidEmployeeDataException {
        if (department == null || department.trim().isEmpty()) {
            throw new InvalidEmployeeDataException("phòng ban không được để trống");
        }
        this.department = department.trim();
    }

    public void setBaseSalary(double baseSalary) throws InvalidEmployeeDataException {
        if (baseSalary < 0) {
            throw new InvalidEmployeeDataException("Lương cơ bản không được âm");
        }
        this.baseSalary = baseSalary;
    }

    public void setHireDate(LocalDate hireDate) throws InvalidEmployeeDataException {
        if (hireDate == null || hireDate.isAfter(LocalDate.now()))
            throw new InvalidEmployeeDataException("Ngày vào làm không hợp lệ");
        this.hireDate = hireDate;
    }
}
