package com.hr.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/** Hàm/hằng số dùng chung cho các màn hình. */
final class UIStyle {
    static final Color PRIMARY = new Color(0xDC2626);
    static final Color PRIMARY_DARK = new Color(0xB91C1C);
    static final Color PRIMARY_LIGHT = new Color(0xFEE2E2);
    static final Color MUTED = new Color(0x6B7280);
    static final Color BORDER = new Color(0xE5E7EB);
    static final Font PLAIN = new Font("Segoe UI", Font.PLAIN, 13);
    static final Font BOLD = new Font("Segoe UI", Font.BOLD, 13);

    private UIStyle() {}

    // ---------- Button ----------
    static JButton button(String text, Runnable action) {
        return button(text, PRIMARY, PRIMARY_DARK, action);
    }

    static JButton button(String text, Color normal, Color hover, Runnable action) {
        JButton b = new JButton(text);
        b.setFont(BOLD);
        b.setForeground(Color.WHITE);
        b.setBackground(normal);
        b.setOpaque(true);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setBorder(new EmptyBorder(8, 16, 8, 16));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { if (b.isEnabled()) b.setBackground(hover); }
            @Override public void mouseExited(MouseEvent e)  { b.setBackground(normal); }
        });
        b.addActionListener(e -> action.run());
        return b;
    }

    // ---------- Panel / Card ----------
    /** JPanel trong suốt (để thấy nền xám nhạt của trang phía sau). */
    static JPanel panel(LayoutManager lm) {
        JPanel p = new JPanel(lm);
        p.setOpaque(false);
        return p;
    }

    /** Khung trắng có viền, tiêu đề đậm phía trên (giống các "card" trong mẫu). */
    static JPanel card(String title, JComponent body) {
        JPanel p = new JPanel(new BorderLayout(0, 8));
        p.setBackground(Color.WHITE);
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER), new EmptyBorder(12, 14, 12, 14)));
        JLabel l = new JLabel(title);
        l.setFont(new Font("Segoe UI", Font.BOLD, 15));
        p.add(l, BorderLayout.NORTH);
        p.add(body, BorderLayout.CENTER);
        return p;
    }

    static JScrollPane plainScroll(JComponent c) {
        JScrollPane sp = new JScrollPane(c);
        sp.setBorder(BorderFactory.createLineBorder(BORDER));
        return sp;
    }

    static JScrollPane scroll(JComponent c, String title) {
        JScrollPane sp = new JScrollPane(c);
        sp.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(0xD1D5DB)), title,
                TitledBorder.LEFT, TitledBorder.TOP, BOLD, MUTED));
        return sp;
    }

    /** Một hàng component xếp ngang, căn trái. */
    static JPanel row(Component... cs) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        p.setOpaque(false);
        for (Component c : cs) p.add(c);
        return p;
    }

    static JLabel bold(String text) {
        JLabel l = new JLabel(text);
        l.setFont(BOLD);
        return l;
    }

    static JLabel muted(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        l.setForeground(MUTED);
        return l;
    }

    // ---------- Table ----------
    /** Model chỉ đọc (không cho sửa trực tiếp trên bảng). */
    static DefaultTableModel model(String... cols) {
        return new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
    }

    static JTable table(DefaultTableModel m) {
        JTable t = new JTable(m);
        styleTable(t, 28);
        t.setRowSorter(new TableRowSorter<>(m));
        return t;
    }

    /** Bảng lịch tuần: hàng = ca, cột = thứ. */
    static JTable grid(DefaultTableModel m) {
        JTable t = new JTable(m);
        styleTable(t, 56);
        t.setDefaultRenderer(Object.class, new GridRenderer());
        return t;
    }

    private static void styleTable(JTable t, int rowHeight) {
        t.setFont(PLAIN);
        t.setRowHeight(rowHeight);
        t.setFillsViewportHeight(true);
        t.setSelectionBackground(PRIMARY_LIGHT);
        t.setSelectionForeground(Color.BLACK);
        t.getTableHeader().setFont(BOLD);
        t.getTableHeader().setReorderingAllowed(false);
    }

    private static class GridRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int r, int c) {
            String s = v == null ? "" : v.toString();
            String text = s.isEmpty() ? "" : "<html><center>" + s.replace(", ", "<br>") + "</center></html>";
            super.getTableCellRendererComponent(t, text, false, false, r, c);
            setHorizontalAlignment(CENTER);
            if (c == 0) {
                setFont(BOLD);
                setBackground(new Color(0xF3F4F6));
            } else {
                setFont(PLAIN);
                setBackground(s.isEmpty() ? Color.WHITE : PRIMARY_LIGHT);
            }
            return this;
        }
    }

    /** Gọi action mỗi khi nội dung ô nhập thay đổi (dùng cho tìm kiếm tức thì). */
    static void onChange(JTextComponent c, Runnable r) {
        c.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e)  { r.run(); }
            @Override public void removeUpdate(DocumentEvent e)  { r.run(); }
            @Override public void changedUpdate(DocumentEvent e) { r.run(); }
        });
    }

    // ---------- Avatar tròn có chữ cái đầu ----------
    static final class Avatar extends JComponent {
        private final String letter;
        private final Color bg, fg;

        Avatar(String name, int size, Color bg, Color fg) {
            String n = name == null ? "" : name.trim();
            this.letter = n.isEmpty() ? "?" : n.substring(0, 1).toUpperCase();
            this.bg = bg;
            this.fg = fg;
            Dimension d = new Dimension(size, size);
            setPreferredSize(d);
            setMinimumSize(d);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int d = Math.min(getWidth(), getHeight());
            int x = (getWidth() - d) / 2, y = (getHeight() - d) / 2;
            g2.setColor(bg);
            g2.fillOval(x, y, d, d);
            g2.setColor(fg);
            g2.setFont(new Font("Segoe UI", Font.BOLD, d * 45 / 100));
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(letter, x + (d - fm.stringWidth(letter)) / 2, y + (d - fm.getHeight()) / 2 + fm.getAscent());
            g2.dispose();
        }
    }
}
