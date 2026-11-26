package com.hr.ui.manager;

import com.hr.model.Employee;
import com.hr.model.Position;
import com.hr.model.enums.Gender;
import com.hr.model.enums.SalaryType;
import com.hr.ui.UIStyle;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Hộp thoại Thêm / Sửa nhân viên, chia 3 tab:
 *   1. Thông tin cá nhân   2. Liên hệ   3. Công việc & ngân hàng
 * show(...) trả về Employee đã nhập, hoặc null nếu bấm Hủy.
 */
final class EmployeeDialog extends JDialog {
    private static final DateTimeFormatter DATE =
            DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT);
    private static final String[] GENDER_VI = {"Nam", "Nữ", "Khác"};
    private static final Gender[] GENDERS = {Gender.MALE, Gender.FEMALE, Gender.OTHER};
    private static final String[] ETHNIC = {"Kinh", "Hoa", "Tày", "Thái", "Mường", "Khmer", "Nùng", "H'Mông", "Dao", "Khác"};
    private static final String[] EDUCATION = {"", "THCS", "THPT", "Trung cấp", "Cao đẳng", "Đại học", "Sau đại học", "Khác"};
    private static final String[] BANKS = {"", "Vietcombank", "VietinBank", "BIDV", "Agribank", "Techcombank",
            "MB Bank", "ACB", "VPBank", "TPBank", "Sacombank", "HDBank", "VIB", "SHB", "MSB", "OCB", "Eximbank"};
    private static final String[] TAB_NAMES = {"Thông tin cá nhân", "Liên hệ", "Công việc & ngân hàng"};
    private static final int MIN_AGE = 16;
    private static final String HINT = "* Thông tin bắt buộc";

    private final List<Position> positions;
    private final String employeeId;

    // --- Tab 1: cá nhân ---
    private final HintField name = new HintField("VD: Nguyễn Văn An");
    private final JComboBox<String> gender = new JComboBox<>(GENDER_VI);
    private final HintField birth = new HintField("dd/MM/yyyy");
    private final HintField citizen = new HintField("12 chữ số");
    private final HintField hometown = new HintField("Tỉnh / thành phố");
    private final JComboBox<String> ethnic = new JComboBox<>(ETHNIC);
    private final JComboBox<String> education = new JComboBox<>(EDUCATION);
    private final HintField school = new HintField("Tên trường đã / đang theo học");

    // --- Tab 2: liên hệ ---
    private final HintField phone = new HintField("10 số, bắt đầu bằng 0");
    private final HintField mail = new HintField("ten@email.com (không bắt buộc)");
    private final HintField addr = new HintField("Số nhà, đường, phường / xã, quận / huyện, tỉnh / thành");
    private final JCheckBox chkSame = new JCheckBox("Tạm trú giống thường trú");
    private final HintField tempAddr = new HintField("Nơi đang ở hiện tại");

    // --- Tab 3: công việc + ngân hàng ---
    private final HintField code = new HintField("");
    private final JComboBox<String> pos = new JComboBox<>();
    private final HintField salary = new HintField("");
    private final HintField hire = new HintField("dd/MM/yyyy");
    private final JComboBox<String> bank = new JComboBox<>(BANKS);
    private final HintField account = new HintField("6 – 20 chữ số");

    // --- khung ---
    private final CardLayout cards = new CardLayout();
    private final JPanel cardPanel = new JPanel(cards);
    private final JLabel[] tabLabels = new JLabel[TAB_NAMES.length];
    private final boolean[] tabError = new boolean[TAB_NAMES.length];
    private final JLabel lblError = new JLabel(HINT);
    private int current = 0;

    private Employee result;

    private EmployeeDialog(Window owner, Employee cur, String newId, List<Position> positions) {
        super(owner, cur == null ? "Thêm nhân viên" : "Sửa nhân viên", ModalityType.APPLICATION_MODAL);
        this.positions = positions;
        boolean edit = cur != null;
        this.employeeId = edit ? cur.getEmployeeId() : newId;

        initFields(cur, edit);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Color.WHITE);
        root.setBorder(BorderFactory.createMatteBorder(5, 0, 0, 0, UIStyle.PRIMARY));   // viền trên đỏ
        root.add(buildTabBar(), BorderLayout.NORTH);

        cardPanel.setBackground(Color.WHITE);
        cardPanel.add(pagePersonal(), "t0");
        cardPanel.add(pageContact(), "t1");
        cardPanel.add(pageWork(), "t2");
        root.add(cardPanel, BorderLayout.CENTER);
        root.add(buildFooter(edit), BorderLayout.SOUTH);

        setContentPane(root);
        getRootPane().registerKeyboardAction(e -> dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
        setResizable(false);
        selectTab(0);
        pack();
        setSize(Math.max(getWidth(), 720), getHeight());
        setLocationRelativeTo(owner);
    }

    /** Mở hộp thoại. cur = null -> thêm mới (dùng newId); cur != null -> sửa. */
    static Employee show(Component owner, Employee cur, String newId, List<Position> positions) {
        Window w = owner instanceof Window ? (Window) owner : SwingUtilities.getWindowAncestor(owner);
        EmployeeDialog d = new EmployeeDialog(w, cur, newId, positions);
        d.setVisible(true);
        return d.result;
    }

    // =====================================================================
    // Khởi tạo dữ liệu
    // =====================================================================
    private void initFields(Employee cur, boolean edit) {
        for (Position p : positions) pos.addItem(p.getPositionName());
        for (JComboBox<String> cb : List.of(gender, ethnic, education, pos, bank)) {
            cb.setFont(UIStyle.PLAIN);
            cb.setBackground(Color.WHITE);
            cb.setPreferredSize(new Dimension(10, 38));
        }
        ethnic.setEditable(true);
        bank.setEditable(true);

        for (HintField f : List.of(code, salary)) {
            f.setEditable(false);
            f.setBackgroundColor(new Color(0xF3F4F6));
        }
        code.setText(employeeId);
        pos.addActionListener(e -> showSalary());

        UIStyle.onChange(addr, () -> {
            if (chkSame.isSelected()) tempAddr.setText(addr.getText());
        });
        chkSame.addActionListener(e -> syncTemp());
        chkSame.setOpaque(false);
        chkSame.setFont(UIStyle.PLAIN);
        chkSame.setFocusPainted(false);

        if (edit) {
            name.setText(nz(cur.getFullName()));
            for (int i = 0; i < GENDERS.length; i++) if (GENDERS[i] == cur.getGender()) gender.setSelectedIndex(i);
            if (cur.getBirthDay() != null) birth.setText(cur.getBirthDay().format(DATE));
            citizen.setText(nz(cur.getCitizenId()));
            hometown.setText(nz(cur.getHometown()));
            if (!nz(cur.getEthnicity()).isEmpty()) ethnic.setSelectedItem(cur.getEthnicity());
            education.setSelectedItem(nz(cur.getEducationLevel()));
            school.setText(nz(cur.getSchool()));

            phone.setText(nz(cur.getPhoneNumber()));
            mail.setText(nz(cur.getEmail()));
            addr.setText(nz(cur.getPermanentAddress()));
            tempAddr.setText(nz(cur.getAddress()));
            chkSame.setSelected(!addr.getText().isEmpty() && addr.getText().equals(tempAddr.getText()));

            pos.setSelectedItem(cur.getPositionName());
            if (cur.getHireDate() != null) hire.setText(cur.getHireDate().format(DATE));
            bank.setSelectedItem(nz(cur.getBankName()));
            account.setText(nz(cur.getBankAccount()));
        } else {
            hire.setText(LocalDate.now().format(DATE));
        }
        syncTemp();
        showSalary();
    }

    private void syncTemp() {
        boolean same = chkSame.isSelected();
        if (same) tempAddr.setText(addr.getText());
        tempAddr.setEditable(!same);
        tempAddr.setBackgroundColor(same ? new Color(0xF3F4F6) : Color.WHITE);
    }

    private void showSalary() {
        Position p = positions.get(Math.max(0, pos.getSelectedIndex()));
        salary.setText(p.getSalaryType() == SalaryType.FIXED
                ? "Full-time · " + money(p.getSalaryRate()) + " / tháng"
                : "Part-time · " + money(p.getSalaryRate()) + " / giờ");
    }

    // =====================================================================
    // Thanh tab + chân hộp thoại
    // =====================================================================
    private JComponent buildTabBar() {
        JPanel bar = new JPanel(new GridLayout(1, TAB_NAMES.length));
        bar.setBackground(Color.WHITE);
        bar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, UIStyle.BORDER));
        for (int i = 0; i < TAB_NAMES.length; i++) {
            int idx = i;
            JLabel l = new JLabel("", SwingConstants.CENTER);
            l.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            l.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    selectTab(idx);
                }
            });
            tabLabels[i] = l;
            bar.add(l);
        }
        return bar;
    }

    private void selectTab(int i) {
        current = i;
        cards.show(cardPanel, "t" + i);
        refreshTabs();
    }

    private void refreshTabs() {
        for (int i = 0; i < tabLabels.length; i++) {
            boolean active = i == current;
            JLabel l = tabLabels[i];
            String dot = tabError[i] ? " <font color='#DC2626'>●</font>" : "";
            l.setText("<html>" + (i + 1) + ".&nbsp; " + TAB_NAMES[i] + dot + "</html>");
            l.setFont(new Font("Segoe UI", active ? Font.BOLD : Font.PLAIN, 13));
            l.setForeground(active ? UIStyle.PRIMARY : new Color(0x4B5563));
            l.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 3, 0, active ? UIStyle.PRIMARY : Color.WHITE),
                    new EmptyBorder(12, 8, 10, 8)));
        }
    }

    private JComponent buildFooter(boolean edit) {
        lblError.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblError.setForeground(UIStyle.MUTED);

        JButton cancel = UIStyle.button("Hủy", UIStyle.MUTED, new Color(0x4B5563), this::dispose);
        JButton save = UIStyle.button(edit ? "Lưu thay đổi" : "Thêm nhân viên", this::onSave);
        getRootPane().setDefaultButton(save);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btns.setOpaque(false);
        btns.add(cancel);
        btns.add(save);

        JPanel p = new JPanel(new BorderLayout(12, 0));
        p.setBackground(new Color(0xF9FAFB));
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, UIStyle.BORDER), new EmptyBorder(14, 24, 14, 24)));
        p.add(lblError, BorderLayout.CENTER);
        p.add(btns, BorderLayout.EAST);
        return p;
    }

    // =====================================================================
    // Nội dung từng tab
    // =====================================================================
    private JComponent pagePersonal() {
        JPanel p = page();
        p.add(pair(labeled("Họ tên *", name), labeled("Giới tính", gender)));
        p.add(pair(labeled("Ngày sinh *", birth), labeled("Số CCCD *", citizen)));
        p.add(pair(labeled("Quê quán", hometown), labeled("Dân tộc", ethnic)));
        p.add(pair(labeled("Trình độ học vấn", education), labeled("Trường", school)));
        p.add(Box.createVerticalGlue());
        return p;
    }

    private JComponent pageContact() {
        JPanel p = page();
        p.add(pair(labeled("Số điện thoại *", phone), labeled("Email", mail)));
        p.add(pair(labeled("Địa chỉ thường trú", addr), null, true));
        JPanel same = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        same.setOpaque(false);
        same.setAlignmentX(Component.LEFT_ALIGNMENT);
        same.setBorder(new EmptyBorder(0, 0, 8, 0));
        same.add(chkSame);
        same.setMaximumSize(new Dimension(Integer.MAX_VALUE, same.getPreferredSize().height + 8));
        p.add(same);
        p.add(pair(labeled("Địa chỉ tạm trú", tempAddr), null, true));
        p.add(Box.createVerticalGlue());
        return p;
    }

    private JComponent pageWork() {
        JPanel p = page();
        p.add(section("Công việc"));
        p.add(pair(labeled("Mã nhân viên", code), labeled("Chức vụ", pos)));
        p.add(pair(labeled("Mức lương hiện tại", salary), labeled("Ngày bắt đầu vào làm *", hire)));
        p.add(section("Tài khoản ngân hàng"));
        p.add(pair(labeled("Ngân hàng", bank), labeled("Số tài khoản", account)));
        p.add(Box.createVerticalGlue());
        return p;
    }

    private static JPanel page() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(Color.WHITE);
        p.setBorder(new EmptyBorder(18, 24, 8, 24));
        return p;
    }

    /** Tiêu đề nhóm: chữ đỏ + đường kẻ mảnh. */
    private static JComponent section(String text) {
        JLabel l = new JLabel(text.toUpperCase(Locale.forLanguageTag("vi")));
        l.setFont(new Font("Segoe UI", Font.BOLD, 12));
        l.setForeground(UIStyle.PRIMARY);
        JPanel p = new JPanel(new BorderLayout(10, 0));
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(0, 0, 10, 0));
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(l, BorderLayout.WEST);
        JSeparator sep = new JSeparator();
        sep.setForeground(UIStyle.BORDER);
        p.add(sep, BorderLayout.CENTER);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, p.getPreferredSize().height));
        return p;
    }

    /** Nhãn nằm trên, ô nhập nằm dưới. */
    private static JPanel labeled(String label, JComponent field) {
        JLabel l = new JLabel(label);
        l.setFont(new Font("Segoe UI", Font.BOLD, 12));
        l.setForeground(new Color(0x374151));
        JPanel p = new JPanel(new BorderLayout(0, 4));
        p.setOpaque(false);
        p.add(l, BorderLayout.NORTH);
        p.add(field, BorderLayout.CENTER);
        return p;
    }

    private static JPanel pair(JComponent a, JComponent b) {
        return pair(a, b, false);
    }

    /** Một hàng 2 cột; fullWidth = true thì ô a chiếm cả hàng. */
    private static JPanel pair(JComponent a, JComponent b, boolean fullWidth) {
        JPanel p = new JPanel(fullWidth ? new BorderLayout() : new GridLayout(1, 2, 16, 0));
        p.setOpaque(false);
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.setBorder(new EmptyBorder(0, 0, 12, 0));
        p.add(a, BorderLayout.CENTER);
        if (!fullWidth) p.add(b != null ? b : new JLabel());
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, p.getPreferredSize().height + 6));
        return p;
    }

    // =====================================================================
    // Kiểm tra + lưu
    // =====================================================================
    private void onSave() {
        Arrays.fill(tabError, false);
        for (HintField f : List.of(name, birth, citizen, phone, mail, hire, account)) f.setError(false);
        lblError.setText(HINT);
        lblError.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblError.setForeground(UIStyle.MUTED);
        refreshTabs();

        // ----- Tab 1 -----
        if (name.getText().isBlank()) { fail(0, name, "Vui lòng nhập họ tên."); return; }

        LocalDate bd;
        try {
            bd = LocalDate.parse(birth.getText().trim(), DATE);
        } catch (DateTimeParseException ex) {
            fail(0, birth, "Ngày sinh không hợp lệ. Nhập theo dạng dd/MM/yyyy, ví dụ 25/12/2000.");
            return;
        }
        if (bd.isAfter(LocalDate.now().minusYears(MIN_AGE))) { fail(0, birth, "Nhân viên phải từ " + MIN_AGE + " tuổi trở lên."); return; }
        if (bd.isBefore(LocalDate.now().minusYears(100))) { fail(0, birth, "Ngày sinh không hợp lệ."); return; }
        if (!citizen.getText().trim().matches("\\d{12}")) { fail(0, citizen, "Số CCCD phải gồm đúng 12 chữ số."); return; }

        // ----- Tab 2 -----
        if (!phone.getText().trim().matches("0\\d{9}")) { fail(1, phone, "SĐT phải gồm 10 số, bắt đầu bằng 0."); return; }
        String m = mail.getText().trim();
        if (!m.isEmpty() && !m.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) { fail(1, mail, "Email không hợp lệ."); return; }

        // ----- Tab 3 -----
        LocalDate hd;
        try {
            hd = LocalDate.parse(hire.getText().trim(), DATE);
        } catch (DateTimeParseException ex) {
            fail(2, hire, "Ngày vào làm không hợp lệ. Nhập theo dạng dd/MM/yyyy.");
            return;
        }
        if (hd.isBefore(bd.plusYears(MIN_AGE))) { fail(2, hire, "Ngày vào làm phải sau khi nhân viên đủ " + MIN_AGE + " tuổi."); return; }

        String bankName = text(bank), acc = account.getText().trim();
        if (!acc.isEmpty() && !acc.matches("\\d{6,20}")) { fail(2, account, "Số tài khoản phải gồm 6 – 20 chữ số."); return; }
        if (!acc.isEmpty() && bankName.isEmpty()) { fail(2, bank, "Vui lòng chọn ngân hàng cho số tài khoản này."); return; }
        if (acc.isEmpty() && !bankName.isEmpty()) { fail(2, account, "Vui lòng nhập số tài khoản."); return; }

        Employee e = new Employee();
        e.setEmployeeId(employeeId);
        e.setFullName(name.getText().trim());
        e.setGender(GENDERS[gender.getSelectedIndex()]);
        e.setBirthDay(bd);
        e.setCitizenId(citizen.getText().trim());
        e.setHometown(hometown.getText().trim());
        e.setEthnicity(text(ethnic));
        e.setEducationLevel(text(education));
        e.setSchool(school.getText().trim());

        e.setPhoneNumber(phone.getText().trim());
        e.setEmail(m);
        e.setPermanentAddress(addr.getText().trim());                          // thường trú -> PersonalInfo.permanent_address
        e.setAddress(chkSame.isSelected() ? addr.getText().trim() : tempAddr.getText().trim());  // tạm trú -> Employee.address

        e.setPositionId(positions.get(pos.getSelectedIndex()).getPositionId());
        e.setHireDate(hd);
        e.setBankName(bankName);
        e.setBankAccount(acc);
        result = e;
        dispose();
    }

    private void fail(int tab, JComponent f, String msg) {
        tabError[tab] = true;
        selectTab(tab);
        if (f instanceof HintField) ((HintField) f).setError(true);
        lblError.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblError.setForeground(UIStyle.PRIMARY);
        lblError.setText(msg);
        f.requestFocusInWindow();
    }

    private static String text(JComboBox<String> cb) {
        Object o = cb.getSelectedItem();
        return o == null ? "" : o.toString().trim();
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }

    private static String money(BigDecimal v) {
        return NumberFormat.getInstance(Locale.forLanguageTag("vi-VN")).format(v == null ? BigDecimal.ZERO : v) + " ₫";
    }

    // =====================================================================
    // Ô nhập bo góc: gợi ý khi trống, viền đỏ khi focus / lỗi
    // =====================================================================
    private static final class HintField extends JTextField {
        private final String hint;
        private boolean error;
        private Color fill = Color.WHITE;

        HintField(String hint) {
            this.hint = hint;
            setFont(UIStyle.PLAIN);
            setOpaque(false);
            setBorder(new EmptyBorder(8, 12, 8, 12));
            setPreferredSize(new Dimension(10, 38));
        }

        void setError(boolean e) {
            error = e;
            repaint();
        }

        void setBackgroundColor(Color c) {
            fill = c;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(fill);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
            boolean red = error || (hasFocus() && isEditable());
            g2.setColor(red ? UIStyle.PRIMARY : UIStyle.BORDER);
            g2.setStroke(new BasicStroke(error ? 2f : 1f));
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
            g2.dispose();
            super.paintComponent(g);
            if (getText().isEmpty() && !hint.isEmpty() && isEditable()) {
                Graphics2D h = (Graphics2D) g.create();
                h.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                h.setFont(getFont());
                h.setColor(new Color(0x9CA3AF));
                FontMetrics fm = h.getFontMetrics();
                h.drawString(hint, getInsets().left, (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
                h.dispose();
            }
        }
    }
}