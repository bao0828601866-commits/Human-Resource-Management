package com.hr.ui;

import java.text.NumberFormat;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Dữ liệu giả phản chiếu database.sql (ShiftTime, Position, Employee, WorkSchedule, Attendance...).
 * Khi nối DB thật, thay các hàm ở đây bằng DAO; các màn hình không phải sửa nhiều.
 */
final class DemoData {
    private DemoData() {}

    // =====================================================================
    // Hằng số nghiệp vụ
    // =====================================================================
    static final String[] DAYS = {"Thứ 2", "Thứ 3", "Thứ 4", "Thứ 5", "Thứ 6", "Thứ 7", "CN"};
    /** Cho phép vào trễ tối đa bao nhiêu phút so với giờ đã đăng ký. */
    static final int LATE_GRACE_MIN = 5;
    /** Part-time đăng ký tối thiểu mỗi lần (phút). */
    static final int MIN_PART_MINUTES = 240;
    /** Số người tối đa làm cùng một thời điểm. */
    static final int MAX_CONCURRENT = 10;

    // =====================================================================
    // Ca (bảng ShiftTime)
    // =====================================================================
    static final class Shift {
        final String name;
        final LocalTime start, end;

        Shift(String name, LocalTime start, LocalTime end) {
            this.name = name;
            this.start = start;
            this.end = end;
        }
    }

    static final Shift[] SHIFTS = {
            new Shift("Ca sáng", LocalTime.of(6, 0), LocalTime.of(15, 0)),
            new Shift("Ca chiều", LocalTime.of(15, 0), LocalTime.of(23, 0))
    };

    static Shift shift(String name) {
        for (Shift s : SHIFTS) if (s.name.equals(name)) return s;
        return null;
    }

    static String[] shiftNames() {
        String[] r = new String[SHIFTS.length];
        for (int i = 0; i < r.length; i++) r[i] = SHIFTS[i].name;
        return r;
    }

    /** Các mốc giờ (bước 30 phút) trong khung của ca, gồm cả hai đầu. */
    static String[] times(Shift s) {
        List<String> l = new ArrayList<>();
        int end = s.end.toSecondOfDay() / 60;
        for (int m = s.start.toSecondOfDay() / 60; m <= end; m += 30)
            l.add(hm(LocalTime.ofSecondOfDay(m * 60L)));
        return l.toArray(new String[0]);
    }

    /**
     * Kiểm tra khoảng giờ đăng ký. Trả về thông báo lỗi, hoặc null nếu hợp lệ.
     * Full-time: đủ khung ca. Part-time: nằm trong khung ca và tối thiểu MIN_PART_MINUTES.
     */
    static String checkRange(boolean fixed, String shiftName, LocalTime from, LocalTime to) {
        Shift s = shift(shiftName);
        if (s == null) return "Ca không hợp lệ.";
        if (!to.isAfter(from)) return "Giờ kết thúc phải sau giờ bắt đầu.";
        if (from.isBefore(s.start) || to.isAfter(s.end))
            return shiftName + " chỉ từ " + hm(s.start) + " đến " + hm(s.end) + ".";
        if (fixed) {
            if (!from.equals(s.start) || !to.equals(s.end))
                return "Full-time phải đăng ký đủ ca (" + range(s.start, s.end) + ").";
        } else if (Duration.between(from, to).toMinutes() < MIN_PART_MINUTES) {
            return "Part-time đăng ký tối thiểu " + (MIN_PART_MINUTES / 60) + " giờ mỗi lần.";
        }
        return null;
    }

    // =====================================================================
    // Chức vụ (bảng Position)
    // =====================================================================
    static final class Position {
        final String name, type;
        final long rate, allowance;

        Position(String name, String type, long rate, long allowance) {
            this.name = name;
            this.type = type;
            this.rate = rate;
            this.allowance = allowance;
        }

        boolean fixed() { return "FIXED".equals(type); }
    }

    static final Position[] POSITIONS = {
            new Position("Quản lý", "FIXED", 10_000_000, 1_000_000),
            new Position("Master Barista Leader", "FIXED", 8_000_000, 500_000),
            new Position("Master Barista", "FIXED", 7_000_000, 0),
            new Position("Thu ngân", "FIXED", 6_500_000, 300_000),
            new Position("Security", "FIXED", 6_000_000, 300_000),
            new Position("Barista", "HOURLY", 27_000, 0),
            new Position("Server", "HOURLY", 27_000, 0)
    };

    static Position position(String name) {
        for (Position p : POSITIONS) if (p.name.equals(name)) return p;
        return POSITIONS[5];
    }

    static String[] positionNames() {
        String[] r = new String[POSITIONS.length];
        for (int i = 0; i < r.length; i++) r[i] = POSITIONS[i].name;
        return r;
    }

    // =====================================================================
    // Nhân viên (bảng Employee)
    // =====================================================================
    static final String[] GENDER_CODES = {"MALE", "FEMALE", "OTHER"};
    static final String[] GENDER_VI = {"Nam", "Nữ", "Khác"};

    static String genderVi(String code) {
        for (int i = 0; i < GENDER_CODES.length; i++) if (GENDER_CODES[i].equals(code)) return GENDER_VI[i];
        return "Khác";
    }

    static final class Emp {
        final String id;
        String name, gender, position, phone, email, address;
        boolean active = true;

        Emp(String id, String name, String gender, String position, String phone, String email, String address) {
            this.id = id;
            this.name = name;
            this.gender = gender;
            this.position = position;
            this.phone = phone;
            this.email = email;
            this.address = address;
        }

        int num() {
            try {
                return Integer.parseInt(id.substring(id.length() - 3));
            } catch (RuntimeException ex) {
                return 0;
            }
        }

        Position pos() { return position(position); }

        /** FIXED = full-time, HOURLY = part-time. */
        boolean fixed() { return pos().fixed(); }
    }

    static List<Emp> employees() {
        List<Emp> l = new ArrayList<>();
        l.add(new Emp("E26_001", "Nguyễn Quản Lý", "MALE", "Quản lý", "0901000001", "ql.nguyen@highland.vn", "12 Lê Lợi, Quận 1, TP.HCM"));
        l.add(new Emp("E26_002", "Nguyễn Văn Trí", "MALE", "Master Barista Leader", "0901000002", "tri.nguyen@highland.vn", "45 Nguyễn Trãi, Quận 5, TP.HCM"));
        l.add(new Emp("E26_003", "Trần Thị Hoàng Anh", "FEMALE", "Master Barista", "0901000003", "anh.tran@highland.vn", "78 Cách Mạng Tháng 8, Quận 3, TP.HCM"));
        l.add(new Emp("E26_004", "Lê Minh Cường", "MALE", "Master Barista", "0901000004", "cuong.le@highland.vn", "23 Võ Văn Tần, Quận 3, TP.HCM"));
        l.add(new Emp("E26_005", "Phạm Thị Mai Dung", "FEMALE", "Thu ngân", "0901000005", "dung.pham@highland.vn", "9 Trần Hưng Đạo, Quận 1, TP.HCM"));
        l.add(new Emp("E26_006", "Hoàng Quốc Giang", "MALE", "Thu ngân", "0901000006", "giang.hoang@highland.vn", "56 Hai Bà Trưng, Quận 1, TP.HCM"));
        l.add(new Emp("E26_007", "Đỗ Hải Huy", "MALE", "Barista", "0901000007", "huy.do@highland.vn", "101 Lý Thái Tổ, Quận 10, TP.HCM"));
        l.add(new Emp("E26_008", "Vũ Thị Kim Khoa", "FEMALE", "Barista", "0901000008", "khoa.vu@highland.vn", "34 Nguyễn Đình Chiểu, Quận 3, TP.HCM"));
        l.add(new Emp("E26_009", "Bùi Tấn Long", "MALE", "Barista", "0901000009", "long.bui@highland.vn", "67 Phan Xích Long, Phú Nhuận, TP.HCM"));
        l.add(new Emp("E26_010", "Đặng Thanh Nam", "MALE", "Server", "0901000010", "nam.dang@highland.vn", "88 Cộng Hòa, Tân Bình, TP.HCM"));
        l.add(new Emp("E26_011", "Ngô Thị Bích Phượng", "FEMALE", "Server", "0901000011", "phuong.ngo@highland.vn", "15 Âu Cơ, Tân Phú, TP.HCM"));
        l.add(new Emp("E26_012", "Dương Minh Quân", "MALE", "Server", "0901000012", "quan.duong@highland.vn", "29 Quang Trung, Gò Vấp, TP.HCM"));
        l.add(new Emp("E26_013", "Lý Thị Thu Sơn", "FEMALE", "Security", "0901000013", "son.ly@highland.vn", "5 Nguyễn Văn Cừ, Quận 5, TP.HCM"));
        l.add(new Emp("E26_014", "Hồ Ngọc Tuấn", "MALE", "Security", "0901000014", "tuan.ho@highland.vn", "72 Lê Văn Sỹ, Quận 3, TP.HCM"));
        l.add(new Emp("E26_015", "Phan Văn Uy", "MALE", "Security", "0901000015", "uy.phan@highland.vn", "41 Bà Hạt, Quận 10, TP.HCM"));
        Emp resigned = new Emp("E26_016", "Nguyễn Tấn Việt", "MALE", "Barista", "0901000016", "viet.nguyen@highland.vn", "18 Nguyễn Thị Minh Khai, Quận 3, TP.HCM");
        resigned.active = false;
        l.add(resigned);
        return l;
    }

    private static final List<Emp> PROFILES = employees();

    /** Tìm nhân viên theo mã; không thấy thì tạo hồ sơ tạm (part-time) để màn hình vẫn chạy. */
    static Emp findOrDefault(String id) {
        for (Emp e : PROFILES) if (e.id.equalsIgnoreCase(id)) return e;
        Emp e = new Emp(id, id, "OTHER", "Barista", "", "", "");
        PROFILES.add(e);
        return e;
    }

    // =====================================================================
    // Lịch làm (bảng WorkSchedule) - cùng quy tắc với dữ liệu mẫu trong SQL
    // =====================================================================
    static final class Sched {
        final String empId;
        final LocalDate date;
        String shift;
        LocalTime start, end;
        String status; // REGISTERED | CONFIRMED | CANCELLED

        Sched(String empId, LocalDate date, String shift, LocalTime start, LocalTime end, String status) {
            this.empId = empId;
            this.date = date;
            this.shift = shift;
            this.start = start;
            this.end = end;
            this.status = status;
        }

        boolean active() { return !"CANCELLED".equals(status); }

        boolean overlaps(LocalTime from, LocalTime to) { return start.isBefore(to) && from.isBefore(end); }
    }

    static String schedVi(String code) {
        switch (code) {
            case "CONFIRMED": return "Đã chốt";
            case "CANCELLED": return "Đã hủy";
            default: return "Chờ chốt";
        }
    }

    /**
     * Lịch một tuần của một nhân viên.
     * Full-time: T2-T7, mã lẻ = ca sáng, mã chẵn = ca chiều, đủ khung ca.
     * Part-time: cả tuần, xoay ca, giờ linh động theo (mã % 3).
     * pending = true: tuần sau, từ Thứ 6 trở đi của part-time mới "chờ chốt".
     */
    static List<Sched> scheduleFor(Emp e, LocalDate monday, boolean pending) {
        List<Sched> out = new ArrayList<>();
        int n = e.num();
        for (int d = 0; d < 7; d++) {
            if (e.fixed() && d > 5) continue;
            boolean morning = e.fixed() ? n % 2 == 1 : (n + d) % 2 == 1;
            Shift s = SHIFTS[morning ? 0 : 1];
            LocalTime end = s.end;
            if (!e.fixed()) {
                int m = n % 3;
                end = morning ? LocalTime.of(m == 0 ? 12 : m == 1 ? 13 : 15, 0)
                              : LocalTime.of(m == 0 ? 20 : m == 1 ? 21 : 23, 0);
            }
            String st = (!pending || e.fixed() || d <= 3) ? "CONFIRMED" : "REGISTERED";
            out.add(new Sched(e.id, monday.plusDays(d), s.name, s.start, end, st));
        }
        return out;
    }

    // =====================================================================
    // Chấm công (bảng Attendance)
    // =====================================================================
    static final String[] ATT_CODES = {"ON_TIME", "LATE", "EARLY_LEAVE", "ABSENT"};
    static final String[] ATT_VI = {"Đúng giờ", "Đi trễ", "Về sớm", "Vắng không phép"};

    static String attVi(String code) {
        for (int i = 0; i < ATT_CODES.length; i++) if (ATT_CODES[i].equals(code)) return ATT_VI[i];
        return code;
    }

    static final class Att {
        final String empId;
        final LocalDate date;
        final LocalTime start, end;   // giờ đã đăng ký
        LocalTime in, out;            // giờ chấm công thực tế (null = chưa có)
        String status;

        Att(String empId, LocalDate date, LocalTime start, LocalTime end) {
            this.empId = empId;
            this.date = date;
            this.start = start;
            this.end = end;
        }
    }

    /** Chấm công mẫu cho các ngày đã qua (tuần trước + tuần này đến hôm qua), cũ -> mới. */
    static List<Att> pastAttendance(Emp e, LocalDate today) {
        LocalDate mon = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        List<Sched> src = new ArrayList<>(scheduleFor(e, mon.minusWeeks(1), false));
        src.addAll(scheduleFor(e, mon, false));
        List<Att> out = new ArrayList<>();
        int n = e.num();
        for (Sched s : src) {
            if (!s.date.isBefore(today)) continue;
            int k = (n + s.date.getDayOfWeek().getValue()) % 7;
            Att a = new Att(e.id, s.date, s.start, s.end);
            a.in = s.start.plusMinutes(k == 0 ? 20 : -5);
            a.out = s.end.plusMinutes(k == 3 ? -30 : 5);
            a.status = k == 0 ? "LATE" : k == 3 ? "EARLY_LEAVE" : "ON_TIME";
            out.add(a);
        }
        // vài trường hợp để thử chức năng sửa chấm công
        if ("E26_009".equals(e.id) && out.size() > 1) {
            Att a = out.get(1);
            a.in = null;
            a.out = null;
            a.status = "ABSENT";
        }
        if ("E26_008".equals(e.id) && out.size() > 2) out.get(2).out = null;
        return out;
    }

    // =====================================================================
    // Lương (bảng MonthlyHours / Payroll)
    // =====================================================================
    private static final double[] OCT_HOURS = {198, 207, 198, 189, 198, 180, 92, 128, 85, 104, 96, 120, 207, 198, 189};

    /** Giờ làm tháng đã chốt: monthsAgo = 1 (tháng trước), 2 (hai tháng trước). */
    static double hoursPrev(Emp e, int monthsAgo) {
        int n = e.num();
        if (n == 16) return monthsAgo == 2 ? 64 : 0;
        if (n < 1 || n > OCT_HOURS.length) return 0;
        double oct = OCT_HOURS[n - 1];
        return monthsAgo == 1 ? oct : oct - (n % 4) * 9;
    }

    static long baseSalary(Emp e, double hours) {
        return e.fixed() ? e.pos().rate : Math.round(e.pos().rate * hours);
    }

    static long bonus(Emp e) { return e.fixed() ? 200_000 : 100_000; }

    private static final NumberFormat VN = NumberFormat.getIntegerInstance(Locale.forLanguageTag("vi"));

    static String money(long v) { return VN.format(v) + " đ"; }

    // =====================================================================
    // Doanh thu (bảng MonthlyRevenue), đơn vị: triệu đồng
    // =====================================================================
    static final double[] REVENUE_2025 = {520, 480, 545, 560, 590, 575, 610, 625, 598, 640, 655, 720};
    static final double[] REVENUE_2026 = {560, 515, 585, 600, 630, 615, 655, 670, 640, 690, 340, 0}; // T11 là lũy kế

    // =====================================================================
    // Thông báo (bảng Notification): {người nhận ("" = tất cả), tiêu đề, nội dung, thời gian}
    // =====================================================================
    static final String[][] NOTIS = {
            {"", "Lịch làm tuần tới", "Cửa hàng trưởng đã chốt lịch làm tuần tới. Vui lòng kiểm tra và đăng ký các ca còn lại trước 20h Chủ nhật.", "13/11/2026 18:00"},
            {"", "Họp cửa hàng thứ Hai", "Họp toàn bộ nhân viên lúc 05:45 sáng thứ Hai tại cửa hàng.", "13/11/2026 18:30"},
            {"E26_009", "Nhắc nhở vắng không phép", "Ngày 10/11 bạn vắng ca. Đơn xin nghỉ đã được duyệt, hãy báo trước sớm hơn cho lần sau.", "10/11/2026 08:00"},
            {"E26_008", "Bổ sung chấm công ra", "Ngày 12/11 bạn chưa chấm công ra ca. Vui lòng liên hệ cửa hàng trưởng để xác nhận.", "13/11/2026 09:00"},
            {"", "Bảng lương tháng 10 đã chốt", "Bảng lương kỳ 01/10/2026 - 31/10/2026 đã được cập nhật, vui lòng xem trong mục Bảng lương.", "05/11/2026 10:00"}
    };

    // =====================================================================
    // Tiện ích định dạng
    // =====================================================================
    static String hm(LocalTime t) { return String.format("%02d:%02d", t.getHour(), t.getMinute()); }

    static String range(LocalTime a, LocalTime b) { return hm(a) + "–" + hm(b); }

    /** 6h–12h, 13h30–15h: gọn để nhét vào ô lịch tuần. */
    static String shortRange(LocalTime a, LocalTime b) { return shortTime(a) + "–" + shortTime(b); }

    private static String shortTime(LocalTime t) {
        return t.getHour() + "h" + (t.getMinute() == 0 ? "" : String.format("%02d", t.getMinute()));
    }

    static String dayName(LocalDate d) { return DAYS[d.getDayOfWeek().getValue() - 1]; }

    /** "Nguyễn Văn An" -> "An". */
    static String lastWord(String s) {
        String t = s == null ? "" : s.trim();
        int i = t.lastIndexOf(' ');
        return i < 0 ? t : t.substring(i + 1);
    }

    static String[] prepend(String first, String[] rest) {
        String[] r = new String[rest.length + 1];
        r[0] = first;
        System.arraycopy(rest, 0, r, 1, rest.length);
        return r;
    }
}
