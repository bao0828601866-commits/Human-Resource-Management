package com.hr.ui;

import com.hr.controller.EmployeeController;
import com.hr.controller.NotificationController;
import com.hr.dao.EmployeeDAO;
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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public abstract class BaseFrame extends JFrame {
    protected static final Color PAGE_BG = new Color(0xF9FAFB);

    protected final Account acc;
    private static final String[] GENDER_VI = {"Nam", "Nữ", "Khác"};
    private static final Gender[] GENDERS = {Gender.MALE, Gender.FEMALE, Gender.OTHER};

    /** Hồ sơ của người đăng nhập, đọc từ bảng Employee (JOIN Position) khi mở khung. */
    protected final Employee profile;
    private final String roleLabel;

    protected final EmployeeController employeeController = new EmployeeController();
    protected final NotificationController notificationController = new NotificationController();

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

    /** Khung tên + vai trò; bấm vào để mở hộp thoại thông tin cá nhân. */
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
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        card.setToolTipText("Bấm để xem / sửa thông tin cá nhân");

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
        card.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                showProfile();
            }
        });
        return card;
    }

    private void logout() {
        dispose();
        new LoginFrame().setVisible(true);
    }

    // =====================================================================
    // Thông tin cá nhân + đổi mật khẩu
    // =====================================================================
    private void showProfile() {
        JDialog d = new JDialog(this, "Thông tin cá nhân", true);

        JTextField code = readOnly(acc.getEmployeeId());
        JTextField name = readOnly(nz(profile.getFullName()));
        JTextField pos = readOnly(nz(profile.getPositionName()) + (profile.isFullTime() ? " (Full-time)" : " (Part-time)"));
        JTextField mail = readOnly(nz(profile.getEmail()));
        JTextField phone = new JTextField(nz(profile.getPhoneNumber()), 20);
        JTextField addr = new JTextField(nz(profile.getAddress()), 20);
        JComboBox<String> gender = new JComboBox<>(GENDER_VI);
        for (int i = 0; i < GENDERS.length; i++) {
            if (GENDERS[i] == profile.getGender()) gender.setSelectedIndex(i);
        }

        JPanel form = new JPanel(new GridBagLayout());
        addRow(form, 0, "Mã đăng nhập:", code);
        addRow(form, 1, "Họ tên:", name);
        addRow(form, 2, "Chức vụ:", pos);
        addRow(form, 3, "Email:", mail);
        addRow(form, 4, "Số điện thoại:", phone);
        addRow(form, 5, "Địa chỉ:", addr);
        addRow(form, 6, "Giới tính:", gender);

        JPanel actions = UIStyle.row(
                UIStyle.button("Lưu thông tin", () -> {
                    Gender newGender = GENDERS[gender.getSelectedIndex()];
                    try {
                        boolean ok = employeeController.updateProfile(
                                acc.getEmployeeId(), phone.getText(), addr.getText(), newGender);
                        if (!ok) {
                            warn("Không tìm thấy nhân viên để cập nhật.");
                            return;
                        }
                    } catch (IllegalArgumentException | IllegalStateException ex) {
                        warn(ex.getMessage());
                        return;
                    }
                    // Cập nhật bản sao trong bộ nhớ để màn hình hiển thị đúng ngay
                    profile.setPhoneNumber(phone.getText().trim());
                    profile.setAddress(addr.getText().trim());
                    profile.setGender(newGender);
                    d.dispose();
                    info("Đã lưu thông tin.");
                }),
                UIStyle.button("Đổi mật khẩu", this::changePassword),
                UIStyle.button("Đóng", UIStyle.MUTED, new Color(0x4B5563), d::dispose));

        JPanel root = new JPanel(new BorderLayout(0, 10));
        root.setBorder(new EmptyBorder(16, 16, 12, 16));
        root.add(form, BorderLayout.CENTER);
        root.add(actions, BorderLayout.SOUTH);

        d.setContentPane(root);
        d.pack();
        d.setLocationRelativeTo(this);
        d.setVisible(true);
    }

    private static JTextField readOnly(String text) {
        JTextField t = new JTextField(text, 20);
        t.setEditable(false);
        return t;
    }

    private void addRow(JPanel p, int y, String label, JComponent field) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = y;
        c.insets = new Insets(6, 4, 6, 12);
        c.anchor = GridBagConstraints.WEST;
        c.gridx = 0;
        p.add(UIStyle.bold(label), c);
        c.gridx = 1;
        p.add(field, c);
    }

    private void changePassword() {
        if (ChangePasswordDialog.show(this, acc.getEmployeeId(), false, null)) {
            info("Đổi mật khẩu thành công.");
        }
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