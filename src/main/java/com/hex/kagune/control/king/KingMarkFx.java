package com.hex.kagune.control.king;

import com.hex.kagune.control.ayato.UkakuModel;

/**
 * 1.0.19: the marks a final kakuja's blows leave in the world (pure): what every hit, crush, cut and quake of War and Peace shows instead
 * of the game's own dust. Drawn as shapes in the world, the same on every client from where and which way the server says it was:
 * sparks thrown along the blow on real arcs, a flash where it met, a ring of shock running out, dust rolling over the ground. (Each
 * client adds its own motes on top: real drops of blood that fall and stain, smoke, ash; see {@code KingMarks}.)
 *
 * <p>World coordinates, blocks; {@code age} ticks since it was struck.
 */
public final class KingMarkFx {
    /** A body cut or struck: blood thrown along the blow, a dark red flash, a few red sparks. */
    public static final int BLOOD = 0;
    /** A body crushed, cut in half, killed: a great deal of blood both ways, a flash, red sparks, the view kicked. */
    public static final int GORE = 1;
    /** Black smoke and ash: War's darkness where it passes. */
    public static final int SOOT = 2;
    /** The ground struck: a ring of dust rolling out over it, earth thrown up, a crack of red at its middle, the view kicked. */
    public static final int QUAKE = 3;
    /** Crimson kagune meeting something: a hot flash, a ring of shock, crimson sparks. */
    public static final int FLARE = 4;
    /** Peace's blades meeting something: a white glint, a pale ring, white sparks. */
    public static final int GLINT = 5;
    /** A body moving through the air at speed: soot streaming behind it, embers. */
    public static final int TRAIL = 6;
    /**
     * 1.0.40: two kagune meeting head on ({@code KaguneClash}): a white-hot flash where they crossed, the wind of it blown out in two
     * rings square to the line they met along, streaks of air whipping out all round, sparks thrown back both ways.
     */
    public static final int CLASH = 7;

    /** How long any mark has shapes to draw (ticks). */
    public static final int LASTS = 16;

    private static final double[] UP = {0.0, 1.0, 0.0};
    private static final double[] FLAT = normalize(new double[] {0.0, 1.0, 0.002});

    private KingMarkFx() {
    }

    /**
     * Mark {@code style} struck at {@code at} along {@code way} (unit, or zero), {@code power} strong (1: an ordinary blow), drawn
     * {@code age} ticks on, seen from {@code eye}.
     */
    public static void draw(UkakuModel.Sink sink, int style, double[] at, double[] way, double power, double age, long seed, double[] eye) {
        if (age < 0.0 || age > LASTS) {
            return;
        }
        double p = Math.max(0.3, Math.min(3.0, power));
        switch (style) {
            case BLOOD -> {
                flash(sink, at, eye, 0.22 * p, age, 3.0, 0xFFB01020);
                sparks(sink, at, way, 0.7, 6 + (int)(4 * p), 0.28 * p, 0.04, age, 7.0, seed, eye, 0xFFFF4050, 0xFF6A0410, 0.035);
            }
            case GORE -> {
                flash(sink, at, eye, 0.4 * p, age, 4.0, 0xFFC01020);
                sparks(sink, at, way, 0.45, 14 + (int)(6 * p), 0.35 * p, 0.05, age, 9.0, seed, eye, 0xFFFF3040, 0xFF5A0410, 0.05);
                ring(sink, at, way, 0.3 + 0.9 * p * out(age / 6.0), 0.12 * p, 1.0 - age / 6.0, 0xFFB0101E);
            }
            case QUAKE -> {
                double r = (0.8 + 2.4 * p) * out(age / 10.0);
                double fade = 1.0 - age / LASTS;
                dust(sink, at, r, 0.55 + 0.5 * p, fade, 0xFF5A524A);
                if (age < 7.0) {
                    KingComingFx.ring(sink, at[0], at[1] + 0.03, at[2], Math.max(0.0, r * 0.35 - 0.3), r * 0.35, KingComingFx.glowRed(0.0),
                        KingComingFx.glowRed(0.7 * (1.0 - age / 7.0)), UP);
                }
                flash(sink, add(at, new double[] {0.0, 0.25, 0.0}), eye, 0.35 * p, age, 3.0, 0xFFFF5A3A);
                sparks(sink, add(at, new double[] {0.0, 0.1, 0.0}), UP, 0.9, 10 + (int)(6 * p), 0.3 * p, 0.05, age, 10.0, seed, eye, 0xFFFFB070, 0xFF702008,
                    0.04);
            }
            case FLARE -> {
                flash(sink, at, eye, 0.35 * p, age, 4.0, 0xFFFF2A3A);
                ring(sink, at, way, 0.25 + 1.0 * p * out(age / 7.0), 0.1 * p, 1.0 - age / 7.0, 0xFFE8121C);
                sparks(sink, at, way, 0.6, 10 + (int)(6 * p), 0.4 * p, 0.03, age, 8.0, seed, eye, 0xFFFFC0A0, 0xFFB0101E, 0.04);
            }
            case CLASH -> {
                flash(sink, at, eye, 0.6 * p, age, 4.0, 0xFFFFF4EC);
                star(sink, at, eye, 1.1 * p * (1.0 - out(age / 4.0)) + 0.05, age, 4.0);
                ring(sink, at, way, 0.3 + 2.6 * p * out(age / 9.0), 0.22 * p, 1.0 - age / 9.0, 0xFFDDE6F0);
                ring(sink, at, way, 0.2 + 1.5 * p * out(age / 6.0), 0.12 * p, 0.8 * (1.0 - age / 6.0), 0xFFFFFFFF);
                gusts(sink, at, way, 2.8 * p, 14, age, 8.0, seed, eye);
                sparks(sink, at, way, 0.55, 8 + (int)(4 * p), 0.45 * p, 0.03, age, 7.0, seed, eye, 0xFFFFFFFF, 0xFFB0101E, 0.035);
                sparks(sink, at, mul(way, -1.0), 0.55, 8 + (int)(4 * p), 0.45 * p, 0.03, age, 7.0, seed ^ 0x5DEECE66DL, eye, 0xFFFFFFFF, 0xFFB0101E, 0.035);
            }
            case GLINT -> {
                star(sink, at, eye, 0.7 * p * (1.0 - out(age / 5.0)) + 0.05, age, 5.0);
                ring(sink, at, way, 0.2 + 0.9 * p * out(age / 6.0), 0.06 * p, 0.7 * (1.0 - age / 6.0), 0xFFFFE6EE);
                sparks(sink, at, way, 0.55, 8 + (int)(4 * p), 0.38 * p, 0.02, age, 7.0, seed, eye, 0xFFFFFFFF, 0xFFFF90A8, 0.03);
            }
            default -> {
            }
        }
    }

    /** One of War's RC spikes in flight: a burning crimson streak from {@code tail} to {@code head}, white-hot at its head. */
    public static void bolt(UkakuModel.Sink sink, double[] tail, double[] head, double[] eye) {
        double[] mid = add(tail, mul(sub(head, tail), 0.7));
        streak(sink, tail, mid, 0.12, eye, alpha(0xFFE8121C, 0.8), NONE);
        streak(sink, mid, head, 0.16, eye, 0xFFFFE8E0, alpha(0xFFE8121C, 0.8));
        flash(sink, head, eye, 0.22, 0.0, 1.0, 0xFFFF6A60);
    }

    /** True if {@code style} has shapes to draw (the others are all motes). */
    public static boolean drawn(int style) {
        return style != SOOT && style != TRAIL;
    }

    /**
     * {@code count} sparks thrown from {@code at} mostly along {@code way} ({@code bias} of the way theirs; the rest any way), up to
     * {@code speed} blocks a tick, falling by {@code fall}, slowing; each a short streak of its path, hot to cool over its {@code life}.
     */
    static void sparks(UkakuModel.Sink sink, double[] at, double[] way, double bias, int count, double speed, double fall, double age, double life,
                       long seed, double[] eye, int hot, int cool, double width) {
        if (age > life) {
            return;
        }
        boolean aimed = way[0] * way[0] + way[1] * way[1] + way[2] * way[2] > 1.0E-6;
        for (int i = 0; i < count; i++) {
            double h1 = hash(seed, i * 5);
            double h2 = hash(seed, i * 5 + 1);
            double h3 = hash(seed, i * 5 + 2);
            double h4 = hash(seed, i * 5 + 3);
            double theta = h1 * Math.PI * 2.0;
            double phi = Math.acos(2.0 * h2 - 1.0);
            double[] any = {Math.sin(phi) * Math.cos(theta), Math.abs(Math.cos(phi)) * 0.7 + 0.15, Math.sin(phi) * Math.sin(theta)};
            double[] d = aimed ? normalize(add(mul(way, bias * 1.6), mul(any, 1.0 - bias * 0.6))) : normalize(any);
            double s = speed * (0.45 + 0.55 * h3);
            double own = life * (0.6 + 0.4 * h4);
            if (age > own) {
                continue;
            }
            double[] head = path(at, d, s, fall, age);
            double[] tail = path(at, d, s, fall, Math.max(0.0, age - 1.2));
            double k = 1.0 - age / own;
            streak(sink, tail, head, width * (0.6 + 0.4 * k), eye, alpha(mix(cool, hot, k), 0.4 + 0.6 * k), NONE);
        }
    }

    /**
     * 1.0.40: the wind of a clash: {@code count} pale streaks of air whipping out from {@code at} across the plane square to {@code way},
     * as far as {@code radius}, each a little off the plane and curling as it goes, gone in {@code life} ticks.
     */
    static void gusts(UkakuModel.Sink sink, double[] at, double[] way, double radius, int count, double age, double life, long seed, double[] eye) {
        if (age > life) {
            return;
        }
        boolean aimed = way[0] * way[0] + way[1] * way[1] + way[2] * way[2] > 1.0E-6;
        double[] n = aimed ? normalize(way) : UP;
        double[] u = normalize(Math.abs(n[1]) > 0.9 ? cross(n, new double[] {1.0, 0.0, 0.0}) : cross(n, UP));
        double[] w = normalize(cross(n, u));
        double k = 1.0 - age / life;
        for (int i = 0; i < count; i++) {
            double angle = Math.PI * 2.0 * (i + 0.35 * hash(seed, i * 3)) / count;
            double off = (hash(seed, i * 3 + 1) - 0.5) * 0.5;
            double reach = radius * (0.7 + 0.3 * hash(seed, i * 3 + 2));
            double curl = 0.35 * (hash(seed, i * 3 + 1) - 0.5);
            double[] dir = normalize(add(add(mul(u, Math.cos(angle)), mul(w, Math.sin(angle))), mul(n, off)));
            double[] side = normalize(cross(n, dir));
            double head = reach * out(age / life);
            double tail = Math.max(0.0, head - 0.9 * k - 0.25);
            double[] a = add(add(at, mul(dir, tail)), mul(side, curl * tail * 0.4));
            double[] b = add(add(at, mul(dir, head)), mul(side, curl * head * 0.4));
            streak(sink, a, b, 0.07 * (0.5 + k), eye, alpha(0xFFE8F0FF, 0.15 + 0.45 * k), alpha(0xFFFFFFFF, 0.7 * k));
        }
    }

    /** Where a spark set out from {@code at} along {@code d} at {@code s} is {@code t} ticks on: slowing as it goes, falling. */
    static double[] path(double[] at, double[] d, double s, double fall, double t) {
        double tau = 5.0;
        double run = s * tau * (1.0 - Math.exp(-t / tau));
        return new double[] {at[0] + d[0] * run, at[1] + d[1] * run - 0.5 * fall * t * t, at[2] + d[2] * run};
    }

    /** A thin burning streak from {@code a} to {@code b}, turned to face {@code eye}. */
    static void streak(UkakuModel.Sink sink, double[] a, double[] b, double width, double[] eye, int ca, int cb) {
        double[] along = sub(b, a);
        if (dot(along, along) < 1.0E-8) {
            return;
        }
        double[] toEye = sub(eye, a);
        double[] side = cross(along, toEye);
        if (dot(side, side) < 1.0E-10) {
            return;
        }
        side = mul(normalize(side), width * 0.5);
        sink.quad(add(a, side), add(b, side), sub(b, side), sub(a, side), UP, cb, ca, ca, cb);
    }

    /** A burning flash at {@code at}, {@code radius} across, gone in {@code life} ticks. */
    static void flash(UkakuModel.Sink sink, double[] at, double[] eye, double radius, double age, double life, int colour) {
        if (age > life) {
            return;
        }
        double k = 1.0 - age / life;
        double[] toEye = normalize(sub(eye, at));
        double[] u = normalize(Math.abs(toEye[1]) > 0.95 ? cross(toEye, new double[] {1.0, 0.0, 0.0}) : cross(toEye, UP));
        double[] w = cross(toEye, u);
        int steps = 16;
        int hot = alpha(colour, 0.75 * k * k);
        int none = NONE;
        double r = radius * (0.6 + 0.6 * (1.0 - k));
        for (int i = 0; i < steps; i++) {
            double a0 = Math.PI * 2.0 * i / steps;
            double a1 = Math.PI * 2.0 * (i + 1) / steps;
            double[] e0 = add(at, add(mul(u, Math.cos(a0) * r), mul(w, Math.sin(a0) * r)));
            double[] e1 = add(at, add(mul(u, Math.cos(a1) * r), mul(w, Math.sin(a1) * r)));
            sink.quad(at, e0, e1, at, UP, hot, none, none, hot);
        }
    }

    /** A four-pointed glint at {@code at}, turned to face {@code eye}, {@code size} across. */
    static void star(UkakuModel.Sink sink, double[] at, double[] eye, double size, double age, double life) {
        if (age > life) {
            return;
        }
        double k = 1.0 - age / life;
        double[] toEye = normalize(sub(eye, at));
        double[] u = normalize(Math.abs(toEye[1]) > 0.95 ? cross(toEye, new double[] {1.0, 0.0, 0.0}) : cross(toEye, UP));
        double[] w = cross(toEye, u);
        int hot = alpha(0xFFFFFFFF, k);
        int none = NONE;
        for (double[] d : new double[][] {u, w, mul(u, -1.0), mul(w, -1.0)}) {
            double[] side = cross(d, toEye);
            sink.quad(add(at, mul(side, size * 0.07)), add(at, mul(d, size)), add(at, mul(d, size)), sub(at, mul(side, size * 0.07)), UP, hot, none, none,
                hot);
        }
        flash(sink, at, eye, size * 0.35, age, life, 0xFFFFD8E4);
    }

    /** A burning ring of shock round {@code at}, square to {@code way} (flat on the ground when there is none). */
    static void ring(UkakuModel.Sink sink, double[] at, double[] way, double radius, double width, double alpha, int colour) {
        if (alpha <= 0.01) {
            return;
        }
        boolean aimed = way[0] * way[0] + way[1] * way[1] + way[2] * way[2] > 1.0E-6;
        double[] n = aimed ? normalize(way) : UP;
        double[] u = normalize(Math.abs(n[1]) > 0.9 ? cross(n, new double[] {1.0, 0.0, 0.0}) : cross(n, UP));
        double[] w = normalize(cross(n, u));
        int hot = alpha(colour, 0.7 * alpha);
        int cool = NONE;
        double inner = Math.max(0.0, radius - width);
        int steps = 24;
        for (int i = 0; i < steps; i++) {
            double a0 = Math.PI * 2.0 * i / steps;
            double a1 = Math.PI * 2.0 * (i + 1) / steps;
            double[] d0 = add(mul(u, Math.cos(a0)), mul(w, Math.sin(a0)));
            double[] d1 = add(mul(u, Math.cos(a1)), mul(w, Math.sin(a1)));
            sink.quad(add(at, mul(d0, inner)), add(at, mul(d0, radius)), add(at, mul(d1, radius)), add(at, mul(d1, inner)), UP, cool, hot, hot, cool);
        }
    }

    /** Dust rolling out over the ground round {@code at}: a soft ring of it {@code radius} out, {@code width} wide, as thick as {@code fade}. */
    static void dust(UkakuModel.Sink sink, double[] at, double radius, double width, double fade, int colour) {
        if (fade <= 0.01 || radius <= 0.05) {
            return;
        }
        int steps = 32;
        double inner = Math.max(0.0, radius - width);
        int solid = (int)Math.round(170 * fade) << 24 | colour & 0xFFFFFF;
        int clear = colour & 0xFFFFFF;
        for (int i = 0; i < steps; i++) {
            double a0 = Math.PI * 2.0 * i / steps;
            double a1 = Math.PI * 2.0 * (i + 1) / steps;
            double lift = 0.05;
            double[] i0 = {at[0] + Math.cos(a0) * inner, at[1] + lift, at[2] + Math.sin(a0) * inner};
            double[] i1 = {at[0] + Math.cos(a1) * inner, at[1] + lift, at[2] + Math.sin(a1) * inner};
            double[] o0 = {at[0] + Math.cos(a0) * radius, at[1] + lift + 0.25 * width, at[2] + Math.sin(a0) * radius};
            double[] o1 = {at[0] + Math.cos(a1) * radius, at[1] + lift + 0.25 * width, at[2] + Math.sin(a1) * radius};
            sink.quad(i0, o0, o1, i1, FLAT, clear, solid, solid, clear);
        }
    }

    // ================================================================== tools

    static double out(double t) {
        t = Math.max(0.0, Math.min(1.0, t));
        double k = 1.0 - t;
        return 1.0 - k * k * k;
    }

    /** A colour that burns, as bright as {@code a} (it is added onto what is behind it: its light is in its colour, black is none). */
    static int alpha(int argb, double a) {
        a = Math.max(0.0, Math.min(1.0, a));
        return 0xFF000000 | (int)Math.round(((argb >> 16) & 0xFF) * a) << 16 | (int)Math.round(((argb >> 8) & 0xFF) * a) << 8
            | (int)Math.round((argb & 0xFF) * a);
    }

    /** No light at all. */
    static final int NONE = 0xFF000000;

    static int mix(int a, int b, double t) {
        return KingComingFx.mixColour(a, b, t);
    }

    /** 0..1 from {@code seed} and {@code n}. */
    static double hash(long seed, int n) {
        long x = seed * 0x9E3779B97F4A7C15L + n * 0xC2B2AE3D27D4EB4FL;
        x ^= x >>> 31;
        x *= 0xBF58476D1CE4E5B9L;
        x ^= x >>> 29;
        return (x >>> 11) * 0x1.0p-53;
    }

    static double[] add(double[] a, double[] b) {
        return new double[] {a[0] + b[0], a[1] + b[1], a[2] + b[2]};
    }

    static double[] sub(double[] a, double[] b) {
        return new double[] {a[0] - b[0], a[1] - b[1], a[2] - b[2]};
    }

    static double[] mul(double[] a, double k) {
        return new double[] {a[0] * k, a[1] * k, a[2] * k};
    }

    static double dot(double[] a, double[] b) {
        return a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
    }

    static double[] cross(double[] a, double[] b) {
        return new double[] {a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]};
    }

    static double[] normalize(double[] a) {
        double l = Math.sqrt(dot(a, a));
        return l < 1.0E-9 ? new double[] {0.0, 1.0, 0.0} : mul(a, 1.0 / l);
    }
}
