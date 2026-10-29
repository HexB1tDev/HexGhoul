package com.hex.kagune.control.king;

import static com.hex.kagune.control.furuta.FurutaMesh.add;
import static com.hex.kagune.control.furuta.FurutaMesh.clamp01;
import static com.hex.kagune.control.furuta.FurutaMesh.cross;
import static com.hex.kagune.control.furuta.FurutaMesh.dot;
import static com.hex.kagune.control.furuta.FurutaMesh.lerp;
import static com.hex.kagune.control.furuta.FurutaMesh.mix;
import static com.hex.kagune.control.furuta.FurutaMesh.mul;
import static com.hex.kagune.control.furuta.FurutaMesh.normalize;
import static com.hex.kagune.control.furuta.FurutaMesh.sub;
import static com.hex.kagune.control.king.WarModel.v;

import com.hex.kagune.control.ayato.CentipedeTail;
import com.hex.kagune.control.ayato.KakujaModel;
import com.hex.kagune.control.ayato.UkakuModel;
import com.hex.kagune.control.furuta.FurutaMesh;

/**
 * 1.0.13: what War's abilities put out into the world (pure, in blocks, wherever the caller sets the origin): the black centipedes
 * of King's Execution and the Black Sun's rain (the Centipede Form's own, {@link CentipedeTail}, black where it is plum), the
 * centipedes of the Spire Wave bursting up out of the ground, the Black Sun itself, its crescent, and the slashes of the bladed arm.
 * A quad with a normal of exactly (0, 1, 0) burns (drawn additive and full-bright), as everywhere else.
 */
public final class KingFxModel {
    static final int BLACK = 0xFF0C0A0C;
    static final int CHAR = 0xFF221C20;
    static final int RED = 0xFFE01828;
    static final int EMBER = 0xFFFF5A2A;
    static final int BLOOD = 0xFF5A0610;
    static final double[] UP = {0.0, 1.0, 0.0};

    private KingFxModel() {
    }

    // ================================================================== the centipedes

    /** {@code sink} with the Centipede Form's plum armour turned black chitin; its red flesh, burning, stays as it is. */
    public static UkakuModel.Sink black(UkakuModel.Sink sink) {
        return (a, b, c, d, n, ca, cb, cc, cd) -> sink.quad(a, b, c, d, n, blacken(ca), blacken(cb), blacken(cc), blacken(cd));
    }

    /**
     * One colour of the centipede, blackened: the flesh between the plates from burning red to a deep ember, the rest to black with
     * grey light on it.
     */
    public static int blacken(int argb) {
        int r = argb >> 16 & 0xFF;
        int g = argb >> 8 & 0xFF;
        int b = argb & 0xFF;
        if (r > 90 && r > g * 1.8 && r > b * 1.6) {
            return argb & 0xFF000000 | (int)(r * 0.5) << 16 | (int)(g * 0.22) << 8 | (int)(b * 0.25);
        }
        double l = 0.3 * r + 0.59 * g + 0.11 * b;
        int k = (int)Math.round(l * 0.42);
        return argb & 0xFF000000 | Math.min(255, (int)(k * 1.1)) << 16 | k << 8 | Math.min(255, (int)(k * 1.04));
    }

    /** One black centipede along {@code points} (blocks), {@code width} wide, its jaw {@code jaw} open, faded to {@code alpha}. */
    public static void centipede(UkakuModel.Sink sink, double[][] points, double width, int seed, double jaw, boolean fine, double alpha, double phase) {
        if (points.length < 2 || alpha <= 0.01) {
            return;
        }
        double[] along = normalize(sub(points[points.length - 1], points[0]));
        double[] dorsal = Math.abs(dot(along, UP)) > 0.9 ? v(1, 0, 0) : UP;
        KakujaModel.Spine spine = CentipedeTail.through(points, fine ? 56 : 32, seed % 2 == 0 ? 1 : -1, dorsal);
        if (spine.length() < 0.2) {
            return;
        }
        int plates = Math.max(4, Math.min(36, (int)Math.round(spine.length() / (width * 2.4))));
        CentipedeTail.tail(black(sink), spine, new CentipedeTail.Look(width, plates, fine, alpha, seed, jaw, 1.25, 0.35, phase));
    }

    /**
     * King's Execution: arm {@code index} of {@code arms}: out of the hand at {@code hand}, fanned out round the line to what it seizes
     * at {@code target} ({@code radius} round, {@code height} tall), and coiled round it. {@code reach} (0..1) is how far out it is
     * (it keeps its shape, shorter), {@code coil} (0..1) how far round, {@code squeeze} (0..1) how tight.
     */
    public static double[][] grasp(double[] hand, double[] target, double radius, double height, int index, int arms, double reach, double coil,
                                   double squeeze, double time) {
        double[] dir = normalize(sub(target, hand));
        double[] side = normalize(Math.abs(dot(dir, UP)) > 0.95 ? v(1, 0, 0) : cross(dir, UP));
        double[] up = normalize(cross(side, dir));
        double a = Math.PI * 2.0 * index / arms + 0.4;
        double[] fan = add(mul(side, Math.cos(a)), mul(up, Math.sin(a)));
        double distance = Math.sqrt(dot(sub(target, hand), sub(target, hand)));
        double wave = 0.25 * Math.sin(time * 0.6 + index * 1.7);
        java.util.List<double[]> pts = new java.util.ArrayList<>();
        pts.add(hand);
        pts.add(add(add(hand, mul(dir, Math.min(1.2, distance * 0.2))), mul(fan, 0.35)));
        pts.add(add(add(lerp(hand, target, 0.45), mul(fan, 1.2 + 0.15 * distance / 4.0 + wave)), mul(up, 0.4)));
        double hug = (radius + 0.35) * (1.0 - 0.35 * clamp01(squeeze));
        double base = target[1] - height * 0.35;
        double start = Math.atan2(fan[2], fan[0]);
        pts.add(v(target[0] - dir[0] * hug + fan[0] * hug * 0.6, base + height * (0.15 + 0.12 * index / arms), target[2] - dir[2] * hug + fan[2] * hug * 0.6));
        int turns = 7;
        double sweep = Math.PI * 2.0 * 1.3 * clamp01(coil);
        for (int k = 1; k <= turns && coil > 0.02; k++) {
            double t = (double)k / turns;
            double angle = start + (index % 2 == 0 ? 1 : -1) * sweep * t;
            pts.add(v(target[0] + Math.cos(angle) * hug, base + height * (0.15 + 0.12 * index / arms + 0.55 * t), target[2] + Math.sin(angle) * hug));
        }
        double[][] out = pts.toArray(new double[0][]);
        double r = clamp01(reach);
        for (int i = 1; i < out.length; i++) {
            out[i] = add(hand, mul(sub(out[i], hand), r));
        }
        return out;
    }

    /**
     * The Spire Wave: a centipede bursting up out of the ground at {@code base} ({@code out}, 0..1, how far up), {@code height} tall
     * when all out, leaning off upright by {@code lean} (blocks at the top), its head the point.
     */
    public static double[][] spire(double[] base, double[] lean, double height, double out, double time, int seed) {
        double h = height * FurutaMesh.smooth(clamp01(out));
        double sway = 0.08 * Math.sin(time * 0.3 + seed);
        return new double[][] {
            sub(base, v(0, 1.4, 0)),
            add(base, add(mul(lean, 0.08), v(0, h * 0.25, 0))),
            add(base, add(mul(lean, 0.35 + sway), v(0, h * 0.6, 0))),
            add(base, add(mul(lean, 0.75 + sway), v(0, h * 0.88, 0))),
            add(base, add(lean, v(0, h, 0))),
        };
    }

    /** 1.0.27: how many faces round a ground spike, how deep its root goes under the ground (blocks). */
    static final int NEEDLE_SIDES = 5;
    static final double NEEDLE_ROOT = 0.35;

    /**
     * 1.0.27: a spike broken up out of the ground (the warpath's RMB cone, King's Execution's rings): thin, straight and needle-sharp,
     * a few uneven faces like a shard of black bone, dark red where it comes out of the ground and black up to the point, the ground
     * glowing red where it went through. {@code base} is where it breaks the ground, {@code lean} (blocks at its top, sideways) its tilt
     * off upright, {@code height} its length out of the ground, {@code radius} its thickness there; {@code out} (0..1) how far it has
     * shot up (it comes up whole, point first, and goes back down the same way), {@code seed} its own unevenness.
     */
    public static void needle(UkakuModel.Sink sink, double[] base, double[] lean, double height, double radius, double out, int seed) {
        if (out <= 0.01 || height <= 0.05 || radius <= 0.0) {
            return;
        }
        double[] dir = normalize(v(lean[0] + (needleRandom(seed, 1) - 0.5) * 0.5, height, lean[2] + (needleRandom(seed, 2) - 0.5) * 0.5));
        double[] side = normalize(cross(dir, Math.abs(dir[0]) < 0.9 ? v(1, 0, 0) : v(0, 0, 1)));
        double[] other = cross(dir, side);
        double length = height + NEEDLE_ROOT;
        double k = 1.0 - clamp01(out);
        double shot = 1.0 - k * k * k;
        double[] root = sub(base, mul(dir, NEEDLE_ROOT + (1.0 - shot) * length));
        double[] tip = add(root, mul(dir, length));
        double[] neck = add(root, mul(dir, length * 0.3));
        double[][] low = new double[NEEDLE_SIDES][];
        double[][] high = new double[NEEDLE_SIDES][];
        double turn = needleRandom(seed, 3) * Math.PI * 2.0;
        for (int i = 0; i < NEEDLE_SIDES; i++) {
            double a = turn + Math.PI * 2.0 * i / NEEDLE_SIDES;
            double wide = radius * (0.75 + 0.5 * needleRandom(seed, 10 + i));
            low[i] = add(root, add(mul(side, Math.cos(a) * wide), mul(other, Math.sin(a) * wide)));
            double b = a + 0.3;
            double narrow = radius * 0.62 * (0.8 + 0.4 * needleRandom(seed, 20 + i));
            high[i] = add(neck, add(mul(side, Math.cos(b) * narrow), mul(other, Math.sin(b) * narrow)));
        }
        int deep = 0xFF6A0E16;
        int mid = 0xFF2E090D;
        int point = 0xFF0A0405;
        for (int i = 0; i < NEEDLE_SIDES; i++) {
            int j = (i + 1) % NEEDLE_SIDES;
            double[] outward = sub(add(low[i], low[j]), mul(root, 2.0));
            sink.quad(low[i], low[j], high[j], high[i], facing(low[i], low[j], high[i], outward), deep, deep, mid, mid);
            double[] outer = sub(add(high[i], high[j]), mul(neck, 2.0));
            sink.quad(high[i], high[j], tip, tip, facing(high[i], high[j], tip, outer), mid, mid, point, point);
        }
        // Where it went through, the ground split and glowing red (going out as it goes back down).
        double heat = clamp01(out * 1.4 - 0.3);
        if (heat > 0.02) {
            int hot = glow(heat * 0.85);
            int cold = glow(0.0);
            for (int i = 0; i < 3; i++) {
                double a = turn + i * 2.1 + needleRandom(seed, 30 + i);
                double reach = radius * (2.5 + 2.5 * needleRandom(seed, 40 + i));
                double[] along = v(Math.cos(a), 0.0, Math.sin(a));
                double[] across = v(-Math.sin(a) * 0.04, 0.0, Math.cos(a) * 0.04);
                double[] from = add(base, v(0.0, 0.03, 0.0));
                double[] to = add(from, mul(along, reach));
                sink.quad(sub(from, across), add(from, across), add(to, across), sub(to, across), UP, hot, hot, cold, cold);
            }
        }
    }

    /** A face's normal, turned to point the way of {@code outward}. */
    static double[] facing(double[] a, double[] b, double[] c, double[] outward) {
        double[] n = normalize(cross(sub(b, a), sub(c, a)));
        return dot(n, outward) < 0.0 ? mul(n, -1.0) : n;
    }

    /** A number in [0, 1) from {@code seed} and {@code k}, the same every time. */
    static double needleRandom(int seed, int k) {
        long h = seed * 0x9E3779B97F4A7C15L + k * 0xC2B2AE3D27D4EB4FL;
        h ^= h >>> 33;
        h *= 0xFF51AFD7ED558CCDL;
        h ^= h >>> 33;
        return (h >>> 11) * 0x1.0p-53;
    }

    /**
     * A spike of War's own rinkaku, out in the world (the Spire Wave's, the Black Sun's rain): black segments along {@code points}
     * (blocks), {@code width} thick at the root and coming to a point, a ridge down its back with a hooked barb raking back off every
     * segment, crimson underneath with a red vein burning down it, and a long claw at the end. Faded to {@code alpha}.
     */
    public static void spike(UkakuModel.Sink sink, double[][] points, double width, int seed, double time, double alpha) {
        if (points.length < 2 || alpha <= 0.01) {
            return;
        }
        java.util.List<double[]> control = new java.util.ArrayList<>(java.util.List.of(points));
        double[] along = normalize(sub(points[points.length - 1], points[0]));
        double[] up = Math.abs(dot(along, UP)) > 0.9 ? v(0, 0, 1) : UP;
        KakujaModel.Spine sp = WarModel.spine(control, 40, up);
        if (sp.length() < 0.2) {
            return;
        }
        int segments = Math.max(4, Math.min(18, (int)Math.round(sp.length() / (width * 2.2))));
        int a = (int)Math.round(clamp01(alpha) * 255.0) << 24;
        FurutaMesh.tube(sink, sp, 0.0, 0.96, 40, 7, new FurutaMesh.Surface() {
            @Override
            public double radius(double s, double around) {
                double seg = (s * segments) % 1.0;
                double ridge = Math.pow(Math.max(0.0, Math.sin(around)), 6) * 0.45;
                return spikeRadius(width, s) * (seg < 0.16 ? 0.9 : 1.0) * (1.0 + ridge - 0.12 * Math.abs(Math.cos(around)));
            }

            @Override
            public int colour(double s, double around) {
                double under = Math.max(0.0, -Math.sin(around));
                double seg = (s * segments) % 1.0;
                int c = mix(BLACK, 0xFF5A0A12, clamp01(under * 1.2 + s * 0.2));
                c = mix(c, seg < 0.22 ? 0xFF3A3236 : CHAR, 0.5 * Math.max(0.0, Math.sin(around)) * (1.0 - 0.6 * s));
                return a | (c & 0xFFFFFF);
            }
        }, 1.0);
        // The vein down its underside.
        for (int i = 0; i < 24; i++) {
            double s0 = 0.04 + 0.86 * i / 24.0;
            double s1 = 0.04 + 0.86 * (i + 1) / 24.0;
            double[][] f0 = sp.at(s0);
            double[][] f1 = sp.at(s1);
            double[] d0 = mul(f0[2], -spikeRadius(width, s0) * 0.88);
            double[] d1 = mul(f1[2], -spikeRadius(width, s1) * 0.88);
            double[] e0 = mul(f0[3], spikeRadius(width, s0) * 0.2);
            double[] e1 = mul(f1[3], spikeRadius(width, s1) * 0.2);
            int hot = glow(alpha * (0.8 - 0.4 * s0));
            sink.quad(add(add(f0[0], d0), e0), add(add(f1[0], d1), e1), sub(add(f1[0], d1), e1), sub(add(f0[0], d0), e0), UP, hot, hot, hot, hot);
        }
        // A hooked barb off the ridge at every segment, raking back toward the root.
        for (int k = 1; k < segments; k++) {
            double s = (double)k / segments + 0.01;
            double[][] f = sp.at(s);
            double r = spikeRadius(width, s);
            double[] ridge = add(f[0], mul(f[2], r * 1.2));
            barb(sink, ridge, add(f[2], mul(f[1], -0.6)), f[3], (0.9 + 0.6 * FurutaMesh.hash(seed * 31 + k)) * r * 2.4, r * 0.8, a);
        }
        // The claw at the end.
        double[][] tip = sp.at(0.96);
        double[][] before = sp.at(0.86);
        barb(sink, before[0], sub(tip[0], before[0]), tip[3], width * 3.2, spikeRadius(width, 0.86) * 1.8, a);
    }

    static double spikeRadius(double width, double s) {
        return width * (1.0 - 0.7 * Math.pow(clamp01(s), 1.2)) + 0.02;
    }

    /** A barb: a four-sided point from {@code base} along {@code dir}, {@code length} long and {@code width} across {@code side}, black to grey. */
    static void barb(UkakuModel.Sink sink, double[] base, double[] dir, double[] side, double length, double width, int alpha) {
        double[] d = normalize(dir);
        double[] across = normalize(sub(side, mul(d, dot(side, d))));
        double[] through = normalize(cross(d, across));
        double[] tip = add(base, mul(d, length));
        double[] mid = add(base, mul(d, length * 0.3));
        double[][] rim = {add(mid, mul(across, width)), add(mid, mul(through, width * 0.5)), sub(mid, mul(across, width)), sub(mid, mul(through, width * 0.5))};
        int root = alpha | (BLACK & 0xFFFFFF);
        int point = alpha | 0x4A4246;
        for (int k = 0; k < 4; k++) {
            double[] p0 = rim[k];
            double[] p1 = rim[(k + 1) % 4];
            double[] n = normalize(cross(sub(p1, p0), sub(tip, p0)));
            sink.quad(p0, p1, tip, tip, n, root, root, point, point);
            double[] m = normalize(cross(sub(base, p0), sub(p1, p0)));
            sink.quad(p0, base, base, p1, m, root, root, root, root);
        }
    }

    /**
     * The Spire Wave's layout: at step {@code step} of the wave, spike {@code k} (0: the big one, 1: a small one on the other side, on
     * every other step only; null where there is none): {sideways offset, forward offset, sideways lean, forward lean, height,
     * width}, in blocks across and along the wave.
     */
    public static double[] spireAt(int step, int k) {
        double side = step % 2 == 0 ? 1.0 : -1.0;
        double h = FurutaMesh.hash(step * 7 + 3);
        if (k == 0) {
            return new double[] {side * (0.25 + 0.2 * h), 0.0, side * (0.5 + 0.5 * h), 0.35 + 0.3 * FurutaMesh.hash(step * 5 + 1), 2.6 + 1.3 * h,
                0.38 + 0.06 * h};
        }
        if (k == 1 && step % 2 == 1) {
            return new double[] {-side * 0.55, 0.4, -side * 0.9, 0.2, 1.4 + 0.7 * FurutaMesh.hash(step * 11 + 2), 0.27};
        }
        return null;
    }

    /**
     * The Black Sun's rain: a centipede from the sun at {@code sun} plunging down onto {@code target}; {@code out} (0..1) how far down
     * it is, its head first.
     */
    public static double[][] rain(double[] sun, double[] target, double out, int seed) {
        double[] mid = lerp(sun, target, 0.5);
        double[] kink = add(mid, v(Math.sin(seed * 2.3) * 3.0, 2.0, Math.cos(seed * 1.7) * 3.0));
        double[][] full = {sun, lerp(sun, kink, 0.6), kink, lerp(kink, target, 0.6), target};
        double r = clamp01(out);
        for (int i = 1; i < full.length; i++) {
            full[i] = add(sun, mul(sub(full[i], sun), r));
        }
        return full;
    }

    // ================================================================== the Black Sun

    /**
     * The Black Sun: a sphere of black at {@code centre}, {@code radius} wide, red veins crawling over it, a corona of red and ember
     * burning round its rim as seen from the camera ({@code toCamera}: from the centre toward it).
     */
    public static void sun(UkakuModel.Sink sink, double[] centre, double radius, double[] toCamera, double time) {
        if (radius <= 0.05) {
            return;
        }
        int bands = 14;
        int segments = 28;
        double[][][] grid = new double[bands + 1][segments + 1][];
        for (int i = 0; i <= bands; i++) {
            double theta = Math.PI * i / bands;
            for (int j = 0; j <= segments; j++) {
                double phi = Math.PI * 2.0 * j / segments;
                grid[i][j] = v(Math.sin(theta) * Math.cos(phi), Math.cos(theta), Math.sin(theta) * Math.sin(phi));
            }
        }
        for (int i = 0; i < bands; i++) {
            for (int j = 0; j < segments; j++) {
                double[][] corner = {grid[i][j], grid[i + 1][j], grid[i + 1][j + 1], grid[i][j + 1]};
                double[][] at = new double[4][];
                int[] colour = new int[4];
                for (int k = 0; k < 4; k++) {
                    at[k] = add(centre, mul(corner[k], radius));
                    double vein = Math.sin(corner[k][0] * 7.0 + time * 0.05) * Math.sin(corner[k][1] * 9.0 - time * 0.04) * Math.sin(corner[k][2] * 8.0);
                    colour[k] = mix(BLACK, 0xFF3A0610, clamp01(vein * 3.0 - 2.0));
                }
                double[] mid = normalize(add(add(corner[0], corner[1]), add(corner[2], corner[3])));
                sink.quad(at[0], at[1], at[2], at[3], mid, colour[0], colour[1], colour[2], colour[3]);
            }
        }
        // The corona: rings round the rim, facing the camera, burning out from it.
        double[] n = normalize(toCamera);
        double[] u = normalize(Math.abs(dot(n, UP)) > 0.95 ? cross(n, v(1, 0, 0)) : cross(n, UP));
        double[] w = cross(n, u);
        double[] face = add(centre, mul(n, radius * 0.2));
        double[][] rings = {{0.96, 1.08, 1.0}, {1.08, 1.35, 0.55}, {1.35, 1.9, 0.22}, {1.9, 2.6, 0.0}};
        int sides = 40;
        for (double[] ring : rings) {
            for (int s = 0; s < sides; s++) {
                double a0 = Math.PI * 2.0 * s / sides;
                double a1 = Math.PI * 2.0 * (s + 1) / sides;
                double flicker = 0.85 + 0.15 * Math.sin(time * 0.7 + s * 1.3);
                int in = glow(ring[2] * flicker);
                int outer = glow(Math.max(0.0, ring[2] - 0.45) * flicker);
                double[] i0 = add(face, add(mul(u, Math.cos(a0) * radius * ring[0]), mul(w, Math.sin(a0) * radius * ring[0])));
                double[] o0 = add(face, add(mul(u, Math.cos(a0) * radius * ring[1]), mul(w, Math.sin(a0) * radius * ring[1])));
                double[] o1 = add(face, add(mul(u, Math.cos(a1) * radius * ring[1]), mul(w, Math.sin(a1) * radius * ring[1])));
                double[] i1 = add(face, add(mul(u, Math.cos(a1) * radius * ring[0]), mul(w, Math.sin(a1) * radius * ring[0])));
                sink.quad(i0, o0, o1, i1, UP, in, outer, outer, in);
            }
        }
    }

    /** Red burning at {@code amount} (0..1): ember at the hottest, down through red to nothing. */
    static int glow(double amount) {
        double k = clamp01(amount);
        int c = k > 0.7 ? mix(RED, EMBER, (k - 0.7) / 0.3) : mix(0xFF000000, RED, k / 0.7);
        return c | 0xFF000000;
    }

    /**
     * The Black Sun's crescent: a ring of black blade round {@code centre} at {@code radius}, {@code height} tall, its cross-section a
     * crescent leaning out, burning red along its inner edge; faded to {@code alpha}.
     */
    public static void crescent(UkakuModel.Sink sink, double[] centre, double radius, double height, double alpha, double time) {
        if (radius <= 0.1 || alpha <= 0.01) {
            return;
        }
        int sides = Math.max(24, Math.min(160, (int)(radius * 5.0)));
        // The cross-section: from the foot up the curve of the blade, leaning out at the top.
        double[][] profile = {{0.0, 0.0}, {0.35, 0.3}, {0.55, 0.6}, {0.6, 0.85}, {0.45, 1.0}};
        int a = (int)Math.round(alpha * 255.0) << 24;
        for (int s = 0; s < sides; s++) {
            double a0 = Math.PI * 2.0 * s / sides;
            double a1 = Math.PI * 2.0 * (s + 1) / sides;
            double[] r0 = v(Math.cos(a0), 0, Math.sin(a0));
            double[] r1 = v(Math.cos(a1), 0, Math.sin(a1));
            for (int k = 0; k + 1 < profile.length; k++) {
                double[] p00 = add(centre, add(mul(r0, radius + profile[k][0] * height), v(0, profile[k][1] * height, 0)));
                double[] p01 = add(centre, add(mul(r0, radius + profile[k + 1][0] * height), v(0, profile[k + 1][1] * height, 0)));
                double[] p11 = add(centre, add(mul(r1, radius + profile[k + 1][0] * height), v(0, profile[k + 1][1] * height, 0)));
                double[] p10 = add(centre, add(mul(r1, radius + profile[k][0] * height), v(0, profile[k][1] * height, 0)));
                double[] normal = normalize(cross(sub(p01, p00), sub(p10, p00)));
                int c0 = a | (mix(BLACK, CHAR, k / 4.0) & 0xFFFFFF);
                sink.quad(p00, p01, p11, p10, normal, c0, c0, c0, c0);
            }
            // The inner edge burning, a little in front of the blade.
            double[] e0 = add(centre, mul(r0, radius - 0.15));
            double[] e1 = add(centre, mul(r1, radius - 0.15));
            double flicker = 0.75 + 0.25 * Math.sin(time * 0.9 + s * 0.7);
            int hot = glow(alpha * flicker);
            int cool = glow(alpha * flicker * 0.2);
            sink.quad(e0, add(e0, v(0, height * 0.7, 0)), add(e1, v(0, height * 0.7, 0)), e1, UP, hot, cool, cool, hot);
        }
    }

    // ================================================================== the bladed arm

    /**
     * A slash of the bladed arm: an arc round {@code centre} in the plane of {@code a} and {@code b} (unit, square), {@code radius} out,
     * from angle {@code from} to {@code to} (radians from {@code a} toward {@code b}), {@code width} thick at its middle and coming to
     * points at its ends: black, its edge burning; faded to {@code alpha}.
     */
    public static void slash(UkakuModel.Sink sink, double[] centre, double[] a, double[] b, double radius, double width, double from, double to,
                             double alpha) {
        if (alpha <= 0.01) {
            return;
        }
        int steps = 28;
        int edgeHot = glow(alpha);
        for (int i = 0; i < steps; i++) {
            double t0 = (double)i / steps;
            double t1 = (double)(i + 1) / steps;
            double g0 = from + (to - from) * t0;
            double g1 = from + (to - from) * t1;
            double w0 = width * Math.sin(Math.PI * t0);
            double w1 = width * Math.sin(Math.PI * t1);
            double[] d0 = add(mul(a, Math.cos(g0)), mul(b, Math.sin(g0)));
            double[] d1 = add(mul(a, Math.cos(g1)), mul(b, Math.sin(g1)));
            double[] outer0 = add(centre, mul(d0, radius));
            double[] outer1 = add(centre, mul(d1, radius));
            double[] inner0 = add(centre, mul(d0, radius - w0));
            double[] inner1 = add(centre, mul(d1, radius - w1));
            int body = (int)Math.round(alpha * 150.0 * Math.sin(Math.PI * (t0 + t1) * 0.5)) << 24 | (BLACK & 0xFFFFFF);
            double[] normal = normalize(cross(a, b));
            sink.quad(inner0, outer0, outer1, inner1, normal, body, body, body, body);
            // The burning edge along the outside.
            double[] edge0 = add(centre, mul(d0, radius + 0.08 * w0 / Math.max(1.0E-6, width)));
            double[] edge1 = add(centre, mul(d1, radius + 0.08 * w1 / Math.max(1.0E-6, width)));
            double[] lip0 = add(centre, mul(d0, radius - 0.3 * w0));
            double[] lip1 = add(centre, mul(d1, radius - 0.3 * w1));
            int fade0 = glow(alpha * Math.sin(Math.PI * t0));
            int fade1 = glow(alpha * Math.sin(Math.PI * t1));
            sink.quad(lip0, edge0, edge1, lip1, UP, fade0 == 0 ? edgeHot : fade0, fade0, fade1, fade1);
        }
    }

    // ================================================================== 1.0.14: the M1

    /**
     * The shock of a fist or a foot landing: a burning ring round {@code centre} square to {@code normal}, {@code radius} out and
     * {@code width} wide, its glow at {@code alpha}, hottest on its outer edge.
     */
    public static void shock(UkakuModel.Sink sink, double[] centre, double[] normal, double radius, double width, double alpha) {
        if (alpha <= 0.01 || radius <= 0.0) {
            return;
        }
        double[] n = normalize(normal);
        double[] u = normalize(Math.abs(dot(n, UP)) > 0.9 ? cross(n, v(1, 0, 0)) : cross(n, UP));
        double[] w = normalize(cross(n, u));
        int steps = 24;
        int hot = glow(alpha);
        int cool = glow(alpha * 0.2);
        double inner = Math.max(0.0, radius - width);
        for (int i = 0; i < steps; i++) {
            double a0 = Math.PI * 2.0 * i / steps;
            double a1 = Math.PI * 2.0 * (i + 1) / steps;
            double[] d0 = add(mul(u, Math.cos(a0)), mul(w, Math.sin(a0)));
            double[] d1 = add(mul(u, Math.cos(a1)), mul(w, Math.sin(a1)));
            sink.quad(add(centre, mul(d0, inner)), add(centre, mul(d0, radius)), add(centre, mul(d1, radius)), add(centre, mul(d1, inner)), UP, cool,
                hot, hot, cool);
        }
    }

    /**
     * Where the bladed arm's arc is {@code age} ticks into a blow that lands at {@code impact}: {from, to} (radians, as {@link #slash}
     * takes them) between {@code start} and {@code end}, its head through the middle of the sweep on the impact tick and its tail
     * following; null when there is nothing of it to draw.
     */
    public static double[] sweep(double start, double end, double age, int impact) {
        double head = clamp01((age - (impact - 2.0)) / 4.0);
        double tail = clamp01((age - (impact - 1.0)) / 6.0);
        if (head <= 0.0 || head - tail < 0.02) {
            return null;
        }
        return new double[] {start + (end - start) * tail, start + (end - start) * head};
    }

    /**
     * A blow's swing out in the world, {@code age} ticks after it began ({@link WarStrike}): the bladed arm's arc for the slashes (swept
     * right to left for the reap, low left to high right rising, overhead down for the Verdict) and the heel's ring round his hips, round
     * his {@code chest}, {@code facing} the way he strikes (level, unit). Fists and feet have none: theirs is the {@link #shock}.
     */
    public static void swing(UkakuModel.Sink sink, WarStrike blow, double[] chest, double[] facing, double age) {
        double[] f = facing;
        double[] r = {-f[2], 0.0, f[0]};
        double fade = clamp01(1.0 - (age - blow.impact) / (blow.duration - blow.impact + 4.0));
        switch (blow) {
            case REAP -> {
                // Tipped well over (high on his right, low on his left), so it reads from eye level, not as a line seen edge on.
                double[] at = sweep(-0.35, Math.PI + 0.35, age, blow.impact);
                if (at != null) {
                    slash(sink, add(chest, v(0, 0.3, 0)), normalize(add(r, v(0, 0.75, 0))), f, 2.3, 0.7, at[0], at[1], 0.95 * fade);
                }
            }
            case RISING -> {
                double[] at = sweep(-0.25, Math.PI + 0.25, age, blow.impact);
                if (at != null) {
                    slash(sink, add(chest, v(0, 0.2, 0)), normalize(add(mul(r, -1.0), v(0, -1.0, 0))), f, 2.2, 0.65, at[0], at[1], 0.95 * fade);
                }
            }
            case HEEL -> {
                // The full turn's ring at the hips, the heel's path all round him.
                double[] at = sweep(0.0, Math.PI * 2.0, age + 1.0, blow.impact);
                if (at != null) {
                    slash(sink, add(chest, v(0, -0.35, 0)), f, r, 1.7, 0.35, at[0], at[1], 0.8 * fade);
                }
            }
            case VERDICT, CLEAVER, DOWNFALL -> {
                // Ahead of the others: on the impact tick it is already coming down through them, not still overhead.
                double[] at = sweep(-0.45, 2.3, age + 1.5, blow.impact);
                if (at != null) {
                    double big = blow == WarStrike.CLEAVER ? 1.15 : 1.0;
                    slash(sink, add(chest, v(0, 0.5, 0)), UP, f, 2.5 * big, 0.85 * big, at[0], at[1], fade);
                }
            }
            case REND -> {
                // 1.0.20: the rinkaku driven straight through: a narrow arc thrown out flat before him, then swept wide.
                double[] at = sweep(-0.5, 0.5, age + 0.5, blow.impact);
                if (at != null) {
                    slash(sink, add(chest, mul(f, -1.2)), r, f, 3.4, 0.5, at[0], at[1], fade);
                }
            }
            case ASCENSION -> {
                // 1.0.20: from low before him up over his head: the arc stood on end, rising.
                double[] at = sweep(2.4, -0.4, age + 1.0, blow.impact);
                if (at != null) {
                    slash(sink, add(chest, v(0, 0.1, 0)), UP, f, 2.3, 0.8, at[0], at[1], fade);
                }
            }
            case CALAMITY -> {
                // 1.0.20: two arcs brought down together, the blade's and the rinkaku's, wide.
                double[] at = sweep(-0.6, 2.4, age + 1.5, blow.impact);
                if (at != null) {
                    slash(sink, add(chest, v(0, 0.6, 0)), UP, f, 2.9, 1.0, at[0], at[1], fade);
                    slash(sink, add(chest, v(0, 0.6, 0)), normalize(add(UP, mul(r, 0.5))), f, 2.4, 0.6, at[0], at[1], 0.8 * fade);
                    slash(sink, add(chest, v(0, 0.6, 0)), normalize(add(UP, mul(r, -0.5))), f, 2.4, 0.6, at[0], at[1], 0.8 * fade);
                }
            }
            default -> {
            }
        }
    }

    /**
     * Where a blow met a body ({@code at}), {@code age} ticks after: a fist's or a foot's shock (twin rings racing out square to the
     * blow, bigger for a kick); a blade's cut needs none (its arc is the {@link #swing}).
     */
    public static void impact(UkakuModel.Sink sink, WarStrike blow, double[] at, double[] facing, double age) {
        if (blow.kind != WarStrike.Kind.FIST && blow.kind != WarStrike.Kind.KICK) {
            return;
        }
        double big = blow.kind == WarStrike.Kind.KICK ? 1.7 : 1.0;
        double fade = clamp01(1.0 - age / 7.0);
        double grow = FurutaMesh.smooth(clamp01(age / 4.0));
        double lag = FurutaMesh.smooth(clamp01((age - 1.0) / 4.0));
        shock(sink, at, facing, big * (0.25 + 0.75 * grow), 0.12 * big, 0.95 * fade);
        shock(sink, add(at, mul(facing, 0.25 * big)), facing, big * (0.15 + 0.55 * lag), 0.08 * big, 0.6 * fade);
    }

    /**
     * King's Verdict, {@code age} ticks after it landed on {@code ground} (blocks; where the rinkaku come up): four of them bursting up
     * out of the ground round it, crossing over it, then sinking back; the ground's black crescent racing out.
     */
    public static void verdict(UkakuModel.Sink sink, double[] ground, double[] facing, double age, double time) {
        double out = age < 3.0 ? age / 3.0 : age < 9.0 ? 1.0 : clamp01(1.0 - (age - 9.0) / 6.0);
        if (out <= 0.01) {
            return;
        }
        for (int k = 0; k < 4; k++) {
            double a = Math.PI * 0.25 + k * Math.PI * 0.5;
            double[] side = {facing[0] * Math.cos(a) - facing[2] * Math.sin(a), 0.0, facing[2] * Math.cos(a) + facing[0] * Math.sin(a)};
            double[] base = add(ground, mul(side, 1.1));
            // Leaning in over it, crossing above where it stood.
            double[] lean = mul(side, -1.5);
            spike(sink, spire(base, lean, 2.5, out, time, 90 + k), 0.26, 90 + k, time, 1.0);
        }
        double ring = WarStrike.VERDICT_RADIUS * 1.25 * FurutaMesh.smooth(clamp01(age / 5.0));
        crescent(sink, ground, ring, 0.55, 0.85 * clamp01(1.0 - age / 12.0), time);
    }

    // ================================================================== 1.0.15: the Black Sun out of the ground

    /**
     * One of the Black Sun's centipedes: up out of the ground at {@code base} (below it, at first), rising beside the body it seized
     * ({@code body} its centre, {@code radius} round and {@code height} tall) and coiled round it, swaying with {@code time}; only
     * {@code out} (0..1) of it has come up yet, its point leading.
     */
    public static double[][] seize(double[] base, double[] body, double radius, double height, double out, double time, int seed) {
        java.util.List<double[]> points = new java.util.ArrayList<>();
        double turn = FurutaMesh.hash(seed * 7 + 3) * Math.PI * 2.0;
        double r = radius + 0.32;
        double[] bottom = v(body[0] + Math.cos(turn) * r, body[1] - height * 0.5 - 0.2, body[2] + Math.sin(turn) * r);
        double[] root = sub(base, v(0, 1.4, 0));
        double sway = 0.35 * Math.sin(time * 0.21 + seed);
        points.add(root);
        points.add(base);
        int column = (int)Math.max(2, Math.min(8, Math.round(Math.abs(bottom[1] - base[1]) / 2.5)));
        for (int k = 1; k <= column; k++) {
            double t = (double)k / (column + 1);
            double[] p = lerp(base, bottom, t);
            points.add(add(p, v(sway * Math.sin(t * Math.PI * 2.0 + seed), 0.0, sway * Math.cos(t * Math.PI * 2.0 + seed))));
        }
        points.add(bottom);
        for (int k = 1; k <= 10; k++) {
            double a = turn + k * 0.78;
            points.add(v(body[0] + Math.cos(a) * r, bottom[1] + height * 1.15 * k / 10.0, body[2] + Math.sin(a) * r));
        }
        return upTo(points, clamp01(out));
    }

    /** The first {@code share} of the line through {@code points}, by length. */
    static double[][] upTo(java.util.List<double[]> points, double share) {
        double total = 0.0;
        for (int i = 1; i < points.size(); i++) {
            double[] d = sub(points.get(i), points.get(i - 1));
            total += Math.sqrt(dot(d, d));
        }
        double keep = total * share;
        java.util.List<double[]> out = new java.util.ArrayList<>();
        out.add(points.get(0));
        double run = 0.0;
        for (int i = 1; i < points.size(); i++) {
            double[] d = sub(points.get(i), points.get(i - 1));
            double step = Math.sqrt(dot(d, d));
            if (run + step >= keep) {
                double f = step < 1.0E-9 ? 0.0 : (keep - run) / step;
                out.add(add(points.get(i - 1), mul(d, f)));
                break;
            }
            run += step;
            out.add(points.get(i));
        }
        if (out.size() < 2) {
            out.add(add(points.get(0), v(0, 0.01, 0)));
        }
        return out.toArray(new double[0][]);
    }

    // ================================================================== 1.0.16: Peace

    /** Peace's pale light: white with a little rose, at {@code k} (0..1; it adds, so the alpha is how bright). */
    static int pale(double k) {
        int a = (int)Math.round(255.0 * clamp01(k));
        return a << 24 | 0xFFE6EE;
    }

    /**
     * A thin trail along {@code path} (blocks; oldest first): a ribbon {@code width} wide square to the way it was seen from above and
     * below alike, pale, brightest at its newest end and gone at its oldest.
     */
    public static void trail(UkakuModel.Sink sink, double[][] path, double width, double alpha) {
        if (path.length < 2 || alpha <= 0.01) {
            return;
        }
        for (int i = 0; i + 1 < path.length; i++) {
            double[] a = path[i];
            double[] b = path[i + 1];
            double[] d = sub(b, a);
            if (dot(d, d) < 1.0E-8) {
                continue;
            }
            double[] side = cross(normalize(d), UP);
            side = dot(side, side) < 1.0E-6 ? v(1, 0, 0) : normalize(side);
            double k0 = (double)i / (path.length - 1);
            double k1 = (double)(i + 1) / (path.length - 1);
            double w0 = width * k0;
            double w1 = width * k1;
            int c0 = pale(alpha * k0 * k0);
            int c1 = pale(alpha * k1 * k1);
            sink.quad(add(a, mul(side, w0)), add(b, mul(side, w1)), sub(b, mul(side, w1)), sub(a, mul(side, w0)), UP, c0, c1, c1, c0);
            double[] lift = v(0, 1, 0);
            sink.quad(add(a, mul(lift, w0)), add(b, mul(lift, w1)), sub(b, mul(lift, w1)), sub(a, mul(lift, w0)), UP, c0, c1, c1, c0);
        }
    }

    /** An X of pale light at {@code centre} square to {@code ahead}, {@code size} from its middle to each end, {@code width} thick. */
    public static void xMark(UkakuModel.Sink sink, double[] centre, double[] ahead, double size, double width, double alpha) {
        if (alpha <= 0.01) {
            return;
        }
        double[] side = normalize(cross(ahead, UP));
        for (int k = 0; k < 2; k++) {
            double s = k == 0 ? 1.0 : -1.0;
            double[] arm = normalize(add(mul(side, s), UP));
            double[] across = normalize(cross(arm, ahead));
            double[] a = add(centre, mul(arm, size));
            double[] b = sub(centre, mul(arm, size));
            int hot = pale(alpha);
            int end = pale(alpha * 0.1);
            sink.quad(add(a, mul(across, width * 0.2)), add(centre, mul(across, width)), sub(centre, mul(across, width)), sub(a, mul(across, width * 0.2)), UP,
                end, hot, hot, end);
            sink.quad(add(centre, mul(across, width)), add(b, mul(across, width * 0.2)), sub(b, mul(across, width * 0.2)), sub(centre, mul(across, width)), UP,
                hot, end, end, hot);
        }
    }

    /**
     * 1.0.33: Black Sun's ring of air: a pale band {@code width} wide at {@code r} round {@code at}, in the plane of {@code across} and
     * {@code up}, facing {@code way} (the punch's, so it stands up across the sky square to it), as see-through as {@code fade} leaves
     * it; nothing of it below {@code floor} (the ground).
     */
    public static void airRing(UkakuModel.Sink sink, double[] at, double r, double width, double fade, double[] across, double[] up, double[] way,
                               double floor) {
        if (fade <= 0.01 || r < 0.2) {
            return;
        }
        int pale = (int)Math.round(120 * fade) << 24 | 0xF2F4F8;
        int segments = 64;
        double[] n = {way[0] * 0.9995, way[1] * 0.9995, way[2] * 0.9995};
        double[] back = {-n[0], -n[1], -n[2]};
        for (int i = 0; i < segments; i++) {
            double a0 = i * Math.PI * 2.0 / segments;
            double a1 = (i + 1) * Math.PI * 2.0 / segments;
            double[] in0 = onRing(at, across, up, way, a0, r - width, 0.0);
            double[] out0 = onRing(at, across, up, way, a0, r, 0.1);
            double[] out1 = onRing(at, across, up, way, a1, r, 0.1);
            double[] in1 = onRing(at, across, up, way, a1, r - width, 0.0);
            if (Math.min(Math.min(in0[1], in1[1]), Math.min(out0[1], out1[1])) < floor) {
                continue;
            }
            sink.quad(in0, out0, out1, in1, n, pale, pale, pale, pale);
            sink.quad(in1, out1, out0, in0, back, pale, pale, pale, pale);
        }
    }

    private static double[] onRing(double[] at, double[] u, double[] v, double[] w, double a, double r, double lift) {
        double c = Math.cos(a) * r;
        double s = Math.sin(a) * r;
        return new double[] {at[0] + u[0] * c + v[0] * s + w[0] * lift, at[1] + u[1] * c + v[1] * s + w[1] * lift, at[2] + u[2] * c + v[2] * s + w[2] * lift};
    }
}
