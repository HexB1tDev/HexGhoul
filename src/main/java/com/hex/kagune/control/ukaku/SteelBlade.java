package com.hex.kagune.control.ukaku;

import java.util.ArrayList;
import java.util.List;

/**
 * The final kakuja's Steel Blade (pure, tested; 5.0.17, rebuilt in 5.0.19 after the manga's "close combat" panels). C closes his
 * steel wings round his right arm: the steel tentacles draw back into his back while the wing plates sweep round the arm from
 * behind and wrap it, the steel swells out under them, a pauldron fans in over the shoulder, then the blade drives out past the
 * hand, its edge unfurls, the plates' barbs spring out down its back and a glint runs to the point. C again plays it backwards.
 * While it is out he is far faster and jumps far higher, M1 swings it wide and fast, and its reach is its length ({@link #REACH}).
 *
 * <p>It is no flat blade but a cone with an edge: round and massive where it swallows the arm from the shoulder, tapering to a
 * needle point, one side drawn out into a single long curved cutting edge (its cross-section a teardrop: the cone's round, and
 * the edge's two hollow-ground flanks). Over its back and sides lie the wing plates it was made of, long feathers stacked one on
 * another, the outer ones shorter, each narrowing to a point that peels up off the one beneath into a barb swept toward the
 * tip: the jagged row along its back. Mottled dark steel clouded with white, dark rims, a bright line along the edge.
 *
 * <p>{@link #build} lays it out in the right arm's own pixels (x to the model's left, y down the arm from the shoulder, z
 * backward), so it rides every pose of the arm: its axis runs down the arm, the edge toward his body (+x: below the arm when it
 * is raised out to the side, leading a swing across him), the barbs on the outside.
 */
public final class SteelBlade {
    /** Ticks the wings take to close into the blade (and to open again). */
    public static final int FORM_TICKS = 24;
    /** The blade's length past the hand (blocks), and his reach with it: the arm, then all of the blade. */
    public static final double LENGTH = 2.2;
    public static final double ARM = 0.75;
    public static final double REACH = ARM + LENGTH + 0.4;
    /** Extra levels of Speed and Jump Boost while it is out. */
    public static final int SPEED = 3;
    public static final int JUMP = 2;
    /** Model pixels per block (the player model is drawn at 15/16). */
    public static final double PIXELS = 16.0 / 0.9375;
    /** The shoulder line, where the blade drives out of (inside the fist), the hand's end and the point (arm pixels down). */
    public static final double BASE_Y = -2.0;
    public static final double GRIP_Y = 9.0;
    public static final double HAND_Y = 10.0;
    public static final double TIP_Y = HAND_Y + LENGTH * PIXELS;
    /** The cone's radius round the upper arm, how far its edge stands from its axis at most, and at the shoulder. */
    public static final double BASE_RADIUS = 4.1;
    static final double EDGE_MOST = 5.4;
    static final double EDGE_BASE = 4.6;
    /** Where along it (share of its length) the edge stands out furthest. */
    static final double EDGE_PEAK = 0.22;
    /** The dome over the shoulder, as a share of the length above the shoulder line. */
    static final double DOME = 2.4 / (TIP_Y - BASE_Y);
    /** How far the edge's flanks are hollowed (share of how far each stands past the round). */
    static final double HOLLOW = 0.3;
    /** How long each barb's rise is (share of the length): about the gap between barbs, so they stand in a saw-toothed row. */
    static final double RAMP = 0.1;
    /**
     * The wing plates along it, innermost (the longest) first: where each ends (share of the length), how far its barb stands
     * out, and its half-span round the cone (share of a half turn, centred on the back).
     */
    static final double[][] PLATES = {{0.72, 2.3, 0.88}, {0.62, 2.8, 0.82}, {0.525, 3.2, 0.76}, {0.435, 3.5, 0.7}, {0.35, 3.7, 0.64},
        {0.27, 3.8, 0.58}};
    /** Each plate's thickness (how far it stands over the one beneath). */
    static final double PLATE = 0.36;

    static final int STEEL = 0x5C6069;
    static final int STEEL_DARK = 0x24262D;
    static final int STEEL_LIGHT = 0xA5ABB5;
    static final int CLOUD = 0xE4E8EE;
    static final int EDGE = 0xF6F8FB;
    static final int INK = 0x141519;
    /** 5.0.20: blood on the steel, fresh and dried. */
    static final int BLOOD_FRESH = 0x9C070D;
    static final int BLOOD_DRY = 0x3A0909;

    /** Grid sizes: stations down the cone and round it, along and across each plate. */
    static final int STATIONS = 54;
    static final int AROUND = 36;
    static final int ALONG = 20;
    static final int ACROSS = 8;

    private SteelBlade() {
    }

    /** Takes the blade's quads, four corners in a row each: where (arm pixels), its colour (ARGB), its normal (unit, outward). */
    public interface Sink {
        void vertex(double x, double y, double z, int argb, double nx, double ny, double nz);
    }

    /** One quad: four corners (arm pixels), a colour each (ARGB) and a normal each (unit, outward). */
    public record Quad(double[][] corners, int[] argb, double[][] normals) {
    }

    // ------------------------------------------------------------------ the forming

    /** How closed into the blade the wings are, {@code since} ticks after C was pressed ({@code on}: closing), 0..1. */
    public static double formed(boolean on, double since) {
        double t = clamp01(since / FORM_TICKS);
        return on ? t : 1.0 - t;
    }

    /** How far the steel tentacles have drawn back into his back at {@code formed} (0 out .. 1 gone). */
    public static double fold(double formed) {
        return smoother(formed / 0.42);
    }

    /** How far the pauldron has fanned in over the shoulder (0..1). */
    static double pauldron(double formed) {
        return smoother((formed - 0.04) / 0.32);
    }

    /** How far wing plate {@code k} has swept round the arm from behind and closed on it (0..1), the innermost first. */
    static double wrap(double formed, int k) {
        return smoother((formed - 0.03 - 0.04 * k) / 0.28);
    }

    /** How far plate {@code k} has slid down to its full length (0..1). */
    static double slide(double formed, int k) {
        return smoother((formed - 0.1 - 0.045 * k) / 0.36);
    }

    /** How far the steel has swelled out of the arm into the cone round it (0..1). */
    static double swell(double formed) {
        return smoother((formed - 0.1) / 0.3);
    }

    /** How far the blade has driven out past the hand (a touch past 1 as it lands, then settles). */
    public static double extend(double formed) {
        return backOut((formed - 0.28) / 0.44);
    }

    /** How far its edge has unfurled out of the cone. */
    static double unfurl(double formed) {
        return 0.22 + 0.78 * smoother((formed - 0.42) / 0.36);
    }

    /** How far plate {@code k}'s barb has sprung up (0..1, a touch past as it lands): from the shoulder's toward the point's. */
    static double barb(double formed, int k) {
        return backOut((formed - 0.575 - 0.034 * (PLATES.length - 1 - k)) / 0.255);
    }

    // ------------------------------------------------------------------ the shape

    /** Where station {@code s} (0 the shoulder line .. 1 the point; below 0 the dome over the shoulder) is down the arm. */
    public static double station(double s) {
        return BASE_Y + s * (TIP_Y - BASE_Y);
    }

    /** The cone's round radius at {@code s}: massive round the arm, a needle at the point. */
    public static double radius(double s) {
        if (s >= 1.0) {
            return 0.0;
        }
        if (s < 0.0) {
            return BASE_RADIUS * dome(s);
        }
        return BASE_RADIUS * Math.pow(1.0 - s, 1.18) * (1.0 + 0.32 * s);
    }

    /** How far its cutting edge stands from its axis at {@code s}: one long curve, out from the shoulder, in to the point. */
    public static double reach(double s) {
        if (s >= 1.0) {
            return 0.0;
        }
        if (s < 0.0) {
            return EDGE_BASE * dome(s);
        }
        if (s < EDGE_PEAK) {
            return EDGE_BASE + (EDGE_MOST - EDGE_BASE) * Math.sin(Math.PI * 0.5 * s / EDGE_PEAK);
        }
        double u = (s - EDGE_PEAK) / (1.0 - EDGE_PEAK);
        return EDGE_MOST * Math.pow(1.0 - u, 1.15) * (1.0 + 0.25 * u * (1.0 - u));
    }

    /** The dome over the shoulder: 1 at the shoulder line, closing to 0 at its top. */
    static double dome(double s) {
        double d = clamp01(-s / DOME);
        return Math.sqrt(1.0 - d * d);
    }

    /**
     * How far the cone's surface is from its axis toward {@code theta} (0 the edge, +x; a half turn the back), at a section of
     * round radius {@code r} with its edge {@code e} out: the round, and two flanks drawn out from it to the edge, hollowed a touch.
     */
    public static double section(double r, double e, double theta) {
        double t = Math.abs(wrapAngle(theta));
        if (e <= r + 1.0E-6) {
            return r;
        }
        if (r <= 1.0E-6) {
            return t < 1.0E-6 ? e : 0.0;
        }
        double alpha = Math.acos(r / e);
        if (t >= alpha) {
            return r;
        }
        // Hollow-ground: the flanks dished between the edge and the round, never inside the round.
        return r + (r / Math.cos(alpha - t) - r) * (1.0 - HOLLOW * Math.sin(Math.PI * t / alpha));
    }

    /** {@code a} into (-pi, pi]. */
    static double wrapAngle(double a) {
        double x = a % (Math.PI * 2.0);
        if (x > Math.PI) {
            x -= Math.PI * 2.0;
        } else if (x <= -Math.PI) {
            x += Math.PI * 2.0;
        }
        return x;
    }

    /** The blade's whole length from the shoulder line to the point (pixels). */
    public static double fullLength() {
        return TIP_Y - BASE_Y;
    }

    /** The axis's x for the arm: a little out from the arm's own middle, so the cone swallows it with room to spare. */
    static double axis(boolean slim) {
        return (slim ? -0.5 : -1.0) - 0.4;
    }

    /** One frame of it: how far each part of the forming has got, and the ambient life. */
    private static final class Frame {
        final double formed;
        final double time;
        final double ax;
        final double cx;
        final double halfX;
        final double swell;
        final double ext;
        final double unfurl;
        final double glint;
        final double shing;
        final double[] wrap = new double[PLATES.length];
        final double[] slide = new double[PLATES.length];
        final double[] barb = new double[PLATES.length];
        /** How bloodied it is (0..1) and how dry the blood (0 fresh .. 1 dry). */
        double blood;
        double dry;

        Frame(double formed, double time, boolean slim) {
            this(formed, time, slim, 1.0);
        }

        /** {@code reach} (0..1): how much of its driven-out length is out (6.0.4: drawn in to rest off the ground). */
        Frame(double formed, double time, boolean slim, double reach) {
            this.formed = formed;
            this.time = time;
            this.ax = axis(slim);
            this.cx = slim ? -0.5 : -1.0;
            this.halfX = slim ? 1.5 : 2.0;
            this.swell = SteelBlade.swell(formed);
            this.ext = extend(formed) * (Double.isFinite(reach) ? Math.max(0.0, Math.min(1.0, reach)) : 1.0);
            this.unfurl = SteelBlade.unfurl(formed);
            this.glint = ((time * 0.36) % 1.0) * 1.6 - 0.3;
            this.shing = formed < 0.999 ? (formed - 0.78) / 0.22 * 1.35 - 0.2 : -9.0;
            for (int k = 0; k < PLATES.length; k++) {
                wrap[k] = SteelBlade.wrap(formed, k);
                slide[k] = SteelBlade.slide(formed, k);
                barb[k] = SteelBlade.barb(formed, k);
            }
        }

        /**
         * Where station {@code s} is down the arm now: past the fist, drawn in as far as the blade has driven out; the dome over
         * the shoulder rising as the steel swells.
         */
        double y(double s) {
            double y = station(s);
            if (y < BASE_Y) {
                return BASE_Y + (y - BASE_Y) * swell;
            }
            return y <= GRIP_Y ? y : GRIP_Y + (y - GRIP_Y) * ext;
        }

        /** The cone's surface from its axis toward {@code theta} at {@code s}, as far as it has swelled and unfurled. */
        double core(double s, double theta) {
            double r = radius(s);
            double e = r + (reach(s) - r) * unfurl;
            double grown = 0.28 + 0.72 * swell;
            return section(r * grown, e * grown, theta);
        }

        /** How far from the axis the arm itself reaches toward {@code theta} at {@code y} (nothing past the hand). */
        double arm(double y, double theta) {
            if (y < BASE_Y - 0.2 || y > HAND_Y + 0.4) {
                return 0.0;
            }
            double c = Math.cos(theta);
            double sn = Math.sin(theta);
            double tx = c > 1.0E-6 ? (cx + halfX - ax) / c : c < -1.0E-6 ? (cx - halfX - ax) / c : 1.0E9;
            double tz = Math.abs(sn) > 1.0E-6 ? 2.0 / Math.abs(sn) : 1.0E9;
            return Math.min(tx, tz);
        }

        /** {@code colour} with the blood on the steel at (x, y, z), {@code s} along it, {@code edgeward} 0..1 toward the edge. */
        int bloodied(int colour, double x, double y, double z, double s, double edgeward) {
            if (this.blood <= 0.0) {
                return colour;
            }
            double n = 0.5 + 0.5 * Math.sin(x * 1.3 + y * 0.53 + z * 0.9) * Math.sin(y * 0.29 - z * 1.1 + 2.1) + 0.3 * Math.sin(y * 1.7 + x * 2.3);
            double along = smoother((s - 0.22) / 0.2) * (1.0 - smoother((s - 0.93) / 0.07));
            double threshold = 1.05 - this.blood * (0.6 + 0.5 * edgeward) * along;
            double mask = smoother((n - threshold) / 0.2);
            return mix(colour, mix(BLOOD_FRESH, BLOOD_DRY, this.dry), 0.92 * mask);
        }

        /** How brightly the glints light it at {@code s}: the ambient one running down the edge, and the one as it forms. */
        double shine(double s) {
            double a = Math.exp(-Math.pow((s - glint) / 0.055, 2.0));
            double b = Math.exp(-Math.pow((s - shing) / 0.08, 2.0));
            return Math.min(1.0, a * 0.85 + b);
        }
    }

    /** The blade and its armour, {@code formed} of the way closed (0..1), at {@code time} seconds, as quads. */
    public static List<Quad> build(double formed, double time, boolean slim) {
        List<Quad> out = new ArrayList<>();
        double[][] corners = new double[4][];
        int[] colours = new int[4];
        double[][] normals = new double[4][];
        int[] n = {0};
        build(formed, time, slim, true, (x, y, z, argb, nx, ny, nz) -> {
            int k = n[0]++;
            corners[k] = new double[] {x, y, z};
            colours[k] = argb;
            normals[k] = new double[] {nx, ny, nz};
            if (n[0] == 4) {
                out.add(new Quad(corners.clone(), colours.clone(), normals.clone()));
                n[0] = 0;
            }
        });
        return out;
    }

    /**
     * The blade and its armour, {@code formed} of the way closed (0..1), at {@code time} seconds (the ambient life: a glint
     * running down the edge, the plates breathing, the barbs flexing), into {@code sink}. {@code slim} for the three-pixel arms;
     * {@code shoulder} false leaves the pauldron off (in first person it would fill the view).
     */
    public static void build(double formed, double time, boolean slim, boolean shoulder, Sink sink) {
        build(formed, time, slim, shoulder, 0.0, 0.0, sink);
    }

    /** As {@link #build(double, double, boolean, boolean, Sink)}, bloodied {@code blood} (0..1), the blood {@code dry} (0..1). */
    public static void build(double formed, double time, boolean slim, boolean shoulder, double blood, double dry, Sink sink) {
        build(formed, time, slim, shoulder, blood, dry, 1.0, sink);
    }

    /** As above, {@code reach} (0..1) of its driven-out length out (6.0.4: drawn in, resting near the ground). */
    public static void build(double formed, double time, boolean slim, boolean shoulder, double blood, double dry, double reach, Sink sink) {
        build(formed, time, slim, CORE | WING_PLATES | (shoulder ? PAULDRON : 0), blood, dry, reach, sink);
    }

    /** Its parts, for {@link #build(double, double, boolean, int, Sink)}. */
    static final int CORE = 1;
    static final int WING_PLATES = 2;
    static final int PAULDRON = 4;

    /** The {@code parts} of it asked for (any of {@link #CORE}, {@link #WING_PLATES}, {@link #PAULDRON}). */
    static void build(double formed, double time, boolean slim, int parts, Sink sink) {
        build(formed, time, slim, parts, 0.0, 0.0, sink);
    }

    static void build(double formed, double time, boolean slim, int parts, double blood, double dry, Sink sink) {
        build(formed, time, slim, parts, blood, dry, 1.0, sink);
    }

    static void build(double formed, double time, boolean slim, int parts, double blood, double dry, double reach, Sink sink) {
        if (formed <= 0.005) {
            return;
        }
        Frame f = new Frame(formed, time, slim, reach);
        f.blood = Math.max(0.0, Math.min(1.0, blood));
        f.dry = Math.max(0.0, Math.min(1.0, dry));
        if ((parts & CORE) != 0) {
            core(f, sink);
        }
        if ((parts & WING_PLATES) != 0) {
            for (int k = 0; k < PLATES.length; k++) {
                plate(f, k, sink);
            }
        }
        if ((parts & PAULDRON) != 0) {
            pauldron(f, sink);
        }
    }

    /**
     * How far from its axis any part of it reaches at {@code s} (0 the shoulder line .. 1 the point; pixels): every plate, barb
     * and the pauldron, fully formed, with room for the barbs' flex and the plates' breathing. For keeping it off the ground.
     */
    public static double extent(double s) {
        double[] table = Extents.TABLE;
        double at = clamp01(s) * (table.length - 1);
        int i = (int)Math.min(table.length - 2, Math.floor(at));
        double t = at - i;
        return table[i] + (table[i + 1] - table[i]) * t;
    }

    /** {@link #extent}'s table, from the built blade (once). */
    private static final class Extents {
        static final double[] TABLE = compute();

        static double[] compute() {
            int bins = 64;
            double[] most = new double[bins + 1];
            double ax = axis(false);
            for (Quad q : build(1.0, 0.0, false)) {
                for (double[] c : q.corners()) {
                    double s = clamp01((c[1] - BASE_Y) / fullLength());
                    int i = (int)Math.round(s * bins);
                    most[i] = Math.max(most[i], Math.hypot(c[0] - ax, c[2]));
                }
            }
            double[] out = new double[bins + 1];
            for (int i = 0; i <= bins; i++) {
                double m = most[i];
                for (int j = Math.max(0, i - 2); j <= Math.min(bins, i + 2); j++) {
                    m = Math.max(m, most[j]);
                }
                // The barbs flex 6% further out, the plates breathe, a slim arm sits half a pixel over.
                out[i] = m + 0.8;
            }
            return out;
        }
    }

    /** The point of the blade's edge at {@code s}, {@code formed} of the way closed (arm pixels): for the swing trail. */
    public static double[] edgePoint(double s, double formed, boolean slim) {
        return edgePoint(s, formed, slim, 1.0);
    }

    /** {@link #edgePoint(double, double, boolean)} with {@code reach} of it out. */
    public static double[] edgePoint(double s, double formed, boolean slim, double reach) {
        Frame f = new Frame(formed, 0.0, slim, reach);
        double e = f.core(s, 0.0);
        return new double[] {f.ax + e, f.y(s), 0.0};
    }

    // ------------------------------------------------------------------ the cone

    /** The stations down the cone: the dome, then down to the point, closer together toward it. */
    static double[] stations() {
        double[] s = new double[STATIONS + 4];
        for (int i = 0; i < 4; i++) {
            s[i] = -DOME * (1.0 - i / 4.0) * 0.999;
        }
        for (int i = 0; i < STATIONS; i++) {
            double t = i / (double)(STATIONS - 1);
            s[4 + i] = 0.55 * t + 0.45 * (1.0 - Math.pow(1.0 - t, 1.7));
        }
        return s;
    }

    /** The angles round the cone, from the edge (0) round the back and to the edge again (2 pi), closer together at the edge. */
    static double[] around() {
        double[] a = new double[AROUND + 1];
        for (int j = 0; j <= AROUND; j++) {
            double t = 2.0 * j / AROUND - 1.0;
            double m = Math.abs(t);
            a[j] = Math.PI + Math.PI * Math.signum(t) * (0.55 * m + 0.45 * (1.0 - (1.0 - m) * (1.0 - m)));
        }
        return a;
    }

    private static void core(Frame f, Sink sink) {
        double[] ss = stations();
        double[] th = around();
        double[][][] p = new double[ss.length][th.length][];
        int[][] c = new int[ss.length][th.length];
        for (int i = 0; i < ss.length; i++) {
            double s = ss[i];
            double y = f.y(s);
            double shine = f.shine(s);
            for (int j = 0; j < th.length; j++) {
                double rho = f.core(s, th[j]);
                double x = f.ax + rho * Math.cos(th[j]);
                double z = rho * Math.sin(th[j]);
                p[i][j] = new double[] {x, y, z};
                // The edge line bright, the flanks catching the glint, the round mottled.
                int fromEdge = Math.min(j, th.length - 1 - j);
                double flank = Math.abs(wrapAngle(th[j]));
                int colour = steel(x, y, z, 0.75 + 0.25 * Math.cos(th[j]));
                if (fromEdge == 0) {
                    colour = mix(STEEL_LIGHT, EDGE, 0.55 + 0.45 * shine);
                } else if (fromEdge == 1) {
                    colour = mix(mix(colour, STEEL_LIGHT, 0.75), EDGE, 0.9 * shine);
                } else if (flank < 1.2) {
                    colour = mix(colour, CLOUD, 0.5 * shine * (1.0 - flank / 1.2));
                }
                c[i][j] = argb(f.bloodied(colour, x, y, z, s, Math.max(0.0, Math.cos(th[j]))));
            }
        }
        double ax = f.ax;
        grid(sink, p, c, false, (i, j) -> new double[] {p[i][j][0] - ax, 0.0, p[i][j][2]});
    }

    // ------------------------------------------------------------------ the wing plates

    /**
     * Wing plate {@code k}: a long feather laid over the cone's back and sides from the shoulder, narrowing to a point near its
     * end; there it peels up off the plate beneath into a barb swept toward the tip. It sweeps round the arm from behind as it
     * closes, hugging the arm until the cone swells out under it, and slides down to its length as the blade drives out.
     */
    private static void plate(Frame f, int k, Sink sink) {
        double wrap = f.wrap[k];
        if (wrap <= 0.01) {
            return;
        }
        double start = -DOME * 0.55 - 0.004 * k;
        double end = start + (PLATES[k][0] - start) * (0.08 + 0.92 * f.slide[k]);
        double barbHeight = PLATES[k][1] * f.barb[k] * (1.0 + 0.06 * Math.sin(f.time * 3.1 + k * 1.7));
        double span = Math.PI * PLATES[k][2] * (0.12 + 0.88 * wrap);
        // From behind the arm (+z, a quarter turn) round to the back of the cone, each a touch off the last.
        double centre = Math.PI * 0.5 + (Math.PI * 0.5 + 0.06 * (k - 2.5)) * wrap;
        double over = PLATE * (k + 1) * (0.55 + 0.45 * wrap);
        double[][][] outer = new double[ALONG + 1][ACROSS + 1][];
        double[][][] under = new double[ALONG + 1][ACROSS + 1][];
        int[][] co = new int[ALONG + 1][ACROSS + 1];
        int[][] cu = new int[ALONG + 1][ACROSS + 1];
        for (int i = 0; i <= ALONG; i++) {
            double u = i / (double)ALONG;
            double s = start + (end - start) * u;
            double y = f.y(s);
            // A feather's point: narrowing down its whole length, sharply at the end.
            double narrow = Math.max(0.0, 1.0 - Math.pow(u, 1.6)) * (1.0 - 0.25 * u * u);
            // Its point peels up into a thorn: the plate's spine rising off the plate beneath toward the tip, over about the gap
            // to the next barb, so the row along the back is saw-toothed.
            double lift = barbHeight * Math.pow(smoother(1.0 - (end - s) / (RAMP * (0.15 + 0.85 * f.slide[k]))), 1.2);
            double thick = PLATE * (1.0 - Math.pow(u, 3.0)) + 0.03;
            double breathe = 0.07 * Math.sin(f.time * 2.2 - s * 7.0 + k);
            double shine = f.shine(s);
            for (int j = 0; j <= ACROSS; j++) {
                double v = 2.0 * j / ACROSS - 1.0;
                double theta = centre + v * span * narrow;
                double rest = Math.max(f.core(s, theta), f.arm(y, theta) + 0.18);
                double rho = rest + over + breathe + lift * (0.2 + 0.8 * Math.exp(-v * v / 0.18));
                double cos = Math.cos(theta);
                double sin = Math.sin(theta);
                outer[i][j] = new double[] {f.ax + rho * cos, y, rho * sin};
                double low = rho - thick;
                under[i][j] = new double[] {f.ax + low * cos, y, low * sin};
                // A streak of light along each feather, its sides inked dark; the barb's point catches the glint.
                double streak = Math.exp(-Math.pow((v + 0.3) / 0.28, 2.0));
                int base = steel(outer[i][j][0], y, outer[i][j][2] + k * 2.3, 0.55 + 0.35 * streak);
                base = mix(base, CLOUD, 0.35 * streak * (0.6 + 0.4 * u));
                base = mix(base, INK, 0.75 * Math.pow(Math.abs(v), 6.0));
                co[i][j] = argb(f.bloodied(mix(base, EDGE, 0.6 * shine * (0.4 + 0.6 * u)), outer[i][j][0], y, outer[i][j][2], s, 0.25));
                cu[i][j] = argb(mix(STEEL_DARK, INK, 0.4));
            }
        }
        double ax = f.ax;
        grid(sink, outer, co, false, (i, j) -> new double[] {outer[i][j][0] - ax, 0.0, outer[i][j][2]});
        // Where it peels up, its underside shows.
        int from = (int)(ALONG * 0.4);
        double[][][] underTail = new double[ALONG + 1 - from][][];
        int[][] cuTail = new int[ALONG + 1 - from][];
        for (int i = from; i <= ALONG; i++) {
            underTail[i - from] = under[i];
            cuTail[i - from] = cu[i];
        }
        grid(sink, underTail, cuTail, true, (i, j) -> new double[] {ax - underTail[i][j][0], 0.0, -underTail[i][j][2]});
        // Its rims: down each side and across its top, dark ink.
        int ink = argb(INK);
        int rim = argb(mix(INK, STEEL_DARK, 0.6));
        for (int side = 0; side <= ACROSS; side += ACROSS) {
            int inward = side == 0 ? 1 : ACROSS - 1;
            for (int i = 0; i < ALONG; i++) {
                double[] tangent = sub(outer[i][side], outer[i][inward]);
                quad(sink, outer[i][side], outer[i + 1][side], under[i + 1][side], under[i][side], new int[] {rim, rim, ink, ink}, tangent);
            }
        }
        for (int j = 0; j < ACROSS; j++) {
            quad(sink, outer[0][j], outer[0][j + 1], under[0][j + 1], under[0][j], new int[] {rim, rim, ink, ink}, new double[] {0.0, -1.0, 0.0});
        }
    }

    // ------------------------------------------------------------------ the pauldron

    /**
     * The pauldron: three plates layered over the shoulder like the wings' own feathers, each a squat dome over the cone's top
     * and down its outside, the lower ones larger and over the ones above; their back edges run out into points swept back and
     * up. They fan in from behind the shoulder.
     */
    private static void pauldron(Frame f, Sink sink) {
        double grow = pauldron(f.formed);
        if (grow <= 0.01) {
            return;
        }
        double[] pivot = {f.ax, -1.0, 4.5};
        double swing = (1.0 - grow) * 1.6;
        double scale = 0.3 + 0.7 * grow;
        int na = 12;
        int nb = 10;
        for (int layer = 0; layer < 3; layer++) {
            double sway = 0.05 * Math.sin(f.time * 1.6 + layer * 1.3);
            double[][][] outer = new double[na + 1][nb + 1][];
            double[][][] inner = new double[na + 1][nb + 1][];
            int[][] c = new int[na + 1][nb + 1];
            for (int i = 0; i <= na; i++) {
                for (int j = 0; j <= nb; j++) {
                    double[] p = shell(f.ax, layer, i / (double)na, j / (double)nb, sway, 0.0);
                    outer[i][j] = fanned(p, pivot, swing, scale);
                    inner[i][j] = fanned(shell(f.ax, layer, i / (double)na, j / (double)nb, sway, -0.4), pivot, swing, scale);
                    double top = 1.0 - i / (double)na;
                    int base = steel(p[0] * 1.3, p[2] * 1.7 + layer * 3.0, p[1], 0.5 + 0.45 * top);
                    double rimmed = Math.pow(Math.max(Math.abs(2.0 * j / nb - 1.0), i / (double)na), 8.0);
                    c[i][j] = argb(mix(mix(base, CLOUD, 0.25 * top), INK, 0.7 * rimmed));
                }
            }
            double[] centre = fanned(new double[] {f.ax, -0.6 + 1.75 * layer, 0.0}, pivot, swing, scale);
            grid(sink, outer, c, false, (i, j) -> sub(outer[i][j], centre));
            // Its rims, turned under: the plate's thickness.
            int rim = argb(mix(INK, STEEL_DARK, 0.5));
            int[] rims = {rim, rim, rim, rim};
            for (int j = 0; j < nb; j++) {
                quad(sink, outer[na][j], outer[na][j + 1], inner[na][j + 1], inner[na][j], rims, sub(outer[na][j], centre));
            }
            for (int i = 0; i < na; i++) {
                quad(sink, outer[i][nb], outer[i + 1][nb], inner[i + 1][nb], inner[i][nb], rims, new double[] {0.0, 0.0, 1.0});
                quad(sink, outer[i][0], outer[i + 1][0], inner[i + 1][0], inner[i][0], rims, new double[] {0.0, 0.0, -1.0});
            }
        }
    }

    /**
     * A point on pauldron plate {@code layer} (0 the top plate .. 2 the lowest), {@code depth} in from its outer face:
     * {@code u} from over the top (toward the neck) down the outside, {@code w} from its front edge to its back edge, which runs
     * out into points swept back and up. A squat dome round a centre inside the top of the cone.
     */
    static double[] shell(double ax, int layer, double u, double w, double sway, double depth) {
        double a0 = -0.42 + 0.24 * layer;
        double a1 = 1.75;
        double a = a0 + (a1 - a0) * u;
        double reach = 1.05 + 0.05 * layer;
        double feather = Math.pow(Math.abs(Math.sin(u * Math.PI * 2.5 + layer * 0.9)), 4.0) * (0.45 + 0.08 * layer) * Math.pow(w, 3.0);
        double b = -reach + 2.0 * reach * w + feather;
        double r = BASE_RADIUS + 2.65 + 0.42 * layer + depth;
        double cy = -0.6 + 1.75 * layer;
        double cb = Math.cos(b);
        double lift = feather * (2.4 + sway * 10.0);
        return new double[] {ax - Math.sin(a) * cb * r, cy - Math.cos(a) * cb * r * 0.64 - lift, Math.sin(b) * r};
    }

    /** {@code p} turned up and back about {@code pivot} by {@code swing} radians and drawn in to {@code scale}: still behind his shoulder. */
    static double[] fanned(double[] p, double[] pivot, double swing, double scale) {
        double y = (p[1] - pivot[1]) * scale;
        double z = (p[2] - pivot[2]) * scale;
        double x = (p[0] - pivot[0]) * scale;
        double c = Math.cos(swing);
        double s = Math.sin(swing);
        return new double[] {pivot[0] + x, pivot[1] + y * c - z * s, pivot[2] + y * s + z * c};
    }

    // ------------------------------------------------------------------ emitting

    /** Gives the way a grid point's normal should face (any vector on the outward side). */
    interface Outward {
        double[] at(int i, int j);
    }

    /**
     * A grid of points as quads, each corner with its own smooth normal (from its neighbours along and across the grid; one-sided
     * at the grid's borders, so a seam such as the edge stays sharp), turned to face {@code outward}.
     */
    static void grid(Sink sink, double[][][] p, int[][] c, boolean flip, Outward outward) {
        int ni = p.length;
        int nj = p[0].length;
        double[][][] n = new double[ni][nj][];
        for (int i = 0; i < ni; i++) {
            for (int j = 0; j < nj; j++) {
                double[] along = sub(p[Math.min(ni - 1, i + 1)][j], p[Math.max(0, i - 1)][j]);
                double[] across = sub(p[i][Math.min(nj - 1, j + 1)], p[i][Math.max(0, j - 1)]);
                double[] v = cross(along, across);
                double l = Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]);
                double[] out = outward.at(i, j);
                if (l < 1.0E-9) {
                    double lo = Math.sqrt(out[0] * out[0] + out[1] * out[1] + out[2] * out[2]);
                    n[i][j] = lo < 1.0E-9 ? new double[] {0.0, 1.0, 0.0} : new double[] {out[0] / lo, out[1] / lo, out[2] / lo};
                    continue;
                }
                double sign = v[0] * out[0] + v[1] * out[1] + v[2] * out[2] < 0.0 ? -1.0 : 1.0;
                n[i][j] = new double[] {sign * v[0] / l, sign * v[1] / l, sign * v[2] / l};
            }
        }
        for (int i = 0; i + 1 < ni; i++) {
            for (int j = 0; j + 1 < nj; j++) {
                int[][] order = flip ? new int[][] {{i, j}, {i, j + 1}, {i + 1, j + 1}, {i + 1, j}}
                    : new int[][] {{i, j}, {i + 1, j}, {i + 1, j + 1}, {i, j + 1}};
                for (int[] o : order) {
                    double[] q = p[o[0]][o[1]];
                    double[] m = n[o[0]][o[1]];
                    sink.vertex(q[0], q[1], q[2], c[o[0]][o[1]], m[0], m[1], m[2]);
                }
            }
        }
    }

    /** One flat quad, its normal turned toward {@code outward}. */
    static void quad(Sink sink, double[] a, double[] b, double[] c, double[] d, int[] argb, double[] outward) {
        double[] v = cross(sub(c, a), sub(d, b));
        double l = Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]);
        if (l < 1.0E-9) {
            v = outward;
            l = Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]);
            if (l < 1.0E-9) {
                v = new double[] {0.0, 1.0, 0.0};
                l = 1.0;
            }
        } else if (v[0] * outward[0] + v[1] * outward[1] + v[2] * outward[2] < 0.0) {
            v = new double[] {-v[0], -v[1], -v[2]};
        }
        double[][] q = {a, b, c, d};
        for (int k = 0; k < 4; k++) {
            sink.vertex(q[k][0], q[k][1], q[k][2], argb[k], v[0] / l, v[1] / l, v[2] / l);
        }
    }

    static double[] sub(double[] a, double[] b) {
        return new double[] {a[0] - b[0], a[1] - b[1], a[2] - b[2]};
    }

    static double[] cross(double[] a, double[] b) {
        return new double[] {a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]};
    }

    // ------------------------------------------------------------------ easing and colour

    /** Mottled steel at (x, y, z): dark steel clouded with white, specked darker; {@code light} 0..1 how bright its plate is. */
    static int steel(double x, double y, double z, double light) {
        double cloud = 0.5 + 0.5 * Math.sin(x * 0.9 + y * 0.37 + z * 0.7) * Math.sin(y * 0.21 - x * 0.6 + z * 0.45 + 1.3)
            + 0.25 * Math.sin(y * 0.83 + x * 1.7 - z * 1.1);
        double blotch = smoother((cloud - 0.55) / 0.35);
        double speck = smoother((0.18 - cloud) / 0.2);
        int base = mix(STEEL_DARK, STEEL, 0.3 + 0.7 * clamp01(light));
        base = mix(base, CLOUD, 0.62 * blotch);
        return mix(base, STEEL_DARK, 0.55 * speck);
    }

    static double clamp01(double t) {
        return t < 0.0 ? 0.0 : t > 1.0 ? 1.0 : t;
    }

    /** Smootherstep: no jerk at either end. */
    static double smoother(double t) {
        double x = clamp01(t);
        return x * x * x * (x * (x * 6.0 - 15.0) + 10.0);
    }

    /**
     * Eased out a touch past 1 and back (a blade driven home): smootherstep up to {@link #OVERSHOOT} by 0.75, then eased back to
     * 1; no jerk anywhere, exactly 0 and 1 at the ends.
     */
    static double backOut(double t) {
        if (t <= 0.0) {
            return 0.0;
        }
        if (t >= 1.0) {
            return 1.0;
        }
        if (t < 0.75) {
            return OVERSHOOT * smoother(t / 0.75);
        }
        return OVERSHOOT - (OVERSHOOT - 1.0) * smoother((t - 0.75) / 0.25);
    }

    /** How far past home the blade and its barbs are driven before they settle. */
    static final double OVERSHOOT = 1.05;

    static int argb(int rgb) {
        return 0xFF000000 | rgb;
    }

    static int mix(int a, int b, double t) {
        double s = clamp01(t);
        int r = (int)Math.round((a >> 16 & 0xFF) + ((b >> 16 & 0xFF) - (a >> 16 & 0xFF)) * s);
        int g = (int)Math.round((a >> 8 & 0xFF) + ((b >> 8 & 0xFF) - (a >> 8 & 0xFF)) * s);
        int bl = (int)Math.round((a & 0xFF) + ((b & 0xFF) - (a & 0xFF)) * s);
        return r << 16 | g << 8 | bl;
    }
}
