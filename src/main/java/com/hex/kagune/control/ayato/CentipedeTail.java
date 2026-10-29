package com.hex.kagune.control.ayato;

import static com.hex.kagune.control.ayato.KakujaModel.add;
import static com.hex.kagune.control.ayato.KakujaModel.argb;
import static com.hex.kagune.control.ayato.KakujaModel.dot;
import static com.hex.kagune.control.ayato.KakujaModel.facet;
import static com.hex.kagune.control.ayato.KakujaModel.glow;
import static com.hex.kagune.control.ayato.KakujaModel.mix;
import static com.hex.kagune.control.ayato.KakujaModel.mul;
import static com.hex.kagune.control.ayato.KakujaModel.normalize;
import static com.hex.kagune.control.ayato.KakujaModel.sub;

/**
 * Concept (preview only, not in the game yet): Kaneki's centipede leg after the user's reference. Long boxy plates of dusky
 * plum armor with chamfered corners, a pale mauve rim round each plate's root edge (V-notched on the back), lighter streaks
 * on the faces; glowing red flesh in the gaps; one pair of big black sickle claws per plate, jutting toward the head and
 * hooking back toward the root like a centipede's legs; thin where it leaves the back and swelling toward the head, which is
 * a square fang with a pair of jaw claws at its base (they open to grab). Drawn along a {@link KakujaModel.Spine}: s = 0 is
 * the root in his back, s = 1 the fang's point. It curls sideways like a centipede, so the claw rows splay round the outside
 * and inside of the curl and the back faces out of it.
 */
public final class CentipedeTail {
    static final int PLATE_SHADOW = 0x22142A;
    static final int PLATE_DARK = 0x36213C;
    static final int PLATE = 0x563A5E;
    static final int PLATE_HI = 0x6F5078;
    static final int STREAK = 0x9A7EA6;
    static final int RIM = 0xC6A4C0;
    static final int RIM_SHADE = 0x8C6C8A;
    static final int FLESH_DARK = 0x6E1016;
    static final int FLESH = 0xC82A28;
    static final int FLESH_HOT = 0xF2493A;
    static final int CLAW = 0x121016;
    static final int CLAW_SHEEN = 0x3E3846;
    static final int CLAW_EDGE = 0xB4A6BE;
    static final int FANG_POINT = 0x170D19;

    /**
     * A plate's cross-section: x across (unit half-width), y dorsal (unit half-height). A box with chamfered corners and a
     * point on the back's centre line (index 0, where the rim's V-notch is cut).
     */
    static final double[][] RING = {
        {0.0, 1.0}, {0.6, 1.0}, {1.0, 0.6}, {1.0, -0.55}, {0.62, -0.92}, {-0.62, -0.92}, {-1.0, -0.55}, {-1.0, 0.6}, {-0.6, 1.0}};
    /**
     * 1.0.32: the same section rounded (each face split in two, its middle swelled out to its corners' distance), for a body big
     * enough that the plain one reads as a box: face {@code k} of it is face {@code k / 2} of {@link #RING}.
     */
    static final double[][] ROUND = rounded(RING);
    /** Half-height over half-width: a touch flatter than square. */
    static final double THICK = 0.9;
    /** The flesh between plates sits this far in. */
    static final double FLESH_INSET = 0.72;
    /** The share of the leg that is the fang. */
    public static final double FANG = 0.12;
    /** The share of each plate's step that is flesh before it. */
    static final double BAND = 0.24;

    /**
     * How one leg looks.
     *
     * @param width  half-width where it is thickest (blocks)
     * @param plates armor plates before the fang (the last is the head plate with the jaw claws)
     * @param fine   full detail (streaks, the rim's notch, rounder claws); off for far away
     * @param alpha  opacity
     * @param seed   varies the claws between legs
     * @param jaw    the jaw claws: 0 shut along the fang, 1 wide open
     * @param flare  the leg claws: 0 folded back along the body, 1 at rest, above 1 bristling out
     * @param ripple how far the claws swing in the walking wave (radians)
     * @param phase  where the wave is (radians); it runs from the head to the root
     */
    public record Look(double width, int plates, boolean fine, double alpha, int seed, double jaw, double flare, double ripple, double phase,
                       java.util.function.DoubleUnaryOperator profile, boolean headless, boolean round) {
        public Look(double width, int plates, boolean fine, double alpha, int seed) {
            this(width, plates, fine, alpha, seed, 0.0, 1.0, 0.0, 0.0);
        }

        public Look(double width, int plates, boolean fine, double alpha, int seed, double jaw, double flare, double ripple, double phase) {
            this(width, plates, fine, alpha, seed, jaw, flare, ripple, phase, null, false, false);
        }
    }

    private static double[][] rounded(double[][] ring) {
        double[][] out = new double[ring.length * 2][];
        for (int k = 0; k < ring.length; k++) {
            double[] a = ring[k];
            double[] b = ring[(k + 1) % ring.length];
            double[] m = {(a[0] + b[0]) * 0.5, (a[1] + b[1]) * 0.5};
            double reach = (Math.hypot(a[0], a[1]) + Math.hypot(b[0], b[1])) * 0.5 / Math.max(1.0E-6, Math.hypot(m[0], m[1]));
            out[k * 2] = a;
            out[k * 2 + 1] = new double[] {m[0] * reach, m[1] * reach};
        }
        return out;
    }

    /** The cross-section {@code look} is drawn with. */
    static double[][] section(Look look) {
        return look.round() ? ROUND : RING;
    }

    /**
     * 1.0.31: the half-width along it for {@code look}: the leg's own ({@link #widthAt}), or its {@code profile} (as the Dragon's whole
     * body has it: plate after plate of this same armour, as thick as the body is there).
     */
    static double width(Look look, double s) {
        return look.profile() == null ? widthAt(s) : look.profile().applyAsDouble(s);
    }

    private CentipedeTail() {
    }

    /** Half-width along the leg (share of the thickest): thin from the back, swelling toward the head, then the fang's taper. */
    public static double widthAt(double s) {
        double c = Math.max(0.0, Math.min(1.0, s));
        double body = 0.6 + 0.4 * UkakuModel.smooth(Math.min(1.0, c / 0.62));
        double fangStart = 1.0 - FANG;
        if (c <= fangStart) {
            return body;
        }
        double u = (c - fangStart) / FANG;
        return Math.pow(Math.max(0.0, 1.0 - u), 0.8);
    }

    /** One leg along {@code spine}. */
    public static void tail(UkakuModel.Sink sink, KakujaModel.Spine spine, Look look) {
        double alpha = UkakuModel.clamp01(look.alpha());
        if (alpha <= 0.003 || spine.length() <= 1.0E-4 || look.width() <= 1.0E-5) {
            return;
        }
        int plates = Math.max(3, look.plates());
        // (1.0.31: headless, as the Dragon's body is under its own head: plates right to its end, no fang, no claws.)
        double bodyEnd = look.headless() ? 1.0 : 1.0 - FANG;
        // The last stretch before the fang is one more band of flesh.
        double step = bodyEnd / (plates + BAND);
        for (int i = 0; i < plates; i++) {
            double s0 = i * step;
            double p0 = s0 + BAND * step;
            double p1 = s0 + step;
            boolean head = i == plates - 1 && !look.headless();
            flesh(sink, spine, look, s0, p0, alpha);
            plate(sink, spine, look, p0, p1, i, head ? 1.05 : 1.0, alpha);
            int seed = look.seed() * 131 + i * 17;
            // None on the plate in his back (they would stab through him); small on the next; the head plate has the jaw. (1.0.31:
            // none at all on the Dragon's body: it walks on legs of its own.)
            if (i > 0 && !head && !look.headless()) {
                double wave = look.ripple() * Math.sin(look.phase() + i * 0.95);
                for (int side = -1; side <= 1; side += 2) {
                    claw(sink, spine, look, p0 + 0.6 * (p1 - p0), side, i == 1 ? 0.7 : 1.0, wave * side, seed + (side > 0 ? 5 : 0), alpha);
                }
            }
            if (head) {
                for (int side = -1; side <= 1; side += 2) {
                    jaw(sink, spine, look, p0 + 0.72 * (p1 - p0), side, alpha);
                }
            }
        }
        flesh(sink, spine, look, plates * step, bodyEnd, alpha);
        if (!look.headless()) fang(sink, spine, look, bodyEnd, alpha);
    }

    /** The cross-section at {@code s}, {@code scale} times the leg's width there; {@code notch} pushes the back's centre point on. */
    static double[][] ring(KakujaModel.Spine spine, Look look, double s, double scale, double notch) {
        double[][] f = spine.at(s);
        double w = look.width() * width(look, s) * scale;
        double h = w * THICK;
        double[][] shape = section(look);
        double[][] out = new double[shape.length][];
        for (int k = 0; k < shape.length; k++) {
            out[k] = add(f[0], add(mul(f[3], shape[k][0] * w), mul(f[2], shape[k][1] * h)));
        }
        if (notch != 0.0) {
            out[0] = add(out[0], add(mul(f[1], notch), mul(f[2], -0.08 * h)));
        }
        return out;
    }

    /**
     * One plate from {@code p0} (its root edge) to {@code p1}: the pale rim facing the root, a thin pale lip, a short shoulder,
     * the long faces (streaked), and the dark end facing the head.
     */
    static void plate(UkakuModel.Sink sink, KakujaModel.Spine spine, Look look, double p0, double p1, int index, double size, double alpha) {
        double len = p1 - p0;
        double notch = look.fine() ? 0.16 * len * spine.length() : 0.0;
        double[][] inner = ring(spine, look, p0, FLESH_INSET * size, 0.0);
        double[][] rim = ring(spine, look, p0, 1.07 * size, notch);
        double[][] lip = ring(spine, look, p0 + 0.09 * len, 1.07 * size, notch * 0.9);
        double[][] shoulder = ring(spine, look, p0 + 0.13 * len, 1.0 * size, notch * 0.55);
        double[][] mid = ring(spine, look, p0 + 0.56 * len, 0.99 * size, 0.0);
        double[][] end = ring(spine, look, p1, 0.93 * size, 0.0);
        double[][] endInner = ring(spine, look, p1, FLESH_INSET * size, 0.0);
        double[] centre = spine.at(p0 + 0.5 * len)[0];
        int seed = look.seed() * 977 + index * 61;
        band(sink, inner, rim, centre, alpha, k -> mix(RIM, RIM_SHADE, belly(k) * 0.8), k -> mix(RIM_SHADE, PLATE_DARK, belly(k)));
        band(sink, rim, lip, centre, alpha, k -> mix(RIM, RIM_SHADE, belly(k)), k -> mix(RIM, RIM_SHADE, belly(k)));
        band(sink, lip, shoulder, centre, alpha, k -> mix(PLATE_DARK, PLATE_SHADOW, belly(k)), k -> tone(k, 0.0));
        band(sink, shoulder, mid, centre, alpha, k -> tone(k, 0.0), k -> streak(k, seed, 0.9));
        band(sink, mid, end, centre, alpha, k -> streak(k, seed, 0.9), k -> mix(tone(k, 0.0), PLATE_SHADOW, 0.35));
        band(sink, end, endInner, centre, alpha, k -> PLATE_DARK, k -> PLATE_SHADOW);
    }

    /**
     * The head's frame (where the jaw claws close: what a grab holds), as {position, toward the point, back, edge}; for
     * placing a held body and for previews.
     */
    public static double[][] head(KakujaModel.Spine spine, Look look) {
        int plates = Math.max(3, look.plates());
        double step = (1.0 - FANG) / (plates + BAND);
        double[][] f = spine.at(1.0 - FANG);
        double w = look.width() * width(look, 1.0 - FANG);
        double[] at = add(f[0], add(mul(f[1], 1.6 * w), mul(f[2], -0.25 * w)));
        return new double[][] {at, f[1], f[2], f[3], {step}};
    }

    /** How much face {@code k} (between ring points k and k + 1) faces the belly: 0 the back, 1 straight down. */
    static double belly(int k) {
        int j = (k + 1) % RING.length;
        double y = (RING[k][1] + RING[j][1]) * 0.5;
        return UkakuModel.clamp01((1.0 - y) * 0.5);
    }

    /**
     * The plate's flat cel tone for face {@code k}: armor all round (the legs curl sideways, so the belly faces the camera as
     * often as the back): a lit back, pale bevels on the chamfers, plum flanks and belly a shade darker.
     */
    static int tone(int k, double light) {
        int base = switch (k) {
            case 0, 8 -> PLATE_HI;
            case 1, 7 -> mix(PLATE_HI, STREAK, 0.35);
            case 2, 6 -> PLATE;
            case 3, 5 -> mix(PLATE, STREAK, 0.12);
            default -> mix(PLATE, PLATE_DARK, 0.3);
        };
        return mix(base, STREAK, light);
    }

    /** A face with a lighter streak on some plates (the reference's painted highlights). */
    static int streak(int k, int seed, double amount) {
        double h = UkakuModel.hash(seed + k * 7);
        return tone(k, h > 0.4 ? amount * (h - 0.4) / 0.6 : 0.0);
    }

    /** Facets between two rings, the colour of each face at its first and second ring (by its face of {@link #RING}). */
    static void band(UkakuModel.Sink sink, double[][] from, double[][] to, double[] centre, double alpha,
                     java.util.function.IntUnaryOperator first, java.util.function.IntUnaryOperator second) {
        int count = from.length;
        int per = Math.max(1, count / RING.length);
        for (int k = 0; k < count; k++) {
            int j = (k + 1) % count;
            int a = argb(first.applyAsInt(k / per), alpha);
            int b = argb(second.applyAsInt(k / per), alpha);
            facet(sink, from[k], to[k], to[j], from[j], centre, a, b, b, a);
        }
    }

    /** Raw flesh between plates: dark at its edges, glowing hot through the middle. */
    static void flesh(UkakuModel.Sink sink, KakujaModel.Spine spine, Look look, double s0, double s1, double alpha) {
        double[][] a = ring(spine, look, s0, FLESH_INSET, 0.0);
        double[][] m = ring(spine, look, (s0 + s1) * 0.5, FLESH_INSET * 1.04, 0.0);
        double[][] b = ring(spine, look, s1, FLESH_INSET, 0.0);
        int count = a.length;
        int per = Math.max(1, count / RING.length);
        for (int k = 0; k < count; k++) {
            int j = (k + 1) % count;
            double under = belly(k / per);
            int edge = argb(mix(FLESH, FLESH_DARK, 0.45 + 0.4 * under), alpha);
            int hot = argb(mix(FLESH_HOT, FLESH, 0.25 + 0.6 * under), alpha);
            glow(sink, a[k], m[k], m[j], a[j], edge, hot, hot, edge);
            glow(sink, m[k], b[k], b[j], m[j], hot, edge, edge, hot);
        }
    }

    /**
     * A leg claw on {@code side} at {@code s}: a broad black blade out of the flank that juts toward the head, then sweeps out
     * and hooks back toward the root, its inner edge pale. {@code swing} turns it on the walking wave (radians, + toward the
     * head).
     */
    static void claw(UkakuModel.Sink sink, KakujaModel.Spine spine, Look look, double s, int side, double size, double swing, int seed,
                     double alpha) {
        double[][] f = spine.at(s);
        double w = look.width() * width(look, s);
        double vary = 0.9 + 0.2 * UkakuModel.hash(seed);
        double l = 1.6 * w * size * vary;
        // Out of the flank, tipped a little toward the belly like a leg.
        double drop = Math.toRadians(10.0 + 10.0 * UkakuModel.hash(seed + 1));
        double[] out = normalize(add(mul(f[3], side * Math.cos(drop)), mul(f[2], -Math.sin(drop))));
        double[] along = f[1];
        // Folded claws lie back along the body; the wave swings them toward the head and back.
        double fold = Math.toRadians(55.0) * (1.0 - Math.max(0.0, Math.min(1.6, look.flare()))) - swing;
        double[] o = normalize(add(mul(out, Math.cos(fold)), mul(along, -Math.sin(fold))));
        double[] t = normalize(add(mul(along, Math.cos(fold)), mul(out, Math.sin(fold))));
        double reach = Math.max(0.6, look.flare());
        double[] base = add(f[0], add(mul(out, 0.8 * w), mul(f[2], -0.05 * w * THICK)));
        double[][] c = {
            base,
            add(base, add(mul(o, 0.45 * l * reach), mul(t, 0.5 * l))),
            add(base, add(mul(o, 1.05 * l * reach), mul(t, 0.32 * l))),
            add(base, add(mul(o, 1.2 * l * reach), mul(t, -0.42 * l)))};
        double[] plane = normalize(KakujaModel.cross(t, o));
        blade(sink, c, plane, 0.42 * l, 0.1 * l, look.fine() ? 7 : 4, alpha);
    }

    /**
     * A jaw claw at the head plate's flank: thick, curving forward along the fang and in under it. {@code look.jaw()} swings it
     * open out to the side.
     */
    static void jaw(UkakuModel.Sink sink, KakujaModel.Spine spine, Look look, double s, int side, double alpha) {
        double[][] f = spine.at(s);
        double w = look.width() * width(look, s) * 1.05;
        double[] out = normalize(add(mul(f[3], side), mul(f[2], -0.35)));
        double[] fwd = normalize(sub(f[1], mul(out, dot(f[1], out))));
        // Open swings the whole jaw out to the side, about the back's axis.
        double open = Math.toRadians(75.0) * UkakuModel.clamp01(look.jaw());
        double[] t = add(mul(fwd, Math.cos(open)), mul(out, Math.sin(open)));
        double[] o = sub(mul(out, Math.cos(open)), mul(fwd, Math.sin(open)));
        double[] down = mul(f[2], -1.0);
        double[] base = add(f[0], add(mul(out, 0.8 * w), mul(f[2], -0.2 * w * THICK)));
        double[][] c = {
            base,
            add(base, add(mul(t, 0.75 * w), mul(o, 0.6 * w))),
            add(base, add(mul(t, 2.2 * w), mul(o, 0.65 * w))),
            add(base, add(add(mul(t, 3.0 * w), mul(o, -0.65 * w)), mul(down, 0.3 * w)))};
        double[] plane = normalize(KakujaModel.cross(t, o));
        blade(sink, c, plane, 0.6 * w, 0.2 * w, look.fine() ? 8 : 5, alpha);
    }

    /**
     * A curved, tapering blade along the cubic Bezier {@code c}: a flattened diamond {@code width} across in its own plane and
     * {@code thick} through it, its inner (concave) edge pale, the face toward {@code plane} sheened, the rest black.
     */
    static void blade(UkakuModel.Sink sink, double[][] c, double[] plane, double width, double thick, int stations, double alpha) {
        double[][][] rings = new double[stations + 1][][];
        for (int i = 0; i <= stations; i++) {
            double u = i / (double)stations;
            double[] p = bezier(c, u);
            double[] t = normalize(sub(bezier(c, Math.min(1.0, u + 0.02)), bezier(c, Math.max(0.0, u - 0.02))));
            double[] a = normalize(KakujaModel.cross(plane, t));
            // The concave side: toward where the curve is bending.
            double[] bend = sub(add(bezier(c, Math.min(1.0, u + 0.05)), bezier(c, Math.max(0.0, u - 0.05))), mul(p, 2.0));
            if (dot(bend, a) < 0.0) {
                a = mul(a, -1.0);
            }
            double taper = Math.pow(Math.max(0.0, 1.0 - u), 0.85);
            double r = width * Math.pow(Math.max(0.0, 1.0 - u), 1.25) + 1.0E-4;
            double d = thick * taper + 1.0E-4;
            rings[i] = new double[][] {add(p, mul(a, r * 0.75)), add(p, mul(plane, d)), add(p, mul(a, -r)), add(p, mul(plane, -d))};
        }
        // The pale edge, the sheened face, the back, the shadowed face.
        int[] colour = {CLAW_EDGE, CLAW_SHEEN, CLAW, mix(CLAW, CLAW_SHEEN, 0.25)};
        for (int i = 0; i < stations; i++) {
            double u0 = i / (double)stations;
            double u1 = (i + 1) / (double)stations;
            double[] mid = bezier(c, (u0 + u1) * 0.5);
            for (int k = 0; k < 4; k++) {
                int j = (k + 1) % 4;
                facet(sink, rings[i][k], rings[i + 1][k], rings[i + 1][j], rings[i][j], mid, argb(edgeFade(colour[k], u0), alpha),
                    argb(edgeFade(colour[k], u1), alpha), argb(edgeFade(colour[j], u1), alpha), argb(edgeFade(colour[j], u0), alpha));
            }
        }
    }

    /** The edge's shine fades a little toward the point. */
    static int edgeFade(int colour, double u) {
        return mix(colour, CLAW, u * 0.3);
    }

    /** The fang: a square blade out of the head, tapering to a dark point that hooks on round the curl. */
    static void fang(UkakuModel.Sink sink, KakujaModel.Spine spine, Look look, double from, double alpha) {
        int stations = look.fine() ? 7 : 4;
        double[][] f0 = spine.at(from);
        double w0 = look.width() * width(look, from);
        double[][] previous = ring(spine, look, from, 1.04, 0.0);
        // The head's rim, facing the fang.
        double[][] lip = ring(spine, look, from, FLESH_INSET, 0.0);
        band(sink, lip, previous, f0[0], alpha, k -> PLATE_DARK, k -> mix(RIM, RIM_SHADE, belly(k)));
        double hook = w0 * 0.45;
        double[] inward = curlIn(spine, from);
        for (int i = 1; i <= stations; i++) {
            double u = i / (double)stations;
            double s = from + (1.0 - from) * u;
            double[][] next;
            double[][] f = spine.at(s);
            if (i == stations) {
                double[] point = add(add(f[0], mul(f[1], w0 * 0.5)), mul(inward, hook));
                next = new double[previous.length][];
                for (int k = 0; k < next.length; k++) {
                    next[k] = point;
                }
            } else {
                next = ring(spine, look, s, 1.04, 0.0);
                double[] bend = mul(inward, hook * u * u);
                for (int k = 0; k < next.length; k++) {
                    next[k] = add(next[k], bend);
                }
            }
            double v0 = (i - 1) / (double)stations;
            double v1 = u;
            double[] centre = add(spine.at(Math.min(1.0, s - 0.5 / stations * (1.0 - from)))[0], mul(inward, hook * u * u));
            band(sink, previous, next, centre, alpha, k -> fangTone(k, v0), k -> fangTone(k, v1));
            previous = next;
        }
    }

    /** Toward the inside of the curl near the head (square to the leg; the belly where it runs straight). */
    static double[] curlIn(KakujaModel.Spine spine, double from) {
        double[] a = spine.at(Math.max(0.0, from - 0.12))[0];
        double[][] f = spine.at(from);
        double[] b = spine.at(1.0)[0];
        double[] k = sub(add(a, b), mul(f[0], 2.0));
        k = sub(k, mul(f[1], dot(k, f[1])));
        return dot(k, k) < 1.0E-8 ? mul(f[2], -1.0) : normalize(k);
    }

    /** The fang's tone: the plate's, with its back chamfers drawn as pale edges, darkening to the point. */
    static int fangTone(int k, double u) {
        int base = k == 1 || k == 7 ? mix(RIM, PLATE_HI, 0.35) : tone(k, 0.0);
        return mix(base, FANG_POINT, UkakuModel.smooth(Math.max(0.0, (u - 0.45) / 0.55)) * 0.85);
    }

    /**
     * A spine through {@code controls} (rig space, root first), {@code samples} segments long. Its back starts on the side
     * {@code dorsal} points to (made square to the root's direction) and is carried along without twisting; its edge
     * {@code side} (+1 right, -1 left).
     */
    public static KakujaModel.Spine through(double[][] controls, int samples, int side, double[] dorsal) {
        double[][] ps = new double[samples + 1][];
        for (int i = 0; i <= samples; i++) {
            ps[i] = catmullRom(controls, i / (double)samples);
        }
        double[][] ts = new double[samples + 1][];
        double[][] ns = new double[samples + 1][];
        double[][] bs = new double[samples + 1][];
        double length = 0.0;
        for (int i = 0; i <= samples; i++) {
            double[] t = normalize(sub(ps[Math.min(samples, i + 1)], ps[Math.max(0, i - 1)]));
            double[] n;
            if (i == 0) {
                n = sub(dorsal, mul(t, dot(dorsal, t)));
                n = dot(n, n) < 1.0E-6 ? perpendicular(t) : normalize(n);
            } else {
                n = sub(ns[i - 1], mul(t, dot(ns[i - 1], t)));
                n = dot(n, n) < 1.0E-10 ? perpendicular(t) : normalize(n);
                double[] d = sub(ps[i], ps[i - 1]);
                length += Math.sqrt(dot(d, d));
            }
            ts[i] = t;
            ns[i] = n;
            bs[i] = mul(KakujaModel.cross(t, n), side >= 0 ? 1.0 : -1.0);
        }
        return new KakujaModel.Spine(ps, ts, ns, bs, length);
    }

    public static double[] catmullRom(double[][] c, double u) {
        int segments = c.length - 1;
        double x = Math.max(0.0, Math.min(1.0, u)) * segments;
        int i = Math.min(segments - 1, (int)Math.floor(x));
        double t = x - i;
        double[] p0 = c[Math.max(0, i - 1)];
        double[] p1 = c[i];
        double[] p2 = c[i + 1];
        double[] p3 = c[Math.min(segments, i + 2)];
        double t2 = t * t;
        double t3 = t2 * t;
        double[] out = new double[3];
        for (int k = 0; k < 3; k++) {
            out[k] = 0.5 * (2.0 * p1[k] + (-p0[k] + p2[k]) * t + (2.0 * p0[k] - 5.0 * p1[k] + 4.0 * p2[k] - p3[k]) * t2
                + (-p0[k] + 3.0 * p1[k] - 3.0 * p2[k] + p3[k]) * t3);
        }
        return out;
    }

    static double[] bezier(double[][] c, double u) {
        double v = 1.0 - u;
        double a = v * v * v;
        double b = 3.0 * v * v * u;
        double d = 3.0 * v * u * u;
        double e = u * u * u;
        return new double[] {a * c[0][0] + b * c[1][0] + d * c[2][0] + e * c[3][0], a * c[0][1] + b * c[1][1] + d * c[2][1] + e * c[3][1],
            a * c[0][2] + b * c[1][2] + d * c[2][2] + e * c[3][2]};
    }

    static double[] perpendicular(double[] t) {
        double[] axis = Math.abs(t[1]) < 0.9 ? new double[] {0.0, 1.0, 0.0} : new double[] {1.0, 0.0, 0.0};
        return normalize(sub(axis, mul(t, dot(axis, t))));
    }
}
