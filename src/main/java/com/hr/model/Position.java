package com.hr.model;

import com.hr.model.enums.SalaryType;
import java.math.BigDecimal;

public class Position {
    private final int positionId;
    private final String positionName;
    private final SalaryType salaryType;  // FIXED: lương/tháng, HOURLY: lương/giờ
    private final BigDecimal salaryRate;

    public Position(int positionId, String positionName, SalaryType salaryType, BigDecimal salaryRate) {
        this.positionId = positionId;
        this.positionName = positionName;
        this.salaryType = salaryType;
        this.salaryRate = salaryRate;
    }

    public int getPositionId()         { return positionId; }
    public String getPositionName()    { return positionName; }
    public SalaryType getSalaryType()  { return salaryType; }
    public BigDecimal getSalaryRate()  { return salaryRate; }

    @Override
    public String toString() {          // để JComboBox hiển thị tên chức vụ
        return positionName;
    }
}
