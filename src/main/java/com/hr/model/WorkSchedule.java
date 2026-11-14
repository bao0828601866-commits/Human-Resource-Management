package com.hr.model;

import com.hr.model.enums.SalaryType;
import com.hr.model.enums.ScheduleStatus;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;

public class WorkSchedule {
    private int scheduleId;
    private String employeeId;
    private LocalDate workDate;
    private String shiftName;
    private LocalTime startTime;
    private LocalTime endTime;
    private ScheduleStatus status;

    // Lấy từ JOIN Employee (vw_ScheduleDetail), chỉ để hiển thị
    private String fullName;
    private SalaryType salaryType;

    public WorkSchedule() { }

    /** Số giờ của ca đăng ký. */
    public double getHours() {
        return Duration.between(startTime, endTime).toMinutes() / 60.0;
    }

    public int getScheduleId()               { return scheduleId; }
    public void setScheduleId(int v)         { this.scheduleId = v; }

    public String getEmployeeId()            { return employeeId; }
    public void setEmployeeId(String v)      { this.employeeId = v; }

    public LocalDate getWorkDate()           { return workDate; }
    public void setWorkDate(LocalDate v)     { this.workDate = v; }

    public String getShiftName()             { return shiftName; }
    public void setShiftName(String v)       { this.shiftName = v; }

    public LocalTime getStartTime()          { return startTime; }
    public void setStartTime(LocalTime v)    { this.startTime = v; }

    public LocalTime getEndTime()            { return endTime; }
    public void setEndTime(LocalTime v)      { this.endTime = v; }

    public ScheduleStatus getStatus()        { return status; }
    public void setStatus(ScheduleStatus v)  { this.status = v; }

    public String getFullName()              { return fullName; }
    public void setFullName(String v)        { this.fullName = v; }

    public SalaryType getSalaryType()        { return salaryType; }
    public void setSalaryType(SalaryType v)  { this.salaryType = v; }
}
