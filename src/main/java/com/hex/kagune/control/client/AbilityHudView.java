package com.hex.kagune.control.client;

import com.hex.kagune.control.AbilityGuide;
import com.hex.kagune.control.client.ui.UiCanvas;
import java.util.List;

/**
 * The abilities panel drawn on any {@link UiCanvas} (the game's GUI or the preview's image): a small dark card with the form's
 * colour down its left edge, its title, and one line a move: the player's keys in little chips, the move's name in its colour;
 * a move not reached yet dim, with the mastery it needs. Laid out once per panel ({@link Layout}), so a frame only fills and
 * writes.
 */
public final class AbilityHudView {
    static final int PAD = 4;
    static final int LINE = 10;
    static final int CHIP_PAD = 3;
    static final int GAP = 5;
    static final int BACK = 0xC0100C13;
    static final int EDGE = 0x30FFFFFF;
    static final int CHIP = 0xFF2A2430;
    static final int CHIP_SHADE = 0xFF17131B;

    /** Where everything goes, worked out once per panel; {@code tail}: room kept at the right for a cooldown (1.0.22). */
    public record Layout(AbilityGuide.Panel panel, int width, int height, int keyWidth, String[] needs, int tail) {
    }

    /** The widest cooldown a row shows ({@link #seconds}). */
    static final String WIDEST = "00.0s";

    private AbilityHudView() {
    }

    /** The panel's layout on {@code canvas} (text widths measured once). */
    public static Layout layout(UiCanvas canvas, AbilityGuide.Panel panel) {
        return layout(canvas, panel, false);
    }

    /** 1.0.22: the same, with room for each row's cooldown ({@code cooldowns}: War's and Peace's abilities). */
    public static Layout layout(UiCanvas canvas, AbilityGuide.Panel panel, boolean cooldowns) {
        int keyWidth = 0;
        int nameWidth = 0;
        List<AbilityGuide.Row> rows = panel.rows();
        String[] needs = new String[rows.size()];
        for (int i = 0; i < rows.size(); i++) {
            AbilityGuide.Row row = rows.get(i);
            keyWidth = Math.max(keyWidth, canvas.width(row.keys()) + 2 * CHIP_PAD);
            needs[i] = row.locked() ? " " + row.needs() : "";
            nameWidth = Math.max(nameWidth, canvas.width(row.name() + needs[i]));
        }
        int header = Math.max(canvas.width(panel.title()) + 1, canvas.width(panel.subtitle()));
        int tail = cooldowns ? canvas.width(WIDEST) : 0;
        int width = 2 + PAD + Math.max(keyWidth + GAP + nameWidth + (tail > 0 ? GAP + tail : 0), header) + PAD + 2;
        int height = PAD + 9 + 9 + 3 + rows.size() * LINE + PAD - 1;
        return new Layout(panel, width, height, keyWidth, needs, tail);
    }

    /** Draws the panel with its bottom-left corner at ({@code left}, {@code bottom}), {@code scale} times its size. */
    public static void draw(UiCanvas c, Layout layout, int left, int bottom, float scale) {
        draw(c, layout, left, bottom, scale, null, null);
    }

    /**
     * 1.0.22: the same, each row {@code i} with how far its cooldown has come ({@code ready[i]}, 0..1; 1 or past the end: ready),
     * shown as its key cap's underline filling back up and its name dim, and how long is left ({@code wait[i]}, ticks) at the right.
     */
    public static void draw(UiCanvas c, Layout layout, int left, int bottom, float scale, double[] ready, long[] wait) {
        AbilityGuide.Panel panel = layout.panel();
        int w = layout.width();
        int h = layout.height();
        c.push();
        c.translate(left, bottom - h * scale);
        c.scale(scale);
        // The card, its corners cut, and the form's colour down the left.
        c.fill(1, 0, w - 1, h, BACK);
        c.fill(0, 1, 1, h - 1, BACK);
        c.fill(w - 1, 1, w, h - 1, BACK);
        c.fill(1, 0, w - 1, 1, EDGE);
        c.fill(0, 1, 2, h - 1, panel.accent());
        int x = 2 + PAD;
        int y = PAD;
        c.text(panel.title(), x, y, panel.accent(), true);
        c.text(panel.subtitle(), x, y + 9, AbilityGuide.DIM, false);
        y += 9 + 9 + 1;
        c.fill(x, y, w - PAD, y + 1, (panel.accent() & 0x00FFFFFF) | 0x50000000);
        y += 2;
        List<AbilityGuide.Row> rows = panel.rows();
        for (int i = 0; i < rows.size(); i++) {
            AbilityGuide.Row row = rows.get(i);
            int top = y + i * LINE;
            int chip = c.width(row.keys()) + 2 * CHIP_PAD;
            double share = ready == null || i >= ready.length ? 1.0 : Math.max(0.0, Math.min(1.0, ready[i]));
            boolean waiting = share < 1.0;
            // The chip, right-aligned in the key column so the names line up.
            int chipLeft = x + layout.keyWidth() - chip;
            // A little key cap: lit face, the move's colour as its underline (filling back up while it recharges).
            c.fill(chipLeft, top - 1, chipLeft + chip, top + 8, row.locked() ? CHIP_SHADE : CHIP);
            c.fill(chipLeft, top + 8, chipLeft + chip, top + 9, row.locked() || waiting ? CHIP_SHADE : (row.colour() & 0x00FFFFFF) | 0xA0000000);
            if (waiting && !row.locked()) {
                c.fill(chipLeft, top + 8, chipLeft + (int)Math.round(chip * share), top + 9, (row.colour() & 0x00FFFFFF) | 0x70000000);
            }
            c.text(row.keys(), chipLeft + CHIP_PAD, top, row.locked() || waiting ? AbilityGuide.DIM : AbilityGuide.TEXT, false);
            int nameX = x + layout.keyWidth() + GAP;
            c.text(row.name(), nameX, top, row.locked() || waiting ? AbilityGuide.DIM : row.colour(), false);
            if (row.locked()) {
                c.text(layout.needs()[i], nameX + c.width(row.name()), top, 0xFF5A5260, false);
            }
            if (waiting && wait != null && i < wait.length && layout.tail() > 0) {
                String tail = seconds(wait[i]);
                c.text(tail, w - PAD - 2 - c.width(tail), top, AbilityGuide.DIM, false);
            }
        }
        c.pop();
    }

    /** {@code ticks} as seconds, one decimal under ten. */
    public static String seconds(long ticks) {
        double s = ticks / 20.0;
        return s < 10.0 ? String.format(java.util.Locale.ROOT, "%.1fs", s) : Math.round(s) + "s";
    }
}
