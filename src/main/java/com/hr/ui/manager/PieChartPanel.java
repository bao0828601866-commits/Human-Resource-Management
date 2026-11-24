package com.hr.ui.manager;

import javax.swing.*;
import java.awt.*;

/** Biểu đồ tròn + chú thích bên phải, vẽ bằng Graphics2D (không cần thư viện ngoài). */
final class PieChartPanel extends JPanel {
    private static final Color[] PALETTE = {
            new Color(0xDC2626), new Color(0xF87171), new Color(0x7F1D1D), new Color(0xFCA5A5),
            new Color(0x6B7280), new Color(0x9CA3AF), new Color(0xD1D5DB), new Color(0xB91C1C)
    };

    private final String[] labels;
    private final double[] values;

    PieChartPanel(String[] labels, double[] values) {
        this.labels = labels;
        this.values = values;
        setBackground(Color.WHITE);
        setPreferredSize(new Dimension(300, 160));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        FontMetrics fm = g2.getFontMetrics();

        double total = 0;
        for (double v : values) total += v;
        if (total <= 0) {
            g2.setColor(new Color(0x6B7280));
            String s = "Chưa có dữ liệu";
            g2.drawString(s, (getWidth() - fm.stringWidth(s)) / 2, getHeight() / 2);
            g2.dispose();
            return;
        }

        int pad = 8;
        int d = Math.min(getHeight() - 2 * pad, 220);
        if (d <= 0) { g2.dispose(); return; }

        // độ rộng chú thích = nhãn dài nhất
        int maxLbl = 0;
        for (int i = 0; i < labels.length; i++) {
            maxLbl = Math.max(maxLbl, fm.stringWidth(labels[i] + " (" + (int) values[i] + ")"));
        }
        int gap = 40;                                   // khoảng cách giữa biểu đồ và chú thích
        int groupW = d + gap + 16 + maxLbl;
        int x = Math.max(pad, (getWidth() - groupW) / 2);   // căn giữa cả nhóm
        int y = (getHeight() - d) / 2;

        double acc = 0;
        for (int i = 0; i < values.length; i++) {
            int start = (int) Math.round(acc / total * 360);
            acc += values[i];
            int end = (int) Math.round(acc / total * 360);
            g2.setColor(PALETTE[i % PALETTE.length]);
            g2.fillArc(x, y, d, d, 90 - start, -(end - start));
        }
        // viền trắng giữa các miếng
        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(2f));
        acc = 0;
        for (int i = 0; i < values.length; i++) {
            int start = (int) Math.round(acc / total * 360);
            acc += values[i];
            double rad = Math.toRadians(90 - start);
            g2.drawLine(x + d / 2, y + d / 2,
                    x + d / 2 + (int) (Math.cos(rad) * d / 2), y + d / 2 - (int) (Math.sin(rad) * d / 2));
        }

        // chú thích
        int lx = x + d + gap;
        int rowH = fm.getHeight() + 6;
        int ly = Math.max(pad, (getHeight() - rowH * labels.length) / 2) + fm.getAscent();
        for (int i = 0; i < labels.length; i++) {
            g2.setColor(PALETTE[i % PALETTE.length]);
            g2.fillRect(lx, ly - 9, 10, 10);
            g2.setColor(new Color(0x374151));
            g2.drawString(labels[i] + " (" + (int) values[i] + ")", lx + 16, ly);
            ly += rowH;
        }
        g2.dispose();
    }
}