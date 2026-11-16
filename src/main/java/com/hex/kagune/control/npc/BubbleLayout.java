package com.hex.kagune.control.npc;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 1.3.52: speech bubbles kept apart (pure, tested; the owner: "speech bubbles overlapping each other during squabbles"). Each bubble up
 * this frame is seen from the camera: its point (over the speaker's head) as angles across and up the view, and its box in its own text
 * pixels ({@link BubbleShape}), so many radians a pixel at its distance. Two that would overlap are spread apart sideways, each half the
 * way and toward its own speaker's side, their tails leaning back down to their heads (as a comic places two speakers' bubbles: never one
 * stacked over the other with its tail through the other's words); only when that would take one too far from its speaker does the newer
 * go up over the older instead. A talk's turns no longer overlap at all (each line comes down as the reply goes up: {@code S2CNpcSay}'s
 * life); this is for everything else (two talks side by side, a child on its parent's hip saying something while the parent's own remark
 * is up, a word across a squabble).
 */
public final class BubbleLayout {
    /** Pixels kept clear between two bubbles. */
    public static final float GAP = 3.0F;
    /** How far past its own half width a bubble may be moved aside (pixels) before the newer goes up instead. */
    public static final float REACH = 40.0F;
    /** Rounds of spreading at most (each pair that overlaps pushed apart, again until none does). */
    static final int ROUNDS = 40;

    /**
     * One bubble as seen this frame: whose ({@code id}), how long it has been up ({@code age}, ticks), its point's place across
     * ({@code x}, to the right) and up ({@code y}) the view (radians), radians a text pixel at its distance, and its box about its point in
     * its own pixels (x right, y down, as {@link BubbleShape} has it, unmoved), and how far what is drawn reaches past that box (a cloud's
     * puffs, the outline: {@link BubbleShape#rim}).
     */
    public record Seen(int id, double age, double x, double y, double perPixel, float x0, float y0, float x1, float y1, float rim) {
        public static Seen of(int id, double age, double x, double y, double perPixel, BubbleShape shape) {
            return new Seen(id, age, x, y, perPixel, shape.x0, shape.y0, shape.x1, shape.y1, shape.rim());
        }

        double most() {
            return (this.x1 - this.x0) / 2.0 + REACH;
        }
    }

    /** Where a bubble goes: so many pixels to the right ({@code shift}) and up ({@code lift}) of its usual place. */
    public record Place(float shift, float lift) {
        public static final Place HOME = new Place(0.0F, 0.0F);
    }

    private BubbleLayout() {
    }

    /** What it covers on the view (radians: left, bottom, right, top; y up; its rim in) moved by {@code shift} and {@code lift} pixels. */
    static double[] box(Seen b, double shift, double lift) {
        double k = b.perPixel();
        double r = b.rim();
        return new double[] {b.x() + (b.x0() - r + shift) * k, b.y() + (-b.y1() - r + lift) * k, b.x() + (b.x1() + r + shift) * k,
            b.y() + (-b.y0() + r + lift) * k};
    }

    /** True if two boxes on the view overlap, {@code gap} radians kept clear. */
    static boolean overlaps(double[] a, double[] b, double gap) {
        return a[0] < b[2] + gap && b[0] < a[2] + gap && a[1] < b[3] + gap && b[1] < a[3] + gap;
    }

    /** Every bubble's place this frame: over its own speaker if it can be, else spread apart from the others (the same whatever order). */
    public static Map<Integer, Place> arrange(List<Seen> seen) {
        List<Seen> order = new ArrayList<>(seen);
        order.sort(Comparator.comparingDouble(Seen::age).reversed().thenComparingInt(Seen::id));
        int n = order.size();
        double[] shift = new double[n];
        double[] lift = new double[n];
        // Spread sideways: each pair that overlaps pushed apart, half each (the rest by the other if one can go no further).
        for (int round = 0; round < ROUNDS; round++) {
            boolean moved = false;
            for (int i = 0; i < n; i++) {
                for (int j = i + 1; j < n; j++) {
                    moved |= spread(order.get(i), order.get(j), shift, lift, i, j);
                }
            }
            if (!moved) {
                break;
            }
        }
        // Moved well aside, a little up too: its tail angles down to the head instead of lying flat.
        for (int i = 0; i < n; i++) {
            lift[i] += tilt(order.get(i), shift[i]);
        }
        // Still in each other's way (no room aside): the newer up over the older, as far as it takes.
        for (int round = 0; round < 8; round++) {
            boolean moved = false;
            for (int i = 0; i < n; i++) {
                for (int j = i + 1; j < n; j++) {
                    Seen a = order.get(i);
                    Seen b = order.get(j);
                    double gap = GAP * Math.min(a.perPixel(), b.perPixel());
                    double[] p = box(a, shift[i], lift[i]);
                    double[] q = box(b, shift[j], lift[j]);
                    if (overlaps(p, q, gap)) {
                        lift[j] += (p[3] + gap - q[1]) / Math.max(1.0E-9, b.perPixel()) + 0.01;
                        moved = true;
                    }
                }
            }
            if (!moved) {
                break;
            }
        }
        Map<Integer, Place> out = new HashMap<>();
        for (int i = 0; i < n; i++) {
            out.put(order.get(i).id(), Math.abs(shift[i]) < 1.0E-6 && lift[i] < 1.0E-6 ? Place.HOME : new Place((float)shift[i], (float)lift[i]));
        }
        return out;
    }

    /** {@code a} (the older) and {@code b} pushed apart sideways if they overlap; true if either moved. */
    private static boolean spread(Seen a, Seen b, double[] shift, double[] lift, int i, int j) {
        double ka = Math.max(1.0E-9, a.perPixel());
        double kb = Math.max(1.0E-9, b.perPixel());
        double gap = GAP * Math.min(ka, kb);
        double[] p = box(a, shift[i], lift[i]);
        double[] q = box(b, shift[j], lift[j]);
        if (!overlaps(p, q, gap)) {
            return false;
        }
        // Which way: b to the side its own speaker is on (or its box already is); right on top of each other, the newer to the right.
        double apart = b.x() - a.x();
        if (Math.abs(apart) < 1.0E-6) {
            apart = (q[0] + q[2]) - (p[0] + p[2]);
        }
        int dir = apart < -1.0E-6 ? -1 : 1;
        double need = dir > 0 ? p[2] + gap - q[0] : q[2] + gap - p[0]; // radians
        if (need <= 0.0) {
            return false;
        }
        double half = need / 2.0;
        double wantA = shift[i] - dir * half / ka;
        double wantB = shift[j] + dir * half / kb;
        double gotA = clamp(wantA, a.most());
        double gotB = clamp(wantB, b.most());
        // One that can go no further: the other takes the rest, as far as it can.
        double shortA = Math.abs(wantA - gotA) * ka;
        double shortB = Math.abs(wantB - gotB) * kb;
        gotB = clamp(gotB + dir * shortA / kb, b.most());
        gotA = clamp(gotA - dir * shortB / ka, a.most());
        boolean moved = Math.abs(gotA - shift[i]) > 1.0E-4 || Math.abs(gotB - shift[j]) > 1.0E-4;
        shift[i] = gotA;
        shift[j] = gotB;
        return moved;
    }

    /**
     * How far up a bubble moved {@code shift} pixels aside goes so its tail isn't flat (pixels): a share of how far the tail runs sideways
     * from the box's corner to the head ({@link BubbleShape}'s tail base and point), at most {@link #TILT}.
     */
    static double tilt(Seen b, double shift) {
        double base = Math.max(b.x0() + shift + 6.5, Math.min(b.x1() + shift - 6.5, -2.5));
        double run = Math.abs(base + 4.5);
        return Math.max(0.0, Math.min(TILT, (run - 12.0) * 0.3));
    }

    /** The most a bubble goes up for its tail's sake (pixels). */
    public static final float TILT = 12.0F;

    private static double clamp(double shift, double most) {
        return Math.max(-most, Math.min(most, shift));
    }
}
