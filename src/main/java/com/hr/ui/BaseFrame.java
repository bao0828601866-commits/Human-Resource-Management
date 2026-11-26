package com.hr.ui;

import com.hr.controller.NotificationController;
import com.hr.dao.EmployeeDAO;
import com.hr.dao.EmployeeDetailDAO;
import com.hr.model.Account;
import com.hr.model.Employee;
import com.hr.model.enums.EmployeeStatus;
import com.hr.model.enums.Gender;
import com.hr.model.enums.SalaryType;
import com.hr.ui.auth.ChangePasswordDialog;
import com.hr.ui.auth.LoginFrame;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public abstract class BaseFrame extends JFrame {
    protected static final Color PAGE_BG = new Color(0xF9FAFB);

    protected final Account acc;
    private static final String[] GENDER_VI = {"Nam", "Nữ", "Khác"};
    private static final Gender[] GENDERS = {Gender.MALE, Gender.FEMALE, Gender.OTHER};
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /** Hồ sơ của người đăng nhập, đọc từ bảng Employee (JOIN Position) khi mở khung. */
    protected final Employee profile;
    private final String roleLabel;

    protected final NotificationController notificationController = new NotificationController();

    // --- Hồ sơ nhân viên (chỉ xem) ---
    private final EmployeeDAO profileEmpDao = new EmployeeDAO();
    private final EmployeeDetailDAO profileDetailDao = new EmployeeDetailDAO();
    private final JPanel profileBody = UIStyle.panel(new BorderLayout());

    private final CardLayout cards = new CardLayout();
    private final JPanel content = new JPanel(cards);
    private final JPanel nav = new JPanel();
    private final Map<String, NavButton> buttons = new LinkedHashMap<>();
    private final Map<String, String> labels = new HashMap<>();
    private final Map<String, Runnable> onShow = new HashMap<>();

    /** Một dòng thông báo: nội dung HTML + đã đọc hay chưa. */
    public static final class NotiItem {
        public final String html;
        public final boolean read;

        public NotiItem(String html, boolean read) {
            this.html = html;
            this.read = read;
        }
    }

    protected BaseFrame(Account acc, String roleLabel) {
        if (acc == null) {
            throw new IllegalArgumentException("Account không được null.");
        }
        this.acc = acc;
        this.profile = loadProfile(acc.getEmployeeId());
        this.roleLabel = roleLabel == null ? "" : roleLabel;

        setTitle("HRM - " + this.roleLabel);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        add(buildSidebar(), BorderLayout.WEST);
        content.setBackground(PAGE_BG);
        add(content, BorderLayout.CENTER);

        setMinimumSize(new Dimension(1000, 620));
        setSize(1240, 740);                 // kích thước khi người dùng bấm "thu nhỏ" (restore)
        setLocationRelativeTo(null);
        setExtendedState(JFrame.MAXIMIZED_BOTH);   // mặc định mở toàn màn hình
    }

    /** Đọc hồ sơ từ DB. Lỗi DB / không có dòng -> hồ sơ tối thiểu để khung vẫn mở được. */
    private static Employee loadProfile(String employeeId) {
        try {
            Employee e = new EmployeeDAO().findById(employeeId);
            if (e != null) return e;
        } catch (SQLException ex) {
            System.err.println("Không đọc được hồ sơ " + employeeId + ": " + ex.getMessage());
        }
        Employee e = new Employee();
        e.setEmployeeId(employeeId);
        e.setFullName(employeeId);
        e.setGender(Gender.OTHER);
        e.setPositionName("");
        e.setSalaryType(SalaryType.HOURLY);
        e.setStatus(EmployeeStatus.ACTIVE);
        return e;
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }

    /** "Nguyễn Văn An" -> "An" (chữ cái đầu của tên cho avatar). */
    private static String lastWord(String s) {
        String t = s == null ? "" : s.trim();
        int i = t.lastIndexOf(' ');
        return i < 0 ? t : t.substring(i + 1);
    }

    // =====================================================================
    // API cho lớp con
    // =====================================================================

    /** Nút menu cấp 1: bấm vào sẽ hiện trang có cùng key. */
    protected void addMenu(String key, String label) {
        NavButton b = new NavButton(label, 0);
        b.addActionListener(e -> show(key));
        buttons.put(key, b);
        labels.put(key, label);
        nav.add(b);
    }

    /** Nhóm menu có các mục con (bấm tiêu đề để thu gọn / mở rộng). */
    protected void addGroup(String title, String[] keys, String[] subLabels) {
        if (keys == null || subLabels == null || keys.length != subLabels.length) {
            throw new IllegalArgumentException("keys và subLabels phải khác null và cùng độ dài.");
        }

        JPanel sub = new JPanel();
        sub.setOpaque(false);
        sub.setLayout(new BoxLayout(sub, BoxLayout.Y_AXIS));
        sub.setAlignmentX(Component.LEFT_ALIGNMENT);

        NavButton head = new NavButton("▾  " + title, 0);
        boolean[] open = {true};
        head.addActionListener(e -> {
            open[0] = !open[0];
            sub.setVisible(open[0]);
            head.setText((open[0] ? "▾  " : "▸  ") + title);
            nav.revalidate();
            nav.repaint();
        });
        nav.add(head);

        for (int i = 0; i < keys.length; i++) {
            String key = keys[i];
            NavButton b = new NavButton(subLabels[i], 26);
            b.addActionListener(e -> show(key));
            buttons.put(key, b);
            labels.put(key, subLabels[i]);
            sub.add(b);
        }
        nav.add(sub);
    }

    protected void addPage(String key, JComponent page) {
        addPage(key, page, null);
    }

    /** Đăng ký trang nội dung; onShow (nếu có) chạy mỗi lần trang được mở. */
    protected void addPage(String key, JComponent page, Runnable onShowAction) {
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setBackground(PAGE_BG);
        wrap.setBorder(new EmptyBorder(16, 16, 16, 16));
        wrap.add(page, BorderLayout.CENTER);
        content.add(wrap, key);
        if (onShowAction != null) onShow.put(key, onShowAction);
    }

    protected void show(String key) {
        cards.show(content, key);
        buttons.forEach((k, b) -> b.setActive(k.equals(key)));
        Runnable r = onShow.get(key);
        if (r != null) r.run();
    }

    protected void start(String firstKey) {
        show(firstKey);
    }

    /** Hiện số đếm cạnh tên mục menu, ví dụ "Thông báo (2)". */
    protected void setBadge(String key, int n) {
        NavButton b = buttons.get(key);
        if (b != null) b.setText(labels.get(key) + (n > 0 ? "  (" + n + ")" : ""));
    }

    /** Trang thông báo không lưu trạng thái đọc vào DB (dùng cho hộp thư tạm của quản lý). */
    protected JComponent notificationPage(String key, List<String> items) {
        List<NotiItem> l = new ArrayList<>();
        if (items != null) {
            for (String s : items) l.add(new NotiItem(s, false));
        }
        return notificationPage(key, l, null);
    }

    /**
     * Trang thông báo: danh sách + nút "đánh dấu đã đọc".
     * onMarkRead (nếu có) chạy khi bấm nút, dùng để ghi NotificationRead vào DB;
     * nếu nó ném IllegalStateException thì giao diện không đổi.
     */
    protected JComponent notificationPage(String key, List<NotiItem> items, Runnable onMarkRead) {
        JPanel list = new JPanel();
        list.setOpaque(false);
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        List<JLabel> rows = new ArrayList<>();
        int unread = 0;

        if (items != null) {
            for (NotiItem it : items) {
                JLabel l = new JLabel("<html>" + (it.html == null ? "" : it.html) + "</html>");
                l.setFont(UIStyle.PLAIN);
                l.setOpaque(true);
                l.setBackground(Color.WHITE);
                l.setAlignmentX(Component.LEFT_ALIGNMENT);
                l.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));
                styleNotiRow(l, it.read);
                if (!it.read) unread++;
                rows.add(l);
                list.add(l);
                list.add(Box.createVerticalStrut(8));
            }
        }
        setBadge(key, unread);

        JButton readAll = UIStyle.button("Đánh dấu tất cả đã đọc", () -> {
            if (onMarkRead != null) {
                try {
                    onMarkRead.run();
                } catch (IllegalStateException ex) {
                    warn(ex.getMessage());
                    return;
                }
            }
            setBadge(key, 0);
            for (JLabel l : rows) styleNotiRow(l, true);
        });

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(list, BorderLayout.NORTH);

        JPanel page = UIStyle.panel(new BorderLayout(0, 10));
        page.add(UIStyle.row(readAll), BorderLayout.NORTH);
        JScrollPane sp = new JScrollPane(top);
        sp.setBorder(null);
        sp.setOpaque(false);
        sp.getViewport().setOpaque(false);
        page.add(sp, BorderLayout.CENTER);
        return page;
    }

    private static void styleNotiRow(JLabel l, boolean read) {
        l.setForeground(read ? UIStyle.MUTED : Color.BLACK);
        l.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 4, 0, 0, read ? UIStyle.BORDER : UIStyle.PRIMARY),
                new EmptyBorder(12, 12, 12, 12)));
    }

    protected void warn(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Thông báo", JOptionPane.WARNING_MESSAGE);
    }

    protected void info(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Thông báo", JOptionPane.INFORMATION_MESSAGE);
    }

    // =====================================================================
    // Hồ sơ nhân viên (chỉ xem) - dùng chung cho Manager và Employee
    // =====================================================================

    /** Gọi trong constructor của lớp con để thêm menu + trang "Hồ sơ nhân viên". */
    protected void addProfileMenu() {
        addMenu("profile", "Hồ sơ nhân viên");
        profileBody.setOpaque(false);
        addPage("profile", profileBody, this::refreshProfilePage);
    }

    /** Đọc lại hồ sơ đầy đủ từ DB mỗi lần mở trang. */
    private void refreshProfilePage() {
        Employee e = profile;
        try {
            Employee fresh = profileEmpDao.findById(acc.getEmployeeId());
            if (fresh != null) {
                profileDetailDao.loadInto(fresh);   // quê quán, dân tộc, học vấn, trường, tạm trú, ngân hàng
                e = fresh;
            }
        } catch (SQLException ex) {
            warn("Không tải được hồ sơ: " + ex.getMessage());
        }
        profileBody.removeAll();
        profileBody.add(buildProfileContent(e), BorderLayout.CENTER);
        profileBody.revalidate();
        profileBody.repaint();
    }

    private JComponent buildProfileContent(Employee e) {
        // --- Thẻ đầu trang: avatar + tên + chức vụ + nút đổi mật khẩu ---
        JLabel name = new JLabel(nz(e.getFullName()));
        name.setFont(new Font("Segoe UI", Font.BOLD, 20));
        JPanel who = UIStyle.panel(null);
        who.setLayout(new BoxLayout(who, BoxLayout.Y_AXIS));
        who.add(name);
        who.add(Box.createVerticalStrut(2));
        who.add(UIStyle.muted(nz(e.getPositionName()) + " · " + nz(e.getEmployeeId())));

        JPanel head = new JPanel(new BorderLayout(16, 0));
        head.setBackground(Color.WHITE);
        head.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIStyle.BORDER), new EmptyBorder(14, 16, 14, 16)));
        head.add(new UIStyle.Avatar(lastWord(nz(e.getFullName())), 56, UIStyle.PRIMARY_LIGHT, UIStyle.PRIMARY_DARK),
                BorderLayout.WEST);
        head.add(who, BorderLayout.CENTER);

        // Nút đổi mật khẩu: nằm bên phải, GridBagLayout mặc định căn giữa theo chiều dọc
        JPanel pwdWrap = UIStyle.panel(new GridBagLayout());
        pwdWrap.add(UIStyle.button("Đổi mật khẩu", this::changePassword));
        head.add(pwdWrap, BorderLayout.EAST);

        // --- 3 nhóm thông tin ---
        JPanel personal = infoCard("Thông tin cá nhân", new String[][]{
                {"Họ tên", nz(e.getFullName())},
                {"Giới tính", genderVi(e.getGender())},
                {"Ngày sinh", date(e.getBirthDay())},
                {"Số CCCD", nz(e.getCitizenId())},
                {"Quê quán", nz(e.getHometown())},
                {"Dân tộc", nz(e.getEthnicity())},
                {"Trình độ học vấn", nz(e.getEducationLevel())},
                {"Trường", nz(e.getSchool())}});

        JPanel contact = infoCard("Thông tin liên hệ", new String[][]{
                {"Số điện thoại", nz(e.getPhoneNumber())},
                {"Email", nz(e.getEmail())},
                {"Địa chỉ thường trú", nz(e.getPermanentAddress())},
                {"Địa chỉ tạm trú", nz(e.getAddress())}});

        String salary = e.getSalaryType() == SalaryType.FIXED
                ? "Full-time · " + money(e.getSalaryRate()) + " / tháng"
                : "Part-time · " + money(e.getSalaryRate()) + " / giờ";
        JPanel work = infoCard("Công việc & ngân hàng", new String[][]{
                {"Mã nhân viên", nz(e.getEmployeeId())},
                {"Chức vụ", nz(e.getPositionName())},
                {"Mức lương", salary},
                {"Ngày vào làm", date(e.getHireDate())},
                {"Ngân hàng", nz(e.getBankName())},
                {"Số tài khoản", nz(e.getBankAccount())}});

        JPanel cols = UIStyle.panel(new GridLayout(1, 3, 12, 0));
        cols.add(personal);
        cols.add(contact);
        cols.add(work);

        JPanel stack = UIStyle.panel(new BorderLayout(0, 12));
        stack.add(head, BorderLayout.NORTH);
        stack.add(cols, BorderLayout.CENTER);

        JPanel wrapper = UIStyle.panel(new BorderLayout());
        wrapper.add(stack, BorderLayout.NORTH);

        JLabel title = new JLabel("Hồ sơ nhân viên");
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));

        JScrollPane sp = new JScrollPane(wrapper);
        sp.setBorder(null);
        sp.setOpaque(false);
        sp.getViewport().setOpaque(false);
        sp.getVerticalScrollBar().setUnitIncrement(16);

        JPanel page = UIStyle.panel(new BorderLayout(0, 14));
        page.add(title, BorderLayout.NORTH);
        page.add(sp, BorderLayout.CENTER);
        return page;
    }

    /** Thẻ gồm nhiều dòng "nhãn nhỏ phía trên, giá trị phía dưới" - chỉ đọc. */
    private JPanel infoCard(String title, String[][] rows) {
        JPanel p = new JPanel(new GridBagLayout());
        p.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.NORTHWEST;

        for (String[] r : rows) {
            JTextArea v = new JTextArea(r[1].isBlank() ? "—" : r[1]);
            v.setEditable(false);
            v.setFocusable(false);
            v.setLineWrap(true);
            v.setWrapStyleWord(true);
            v.setOpaque(false);
            v.setFont(UIStyle.PLAIN);
            v.setBorder(null);

            JPanel item = new JPanel(new BorderLayout(0, 2));
            item.setOpaque(false);
            item.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, UIStyle.BORDER), new EmptyBorder(8, 0, 8, 0)));
            item.add(UIStyle.muted(r[0]), BorderLayout.NORTH);
            item.add(v, BorderLayout.CENTER);
            p.add(item, c);
            c.gridy++;
        }
        c.weighty = 1;
        c.fill = GridBagConstraints.BOTH;
        p.add(new JLabel(), c);   // đẩy các dòng lên trên
        return UIStyle.card(title, p);
    }

    private static String genderVi(Gender g) {
        for (int i = 0; i < GENDERS.length; i++) if (GENDERS[i] == g) return GENDER_VI[i];
        return "";
    }

    private static String date(LocalDate d) {
        return d == null ? "" : d.format(DATE_FMT);
    }

    private static String money(BigDecimal v) {
        return NumberFormat.getInstance(Locale.forLanguageTag("vi-VN"))
                .format(v == null ? BigDecimal.ZERO : v) + " ₫";
    }

    // =====================================================================
    // Đổi mật khẩu (mở từ nút trong trang "Hồ sơ nhân viên")
    // =====================================================================
    private void changePassword() {
        if (ChangePasswordDialog.show(this, acc.getEmployeeId(), false, null)) {
            info("Đổi mật khẩu thành công.");
        }
    }

    // =====================================================================
    // Sidebar
    // =====================================================================
    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setPaint(new GradientPaint(0, 0, new Color(0xB91C1C), 0, getHeight(), new Color(0xEF4444)));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        sidebar.setPreferredSize(new Dimension(240, 0));

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.setBorder(new EmptyBorder(14, 10, 14, 10));
        top.add(buildProfileCard(), BorderLayout.CENTER);

        nav.setOpaque(false);
        nav.setLayout(new BoxLayout(nav, BoxLayout.Y_AXIS));
        nav.setBorder(new EmptyBorder(0, 8, 0, 8));

        NavButton logout = new NavButton("Đăng xuất", 0);
        logout.addActionListener(e -> logout());
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);
        bottom.setBorder(new EmptyBorder(0, 8, 12, 8));
        bottom.add(logout, BorderLayout.CENTER);

        sidebar.add(top, BorderLayout.NORTH);
        sidebar.add(nav, BorderLayout.CENTER);
        sidebar.add(bottom, BorderLayout.SOUTH);
        return sidebar;
    }

    /** Khung tên + vai trò ở đầu sidebar (chỉ hiển thị, không bấm được). */
    private JPanel buildProfileCard() {
        String displayName = (profile != null && profile.getFullName() != null) ? profile.getFullName() : acc.getEmployeeId();

        JPanel card = new JPanel(new BorderLayout(10, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(255, 255, 255, 50));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 18, 18);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(10, 12, 10, 12));

        JLabel name = new JLabel(displayName);
        name.setFont(new Font("Segoe UI", Font.BOLD, 14));
        name.setForeground(Color.WHITE);

        JLabel role = new JLabel(roleLabel + " · " + acc.getEmployeeId());
        role.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        role.setForeground(new Color(0xFEE2E2));

        JPanel info = new JPanel();
        info.setOpaque(false);
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        info.add(Box.createVerticalGlue());
        info.add(name);
        info.add(role);
        info.add(Box.createVerticalGlue());

        card.add(new UIStyle.Avatar(lastWord(displayName), 42, Color.WHITE, UIStyle.PRIMARY_DARK),
                BorderLayout.WEST);
        card.add(info, BorderLayout.CENTER);
        return card;
    }

    private void logout() {
        dispose();
        new LoginFrame().setVisible(true);
    }

    // =====================================================================
    // Nút menu: trong suốt, sáng lên khi hover / đang chọn
    // =====================================================================
    private static class NavButton extends JButton {
        private boolean active;

        NavButton(String text, int indent) {
            super(text);
            setFont(new Font("Segoe UI", Font.PLAIN, 14));
            setForeground(Color.WHITE);
            setContentAreaFilled(false);
            setOpaque(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setRolloverEnabled(true);
            setHorizontalAlignment(LEFT);
            setBorder(new EmptyBorder(10, 14 + indent, 10, 10));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setAlignmentX(Component.LEFT_ALIGNMENT);
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        }

        void setActive(boolean a) {
            active = a;
            setFont(getFont().deriveFont(a ? Font.BOLD : Font.PLAIN));
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            if (active || getModel().isRollover()) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(255, 255, 255, active ? 70 : 35));
                g2.fillRoundRect(0, 2, getWidth(), getHeight() - 4, 16, 16);
                g2.dispose();
            }
            super.paintComponent(g);
        }
    }
}