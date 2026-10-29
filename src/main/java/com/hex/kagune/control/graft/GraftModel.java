package com.hex.kagune.control.graft;

import static com.hex.kagune.control.graft.GraftMesh.add;
import static com.hex.kagune.control.graft.GraftMesh.argb;
import static com.hex.kagune.control.graft.GraftMesh.clamp01;
import static com.hex.kagune.control.graft.GraftMesh.dot;
import static com.hex.kagune.control.graft.GraftMesh.facet;
import static com.hex.kagune.control.graft.GraftMesh.glow;
import static com.hex.kagune.control.graft.GraftMesh.hash;
import static com.hex.kagune.control.graft.GraftMesh.horn;
import static com.hex.kagune.control.graft.GraftMesh.lerp;
import static com.hex.kagune.control.graft.GraftMesh.mix;
import static com.hex.kagune.control.graft.GraftMesh.mul;
import static com.hex.kagune.control.graft.GraftMesh.noise;
import static com.hex.kagune.control.graft.GraftMesh.normalize;
import static com.hex.kagune.control.graft.GraftMesh.smooth;
import static com.hex.kagune.control.graft.GraftMesh.sub;

import com.hex.kagune.control.ukaku.KakujaModel;
import com.hex.kagune.control.ukaku.UkakuModel;

/**
 * Graft DNA's kagune as real 3D geometry (pure, tested; 7.0.0), after the manga (his reveal against the Owl, the centipede limbs,
 * chapters 172 to 174) with the anime's colours: dark blood-red living muscle, black carapace, crimson light in the seams.
 * Each limb is built along a {@link KakujaModel.Spine} (s = 0 the root in his lower back, s = 1 its point) the way the kakuja
 * tentacles and centipede legs are, from the same frame (position, tangent, back, edge):
 * <ul>
 *   <li>{@link #rinkaku}: Rize's transplanted kagune, one or two muscular tails: a thick bulging root, an irregular striated
 *       surface of muscle bundles, a long smooth taper to a spear point, red light running in its grooves;</li>
 *   <li>{@link #centipede}: the centipede limbs: overlapping hardened segments over dense muscle, a keel of hooks down the back,
 *       spikes out of both flanks (the edges that hurt), a barbed stinger; the first ends in a real mouth ({@link #maw}) whose two
 *       jaws open, close and bite, lined with teeth;</li>
 *   <li>{@link #thorn}: the four thorns: a massive muscular core ringed by black armored ridges, each ridge set with long curved
 *       thorns, ending in a huge serrated point;</li>
 *   <li>{@link #crescent}: the many-eyed kagune: vast, lumpy crescent tentacles of black and crimson flesh stacked in layers to one
 *       side of him (the manga's "let's play house"), clusters of spikes that grow on command, and big almond eyes in rings of black
 *       flesh along them that blink, roll and stare;</li>
 *   <li>{@link #colossus}: the kakuja's two colossal centipedes: heavy plates, two rows of long spikes down each flank, hooks
 *       along the back, oversized eyes in folds of tissue, and a head with mandibles round a gaping toothed mouth.</li>
 * </ul>
 */
public final class GraftModel {
    static final int FLESH_DEEP = 0x1E0307;
    static final int FLESH_DARK = 0x42060E;
    static final int FLESH = 0x6C0C16;
    static final int FLESH_HI = 0x98202A;
    static final int FLESH_SHEEN = 0xC13B42;
    static final int VEIN = 0xE3202A;
    static final int VEIN_DIM = 0x6A040C;
    static final int SHELL_DARK = 0x0A0406;
    static final int SHELL = 0x1C0A0E;
    static final int SHELL_HI = 0x40191F;
    static final int SHELL_EDGE = 0x76222C;
    static final int SPIKE_ROOT = 0x520A13;
    static final int SPIKE_TIP = 0x060203;
    static final int MASS = 0x0B0406;
    static final int MASS_CREASE = 0x4A0711;
    static final int GUM = 0x8A1020;
    static final int THROAT = 0x16020A;

    /**
     * How one limb looks this frame.
     *
     * @param width  half-width where it is thickest (blocks)
     * @param fine   full detail (close up); off for far away and first person
     * @param seed   varies each limb
     * @param jaw    its mouth (0 shut .. 1 wide), if it has one
     * @param flare  its spikes (1 at rest, up to 2 bristling, below 1 laid flat)
     * @param stare  its eyes (0 at rest, 1 staring wide at something)
     * @param maw    it ends in a mouth
     * @param time   ticks (blinks, the veins' pulse)
     * @param gaze   the way its eyes look (spine space), null to stare straight out
     */
    public record Look(double width, boolean fine, double alpha, int seed, double jaw, double flare, double stare, boolean maw, double time,
                       double[] gaze, boolean outward) {
        public Look(double width, boolean fine, double alpha, int seed, double jaw, double flare, double stare, boolean maw, double time,
                    double[] gaze) {
            this(width, fine, alpha, seed, jaw, flare, stare, maw, time, gaze, false);
        }
    }

    private GraftModel() {
    }

    /** One limb of {@code stage} along {@code spine}. */
    public static void limb(UkakuModel.Sink sink, GraftStage stage, KakujaModel.Spine spine, Look look) {
        if (look.alpha() <= 0.003 || spine.length() <= 1.0E-3 || look.width() <= 1.0E-4) {
            return;
        }
        switch (stage) {
            case TRANSPLANT -> rinkaku(sink, spine, look);
            case CENTIPEDE -> centipede(sink, spine, look);
            case THORNS -> thorn(sink, spine, look);
            case DRAGON -> crescent(sink, spine, look);
            case KAKUJA -> colossus(sink, spine, look);
        }
        if (look.maw() && stage != GraftStage.CENTIPEDE && stage != GraftStage.KAKUJA) {
            // Flesh Devourer: its point swells and splits into a toothed mouth.
            maw(sink, spine, look, 0.8, look.width() * 0.72, false);
        }
    }

    // ---------------------------------------------------------------- Rize's kakuhou: the rinkaku tails

    /**
     * The tail's half-width along it (share of the thickest): thin where it tears out of the small of his back (never a lump on
     * him), swelling to its muscle a stride out from him, then a spear's taper.
     */
    public static double rinkakuWidth(double s) {
        double c = clamp01(s);
        double bulge = 1.0 + 0.16 * Math.exp(-Math.pow((c - 0.3) / 0.1, 2.0));
        return emerge(c, 0.22) * bulge * Math.pow(Math.max(0.0, 1.0 - c), 0.55 + 0.6 * c);
    }

    /**
     * 7.0.0: how far a limb has grown to its girth {@code c} along it: a third of it where it leaves his back, all of it by
     * {@code by} (so the limbs come out of him, they are not a mass on his back).
     */
    static double emerge(double c, double by) {
        return 0.32 + 0.68 * smooth(c / by);
    }

    public static void rinkaku(UkakuModel.Sink sink, KakujaModel.Spine spine, Look look) {
        double w = look.width();
        int seed = look.seed();
        int stations = look.fine() ? Math.max(22, Math.min(46, (int)Math.round(spine.length() * 8.0))) : 12;
        int sides = look.fine() ? 10 : 6;
        GraftMesh.tube(sink, spine, 0.0, 1.0, stations, sides, new GraftMesh.Surface() {
            @Override
            public double radius(double s, double a) {
                return w * rinkakuWidth(s) * (1.0 + (muscle(s, a, seed) + segment(s, spine.length(), w)) * Math.sqrt(Math.max(0.0, 1.0 - s)));
            }

            @Override
            public int colour(double s, double a) {
                double m = muscle(s, a, seed) + 0.6 * segment(s, spine.length(), w);
                double back = Math.sin(a);
                int base = mix(FLESH_DARK, FLESH_HI, clamp01(0.45 + m * 4.5 - 0.2 * back));
                base = mix(base, FLESH_SHEEN, clamp01((m - 0.07) * 6.0) * 0.6);
                base = mix(base, FLESH_DEEP, clamp01(-m * 5.0) * 0.7);
                // The point darkens to near black, honed.
                return mix(base, SPIKE_TIP, smooth((s - 0.78) / 0.2) * 0.85);
            }
        }, look.alpha());
        veins(sink, spine, look, w, 0.06, 0.82, 2, GraftModel::rinkakuWidth);
    }

    /** Bands of muscle swelling along a tail, about two widths apart, fading out toward its point. */
    static double segment(double s, double length, double w) {
        double phase = s * length / Math.max(0.05, 2.1 * w);
        return 0.075 * Math.pow(Math.abs(Math.sin(Math.PI * phase)), 3.0) * (1.0 - smooth((s - 0.55) / 0.35));
    }

    /** Muscle bundles on a tail: twisted striations, rough, never in step with the other tail. */
    static double muscle(double s, double a, int seed) {
        return 0.1 * Math.sin(3.0 * a + 7.5 * s + seed * 1.3) + 0.06 * Math.sin(5.0 * a - 13.0 * s + seed * 2.1)
            + 0.04 * (noise(seed, s * 18.0, a * 1.6) - 0.5);
    }

    /**
     * {@code count} glowing threads twisting along the surface from {@code s0} to {@code s1}, just over it: the RC in the kagune.
     */
    static void veins(UkakuModel.Sink sink, KakujaModel.Spine spine, Look look, double w, double s0, double s1, int count,
                      java.util.function.DoubleUnaryOperator width) {
        if (!look.fine()) {
            return;
        }
        int steps = 18;
        double pulse = 0.75 + 0.25 * Math.sin(look.time() * 0.15 + look.seed());
        for (int k = 0; k < count; k++) {
            double phase = k * Math.PI * 2.0 / count + look.seed();
            double[] lastA = null;
            double[] lastB = null;
            for (int i = 0; i <= steps; i++) {
                double s = s0 + (s1 - s0) * i / steps;
                double a = phase + s * 5.0 + 0.4 * Math.sin(s * 17.0 + k);
                double r = w * width.applyAsDouble(s) * 1.035;
                double[][] here = GraftMesh.onSurface(spine, s, a, r);
                double[] across = normalize(GraftMesh.cross(here[1], here[2]));
                double thin = w * 0.05 * (1.0 - 0.6 * s);
                double[] pa = add(here[0], mul(across, thin));
                double[] pb = add(here[0], mul(across, -thin));
                if (lastA != null) {
                    int c0 = argb(mix(VEIN_DIM, VEIN, pulse * (1.0 - 0.6 * (s - 0.05))), look.alpha());
                    glow(sink, lastA, pa, pb, lastB, c0, c0, c0, c0);
                }
                lastA = pa;
                lastB = pb;
            }
        }
    }

    // ---------------------------------------------------------------- the centipedes

    /** A centipede limb's half-width along it (share): thin out of the back, swelling, narrowing toward the head or the stinger. */
    public static double centipedeWidth(double s) {
        double c = clamp01(s);
        return emerge(c, 0.16) * (0.68 + 0.32 * smooth(c / 0.35)) * (1.0 - 0.3 * smooth((c - 0.55) / 0.45));
    }

    /** Where a centipede limb's head (or stinger) begins. */
    public static final double HEAD = 0.84;

    public static void centipede(UkakuModel.Sink sink, KakujaModel.Spine spine, Look look) {
        double w = look.width();
        double bodyEnd = HEAD;
        int plates = Math.max(6, Math.min(14, (int)Math.round(spine.length() * bodyEnd / (2.2 * w))));
        segments(sink, spine, look, w, 0.0, bodyEnd, plates, GraftModel::centipedeWidth, false);
        if (look.maw()) {
            maw(sink, spine, look, bodyEnd, w * centipedeWidth(bodyEnd) * 1.1, false);
        } else {
            stinger(sink, spine, look, bodyEnd, w * centipedeWidth(bodyEnd));
        }
    }

    /**
     * Overlapping armored segments from {@code s0} to {@code s1}: between each a band of raw glowing muscle, each plate a flared
     * lip riding over the band before it, a keel down its back, spikes out of its flanks, a hook on the keel every other plate
     * (and, {@code colossal}, a second row of spikes and the eyes in folds of tissue).
     */
    static void segments(UkakuModel.Sink sink, KakujaModel.Spine spine, Look look, double w, double s0, double s1, int plates,
                         java.util.function.DoubleUnaryOperator width, boolean colossal) {
        double step = (s1 - s0) / plates;
        int sides = look.fine() ? 8 : 6;
        int seed = look.seed();
        for (int i = 0; i < plates; i++) {
            double a0 = s0 + i * step;
            double p0 = a0 + 0.2 * step;
            double p1 = a0 + step + 0.06 * step;
            // Each plate a little different: never a row of copies.
            double jitter = 1.0 + 0.07 * (hash(seed * 31 + i) - 0.5) * 2.0;
            double tilt = (hash(seed * 17 + i * 3) - 0.5) * 0.5;
            int plateIndex = i;
            GraftMesh.tube(sink, spine, a0, p0, 1, sides, new GraftMesh.Surface() {
                @Override
                public double radius(double s, double a) {
                    return w * width.applyAsDouble(s) * 0.8;
                }

                @Override
                public int colour(double s, double a) {
                    return mix(FLESH, FLESH_HI, 0.5 + 0.5 * Math.cos(a * 2.0 + plateIndex));
                }
            }, look.alpha());
            // A glowing seam deep in the band.
            if (look.fine()) {
                double mid = (a0 + p0) * 0.5;
                double r = w * width.applyAsDouble(mid) * 0.82;
                for (int k = 0; k < sides; k += 2) {
                    double a = Math.PI * 2.0 * k / sides;
                    double b = Math.PI * 2.0 * (k + 1) / sides;
                    double[][] q0 = GraftMesh.onSurface(spine, mid - 0.15 * step * 0.2, a, r);
                    double[][] q1 = GraftMesh.onSurface(spine, mid + 0.15 * step * 0.2, a, r);
                    double[][] q2 = GraftMesh.onSurface(spine, mid + 0.15 * step * 0.2, b, r);
                    double[][] q3 = GraftMesh.onSurface(spine, mid - 0.15 * step * 0.2, b, r);
                    int c = argb(VEIN, look.alpha());
                    glow(sink, q0[0], q1[0], q2[0], q3[0], c, c, c, c);
                }
            }
            GraftMesh.tube(sink, spine, p0, p1, look.fine() ? 3 : 2, sides, new GraftMesh.Surface() {
                @Override
                public double radius(double s, double a) {
                    double u = (s - p0) / (p1 - p0);
                    double lip = 1.16 - 0.24 * smooth(u * 1.6);
                    double keel = 0.16 * Math.pow(Math.max(0.0, Math.sin(a)), 6.0);
                    double rough = 0.05 * (noise(seed + plateIndex, a * 2.0, u * 3.0) - 0.5);
                    double lean = 1.0 + tilt * 0.12 * Math.cos(a);
                    return w * width.applyAsDouble(s) * jitter * (lip + keel + rough) * lean * (colossal ? 1.05 : 1.0);
                }

                @Override
                public int colour(double s, double a) {
                    double u = (s - p0) / (p1 - p0);
                    double back = Math.max(0.0, Math.sin(a));
                    int base = mix(SHELL, SHELL_HI, 0.25 + 0.6 * back * back);
                    base = mix(base, SHELL_EDGE, clamp01(1.0 - u * 4.0) * 0.8);
                    return mix(base, SHELL_DARK, clamp01((u - 0.6) * 2.0) * 0.6);
                }
            }, look.alpha());
            if (i == 0) {
                continue; // none on the plate in his back (they would stab through him)
            }
            double spikeAt = p0 + 0.45 * (p1 - p0);
            double r = w * width.applyAsDouble(spikeAt) * jitter;
            double flare = look.flare();
            double size = (0.85 + 0.5 * hash(seed * 7 + i)) * (i == 1 ? 0.6 : 1.0);
            int rows = colossal ? 2 : 1;
            for (int side = -1; side <= 1; side += 2) {
                for (int row = 0; row < rows; row++) {
                    double angle = (side > 0 ? 0.0 : Math.PI) + side * (row == 0 ? -0.18 : 0.42);
                    double[][] at = GraftMesh.onSurface(spine, spikeAt + row * 0.08 * step, angle, r * 0.95);
                    double rake = Math.toRadians(40.0 - 25.0 * Math.min(1.6, flare));
                    double[] dir = add(mul(at[1], Math.cos(rake)), mul(at[2], Math.sin(rake)));
                    double length = r * (colossal ? 1.55 : 1.25) * size * Math.max(0.35, flare) * (row == 0 ? 1.0 : 0.7);
                    horn(sink, at[0], dir, at[2], length, r * 0.2, look.fine() ? 5 : 4, look.fine() ? 3 : 2, SPIKE_ROOT, SPIKE_TIP, look.alpha());
                }
            }
            if (i % 2 == 0 || colossal) {
                double[][] keel = GraftMesh.onSurface(spine, p0 + 0.35 * (p1 - p0), Math.PI * 0.5, r * 1.1);
                double[] dir = normalize(add(mul(keel[1], 0.85), mul(keel[2], 0.55)));
                horn(sink, keel[0], dir, mul(keel[2], -1.0), r * (colossal ? 1.15 : 0.85) * Math.max(0.5, flare), r * 0.24, look.fine() ? 5 : 4,
                    look.fine() ? 3 : 2, SHELL_HI, SPIKE_TIP, look.alpha());
            }
            if (colossal && i % 3 == 1 && i < plates - 1) {
                // An oversized eye in a fold of tissue on the flank.
                int side = (i / 3) % 2 == 0 ? 1 : -1;
                double angle = side > 0 ? 0.55 : Math.PI - 0.55;
                double s = p0 + 0.55 * (p1 - p0);
                double[][] at = GraftMesh.onSurface(spine, s, angle, r * 0.92);
                eyeAt(sink, spine, look, at, s, r * 0.5, seed * 101 + i);
            }
        }
    }

    /** One limb eye (blinking on its own rhythm, staring when the limb strikes). */
    static void eyeAt(UkakuModel.Sink sink, KakujaModel.Spine spine, Look look, double[][] at, double s, double radius, int seed) {
        double period = 70.0 + 90.0 * hash(seed);
        double phase = (look.time() + period * hash(seed + 1)) % period;
        double blink = phase < 5.0 ? Math.sin(Math.PI * phase / 5.0) : 0.0;
        double open = 1.0 - blink * (1.0 - 0.7 * look.stare());
        GraftMesh.eye(sink, at[0], at[1], at[2], radius, look.gaze(), open, look.stare(), FLESH, FLESH_DEEP, look.fine(), look.alpha());
    }

    /** A barbed stinger from {@code from} to the point: the plain centipedes' end. */
    static void stinger(UkakuModel.Sink sink, KakujaModel.Spine spine, Look look, double from, double radius) {
        double span = 1.0 - from;
        GraftMesh.tube(sink, spine, from, 1.0, look.fine() ? 6 : 3, look.fine() ? 8 : 5, new GraftMesh.Surface() {
            @Override
            public double radius(double s, double a) {
                double u = (s - from) / span;
                return radius * Math.pow(Math.max(0.0, 1.0 - u), 1.1) * (1.0 + 0.1 * Math.pow(Math.max(0.0, Math.sin(a)), 4.0));
            }

            @Override
            public int colour(double s, double a) {
                double u = (s - from) / span;
                return mix(SHELL_HI, SPIKE_TIP, u);
            }
        }, look.alpha());
        for (int k = 1; k <= 3; k++) {
            double s = from + span * (0.18 * k);
            double r = radius * Math.pow(1.0 - 0.18 * k, 1.1);
            for (int side = -1; side <= 1; side += 2) {
                double[][] at = GraftMesh.onSurface(spine, s, side > 0 ? 0.0 : Math.PI, r);
                double[] dir = normalize(add(mul(at[1], 0.6), mul(at[2], -0.8)));
                horn(sink, at[0], dir, mul(at[2], -1.0), r * 0.9, r * 0.25, 4, 2, SPIKE_ROOT, SPIKE_TIP, look.alpha());
            }
        }
    }

    /**
     * A mouth at the end of a limb, from {@code from} on: the head swells, then splits into an upper and a lower jaw that open apart
     * ({@code look.jaw}), each a shell of carapace hooking in toward the other, lined with teeth along both rims, a glowing gum and
     * a dark throat between them. {@code mandibles}: two more hooks at its sides that open with it (the kakuja's heads).
     */
    static void maw(UkakuModel.Sink sink, KakujaModel.Spine spine, Look look, double from, double radius, boolean mandibles) {
        double headEnd = from + (1.0 - from) * 0.35;
        GraftMesh.tube(sink, spine, from, headEnd, look.fine() ? 3 : 2, look.fine() ? 8 : 6, new GraftMesh.Surface() {
            @Override
            public double radius(double s, double a) {
                double u = (s - from) / (headEnd - from);
                return radius * (1.0 + 0.18 * Math.sin(Math.PI * u)) * (1.0 + 0.1 * Math.pow(Math.max(0.0, Math.sin(a)), 4.0));
            }

            @Override
            public int colour(double s, double a) {
                return mix(SHELL, SHELL_HI, 0.3 + 0.5 * Math.max(0.0, Math.sin(a)));
            }
        }, look.alpha());
        double[][] f = spine.at(headEnd);
        double[] p = f[0];
        double[] t = f[1];
        double[] n = f[2];
        double[] b = f[3];
        double jawLength = Math.max(radius * 2.4, spine.length() * (1.0 - headEnd) * 1.1);
        // Ferocious: it gapes wide (close on 90 degrees), snapping shut hard.
        double open = Math.toRadians(8.0 + 112.0 * clamp01(look.jaw()));
        int stations = look.fine() ? 5 : 3;
        int half = look.fine() ? 5 : 3;
        double[][][][] jaws = new double[2][][][];
        for (int jaw = -1; jaw <= 1; jaw += 2) {
            double[] out = mul(n, jaw);
            double[] dir = normalize(add(mul(t, Math.cos(open * 0.5)), mul(out, Math.sin(open * 0.5))));
            double[] inward = mul(out, -1.0);
            double[][][] rings = new double[stations + 1][half + 1][];
            jaws[(jaw + 1) / 2] = rings;
            double[][] centres = new double[stations + 1][];
            for (int i = 0; i <= stations; i++) {
                double u = i / (double)stations;
                double[] c = add(add(p, mul(dir, jawLength * u)), mul(inward, radius * 0.45 * u * u));
                double r = radius * 0.95 * Math.pow(Math.max(0.0, 1.0 - u), 0.8) + 1.0E-4;
                centres[i] = c;
                for (int k = 0; k <= half; k++) {
                    double theta = Math.PI * k / half;
                    rings[i][k] = add(c, add(mul(b, Math.cos(theta) * r), mul(out, Math.sin(theta) * r * 0.75)));
                }
            }
            for (int i = 0; i < stations; i++) {
                double[] centre = lerp(centres[i], centres[i + 1], 0.5);
                double u = (i + 0.5) / stations;
                int shell = argb(mix(mix(SHELL, SHELL_HI, 0.45), SPIKE_TIP, u * 0.7), look.alpha());
                for (int k = 0; k < half; k++) {
                    facet(sink, rings[i][k], rings[i + 1][k], rings[i + 1][k + 1], rings[i][k + 1], add(centre, mul(out, -radius * 0.3)), shell, shell,
                        shell, shell);
                }
                // The palate between the rims: wet, dark red.
                int gum = argb(mix(GUM, THROAT, u), look.alpha());
                GraftMesh.facet(sink, rings[i][0], rings[i + 1][0], rings[i + 1][half], rings[i][half], add(centre, mul(out, radius)), gum, gum, gum, gum);
            }
            // Teeth down both rims, pointing in at the other jaw (as many as it ever had: its ferocity is in how it moves, not in
            // more teeth).
            int count = look.fine() ? 5 : 3;
            double bared = 0.3 + 0.7 * smooth(look.jaw() / 0.5);
            for (int edge = 0; edge <= 1; edge++) {
                int k = edge == 0 ? 0 : half;
                double[] a = rings[0][k];
                double[] z = rings[stations - 1][k];
                double[] bite = normalize(add(inward, mul(dir, 0.3)));
                GraftMesh.teeth(sink, a, z, bite, count, radius * 0.7 * bared, radius * 0.12, look.seed() * 53 + jaw * 7 + edge, look.alpha());
            }
            // Raw red gums glowing along both rims.
            if (look.fine()) {
                for (int edge = 0; edge <= 1; edge++) {
                    int k = edge == 0 ? 0 : half;
                    for (int i = 0; i < stations - 1; i++) {
                        double[] g0 = lerp(rings[i][k], centres[i], 0.12);
                        double[] g1 = lerp(rings[i + 1][k], centres[i + 1], 0.12);
                        double[] h0 = lerp(rings[i][k], centres[i], 0.28);
                        double[] h1 = lerp(rings[i + 1][k], centres[i + 1], 0.28);
                        int gum = argb(mix(VEIN, GUM, 0.35 + 0.4 * i / (double)stations), look.alpha());
                        glow(sink, g0, g1, h1, h0, gum, gum, gum, gum);
                    }
                }
            }
        }
        // Drool: wet strings stretched from jaw to jaw across the gape, sagging, swaying.
        if (look.fine() && look.jaw() > 0.25) {
            for (int d = 0; d < 4; d++) {
                double u = 0.3 + 0.15 * d;
                int i = Math.min(stations - 1, (int)Math.round(u * stations));
                double side = (d % 2 == 0 ? 0.3 : 0.7);
                double[] top = lerp(jaws[0][i][0], jaws[0][i][half], side);
                double[] bottom = lerp(jaws[1][i][0], jaws[1][i][half], 1.0 - side);
                double sway = 0.1 * radius * Math.sin(look.time() * 0.2 + d * 1.7);
                double[] sag = add(lerp(top, bottom, 0.5), add(mul(t, -0.25 * radius), mul(b, sway)));
                double thin = radius * 0.035;
                double[] across = mul(b, thin);
                int drool = argb(0xD06878, look.alpha() * 0.85);
                int wet = argb(0xF0A8B4, look.alpha() * 0.85);
                glow(sink, sub(top, across), add(top, across), add(sag, across), sub(sag, across), drool, drool, wet, wet);
                glow(sink, sub(sag, across), add(sag, across), add(bottom, across), sub(bottom, across), wet, wet, drool, drool);
            }
        }
        // The cheeks: wet flesh stretched from jaw to jaw down both sides of the mouth, so it is a cavity and never a hole to see
        // through; they reach most of the way to the tips, slack when it is shut, drawn taut as it opens.
        int reach = Math.max(1, (int)Math.round(stations * 0.8));
        for (int edge = 0; edge <= 1; edge++) {
            int k = edge == 0 ? 0 : half;
            for (int i = 0; i < reach; i++) {
                double u0 = i / (double)stations;
                double u1 = (i + 1) / (double)stations;
                // Inset a little from the rims, so the teeth stand clear of it.
                double[] a0 = lerp(jaws[0][i][k], jaws[1][i][k], 0.06);
                double[] a1 = lerp(jaws[0][i + 1][k], jaws[1][i + 1][k], 0.06);
                double[] b0 = lerp(jaws[0][i][k], jaws[1][i][k], 0.94);
                double[] b1 = lerp(jaws[0][i + 1][k], jaws[1][i + 1][k], 0.94);
                double[] sideOut = mul(b, edge == 0 ? 1.0 : -1.0);
                double sag = radius * 0.12 * (1.0 - 0.5 * clamp01(look.jaw()));
                double[] m0 = add(lerp(a0, b0, 0.5), mul(sideOut, -sag));
                double[] m1 = add(lerp(a1, b1, 0.5), mul(sideOut, -sag));
                int cheek = argb(mix(GUM, THROAT, 0.35 + 0.5 * u0), look.alpha());
                int deep = argb(mix(GUM, THROAT, 0.35 + 0.5 * u1), look.alpha());
                double[] centre = lerp(centres(p, jaws, i), centres(p, jaws, i + 1), 0.5);
                GraftMesh.facet(sink, a0, a1, m1, m0, centre, cheek, deep, deep, cheek);
                GraftMesh.facet(sink, m0, m1, b1, b0, centre, cheek, deep, deep, cheek);
            }
        }
        // The throat: a dark hole into the head between the jaws, its rim glowing; it closes the whole end of the head.
        double ring = radius * 0.98;
        int segments = look.fine() ? 8 : 5;
        for (int k = 0; k < segments; k++) {
            double a0 = Math.PI * 2.0 * k / segments;
            double a1 = Math.PI * 2.0 * (k + 1) / segments;
            double[] q0 = add(p, add(mul(b, Math.cos(a0) * ring), mul(n, Math.sin(a0) * ring * 0.8)));
            double[] q1 = add(p, add(mul(b, Math.cos(a1) * ring), mul(n, Math.sin(a1) * ring * 0.8)));
            double[] deep = sub(p, mul(t, radius * 0.6));
            int rim = argb(GUM, look.alpha());
            int dark = argb(THROAT, look.alpha());
            glow(sink, q0, q1, lerp(q1, deep, 0.6), lerp(q0, deep, 0.6), rim, rim, dark, dark);
        }
        if (mandibles) {
            for (int side = -1; side <= 1; side += 2) {
                double[] base = add(p, mul(b, side * radius * 0.9));
                double swing = Math.toRadians(15.0 + 50.0 * clamp01(look.jaw()));
                double[] dir = normalize(add(mul(t, Math.cos(swing)), mul(b, side * Math.sin(swing))));
                horn(sink, base, dir, mul(b, -side), jawLength * 1.05, radius * 0.32, look.fine() ? 5 : 4, look.fine() ? 4 : 2, SHELL_HI, SPIKE_TIP,
                    look.alpha());
            }
        }
    }

    /** The middle of the mouth at a station: halfway between the two jaws' rims there. */
    private static double[] centres(double[] p, double[][][][] jaws, int i) {
        int half = jaws[0][i].length - 1;
        double[] sum = new double[3];
        for (double[][][] rings : jaws) {
            sum = add(sum, add(rings[i][0], rings[i][half]));
        }
        return mul(sum, 0.25);
    }

    // ---------------------------------------------------------------- four thorns

    /** A thorn limb's half-width along its core (share), to where its great point begins. */
    public static double thornWidth(double s) {
        double c = clamp01(s);
        return emerge(c, 0.15) * (1.0 + 0.14 * Math.exp(-Math.pow((c - 0.2) / 0.08, 2.0))) * (1.0 - 0.5 * Math.pow(c, 1.3));
    }

    /** Where a thorn limb's point begins. */
    public static final double POINT = 0.82;

    public static void thorn(UkakuModel.Sink sink, KakujaModel.Spine spine, Look look) {
        double w = look.width();
        int seed = look.seed();
        int ridges = Math.max(5, Math.min(12, (int)Math.round(spine.length() * POINT / (1.5 * w))));
        double span = POINT / ridges;
        int stations = look.fine() ? ridges * 4 : ridges * 2;
        int sides = look.fine() ? 10 : 6;
        GraftMesh.tube(sink, spine, 0.0, POINT, stations, sides, new GraftMesh.Surface() {
            @Override
            public double radius(double s, double a) {
                double phase = (s / span) % 1.0;
                double ridge = Math.exp(-Math.pow((phase - 0.5) / 0.14, 2.0));
                double serration = 0.06 * ridge * Math.max(0.0, Math.cos(a * 4.0 + (int)(s / span)));
                return w * thornWidth(s) * (1.0 + 0.2 * ridge + serration + 0.6 * muscle(s, a, seed) * (1.0 - ridge));
            }

            @Override
            public int colour(double s, double a) {
                double phase = (s / span) % 1.0;
                double ridge = Math.exp(-Math.pow((phase - 0.5) / 0.16, 2.0));
                double m = muscle(s, a, seed);
                int flesh = mix(FLESH_DARK, FLESH_HI, clamp01(0.45 + m * 4.0));
                int shell = mix(SHELL, SHELL_HI, 0.3 + 0.5 * Math.max(0.0, Math.sin(a)));
                return mix(flesh, shell, ridge);
            }
        }, look.alpha());
        veins(sink, spine, look, w * 0.9, 0.05, POINT - 0.05, 3, GraftModel::thornWidth);
        // Thorns on every ridge, three or four round it, turned a little from ridge to ridge, raked toward the point.
        int perRidge = look.fine() ? 4 : 3;
        for (int i = 1; i < ridges; i++) {
            double s = (i + 0.5) * span;
            double r = w * thornWidth(s) * 1.15;
            for (int k = 0; k < perRidge; k++) {
                double a = Math.PI * 2.0 * k / perRidge + i * 0.9 + seed;
                double[][] at = GraftMesh.onSurface(spine, s, a, r * 0.95);
                double rake = Math.toRadians(42.0 - 20.0 * Math.min(1.8, look.flare()));
                double[] dir = normalize(add(mul(at[1], Math.cos(rake)), mul(at[2], Math.sin(rake))));
                double length = r * (1.5 - 0.7 * s / POINT) * (0.8 + 0.5 * hash(seed * 13 + i * 5 + k)) * Math.max(0.4, look.flare());
                horn(sink, at[0], dir, at[2], length, r * 0.2, look.fine() ? 5 : 4, look.fine() ? 3 : 2, SPIKE_ROOT, SPIKE_TIP, look.alpha());
            }
        }
        // The great point: a long, flattened blade of carapace with serrated edges.
        double r0 = w * thornWidth(POINT) * 1.05;
        GraftMesh.tube(sink, spine, POINT, 1.0, look.fine() ? 6 : 3, look.fine() ? 8 : 6, new GraftMesh.Surface() {
            @Override
            public double radius(double s, double a) {
                double u = (s - POINT) / (1.0 - POINT);
                double flat = 0.62 + 0.38 * Math.abs(Math.cos(a));
                return r0 * Math.pow(Math.max(0.0, 1.0 - u), 0.85) * flat;
            }

            @Override
            public int colour(double s, double a) {
                double u = (s - POINT) / (1.0 - POINT);
                return mix(mix(SHELL_HI, SHELL_EDGE, Math.abs(Math.cos(a)) * 0.6), SPIKE_TIP, u);
            }
        }, look.alpha());
        for (int k = 0; k < 4; k++) {
            double s = POINT + (1.0 - POINT) * (0.12 + 0.2 * k);
            double u = (s - POINT) / (1.0 - POINT);
            double r = r0 * Math.pow(1.0 - u, 0.85);
            for (int side = -1; side <= 1; side += 2) {
                double[][] at = GraftMesh.onSurface(spine, s, side > 0 ? 0.0 : Math.PI, r);
                double[] dir = normalize(add(mul(at[1], 0.7), mul(at[2], -0.7)));
                horn(sink, at[0], dir, mul(at[2], -1.0), r * 0.8, r * 0.2, 4, 2, SHELL_EDGE, SPIKE_TIP, look.alpha());
            }
        }
    }

    // ---------------------------------------------------------------- the many-eyed kagune

    /**
     * A crescent's half-width along it (share), as the manga's: thin where it leaves his back, swelling to its full thickness and
     * holding it, lumpy, through most of its length, then drawn out over its last third into a sharp hooked blade.
     */
    public static double crescentWidth(double s) {
        double c = clamp01(s);
        double swell = 1.0 + 0.12 * Math.sin(Math.PI * Math.min(1.0, c / 0.72));
        double end = c < CRESCENT_HOLD ? 1.0 : Math.pow(Math.max(0.0, 1.0 - (c - CRESCENT_HOLD) / (1.0 - CRESCENT_HOLD)), 1.15);
        return emerge(c, 0.24) * swell * end;
    }

    /** Where a crescent stops holding its thickness and begins its taper. */
    static final double CRESCENT_HOLD = 0.74;

    /** A crescent is a little flattened: its sides (where the eyes are) this much wider than it is deep; round, not a slab. */
    static final double FLAT = 0.84;
    /** How far its crest stands out of its back (share of its half-width). */
    static final double CREST = 0.32;

    /** The eyes along a crescent: where (s), on which face (+1 its front, -1 its back), and how big (share of the face). */
    static final double[][] CRESCENT_EYES = {{0.24, 1.0, 1.0}, {0.36, -1.0, 0.95}, {0.5, 1.0, 0.9}, {0.62, -1.0, 0.85}, {0.74, 1.0, 0.72}};

    /**
     * One crescent of the many-eyed kagune: a vast lumpy tentacle of black and crimson flesh, flattened, its faces set with great
     * almond eyes in thick black rims that blink, roll toward what it is after and stare, clusters of spikes on its edges that grow
     * when it strikes.
     */
    public static void crescent(UkakuModel.Sink sink, KakujaModel.Spine spine, Look look) {
        double w = look.width();
        int seed = look.seed();
        int stations = look.fine() ? Math.max(28, Math.min(52, (int)Math.round(spine.length() * 6.0))) : 14;
        int sides = look.fine() ? 16 : 8;
        GraftMesh.tube(sink, spine, 0.0, 1.0, stations, sides, new GraftMesh.Surface() {
            @Override
            public double radius(double s, double a) {
                double ca = Math.cos(a);
                double sa = Math.sin(a) / FLAT;
                double ellipse = 1.0 / Math.sqrt(ca * ca + sa * sa);
                double lump = 0.16 * (noise(seed, s * 9.0, a * 1.4) - 0.5) + 0.05 * Math.sin(4.0 * a + 9.0 * s + seed);
                // A hard crest along its back (angle pi): the sharp outer edge of the crescent.
                double crest = CREST * Math.pow(Math.max(0.0, -ca), 18.0) * smooth((s - 0.12) / 0.2) * (1.0 - smooth((s - 0.9) / 0.1));
                return w * crescentWidth(s) * (ellipse * (1.0 + lump * (1.0 - 0.6 * s)) + crest);
            }

            @Override
            public int colour(double s, double a) {
                double crease = noise(seed + 3, s * 9.0, a * 1.4);
                int base = mix(MASS, MASS_CREASE, clamp01((0.5 - crease) * 2.2));
                return mix(base, FLESH_DEEP, clamp01(Math.abs(Math.cos(a))) * 0.4);
            }
        }, look.alpha());
        tendrils(sink, spine, look);
        veins(sink, spine, look, w * 0.75, 0.04, 0.88, 2, GraftModel::crescentWidth);
        int eyes = look.fine() ? CRESCENT_EYES.length : 3;
        double[] root = spine.at(0.0)[0];
        for (int e = 0; e < eyes; e++) {
            double s = CRESCENT_EYES[e][0];
            double r = w * crescentWidth(s);
            double[][] f = spine.at(s);
            // Wrapped round him as a wall (Living Fortress), every eye looks out, on the face away from him.
            double face = look.outward() ? (dot(sub(f[0], root), f[2]) - (f[0][1] - root[1]) * f[2][1] >= 0.0 ? 1.0 : -1.0) : CRESCENT_EYES[e][1];
            double[] out = mul(f[2], face);
            double[] at = add(f[0], mul(out, r * FLAT * 0.98));
            double period = 70.0 + 80.0 * hash(seed * 37 + e);
            double phase = (look.time() + period * hash(seed * 37 + e + 1)) % period;
            double blink = phase < 6.0 ? Math.sin(Math.PI * phase / 6.0) : 0.0;
            double open = 1.0 - blink * (1.0 - 0.6 * look.stare());
            double size = CRESCENT_EYES[e][2];
            // (Always the almond eyes: the user rejected the dark disc eyes outright.)
            GraftMesh.almond(sink, at, out, f[1], r * 3.0 * size, r * 1.6 * size, look.gaze(), open, look.stare(), MASS, 0x7A0A14, look.fine(),
                look.alpha());
        }
        // Clusters of spikes on its edges: barely buds at rest, bristling out when the kagune calls them.
        double grow = Math.max(0.15, look.flare() - 0.7);
        int clusters = look.fine() ? 4 : 2;
        for (int c = 0; c < clusters; c++) {
            double s = 0.3 + 0.16 * c + 0.04 * hash(seed + c);
            double around = c % 2 == 0 ? 0.0 : Math.PI;
            double r = w * crescentWidth(s);
            int count = 3 + (int)(hash(seed * 3 + c) * 3.0);
            for (int k = 0; k < count; k++) {
                double a = around + (k - count * 0.5) * 0.3;
                double[][] at = GraftMesh.onSurface(spine, s + (k % 2) * 0.02, a, r * 0.95);
                double[] dir = normalize(add(mul(at[1], 0.95), mul(at[2], 0.3)));
                horn(sink, at[0], dir, at[2], r * (0.55 + 0.45 * hash(seed * 7 + c * 11 + k)) * grow * 1.5, r * 0.14, 4, 2, SPIKE_ROOT, SPIKE_TIP,
                    look.alpha());
            }
        }
    }

    /** How many whip tendrils sprout off a crescent (fine), where along it, how long (blocks per unit of its width). */
    static final int TENDRILS = 3;

    /**
     * Thin, sharp tendrils off a crescent's far end, as the manga's mass bristles with them there: each out of it near its point,
     * reaching on past it, bending away and curling over like a claw, drawn out to a needle, writhing slowly on its own beat.
     */
    static void tendrils(UkakuModel.Sink sink, KakujaModel.Spine spine, Look look) {
        double w = look.width();
        int seed = look.seed();
        int count = look.fine() ? TENDRILS : 1;
        for (int t = 0; t < count; t++) {
            double s = 0.68 + 0.1 * t + 0.05 * (hash(seed * 5 + t) - 0.5);
            double r = w * crescentWidth(s);
            double a = Math.PI * 2.0 * hash(seed * 9 + t);
            double[][] at = GraftMesh.onSurface(spine, s, a, r * 0.92);
            double[] out = at[1];
            double[] along = at[2];
            double[] across = normalize(GraftMesh.cross(out, along));
            double length = w * (2.2 + 2.4 * hash(seed * 17 + t));
            double phase = look.time() * (0.05 + 0.03 * hash(seed * 23 + t)) + 6.0 * hash(seed + t * 31);
            int n = 7;
            double[][] points = new double[n][];
            for (int i = 0; i < n; i++) {
                double f = i / (double)(n - 1);
                // On past the point along it, bending away from it and curling over at the end like a claw.
                double bend = Math.sin(f * 2.2 + phase) * 0.22 * f;
                double[] p = add(at[0], mul(out, length * (0.45 * f - 0.3 * f * f * f)));
                p = add(p, mul(along, length * (0.75 * f + 0.1 * f * f)));
                points[i] = add(p, mul(across, length * bend));
            }
            KakujaModel.Spine whip = com.hex.kagune.control.ukaku.CentipedeTail.through(points, 24, 1, out);
            double thick = r * 0.16;
            GraftMesh.tube(sink, whip, 0.0, 1.0, look.fine() ? 10 : 5, look.fine() ? 5 : 4, new GraftMesh.Surface() {
                @Override
                public double radius(double u, double angle) {
                    return thick * Math.pow(Math.max(0.0, 1.0 - u), 0.85) * (0.6 + 0.4 * smooth(u / 0.15));
                }

                @Override
                public int colour(double u, double angle) {
                    return mix(MASS, MASS_CREASE, 0.3 + 0.4 * (1.0 - u));
                }
            }, look.alpha());
        }
    }

    // ---------------------------------------------------------------- the kakuja's colossi

    /** A colossus's half-width along it (share): thick far out of the back, swelling to its great head. */
    public static double colossusWidth(double s) {
        double c = clamp01(s);
        return (0.6 + 0.4 * smooth(c / 0.45)) * (1.0 - 0.12 * smooth((c - 0.7) / 0.3));
    }

    /** Where a colossus's head begins. */
    public static final double COLOSSUS_HEAD = 0.86;

    public static void colossus(UkakuModel.Sink sink, KakujaModel.Spine spine, Look look) {
        double w = look.width();
        int plates = Math.max(9, Math.min(18, (int)Math.round(spine.length() * COLOSSUS_HEAD / (1.7 * w))));
        segments(sink, spine, look, w, 0.0, COLOSSUS_HEAD, plates, GraftModel::colossusWidth, true);
        double r = w * colossusWidth(COLOSSUS_HEAD) * 1.12;
        maw(sink, spine, look, COLOSSUS_HEAD, r, true);
        // Two eyes on top of the head, over the mouth.
        double s = COLOSSUS_HEAD + (1.0 - COLOSSUS_HEAD) * 0.2;
        for (int side = -1; side <= 1; side += 2) {
            double[][] at = GraftMesh.onSurface(spine, s, Math.PI * 0.5 - side * 0.7, r * 1.05);
            eyeAt(sink, spine, look, at, s, r * 0.38, look.seed() * 7 + side + 40);
        }
    }

    /**
     * 1.0.31: a colossus's head alone, all of {@code spine} (its width {@code look.width()}): the swell, the maw splitting into its two
     * toothed jaws, the mandibles at its sides, the two eyes over the mouth. The Dragon's heads are these.
     */
    public static void colossusHead(UkakuModel.Sink sink, KakujaModel.Spine spine, Look look) {
        double r = look.width();
        maw(sink, spine, look, 0.0, r, true);
        double s = 0.2 * 0.35;
        for (int side = -1; side <= 1; side += 2) {
            double[][] at = GraftMesh.onSurface(spine, s + 0.12, Math.PI * 0.5 - side * 0.7, r * 1.05);
            eyeAt(sink, spine, look, at, s + 0.12, r * 0.38, look.seed() * 7 + side + 40);
        }
    }

    /** The point of a limb's mouth (where a bite closes), as {position, toward the point}, for placing what it holds. */
    public static double[][] mouth(KakujaModel.Spine spine, double from) {
        double[][] f = spine.at(from + (1.0 - from) * 0.6);
        return new double[][] {f[0], f[1]};
    }

    /** A direction in the spine's frame for the eyes' gaze: the unit vector from {@code eye} toward {@code target}. */
    public static double[] gazeToward(double[] eye, double[] target) {
        return target == null ? null : normalize(sub(target, eye));
    }
}
