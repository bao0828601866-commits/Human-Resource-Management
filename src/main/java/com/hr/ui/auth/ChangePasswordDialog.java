package com.hr.ui.auth;

import com.hr.controller.AuthController;
import com.hr.ui.UIStyle;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public final class ChangePasswordDialog {
    private static final AuthController AUTH = new AuthController();

    private ChangePasswordDialog() {}

    /** @return true nếu đổi mật khẩu thành công. */
    public static boolean show(Component parent, String employeeId, boolean forced, String knownOldPass) {
        Window owner = parent instanceof Window ? (Window) parent : SwingUtilities.getWindowAncestor(parent);
        JDialog d = new JDialog(owner, "Đổi mật khẩu", Dialog.ModalityType.APPLICATION_MODAL);
        boolean[] success = {false};

        JPasswordField oldPw = new JPasswordField(), newPw = new JPasswordField(), cfPw = new JPasswordField();
        JLabel err = new JLabel(" ");
        err.setForeground(new Color(0xB45309));
        err.setFont(UIStyle.PLAIN);

        JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
        if (knownOldPass == null) {
            form.add(new JLabel("Mật khẩu cũ:"));
            form.add(oldPw);
        }
        form.add(new JLabel("Mật khẩu mới:"));
        form.add(newPw);
        form.add(new JLabel("Nhập lại mật khẩu:"));
        form.add(cfPw);

        Runnable submit = () -> {
            String o = knownOldPass != null ? knownOldPass : new String(oldPw.getPassword());
            String n = new String(newPw.getPassword());
            String c = new String(cfPw.getPassword());
            if (o.isEmpty()) { err.setText("Vui lòng nhập mật khẩu cũ."); return; }
            if (n.length() < 6) { err.setText("Mật khẩu mới tối thiểu 6 ký tự."); return; }
            if (!n.equals(c)) { err.setText("Mật khẩu nhập lại không khớp."); return; }
            if (n.equals(o)) { err.setText("Mật khẩu mới phải khác mật khẩu cũ."); return; }
            if (n.equals("123456")) { err.setText("Không dùng mật khẩu mặc định."); return; }
            try {
                if (!AUTH.changePassword(employeeId, o, n)) {
                    err.setText("Mật khẩu cũ không đúng.");
                    return;
                }
                success[0] = true;
                d.dispose();
            } catch (IllegalArgumentException | IllegalStateException ex) {
                err.setText(ex.getMessage());
            }
        };

        JButton ok = UIStyle.button("Đổi mật khẩu", submit);
        JButton cancel = UIStyle.button(forced ? "Thoát" : "Hủy", UIStyle.MUTED, new Color(0x4B5563), d::dispose);

        JPanel root = new JPanel(new BorderLayout(0, 10));
        root.setBorder(new EmptyBorder(16, 16, 12, 16));
        if (forced) {
            JLabel note = new JLabel("<html>Đây là lần đăng nhập đầu tiên.<br>Vui lòng đổi mật khẩu để tiếp tục.</html>");
            note.setFont(UIStyle.BOLD);
            root.add(note, BorderLayout.NORTH);
        }
        JPanel center = new JPanel(new BorderLayout(0, 8));
        center.add(form, BorderLayout.CENTER);
        center.add(err, BorderLayout.SOUTH);
        root.add(center, BorderLayout.CENTER);
        root.add(UIStyle.row(ok, cancel), BorderLayout.SOUTH);

        d.setContentPane(root);
        d.getRootPane().setDefaultButton(ok);
        if (forced) {
            d.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
            d.addWindowListener(new WindowAdapter() {
                @Override public void windowClosing(WindowEvent e) { d.dispose(); }
            });
        }
        d.pack();
        d.setSize(Math.max(d.getWidth(), 380), d.getHeight());
        d.setResizable(false);
        d.setLocationRelativeTo(parent);
        d.setVisible(true);
        return success[0];
    }
}