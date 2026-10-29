package com.hex.kagune.control.eto;

import java.util.List;

/**
 * The steam off Eto's base form (6.1.0; pure, tested): where on her body a wisp leaves, how often, how it grows, rises, curls
 * and fades, and how its three crossed planes are turned. Restrained by design: a handful of faint wisps at a time, airy, never
 * a cloud, never colour, never a trail.
 *
 * <p>Rig space: x to her right, y up from her feet, z forward, in blocks.
 */
public final class EtoSteamRules {
    /** Pale whitish grey. */
    public static final int COLOUR = 0xE6EAEC;
    /** How opaque a wisp is at its thickest (all three planes together). */
    public static final float PEAK_ALPHA = 0.2F;
    /** Wisps a tick at full strength (before the client's particle setting). */
    public static final float RATE = 0.42F;
    /** Steam is only made for Etos within this many blocks of the camera. */
    public static final double RANGE = 40.0;
    /** In first person nothing starts closer than this to her own eyes. */
    public static final double FIRST_PERSON_CLEARANCE = 0.55;
    /** How much of her movement a wisp keeps as it leaves her (then the air slows it: it drifts, it doesn't trail). */
    public static final double INHERIT = 0.8;
    public static final double OUTWARD = 0.004;
    public static final double RISE_MIN = 0.010;
    public static final double RISE_MAX = 0.020;
    public static final int LIFE_MIN = 34;
    public static final int LIFE_MAX = 56;
    /** Radians a tick each plane axis turns at most (the slow tumble). */
    public static final float TUMBLE = 0.06F;
    public static final float CURL_RATE = 0.22F;
    public static final float CURL = 0.0011F;
    public static final float DRAG = 0.955F;
    public static final float BUOYANCY = 0.0006F;
    public static final float WANDER = 0.0012F;
    /** The hips, where the upper body leans from (blocks above her feet). */
    public static final double HIP = 0.72;

    /** A place on her body a wisp can leave from: where (rig space, standing straight), how often, and whether it is upper body. */
    public record Point(double x, double y, double z, double weight, boolean upper, boolean firstPerson) {
    }

    /** Shoulders and upper back most, arms and chest less, the legs now and then. */
    public static final List<Point> POINTS = List.of(
        new Point(0.27, 1.40, -0.02, 3.0, true, false), new Point(-0.27, 1.40, -0.02, 3.0, true, false),
        new Point(0.12, 1.30, -0.16, 2.5, true, true), new Point(-0.12, 1.30, -0.16, 2.5, true, true),
        new Point(0.0, 1.08, -0.16, 1.5, true, true),
        new Point(0.37, 1.18, 0.0, 2.0, true, false), new Point(-0.37, 1.18, 0.0, 2.0, true, false),
        new Point(0.37, 0.88, 0.03, 1.5, true, true), new Point(-0.37, 0.88, 0.03, 1.5, true, true),
        new Point(0.10, 1.22, 0.14, 1.5, true, false), new Point(-0.10, 1.22, 0.14, 1.5, true, false),
        new Point(0.12, 0.48, 0.0, 0.6, false, true), new Point(-0.12, 0.48, 0.0, 0.6, false, true));

    private EtoSteamRules() {
    }

    /** The point for a uniform {@code r} (0..1), weighted; in first person only those away from the face. */
    public static Point pick(double r, boolean firstPerson) {
        double total = 0.0;
        for (Point point : POINTS) {
            total += !firstPerson || point.firstPerson() ? point.weight() : 0.0;
        }
        double at = Math.max(0.0, Math.min(0.999999, r)) * total;
        Point last = POINTS.get(0);
        for (Point point : POINTS) {
            if (firstPerson && !point.firstPerson()) {
                continue;
            }
            last = point;
            at -= point.weight();
            if (at < 0.0) {
                return point;
            }
        }
        return last;
    }

    /**
     * Where {@code point} is this frame (rig space), with the upper body leaned {@code lean} forward and {@code side} sideways
     * about the hips and the chest turned {@code twist} (radians, the body animation's), the body {@code scaleY} tall (a crouch),
     * nudged by ({@code jx, jy, jz}).
     */
    public static double[] place(Point point, double lean, double side, double twist, double scaleY, double jx, double jy, double jz) {
        double x = point.x() + jx;
        double y = point.y() * scaleY + jy;
        double z = point.z() + jz;
        if (point.upper() && y > HIP) {
            double cos = Math.cos(twist);
            double sin = Math.sin(twist);
            double tx = x * cos + z * sin;
            double tz = -x * sin + z * cos;
            double up = y - HIP;
            // Lean forward: the chest goes forward and down about the hips; sideways the same toward her side.
            double forward = up * Math.sin(lean);
            double sideways = up * Math.sin(side);
            double height = up * Math.cos(lean) * Math.cos(side);
            x = tx + sideways;
            z = tz + forward;
            y = HIP + height;
        }
        return new double[] {x, y, z};
    }

    /** Wisps owed this tick: fewer on the move (no trail), far fewer in first person. */
    public static float rate(float strength, double speed, boolean firstPerson) {
        float moving = speed > 0.2 ? 0.55F : speed > 0.08 ? 0.8F : 1.0F;
        return RATE * Math.max(0.0F, Math.min(1.0F, strength)) * moving * (firstPerson ? 0.35F : 1.0F);
    }

    /** The emission's strength next tick: it wakes over a second and dies away a little quicker. */
    public static float ease(float strength, boolean on) {
        return on ? Math.min(1.0F, strength + 0.05F) : Math.max(0.0F, strength - 0.0625F);
    }

    /** Half the width of a wisp's planes at {@code t} (0..1 of its life): it spreads as it rises. */
    public static float size(float t) {
        float u = 1.0F - Math.max(0.0F, Math.min(1.0F, t));
        return 0.07F + 0.31F * (1.0F - u * u);
    }

    /** How much of its peak a wisp shows at {@code t} of its life ({@code age} ticks old): it opens, then thins away. */
    public static float opacity(float t, int age) {
        float open = Math.min(1.0F, Math.max(0.0F, age / 6.0F));
        open = open * open * (3.0F - 2.0F * open);
        float fade = (float)Math.pow(1.0F - Math.max(0.0F, Math.min(1.0F, t)), 1.4);
        return open * fade;
    }

    /**
     * Three orthonormal axes (flattened: axis i is {@code [3i, 3i+1, 3i+2]}) turned by the angles: Rz(c) Ry(b) Rx(a). Each is
     * the normal of one of a wisp's planes.
     */
    public static float[] axes(float a, float b, float c) {
        double ca = Math.cos(a);
        double sa = Math.sin(a);
        double cb = Math.cos(b);
        double sb = Math.sin(b);
        double cc = Math.cos(c);
        double sc = Math.sin(c);
        // Columns of R = Rz Ry Rx.
        return new float[] {
            (float)(cc * cb), (float)(sc * cb), (float)(-sb),
            (float)(cc * sb * sa - sc * ca), (float)(sc * sb * sa + cc * ca), (float)(cb * sa),
            (float)(cc * sb * ca + sc * sa), (float)(sc * sb * ca - cc * sa), (float)(cb * ca)};
    }
}
