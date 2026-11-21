package com.hr.model;

import java.time.LocalTime;

public class ShiftTime {
    private final String shiftName;
    private final LocalTime startTime;
    private final LocalTime endTime;

    public ShiftTime(String shiftName, LocalTime startTime, LocalTime endTime) {
        this.shiftName = shiftName;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public String getShiftName() {
        return shiftName;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    @Override
    public String toString() {
        return shiftName + " (" + startTime + " - " + endTime + ")";
    }
}