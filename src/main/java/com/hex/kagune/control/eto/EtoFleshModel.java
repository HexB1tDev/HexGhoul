package com.hex.kagune.control.eto;

import com.hex.kagune.control.ayato.CentipedeTail;
import com.hex.kagune.control.ayato.KakujaModel;
import com.hex.kagune.control.ayato.UkakuModel;

/**
 * Eto's pale flesh (6.1.0; pure, tested): one great living mass out of her upper back, up over her right shoulder and out to
 * whatever it is doing: raking (the swipe), reaching with a hand that opens as it nears, closing round what it caught (the grab),
 * lashing (thrown with what it held). Its trunk is never a cylinder: lumps and folds run round and along it and shift as it
 * lives, smaller tendrils writhe off it, and it swells over the shoulder and tapers to the tip.
 *
 * <p>Everything here is in rig space (blocks; x to her right, y up from her feet, z forward), the root, the back's outward
 * direction and the shoulder taken from her posed body as it is drawn ({@code BodyFrame}), so the flesh rides every twist,
 * lean and lunge.
 */
public final class EtoFleshModel {
    /** Pale flesh, its folds, its creases, its sheen. */
    public static final int FLESH = 0xFFE9BFAE;
    public static final int FOLD = 0xFFCC9C8C;
    public static final int CREASE = 0xFFA06E64;
    public static final int SHEEN = 0xFFF7E0D3;

    /** The trunk's thickness (blocks): at the root, the swell over the shoulder, the tip. */
    public static final double ROOT_RADIUS = 0.16;
    public static final double SWELL_RADIUS = 0.36;
    public static final double TIP_RADIUS = 0.13;
    /** Rings along the trunk (the fewest: a long one has more) and round each ring. */
    static final int RINGS = 40;
    static final int SIDES = 12;

    /** What the tip is. */
    public enum Shape {
        /** Great uneven tendrils fanned up and down off a knot of flesh: the swipe's rake (before 6.2.0). */
        RAKE,
        /**
         * 6.2.0, the swipe's blade: one long sabre of bone lying flat out of the end of the flesh, its leading edge honed sharp, a
         * needle point (see {@link EtoFleshModel#blade}).
         */
        BLADE,
        /** A knot of flesh, great tendrils off it thrown wide, reaching. */
        HAND_OPEN,
        /** The tendrils wrapped round what it holds. */
        HAND_CLOSED,
        /** A bare tip (thrown out, drawn back). */
        LASH,
        /**
         * The hammer: a club of flesh grown round her leg from the shin, past the foot, swelling to a great lumpy head ({@link
         * Pose#grip} is then its thickest radius).
         */
        CLUB,
        /**
         * 6.2.0, a tentacle of Watch This: a slimmer length of the same flesh out of her upper back, its end knotted round a long
         * hooked beak of bone, for impaling ({@link EtoFleshModel#beak}).
         */
        BEAK,
        /**
         * 6.2.0, the Mutated Arm: her right arm turned to the flesh, swollen ({@link Pose#grip} its thickest radius), its great
         * hand open as it reaches...
         */
        ARM_OPEN,
        /** ...shut on what it holds... */
        ARM_SHUT,
        /** ...and folded into a fist as it draws back into her, its claws going back in with the mutation. */
        ARM_FIST,
        /** 6.2.0, True Anonymity: the hammer's club ({@link #CLUB}) bristling with bone spikes all over its flesh ({@link #boneSpikes}). */
        CLUB_SPIKED
    }

    /**
     * The flesh this frame: the {@code path} its trunk runs through (rig space, blocks: its root in her upper back, out of the back,
     * over her right shoulder, the arch, the tip; see {@link #path}), her {@code up}, her body's middle line (hips, neck, head top:
     * the tendrils writhe away from it), how {@code grown} it is (0 in her back, 1 all out), what the tip is, how wide what it grips
     * is (blocks), the time (ticks: it never stops moving), a seed, and how to keep what grows off the trunk (its tendrils, the knot
     * and the tendrils at its tip) clear of her body.
     */
    public record Pose(double[][] path, double[] up, double[][] axis, double grown, Shape shape, double grip, double time, int seed, Keep keep) {
    }

    /** Her body, for what grows off the flesh to keep clear of. */
    public interface Keep {
        /** The point moved the shortest way to {@code radius} blocks clear of her body, or itself. */
        double[] clear(double[] point, double radius);

        /** How deep (blocks) a ball of {@code radius} round the point sinks into her body; 0 or less when it is clear. */
        double depth(double[] point, double radius);
    }

    /** Nothing to keep clear of. */
    public static final Keep ANYWHERE = new Keep() {
        @Override
        public double[] clear(double[] point, double radius) {
            return point;
        }

        @Override
        public double depth(double[] point, double radius) {
            return -1.0;
        }
    };
    /** The most the lumps swell a tube (a fraction of its radius). */
    static final double LUMPED = 1.36;

    private EtoFleshModel() {
    }

    // ------------------------------------------------------------------ the trunk

    /**
     * The trunk's whole path (all out) before it is kept clear of her: from its {@code root} on her upper back, straight out of
     * the back ({@code back}: outward there), rearing up over her right {@code shoulder} (well above it) and arching out to
     * {@code tip}; {@code fromAbove}, the arch rises high and comes down onto the tip from above (what it grabs, it grabs from
     * over the top). While it is still coming out it has grown only so far along this path ({@link #grownAlong}).
     */
    public static double[][] path(double[] root, double[] back, double[] shoulder, double[] up, double[] tip, boolean fromAbove) {
        // Straight out of the back first, whichever way it then goes.
        double[] stem = add(root, mul(back, 0.3));
        double[] out = add(root, add(mul(back, 0.7), mul(up, 0.05)));
        double[] over = add(shoulder, add(mul(up, 0.72), mul(back, 0.6)));
        double reach = length(sub(tip, over));
        if (!fromAbove) {
            double[] arch = add(lerp(over, tip, 0.42), mul(up, Math.min(1.4, 0.12 + reach * 0.16)));
            return new double[][] {root, stem, out, over, arch, tip};
        }
        // High enough over it for the knot to sit above what it grabs and the tendrils to come down round it.
        double drop = Math.max(1.3, Math.min(2.0, 0.9 + reach * 0.1));
        // Over the top of it, a little to her side of it.
        double[] toward = sub(over, tip);
        toward = sub(toward, mul(up, dot(toward, up)));
        toward = length(toward) < 1.0E-6 ? new double[] {0.0, 0.0, 0.0} : normalize(toward);
        double[] above = add(tip, add(mul(up, drop), mul(toward, drop * 0.3)));
        double[] arch = add(lerp(over, above, 0.5), mul(up, Math.min(1.6, 0.25 + reach * 0.18)));
        return new double[][] {root, stem, out, over, arch, above, tip};
    }

    /**
     * The part of {@code path} the flesh has grown along, {@code grown} (0..1) of its length from the root, as points evenly spaced
     * along it (so that a trunk through them is as far along at {@code s} as {@code s} of its length): it comes out the way it will
     * go, its tip leading. Null when nothing is out yet.
     */
    public static double[][] grownAlong(double[][] path, double[] up, double grown) {
        double g = clamp01(grown);
        double[][] dense = trunk(path, up, 96).p();
        double[] lengths = new double[dense.length];
        for (int i = 1; i < dense.length; i++) {
            lengths[i] = lengths[i - 1] + length(sub(dense[i], dense[i - 1]));
        }
        double end = lengths[dense.length - 1] * g;
        if (end < 0.02) {
            return null;
        }
        // Close enough together to keep the way it leaves her back, however long it is.
        int points = Math.max(13, Math.min(64, (int)Math.ceil(end / 0.25) + 1));
        double[][] out = new double[points][];
        int j = 1;
        for (int k = 0; k < points; k++) {
            double want = end * k / (points - 1);
            while (j < dense.length - 1 && lengths[j] < want) {
                j++;
            }
            double span = Math.max(1.0E-9, lengths[j] - lengths[j - 1]);
            out[k] = lerp(dense[j - 1], dense[j], clamp01((want - lengths[j - 1]) / span));
        }
        return out;
    }

    /** The trunk's control points, as far as it has grown (root first, tip last). */
    public static double[][] controls(Pose pose) {
        return pose.path();
    }

    public static KakujaModel.Spine trunk(Pose pose) {
        // A ring every eighth of a block or so (the tube has as many rings as its spine has samples).
        double[][] path = pose.path();
        double length = 0.0;
        for (int i = 1; i < path.length; i++) {
            length += length(sub(path[i], path[i - 1]));
        }
        return trunk(path, pose.up(), Math.max(RINGS, Math.min(120, (int)Math.ceil(length * 8.0))));
    }

    public static KakujaModel.Spine trunk(double[][] path, double[] up, int samples) {
        return CentipedeTail.through(path, samples, 1, up);
    }

    /** Blocks out from her back where the trunk is thickest (on a shorter trunk, a little past its middle). */
    static final double SWELL_AT = 0.9;

    /**
     * The trunk's radius {@code s} of the way along it (0 root .. 1 tip; by length), the trunk {@code length} blocks long, before its
     * lumps: narrow where it comes out of her back, swelling a block out, tapering to the tip; thinner while it is still coming out.
     */
    public static double radius(double s, double grown, double length) {
        double g = 0.25 + 0.75 * clamp01(grown);
        double at = clamp01(s) * length;
        double swell = Math.min(SWELL_AT, length * 0.55);
        double base = at < swell ? ROOT_RADIUS + (SWELL_RADIUS - ROOT_RADIUS) * smooth(at / swell)
            : SWELL_RADIUS + (TIP_RADIUS - SWELL_RADIUS) * smooth((at - swell) / Math.max(1.0E-6, length - swell));
        // Knotted along its length, like gathered muscle.
        double knots = 1.0 + 0.1 * Math.sin(at * 4.3 + 0.7) * smooth(at / 0.5);
        return g * (base + 0.04 * Math.exp(-((at - swell) * (at - swell)) / 0.08)) * knots;
    }

    /** The radius {@code s} of the way along whatever {@code pose} is (the trunk, or the hammer's club), {@code length} long. */
    public static double profile(Pose pose, double s, double length) {
        return switch (pose.shape()) {
            case CLUB, CLUB_SPIKED -> clubRadius(s, pose.grown(), pose.grip(), length);
            case BEAK -> radius(s, pose.grown(), length) * TENTACLE_GIRTH;
            case ARM_OPEN, ARM_SHUT, ARM_FIST -> armRadius(s, pose.grip(), length);
            default -> radius(s, pose.grown(), length);
        };
    }

    /** How much slimmer a tentacle is than the flesh that sweeps and grabs. */
    public static final double TENTACLE_GIRTH = 0.62;

    /**
     * 6.2.0, a tentacle's path (all out): from its {@code root} in her upper back, straight out of it ({@code back}), up over the
     * {@code shoulder} on its side (well above it), and round to its {@code knot} (where the beak is gripped), coming at it along
     * {@code dir} (the way the beak points), so the beak carries straight on out of it.
     */
    public static double[][] tentaclePath(double[] root, double[] back, double[] shoulder, double[] up, double[] knot, double[] dir) {
        double[] stem = add(root, mul(back, 0.25));
        double[] out = add(root, add(mul(back, 0.6), mul(up, 0.08)));
        double[] over = add(shoulder, add(mul(up, 0.62), mul(back, 0.45)));
        double reach = length(sub(knot, over));
        double[] d = normalize(dir);
        double[] before = sub(knot, mul(d, Math.min(1.0, 0.25 + reach * 0.3)));
        double[] arch = add(lerp(over, before, 0.45), mul(up, Math.min(1.1, 0.2 + reach * 0.14)));
        return new double[][] {root, stem, out, over, arch, before, knot};
    }

    /**
     * The hammer's club {@code s} of the way along it (from her shin), {@code length} long, at its thickest {@code girth}: tight
     * round the shin, swelling past the foot into a great head lumped like a fist of muscle, rounded off at the end; thinner while
     * it is still growing.
     */
    public static double clubRadius(double s, double grown, double girth, double length) {
        double g = 0.3 + 0.7 * clamp01(grown);
        double c = clamp01(s);
        double head = smooth(c / 0.78);
        double round = c > 0.86 ? Math.sqrt(Math.max(0.0, 1.0 - (c - 0.86) * (c - 0.86) / (0.14 * 0.14))) : 1.0;
        double at = c * length;
        double bulges = 1.0 + 0.12 * Math.sin(at * 4.7 + 1.3) * head + 0.06 * Math.sin(at * 9.3 + 0.4) * head;
        return g * (CLUB_SHIN + (girth - CLUB_SHIN) * head) * (0.3 + 0.7 * round) * bulges;
    }

    /** How tight the club is round her shin where it starts (blocks: the leg is a quarter block thick). */
    public static final double CLUB_SHIN = 0.17;

    /** The radius at the tip. */
    public static double tipRadius(double grown) {
        return (0.25 + 0.75 * clamp01(grown)) * TIP_RADIUS;
    }

    /** How far out (a fraction of the radius) the surface is at {@code s}, round at {@code angle}: lumps and folds, alive. */
    public static double lumps(double s, double angle, double time, int seed) {
        return 0.17 * Math.sin(3.0 * angle + s * 11.0 + time * 0.31 + seed)
            + 0.11 * Math.sin(5.0 * angle - s * 17.0 + time * 0.47 + seed * 1.7)
            + 0.07 * Math.sin(2.0 * angle + s * 29.0 - time * 0.23 + seed * 0.6);
    }

    // ------------------------------------------------------------------ the mesh

    /** The whole mass as quads into {@code sink}: the trunk, the tendrils off it, the tip. */
    public static void mesh(UkakuModel.Sink sink, Pose pose) {
        if (pose.grown() <= 0.01) {
            return;
        }
        KakujaModel.Spine spine = trunk(pose);
        double length = spine.length();
        tube(sink, spine, s -> profile(pose, s, length), pose.time(), pose.seed(), 0.0, 1.0, true);
        strands(sink, spine, pose);
        boolean arm = pose.shape() == Shape.ARM_OPEN || pose.shape() == Shape.ARM_SHUT || pose.shape() == Shape.ARM_FIST;
        if (!arm) {
            tendrils(sink, spine, pose); // (an arm is muscle and its hand, nothing hanging off it)
        }
        switch (pose.shape()) {
            case RAKE -> rake(sink, spine, pose);
            case BLADE -> blade(sink, spine, pose);
            case HAND_OPEN -> hand(sink, spine, pose, 0.0);
            case HAND_CLOSED -> hand(sink, spine, pose, 1.0);
            case LASH -> cap(sink, spine, pose, tipRadius(pose.grown()));
            case CLUB -> cap(sink, spine, pose, profile(pose, 1.0, length));
            case CLUB_SPIKED -> {
                cap(sink, spine, pose, profile(pose, 1.0, length));
                boneSpikes(sink, spine, pose, length);
            }
            case BEAK -> beak(sink, spine, pose);
            case ARM_OPEN -> armHand(sink, spine, pose, 0.0, false);
            case ARM_SHUT -> armHand(sink, spine, pose, 1.0, false);
            case ARM_FIST -> armHand(sink, spine, pose, 1.0, true);
        }
    }

    // ---- The Mutated Arm.
    /** Her own arm's thickness (blocks, a radius): how mutated its hand is next to it. */
    static final double ARM_ROOT = 0.08;
    /**
     * 6.2.0: the flesh bursts out all round her own arm: from just above her elbow ({@link #ARM_SHEATH_LENGTH} blocks of it, down her
     * forearm to the end of her fist) it sheathes the arm this tight (blocks, a radius: her arm is a quarter block thick, so between
     * its lumps her sleeve and skin show through), then swells on out of her fist.
     */
    public static final double ARM_SHEATH = 0.135;
    public static final double ARM_SHEATH_LENGTH = 0.41;

    /**
     * The Mutated Arm's radius {@code s} of the way along it ({@code length} long, from above her elbow), its thickest {@code girth}:
     * bursting out round her forearm and fist, then swelling over the next block and a half, knotted with great lumps of muscle all
     * along, easing in a little before the hand.
     */
    public static double armRadius(double s, double girth, double length) {
        double at = clamp01(s) * length;
        double swell = smooth((at - ARM_SHEATH_LENGTH) / 1.4);
        double end = 1.0 - 0.18 * smooth((at - (length - 0.8)) / 0.8);
        double knots = 1.0 + 0.16 * Math.sin(at * 2.3 + 0.4) * swell + 0.09 * Math.sin(at * 5.9 + 1.7) * swell;
        double sheath = sheathRadius(at);
        return (sheath + (Math.max(girth, ARM_SHEATH) - sheath) * swell) * end * knots;
    }

    /** How thick (blocks, a radius) the flesh is round her forearm {@code at} blocks down it: thin where it splits out, then tight round it. */
    public static double sheathRadius(double at) {
        return ARM_SHEATH * (0.45 + 0.55 * smooth(at / 0.2));
    }

    /**
     * The Mutated Arm's hand, as big as the arm has swollen: a knot of flesh and five great claws off it, thrown wide and hooked as
     * it reaches ({@code close} 0), clenched round what it holds ({@code close} 1).
     */
    static void armHand(UkakuModel.Sink sink, KakujaModel.Spine spine, Pose pose, double close, boolean fist) {
        double girth = Math.max(ARM_ROOT, pose.grip());
        // As mutated as it is swollen: her own hand's size and barely clawed near her own arm's thickness, a great clawed hand
        // swollen; a fist folds the claws in tight.
        double mutation = smooth((girth - 0.1) / 0.18);
        double knot = girth * (1.0 + 0.5 * mutation);
        double[][] at = knot(sink, spine, pose, knot);
        double[] t = at[1];
        double wrap = fist ? girth * 0.45 : girth * 1.7 + 0.2;
        double[] held = add(at[0], mul(t, knot + wrap * 0.8));
        for (int i = 0; i < 5; i++) {
            double turn = i * Math.PI * 2.0 / 5.0 + 0.3 + 0.2 * Math.sin(pose.seed() * 1.1 + i);
            double[] rim = normalize(add(mul(at[2], Math.cos(turn)), mul(at[3], Math.sin(turn))));
            double[] round = normalize(cross(t, rim));
            double length = girth * 3.4 * CLAW_LENGTH[i % CLAW_LENGTH.length] * (0.3 + 0.7 * mutation) * (fist ? 0.6 : 1.0);
            double wiggle = 0.06 * Math.sin(pose.time() * 0.37 + i * 1.7);
            double[] root = add(at[0], add(mul(rim, knot * 0.7), mul(t, knot * 0.3)));
            double[] open1 = add(root, add(mul(rim, length * 0.42), mul(t, length * 0.25)));
            double[] open2 = add(root, add(mul(rim, length * (0.6 + wiggle)), mul(t, length * 0.62)));
            double[] open3 = add(root, add(mul(rim, length * 0.38), add(mul(t, length * 0.98), mul(round, length * wiggle))));
            double[] shut1 = add(add(at[0], mul(t, knot * 0.6)), mul(rim, wrap * 0.9 + knot * 0.3));
            double[] shut2 = add(held, add(mul(rim, wrap), mul(round, wrap * 0.3)));
            double[] shut3 = add(held, add(mul(t, wrap * 0.8), add(mul(rim, wrap * 0.2), mul(round, wrap * 0.5))));
            double c = smooth(close);
            claw(sink, pose, new double[][] {root, lerp(open1, shut1, c), lerp(open2, shut2, c), lerp(open3, shut3, c)}, rim,
                girth * 0.5 * CLAW_THICK[i % CLAW_THICK.length], i);
        }
    }

    /** How many rings and how many ridges the sphere has. */
    static final int COCOON_RINGS = 22;
    static final int COCOON_SIDES = 26;
    static final int COCOON_FOLDS = 7;

    /**
     * 6.2.0, the Mutated Arm's sphere, round {@code centre}, {@code radius} across, the arm coming into it from {@code back} (unit,
     * from the centre to where the arm joins): living flesh grown out of the end of the arm over what it caught, its ragged rim
     * folding in from where the arm joins round to the far side as it shuts ({@code shut} 0..1), ridged with great folds, lumped and
     * crawling; squeezing ({@code squeeze} 0..1) clenches it smaller, the folds bulging.
     */
    public static void cocoon(UkakuModel.Sink sink, double[] centre, double[] back, double radius, double shut, double squeeze, double time, int seed) {
        if (shut <= 0.01) {
            return;
        }
        double[] axis = normalize(back);
        double[] a = Math.abs(axis[1]) > 0.9 ? new double[] {1.0, 0.0, 0.0} : new double[] {0.0, 1.0, 0.0};
        double[] u = normalize(cross(axis, a));
        double[] v = cross(axis, u);
        // From the pole where the arm joins, as far round as it has grown: a little, all the way.
        double reach = Math.PI * (0.18 + 0.82 * smooth(shut));
        double r0 = radius * (1.0 - 0.24 * smooth(squeeze));
        double[][][] grid = new double[COCOON_RINGS + 1][COCOON_SIDES + 1][];
        int[][] tone = new int[COCOON_RINGS + 1][COCOON_SIDES + 1];
        for (int i = 0; i <= COCOON_RINGS; i++) {
            double polar = reach * i / COCOON_RINGS;
            for (int k = 0; k <= COCOON_SIDES; k++) {
                double round = k * Math.PI * 2.0 / COCOON_SIDES;
                // The rim, ragged where it is still growing over.
                double edge = i == COCOON_RINGS && shut < 0.99 ? 0.12 * Math.sin(round * 5.0 + seed) : 0.0;
                double p = polar + edge;
                double fold = Math.pow(Math.max(0.0, Math.cos(round * COCOON_FOLDS * 0.5 + seed)), 6.0);
                double bump = 1.0 + 0.1 * Math.sin(round * 3.0 + p * 4.0 + time * 0.23 + seed) + 0.07 * Math.sin(round * 8.0 - p * 6.0 - time * 0.31)
                    + (0.12 + 0.12 * squeeze) * fold;
                double r = r0 * bump;
                double[] dir = add(mul(axis, Math.cos(p)), mul(add(mul(u, Math.cos(round)), mul(v, Math.sin(round))), Math.sin(p)));
                grid[i][k] = add(centre, mul(dir, r));
                tone[i][k] = fold > 0.5 ? CREASE : bump > 1.08 ? SHEEN : i % 3 == 0 ? FOLD : FLESH;
            }
        }
        for (int i = 0; i < COCOON_RINGS; i++) {
            for (int k = 0; k < COCOON_SIDES; k++) {
                double[] p0 = grid[i][k];
                double[] p1 = grid[i][k + 1];
                double[] p2 = grid[i + 1][k + 1];
                double[] p3 = grid[i + 1][k];
                double[] inside = centre;
                sink.quad(p0, p3, p2, p1, normal(p0, p3, p2, p1, inside), tone[i][k], tone[i + 1][k], tone[i + 1][k + 1], tone[i][k + 1]);
            }
        }
    }

    /** A living tube along {@code spine} from {@code s0} to {@code s1}, its radius from {@code profile}. */
    static void tube(UkakuModel.Sink sink, KakujaModel.Spine spine, java.util.function.DoubleUnaryOperator profile, double time, int seed,
                     double s0, double s1, boolean shade) {
        int rings = Math.max(4, (int)Math.round(spine.samples() * (s1 - s0)));
        double[][] previous = null;
        double[] previousLight = null;
        for (int i = 0; i <= rings; i++) {
            double s = s0 + (s1 - s0) * i / rings;
            double[][] frame = frame(spine, s);
            double r = profile.applyAsDouble(s);
            double[][] ring = new double[SIDES + 1][];
            double[] light = new double[SIDES + 1];
            for (int k = 0; k <= SIDES; k++) {
                double angle = k * Math.PI * 2.0 / SIDES;
                double bump = lumps(s, angle, time, seed);
                double[] dir = add(mul(frame[2], Math.cos(angle)), mul(frame[3], Math.sin(angle)));
                ring[k] = add(frame[0], mul(dir, r * (1.0 + bump)));
                light[k] = bump;
            }
            if (previous != null) {
                for (int k = 0; k < SIDES; k++) {
                    double[] a = previous[k];
                    double[] b = previous[k + 1];
                    double[] c = ring[k + 1];
                    double[] d = ring[k];
                    double[] n = normal(a, b, c, d, frame[0]);
                    sink.quad(a, b, c, d, n, tone(previousLight[k], shade), tone(previousLight[k + 1], shade), tone(light[k + 1], shade),
                        tone(light[k], shade));
                }
            }
            previous = ring;
            previousLight = light;
        }
    }

    /** Cords of muscle twisting round the trunk from her back almost to the tip, swelling out of it and sinking back in. */
    static void strands(UkakuModel.Sink sink, KakujaModel.Spine spine, Pose pose) {
        double g = clamp01(pose.grown());
        double length = spine.length();
        int points = 24;
        for (int k = 0; k < 3; k++) {
            double phase = k * Math.PI * 2.0 / 3.0 + pose.seed() * 0.37;
            double turns = 1.4 + 0.35 * k;
            double[][] cord = new double[points + 1][];
            for (int i = 0; i <= points; i++) {
                double u = i / (double)points;
                double s = 0.05 + 0.78 * u;
                double[][] frame = frame(spine, s);
                double angle = phase + turns * Math.PI * 2.0 * s + 0.25 * Math.sin(pose.time() * 0.11 + k * 1.7);
                double off = profile(pose, s, length) * 0.7 * Math.sqrt(Math.sin(Math.PI * u));
                cord[i] = add(frame[0], mul(add(mul(frame[2], Math.cos(angle)), mul(frame[3], Math.sin(angle))), off));
            }
            KakujaModel.Spine strand = CentipedeTail.through(cord, points * 2, 1, pose.up());
            int cordIndex = k;
            tube(sink, strand, u -> profile(pose, 0.05 + 0.78 * u, length) * 0.4 * (0.6 + 0.4 * Math.sin(Math.PI * u)), pose.time(),
                pose.seed() + 53 * (cordIndex + 1), 0.0, 1.0, true);
        }
    }

    /** Tendrils off the trunk, each its own way but always away from her body, curling and writhing. */
    static void tendrils(UkakuModel.Sink sink, KakujaModel.Spine spine, Pose pose) {
        double g = clamp01(pose.grown());
        double[] at = {0.16, 0.27, 0.36, 0.46, 0.56, 0.68};
        for (int i = 0; i < at.length; i++) {
            double s = at[i];
            double[][] frame = frame(spine, s);
            double phase = pose.seed() * 0.9 + i * 2.1;
            // Away from her middle line, square to the trunk, swung either way by up to 50 degrees.
            double[] away = sub(frame[0], nearest(pose.axis(), frame[0]));
            away = sub(away, mul(frame[1], dot(away, frame[1])));
            away = length(away) < 1.0E-6 ? frame[2] : normalize(away);
            double angle = 0.85 * Math.sin(phase + 0.4 * Math.sin(pose.time() * 0.17 + i));
            double[] across = normalize(cross(frame[1], away));
            double[] side = add(mul(away, Math.cos(angle)), mul(across, Math.sin(angle)));
            double base = profile(pose, s, spine.length());
            double length = (0.6 + 0.5 * ((i * 7 + pose.seed()) % 3) / 2.0) * g;
            double curl = 0.6 + 0.4 * Math.sin(pose.time() * 0.23 + phase);
            double[] start = add(frame[0], mul(side, base * 0.6));
            double[] a = add(start, add(mul(side, length * 0.4), mul(frame[1], length * 0.15)));
            double[] b = add(a, add(mul(side, length * 0.3), mul(frame[1], length * 0.25 * curl)));
            // The end curls back over, toward the tip.
            double[] c = add(b, add(mul(side, length * 0.05), add(mul(frame[1], length * 0.25), mul(across, length * 0.2 * curl))));
            double r0 = base * 0.34;
            java.util.function.DoubleUnaryOperator profile = u -> r0 * (1.0 - 0.75 * u) * (1.0 + 0.3 * Math.exp(-((u - 0.85) * (u - 0.85)) / 0.006));
            double[][] points = recoiled(pose, new double[][] {start, a, b, c}, frame[2], profile);
            KakujaModel.Spine branch = CentipedeTail.through(kept(pose, points, frame[2], profile), 14, 1, frame[2]);
            tube(sink, branch, profile, pose.time(), pose.seed() + 7 * i, 0.0, 1.0, true);
        }
    }

    /** The point of the polyline {@code line} nearest {@code p}. */
    static double[] nearest(double[][] line, double[] p) {
        double[] best = line[0];
        double bestGap = Double.MAX_VALUE;
        for (int i = 0; i + 1 < line.length; i++) {
            double[] ab = sub(line[i + 1], line[i]);
            double t = clamp01(dot(sub(p, line[i]), ab) / Math.max(1.0E-12, dot(ab, ab)));
            double[] q = add(line[i], mul(ab, t));
            double gap = dot(sub(p, q), sub(p, q));
            if (gap < bestGap) {
                bestGap = gap;
                best = q;
            }
        }
        return best;
    }

    /** The knot of flesh at the tip the grabbing tendrils sprout from (its radius, blocks, all out). */
    static final double KNOT_RADIUS = 0.32;
    /** The grabbing tendrils: how many, each one's turn round the tip, length and thickness (irregular: no two alike). */
    static final double[] CLAW_TURN = {0.35, 1.95, 3.45, 4.9};
    static final double[] CLAW_LENGTH = {1.0, 0.8, 1.12, 0.66};
    static final double[] CLAW_THICK = {1.0, 0.82, 1.1, 0.7};
    static final double CLAW_REACH = 1.5;
    static final double CLAW_RADIUS = 0.17;

    /** The knot's radius (blocks) at {@code grown}. */
    public static double knotRadius(double grown) {
        return KNOT_RADIUS * (0.3 + 0.7 * clamp01(grown));
    }

    /** How far past the trunk's tip the middle of what the tendrils hold is (blocks), for a body {@code grip} wide. */
    public static double holdOffset(double grip) {
        return KNOT_RADIUS * 1.2 + Math.max(0.3, grip) * 0.85;
    }

    /**
     * The tip of the flesh swelling into a lumpy knot: a mass at the end of the trunk, {@code knot} blocks round. Returns its
     * middle and the way the trunk was going there (rig space).
     */
    static double[][] knot(UkakuModel.Sink sink, KakujaModel.Spine spine, Pose pose, double knot) {
        double[][] tip = frame(spine, 1.0);
        double[] t = tip[1];
        double from = tipRadius(pose.grown());
        double[] start = add(tip[0], mul(t, -knot * 0.5));
        double[] side = mul(tip[2], knot * 0.12 * Math.sin(pose.time() * 0.13 + pose.seed()));
        double[] centre = pose.keep().clear(add(add(tip[0], mul(t, knot * 0.55)), side), knot * LUMPED + 0.04);
        double[] end = pose.keep().clear(add(centre, mul(t, knot * 0.95)), knot * 0.6 * LUMPED + 0.04);
        KakujaModel.Spine mass = CentipedeTail.through(new double[][] {start, centre, end}, 10, 1, tip[2]);
        tube(sink, mass, u -> from + (knot - from) * Math.pow(Math.sin(Math.PI * Math.min(1.0, u * 1.15)), 0.7) + 0.02, pose.time(), pose.seed() + 3, 0.0,
            1.0, true);
        return new double[][] {centre, t, tip[2], tip[3]};
    }

    /**
     * The swipe's rake: the tip knotted, three or four great uneven tendrils off it fanned up and down, curling back from the way
     * it sweeps.
     */
    static void rake(UkakuModel.Sink sink, KakujaModel.Spine spine, Pose pose) {
        double g = clamp01(pose.grown());
        double knot = knotRadius(g);
        double[][] at = knot(sink, spine, pose, knot);
        double[] t = at[1];
        double[] across = normalize(cross(t, pose.up()));
        if (length(across) < 1.0E-6) {
            across = at[2];
        }
        double[] fanUp = normalize(cross(across, t));
        double[] angles = {-0.85, -0.25, 0.3, 0.9};
        for (int i = 0; i < angles.length; i++) {
            double a = angles[i];
            double[] rim = normalize(add(mul(fanUp, Math.sin(a)), mul(across, 0.25 * Math.cos(a))));
            double length = CLAW_REACH * 1.25 * CLAW_LENGTH[i] * g;
            double sway = 0.25 * Math.sin(pose.time() * 0.29 + i * 1.3);
            double[] root = add(at[0], add(mul(rim, knot * 0.7), mul(t, knot * 0.35)));
            // Out and forward, then hooked back against the sweep (the way {@code across} points is the way it has come from).
            double[] a1 = add(root, add(mul(t, length * 0.35), mul(rim, length * 0.3)));
            double[] a2 = add(root, add(mul(t, length * 0.7), add(mul(rim, length * 0.38), mul(across, length * (0.12 + 0.06 * sway)))));
            double[] end = add(root, add(mul(t, length * 0.9), add(mul(rim, length * 0.25), mul(across, length * (0.42 + 0.1 * sway)))));
            claw(sink, pose, new double[][] {root, a1, a2, end}, rim, CLAW_RADIUS * CLAW_THICK[i] * (0.3 + 0.7 * g), i);
        }
    }

    // ---- The bone: ivory, pale along its ridges, yellowed toward its edges, flushed where it comes out of the flesh.
    public static final int BONE = 0xFFE9E1CD;
    public static final int BONE_HI = 0xFFF8F4EA;
    public static final int BONE_EDGE = 0xFFC2B48F;
    public static final int BONE_ROOT = 0xFFD9A99A;
    /** The blade: its length (blocks, out of the knot), its widest, its thickness along the spine, how far it curves back. */
    public static final double BLADE_LENGTH = 2.6;
    static final double BLADE_WIDTH = 0.44;
    static final double BLADE_THICK = 0.075;
    static final double BLADE_CURVE = 0.07;
    /** Along the blade (0 root .. 1 point): where it is widest, and where its back drops straight down into the point. */
    static final double BLADE_WIDEST = 0.32;
    static final double BLADE_CLIP = 0.66;
    /** How much of its width is the honed bevel along the edge (the rest the flat, then the spine). */
    static final double BEVEL = 0.34;
    /** The teeth down its back: how many, from where to where along it. */
    static final int TEETH = 6;
    static final double TEETH_FROM = 0.1;
    static final double TEETH_TO = 0.5;
    static final int BLADE_RINGS = 36;
    /** The bone of the blade: its honed edge near white, the flat duller, the spine dark. */
    static final int HONED = 0xFFFBF8F0;
    static final int FLAT = 0xFFDCD0B4;
    static final int SPINE = 0xFFA4937A;

    /**
     * 6.2.0, the swipe's blade: one long sabre of bone lying flat (level, whatever way the flesh comes at it) out of the end of the
     * flesh, carrying on the way the flesh runs and bowed a little back against the sweep (from her right round to her left), so
     * its leading edge, the one that cuts, is the long convex one: honed to nothing along a bright bevel. Its back is a thick dark
     * spine, toothed like a saw near the root, that drops straight down into a long needle point (a clip point) out on the cutting
     * side. The knot of flesh grips its root with short thick cords. It turns as the end of the flesh turns, lagging and swinging
     * with it.
     */
    static void blade(UkakuModel.Sink sink, KakujaModel.Spine spine, Pose pose) {
        double g = clamp01(pose.grown());
        double knot = knotRadius(g) * 0.72;
        double[][] at = knot(sink, spine, pose, knot);
        double[] t = at[1];
        double[] level = {0.0, 1.0, 0.0};
        double[] ahead = sub(t, mul(level, dot(t, level)));
        ahead = length(ahead) < 1.0E-4 ? at[2] : normalize(ahead);
        // The way it cuts: the left of the way it runs (it sweeps from her right round to her left).
        double[] lead = mul(normalize(cross(level, ahead)), -1.0);
        double length = BLADE_LENGTH * Math.pow(g, 1.3);
        double size = 0.4 + 0.6 * g;
        double[] root = add(at[0], mul(ahead, -knot * 0.3));
        int n = BLADE_RINGS;
        double[][] line = new double[n + 1][];
        for (int i = 0; i <= n; i++) {
            double u = i / (double)n;
            line[i] = add(root, add(mul(ahead, u * length), add(mul(lead, -BLADE_CURVE * length * u * u), mul(level, 0.02 * length * u * u))));
        }
        double[][][] rings = new double[n + 1][][];
        double[][] backAt = new double[n + 1][];
        double[][] acrossAt = new double[n + 1][];
        for (int i = 0; i <= n; i++) {
            double u = i / (double)n;
            double[] along = normalize(sub(line[Math.min(n, i + 1)], line[Math.max(0, i - 1)]));
            double[] across = normalize(cross(along, level));
            if (dot(across, lead) < 0.0) {
                across = mul(across, -1.0);
            }
            double w = BLADE_WIDTH * size;
            // The edge: out from the root to its widest, then sweeping in to the point.
            double edge = w * 0.62 * (0.3 + 0.7 * smooth(u / BLADE_WIDEST)) * Math.pow(1.0 - u, 0.42) + 0.002;
            // The back: straight along, then dropping in a straight line to the point (past the middle line, onto the cutting side).
            double clip = clamp01((u - BLADE_CLIP) / (1.0 - BLADE_CLIP));
            double back = w * 0.38 * (1.0 - 0.15 * u) * (1.0 - clip) - edge * 0.55 * clip;
            double h = BLADE_THICK * size * (0.8 + 0.2 * smooth(u / 0.12)) * Math.pow(1.0 - u, 0.5) + 0.0015;
            double[] c = line[i];
            double bevel = edge - (edge + back) * BEVEL;
            rings[i] = new double[][] {add(c, mul(across, edge)), add(add(c, mul(across, bevel)), mul(level, h * 0.62)),
                add(add(c, mul(across, -back * 0.55)), mul(level, h)), add(add(c, mul(across, -back)), mul(level, h * 0.8)),
                add(add(c, mul(across, -back)), mul(level, -h * 0.8)), add(add(c, mul(across, -back * 0.55)), mul(level, -h)),
                add(add(c, mul(across, bevel)), mul(level, -h * 0.62))};
            backAt[i] = add(c, mul(across, -back));
            acrossAt[i] = across;
        }
        int sides = rings[0].length;
        for (int i = 0; i < n; i++) {
            double u0 = i / (double)n;
            double u1 = (i + 1) / (double)n;
            double[] mid = lerp(line[i], line[i + 1], 0.5);
            for (int k = 0; k < sides; k++) {
                int k1 = (k + 1) % sides;
                double[] a = rings[i][k];
                double[] b = rings[i][k1];
                double[] c = rings[i + 1][k1];
                double[] d = rings[i + 1][k];
                // Each face one tone (the bevel's edge crisp against the flat): the colour of its first side round.
                int tone0 = bladeTone(k, k1, u0);
                int tone1 = bladeTone(k, k1, u1);
                sink.quad(a, b, c, d, normal(a, b, c, d, mid), tone0, tone0, tone1, tone1);
            }
        }
        // The teeth down its back, raked toward the root.
        for (int j = 0; j < TEETH; j++) {
            double u = TEETH_FROM + (TEETH_TO - TEETH_FROM) * j / (TEETH - 1.0);
            int i = (int)Math.round(u * n);
            double[] along = normalize(sub(line[Math.min(n, i + 1)], line[Math.max(0, i - 1)]));
            double reach = (0.15 - 0.05 * j / (TEETH - 1.0)) * size * Math.pow(g, 1.2);
            double[] base = backAt[i];
            double[] point = add(base, add(mul(acrossAt[i], -reach), mul(along, -reach * 0.55)));
            barb(sink, base, point, along, level, BLADE_THICK * size * 0.6, 0.06 * size);
        }
        // The flesh's cords, gripping its root above and below.
        double[] centre = at[0];
        for (int k = 0; k < 2; k++) {
            double[] face = k == 0 ? level : mul(level, -1.0);
            double[] onto = mul(lead, BLADE_WIDTH * size * 0.08);
            double[][] cord = {add(centre, mul(face, knot * 0.55)), add(centre, add(mul(ahead, knot * 0.85), add(mul(face, knot * 0.4), onto))),
                add(centre, add(mul(ahead, knot + 0.07 * length), add(mul(face, BLADE_THICK * size * 1.3), onto))),
                add(centre, add(mul(ahead, knot + 0.13 * length), add(mul(face, BLADE_THICK * size * 0.7), onto)))};
            claw(sink, pose, cord, face, 0.065 * size, k);
        }
    }

    // ---- Watch This: the beak.
    /** The beak: its length (blocks, out of the knot), how deep and wide it is at the root, and how far its end hooks down. */
    public static final double BEAK_LENGTH = 1.3;
    static final double BEAK_DEEP = 0.17;
    static final double BEAK_WIDE = 0.12;
    static final double BEAK_HOOK = 0.09;
    static final int BEAK_RINGS = 24;
    /** The beak's ridge along the top near white, its flanks bone, its keel underneath darker; its point dark with old blood. */
    static final int RIDGE = 0xFFF6F0E2;
    static final int KEEL = 0xFFB3A286;
    static final int STAINED = 0xFF7A2E26;

    /**
     * 6.2.0, a tentacle's beak: one long spike of bone carrying straight on out of the end of the flesh, deep and narrow like a
     * bird's (a sharp ridge along the top, a keel underneath), tapering to a needle that hooks a little down at its end, its point
     * stained dark. The knot of flesh grips its root with short thick cords.
     */
    static void beak(UkakuModel.Sink sink, KakujaModel.Spine spine, Pose pose) {
        double g = clamp01(pose.grown());
        double knot = knotRadius(g) * 0.5;
        double[][] at = knot(sink, spine, pose, knot);
        double[] ahead = normalize(at[1]);
        double[] upward = sub(new double[] {0.0, 1.0, 0.0}, mul(ahead, ahead[1]));
        upward = length(upward) < 1.0E-4 ? at[2] : normalize(upward);
        double[] side = normalize(cross(ahead, upward));
        double length = BEAK_LENGTH * Math.pow(g, 1.2);
        double size = 0.4 + 0.6 * g;
        double[] root = add(at[0], mul(ahead, -knot * 0.2));
        int n = BEAK_RINGS;
        double[][][] rings = new double[n + 1][][];
        double[][] line = new double[n + 1][];
        for (int i = 0; i <= n; i++) {
            double u = i / (double)n;
            line[i] = add(root, add(mul(ahead, u * length), mul(upward, -BEAK_HOOK * length * Math.pow(u, 2.5))));
        }
        for (int i = 0; i <= n; i++) {
            double u = i / (double)n;
            double[] along = normalize(sub(line[Math.min(n, i + 1)], line[Math.max(0, i - 1)]));
            double[] top = normalize(sub(upward, mul(along, dot(upward, along))));
            double[] across = normalize(cross(along, top));
            double taper = Math.pow(1.0 - u, 0.8) * (0.85 + 0.15 * smooth(u / 0.1)) + 0.002;
            double h = BEAK_DEEP * size * taper * 0.5;
            double w = BEAK_WIDE * size * taper * 0.5;
            double[] c = line[i];
            // A ridge on top, a keel beneath, the flanks between.
            rings[i] = new double[][] {add(c, mul(top, h)), add(c, add(mul(top, h * 0.35), mul(across, w))), add(c, add(mul(top, -h * 0.45), mul(across, w * 0.8))),
                add(c, mul(top, -h * 0.85)), add(c, add(mul(top, -h * 0.45), mul(across, -w * 0.8))), add(c, add(mul(top, h * 0.35), mul(across, -w)))};
        }
        int sides = rings[0].length;
        for (int i = 0; i < n; i++) {
            double u0 = i / (double)n;
            double u1 = (i + 1) / (double)n;
            double[] mid = lerp(line[i], line[i + 1], 0.5);
            for (int k = 0; k < sides; k++) {
                int k1 = (k + 1) % sides;
                double[] a = rings[i][k];
                double[] b = rings[i][k1];
                double[] c = rings[i + 1][k1];
                double[] d = rings[i + 1][k];
                int tone0 = beakTone(k, u0);
                int tone1 = beakTone(k, u1);
                sink.quad(a, b, c, d, normal(a, b, c, d, mid), tone0, tone0, tone1, tone1);
            }
        }
        // The flesh's cords, gripping its root above, below and at its sides.
        double[] centre = at[0];
        for (int k = 0; k < 3; k++) {
            double turn = k * Math.PI * 2.0 / 3.0 + 0.4;
            double[] face = add(mul(upward, Math.cos(turn)), mul(side, Math.sin(turn)));
            double[][] cord = {add(centre, mul(face, knot * 0.6)), add(centre, add(mul(ahead, knot * 0.8), mul(face, knot * 0.45))),
                add(centre, add(mul(ahead, knot + 0.09 * length), mul(face, BEAK_DEEP * size * 0.42))),
                add(centre, add(mul(ahead, knot + 0.16 * length), mul(face, BEAK_DEEP * size * 0.3)))};
            claw(sink, pose, cord, face, 0.05 * size, k);
        }
    }

    /** The colour of the beak's face {@code k} round its section, {@code u} out along it (0..1). */
    static int beakTone(int k, double u) {
        int c = k == 0 || k == 5 ? RIDGE : k == 1 || k == 4 ? BONE : KEEL;
        c = mixColour(c, BONE_ROOT, Math.max(0.0, 1.0 - u / 0.12) * 0.6);
        return mixColour(c, STAINED, smooth((u - 0.72) / 0.28) * 0.75);
    }

    /** A tooth of bone from {@code base} to {@code point}: a flat four-sided spike. */
    static void barb(UkakuModel.Sink sink, double[] base, double[] point, double[] along, double[] level, double thick, double wide) {
        double[] a = add(base, mul(along, wide));
        double[] b = add(base, mul(level, thick));
        double[] c = add(base, mul(along, -wide));
        double[] d = add(base, mul(level, -thick));
        double[][] corners = {a, b, c, d};
        for (int k = 0; k < 4; k++) {
            double[] p0 = corners[k];
            double[] p1 = corners[(k + 1) % 4];
            sink.quad(p0, p1, point, point, normal(p0, p1, point, point, base), SPINE, SPINE, FLAT, FLAT);
        }
    }

    /**
     * The colour of the blade's face between points {@code k} and {@code k1} round its section, {@code u} out along it (0..1): the
     * bevel down to the edge honed near white, the flats duller, the spine dark; flushed where it comes out of the flesh.
     */
    static int bladeTone(int k, int k1, double u) {
        int c = k == 0 || k == 6 ? HONED : k == 1 || k == 5 ? FLAT : SPINE;
        return mixColour(c, BONE_ROOT, Math.max(0.0, 1.0 - u / 0.1) * 0.6);
    }

    static int mixColour(int a, int b, double t) {
        double k = clamp01(t);
        int r = (int)Math.round(((a >> 16) & 0xFF) * (1.0 - k) + ((b >> 16) & 0xFF) * k);
        int gg = (int)Math.round(((a >> 8) & 0xFF) * (1.0 - k) + ((b >> 8) & 0xFF) * k);
        int bl = (int)Math.round((a & 0xFF) * (1.0 - k) + (b & 0xFF) * k);
        return 0xFF000000 | r << 16 | gg << 8 | bl;
    }

    /**
     * The grabbing end: the tip knotted, three or four great uneven tendrils off it, splayed wide as it reaches ({@code close} 0)
     * and wrapped round what it holds ({@code close} 1: a body {@link Pose#grip} wide, its middle {@link #holdOffset} past the tip).
     */
    static void hand(UkakuModel.Sink sink, KakujaModel.Spine spine, Pose pose, double close) {
        double g = clamp01(pose.grown());
        double knot = knotRadius(g);
        double[][] at = knot(sink, spine, pose, knot);
        double[] t = at[1];
        double grip = Math.max(0.3, pose.grip());
        double[] held = add(frame(spine, 1.0)[0], mul(t, holdOffset(grip)));
        double wrap = grip * 0.85 + 0.12;
        for (int i = 0; i < CLAW_TURN.length; i++) {
            double turn = CLAW_TURN[i] + 0.25 * Math.sin(pose.seed() * 1.3 + i);
            double[] rim = normalize(add(mul(at[2], Math.cos(turn)), mul(at[3], Math.sin(turn))));
            double[] round = normalize(cross(t, rim));
            double length = CLAW_REACH * CLAW_LENGTH[i] * g * Math.max(1.0, grip * 1.3);
            double wiggle = 0.08 * Math.sin(pose.time() * 0.41 + i * 1.9);
            double[] root = add(at[0], add(mul(rim, knot * 0.72), mul(t, knot * 0.3)));
            // Open: thrown wide and forward, hooked at the end, writhing.
            double[] open1 = add(root, add(mul(rim, length * 0.38), mul(t, length * 0.22)));
            double[] open2 = add(root, add(mul(rim, length * (0.62 + wiggle)), mul(t, length * 0.6)));
            double[] open3 = add(root, add(mul(rim, length * 0.5), add(mul(t, length * 0.95), mul(round, length * wiggle))));
            // Closed: along the near side of the body, round past its middle, and curled in over its far side, spiralling a little.
            double[] shut1 = add(add(at[0], mul(t, knot * 0.5 + grip * 0.25)), mul(rim, wrap + knot * 0.4));
            double[] shut2 = add(held, add(mul(rim, wrap), mul(round, wrap * 0.35)));
            double[] shut3 = add(held, add(mul(t, wrap * 0.85), add(mul(rim, wrap * 0.25), mul(round, wrap * 0.55))));
            double c = smooth(close);
            claw(sink, pose, new double[][] {root, lerp(open1, shut1, c), lerp(open2, shut2, c), lerp(open3, shut3, c)}, rim,
                CLAW_RADIUS * CLAW_THICK[i] * (0.3 + 0.7 * g), i);
        }
    }

    /** One great tendril through {@code points}: thick at its root, knobbed, tapering to a blunt hooked end; kept clear of her. */
    static void claw(UkakuModel.Sink sink, Pose pose, double[][] points, double[] dorsal, double r0, int index) {
        java.util.function.DoubleUnaryOperator profile = u -> r0 * (1.0 - 0.72 * u) * (1.0 + 0.18 * Math.sin(u * 8.0 + index * 1.7));
        KakujaModel.Spine claw = CentipedeTail.through(kept(pose, recoiled(pose, points, dorsal, profile), dorsal, profile), 18, 1, dorsal);
        tube(sink, claw, profile, pose.time(), pose.seed() + 31 * (index + 1), 0.0, 1.0, true);
        cap(sink, claw, pose, r0 * 0.3);
    }

    /** How many points along a tube are checked against her body. */
    static final int KEPT_SAMPLES = 14;

    /**
     * What grows off the flesh recoils from her body: if a tube through {@code points} would touch her, it draws in toward its
     * root (shorter each time) until it does not, or as short as it goes.
     */
    static double[][] recoiled(Pose pose, double[][] points, double[] dorsal, java.util.function.DoubleUnaryOperator profile) {
        if (pose.keep() == ANYWHERE) {
            return points;
        }
        double[][] p = points;
        for (int attempt = 1; attempt <= 5 && touches(pose, p, dorsal, profile); attempt++) {
            double k = Math.pow(0.8, attempt);
            p = new double[points.length][];
            for (int i = 0; i < points.length; i++) {
                p[i] = add(points[0], mul(sub(points[i], points[0]), k));
            }
        }
        return p;
    }

    /** Whether a tube through {@code points} touches her body (past its first point, where it grows from). */
    static boolean touches(Pose pose, double[][] points, double[] dorsal, java.util.function.DoubleUnaryOperator profile) {
        KakujaModel.Spine spine = CentipedeTail.through(points, KEPT_SAMPLES, 1, dorsal);
        for (int i = 1; i <= KEPT_SAMPLES; i++) {
            double u = i / (double)KEPT_SAMPLES;
            if (pose.keep().depth(spine.p()[i], profile.applyAsDouble(u) * LUMPED + 0.05) > 0.0) {
                return true;
            }
        }
        return false;
    }

    /**
     * A tube's control points kept clear of her ({@link Pose#keep}), its radius along it from {@code profile}: each point moved clear,
     * then the curve through them followed and each point of it moved clear, its first point (where it grows from) left where it is.
     */
    static double[][] kept(Pose pose, double[][] points, double[] dorsal, java.util.function.DoubleUnaryOperator profile) {
        if (pose.keep() == ANYWHERE) {
            return points;
        }
        double[][] p = points.clone();
        for (int i = 1; i < p.length; i++) {
            p[i] = pose.keep().clear(p[i], profile.applyAsDouble(i / (double)(p.length - 1)) * LUMPED + 0.05);
        }
        KakujaModel.Spine spine = CentipedeTail.through(p, KEPT_SAMPLES, 1, dorsal);
        double[][] out = new double[KEPT_SAMPLES + 1][];
        out[0] = p[0];
        for (int i = 1; i <= KEPT_SAMPLES; i++) {
            out[i] = pose.keep().clear(spine.p()[i], profile.applyAsDouble(i / (double)KEPT_SAMPLES) * LUMPED + 0.05);
        }
        return out;
    }

    /** A blunt end on {@code spine}'s tip. */
    static void cap(UkakuModel.Sink sink, KakujaModel.Spine spine, Pose pose, double r) {
        double[][] tip = frame(spine, 1.0);
        double[] point = add(tip[0], mul(tip[1], r * 1.2));
        for (int k = 0; k < SIDES; k++) {
            double a0 = k * Math.PI * 2.0 / SIDES;
            double a1 = (k + 1) * Math.PI * 2.0 / SIDES;
            double[] p0 = add(tip[0], mul(add(mul(tip[2], Math.cos(a0)), mul(tip[3], Math.sin(a0))), r));
            double[] p1 = add(tip[0], mul(add(mul(tip[2], Math.cos(a1)), mul(tip[3], Math.sin(a1))), r));
            double[] n = normal(p0, p1, point, point, tip[0]);
            sink.quad(p0, p1, point, point, n, FLESH, FLESH, SHEEN, SHEEN);
        }
    }

    /** 6.2.0, True Anonymity's spikes: how many, how long (blocks, at the full size), how thick at the root, and their bone. */
    static final int SPIKES = 20;
    static final double SPIKE_LENGTH = 0.8;
    static final double SPIKE_ROOT = 0.095;
    static final int SPIKE_BONE = 0xFFEDE3CC;
    static final int BONE_DARK = 0xFFB8A888;
    static final int SPIKE_TIP = 0xFF4A3A30;
    static final int RAW = 0xFFB0303A;

    /**
     * 6.2.0, True Anonymity: bone spikes all over the club, each breaking out of the flesh where it is (a raw red collar at the root),
     * pointing out from it and swept a little toward its head, longer on the head than down by her shin, uneven, growing out with the
     * club. Laid out from the seed alone, so every client draws the same ones.
     */
    static void boneSpikes(UkakuModel.Sink sink, KakujaModel.Spine spine, Pose pose, double length) {
        double grown = clamp01(pose.grown());
        if (grown <= 0.05) {
            return;
        }
        java.util.Random random = new java.util.Random(pose.seed() * 7919L + 31L);
        double scale = Math.max(0.6, Math.min(1.6, pose.grip() / 0.5));
        for (int i = 0; i < SPIKES; i++) {
            // Down the club (thicker toward its head), turned evenly round it with a jitter.
            double s = 0.22 + 0.76 * Math.sqrt((i + random.nextDouble()) / SPIKES);
            double angle = i * 2.39996 + random.nextDouble() * 0.6;
            double[][] f = frame(spine, s);
            double[] out = normalize(add(mul(f[2], Math.cos(angle)), mul(f[3], Math.sin(angle))));
            double r = profile(pose, s, length);
            double[] base = add(f[0], mul(out, r * 0.86));
            double tilt = 0.25 + 0.35 * random.nextDouble();
            double[] dir = normalize(add(mul(out, Math.cos(tilt)), mul(f[1], Math.sin(tilt))));
            double spikeLength = SPIKE_LENGTH * scale * (0.55 + 0.6 * random.nextDouble()) * (0.5 + 0.5 * s) * grown;
            spike(sink, base, dir, SPIKE_ROOT * scale * (0.8 + 0.4 * random.nextDouble()) * (0.4 + 0.6 * grown), spikeLength);
        }
    }

    /** One bone spike: a five-sided cone out of {@code base} along {@code dir}, raw at the root, bone, dark at the point. */
    static void spike(UkakuModel.Sink sink, double[] base, double[] dir, double root, double length) {
        double[] side = Math.abs(dir[1]) < 0.9 ? normalize(cross(dir, new double[] {0.0, 1.0, 0.0})) : normalize(cross(dir, new double[] {1.0, 0.0, 0.0}));
        double[] other = cross(dir, side);
        double[] mid = add(base, mul(dir, length * 0.35));
        double[] tip = add(base, mul(dir, length));
        int sides = 5;
        for (int k = 0; k < sides; k++) {
            double a0 = k * Math.PI * 2.0 / sides;
            double a1 = (k + 1) * Math.PI * 2.0 / sides;
            double[] r0 = add(mul(side, Math.cos(a0)), mul(other, Math.sin(a0)));
            double[] r1 = add(mul(side, Math.cos(a1)), mul(other, Math.sin(a1)));
            double[] b0 = add(base, mul(r0, root));
            double[] b1 = add(base, mul(r1, root));
            double[] m0 = add(mid, mul(r0, root * 0.62));
            double[] m1 = add(mid, mul(r1, root * 0.62));
            sink.quad(b0, b1, m1, m0, normal(b0, b1, m1, m0, base), RAW, RAW, BONE_DARK, BONE_DARK);
            sink.quad(m0, m1, tip, tip, normal(m0, m1, tip, tip, mid), SPIKE_BONE, SPIKE_BONE, SPIKE_TIP, SPIKE_TIP);
        }
    }

    /** Flesh tone: bulges pale and catching the light, creases deep. */
    static int tone(double bump, boolean shade) {
        if (!shade) {
            return FLESH;
        }
        double u = Math.max(-1.0, Math.min(1.0, bump / 0.3));
        return u >= 0.0 ? mix(FLESH, SHEEN, u * 0.7) : mix(FLESH, u < -0.5 ? CREASE : FOLD, Math.min(1.0, -u * 1.3));
    }

    static int mix(int a, int b, double t) {
        int out = 0xFF000000;
        for (int shift = 0; shift <= 16; shift += 8) {
            int ca = (a >> shift) & 255;
            int cb = (b >> shift) & 255;
            out |= ((int)Math.round(ca + (cb - ca) * t) & 255) << shift;
        }
        return out;
    }

    /** {position, tangent, normal, binormal} along a spine (its own frame, kept square). */
    static double[][] frame(KakujaModel.Spine spine, double s) {
        int last = spine.samples();
        double x = Math.max(0.0, Math.min(1.0, s)) * last;
        int i = Math.min(last - 1, (int)Math.floor(x));
        double f = x - i;
        double[] p = lerp(spine.p()[i], spine.p()[i + 1], f);
        double[] t = normalize(lerp(spine.t()[i], spine.t()[i + 1], f));
        double[] n = normalize(lerp(spine.n()[i], spine.n()[i + 1], f));
        n = normalize(sub(n, mul(t, dot(n, t))));
        double[] b = normalize(cross(t, n));
        return new double[][] {p, t, n, b};
    }

    /** A quad's outward normal (away from {@code inside}). */
    static double[] normal(double[] a, double[] b, double[] c, double[] d, double[] inside) {
        double[] n = normalize(cross(sub(c, a), sub(d, b)));
        double[] mid = mul(add(add(a, b), add(c, d)), 0.25);
        return dot(n, sub(mid, inside)) < 0.0 ? mul(n, -1.0) : n;
    }

    // ------------------------------------------------------------------ vectors

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

    static double dot(double[] a, double[] b) {
        return a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
    }

    static double[] cross(double[] a, double[] b) {
        return new double[] {a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]};
    }

    static double length(double[] a) {
        return Math.sqrt(dot(a, a));
    }

    static double[] normalize(double[] a) {
        double l = length(a);
        return l < 1.0E-9 ? new double[] {0.0, 0.0, 0.0} : mul(a, 1.0 / l);
    }

    static double clamp01(double v) {
        return Math.max(0.0, Math.min(1.0, v));
    }

    static double smooth(double u) {
        double c = clamp01(u);
        return c * c * (3.0 - 2.0 * c);
    }
}
