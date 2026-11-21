package com.hr.model;

import com.hr.model.enums.RequestStatus;
import com.hr.model.enums.RequestType;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class LeaveRequest {
    private int requestId;
    private String employeeId;
    private RequestType requestType;
    private LocalDate targetDate;
    private String shiftName;          
    private String newShiftName;       
    private LocalTime newStartTime;    
    private LocalTime newEndTime;      
    private String reason;
    private RequestStatus status;
    private LocalDateTime createdAt;
    private String reviewedBy;
    private LocalDateTime reviewedAt;

    private String fullName;

    public LeaveRequest() { }

    public int getRequestId() {
        return requestId;
    }

    public void setRequestId(int v) {
        this.requestId = v;
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(String v) {
        this.employeeId = v;
    }

    public RequestType getRequestType() {
        return requestType;
    }

    public void setRequestType(RequestType v) {
        this.requestType = v;
    }

    public LocalDate getTargetDate() {
        return targetDate;
    }

    public void setTargetDate(LocalDate v) {
        this.targetDate = v;
    }

    public String getShiftName() {
        return shiftName;
    }

    public void setShiftName(String v) {
        this.shiftName = v;
    }

    public String getNewShiftName() {
        return newShiftName;
    }

    public void setNewShiftName(String v) {
        this.newShiftName = v;
    }

    public LocalTime getNewStartTime() {
        return newStartTime;
    }

    public void setNewStartTime(LocalTime v) {
        this.newStartTime = v;
    }

    public LocalTime getNewEndTime() {
        return newEndTime;
    }

    public void setNewEndTime(LocalTime v) {
        this.newEndTime = v;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String v) {
        this.reason = v;
    }

    public RequestStatus getStatus() {
        return status;
    }

    public void setStatus(RequestStatus v) {
        this.status = v;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime v) {
        this.createdAt = v;
    }

    public String getReviewedBy() {
        return reviewedBy;
    }

    public void setReviewedBy(String v) {
        this.reviewedBy = v;
    }

    public LocalDateTime getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(LocalDateTime v) {
        this.reviewedAt = v;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String v) {
        this.fullName = v;
    }
}