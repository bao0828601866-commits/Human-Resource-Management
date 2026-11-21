package com.hr.ui.employee;

import com.hr.dao.AttendanceDAO;
import com.hr.dao.LeaveRequestDAO;
import com.hr.dao.NotificationReadDAO;
import com.hr.dao.SalaryDAO;
import com.hr.dao.ShiftTimeDAO;
import com.hr.dao.WorkScheduleDAO;
import com.hr.model.Account;
import com.hr.model.Attendance;
import com.hr.model.LeaveRequest;
import com.hr.model.SalaryRow;
import com.hr.model.ShiftTime;
import com.hr.model.WorkSchedule;
import com.hr.model.enums.AttendanceStatus;
import com.hr.model.enums.RequestType;
import com.hr.model.enums.SalaryType;
import com.hr.model.enums.ScheduleStatus;
import com.hr.ui.BaseFrame;
import com.hr.ui.UIStyle;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class EmployeeFrame extends BaseFrame {
    private static final DateTimeFormatter D = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DM = DateTimeFormatter.ofPattern("dd/MM");
    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter HM = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter HMS = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final String[] DAYS = {"Thứ 2", "Thứ 3", "Thứ 4", "Thứ 5", "Thứ 6", "Thứ 7", "CN"};
    private static final String[] REQ_TYPES = {"Xin nghỉ", "Đổi ca"};

    // Vào ca muộn hơn giờ đăng ký quá số phút này thì tính là đi trễ
    private static final int LATE_GRACE_MIN = 5;
    // Part-time đăng ký tối thiểu 4 giờ mỗi lần (khớp trigger trg_WorkSchedule_Validate)
    private static final int MIN_PART_MINUTES = 240;

    // ----- DAO -----
    private final WorkScheduleDAO workDao = new WorkScheduleDAO();
    private final AttendanceDAO attDao = new AttendanceDAO();
    private final LeaveRequestDAO leaveDao = new LeaveRequestDAO();
    private final SalaryDAO salaryDao = new SalaryDAO();
    private final ShiftTimeDAO shiftDao = new ShiftTimeDAO();
    private final NotificationReadDAO notiReadDao = new NotificationReadDAO();

    private final String empId;
    private final boolean fixed;
    private final LocalDate today = LocalDate.now();
    private final LocalDate monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    private final LocalDate nextMonday = monday.plusWeeks(1);

    // ----- Dữ liệu đọc từ DB -----
    private final List<ShiftTime> shifts = new ArrayList<>();
    /** Lịch của tôi: tuần này + tuần sau (WorkSchedule). */
    private final List<WorkSchedule> mine = new ArrayList<>();
    /** Các dòng đang hiện ở bảng đăng ký (tuần sau), cùng thứ tự với regModel. */
    private final List<WorkSchedule> regRows = new ArrayList<>();
    /** Chấm công 3 tháng gần nhất, cùng thứ tự với attModel. */
    private final List<Attendance> attRows = new ArrayList<>();
    private final List<LeaveRequest> reqRows = new ArrayList<>();

    // ----- Chấm công -----
    private final JLabel lblClock = new JLabel("--:--:--", SwingConstants.CENTER);
    private final JLabel lblState = new JLabel("Chưa vào ca", SwingConstants.CENTER);
    private final DefaultTableModel attModel =
            UIStyle.model("Ngày", "Ca đăng ký", "Giờ vào", "Giờ ra", "Số giờ", "Trạng thái");
    private final JButton btnIn = UIStyle.button("VÀO CA", this::clockIn);
    private final JButton btnOut = UIStyle.button("RA CA", this::clockOut);

    // ----- Ca kế tiếp + thống kê -----
    private final JLabel lblNextTitle = UIStyle.bold("");
    private final JLabel lblNextTime = UIStyle.muted("");
    private final JLabel lblNextNote = UIStyle.muted("");
    private final JLabel lblHours = statValue();
    private final JLabel lblLate = statValue();
    private final JLabel lblPay = statValue();

    // ----- Lịch tuần -----
    private final DefaultTableModel thisWeekModel = UIStyle.model(prepend("Ca", DAYS));
    private final DefaultTableModel nextWeekModel = UIStyle.model(prepend("Ca", DAYS));

    // ----- Đăng ký ca / đơn từ -----
    private final DefaultTableModel regModel = UIStyle.model("Ngày", "Ca", "Giờ làm", "Trạng thái");
    private final DefaultTableModel reqModel =
            UIStyle.model("Loại đơn", "Ngày", "Ca hiện tại", "Nội dung", "Lý do", "Trạng thái");
    private final JComboBox<String> cbRegDate = new JComboBox<>();
    private final JComboBox<String> cbRegShift = new JComboBox<>();
    private final JComboBox<String> cbRegFrom = new JComboBox<>();
    private final JComboBox<String> cbRegTo = new JComboBox<>();

    // ----- Lương -----
    private final DefaultTableModel payModel = UIStyle.model(
            "Kỳ lương", "Tổng giờ", "Công", "Loại lương", "Lương cơ bản", "Phụ cấp", "Thưởng DT", "Thực lĩnh", "Tình trạng");

    public EmployeeFrame(Account acc) {
        super(acc, "Nhân viên");
        this.empId = acc.getEmployeeId();
        this.fixed = profile.isFullTime();

        loadShifts();   // phải nạp trước khi dựng combobox ca

        // ----- Menu bên trái -----
        addMenu("overview", "Tổng quan");
        addGroup("Lịch làm", new String[]{"schedule", "approve"}, new String[]{"Lịch làm", "Duyệt lịch làm"});
        addMenu("noti", "Thông báo");
        addMenu("pay", "Bảng lương");

        // ----- Trang nội dung bên phải -----
        addPage("overview", buildOverview(), () -> { reloadAttendance(); refreshAll(); });
        addPage("schedule", buildSchedule(), this::reloadSchedules);
        addPage("approve", buildApprove(), () -> { reloadSchedules(); reloadRequests(); });
        addPage("noti", notificationPage("noti", notifications(), this::markAllNotificationsRead));
        addPage("pay", buildPay(), () -> { reloadAttendance(); refreshAll(); });

        // Không nạp dữ liệu ở đây: start("overview") sẽ chạy onShow của trang Tổng quan,
        // các trang còn lại tự nạp khi được mở.
        startClock();
        start("overview");
    }

    // =====================================================================
    // NẠP DỮ LIỆU TỪ DB
    // =====================================================================
    private void loadShifts() {
        try {
            shifts.clear();
            shifts.addAll(shiftDao.findAll());
        } catch (SQLException ex) {
            warn("Không tải được khung giờ ca: " + ex.getMessage());
        }
        cbRegShift.removeAllItems();
        for (ShiftTime s : shifts) cbRegShift.addItem(s.getShiftName());
    }

    private ShiftTime shift(String name) {
        for (ShiftTime s : shifts) if (s.getShiftName().equals(name)) return s;
        return null;
    }

    private String[] shiftNames() {
        String[] r = new String[shifts.size()];
        for (int i = 0; i < r.length; i++) r[i] = shifts.get(i).getShiftName();
        return r;
    }

    /** Lịch tuần này + tuần sau -> lưới lịch + bảng đăng ký. */
    private void reloadSchedules() {
        try {
            List<WorkSchedule> fresh = workDao.findByEmployee(empId, monday, nextMonday.plusDays(6));
            mine.clear();
            mine.addAll(fresh);
        } catch (SQLException ex) {
            warn("Không tải được lịch làm: " + ex.getMessage());
            return;
        }
        fillWeek(thisWeekModel, monday);
        fillWeek(nextWeekModel, nextMonday);

        regRows.clear();
        regModel.setRowCount(0);
        List<WorkSchedule> nx = new ArrayList<>();
        for (WorkSchedule s : mine) if (!s.getWorkDate().isBefore(nextMonday)) nx.add(s);
        nx.sort(Comparator.comparing(WorkSchedule::getWorkDate).thenComparing(WorkSchedule::getStartTime));
        for (WorkSchedule s : nx) {
            regRows.add(s);
            regModel.addRow(new Object[]{dayLabel(s.getWorkDate()), s.getShiftName(),
                    range(s.getStartTime(), s.getEndTime()), schedVi(s.getStatus())});
        }
        refreshNextShift();
    }

    /** Chấm công từ đầu tháng của 2 tháng trước đến hôm nay (đủ cho bảng lương 3 tháng). */
    private void reloadAttendance() {
        LocalDate now = LocalDate.now();
        try {
            List<Attendance> fresh = attDao.findByEmployee(empId, YearMonth.from(now).minusMonths(2).atDay(1), now);
            attRows.clear();
            attRows.addAll(fresh);
        } catch (SQLException ex) {
            warn("Không tải được chấm công: " + ex.getMessage());
            return;
        }
        attModel.setRowCount(0);
        for (Attendance a : attRows) {
            boolean full = a.getCheckInTime() != null && a.getCheckOutTime() != null;
            double h = full ? Duration.between(a.getCheckInTime(), a.getCheckOutTime()).toMinutes() / 60.0 : 0;
            attModel.addRow(new Object[]{a.getWorkDate().format(D),
                    range(a.getScheduledStart(), a.getScheduledEnd()),
                    a.getCheckInTime() == null ? "--" : hm(a.getCheckInTime().toLocalTime()),
                    a.getCheckOutTime() == null ? "--" : hm(a.getCheckOutTime().toLocalTime()),
                    full ? String.format(Locale.ROOT, "%.1f", h) : "--",
                    attVi(a)});
        }
        updateClockState();
    }

    private void reloadRequests() {
        try {
            List<LeaveRequest> fresh = leaveDao.findByEmployee(empId);
            reqRows.clear();
            reqRows.addAll(fresh);
        } catch (SQLException ex) {
            warn("Không tải được đơn nghỉ / đổi ca: " + ex.getMessage());
            return;
        }
        reqModel.setRowCount(0);
        for (LeaveRequest r : reqRows) {
            boolean change = r.getRequestType() == RequestType.SHIFT_CHANGE;
            String content = r.getShiftName() == null ? "Nghỉ cả ngày" : "Nghỉ " + r.getShiftName().toLowerCase();
            if (change) {
                content = "→ " + r.getNewShiftName() + " " + range(r.getNewStartTime(), r.getNewEndTime());
            }
            reqModel.addRow(new Object[]{change ? REQ_TYPES[1] : REQ_TYPES[0], r.getTargetDate().format(D),
                    r.getShiftName() == null ? "Cả ngày" : r.getShiftName(), content, r.getReason(),
                    reqVi(r.getStatus().name())});
        }
    }

    // =====================================================================
    // TỔNG QUAN: chấm công vào/ra ca + ca kế tiếp + thống kê tháng + chấm công gần đây
    // =====================================================================
    private JComponent buildOverview() {
        lblClock.setFont(new Font("Segoe UI", Font.BOLD, 34));
        lblState.setFont(UIStyle.PLAIN);
        lblState.setForeground(UIStyle.MUTED);
        btnOut.setEnabled(false);

        JPanel buttons = UIStyle.panel(new FlowLayout(FlowLayout.CENTER, 16, 0));
        buttons.add(btnIn);
        buttons.add(btnOut);
        JPanel clockBody = UIStyle.panel(new GridLayout(3, 1, 0, 4));
        clockBody.add(lblClock);
        clockBody.add(lblState);
        clockBody.add(buttons);

        JPanel nextBody = UIStyle.panel(new GridLayout(3, 1, 0, 4));
        nextBody.add(lblNextTitle);
        nextBody.add(lblNextTime);
        nextBody.add(lblNextNote);

        JPanel top = UIStyle.panel(new GridLayout(1, 2, 12, 0));
        top.add(UIStyle.card("Chấm công", clockBody));
        top.add(UIStyle.card("Ca làm kế tiếp", nextBody));
        top.setPreferredSize(new Dimension(0, 190));

        JPanel stats = UIStyle.panel(new GridLayout(1, 3, 12, 0));
        stats.add(UIStyle.card("Tổng giờ làm tháng này", lblHours));
        stats.add(UIStyle.card("Số lần đi trễ", lblLate));
        stats.add(UIStyle.card("Lương tạm tính (cơ bản + phụ cấp)", lblPay));
        stats.setPreferredSize(new Dimension(0, 90));

        JPanel middle = UIStyle.panel(new BorderLayout(0, 12));
        middle.add(stats, BorderLayout.NORTH);
        middle.add(UIStyle.card("Chấm công gần đây", UIStyle.plainScroll(UIStyle.table(attModel))),
                BorderLayout.CENTER);

        JPanel page = UIStyle.panel(new BorderLayout(0, 12));
        page.add(top, BorderLayout.NORTH);
        page.add(middle, BorderLayout.CENTER);
        return page;
    }

    private static JLabel statValue() {
        JLabel v = new JLabel("-");
        v.setFont(new Font("Segoe UI", Font.BOLD, 22));
        v.setForeground(UIStyle.PRIMARY);
        return v;
    }

    private void startClock() {
        javax.swing.Timer t = new javax.swing.Timer(1000, e -> lblClock.setText(LocalTime.now().format(HMS)));
        t.setInitialDelay(0);
        t.start();
    }

    /** Lượt chấm công hôm nay đã vào ca nhưng chưa ra ca; null nếu không có. */
    private Attendance openAttendance() {
        LocalDate now = LocalDate.now();
        for (Attendance a : attRows) {
            if (a.getWorkDate().equals(now) && a.getCheckInTime() != null && a.getCheckOutTime() == null) return a;
        }
        return null;
    }

    /** Bật/tắt nút VÀO CA / RA CA và dòng trạng thái theo dữ liệu chấm công trong DB. */
    private void updateClockState() {
        Attendance open = openAttendance();
        btnIn.setEnabled(open == null);
        btnOut.setEnabled(open != null);
        if (open != null) {
            lblState.setText("Đang trong ca từ " + hm(open.getCheckInTime().toLocalTime()) + " (đăng ký "
                    + range(open.getScheduledStart(), open.getScheduledEnd()) + ")");
            return;
        }
        LocalDate now = LocalDate.now();
        Attendance last = null;
        for (Attendance a : attRows) {
            if (a.getWorkDate().equals(now) && a.getCheckOutTime() != null
                    && (last == null || a.getCheckOutTime().isAfter(last.getCheckOutTime()))) last = a;
        }
        lblState.setText(last == null ? "Chưa vào ca" : "Đã ra ca lúc " + hm(last.getCheckOutTime().toLocalTime()));
    }

    /**
     * Vào ca: chỉ khi hôm nay có ca ĐÃ CHỐT chưa chấm công; trễ nếu vào muộn hơn giờ đăng ký
     * quá LATE_GRACE_MIN phút. Ghi INSERT Attendance.
     */
    private void clockIn() {
        LocalDate now = LocalDate.now();
        try {
            if (openAttendance() != null) {
                warn("Bạn đang trong ca, hãy RA CA trước.");
                return;
            }
            List<WorkSchedule> confirmed = workDao.findConfirmedOn(empId, now);
            if (confirmed.isEmpty()) {
                warn("Hôm nay bạn không có ca đã chốt nên không thể chấm công.");
                return;
            }
            WorkSchedule target = null;
            for (WorkSchedule s : confirmed) {
                if (attDao.findByScheduleId(s.getScheduleId()) == null) { target = s; break; }
            }
            if (target == null) {
                warn("Hôm nay bạn đã chấm công hết các ca rồi.");
                return;
            }
            attDao.clockIn(target, LocalDateTime.now().withNano(0), LATE_GRACE_MIN);
        } catch (IllegalStateException | SQLException ex) {
            warn("Không chấm công vào được: " + ex.getMessage());
            return;
        }
        reloadAttendance();
        refreshAll();
    }

    /** Ra ca: UPDATE Attendance. Sớm hơn giờ đã đăng ký -> "Về sớm" (đã "Đi trễ" thì giữ "Đi trễ"). */
    private void clockOut() {
        Attendance open = openAttendance();
        if (open == null) {
            warn("Bạn chưa vào ca nên không thể ra ca.");
            reloadAttendance();
            return;
        }
        try {
            if (!attDao.clockOut(open.getScheduleId(), LocalDateTime.now().withNano(0))) {
                warn("Ca này đã được chấm công ra rồi.");
            }
        } catch (SQLException ex) {
            warn("Không chấm công ra được: " + ex.getMessage());
            return;
        }
        reloadAttendance();
        refreshAll();
    }

    private void refreshNextShift() {
        try {
            WorkSchedule next = workDao.findNextForEmployee(empId, LocalDateTime.now());
            if (next == null) {
                lblNextTitle.setText("Chưa có ca tiếp theo");
                lblNextTime.setText("");
                lblNextNote.setText("");
                return;
            }
            lblNextTitle.setText(dayName(next.getWorkDate()) + " " + next.getWorkDate().format(DM)
                    + " · " + next.getShiftName());
            lblNextTime.setText(range(next.getStartTime(), next.getEndTime()));
            lblNextNote.setText(String.format(Locale.ROOT, "%.1f giờ · %s", next.getHours(),
                    fixed ? "Full-time" : "Part-time"));
        } catch (SQLException ex) {
            warn("Không tải được ca kế tiếp: " + ex.getMessage());
        }
    }

    /** Tính lại thống kê tháng này + bảng lương: mỗi thứ đọc DB đúng một lần. */
    private void refreshAll() {
        YearMonth ym = YearMonth.from(LocalDate.now());
        try {
            BigDecimal hours = attDao.workedHours(empId, ym.getMonthValue(), ym.getYear());
            int late = attDao.lateCount(empId, ym.getMonthValue(), ym.getYear());

            Set<YearMonth> done = salaryDao.finalizedMonths(ym.minusMonths(2), ym);
            List<SalaryRow> rows = salaryDao.findRecent(empId, ym, 3, done);

            // Lương tháng này lấy luôn từ danh sách vừa đọc, không truy vấn thêm
            SalaryRow cur = null;
            for (SalaryRow r : rows) {
                if (r.getMonth() == ym.getMonthValue() && r.getYear() == ym.getYear()) { cur = r; break; }
            }
            lblHours.setText(String.format(Locale.ROOT, "%.1f giờ", hours.doubleValue()));
            lblLate.setText(String.valueOf(late));
            lblPay.setText(cur == null ? money(BigDecimal.ZERO) : money(cur.getBaseSalary().add(cur.getAllowance())));
            fillPay(rows, done);
        } catch (SQLException ex) {
            warn("Không tải được thống kê tháng này: " + ex.getMessage());
        }
        refreshNextShift();
    }

    // =====================================================================
    // LỊCH LÀM: tuần này + tuần sau (giờ làm thực tế của từng ca)
    // =====================================================================
    private JComponent buildSchedule() {
        JPanel p = UIStyle.panel(new GridLayout(2, 1, 0, 12));
        p.add(UIStyle.card("Tuần này (" + weekRange(monday) + ")",
                UIStyle.plainScroll(UIStyle.grid(thisWeekModel))));
        p.add(UIStyle.card("Tuần sau (" + weekRange(nextMonday) + ") · \"chờ chốt\" = chưa được cửa hàng trưởng chốt",
                UIStyle.plainScroll(UIStyle.grid(nextWeekModel))));
        return p;
    }

    private void fillWeek(DefaultTableModel m, LocalDate mon) {
        m.setRowCount(0);
        for (ShiftTime sh : shifts) {
            Object[] row = new Object[DAYS.length + 1];
            row[0] = sh.getShiftName();
            for (int d = 0; d < DAYS.length; d++) {
                LocalDate date = mon.plusDays(d);
                List<WorkSchedule> cell = new ArrayList<>();
                for (WorkSchedule s : mine) {
                    if (s.getWorkDate().equals(date) && sh.getShiftName().equals(s.getShiftName())
                            && s.getStatus() != ScheduleStatus.CANCELLED) cell.add(s);
                }
                cell.sort(Comparator.comparing(WorkSchedule::getStartTime));
                List<String> parts = new ArrayList<>();
                for (WorkSchedule s : cell) {
                    parts.add(range(s.getStartTime(), s.getEndTime()));
                    if (s.getStatus() == ScheduleStatus.REGISTERED) parts.add("(chờ chốt)");
                }
                row[d + 1] = String.join(", ", parts);
            }
            m.addRow(row);
        }
    }

    private static String weekRange(LocalDate mon) {
        return mon.format(DM) + " – " + mon.plusDays(6).format(DM);
    }

    // =====================================================================
    // DUYỆT LỊCH LÀM: đăng ký ca (giờ linh động với part-time) + xin nghỉ/đổi ca
    // =====================================================================
    private JComponent buildApprove() {
        for (int i = 0; i < 7; i++) cbRegDate.addItem(dayLabel(nextMonday.plusDays(i)));
        cbRegShift.addActionListener(e -> {
            fillTimes(cbRegShift, cbRegFrom, cbRegTo);
            cbRegFrom.setEnabled(!fixed);
            cbRegTo.setEnabled(!fixed);
        });
        if (cbRegShift.getItemCount() > 0) cbRegShift.setSelectedIndex(0);
        fillTimes(cbRegShift, cbRegFrom, cbRegTo);
        cbRegFrom.setEnabled(!fixed);
        cbRegTo.setEnabled(!fixed);

        JTable regTable = UIStyle.table(regModel);
        String hint = fixed
                ? "Full-time: chọn ca sáng hoặc ca chiều, giờ làm là đủ ca."
                : "Part-time: chọn giờ trong khung ca (tối thiểu " + (MIN_PART_MINUTES / 60) + " giờ/lần).";
        JPanel regBody = UIStyle.panel(new BorderLayout(0, 8));
        JPanel regTop = UIStyle.panel(new GridLayout(2, 1));
        regTop.add(UIStyle.row(new JLabel("Ngày:"), cbRegDate, new JLabel("Ca:"), cbRegShift,
                new JLabel("Từ:"), cbRegFrom, new JLabel("Đến:"), cbRegTo,
                UIStyle.button("Đăng ký ca", this::registerShift),
                UIStyle.button("Hủy đăng ký", UIStyle.MUTED, new Color(0x4B5563), () -> cancelRegistration(regTable))));
        regTop.add(UIStyle.row(UIStyle.muted(hint)));
        regBody.add(regTop, BorderLayout.NORTH);
        regBody.add(UIStyle.plainScroll(regTable), BorderLayout.CENTER);

        JPanel reqBody = UIStyle.panel(new BorderLayout(0, 8));
        reqBody.add(UIStyle.row(UIStyle.button("Gửi đơn nghỉ / đổi ca", this::sendRequest)), BorderLayout.NORTH);
        reqBody.add(UIStyle.plainScroll(UIStyle.table(reqModel)), BorderLayout.CENTER);

        JPanel p = UIStyle.panel(new GridLayout(2, 1, 0, 12));
        p.add(UIStyle.card("Đăng ký ca mong muốn (tuần sau " + weekRange(nextMonday) + ")", regBody));
        p.add(UIStyle.card("Đơn xin nghỉ / đổi ca của tôi", reqBody));
        return p;
    }

    private void fillTimes(JComboBox<String> shiftBox, JComboBox<String> from, JComboBox<String> to) {
        ShiftTime sh = shift((String) shiftBox.getSelectedItem());
        String[] ts = sh == null ? new String[0] : times(sh);
        from.setModel(new DefaultComboBoxModel<>(ts));
        to.setModel(new DefaultComboBoxModel<>(ts));
        if (ts.length > 0) {
            from.setSelectedIndex(0);
            to.setSelectedIndex(ts.length - 1);
        }
    }

    private String dayLabel(LocalDate d) {
        return dayName(d) + " " + d.format(D);
    }

    /** Đăng ký ca tuần sau (REGISTERED). Trigger trg_WorkSchedule_Validate kiểm tra lại ở DB. */
    private void registerShift() {
        if (cbRegShift.getSelectedItem() == null || cbRegFrom.getSelectedItem() == null) {
            warn("Chưa có dữ liệu ca làm.");
            return;
        }
        LocalDate date = nextMonday.plusDays(cbRegDate.getSelectedIndex());
        String shiftName = (String) cbRegShift.getSelectedItem();
        LocalTime from = LocalTime.parse((String) cbRegFrom.getSelectedItem());
        LocalTime to = LocalTime.parse((String) cbRegTo.getSelectedItem());

        String err = checkRange(fixed, shift(shiftName), from, to);
        if (err != null) { warn(err); return; }

        for (WorkSchedule s : mine) {
            if (!s.getWorkDate().equals(date) || s.getStatus() == ScheduleStatus.CANCELLED) continue;
            if (fixed) { warn("Full-time chỉ đăng ký 1 ca mỗi ngày."); return; }
            if (s.getStartTime().isBefore(to) && from.isBefore(s.getEndTime())) {
                warn("Trùng giờ với " + s.getShiftName() + " " + range(s.getStartTime(), s.getEndTime())
                        + " đã đăng ký.");
                return;
            }
        }
        try {
            workDao.register(empId, date, shiftName, from, to);
        } catch (SQLException ex) {
            warn("Không đăng ký được ca: " + ex.getMessage());   // gồm cả lỗi từ trigger
            return;
        }
        reloadSchedules();
    }

    private void cancelRegistration(JTable t) {
        int r = t.getSelectedRow();
        if (r < 0) { warn("Vui lòng chọn một dòng trước."); return; }
        WorkSchedule s = regRows.get(t.convertRowIndexToModel(r));
        if (s.getStatus() != ScheduleStatus.REGISTERED) {
            warn("Chỉ hủy được ca đang chờ chốt. Ca đã chốt phải gửi đơn xin nghỉ / đổi ca.");
            return;
        }
        try {
            if (!workDao.cancelRegistration(s.getScheduleId(), empId)) {
                warn("Đăng ký này đã được xử lý, không thể hủy.");
            }
        } catch (SQLException ex) {
            warn("Không hủy được đăng ký: " + ex.getMessage());
            return;
        }
        reloadSchedules();
    }

    private void sendRequest() {
        List<LocalDate> dates = new ArrayList<>();
        for (int i = 1; i <= 14; i++) dates.add(today.plusDays(i));

        JComboBox<String> cbType = new JComboBox<>(REQ_TYPES);
        JComboBox<String> cbDate = new JComboBox<>();
        for (LocalDate d : dates) cbDate.addItem(dayLabel(d));
        JComboBox<String> cbCur = new JComboBox<>(prepend("Cả ngày", shiftNames()));
        JComboBox<String> cbNew = new JComboBox<>(shiftNames());
        JComboBox<String> cbFrom = new JComboBox<>(), cbTo = new JComboBox<>();
        JTextField reason = new JTextField();

        Runnable refresh = () -> {
            boolean change = cbType.getSelectedIndex() == 1;
            cbNew.setEnabled(change);
            cbFrom.setEnabled(change && !fixed);
            cbTo.setEnabled(change && !fixed);
        };
        cbNew.addActionListener(ev -> { fillTimes(cbNew, cbFrom, cbTo); refresh.run(); });
        cbType.addActionListener(ev -> refresh.run());
        fillTimes(cbNew, cbFrom, cbTo);
        refresh.run();

        JPanel f = new JPanel(new GridLayout(0, 2, 8, 8));
        f.add(new JLabel("Loại đơn:"));         f.add(cbType);
        f.add(new JLabel("Ngày:"));             f.add(cbDate);
        f.add(new JLabel("Ca hiện tại:"));      f.add(cbCur);
        f.add(new JLabel("Đổi sang ca:"));      f.add(cbNew);
        f.add(new JLabel("Giờ mới từ:"));       f.add(cbFrom);
        f.add(new JLabel("Giờ mới đến:"));      f.add(cbTo);
        f.add(new JLabel("Lý do:"));            f.add(reason);

        int ok = JOptionPane.showConfirmDialog(this, f, "Gửi đơn", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);
        if (ok != JOptionPane.OK_OPTION) return;

        LocalDate date = dates.get(cbDate.getSelectedIndex());
        boolean change = cbType.getSelectedIndex() == 1;
        String cur = cbCur.getSelectedIndex() == 0 ? null : (String) cbCur.getSelectedItem();

        if (reason.getText().isBlank()) { warn("Vui lòng nhập lý do."); return; }
        if (change && cur == null) { warn("Đổi ca: hãy chọn ca hiện tại cần đổi."); return; }

        LeaveRequest req = new LeaveRequest();
        req.setEmployeeId(empId);
        req.setRequestType(change ? RequestType.SHIFT_CHANGE : RequestType.LEAVE);
        req.setTargetDate(date);
        req.setShiftName(cur);
        req.setReason(reason.getText().trim());

        if (change) {
            String ns = (String) cbNew.getSelectedItem();
            LocalTime from = LocalTime.parse((String) cbFrom.getSelectedItem());
            LocalTime to = LocalTime.parse((String) cbTo.getSelectedItem());
            String err = checkRange(fixed, shift(ns), from, to);
            if (err != null) { warn(err); return; }
            req.setNewShiftName(ns);
            req.setNewStartTime(from);
            req.setNewEndTime(to);
        }

        try {
            if (!workDao.hasSchedule(empId, date, cur)) {
                warn("Bạn không có ca làm tương ứng vào ngày này.");
                return;
            }
            for (LeaveRequest r : reqRows) {
                boolean sameTarget = r.getTargetDate().equals(date)
                        && (r.getShiftName() == null || cur == null || r.getShiftName().equals(cur));
                if (sameTarget && "PENDING".equals(r.getStatus().name())) {
                    warn("Bạn đã có đơn đang chờ duyệt cho ca này.");
                    return;
                }
            }
            leaveDao.insert(req);
        } catch (SQLException ex) {
            warn("Không gửi được đơn: " + ex.getMessage());
            return;
        }
        reloadRequests();
    }

    // =====================================================================
    // BẢNG LƯƠNG (tab: Bảng lương | Bảng chấm công)
    // =====================================================================
    private JComponent buildPay() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(UIStyle.BOLD);
        tabs.addTab("Bảng lương", pad(UIStyle.plainScroll(UIStyle.table(payModel))));
        tabs.addTab("Bảng chấm công", pad(UIStyle.plainScroll(UIStyle.table(attModel))));
        return tabs;
    }

    private JPanel pad(JComponent c) {
        JPanel p = UIStyle.panel(new BorderLayout());
        p.setBorder(new EmptyBorder(10, 10, 10, 10));
        p.add(c, BorderLayout.CENTER);
        return p;
    }

    /** Đổ 3 tháng gần nhất vào bảng lương: tháng có trong Payroll = "Đã chốt", còn lại = "Tạm tính". */
    private void fillPay(List<SalaryRow> rows, Set<YearMonth> done) {
        payModel.setRowCount(0);
        for (SalaryRow r : rows) {
            boolean finalized = done.contains(YearMonth.of(r.getYear(), r.getMonth()));
            payModel.addRow(new Object[]{String.format("%02d/%d", r.getMonth(), r.getYear()),
                    String.format(Locale.ROOT, "%.1f", r.getHours().doubleValue()),
                    String.format(Locale.ROOT, "%.1f", r.getWorkDays().doubleValue()),
                    r.getSalaryType() == SalaryType.FIXED ? "Cố định" : money(r.getSalaryRate()) + "/giờ",
                    money(r.getBaseSalary()), money(r.getAllowance()), money(r.getRevenueBonus()),
                    money(r.getTotalSalary()), finalized ? "Đã chốt" : "Tạm tính"});
        }
    }

    // =====================================================================
    // THÔNG BÁO (Notification + NotificationRead)
    // =====================================================================
    private List<NotiItem> notifications() {
        List<NotiItem> l = new ArrayList<>();
        try {
            for (NotificationReadDAO.Item n : notiReadDao.findInbox(empId)) {
                l.add(new NotiItem("<b>" + esc(n.title) + "</b> <i>(" + n.createdAt.format(DT) + ")</i><br>"
                        + esc(n.content), n.read));
            }
        } catch (SQLException ex) {
            warn("Không tải được thông báo: " + ex.getMessage());
        }
        return l;
    }

    /** BaseFrame bắt IllegalStateException để báo lỗi và giữ nguyên giao diện. */
    private void markAllNotificationsRead() {
        try {
            notiReadDao.markAllRead(empId);
        } catch (SQLException ex) {
            throw new IllegalStateException("Không đánh dấu đã đọc được: " + ex.getMessage(), ex);
        }
    }

    private static String esc(String s) {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    // =====================================================================
    // TIỆN ÍCH ĐỊNH DẠNG
    // =====================================================================
    private static String[] prepend(String first, String[] rest) {
        String[] r = new String[rest.length + 1];
        r[0] = first;
        System.arraycopy(rest, 0, r, 1, rest.length);
        return r;
    }

    private static String hm(LocalTime t) {
        return t == null ? "--:--" : t.format(HM);
    }

    private static String range(LocalTime a, LocalTime b) {
        return hm(a) + "–" + hm(b);
    }

    private static String dayName(LocalDate d) {
        return DAYS[d.getDayOfWeek().getValue() - 1];
    }

    private static String money(BigDecimal v) {
        return NumberFormat.getInstance(Locale.forLanguageTag("vi-VN")).format(v == null ? BigDecimal.ZERO : v) + " ₫";
    }

    private static String schedVi(ScheduleStatus s) {
        if (s == ScheduleStatus.CONFIRMED) return "Đã chốt";
        if (s == ScheduleStatus.CANCELLED) return "Đã hủy";
        return "Chờ chốt";
    }

    private static String reqVi(String s) {
        switch (s) {
            case "APPROVED": return "Đã duyệt";
            case "REJECTED": return "Từ chối";
            default: return "Chờ duyệt";
        }
    }

    /** Trạng thái chấm công hiển thị cho nhân viên. */
    private String attVi(Attendance a) {
        AttendanceStatus st = a.getStatus();
        if (st == AttendanceStatus.ABSENT) return "Vắng";
        if (a.getCheckInTime() != null && a.getCheckOutTime() == null) {
            if (a.getWorkDate().isBefore(LocalDate.now())) return "Chưa chấm công ra";
            return st == AttendanceStatus.LATE ? "Đi trễ" : "Đang làm";
        }
        if (st == AttendanceStatus.LATE) return "Đi trễ";
        if (st == AttendanceStatus.EARLY_LEAVE) return "Về sớm";
        return "Đúng giờ";
    }

    /** Các mốc giờ (bước 30 phút) trong khung của ca, gồm cả hai đầu. */
    private static String[] times(ShiftTime s) {
        List<String> l = new ArrayList<>();
        int end = s.getEndTime().toSecondOfDay() / 60;
        for (int m = s.getStartTime().toSecondOfDay() / 60; m <= end; m += 30) {
            l.add(hm(LocalTime.ofSecondOfDay(m * 60L)));
        }
        return l.toArray(new String[0]);
    }

    /** Full-time: đủ khung ca. Part-time: trong khung ca và tối thiểu MIN_PART_MINUTES. Trả về lỗi hoặc null. */
    private static String checkRange(boolean fixed, ShiftTime s, LocalTime from, LocalTime to) {
        if (s == null) return "Không tìm thấy ca đã chọn.";
        if (!to.isAfter(from)) return "Giờ kết thúc phải sau giờ bắt đầu.";
        if (from.isBefore(s.getStartTime()) || to.isAfter(s.getEndTime())) {
            return s.getShiftName() + " chỉ từ " + hm(s.getStartTime()) + " đến " + hm(s.getEndTime()) + ".";
        }
        if (fixed) {
            if (!from.equals(s.getStartTime()) || !to.equals(s.getEndTime())) {
                return "Full-time phải đăng ký đủ ca (" + range(s.getStartTime(), s.getEndTime()) + ").";
            }
        } else if (Duration.between(from, to).toMinutes() < MIN_PART_MINUTES) {
            return "Part-time đăng ký tối thiểu " + (MIN_PART_MINUTES / 60) + " giờ mỗi lần.";
        }
        return null;
    }
}