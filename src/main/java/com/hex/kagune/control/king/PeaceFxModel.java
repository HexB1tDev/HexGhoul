package com.hex.kagune.control.king;

import static com.hex.kagune.control.furuta.FurutaMesh.add;
import static com.hex.kagune.control.furuta.FurutaMesh.clamp01;
import static com.hex.kagune.control.furuta.FurutaMesh.cross;
import static com.hex.kagune.control.furuta.FurutaMesh.dot;
import static com.hex.kagune.control.furuta.FurutaMesh.mix;
import static com.hex.kagune.control.furuta.FurutaMesh.mul;
import static com.hex.kagune.control.furuta.FurutaMesh.normalize;
import static com.hex.kagune.control.furuta.FurutaMesh.sub;

import com.hex.kagune.control.ayato.KakujaModel;
import com.hex.kagune.control.ayato.UkakuModel;
import com.hex.kagune.control.furuta.FurutaMesh;
import java.util.List;

/**
 * 1.0.34: the shapes Peace's reworked abilities put out into the world (pure, in blocks, world space): Aegis of :Re's three shield
 * tendrils (a tendril of its kagune, white plates down it, a great white shield at its end), White Wing Ascent's two lifting tendrils,
 * and the red flesh tendrils its landing tears up out of the ground ({@link #eruption} lays them out the same on every side).
 */
public final class PeaceFxModel {
    private PeaceFxModel() {
    }

    /** A lit quad in world space (its normal worked out, faced away from {@code centre}; never the glow's straight up). */
    static void shaded(UkakuModel.Sink sink, double[] a, double[] b, double[] c, double[] d, double[] centre, int ca, int cb, int cc, int cd) {
        double[] n = cross(sub(c, a), sub(d, b));
        if (dot(n, n) < 1.0E-12) {
            n = cross(sub(b, a), sub(c, a));
            if (dot(n, n) < 1.0E-12) {
                return;
            }
        }
        n = normalize(n);
        double[] mid = mul(add(add(a, b), add(c, d)), 0.25);
        if (dot(n, sub(mid, centre)) < 0.0) {
            n = mul(n, -1.0);
        }
        if (n[0] == 0.0 && n[1] == 1.0 && n[2] == 0.0) {
            n = normalize(new double[] {1.0E-4, 1.0, 1.0E-4});
        }
        sink.quad(a, b, c, d, n, solid(ca), solid(cb), solid(cc), solid(cd));
    }

    static int solid(int colour) {
        return colour >>> 24 == 0 ? colour | 0xFF000000 : colour;
    }

    /** A glowing quad (full bright), seen from both sides by the caller's glow pass. */
    static void glow(UkakuModel.Sink sink, double[] a, double[] b, double[] c, double[] d, int ca, int cb, int cc, int cd) {
        sink.quad(a, b, c, d, new double[] {0.0, 1.0, 0.0}, solid(ca), solid(cb), solid(cc), solid(cd));
    }

    // ================================================================== a tendril of its kagune

    /**
     * A tendril of Peace's kagune along {@code control} (blocks; root first): crimson, its fibres twisting down it, thick at the root
     * ({@code r0}) and thinner toward its end ({@code r1}), white plates down it every so often, growing out {@code grow} of its length.
     */
    public static KakujaModel.Spine tendril(UkakuModel.Sink sink, List<double[]> control, double r0, double r1, double grow, double time, int seed,
                                            int sides) {
        if (control.size() < 2 || grow <= 0.01) {
            return null;
        }
        KakujaModel.Spine sp = WarModel.spine(new java.util.ArrayList<>(control), 24, new double[] {0.0, 1.0, 0.0});
        double end = clamp01(grow);
        double crawl = time * 0.06;
        FurutaMesh.tube(sink, sp, 0.0, end, Math.max(6, (int)Math.round(22 * end)), sides, new FurutaMesh.Surface() {
            @Override
            public double radius(double s, double a) {
                double t = s / Math.max(1.0E-6, end);
                double swell = 1.0 + 0.12 * Math.sin(s * 19.0 - time * 0.3 + seed);
                return (r0 + (r1 - r0) * s) * swell * (t > 0.92 ? Math.max(0.35, 1.0 - (t - 0.92) / 0.08 * 0.65) : 1.0);
            }

            @Override
            public int colour(double s, double a) {
                return PeaceModel.flesh(s, a - crawl, seed);
            }
        }, 1.0);
        // White plates down it, overlapping like the blade arms'.
        int plates = Math.max(2, (int)Math.round(sp.length() / 0.55));
        for (int k = 1; k < plates; k++) {
            double s = (double)k / plates;
            if (s > end - 0.06) {
                break;
            }
            double[][] f = sp.at(s);
            double r = r0 + (r1 - r0) * s;
            double around = seed * 1.7 + k * 2.2;
            double[] out = normalize(add(mul(f[2], Math.cos(around)), mul(f[3], Math.sin(around))));
            plate(sink, add(f[0], mul(out, r * 0.78)), f[1], out, r * 1.5, r * 1.15);
        }
        return sp;
    }

    /** A small curved white plate at {@code at} lying along {@code along}, its face out along {@code out}. */
    static void plate(UkakuModel.Sink sink, double[] at, double[] along, double[] out, double length, double width) {
        double[] d = normalize(sub(along, mul(out, dot(along, out))));
        double[] side = normalize(cross(out, d));
        double[] a = sub(at, mul(d, length * 0.45));
        double[] b = add(at, mul(d, length * 0.55));
        double[] l = add(at, mul(side, width * 0.5));
        double[] r = sub(at, mul(side, width * 0.5));
        double[] lift = mul(out, width * 0.18);
        shaded(sink, a, add(l, lift), b, add(r, lift), sub(at, out), PeaceModel.WHITE, PeaceModel.SILVER, PeaceModel.ICE, PeaceModel.SILVER);
        shaded(sink, a, add(r, lift), b, add(l, lift), sub(at, out), PeaceModel.SILVER, PeaceModel.SHADOW, PeaceModel.SILVER, PeaceModel.SHADOW);
    }

    // ================================================================== 1.0.36: Piercing Salvation's lance: the six closed into one cone

    /** How far a lance's blades lean in to its axis (radians): their points meet at its tip, their bases spread round it. */
    public static final double CONE_LEAN = Math.toRadians(13.0);

    /**
     * The lance the six blades close into, its tip at {@code tip} (world) along {@code way} (unit), the blades {@code size} (their
     * aims'): each blade round the axis (a sixth of a turn apart), leaning in so its long point is at the tip, its flat turned out (a
     * cone's petals, closed). Per blade {the way it points (unit), its face (unit), how far back from the tip its heart is (blocks)}.
     */
    public static double[][] lanceCone(double[] way, double size) {
        double[] w = normalize(way);
        double[] any = Math.abs(w[1]) < 0.9 ? new double[] {0.0, 1.0, 0.0} : new double[] {1.0, 0.0, 0.0};
        double[] u1 = normalize(cross(w, any));
        double[] u2 = normalize(cross(w, u1));
        double[][] out = new double[PeaceMotion.BLADES][];
        for (int blade = 0; blade < PeaceMotion.BLADES; blade++) {
            double a = Math.PI / 6.0 + blade * Math.PI / 3.0;
            double[] radial = add(mul(u1, Math.cos(a)), mul(u2, Math.sin(a)));
            double[] d = normalize(sub(mul(w, Math.cos(CONE_LEAN)), mul(radial, Math.sin(CONE_LEAN))));
            double reach = PeaceModel.longPoint(blade) * PeaceMotion.size(blade) * size / PeaceMotion.BLOCK;
            out[blade] = new double[] {d[0], d[1], d[2], radial[0], radial[1], radial[2], reach * 0.92};
        }
        return out;
    }

    // ================================================================== 1.0.36: the Waltz's blades, each its own way in

    /**
     * Where Crossblade Waltz's blade {@code blade} goes into a body {@code width} wide and {@code height} tall standing at {@code feet}
     * (world): on its own side of it (his right's blades its right, his left's its left), at its own height (the upper pair the chest,
     * the middle the waist, the lower the hips), {@code right} his right (level, unit).
     */
    public static double[] waltzEntry(int blade, double[] feet, double width, double height, double[] right) {
        int r = PeaceMotion.right(blade);
        int tier = PeaceMotion.tier(blade);
        double share = tier == 0 ? 0.7 : tier == 1 ? 0.52 : 0.34;
        double off = r * width * 0.3;
        return new double[] {feet[0] + right[0] * off, feet[1] + height * share, feet[2] + right[2] * off};
    }

    /**
     * The way blade {@code blade} is driven in (unit, world), {@code ahead} the way from him to the body (unit): in from its own side,
     * slanting across through it (his right's to the left, his left's to the right: they cross inside it), the upper ones down into it,
     * the lower ones up.
     */
    public static double[] waltzWay(int blade, double[] ahead, double[] right) {
        int r = PeaceMotion.right(blade);
        int tier = PeaceMotion.tier(blade);
        double slant = tier == 1 ? 0.75 : 0.55;
        double tilt = tier == 0 ? -0.4 : tier == 2 ? 0.35 : 0.0;
        return normalize(new double[] {ahead[0] - right[0] * r * slant, ahead[1] + tilt, ahead[2] - right[2] * r * slant});
    }

    // ================================================================== Aegis of :Re's shield

    /**
     * A great shield at {@code centre} facing {@code face} (out from him), its top toward {@code up}, {@code size} blocks tall: a white
     * heater shape (square shoulders, coming to a point below), bowed out, as thick as a hand, silver-rimmed, a crimson cross down and
     * across its face with a red-hot heart where the arms meet, a ridge down its middle.
     */
    public static void shield(UkakuModel.Sink sink, double[] centre, double[] face, double[] up, double size, double time) {
        if (size < 0.05) {
            return;
        }
        double[] n = normalize(face);
        double[] u = normalize(sub(up, mul(n, dot(up, n))));
        double[] s = normalize(cross(u, n));
        double h = size;
        double w = size * 0.78;
        double bow = size * 0.12;
        double thick = size * 0.07;
        int cols = 8;
        int rows = 8;
        double[][][] front = new double[cols + 1][rows + 1][];
        double[][][] back = new double[cols + 1][rows + 1][];
        for (int i = 0; i <= cols; i++) {
            double a = -1.0 + 2.0 * i / cols;
            for (int j = 0; j <= rows; j++) {
                double v = (double)j / rows;
                double half = outline(v);
                double x = a * half;
                double[] p = add(add(centre, mul(s, x * w * 0.5)), mul(u, (0.45 - v) * h));
                double dome = bow * (1.0 - x * x) * (1.0 - 0.5 * v) - Math.abs(x) * thick * 0.6;
                front[i][j] = add(p, mul(n, dome + thick * 0.5));
                back[i][j] = add(p, mul(n, dome - thick * 0.5));
            }
        }
        double[] inside = sub(centre, mul(n, size * 2.0));
        double[] outside = add(centre, mul(n, size * 2.0));
        double beat = Math.pow(Math.max(0.0, Math.sin(time * 0.11)), 6.0);
        for (int i = 0; i < cols; i++) {
            for (int j = 0; j < rows; j++) {
                double a = -1.0 + 2.0 * (i + 0.5) / cols;
                double v = (j + 0.5) / rows;
                boolean rim = i == 0 || i == cols - 1 || j == 0 || Math.abs(a) * outline(v) > 0.86;
                boolean cross = Math.abs(a) < 0.16 || Math.abs(v - 0.3) < 0.07;
                int c = rim ? PeaceModel.SILVER : cross ? mix(PeaceModel.CRIMSON_L, PeaceModel.BLADE_RED, 0.5) : mix(PeaceModel.WHITE, PeaceModel.RIM, Math.abs(a) * 0.6 + v * 0.3);
                int c2 = rim ? PeaceModel.SHADOW : cross ? PeaceModel.CRIMSON : mix(PeaceModel.WHITE, PeaceModel.SILVER, Math.abs(a) * 0.7 + v * 0.3);
                shaded(sink, front[i][j], front[i + 1][j], front[i + 1][j + 1], front[i][j + 1], inside, c, c, c2, c2);
                shaded(sink, back[i][j], back[i][j + 1], back[i + 1][j + 1], back[i + 1][j], outside, PeaceModel.SHADOW, PeaceModel.UNDER_L, PeaceModel.UNDER_L,
                    PeaceModel.SHADOW);
            }
        }
        // Its rim, all round between the faces.
        for (int i = 0; i < cols; i++) {
            shaded(sink, front[i][0], front[i + 1][0], back[i + 1][0], back[i][0], centre, PeaceModel.SILVER, PeaceModel.SILVER, PeaceModel.SHADOW, PeaceModel.SHADOW);
        }
        for (int j = 0; j < rows; j++) {
            shaded(sink, front[0][j], front[0][j + 1], back[0][j + 1], back[0][j], centre, PeaceModel.SILVER, PeaceModel.SILVER, PeaceModel.SHADOW, PeaceModel.SHADOW);
            shaded(sink, front[cols][j], front[cols][j + 1], back[cols][j + 1], back[cols][j], centre, PeaceModel.SILVER, PeaceModel.SILVER, PeaceModel.SHADOW,
                PeaceModel.SHADOW);
        }
        // The heart where the cross's arms meet, red-hot, beating.
        double[] heart = add(add(centre, mul(u, (0.45 - 0.3) * h)), mul(n, bow * 0.85 + thick * 0.5 + 0.02));
        int core = mix(PeaceModel.CORE, 0xFFFF9AA2, 0.5 * beat);
        double r = size * (0.09 + 0.02 * beat);
        for (int k = 0; k < 8; k++) {
            double a0 = Math.PI * 2.0 * k / 8;
            double a1 = Math.PI * 2.0 * (k + 1) / 8;
            double[] p0 = add(heart, add(mul(s, Math.cos(a0) * r), mul(u, Math.sin(a0) * r)));
            double[] p1 = add(heart, add(mul(s, Math.cos(a1) * r), mul(u, Math.sin(a1) * r)));
            glow(sink, heart, p0, p1, heart, 0xFFFFFFFF, core, core, 0xFFFFFFFF);
        }
    }

    /** Where Aegis shield {@code i} rests (world): over his head, or out at his side; {@code chest} his chest, his {@code ahead} and {@code right}. */
    public static double[] shieldRest(int i, double[] chest, double[] ahead, double[] right, double time) {
        double side = i == PeaceRules.SHIELD_LEFT ? -1.0 : i == PeaceRules.SHIELD_RIGHT ? 1.0 : 0.0;
        double sway = Math.sin(time * 0.07 + i * 2.1);
        double[] up = {0.0, 1.0, 0.0};
        return i == PeaceRules.SHIELD_TOP ? add(add(chest, mul(up, 1.05 + 0.05 * sway)), mul(ahead, 0.45))
            : add(add(add(chest, mul(right, side * (1.3 + 0.05 * sway))), mul(ahead, 0.3)), mul(up, -0.1));
    }

    /**
     * Aegis shield {@code i}'s tendril out of his back (between the shoulders) round to {@code anchor} (world), and the shield on its
     * end there facing out from his chest ({@code shown}: drawn), out {@code grow}.
     */
    public static void aegisShield(UkakuModel.Sink sink, int i, double[] chest, double[] ahead, double[] right, double[] anchor, double grow, double time,
                                   boolean shown, int sides) {
        double side = i == PeaceRules.SHIELD_LEFT ? -1.0 : i == PeaceRules.SHIELD_RIGHT ? 1.0 : 0.0;
        double[] up = {0.0, 1.0, 0.0};
        double[] face = normalize(new double[] {anchor[0] - chest[0], i == PeaceRules.SHIELD_TOP ? anchor[1] - chest[1] : (anchor[1] - chest[1]) * 0.3,
            anchor[2] - chest[2]});
        double[] shieldUp = i == PeaceRules.SHIELD_TOP && Math.abs(face[1]) > 0.7 ? ahead : up;
        double[] root = add(add(add(chest, mul(ahead, -0.25)), mul(up, 0.2)), mul(right, side * 0.12));
        double[] out = add(add(add(root, mul(ahead, -0.45)), mul(up, i == PeaceRules.SHIELD_TOP ? 0.6 : 0.25)), mul(right, side * 0.4));
        double[] hold = sub(anchor, mul(face, 0.25));
        double[] mid = add(add(mul(add(out, hold), 0.5), mul(up, 0.2)), mul(right, side * 0.3));
        tendril(sink, List.of(root, out, mid, hold), 0.17, 0.11, grow, time, 30 + i * 7, sides);
        if (shown) {
            shield(sink, add(anchor, mul(face, 0.04)), face, shieldUp, 1.15 * grow, time);
        }
    }

    /** One of White Wing Ascent's two tendrils ({@code side} -1 his left, +1 his right): out of his back, arching out, down to {@code foot}. */
    public static void liftTendril(UkakuModel.Sink sink, int side, double[] chest, double[] ahead, double[] right, double[] foot, double grow, double time,
                                   int sides) {
        double[] up = {0.0, 1.0, 0.0};
        double[] root = add(add(add(chest, mul(ahead, -0.22)), mul(right, side * 0.22)), mul(up, 0.1));
        double[] out = add(add(add(root, mul(right, side * 0.9)), mul(up, 0.35)), mul(ahead, -0.35));
        double[] mid = add(mul(add(root, foot), 0.5), mul(right, side * 1.1));
        tendril(sink, List.of(root, out, mid, foot), 0.24, 0.15, grow, time, 50 + side, sides);
    }

    /** How wide the shield is {@code v} down it (0 its top, 1 its point), of its full width. */
    static double outline(double v) {
        if (v < 0.5) {
            return 1.0 - 0.06 * (0.5 - v);
        }
        double k = (v - 0.5) / 0.5;
        return Math.max(0.0, 1.0 - Math.pow(k, 1.6));
    }

    // ================================================================== the flesh torn up out of the ground

    /** What the eruption lays out per tendril: its place {dx, dz} (blocks off where he landed), height, thickness, lean (way, how far),
     *  curl, and how far out it is (for when it comes up). */
    public static final int ERUPT_FIELDS = 8;

    /**
     * The eruption's tendrils, laid out from {@code seed} the same on the server and every client: {@code count} of them over a disc
     * {@code radius} round where he landed (spread evenly by area, none right under him), each a different height, thickness and shape:
     * straight up, leaning, curving over or out.
     */
    public static double[][] eruption(long seed, int count, double radius) {
        double[][] out = new double[count][];
        java.util.SplittableRandom random = new java.util.SplittableRandom(seed);
        for (int i = 0; i < count; i++) {
            double far = radius * Math.sqrt(0.02 + 0.98 * random.nextDouble());
            double a = random.nextDouble() * Math.PI * 2.0;
            double big = random.nextDouble();
            double height = 2.0 + 7.5 * big * big + random.nextDouble() * 1.5;
            double thick = 0.22 + 0.5 * big + random.nextDouble() * 0.15;
            double way = random.nextDouble() * Math.PI * 2.0;
            double kind = random.nextDouble();
            double lean = kind < 0.35 ? random.nextDouble() * 0.15 : kind < 0.75 ? 0.25 + random.nextDouble() * 0.45 : 0.6 + random.nextDouble() * 0.5;
            double curl = (random.nextDouble() - 0.5) * (kind < 0.75 ? 0.8 : 2.2);
            out[i] = new double[] {Math.cos(a) * far, Math.sin(a) * far, height, thick, way, lean, curl, far};
        }
        return out;
    }

    /** One eruption tendril's centre line (blocks): from {@code base} on the ground up along its lean and curl, {@code t} its layout. */
    public static double[][] eruptionPath(double[] base, double[] t, int points) {
        double h = t[2];
        double way = t[4];
        double lean = t[5];
        double curl = t[6];
        double[][] p = new double[points][];
        for (int k = 0; k < points; k++) {
            double u = (double)k / (points - 1);
            double tilt = lean * u + curl * u * u * 0.6;
            double turn = way + curl * u * 0.8;
            double out = h * Math.sin(Math.min(1.4, tilt)) * u * 0.9;
            double y = h * u * Math.cos(Math.min(1.4, tilt * 0.7));
            p[k] = new double[] {base[0] + Math.cos(turn) * out, base[1] - 0.6 + y + 0.6 * Math.min(1.0, u * 4.0), base[2] + Math.sin(turn) * out};
        }
        return p;
    }

    /**
     * A red flesh tendril along {@code path} (blocks, its base in the ground first), {@code thick} at its base and coming to a point, out
     * {@code grow} of its length (rising up out of the ground, sinking back), its end bent over to {@code lash} (null: none) by
     * {@code lashing} 0..1; living: a pulse runs up it, veins dark round it.
     */
    public static void flesh(UkakuModel.Sink sink, double[][] path, double thick, double grow, double[] lash, double lashing, double time, int seed,
                             int sides) {
        if (grow <= 0.02 || path.length < 2) {
            return;
        }
        double[][] p = path.clone();
        if (lash != null && lashing > 0.0) {
            int n = p.length;
            for (int k = n / 3; k < n; k++) {
                double u = (k - n / 3.0) / Math.max(1.0, n - 1 - n / 3.0);
                double w = lashing * u * u * (3.0 - 2.0 * u);
                p[k] = add(mul(p[k], 1.0 - w), mul(lash, w));
            }
        }
        List<double[]> control = new java.util.ArrayList<>(java.util.Arrays.asList(p));
        KakujaModel.Spine sp = WarModel.spine(control, 20, new double[] {1.0, 0.0, 0.0});
        double end = clamp01(grow);
        FurutaMesh.tube(sink, sp, 0.0, end, Math.max(4, (int)Math.round(18 * end)), sides, new FurutaMesh.Surface() {
            @Override
            public double radius(double s, double a) {
                double t = s / Math.max(1.0E-6, end);
                double pulse = 1.0 + 0.1 * Math.sin(s * 14.0 - time * 0.5 + seed);
                return thick * Math.pow(Math.max(0.0, 1.0 - s), 0.7) * pulse * (0.82 + 0.18 * Math.cos(a * 3.0 + s * 9.0 + seed)) * (t > 0.95 ? 0.6 : 1.0);
            }

            @Override
            public int colour(double s, double a) {
                double vein = Math.sin(a * 4.0 + s * 21.0 + seed * 0.7);
                int c = vein > 0.55 ? 0xFF3A040A : mix(0xFF8E0E1A, 0xFFC22A30, 0.5 + 0.5 * Math.sin(a + seed));
                return mix(c, 0xFFE8505A, 0.25 * Math.max(0.0, Math.sin(s * 10.0 - time * 0.4 + seed)) * (1.0 - s));
            }
        }, 1.0);
    }

    // ================================================================== Rebirth Instinct's creatures

    /** 1.2.3: a creature's model pixel (blocks): Steve's 32 pixels to its 1.95 blocks. */
    static final double PX = 1.95 / 32.0;

    /**
     * One of Rebirth Instinct's creatures: a man of living flesh and kagune. 1.2.3: in the shape of the player's own model (the user's
     * ask): Steve's head, body, arms and legs, box for box, each of crimson fibre; its ribs and the tops of its shoulders white plates as
     * its kagune's, two red-hot eyes where Steve's are. At {@code feet} turned {@code yaw} (radians, as the game turns a body: ahead is
     * (-sin, 0, cos)); walking {@code walking} (0..1) at {@code stride} (radians), its limbs swinging as a player's do; {@code blades}
     * (0..1) its forearms grown out into long curved blades, both arms held up before it; {@code swing} (0..1 through one, NaN: none) a
     * cut of the {@code side} arm (+1 its right) from high over it down and across it low; {@code rise} (0..1) climbing up out of the ground
     * (out of a pool of itself), {@code melt} (0..1) slumping and melting back down into it.
     */
    public static void thrall(UkakuModel.Sink sink, double[] feet, double yaw, double stride, double walking, double blades, double swing, int side,
                              double rise, double melt, double time, int seed) {
        double[] f = {-Math.sin(yaw), 0.0, Math.cos(yaw)};
        double[] r = {-Math.cos(yaw), 0.0, -Math.sin(yaw)};
        double[] u = {0.0, 1.0, 0.0};
        double up = smooth(rise);
        double down = smooth(melt);
        double sunk = (1.0 - up) * 2.15 + down * 1.75;
        double[] base = sub(feet, mul(u, sunk));
        // Melting, it slumps: shorter, wider, bowed over.
        double squash = 1.0 - 0.35 * down;
        double swell = 1.0 + 0.3 * down;
        double hunch = 0.08 + 0.55 * down + 0.1 * (1.0 - up);
        double breath = 0.012 * Math.sin(time * 0.12 + seed);
        double pool = Math.max(1.0 - up, down);
        if (pool > 0.02) {
            puddle(sink, add(feet, mul(u, 0.03)), 0.35 + 0.65 * pool, time, seed);
        }
        double walk = 0.9 * walking;
        // Legs: from the hips (12 pixels up), swinging as a player's.
        double[] hips = at(base, r, u, f, 0.0, 12.0 * PX * squash, 0.0);
        for (int s2 = -1; s2 <= 1; s2 += 2) {
            double pitch = walk * Math.sin(stride + (s2 > 0 ? 0.0 : Math.PI)) + 0.25 * down * s2;
            double[][] axes = limb(r, u, f, pitch, 0.0, 0.0);
            double[] pivot = add(hips, mul(r, s2 * 2.0 * PX * swell));
            box(sink, pivot, axes, -2, 2, -12 * squash, 0, -2, 2, swell, time, seed + 5 + s2, 2);
        }
        // Body: 8 by 12 by 4, bowed forward from the hips as it hunches.
        double[][] trunk = limb(r, u, f, -hunch, 0.0, 0.0);
        double[] chestTop = add(hips, mul(trunk[1], (12.0 + breath / PX) * PX * squash));
        box(sink, hips, trunk, -4, 4, 0, 12 * squash, -2, 2, swell, time, seed, 2);
        // Its ribs, white, as its kagune's, across the front of the chest; plates over the shoulders.
        for (int k = 0; k < 3; k++) {
            for (int s2 = -1; s2 <= 1; s2 += 2) {
                double[] rib = add(hips, add(add(mul(trunk[0], s2 * 2.2 * PX * swell), mul(trunk[1], (5.5 + 1.9 * k) * PX * squash)),
                    mul(trunk[2], 2.15 * PX * swell)));
                plate(sink, rib, add(mul(trunk[0], s2), mul(trunk[1], -0.45)), normalize(add(trunk[2], mul(trunk[0], s2 * 0.35))), 0.17, 0.055);
            }
        }
        // Head: 8 by 8 by 8 on the neck, two eyes burning in it where Steve's are.
        double[][] head = limb(r, u, f, -hunch * 0.6 - 0.1 * down, 0.15 * Math.sin(time * 0.03 + seed) * (1.0 - down), 0.0);
        box(sink, chestTop, head, -4, 4, 0, 8 * squash, -4, 4, swell, time, seed + 3, 2);
        for (int s2 = -1; s2 <= 1; s2 += 2) {
            double[] eye = add(chestTop, add(add(mul(head[0], s2 * 2.0 * PX * swell), mul(head[1], 3.5 * PX * squash)), mul(head[2], 4.08 * PX * swell)));
            double ex = 0.9 * PX;
            double ey = 0.55 * PX;
            glow(sink, add(eye, add(mul(head[0], -ex), mul(head[1], -ey))), add(eye, add(mul(head[0], ex), mul(head[1], -ey))),
                add(eye, add(mul(head[0], ex), mul(head[1], ey))), add(eye, add(mul(head[0], -ex), mul(head[1], ey))), PeaceModel.CORE, PeaceModel.CORE,
                0xFFFF9AA2, 0xFFFF9AA2);
        }
        // Arms: from the shoulders (2 pixels under the top of the body), swinging as it walks; with the blades out both held up before it;
        // the one cutting raised high over it, then down and across it low.
        double guard = smooth(blades);
        for (int s2 = -1; s2 <= 1; s2 += 2) {
            double hang = -walk * Math.sin(stride + (s2 > 0 ? 0.0 : Math.PI)) + 0.3 * down;
            double pitch = hang + (-1.35 - hang) * guard;
            double turn = -0.25 * s2 * guard;
            double roll = 0.06 * s2 * (1.0 - guard);
            if (!Double.isNaN(swing) && s2 == side) {
                double wind = smooth(swing / 0.3);
                double cut = smooth((swing - 0.3) / 0.2);
                double back = smooth((swing - 0.55) / 0.45);
                double high = -2.7;
                double low = -0.55;
                pitch = mix1(mix1(mix1(pitch, high, wind), low, cut), pitch, back);
                turn = mix1(mix1(turn, 0.0, wind), 0.75 * s2, cut) * (1.0 - back) + turn * back;
            }
            double[][] axes = limb(trunk[0], trunk[1], trunk[2], pitch, turn, roll);
            double[] shoulder = add(hips, add(mul(trunk[0], s2 * 6.0 * PX * swell), mul(trunk[1], 10.0 * PX * squash)));
            box(sink, shoulder, axes, -2, 2, -10 * squash, 2, -2, 2, swell, time, seed + 9 + s2, 2);
            double[] top = add(shoulder, add(mul(axes[1], 2.1 * PX), mul(axes[0], s2 * 0.6 * PX)));
            plate(sink, top, add(mul(axes[0], s2), mul(axes[1], -0.3)), normalize(add(axes[1], mul(axes[0], s2 * 0.6))), 0.2, 0.13);
            if (blades > 0.02) {
                double[] elbow = add(shoulder, mul(axes[1], -4.5 * PX * squash));
                double[] hand = add(shoulder, mul(axes[1], -10.0 * PX * squash));
                // Its edge out and under the forearm: broad to the eye from before it and from the side, held up or cutting.
                armBlade(sink, elbow, hand, normalize(sub(mul(axes[0], s2), axes[2])), blades, seed + s2);
            }
        }
    }

    /**
     * A limb's own axes {right, up (from its end to its pivot), ahead}: hanging down from its pivot, swung forward by {@code pitch} (radians,
     * + back, - forward and up, as vanilla's xRot), turned {@code turn} about its body's up (+ toward its left), rolled {@code roll} out.
     */
    private static double[][] limb(double[] r, double[] u, double[] f, double pitch, double turn, double roll) {
        // Turned about up, then swung about its right, then rolled about its ahead.
        double[] r1 = add(mul(r, Math.cos(turn)), mul(f, Math.sin(turn)));
        double[] f1 = sub(mul(f, Math.cos(turn)), mul(r, Math.sin(turn)));
        double[] u2 = add(mul(u, Math.cos(pitch)), mul(f1, Math.sin(pitch)));
        double[] f2 = sub(mul(f1, Math.cos(pitch)), mul(u, Math.sin(pitch)));
        double[] r3 = add(mul(r1, Math.cos(roll)), mul(u2, Math.sin(roll)));
        double[] u3 = sub(mul(u2, Math.cos(roll)), mul(r1, Math.sin(roll)));
        return new double[][] {r3, u3, f2};
    }

    private static double mix1(double a, double b, double t) {
        return a + (b - a) * t;
    }

    /**
     * A box of its flesh at {@code pivot} in {@code axes} ({right, up, ahead}): from ({@code x0}, {@code y0}, {@code z0}) to ({@code x1},
     * {@code y1}, {@code z1}) model pixels, widened {@code swell} (not along its length), each face a grid of {@code cell}-pixel cells of
     * crimson fibre, crawling.
     */
    private static void box(UkakuModel.Sink sink, double[] pivot, double[][] axes, double x0, double x1, double y0, double y1, double z0, double z1,
                            double swell, double time, int seed, int cell) {
        double[] centre = point(pivot, axes, (x0 + x1) * 0.5, (y0 + y1) * 0.5, (z0 + z1) * 0.5, swell);
        double[][] lo = {{x0, y0, z0}, {x1, y1, z1}};
        // Each face: the axis it faces along, which end, and the two axes across it.
        int[][] faces = {{0, 0, 1, 2}, {0, 1, 1, 2}, {1, 0, 0, 2}, {1, 1, 0, 2}, {2, 0, 0, 1}, {2, 1, 0, 1}};
        for (int[] face : faces) {
            int along = face[0];
            int a = face[2];
            int b = face[3];
            double fixed = lo[face[1]][along];
            double a0 = lo[0][a];
            double a1 = lo[1][a];
            double b0 = lo[0][b];
            double b1 = lo[1][b];
            int na = Math.max(1, (int)Math.round(Math.abs(a1 - a0) / cell));
            int nb = Math.max(1, (int)Math.round(Math.abs(b1 - b0) / cell));
            for (int i = 0; i < na; i++) {
                for (int j = 0; j < nb; j++) {
                    double[][] corners = new double[4][];
                    double[][] uv = {{i, j}, {i + 1, j}, {i + 1, j + 1}, {i, j + 1}};
                    for (int k = 0; k < 4; k++) {
                        double[] c = new double[3];
                        c[along] = fixed;
                        c[a] = a0 + (a1 - a0) * uv[k][0] / na;
                        c[b] = b0 + (b1 - b0) * uv[k][1] / nb;
                        corners[k] = point(pivot, axes, c[0], c[1], c[2], swell);
                    }
                    // Its fibre as the old body's: by where on the face, and which face, so each box reads as one muscle.
                    double s1 = (j + 0.5) / nb;
                    double s2 = (i + 0.5) / na + along * 1.7 + face[1] * 0.9;
                    int colour = PeaceModel.flesh(s1 * 0.55, s2 * 2.4 - time * 0.02, seed);
                    shaded(sink, corners[0], corners[1], corners[2], corners[3], centre, colour, colour, colour, colour);
                }
            }
        }
    }

    /** ({@code x}, {@code y}, {@code z}) model pixels off {@code pivot} in {@code axes}, widened {@code swell} across. */
    private static double[] point(double[] pivot, double[][] axes, double x, double y, double z, double swell) {
        return add(pivot, add(add(mul(axes[0], x * PX * swell), mul(axes[1], y * PX)), mul(axes[2], z * PX * swell)));
    }

    /** A point {@code x} to its right, {@code y} up, {@code z} ahead of {@code base}. */
    private static double[] at(double[] base, double[] r, double[] u, double[] f, double x, double y, double z) {
        return add(base, add(mul(r, x), add(mul(u, y), mul(f, z))));
    }

    private static double[] mix3(double[] a, double[] b, double t) {
        return new double[] {a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t, a[2] + (b[2] - a[2]) * t};
    }

    private static double smooth(double x) {
        double t = clamp01(x);
        return t * t * (3.0 - 2.0 * t);
    }

    /**
     * A blade grown out of a forearm: from just past the elbow on out beyond the hand, long and curving out to {@code out}, flat across,
     * dark crimson, its outer edge white and keen; out {@code grown} (0..1) of its length.
     */
    private static void armBlade(UkakuModel.Sink sink, double[] elbow, double[] hand, double[] out, double grown, int seed) {
        double[] d = normalize(sub(hand, elbow));
        double[] flat = normalize(cross(d, out));
        if (dot(flat, flat) < 1.0E-6) {
            return;
        }
        double[] edge = normalize(cross(flat, d));
        double length = 0.95 * grown;
        double[] from = add(elbow, mul(d, 0.12));
        double[] mid = add(add(hand, mul(d, length * 0.45)), mul(edge, 0.08 * grown));
        double[] tip = add(add(hand, mul(d, length)), mul(edge, 0.26 * grown));
        int n = 7;
        double[][] spine = new double[n + 1][];
        double[] width = new double[n + 1];
        for (int k = 0; k <= n; k++) {
            double t = (double)k / n;
            double[] a = mix3(from, mid, t);
            double[] b = mix3(mid, tip, t);
            spine[k] = mix3(a, b, t);
            width[k] = (0.03 + 0.11 * Math.sin(Math.PI * Math.min(1.0, t * 1.4 + 0.15))) * (1.0 - t * t * 0.9) * (0.4 + 0.6 * grown);
        }
        double thick = 0.018;
        for (int k = 0; k < n; k++) {
            double[] a = spine[k];
            double[] b = spine[k + 1];
            double[] ao = add(a, mul(edge, width[k]));
            double[] bo = add(b, mul(edge, width[k + 1]));
            double[] ai = sub(a, mul(edge, width[k] * 0.25));
            double[] bi = sub(b, mul(edge, width[k + 1] * 0.25));
            int dark = mix(0xFF4A0710, PeaceModel.BLADE_RED, (double)k / n);
            for (int face = -1; face <= 1; face += 2) {
                double[] lift = mul(flat, face * thick * (1.0 - (double)k / n));
                double[] inside = sub(a, mul(flat, face));
                shaded(sink, add(ai, lift), add(bi, lift), bo, ao, inside, dark, dark, PeaceModel.WHITE, PeaceModel.SILVER);
            }
        }
    }

    /** A pool of its flesh on the ground at {@code at}, {@code radius} across, its rim rippling. */
    private static void puddle(UkakuModel.Sink sink, double[] at, double radius, double time, int seed) {
        int n = 12;
        double[] below = sub(at, new double[] {0.0, 1.0, 0.0});
        for (int k = 0; k < n; k++) {
            double a0 = Math.PI * 2.0 * k / n;
            double a1 = Math.PI * 2.0 * (k + 1) / n;
            double r0 = radius * (0.85 + 0.15 * Math.sin(a0 * 3.0 + seed + time * 0.1));
            double r1 = radius * (0.85 + 0.15 * Math.sin(a1 * 3.0 + seed + time * 0.1));
            double[] p0 = {at[0] + Math.cos(a0) * r0, at[1], at[2] + Math.sin(a0) * r0};
            double[] p1 = {at[0] + Math.cos(a1) * r1, at[1], at[2] + Math.sin(a1) * r1};
            double[] q0 = {at[0] + Math.cos(a0) * r0 * 0.5, at[1] + 0.02, at[2] + Math.sin(a0) * r0 * 0.5};
            double[] q1 = {at[0] + Math.cos(a1) * r1 * 0.5, at[1] + 0.02, at[2] + Math.sin(a1) * r1 * 0.5};
            shaded(sink, q0, q1, p1, p0, below, 0xFF8E0E1A, 0xFF8E0E1A, 0xFF3A040A, 0xFF3A040A);
            shaded(sink, add(at, new double[] {0.0, 0.03, 0.0}), add(at, new double[] {0.0, 0.03, 0.0}), q1, q0, below, 0xFFB01E2A, 0xFFB01E2A, 0xFF8E0E1A,
                0xFF8E0E1A);
        }
    }
}
