package com.hex.kagune.control.client;

import com.hex.kagune.control.AbilityGuide;
import com.hex.kagune.control.client.ui.UiCanvas;
import java.util.List;

/**
 * The abilities panel drawn on any {@link UiCanvas} (the game's GUI or the preview's image): a small dark card with the form's
 * colour down its left edge, its title, and one line a move: the player's keys in little chips, the move's name in its colour;
 * a move not reached yet dim, with the mastery it needs. Laid out once per panel ({@link Layout}), so a frame only fills and
 * writes. 3.4.6 (the owner: "it's time to redo the GUI. This looks super, super clanky. As in, like, it's too big. We need to summarize
 * things a bit more."): the panel in brief ({@link AbilityGuide#brief}), its title and subtitle on one line. 3.4.7 (the owner, of
 * 3.4.6's two columns: "it should be vertical, and you made it horizontal ... It looks like shit"): one narrow column again, its
 * rows packed closer, drawn smaller ({@link AbilityHud#SCALE}).
 */
public final class AbilityHudView {
    static final int PAD = 3;
    static final int LINE = 10;
    static final int CHIP_PAD = 2;
    static final int GAP = 3;
    /** 3.4.6: between the title and the subtitle; between columns (3.4.7: there is only ever one). */
    static final int TITLE_GAP = 4;
    static final int COLUMN_GAP = 9;
    static final int BACK = 0xC0100C13;
    static final int EDGE = 0x30FFFFFF;
    static final int CHIP = 0xFF2A2430;
    static final int CHIP_SHADE = 0xFF17131B;

    /**
     * Where everything goes, worked out once per panel (the panel in brief): its size, how many columns (3.4.7: one) and moves to a
     * column, each column's left edge, key column and name column widths; {@code tail}: room kept after each name for a cooldown (1.0.22).
     */
    public record Layout(AbilityGuide.Panel panel, int width, int height, int columns, int perColumn, int[] left, int[] keyWidth, int[] nameWidth,
                         String[] needs, int tail) {
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
    public static Layout layout(UiCanvas canvas, AbilityGuide.Panel full, boolean cooldowns) {
        AbilityGuide.Panel panel = AbilityGuide.brief(full);
        List<AbilityGuide.Row> rows = panel.rows();
        int n = rows.size();
        // 3.4.7: one column, top to bottom, whatever its length.
        int columns = 1;
        int per = Math.max(1, (n + columns - 1) / columns);
        int[] keyWidth = new int[columns];
        int[] nameWidth = new int[columns];
        String[] needs = new String[n];
        for (int i = 0; i < n; i++) {
            AbilityGuide.Row row = rows.get(i);
            int column = Math.min(columns - 1, i / per);
            keyWidth[column] = Math.max(keyWidth[column], canvas.width(row.keys()) + 2 * CHIP_PAD);
            needs[i] = row.locked() ? " " + row.needs() : "";
            nameWidth[column] = Math.max(nameWidth[column], canvas.width(row.name() + needs[i]));
        }
        int tail = cooldowns ? canvas.width(WIDEST) : 0;
        int[] left = new int[columns];
        int x = 2 + PAD;
        for (int column = 0; column < columns; column++) {
            left[column] = x;
            x += keyWidth[column] + GAP + nameWidth[column] + (tail > 0 ? GAP + tail : 0) + COLUMN_GAP;
        }
        int body = x - COLUMN_GAP - (2 + PAD);
        int header = canvas.width(panel.title()) + 1 + (panel.subtitle().isEmpty() ? 0 : TITLE_GAP + canvas.width(panel.subtitle()));
        int width = 2 + PAD + Math.max(body, header) + PAD + 2;
        int height = PAD + 9 + 3 + per * LINE + PAD - 1;
        return new Layout(panel, width, height, columns, per, left, keyWidth, nameWidth, needs, tail);
    }

    /** Draws the panel with its bottom-left corner at ({@code left}, {@code bottom}), {@code scale} times its size. */
    public static void draw(UiCanvas c, Layout layout, int left, int bottom, float scale) {
        draw(c, layout, left, bottom, scale, null, null);
    }

    /**
     * 1.0.22: the same, each row {@code i} with how far its cooldown has come ({@code ready[i]}, 0..1; 1 or past the end: ready),
     * shown as its key cap's underline filling back up and its name dim, and how long is left ({@code wait[i]}, ticks) after its name.
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
        // 3.4.6: the title and what the form is, on one line.
        c.text(panel.title(), x, y, panel.accent(), true);
        if (!panel.subtitle().isEmpty()) {
            c.text(panel.subtitle(), x + c.width(panel.title()) + 1 + TITLE_GAP, y, AbilityGuide.DIM, false);
        }
        y += 9 + 1;
        c.fill(x, y, w - PAD, y + 1, (panel.accent() & 0x00FFFFFF) | 0x50000000);
        y += 2;
        List<AbilityGuide.Row> rows = panel.rows();
        for (int i = 0; i < rows.size(); i++) {
            AbilityGuide.Row row = rows.get(i);
            int column = Math.min(layout.columns() - 1, i / layout.perColumn());
            int top = y + (i - column * layout.perColumn()) * LINE;
            int keys = layout.left()[column];
            int keyWidth = layout.keyWidth()[column];
            int chip = c.width(row.keys()) + 2 * CHIP_PAD;
            double share = ready == null || i >= ready.length ? 1.0 : Math.max(0.0, Math.min(1.0, ready[i]));
            boolean waiting = share < 1.0;
            // The chip, right-aligned in its column's keys so the names line up.
            int chipLeft = keys + keyWidth - chip;
            // A little key cap: lit face, the move's colour as its underline (filling back up while it recharges).
            c.fill(chipLeft, top - 1, chipLeft + chip, top + 8, row.locked() ? CHIP_SHADE : CHIP);
            c.fill(chipLeft, top + 8, chipLeft + chip, top + 9, row.locked() || waiting ? CHIP_SHADE : (row.colour() & 0x00FFFFFF) | 0xA0000000);
            if (waiting && !row.locked()) {
                c.fill(chipLeft, top + 8, chipLeft + (int)Math.round(chip * share), top + 9, (row.colour() & 0x00FFFFFF) | 0x70000000);
            }
            c.text(row.keys(), chipLeft + CHIP_PAD, top, row.locked() || waiting ? AbilityGuide.DIM : AbilityGuide.TEXT, false);
            int nameX = keys + keyWidth + GAP;
            c.text(row.name(), nameX, top, row.locked() || waiting ? AbilityGuide.DIM : row.colour(), false);
            if (row.locked()) {
                c.text(layout.needs()[i], nameX + c.width(row.name()), top, 0xFF5A5260, false);
            }
            if (waiting && wait != null && i < wait.length && layout.tail() > 0) {
                String tail = seconds(wait[i]);
                c.text(tail, nameX + layout.nameWidth()[column] + GAP + layout.tail() - c.width(tail), top, AbilityGuide.DIM, false);
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
