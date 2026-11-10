package com.hex.kagune.control.outfit;

import java.util.ArrayList;
import java.util.List;

/**
 * 4.0.3 (the owner, of the renders: "Do I need to say anything about both outfit's legs and arms?"): a coat's tails over the legs. Down to
 * the knee's line its fronts are turned {@code front} about the hip and its backs {@code back} (radians about x, a leg's own sense: forward
 * is less than 0), each side spread between the two; below it they fold on the leg's middle, as bendy-lib bends the trousers, and hang
 * {@code frontLow} and {@code backLow}. Both fronts go with the leg furthest forward and both backs with the one furthest back, so the
 * tails never part down the middle; and wherever a leg reaches (its trousers, a bent knee, a shin, a boot's toe or heel: walking, running,
 * crouching, sitting, kneeling, kicking) they are pushed just clear of it. Otherwise they hang straight down.
 */
public record CoatDrape(float front, float back, float frontLow, float backLow) {
    /** Where the tails fold (pixels below the hip: the knee's line, where they are cut) and over how much of their height either side. */
    public static final float KNEE = 6.0F;
    public static final float FOLD = 0.35F;
    /** How far below the knee's line the tails reach (a little past their hem). */
    public static final float BELOW = 4.0F;
    /** How far in front of a leg's middle and behind it the tails hang (pixels): just over a trouser leg, a boot's shaft. */
    public static final float FRONT = 2.45F;
    public static final float BACK = 2.35F;
    /** How much clearer of a leg than its own outline they keep (pixels). */
    static final float GROW = 0.15F;
    /** How far the upper tails and the lower may turn (radians, either way). */
    static final float UP_MOST = 1.5F;
    static final float LOW_MOST = 2.2F;
    /** How closely along a leg's outline the tails look for it (pixels). */
    static final float STEP = 0.5F;

    /**
     * Hung over thighs turned {@code thigh} with knees bent {@code knee} (right, left: the shin back from the thigh), from {@code front} and
     * {@code back} at least as far as they already go (the leg furthest forward, the one furthest back, or further while they fall back).
     */
    public static CoatDrape over(float front, float back, float[] thigh, float[] knee) {
        List<float[]> upper = new ArrayList<>();
        List<float[]> lower = new ArrayList<>();
        for (int leg = 0; leg < 2; leg++) {
            for (int side = -1; side <= 1; side += 2) {
                float[][][] lines = outline(thigh[leg], knee[leg], side, GROW);
                for (int k = 0; k < lines.length; k++) {
                    points(lines[k], k == 0 ? upper : lower);
                }
            }
        }
        upper.addAll(lower);
        // Above the knee's line: whatever of a leg runs alongside the tails (a thigh, a shin folded up behind it) is under them.
        double reachUp = Math.sqrt(KNEE * KNEE + FRONT * FRONT) + 0.3;
        for (float[] q : upper) {
            double r = Math.sqrt(q[0] * q[0] + q[1] * q[1]);
            if (r > reachUp || r <= BACK) {
                continue;
            }
            double at = Math.atan2(q[1], q[0]);
            if (r > FRONT && Math.cos(at - front) >= 0.0) {
                front = (float)Math.min(front, at + Math.asin(FRONT / r));
            }
            if (Math.cos(at - back) >= 0.0) {
                back = (float)Math.max(back, at - Math.asin(BACK / r));
            }
        }
        front = Math.max(-UP_MOST, front);
        back = Math.min(UP_MOST, back);
        // Below it, folded on the leg's middle: just clear of whatever of a leg is there (a shin, a boot), hanging straight otherwise.
        float frontLow = 0.0F;
        float backLow = 0.0F;
        double fy = KNEE * Math.cos(front);
        double fz = KNEE * Math.sin(front);
        double by = KNEE * Math.cos(back);
        double bz = KNEE * Math.sin(back);
        double reachLow = Math.sqrt(BELOW * BELOW + FRONT * FRONT) + 0.3;
        for (float[] q : lower) {
            double dy = q[0] - fy;
            double dz = q[1] - fz;
            double r = Math.sqrt(dy * dy + dz * dz);
            if (r > FRONT && r <= reachLow && (dy > 0.0 || dy * Math.cos(front) + dz * Math.sin(front) > 0.0)) {
                frontLow = (float)Math.min(frontLow, Math.atan2(dz, dy) + Math.asin(FRONT / r));
            }
            dy = q[0] - by;
            dz = q[1] - bz;
            r = Math.sqrt(dy * dy + dz * dz);
            if (r > BACK && r <= reachLow && (dy > 0.0 || dy * Math.cos(back) + dz * Math.sin(back) > 0.0)) {
                backLow = (float)Math.max(backLow, Math.atan2(dz, dy) - Math.asin(BACK / r));
            }
        }
        return new CoatDrape(front, back, Math.max(-LOW_MOST, frontLow), Math.min(LOW_MOST, backLow));
    }

    /**
     * A leg's outfit on its front ({@code side} -1) or its back (1), {@code grow} out from it, as lines of (y, z) from the hip, as the layer
     * draws it: the trouser's upper half to its cut at the knee (the cut's corners half bent), then below the knee (wholly bent) the
     * trouser's lower half, the boot's shaft and its toe (in front) or heel (behind). The first line is the thigh's.
     */
    public static float[][][] outline(float thigh, float knee, int side, float grow) {
        double ky = KNEE * Math.cos(thigh);
        double kz = KNEE * Math.sin(thigh);
        double shin = thigh + knee;
        float[] hip = at(0.0, 0.0, thigh, 0.0, side * (2.09 + grow));
        float[] cut = at(ky, kz, thigh + knee * 0.5, 0.0, side * (2.09 + grow));
        float[][][] lines = new float[side < 0 ? 5 : 4][][];
        lines[0] = new float[][] {hip, cut};
        lines[1] = new float[][] {cut, at(ky, kz, shin, 2.1, side * (2.09 + grow))};
        lines[2] = new float[][] {at(ky, kz, shin, 1.6, side * (2.25 + grow)), at(ky, kz, shin, 4.9, side * (2.25 + grow))};
        if (side < 0) {
            lines[3] = new float[][] {at(ky, kz, shin, 4.0, -(2.66 + grow)), at(ky, kz, shin, 5.55, -(2.66 + grow))};
            lines[4] = new float[][] {at(ky, kz, shin, 5.45, -(2.72 + grow)), at(ky, kz, shin, 6.0, -(2.72 + grow))};
        } else {
            lines[3] = new float[][] {at(ky, kz, shin, 5.45, 2.36 + grow), at(ky, kz, shin, 6.0, 2.36 + grow)};
        }
        return lines;
    }

    /** ({@code y}, {@code z}) turned {@code turn} about x and set at ({@code oy}, {@code oz}). */
    private static float[] at(double oy, double oz, double turn, double y, double z) {
        return new float[] {(float)(oy + y * Math.cos(turn) - z * Math.sin(turn)), (float)(oz + y * Math.sin(turn) + z * Math.cos(turn))};
    }

    /** Points along the line {@code line}, every {@link #STEP} or so, its ends too, into {@code out}. */
    private static void points(float[][] line, List<float[]> out) {
        float[] a = line[0];
        float[] b = line[1];
        double length = Math.sqrt((b[0] - a[0]) * (b[0] - a[0]) + (b[1] - a[1]) * (b[1] - a[1]));
        int n = Math.max(1, (int)(length / STEP));
        for (int i = 0; i <= n; i++) {
            float t = (float)i / n;
            out.add(new float[] {a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t});
        }
    }

    /** How far a corner {@code z} pixels from the leg's middle (front to back) goes with the back's turn rather than the front's. */
    public static float backward(float z) {
        return Math.max(0.0F, Math.min(1.0F, (z + 2.0F) / 4.0F));
    }

    /** The turn about x a corner {@code y} below the hip takes, {@code t} of the way back: the upper's, the lower's, half each on the fold. */
    public float turn(float y, float t) {
        float upper = this.front + (this.back - this.front) * t;
        float lower = this.frontLow + (this.backLow - this.frontLow) * t;
        if (y <= KNEE - FOLD) {
            return upper;
        }
        if (y >= KNEE + FOLD) {
            return lower;
        }
        return upper + (lower - upper) * (y - (KNEE - FOLD)) / (2.0F * FOLD);
    }

    /** ({@code v}: x, y, z, pixels from the hip) where the drape hangs it, in place: down its line from the hip (folded at the knee), then out from it. */
    public void place(float[] v) {
        float t = backward(v[2]);
        float upper = this.front + (this.back - this.front) * t;
        float y = v[1];
        float z = v[2];
        double cy;
        double cz;
        if (y <= KNEE) {
            cy = y * Math.cos(upper);
            cz = y * Math.sin(upper);
        } else {
            float lower = this.frontLow + (this.backLow - this.frontLow) * t;
            cy = KNEE * Math.cos(upper) + (y - KNEE) * Math.cos(lower);
            cz = KNEE * Math.sin(upper) + (y - KNEE) * Math.sin(lower);
        }
        double out = turn(y, t);
        v[1] = (float)(cy - z * Math.sin(out));
        v[2] = (float)(cz + z * Math.cos(out));
    }
}
