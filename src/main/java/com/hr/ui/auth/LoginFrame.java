package com.hr.ui.auth;

import com.hr.ui.manager.ManagerFrame;
import com.hr.ui.employee.EmployeeFrame;

import com.hr.controller.AuthController;
import com.hr.model.Account;
import com.hr.model.enums.Role;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutionException;

public class LoginFrame extends JFrame {
    private static final Color PRIMARY = new Color(0xDC2626);
    private static final Color PRIMARY_DARK = new Color(0xB91C1C);
    private static final Color TEXT_MUTED = new Color(0x6B7280);
    private static final Color ERROR = new Color(0xB45309);

    private final JTextField txtEmployeeId  = new JTextField();
    private final JPasswordField txtPassword = new JPasswordField();
    private final JCheckBox chkShow = new JCheckBox("Hiển thị mật khẩu");
    private final JLabel lblError = new JLabel(" ");
    private final JButton btnLogin = new JButton("ĐĂNG NHẬP");

    private final AuthController authController = new AuthController();

    public LoginFrame() {
        setTitle("HRM - Đăng nhập");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setResizable(false); // Khóa bố cục

        // Bố cục gồm 2 cột 
        JPanel root = new JPanel(new GridLayout(1, 2));
        root.add(buildBrandPanel());
        root.add(buildFormPanel());
        setContentPane(root);

        pack(); // Tính kích thước dựa trên preferredSize của các panel con
        setLocationRelativeTo(null); // Đưa cửa số ra giữa 

        // Cho phép nhấn Enter để đăng nhập
        getRootPane().setDefaultButton(btnLogin);
        // tự động đặt con trỏ chuột vào ô Mã nhân viên ngay khi cửa sổ mở.
        SwingUtilities.invokeLater(txtEmployeeId::requestFocusInWindow);
        new Thread(com.hr.utils.DatabaseConnection::warmUp, "db-warmup").start();
    }

    // Panel bên trái: logo
    private JPanel buildBrandPanel() {
        BufferedImage img = loadImage("/images/brand.png");

        JPanel p = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int w = getWidth(), h = getHeight();

                // Nền trắng cho khớp với nền ảnh
                g2.setColor(Color.WHITE);
                g2.fillRect(0, 0, w, h);

                if (img != null) {
                    // "Fit": hiển thị trọn ảnh, giữ tỉ lệ, không cắt viền đỏ
                    double scale = Math.min((double) w / img.getWidth(),
                                            (double) h / img.getHeight());
                    int dw = (int) (img.getWidth() * scale);
                    int dh = (int) (img.getHeight() * scale);
                    g2.drawImage(img, (w - dw) / 2, (h - dh) / 2, dw, dh, null);
                }
                g2.dispose();
            }
        };
        p.setPreferredSize(new Dimension(410, 480));
        return p;
    }

    private BufferedImage loadImage(String path) {
        try (InputStream in = getClass().getResourceAsStream(path)) {
            return in == null ? null : ImageIO.read(in);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    // Panel bên phải: Form Login
    private JPanel buildFormPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(Color.WHITE);

        JPanel form = new JPanel();
        form.setOpaque(false);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setPreferredSize(new Dimension(300, 340));

        JLabel title = new JLabel("Hệ Thống Quản Lý Nhân Sự");
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        title.setAlignmentX(LEFT_ALIGNMENT);

        JLabel hint = new JLabel("Vui lòng đăng nhập để tiếp tục");
        hint.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        hint.setForeground(TEXT_MUTED);
        hint.setAlignmentX(LEFT_ALIGNMENT);

        styleField(txtEmployeeId);
        styleField(txtPassword);

        chkShow.setOpaque(false);
        chkShow.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        chkShow.setForeground(TEXT_MUTED);
        chkShow.setFocusPainted(false);
        chkShow.setAlignmentX(LEFT_ALIGNMENT);
        char defaultEcho = txtPassword.getEchoChar();
        chkShow.addActionListener(e ->
                txtPassword.setEchoChar(chkShow.isSelected() ? (char) 0 : defaultEcho));

        lblError.setForeground(ERROR);
        lblError.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblError.setAlignmentX(LEFT_ALIGNMENT);

        styleButton(btnLogin);
        btnLogin.addActionListener(e -> doLogin());

        form.add(title);
        form.add(Box.createVerticalStrut(4));
        form.add(hint);
        form.add(Box.createVerticalStrut(24));
        form.add(label("Mã nhân viên"));
        form.add(Box.createVerticalStrut(6));
        form.add(txtEmployeeId);
        form.add(Box.createVerticalStrut(14));
        form.add(label("Mật khẩu"));
        form.add(Box.createVerticalStrut(6));
        form.add(txtPassword);
        form.add(Box.createVerticalStrut(8));
        form.add(chkShow);
        form.add(Box.createVerticalStrut(6));
        form.add(lblError);
        form.add(Box.createVerticalStrut(10));
        form.add(btnLogin);

        p.add(form);
        return p;
    }

    private JLabel label(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", Font.BOLD, 12));
        l.setAlignmentX(LEFT_ALIGNMENT);
        return l;
    }

    private void styleField(JTextField f) {
        f.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        f.setAlignmentX(LEFT_ALIGNMENT);
        f.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        f.setPreferredSize(new Dimension(300, 38));
        f.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xD1D5DB)),
                new EmptyBorder(4, 10, 4, 10)));
    }

    private void styleButton(JButton b) {
        b.setAlignmentX(LEFT_ALIGNMENT);
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        b.setPreferredSize(new Dimension(300, 42));
        b.setFont(new Font("Segoe UI", Font.BOLD, 14));
        b.setForeground(Color.WHITE);
        b.setBackground(PRIMARY);
        b.setOpaque(true);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addMouseListener(new MouseAdapter() {

            @Override public void mouseEntered(MouseEvent e) { 
                if (b.isEnabled()) b.setBackground(PRIMARY_DARK); 
            }

            @Override public void mouseExited(MouseEvent e) { 
                b.setBackground(PRIMARY); 
            }
        });
    }

    private void doLogin() {
        String user = txtEmployeeId.getText();
        String pass = new String(txtPassword.getPassword());

        lblError.setText(" ");
        btnLogin.setEnabled(false);
        btnLogin.setText("Đang kiểm tra...");

        // Chạy ở luồng nền để giao diện không bị đơ khi truy vấn DB
        new SwingWorker<Account, Void>() {
            @Override
            protected Account doInBackground() {
                return authController.login(user, pass);
            }

            @Override
            protected void done() {
                btnLogin.setEnabled(true);
                btnLogin.setText("ĐĂNG NHẬP");
                try {
                    Account acc = get();
                    if (acc == null) {
                        lblError.setText("Sai tài khoản/mật khẩu.");
                        txtPassword.setText("");
                        txtPassword.requestFocusInWindow();
                        return;
                    }
                    openMain(acc, pass);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException ex) {
                    // Lỗi do Controller ném ra (validate hoặc lỗi DB)
                    Throwable cause = ex.getCause();
                    if (cause instanceof IllegalArgumentException || cause instanceof IllegalStateException) {
                        lblError.setText(cause.getMessage());
                    } else {
                        lblError.setText("Đã xảy ra lỗi không mong muốn.");
                        cause.printStackTrace();
                    }
                }
            }
        }.execute();
    }

    private void openMain(Account acc, String typedPassword) {
        if (acc.isMustChangePassword()) {
            boolean changed = ChangePasswordDialog.show(this, acc.getEmployeeId(), true, typedPassword);
            if (!changed) {
                lblError.setText("Bạn phải đổi mật khẩu ở lần đăng nhập đầu tiên.");
                txtPassword.setText("");
                txtPassword.requestFocusInWindow();
                return;
            }
        }
        dispose(); // đóng LoginFram
        if (acc.getRole() == Role.MANAGER) {
            new ManagerFrame(acc).setVisible(true);
        } else {
            new EmployeeFrame(acc).setVisible(true);
        }
    }
}