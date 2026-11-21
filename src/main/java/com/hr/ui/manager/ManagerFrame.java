package com.hr.ui.manager;

import com.hr.dao.OverviewDao;
import com.hr.dao.AttendanceDAO;
import com.hr.dao.EmployeeDAO;
import com.hr.dao.NotificationDAO;
import com.hr.dao.PositionDAO;
import com.hr.dao.SalaryDAO;
import com.hr.dao.ShiftTimeDAO;
import com.hr.dao.WorkScheduleDAO;
import com.hr.model.Account;
import com.hr.model.Attendance;
import com.hr.model.AttendanceLog;
import com.hr.model.Employee;
import com.hr.model.Notification;
import com.hr.model.Position;
import com.hr.model.SalaryRow;
import com.hr.model.ShiftTime;
import com.hr.model.WorkSchedule;
import com.hr.model.enums.AttendanceStatus;
import com.hr.model.enums.EmployeeStatus;
import com.hr.model.enums.Gender;
import com.hr.model.enums.Role;
import com.hr.model.enums.ScheduleStatus;
import com.hr.model.enums.SalaryType;
import com.hr.ui.BaseFrame; // hoặc com.hr.ui.components.BaseFrame nếu đã chuyển
import com.hr.ui.UIStyle;    // hoặc com.hr.ui.components.UIStyle

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.sql.SQLException;
import java.math.BigDecimal;
import java.text.Collator;
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
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

public class ManagerFrame extends BaseFrame {
    private static final DateTimeFormatter D = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DM = DateTimeFormatter.ofPattern("dd/MM");
    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final LocalDate today = LocalDate.now();
    private final LocalDate monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    // --- DAO + dữ liệu đọc từ SQL (Nhân sự) ---
    private final EmployeeDAO empDao = new EmployeeDAO();
    private final PositionDAO posDao = new PositionDAO();
    private final AttendanceDAO attDao = new AttendanceDAO();
    private final SalaryDAO salaryDao = new SalaryDAO();
    private final WorkScheduleDAO workDao = new WorkScheduleDAO();
    private final ShiftTimeDAO shiftDao = new ShiftTimeDAO();
    private final NotificationDAO notiDao = new NotificationDAO();
    private final List<Employee> staff = new ArrayList<>();        // hàng i của empModel = staff[i]
    private final List<Position> positions = new ArrayList<>();
    private final List<Attendance> attRows = new ArrayList<>();    // hàng i của attModel = attRows[i]

    // --- Nhân sự ---
    private final JTabbedPane staffTabs = new JTabbedPane();
    private final DefaultTableModel empModel = UIStyle.model("Mã NV", "Họ tên", "Giới tính", "Chức vụ", "SĐT", "Địa chỉ", "Trạng thái");
    private final JTable empTable = UIStyle.table(empModel);
    private final TableRowSorter<DefaultTableModel> empSorter = new TableRowSorter<>(empModel);
    private final JTextField txtSearch = new JTextField(14);
    private final JComboBox<String> cbPos = new JComboBox<>(new String[]{"Tất cả"});
    private final JComboBox<String> cbGender = new JComboBox<>(new String[]{"Tất cả", "Nam", "Nữ", "Khác"});
    private final JComboBox<String> cbStatus = new JComboBox<>(new String[]{"Đang làm", "Đã nghỉ", "Tất cả"});
    private final JComboBox<String> cbSort = new JComboBox<>(new String[]{"Sắp xếp: Mã NV", "Sắp xếp: Tên"});

    // --- Chấm công / Lương ---
    private final DefaultTableModel attModel = UIStyle.model("Mã NV", "Họ tên", "Ngày", "Ca đăng ký", "Giờ vào", "Giờ ra", "Trạng thái");
    private final JTable attTable = UIStyle.table(attModel);
    private final TableRowSorter<DefaultTableModel> attSorter = new TableRowSorter<>(attModel);
    private final JTextField txtAtt = new JTextField(14);
    private final DefaultTableModel logModel = UIStyle.model("Thời gian", "Người sửa", "Bản ghi", "Trước khi sửa", "Sau khi sửa", "Lý do");
    private final DefaultTableModel payModel = UIStyle.model("Mã NV", "Họ tên", "Chức vụ", "Loại", "Tổng giờ", "Lương cơ bản", "Phụ cấp", "Thưởng DT", "Thực lĩnh");
    private final JTable payTable = UIStyle.table(payModel);
    private final List<YearMonth> months = new ArrayList<>();
    private final JComboBox<String> cbMonth = new JComboBox<>();
    private final JLabel lblPayState = UIStyle.bold("");

    // --- Lịch làm ---
    private final DefaultTableModel gridModel = UIStyle.model(prepend("Ca", DAYS));
    private final JLabel lblWeek = UIStyle.bold("");
    private int weekOffset = 0;

    // --- Duyệt lịch làm ---
    private final DefaultTableModel regModel = UIStyle.model("Mã NV", "Họ tên", "Loại", "Ngày", "Ca", "Giờ làm", "Trạng thái");
    private final List<WorkSchedule> regRows = new ArrayList<>();
    private final Set<Integer> processed = new HashSet<>();   // schedule_id vừa xử lý trong phiên

    private final OverviewDao dao = new OverviewDao();
    private final List<OverviewDao.Leave> leaves = new ArrayList<>();
    private final DefaultTableModel leaveModel = UIStyle.model("Mã NV", "Họ tên", "Loại đơn", "Ngày", "Ca", "Nội dung", "Lý do", "Trạng thái");

    // --- Tổng quan ---
    private static final String STORE_NAME = "Highland 270C Bạch Đằng";
    private static final DateTimeFormatter HM = DateTimeFormatter.ofPattern("HH:mm");
    private final JLabel lblStaffInfo = UIStyle.muted("");
    private final JPanel statsPanel = UIStyle.panel(new GridLayout(2, 2, 10, 10));
    private final JPanel piePanel = UIStyle.panel(new BorderLayout());
    private final JPanel nextPanel = UIStyle.panel(null);
    private final JPanel revenueHolder = UIStyle.panel(new BorderLayout());

    // --- Thông báo ---
    private final JPanel inboxHolder = UIStyle.panel(new BorderLayout());
    private final DefaultTableModel sentModel = UIStyle.model("Thời gian", "Người nhận", "Tiêu đề", "Nội dung");

    public ManagerFrame(Account acc) {
        super(acc, "Cửa hàng trưởng");

        addMenu("overview", "Tổng quan");
        addMenu("staff", "Nhân sự");
        addGroup("Lịch làm", new String[]{"schedule", "approve"}, new String[]{"Lịch làm", "Duyệt lịch làm"});
        addMenu("noti", "Thông báo");

        addPage("overview", buildOverview(), this::refreshOverview);
        addPage("staff", buildStaffPage(), this::reloadStaff);
        addPage("schedule", buildSchedulePage(), this::loadGrid);
        addPage("approve", buildApprovePage(), () -> { reloadLeaves(); reloadReg(); });
        addPage("noti", buildNotiPage(), this::reloadNoti);

        start("overview");
    }

    // TỔNG QUAN: 3 hàng cân đối
    private JComponent buildOverview() {
        nextPanel.setLayout(new BoxLayout(nextPanel, BoxLayout.Y_AXIS));

        JPanel staffBody = UIStyle.panel(new BorderLayout(0, 8));
        staffBody.add(lblStaffInfo, BorderLayout.NORTH);
        JPanel inner = UIStyle.panel(new GridLayout(1, 2, 12, 0));
        inner.add(statsPanel);
        inner.add(piePanel);
        staffBody.add(inner, BorderLayout.CENTER);

        JPanel row1 = UIStyle.panel(new GridLayout(1, 2, 12, 0));
        row1.add(UIStyle.card(STORE_NAME, staffBody));
        row1.add(UIStyle.card("Ca làm tiếp theo của tôi", UIStyle.plainScroll(nextPanel)));

        JPanel row2 = UIStyle.card(String.format(
                "Doanh thu cửa hàng (triệu đồng, T%d/%d là lũy kế)", today.getMonthValue(), today.getYear()),
                revenueHolder);
        JPanel row3 = UIStyle.card("Yêu cầu nghỉ phép / đổi ca", leavePanel());

        JPanel page = UIStyle.panel(new GridLayout(3, 1, 0, 12));
        page.add(row1);
        page.add(row2);
        page.add(row3);
        return page;
    }

    /** Nạp lại toàn bộ trang Tổng quan từ DB (gọi mỗi lần mở trang). */
    private void refreshOverview() {
        LocalDateTime now = LocalDateTime.now();
        try {
            // --- Nhân sự hiện tại + biểu đồ tròn
            OverviewDao.StaffStats st = dao.staffStats(now);
            lblStaffInfo.setText("Nhân sự hiện tại · đã nghỉ việc: " + st.resigned
                    + " · cập nhật " + now.format(HM));

            statsPanel.removeAll();
            statsPanel.add(statTile("Đang làm việc", st.active, UIStyle.PRIMARY));
            statsPanel.add(statTile("Đang trong ca", st.inShift, new Color(0x16A34A)));
            statsPanel.add(statTile("Full-time", st.fullTime, new Color(0x2563EB)));
            statsPanel.add(statTile("Part-time", st.partTime, new Color(0xD97706)));

            String[] labels = st.byPosition.keySet().toArray(new String[0]);
            double[] values = new double[labels.length];
            for (int i = 0; i < labels.length; i++) values[i] = st.byPosition.get(labels[i]);
            piePanel.removeAll();
            piePanel.add(new PieChartPanel(labels, values), BorderLayout.CENTER);

            // --- Ca làm tiếp theo: chỉ của bản thân
            nextPanel.removeAll();
            List<OverviewDao.NextShift> mine = dao.nextShifts(acc.getEmployeeId(), now, 5);
            for (OverviewDao.NextShift s : mine) nextPanel.add(nextRow(s));
            if (mine.isEmpty()) nextPanel.add(UIStyle.muted("Chưa có ca tiếp theo."));

            // --- Doanh thu 2 năm
            String[] mon = {"T1", "T2", "T3", "T4", "T5", "T6", "T7", "T8", "T9", "T10", "T11", "T12"};
            revenueHolder.removeAll();
            revenueHolder.add(new BarChartPanel(mon,
                    String.valueOf(today.getYear() - 1), dao.revenueByMonth(today.getYear() - 1),
                    String.valueOf(today.getYear()), dao.revenueByMonth(today.getYear())), BorderLayout.CENTER);
        } catch (SQLException ex) {
            warn("Không tải được dữ liệu tổng quan: " + ex.getMessage());
        }
        reloadLeaves();

        for (JComponent c : new JComponent[]{statsPanel, piePanel, nextPanel, revenueHolder}) {
            c.revalidate();
            c.repaint();
        }
    }

    /** Ô thống kê: số lớn + nhãn, viền màu bên trái. */
    private JPanel statTile(String title, int value, Color accent) {
        JLabel v = new JLabel(String.valueOf(value));
        v.setFont(new Font("Segoe UI", Font.BOLD, 28));
        v.setForeground(accent);
        JPanel p = new JPanel(new BorderLayout(0, 2));
        p.setBackground(new Color(0xFAFAFA));
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 4, 0, 0, accent), new EmptyBorder(8, 12, 8, 12)));
        p.add(UIStyle.muted(title), BorderLayout.NORTH);
        p.add(v, BorderLayout.CENTER);
        return p;
    }

    /** Một dòng ca của bản thân: [A/B]  Thứ 7 · 21/11  /  Ca A · 15:00–23:00. Ca sáng = A, Ca chiều = B. */
    private JPanel nextRow(OverviewDao.NextShift s) {
        String code = "Ca sáng".equals(s.shift) ? "A" : "B";
        JPanel info = UIStyle.panel(null);
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        info.add(UIStyle.bold(dayVi(s.date) + " · " + s.date.format(DM)));
        info.add(UIStyle.muted("Ca " + code + " (" + s.shift + ") · " + s.start.format(HM) + "–" + s.end.format(HM)));

        JPanel p = new JPanel(new BorderLayout(10, 0));
        p.setBackground(Color.WHITE);
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, UIStyle.BORDER), new EmptyBorder(6, 6, 6, 6)));
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 56));
        p.add(new UIStyle.Avatar(code, 34, UIStyle.PRIMARY, Color.WHITE), BorderLayout.WEST);
        p.add(info, BorderLayout.CENTER);
        return p;
    }

    private static String dayVi(LocalDate d) {
        switch (d.getDayOfWeek()) {
            case MONDAY:    return "Thứ 2";
            case TUESDAY:   return "Thứ 3";
            case WEDNESDAY: return "Thứ 4";
            case THURSDAY:  return "Thứ 5";
            case FRIDAY:    return "Thứ 6";
            case SATURDAY:  return "Thứ 7";
            default:        return "Chủ nhật";
        }
    }

    // =====================================================================
    // NHÂN SỰ (tab: Danh sách | Chấm công | Lịch sử chỉnh sửa | Lương) - dữ liệu từ SQL
    // =====================================================================
    private static final String[] GENDER_VI = {"Nam", "Nữ", "Khác"};
    private static final Gender[] GENDERS = {Gender.MALE, Gender.FEMALE, Gender.OTHER};
    private static final String[] ATT_VI = {"Đúng giờ", "Đi trễ", "Về sớm", "Vắng không phép"};
    private static final AttendanceStatus[] ATT_CODES = {
            AttendanceStatus.ON_TIME, AttendanceStatus.LATE, AttendanceStatus.EARLY_LEAVE, AttendanceStatus.ABSENT};

    private static String genderVi(Gender g) {
        if (g == Gender.MALE) return "Nam";
        if (g == Gender.FEMALE) return "Nữ";
        return "Khác";
    }

    private static String attVi(AttendanceStatus s) {
        for (int i = 0; i < ATT_CODES.length; i++) if (ATT_CODES[i] == s) return ATT_VI[i];
        return "";
    }

    private static String hm(LocalDateTime t) { return t == null ? "--" : t.format(HM); }

    private static String lastWord(String name) {
        String n = name == null ? "" : name.trim();
        int i = n.lastIndexOf(' ');
        return i < 0 ? n : n.substring(i + 1);
    }

    private static String money(BigDecimal v) {
        return NumberFormat.getInstance(Locale.forLanguageTag("vi-VN")).format(v == null ? BigDecimal.ZERO : v) + " ₫";
    }

    /** Nạp lại nhân viên + chức vụ + chấm công + lương từ DB (chạy mỗi lần mở trang Nhân sự). */
    private void reloadStaff() {
        try {
            positions.clear();
            positions.addAll(posDao.findAll());
            staff.clear();
            staff.addAll(empDao.findAll());
        } catch (SQLException ex) {
            warn("Không tải được danh sách nhân viên: " + ex.getMessage());
            return;
        }
        Object sel = cbPos.getSelectedItem();
        cbPos.removeAllItems();
        cbPos.addItem("Tất cả");
        for (Position p : positions) cbPos.addItem(p.getPositionName());
        cbPos.setSelectedItem(sel != null ? sel : "Tất cả");

        reloadEmpModel();
        reloadAttendance();
        refreshPayroll();
    }

    private void reloadEmpModel() {
        empModel.setRowCount(0);
        for (Employee e : staff) {
            empModel.addRow(new Object[]{e.getEmployeeId(), e.getFullName(), genderVi(e.getGender()),
                    e.getPositionName(), e.getPhoneNumber(), e.getAddress(),
                    e.getStatus() == EmployeeStatus.ACTIVE ? "Đang làm" : "Đã nghỉ"});
        }
    }

    /** Chấm công 3 tháng gần nhất + nhật ký chỉnh sửa. */
    private void reloadAttendance() {
        List<AttendanceLog> logs;
        try {
            attRows.clear();
            attRows.addAll(attDao.findByRange(YearMonth.from(today).minusMonths(2).atDay(1), today));
            logs = attDao.findLogs();
        } catch (SQLException ex) {
            warn("Không tải được chấm công: " + ex.getMessage());
            return;
        }
        attModel.setRowCount(0);
        for (Attendance a : attRows) {
            attModel.addRow(new Object[]{a.getEmployeeId(), a.getFullName(), a.getWorkDate().format(D),
                    a.getScheduledStart().format(HM) + "–" + a.getScheduledEnd().format(HM),
                    hm(a.getCheckInTime()), hm(a.getCheckOutTime()), attVi(a.getStatus())});
        }
        logModel.setRowCount(0);
        for (AttendanceLog g : logs) {
            logModel.addRow(new Object[]{g.getEditedAt().format(DT), g.getEditedBy(),
                    g.getEmployeeId() + " · " + g.getWorkDate().format(D),
                    hm(g.getOldIn()) + " – " + hm(g.getOldOut()) + " (" + attVi(g.getOldStatus()) + ")",
                    hm(g.getNewIn()) + " – " + hm(g.getNewOut()) + " (" + attVi(g.getNewStatus()) + ")",
                    g.getReason()});
        }
    }

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
        empSorter.setComparator(1, (a, b) -> vi.compare(lastWord((String) a), lastWord((String) b)));
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
                UIStyle.muted("Nghỉ việc sẽ khóa tài khoản (trigger) và hủy các ca từ ngày mai, không xóa dữ liệu"));

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
        Employee e = staff.get(r);
        boolean isActive = e.getStatus() == EmployeeStatus.ACTIVE;
        if (isActive == active) { warn(active ? "Nhân viên này đang làm việc." : "Nhân viên này đã nghỉ việc."); return; }
        if (!active && e.getEmployeeId().equals(acc.getEmployeeId())) { warn("Không thể cho chính mình nghỉ việc."); return; }
        int ok = JOptionPane.showConfirmDialog(this,
                (active ? "Cho đi làm lại: " : "Cho nghỉ việc: ") + e.getFullName() + "?", "Xác nhận", JOptionPane.YES_NO_OPTION);
        if (ok != JOptionPane.YES_OPTION) return;
        try {
            if (active) empDao.reactivate(e.getEmployeeId());
            else empDao.resign(e.getEmployeeId());   // trigger khóa tài khoản + hủy ca từ ngày mai
        } catch (SQLException ex) {
            warn("Không cập nhật được: " + ex.getMessage());
            return;
        }
        reloadStaff();
    }

    private void showEmpDialog(int idx) {
        boolean edit = idx >= 0;
        Employee cur = edit ? staff.get(idx) : null;
        if (positions.isEmpty()) { warn("Chưa có chức vụ nào trong CSDL."); return; }

        String newId = null;
        if (!edit) {
            try {
                newId = empDao.nextEmployeeId();
            } catch (SQLException ex) {
                warn("Không sinh được mã nhân viên: " + ex.getMessage());
                return;
            }
        }

        JTextField code = new JTextField(edit ? cur.getEmployeeId() : newId);
        code.setEditable(false);
        JTextField name = new JTextField(edit ? cur.getFullName() : "");
        JComboBox<String> gender = new JComboBox<>(GENDER_VI);
        JComboBox<String> pos = new JComboBox<>();
        for (Position p : positions) pos.addItem(p.getPositionName());
        JLabel salary = UIStyle.muted("");
        JTextField phone = new JTextField(edit && cur.getPhoneNumber() != null ? cur.getPhoneNumber() : "");
        JTextField mail = new JTextField(edit && cur.getEmail() != null ? cur.getEmail() : "");
        JTextField addr = new JTextField(edit && cur.getAddress() != null ? cur.getAddress() : "");
        if (edit) {
            gender.setSelectedItem(genderVi(cur.getGender()));
            pos.setSelectedItem(cur.getPositionName());
        }
        Runnable showSalary = () -> {
            Position p = positions.get(Math.max(0, pos.getSelectedIndex()));
            salary.setText(p.getSalaryType() == SalaryType.FIXED
                    ? "Full-time · " + money(p.getSalaryRate()) + "/tháng"
                    : "Part-time · " + money(p.getSalaryRate()) + "/giờ");
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

        Employee e = new Employee();
        e.setEmployeeId(code.getText());
        e.setFullName(name.getText().trim());
        e.setGender(GENDERS[gender.getSelectedIndex()]);
        e.setPositionId(positions.get(pos.getSelectedIndex()).getPositionId());
        e.setPhoneNumber(phone.getText().trim());
        e.setEmail(mail.getText().trim());
        e.setAddress(addr.getText().trim());
        try {
            if (edit) {
                // giữ nguyên các cột không có trên form (UPDATE ghi đè toàn bộ)
                e.setBirthDay(cur.getBirthDay());
                e.setCitizenId(cur.getCitizenId());
                e.setHireDate(cur.getHireDate());
                empDao.update(e);   // đổi chức vụ -> salary_type, salary_rate đổi theo
            } else {
                e.setHireDate(today);
                e.setStatus(EmployeeStatus.ACTIVE);
                empDao.insert(e, Role.EMPLOYEE);   // kèm Account (mật khẩu mặc định, bắt đổi lần đầu)
            }
        } catch (SQLException ex) {
            warn("Không lưu được nhân viên: " + ex.getMessage());
            return;
        }
        reloadStaff();
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
                UIStyle.button("Ghi nhận đi trễ", () -> quickMark(AttendanceStatus.LATE)),
                UIStyle.button("Ghi nhận về sớm", () -> quickMark(AttendanceStatus.EARLY_LEAVE)),
                UIStyle.button("Ghi nhận vắng", () -> quickMark(AttendanceStatus.ABSENT))), BorderLayout.NORTH);
        p.add(UIStyle.plainScroll(attTable), BorderLayout.CENTER);
        return p;
    }

    private void editAttendance() {
        int r = selected(attTable);
        if (r < 0) return;
        Attendance a = attRows.get(r);

        JTextField tIn = new JTextField(hm(a.getCheckInTime()));
        JTextField tOut = new JTextField(hm(a.getCheckOutTime()));
        JComboBox<String> cbSt = new JComboBox<>(ATT_VI);
        cbSt.setSelectedItem(attVi(a.getStatus()));
        JTextField tReason = new JTextField();

        JPanel f = new JPanel(new GridLayout(0, 2, 8, 8));
        f.add(new JLabel("Ca đăng ký:"));              f.add(new JLabel(str(attModel, r, 3)));
        f.add(new JLabel("Giờ vào (HH:mm hoặc --):")); f.add(tIn);
        f.add(new JLabel("Giờ ra (HH:mm hoặc --):"));  f.add(tOut);
        f.add(new JLabel("Trạng thái:"));              f.add(cbSt);
        f.add(new JLabel("Lý do sửa (bắt buộc):"));    f.add(tReason);

        int ok = JOptionPane.showConfirmDialog(this, f,
                "Sửa chấm công " + a.getFullName() + " - " + str(attModel, r, 2),
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (ok != JOptionPane.OK_OPTION) return;

        String in = tIn.getText().trim(), out = tOut.getText().trim();
        if (!validTime(in) || !validTime(out)) { warn("Giờ không hợp lệ. Dùng định dạng HH:mm hoặc --."); return; }
        if (isTime(in) && isTime(out) && !LocalTime.parse(out).isAfter(LocalTime.parse(in))) {
            warn("Giờ ra phải sau giờ vào.");
            return;
        }
        if (tReason.getText().isBlank()) { warn("Bắt buộc nhập lý do sửa."); return; }
        try {
            attDao.edit(a.getAttendanceId(), at(a, in), at(a, out), ATT_CODES[cbSt.getSelectedIndex()],
                    tReason.getText().trim(), acc.getEmployeeId());
        } catch (SQLException | IllegalArgumentException | IllegalStateException ex) {
            warn("Không sửa được chấm công: " + ex.getMessage());
            return;
        }
        afterAttendanceChange();
    }

    private void quickMark(AttendanceStatus status) {
        int r = selected(attTable);
        if (r < 0) return;
        String reason = JOptionPane.showInputDialog(this, "Lý do ghi nhận \"" + attVi(status) + "\":");
        if (reason == null) return;
        if (reason.isBlank()) { warn("Bắt buộc nhập lý do."); return; }
        try {
            attDao.quickMark(attRows.get(r).getAttendanceId(), status, reason.trim(), acc.getEmployeeId());
        } catch (SQLException | IllegalArgumentException | IllegalStateException ex) {
            warn("Không ghi nhận được: " + ex.getMessage());
            return;
        }
        afterAttendanceChange();
    }

    /** "HH:mm" -> thời điểm trong ngày làm của bản ghi; "--" -> null. */
    private static LocalDateTime at(Attendance a, String s) {
        return isTime(s) ? LocalDateTime.of(a.getWorkDate(), LocalTime.parse(s)) : null;
    }

    private void afterAttendanceChange() {
        reloadAttendance();   // gồm cả nhật ký (DAO đã ghi AttendanceLog)
        refreshPayroll();
    }

    // ----- Lương -----
    private JPanel buildPayroll() {
        for (int i = 0; i < 3; i++) {
            YearMonth ym = YearMonth.from(today).minusMonths(i);
            months.add(ym);
            cbMonth.addItem(String.format("%02d/%d", ym.getMonthValue(), ym.getYear()));
        }
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
        try {
            if (salaryDao.isFinalized(ym.getMonthValue(), ym.getYear())) { info("Kỳ lương này đã chốt."); return; }
            int ok = JOptionPane.showConfirmDialog(this,
                    "Chốt lương " + String.format("%02d/%d", ym.getMonthValue(), ym.getYear())
                            + "?\nSau khi chốt, mức lương và giờ công được lưu vào bảng Payroll.",
                    "Chốt lương", JOptionPane.OK_CANCEL_OPTION);
            if (ok != JOptionPane.OK_OPTION) return;
            int n = salaryDao.finalizePayroll(ym.getMonthValue(), ym.getYear());   // sp_RefreshMonthlyHours + INSERT Payroll
            info("Đã chốt lương cho " + n + " nhân viên.");
        } catch (SQLException ex) {
            warn("Không chốt được lương: " + ex.getMessage());
            return;
        }
        refreshPayroll();
    }

    /** Chưa chốt: tạm tính từ Attendance/MonthlyHours. Đã chốt: đọc bảng Payroll. */
    private void refreshPayroll() {
        if (months.isEmpty()) return;
        YearMonth ym = selectedMonth();
        try {
            boolean isClosed = salaryDao.isFinalized(ym.getMonthValue(), ym.getYear());
            lblPayState.setText(isClosed ? "● Đã chốt" : "● Chưa chốt");
            lblPayState.setForeground(isClosed ? new Color(0x16A34A) : UIStyle.PRIMARY);

            payModel.setRowCount(0);
            for (SalaryRow r : salaryDao.findByMonth(ym.getMonthValue(), ym.getYear())) {
                payModel.addRow(new Object[]{r.getEmployeeId(), r.getFullName(), r.getPositionName(),
                        r.getSalaryType() == SalaryType.FIXED ? "Cố định" : "Theo giờ",
                        String.format(Locale.ROOT, "%.1f", r.getHours()),
                        money(r.getBaseSalary()), money(r.getAllowance()),
                        money(r.getRevenueBonus()), money(r.getTotalSalary())});
            }
        } catch (SQLException ex) {
            warn("Không tải được bảng lương: " + ex.getMessage());
        }
    }

    // =====================================================================
    // LỊCH LÀM (lưới tuần: hàng = ca, cột = ngày) - đọc WorkSchedule + ShiftTime từ SQL
    // =====================================================================
    private static final String[] DAYS = {"Thứ 2", "Thứ 3", "Thứ 4", "Thứ 5", "Thứ 6", "Thứ 7", "CN"};
    /** Quy tắc nghiệp vụ (không phải dữ liệu). Trigger trg_WorkSchedule_Validate cũng kiểm tra lại ở DB. */
    private static final int MAX_CONCURRENT = 10;
    private static final int MIN_PART_MINUTES = 240;

    private JComponent buildSchedulePage() {
        JPanel p = UIStyle.panel(new BorderLayout(0, 8));
        p.add(UIStyle.row(
                UIStyle.button("◀ Tuần trước", () -> shiftWeek(-1)), lblWeek,
                UIStyle.button("Tuần sau ▶", () -> shiftWeek(1)),
                UIStyle.muted("Tối đa " + MAX_CONCURRENT + " người làm cùng lúc")), BorderLayout.NORTH);
        JTable grid = UIStyle.grid(gridModel);
        grid.setRowHeight(170);
        p.add(UIStyle.card("Lịch làm chính thức (đã chốt)", UIStyle.plainScroll(grid)), BorderLayout.CENTER);
        return p;
    }

    private void shiftWeek(int delta) {
        weekOffset += delta;
        loadGrid();
    }

    private void loadGrid() {
        LocalDate mon = monday.plusWeeks(weekOffset);
        lblWeek.setText("Tuần " + mon.format(DM) + " – " + mon.plusDays(6).format(D));

        List<ShiftTime> shifts;
        List<WorkSchedule> wk;
        try {
            shifts = shiftDao.findAll();
            wk = workDao.findByRange(mon, mon.plusDays(6));   // đã sắp theo ngày, giờ bắt đầu, mã NV
        } catch (SQLException ex) {
            warn("Không tải được lịch làm: " + ex.getMessage());
            return;
        }

        gridModel.setRowCount(0);
        for (ShiftTime sh : shifts) {
            Object[] row = new Object[DAYS.length + 1];
            row[0] = sh.getShiftName();
            for (int d = 0; d < DAYS.length; d++) {
                LocalDate date = mon.plusDays(d);
                List<String> parts = new ArrayList<>();
                for (WorkSchedule s : wk) {
                    if (s.getStatus() == ScheduleStatus.CONFIRMED && s.getWorkDate().equals(date)
                            && sh.getShiftName().equals(s.getShiftName())) {
                        parts.add(lastWord(s.getFullName()) + " " + shortRange(s.getStartTime(), s.getEndTime()));
                    }
                }
                row[d + 1] = String.join(", ", parts);
            }
            gridModel.addRow(row);
        }
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

    /** Các đăng ký đang chờ chốt (từ hôm nay đến hết tuần sau) + những dòng vừa xử lý trong phiên này. */
    private void reloadReg() {
        List<WorkSchedule> src;
        try {
            src = workDao.findByRange(today, monday.plusWeeks(1).plusDays(6));
        } catch (SQLException ex) {
            warn("Không tải được đăng ký ca: " + ex.getMessage());
            return;
        }
        regRows.clear();
        regModel.setRowCount(0);
        for (WorkSchedule s : src) {
            if (s.getStatus() != ScheduleStatus.REGISTERED && !processed.contains(s.getScheduleId())) continue;
            regRows.add(s);
            regModel.addRow(new Object[]{s.getEmployeeId(), s.getFullName(),
                    s.getSalaryType() == SalaryType.FIXED ? "Full-time" : "Part-time",
                    dayName(s.getWorkDate()) + " " + s.getWorkDate().format(D), s.getShiftName(),
                    range(s.getStartTime(), s.getEndTime()), schedVi(s.getStatus())});
        }
    }

    private void confirmShift(JTable regTable) {
        int r = selected(regTable);
        if (r < 0) return;
        WorkSchedule s = regRows.get(r);
        if (s.getStatus() != ScheduleStatus.REGISTERED) { warn("Đăng ký này đã được xử lý."); return; }
        boolean fixed = s.getSalaryType() == SalaryType.FIXED;

        ShiftTime sh = null;
        try {
            for (ShiftTime t : shiftDao.findAll()) if (t.getShiftName().equals(s.getShiftName())) sh = t;
        } catch (SQLException ex) {
            warn("Không tải được khung giờ ca: " + ex.getMessage());
            return;
        }
        if (sh == null) { warn("Ca không hợp lệ."); return; }

        String[] ts = times(sh);
        JComboBox<String> cbFrom = new JComboBox<>(ts), cbTo = new JComboBox<>(ts);
        cbFrom.setSelectedItem(hm(s.getStartTime()));
        cbTo.setSelectedItem(hm(s.getEndTime()));
        cbFrom.setEnabled(!fixed);
        cbTo.setEnabled(!fixed);

        JPanel f = new JPanel(new GridLayout(0, 2, 8, 8));
        f.add(new JLabel("Nhân viên:")); f.add(new JLabel(s.getFullName() + (fixed ? " (full-time)" : " (part-time)")));
        f.add(new JLabel("Ngày:"));      f.add(new JLabel(dayName(s.getWorkDate()) + " " + s.getWorkDate().format(D)));
        f.add(new JLabel("Ca:"));        f.add(new JLabel(s.getShiftName() + " (" + range(sh.getStartTime(), sh.getEndTime()) + ")"));
        f.add(new JLabel("Giờ làm từ:")); f.add(cbFrom);
        f.add(new JLabel("Giờ làm đến:")); f.add(cbTo);

        int ok = JOptionPane.showConfirmDialog(this, f, "Chốt ca", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);
        if (ok != JOptionPane.OK_OPTION) return;

        LocalTime from = LocalTime.parse((String) cbFrom.getSelectedItem());
        LocalTime to = LocalTime.parse((String) cbTo.getSelectedItem());
        String err = checkRange(fixed, sh, from, to);
        if (err != null) { warn(err); return; }

        try {
            int pk = workDao.peakConcurrent(s.getWorkDate(), from, to, s.getScheduleId());
            if (pk + 1 > MAX_CONCURRENT) {
                warn("Khung giờ " + range(from, to) + " đã có " + pk + " người làm cùng lúc "
                        + "(tối đa " + MAX_CONCURRENT + "). Hãy rút ngắn giờ hoặc từ chối.");
                return;
            }
            int ok2 = JOptionPane.showConfirmDialog(this,
                    "Chốt " + s.getFullName() + " làm " + s.getShiftName() + " " + dayName(s.getWorkDate()) + " "
                            + s.getWorkDate().format(DM) + " (" + range(from, to) + ")?\n"
                            + "Sau khi chốt, nhân viên không được tự ý bỏ ca (phải xin đổi ca/nghỉ).",
                    "Chốt ca", JOptionPane.OK_CANCEL_OPTION);
            if (ok2 != JOptionPane.OK_OPTION) return;

            if (!workDao.confirm(s.getScheduleId(), from, to)) {
                warn("Đăng ký này đã được xử lý.");
                reloadReg();
                return;
            }
        } catch (SQLException ex) {
            warn("Không chốt được ca: " + ex.getMessage());   // gồm cả lỗi từ trigger
            return;
        }
        processed.add(s.getScheduleId());
        reloadReg();
        loadGrid();
    }

    private void rejectShift(JTable regTable) {
        int r = selected(regTable);
        if (r < 0) return;
        WorkSchedule s = regRows.get(r);
        if (s.getStatus() != ScheduleStatus.REGISTERED) { warn("Đăng ký này đã được xử lý."); return; }
        try {
            if (!workDao.reject(s.getScheduleId())) warn("Đăng ký này đã được xử lý.");
        } catch (SQLException ex) {
            warn("Không từ chối được: " + ex.getMessage());
            return;
        }
        processed.add(s.getScheduleId());
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

    /** Duyệt: nghỉ -> hủy ca; đổi ca -> sang ca/giờ mới (ghi thẳng DB). Từ chối: chỉ đổi trạng thái. */
    private void decide(JTable t, boolean approve) {
        int r = selected(t);
        if (r < 0) return;
        OverviewDao.Leave l = leaves.get(r);
        if (!"PENDING".equals(l.status)) { warn("Đơn này đã được xử lý."); return; }
        try {
            dao.decideLeave(l.id, approve, acc.getEmployeeId());
        } catch (IllegalStateException | SQLException ex) {
            warn(ex.getMessage());
            reloadLeaves();
            return;
        }
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
        try {
            List<OverviewDao.Leave> fresh = dao.leaves();
            leaves.clear();
            leaves.addAll(fresh);
        } catch (SQLException ex) {
            warn("Không tải được đơn nghỉ / đổi ca: " + ex.getMessage());
            return;
        }
        leaveModel.setRowCount(0);
        for (OverviewDao.Leave l : leaves) {
            boolean isLeave = "LEAVE".equals(l.type);
            String content = isLeave
                    ? (l.shift == null ? "Nghỉ cả ngày" : "Nghỉ " + l.shift.toLowerCase())
                    : "→ " + l.newShift + " " + l.newStart.format(HM) + "–" + l.newEnd.format(HM);
            leaveModel.addRow(new Object[]{l.empId, l.empName, isLeave ? "Xin nghỉ" : "Đổi ca",
                    l.date.format(D), l.shift == null ? "Cả ngày" : l.shift, content, l.reason, leaveVi(l.status)});
        }
    }

    // =====================================================================
    // THÔNG BÁO (tab: Hộp thư | Đã gửi) - Notification lấy từ SQL
    // =====================================================================
    private JComponent buildNotiPage() {
        JPanel sent = UIStyle.panel(new BorderLayout(0, 8));
        sent.setBorder(new EmptyBorder(10, 10, 10, 10));
        sent.add(UIStyle.row(UIStyle.button("Soạn thông báo", this::composeNotification)), BorderLayout.NORTH);
        sent.add(UIStyle.plainScroll(UIStyle.table(sentModel)), BorderLayout.CENTER);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(UIStyle.BOLD);
        tabs.addTab("Hộp thư", withPadding(inboxHolder));
        tabs.addTab("Đã gửi", sent);
        return tabs;
    }

    /** Hộp thư của quản lý = các đơn nghỉ / đổi ca đang chờ duyệt (đọc từ DB mỗi lần mở trang). */
    private void reloadNoti() {
        reloadLeaves();
        List<String> inbox = new ArrayList<>();
        for (OverviewDao.Leave l : leaves) {
            if (!"PENDING".equals(l.status)) continue;
            String who = "<b>" + l.empName + "</b>";
            inbox.add("LEAVE".equals(l.type)
                    ? who + " xin nghỉ ngày " + l.date.format(D) + " (" + l.reason + ")."
                    : who + " xin đổi ca ngày " + l.date.format(D) + ": " + l.shift + " → " + l.newShift + ".");
        }
        inboxHolder.removeAll();
        inboxHolder.add(notificationPage("noti", inbox), BorderLayout.CENTER);
        inboxHolder.revalidate();
        inboxHolder.repaint();
        reloadSent();
    }

    private void reloadSent() {
        try {
            List<Notification> list = notiDao.findSentBy(acc.getEmployeeId());
            sentModel.setRowCount(0);
            for (Notification n : list) {
                sentModel.addRow(new Object[]{n.getCreatedAt().format(DT), receiverLabel(n.getReceiverId()),
                        n.getTitle(), n.getContent()});
            }
        } catch (SQLException ex) {
            warn("Không tải được thông báo đã gửi: " + ex.getMessage());
        }
    }

    private String receiverLabel(String id) {
        if (id == null) return "Tất cả nhân viên";
        for (Employee e : staff) if (e.getEmployeeId().equals(id)) return id + " - " + e.getFullName();
        return id;
    }

    private void composeNotification() {
        List<Employee> recipients = new ArrayList<>();
        JComboBox<String> to = new JComboBox<>();
        to.addItem("Tất cả nhân viên");
        for (Employee e : staff) {
            if (e.getStatus() == EmployeeStatus.ACTIVE && !e.getEmployeeId().equals(acc.getEmployeeId())) {
                recipients.add(e);
                to.addItem(e.getEmployeeId() + " - " + e.getFullName());
            }
        }
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
        String t = title.getText().trim(), c = body.getText().trim();
        if (t.isEmpty() || c.isEmpty()) { warn("Nhập đủ tiêu đề và nội dung."); return; }
        if (t.length() > 150) { warn("Tiêu đề tối đa 150 ký tự."); return; }
        if (c.length() > 1000) { warn("Nội dung tối đa 1000 ký tự."); return; }

        int i = to.getSelectedIndex();
        String receiver = i <= 0 ? null : recipients.get(i - 1).getEmployeeId();   // null = gửi tất cả
        try {
            notiDao.send(acc.getEmployeeId(), receiver, t, c);
        } catch (SQLException ex) {
            warn("Không gửi được thông báo: " + ex.getMessage());
            return;
        }
        reloadSent();
    }

    // =====================================================================
    // TIỆN ÍCH ĐỊNH DẠNG / BẢNG
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

    /** 6h–12h, 13h30–15h: gọn để nhét vào ô lịch tuần. */
    private static String shortRange(LocalTime a, LocalTime b) {
        return shortTime(a) + "–" + shortTime(b);
    }

    private static String shortTime(LocalTime t) {
        return t.getHour() + "h" + (t.getMinute() == 0 ? "" : String.format("%02d", t.getMinute()));
    }

    private static String dayName(LocalDate d) {
        return DAYS[d.getDayOfWeek().getValue() - 1];
    }

    private static String schedVi(ScheduleStatus s) {
        if (s == ScheduleStatus.CONFIRMED) return "Đã chốt";
        if (s == ScheduleStatus.CANCELLED) return "Đã hủy";
        return "Chờ chốt";
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
