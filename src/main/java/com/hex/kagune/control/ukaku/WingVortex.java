package com.hex.kagune.control.ukaku;

import static com.hex.kagune.control.ukaku.UkakuModel.argb;
import static com.hex.kagune.control.ukaku.UkakuModel.hash;
import static com.hex.kagune.control.ukaku.UkakuModel.mixInt;
import static com.hex.kagune.control.ukaku.UkakuModel.smooth;

/**
 * The Wing Vortex (pure, tested): one of Ukaku's wings shooting forward as a spinning vortex, still attached to his
 * shoulder blade. The wing's flame tongues are wound into broad ribbons spiralling from the shoulder blade (a narrow jet,
 * where the wing grows from) and opening out like a tornado lying on its side into the big spinning mass at its head, with
 * a white-hot core at the front; inside them a cone of dark red flame fills it, so it reads as one twisting, burning mass. Everything spins,
 * shaded like the wing's own fire ({@link UkakuFlame}): dense red facing the camera, the lavender-pink glow at its
 * silhouette, dark streaks winding along it, molten toward the point, the starry navy of the √A and :re wings further back. A
 * soft pink aura hangs along it all.
 *
 * <p>When the vortex ends it detaches: {@code unwind} 0 to 1 loosens the spiral, slows the spin and lets it spread as it fades.
 *
 * <p>Coordinates are whatever space {@code root}, {@code head} and {@code eye} share (the renderer uses camera-relative world
 * blocks). {@code size} 1 is a full :re-sized wing.
 */
public final class WingVortex {
    /** Widest radius of a size-1 vortex (at its head), and how far back from the front its head rounds off (blocks). */
    public static final double RADIUS = 1.5; // 2.4.6: a massive blow (was 1.1)
    public static final double POINT = 1.0;
    /** Flame tongues wound into it. */
    static final int TONGUES = 7;
    /** Turns a second, and turns of the spiral per block of its length. */
    static final double SPIN = 0.9; // 2.4.6: slow, heavy turning (was 3.2)
    static final double TWIST = 0.32;
    /** Rings along the vortex: at least, and at most (long vortices are drawn with longer steps). */
    static final int FEWEST = 10;
    static final int MOST = 40;
    static final int SIDES = 5;
    static final int BODY_SIDES = 7;
    /** Quads one vortex costs at most (the near version), for budgets. */
    public static final int QUADS = TONGUES * MOST * SIDES + MOST * BODY_SIDES + 4 + 10;

    private WingVortex() {
    }

    /**
     * The vortex from {@code root} (the shoulder blade it grows from) to {@code head} (its point), {@code size} as above,
     * {@code time} seconds (spin and flicker), {@code seed} to vary it, seen from {@code eye}. {@code fade} (0..1) dims it,
     * {@code unwind} (0..1) loosens it once it has detached, {@code dusk} (0..1) is the wing's √A/:re night colour,
     * {@code fine} false for the cheap far version.
     */
    public static void build(RcShard.Sink sink, double[] root, double[] head, double size, double time, int seed, double[] eye, double fade,
                             double unwind, double dusk, boolean fine) {
        double alpha = clamp01(fade);
        double[] axis = sub(head, root);
        double length = Math.sqrt(dot(axis, axis));
        if (alpha <= 0.003 || size <= 0.0 || length < 0.05) {
            return;
        }
        double[] dir = scale(axis, 1.0 / length);
        double[] helper = Math.abs(dir[1]) < 0.95 ? new double[] {0.0, 1.0, 0.0} : new double[] {1.0, 0.0, 0.0};
        double[] u = normalize(cross(dir, helper));
        double[] v = cross(dir, u);
        double loose = clamp01(unwind);
        double radius = RADIUS * size * (1.0 + 1.3 * loose);
        double spin = time * Math.PI * 2.0 * SPIN * (1.0 - 0.6 * loose);
        double twist = Math.PI * 2.0 * TWIST * (1.0 - 0.5 * loose) / Math.max(0.6, size);
        int stations = Math.max(FEWEST, Math.min(fine ? MOST : 16, (int)Math.round(length * (fine ? 2.4 : 1.0))));
        Shape shape = new Shape(root, dir, u, v, length, radius, size, spin, twist, Coil.of(root, dir, length, size));
        for (int tongue = 0; tongue < TONGUES; tongue++) {
            double phase = Math.PI * 2.0 * tongue / TONGUES + 0.5 * hash(seed * 13 + tongue);
            double width = size * (0.4 + 0.12 * hash(seed * 5 + tongue + 9)) * (1.0 + 0.4 * loose); // 2.4.6: broad, overlapping: one mass
            double offset = 0.85 + 0.15 * hash(seed * 7 + tongue + 3); // how far out on the vortex this tongue lies
            tube(sink, shape, phase, offset, width, stations, fine ? SIDES : 4, tongue + seed * 31, time, eye, alpha * (1.0 - 0.6 * loose * loose),
                dusk);
        }
        body(sink, shape, stations, fine ? BODY_SIDES : 5, eye, alpha * (1.0 - 0.8 * loose), dusk);
        double[] front = shape.coil().centre(1.0);
        double[] frontWay = shape.coil().tangent(1.0);
        core(sink, front, frontWay, size, eye, alpha * (1.0 - loose) * 0.35);
        aura(sink, root, front, normalize(sub(front, root)), radius, eye, alpha * (1.0 - 0.7 * loose) * 0.6);
    }

    /**
     * 2.4.6 (the owner: "the vortex move should be a slow massive twisting blow not a fast horizontal tornado, please check the reference
     * gif and match it"): the line it follows. Out of the shoulder blade it rears up and comes down ahead, then rolls on along the ground
     * in great upright loops (a prolate cycloid in the upright plane through its flight: {@link #centre}), as the reference's black-and-pink
     * mass coils forward; the loops grow in as it lengthens, so a short one is a rising arch.
     */
    record Coil(double[] root, double[] forward, double[] side, double length, int loops, double coil, double drop) {
        static final double[] UP = {0.0, 1.0, 0.0};
        /** How far (blocks, a size-1 vortex) each loop carries it on, how tall a loop is (radius), how far down from the shoulder it rolls. */
        static final double LOOP = 4.5;
        static final double HEIGHT = 1.5;
        static final double DROP = 1.05;

        static Coil of(double[] root, double[] dir, double length, double size) {
            double[] flat = {dir[0], 0.0, dir[2]};
            double[] forward = dot(flat, flat) < 1.0E-6 ? dir : normalize(flat);
            double[] side = normalize(cross(forward, UP));
            if (dot(side, side) < 0.5) {
                side = new double[] {1.0, 0.0, 0.0};
            }
            int loops = Math.max(1, (int)Math.round(length / (LOOP * Math.max(0.3, size))));
            double grown = smooth(length / (4.0 * Math.max(0.3, size)));
            return new Coil(root, forward, side, length, loops, HEIGHT * size * grown, DROP * size * grown);
        }

        /** Its line at {@code s} (0 the shoulder blade .. 1 its front). */
        double[] centre(double s) {
            double theta = 2.0 * Math.PI * this.loops * s;
            // Forward along the bottom of each loop, curling back over its top (a breaking wave's roll): never back behind his shoulder.
            double x = this.length * s + this.coil * Math.sin(theta);
            double y = this.coil * (1.0 - Math.cos(theta)) - this.drop * smooth(s / 0.15);
            return new double[] {this.root[0] + this.forward[0] * x, this.root[1] + y, this.root[2] + this.forward[2] * x};
        }

        /** Its way at {@code s}. */
        double[] tangent(double s) {
            double h = 0.004;
            return normalize(sub(this.centre(Math.min(1.0, s + h)), this.centre(Math.max(0.0, s - h))));
        }

        /** Out from it in its upright plane, at {@code s}. */
        double[] normal(double s) {
            return normalize(cross(this.tangent(s), this.side));
        }
    }

    /** The vortex's line and how it opens: shared by every tongue and the body. */
    record Shape(double[] root, double[] dir, double[] u, double[] v, double length, double radius, double size, double spin, double twist, Coil coil) {
        /**
         * Radius at {@code s} (0 the shoulder blade .. 1 the head): a narrow jet out of the back, opening out like a tornado
         * along its length into the big spinning mass at its head, rounded at the very front.
         */
        double radiusAt(double s) {
            double fromRoot = s * this.length;
            double fromHead = (1.0 - s) * this.length;
            double cone = 0.2 + 0.8 * smooth(fromRoot / Math.max(2.5 * this.size, 0.75 * this.length));
            double front = 0.3 + 0.7 * Math.sqrt(smooth(fromHead / (POINT * this.size)));
            return this.radius * cone * front;
        }

        /** The spiral's angle at {@code s}, counted from the point (so the drill keeps its look as the vortex lengthens). */
        double angleAt(double s) {
            return this.spin + this.twist * (1.0 - s) * this.length;
        }

        double[] at(double s, double angle, double r) {
            // 2.4.6: round its rolling line (Coil), not a straight one.
            double[] centre = this.coil.centre(s);
            double[] side = this.coil.side();
            double[] out = this.coil.normal(s);
            double c = Math.cos(angle) * r;
            double n = Math.sin(angle) * r;
            return new double[] {centre[0] + side[0] * c + out[0] * n, centre[1] + side[1] * c + out[1] * n, centre[2] + side[2] * c + out[2] * n};
        }

        /** 2.4.6: the outward way at {@code s}, {@code angle} round it. */
        double[] normal(double s, double angle) {
            double[] side = this.coil.side();
            double[] out = this.coil.normal(s);
            return normalize(new double[] {side[0] * Math.cos(angle) + out[0] * Math.sin(angle), side[1] * Math.cos(angle) + out[1] * Math.sin(angle),
                side[2] * Math.cos(angle) + out[2] * Math.sin(angle)});
        }
    }

    /** Where a tongue's spine is at {@code s}. */
    static double[] spine(Shape shape, double phase, double offset, double s) {
        return shape.at(s, phase + shape.angleAt(s), shape.radiusAt(s) * offset);
    }

    /** How hot the fire is at {@code s}: white-hot at the point, through red, crimson further back (0 hot .. 1 deep). */
    static double depth(Shape shape, double s) {
        return Math.min(1.0, (1.0 - s) * shape.length() / (6.0 * shape.size()));
    }

    /** One tongue: a broad ribbon of fire along its spiral, lying on the vortex's surface, shaded from the camera. */
    private static void tube(RcShard.Sink sink, Shape shape, double phase, double offset, double width, int stations, int sides, int seed, double time,
                             double[] eye, double alpha, double dusk) {
        double[][][] ring = new double[stations + 1][sides][];
        int[][] colour = new int[stations + 1][sides];
        double step = 0.5 / Math.max(1.0, shape.length());
        for (int k = 0; k <= stations; k++) {
            double s = k / (double)stations;
            double[] centre = spine(shape, phase, offset, s);
            double[] tangent = normalize(sub(spine(shape, phase, offset, Math.min(1.0, s + step)), spine(shape, phase, offset, Math.max(0.0, s - step))));
            // n1 points out from the flight line, n2 along the surface: a broad ribbon, so neighbouring tongues overlap.
            double[] n1 = normalize(cross(tangent, shape.coil().tangent(s)));
            if (dot(n1, n1) < 0.5 || Math.abs(dot(tangent, shape.coil().tangent(s))) > 0.999) {
                n1 = shape.coil().side();
            }
            double[] n2 = cross(tangent, n1);
            double grow = shape.radiusAt(s) / Math.max(1.0E-6, shape.radius());
            double wide = width * (0.35 + 0.65 * grow);
            double half = wide * 0.38;
            double deep = wide;
            double g = depth(shape, s);
            for (int j = 0; j < sides; j++) {
                double theta = 2.0 * Math.PI * j / sides;
                double c = Math.cos(theta);
                double n = Math.sin(theta);
                double[] point = {centre[0] + n1[0] * c * half + n2[0] * n * deep, centre[1] + n1[1] * c * half + n2[1] * n * deep,
                    centre[2] + n1[2] * c * half + n2[2] * n * deep};
                double[] normal = normalize(new double[] {n1[0] * c * deep + n2[0] * n * half, n1[1] * c * deep + n2[1] * n * half,
                    n1[2] * c * deep + n2[2] * n * half});
                double facing = Math.min(1.0, Math.abs(dot(normal, normalize(sub(eye, point)))));
                double streak = Math.pow(Math.max(0.0, Math.sin(3.0 * theta + 1.6 * (1.0 - s) * shape.length() + seed + 2.0 * time)), 4.0);
                colour[k][j] = fire(g, facing, streak, dusk, alpha);
                ring[k][j] = point;
            }
        }
        for (int k = 0; k < stations; k++) {
            for (int j = 0; j < sides; j++) {
                int n = (j + 1) % sides;
                sink.quad(ring[k][j], ring[k][n], ring[k + 1][n], ring[k + 1][j], colour[k][j], colour[k][n], colour[k + 1][n], colour[k + 1][j]);
            }
        }
    }

    /** The wing's fire at depth {@code g} (0 the point .. 1 deep in): molten, red, crimson; the :re night further back. */
    static int fire(double g, double facing, double streak, double dusk, double alpha) {
        int c = UkakuFlame.fireRamp(0.06 + 0.66 * g);
        if (dusk > 0.0) {
            int night = mixInt(UkakuFlame.NAVY, UkakuFlame.ABYSS, smooth((g - 0.7) / 0.3));
            c = mixInt(c, night, dusk * smooth((g - 0.35) / 0.35));
            c = mixInt(c, UkakuFlame.RED, dusk * streak * 0.7);
        }
        c = mixInt(c, UkakuFlame.EMBER, (1.0 - dusk) * streak * 0.55 * smooth((g - 0.12) / 0.2));
        c = mixInt(c, UkakuFlame.CORE, 0.6 * (1.0 - smooth(g / 0.18)));
        // 2.4.6: the reference's mass: near-black maroon through and through, its edges burning pink.
        c = mixInt(c, 0x14040C, 0.78);
        double edge = 1.0 - smooth(facing / 0.5);
        c = mixInt(c, mixInt(0xFF5FC4, UkakuFlame.RIM, 0.25 + 0.5 * dusk), edge * 0.95);
        double a = 0.94 * (0.55 + 0.45 * smooth(facing / 0.6)); // 2.4.6: solid, not see-through
        return argb(c, a * alpha);
    }

    /** The fire filling the vortex: a spinning cone of dark red flame inside the tongues, so it reads as one burning mass. */
    private static void body(RcShard.Sink sink, Shape shape, int stations, int sides, double[] eye, double alpha, double dusk) {
        if (alpha <= 0.003) {
            return;
        }
        double[][][] ring = new double[stations + 1][sides][];
        int[][] colour = new int[stations + 1][sides];
        for (int k = 0; k <= stations; k++) {
            double s = k / (double)stations;
            double r = shape.radiusAt(s) * 0.85; // 2.4.6: a solid dark mass inside the coils
            double g = depth(shape, s);
            for (int j = 0; j < sides; j++) {
                double theta = 2.0 * Math.PI * j / sides + shape.angleAt(s) * 0.7;
                double[] point = shape.at(s, theta, r);
                double[] normal = shape.normal(s, theta);
                double facing = Math.min(1.0, Math.abs(dot(normal, normalize(sub(eye, point)))));
                double streak = Math.pow(Math.max(0.0, Math.sin(2.0 * theta + 3.0 * (1.0 - s) * shape.length())), 3.0);
                int c = fire(0.15 + 0.85 * g, facing, streak, dusk, alpha * 0.98);
                colour[k][j] = argb(mixInt(c & 0xFFFFFF, 0x0E0208, 0.4), (c >>> 24) / 255.0);
                ring[k][j] = point;
            }
        }
        for (int k = 0; k < stations; k++) {
            for (int j = 0; j < sides; j++) {
                int n = (j + 1) % sides;
                sink.quad(ring[k][j], ring[k][n], ring[k + 1][n], ring[k + 1][j], colour[k][j], colour[k][n], colour[k + 1][n], colour[k + 1][j]);
            }
        }
    }

    /** The white-hot point: a spindle along the flight line in two crossed planes, facing the camera. */
    private static void core(RcShard.Sink sink, double[] head, double[] dir, double size, double[] eye, double alpha) {
        if (alpha <= 0.003) {
            return;
        }
        double[] view = normalize(sub(eye, head));
        double[] across = cross(dir, view);
        if (dot(across, across) < 1.0E-8) {
            across = cross(dir, new double[] {0.0, 1.0, 0.0});
        }
        across = normalize(across);
        double[] lift = normalize(cross(across, dir));
        double back = POINT * size * 0.8;
        double width = 0.16 * size;
        int hot = argb(UkakuFlame.CORE, alpha * 0.95);
        int warm = argb(UkakuFlame.MOLTEN, alpha * 0.6);
        int clear = argb(UkakuFlame.FIRE, 0.0);
        double[] front = add(head, scale(dir, 0.2 * size));
        double[] middle = add(head, scale(dir, -back * 0.3));
        double[] tail = add(head, scale(dir, -back));
        for (double[] side : new double[][] {across, lift}) {
            double[] l = add(middle, scale(side, width));
            double[] r = add(middle, scale(side, -width));
            sink.quad(front, l, middle, r, hot, clear, hot, clear);
            sink.quad(middle, l, tail, r, warm, clear, clear, clear);
        }
    }

    /** The pink aura: a soft camera-facing band along the whole vortex and a round glow at its point. */
    private static void aura(RcShard.Sink sink, double[] root, double[] head, double[] dir, double radius, double[] eye, double alpha) {
        if (alpha <= 0.003) {
            return;
        }
        double[] middle = scale(add(root, head), 0.5);
        double[] view = normalize(sub(eye, middle));
        double[] across = cross(dir, view);
        if (dot(across, across) < 1.0E-8) {
            across = cross(dir, new double[] {0.0, 1.0, 0.0});
        }
        across = normalize(across);
        double[] lift = normalize(cross(view, across));
        int glow = argb(UkakuFlame.RIM, alpha * 0.24);
        int warm = argb(UkakuFlame.RIM_HOT, alpha * 0.3);
        int none = argb(UkakuFlame.RIM, 0.0);
        double wide = radius * 1.6;
        double[] l0 = add(root, scale(across, wide * 0.12));
        double[] r0 = add(root, scale(across, -wide * 0.12));
        double[] l1 = add(head, scale(across, wide));
        double[] r1 = add(head, scale(across, -wide));
        // Along it: a band from the shoulder blade to the point, its edges clear.
        sink.quad(root, l0, l1, head, glow, none, none, glow);
        sink.quad(root, r0, r1, head, glow, none, none, glow);
        // Round at the point: eight soft wedges.
        double ring = radius * 0.95;
        for (int k = 0; k < 8; k++) {
            double a0 = Math.PI * 2.0 * k / 8.0;
            double a1 = Math.PI * 2.0 * (k + 1) / 8.0;
            double[] p0 = add(head, add(scale(across, Math.cos(a0) * ring), scale(lift, Math.sin(a0) * ring)));
            double[] p1 = add(head, add(scale(across, Math.cos(a1) * ring), scale(lift, Math.sin(a1) * ring)));
            sink.quad(head, p0, p1, p1, warm, none, none, none);
        }
    }

    // ------------------------------------------------------------------ vectors

    static double clamp01(double value) {
        return UkakuModel.clamp01(value);
    }

    private static double[] add(double[] a, double[] b) {
        return new double[] {a[0] + b[0], a[1] + b[1], a[2] + b[2]};
    }

    private static double[] sub(double[] a, double[] b) {
        return new double[] {a[0] - b[0], a[1] - b[1], a[2] - b[2]};
    }

    private static double[] scale(double[] a, double k) {
        return new double[] {a[0] * k, a[1] * k, a[2] * k};
    }

    private static double dot(double[] a, double[] b) {
        return a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
    }

    private static double[] cross(double[] a, double[] b) {
        return new double[] {a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]};
    }

    private static double[] normalize(double[] a) {
        double length = Math.sqrt(dot(a, a));
        return length < 1.0E-12 ? new double[] {0.0, 0.0, 1.0} : scale(a, 1.0 / length);
    }
}
