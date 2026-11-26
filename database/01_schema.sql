IF DB_ID('QuanLyNhanSu') IS NULL CREATE DATABASE QuanLyNhanSu;
GO
USE QuanLyNhanSu;
GO

CREATE TABLE Position (
    position_id   INT IDENTITY(1,1) PRIMARY KEY,
    position_name NVARCHAR(50)  NOT NULL UNIQUE,
    salary_type   VARCHAR(10)   NOT NULL CHECK (salary_type IN ('FIXED', 'HOURLY')),
    salary_rate   DECIMAL(12,0) NOT NULL CHECK (salary_rate >= 0)  -- FIXED: luong/thang, HOURLY: luong/gio
);

CREATE TABLE Employee (
    employee_id  VARCHAR(10)   NOT NULL PRIMARY KEY, -- VD E26_001
    full_name    NVARCHAR(100) NOT NULL,
    gender       VARCHAR(10)   NOT NULL CHECK (gender IN ('MALE', 'FEMALE', 'OTHER')),
    birth_day    DATE          NULL,
    phone_number VARCHAR(15)   NULL,
    email        VARCHAR(100)  NULL,
    citizen_id   VARCHAR(12)   NULL,
    address      NVARCHAR(255) NULL, -- Dia chi tam tru (noi dang o, TP.HCM)
    position_id  INT           NOT NULL,
    hire_date    DATE          NOT NULL DEFAULT CAST(GETDATE() AS DATE),
    status       VARCHAR(10)   NOT NULL DEFAULT 'ACTIVE'
                 CHECK (status IN ('ACTIVE', 'RESIGNED')),
    CONSTRAINT FK_Employee_Position FOREIGN KEY (position_id) REFERENCES Position(position_id),
    CONSTRAINT UQ_Employee_CitizenId UNIQUE (citizen_id)
);

CREATE TABLE PersonalInfo (
    employee_id        VARCHAR(10)   NOT NULL PRIMARY KEY,
    permanent_address  NVARCHAR(255) NULL, -- Dia chi thuong tru (theo que quan)
    hometown           NVARCHAR(100) NULL, -- Que quan
    ethnicity          NVARCHAR(30)  NULL, -- Dan toc
    education_level    NVARCHAR(30)  NULL, -- Trinh do hoc van
    school             NVARCHAR(150) NULL, -- Truong 
    CONSTRAINT FK_Profile_Employee FOREIGN KEY (employee_id) REFERENCES Employee(employee_id)
);

CREATE TABLE BankAccount (
    employee_id  VARCHAR(10)  NOT NULL PRIMARY KEY,
    bank_name    NVARCHAR(60) NOT NULL,
    bank_account VARCHAR(20)  NOT NULL
                 CHECK (LEN(bank_account) >= 6 AND bank_account NOT LIKE '%[^0-9]%'),
    CONSTRAINT FK_Bank_Employee FOREIGN KEY (employee_id) REFERENCES Employee(employee_id)
);

CREATE TABLE Account (
    employee_id          VARCHAR(10)  NOT NULL PRIMARY KEY,     -- chinh la username
    password             VARCHAR(100) NOT NULL DEFAULT '123456',-- mat khau mac dinh (hoac hash)
    role                 VARCHAR(10)  NOT NULL DEFAULT 'EMPLOYEE'
                         CHECK (role IN ('MANAGER', 'EMPLOYEE')),
    is_active            BIT          NOT NULL DEFAULT 1,       -- 0 = nghi viec, chan dang nhap
    must_change_password BIT          NOT NULL DEFAULT 1,       -- 1 = bat doi mat khau o lan dang nhap dau
    CONSTRAINT FK_Account_Employee FOREIGN KEY (employee_id) REFERENCES Employee(employee_id)
);

CREATE TABLE ShiftTime (
    shift_name NVARCHAR(20) NOT NULL PRIMARY KEY,
    start_time TIME NOT NULL,
    end_time   TIME NOT NULL,
    CONSTRAINT CK_ShiftTime CHECK (end_time > start_time)
);

CREATE TABLE WorkSchedule (
    schedule_id INT IDENTITY(1,1) PRIMARY KEY,
    employee_id VARCHAR(10)  NOT NULL,
    work_date   DATE         NOT NULL,
    shift_name  NVARCHAR(20) NOT NULL,
    start_time  TIME         NOT NULL,
    end_time    TIME         NOT NULL,
    status      VARCHAR(12)  NOT NULL DEFAULT 'REGISTERED'
                CHECK (status IN ('REGISTERED', 'CONFIRMED', 'CANCELLED')),
    CONSTRAINT FK_WorkSchedule_Employee FOREIGN KEY (employee_id) REFERENCES Employee(employee_id),
    CONSTRAINT FK_WorkSchedule_Shift    FOREIGN KEY (shift_name)  REFERENCES ShiftTime(shift_name),
    CONSTRAINT CK_WorkSchedule_Time     CHECK (end_time > start_time)
);

CREATE TABLE Attendance (
    attendance_id  INT IDENTITY(1,1) PRIMARY KEY,
    schedule_id    INT           NOT NULL UNIQUE,               -- 1 lich lam = 1 luot cham cong
    employee_id    VARCHAR(10)   NOT NULL,
    work_date      DATE          NOT NULL,
    check_in_time  DATETIME      NULL,
    check_out_time DATETIME      NULL,
    status         VARCHAR(12)   NOT NULL DEFAULT 'ON_TIME'
                   CHECK (status IN ('ON_TIME', 'LATE', 'EARLY_LEAVE', 'ABSENT')),
    edit_reason    NVARCHAR(500) NULL,         -- ly do sua thu cong (lan sua gan nhat)
    edited_by      VARCHAR(10)   NULL,
    edited_at      DATETIME      NULL,
    CONSTRAINT FK_Attendance_Schedule FOREIGN KEY (schedule_id) REFERENCES WorkSchedule(schedule_id),
    CONSTRAINT FK_Attendance_Employee FOREIGN KEY (employee_id) REFERENCES Employee(employee_id),
    CONSTRAINT FK_Attendance_EditedBy FOREIGN KEY (edited_by)   REFERENCES Employee(employee_id),
    CONSTRAINT CK_Attendance_Time CHECK (check_out_time IS NULL OR check_in_time IS NULL OR check_out_time > check_in_time)
);

CREATE TABLE AttendanceLog (
    log_id        INT IDENTITY(1,1) PRIMARY KEY,
    attendance_id INT           NOT NULL,
    edited_by     VARCHAR(10)   NOT NULL,
    edited_at     DATETIME      NOT NULL DEFAULT GETDATE(),
    old_in        DATETIME      NULL,
    old_out       DATETIME      NULL,
    old_status    VARCHAR(12)   NULL,
    new_in        DATETIME      NULL,
    new_out       DATETIME      NULL,
    new_status    VARCHAR(12)   NULL,
    reason        NVARCHAR(500) NOT NULL,
    CONSTRAINT FK_AttLog_Attendance FOREIGN KEY (attendance_id) REFERENCES Attendance(attendance_id),
    CONSTRAINT FK_AttLog_EditedBy   FOREIGN KEY (edited_by)     REFERENCES Employee(employee_id)
);

CREATE TABLE MonthlyHours (
    record_id   INT IDENTITY(1,1) PRIMARY KEY,
    employee_id VARCHAR(10)  NOT NULL,
    month       TINYINT      NOT NULL CHECK (month BETWEEN 1 AND 12),
    year        SMALLINT     NOT NULL CHECK (year >= 2000),
    hours       DECIMAL(6,1) NOT NULL CHECK (hours >= 0),
    work_days   AS CAST(hours / 9.0 AS DECIMAL(5,1)) PERSISTED,
    CONSTRAINT UQ_MonthlyHours UNIQUE (employee_id, month, year),
    CONSTRAINT FK_MonthlyHours_Employee FOREIGN KEY (employee_id) REFERENCES Employee(employee_id)
);

CREATE TABLE Payroll (
    employee_id   VARCHAR(10)   NOT NULL,
    month         TINYINT       NOT NULL CHECK (month BETWEEN 1 AND 12),
    year          SMALLINT      NOT NULL CHECK (year >= 2000),
    period_start  DATE          NOT NULL,
    period_end    DATE          NOT NULL,
    salary_type   VARCHAR(10)   NOT NULL CHECK (salary_type IN ('FIXED', 'HOURLY')),
    salary_rate   DECIMAL(12,0) NOT NULL,
    hours         DECIMAL(6,1)  NOT NULL DEFAULT 0,
    base_salary   DECIMAL(14,0) NOT NULL DEFAULT 0,
    allowance     DECIMAL(12,0) NOT NULL DEFAULT 0,
    revenue_bonus DECIMAL(12,0) NOT NULL DEFAULT 0,
    total_salary  DECIMAL(14,0) NOT NULL,
    created_at    DATETIME      NOT NULL DEFAULT GETDATE(),
    PRIMARY KEY (employee_id, month, year),
    CONSTRAINT FK_Payroll_Employee FOREIGN KEY (employee_id) REFERENCES Employee(employee_id),
    CONSTRAINT CK_Payroll_Period CHECK (period_end >= period_start)
);

CREATE TABLE LeaveRequest (
    request_id     INT IDENTITY(1,1) PRIMARY KEY,
    employee_id    VARCHAR(10)   NOT NULL,
    request_type   VARCHAR(15)   NOT NULL CHECK (request_type IN ('LEAVE', 'SHIFT_CHANGE')),
    target_date    DATE          NOT NULL,
    shift_name     NVARCHAR(20)  NULL,
    new_shift_name NVARCHAR(20)  NULL,
    new_start_time TIME          NULL,
    new_end_time   TIME          NULL,
    reason         NVARCHAR(500) NOT NULL,
    status         VARCHAR(10)   NOT NULL DEFAULT 'PENDING'
                   CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED')),
    created_at     DATETIME      NOT NULL DEFAULT GETDATE(),
    reviewed_by    VARCHAR(10)   NULL,
    reviewed_at    DATETIME      NULL,
    CONSTRAINT FK_LeaveRequest_Employee FOREIGN KEY (employee_id)    REFERENCES Employee(employee_id),
    CONSTRAINT FK_LeaveRequest_Reviewer FOREIGN KEY (reviewed_by)    REFERENCES Employee(employee_id),
    CONSTRAINT FK_LeaveRequest_Shift    FOREIGN KEY (shift_name)     REFERENCES ShiftTime(shift_name),
    CONSTRAINT FK_LeaveRequest_NewShift FOREIGN KEY (new_shift_name) REFERENCES ShiftTime(shift_name),
    CONSTRAINT CK_LeaveRequest_Change CHECK (
        request_type = 'LEAVE'
        OR (new_shift_name IS NOT NULL AND new_start_time IS NOT NULL
            AND new_end_time IS NOT NULL AND new_end_time > new_start_time))
);

CREATE TABLE Notification (
    notification_id INT IDENTITY(1,1) PRIMARY KEY,
    sender_id       VARCHAR(10)    NOT NULL,
    receiver_id     VARCHAR(10)    NULL,
    title           NVARCHAR(150)  NOT NULL,
    content         NVARCHAR(1000) NOT NULL,
    created_at      DATETIME       NOT NULL DEFAULT GETDATE(),
    CONSTRAINT FK_Notification_Sender   FOREIGN KEY (sender_id)   REFERENCES Employee(employee_id),
    CONSTRAINT FK_Notification_Receiver FOREIGN KEY (receiver_id) REFERENCES Employee(employee_id)
);

CREATE TABLE NotificationRead (
    notification_id INT         NOT NULL,
    employee_id     VARCHAR(10) NOT NULL,
    read_at         DATETIME    NOT NULL DEFAULT GETDATE(),
    PRIMARY KEY (notification_id, employee_id),
    CONSTRAINT FK_NotiRead_Notification FOREIGN KEY (notification_id) REFERENCES Notification(notification_id),
    CONSTRAINT FK_NotiRead_Employee     FOREIGN KEY (employee_id)     REFERENCES Employee(employee_id)
);

CREATE TABLE MonthlyRevenue (
    revenue_id    INT IDENTITY(1,1) PRIMARY KEY,
    month         TINYINT       NOT NULL CHECK (month BETWEEN 1 AND 12),
    year          SMALLINT      NOT NULL CHECK (year >= 2000),
    total_revenue DECIMAL(15,0) NOT NULL CHECK (total_revenue >= 0),
    CONSTRAINT UQ_MonthlyRevenue UNIQUE (month, year)
);

-- Index ho tro truy van
CREATE INDEX IX_Employee_Position     ON Employee(position_id);
CREATE INDEX IX_Employee_Status       ON Employee(status);
CREATE INDEX IX_WorkSchedule_Date     ON WorkSchedule(work_date);
CREATE INDEX IX_WorkSchedule_Emp      ON WorkSchedule(employee_id, work_date);
CREATE INDEX IX_Attendance_Date       ON Attendance(work_date);
CREATE INDEX IX_LeaveRequest_Status   ON LeaveRequest(status);
CREATE INDEX IX_Notification_Receiver ON Notification(receiver_id);
GO

CREATE TRIGGER trg_Employee_StatusSync ON Employee
AFTER UPDATE
AS
BEGIN
    SET NOCOUNT ON;
    IF UPDATE(status)
        UPDATE a
        SET a.is_active = CASE WHEN i.status = 'ACTIVE' THEN 1 ELSE 0 END
        FROM Account a
        JOIN inserted i ON i.employee_id = a.employee_id;
END;
GO

CREATE TRIGGER trg_WorkSchedule_Validate ON WorkSchedule
AFTER INSERT, UPDATE
AS
BEGIN
    SET NOCOUNT ON;

    IF EXISTS (SELECT 1 FROM inserted i
               JOIN ShiftTime s ON s.shift_name = i.shift_name
               WHERE i.start_time < s.start_time OR i.end_time > s.end_time)
    BEGIN
        RAISERROR(N'Gio lam nam ngoai khung gio cua ca.', 16, 1);
        ROLLBACK TRANSACTION; RETURN;
    END;

    IF EXISTS (SELECT 1 FROM inserted i
               JOIN Employee e  ON e.employee_id = i.employee_id
               JOIN Position p ON p.position_id = e.position_id
               JOIN ShiftTime s ON s.shift_name  = i.shift_name
               WHERE p.salary_type = 'FIXED'
                 AND (i.start_time <> s.start_time OR i.end_time <> s.end_time))
    BEGIN
        RAISERROR(N'Nhan vien full-time phai dang ky du khung gio cua ca.', 16, 1);
        ROLLBACK TRANSACTION; RETURN;
    END;

    IF EXISTS (SELECT 1 FROM inserted i
               JOIN Employee e ON e.employee_id = i.employee_id
               JOIN Position p ON p.position_id = e.position_id
               WHERE p.salary_type = 'HOURLY'
                 AND DATEDIFF(MINUTE, i.start_time, i.end_time) < 240)
    BEGIN
        RAISERROR(N'Nhan vien part-time dang ky toi thieu 4 gio moi lan.', 16, 1);
        ROLLBACK TRANSACTION; RETURN;
    END;

    IF EXISTS (SELECT 1 FROM inserted i
               JOIN WorkSchedule w ON w.employee_id = i.employee_id
                                  AND w.work_date   = i.work_date
                                  AND w.schedule_id <> i.schedule_id
               WHERE i.status <> 'CANCELLED' AND w.status <> 'CANCELLED'
                 AND w.start_time < i.end_time AND i.start_time < w.end_time)
    BEGIN
        RAISERROR(N'Khoang gio dang ky bi chong voi ca khac trong cung ngay.', 16, 1);
        ROLLBACK TRANSACTION; RETURN;
    END;

    IF EXISTS (SELECT 1 FROM inserted i
               JOIN Employee e ON e.employee_id = i.employee_id
               JOIN Position p ON p.position_id = e.position_id
               JOIN WorkSchedule w ON w.employee_id = i.employee_id
                                  AND w.work_date   = i.work_date
                                  AND w.schedule_id <> i.schedule_id
               WHERE p.salary_type = 'FIXED'
                 AND i.status <> 'CANCELLED' AND w.status <> 'CANCELLED')
    BEGIN
        RAISERROR(N'Nhan vien full-time chi duoc dang ky 1 ca moi ngay.', 16, 1);
        ROLLBACK TRANSACTION; RETURN;
    END;
END;
GO

CREATE VIEW vw_MonthlySalary AS
SELECT e.employee_id,
       e.full_name,
       p.position_name,
       h.month,
       h.year,
       p.salary_type,
       p.salary_rate,
       h.hours,
       h.work_days,
       CAST(CASE WHEN p.salary_type = 'FIXED' THEN p.salary_rate
                 ELSE p.salary_rate * h.hours END AS DECIMAL(14,0)) AS base_salary
FROM MonthlyHours h
JOIN Employee e ON e.employee_id = h.employee_id
JOIN Position p ON p.position_id = e.position_id;
GO

CREATE VIEW vw_ScheduleDetail AS
SELECT w.schedule_id, w.employee_id, e.full_name, p.salary_type,
       w.work_date, w.shift_name, w.start_time, w.end_time, w.status,
       CAST(DATEDIFF(MINUTE, w.start_time, w.end_time) / 60.0 AS DECIMAL(4,1)) AS hours
FROM WorkSchedule w
JOIN Employee e ON e.employee_id = w.employee_id
JOIN Position p ON p.position_id = e.position_id;
GO

CREATE PROCEDURE sp_RefreshMonthlyHours @month TINYINT, @year SMALLINT
AS
BEGIN
    SET NOCOUNT ON;
    MERGE MonthlyHours AS t
    USING (SELECT employee_id,
                  CAST(SUM(DATEDIFF(MINUTE, check_in_time, check_out_time)) / 60.0 AS DECIMAL(6,1)) AS hours
           FROM Attendance
           WHERE MONTH(work_date) = @month AND YEAR(work_date) = @year
             AND check_in_time IS NOT NULL AND check_out_time IS NOT NULL
           GROUP BY employee_id) AS s
    ON t.employee_id = s.employee_id AND t.month = @month AND t.year = @year
    WHEN MATCHED THEN UPDATE SET hours = s.hours
    WHEN NOT MATCHED THEN INSERT (employee_id, month, year, hours)
         VALUES (s.employee_id, @month, @year, s.hours);
END;
GO