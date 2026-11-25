USE QuanLyNhanSu;

-- Check Account
SELECT TOP (1000) [employee_id]
      ,[password]
      ,[role]
      ,[is_active]
      ,[must_change_password]
  FROM [QuanLyNhanSu].[dbo].[Account]


  -- Check Employee
  SELECT TOP (1000) [employee_id]
      ,[full_name]
      ,[gender]
      ,[birth_day]
      ,[phone_number]
      ,[email]
      ,[citizen_id]
      ,[address]
      ,[position_id]
      ,[salary_type]
      ,[salary_rate]
      ,[hire_date]
      ,[status]
  FROM [QuanLyNhanSu].[dbo].[Employee]

  -- Check EmployeeBank
  SELECT TOP (1000) [employee_id]
      ,[bank_name]
      ,[bank_account]
  FROM [QuanLyNhanSu].[dbo].[EmployeeBank]


  -- Check EmployeeProfile
  SELECT TOP (1000) [employee_id]
      ,[hometown]
      ,[ethnicity]
      ,[education_level]
      ,[school]
      ,[temp_address]
  FROM [QuanLyNhanSu].[dbo].[EmployeeProfile]


  -- Check Attendance
  SELECT TOP (1000) [attendance_id]
      ,[schedule_id]
      ,[employee_id]
      ,[work_date]
      ,[check_in_time]
      ,[check_out_time]
      ,[status]
      ,[edit_reason]
      ,[edited_by]
      ,[edited_at]
  FROM [QuanLyNhanSu].[dbo].[Attendance]


  -- Check [AttendanceLog]
  SELECT TOP (1000) [log_id]
      ,[attendance_id]
      ,[edited_by]
      ,[edited_at]
      ,[old_in]
      ,[old_out]
      ,[old_status]
      ,[new_in]
      ,[new_out]
      ,[new_status]
      ,[reason]
  FROM [QuanLyNhanSu].[dbo].[AttendanceLog]


  -- Check [LeaveRequest]
  SELECT TOP (1000) [request_id]
      ,[employee_id]
      ,[request_type]
      ,[target_date]
      ,[shift_name]
      ,[new_shift_name]
      ,[new_start_time]
      ,[new_end_time]
      ,[reason]
      ,[status]
      ,[created_at]
      ,[reviewed_by]
      ,[reviewed_at]
  FROM [QuanLyNhanSu].[dbo].[LeaveRequest]

  -- Check [MonthlyHours]
  SELECT TOP (1000) [record_id]
      ,[employee_id]
      ,[month]
      ,[year]
      ,[hours]
      ,[work_days]
  FROM [QuanLyNhanSu].[dbo].[MonthlyHours]


  -- Check [MonthlyRevenue]
  SELECT TOP (1000) [revenue_id]
      ,[month]
      ,[year]
      ,[total_revenue]
  FROM [QuanLyNhanSu].[dbo].[MonthlyRevenue]

-- Check [Notification]
  SELECT TOP (1000) [notification_id]
      ,[sender_id]
      ,[receiver_id]
      ,[title]
      ,[content]
      ,[created_at]
  FROM [QuanLyNhanSu].[dbo].[Notification]


  -- Check [NotificationRead]
  SELECT TOP (1000) [notification_id]
      ,[employee_id]
      ,[read_at]
  FROM [QuanLyNhanSu].[dbo].[NotificationRead]


  -- Check [Payroll]
  SELECT TOP (1000) [employee_id]
      ,[month]
      ,[year]
      ,[period_start]
      ,[period_end]
      ,[salary_type]
      ,[salary_rate]
      ,[hours]
      ,[base_salary]
      ,[allowance]
      ,[revenue_bonus]
      ,[total_salary]
      ,[created_at]
  FROM [QuanLyNhanSu].[dbo].[Payroll]


  -- Check [Position]
  SELECT TOP (1000) [position_id]
      ,[position_name]
      ,[salary_type]
      ,[salary_rate]
      ,[allowance]
  FROM [QuanLyNhanSu].[dbo].[Position]



-- Check [ShiftTime]
SELECT TOP (1000) [shift_name]
      ,[start_time]
      ,[end_time]
  FROM [QuanLyNhanSu].[dbo].[ShiftTime]


-- Check [WorkSchedule]
  SELECT TOP (1000) [schedule_id]
      ,[employee_id]
      ,[work_date]
      ,[shift_name]
      ,[start_time]
      ,[end_time]
      ,[status]
  FROM [QuanLyNhanSu].[dbo].[WorkSchedule]

