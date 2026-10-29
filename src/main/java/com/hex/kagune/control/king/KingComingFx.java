package com.hex.kagune.control.king;

import com.hex.kagune.control.ukaku.UkakuModel;
import java.util.Random;

/**
 * 1.0.17: what a final kakuja's coming ({@link KingTransform}) leaves in the world, as shapes (pure; every client draws the same from
 * the tick it was put on and who it was put on, nothing sent): War's darkness pouring into him, the dark dome of its burst, the
 * black shock along the ground with blood red inside it, the blood thrown out in arcs and lying where it fell; Peace's soft ring of
 * light at his feet, the halo, the white light at its end and the feathers that drift down after.
 *
 * <p>A quad whose normal is exactly up burns (drawn additively, at full light); every other is lit, translucent by its alpha.
 */
public final class KingComingFx {
    private static final double[] UP = {0.0, 1.0, 0.0};
    /** Up, for a lit quad lying on the ground (exactly up would burn). */
    private static final double[] FLAT = normalize(new double[] {0.0, 1.0, 0.002});
    /** How long after it was put on there is still something to draw (ticks): War's blood lies a while. */
    public static final int WAR_LASTS = 130;
    public static final int PEACE_LASTS = 120;
    /** Blood: how many drops, how hard they fall (blocks a tick, squared). */
    static final int DROPS = 72;
    static final double FALL = 0.055;
    static final int FEATHERS = 28;

    private KingComingFx() {
    }

    /** How long {@code form}'s coming leaves something to draw (ticks). */
    public static int lasts(KingForm form) {
        return form == KingForm.WAR ? WAR_LASTS : form == KingForm.PEACE ? PEACE_LASTS : 0;
    }

    // ================================================================== War

    /**
     * War's coming, {@code age} ticks after it was put on, round {@code feet} (world blocks; the ground there), {@code seed} its own
     * (the same for every client), {@code eye} where it is seen from.
     */
    public static void war(UkakuModel.Sink sink, double[] feet, double age, long seed, double[] eye, double time) {
        double burst = KingTransform.WAR_BURST;
        double[] chest = {feet[0], feet[1] + 1.2, feet[2]};
        if (age < burst + 2.0) {
            gathering(sink, feet, chest, age, seed, eye, time);
        }
        double t = age - burst;
        if (t < 0.0) {
            return;
        }
        // The dark dome it bursts out in, gone in a moment.
        if (t < 9.0) {
            double r = 0.6 + 2.8 * out(t / 4.0);
            double a = 0.62 * (1.0 - smooth(t / 9.0));
            dome(sink, chest, r, alpha(0xFF050304, a));
        }
        // The black shock along the ground, blood red inside it.
        if (t < 12.0) {
            double r = 0.5 + 8.5 * out(t / 9.0);
            double a = 1.0 - smooth(t / 12.0);
            ring(sink, feet[0], feet[1] + 0.04, feet[2], Math.max(0.0, r - 1.5 + t * 0.08), r, alpha(0xFF040203, 0.85 * a), alpha(0xFF040203, 0.0), FLAT);
            ring(sink, feet[0], feet[1] + 0.05, feet[2], Math.max(0.0, r - 2.4), Math.max(0.0, r - 1.2), glowRed(0.0), glowRed(0.7 * a), UP);
        }
        blood(sink, feet, chest, t, seed, eye);
    }

    /** The darkness pouring into him before the burst: black wisps winding in from all round to his chest, the ground going dark under him. */
    static void gathering(UkakuModel.Sink sink, double[] feet, double[] chest, double age, long seed, double[] eye, double time) {
        double u = Math.min(1.0, age / KingTransform.WAR_BURST);
        double fade = Math.min(1.0, age / 3.0) * (age > KingTransform.WAR_BURST ? Math.max(0.0, 1.0 - (age - KingTransform.WAR_BURST) / 2.0) : 1.0);
        Random random = new Random(seed * 31L + 7L);
        int wisps = 8;
        for (int k = 0; k < wisps; k++) {
            double start = random.nextDouble() * Math.PI * 2.0;
            double height = 0.1 + random.nextDouble() * 2.2;
            double far = (4.5 + random.nextDouble() * 2.0) * (1.0 - 0.8 * u);
            double turn = (random.nextBoolean() ? 1.0 : -1.0) * (1.6 + random.nextDouble());
            int n = 18;
            double[][] path = new double[n + 1][];
            for (int i = 0; i <= n; i++) {
                double s = (double)i / n;
                double r = far + (0.35 - far) * s * s;
                double a = start + turn * s + age * 0.18 * Math.signum(turn);
                double y = height + (chest[1] - feet[1] - height) * s;
                path[i] = new double[] {chest[0] + Math.cos(a) * r, feet[1] + y, chest[2] + Math.sin(a) * r};
            }
            // Thin streams of it, nothing at their far end, thickest where they pour into him.
            ribbon(sink, path, 0.012, 0.07, eye, alpha(0xFF030203, 0.0), alpha(0xFF120407, 0.8 * fade));
        }
        disc(sink, feet[0], feet[1] + 0.03, feet[2], 0.6 + 2.2 * smooth(u), alpha(0xFF020102, 0.55 * fade));
    }

    /** The blood: thrown out of him in arcs at the burst, each drop lying where it fell a while, then soaking away. */
    static void blood(UkakuModel.Sink sink, double[] feet, double[] chest, double t, long seed, double[] eye) {
        Random random = new Random(seed * 17L + 3L);
        double ground = feet[1] + 0.02;
        for (int i = 0; i < DROPS; i++) {
            double azimuth = random.nextDouble() * Math.PI * 2.0;
            double rise = Math.toRadians(8.0 + random.nextDouble() * 62.0);
            double speed = 0.22 + random.nextDouble() * 0.55;
            double[] v = {Math.cos(azimuth) * Math.cos(rise) * speed, Math.sin(rise) * speed, Math.sin(azimuth) * Math.cos(rise) * speed};
            double[] from = {chest[0] + (random.nextDouble() - 0.5) * 0.5, chest[1] + (random.nextDouble() - 0.5) * 0.6, chest[2] + (random.nextDouble() - 0.5) * 0.5};
            double size = 0.6 + random.nextDouble() * 0.8;
            int colour = random.nextDouble() < 0.5 ? 0xFF6A0610 : 0xFF9A0E1A;
            double h = from[1] - ground;
            double land = (v[1] + Math.sqrt(v[1] * v[1] + 2.0 * FALL * h)) / FALL;
            if (t < land) {
                double[] p = {from[0] + v[0] * t, from[1] + v[1] * t - 0.5 * FALL * t * t, from[2] + v[2] * t};
                double[] vel = {v[0], v[1] - FALL * t, v[2]};
                double[] tail = {p[0] - vel[0] * 0.45, p[1] - vel[1] * 0.45, p[2] - vel[2] * 0.45};
                ribbon(sink, new double[][] {tail, p}, 0.035 * size, 0.06 * size, eye, alpha(colour, 0.7), alpha(colour, 0.95));
            } else {
                double soak = 1.0 - smooth((t - 60.0) / 50.0);
                if (soak <= 0.01) {
                    continue;
                }
                double[] at = {from[0] + v[0] * land, ground, from[2] + v[2] * land};
                double spread = smooth((t - land) / 3.0);
                splat(sink, at, (0.1 + 0.22 * size * speed / 0.5) * (0.5 + 0.5 * spread), i, alpha(darken(colour, 0.75), 0.88 * soak));
            }
        }
    }

    // ================================================================== Peace

    /** Peace's coming, {@code age} ticks after it was put on, round {@code feet}. */
    public static void peace(UkakuModel.Sink sink, double[] feet, double age, long seed, double[] eye, double time) {
        // The soft ring of light at his feet, while the wings open and the armour grows.
        double ringIn = smooth((age - 4.0) / 8.0) * (1.0 - smooth((age - KingTransform.PEACE_LIGHT) / 6.0));
        if (ringIn > 0.01) {
            double r = 1.1 + 0.08 * Math.sin(time * 0.2);
            ring(sink, feet[0], feet[1] + 0.03, feet[2], r - 0.22, r, glowWhite(0.0), glowWhite(0.55 * ringIn), UP);
            ring(sink, feet[0], feet[1] + 0.03, feet[2], r, r + 0.35, glowWhite(0.55 * ringIn), glowWhite(0.0), UP);
        }
        // The halo over his head as the armour closes.
        double halo = smooth((age - 30.0) / 10.0) * (1.0 - smooth((age - KingTransform.PEACE_TICKS) / 8.0));
        if (halo > 0.01) {
            ring(sink, feet[0], feet[1] + 2.28 + 0.02 * Math.sin(time * 0.13), feet[2], 0.3, 0.38, glowWhite(0.85 * halo), glowWhite(0.85 * halo), UP);
        }
        // The white light at its end: a ring running out over the ground, a glow round him.
        double t = age - KingTransform.PEACE_LIGHT;
        if (t >= 0.0 && t < 14.0) {
            double r = 0.5 + 4.8 * out(t / 10.0);
            double a = 1.0 - smooth(t / 14.0);
            ring(sink, feet[0], feet[1] + 0.05, feet[2], Math.max(0.0, r - 0.9), r, glowWhite(0.0), glowWhite(0.75 * a), UP);
            double light = KingTransform.light(KingForm.PEACE, true, age);
            glowDisc(sink, new double[] {feet[0], feet[1] + 1.2, feet[2]}, 0.6 + 1.8 * out(t / 6.0), eye, 0.8 * light);
        }
        feathers(sink, feet, t, seed, eye);
    }

    /** The feathers let go at the light: drifting out from his back and wings, swaying, turning, falling slowly, fading. */
    static void feathers(UkakuModel.Sink sink, double[] feet, double t, long seed, double[] eye) {
        if (t < 0.0) {
            return;
        }
        Random random = new Random(seed * 13L + 11L);
        for (int i = 0; i < FEATHERS; i++) {
            double azimuth = random.nextDouble() * Math.PI * 2.0;
            double r0 = 0.3 + random.nextDouble() * 1.4;
            double[] from = {feet[0] + Math.cos(azimuth) * r0, feet[1] + 1.2 + random.nextDouble() * 1.4, feet[2] + Math.sin(azimuth) * r0};
            double out = 0.025 + random.nextDouble() * 0.05;
            double up = 0.02 + random.nextDouble() * 0.03;
            double phase = random.nextDouble() * Math.PI * 2.0;
            double life = 70.0 + random.nextDouble() * 40.0;
            if (t > life) {
                continue;
            }
            // Out and a little up at first, then falling as a feather does (slowly, never faster than it drifts), swaying side to side.
            double drift = out * 18.0 * (1.0 - Math.exp(-t / 18.0));
            double fall = up * 10.0 * (1.0 - Math.exp(-t / 10.0)) - 0.012 * t;
            double sway = 0.18 * Math.sin(t * 0.17 + phase);
            double[] at = {from[0] + Math.cos(azimuth) * drift - Math.sin(azimuth) * sway, Math.max(feet[1] + 0.03, from[1] + fall),
                from[2] + Math.sin(azimuth) * drift + Math.cos(azimuth) * sway};
            double fade = 1.0 - smooth((t - life + 25.0) / 25.0);
            feather(sink, at, t * 0.09 + phase, phase, alpha(0xFFFBF8F2, 0.95 * fade));
        }
    }

    // ================================================================== shapes

    /** A flat ring round (x, y, z) from {@code inner} to {@code outer}, coloured {@code cin} inside and {@code cout} outside. */
    static void ring(UkakuModel.Sink sink, double x, double y, double z, double inner, double outer, int cin, int cout, double[] normal) {
        if (outer <= 0.0) {
            return;
        }
        int steps = 40;
        for (int i = 0; i < steps; i++) {
            double a0 = Math.PI * 2.0 * i / steps;
            double a1 = Math.PI * 2.0 * (i + 1) / steps;
            double c0 = Math.cos(a0);
            double s0 = Math.sin(a0);
            double c1 = Math.cos(a1);
            double s1 = Math.sin(a1);
            sink.quad(new double[] {x + c0 * inner, y, z + s0 * inner}, new double[] {x + c0 * outer, y, z + s0 * outer},
                new double[] {x + c1 * outer, y, z + s1 * outer}, new double[] {x + c1 * inner, y, z + s1 * inner}, normal, cin, cout, cout, cin);
        }
    }

    /** A dark disc on the ground, soft at its edge. */
    static void disc(UkakuModel.Sink sink, double x, double y, double z, double radius, int colour) {
        ring(sink, x, y, z, 0.0, radius * 0.6, colour, colour, FLAT);
        ring(sink, x, y, z, radius * 0.6, radius, colour, colour & 0x00FFFFFF, FLAT);
    }

    /** A low dome of {@code radius} round {@code centre}, all one {@code colour}. */
    static void dome(UkakuModel.Sink sink, double[] centre, double radius, int colour) {
        int around = 16;
        int up = 7;
        for (int j = 0; j < up; j++) {
            double p0 = -Math.PI * 0.35 + Math.PI * 0.85 * j / up;
            double p1 = -Math.PI * 0.35 + Math.PI * 0.85 * (j + 1) / up;
            for (int i = 0; i < around; i++) {
                double a0 = Math.PI * 2.0 * i / around;
                double a1 = Math.PI * 2.0 * (i + 1) / around;
                double[] q00 = sphere(centre, radius, a0, p0);
                double[] q10 = sphere(centre, radius, a1, p0);
                double[] q11 = sphere(centre, radius, a1, p1);
                double[] q01 = sphere(centre, radius, a0, p1);
                double[] n = normalize(sub(lerp(q00, q11, 0.5), centre));
                sink.quad(q00, q10, q11, q01, n, colour, colour, colour, colour);
            }
        }
    }

    private static double[] sphere(double[] c, double r, double azimuth, double rise) {
        return new double[] {c[0] + Math.cos(azimuth) * Math.cos(rise) * r, c[1] + Math.sin(rise) * r, c[2] + Math.sin(azimuth) * Math.cos(rise) * r};
    }

    /** A blood splat on the ground: ragged, a little longer one way. */
    static void splat(UkakuModel.Sink sink, double[] at, double radius, int seed, int colour) {
        int n = 9;
        double turn = WarCloth.hash(seed * 7 + 1) * Math.PI;
        for (int i = 0; i < n; i++) {
            double a0 = Math.PI * 2.0 * i / n;
            double a1 = Math.PI * 2.0 * (i + 1) / n;
            double r0 = radius * (0.65 + 0.5 * WarCloth.hash(seed * 31 + i)) * (1.0 + 0.35 * Math.cos(2.0 * (a0 - turn)));
            double r1 = radius * (0.65 + 0.5 * WarCloth.hash(seed * 31 + (i + 1) % n)) * (1.0 + 0.35 * Math.cos(2.0 * (a1 - turn)));
            double[] c = {at[0], at[1], at[2]};
            sink.quad(c, new double[] {at[0] + Math.cos(a0) * r0, at[1], at[2] + Math.sin(a0) * r0}, new double[] {at[0] + Math.cos(a1) * r1, at[1], at[2] + Math.sin(a1) * r1},
                c, FLAT, colour, colour, colour, colour);
        }
    }

    /** A soft round glow facing {@code eye}: bright in the middle, gone at its edge. */
    static void glowDisc(UkakuModel.Sink sink, double[] centre, double radius, double[] eye, double alpha) {
        if (alpha <= 0.01) {
            return;
        }
        double[] toEye = normalize(sub(eye, centre));
        double[] u = normalize(Math.abs(toEye[1]) > 0.95 ? cross(toEye, new double[] {1, 0, 0}) : cross(toEye, UP));
        double[] w = normalize(cross(toEye, u));
        int steps = 24;
        double[] radii = {0.0, 0.35, 1.0};
        double[] glow = {1.0, 0.45, 0.0};
        for (int r = 0; r + 1 < radii.length; r++) {
            for (int i = 0; i < steps; i++) {
                double a0 = Math.PI * 2.0 * i / steps;
                double a1 = Math.PI * 2.0 * (i + 1) / steps;
                double[] d0 = add(mul(u, Math.cos(a0)), mul(w, Math.sin(a0)));
                double[] d1 = add(mul(u, Math.cos(a1)), mul(w, Math.sin(a1)));
                int cin = glowWhite(alpha * glow[r]);
                int cout = glowWhite(alpha * glow[r + 1]);
                sink.quad(add(centre, mul(d0, radius * radii[r])), add(centre, mul(d0, radius * radii[r + 1])), add(centre, mul(d1, radius * radii[r + 1])),
                    add(centre, mul(d1, radius * radii[r])), UP, cin, cout, cout, cin);
            }
        }
    }

    /** A thin ribbon along {@code path}, facing {@code eye}, {@code from} wide at its start to {@code to} at its end. */
    static void ribbon(UkakuModel.Sink sink, double[][] path, double from, double to, double[] eye, int cfrom, int cto) {
        int n = path.length - 1;
        for (int i = 0; i < n; i++) {
            double[] a = path[i];
            double[] b = path[i + 1];
            double[] along = normalize(sub(b, a));
            double[] toEye = normalize(sub(eye, lerp(a, b, 0.5)));
            double[] across = normalize(cross(along, toEye));
            double wa = from + (to - from) * i / n;
            double wb = from + (to - from) * (i + 1) / n;
            int ca = mixColour(cfrom, cto, (double)i / n);
            int cb = mixColour(cfrom, cto, (double)(i + 1) / n);
            sink.quad(add(a, mul(across, wa)), add(b, mul(across, wb)), sub(b, mul(across, wb)), sub(a, mul(across, wa)), toEye, ca, cb, cb, ca);
        }
    }

    /** A small white feather at {@code at}: a long thin vane round its quill, turned {@code spin} about its length, tilted by {@code tilt}. */
    static void feather(UkakuModel.Sink sink, double[] at, double spin, double tilt, int colour) {
        double[] length = normalize(new double[] {Math.cos(tilt), 0.35 * Math.sin(spin * 0.7), Math.sin(tilt)});
        double[] side = normalize(cross(length, UP));
        double[] face = normalize(cross(side, length));
        double[] across = add(mul(side, Math.cos(spin)), mul(face, Math.sin(spin)));
        double l = 0.13;
        double w = 0.045;
        double[] tip = add(at, mul(length, l));
        double[] root = sub(at, mul(length, l));
        double[] mid = add(at, mul(length, l * 0.2));
        double[] n = normalize(cross(length, across));
        sink.quad(root, add(mid, mul(across, w)), tip, sub(mid, mul(across, w)), n, colour, colour, colour, colour);
    }

    // ================================================================== small things

    /** A colour that burns: red, as bright as {@code a}. */
    static int glowRed(double a) {
        a = Math.max(0.0, Math.min(1.0, a));
        return 0xFF000000 | (int)Math.round(235 * a) << 16 | (int)Math.round(20 * a) << 8 | (int)Math.round(34 * a);
    }

    /** A colour that burns: white with a little warmth, as bright as {@code a}. */
    static int glowWhite(double a) {
        a = Math.max(0.0, Math.min(1.0, a));
        return 0xFF000000 | (int)Math.round(255 * a) << 16 | (int)Math.round(250 * a) << 8 | (int)Math.round(236 * a);
    }

    static int alpha(int argb, double a) {
        int al = (int)Math.round(255.0 * Math.max(0.0, Math.min(1.0, a)));
        return al << 24 | argb & 0x00FFFFFF;
    }

    static int darken(int argb, double k) {
        int r = (int)(((argb >> 16) & 0xFF) * k);
        int g = (int)(((argb >> 8) & 0xFF) * k);
        int b = (int)((argb & 0xFF) * k);
        return argb & 0xFF000000 | r << 16 | g << 8 | b;
    }

    static int mixColour(int a, int b, double t) {
        int out = 0;
        for (int shift = 0; shift <= 24; shift += 8) {
            int ca = (a >>> shift) & 0xFF;
            int cb = (b >>> shift) & 0xFF;
            out |= (int)Math.round(ca + (cb - ca) * t) << shift;
        }
        return out;
    }

    static double smooth(double t) {
        t = Math.max(0.0, Math.min(1.0, t));
        return t * t * (3.0 - 2.0 * t);
    }

    static double out(double t) {
        t = Math.max(0.0, Math.min(1.0, t));
        return 1.0 - (1.0 - t) * (1.0 - t) * (1.0 - t);
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

    static double[] lerp(double[] a, double[] b, double t) {
        return new double[] {a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t, a[2] + (b[2] - a[2]) * t};
    }

    static double[] cross(double[] a, double[] b) {
        return new double[] {a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]};
    }

    static double[] normalize(double[] a) {
        double l = Math.sqrt(a[0] * a[0] + a[1] * a[1] + a[2] * a[2]);
        return l < 1.0E-12 ? new double[] {0.0, 1.0, 0.0} : new double[] {a[0] / l, a[1] / l, a[2] / l};
    }
}
