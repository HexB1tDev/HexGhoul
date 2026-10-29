package com.hex.kagune.control.king;

import static com.hex.kagune.control.furuta.FurutaMesh.add;
import static com.hex.kagune.control.furuta.FurutaMesh.clamp01;
import static com.hex.kagune.control.furuta.FurutaMesh.cross;
import static com.hex.kagune.control.furuta.FurutaMesh.lerp;
import static com.hex.kagune.control.furuta.FurutaMesh.mix;
import static com.hex.kagune.control.furuta.FurutaMesh.mul;
import static com.hex.kagune.control.furuta.FurutaMesh.normalize;
import static com.hex.kagune.control.furuta.FurutaMesh.sub;
import static com.hex.kagune.control.king.WarModel.v;

import com.hex.kagune.control.ayato.KakujaModel;
import com.hex.kagune.control.ayato.UkakuModel;
import com.hex.kagune.control.furuta.FurutaMesh;
import java.util.ArrayList;
import java.util.List;

/**
 * 1.0.11: Kaneki turning into the Dragon (pure), in the seconds before it erupts out of him: kagune rips out of his chest, then out
 * of his head; black centipede plates spread over him from the wound, red cracks running on ahead of them; a segmented tail tears
 * out of his lower back; his one kakugan burns. {@code p} (0..1) is how far it has gone: the prelude's progress, {@link #PRELUDE_TICKS}
 * long. Built in body pixels as {@link WarModel} is (x to the player's left, y up from the feet, z backward).
 *
 * <p>The beats ({@link #CHEST_RIP}, {@link #HEAD_RIP}, {@link #BURSTS}, {@link #SCREAMS}) are shared with the server, which plays the
 * screams and the explosions on them, and with the pose ({@code WarPose.turning}).
 */
public final class TurningModel {
    /** How long it takes (ticks): nine seconds of it before the Dragon breaks out. */
    public static final int PRELUDE_TICKS = 180;
    /** When the kagune tears out of his chest, and out of his head (ticks into the prelude). */
    public static final int CHEST_RIP = 22;
    public static final int HEAD_RIP = 70;
    /** The violent bursts: {tick, how hard (0..1)}; the last is the one the Dragon comes out of. */
    public static final int[][] BURSTS = {{CHEST_RIP, 6}, {HEAD_RIP, 7}, {104, 8}, {124, 9}, {150, 8}, {168, 10}};
    /** The screams: {tick, how long (ticks)}. */
    public static final int[][] SCREAMS = {{CHEST_RIP - 2, 30}, {HEAD_RIP - 2, 30}, {146, 34}};

    static final int SHELL = 0xFF0E0B0D;
    static final int SHELL_EDGE = 0xFF2A1A1F;
    static final int GROOVE = 0xFF4A0911;
    static final int CRIMSON = 0xFF8E0F1A;
    static final int VEIN = 0xFFC0182A;
    static final int FLESH = 0xFF3B0A10;
    static final int CRACK = 0xFFFF4A1E;
    static final int BLOOD = 0xFF5A0610;
    static final int IRIS = 0xFFFF1C22;

    /** Where it starts: the wound in the middle of his chest (body pixels). */
    static final double[] ORIGIN = {0.0, 19.0, -2.0};
    /** How far (body pixels from the wound) the plates have spread at the end. */
    static final double REACH = 26.0;
    /** The tendrils out of his chest: roots {x, y} on the front of the torso, and where each reaches (relative, body pixels). */
    static final double[][][] CHEST = {
        {{1.3, 20.4}, {3.0, 3.0, -6.0}, {8.0, 6.0, -7.0}, {12.0, 2.0, -1.0}, {12.5, -3.0, 4.0}},
        {{-1.3, 20.4}, {-3.0, 3.5, -6.5}, {-8.5, 7.0, -6.0}, {-12.0, 3.0, 0.0}, {-11.0, -2.0, 5.0}},
        {{1.0, 17.2}, {2.5, -1.0, -6.0}, {7.0, -4.0, -8.0}, {11.0, -8.0, -4.0}, {13.0, -10.0, 2.0}},
        {{-1.0, 17.2}, {-2.5, -1.5, -6.0}, {-7.5, -5.0, -7.0}, {-10.5, -9.0, -2.0}, {-12.0, -10.0, 3.0}},
    };
    /** The tendrils out of his head: roots on the head (body pixels) and where each reaches (relative). */
    static final double[][][] HEAD = {
        {{0.0, 32.0, 1.0}, {0.0, 6.0, 2.0}, {1.5, 12.0, 6.0}, {0.5, 15.0, 12.0}, {-1.0, 13.0, 17.0}},
        {{3.2, 30.6, 2.4}, {4.0, 4.0, 3.0}, {8.0, 8.0, 7.0}, {11.0, 7.0, 12.0}, {12.0, 3.0, 15.0}},
        {{-3.2, 30.6, 2.4}, {-4.0, 4.5, 3.0}, {-8.5, 9.0, 6.0}, {-11.5, 8.0, 11.0}, {-12.0, 4.0, 15.0}},
    };
    /** The tail out of his lower back: root, then the curl back and up (body pixels). */
    static final double[][] TAIL = {{0.0, 13.6, 2.0}, {0.0, 11.0, 7.0}, {0.0, 8.5, 13.0}, {0.0, 10.0, 20.0}, {0.0, 15.5, 25.0}, {0.0, 22.0, 27.0},
        {0.0, 27.0, 25.5}};
    static final int TAIL_SEGMENTS = 14;

    /** A body part as a box (body pixels): its centre, half width (x) and half depth (z), and its bottom and top. */
    record Box(double cx, double cz, double hx, double hz, double y0, double y1) {
    }

    static final Box HEAD_BOX = new Box(0.0, 0.0, 4.0, 4.0, 24.0, 32.0);
    static final Box TORSO_BOX = new Box(0.0, 0.0, 4.0, 2.0, 12.0, 24.0);
    static final Box RIGHT_ARM_BOX = new Box(-6.0, 0.0, 2.0, 2.0, 12.0, 24.0);
    static final Box LEFT_ARM_BOX = new Box(6.0, 0.0, 2.0, 2.0, 12.0, 24.0);
    static final Box RIGHT_LEG_BOX = new Box(-2.0, 0.0, 2.0, 2.0, 0.0, 12.0);
    static final Box LEFT_LEG_BOX = new Box(2.0, 0.0, 2.0, 2.0, 0.0, 12.0);

    private TurningModel() {
    }

    // ================================================================== the beats

    /** How far out of his chest the kagune is at {@code p}: most of it in a violent instant at the rip, the rest slowly after. */
    public static double chest(double p) {
        return rip(p * PRELUDE_TICKS - CHEST_RIP);
    }

    /** How far out of his head. */
    public static double head(double p) {
        return rip(p * PRELUDE_TICKS - HEAD_RIP);
    }

    /** {@code since} ticks after a rip: over half out in the first six ticks, the rest over the next three seconds. */
    static double rip(double since) {
        return 0.6 * FurutaMesh.smooth(clamp01(since / 6.0)) + 0.4 * FurutaMesh.smooth(clamp01((since - 6.0) / 60.0));
    }

    /** How far out of his back the tail is. */
    public static double tail(double p) {
        return FurutaMesh.smooth(clamp01((p - 0.55) / 0.4));
    }

    /** How far from the wound (body pixels) the plates have spread. */
    public static double front(double p) {
        return REACH * FurutaMesh.smooth(clamp01((p - 0.12) / 0.8));
    }

    /** How hard he is screaming at {@code p} (0..1): up fast, held, and down. */
    public static double scream(double p) {
        double tick = p * PRELUDE_TICKS;
        double most = 0.0;
        for (int[] s : SCREAMS) {
            double into = tick - s[0];
            if (into < 0.0 || into > s[1]) {
                continue;
            }
            double rise = clamp01(into / 4.0);
            double fall = clamp01((s[1] - into) / 10.0);
            most = Math.max(most, Math.min(rise, fall));
        }
        return most;
    }

    /** How strong the last burst still is at {@code p} (1 as it goes off, gone in half a second). */
    public static double burst(double p) {
        double tick = p * PRELUDE_TICKS;
        double most = 0.0;
        for (int[] b : BURSTS) {
            double since = tick - b[0];
            if (since >= 0.0 && since < 10.0) {
                most = Math.max(most, (1.0 - since / 10.0) * b[1] / 10.0);
            }
        }
        return most;
    }

    // ================================================================== the body

    /** Everything of the turning that rides on {@code part}, in that part's own pixels, {@code p} into it at {@code time} (ticks). */
    public static void build(UkakuModel.Sink sink, WarModel.Part part, double time, double p) {
        build(sink, part, time, p, null);
    }

    /** ...with his kakugan where he placed it ({@code eye}, {@code WarModel.eyeOn}; null: the usual left eye). */
    public static void build(UkakuModel.Sink sink, WarModel.Part part, double time, double p, double[] eye) {
        if (p <= 0.0) {
            return;
        }
        double[] at = eye == null ? WarModel.DEFAULT_EYE : eye;
        WarModel.Body body = new WarModel.Body(sink, part);
        double front = front(p);
        // It shakes him harder the further it has gone, and with every scream and burst.
        double writhe = 0.5 + 1.5 * p + 1.5 * scream(p) + 2.0 * burst(p);
        switch (part) {
            case HEAD -> {
                kakugan(body, p, at);
                plates(body, HEAD_BOX, front, time, 10, at[0]);
                for (int i = 0; i < HEAD.length; i++) {
                    tendril(body, HEAD[i][0], HEAD[i], head(p), time, writhe, 20 + i, 0.95);
                }
            }
            case TORSO -> {
                wound(body, chest(p));
                plates(body, TORSO_BOX, front, time, 10, 0.0);
                for (int i = 0; i < CHEST.length; i++) {
                    double[] root = v(CHEST[i][0][0], CHEST[i][0][1], -2.0);
                    tendril(body, root, CHEST[i], chest(p) * (i < 2 ? 1.0 : FurutaMesh.smooth(clamp01(chest(p) * 1.4 - 0.4))), time, writhe, i, 1.25);
                }
                tail(body, tail(p), time, writhe);
            }
            case RIGHT_ARM -> plates(body, RIGHT_ARM_BOX, front, time, 7, 0.0);
            case LEFT_ARM -> plates(body, LEFT_ARM_BOX, front, time, 7, 0.0);
            case RIGHT_LEG -> plates(body, RIGHT_LEG_BOX, front, time, 7, 0.0);
            case LEFT_LEG -> plates(body, LEFT_LEG_BOX, front, time, 7, 0.0);
        }
    }

    /** His one kakugan (where he placed it, {@code at}): black, a red iris burning in it, open from the first second. */
    static void kakugan(WarModel.Body body, double p, double[] at) {
        double open = clamp01(p * 12.0);
        if (open <= 0.0) {
            return;
        }
        double z = -4.06;
        double[] c = {at[0], at[1], z};
        double w = 1.0 * open;
        body.shaded(v(c[0] - w, c[1] - 0.5, z), v(c[0] + w, c[1] - 0.5, z), v(c[0] + w, c[1] + 0.5, z), v(c[0] - w, c[1] + 0.5, z), v(c[0], c[1], 0.0),
            0xFF050304, 0xFF050304, 0xFF050304, 0xFF050304);
        double r = 0.42 * open;
        body.glow(v(c[0] - r, c[1] - r, z - 0.02), v(c[0] + r, c[1] - r, z - 0.02), v(c[0] + r, c[1] + r, z - 0.02), v(c[0] - r, c[1] + r, z - 0.02),
            IRIS, IRIS, CRACK, CRACK);
    }

    /** The wound the chest tendrils tear out of: torn flesh and blood round their roots. */
    static void wound(WarModel.Body body, double out) {
        if (out <= 0.0) {
            return;
        }
        double[] n = v(0, 0, -1);
        double r = 2.6 * FurutaMesh.smooth(clamp01(out * 2.0));
        WarModel.disc(body, v(0.0, 19.0, -2.08), n, v(0, 1, 0), 0.0, r, r * 1.25, FLESH, BLOOD, 14, false);
        WarModel.disc(body, v(0.0, 19.0, -2.12), n, v(0, 1, 0), 0.0, r * 0.55, r * 0.7, 0xFF1A0306, FLESH, 12, false);
        // Blood run down from it.
        for (int k = 0; k < 5; k++) {
            double x = -2.0 + k;
            double length = (2.0 + 3.0 * FurutaMesh.hash(k * 13 + 5)) * clamp01(out * 1.5 - 0.2);
            if (length < 0.2) {
                continue;
            }
            double top = 18.0 - Math.abs(x) * 0.4;
            body.shaded(v(x - 0.3, top, -2.09), v(x + 0.3, top, -2.09), v(x + 0.2, top - length, -2.09), v(x - 0.2, top - length, -2.09), v(x, top, 0.0), BLOOD,
                BLOOD, 0xFF3A040A, 0xFF3A040A);
        }
    }

    /**
     * The centipede plates on one body part: rings of black plates round it, each laid over the one below like a centipede's, coming
     * in where the spreading front ({@code front}, body pixels from the wound) has reached; red cracks glow on the skin just ahead. On
     * the head the face is left bare on the kakugan's side ({@code eyeX}: its x), the half mask growing over the other.
     */
    static void plates(WarModel.Body body, Box box, double front, double time, int around, double eyeX) {
        double ring = 2.3;
        for (double y = box.y1() - 0.6; y > box.y0() + 0.3; y -= ring) {
            for (int k = 0; k < around; k++) {
                double a = Math.PI * 2.0 * (k + 0.5 * ((int)Math.round(y / ring) % 2)) / around;
                double ca = Math.cos(a);
                double sa = Math.sin(a);
                // Round the box: a rounded square, and the way out from it there.
                double px = box.cx() + box.hx() * WarModel.sgnpow(ca, 0.5);
                double pz = box.cz() + box.hz() * WarModel.sgnpow(sa, 0.5);
                double[] out = normalize(v(ca / box.hx(), 0.0, sa / box.hz()));
                double[] at = v(px, y, pz);
                // His face stays his own but for a half mask over its right side: the scream and the kakugan show.
                if (box == HEAD_BOX && sa < -0.35 && px * Math.signum(eyeX) > -0.8 && y < 31.0) {
                    continue;
                }
                double d = Math.sqrt(dist2(at, ORIGIN));
                double in = clamp01((front - d) / 3.0);
                if (in > 0.02) {
                    double[] base = add(at, mul(out, 0.3));
                    double[] side = normalize(cross(out, v(0, 1, 0)));
                    double length = (2.5 + 0.3 * FurutaMesh.hash((int)(y * 7) * 31 + k)) * in;
                    // Laid flat over the one below, broad, a scale of a centipede's armour, not a spine.
                    WarModel.shard(body, base, add(v(0, -1, 0), mul(out, 0.12)), out, length, 2.0 * in + 0.2, 0.4, side, SHELL, SHELL_EDGE,
                        k % 3 == 0 ? GROOVE : 0, 3, true);
                }
                // The cracks ahead of it: red light running under the skin.
                double ahead = d - front;
                if (ahead > -1.0 && ahead < 5.0 && front > 0.5) {
                    double glow = 1.0 - Math.abs(ahead - 1.5) / 3.5;
                    double flicker = 0.75 + 0.25 * Math.sin(time * 0.9 + k * 1.7 + y);
                    int c = mix(0xFF200004, CRACK, clamp01(glow) * flicker);
                    double[] skin = add(at, mul(out, 0.12));
                    double[] along = normalize(add(v(0, 1, 0), mul(cross(out, v(0, 1, 0)), FurutaMesh.hash(k * 7 + (int)y) - 0.5)));
                    double[] across = mul(normalize(cross(out, along)), 0.14);
                    double[] end = add(skin, mul(along, 1.8));
                    body.glow(add(skin, across), add(end, across), sub(end, across), sub(skin, across), c, mix(c, 0xFF200004, 0.6), mix(c, 0xFF200004, 0.6), c);
                }
            }
        }
    }

    /**
     * One tendril tearing out of him: from {@code root} through the reach points ({@code shape}[1..]) as far as {@code out}, black
     * centipede segments with red seams over a crimson underside, writhing, ending in a hooked point.
     */
    static void tendril(WarModel.Body body, double[] root, double[][] shape, double out, double time, double writhe, int seed, double width) {
        if (out <= 0.02) {
            return;
        }
        List<double[]> pts = new ArrayList<>();
        pts.add(root);
        for (int k = 1; k < shape.length; k++) {
            double u = (double)k / (shape.length - 1);
            double[] full = add(root, v(shape[k][0], shape[k][1], shape[k][2]));
            double[] p = lerp(root, full, FurutaMesh.smooth(clamp01(out * 1.35 - u * 0.35)));
            double w = Math.pow(u, 1.2) * writhe * out;
            p = add(p, v(Math.sin(time * 0.31 + seed * 1.9 + u * 3.1) * w, Math.sin(time * 0.27 + seed * 2.3 + u * 2.6) * w * 0.8,
                Math.cos(time * 0.29 + seed * 0.8 + u * 2.9) * w));
            pts.add(p);
        }
        KakujaModel.Spine sp = WarModel.spine(pts, 36, v(0, 1, 0));
        double length = Math.max(0.6, sp.length());
        int segments = Math.max(3, (int)Math.round(length / 2.2));
        FurutaMesh.tube(new WarModel.SinkAdapter(body), sp, 0.0, 1.0, 36, 7, new FurutaMesh.Surface() {
            @Override
            public double radius(double s, double a) {
                double seg = (s * segments) % 1.0;
                double ridge = seg < 0.18 ? 0.88 : 1.0;
                double r = width * (1.0 - 0.6 * s) * ridge * Math.min(1.0, 0.4 + out);
                return r * (1.0 - Math.pow(clamp01((s - 0.8) / 0.2), 1.6));
            }

            @Override
            public int colour(double s, double a) {
                double seg = (s * segments) % 1.0;
                double under = Math.max(0.0, -Math.sin(a));
                int c = seg < 0.18 ? GROOVE : mix(SHELL, SHELL_EDGE, 0.5 * Math.max(0.0, Math.sin(a)));
                return mix(c, CRIMSON, clamp01(under * 1.3 - 0.2));
            }
        }, 1.0);
        // Legs down both sides, a pair to a segment: the centipede in him.
        for (int k = 1; k < segments; k++) {
            double s = (double)k / segments;
            if (s > 0.85) {
                break;
            }
            double[][] f = sp.at(s);
            double r = width * (1.0 - 0.6 * s);
            for (int e = -1; e <= 1; e += 2) {
                double[] flank = add(f[0], mul(f[3], e * r * 0.8));
                WarModel.shard(body, flank, add(mul(f[3], e), mul(f[2], -0.5)), mul(f[1], -1), (0.9 + 0.5 * FurutaMesh.hash(seed * 41 + k)) * out + 0.2, 0.35, 0.3,
                    f[2], SHELL, SHELL_EDGE, 0, 2);
            }
        }
    }

    /** The tail tearing out of his lower back: thick, segmented, a pair of legs to a segment, curling up behind him. */
    static void tail(WarModel.Body body, double out, double time, double writhe) {
        if (out <= 0.02) {
            return;
        }
        double[] root = v(TAIL[0][0], TAIL[0][1], TAIL[0][2]);
        List<double[]> pts = new ArrayList<>();
        for (int k = 0; k < TAIL.length; k++) {
            double u = (double)k / (TAIL.length - 1);
            double[] p = lerp(root, v(TAIL[k][0], TAIL[k][1], TAIL[k][2]), FurutaMesh.smooth(clamp01(out * 1.3 - u * 0.3)));
            double w = u * writhe * 0.7 * out;
            p = add(p, v(Math.sin(time * 0.21 + u * 2.4) * w * 1.6, Math.sin(time * 0.17 + u * 3.0) * w * 0.5, 0.0));
            pts.add(p);
        }
        KakujaModel.Spine sp = WarModel.spine(pts, 48, v(0, 1, 0));
        FurutaMesh.tube(new WarModel.SinkAdapter(body), sp, 0.0, 1.0, 48, 8, new FurutaMesh.Surface() {
            @Override
            public double radius(double s, double a) {
                double seg = (s * TAIL_SEGMENTS) % 1.0;
                double r = (2.2 - 1.4 * s) * (seg < 0.15 ? 0.86 : 1.0) * Math.min(1.0, 0.3 + out);
                double flat = 0.85 + 0.15 * Math.abs(Math.sin(a));
                return r * flat * (1.0 - Math.pow(clamp01((s - 0.86) / 0.14), 1.5));
            }

            @Override
            public int colour(double s, double a) {
                double seg = (s * TAIL_SEGMENTS) % 1.0;
                double under = Math.max(0.0, -Math.sin(a));
                int c = seg < 0.15 ? GROOVE : mix(SHELL, SHELL_EDGE, 0.6 * Math.max(0.0, Math.sin(a)));
                return mix(c, FLESH, clamp01(under * 1.4 - 0.3));
            }
        }, 1.0);
        for (int k = 1; k < TAIL_SEGMENTS; k++) {
            double s = (double)k / TAIL_SEGMENTS;
            if (s > out * 0.95) {
                break;
            }
            double[][] f = sp.at(s);
            double r = 2.2 - 1.4 * s;
            double kick = Math.sin(time * 0.5 + k * 0.9) * 0.4;
            for (int e = -1; e <= 1; e += 2) {
                double[] flank = add(f[0], mul(f[3], e * r * 0.85));
                double[] dir = add(add(mul(f[3], e), mul(f[2], -0.8)), mul(f[1], kick));
                WarModel.shard(body, flank, dir, mul(f[1], -1), (2.2 - s) * out + 0.3, 0.45, 0.35, f[2], SHELL, SHELL_EDGE, 0, 3);
            }
        }
    }

    static double dist2(double[] a, double[] b) {
        double dx = a[0] - b[0];
        double dy = a[1] - b[1];
        double dz = a[2] - b[2];
        return dx * dx + dy * dy + dz * dz;
    }
}
