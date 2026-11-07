package com.hex.kagune.control.ukaku;

import static com.hex.kagune.control.ukaku.UkakuModel.argb;
import static com.hex.kagune.control.ukaku.UkakuModel.hash;
import static com.hex.kagune.control.ukaku.UkakuModel.mixInt;
import static com.hex.kagune.control.ukaku.UkakuModel.smooth;

/**
 * The Wing Vortex (pure, tested): one of Ukaku's wings shooting forward as a spinning vortex, still attached to his
 * shoulder blade. 3.2.9 (the owner: "meant to be less janky, thinner and more circular, can't see anything in first person"): a
 * clean corkscrew round its flight line, straight from the shoulder blade to its point: {@link #STRANDS} round tubes of the wing's
 * fire wound round it in circles (a loop every four and a half blocks), narrow where it leaves his back, opening to {@link #RADIUS} and
 * rounding off into its point, a thin dark jet of flame down its middle, a white-hot core at the front, a soft pink aura. It
 * spins as it goes. 2.4.6's massive rolling loops of broad flat ribbons (and the solid cone filling them) are gone: from close they
 * were a heap of crumpled panels, and they filled the view.
 *
 * <p>When the vortex ends it detaches: {@code unwind} 0 to 1 loosens the spiral, slows the spin and lets it spread as it fades.
 * Seen by its own thrower in first person ({@code own}), it is a thin jet near his eyes, opening only as it goes away from him.
 *
 * <p>Coordinates are whatever space {@code root}, {@code head} and {@code eye} share (the renderer uses camera-relative world
 * blocks). {@code size} 1 is a full :re-sized wing.
 */
public final class WingVortex {
    /** Widest radius of a size-1 vortex's spiral (blocks; 2.4.6's was 1.5, rolling in loops 1.5 tall round its line), and how far back from the front it rounds off. */
    public static final double RADIUS = 0.8;
    public static final double POINT = 1.0;
    /** 3.2.9: the strands wound round it, how thick each is (its radius, blocks, size 1), and the jet down its middle (its radius). */
    public static final int STRANDS = 3;
    static final double THICK = 0.2;
    static final double JET = 0.11;
    /** Turns a second, and turns of the spiral per block of its length (a loop every four and a half blocks). */
    public static final double SPIN = 1.1;
    public static final double TWIST = 0.22;
    /** Rings along the vortex: at least, and at most (long vortices are drawn with longer steps). */
    static final int FEWEST = 12;
    static final int MOST = 96;
    static final int SIDES = 8;
    static final int JET_SIDES = 6;
    /** 3.2.9: from its own thrower's eyes: how far round it may reach at a block from them (and nothing within {@link #CLEAR} of them). */
    public static final double NEAR_OPEN = 0.2;
    public static final double CLEAR = 0.6;
    /** 3.2.9: from its own thrower's eyes, how far past {@link #CLEAR} it takes to come in solid (blocks): see-through near him. */
    public static final double FADE_IN = 3.5;
    /** Quads one vortex costs at most (the near version), for budgets. */
    public static final int QUADS = STRANDS * MOST * SIDES + MOST * JET_SIDES + 4 + 10;

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
        build(sink, root, head, size, time, seed, eye, fade, unwind, dusk, fine, false);
    }

    /** The same; {@code own}: its thrower's own, seen from his own eyes (first person). */
    public static void build(RcShard.Sink sink, double[] root, double[] head, double size, double time, int seed, double[] eye, double fade,
                             double unwind, double dusk, boolean fine, boolean own) {
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
        int stations = Math.max(FEWEST, Math.min(fine ? MOST : 32, (int)Math.round(length * (fine ? 4.5 : 1.6))));
        Shape shape = new Shape(root, dir, u, v, length, radius, size, spin, twist, own ? eye : null);
        for (int strand = 0; strand < STRANDS; strand++) {
            double phase = Math.PI * 2.0 * strand / STRANDS + 0.4 * hash(seed * 13 + strand);
            double thick = THICK * size * (0.85 + 0.3 * hash(seed * 5 + strand + 9)) * (1.0 + 0.4 * loose);
            tube(sink, shape, phase, thick, stations, fine ? SIDES : 5, strand + seed * 31, time, eye, alpha * (1.0 - 0.6 * loose * loose), dusk);
        }
        jet(sink, shape, stations, fine ? JET_SIDES : 4, eye, alpha * (1.0 - 0.8 * loose), dusk);
        if (!own || dist(head, eye) > 2.0) {
            core(sink, head, dir, size, eye, alpha * (1.0 - loose) * 0.35);
        }
        aura(sink, own ? shape.at(Math.min(1.0, 2.0 / length), 0.0, 0.0) : root, head, dir, radius * 0.7, eye, alpha * (1.0 - 0.7 * loose) * 0.45);
    }

    /** The vortex's line and how it opens: shared by every strand and the jet. {@code own} its thrower's eyes in first person, or null. */
    record Shape(double[] root, double[] dir, double[] u, double[] v, double length, double radius, double size, double spin, double twist, double[] own) {
        /**
         * Radius of the spiral at {@code s} (0 the shoulder blade .. 1 the head): a narrow jet out of the back, opening along its
         * first blocks, rounded off at the very front; from its thrower's own eyes no wider than {@link #NEAR_OPEN} a block away
         * from them.
         */
        double radiusAt(double s) {
            double fromRoot = s * this.length;
            double fromHead = (1.0 - s) * this.length;
            double cone = 0.15 + 0.85 * smooth(fromRoot / Math.max(2.5 * this.size, 0.4 * Math.min(this.length, 8.0)));
            double front = 0.25 + 0.75 * Math.sqrt(smooth(fromHead / (POINT * this.size)));
            double r = this.radius * cone * front;
            if (this.own != null) {
                r = Math.min(r, Math.max(0.0, (dist(this.axis(s), this.own) - CLEAR) * NEAR_OPEN));
            }
            return r;
        }

        /** How much of it is drawn at {@code s}: all of it, but from its thrower's eyes nothing within {@link #CLEAR}, fading in past it. */
        double shown(double s) {
            return this.own == null ? 1.0 : smooth((dist(this.axis(s), this.own) - CLEAR) / FADE_IN);
        }

        /** The spiral's angle at {@code s}, counted from the point (so the drill keeps its look as the vortex lengthens). */
        double angleAt(double s) {
            return this.spin + this.twist * (1.0 - s) * this.length;
        }

        /** Its straight line at {@code s}. */
        double[] axis(double s) {
            double d = s * this.length;
            return new double[] {this.root[0] + this.dir[0] * d, this.root[1] + this.dir[1] * d, this.root[2] + this.dir[2] * d};
        }

        double[] at(double s, double angle, double r) {
            double[] centre = this.axis(s);
            double c = Math.cos(angle) * r;
            double n = Math.sin(angle) * r;
            return new double[] {centre[0] + this.u[0] * c + this.v[0] * n, centre[1] + this.u[1] * c + this.v[1] * n, centre[2] + this.u[2] * c + this.v[2] * n};
        }

        /** The outward way at {@code s}, {@code angle} round it. */
        double[] normal(double angle) {
            return normalize(new double[] {this.u[0] * Math.cos(angle) + this.v[0] * Math.sin(angle), this.u[1] * Math.cos(angle) + this.v[1] * Math.sin(angle),
                this.u[2] * Math.cos(angle) + this.v[2] * Math.sin(angle)});
        }
    }

    /** Where a strand's spine is at {@code s}. */
    static double[] spine(Shape shape, double phase, double s) {
        return shape.at(s, phase + shape.angleAt(s), shape.radiusAt(s));
    }

    /** How hot the fire is at {@code s}: white-hot at the point, through red, crimson further back (0 hot .. 1 deep). */
    static double depth(Shape shape, double s) {
        return Math.min(1.0, (1.0 - s) * shape.length() / (6.0 * shape.size()));
    }

    /** 3.2.9: one strand: a round tube of fire along its spiral, thin where it leaves his back, shaded from the camera. */
    private static void tube(RcShard.Sink sink, Shape shape, double phase, double thick, int stations, int sides, int seed, double time, double[] eye,
                             double alpha, double dusk) {
        double[][][] ring = new double[stations + 1][sides][];
        int[][] colour = new int[stations + 1][sides];
        double step = 0.5 / Math.max(1.0, shape.length());
        for (int k = 0; k <= stations; k++) {
            double s = k / (double)stations;
            double[] centre = spine(shape, phase, s);
            double[] tangent = normalize(sub(spine(shape, phase, Math.min(1.0, s + step)), spine(shape, phase, Math.max(0.0, s - step))));
            double[] n1 = cross(tangent, shape.dir());
            if (dot(n1, n1) < 1.0E-6) {
                n1 = shape.u();
            }
            n1 = normalize(n1);
            double[] n2 = cross(tangent, n1);
            double grow = shape.radiusAt(s) / Math.max(1.0E-6, shape.radius());
            double shown = shape.shown(s);
            double r = thick * (0.45 + 0.55 * Math.min(1.0, grow * 1.4)) * (0.2 + 0.8 * shown);
            double g = depth(shape, s);
            for (int j = 0; j < sides; j++) {
                double theta = 2.0 * Math.PI * j / sides;
                double c = Math.cos(theta);
                double n = Math.sin(theta);
                double[] normal = {n1[0] * c + n2[0] * n, n1[1] * c + n2[1] * n, n1[2] * c + n2[2] * n};
                double[] point = {centre[0] + normal[0] * r, centre[1] + normal[1] * r, centre[2] + normal[2] * r};
                double facing = Math.min(1.0, Math.abs(dot(normal, normalize(sub(eye, point)))));
                double streak = Math.pow(Math.max(0.0, Math.sin(2.0 * theta + 1.6 * (1.0 - s) * shape.length() + seed + 2.0 * time)), 4.0);
                colour[k][j] = fire(g, facing, streak, dusk, alpha * shown);
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

    /** 3.2.9: the thin jet of dark flame down the vortex's middle (2.4.6's solid cone filling it is gone), turning with it. */
    private static void jet(RcShard.Sink sink, Shape shape, int stations, int sides, double[] eye, double alpha, double dusk) {
        if (alpha <= 0.003) {
            return;
        }
        double[][][] ring = new double[stations + 1][sides][];
        int[][] colour = new int[stations + 1][sides];
        for (int k = 0; k <= stations; k++) {
            double s = k / (double)stations;
            double grow = shape.radiusAt(s) / Math.max(1.0E-6, shape.radius());
            double shown = shape.shown(s);
            double r = JET * shape.size() * (0.4 + 0.6 * Math.min(1.0, grow * 1.4)) * shown;
            double g = depth(shape, s);
            for (int j = 0; j < sides; j++) {
                double theta = 2.0 * Math.PI * j / sides + shape.angleAt(s) * 0.7;
                double[] point = shape.at(s, theta, r);
                double[] normal = shape.normal(theta);
                double facing = Math.min(1.0, Math.abs(dot(normal, normalize(sub(eye, point)))));
                double streak = Math.pow(Math.max(0.0, Math.sin(2.0 * theta + 3.0 * (1.0 - s) * shape.length())), 3.0);
                int c = fire(0.15 + 0.85 * g, facing, streak, dusk, alpha * 0.9 * shown);
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

    static double dist(double[] a, double[] b) {
        double[] d = sub(a, b);
        return Math.sqrt(dot(d, d));
    }

    private static double[] normalize(double[] a) {
        double length = Math.sqrt(dot(a, a));
        return length < 1.0E-12 ? new double[] {0.0, 0.0, 1.0} : scale(a, 1.0 / length);
    }
}
