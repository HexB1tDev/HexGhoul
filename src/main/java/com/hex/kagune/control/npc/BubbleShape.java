package com.hex.kagune.control.npc;

import java.util.ArrayList;
import java.util.List;

/**
 * 1.3.38: a speech bubble's and a thought cloud's shapes (pure), in text pixels: x across (0 in the middle), y down, the point under it
 * (its tail's tip, or the last puff of its trail) at y 0, so everything sits above it. A speech bubble is a white rounded box with a dark
 * outline and a tail leaning down to the speaker; a thought cloud is scalloped all round (puffs along every edge), off-white with a softer
 * outline, two little puffs trailing down to the head. The outline is the same shapes a little bigger, drawn behind
 * ({@link #OUTLINE_DEPTH}); the body over it ({@link #BODY_DEPTH}); the words at depth 0, in front of both.
 */
public final class BubbleShape {
    /** Receives one quad (a triangle is a quad with its last corner twice) in colour {@code argb} at {@code depth} (behind the words). */
    public interface Fill {
        void quad(float[] a, float[] b, float[] c, float[] d, int argb, float depth);
    }

    public static final int PAD = 4;
    public static final int LINE = 10;
    public static final float TAIL = 7.0F;
    public static final float TRAIL = 9.0F;
    public static final float BODY_DEPTH = 1.5F;
    public static final float OUTLINE_DEPTH = 3.0F;
    public static final int WHITE = 0xFFFFFFFF;
    public static final int CLOUD = 0xFFF2F2F6;
    public static final int INK = 0xFF1C1C20;
    public static final int CLOUD_INK = 0xFF4A4A54;
    static final float RIM = 1.25F;

    /** Its box (x0..x1, y0..y1) and kind. */
    public final float x0;
    public final float y0;
    public final float x1;
    public final float y1;
    public final boolean thought;

    private BubbleShape(float x0, float y0, float x1, float y1, boolean thought) {
        this.x0 = x0;
        this.y0 = y0;
        this.x1 = x1;
        this.y1 = y1;
        this.thought = thought;
    }

    /** The shape round {@code lines} lines of text at most {@code width} pixels wide. */
    public static BubbleShape of(int width, int lines, boolean thought) {
        float w = Math.max(18, width) + 2 * PAD + (thought ? 4 : 0);
        float h = Math.max(1, lines) * LINE - 1 + 2 * PAD + (thought ? 2 : 0);
        float bottom = -(thought ? TRAIL : TAIL);
        return new BubbleShape(-w / 2.0F, bottom - h, w / 2.0F, bottom, thought);
    }

    /** Where line {@code i}'s top is. */
    public float lineY(int i) {
        return this.y0 + PAD + (this.thought ? 1 : 0) + i * LINE;
    }

    /** Every quad: the outline behind, then the body. */
    public void fill(Fill fill) {
        int ink = this.thought ? CLOUD_INK : INK;
        int body = this.thought ? CLOUD : WHITE;
        this.shapes(fill, RIM, ink, OUTLINE_DEPTH);
        this.shapes(fill, 0.0F, body, BODY_DEPTH);
    }

    private void shapes(Fill fill, float grow, int argb, float depth) {
        if (!this.thought) {
            rounded(fill, this.x0 - grow, this.y0 - grow, this.x1 + grow, this.y1 + grow, 4.0F + grow, argb, depth);
            // The tail: down and a little to one side, to the speaker's head; its base sunk into the box.
            float g = grow * 1.6F;
            float[] left = {-6.5F - g, this.y1 - 1.0F};
            float[] right = {1.5F + g, this.y1 - 1.0F};
            float[] tip = {-4.5F - grow * 0.4F, grow * 1.4F};
            fill.quad(left, right, tip, tip, argb, depth);
            return;
        }
        // A cloud: a rounded body, puffs every few pixels along each edge.
        rounded(fill, this.x0 - grow, this.y0 - grow, this.x1 + grow, this.y1 + grow, 5.0F + grow, argb, depth);
        for (float[] puff : this.puffs()) {
            circle(fill, puff[0], puff[1], puff[2] + grow, argb, depth);
        }
        // The trail of little puffs down to the head.
        circle(fill, -4.0F, -5.2F, 2.5F + grow, argb, depth);
        circle(fill, -6.5F, -1.6F, 1.5F + grow, argb, depth);
    }

    /** The cloud's puffs (x, y, radius) along its edges. */
    List<float[]> puffs() {
        List<float[]> out = new ArrayList<>();
        float w = this.x1 - this.x0;
        float h = this.y1 - this.y0;
        int across = Math.max(2, Math.round(w / 9.0F));
        for (int i = 0; i <= across; i++) {
            float x = this.x0 + 3.0F + (w - 6.0F) * i / across;
            float r = 4.2F + (i % 2 == 0 ? 0.8F : 0.0F);
            out.add(new float[] {x, this.y0 + 0.5F, r});
            out.add(new float[] {x, this.y1 - 0.5F, r - 0.4F});
        }
        int down = Math.max(1, Math.round(h / 9.0F));
        for (int i = 0; i <= down; i++) {
            float y = this.y0 + 3.0F + (h - 6.0F) * i / down;
            out.add(new float[] {this.x0 + 0.5F, y, 4.0F});
            out.add(new float[] {this.x1 - 0.5F, y, 4.0F});
        }
        return out;
    }

    static void rounded(Fill fill, float x0, float y0, float x1, float y1, float r, int argb, float depth) {
        float rr = Math.max(0.0F, Math.min(r, Math.min((x1 - x0) / 2.0F, (y1 - y0) / 2.0F)));
        rect(fill, x0 + rr, y0, x1 - rr, y1, argb, depth);
        rect(fill, x0, y0 + rr, x0 + rr, y1 - rr, argb, depth);
        rect(fill, x1 - rr, y0 + rr, x1, y1 - rr, argb, depth);
        corner(fill, x0 + rr, y0 + rr, rr, Math.PI, argb, depth);
        corner(fill, x1 - rr, y0 + rr, rr, 1.5 * Math.PI, argb, depth);
        corner(fill, x1 - rr, y1 - rr, rr, 0.0, argb, depth);
        corner(fill, x0 + rr, y1 - rr, rr, 0.5 * Math.PI, argb, depth);
    }

    static void rect(Fill fill, float x0, float y0, float x1, float y1, int argb, float depth) {
        if (x1 <= x0 || y1 <= y0) {
            return;
        }
        fill.quad(new float[] {x0, y0}, new float[] {x0, y1}, new float[] {x1, y1}, new float[] {x1, y0}, argb, depth);
    }

    /** A quarter of a circle round ({@code cx}, {@code cy}) from angle {@code from} (y down: 0 is +x, a quarter turn on is +y). */
    static void corner(Fill fill, float cx, float cy, float r, double from, int argb, float depth) {
        int segments = 5;
        for (int k = 0; k < segments; k++) {
            double a = from + Math.PI / 2.0 * k / segments;
            double b = from + Math.PI / 2.0 * (k + 1) / segments;
            float[] c = {cx, cy};
            float[] p = {cx + (float)(Math.cos(a) * r), cy + (float)(Math.sin(a) * r)};
            float[] q = {cx + (float)(Math.cos(b) * r), cy + (float)(Math.sin(b) * r)};
            fill.quad(c, p, q, q, argb, depth);
        }
    }

    static void circle(Fill fill, float cx, float cy, float r, int argb, float depth) {
        for (int quarter = 0; quarter < 4; quarter++) {
            corner(fill, cx, cy, r, quarter * Math.PI / 2.0, argb, depth);
        }
    }

    /** How big it is drawn {@code age} ticks after it was said: popping in a little past its size and settling. */
    public static float pop(double age) {
        if (age < 0.0 || age >= 4.0) {
            return 1.0F;
        }
        if (age < 2.5) {
            return (float)(0.55 + 0.53 * (age / 2.5));
        }
        return (float)(1.08 - 0.08 * ((age - 2.5) / 1.5));
    }

    /** True if every line is an action or a thought (NpcSpeech styles them grey and slanted): a cloud, not a bubble. */
    public static boolean thought(List<String> lines) {
        for (String line : lines) {
            if (!line.startsWith("§7§o")) {
                return false;
            }
        }
        return !lines.isEmpty();
    }

    /** A line inked for the white bubble: words black, a thought dark grey and slanted (the formatting codes it came with taken off). */
    public static String ink(String styled, boolean thought) {
        String bare = styled.replaceAll("§.", "");
        return (thought ? "§8§o" : "§0") + bare;
    }

    /** A sleeper's cloud {@code phase} ticks into its round: "z", then "z Z", then "z Z z". */
    public static String zees(double phase) {
        return phase < 15.0 ? "z" : phase < 30.0 ? "z Z" : "z Z z";
    }
}
