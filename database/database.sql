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
    employee_id  VARCHAR(10)   NOT NULL PRIMARY KEY,            -- vd E26_001
    full_name    NVARCHAR(100) NOT NULL,
    gender       VARCHAR(10)   NOT NULL CHECK (gender IN ('MALE', 'FEMALE', 'OTHER')),
    birth_day    DATE          NULL,
    phone_number VARCHAR(15),
    email        VARCHAR(100)  NULL,
    citizen_id   VARCHAR(12)   NULL,
    address      NVARCHAR(255) NULL,
    position_id  INT           NOT NULL,
    salary_type  VARCHAR(10)   NOT NULL CHECK (salary_type IN ('FIXED', 'HOURLY')),
    salary_rate  DECIMAL(12,0) NOT NULL CHECK (salary_rate >= 0),
    hire_date    DATE          NOT NULL DEFAULT CAST(GETDATE() AS DATE),
    status       VARCHAR(10)   NOT NULL DEFAULT 'ACTIVE'
                 CHECK (status IN ('ACTIVE', 'RESIGNED')),
    CONSTRAINT FK_Employee_Position FOREIGN KEY (position_id) REFERENCES Position(position_id),
    CONSTRAINT UQ_Employee_CitizenId UNIQUE (citizen_id)
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
               JOIN ShiftTime s ON s.shift_name  = i.shift_name
               WHERE e.salary_type = 'FIXED'
                 AND (i.start_time <> s.start_time OR i.end_time <> s.end_time))
    BEGIN
        RAISERROR(N'Nhan vien full-time phai dang ky du khung gio cua ca.', 16, 1);
        ROLLBACK TRANSACTION; RETURN;
    END;

    IF EXISTS (SELECT 1 FROM inserted i
               JOIN Employee e ON e.employee_id = i.employee_id
               WHERE e.salary_type = 'HOURLY'
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
               JOIN WorkSchedule w ON w.employee_id = i.employee_id
                                  AND w.work_date   = i.work_date
                                  AND w.schedule_id <> i.schedule_id
               WHERE e.salary_type = 'FIXED'
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
       e.salary_type,
       e.salary_rate,
       h.hours,
       h.work_days,
       CAST(CASE WHEN e.salary_type = 'FIXED' THEN e.salary_rate
                 ELSE e.salary_rate * h.hours END AS DECIMAL(14,0)) AS base_salary
FROM MonthlyHours h
JOIN Employee e ON e.employee_id = h.employee_id
JOIN Position p ON p.position_id = e.position_id;
GO

CREATE VIEW vw_ScheduleDetail AS
SELECT w.schedule_id, w.employee_id, e.full_name, e.salary_type,
       w.work_date, w.shift_name, w.start_time, w.end_time, w.status,
       CAST(DATEDIFF(MINUTE, w.start_time, w.end_time) / 60.0 AS DECIMAL(4,1)) AS hours
FROM WorkSchedule w
JOIN Employee e ON e.employee_id = w.employee_id;
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

-- =========================================================
-- DU LIEU MAU
-- =========================================================

-- Khung gio cua ca
INSERT INTO ShiftTime (shift_name, start_time, end_time) VALUES
(N'Ca sáng',  '06:00', '15:00'),
(N'Ca chiều', '15:00', '23:00');

-- Position
INSERT INTO Position (position_name, salary_type, salary_rate) VALUES
(N'Quản lý',               'FIXED',  10000000),
(N'Master Barista Leader', 'FIXED',  8000000),
(N'Master Barista',        'FIXED',  7000000),
(N'Thu ngân',              'FIXED',  6500000),
(N'Security',              'FIXED',  6000000),
(N'Barista',               'HOURLY', 27000),
(N'Server',                'HOURLY', 27000);

-- Employee: 1 quan ly, 1 MBL, 2 MB, 2 thu ngan, 3 barista, 3 server, 3 security + 1 barista da nghi
INSERT INTO Employee (employee_id, full_name, gender, birth_day, phone_number, email, citizen_id, address,
                      position_id, salary_type, salary_rate, hire_date, status)
SELECT v.employee_id, v.full_name, v.gender, v.birth_day, v.phone_number, v.email, v.citizen_id, v.address,
       p.position_id, p.salary_type, p.salary_rate, v.hire_date, 'ACTIVE'
FROM (VALUES
 ('E26_001', N'Nguyễn Hải Lý',       'MALE',   '1990-03-15', '0901000001', 'ql.nguyen@highland.vn',     '079190000001', N'12 Lê Lợi, Quận 1, TP.HCM',          N'Quản lý',               '2026-01-01'),
 ('E26_002', N'Nguyễn Văn Trí',      'MALE',   '1993-07-22', '0901000002', 'tri.nguyen@highland.vn',    '079193000002', N'45 Nguyễn Trãi, Quận 5, TP.HCM',     N'Master Barista Leader', '2026-01-01'),
 ('E26_003', N'Trần Thị Hoàng Anh',  'FEMALE', '1996-11-05', '0901000003', 'anh.tran@highland.vn',      '079196000003', N'78 Cách Mạng Tháng 8, Quận 3, TP.HCM', N'Master Barista',      '2026-01-01'),
 ('E26_004', N'Lê Minh Cường',       'MALE',   '1995-02-18', '0901000004', 'cuong.le@highland.vn',      '079195000004', N'23 Võ Văn Tần, Quận 3, TP.HCM',      N'Master Barista',        '2026-01-01'),
 ('E26_005', N'Phạm Thị Mai Dung',   'FEMALE', '1998-09-30', '0901000005', 'dung.pham@highland.vn',     '079198000005', N'9 Trần Hưng Đạo, Quận 1, TP.HCM',    N'Thu ngân',              '2026-01-01'),
 ('E26_006', N'Hoàng Quốc Giang',    'MALE',   '1997-05-12', '0901000006', 'giang.hoang@highland.vn',   '079197000006', N'56 Hai Bà Trưng, Quận 1, TP.HCM',    N'Thu ngân',              '2026-01-01'),
 ('E26_007', N'Đỗ Hải Huy',          'MALE',   '2003-01-25', '0901000007', 'huy.do@highland.vn',        '079203000007', N'101 Lý Thái Tổ, Quận 10, TP.HCM',    N'Barista',               '2026-02-01'),
 ('E26_008', N'Vũ Thị Kim Khoa',     'FEMALE', '2002-08-14', '0901000008', 'khoa.vu@highland.vn',       '079202000008', N'34 Nguyễn Đình Chiểu, Quận 3, TP.HCM', N'Barista',             '2026-02-01'),
 ('E26_009', N'Bùi Tấn Long',        'MALE',   '2004-04-03', '0901000009', 'long.bui@highland.vn',      '079204000009', N'67 Phan Xích Long, Phú Nhuận, TP.HCM', N'Barista',             '2026-03-01'),
 ('E26_010', N'Đặng Thanh Nam',      'MALE',   '2003-12-20', '0901000010', 'nam.dang@highland.vn',      '079203000010', N'88 Cộng Hòa, Tân Bình, TP.HCM',      N'Server',                '2026-03-01'),
 ('E26_011', N'Ngô Thị Bích Phượng', 'FEMALE', '2004-06-09', '0901000011', 'phuong.ngo@highland.vn',    '079204000011', N'15 Âu Cơ, Tân Phú, TP.HCM',          N'Server',                '2026-03-15'),
 ('E26_012', N'Dương Minh Quân',     'MALE',   '2002-10-28', '0901000012', 'quan.duong@highland.vn',    '079202000012', N'29 Quang Trung, Gò Vấp, TP.HCM',     N'Server',                '2026-04-01'),
 ('E26_013', N'Lý Thị Thu Sơn',      'FEMALE', '1988-03-07', '0901000013', 'son.ly@highland.vn',        '079188000013', N'5 Nguyễn Văn Cừ, Quận 5, TP.HCM',    N'Security',              '2026-01-01'),
 ('E26_014', N'Hồ Ngọc Tuấn',        'MALE',   '1985-09-16', '0901000014', 'tuan.ho@highland.vn',       '079185000014', N'72 Lê Văn Sỹ, Quận 3, TP.HCM',       N'Security',              '2026-01-01'),
 ('E26_015', N'Phan Văn Uy',         'MALE',   '1987-12-01', '0901000015', 'uy.phan@highland.vn',       '079187000015', N'41 Bà Hạt, Quận 10, TP.HCM',         N'Security',              '2026-01-01'),
 ('E26_016', N'Nguyễn Tấn Việt',     'MALE',   '2003-07-19', '0901000016', 'viet.nguyen@highland.vn',   '079203000016', N'18 Nguyễn Thị Minh Khai, Quận 3, TP.HCM', N'Barista',          '2026-01-01')
) AS v(employee_id, full_name, gender, birth_day, phone_number, email, citizen_id, address, position_name, hire_date)
JOIN Position p ON p.position_name = v.position_name;

-- Account: username = employee_id, mat khau mac dinh 123456
INSERT INTO Account (employee_id, password, role, is_active, must_change_password)
SELECT employee_id, '123456',
       CASE WHEN employee_id = 'E26_001' THEN 'MANAGER' ELSE 'EMPLOYEE' END,
       1, 1
FROM Employee;

-- Nhan vien E26_016 da nghi viec -> trigger tu khoa tai khoan
UPDATE Employee SET status = 'RESIGNED' WHERE employee_id = 'E26_016';
GO

-- MonthlyHours thang 10/2026 (E26_016 da nghi nen khong co)
INSERT INTO MonthlyHours (employee_id, month, year, hours) VALUES
('E26_001', 10, 2026, 198),
('E26_002', 10, 2026, 207),
('E26_003', 10, 2026, 198),
('E26_004', 10, 2026, 189),
('E26_005', 10, 2026, 198),
('E26_006', 10, 2026, 180),
('E26_007', 10, 2026, 92),
('E26_008', 10, 2026, 128),
('E26_009', 10, 2026, 85),
('E26_010', 10, 2026, 104),
('E26_011', 10, 2026, 96),
('E26_012', 10, 2026, 120),
('E26_013', 10, 2026, 207),
('E26_014', 10, 2026, 198),
('E26_015', 10, 2026, 189);

-- MonthlyHours thang 9/2026 (suy ra tu thang 10, them E26_016 truoc khi nghi)
INSERT INTO MonthlyHours (employee_id, month, year, hours)
SELECT employee_id, 9, 2026,
       CAST(hours - (CAST(RIGHT(employee_id, 3) AS INT) % 4) * 9 AS DECIMAL(6,1))
FROM MonthlyHours
WHERE month = 10 AND year = 2026;

INSERT INTO MonthlyHours (employee_id, month, year, hours) VALUES ('E26_016', 9, 2026, 64);

-- Payroll thang 9 va 10/2026 (da chot)
INSERT INTO Payroll (employee_id, month, year, period_start, period_end, salary_type, salary_rate,
                     hours, base_salary, allowance, revenue_bonus, total_salary)
SELECT e.employee_id, m.mth, 2026, m.ps, m.pe, e.salary_type, e.salary_rate,
       h.hours, b.base_salary, a.allowance, a.bonus, b.base_salary + a.allowance + a.bonus
FROM Employee e
JOIN Position p ON p.position_id = e.position_id
CROSS JOIN (VALUES (9,  CAST('2026-09-01' AS DATE), CAST('2026-09-30' AS DATE)),
                   (10, CAST('2026-10-01' AS DATE), CAST('2026-10-31' AS DATE))) AS m(mth, ps, pe)
JOIN MonthlyHours h ON h.employee_id = e.employee_id AND h.month = m.mth AND h.year = 2026
CROSS APPLY (SELECT CAST(CASE WHEN e.salary_type = 'FIXED' THEN e.salary_rate
                              ELSE e.salary_rate * h.hours END AS DECIMAL(14,0)) AS base_salary) b
CROSS APPLY (SELECT CAST(CASE p.position_name
                              WHEN N'Quản lý'               THEN 1000000
                              WHEN N'Master Barista Leader' THEN 500000
                              WHEN N'Thu ngân'              THEN 300000
                              WHEN N'Security'              THEN 300000
                              ELSE 0 END AS DECIMAL(12,0)) AS allowance,
                    CAST(CASE WHEN e.salary_type = 'FIXED' THEN 200000 ELSE 100000 END AS DECIMAL(12,0)) AS bonus) a;
GO

-- =========================================================
-- WorkSchedule: 2 tuan, 09/11 - 22/11/2026
--   Full-time : Thu 2 -> Thu 7, 1 ca/ngay, ma le = Ca sang, ma chan = Ca chieu, DU khung ca
--   Part-time : ca tuan, xoay ca, gio linh dong theo (ma % 3):
--                 Ca sang  : 06-12 | 06-13 | 06-15
--                 Ca chieu : 15-20 | 15-21 | 15-23
--   Tuan 09/11: da chot het. Tuan 16/11: 4 ngay dau da chot, cac ngay sau moi dang ky.
-- =========================================================
;WITH d AS (
    SELECT CAST('2026-11-09' AS DATE) AS work_date
    UNION ALL
    SELECT DATEADD(DAY, 1, work_date) FROM d WHERE work_date < '2026-11-22'
)
INSERT INTO WorkSchedule (employee_id, work_date, shift_name, start_time, end_time, status)
SELECT e.employee_id, d.work_date, z.shift_name, s.start_time,
       CASE WHEN e.salary_type = 'FIXED' THEN s.end_time
            WHEN y.is_morning = 1 THEN CASE x.n % 3 WHEN 0 THEN CAST('12:00' AS TIME)
                                                    WHEN 1 THEN CAST('13:00' AS TIME)
                                                    ELSE        CAST('15:00' AS TIME) END
            ELSE                       CASE x.n % 3 WHEN 0 THEN CAST('20:00' AS TIME)
                                                    WHEN 1 THEN CAST('21:00' AS TIME)
                                                    ELSE        CAST('23:00' AS TIME) END
       END,
       CASE WHEN y.wk = 0 OR e.salary_type = 'FIXED' OR y.wd <= 3 THEN 'CONFIRMED' ELSE 'REGISTERED' END
FROM Employee e
CROSS JOIN d
CROSS APPLY (SELECT CAST(RIGHT(e.employee_id, 3) AS INT) AS n,
                    DATEDIFF(DAY, '2026-11-09', d.work_date) AS dn) x
CROSS APPLY (SELECT x.dn % 7 AS wd, x.dn / 7 AS wk,
                    CASE WHEN e.salary_type = 'FIXED' THEN x.n % 2
                         ELSE (x.n + x.dn % 7) % 2 END AS is_morning) y
CROSS APPLY (SELECT CASE WHEN y.is_morning = 1 THEN N'Ca sáng' ELSE N'Ca chiều' END AS shift_name) z
JOIN ShiftTime s ON s.shift_name = z.shift_name
WHERE e.status = 'ACTIVE'
  AND (e.salary_type = 'HOURLY' OR y.wd <= 5);

-- 1 ca bi huy
UPDATE WorkSchedule SET status = 'CANCELLED'
WHERE employee_id = 'E26_009' AND work_date = '2026-11-18';
GO

-- =========================================================
-- Attendance: tuan 09/11 - 13/11/2026, theo gio DA DANG KY
--   vao: som 5 phut (tre 20 phut khi k = 0); ra: tre 5 phut (som 30 phut khi k = 3)
-- =========================================================
INSERT INTO Attendance (schedule_id, employee_id, work_date, check_in_time, check_out_time, status)
SELECT w.schedule_id, w.employee_id, w.work_date,
       DATEADD(MINUTE, CASE WHEN k.k = 0 THEN 20 ELSE -5 END,
               CAST(w.work_date AS DATETIME) + CAST(w.start_time AS DATETIME)),
       DATEADD(MINUTE, CASE WHEN k.k = 3 THEN -30 ELSE 5 END,
               CAST(w.work_date AS DATETIME) + CAST(w.end_time AS DATETIME)),
       CASE WHEN k.k = 0 THEN 'LATE' WHEN k.k = 3 THEN 'EARLY_LEAVE' ELSE 'ON_TIME' END
FROM WorkSchedule w
CROSS APPLY (SELECT (CAST(RIGHT(w.employee_id, 3) AS INT)
                     + DATEDIFF(DAY, '2026-11-09', w.work_date)) % 7 AS k) k
WHERE w.work_date BETWEEN '2026-11-09' AND '2026-11-13'
  AND w.status = 'CONFIRMED';

-- Vang khong phep
UPDATE Attendance
SET status = 'ABSENT', check_in_time = NULL, check_out_time = NULL
WHERE employee_id = 'E26_009' AND work_date = '2026-11-10';

-- Quen cham cong ra (cho cua hang truong duyet nhanh)
UPDATE Attendance
SET check_out_time = NULL
WHERE employee_id = 'E26_008' AND work_date = '2026-11-12';

-- Cua hang truong chinh sua co luu vet (bo sung gio vao = som 5 phut so voi gio dang ky)
UPDATE a
SET a.check_in_time = DATEADD(MINUTE, -5, CAST(a.work_date AS DATETIME) + CAST(w.start_time AS DATETIME)),
    a.status        = 'ON_TIME',
    a.edit_reason   = N'Nhân viên quên chấm công vào, đã xác minh với trưởng ca',
    a.edited_by     = 'E26_001',
    a.edited_at     = '2026-11-12 09:00'
FROM Attendance a
JOIN WorkSchedule w ON w.schedule_id = a.schedule_id
WHERE a.employee_id = 'E26_010' AND a.work_date = '2026-11-11';

INSERT INTO AttendanceLog (attendance_id, edited_by, edited_at, old_in, old_out, old_status,
                           new_in, new_out, new_status, reason)
SELECT attendance_id, 'E26_001', '2026-11-12 09:00', NULL, check_out_time, 'ON_TIME',
       check_in_time, check_out_time, 'ON_TIME', edit_reason
FROM Attendance
WHERE employee_id = 'E26_010' AND work_date = '2026-11-11';
GO

-- =========================================================
-- LeaveRequest (shift_name NULL = nghi ca ngay)
-- =========================================================
INSERT INTO LeaveRequest (employee_id, request_type, target_date, shift_name, new_shift_name, new_start_time, new_end_time,
                          reason, status, created_at, reviewed_by, reviewed_at) VALUES
('E26_007', 'LEAVE',        '2026-11-17', N'Ca chiều', NULL,        NULL,    NULL,
    N'Có lịch thi học phần',                                  'PENDING',  '2026-11-12 20:15', NULL,      NULL),
('E26_003', 'LEAVE',        '2026-11-20', N'Ca sáng',  NULL,        NULL,    NULL,
    N'Việc gia đình',                                         'PENDING',  '2026-11-13 08:30', NULL,      NULL),
('E26_011', 'SHIFT_CHANGE', '2026-11-19', N'Ca chiều', N'Ca sáng',  '06:00', '12:00',
    N'Xin đổi từ ca chiều sang ca sáng do bận học buổi chiều', 'PENDING', '2026-11-13 21:00', NULL,      NULL),
('E26_009', 'LEAVE',        '2026-11-10', N'Ca chiều', NULL,        NULL,    NULL,
    N'Bị ốm đột xuất',                                        'APPROVED', '2026-11-09 22:00', 'E26_001', '2026-11-10 07:30'),
('E26_005', 'SHIFT_CHANGE', '2026-11-14', N'Ca sáng',  N'Ca chiều', '15:00', '23:00',
    N'Đổi ca với đồng nghiệp E26_006',                        'APPROVED', '2026-11-11 10:00', 'E26_001', '2026-11-11 14:00'),
('E26_012', 'LEAVE',        '2026-11-15', NULL,        NULL,        NULL,    NULL,
    N'Đi chơi với bạn',                                       'REJECTED', '2026-11-12 09:00', 'E26_001', '2026-11-12 16:00');

-- =========================================================
-- Notification + NotificationRead
-- =========================================================
INSERT INTO Notification (sender_id, receiver_id, title, content, created_at) VALUES
('E26_001', NULL,      N'Lịch làm tuần 16/11 - 22/11',  N'Cửa hàng trưởng đã chốt lịch làm tuần tới. Vui lòng kiểm tra và đăng ký các ca còn lại trước 20h Chủ nhật.', '2026-11-13 18:00'),
('E26_001', NULL,      N'Họp cửa hàng thứ Hai',         N'Họp toàn bộ nhân viên lúc 05:45 sáng thứ Hai ngày 16/11 tại cửa hàng.',                                   '2026-11-13 18:30'),
('E26_001', 'E26_009', N'Nhắc nhở vắng không phép',     N'Ngày 10/11 bạn vắng ca. Đơn xin nghỉ đã được duyệt, hãy báo trước sớm hơn cho lần sau.',                   '2026-11-10 08:00'),
('E26_001', 'E26_008', N'Bổ sung chấm công ra',         N'Ngày 12/11 bạn chưa chấm công ra ca. Vui lòng liên hệ cửa hàng trưởng để xác nhận.',                       '2026-11-13 09:00'),
('E26_001', NULL,      N'Bảng lương tháng 10 đã chốt',  N'Bảng lương kỳ 01/10/2026 - 31/10/2026 đã được cập nhật, vui lòng xem trong mục Bảng lương.',                '2026-11-05 10:00');

-- Thong bao 3 da doc boi nguoi nhan; thong bao 5 (gui tat ca) da doc boi moi nhan vien dang lam
INSERT INTO NotificationRead (notification_id, employee_id) VALUES (3, 'E26_009');
INSERT INTO NotificationRead (notification_id, employee_id)
SELECT 5, employee_id FROM Employee WHERE status = 'ACTIVE';
GO

-- MonthlyRevenue: 2025 (12 thang) + 2026 (thang 1-11, thang 11 la luy ke den hien tai)
INSERT INTO MonthlyRevenue (month, year, total_revenue) VALUES
(1, 2025, 520000000), (2, 2025, 480000000), (3, 2025, 545000000), (4, 2025, 560000000),
(5, 2025, 590000000), (6, 2025, 575000000), (7, 2025, 610000000), (8, 2025, 625000000),
(9, 2025, 598000000), (10, 2025, 640000000), (11, 2025, 655000000), (12, 2025, 720000000),
(1, 2026, 560000000), (2, 2026, 515000000), (3, 2026, 585000000), (4, 2026, 600000000),
(5, 2026, 630000000), (6, 2026, 615000000), (7, 2026, 655000000), (8, 2026, 670000000),
(9, 2026, 640000000), (10, 2026, 690000000), (11, 2026, 340000000);
GO

-- =========================================================
-- KIEM TRA NHANH (bo comment de chay)
-- =========================================================
SELECT * FROM vw_ScheduleDetail WHERE work_date BETWEEN '2026-11-16' AND '2026-11-22' ORDER BY work_date, shift_name, start_time;
SELECT * FROM Attendance ORDER BY work_date, employee_id;
SELECT * FROM Payroll;
SELECT * FROM LeaveRequest;
-- thu trigger: full-time dang ky thieu gio (phai bao loi)
INSERT INTO WorkSchedule (employee_id, work_date, shift_name, start_time, end_time) VALUES ('E26_001','2026-11-23',N'Ca sáng','06:00','12:00');
-- thu trigger: part-time duoi 4 gio (phai bao loi)
INSERT INTO WorkSchedule (employee_id, work_date, shift_name, start_time, end_time) VALUES ('E26_007','2026-11-23',N'Ca sáng','06:00','09:00');
-- so nguoi dang lam tai 10:00 ngay 17/11
SELECT COUNT(*) FROM WorkSchedule WHERE work_date='2026-11-17' AND status='CONFIRMED' AND start_time<='10:00' AND end_time>'10:00';
EXEC sp_RefreshMonthlyHours 11, 2026;