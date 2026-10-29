package com.hex.kagune.control.king;

import static com.hex.kagune.control.furuta.FurutaMesh.add;
import static com.hex.kagune.control.furuta.FurutaMesh.clamp01;
import static com.hex.kagune.control.furuta.FurutaMesh.cross;
import static com.hex.kagune.control.furuta.FurutaMesh.dot;
import static com.hex.kagune.control.furuta.FurutaMesh.mul;
import static com.hex.kagune.control.furuta.FurutaMesh.normalize;
import static com.hex.kagune.control.furuta.FurutaMesh.smooth;
import static com.hex.kagune.control.furuta.FurutaMesh.sub;
import static com.hex.kagune.control.king.WarModel.v;

/**
 * 1.0.25: Peace's six blade arms alive (pure, tested; one of these per player, client side). Before, each arm was a curve worked out
 * afresh every tick from where its blade was ({@link PeaceModel#armCurve}): a prop, never lagging, never whipping, moving only by a
 * sine. Now each is a chain of {@link #NODES} points with weight. Every point but the two ends (the root on his back and the end
 * holding its blade stay exactly where the blades have them, which is what the server cuts along) follows its place on that curve as
 * a spring does, second order, so it lags, overshoots and rings: stiff by the root, loose and heavy in the middle.
 * <ul>
 *   <li>His body moving and turning leaves the arms behind (they are carried by him, not painted on him), and they swing back.</li>
 *   <li>A blade struck out drags its arm after it in a bow and whips it on past; stopping dead on a body, the arm carries on and
 *       rings.</li>
 *   <li>The curve itself lives: two waves running down each arm from his back (crossed, so it snakes rather than sways), a slow
 *       wander of its own, now and then a coil (the arm twisting round itself and unwinding), now and then a twitch; all of it
 *       wilder while its blade is at work.</li>
 *   <li>Stretched out an arm thins (its muscle drawn out with it), bunched up it thickens, and a pulse runs down it.</li>
 * </ul>
 * It runs a tick at a time ({@link #SUBSTEPS} steps a tick) and keeps {@link #HISTORY} ticks of what it drew, so the frames between
 * two ticks (and the builds made a tick ahead) find what they need.
 */
public final class PeaceArms {
    /** Points down each arm (its line as {@link PeaceModel} draws it). */
    public static final int NODES = PeaceModel.ARM_POINTS + 1;
    static final int SUBSTEPS = 2;
    static final int HISTORY = 16;
    /** Not run for this long (ticks), it starts again from where the blades are. */
    static final long REWIND = 40L;
    /** How much of his moving and turning leaves the arms behind (the rest carries them with him). */
    static final double INERTIA = 0.8;
    /** Blocks to body pixels. */
    static final double BLOCK = PeaceMotion.BLOCK;

    /** What the blades are doing, tick by tick: what moves them, and how grown its kagune is. */
    public interface Source {
        PeaceMotion.State state(long tick);

        double grow(long tick);
    }

    // Each point's spring (second-order dynamics: k1 its damping, k2 its weight, kept stable for the step, k3 how it leads a change).
    static final double[] K1 = new double[NODES];
    static final double[] K2 = new double[NODES];
    static final double[] K3 = new double[NODES];
    /** Each arm's length as it hangs at rest (body pixels): stretched past it the arm thins, bunched short of it it thickens. */
    static final double[] REST_LENGTH = new double[PeaceMotion.BLADES];

    static {
        double step = 1.0 / SUBSTEPS;
        for (int i = 0; i < NODES; i++) {
            double w = Math.sin(Math.PI * i / (NODES - 1));
            double f = 0.19 - 0.11 * w; // cycles a tick: quick by the root, slow and heavy in the middle
            double zeta = 0.5 - 0.16 * w; // under-damped: it overshoots and rings, the middle most
            double r = 1.2 + 1.1 * w; // leading a change: the whip
            double k1 = zeta / (Math.PI * f);
            double k2 = 1.0 / ((2.0 * Math.PI * f) * (2.0 * Math.PI * f));
            K1[i] = k1;
            K2[i] = Math.max(k2, Math.max(step * step / 2.0 + step * k1 / 2.0, step * k1));
            K3[i] = r * zeta / (2.0 * Math.PI * f);
        }
        for (int blade = 0; blade < PeaceMotion.BLADES; blade++) {
            REST_LENGTH[blade] = length(PeaceModel.armCurve(blade, PeaceMotion.rest(blade, 0.0, 0.0, 0.0, 0.0)));
        }
    }

    private final long seed;
    /** Each point where it is and how fast it goes (body pixels, a tick). */
    private final double[][][] y = new double[PeaceMotion.BLADES][NODES][3];
    private final double[][][] yd = new double[PeaceMotion.BLADES][NODES][3];
    /** Where each point would be, still, last tick (the curve it follows). */
    private final double[][][] x = new double[PeaceMotion.BLADES][][];
    /** Each blade's end of its arm last tick, the way it went and how fast; how hard it is at work (0..1). */
    private final double[][] end = new double[PeaceMotion.BLADES][];
    private final double[][] heading = new double[PeaceMotion.BLADES][];
    private final double[] speed = new double[PeaceMotion.BLADES];
    private final double[] activity = new double[PeaceMotion.BLADES];
    /** Where each arm's two waves and its pulse are (radians: run on a tick at a time, so a change of pace never jumps). */
    private final double[][] phase = new double[PeaceMotion.BLADES][3];
    /** How thick each arm is drawn for its blade's size and growth. */
    private final double[] scale = new double[PeaceMotion.BLADES];
    private long tick = Long.MIN_VALUE;
    private final long[] keptAt = new long[HISTORY];
    private final double[][][][] kept = new double[HISTORY][][][];

    public PeaceArms(long seed) {
        this.seed = seed;
        java.util.Arrays.fill(this.keptAt, Long.MIN_VALUE);
    }

    /**
     * Each arm's line at tick {@code at} (per blade, {x, y, z, radius} body pixels, root first), run on to it if need be; {@code motion}:
     * how his body moves a tick ({turn radians (+ to his right), blocks to his right, up, ahead}), or null.
     */
    public double[][][] lines(long at, Source source, double[] motion) {
        if (this.tick == Long.MIN_VALUE || at - this.tick > REWIND) {
            this.start(at, source);
        }
        while (this.tick < at) {
            this.step(this.tick + 1, source, motion);
        }
        double[][][] got = this.kept(at);
        return got != null ? got : this.kept(this.tick);
    }

    /** As above at {@code time} between two ticks: every point between where it is at the tick before and the tick after. */
    public double[][][] lines(double time, Source source, double[] motion) {
        long a = (long)Math.floor(time);
        double k = time - a;
        double[][][] from = this.lines(a, source, motion);
        if (k < 1.0E-6) {
            return from;
        }
        double[][][] to = this.lines(a + 1, source, motion);
        double[][][] out = new double[from.length][][];
        for (int blade = 0; blade < from.length; blade++) {
            out[blade] = new double[from[blade].length][4];
            for (int i = 0; i < from[blade].length; i++) {
                for (int c = 0; c < 4; c++) {
                    out[blade][i][c] = from[blade][i][c] + (to[blade][i][c] - from[blade][i][c]) * k;
                }
            }
        }
        return out;
    }

    /** Starting afresh at {@code at}: every point where it would be, still. */
    private void start(long at, Source source) {
        PeaceMotion.State s = source.state(at);
        double held = PeaceModel.held(source.grow(at), s);
        for (int blade = 0; blade < PeaceMotion.BLADES; blade++) {
            PeaceMotion.Tip tip = PeaceMotion.tip(blade, (double)at, held, s);
            this.scale[blade] = scale(blade, tip, held);
            this.activity[blade] = 0.0;
            this.speed[blade] = 0.0;
            this.heading[blade] = v(0, 0, 0);
            double[][] want = this.intent(blade, PeaceModel.armCurve(blade, tip), at);
            this.x[blade] = want;
            this.end[blade] = want[NODES - 1].clone();
            for (int i = 0; i < NODES; i++) {
                this.y[blade][i] = want[i].clone();
                this.yd[blade][i] = v(0, 0, 0);
            }
        }
        this.tick = at;
        this.keep(at);
    }

    /** One tick on, to {@code t}. */
    private void step(long t, Source source, double[] motion) {
        PeaceMotion.State s = source.state(t);
        double held = PeaceModel.held(source.grow(t), s);
        double turn = motion == null ? 0.0 : motion[0] * INERTIA;
        double[] carried = motion == null ? v(0, 0, 0) : v(motion[1] * BLOCK * INERTIA, -motion[2] * BLOCK * INERTIA, motion[3] * BLOCK * INERTIA);
        double dt = 1.0 / SUBSTEPS;
        for (int blade = 0; blade < PeaceMotion.BLADES; blade++) {
            PeaceMotion.Tip tip = PeaceMotion.tip(blade, (double)t, held, s);
            this.scale[blade] = scale(blade, tip, held);
            // How its blade goes: fast, it is at work; stopped dead after going fast, it struck something and the arm carries on.
            double[][] base = PeaceModel.armCurve(blade, tip);
            double[] at = base[NODES - 1];
            double[] moved = sub(at, this.end[blade]);
            double sp = Math.sqrt(dot(moved, moved));
            if (this.speed[blade] > 4.0 && sp < 0.35 * this.speed[blade]) {
                this.ring(blade, this.heading[blade], this.speed[blade]);
            }
            if (sp > 1.0E-6) {
                this.heading[blade] = mul(moved, 1.0 / sp);
            }
            this.speed[blade] = sp;
            this.end[blade] = at;
            this.activity[blade] = Math.max(this.activity[blade] * 0.88, clamp01(sp / 7.0));
            double act = this.activity[blade];
            this.phase[blade][0] += 0.12 + 0.55 * act;
            this.phase[blade][1] += 0.19 + 0.7 * act;
            this.phase[blade][2] += 0.09 + 0.3 * act;
            double[][] want = this.intent(blade, base, t);
            double[][] was = this.x[blade];
            double[][] p = this.y[blade];
            double[][] pd = this.yd[blade];
            // Left behind by his body: what he moves and turns by, the arm is not (all of it but its two ends).
            for (int i = 1; i < NODES - 1; i++) {
                p[i] = add(turned(p[i], turn), carried);
            }
            this.twitch(blade, t, want);
            // Each point after its place on the curve.
            for (int k = 0; k < SUBSTEPS; k++) {
                double f = (k + 1.0) / SUBSTEPS;
                for (int i = 1; i < NODES - 1; i++) {
                    double[] goal = add(was[i], mul(sub(want[i], was[i]), f));
                    double[] goalSpeed = sub(want[i], was[i]);
                    double[] pull = sub(add(goal, mul(goalSpeed, K3[i])), add(p[i], mul(pd[i], K1[i])));
                    p[i] = add(p[i], mul(pd[i], dt));
                    pd[i] = add(pd[i], mul(pull, dt / K2[i]));
                }
            }
            // One body, not loose beads: each point's way off the curve (and its speed) eased toward its neighbours', so it bends
            // in curves and never kinks.
            smoothOff(p, want, 2);
            smoothOff(pd, null, 1);
            // Never so far off its curve it stops reading as the arm holding that blade.
            double[] span = sub(want[NODES - 1], want[0]);
            double most = 3.0 + 0.3 * Math.sqrt(dot(span, span));
            for (int i = 1; i < NODES - 1; i++) {
                double[] off = sub(p[i], want[i]);
                double d = Math.sqrt(dot(off, off));
                if (d > most) {
                    p[i] = add(want[i], mul(off, most / d));
                    pd[i] = mul(pd[i], 0.5);
                }
            }
            p[0] = want[0].clone();
            p[NODES - 1] = want[NODES - 1].clone();
            pd[0] = v(0, 0, 0);
            pd[NODES - 1] = v(0, 0, 0);
            this.x[blade] = want;
        }
        this.tick = t;
        this.keep(t);
    }

    /**
     * Each point of {@code p} (but the two ends) moved toward its neighbours, {@code passes} times, as its way off {@code want} (or, no
     * {@code want}, as itself): its offsets smoothed down the arm (1 2 1), the ends held at none.
     */
    static void smoothOff(double[][] p, double[][] want, int passes) {
        int n = p.length;
        double[][] off = new double[n][];
        for (int i = 0; i < n; i++) {
            off[i] = want == null ? p[i] : sub(p[i], want[i]);
        }
        off[0] = v(0, 0, 0);
        off[n - 1] = v(0, 0, 0);
        for (int pass = 0; pass < passes; pass++) {
            double[][] next = off.clone();
            for (int i = 1; i < n - 1; i++) {
                next[i] = mul(add(add(off[i - 1], off[i + 1]), mul(off[i], 2.0)), 0.25);
            }
            off = next;
        }
        for (int i = 1; i < n - 1; i++) {
            p[i] = want == null ? off[i] : add(want[i], off[i]);
        }
    }

    /** Its blade stopped dead going {@code speed} along {@code heading}: the arm carries on that way, its middle most, and rings. */
    private void ring(int blade, double[] heading, double speed) {
        for (int i = 1; i < NODES - 1; i++) {
            double w = Math.sin(Math.PI * i / (NODES - 1));
            this.yd[blade][i] = add(this.yd[blade][i], mul(heading, 0.6 * speed * Math.pow(w, 1.2)));
        }
    }

    /** Now and then at rest, a twitch: the arm jerked to one side all at once, and it rings out. */
    private void twitch(int blade, long t, double[][] want) {
        if (this.activity[blade] > 0.3) {
            return;
        }
        long window = 64L;
        long offset = blade * 23L;
        long w = Math.floorDiv(t + offset, window);
        if (hash(this.seed, blade, w, 0) > 0.55) {
            return;
        }
        long when = w * window - offset + (long)(hash(this.seed, blade, w, 1) * window);
        if (when != t) {
            return;
        }
        double[][] frame = frame(want);
        double a = hash(this.seed, blade, w, 2) * Math.PI * 2.0;
        double[] dir = add(mul(frame[1], Math.cos(a)), mul(frame[2], Math.sin(a)));
        double strength = 1.6 + 1.8 * hash(this.seed, blade, w, 3);
        for (int i = 1; i < NODES - 1; i++) {
            double k = Math.pow(Math.sin(Math.PI * i / (NODES - 1)), 1.5);
            this.yd[blade][i] = add(this.yd[blade][i], mul(dir, strength * k));
        }
    }

    /**
     * Where each point of blade {@code blade}'s arm would be at {@code t}: its still shape {@code base} ({@link PeaceModel#armCurve})
     * with life in it: two waves down it from his back, crossed; a slow wander; now and then a coil; its weight.
     */
    double[][] intent(int blade, double[][] base, long t) {
        double[][] frame = frame(base);
        double[] across = frame[1];
        double[] lift = frame[2];
        double length = frame[3][0];
        double act = this.activity[blade];
        double a1 = (1.4 + 0.04 * length) * (1.0 + 1.2 * act);
        double a2 = 0.75 * a1;
        double ph = blade * 1.7 + Math.floorMod(this.seed, 97L) * 0.13;
        double wander = 3.4 * (1.0 - 0.6 * act);
        double nx = Math.sin(t * 0.021 + ph) * 0.6 + Math.sin(t * 0.047 + ph * 2.3) * 0.3 + Math.sin(t * 0.093 + ph * 0.7) * 0.15;
        double ny = Math.sin(t * 0.026 + ph * 1.9) * 0.6 + Math.sin(t * 0.053 + ph * 0.4) * 0.3 + Math.sin(t * 0.081 + ph * 3.1) * 0.15;
        // Now and then a coil: the arm twisted round on itself, a turn down its length, and let go again.
        long window = 170L;
        long offset = blade * 41L;
        long w = Math.floorDiv(t + offset, window);
        double coil = 0.0;
        double coilTurn = 0.0;
        if (hash(this.seed, blade, w, 4) < 0.45) {
            double from = w * window - offset + hash(this.seed, blade, w, 5) * 90.0;
            double u = (t - from) / 50.0;
            if (u > 0.0 && u < 1.0) {
                double b = Math.sin(Math.PI * u);
                coil = (4.5 + 0.05 * length) * b * b;
                coilTurn = 6.0 * u * (hash(this.seed, blade, w, 6) < 0.5 ? -1.0 : 1.0);
            }
        }
        double side = PeaceMotion.right(blade) > 0 ? -1.0 : 1.0;
        double[][] out = new double[NODES][];
        out[0] = base[0];
        out[NODES - 1] = base[NODES - 1];
        for (int i = 1; i < NODES - 1; i++) {
            double s = (double)i / (NODES - 1);
            double env = Math.sin(Math.PI * s);
            double loose = Math.pow(env, 1.2);
            double sway = a1 * env * Math.sin(2.0 * Math.PI * 0.85 * s - this.phase[blade][0]) + wander * loose * nx;
            double rise = a2 * env * Math.sin(2.0 * Math.PI * 1.35 * s - this.phase[blade][1] + 1.1) + wander * loose * ny;
            if (coil > 0.0) {
                double angle = 2.0 * Math.PI * s + coilTurn;
                double k = coil * Math.pow(env, 1.3);
                sway += k * Math.cos(angle);
                rise += k * Math.sin(angle);
            }
            double[] p = add(base[i], add(mul(across, sway), mul(lift, rise)));
            p = add(p, v(0.0, -1.2 * env, 0.0)); // its weight
            out[i] = PeaceModel.clearOf(p, side, s, PeaceModel.radius(blade, s));
        }
        return out;
    }

    /** What it draws at {@code t}: each point as far off its curve as its spring has it (the two ends held), thinned as it is drawn out. */
    private void keep(long t) {
        double[][][] lines = new double[PeaceMotion.BLADES][][];
        for (int blade = 0; blade < PeaceMotion.BLADES; blade++) {
            double side = PeaceMotion.right(blade) > 0 ? -1.0 : 1.0;
            double[][] want = this.x[blade];
            double[][] line = new double[NODES][];
            for (int i = 0; i < NODES; i++) {
                double s = (double)i / (NODES - 1);
                double held = Math.max(1.0 - smooth(clamp01(s / 0.12)), smooth(clamp01((s - 0.62) / 0.38)));
                double[] p = add(want[i], mul(sub(this.y[blade][i], want[i]), 1.0 - held));
                if (i > 0 && i < NODES - 1) {
                    p = PeaceModel.clearOf(p, side, s, PeaceModel.radius(blade, s));
                }
                line[i] = new double[] {p[0], p[1], p[2], 0.0};
            }
            double thin = Math.max(0.7, Math.min(1.3, Math.pow(REST_LENGTH[blade] / Math.max(1.0, length(line)), 0.4)));
            for (int i = 0; i < NODES; i++) {
                double s = (double)i / (NODES - 1);
                double pulse = 1.0 + 0.12 * Math.sin(2.0 * Math.PI * 1.4 * s - this.phase[blade][2] + blade);
                line[i][3] = PeaceModel.radius(blade, s) * this.scale[blade] * thin * pulse;
            }
            lines[blade] = line;
        }
        int slot = (int)Math.floorMod(t, (long)HISTORY);
        this.keptAt[slot] = t;
        this.kept[slot] = lines;
    }

    private double[][][] kept(long t) {
        int slot = (int)Math.floorMod(t, (long)HISTORY);
        return this.keptAt[slot] == t ? this.kept[slot] : null;
    }

    /** How thick an arm is drawn for its blade ({@code tip}'s size against its own) and how far out it is held. */
    static double scale(int blade, PeaceMotion.Tip tip, double held) {
        return Math.max(0.3, held) * Math.min(1.0, tip.size() / (0.8 * PeaceMotion.size(blade)));
    }

    /** {@code p} (body pixels) turned {@code a} radians round his middle as a point fixed in the world is when he turns {@code a} to his right. */
    static double[] turned(double[] p, double a) {
        if (a == 0.0) {
            return p;
        }
        double c = Math.cos(a);
        double s = Math.sin(a);
        return v(p[0] * c - p[2] * s, p[1], p[0] * s + p[2] * c);
    }

    /** An arm's own frame from its line: {along it, across it (level), up off it, {its root-to-end length}}. */
    static double[][] frame(double[][] line) {
        double[] span = sub(line[line.length - 1], line[0]);
        double length = Math.sqrt(dot(span, span));
        double[] dir = length < 1.0E-6 ? v(0, 1, 0) : mul(span, 1.0 / length);
        double[] across = cross(dir, v(0, 1, 0));
        across = dot(across, across) < 1.0E-6 ? v(1, 0, 0) : normalize(across);
        double[] lift = normalize(cross(across, dir));
        return new double[][] {dir, across, lift, {length}};
    }

    /** A line's length along it (its first three numbers a point). */
    static double length(double[][] line) {
        double total = 0.0;
        for (int i = 1; i < line.length; i++) {
            double dx = line[i][0] - line[i - 1][0];
            double dy = line[i][1] - line[i - 1][1];
            double dz = line[i][2] - line[i - 1][2];
            total += Math.sqrt(dx * dx + dy * dy + dz * dz);
        }
        return total;
    }

    /** A number in [0, 1) from {@code seed}, {@code blade}, {@code w} and {@code k}, the same every time. */
    static double hash(long seed, int blade, long w, int k) {
        long h = seed * 0x9E3779B97F4A7C15L + blade * 0xC2B2AE3D27D4EB4FL + w * 0x165667B19E3779F9L + k * 0x27D4EB2F165667C5L;
        h ^= h >>> 33;
        h *= 0xFF51AFD7ED558CCDL;
        h ^= h >>> 33;
        h *= 0xC4CEB9FE1A85EC53L;
        h ^= h >>> 33;
        return (h >>> 11) * 0x1.0p-53;
    }
}
