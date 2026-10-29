package com.hex.kagune.control.client;

import com.hex.kagune.control.client.ui.UiCanvas;
import com.hex.kagune.control.king.KingForm;

/**
 * 1.0.10: the colours of Kaneki's kagune menu (pure), and its marks. Until the final kakuja is chosen it is his crimson over black;
 * War makes it pure black with red veins crawling through it, Peace white with feathers.
 */
public record KingTheme(int panelFill, int panelEdge, int innerFill, int innerEdge, int vein, int veinBright, int title, int subtitle, int header,
                        int label, int value, int trait, int muted, int faint, int barBack, int bar, int rc, int button, int buttonHover, int owned,
                        int available, int locked, int pending, int quest, int tipFill, int hint, int on, int off, boolean feathers, boolean veins) {
    public static final KingTheme KANEKI = new KingTheme(0xF3140608, 0xFF6A1620, 0xF20C0507, 0xFF34101A, 0xFF861A22, 0xFF561218, 0xFFF6E0E0, 0xFFC2585E,
        0xFFF0505A, 0xFFA0646A, 0xFFF2DADA, 0xFFE2B8BC, 0xFF9A6A70, 0xFF6E4C52, 0xFF3C1418, 0xFFE0283A, 0xFFB0283A, 0xFF3A1218, 0xFF5E1A24,
        0xFFE83848, 0xFFF0C060, 0xFF4A3036, 0xFF5A3A14, 0xFFE8A040, 0xF0140608, 0xFF9E5A60, 0xFFFF5A64, 0xFFB9454E, false, false);
    public static final KingTheme WAR = new KingTheme(0xF7040304, 0xFF4A0810, 0xF5010101, 0xFF22050A, 0xFF780C18, 0xFF380810, 0xFFF0E4E4, 0xFFB01828,
        0xFFE8202E, 0xFF84585C, 0xFFEDE2E2, 0xFFD8A4A8, 0xFF84606A, 0xFF54383C, 0xFF240A0E, 0xFFD41A2A, 0xFFA8121F, 0xFF140708, 0xFF320A10,
        0xFFE8202E, 0xFFF0C060, 0xFF341E22, 0xFF4A2A10, 0xFFE8A040, 0xF6040304, 0xFFA87A7E, 0xFFFF3A48, 0xFFB01828, false, true);
    public static final KingTheme PEACE = new KingTheme(0xF7F5F5F8, 0xFFB4B9C4, 0xF4FCFCFE, 0xFFD4D8E0, 0xFFD6DAE2, 0xFFC4C9D2, 0xFF26262C, 0xFF9A1426,
        0xFF8A1020, 0xFF666A74, 0xFF1E2026, 0xFF545862, 0xFF767A84, 0xFFA4A8B2, 0xFFDDE0E6, 0xFFC4121F, 0xFFB01828, 0xFFE4E7EC, 0xFFCDD2DA,
        0xFFC4121F, 0xFFA87418, 0xFFC6CAD2, 0xFFEAD8B0, 0xFFB07418, 0xF6F8F8FA, 0xFF8A8E98, 0xFFC4121F, 0xFF8A8E98, true, false);

    /** The theme for what was chosen. */
    public static KingTheme of(KingForm chosen) {
        return chosen == KingForm.WAR ? WAR : chosen == KingForm.PEACE ? PEACE : KANEKI;
    }

    /** War's veins: red, branching, crawling in from the edges of the board {@code left, top, width, height}, pulsing with {@code time}. */
    public static void veins(UiCanvas c, int left, int top, int width, int height, int vein, int bright, double time) {
        java.util.Random r = new java.util.Random(19);
        for (int i = 0; i < 16; i++) {
            boolean side = i % 2 == 0;
            int x0 = side ? left + (i % 4 == 0 ? 6 : width - 6) : left + 30 + r.nextInt(width - 60);
            int y0 = side ? top + 30 + r.nextInt(height - 60) : top + (i % 4 == 1 ? 6 : height - 6);
            int x1 = x0 + (side ? (x0 < left + width / 2 ? 1 : -1) * (60 + r.nextInt(120)) : r.nextInt(120) - 60);
            int y1 = y0 + (side ? r.nextInt(120) - 60 : (y0 < top + height / 2 ? 1 : -1) * (50 + r.nextInt(90)));
            int cx = (x0 + x1) / 2 + r.nextInt(60) - 30;
            int cy = (y0 + y1) / 2 + r.nextInt(60) - 30;
            int colour = (int)(time * 2.0 + i) % 5 == 0 ? vein : bright;
            AyatoMenuView.drawVein(c, x0, y0, cx, cy, x1, y1, colour, i % 3 == 0 ? 2 : 1);
            // A branch.
            int bx = (x0 + x1) / 2;
            int by = (y0 + y1) / 2;
            AyatoMenuView.drawVein(c, bx, by, bx + r.nextInt(40) - 20, by + r.nextInt(40) - 20, bx + r.nextInt(80) - 40, by + r.nextInt(80) - 40, bright, 1);
        }
    }

    /** Peace's feathers: soft, layered, fanning in from the corners of the board {@code left, top, width, height}. */
    public static void feathers(UiCanvas c, int left, int top, int width, int height, int feather, int shaft, double time) {
        int[][] corners = {{left, top, 1, 1}, {left + width, top, -1, 1}, {left, top + height, 1, -1}, {left + width, top + height, -1, -1}};
        for (int[] corner : corners) {
            for (int k = 0; k < 7; k++) {
                double a = Math.toRadians(8.0 + 11.0 * k + 1.5 * Math.sin(time * 0.6 + k));
                double length = 70.0 + 18.0 * (k % 3) + (k == 3 ? 24.0 : 0.0);
                double dx = Math.cos(a) * corner[2];
                double dy = Math.sin(a) * corner[3];
                feather(c, corner[0] + corner[2] * 4, corner[1] + corner[3] * 4, dx, dy, length, 9.0 + (k % 2) * 2.0, feather, shaft);
            }
        }
    }

    /** One feather from {@code x, y} along {@code dx, dy} (unit), {@code length} long and {@code width} wide: a vane round a shaft. */
    static void feather(UiCanvas c, double x, double y, double dx, double dy, double length, double width, int vane, int shaft) {
        double nx = -dy;
        double ny = dx;
        int n = 10;
        float[] xs = new float[n * 2];
        float[] ys = new float[n * 2];
        for (int i = 0; i < n; i++) {
            double u = (double)i / (n - 1);
            double w = width * 0.5 * Math.sin(Math.PI * Math.pow(u, 0.7)) * (u < 0.15 ? u / 0.15 : 1.0);
            double px = x + dx * length * u;
            double py = y + dy * length * u;
            xs[i] = (float)(px + nx * w);
            ys[i] = (float)(py + ny * w);
            xs[n * 2 - 1 - i] = (float)(px - nx * w * 0.8);
            ys[n * 2 - 1 - i] = (float)(py - ny * w * 0.8);
        }
        // The vane is drawn as quads between neighbouring rows (the outline is not convex).
        for (int i = 0; i + 1 < n; i++) {
            int j = n * 2 - 1 - i;
            c.polygon(new float[] {xs[i], xs[i + 1], xs[j - 1], xs[j]}, new float[] {ys[i], ys[i + 1], ys[j - 1], ys[j]}, vane);
        }
        AyatoMenuView.drawSegment(c, (int)Math.round(x), (int)Math.round(y), (int)Math.round(x + dx * length * 0.96), (int)Math.round(y + dy * length * 0.96),
            shaft, 0);
    }
}
