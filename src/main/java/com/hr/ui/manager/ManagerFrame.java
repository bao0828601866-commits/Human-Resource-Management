package com.hr.ui.manager;

import com.hr.dao.OverviewDao;
import com.hr.dao.EmployeeDAO;
import com.hr.dao.EmployeeDetailDAO;
import com.hr.dao.NotificationDAO;
import com.hr.dao.PositionDAO;
import com.hr.dao.ShiftTimeDAO;
import com.hr.dao.WorkScheduleDAO;
import com.hr.model.Account;
import com.hr.model.Employee;
import com.hr.model.Notification;
import com.hr.model.Position;
import com.hr.model.ShiftTime;
import com.hr.model.WorkSchedule;
import com.hr.model.enums.EmployeeStatus;
import com.hr.model.enums.Gender;
import com.hr.model.enums.Role;
import com.hr.model.enums.ScheduleStatus;
import com.hr.model.enums.SalaryType;
import com.hr.ui.BaseFrame; // hoặc com.hr.ui.components.BaseFrame nếu đã chuyển
import com.hr.ui.UIStyle;    // hoặc com.hr.ui.components.UIStyle

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.sql.SQLException;
import java.math.BigDecimal;
import java.text.Collator;
import java.text.NumberFormat;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public class ManagerFrame extends BaseFrame {
    private static final DateTimeFormatter D = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DM = DateTimeFormatter.ofPattern("dd/MM");
    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final LocalDate today = LocalDate.now();
    private final LocalDate monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    // --- DAO + dữ liệu đọc từ SQL ---
    private final EmployeeDAO empDao = new EmployeeDAO();
    private final EmployeeDetailDAO detailDao = new EmployeeDetailDAO();
    private final PositionDAO posDao = new PositionDAO();
    private final WorkScheduleDAO workDao = new WorkScheduleDAO();
    private final ShiftTimeDAO shiftDao = new ShiftTimeDAO();
    private final NotificationDAO notiDao = new NotificationDAO();
    private final List<Employee> staff = new ArrayList<>();        // hàng i của empModel = staff[i]
    private final List<Position> positions = new ArrayList<>();

    // --- Nhân sự ---
    private static final int COL_ACTION = 7;
    private final DefaultTableModel empModel = UIStyle.model("Mã NV", "Họ tên", "Giới tính", "Chức vụ", "SĐT", "Địa chỉ", "Trạng thái", "Thao tác");
    private final JTable empTable = UIStyle.table(empModel);
    private final TableRowSorter<DefaultTableModel> empSorter = new TableRowSorter<>(empModel);
    private final SearchField txtSearch = new SearchField("Tìm kiếm nhân viên...", 18);
    private final JLabel lblStaffCount = UIStyle.muted("");
    private final JLabel lblStaffFooter = UIStyle.muted("");
    private String roleFilter = null;   // null = tất cả vai trò
    private int sortIdx = 0;            // 0: Mã tăng, 1: Mã giảm, 2: Tên A-Z, 3: Tên Z-A
    private IconButton btnFilter;

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
        addProfileMenu();

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

        // Highland: [thẻ số liệu | biểu đồ tròn + chú thích]
        JPanel staffBody = UIStyle.panel(new BorderLayout(0, 8));
        staffBody.add(lblStaffInfo, BorderLayout.NORTH);

        JPanel inner = UIStyle.panel(new BorderLayout(24, 0));
        statsPanel.setPreferredSize(new Dimension(340, 0));   // 2x2 thẻ số liệu, rộng cố định
        inner.add(statsPanel, BorderLayout.WEST);
        inner.add(piePanel, BorderLayout.CENTER);              // pie + chú thích chiếm phần còn lại
        staffBody.add(inner, BorderLayout.CENTER);

        JPanel highlandCard = UIStyle.card(STORE_NAME, staffBody);
        JPanel nextCard = UIStyle.card("Ca làm tiếp theo", UIStyle.plainScroll(nextPanel));
        nextCard.setPreferredSize(new Dimension(360, 0));      // thu hẹp ô ca làm

        JPanel row1 = UIStyle.panel(new BorderLayout(12, 0));
        row1.add(highlandCard, BorderLayout.CENTER);
        row1.add(nextCard, BorderLayout.EAST);

        JPanel row2 = UIStyle.card(String.format(
                "Doanh thu cửa hàng (triệu đồng, T%d/%d)", today.getMonthValue(), today.getYear()), revenueHolder);
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
            lblStaffInfo.setText("Nhân sự hiện tại · đã nghỉ việc: " + st.resigned + " · cập nhật " + now.format(HM));

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

            // --- Ca làm tiếp theo của bản thân
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
    // NHÂN SỰ - giao diện thẻ + bảng (tìm kiếm, lọc vai trò, sắp xếp) - dữ liệu từ SQL
    // =====================================================================
    private static final String[] GENDER_VI = {"Nam", "Nữ", "Khác"};
    private static final Gender[] GENDERS = {Gender.MALE, Gender.FEMALE, Gender.OTHER};

    private static String genderVi(Gender g) {
        if (g == Gender.MALE) return "Nam";
        if (g == Gender.FEMALE) return "Nữ";
        return "Khác";
    }

    private static String lastWord(String name) {
        String n = name == null ? "" : name.trim();
        int i = n.lastIndexOf(' ');
        return i < 0 ? n : n.substring(i + 1);
    }

    private static String money(BigDecimal v) {
        return NumberFormat.getInstance(Locale.forLanguageTag("vi-VN")).format(v == null ? BigDecimal.ZERO : v) + " ₫";
    }

    /** Nạp nhân viên + chức vụ từ DB. */
    private boolean loadStaffData() {
        try {
            positions.clear();
            positions.addAll(posDao.findAll());
            staff.clear();
            staff.addAll(empDao.findAll());
            return true;
        } catch (SQLException ex) {
            warn("Không tải được danh sách nhân viên: " + ex.getMessage());
            return false;
        }
    }

    /** Chạy mỗi lần mở trang Nhân sự. */
    private void reloadStaff() {
        if (!loadStaffData()) return;
        reloadEmpModel();
    }

    private void reloadEmpModel() {
        empModel.setRowCount(0);
        for (Employee e : staff) {
            empModel.addRow(new Object[]{e.getEmployeeId(), e.getFullName(), genderVi(e.getGender()),
                    e.getPositionName(), e.getPhoneNumber(), e.getAddress(),
                    e.getStatus() == EmployeeStatus.ACTIVE ? "Đang làm" : "Đã nghỉ", ""});
        }
        lblStaffCount.setText("Tổng cộng " + staff.size() + " nhân viên");
        updateStaffFooter();
    }

    /** "Hiển thị 61 / 61 nhân viên (gồm 17 Thu ngân, ...)" theo bộ lọc hiện tại. */
    private void updateStaffFooter() {
        int shown = empSorter.getViewRowCount();
        int total = empModel.getRowCount();
        Map<String, Integer> by = new LinkedHashMap<>();
        for (int i = 0; i < shown; i++) {
            String pn = String.valueOf(empModel.getValueAt(empSorter.convertRowIndexToModel(i), 3));
            by.merge(pn, 1, Integer::sum);
        }
        List<String> parts = new ArrayList<>();
        for (Map.Entry<String, Integer> en : by.entrySet()) parts.add(en.getValue() + " " + en.getKey());
        lblStaffFooter.setText("Hiển thị " + shown + " / " + total + " nhân viên"
                + (parts.isEmpty() ? "" : " (gồm " + String.join(", ", parts) + ")"));
    }

    private JComponent buildStaffPage() {
        // --- bảng ---
        Collator vi = Collator.getInstance(Locale.forLanguageTag("vi"));
        empSorter.setComparator(1, (a, b) -> {
            String sa = (String) a, sb = (String) b;
            int c = vi.compare(lastWord(sa), lastWord(sb));   // so theo "tên" (từ cuối)
            return c != 0 ? c : vi.compare(sa, sb);
        });
        for (int c = 0; c < empModel.getColumnCount(); c++) empSorter.setSortable(c, false); // sắp xếp qua nút
        empTable.setRowSorter(empSorter);
        empSorter.addRowSorterListener(e -> updateStaffFooter());
        applySort();

        styleStaffTable();

        // --- thanh công cụ ---
        UIStyle.onChange(txtSearch, this::applyEmpFilter);
        txtSearch.setPreferredSize(new Dimension(230, 36));

        btnFilter = new IconButton(ICON_FUNNEL, "Lọc theo vai trò");
        btnFilter.addActionListener(e -> showFilterMenu(btnFilter));
        IconButton btnSort = new IconButton(ICON_SORT, "Sắp xếp");
        btnSort.addActionListener(e -> showSortMenu(btnSort));
        JButton btnAdd = UIStyle.button("+ Thêm nhân viên", () -> showEmpDialog(-1));
        btnAdd.setPreferredSize(new Dimension(btnAdd.getPreferredSize().width, 36));

        JPanel tools = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        tools.setOpaque(false);
        tools.add(txtSearch);
        tools.add(btnFilter);
        tools.add(btnSort);
        tools.add(btnAdd);

        JLabel title = new JLabel("Danh sách nhân viên");
        title.setFont(new Font("Segoe UI", Font.BOLD, 16));
        JPanel titles = UIStyle.panel(null);
        titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));
        titles.add(title);
        titles.add(Box.createVerticalStrut(2));
        titles.add(lblStaffCount);

        JPanel header = new JPanel(new BorderLayout(12, 0));
        header.setBackground(Color.WHITE);
        header.setBorder(new EmptyBorder(16, 18, 14, 18));
        header.add(titles, BorderLayout.WEST);
        header.add(tools, BorderLayout.EAST);

        JScrollPane sp = new JScrollPane(empTable);
        sp.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UIStyle.BORDER));
        sp.getViewport().setBackground(Color.WHITE);

        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createLineBorder(UIStyle.BORDER));
        card.add(header, BorderLayout.NORTH);
        card.add(sp, BorderLayout.CENTER);

        JLabel pageTitle = new JLabel("Quản lý nhân viên");
        pageTitle.setFont(new Font("Segoe UI", Font.BOLD, 22));

        JPanel page = UIStyle.panel(new BorderLayout(0, 14));
        page.add(pageTitle, BorderLayout.NORTH);
        page.add(card, BorderLayout.CENTER);
        return page;
    }

    // ----- Định dạng bảng nhân viên -----
    private void styleStaffTable() {
        JTable t = empTable;
        t.setRowHeight(42);
        t.setShowGrid(false);
        t.setIntercellSpacing(new Dimension(0, 0));
        t.setBackground(Color.WHITE);

        // tiêu đề cột: chữ hoa nhỏ, màu xám
        t.getTableHeader().setPreferredSize(new Dimension(0, 38));
        t.getTableHeader().setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tb, Object v, boolean s, boolean f, int r, int c) {
                super.getTableCellRendererComponent(tb, v, false, false, r, c);
                setText(v == null ? "" : v.toString().toUpperCase(Locale.forLanguageTag("vi")));
                setFont(new Font("Segoe UI", Font.BOLD, 11));
                setForeground(UIStyle.MUTED);
                setBackground(Color.WHITE);
                setOpaque(true);
                setHorizontalAlignment(LEFT);
                setBorder(new EmptyBorder(0, 10, 0, 10));
                return this;
            }
        });

        TableCellRenderer plain = new PlainRenderer();
        t.getColumnModel().getColumn(0).setCellRenderer(plain);
        t.getColumnModel().getColumn(1).setCellRenderer(new NameRenderer());
        t.getColumnModel().getColumn(2).setCellRenderer(plain);
        t.getColumnModel().getColumn(3).setCellRenderer(new PillRenderer(false));
        t.getColumnModel().getColumn(4).setCellRenderer(plain);
        t.getColumnModel().getColumn(5).setCellRenderer(plain);
        t.getColumnModel().getColumn(6).setCellRenderer(new PillRenderer(true));
        t.getColumnModel().getColumn(COL_ACTION).setCellRenderer(new ActionRenderer());

        int[] w = {90, 230, 90, 160, 120, 260, 120, 84};
        for (int i = 0; i < w.length; i++) t.getColumnModel().getColumn(i).setPreferredWidth(w[i]);
        TableColumn act = t.getColumnModel().getColumn(COL_ACTION);
        act.setMinWidth(84);
        act.setMaxWidth(84);

        // bấm biểu tượng sửa / nghỉ việc trong cột "Thao tác"
        t.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int hit = actionHit(e.getPoint());
                if (hit == 0) return;
                int vr = empTable.rowAtPoint(e.getPoint());
                int mr = empTable.convertRowIndexToModel(vr);
                if (hit == 1) showEmpDialog(mr);
                else toggleActive(mr);
            }
        });
        t.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int hit = actionHit(e.getPoint());
                empTable.setCursor(hit == 0 ? Cursor.getDefaultCursor() : Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                if (hit == 1) {
                    empTable.setToolTipText("Sửa thông tin nhân viên");
                } else if (hit == 2) {
                    int mr = empTable.convertRowIndexToModel(empTable.rowAtPoint(e.getPoint()));
                    boolean on = "Đang làm".equals(String.valueOf(empModel.getValueAt(mr, 6)));
                    empTable.setToolTipText(on ? "Cho nghỉ việc" : "Cho đi làm lại");
                } else {
                    empTable.setToolTipText(null);
                }
            }
        });
    }

    private static final int ICON = 16, EDIT_X = 10, DEL_X = 42;

    /** 0: không trúng; 1: biểu tượng sửa; 2: biểu tượng nghỉ việc / đi làm lại. */
    private int actionHit(Point p) {
        int vr = empTable.rowAtPoint(p), vc = empTable.columnAtPoint(p);
        if (vr < 0 || vc < 0 || empTable.convertColumnIndexToModel(vc) != COL_ACTION) return 0;
        Rectangle r = empTable.getCellRect(vr, vc, false);
        int x = p.x - r.x;
        if (x >= EDIT_X - 3 && x < EDIT_X + ICON + 3) return 1;
        if (x >= DEL_X - 3 && x < DEL_X + ICON + 3) return 2;
        return 0;
    }

    private static Color rowColor(boolean sel, int row) {
        if (sel) return UIStyle.PRIMARY_LIGHT;
        return row % 2 == 0 ? Color.WHITE : new Color(0xFAFAFB);
    }

    /** Ô chữ thường: sọc nền xen kẽ; cột mã NV màu xám. */
    private static final class PlainRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int r, int c) {
            super.getTableCellRendererComponent(t, v == null ? "" : v, false, false, r, c);
            setFont(UIStyle.PLAIN);
            setForeground(t.convertColumnIndexToModel(c) == 0 ? UIStyle.MUTED : new Color(0x111827));
            setBackground(rowColor(sel, r));
            setOpaque(true);
            setBorder(new EmptyBorder(0, 10, 0, 10));
            return this;
        }
    }

    /** Ô tên: avatar tròn chữ cái đầu + họ tên (chữ vẽ bằng JLabel để đồng bộ với các cột khác). */
    private static final class NameRenderer extends DefaultTableCellRenderer {
        private boolean selected;

        NameRenderer() {
            setOpaque(true);
        }

        @Override
        public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int r, int c) {
            super.getTableCellRendererComponent(t, v == null ? "" : v, false, false, r, c);
            selected = sel;
            setFont(UIStyle.PLAIN);
            setForeground(new Color(0x111827));
            setBackground(rowColor(sel, r));
            setBorder(new EmptyBorder(0, 48, 0, 10));   // chừa chỗ cho avatar bên trái
            return this;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);   // vẽ nền + họ tên như các cột khác

            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int d = 28, x = 10, y = (getHeight() - d) / 2;
            g2.setColor(selected ? Color.WHITE : UIStyle.PRIMARY_LIGHT);   // dòng được chọn: nền trắng để avatar không bị chìm
            g2.fillOval(x, y, d, d);

            String lw = lastWord(getText());
            String letter = lw.isEmpty() ? "?" : lw.substring(0, 1).toUpperCase();
            g2.setFont(new Font("Segoe UI", Font.BOLD, 13));
            FontMetrics fm = g2.getFontMetrics();
            g2.setColor(UIStyle.PRIMARY_DARK);
            g2.drawString(letter, x + (d - fm.stringWidth(letter)) / 2, y + (d - fm.getHeight()) / 2 + fm.getAscent());
            g2.dispose();
        }
    }

    /** Ô dạng "viên thuốc" bo tròn (chức vụ hoặc trạng thái). */
    private static final class PillRenderer extends JPanel implements TableCellRenderer {
        private static final Color[][] PALETTE = {
                {new Color(0xFFEDD5), new Color(0xC2410C)},
                {new Color(0xFCE7F3), new Color(0xBE185D)},
                {new Color(0xE0E7FF), new Color(0x4338CA)},
                {new Color(0xFEF3C7), new Color(0xB45309)},
                {new Color(0xCFFAFE), new Color(0x0E7490)},
                {new Color(0xF3E8FF), new Color(0x7E22CE)}
        };
        private final boolean status;
        private String text = "";
        private Color bg = Color.WHITE, pillBg, pillFg;

        PillRenderer(boolean status) {
            this.status = status;
            setOpaque(false);
        }

        @Override
        public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int r, int c) {
            text = v == null ? "" : v.toString();
            bg = rowColor(sel, r);
            if (status) {
                boolean on = "Đang làm".equals(text);
                pillBg = on ? new Color(0xDCFCE7) : new Color(0xF3F4F6);
                pillFg = on ? new Color(0x15803D) : new Color(0x6B7280);
            } else {
                Color[] p = PALETTE[Math.abs(text.hashCode()) % PALETTE.length];
                pillBg = p[0];
                pillFg = p[1];
            }
            return this;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setColor(bg);
            g2.fillRect(0, 0, getWidth(), getHeight());
            if (!text.isEmpty()) {
                g2.setFont(new Font("Segoe UI", Font.BOLD, 12));
                FontMetrics fm = g2.getFontMetrics();
                int h = 24, x = 10, y = (getHeight() - h) / 2;
                int w = Math.min(getWidth() - 20, fm.stringWidth(text) + 24);
                g2.setColor(pillBg);
                g2.fillRoundRect(x, y, w, h, h, h);
                g2.setColor(pillFg);
                g2.drawString(text, x + (w - fm.stringWidth(text)) / 2, y + (h - fm.getHeight()) / 2 + fm.getAscent());
            }
            g2.dispose();
        }
    }

    /** Cột thao tác: biểu tượng sửa + (thùng rác = cho nghỉ việc | mũi tên = cho đi làm lại). */
    private static final class ActionRenderer extends JPanel implements TableCellRenderer {
        private Color bg = Color.WHITE;
        private boolean active = true;

        ActionRenderer() {
            setOpaque(false);
        }

        @Override
        public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int r, int c) {
            bg = rowColor(sel, r);
            Object st = t.getModel().getValueAt(t.convertRowIndexToModel(r), 6);
            active = "Đang làm".equals(String.valueOf(st));
            return this;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setColor(bg);
            g2.fillRect(0, 0, getWidth(), getHeight());
            int y = (getHeight() - ICON) / 2;
            drawIcon(g2, ICON_EDIT, EDIT_X, y, ICON, new Color(0x374151));
            if (active) drawIcon(g2, ICON_TRASH, DEL_X, y, ICON, UIStyle.PRIMARY);
            else drawIcon(g2, ICON_RESTORE, DEL_X, y, ICON, new Color(0x16A34A));
            g2.dispose();
        }
    }

    // ----- Biểu tượng vẽ tay (không cần ảnh ngoài) -----
    private static final int ICON_FUNNEL = 0, ICON_SORT = 1, ICON_EDIT = 2, ICON_TRASH = 3, ICON_RESTORE = 4;

    private static void drawIcon(Graphics2D g2, int type, int x, int y, int s, Color c) {
        Graphics2D g = (Graphics2D) g2.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(c);
        g.translate(x, y);
        switch (type) {
            case ICON_FUNNEL:
                g.drawPolygon(
                        new int[]{0, s, (int) (s * 0.62), (int) (s * 0.62), (int) (s * 0.38), (int) (s * 0.38)},
                        new int[]{2, 2, (int) (s * 0.5), s - 1, s - 3, (int) (s * 0.5)}, 6);
                break;
            case ICON_SORT: {
                int a = (int) (s * 0.3), b = (int) (s * 0.7);
                g.drawLine(a, s - 2, a, 2);
                g.drawLine(a - 3, 5, a, 2);
                g.drawLine(a + 3, 5, a, 2);
                g.drawLine(b, 2, b, s - 2);
                g.drawLine(b - 3, s - 5, b, s - 2);
                g.drawLine(b + 3, s - 5, b, s - 2);
                break;
            }
            case ICON_EDIT:
                g.drawLine(s - 4, 2, 3, s - 5);
                g.drawLine(s - 2, 4, 5, s - 3);
                g.drawLine(s - 4, 2, s - 2, 4);
                g.drawLine(3, s - 5, 2, s - 2);
                g.drawLine(2, s - 2, 5, s - 3);
                break;
            case ICON_TRASH:
                g.drawLine(2, 4, s - 2, 4);
                g.drawPolyline(new int[]{s / 2 - 2, s / 2 - 2, s / 2 + 2, s / 2 + 2}, new int[]{4, 2, 2, 4}, 4);
                g.drawPolyline(new int[]{4, 5, s - 5, s - 4}, new int[]{6, s - 1, s - 1, 6}, 4);
                break;
            default: { // ICON_RESTORE
                g.drawArc(3, 3, s - 6, s - 6, 50, 290);
                double r = (s - 6) / 2.0;
                int px = (int) (s / 2.0 + r * Math.cos(Math.toRadians(50)));
                int py = (int) (s / 2.0 - r * Math.sin(Math.toRadians(50)));
                g.drawLine(px, py, px + 4, py + 1);
                g.drawLine(px, py, px - 1, py - 4);
                break;
            }
        }
        g.dispose();
    }

    /** Nút vuông bo góc chứa một biểu tượng (lọc / sắp xếp). */
    private static final class IconButton extends JButton {
        private final int icon;
        private boolean highlight;

        IconButton(int icon, String tip) {
            this.icon = icon;
            setToolTipText(tip);
            setPreferredSize(new Dimension(36, 36));
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        void setHighlight(boolean h) {
            highlight = h;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(getModel().isRollover() ? new Color(0xF9FAFB) : Color.WHITE);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
            g2.setColor(highlight ? UIStyle.PRIMARY : UIStyle.BORDER);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
            drawIcon(g2, icon, (getWidth() - ICON) / 2, (getHeight() - ICON) / 2, ICON,
                    highlight ? UIStyle.PRIMARY : new Color(0x374151));
            g2.dispose();
        }
    }

    /** Ô tìm kiếm bo góc, có chữ gợi ý khi để trống. */
    private static final class SearchField extends JTextField {
        private final String hint;

        SearchField(String hint, int cols) {
            super(cols);
            this.hint = hint;
            setFont(UIStyle.PLAIN);
            setOpaque(false);
            setBorder(new EmptyBorder(7, 12, 7, 12));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(Color.WHITE);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
            g2.setColor(hasFocus() ? UIStyle.PRIMARY : UIStyle.BORDER);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
            g2.dispose();
            super.paintComponent(g);
            if (getText().isEmpty()) {
                Graphics2D h = (Graphics2D) g.create();
                h.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                h.setFont(getFont());
                h.setColor(new Color(0x9CA3AF));
                FontMetrics fm = h.getFontMetrics();
                Insets in = getInsets();
                h.drawString(hint, in.left, (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
                h.dispose();
            }
        }
    }

    // ----- Lọc / sắp xếp -----
    private void showFilterMenu(Component anchor) {
        JPopupMenu m = new JPopupMenu();
        ButtonGroup g = new ButtonGroup();
        addRadio(m, g, "Tất cả vai trò", roleFilter == null, () -> setRoleFilter(null));
        for (Position p : positions) {
            String n = p.getPositionName();
            addRadio(m, g, n, n.equals(roleFilter), () -> setRoleFilter(n));
        }
        m.show(anchor, 0, anchor.getHeight() + 2);
    }

    private void showSortMenu(Component anchor) {
        String[] names = {"Mã nhân viên: tăng dần", "Mã nhân viên: giảm dần", "Tên: A → Z", "Tên: Z → A"};
        JPopupMenu m = new JPopupMenu();
        ButtonGroup g = new ButtonGroup();
        for (int i = 0; i < names.length; i++) {
            int idx = i;
            addRadio(m, g, names[i], sortIdx == i, () -> {
                sortIdx = idx;
                applySort();
            });
        }
        m.show(anchor, 0, anchor.getHeight() + 2);
    }

    private static void addRadio(JPopupMenu m, ButtonGroup g, String text, boolean selected, Runnable action) {
        JRadioButtonMenuItem it = new JRadioButtonMenuItem(text, selected);
        it.setFont(UIStyle.PLAIN);
        it.addActionListener(e -> action.run());
        g.add(it);
        m.add(it);
    }

    private void setRoleFilter(String role) {
        roleFilter = role;
        if (btnFilter != null) btnFilter.setHighlight(role != null);
        applyEmpFilter();
    }

    private void applySort() {
        int col = sortIdx <= 1 ? 0 : 1;
        SortOrder order = (sortIdx == 0 || sortIdx == 2) ? SortOrder.ASCENDING : SortOrder.DESCENDING;
        empSorter.setSortKeys(List.of(new RowSorter.SortKey(col, order)));
    }

    private void applyEmpFilter() {
        List<RowFilter<Object, Object>> fs = new ArrayList<>();
        String kw = txtSearch.getText().trim();
        if (!kw.isEmpty()) fs.add(RowFilter.regexFilter("(?iu)" + Pattern.quote(kw), 0, 1)); // mã hoặc tên
        if (roleFilter != null) fs.add(RowFilter.regexFilter("^" + Pattern.quote(roleFilter) + "$", 3));
        empSorter.setRowFilter(fs.isEmpty() ? null : RowFilter.andFilter(fs));
    }

    // ----- Cho nghỉ việc / đi làm lại -----
    private void toggleActive(int modelRow) {
        Employee e = staff.get(modelRow);
        boolean isActive = e.getStatus() == EmployeeStatus.ACTIVE;
        boolean active = !isActive;   // trạng thái muốn chuyển sang
        if (!active && e.getEmployeeId().equals(acc.getEmployeeId())) { warn("Không thể cho chính mình nghỉ việc."); return; }
        int ok = JOptionPane.showConfirmDialog(this,
                (active ? "Cho đi làm lại: " : "Cho nghỉ việc: ") + e.getFullName() + "?"
                        + (active ? "" : "\nTài khoản sẽ bị khóa và các ca từ ngày mai bị hủy (không xóa dữ liệu)."),
                "Xác nhận", JOptionPane.YES_NO_OPTION);
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

    // ----- Thêm / sửa nhân viên -----
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
        } else {
            try {
                detailDao.loadInto(cur);   // nạp hồ sơ + ngân hàng (findAll không mang theo)
            } catch (SQLException ex) {
                warn("Không tải được hồ sơ chi tiết: " + ex.getMessage());
                return;
            }
        }

        Employee e = EmployeeDialog.show(this, cur, newId, positions);
        if (e == null) return;   // bấm Hủy

        try {
            if (edit) {
                empDao.update(e);                  // birthDay, citizenId, hireDate lấy từ form
            } else {
                e.setStatus(EmployeeStatus.ACTIVE);
                empDao.insert(e, Role.EMPLOYEE);   // kèm Account mặc định
            }
            detailDao.save(e);                     // 2 bảng phụ
        } catch (SQLException ex) {
            warn("Không lưu được nhân viên: " + ex.getMessage());
            return;
        }
        reloadStaff();
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
        if (staff.isEmpty()) loadStaffData();   // cần danh sách NV để hiện tên người nhận / soạn thông báo
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
}