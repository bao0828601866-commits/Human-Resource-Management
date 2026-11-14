package com.hr.ui;

import com.hr.model.Account;
import com.hr.ui.DemoData.Att;
import com.hr.ui.DemoData.Emp;
import com.hr.ui.DemoData.Sched;
import com.hr.ui.DemoData.Shift;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.text.Collator;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Cửa hàng trưởng. Menu: Tổng quan | Nhân sự | Lịch làm (Lịch làm, Duyệt lịch làm) | Thông báo.
 * Chấm công + Lương nằm trong trang "Nhân sự" (dạng tab).
 * Lịch làm: ca A/B là khung giờ; mỗi đăng ký có giờ thực tế; giới hạn số người LÀM CÙNG LÚC.
 * DEMO, dữ liệu giả lấy từ DemoData (phản chiếu database.sql).
 */
public class ManagerFrame extends BaseFrame {
    private static final DateTimeFormatter D = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DM = DateTimeFormatter.ofPattern("dd/MM");
    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final LocalDate today = LocalDate.now();
    private final LocalDate monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    private final List<Emp> emps = new ArrayList<>(DemoData.employees());
    private int nextNum = 17;

    // --- Nhân sự ---
    private final JTabbedPane staffTabs = new JTabbedPane();
    private final DefaultTableModel empModel =
            UIStyle.model("Mã NV", "Họ tên", "Giới tính", "Chức vụ", "SĐT", "Địa chỉ", "Trạng thái");
    private final JTable empTable = UIStyle.table(empModel);
    private final TableRowSorter<DefaultTableModel> empSorter = new TableRowSorter<>(empModel);
    private final JTextField txtSearch = new JTextField(14);
    private final JComboBox<String> cbPos = new JComboBox<>(DemoData.prepend("Tất cả", DemoData.positionNames()));
    private final JComboBox<String> cbGender = new JComboBox<>(DemoData.prepend("Tất cả", DemoData.GENDER_VI));
    private final JComboBox<String> cbStatus = new JComboBox<>(new String[]{"Đang làm", "Đã nghỉ", "Tất cả"});
    private final JComboBox<String> cbSort = new JComboBox<>(new String[]{"Sắp xếp: Mã NV", "Sắp xếp: Tên"});

    // --- Chấm công / Lương ---
    private final DefaultTableModel attModel =
            UIStyle.model("Mã NV", "Họ tên", "Ngày", "Ca đăng ký", "Giờ vào", "Giờ ra", "Trạng thái");
    private final JTable attTable = UIStyle.table(attModel);
    private final TableRowSorter<DefaultTableModel> attSorter = new TableRowSorter<>(attModel);
    private final JTextField txtAtt = new JTextField(14);
    private final DefaultTableModel logModel =
            UIStyle.model("Thời gian", "Người sửa", "Bản ghi", "Trước khi sửa", "Sau khi sửa", "Lý do");
    private final DefaultTableModel payModel = UIStyle.model(
            "Mã NV", "Họ tên", "Chức vụ", "Loại", "Tổng giờ", "Lương cơ bản", "Phụ cấp", "Thưởng DT", "Thực lĩnh");
    private final JTable payTable = UIStyle.table(payModel);
    private final List<YearMonth> months = new ArrayList<>();
    private final JComboBox<String> cbMonth = new JComboBox<>();
    private final Set<YearMonth> closed = new HashSet<>();
    private final JLabel lblPayState = UIStyle.bold("");

    // --- Lịch làm ---
    private final DefaultTableModel gridModel = UIStyle.model(DemoData.prepend("Ca", DemoData.DAYS));
    private final Map<Integer, List<Sched>> weeks = new HashMap<>();
    private final JLabel lblWeek = UIStyle.bold("");
    private int weekOffset = 0;

    // --- Duyệt lịch làm ---
    private final DefaultTableModel regModel =
            UIStyle.model("Mã NV", "Họ tên", "Loại", "Ngày", "Ca", "Giờ làm", "Trạng thái");
    private final List<Sched> regRows = new ArrayList<>();
    private final Set<Sched> processed = new HashSet<>();

    private static final class Leave {
        String empId, type, shift, newShift, reason, status;
        LocalDate date;
        LocalTime newStart, newEnd;
    }

    private final List<Leave> leaves = new ArrayList<>();
    private final DefaultTableModel leaveModel = UIStyle.model(
            "Mã NV", "Họ tên", "Loại đơn", "Ngày", "Ca", "Nội dung", "Lý do", "Trạng thái");

    // --- Tổng quan ---
    private final JPanel inShiftPanel = UIStyle.panel(new GridLayout(1, 3, 10, 0));
    private final JPanel nextPanel = UIStyle.panel(null);

    // --- Thông báo ---
    private final DefaultTableModel sentModel = UIStyle.model("Thời gian", "Người nhận", "Tiêu đề", "Nội dung");

    public ManagerFrame(Account acc) {
        super(acc, "Cửa hàng trưởng");

        // ----- Menu bên trái -----
        addMenu("overview", "Tổng quan");
        addMenu("staff", "Nhân sự");
        addGroup("Lịch làm", new String[]{"schedule", "approve"}, new String[]{"Lịch làm", "Duyệt lịch làm"});
        addMenu("noti", "Thông báo");

        // ----- Trang nội dung bên phải -----
        loadMockData();
        addPage("overview", buildOverview(), this::refreshOverview);
        addPage("staff", buildStaffPage());
        addPage("schedule", buildSchedulePage(), this::loadGrid);
        addPage("approve", buildApprovePage(), this::reloadReg);
        addPage("noti", buildNotiPage());

        applyEmpFilter();
        refreshPayroll();
        loadGrid();
        reloadReg();
        start("overview");
    }

    // =====================================================================
    // TỔNG QUAN
    // =====================================================================
    private JComponent buildOverview() {
        nextPanel.setLayout(new BoxLayout(nextPanel, BoxLayout.Y_AXIS));

        JPanel nextCard = UIStyle.card("Ca làm tiếp theo", UIStyle.plainScroll(nextPanel));
        nextCard.setPreferredSize(new Dimension(320, 190));
        JPanel top = UIStyle.panel(new BorderLayout(12, 0));
        top.add(UIStyle.card("Nhân viên đang trong ca", inShiftPanel), BorderLayout.CENTER);
        top.add(nextCard, BorderLayout.EAST);
        top.setPreferredSize(new Dimension(0, 190));

        String[] mon = {"T1", "T2", "T3", "T4", "T5", "T6", "T7", "T8", "T9", "T10", "T11", "T12"};
        JPanel chart = UIStyle.card("Doanh thu cửa hàng (triệu đồng, T11/2026 là lũy kế)",
                new BarChartPanel(mon, "2025", DemoData.REVENUE_2025, "2026", DemoData.REVENUE_2026));

        JPanel leave = UIStyle.card("Yêu cầu nghỉ phép / đổi ca", leavePanel());
        leave.setPreferredSize(new Dimension(0, 210));

        JPanel page = UIStyle.panel(new BorderLayout(0, 12));
        page.add(top, BorderLayout.NORTH);
        page.add(chart, BorderLayout.CENTER);
        page.add(leave, BorderLayout.SOUTH);
        return page;
    }

    /** Làm mới "đang trong ca" và "ca tiếp theo" theo lịch đã chốt (gọi mỗi lần mở Tổng quan). */
    private void refreshOverview() {
        LocalDateTime now = LocalDateTime.now();
        LocalTime nt = now.toLocalTime();

        List<Sched> inShift = new ArrayList<>();
        for (Sched s : week(0)) {
            if ("CONFIRMED".equals(s.status) && s.date.equals(today)
                    && !s.start.isAfter(nt) && nt.isBefore(s.end)) inShift.add(s);
        }
        inShift.sort(Comparator.comparing((Sched s) -> s.start).thenComparing(s -> s.empId));

        inShiftPanel.removeAll();
        if (inShift.isEmpty()) {
            inShiftPanel.add(UIStyle.panel(new BorderLayout()));
            JPanel none = UIStyle.panel(new BorderLayout());
            none.add(UIStyle.muted("Hiện không có nhân viên trong ca."), BorderLayout.NORTH);
            inShiftPanel.add(none);
            inShiftPanel.add(UIStyle.panel(new BorderLayout()));
        } else if (inShift.size() <= 3) {
            for (Sched s : inShift) inShiftPanel.add(staffCard(s));
            for (int i = inShift.size(); i < 3; i++) inShiftPanel.add(UIStyle.panel(new BorderLayout()));
        } else {
            inShiftPanel.add(staffCard(inShift.get(0)));
            inShiftPanel.add(staffCard(inShift.get(1)));
            JPanel more = UIStyle.panel(new BorderLayout());
            more.add(UIStyle.bold("+ " + (inShift.size() - 2) + " người khác"), BorderLayout.NORTH);
            more.add(UIStyle.muted("Đang làm cùng lúc: " + inShift.size()), BorderLayout.CENTER);
            inShiftPanel.add(more);
        }

        List<Sched> upcoming = new ArrayList<>();
        for (int off = 0; off <= 1; off++) {
            for (Sched s : week(off)) {
                if ("CONFIRMED".equals(s.status) && LocalDateTime.of(s.date, s.start).isAfter(now)) upcoming.add(s);
            }
        }
        upcoming.sort(Comparator.comparing((Sched s) -> LocalDateTime.of(s.date, s.start)).thenComparing(s -> s.empId));

        nextPanel.removeAll();
        for (int i = 0; i < Math.min(6, upcoming.size()); i++) nextPanel.add(nextRow(upcoming.get(i)));
        if (upcoming.isEmpty()) nextPanel.add(UIStyle.muted("Chưa có ca tiếp theo."));

        inShiftPanel.revalidate();
        inShiftPanel.repaint();
        nextPanel.revalidate();
        nextPanel.repaint();
    }

    private JPanel staffCard(Sched s) {
        Emp e = empById(s.empId);
        String name = e == null ? s.empId : e.name;
        JPanel info = UIStyle.panel(null);
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        info.add(UIStyle.bold(name));
        info.add(UIStyle.muted(e == null ? "" : e.position));
        info.add(Box.createVerticalStrut(6));
        info.add(UIStyle.muted("Mã số: " + s.empId));
        info.add(UIStyle.muted(s.shift + " " + DemoData.range(s.start, s.end)));

        JPanel p = new JPanel(new BorderLayout(10, 0));
        p.setBackground(new Color(0xFAFAFA));
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIStyle.BORDER), new EmptyBorder(10, 10, 10, 10)));
        p.add(new UIStyle.Avatar(DemoData.lastWord(name), 44, UIStyle.PRIMARY, Color.WHITE), BorderLayout.WEST);
        p.add(info, BorderLayout.CENTER);
        return p;
    }

    private JPanel nextRow(Sched s) {
        Emp e = empById(s.empId);
        String name = e == null ? s.empId : e.name;
        JPanel info = UIStyle.panel(null);
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        info.add(UIStyle.bold(name));
        info.add(UIStyle.muted(DemoData.dayName(s.date) + " " + s.date.format(DM) + " · "
                + DemoData.range(s.start, s.end)));

        JPanel p = new JPanel(new BorderLayout(10, 0));
        p.setBackground(Color.WHITE);
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, UIStyle.BORDER), new EmptyBorder(6, 6, 6, 6)));
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 56));
        p.add(new UIStyle.Avatar(DemoData.lastWord(name), 32, UIStyle.PRIMARY, Color.WHITE), BorderLayout.WEST);
        p.add(info, BorderLayout.CENTER);
        p.add(UIStyle.muted(e == null ? "" : e.position), BorderLayout.EAST);
        return p;
    }

    // =====================================================================
    // NHÂN SỰ (tab: Danh sách | Chấm công | Lịch sử chỉnh sửa | Lương)
    // =====================================================================
    private JComponent buildStaffPage() {
        staffTabs.setFont(UIStyle.BOLD);
        staffTabs.addTab("Danh sách nhân viên", buildStaffList());
        staffTabs.addTab("Chấm công", buildAttendance());
        staffTabs.addTab("Lịch sử chỉnh sửa", withPadding(
                UIStyle.scroll(UIStyle.table(logModel), "Nhật ký chỉnh sửa chấm công (lưu vết)")));
        staffTabs.addTab("Lương", buildPayroll());
        return staffTabs;
    }

    private JPanel buildStaffList() {
        JPanel p = UIStyle.panel(new BorderLayout(0, 8));
        p.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Sắp xếp theo tên Việt: so sánh theo "tên" (từ cuối), vd "Nguyễn Văn An" -> "An"
        Collator vi = Collator.getInstance(Locale.forLanguageTag("vi"));
        empSorter.setComparator(1, (a, b) -> vi.compare(DemoData.lastWord((String) a), DemoData.lastWord((String) b)));
        empTable.setRowSorter(empSorter);

        UIStyle.onChange(txtSearch, this::applyEmpFilter);
        cbPos.addActionListener(e -> applyEmpFilter());
        cbGender.addActionListener(e -> applyEmpFilter());
        cbStatus.addActionListener(e -> applyEmpFilter());
        cbSort.addActionListener(e -> empSorter.setSortKeys(List.of(
                new RowSorter.SortKey(cbSort.getSelectedIndex() == 0 ? 0 : 1, SortOrder.ASCENDING))));

        JPanel filters = UIStyle.row(new JLabel("Tìm tên:"), txtSearch,
                new JLabel("Chức vụ:"), cbPos, new JLabel("Giới tính:"), cbGender,
                new JLabel("Trạng thái:"), cbStatus, cbSort);
        JPanel actions = UIStyle.row(
                UIStyle.button("Thêm", () -> showEmpDialog(-1)),
                UIStyle.button("Sửa", this::editSelectedEmp),
                UIStyle.button("Cho nghỉ việc", UIStyle.MUTED, new Color(0x4B5563), () -> setActive(false)),
                UIStyle.button("Cho đi làm lại", new Color(0x16A34A), new Color(0x15803D), () -> setActive(true)),
                UIStyle.muted("Nghỉ việc sẽ khóa tài khoản (trigger), không xóa dữ liệu"));

        JPanel top = UIStyle.panel(new GridLayout(2, 1));
        top.add(filters);
        top.add(actions);

        p.add(top, BorderLayout.NORTH);
        p.add(UIStyle.plainScroll(empTable), BorderLayout.CENTER);
        return p;
    }

    private void applyEmpFilter() {
        List<RowFilter<Object, Object>> fs = new ArrayList<>();
        String kw = txtSearch.getText().trim();
        if (!kw.isEmpty()) fs.add(RowFilter.regexFilter("(?iu)" + Pattern.quote(kw), 1));
        if (cbPos.getSelectedIndex() > 0)
            fs.add(RowFilter.regexFilter("^" + Pattern.quote((String) cbPos.getSelectedItem()) + "$", 3));
        if (cbGender.getSelectedIndex() > 0)
            fs.add(RowFilter.regexFilter("^" + Pattern.quote((String) cbGender.getSelectedItem()) + "$", 2));
        if (cbStatus.getSelectedIndex() < 2)
            fs.add(RowFilter.regexFilter("^" + Pattern.quote((String) cbStatus.getSelectedItem()) + "$", 6));
        empSorter.setRowFilter(fs.isEmpty() ? null : RowFilter.andFilter(fs));
    }

    private void editSelectedEmp() {
        int r = selected(empTable);
        if (r >= 0) showEmpDialog(r);
    }

    private void setActive(boolean active) {
        int r = selected(empTable);
        if (r < 0) return;
        Emp e = emps.get(r);
        if (e.active == active) { warn(active ? "Nhân viên này đang làm việc." : "Nhân viên này đã nghỉ việc."); return; }
        if (!active && e.id.equals(acc.getEmployeeId())) { warn("Không thể cho chính mình nghỉ việc."); return; }
        int ok = JOptionPane.showConfirmDialog(this,
                (active ? "Cho đi làm lại: " : "Cho nghỉ việc: ") + e.name + "?", "Xác nhận", JOptionPane.YES_NO_OPTION);
        if (ok != JOptionPane.YES_OPTION) return;
        // TODO: UPDATE Employee SET status = 'RESIGNED'/'ACTIVE' (trigger tự khóa/mở Account)
        e.active = active;
        reloadEmpModel();
        refreshPayroll();
    }

    private void showEmpDialog(int idx) {
        boolean edit = idx >= 0;
        Emp cur = edit ? emps.get(idx) : null;

        JTextField code = new JTextField(edit ? cur.id : String.format("E26_%03d", nextNum));
        code.setEditable(false);
        JTextField name = new JTextField(edit ? cur.name : "");
        JComboBox<String> gender = new JComboBox<>(DemoData.GENDER_VI);
        JComboBox<String> pos = new JComboBox<>(DemoData.positionNames());
        JLabel salary = UIStyle.muted("");
        JTextField phone = new JTextField(edit ? cur.phone : "");
        JTextField mail = new JTextField(edit ? cur.email : "");
        JTextField addr = new JTextField(edit ? cur.address : "");
        if (edit) {
            gender.setSelectedItem(DemoData.genderVi(cur.gender));
            pos.setSelectedItem(cur.position);
        }
        Runnable showSalary = () -> {
            DemoData.Position p = DemoData.position((String) pos.getSelectedItem());
            salary.setText(p.fixed() ? "Full-time · " + DemoData.money(p.rate) + "/tháng"
                                     : "Part-time · " + DemoData.money(p.rate) + "/giờ");
        };
        pos.addActionListener(e -> showSalary.run());
        showSalary.run();

        JPanel f = new JPanel(new GridLayout(0, 2, 8, 8));
        f.add(new JLabel("Mã NV:"));     f.add(code);
        f.add(new JLabel("Họ tên:"));    f.add(name);
        f.add(new JLabel("Giới tính:")); f.add(gender);
        f.add(new JLabel("Chức vụ:"));   f.add(pos);
        f.add(new JLabel("Loại lương:")); f.add(salary);
        f.add(new JLabel("SĐT:"));       f.add(phone);
        f.add(new JLabel("Email:"));     f.add(mail);
        f.add(new JLabel("Địa chỉ:"));   f.add(addr);

        int ok = JOptionPane.showConfirmDialog(this, f, edit ? "Sửa nhân viên" : "Thêm nhân viên",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (ok != JOptionPane.OK_OPTION) return;

        if (name.getText().isBlank()) { warn("Họ tên không được để trống."); return; }
        if (!phone.getText().trim().matches("0\\d{9}")) { warn("SĐT phải gồm 10 số, bắt đầu bằng 0."); return; }
        if (!mail.getText().isBlank() && !mail.getText().contains("@")) { warn("Email không hợp lệ."); return; }

        String g = DemoData.GENDER_CODES[gender.getSelectedIndex()];
        if (edit) {
            cur.name = name.getText().trim();
            cur.gender = g;
            cur.position = (String) pos.getSelectedItem();
            cur.phone = phone.getText().trim();
            cur.email = mail.getText().trim();
            cur.address = addr.getText().trim();
            // TODO: UPDATE Employee (đổi chức vụ thì cập nhật cả salary_type, salary_rate)
        } else {
            // TODO: INSERT Employee + Account (mật khẩu mặc định, must_change_password = 1)
            emps.add(new Emp(code.getText(), name.getText().trim(), g, (String) pos.getSelectedItem(),
                    phone.getText().trim(), mail.getText().trim(), addr.getText().trim()));
            nextNum++;
        }
        reloadEmpModel();
        refreshPayroll();
    }

    private void reloadEmpModel() {
        empModel.setRowCount(0);
        for (Emp e : emps) {
            empModel.addRow(new Object[]{e.id, e.name, DemoData.genderVi(e.gender), e.position, e.phone, e.address,
                    e.active ? "Đang làm" : "Đã nghỉ"});
        }
    }

    // ----- Chấm công -----
    private JPanel buildAttendance() {
        attTable.setRowSorter(attSorter);
        UIStyle.onChange(txtAtt, () -> {
            String kw = txtAtt.getText().trim();
            if (kw.isEmpty()) attSorter.setRowFilter(null);
            else attSorter.setRowFilter(RowFilter.regexFilter("(?iu)" + Pattern.quote(kw), 0, 1));
        });
        JPanel p = UIStyle.panel(new BorderLayout(0, 8));
        p.setBorder(new EmptyBorder(10, 10, 10, 10));
        p.add(UIStyle.row(new JLabel("Lọc (mã/tên):"), txtAtt,
                UIStyle.button("Sửa chấm công", this::editAttendance),
                UIStyle.button("Ghi nhận đi trễ", () -> quickMark("Đi trễ")),
                UIStyle.button("Ghi nhận về sớm", () -> quickMark("Về sớm")),
                UIStyle.button("Ghi nhận vắng", () -> quickMark("Vắng không phép"))), BorderLayout.NORTH);
        p.add(UIStyle.plainScroll(attTable), BorderLayout.CENTER);
        return p;
    }

    private void editAttendance() {
        int r = selected(attTable);
        if (r < 0) return;

        JTextField tIn = new JTextField(str(attModel, r, 4));
        JTextField tOut = new JTextField(str(attModel, r, 5));
        JComboBox<String> cbSt = new JComboBox<>(DemoData.ATT_VI);
        cbSt.setSelectedItem(str(attModel, r, 6));
        JTextField tReason = new JTextField();

        JPanel f = new JPanel(new GridLayout(0, 2, 8, 8));
        f.add(new JLabel("Ca đăng ký:"));              f.add(new JLabel(str(attModel, r, 3)));
        f.add(new JLabel("Giờ vào (HH:mm hoặc --):")); f.add(tIn);
        f.add(new JLabel("Giờ ra (HH:mm hoặc --):"));  f.add(tOut);
        f.add(new JLabel("Trạng thái:"));              f.add(cbSt);
        f.add(new JLabel("Lý do sửa (bắt buộc):"));    f.add(tReason);

        int ok = JOptionPane.showConfirmDialog(this, f,
                "Sửa chấm công " + str(attModel, r, 1) + " - " + str(attModel, r, 2),
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (ok != JOptionPane.OK_OPTION) return;

        String in = tIn.getText().trim(), out = tOut.getText().trim();
        if (!validTime(in) || !validTime(out)) { warn("Giờ không hợp lệ. Dùng định dạng HH:mm hoặc --."); return; }
        if (isTime(in) && isTime(out) && !LocalTime.parse(out).isAfter(LocalTime.parse(in))) {
            warn("Giờ ra phải sau giờ vào.");
            return;
        }
        if (tReason.getText().isBlank()) { warn("Bắt buộc nhập lý do sửa."); return; }
        applyAttendanceChange(r, in, out, (String) cbSt.getSelectedItem(), tReason.getText().trim());
    }

    private void quickMark(String status) {
        int r = selected(attTable);
        if (r < 0) return;
        String reason = JOptionPane.showInputDialog(this, "Lý do ghi nhận \"" + status + "\":");
        if (reason == null) return;
        if (reason.isBlank()) { warn("Bắt buộc nhập lý do."); return; }
        boolean absent = "Vắng không phép".equals(status);
        applyAttendanceChange(r,
                absent ? "--" : str(attModel, r, 4),
                absent ? "--" : str(attModel, r, 5),
                status, reason.trim());
    }

    /** Sửa dòng chấm công và GHI LOG (người sửa, trước/sau, lý do). */
    private void applyAttendanceChange(int row, String in, String out, String status, String reason) {
        String before = str(attModel, row, 4) + " – " + str(attModel, row, 5) + " (" + str(attModel, row, 6) + ")";
        attModel.setValueAt(in, row, 4);
        attModel.setValueAt(out, row, 5);
        attModel.setValueAt(status, row, 6);
        // TODO: UPDATE Attendance + INSERT AttendanceLog
        logModel.insertRow(0, new Object[]{
                LocalDateTime.now().format(DT), acc.getEmployeeId(),
                str(attModel, row, 0) + " · " + str(attModel, row, 2) + " · " + str(attModel, row, 3),
                before, in + " – " + out + " (" + status + ")", reason});
        refreshPayroll();
    }

    // ----- Lương -----
    private JPanel buildPayroll() {
        for (int i = 0; i < 3; i++) {
            YearMonth ym = YearMonth.from(today).minusMonths(i);
            months.add(ym);
            cbMonth.addItem(String.format("%02d/%d", ym.getMonthValue(), ym.getYear()));
        }
        closed.add(months.get(1));
        closed.add(months.get(2));
        cbMonth.addActionListener(e -> refreshPayroll());

        JPanel p = UIStyle.panel(new BorderLayout(0, 8));
        p.setBorder(new EmptyBorder(10, 10, 10, 10));
        p.add(UIStyle.row(new JLabel("Kỳ lương (tháng):"), cbMonth, lblPayState,
                UIStyle.button("Chốt lương", this::closePayroll),
                UIStyle.button("Xem chấm công của NV đã chọn", this::jumpToAttendance),
                UIStyle.muted("Lương cố định = mức chức vụ; theo giờ = giờ × lương/giờ; thưởng DT chỉ tính khi chốt")),
                BorderLayout.NORTH);
        p.add(UIStyle.plainScroll(payTable), BorderLayout.CENTER);
        return p;
    }

    private void jumpToAttendance() {
        int r = selected(payTable);
        if (r < 0) return;
        txtAtt.setText(str(payModel, r, 0));
        staffTabs.setSelectedIndex(1);
    }

    private YearMonth selectedMonth() {
        return months.get(Math.max(0, cbMonth.getSelectedIndex()));
    }

    private void closePayroll() {
        YearMonth ym = selectedMonth();
        if (closed.contains(ym)) { info("Kỳ lương này đã chốt."); return; }
        int ok = JOptionPane.showConfirmDialog(this,
                "Chốt lương " + String.format("%02d/%d", ym.getMonthValue(), ym.getYear())
                        + "?\nSau khi chốt, mức lương và giờ công được lưu vào bảng Payroll.",
                "Chốt lương", JOptionPane.OK_CANCEL_OPTION);
        if (ok != JOptionPane.OK_OPTION) return;
        // TODO: EXEC sp_RefreshMonthlyHours, rồi INSERT Payroll cho từng nhân viên
        closed.add(ym);
        refreshPayroll();
    }

    /** Tháng hiện tại: giờ lấy từ bảng chấm công. Tháng trước: lấy từ MonthlyHours (mẫu). */
    private void refreshPayroll() {
        if (months.isEmpty()) return;
        YearMonth ym = selectedMonth();
        int ago = months.indexOf(ym);
        boolean isClosed = closed.contains(ym);
        lblPayState.setText(isClosed ? "● Đã chốt" : "● Chưa chốt");
        lblPayState.setForeground(isClosed ? new Color(0x16A34A) : UIStyle.PRIMARY);

        payModel.setRowCount(0);
        for (Emp e : emps) {
            double h = ago == 0 ? attHours(e.id, ym) : DemoData.hoursPrev(e, ago);
            if (!e.active && h <= 0) continue;
            long base = DemoData.baseSalary(e, h);
            long allow = e.pos().allowance;
            long bonus = isClosed ? DemoData.bonus(e) : 0;
            payModel.addRow(new Object[]{e.id, e.name, e.position, e.fixed() ? "Cố định" : "Theo giờ",
                    String.format(Locale.ROOT, "%.1f", h), DemoData.money(base), DemoData.money(allow),
                    DemoData.money(bonus), DemoData.money(base + allow + bonus)});
        }
    }

    private double attHours(String id, YearMonth ym) {
        double mins = 0;
        for (int i = 0; i < attModel.getRowCount(); i++) {
            if (!id.equals(str(attModel, i, 0))) continue;
            String in = str(attModel, i, 4), out = str(attModel, i, 5);
            if (!isTime(in) || !isTime(out)) continue;
            try {
                if (!YearMonth.from(LocalDate.parse(str(attModel, i, 2), D)).equals(ym)) continue;
            } catch (RuntimeException ex) {
                continue;
            }
            mins += Duration.between(LocalTime.parse(in), LocalTime.parse(out)).toMinutes();
        }
        return Math.round(mins / 60.0 * 10) / 10.0;
    }

    // =====================================================================
    // LỊCH LÀM (lưới tuần: hàng = ca, cột = ngày, mỗi ô liệt kê "Tên giờ-giờ")
    // =====================================================================
    private JComponent buildSchedulePage() {
        JPanel p = UIStyle.panel(new BorderLayout(0, 8));
        p.add(UIStyle.row(
                UIStyle.button("◀ Tuần trước", () -> shiftWeek(-1)), lblWeek,
                UIStyle.button("Tuần sau ▶", () -> shiftWeek(1)),
                UIStyle.muted("Tối đa " + DemoData.MAX_CONCURRENT + " người làm cùng lúc")), BorderLayout.NORTH);
        JTable grid = UIStyle.grid(gridModel);
        grid.setRowHeight(170);
        p.add(UIStyle.card("Lịch làm chính thức (đã chốt)", UIStyle.plainScroll(grid)), BorderLayout.CENTER);
        return p;
    }

    private void shiftWeek(int delta) {
        weekOffset += delta;
        loadGrid();
    }

    /** Lịch cả cửa hàng của tuần (offset 0 = tuần này). Tuần sau (1) có các ca "chờ chốt". */
    private List<Sched> week(int off) {
        return weeks.computeIfAbsent(off, k -> {
            List<Sched> l = new ArrayList<>();
            if (k > 1) return l;
            LocalDate mon = monday.plusWeeks(k);
            for (Emp e : emps) if (e.active) l.addAll(DemoData.scheduleFor(e, mon, k == 1));
            return l;
        });
    }

    private int offsetOf(LocalDate d) {
        return (int) ChronoUnit.WEEKS.between(monday, d.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)));
    }

    private void loadGrid() {
        LocalDate mon = monday.plusWeeks(weekOffset);
        lblWeek.setText("Tuần " + mon.format(DM) + " – " + mon.plusDays(6).format(D));
        gridModel.setRowCount(0);
        List<Sched> wk = week(weekOffset);
        for (Shift sh : DemoData.SHIFTS) {
            Object[] row = new Object[DemoData.DAYS.length + 1];
            row[0] = sh.name;
            for (int d = 0; d < DemoData.DAYS.length; d++) {
                LocalDate date = mon.plusDays(d);
                List<Sched> cell = new ArrayList<>();
                for (Sched s : wk) {
                    if ("CONFIRMED".equals(s.status) && s.date.equals(date) && sh.name.equals(s.shift)) cell.add(s);
                }
                cell.sort(Comparator.comparing((Sched s) -> s.start).thenComparing(s -> s.empId));
                List<String> parts = new ArrayList<>();
                for (Sched s : cell) {
                    Emp e = empById(s.empId);
                    parts.add(DemoData.lastWord(e == null ? s.empId : e.name) + " "
                            + DemoData.shortRange(s.start, s.end));
                }
                row[d + 1] = String.join(", ", parts);
            }
            gridModel.addRow(row);
        }
    }

    /** Số người ĐÃ CHỐT làm cùng lúc nhiều nhất trong khoảng [from, to) của ngày đó (bỏ qua 'ignore'). */
    private int peak(List<Sched> wk, LocalDate date, LocalTime from, LocalTime to, Sched ignore) {
        List<LocalTime> pts = new ArrayList<>();
        pts.add(from);
        for (Sched s : wk) {
            if (s != ignore && "CONFIRMED".equals(s.status) && s.date.equals(date)
                    && s.start.isAfter(from) && s.start.isBefore(to)) pts.add(s.start);
        }
        int max = 0;
        for (LocalTime t : pts) {
            int c = 0;
            for (Sched s : wk) {
                if (s != ignore && "CONFIRMED".equals(s.status) && s.date.equals(date)
                        && !s.start.isAfter(t) && t.isBefore(s.end)) c++;
            }
            max = Math.max(max, c);
        }
        return max;
    }

    // =====================================================================
    // DUYỆT LỊCH LÀM (chốt ca đăng ký + duyệt đơn nghỉ/đổi ca)
    // =====================================================================
    private JComponent buildApprovePage() {
        JTable regTable = UIStyle.table(regModel);
        JPanel regBody = UIStyle.panel(new BorderLayout(0, 8));
        regBody.add(UIStyle.row(
                UIStyle.button("Chốt ca", () -> confirmShift(regTable)),
                UIStyle.button("Từ chối", () -> rejectShift(regTable)),
                UIStyle.muted("Khi chốt có thể chỉnh giờ (part-time); hệ thống kiểm tra giới hạn người cùng lúc")),
                BorderLayout.NORTH);
        regBody.add(UIStyle.plainScroll(regTable), BorderLayout.CENTER);

        JPanel p = UIStyle.panel(new GridLayout(2, 1, 0, 12));
        p.add(UIStyle.card("Nhân viên đăng ký ca tuần sau", regBody));
        p.add(UIStyle.card("Đơn xin nghỉ / đổi ca", leavePanel()));
        return p;
    }

    /** Hiện các đăng ký tuần sau đang chờ chốt + những dòng vừa xử lý. */
    private void reloadReg() {
        regRows.clear();
        regModel.setRowCount(0);
        List<Sched> src = new ArrayList<>();
        for (Sched s : week(1)) if ("REGISTERED".equals(s.status) || processed.contains(s)) src.add(s);
        src.sort(Comparator.comparing((Sched s) -> s.date).thenComparing(s -> s.start).thenComparing(s -> s.empId));
        for (Sched s : src) {
            Emp e = empById(s.empId);
            regRows.add(s);
            regModel.addRow(new Object[]{s.empId, e == null ? "" : e.name,
                    e != null && e.fixed() ? "Full-time" : "Part-time",
                    DemoData.dayName(s.date) + " " + s.date.format(D), s.shift,
                    DemoData.range(s.start, s.end), DemoData.schedVi(s.status)});
        }
    }

    private void confirmShift(JTable regTable) {
        int r = selected(regTable);
        if (r < 0) return;
        Sched s = regRows.get(r);
        if (!"REGISTERED".equals(s.status)) { warn("Đăng ký này đã được xử lý."); return; }
        Emp e = empById(s.empId);
        boolean fixed = e == null || e.fixed();
        String name = e == null ? s.empId : e.name;

        Shift sh = DemoData.shift(s.shift);
        String[] ts = DemoData.times(sh);
        JComboBox<String> cbFrom = new JComboBox<>(ts), cbTo = new JComboBox<>(ts);
        cbFrom.setSelectedItem(DemoData.hm(s.start));
        cbTo.setSelectedItem(DemoData.hm(s.end));
        cbFrom.setEnabled(!fixed);
        cbTo.setEnabled(!fixed);

        JPanel f = new JPanel(new GridLayout(0, 2, 8, 8));
        f.add(new JLabel("Nhân viên:")); f.add(new JLabel(name + (fixed ? " (full-time)" : " (part-time)")));
        f.add(new JLabel("Ngày:"));      f.add(new JLabel(DemoData.dayName(s.date) + " " + s.date.format(D)));
        f.add(new JLabel("Ca:"));        f.add(new JLabel(s.shift + " (" + DemoData.range(sh.start, sh.end) + ")"));
        f.add(new JLabel("Giờ làm từ:")); f.add(cbFrom);
        f.add(new JLabel("Giờ làm đến:")); f.add(cbTo);

        int ok = JOptionPane.showConfirmDialog(this, f, "Chốt ca", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);
        if (ok != JOptionPane.OK_OPTION) return;

        LocalTime from = LocalTime.parse((String) cbFrom.getSelectedItem());
        LocalTime to = LocalTime.parse((String) cbTo.getSelectedItem());
        String err = DemoData.checkRange(fixed, s.shift, from, to);
        if (err != null) { warn(err); return; }

        int pk = peak(week(1), s.date, from, to, s);
        if (pk + 1 > DemoData.MAX_CONCURRENT) {
            warn("Khung giờ " + DemoData.range(from, to) + " đã có " + pk + " người làm cùng lúc "
                    + "(tối đa " + DemoData.MAX_CONCURRENT + "). Hãy rút ngắn giờ hoặc từ chối.");
            return;
        }
        int ok2 = JOptionPane.showConfirmDialog(this,
                "Chốt " + name + " làm " + s.shift + " " + DemoData.dayName(s.date) + " " + s.date.format(DM)
                        + " (" + DemoData.range(from, to) + ")?\n"
                        + "Sau khi chốt, nhân viên không được tự ý bỏ ca (phải xin đổi ca/nghỉ).",
                "Chốt ca", JOptionPane.OK_CANCEL_OPTION);
        if (ok2 != JOptionPane.OK_OPTION) return;

        // TODO: UPDATE WorkSchedule SET start_time, end_time, status = 'CONFIRMED'
        s.start = from;
        s.end = to;
        s.status = "CONFIRMED";
        processed.add(s);
        reloadReg();
        loadGrid();
    }

    private void rejectShift(JTable regTable) {
        int r = selected(regTable);
        if (r < 0) return;
        Sched s = regRows.get(r);
        if (!"REGISTERED".equals(s.status)) { warn("Đăng ký này đã được xử lý."); return; }
        // TODO: UPDATE WorkSchedule SET status = 'CANCELLED'
        s.status = "CANCELLED";
        processed.add(s);
        reloadReg();
    }

    // ----- Đơn nghỉ / đổi ca -----
    /** Bảng đơn nghỉ/đổi ca + nút Duyệt/Từ chối (dùng chung cho Tổng quan và Duyệt lịch làm). */
    private JPanel leavePanel() {
        JTable t = UIStyle.table(leaveModel);
        JPanel p = UIStyle.panel(new BorderLayout(0, 8));
        p.add(UIStyle.row(
                UIStyle.button("Duyệt", new Color(0x16A34A), new Color(0x15803D), () -> decide(t, true)),
                UIStyle.button("Từ chối", UIStyle.MUTED, new Color(0x4B5563), () -> decide(t, false))),
                BorderLayout.NORTH);
        p.add(UIStyle.plainScroll(t), BorderLayout.CENTER);
        return p;
    }

    /** Duyệt: nghỉ -> hủy ca; đổi ca -> sang ca/giờ mới. Từ chối: chỉ đổi trạng thái. */
    private void decide(JTable t, boolean approve) {
        int r = selected(t);
        if (r < 0) return;
        Leave l = leaves.get(r);
        if (!"PENDING".equals(l.status)) { warn("Đơn này đã được xử lý."); return; }

        if (approve) {
            for (Sched s : week(offsetOf(l.date))) {
                if (!s.empId.equals(l.empId) || !s.date.equals(l.date) || !s.active()) continue;
                if (l.shift != null && !l.shift.equals(s.shift)) continue;
                if ("LEAVE".equals(l.type)) {
                    s.status = "CANCELLED";
                } else {
                    s.shift = l.newShift;
                    s.start = l.newStart;
                    s.end = l.newEnd;
                }
            }
        }
        // TODO: UPDATE LeaveRequest (status, reviewed_by, reviewed_at) + cập nhật WorkSchedule
        l.status = approve ? "APPROVED" : "REJECTED";
        reloadLeaves();
        loadGrid();
        reloadReg();
    }

    private static String leaveVi(String s) {
        switch (s) {
            case "APPROVED": return "Đã duyệt";
            case "REJECTED": return "Từ chối";
            default: return "Chờ duyệt";
        }
    }

    private void reloadLeaves() {
        leaveModel.setRowCount(0);
        for (Leave l : leaves) {
            Emp e = empById(l.empId);
            String content = "LEAVE".equals(l.type)
                    ? (l.shift == null ? "Nghỉ cả ngày" : "Nghỉ " + l.shift.toLowerCase())
                    : "→ " + l.newShift + " " + DemoData.range(l.newStart, l.newEnd);
            leaveModel.addRow(new Object[]{l.empId, e == null ? "" : e.name,
                    "LEAVE".equals(l.type) ? "Xin nghỉ" : "Đổi ca", l.date.format(D),
                    l.shift == null ? "Cả ngày" : l.shift, content, l.reason, leaveVi(l.status)});
        }
    }

    // =====================================================================
    // THÔNG BÁO (tab: Hộp thư | Đã gửi)
    // =====================================================================
    private JComponent buildNotiPage() {
        List<String> inbox = new ArrayList<>();
        for (Leave l : leaves) {
            if (!"PENDING".equals(l.status)) continue;
            Emp e = empById(l.empId);
            String who = "<b>" + (e == null ? l.empId : e.name) + "</b>";
            inbox.add("LEAVE".equals(l.type)
                    ? who + " xin nghỉ ngày " + l.date.format(D) + " (" + l.reason + ")."
                    : who + " xin đổi ca ngày " + l.date.format(D) + ": " + l.shift + " → " + l.newShift + ".");
        }

        JPanel sent = UIStyle.panel(new BorderLayout(0, 8));
        sent.setBorder(new EmptyBorder(10, 10, 10, 10));
        sent.add(UIStyle.row(UIStyle.button("Soạn thông báo", this::composeNotification)), BorderLayout.NORTH);
        sent.add(UIStyle.plainScroll(UIStyle.table(sentModel)), BorderLayout.CENTER);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(UIStyle.BOLD);
        tabs.addTab("Hộp thư", withPadding(notificationPage("noti", inbox)));
        tabs.addTab("Đã gửi", sent);
        return tabs;
    }

    private void composeNotification() {
        JComboBox<String> to = new JComboBox<>();
        to.addItem("Tất cả nhân viên");
        for (Emp e : emps) if (e.active && !e.id.equals(acc.getEmployeeId())) to.addItem(e.id + " - " + e.name);
        JTextField title = new JTextField();
        JTextArea body = new JTextArea(5, 24);
        body.setLineWrap(true);
        body.setWrapStyleWord(true);

        JPanel f = new JPanel(new BorderLayout(0, 8));
        JPanel head = new JPanel(new GridLayout(0, 2, 8, 8));
        head.add(new JLabel("Gửi đến:"));  head.add(to);
        head.add(new JLabel("Tiêu đề:"));  head.add(title);
        f.add(head, BorderLayout.NORTH);
        f.add(new JLabel("Nội dung:"), BorderLayout.CENTER);
        f.add(new JScrollPane(body), BorderLayout.SOUTH);

        int ok = JOptionPane.showConfirmDialog(this, f, "Soạn thông báo", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);
        if (ok != JOptionPane.OK_OPTION) return;
        if (title.getText().isBlank() || body.getText().isBlank()) { warn("Nhập đủ tiêu đề và nội dung."); return; }
        // TODO: INSERT Notification (sender_id, receiver_id NULL = tất cả)
        sentModel.insertRow(0, new Object[]{LocalDateTime.now().format(DT), (String) to.getSelectedItem(),
                title.getText().trim(), body.getText().trim()});
    }

    // =====================================================================
    // DỮ LIỆU GIẢ + TIỆN ÍCH
    // =====================================================================
    private void loadMockData() {
        reloadEmpModel();

        // Chấm công: các ngày đã qua của nhân viên đang làm, mới nhất lên đầu
        List<Object[]> rows = new ArrayList<>();
        List<LocalDate> dates = new ArrayList<>();
        for (Emp e : emps) {
            if (!e.active) continue;
            for (Att a : DemoData.pastAttendance(e, today)) {
                rows.add(new Object[]{e.id, e.name, a.date.format(D), DemoData.range(a.start, a.end),
                        a.in == null ? "--" : DemoData.hm(a.in), a.out == null ? "--" : DemoData.hm(a.out),
                        DemoData.attVi(a.status)});
                dates.add(a.date);
            }
        }
        Integer[] order = new Integer[rows.size()];
        for (int i = 0; i < order.length; i++) order[i] = i;
        java.util.Arrays.sort(order, (x, y) -> {
            int c = dates.get(y).compareTo(dates.get(x));
            return c != 0 ? c : ((String) rows.get(x)[0]).compareTo((String) rows.get(y)[0]);
        });
        for (int i : order) attModel.addRow(rows.get(i));

        // Đơn nghỉ / đổi ca
        LocalDate nm = monday.plusWeeks(1);
        leaves.add(leave("E26_007", "LEAVE", nm.plusDays(1), "Có lịch thi học phần", "PENDING"));
        leaves.add(leave("E26_003", "LEAVE", nm.plusDays(4), "Việc gia đình", "PENDING"));
        leaves.add(leave("E26_011", "SHIFT_CHANGE", nm.plusDays(3), "Xin đổi ca do bận học", "PENDING"));
        leaves.add(leave("E26_009", "LEAVE", today.minusDays(4), "Bị ốm đột xuất", "APPROVED"));
        leaves.add(leave("E26_005", "SHIFT_CHANGE", today.minusDays(3), "Đổi ca với đồng nghiệp", "APPROVED"));
        leaves.add(leave("E26_012", "LEAVE", today.plusDays(1), "Đi chơi với bạn", "REJECTED"));
        reloadLeaves();

        // Thông báo đã gửi
        for (String[] n : DemoData.NOTIS) {
            sentModel.addRow(new Object[]{n[3], n[0].isEmpty() ? "Tất cả nhân viên" : n[0], n[1], n[2]});
        }
    }

    /** Tạo đơn mẫu; ca hiện tại tra từ lịch của nhân viên, ca mới là ca còn lại. */
    private Leave leave(String empId, String type, LocalDate date, String reason, String status) {
        Leave l = new Leave();
        l.empId = empId;
        l.type = type;
        l.date = date;
        l.reason = reason;
        l.status = status;
        for (Sched s : week(offsetOf(date))) {
            if (s.empId.equals(empId) && s.date.equals(date)) { l.shift = s.shift; break; }
        }
        if ("SHIFT_CHANGE".equals(type)) {
            Emp e = empById(empId);
            boolean toMorning = "Ca chiều".equals(l.shift);
            Shift ns = DemoData.SHIFTS[toMorning ? 0 : 1];
            l.newShift = ns.name;
            l.newStart = ns.start;
            l.newEnd = e != null && e.fixed() ? ns.end : LocalTime.of(toMorning ? 12 : 20, 0);
        }
        return l;
    }

    private Emp empById(String id) {
        for (Emp e : emps) if (e.id.equals(id)) return e;
        return null;
    }

    private JPanel withPadding(JComponent c) {
        JPanel p = UIStyle.panel(new BorderLayout());
        p.setBorder(new EmptyBorder(10, 10, 10, 10));
        p.add(c, BorderLayout.CENTER);
        return p;
    }

    /** Trả về chỉ số dòng trong MODEL (đã quy đổi khi bảng đang lọc/sắp xếp), hoặc -1. */
    private int selected(JTable t) {
        int r = t.getSelectedRow();
        if (r < 0) { warn("Vui lòng chọn một dòng trước."); return -1; }
        return t.convertRowIndexToModel(r);
    }

    private static String str(DefaultTableModel m, int r, int c) {
        Object v = m.getValueAt(r, c);
        return v == null ? "" : v.toString();
    }

    private static boolean isTime(String s) { return s != null && s.matches("\\d{2}:\\d{2}"); }

    private static boolean validTime(String s) { return "--".equals(s) || s.matches("([01]\\d|2[0-3]):[0-5]\\d"); }
}