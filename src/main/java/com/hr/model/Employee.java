package com.hr.model;

import com.hr.model.enums.EmployeeStatus;
import com.hr.model.enums.Gender;
import com.hr.model.enums.SalaryType;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Nhân viên. Dữ liệu lấy từ nhiều bảng:
 *   Employee (+ Position)  : các trường lõi
 *   EmployeeProfile        : hometown, ethnicity, educationLevel, school, tempAddress
 *   EmployeeBank           : bankName, bankAccount
 */
public class Employee {
    // ----- bảng Employee -----
    private String employeeId;
    private String fullName;
    private Gender gender;
    private LocalDate birthDay;
    private String phoneNumber;
    private String email;
    private String citizenId;
    private String address;            // địa chỉ thường trú
    private int positionId;
    private SalaryType salaryType;
    private BigDecimal salaryRate;
    private LocalDate hireDate;
    private EmployeeStatus status;

    // ----- JOIN Position -----
    private String positionName;

    // ----- bảng EmployeeProfile (có thể chưa có dòng -> null) -----
    private String hometown;           // quê quán
    private String ethnicity;          // dân tộc
    private String educationLevel;     // trình độ học vấn
    private String school;             // trường
    private String tempAddress;        // địa chỉ tạm trú

    // ----- bảng EmployeeBank (có thể chưa có dòng -> null) -----
    private String bankName;
    private String bankAccount;

    public Employee() {
    }

    /** Full-time = lương cố định theo tháng; Part-time = lương theo giờ. */
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

    public String getTempAddress() {
        return tempAddress;
    }

    public void setTempAddress(String v) {
        this.tempAddress = v;
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