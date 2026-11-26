package com.hr.model;

import com.hr.model.enums.EmployeeStatus;
import com.hr.model.enums.Gender;
import com.hr.model.enums.SalaryType;

import java.math.BigDecimal;
import java.time.LocalDate;

public class Employee {
    private String employeeId;
    private String fullName;
    private Gender gender;
    private LocalDate birthDay;
    private String phoneNumber;
    private String email;
    private String citizenId;
    private String address;            // địa chỉ tạm trú (nơi đang ở)
    private int positionId;
    private SalaryType salaryType;     // lấy từ Position (không lưu ở Employee)
    private BigDecimal salaryRate;     // lấy từ Position (không lưu ở Employee)
    private LocalDate hireDate;
    private EmployeeStatus status;

    // ----- JOIN Position -----
    private String positionName;

    // ----- bảng PersonalInfo 
    private String hometown;           // quê quán
    private String ethnicity;          // dân tộc
    private String educationLevel;     // trình độ học vấn
    private String school;             // trường
    private String permanentAddress;   // địa chỉ thường trú

    // ----- bảng BankAccount 
    private String bankName;
    private String bankAccount;

    public Employee() {
    }

    public boolean isFullTime() {
        return salaryType == SalaryType.FIXED;
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(String v) {
        this.employeeId = v;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String v) {
        this.fullName = v;
    }

    public Gender getGender() {
        return gender;
    }

    public void setGender(Gender v) {
        this.gender = v;
    }

    public LocalDate getBirthDay() {
        return birthDay;
    }

    public void setBirthDay(LocalDate v) {
        this.birthDay = v;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String v) {
        this.phoneNumber = v;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String v) {
        this.email = v;
    }

    public String getCitizenId() {
        return citizenId;
    }

    public void setCitizenId(String v) {
        this.citizenId = v;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String v) {
        this.address = v;
    }

    public int getPositionId() {
        return positionId;
    }

    public void setPositionId(int v) {
        this.positionId = v;
    }

    public String getPositionName() {
        return positionName;
    }

    public void setPositionName(String v) {
        this.positionName = v;
    }

    public SalaryType getSalaryType() {
        return salaryType;
    }

    public void setSalaryType(SalaryType v) {
        this.salaryType = v;
    }

    public BigDecimal getSalaryRate() {
        return salaryRate;
    }

    public void setSalaryRate(BigDecimal v) {
        this.salaryRate = v;
    }

    public LocalDate getHireDate() {
        return hireDate;
    }

    public void setHireDate(LocalDate v) {
        this.hireDate = v;
    }

    public EmployeeStatus getStatus() {
        return status;
    }

    public void setStatus(EmployeeStatus v) {
        this.status = v;
    }

    public String getHometown() {
        return hometown;
    }

    public void setHometown(String v) {
        this.hometown = v;
    }

    public String getEthnicity() {
        return ethnicity;
    }

    public void setEthnicity(String v) {
        this.ethnicity = v;
    }

    public String getEducationLevel() {
        return educationLevel;
    }

    public void setEducationLevel(String v) {
        this.educationLevel = v;
    }

    public String getSchool() {
        return school;
    }

    public void setSchool(String v) {
        this.school = v;
    }

    public String getPermanentAddress() {
        return permanentAddress;
    }

    public void setPermanentAddress(String v) {
        this.permanentAddress = v;
    }

    public String getBankName() {
        return bankName;
    }

    public void setBankName(String v) {
        this.bankName = v;
    }

    public String getBankAccount() {
        return bankAccount;
    }

    public void setBankAccount(String v) {
        this.bankAccount = v;
    }

    @Override
    public String toString() {
        return employeeId + " - " + fullName;
    }
}