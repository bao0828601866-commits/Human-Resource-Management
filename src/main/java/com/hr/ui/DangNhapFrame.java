package com.hr.ui;

import com.hr.dao.AccountDAO;
import com.hr.model.Account;

import javax.swing.*;
import java.awt.*;

public class DangNhapFrame extends JFrame {

    private final JTextField txtUser = new JTextField(15);
    private final JPasswordField txtPass = new JPasswordField(15);
    private final JButton btnLogin = new JButton("Đăng nhập");
    private final AccountDAO accountDAO = new AccountDAO();

    public DangNhapFrame() {
        setTitle("Đăng nhập - HRM");
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        JPanel form = new JPanel(new GridLayout(2, 2, 8, 8));
        form.setBorder(BorderFactory.createEmptyBorder(16, 16, 8, 16));
        form.add(new JLabel("Tài khoản:"));
        form.add(txtUser);
        form.add(new JLabel("Mật khẩu:"));
        form.add(txtPass);

        JPanel bottom = new JPanel();
        bottom.setBorder(BorderFactory.createEmptyBorder(0, 16, 16, 16));
        bottom.add(btnLogin);

        add(form, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);

        btnLogin.addActionListener(e -> handleLogin());
        getRootPane().setDefaultButton(btnLogin); // nhấn Enter = bấm nút

        pack();
        setLocationRelativeTo(null); // giữa màn hình
    }

    private void handleLogin() {
        String user = txtUser.getText().trim();
        String pass = new String(txtPass.getPassword());

        if (user.isEmpty() || pass.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập đủ tài khoản và mật khẩu.");
            return;
        }

        Account acc = accountDAO.login(user, pass);
        if (acc == null) {
            JOptionPane.showMessageDialog(this, "Sai tài khoản hoặc mật khẩu.",
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Tạm thời chỉ báo vai trò. Bước 5 sẽ mở màn hình tương ứng.
        JOptionPane.showMessageDialog(this,
                "Xin chào " + acc.getUsername() + "\nVai trò: " + acc.getRole());
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new DangNhapFrame().setVisible(true));
    }
}