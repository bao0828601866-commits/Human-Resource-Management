IF DB_ID('QuanLyNhanSu') IS NULL CREATE DATABASE QuanLyNhanSu;
GO
USE QuanLyNhanSu;
GO

CREATE TABLE Employee (
    employee_id  VARCHAR(10)   NOT NULL PRIMARY KEY,
    full_name    NVARCHAR(100) NOT NULL,
    position     NVARCHAR(50)  NOT NULL,
    salary_type  NVARCHAR(10)  NOT NULL CHECK (salary_type IN ('FIXED', 'HOURLY')),
    salary_rate  DECIMAL(12,0) NOT NULL,
    phone_number VARCHAR(15),
    status       VARCHAR(10)   NOT NULL DEFAULT 'ACTIVE'
                 CHECK (status IN ('ACTIVE','RESIGNED'))
);

CREATE TABLE Account (
    username    VARCHAR(50)  NOT NULL PRIMARY KEY,
    password    VARCHAR(255) NOT NULL,
    role        VARCHAR(10) NOT NULL CHECK (role IN ('MANAGER', 'EMPLOYEE')),
    employee_id VARCHAR(10) NOT NULL UNIQUE,
    FOREIGN KEY (employee_id) REFERENCES Employee(employee_id)
);

CREATE TABLE MonthlyHours (
    employee_id VARCHAR(10),
    month       INT,
    year        INT,
    hours       DECIMAL(6,1) NOT NULL,
    PRIMARY KEY (employee_id, month, year),
    FOREIGN KEY (employee_id) REFERENCES Employee(employee_id)
)

INSERT INTO Employee VALUES
-- 1 Master Barista Leader
('E26_001', N'Nguyễn Văn Trí', N'Master Barista Leader', 'FIXED', 8000000, '12345', 'ACTIVE'),

-- 2 Master Barista
('E26_002', N'Trần Thị Hoàng Anh', N'Master Barista', 'FIXED', 6500000, '12345', 'ACTIVE'),
('E26_003', N'Lê Minh Cường', N'Master Barista', 'FIXED', 6500000, '12345', 'ACTIVE'),

-- 2 Thu ngân
('E26_004', N'Pham Thị Mai Dung', N'Thu ngân', 'FIXED', 7000000, '12345', 'ACTIVE'),
('E26_005', N'Hoàng Quốc Giang', N'Thu ngân', 'FIXED', 7000000, '12345', 'ACTIVE'),

-- 5 Barista
('E26_006', N'Đỗ Hải Huy', N'Barista', 'HOURLY', 27000, '12345', 'ACTIVE'),
('E26_007', N'Vũ Thị Kim Khoa', N'Barista', 'HOURLY', 27000, '12345', 'ACTIVE'),
('E26_008', N'Bùi Tấn Long', N'Barista', 'HOURLY', 27000, '12345', 'ACTIVE'),
('E26_009', N'Đặng Thanh Nam', N'Barista', 'HOURLY', 27000, '12345', 'ACTIVE'),
('E26_010', N'Ngô Thị Bích Phượng', N'Barista', 'HOURLY', 27000, '12345', 'ACTIVE'),

-- 3 Server (Nhân viên phục vụ)
('E26_011', N'Dương Minh Quân', N'Server', 'HOURLY', 27000, '12345', 'ACTIVE'),
('E26_012', N'Lý Thị Thu Sơn', N'Server', 'HOURLY', 27000, '12345', 'ACTIVE'),
('E26_013', N'Hồ Ngọc Tuấn', N'Server', 'HOURLY', 27000, '12345', 'ACTIVE'),

-- 3 Security (Bảo vệ)
('E26_014', N'Phan Văn Uy', N'Security', 'FIXED', 7000000, '12345', 'ACTIVE'),
('E26_015', N'Nguyễn Tấn Việt', N'Security', 'FIXED', 7000000, '12345', 'ACTIVE'),
('E26_016', N'Trịnh Xuân Lộc', N'Security', 'FIXED', 7000000, '12345', 'ACTIVE');

INSERT INTO Account VALUES
('giabao', '123456', 'MANAGER', 'E26_001'),
('hoanganh', '123456', 'EMPLOYEE', 'E26_002'),
('minhcuong', '123456', 'EMPLOYEE', 'E26_003'),
('maidung', '123456', 'EMPLOYEE', 'E26_004'),
('quocgiang', '123456', 'EMPLOYEE', 'E26_005'),
('haihuy', '123456', 'EMPLOYEE', 'E26_006'),
('kimkhoa', '123456', 'EMPLOYEE', 'E26_007'),
('tanlong', '123456', 'EMPLOYEE', 'E26_008'),
('thanhnam', '123456', 'EMPLOYEE', 'E26_009'),
('bichphuong', '123456', 'EMPLOYEE', 'E26_010'),
('minhquan', '123456', 'EMPLOYEE', 'E26_011'),
('thuson', '123456', 'EMPLOYEE', 'E26_012'),
('ngoctuan', '123456', 'EMPLOYEE', 'E26_013'),
('vanuy', '123456', 'EMPLOYEE', 'E26_014'),
('tanviet', '123456', 'EMPLOYEE', 'E26_015'),
('xuanloc', '123456', 'EMPLOYEE', 'E26_016');

INSERT INTO MonthlyHours VALUES
-- FIXED (180h)
('E26_001', 10, 2026, 180),
('E26_002', 10, 2026, 180),
('E26_003', 10, 2026, 180),
('E26_004', 10, 2026, 180),
('E26_005', 10, 2026, 180),

-- HOURLY (Part-time random)
('E26_006', 10, 2026, 115),
('E26_007', 10, 2026, 92),
('E26_008', 10, 2026, 128),
('E26_009', 10, 2026, 85),
('E26_010', 10, 2026, 104),
('E26_011', 10, 2026, 96),
('E26_012', 10, 2026, 120),
('E26_013', 10, 2026, 88),

-- FIXED (180h)
('E26_014', 10, 2026, 180),
('E26_015', 10, 2026, 180),
('E26_016', 10, 2026, 180);
GO
