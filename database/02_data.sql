USE QuanLyNhanSu;
GO

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
INSERT INTO Employee (employee_id, full_name, gender, birth_day, phone_number, email, citizen_id, address, position_id, hire_date, status)
SELECT v.employee_id, v.full_name, v.gender, v.birth_day, v.phone_number, v.email, v.citizen_id, v.address, p.position_id, v.hire_date, 'ACTIVE'
FROM (VALUES
 ('E26_001', N'Nguyễn Hải Lý',       'MALE',   '1990-03-15', '0901000001', 'ql.nguyen@highland.vn',     '079190000001', N'12 Lê Lợi, Quận 1, TP.HCM',               N'Quản lý',               '2026-01-01'),
 ('E26_002', N'Nguyễn Văn Trí',      'MALE',   '1993-07-22', '0901000002', 'tri.nguyen@highland.vn',    '079193000002', N'45 Nguyễn Trãi, Quận 5, TP.HCM',          N'Master Barista Leader', '2026-01-01'),
 ('E26_003', N'Trần Thị Hoàng Anh',  'FEMALE', '1996-11-05', '0901000003', 'anh.tran@highland.vn',      '079196000003', N'78 Cách Mạng Tháng 8, Quận 3, TP.HCM',     N'Master Barista',        '2026-01-01'),
 ('E26_004', N'Lê Minh Cường',       'MALE',   '1995-02-18', '0901000004', 'cuong.le@highland.vn',      '079195000004', N'23 Võ Văn Tần, Quận 3, TP.HCM',           N'Master Barista',        '2026-01-01'),
 ('E26_005', N'Phạm Thị Mai Dung',   'FEMALE', '1998-09-30', '0901000005', 'dung.pham@highland.vn',     '079198000005', N'9 Trần Hưng Đạo, Quận 1, TP.HCM',         N'Thu ngân',              '2026-01-01'),
 ('E26_006', N'Hoàng Quốc Giang',    'MALE',   '1997-05-12', '0901000006', 'giang.hoang@highland.vn',   '079197000006', N'56 Hai Bà Trưng, Quận 1, TP.HCM',         N'Thu ngân',              '2026-01-01'),
 ('E26_007', N'Đỗ Hải Huy',          'MALE',   '2003-01-25', '0901000007', 'huy.do@highland.vn',        '079203000007', N'101 Lý Thái Tổ, Quận 10, TP.HCM',         N'Barista',               '2026-02-01'),
 ('E26_008', N'Vũ Thị Kim Khoa',     'FEMALE', '2002-08-14', '0901000008', 'khoa.vu@highland.vn',       '079202000008', N'34 Nguyễn Đình Chiểu, Quận 3, TP.HCM',     N'Barista',               '2026-02-01'),
 ('E26_009', N'Bùi Tấn Long',        'MALE',   '2004-04-03', '0901000009', 'long.bui@highland.vn',      '079204000009', N'67 Phan Xích Long, Phú Nhuận, TP.HCM',    N'Barista',               '2026-03-01'),
 ('E26_010', N'Đặng Thanh Nam',      'MALE',   '2003-12-20', '0901000010', 'nam.dang@highland.vn',      '079203000010', N'88 Cộng Hòa, Tân Bình, TP.HCM',           N'Server',                '2026-03-01'),
 ('E26_011', N'Ngô Thị Bích Phượng', 'FEMALE', '2004-06-09', '0901000011', 'phuong.ngo@highland.vn',    '079204000011', N'15 Âu Cơ, Tân Phú, TP.HCM',               N'Server',                '2026-03-15'),
 ('E26_012', N'Dương Minh Quân',     'MALE',   '2002-10-28', '0901000012', 'quan.duong@highland.vn',    '079202000012', N'29 Quang Trung, Gò Vấp, TP.HCM',          N'Server',                '2026-04-01'),
 ('E26_013', N'Lý Thị Thu Sơn',      'FEMALE', '1988-03-07', '0901000013', 'son.ly@highland.vn',        '079188000013', N'5 Nguyễn Văn Cừ, Quận 5, TP.HCM',         N'Security',              '2026-01-01'),
 ('E26_014', N'Hồ Ngọc Tuấn',        'MALE',   '1985-09-16', '0901000014', 'tuan.ho@highland.vn',       '079185000014', N'72 Lê Văn Sỹ, Quận 3, TP.HCM',            N'Security',              '2026-01-01'),
 ('E26_015', N'Phan Văn Uy',         'MALE',   '1987-12-01', '0901000015', 'uy.phan@highland.vn',       '079187000015', N'41 Bà Hạt, Quận 10, TP.HCM',              N'Security',              '2026-01-01'),
 ('E26_016', N'Nguyễn Tấn Việt',     'MALE',   '2003-07-19', '0901000016', 'viet.nguyen@highland.vn',   '079203000016', N'18 Nguyễn Thị Minh Khai, Quận 3, TP.HCM', N'Barista',               '2026-01-01')
) AS v(employee_id, full_name, gender, birth_day, phone_number, email, citizen_id, address, position_name, hire_date)
JOIN Position p ON p.position_name = v.position_name;

-- PersonalInfo
INSERT INTO PersonalInfo (employee_id, hometown, ethnicity, education_level, school, permanent_address) VALUES
('E26_001', N'TP. Hồ Chí Minh',  N'Kinh', N'Đại học',   N'Đại học Kinh tế TP.HCM (UEH)',               N'12 Lê Lợi, Phường Bến Nghé, Quận 1, TP.HCM'),
('E26_002', N'Tiền Giang',      N'Kinh', N'Cao đẳng',  N'Cao đẳng Nghề Du lịch Sài Gòn',              N'Ấp Bình Hòa, Xã Mỹ Phong, TP. Mỹ Tho, Tiền Giang'),
('E26_003', N'Long An',         N'Kinh', N'Đại học',   N'Đại học Công nghiệp TP.HCM (IUH)',           N'Khu phố 3, Phường 2, TP. Tân An, Long An'),
('E26_004', N'Đồng Nai',        N'Kinh', N'Cao đẳng',  N'Cao đẳng Kinh tế Đối ngoại',                 N'Khu phố 5, Phường Tân Hiệp, TP. Biên Hòa, Đồng Nai'),
('E26_005', N'Bến Tre',         N'Kinh', N'Đại học',   N'Đại học Tài chính - Marketing (UFM)',        N'Ấp Phú Thạnh, Xã Phú Hưng, TP. Bến Tre, Bến Tre'),
('E26_006', N'TP. Hồ Chí Minh',  N'Kinh', N'Cao đẳng',  N'Cao đẳng Kinh tế TP.HCM',                    N'56 Hai Bà Trưng, Phường Bến Nghé, Quận 1, TP.HCM'),
('E26_007', N'Bình Định',       N'Kinh', N'Đại học',   N'Đại học Bách khoa - ĐHQG TP.HCM',            N'Khu vực 4, Phường Nhơn Bình, TP. Quy Nhơn, Bình Định'),
('E26_008', N'Vĩnh Long',       N'Kinh', N'Đại học',   N'Đại học Khoa học Xã hội và Nhân văn TP.HCM', N'Ấp Tân Thạnh, Xã Tân Ngãi, TP. Vĩnh Long, Vĩnh Long'),
('E26_009', N'Khánh Hòa',       N'Kinh', N'Đại học',   N'Đại học Ngân hàng TP.HCM (HUB)',             N'Thôn Vĩnh Phương, Xã Vĩnh Phương, TP. Nha Trang, Khánh Hòa'),
('E26_010', N'Quảng Nam',       N'Kinh', N'Đại học',   N'Đại học Mở TP.HCM (OU)',                     N'Thôn 2, Xã Tam Phú, TP. Tam Kỳ, Quảng Nam'),
('E26_011', N'Tây Ninh',        N'Kinh', N'Đại học',   N'Đại học Công thương TP.HCM (HUIT)',          N'Ấp Thanh Bình, Xã Thanh Điền, Huyện Châu Thành, Tây Ninh'),
('E26_012', N'Đắk Lắk',         N'Ê-đê', N'Cao đẳng',  N'Cao đẳng Công thương TP.HCM',                N'Buôn Ky, Phường Tân Lập, TP. Buôn Ma Thuột, Đắk Lắk'),
('E26_013', N'Cần Thơ',         N'Hoa',  N'THPT',      N'THPT Nguyễn Trãi',                           N'Khu vực 2, Phường An Khánh, Quận Ninh Kiều, Cần Thơ'),
('E26_014', N'An Giang',        N'Kinh', N'THPT',      N'THPT Lê Quý Đôn',                            N'Khóm Bình Khánh 3, Phường Mỹ Bình, TP. Long Xuyên, An Giang'),
('E26_015', N'Bà Rịa - Vũng Tàu',N'Kinh', N'Trung cấp', N'Trung cấp Kỹ thuật và Nghiệp vụ',            N'Khu phố 6, Phường Thắng Nhì, TP. Vũng Tàu, Bà Rịa - Vũng Tàu'),
('E26_016', N'Bình Thuận',      N'Kinh', N'Đại học',   N'Đại học Kinh tế - Luật - ĐHQG TP.HCM (UEL)',  N'Khu phố 1, Phường Phú Tài, TP. Phan Thiết, Bình Thuận');

-- BankAccount
INSERT INTO BankAccount (employee_id, bank_name, bank_account) VALUES
('E26_001', N'Vietcombank', '0071000918273'),
('E26_002', N'Techcombank', '19036482910018'),
('E26_003', N'MB Bank',     '0982736451209'),
('E26_004', N'ACB',         '248192037'),
('E26_005', N'VietinBank',  '105872394812'),
('E26_006', N'TPBank',      '03918274651'),
('E26_007', N'Vietcombank', '1028394756'),
('E26_008', N'BIDV',        '31410008291746'),
('E26_009', N'Sacombank',   '060291837465'),
('E26_010', N'VPBank',      '782910465'),
('E26_011', N'Techcombank', '19038192746022'),
('E26_012', N'MB Bank',     '0369182746501'),
('E26_013', N'ACB',         '192837465'),
('E26_014', N'Agribank',    '6220205819273'),
('E26_015', N'BIDV',        '31010004928173'),
('E26_016', N'Vietcombank', '0511000482916');

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
SELECT e.employee_id, m.mth, 2026, m.ps, m.pe, p.salary_type, p.salary_rate,
       h.hours, b.base_salary, a.allowance, a.bonus, b.base_salary + a.allowance + a.bonus
FROM Employee e
JOIN Position p ON p.position_id = e.position_id
CROSS JOIN (VALUES (9,  CAST('2026-09-01' AS DATE), CAST('2026-09-30' AS DATE)),
                   (10, CAST('2026-10-01' AS DATE), CAST('2026-10-31' AS DATE))) AS m(mth, ps, pe)
JOIN MonthlyHours h ON h.employee_id = e.employee_id AND h.month = m.mth AND h.year = 2026
CROSS APPLY (SELECT CAST(CASE WHEN p.salary_type = 'FIXED' THEN p.salary_rate
                              ELSE p.salary_rate * h.hours END AS DECIMAL(14,0)) AS base_salary) b
CROSS APPLY (SELECT CAST(CASE p.position_name
                              WHEN N'Quản lý'               THEN 1000000
                              WHEN N'Master Barista Leader' THEN 500000
                              WHEN N'Thu ngân'              THEN 300000
                              WHEN N'Security'              THEN 300000
                              ELSE 0 END AS DECIMAL(12,0)) AS allowance,
                    CAST(CASE WHEN p.salary_type = 'FIXED' THEN 200000 ELSE 100000 END AS DECIMAL(12,0)) AS bonus) a;
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
       CASE WHEN p.salary_type = 'FIXED' THEN s.end_time
            WHEN y.is_morning = 1 THEN CASE x.n % 3 WHEN 0 THEN CAST('12:00' AS TIME)
                                                    WHEN 1 THEN CAST('13:00' AS TIME)
                                                    ELSE        CAST('15:00' AS TIME) END
            ELSE                       CASE x.n % 3 WHEN 0 THEN CAST('20:00' AS TIME)
                                                    WHEN 1 THEN CAST('21:00' AS TIME)
                                                    ELSE        CAST('23:00' AS TIME) END
       END,
       CASE WHEN y.wk = 0 OR p.salary_type = 'FIXED' OR y.wd <= 3 THEN 'CONFIRMED' ELSE 'REGISTERED' END
FROM Employee e
JOIN Position p ON p.position_id = e.position_id
CROSS JOIN d
CROSS APPLY (SELECT CAST(RIGHT(e.employee_id, 3) AS INT) AS n,
                    DATEDIFF(DAY, '2026-11-09', d.work_date) AS dn) x
CROSS APPLY (SELECT x.dn % 7 AS wd, x.dn / 7 AS wk,
                    CASE WHEN p.salary_type = 'FIXED' THEN x.n % 2
                         ELSE (x.n + x.dn % 7) % 2 END AS is_morning) y
CROSS APPLY (SELECT CASE WHEN y.is_morning = 1 THEN N'Ca sáng' ELSE N'Ca chiều' END AS shift_name) z
JOIN ShiftTime s ON s.shift_name = z.shift_name
WHERE e.status = 'ACTIVE'
  AND (p.salary_type = 'HOURLY' OR y.wd <= 5);

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
INSERT INTO LeaveRequest (employee_id, request_type, target_date, shift_name, new_shift_name, new_start_time, new_end_time, reason, status, created_at, reviewed_by, reviewed_at) 
VALUES
('E26_007', 'LEAVE', '2026-11-17', N'Ca chiều', NULL, NULL, NULL, N'Có lịch thi học phần', 'PENDING', '2026-11-12 20:15', NULL, NULL),
('E26_003', 'LEAVE', '2026-11-20', N'Ca sáng', NULL, NULL, NULL, N'Việc gia đình', 'PENDING', '2026-11-13 08:30', NULL, NULL),
('E26_011', 'SHIFT_CHANGE', '2026-11-19', N'Ca chiều', N'Ca sáng', '06:00', '12:00', N'Xin đổi từ ca chiều sang ca sáng do bận học buổi chiều', 'PENDING', '2026-11-13 21:00', NULL,      NULL),
('E26_009', 'LEAVE', '2026-11-10', N'Ca chiều', NULL, NULL, NULL, N'Bị ốm đột xuất', 'APPROVED', '2026-11-09 22:00', 'E26_001', '2026-11-10 07:30'),
('E26_005', 'SHIFT_CHANGE', '2026-11-14', N'Ca sáng',  N'Ca chiều', '15:00', '23:00', N'Đổi ca với đồng nghiệp E26_006', 'APPROVED', '2026-11-11 10:00', 'E26_001', '2026-11-11 14:00'),
('E26_012', 'LEAVE', '2026-11-15', NULL, NULL, NULL, NULL, N'Đi chơi với bạn', 'REJECTED', '2026-11-12 09:00', 'E26_001', '2026-11-12 16:00');

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