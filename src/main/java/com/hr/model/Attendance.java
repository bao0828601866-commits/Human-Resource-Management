package com.hr.model;

import com.hr.model.enums.AttendanceStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class Attendance {
    private int attendanceId;
    private int scheduleId;
    private String employeeId;
    private LocalDate workDate;
    private LocalDateTime checkInTime;    // có thể null
    private LocalDateTime checkOutTime;   // có thể null (quên chấm công ra)
    private AttendanceStatus status;
    private String editReason;            // lý do sửa gần nhất
    private String editedBy;
    private LocalDateTime editedAt;

    // Lấy từ JOIN, chỉ để hiển thị
    private String fullName;
    private LocalTime scheduledStart;     // giờ đã đăng ký (WorkSchedule.start_time)
    private LocalTime scheduledEnd;       // giờ đã đăng ký (WorkSchedule.end_time)

    public Attendance() { }

    public int getAttendanceId()                  { return attendanceId; }
    public void setAttendanceId(int v)            { this.attendanceId = v; }

    public int getScheduleId()                    { return scheduleId; }
    public void setScheduleId(int v)              { this.scheduleId = v; }

    public String getEmployeeId()                 { return employeeId; }
    public void setEmployeeId(String v)           { this.employeeId = v; }

    public LocalDate getWorkDate()                { return workDate; }
    public void setWorkDate(LocalDate v)          { this.workDate = v; }

    public LocalDateTime getCheckInTime()         { return checkInTime; }
    public void setCheckInTime(LocalDateTime v)   { this.checkInTime = v; }

    public LocalDateTime getCheckOutTime()        { return checkOutTime; }
    public void setCheckOutTime(LocalDateTime v)  { this.checkOutTime = v; }

    public AttendanceStatus getStatus()           { return status; }
    public void setStatus(AttendanceStatus v)     { this.status = v; }

    public String getEditReason()                 { return editReason; }
    public void setEditReason(String v)           { this.editReason = v; }

    public String getEditedBy()                   { return editedBy; }
    public void setEditedBy(String v)             { this.editedBy = v; }

    public LocalDateTime getEditedAt()            { return editedAt; }
    public void setEditedAt(LocalDateTime v)      { this.editedAt = v; }

    public String getFullName()                   { return fullName; }
    public void setFullName(String v)             { this.fullName = v; }

    public LocalTime getScheduledStart()          { return scheduledStart; }
    public void setScheduledStart(LocalTime v)    { this.scheduledStart = v; }

    public LocalTime getScheduledEnd()            { return scheduledEnd; }
    public void setScheduledEnd(LocalTime v)      { this.scheduledEnd = v; }
}
