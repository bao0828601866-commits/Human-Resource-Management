package com.hr.model;

import com.hr.model.enums.AttendanceStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Nhật ký một lần sửa chấm công (trước / sau). */
public class AttendanceLog {
    private int logId;
    private int attendanceId;
    private String editedBy;
    private LocalDateTime editedAt;
    private LocalDateTime oldIn, oldOut;
    private AttendanceStatus oldStatus;
    private LocalDateTime newIn, newOut;
    private AttendanceStatus newStatus;
    private String reason;

    // Lấy từ JOIN Attendance, chỉ để hiển thị cột "Bản ghi"
    private String employeeId;
    private LocalDate workDate;

    public AttendanceLog() { }

    public int getLogId()                        { return logId; }
    public void setLogId(int v)                  { this.logId = v; }

    public int getAttendanceId()                 { return attendanceId; }
    public void setAttendanceId(int v)           { this.attendanceId = v; }

    public String getEditedBy()                  { return editedBy; }
    public void setEditedBy(String v)            { this.editedBy = v; }

    public LocalDateTime getEditedAt()           { return editedAt; }
    public void setEditedAt(LocalDateTime v)     { this.editedAt = v; }

    public LocalDateTime getOldIn()              { return oldIn; }
    public void setOldIn(LocalDateTime v)        { this.oldIn = v; }

    public LocalDateTime getOldOut()             { return oldOut; }
    public void setOldOut(LocalDateTime v)       { this.oldOut = v; }

    public AttendanceStatus getOldStatus()       { return oldStatus; }
    public void setOldStatus(AttendanceStatus v) { this.oldStatus = v; }

    public LocalDateTime getNewIn()              { return newIn; }
    public void setNewIn(LocalDateTime v)        { this.newIn = v; }

    public LocalDateTime getNewOut()             { return newOut; }
    public void setNewOut(LocalDateTime v)       { this.newOut = v; }

    public AttendanceStatus getNewStatus()       { return newStatus; }
    public void setNewStatus(AttendanceStatus v) { this.newStatus = v; }

    public String getReason()                    { return reason; }
    public void setReason(String v)              { this.reason = v; }

    public String getEmployeeId()                { return employeeId; }
    public void setEmployeeId(String v)          { this.employeeId = v; }

    public LocalDate getWorkDate()               { return workDate; }
    public void setWorkDate(LocalDate v)         { this.workDate = v; }
}
