package com.hr.ui;

import com.hr.model.Account;
import com.hr.ui.DemoData.Att;
import com.hr.ui.DemoData.Emp;
import com.hr.ui.DemoData.Sched;
import com.hr.ui.DemoData.Shift;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Nhân viên. Menu: Tổng quan | Lịch làm (Lịch làm, Duyệt lịch làm) | Thông báo | Bảng lương.
 * Ca A/B chỉ là khung giờ: full-time đăng ký đủ khung, part-time chọn giờ linh động trong khung.
 * DEMO, dữ liệu giả lấy từ DemoData (phản chiếu database.sql).
 */
public class EmployeeFrame extends BaseFrame {
    private static final DateTimeFormatter D = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DM = DateTimeFormatter.ofPattern("dd/MM");
    private static final DateTimeFormatter HMS = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final String[] REQ_TYPES = {"Xin nghỉ", "Đổi ca"};

    private final Emp me;
    private final LocalDate today = LocalDate.now();
    private final LocalDate monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    private final LocalDate nextMonday = monday.plusWeeks(1);
    /** Lịch của tôi: tuần này + tuần sau (WorkSchedule). */
    private final List<Sched> mine = new ArrayList<>();
    /** Các dòng đang hiện ở bảng đăng ký (tuần sau), cùng thứ tự với regModel. */
    private final List<Sched> regRows = new ArrayList<>();

    // Chấm công
    private final JLabel lblClock = new JLabel("--:--:--", SwingConstants.CENTER);
    private final JLabel lblState = new JLabel("Chưa vào ca", SwingConstants.CENTER);
    private final DefaultTableModel attModel =
            UIStyle.model("Ngày", "Ca đăng ký", "Giờ vào", "Giờ ra", "Số giờ", "Trạng thái");
    private final JButton btnIn = UIStyle.button("VÀO CA", this::clockIn);
    private final JButton btnOut = UIStyle.button("RA CA", this::clockOut);
    private LocalTime inTime;
    private Sched workingShift;
    private boolean lateFlag;

    // Ca kế tiếp + thống kê
    private final JLabel lblNextTitle = UIStyle.bold("");
    private final JLabel lblNextTime = UIStyle.muted("");
    private final JLabel lblNextNote = UIStyle.muted("");
    private final JLabel lblHours = statValue();
    private final JLabel lblLate = statValue();
    private final JLabel lblPay = statValue();

    // Lịch tuần
    private final DefaultTableModel thisWeekModel = UIStyle.model(DemoData.prepend("Ca", DemoData.DAYS));
    private final DefaultTableModel nextWeekModel = UIStyle.model(DemoData.prepend("Ca", DemoData.DAYS));

    // Đăng ký ca / đơn từ
    private final DefaultTableModel regModel = UIStyle.model("Ngày", "Ca", "Giờ làm", "Trạng thái");
    private final DefaultTableModel reqModel =
            UIStyle.model("Loại đơn", "Ngày", "Ca hiện tại", "Nội dung", "Lý do", "Trạng thái");
    private final JComboBox<String> cbRegDate = new JComboBox<>();
    private final JComboBox<String> cbRegShift = new JComboBox<>(DemoData.shiftNames());
    private final JComboBox<String> cbRegFrom = new JComboBox<>();
    private final JComboBox<String> cbRegTo = new JComboBox<>();

    // Lương
    private final DefaultTableModel payModel = UIStyle.model(
            "Kỳ lương", "Tổng giờ", "Công", "Loại lương", "Lương cơ bản", "Phụ cấp", "Thưởng DT", "Thực lĩnh", "Tình trạng");

    public EmployeeFrame(Account acc) {
        super(acc, "Nhân viên");
        this.me = profile;

        // ----- Menu bên trái -----
        addMenu("overview", "Tổng quan");
        addGroup("Lịch làm", new String[]{"schedule", "approve"}, new String[]{"Lịch làm", "Duyệt lịch làm"});
        addMenu("noti", "Thông báo");
        addMenu("pay", "Bảng lương");

        // ----- Trang nội dung bên phải -----
        loadMockData();
        addPage("overview", buildOverview(), this::refreshNextShift);
        addPage("schedule", buildSchedule());
        addPage("approve", buildApprove());
        addPage("noti", notificationPage("noti", notifications()));
        addPage("pay", buildPay());

        refreshAll();
        startClock();
        start("overview");
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

    /** Vào ca: chỉ khi hôm nay có ca ĐÃ CHỐT; trễ nếu vào muộn hơn giờ đã đăng ký quá LATE_GRACE_MIN phút. */
    private void clockIn() {
        String todayText = today.format(D);
        for (int i = 0; i < attModel.getRowCount(); i++) {
            if (todayText.equals(attModel.getValueAt(i, 0))) {
                warn("Hôm nay bạn đã chấm công rồi.");
                return;
            }
        }
        Sched s = mine.stream()
                .filter(x -> x.date.equals(today) && "CONFIRMED".equals(x.status))
                .min(Comparator.comparing(x -> x.start)).orElse(null);
        if (s == null) {
            warn("Hôm nay bạn không có ca đã chốt nên không thể chấm công.");
            return;
        }
        workingShift = s;
        inTime = LocalTime.now().withSecond(0).withNano(0);
        lateFlag = inTime.isAfter(s.start.plusMinutes(DemoData.LATE_GRACE_MIN));
        attModel.insertRow(0, new Object[]{todayText, DemoData.range(s.start, s.end),
                DemoData.hm(inTime), "--", "--", lateFlag ? "Đi trễ" : "Đang làm"});
        btnIn.setEnabled(false);
        btnOut.setEnabled(true);
        lblState.setText("Đang trong ca từ " + DemoData.hm(inTime) + " (đăng ký "
                + DemoData.range(s.start, s.end) + ")");
        refreshAll();
    }

    /** Ra ca: sớm hơn giờ đã đăng ký thì "Về sớm" (nếu đã "Đi trễ" thì giữ "Đi trễ"). */
    private void clockOut() {
        if (inTime == null || workingShift == null) return;
        LocalTime out = LocalTime.now().withSecond(0).withNano(0);
        double hours = Math.max(0, Duration.between(inTime, out).toMinutes() / 60.0);
        String status = lateFlag ? "Đi trễ" : out.isBefore(workingShift.end) ? "Về sớm" : "Đúng giờ";
        attModel.setValueAt(DemoData.hm(out), 0, 3);
        attModel.setValueAt(String.format(Locale.ROOT, "%.1f", hours), 0, 4);
        attModel.setValueAt(status, 0, 5);
        btnIn.setEnabled(true);
        btnOut.setEnabled(false);
        lblState.setText("Đã ra ca lúc " + DemoData.hm(out));
        inTime = null;
        workingShift = null;
        // TODO: INSERT/UPDATE Attendance (schedule_id, check_in_time, check_out_time, status)
        refreshAll();
    }

    private void refreshNextShift() {
        LocalDateTime now = LocalDateTime.now();
        Sched next = mine.stream()
                .filter(s -> "CONFIRMED".equals(s.status) && LocalDateTime.of(s.date, s.end).isAfter(now))
                .min(Comparator.comparing(s -> LocalDateTime.of(s.date, s.start))).orElse(null);
        if (next == null) {
            lblNextTitle.setText("Chưa có ca tiếp theo");
            lblNextTime.setText("");
            lblNextNote.setText("");
            return;
        }
        double h = Duration.between(next.start, next.end).toMinutes() / 60.0;
        lblNextTitle.setText(DemoData.dayName(next.date) + " " + next.date.format(DM) + " · " + next.shift);
        lblNextTime.setText(DemoData.range(next.start, next.end));
        lblNextNote.setText(String.format(Locale.ROOT, "%.1f giờ · %s", h, me.fixed() ? "Full-time" : "Part-time"));
    }

    /** Tính lại thống kê tháng này + bảng lương từ bảng chấm công. */
    private void refreshAll() {
        YearMonth ym = YearMonth.from(today);
        double hours = hoursOf(ym);
        int late = 0;
        for (int i = 0; i < attModel.getRowCount(); i++) {
            if (inMonth(i, ym) && "Đi trễ".equals(attModel.getValueAt(i, 5))) late++;
        }
        lblHours.setText(String.format(Locale.ROOT, "%.1f giờ", hours));
        lblLate.setText(String.valueOf(late));
        lblPay.setText(DemoData.money(DemoData.baseSalary(me, hours) + me.pos().allowance));
        refreshNextShift();
        refreshPay(hours);
    }

    private boolean inMonth(int row, YearMonth ym) {
        try {
            return YearMonth.from(LocalDate.parse(String.valueOf(attModel.getValueAt(row, 0)), D)).equals(ym);
        } catch (RuntimeException ex) {
            return false;
        }
    }

    private double hoursOf(YearMonth ym) {
        double h = 0;
        for (int i = 0; i < attModel.getRowCount(); i++) {
            if (!inMonth(i, ym)) continue;
            String s = String.valueOf(attModel.getValueAt(i, 4));
            if (s.matches("\\d+(\\.\\d+)?")) h += Double.parseDouble(s);
        }
        return h;
    }

    // =====================================================================
    // LỊCH LÀM: tuần này + tuần sau (giờ làm thực tế của từng ca)
    // =====================================================================
    private JComponent buildSchedule() {
        JPanel p = UIStyle.panel(new GridLayout(2, 1, 0, 12));
        p.add(UIStyle.card("Tuần này (" + range(monday) + ")",
                UIStyle.plainScroll(UIStyle.grid(thisWeekModel))));
        p.add(UIStyle.card("Tuần sau (" + range(nextMonday) + ") · \"chờ chốt\" = chưa được cửa hàng trưởng chốt",
                UIStyle.plainScroll(UIStyle.grid(nextWeekModel))));
        return p;
    }

    private void fillWeek(DefaultTableModel m, LocalDate mon) {
        m.setRowCount(0);
        for (Shift sh : DemoData.SHIFTS) {
            Object[] row = new Object[DemoData.DAYS.length + 1];
            row[0] = sh.name;
            for (int d = 0; d < DemoData.DAYS.length; d++) {
                LocalDate date = mon.plusDays(d);
                List<Sched> cell = new ArrayList<>();
                for (Sched s : mine) {
                    if (s.date.equals(date) && sh.name.equals(s.shift) && s.active()) cell.add(s);
                }
                cell.sort(Comparator.comparing(s -> s.start));
                List<String> parts = new ArrayList<>();
                for (Sched s : cell) {
                    parts.add(DemoData.range(s.start, s.end));
                    if ("REGISTERED".equals(s.status)) parts.add("(chờ chốt)");
                }
                row[d + 1] = String.join(", ", parts);
            }
            m.addRow(row);
        }
    }

    private static String range(LocalDate mon) {
        return mon.format(DM) + " – " + mon.plusDays(6).format(DM);
    }

    // =====================================================================
    // DUYỆT LỊCH LÀM: đăng ký ca (giờ linh động với part-time) + xin nghỉ/đổi ca
    // =====================================================================
    private JComponent buildApprove() {
        for (int i = 0; i < 7; i++) cbRegDate.addItem(dayLabel(nextMonday.plusDays(i)));
        cbRegShift.addActionListener(e -> {
            fillTimes(cbRegShift, cbRegFrom, cbRegTo);
            cbRegFrom.setEnabled(!me.fixed());
            cbRegTo.setEnabled(!me.fixed());
        });
        cbRegShift.setSelectedIndex(0);
        fillTimes(cbRegShift, cbRegFrom, cbRegTo);
        cbRegFrom.setEnabled(!me.fixed());
        cbRegTo.setEnabled(!me.fixed());

        JTable regTable = UIStyle.table(regModel);
        String hint = me.fixed()
                ? "Full-time: chọn ca sáng hoặc ca chiều, giờ làm là đủ ca."
                : "Part-time: chọn giờ trong khung ca (tối thiểu " + (DemoData.MIN_PART_MINUTES / 60) + " giờ/lần).";
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
        p.add(UIStyle.card("Đăng ký ca mong muốn (tuần sau " + range(nextMonday) + ")", regBody));
        p.add(UIStyle.card("Đơn xin nghỉ / đổi ca của tôi", reqBody));
        return p;
    }

    private static void fillTimes(JComboBox<String> shiftBox, JComboBox<String> from, JComboBox<String> to) {
        String[] ts = DemoData.times(DemoData.shift((String) shiftBox.getSelectedItem()));
        from.setModel(new DefaultComboBoxModel<>(ts));
        to.setModel(new DefaultComboBoxModel<>(ts));
        from.setSelectedIndex(0);
        to.setSelectedIndex(ts.length - 1);
    }

    private String dayLabel(LocalDate d) {
        return DemoData.dayName(d) + " " + d.format(D);
    }

    private void registerShift() {
        LocalDate date = nextMonday.plusDays(cbRegDate.getSelectedIndex());
        String shift = (String) cbRegShift.getSelectedItem();
        LocalTime from = LocalTime.parse((String) cbRegFrom.getSelectedItem());
        LocalTime to = LocalTime.parse((String) cbRegTo.getSelectedItem());

        String err = DemoData.checkRange(me.fixed(), shift, from, to);
        if (err != null) { warn(err); return; }

        for (Sched s : mine) {
            if (!s.date.equals(date) || !s.active()) continue;
            if (me.fixed()) { warn("Full-time chỉ đăng ký 1 ca mỗi ngày."); return; }
            if (s.overlaps(from, to)) {
                warn("Trùng giờ với " + s.shift + " " + DemoData.range(s.start, s.end) + " đã đăng ký.");
                return;
            }
        }
        // TODO: INSERT WorkSchedule (trigger trg_WorkSchedule_Validate kiểm tra lại ở DB)
        mine.add(new Sched(me.id, date, shift, from, to, "REGISTERED"));
        reloadSchedules();
    }

    private void cancelRegistration(JTable t) {
        int r = t.getSelectedRow();
        if (r < 0) { warn("Vui lòng chọn một dòng trước."); return; }
        Sched s = regRows.get(t.convertRowIndexToModel(r));
        if (!"REGISTERED".equals(s.status)) {
            warn("Chỉ hủy được ca đang chờ chốt. Ca đã chốt phải gửi đơn xin nghỉ / đổi ca.");
            return;
        }
        s.status = "CANCELLED";
        reloadSchedules();
    }

    private void sendRequest() {
        List<LocalDate> dates = new ArrayList<>();
        for (int i = 1; i <= 14; i++) dates.add(today.plusDays(i));

        JComboBox<String> cbType = new JComboBox<>(REQ_TYPES);
        JComboBox<String> cbDate = new JComboBox<>();
        for (LocalDate d : dates) cbDate.addItem(dayLabel(d));
        JComboBox<String> cbCur = new JComboBox<>(DemoData.prepend("Cả ngày", DemoData.shiftNames()));
        JComboBox<String> cbNew = new JComboBox<>(DemoData.shiftNames());
        JComboBox<String> cbFrom = new JComboBox<>(), cbTo = new JComboBox<>();
        JTextField reason = new JTextField();

        Runnable refresh = () -> {
            boolean change = cbType.getSelectedIndex() == 1;
            cbNew.setEnabled(change);
            cbFrom.setEnabled(change && !me.fixed());
            cbTo.setEnabled(change && !me.fixed());
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
        boolean has = mine.stream().anyMatch(s -> s.date.equals(date) && s.active()
                && (cur == null || cur.equals(s.shift)));
        if (!has) { warn("Bạn không có ca làm tương ứng vào ngày này."); return; }

        String content = cur == null ? "Nghỉ cả ngày" : "Nghỉ " + cur.toLowerCase();
        if (change) {
            String ns = (String) cbNew.getSelectedItem();
            LocalTime from = LocalTime.parse((String) cbFrom.getSelectedItem());
            LocalTime to = LocalTime.parse((String) cbTo.getSelectedItem());
            String err = DemoData.checkRange(me.fixed(), ns, from, to);
            if (err != null) { warn(err); return; }
            content = "→ " + ns + " " + DemoData.range(from, to);
        }
        // TODO: INSERT LeaveRequest (request_type, target_date, shift_name, new_shift_name, new_*_time, reason)
        reqModel.addRow(new Object[]{REQ_TYPES[cbType.getSelectedIndex()], date.format(D),
                cur == null ? "Cả ngày" : cur, content, reason.getText().trim(), "Chờ duyệt"});
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

    /** Hai tháng trước: đã chốt (Payroll). Tháng này: tạm tính từ chấm công, chưa có thưởng doanh thu. */
    private void refreshPay(double currentHours) {
        payModel.setRowCount(0);
        YearMonth now = YearMonth.from(today);
        payModel.addRow(payRow(now, currentHours, 0, "Tạm tính"));
        for (int ago = 1; ago <= 2; ago++) {
            payModel.addRow(payRow(now.minusMonths(ago), DemoData.hoursPrev(me, ago), DemoData.bonus(me), "Đã chốt"));
        }
    }

    private Object[] payRow(YearMonth ym, double hours, long bonus, String state) {
        long base = DemoData.baseSalary(me, hours);
        long allow = me.pos().allowance;
        return new Object[]{String.format("%02d/%d", ym.getMonthValue(), ym.getYear()),
                String.format(Locale.ROOT, "%.1f", hours), String.format(Locale.ROOT, "%.1f", hours / 9.0),
                me.fixed() ? "Cố định" : DemoData.money(me.pos().rate) + "/giờ",
                DemoData.money(base), DemoData.money(allow), DemoData.money(bonus),
                DemoData.money(base + allow + bonus), state};
    }

    // =====================================================================
    // Thông báo
    // =====================================================================
    private List<String> notifications() {
        List<String> l = new ArrayList<>();
        for (String[] n : DemoData.NOTIS) {
            if (n[0].isEmpty() || n[0].equals(me.id))
                l.add("<b>" + n[1] + "</b> <i>(" + n[3] + ")</i><br>" + n[2]);
        }
        return l;
    }

    // =====================================================================
    // Dữ liệu giả
    // =====================================================================
    private void loadMockData() {
        mine.addAll(DemoData.scheduleFor(me, monday, false));
        mine.addAll(DemoData.scheduleFor(me, nextMonday, true));

        List<Att> past = DemoData.pastAttendance(me, today);
        Collections.reverse(past); // mới nhất lên đầu
        for (Att a : past) {
            double h = a.in != null && a.out != null ? Duration.between(a.in, a.out).toMinutes() / 60.0 : 0;
            attModel.addRow(new Object[]{a.date.format(D), DemoData.range(a.start, a.end),
                    a.in == null ? "--" : DemoData.hm(a.in), a.out == null ? "--" : DemoData.hm(a.out),
                    a.in != null && a.out != null ? String.format(Locale.ROOT, "%.1f", h) : "--",
                    DemoData.attVi(a.status)});
        }

        LocalDate d = nextMonday.plusDays(2);
        reqModel.addRow(new Object[]{"Xin nghỉ", d.format(D), "Cả ngày", "Nghỉ cả ngày", "Việc gia đình", "Chờ duyệt"});
        reloadSchedules();
    }

    private void reloadSchedules() {
        fillWeek(thisWeekModel, monday);
        fillWeek(nextWeekModel, nextMonday);

        regRows.clear();
        regModel.setRowCount(0);
        List<Sched> nx = new ArrayList<>();
        for (Sched s : mine) if (!s.date.isBefore(nextMonday)) nx.add(s);
        nx.sort(Comparator.comparing((Sched s) -> s.date).thenComparing(s -> s.start));
        for (Sched s : nx) {
            regRows.add(s);
            regModel.addRow(new Object[]{dayLabel(s.date), s.shift, DemoData.range(s.start, s.end),
                    DemoData.schedVi(s.status)});
        }
    }
}