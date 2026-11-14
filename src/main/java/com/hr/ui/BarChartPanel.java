package com.hr.ui;

import javax.swing.*;
import java.awt.*;

/** Biểu đồ cột 2 chuỗi (năm trước / năm nay) vẽ bằng Graphics2D, không cần thư viện ngoài. */
final class BarChartPanel extends JPanel {
    private static final Color PREV = new Color(0xFCA5A5);
    private static final Color CUR = UIStyle.PRIMARY;

    private final String[] labels;
    private final String prevName, curName;
    private final double[] prev, cur;

    BarChartPanel(String[] labels, String prevName, double[] prev, String curName, double[] cur) {
        this.labels = labels;
        this.prevName = prevName;
        this.prev = prev;
        this.curName = curName;
        this.cur = cur;
        setBackground(Color.WHITE);
        setPreferredSize(new Dimension(400, 220));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        FontMetrics fm = g2.getFontMetrics();

        int left = 44, right = 10, top = 8, bottom = 40;
        int cw = getWidth() - left - right, ch = getHeight() - top - bottom;
        if (cw <= 0 || ch <= 0) { g2.dispose(); return; }

        double max = 0;
        for (double v : prev) max = Math.max(max, v);
        for (double v : cur) max = Math.max(max, v);
        double step = niceStep(max <= 0 ? 4 : max / 4);
        int lines = (int) Math.max(1, Math.ceil(max / step));
        double axisMax = lines * step;

        // lưới ngang + nhãn trục Y
        for (int i = 0; i <= lines; i++) {
            int y = top + ch - (int) (ch * (i * step) / axisMax);
            g2.setColor(new Color(0xE5E7EB));
            g2.drawLine(left, y, left + cw, y);
            g2.setColor(UIStyle.MUTED);
            String s = String.format("%.0f", i * step);
            g2.drawString(s, left - 6 - fm.stringWidth(s), y + fm.getAscent() / 2 - 1);
        }

        // cột
        int n = labels.length;
        double gw = cw / (double) n;
        int barW = (int) Math.max(4, gw * 0.3);
        for (int i = 0; i < n; i++) {
            int x0 = left + (int) (i * gw + gw * 0.2);
            drawBar(g2, x0, prev[i], axisMax, top, ch, barW, PREV);
            drawBar(g2, x0 + barW, cur[i], axisMax, top, ch, barW, CUR);
            g2.setColor(UIStyle.MUTED);
            g2.drawString(labels[i], left + (int) (i * gw + gw / 2) - fm.stringWidth(labels[i]) / 2,
                    top + ch + fm.getAscent() + 4);
        }

        // chú thích
        int ly = getHeight() - 10;
        int lx = left + cw / 2 - 70;
        g2.setColor(PREV);
        g2.fillRect(lx, ly - 8, 9, 9);
        g2.setColor(UIStyle.MUTED);
        g2.drawString(prevName, lx + 14, ly);
        lx += 14 + fm.stringWidth(prevName) + 16;
        g2.setColor(CUR);
        g2.fillRect(lx, ly - 8, 9, 9);
        g2.setColor(UIStyle.MUTED);
        g2.drawString(curName, lx + 14, ly);
        g2.dispose();
    }

    private void drawBar(Graphics2D g2, int x, double v, double axisMax, int top, int ch, int w, Color c) {
        if (v <= 0) return;
        int h = (int) (ch * v / axisMax);
        g2.setColor(c);
        g2.fillRect(x, top + ch - h, w, h);
    }

    private static double niceStep(double raw) {
        double pow = Math.pow(10, Math.floor(Math.log10(raw)));
        double f = raw / pow;
        double nf = f <= 1 ? 1 : f <= 2 ? 2 : f <= 5 ? 5 : 10;
        return nf * pow;
    }
}
