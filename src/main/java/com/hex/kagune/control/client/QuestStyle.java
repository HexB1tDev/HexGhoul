package com.hex.kagune.control.client;

import com.hex.kagune.control.client.ui.UiCanvas;
import com.hex.kagune.control.quest.Quest;
import com.hex.kagune.control.quest.QuestLog;

/**
 * 4.1.34 (the owner, of the quest menu: "It looks very bad. Very, very. I don't know. I just don't even feel like opening it half the time
 * ... It's just really dull, really bad looking, not interactive at all"): the journal's pieces (pure, on a {@link UiCanvas}): a quest's
 * icon by where it comes from and how it stands, chips, check boxes, thick lines, rings and small glyphs, all made of fills.
 */
final class QuestStyle {
    private QuestStyle() {
    }

    static final int TILE = 0xFF14070A;
    static final int TILE_EDGE = 0xFF3A1218;
    static final int RED = 0xFFE8323E;
    static final int RED_DARK = 0xFF7A1420;
    static final int GOLD = 0xFFF0B060;
    static final int GOLD_DARK = 0xFF6A4418;
    static final int GREEN = 0xFF7FD08A;
    static final int GREEN_DARK = 0xFF1E4A28;
    static final int GREY = 0xFF6A5A5E;
    static final int GREY_DARK = 0xFF2A2124;
    static final int WHITE = 0xFFF4DADA;

    /** {@code a} to {@code b}, {@code t} of the way (each channel; alpha too). */
    static int mix(int a, int b, float t) {
        t = Math.max(0.0F, Math.min(1.0F, t));
        int out = 0;
        for (int shift = 0; shift <= 24; shift += 8) {
            int ca = a >> shift & 255;
            int cb = b >> shift & 255;
            out |= Math.round(ca + (cb - ca) * t) << shift;
        }
        return out;
    }

    /** A state's colour: open red, done green, closed grey, locked faint. */
    static int colour(QuestLog.State state) {
        return switch (state) {
            case OPEN -> RED;
            case DONE -> GREEN;
            case CLOSED -> GREY;
            default -> 0xFF8A6A70;
        };
    }

    static int dark(QuestLog.State state) {
        return switch (state) {
            case OPEN -> RED_DARK;
            case DONE -> GREEN_DARK;
            case CLOSED -> GREY_DARK;
            default -> GREY_DARK;
        };
    }

    /** A thick line from one point to another ({@code width} pixels), as a filled quad. */
    static void line(UiCanvas c, float x0, float y0, float x1, float y1, float width, int colour) {
        float dx = x1 - x0;
        float dy = y1 - y0;
        float length = (float)Math.sqrt(dx * dx + dy * dy);
        if (length < 1.0E-3F) {
            return;
        }
        float nx = -dy / length * width * 0.5F;
        float ny = dx / length * width * 0.5F;
        c.polygon(new float[] {x0 + nx, x1 + nx, x1 - nx, x0 - nx}, new float[] {y0 + ny, y1 + ny, y1 - ny, y0 - ny}, colour);
    }

    static void circle(UiCanvas c, int x, int y, int radius, int colour) {
        UkakuMenuView.fillCircle(c, x, y, radius, colour);
    }

    /** A ring {@code thick} wide on {@code back} (the colour inside it). */
    static void ring(UiCanvas c, int x, int y, int radius, int thick, int colour, int back) {
        circle(c, x, y, radius, colour);
        circle(c, x, y, Math.max(0, radius - thick), back);
    }

    /** A rounded box (corners cut a pixel), its edge {@code edge}, filled {@code fill}. */
    static void box(UiCanvas c, int x, int y, int width, int height, int fill, int edge) {
        c.fill(x + 1, y, x + width - 1, y + height, edge);
        c.fill(x, y + 1, x + width, y + height - 1, edge);
        c.fill(x + 1, y + 1, x + width - 1, y + height - 1, fill);
    }

    /** A chip: {@code text} in a pill of {@code fill} with an edge, its width returned. */
    static int chip(UiCanvas c, int x, int y, String text, int fill, int edge, int ink) {
        int width = c.width(text) + 12;
        box(c, x, y, width, 13, fill, edge);
        c.text(text, x + 6, y + 3, ink, false);
        return width;
    }

    /** A check mark in a box {@code size} square at {@code x, y}. */
    static void check(UiCanvas c, int x, int y, int size, int colour) {
        line(c, x + size * 0.18F, y + size * 0.52F, x + size * 0.42F, y + size * 0.76F, Math.max(1.5F, size * 0.16F), colour);
        line(c, x + size * 0.42F, y + size * 0.76F, x + size * 0.84F, y + size * 0.26F, Math.max(1.5F, size * 0.16F), colour);
    }

    /** A padlock in a box {@code size} square. */
    static void lock(UiCanvas c, int x, int y, int size, int colour, int back) {
        int bodyTop = y + Math.round(size * 0.45F);
        c.fill(x + Math.round(size * 0.18F), bodyTop, x + Math.round(size * 0.82F), y + Math.round(size * 0.92F), colour);
        int cx = x + size / 2;
        int r = Math.max(2, Math.round(size * 0.24F));
        ring(c, cx, bodyTop, r, Math.max(1, size / 9), colour, back);
        c.fill(cx - r, bodyTop, cx + r + 1, bodyTop + 1, colour);
        c.fill(cx - 1, bodyTop + Math.round(size * 0.18F), cx + 1, bodyTop + Math.round(size * 0.34F), back);
    }

    /** A cross (closed) in a box {@code size} square. */
    static void cross(UiCanvas c, int x, int y, int size, int colour) {
        line(c, x + size * 0.22F, y + size * 0.22F, x + size * 0.78F, y + size * 0.78F, Math.max(1.5F, size * 0.14F), colour);
        line(c, x + size * 0.78F, y + size * 0.22F, x + size * 0.22F, y + size * 0.78F, Math.max(1.5F, size * 0.14F), colour);
    }

    /**
     * A quest's glyph in a box {@code size} square: the path's a kagune's curling tendril, the ghoul's way an eye (the kakugan), a hunt the
     * moon, a favour two hands; done a check, closed a cross, locked a padlock over a dim glyph.
     */
    static void glyph(UiCanvas c, Quest.Line line, QuestLog.State state, int x, int y, int size, int back, double time) {
        int ink = colour(state);
        if (state == QuestLog.State.DONE) {
            check(c, x, y, size, ink);
            return;
        }
        if (state == QuestLog.State.CLOSED) {
            cross(c, x, y, size, ink);
            return;
        }
        int dim = state == QuestLog.State.LOCKED ? mix(ink, back, 0.55F) : ink;
        float cx = x + size * 0.5F;
        float cy = y + size * 0.5F;
        switch (line) {
            case PATH -> {
                // A tendril curling up out of the bottom, thinning to its tip.
                float px = x + size * 0.3F;
                float py = y + size * 0.92F;
                float sway = (float)Math.sin(time * 2.0) * size * 0.04F * (state == QuestLog.State.OPEN ? 1.0F : 0.0F);
                for (int i = 1; i <= 10; i++) {
                    float t = i / 10.0F;
                    float a = (float)(Math.PI * 1.25 * t);
                    float nx = cx + (float)Math.cos(a + Math.PI) * size * 0.28F * (1.0F - 0.3F * t) + sway * t;
                    float ny = y + size * 0.92F - t * size * 0.72F + (float)Math.sin(a) * size * 0.06F;
                    line(c, px, py, nx, ny, Math.max(1.2F, size * 0.2F * (1.0F - 0.75F * t)), dim);
                    px = nx;
                    py = ny;
                }
            }
            case GHOUL -> {
                // An eye: its lens, the red iris, the black slit.
                int n = 18;
                float[] xs = new float[n];
                float[] ys = new float[n];
                for (int i = 0; i < n; i++) {
                    float t = (float)(Math.PI * 2.0 * i / n);
                    xs[i] = cx + (float)Math.cos(t) * size * 0.46F;
                    ys[i] = cy + (float)Math.sin(t) * size * 0.26F * (float)Math.abs(Math.sin(t)) ;
                }
                c.polygon(xs, ys, mix(dim, 0xFF000000, 0.55F));
                circle(c, Math.round(cx), Math.round(cy), Math.max(2, Math.round(size * 0.2F)), dim);
                c.fill(Math.round(cx) - 1, Math.round(cy - size * 0.16F), Math.round(cx) + 1, Math.round(cy + size * 0.16F), 0xFF050203);
            }
            case HUNT -> {
                // The moon: a disc with a bite.
                circle(c, Math.round(cx), Math.round(cy), Math.round(size * 0.38F), dim);
                circle(c, Math.round(cx + size * 0.17F), Math.round(cy - size * 0.1F), Math.round(size * 0.32F), back);
            }
            default -> {
                // A favour: two hands meeting (two slanted palms).
                line(c, x + size * 0.18F, y + size * 0.78F, x + size * 0.46F, y + size * 0.3F, size * 0.22F, dim);
                line(c, x + size * 0.82F, y + size * 0.78F, x + size * 0.54F, y + size * 0.3F, size * 0.22F, dim);
            }
        }
        if (state == QuestLog.State.LOCKED) {
            lock(c, x + size / 2, y + size / 2, size / 2, mix(GOLD, back, 0.2F), back);
        }
    }

    /**
     * 4.1.34 (the owner: "the background and all, they're also super ugly"): the board a clean dark plate, its corners cut, a thin red edge with bright brackets at each corner,
     * a faint honeycomb of RC cells in a corner, a soft rule under its header. {@code x, y, width, height}: the board.
     */
    static void backdrop(UiCanvas c, int x, int y, int width, int height, double time) {
        backdrop(c, x, y, width, height, time, x + 12, y + height - 84, 140, 70);
    }

    /** 4.1.55: the same board with its honeycomb in the box {@code hx, hy, hw, hh} (where nothing is drawn over it: the talk box's header). */
    static void backdrop(UiCanvas c, int x, int y, int width, int height, double time, int hx, int hy, int hw, int hh) {
        // A soft shadow round the box (the world seen past it).
        for (int k = 8; k >= 1; k--) {
            int a = Math.max(0, 0x22 - k * 3) << 24;
            c.fill(x - k + 4, y - k + 6, x + width + k + 4, y + height + k + 6, a);
        }
        // The plate: corners cut, a vertical gradient inside a thin edge.
        int cut = 12;
        float[] xs = {x + cut, x + width - cut, x + width, x + width, x + width - cut, x + cut, x, x};
        float[] ys = {y, y, y + cut, y + height - cut, y + height, y + height, y + height - cut, y + cut};
        c.polygon(xs, ys, 0xFF4A141C);
        float[] ixs = {x + cut, x + width - cut, x + width - 1, x + width - 1, x + width - cut, x + cut, x + 1, x + 1};
        float[] iys = {y + 1, y + 1, y + cut, y + height - cut, y + height - 1, y + height - 1, y + height - cut, y + cut};
        c.polygon(ixs, iys, 0xF20C0507);
        c.gradient(x + cut, y + 2, x + width - cut, y + 80, 0x30E8323E, 0x00E8323E);
        // The honeycomb of RC cells, faint, in the bottom left (under the list, where it runs out).
        honeycomb(c, hx, hy, hw, hh, 0x14E8323E, time + 2.0);
        // Bright brackets at the corners.
        int arm = 22;
        int bright = mix(RED_DARK, RED, 0.7F + 0.3F * (float)Math.sin(time * 1.6));
        line(c, x + cut, y + 1, x + cut + arm, y + 1, 2.0F, bright);
        line(c, x + 1, y + cut, x + 1, y + cut + arm, 2.0F, bright);
        line(c, x + 1, y + cut, x + cut, y + 1, 2.0F, bright);
        line(c, x + width - cut, y + 1, x + width - cut - arm, y + 1, 2.0F, bright);
        line(c, x + width - 1, y + cut, x + width - 1, y + cut + arm, 2.0F, bright);
        line(c, x + width - 1, y + cut, x + width - cut, y + 1, 2.0F, bright);
        line(c, x + cut, y + height - 1, x + cut + arm, y + height - 1, 2.0F, bright);
        line(c, x + 1, y + height - cut, x + 1, y + height - cut - arm, 2.0F, bright);
        line(c, x + 1, y + height - cut, x + cut, y + height - 1, 2.0F, bright);
        line(c, x + width - cut, y + height - 1, x + width - cut - arm, y + height - 1, 2.0F, bright);
        line(c, x + width - 1, y + height - cut, x + width - 1, y + height - cut - arm, 2.0F, bright);
        line(c, x + width - 1, y + height - cut, x + width - cut, y + height - 1, 2.0F, bright);
    }

    /** A faint honeycomb of hexagons outlined in {@code colour} filling the box; a cell or two lit now and then. */
    static void honeycomb(UiCanvas c, int x, int y, int width, int height, int colour, double time) {
        int r = 9;
        float w = (float)(Math.sqrt(3.0) * r);
        int row = 0;
        for (float cy = y + r; cy < y + height - r * 0.5F; cy += r * 1.5F, row++) {
            for (float cx = x + w * 0.5F + (row % 2 == 0 ? 0.0F : w * 0.5F); cx < x + width - w * 0.4F; cx += w) {
                int lit = (int)(Math.floor(time * 0.8) * 7 + cx * 3 + cy) % 23 == 0 ? 0x30E8323E : 0;
                if (lit != 0) {
                    hexagon(c, cx, cy, r - 1.0F, 0.0F, lit, true);
                }
                hexagon(c, cx, cy, r - 1.0F, 1.0F, colour, false);
            }
        }
    }

    /** A hexagon about {@code cx, cy} (pointy top), its outline {@code thick} wide, or filled. */
    static void hexagon(UiCanvas c, float cx, float cy, float r, float thick, int colour, boolean filled) {
        float[] xs = new float[6];
        float[] ys = new float[6];
        for (int i = 0; i < 6; i++) {
            double a = Math.PI / 3.0 * i - Math.PI / 2.0;
            xs[i] = cx + (float)Math.cos(a) * r;
            ys[i] = cy + (float)Math.sin(a) * r;
        }
        if (filled) {
            c.polygon(xs, ys, colour);
            return;
        }
        for (int i = 0; i < 6; i++) {
            int j = (i + 1) % 6;
            line(c, xs[i], ys[i], xs[j], ys[j], thick, colour);
        }
    }

    /** A small diamond about {@code cx, cy}. */
    static void polygonDiamond(UiCanvas c, float cx, float cy, float r, int colour) {
        c.polygon(new float[] {cx, cx + r, cx, cx - r}, new float[] {cy - r, cy, cy + r, cy}, colour);
    }

    /** A glyph for a reward chip: mastery (a rising arrow), an unlock (a key), points (a star), meat (a drop). */
    static void rewardGlyph(UiCanvas c, char kind, int x, int y, int colour, int back) {
        switch (kind) {
            case 'm' -> {
                line(c, x + 5, y + 10, x + 5, y + 2, 2.0F, colour);
                line(c, x + 1.5F, y + 5.5F, x + 5, y + 2, 2.0F, colour);
                line(c, x + 8.5F, y + 5.5F, x + 5, y + 2, 2.0F, colour);
            }
            case 'k' -> {
                ring(c, x + 3, y + 6, 3, 1, colour, back);
                line(c, x + 5.5F, y + 6, x + 10, y + 6, 1.6F, colour);
                c.fill(x + 8, y + 6, x + 9, y + 9, colour);
            }
            case 'p' -> {
                float[] xs = new float[10];
                float[] ys = new float[10];
                for (int i = 0; i < 10; i++) {
                    double a = Math.PI * 2.0 * i / 10 - Math.PI / 2.0;
                    float r = i % 2 == 0 ? 5.0F : 2.2F;
                    xs[i] = x + 5 + (float)Math.cos(a) * r;
                    ys[i] = y + 6 + (float)Math.sin(a) * r;
                }
                for (int i = 0; i < 10; i += 2) {
                    c.polygon(new float[] {x + 5, xs[i], xs[(i + 1) % 10]}, new float[] {y + 6, ys[i], ys[(i + 1) % 10]}, colour);
                    c.polygon(new float[] {x + 5, xs[(i + 9) % 10], xs[i]}, new float[] {y + 6, ys[(i + 9) % 10], ys[i]}, colour);
                }
            }
            default -> {
                circle(c, x + 5, y + 7, 3, colour);
                c.polygon(new float[] {x + 2.2F, x + 5, x + 7.8F}, new float[] {y + 6, y + 1, y + 6}, colour);
            }
        }
    }
}
