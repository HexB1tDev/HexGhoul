package com.hex.kagune.control.client.animation;

import com.hex.kagune.control.pale.ReviveRules;
import com.hex.kagune.control.wound.Wound;
import java.util.Random;

/**
 * 6.2.0, You Thought You'd Get Rid of Me That Easily? (pure, tested): what is left of Pale when the blow that would have killed her
 * blows her apart instead, and how it comes back together ({@link ReviveRules} has the timings).
 *
 * <p>Her body comes apart into {@link Piece}s cut out of her own skin: the head torn off at the neck, the torso torn in two at the
 * waist, each arm and each leg torn off and torn again at the elbow or knee. Every torn end is raw (torn flesh, the bone in it).
 * They are thrown out from her middle, tumbling, come down, bounce once and lie where they fall ({@link #scatter}, the same on
 * every client from the same seed); her guts spill out of both halves of her torso in ropes that unfurl and lie on the ground.
 * After {@link ReviveRules#SCATTER} ticks flesh lines run out of her chest to every piece; then everything is hauled back up to
 * where it belongs in her standing body, the guts reeled in after it ({@link #place}), and she is whole.
 *
 * <p>Rig space throughout (blocks; x to her right, y up from her feet, z the way her body faced, the ground {@code ground} below
 * her feet); a piece's own frame is rig space as it sits in her standing body, about its middle.
 */
public final class Remains {
    /** One piece of her: a slice ({@code top} to {@code bottom}, part pixels down its length) of one part of the player model. */
    public enum Piece {
        HEAD(Wound.Part.HEAD, "head", -8.0, 0.0, false, true, 0.0, 0.0, 32.0, 0.0),
        CHEST(Wound.Part.BODY, "torso", 0.0, 6.5, true, true, 16.0, 16.0, 16.0, 32.0),
        BELLY(Wound.Part.BODY, "torso", 6.5, 12.0, true, false, 16.0, 16.0, 16.0, 32.0),
        RIGHT_UPPER_ARM(Wound.Part.RIGHT_ARM, "rightArm", -2.0, 4.0, true, true, 40.0, 16.0, 40.0, 32.0),
        RIGHT_FOREARM(Wound.Part.RIGHT_ARM, "rightArm", 4.0, 10.0, true, false, 40.0, 16.0, 40.0, 32.0),
        LEFT_UPPER_ARM(Wound.Part.LEFT_ARM, "leftArm", -2.0, 4.0, true, true, 32.0, 48.0, 48.0, 48.0),
        LEFT_FOREARM(Wound.Part.LEFT_ARM, "leftArm", 4.0, 10.0, true, false, 32.0, 48.0, 48.0, 48.0),
        RIGHT_THIGH(Wound.Part.RIGHT_LEG, "rightLeg", 0.0, 6.0, true, true, 0.0, 16.0, 0.0, 32.0),
        RIGHT_SHIN(Wound.Part.RIGHT_LEG, "rightLeg", 6.0, 12.0, true, false, 0.0, 16.0, 0.0, 32.0),
        LEFT_THIGH(Wound.Part.LEFT_LEG, "leftLeg", 0.0, 6.0, true, true, 16.0, 48.0, 0.0, 48.0),
        LEFT_SHIN(Wound.Part.LEFT_LEG, "leftLeg", 6.0, 12.0, true, false, 16.0, 48.0, 0.0, 48.0);

        public final Wound.Part part;
        final String model;
        final double top;
        final double bottom;
        final boolean topTorn;
        final boolean bottomTorn;
        /** Where the part and its second layer are on a 64 x 64 skin (texture pixels). */
        final double u;
        final double v;
        final double layerU;
        final double layerV;

        Piece(Wound.Part part, String model, double top, double bottom, boolean topTorn, boolean bottomTorn, double u, double v, double layerU,
              double layerV) {
            this.part = part;
            this.model = model;
            this.top = top;
            this.bottom = bottom;
            this.topTorn = topTorn;
            this.bottomTorn = bottomTorn;
            this.u = u;
            this.v = v;
            this.layerU = layerU;
            this.layerV = layerV;
        }

        /** Its box (part pixels): {x0, y0, z0, x1, y1, z1}. */
        double[] box(boolean slim) {
            double[] whole = this.part.box(slim);
            return new double[] {whole[0], this.top, whole[2], whole[3], this.bottom, whole[5]};
        }

        /** True for the two halves of her torso (heavy, slow to tumble; her guts spill out of them). */
        boolean torso() {
            return this == CHEST || this == BELLY;
        }

        /**
         * 6.2.0: when (fractions of the pull) the lines start hauling it home and when it is in: what is left of her chest stands
         * first, then her belly, her legs, her arms, and her head is the last thing pulled back on.
         */
        public double[] haul() {
            return switch (this) {
                case CHEST -> new double[] {0.0, 0.56};
                case BELLY -> new double[] {0.06, 0.63};
                case RIGHT_THIGH -> new double[] {0.14, 0.7};
                case LEFT_THIGH -> new double[] {0.17, 0.72};
                case RIGHT_SHIN -> new double[] {0.21, 0.78};
                case LEFT_SHIN -> new double[] {0.24, 0.8};
                case RIGHT_UPPER_ARM -> new double[] {0.28, 0.84};
                case LEFT_UPPER_ARM -> new double[] {0.31, 0.86};
                case RIGHT_FOREARM -> new double[] {0.35, 0.9};
                case LEFT_FOREARM -> new double[] {0.38, 0.93};
                case HEAD -> new double[] {0.42, 1.0};
            };
        }
    }

    /** How hard it is all thrown (blocks a tick, out and up), how fast the pieces tumble, gravity and air. */
    static final double OUT = 0.11;
    static final double OUT_MORE = 0.11;
    static final double UP = 0.22;
    static final double UP_MORE = 0.16;
    static final double SPIN = 0.3;
    static final double SPIN_MORE = 0.45;
    static final double GRAVITY = 0.06;
    static final double AIR = 0.985;
    /** A piece that comes down faster than this bounces once; ticks it takes to roll over onto its flattest side. */
    static final double BOUNCES = 0.12;
    static final int SETTLE = 5;
    /** How much a broad side is preferred to lie on over the side most nearly down. */
    static final double FLAT_BROAD = 3.5;
    /** Her middle, where it bursts from. */
    static final double[] CORE = {0.0, 1.05, 0.0};
    /** How high (blocks) the pull lifts a piece off the ground on its way back. */
    static final double ARC = 0.45;

    /** Her guts: how many ropes, of how many links, each how long (blocks), how thick. */
    public static final int ROPES = 3;
    public static final int LINKS = 12;
    static final double LINK = 0.11;
    public static final double GUT_RADIUS = 0.05;

    private Remains() {
    }

    // ------------------------------------------------------------------ where each piece belongs

    /** Her standing body, the pose every piece is cut from and goes back to. */
    static RigPose standing() {
        return new RigPose();
    }

    /** 6.2.0: the pose her body is drawn in the moment she is whole (the one the pieces went back to). */
    public static RigPose wholePose() {
        return standing();
    }

    /** A point of {@code piece}'s part (part pixels) where it is in her standing body (rig space). */
    static double[] home(Piece piece, double[] local) {
        RigPose body = standing();
        return BodyFrame.world(body, BodyFrame.model(body, piece.model, local));
    }

    /** The middle of {@code piece} where it belongs in her standing body (rig space). */
    public static double[] centre(Piece piece, boolean slim) {
        double[] b = piece.box(slim);
        return home(piece, new double[] {(b[0] + b[3]) / 2.0, (b[1] + b[4]) / 2.0, (b[2] + b[5]) / 2.0});
    }

    /** A point of {@code piece}'s part (part pixels) in the piece's own frame (blocks about its middle). */
    static double[] own(Piece piece, boolean slim, double[] local) {
        return sub(home(piece, local), centre(piece, slim));
    }

    /** Its eight corners in its own frame. */
    public static double[][] corners(Piece piece, boolean slim) {
        double[] b = piece.box(slim);
        double[][] out = new double[8][];
        for (int i = 0; i < 8; i++) {
            out[i] = own(piece, slim, new double[] {(i & 1) == 0 ? b[0] : b[3], (i & 2) == 0 ? b[1] : b[4], (i & 4) == 0 ? b[2] : b[5]});
        }
        return out;
    }

    /**
     * 6.2.0: where {@code piece} is torn open, in its own frame: for each torn end, the middle of it and the way out of it (unit):
     * where the blood pumps out of.
     */
    public static double[][][] openings(Piece piece, boolean slim) {
        double[] b = piece.box(slim);
        double cx = (b[0] + b[3]) / 2.0;
        double cz = (b[2] + b[5]) / 2.0;
        java.util.List<double[][]> ends = new java.util.ArrayList<>();
        if (piece.topTorn) {
            double[] at = own(piece, slim, new double[] {cx, b[1], cz});
            ends.add(new double[][] {at, normalize(sub(at, own(piece, slim, new double[] {cx, b[1] + 1.0, cz})))});
        }
        if (piece.bottomTorn) {
            double[] at = own(piece, slim, new double[] {cx, b[4], cz});
            ends.add(new double[][] {at, normalize(sub(at, own(piece, slim, new double[] {cx, b[4] - 1.0, cz})))});
        }
        return ends.toArray(new double[0][][]);
    }

    /** Where the guts of rope {@code rope} hang out of (a piece's own frame): the torn waist of the belly (two), of the chest (one). */
    static Piece gutsFrom(int rope) {
        return rope < 2 ? Piece.BELLY : Piece.CHEST;
    }

    static double[] gutsAt(int rope, boolean slim) {
        Piece piece = gutsFrom(rope);
        double[] b = piece.box(slim);
        double y = piece == Piece.BELLY ? b[1] : b[4];
        double x = rope == 0 ? -1.5 : rope == 1 ? 1.2 : 0.4;
        return own(piece, slim, new double[] {x, y, -0.6});
    }

    // ------------------------------------------------------------------ thrown apart

    /** One piece's way: its middle and its turn (a quaternion {w, x, y, z}) at each tick from the blow; the tick it first came down. */
    public record Track(double[][] at, double[][] turn, int landed, int bounced) {
    }

    /** Everything thrown apart: each piece's track ({@link Piece} order), and each rope of guts' links at each tick. */
    public record Scatter(Track[] pieces, double[][][][] guts, double ground, boolean slim) {
        public Track track(Piece piece) {
            return this.pieces[piece.ordinal()];
        }
    }

    /**
     * Her body blown apart ({@code seed} chooses how), the ground {@code ground} blocks below her feet (0 or less), her arms
     * {@code slim} or not: every piece's track and her guts, tick by tick, until the lines come for them.
     */
    public static Scatter scatter(long seed, double ground, boolean slim) {
        Random random = new Random(seed);
        Piece[] all = Piece.values();
        Track[] tracks = new Track[all.length];
        for (Piece piece : all) {
            tracks[piece.ordinal()] = fly(piece, slim, ground, random);
        }
        double[][][][] guts = new double[ROPES][][][];
        for (int rope = 0; rope < ROPES; rope++) {
            guts[rope] = spill(rope, tracks[gutsFrom(rope).ordinal()], slim, ground, random);
        }
        return new Scatter(tracks, guts, ground, slim);
    }

    static Track fly(Piece piece, boolean slim, double ground, Random random) {
        double[] centre = centre(piece, slim);
        double[][] corners = corners(piece, slim);
        double[] out = sub(centre, CORE);
        out[1] *= 0.4;
        out = add(normalize(out), new double[] {(random.nextDouble() - 0.5) * 0.7, 0.0, (random.nextDouble() - 0.5) * 0.7});
        out[1] = 0.0;
        out = normalize(out);
        double heft = piece.torso() ? 0.55 : piece == Piece.HEAD ? 1.1 : 1.0;
        double speed = (OUT + OUT_MORE * random.nextDouble()) * heft;
        double[] v = {out[0] * speed, (UP + UP_MORE * random.nextDouble()) * (piece == Piece.HEAD ? 1.4 : piece.torso() ? 0.7 : 1.0), out[2] * speed};
        double[] axis = normalize(new double[] {random.nextGaussian(), random.nextGaussian(), random.nextGaussian()});
        double rate = (SPIN + SPIN_MORE * random.nextDouble()) * (piece.torso() ? 0.5 : 1.0) * (random.nextBoolean() ? 1.0 : -1.0);
        double[] at = centre.clone();
        double[] turn = {1.0, 0.0, 0.0, 0.0};
        double[][] ats = new double[ReviveRules.SCATTER + 1][];
        double[][] turns = new double[ReviveRules.SCATTER + 1][];
        int landed = -1;
        int bounced = -1;
        double[] flat = null;
        double[] lying = null;
        for (int tick = 0; tick <= ReviveRules.SCATTER; tick++) {
            ats[tick] = at.clone();
            turns[tick] = turn.clone();
            if (landed >= 0) {
                // Lying where it fell, rolled over onto its flattest side.
                double u = Math.min(1.0, (tick - landed + 1) / (double)SETTLE);
                turn = nlerp(lying, flat, u * u * (3.0 - 2.0 * u));
                at[1] += ground - lowest(at, turn, corners);
                continue;
            }
            v[1] -= GRAVITY;
            v = mul(v, AIR);
            at = add(at, v);
            turn = multiply(axisAngle(axis, rate), turn);
            double under = ground - lowest(at, turn, corners);
            if (under > 0.0) {
                at[1] += under;
                if (bounced < 0 && v[1] < -BOUNCES) {
                    bounced = tick;
                    v = new double[] {v[0] * 0.45, -v[1] * 0.3, v[2] * 0.45};
                    rate *= 0.4;
                } else {
                    landed = tick;
                    lying = turn.clone();
                    flat = flat(turn, corners);
                    v = new double[] {0.0, 0.0, 0.0};
                }
            }
        }
        return new Track(ats, turns, landed < 0 ? ReviveRules.SCATTER : landed, bounced);
    }

    /** The lowest any corner of a piece turned {@code turn} with its middle {@code at} comes (rig y). */
    static double lowest(double[] at, double[] turn, double[][] corners) {
        double low = Double.MAX_VALUE;
        for (double[] c : corners) {
            low = Math.min(low, at[1] + rotate(turn, c)[1]);
        }
        return low;
    }

    /**
     * {@code turn} rolled over onto a side of its box, lying flat: the side most nearly down, a broad side rather than an end (a
     * limb topples over onto its length, it doesn't stand on its torn end).
     */
    static double[] flat(double[] turn, double[][] corners) {
        // Its box's own three axes (from its corners), each either way, and how broad the side square to each is.
        double[][] edges = {sub(corners[1], corners[0]), sub(corners[2], corners[0]), sub(corners[4], corners[0])};
        double[] sizes = {length(edges[0]), length(edges[1]), length(edges[2])};
        double broadest = Math.max(sizes[0] * sizes[1], Math.max(sizes[1] * sizes[2], sizes[0] * sizes[2]));
        double[] best = null;
        double bestScore = -10.0;
        for (int k = 0; k < 3; k++) {
            double area = sizes[(k + 1) % 3] * sizes[(k + 2) % 3] / broadest;
            for (double sign : new double[] {1.0, -1.0}) {
                double[] w = rotate(turn, mul(normalize(edges[k]), sign));
                double score = -w[1] + FLAT_BROAD * area;
                if (score > bestScore) {
                    bestScore = score;
                    best = w;
                }
            }
        }
        double[] down = {0.0, -1.0, 0.0};
        double[] axis = cross(best, down);
        double s = length(axis);
        if (s < 1.0E-9) {
            return turn.clone();
        }
        double angle = Math.atan2(s, dot(best, down));
        return multiply(axisAngle(mul(axis, 1.0 / s), angle), turn);
    }

    /** One rope of guts spilled out of its half of her torso, flung out with it, unfurling, lying on the ground. */
    static double[][][] spill(int rope, Track from, boolean slim, double ground, Random random) {
        double[] anchor = gutsAt(rope, slim);
        double[][][] ticks = new double[ReviveRules.SCATTER + 1][LINKS][];
        double[][] p = new double[LINKS][];
        double[][] was = new double[LINKS][];
        double[] start = add(from.at[0], rotate(from.turn[0], anchor));
        double[] fling = normalize(new double[] {random.nextGaussian(), 0.4 + random.nextDouble(), random.nextGaussian()});
        for (int i = 0; i < LINKS; i++) {
            p[i] = add(start, mul(fling, LINK * 0.3 * i));
            double kick = 0.12 + 0.12 * random.nextDouble();
            was[i] = sub(p[i], mul(fling, kick * i / (double)LINKS));
        }
        for (int tick = 0; tick <= ReviveRules.SCATTER; tick++) {
            double[] hold = add(from.at[tick], rotate(from.turn[tick], anchor));
            for (int i = 1; i < LINKS; i++) {
                double[] next = add(p[i], mul(sub(p[i], was[i]), 0.94));
                next[1] -= GRAVITY * 0.8;
                was[i] = p[i];
                p[i] = next;
            }
            p[0] = hold;
            for (int round = 0; round < 8; round++) {
                for (int i = 1; i < LINKS; i++) {
                    double[] d = sub(p[i], p[i - 1]);
                    double l = length(d);
                    if (l > 1.0E-9) {
                        double fix = (l - LINK) / l;
                        if (i == 1) {
                            p[i] = sub(p[i], mul(d, fix));
                        } else {
                            p[i] = sub(p[i], mul(d, fix * 0.5));
                            p[i - 1] = add(p[i - 1], mul(d, fix * 0.5));
                        }
                    }
                }
                for (int i = 1; i < LINKS; i++) {
                    if (p[i][1] < ground + GUT_RADIUS) {
                        p[i][1] = ground + GUT_RADIUS;
                        // Dragged on the ground, it soon stops.
                        was[i] = new double[] {p[i][0] + (was[i][0] - p[i][0]) * 0.4, p[i][1], p[i][2] + (was[i][2] - p[i][2]) * 0.4};
                    }
                }
            }
            for (int i = 0; i < LINKS; i++) {
                ticks[tick][i] = p[i].clone();
            }
        }
        return ticks;
    }

    // ------------------------------------------------------------------ where everything is

    /** A piece placed: its middle (rig space) and its turn (a quaternion {w, x, y, z}). */
    public record Placed(double[] at, double[] turn) {
    }

    /**
     * Where {@code piece} is {@code age} ticks after the blow: flying, lying where it fell; then hauled back up in an arc to where it
     * belongs in its turn ({@link Piece#haul}), faster and faster, turned back upright on the way ({@link ReviveRules#upright}) so it
     * slots straight in; home from then on, and every piece is home the moment she is whole.
     */
    public static Placed place(Scatter scatter, Piece piece, double age) {
        Track track = scatter.track(piece);
        double[] home = centre(piece, scatter.slim());
        double[] when = piece.haul();
        double e = ReviveRules.haul(age, when[0], when[1]);
        if (age >= ReviveRules.WHOLE || e >= 1.0) {
            return new Placed(home, new double[] {1.0, 0.0, 0.0, 0.0});
        }
        Placed lying = sample(track, Math.min(age, ReviveRules.SCATTER));
        if (e <= 0.0) {
            return lying;
        }
        double[] at = lerp(lying.at(), home, e);
        // Lifted off the ground on its way (what is left of her chest just stands up).
        at[1] += (piece == Piece.CHEST ? 0.0 : ARC) * Math.sin(Math.PI * e);
        return new Placed(at, nlerp(lying.turn(), new double[] {1.0, 0.0, 0.0, 0.0}, ReviveRules.upright(age, when[0], when[1])));
    }

    /** 6.2.0: whether {@code piece} is back home {@code age} ticks after the blow (its line reeled in with it). */
    public static boolean home(Piece piece, double age) {
        double[] when = piece.haul();
        return age >= ReviveRules.WHOLE || ReviveRules.haul(age, when[0], when[1]) >= 1.0;
    }

    static Placed sample(Track track, double age) {
        double t = Math.max(0.0, Math.min(ReviveRules.SCATTER, age));
        int a = (int)Math.floor(t);
        int b = Math.min(ReviveRules.SCATTER, a + 1);
        double f = t - a;
        return new Placed(lerp(track.at[a], track.at[b], f), nlerp(track.turn[a], track.turn[b], f));
    }

    /** A point of {@code piece}'s own frame where it is {@code age} ticks after the blow (rig space). */
    public static double[] point(Scatter scatter, Piece piece, double age, double[] own) {
        Placed placed = place(scatter, piece, age);
        return add(placed.at(), rotate(placed.turn(), own));
    }

    /**
     * Rope {@code rope} of her guts {@code age} ticks after the blow: its links (rig space), from where it hangs out of her torso;
     * reeled back in after the pull, all of it back inside her as she is whole (null then).
     */
    public static double[][] guts(Scatter scatter, int rope, double age) {
        if (age >= ReviveRules.WHOLE || home(gutsFrom(rope), age)) {
            return null;
        }
        double t = Math.max(0.0, Math.min(ReviveRules.SCATTER, age));
        int a = (int)Math.floor(t);
        int b = Math.min(ReviveRules.SCATTER, a + 1);
        double f = t - a;
        double[][] links = new double[LINKS][];
        for (int i = 0; i < LINKS; i++) {
            links[i] = lerp(scatter.guts()[rope][a][i], scatter.guts()[rope][b][i], f);
        }
        double[] when = gutsFrom(rope).haul();
        double e = Math.min(1.0, ReviveRules.haul(age, when[0], when[1]) * 1.25);
        if (e > 0.0) {
            // Reeled in after its half of her torso, in before it is home: every link drawn in to where it hangs out of her, the far
            // end last.
            double[] anchor = point(scatter, gutsFrom(rope), age, gutsAt(rope, scatter.slim()));
            double[] lay = links[0];
            for (int i = 0; i < LINKS; i++) {
                double in = Math.min(1.0, e * (1.0 + 0.6 * (1.0 - i / (double)LINKS)));
                links[i] = lerp(add(anchor, sub(links[i], lay)), anchor, in);
            }
        }
        return links;
    }

    /**
     * The flesh line out to {@code piece} {@code age} ticks after the blow: from the middle of what is left of her chest to the
     * piece's middle, {@code reach} of the way out (0: none yet), {@code sag} blocks bowed down at its middle (slack as it runs out,
     * taut as it hauls). Null for the chest itself, before the lines, once the piece is home and once she is whole.
     */
    public static Line line(Scatter scatter, Piece piece, double age) {
        if (piece == Piece.CHEST || age < ReviveRules.SCATTER || age >= ReviveRules.WHOLE || home(piece, age)) {
            return null; // none to the chest, none yet, and none once the piece is home (reeled in with it)
        }
        int index = piece.ordinal() - (piece.ordinal() > Piece.CHEST.ordinal() ? 1 : 0);
        double reach = ReviveRules.line(age, index, Piece.values().length - 1);
        if (reach <= 0.0) {
            return null;
        }
        double[] from = place(scatter, Piece.CHEST, age).at();
        double[] to = place(scatter, piece, age).at();
        double[] when = piece.haul();
        double taut = Math.max(reach >= 1.0 ? 0.6 : 0.0, ReviveRules.pull(age) > 0.0 || ReviveRules.haul(age, when[0], when[1]) > 0.0 ? 1.0 : 0.0);
        double sag = (1.0 - taut) * Math.min(0.5, 0.15 * length(sub(to, from)));
        return new Line(from, to, reach, sag);
    }

    /** A flesh line: from, to, how far out it has run (0..1), how much it sags. */
    public record Line(double[] from, double[] to, double reach, double sag) {
    }

    // ------------------------------------------------------------------ the pieces as quads

    /** Receives a piece's faces in its own frame: skin quads (texture 0..1, the second layer or not) and raw flesh polygons. */
    public interface Sink {
        void skin(double[] a, double[] b, double[] c, double[] d, double[] normal, float[] uv, boolean layer);

        void flesh(double[][] polygon, double[] normal, int[] argb);
    }

    // The raw flesh of a torn end (Wound's palette).
    static final int SKIN_EDGE = 0xFFC9897C;
    static final int FAT = 0xFFE4C47C;
    static final int MEAT = 0xFFB2222E;
    static final int MEAT_DEEP = 0xFF6A0E16;
    static final int MEAT_DARK = 0xFF44060C;
    static final int BONE = 0xFFEADFC6;
    static final int MARROW = 0xFF8E1822;
    public static final int GUT = 0xFFD27884;
    public static final int GUT_DARK = 0xFF8E3A4C;
    static final int THROAT = 0xFF2A0608;

    /** {@code piece} as quads: its skin and its skin's second layer, and every torn end raw (with the bone in it). */
    public static void mesh(Piece piece, boolean slim, int seed, Sink sink) {
        double[] b = piece.box(slim);
        double[] whole = piece.part.box(slim);
        box(piece, slim, b, whole[1], 0.0, piece.u, piece.v, false, sink);
        box(piece, slim, b, whole[1], piece.part.outer(), piece.layerU, piece.layerV, true, sink);
        Random random = new Random(seed * 31L + piece.ordinal());
        if (piece.topTorn) {
            torn(piece, slim, b, true, random, sink);
        }
        if (piece.bottomTorn) {
            torn(piece, slim, b, false, random, sink);
        }
    }

    /**
     * The box {@code b} (part pixels) grown {@code grow} each way, its skin at ({@code u}, {@code v}) as vanilla lays a part's
     * faces out, the part's own top at {@code partTop}: its four sides cut to its slice, and each end its own skin where it is not
     * torn (a torn end is raw instead, {@link #torn}; the neck's stump on the chest keeps its shoulders).
     */
    static void box(Piece piece, boolean slim, double[] b, double partTop, double grow, double u, double v, boolean layer, Sink sink) {
        double x0 = b[0] - grow;
        double x1 = b[3] + grow;
        double y0 = b[1] - (piece.topTorn ? 0.0 : grow);
        double y1 = b[4] + (piece.bottomTorn ? 0.0 : grow);
        double z0 = b[2] - grow;
        double z1 = b[5] + grow;
        double w = b[3] - b[0];
        double d = b[5] - b[2];
        double va = v + d + (b[1] - partTop);
        double vb = v + d + (b[4] - partTop);
        face(piece, slim, sink, layer, new double[][] {{x0, y0, z0}, {x1, y0, z0}, {x1, y1, z0}, {x0, y1, z0}},
            new double[] {u + d, va, u + d + w, va, u + d + w, vb, u + d, vb});
        face(piece, slim, sink, layer, new double[][] {{x1, y0, z0}, {x1, y0, z1}, {x1, y1, z1}, {x1, y1, z0}},
            new double[] {u + d + w, va, u + 2 * d + w, va, u + 2 * d + w, vb, u + d + w, vb});
        face(piece, slim, sink, layer, new double[][] {{x1, y0, z1}, {x0, y0, z1}, {x0, y1, z1}, {x1, y1, z1}},
            new double[] {u + 2 * d + w, va, u + 2 * d + 2 * w, va, u + 2 * d + 2 * w, vb, u + 2 * d + w, vb});
        face(piece, slim, sink, layer, new double[][] {{x0, y0, z1}, {x0, y0, z0}, {x0, y1, z0}, {x0, y1, z1}},
            new double[] {u, va, u + d, va, u + d, vb, u, vb});
        boolean partTopEnd = b[1] == partTop;
        if (!piece.topTorn || piece == Piece.CHEST && !layer) {
            // The part's own top (the crown of the head, the shoulders): its skin, where this piece has it.
            if (partTopEnd) {
                face(piece, slim, sink, layer, new double[][] {{x0, y0, z1}, {x1, y0, z1}, {x1, y0, z0}, {x0, y0, z0}},
                    new double[] {u + d, v, u + d + w, v, u + d + w, v + d, u + d, v + d});
            }
        }
        if (!piece.bottomTorn) {
            face(piece, slim, sink, layer, new double[][] {{x0, y1, z0}, {x1, y1, z0}, {x1, y1, z1}, {x0, y1, z1}},
                new double[] {u + d + w, v, u + d + 2 * w, v, u + d + 2 * w, v + d, u + d + w, v + d});
        }
    }

    static void face(Piece piece, boolean slim, Sink sink, boolean layer, double[][] local, double[] uv) {
        double[][] p = new double[4][];
        for (int i = 0; i < 4; i++) {
            p[i] = own(piece, slim, local[i]);
        }
        float[] skin = new float[8];
        for (int i = 0; i < 8; i++) {
            skin[i] = (float)(uv[i] / 64.0);
        }
        sink.skin(p[0], p[1], p[2], p[3], normal(p), skin, layer);
    }

    /**
     * A torn end ({@code top} or the bottom of box {@code b}): ragged skin round a mass of torn meat bulging out of it, the bone
     * standing out of the middle of a limb (the spine at the back of the neck and the waist, the throat, her guts in front).
     */
    static void torn(Piece piece, boolean slim, double[] b, boolean top, Random random, Sink sink) {
        double y = top ? b[1] : b[4];
        double out = top ? -1.0 : 1.0;
        double cx = (b[0] + b[3]) / 2.0;
        double cz = (b[2] + b[5]) / 2.0;
        double hx = (b[3] - b[0]) / 2.0;
        double hz = (b[5] - b[2]) / 2.0;
        if (piece == Piece.CHEST && top) {
            // Only the neck is torn out of the chest's top: a stump between the shoulders.
            hx = 2.0;
            hz = 1.6;
        }
        int around = 16;
        double[][] rim = new double[around][];
        double[][] meat = new double[around][];
        for (int i = 0; i < around; i++) {
            double angle = i * Math.PI * 2.0 / around;
            double c = Math.cos(angle);
            double s = Math.sin(angle);
            // Round the box's edge (a rounded square), ragged.
            double k = 1.0 / Math.max(Math.abs(c), Math.abs(s));
            double rag = 0.88 + 0.12 * random.nextDouble();
            rim[i] = new double[] {cx + c * k * hx * rag, y + out * (0.05 + 0.35 * random.nextDouble()), cz + s * k * hz * rag};
            double in = 0.62 + 0.18 * random.nextDouble();
            meat[i] = new double[] {cx + c * k * hx * in, y + out * (0.5 + 0.6 * random.nextDouble()), cz + s * k * hz * in};
        }
        double[] middle = {cx, y + out * 1.0, cz};
        for (int i = 0; i < around; i++) {
            int j = (i + 1) % around;
            // The cut skin and fat, then the meat bulging out, darkest at its heart.
            gore(piece, slim, sink, new double[][] {rim[i], rim[j], meat[j], meat[i]}, new int[] {SKIN_EDGE, SKIN_EDGE, FAT, FAT}, out);
            gore(piece, slim, sink, new double[][] {meat[i], meat[j], middle, middle}, new int[] {MEAT, MEAT, MEAT_DEEP, MEAT_DEEP}, out);
        }
        boolean limb = !piece.torso() && piece != Piece.HEAD;
        if (limb) {
            bone(piece, slim, sink, new double[] {cx, y, cz}, out, 0.85, 1.7 + random.nextDouble() * 0.8);
        } else {
            // The spine at the back; the throat (at the neck) or the guts (at the waist) in front.
            double back = (b[5] - b[2]) * 0.28;
            bone(piece, slim, sink, new double[] {cx, y, cz + back}, out, 0.75, 1.1);
            double[] front = {cx, y + out * 1.05, cz - back};
            int colour = piece == Piece.HEAD || piece == Piece.CHEST && top ? THROAT : GUT;
            disc(piece, slim, sink, front, out, piece == Piece.HEAD || top ? 0.55 : 1.3, colour, colour == GUT ? GUT_DARK : MEAT_DARK);
        }
    }

    /** A bone standing {@code length} pixels out of a torn end at {@code at}, {@code radius} thick, its marrow showing. */
    static void bone(Piece piece, boolean slim, Sink sink, double[] at, double out, double radius, double length) {
        int sides = 7;
        double[][] base = new double[sides][];
        double[][] end = new double[sides][];
        for (int i = 0; i < sides; i++) {
            double angle = i * Math.PI * 2.0 / sides;
            double r = radius * (0.85 + 0.15 * Math.cos(angle * 3.0));
            base[i] = new double[] {at[0] + Math.cos(angle) * r, at[1], at[2] + Math.sin(angle) * r};
            // Snapped off jagged.
            end[i] = new double[] {at[0] + Math.cos(angle) * r * 0.9, at[1] + out * length * (0.8 + 0.2 * Math.sin(angle * 2.0 + 1.0)),
                at[2] + Math.sin(angle) * r * 0.9};
        }
        for (int i = 0; i < sides; i++) {
            int j = (i + 1) % sides;
            gore(piece, slim, sink, new double[][] {base[i], base[j], end[j], end[i]}, new int[] {BONE, BONE, BONE, BONE}, 0.0);
        }
        double[] tip = {at[0], at[1] + out * length * 0.85, at[2]};
        for (int i = 0; i < sides; i++) {
            int j = (i + 1) % sides;
            gore(piece, slim, sink, new double[][] {end[i], end[j], tip, tip}, new int[] {BONE, BONE, MARROW, MARROW}, out);
        }
    }

    /** A round patch {@code radius} pixels across at {@code at}, from {@code rim} to {@code heart}. */
    static void disc(Piece piece, boolean slim, Sink sink, double[] at, double out, double radius, int heart, int rim) {
        int sides = 8;
        for (int i = 0; i < sides; i++) {
            double a0 = i * Math.PI * 2.0 / sides;
            double a1 = (i + 1) * Math.PI * 2.0 / sides;
            double[] p0 = {at[0] + Math.cos(a0) * radius, at[1], at[2] + Math.sin(a0) * radius};
            double[] p1 = {at[0] + Math.cos(a1) * radius, at[1], at[2] + Math.sin(a1) * radius};
            double[] mid = {at[0], at[1] + out * 0.3, at[2]};
            gore(piece, slim, sink, new double[][] {p0, p1, mid, mid}, new int[] {rim, rim, heart, heart}, out);
        }
    }

    static void gore(Piece piece, boolean slim, Sink sink, double[][] local, int[] argb, double out) {
        double[][] p = new double[local.length][];
        for (int i = 0; i < local.length; i++) {
            p[i] = own(piece, slim, local[i]);
        }
        sink.flesh(p, normal(p), argb);
    }

    static double[] normal(double[][] p) {
        double[] n = cross(sub(p[2], p[0]), sub(p[p.length - 1], p[1]));
        double l = length(n);
        return l < 1.0E-9 ? new double[] {0.0, 1.0, 0.0} : mul(n, 1.0 / l);
    }

    // ------------------------------------------------------------------ vectors and turns

    static double[] add(double[] a, double[] b) {
        return new double[] {a[0] + b[0], a[1] + b[1], a[2] + b[2]};
    }

    static double[] sub(double[] a, double[] b) {
        return new double[] {a[0] - b[0], a[1] - b[1], a[2] - b[2]};
    }

    static double[] mul(double[] a, double k) {
        return new double[] {a[0] * k, a[1] * k, a[2] * k};
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
        return l < 1.0E-12 ? new double[] {0.0, 1.0, 0.0} : mul(a, 1.0 / l);
    }

    static double[] lerp(double[] a, double[] b, double t) {
        return new double[] {a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t, a[2] + (b[2] - a[2]) * t};
    }

    /** A turn of {@code angle} radians about the unit {@code axis}, as a quaternion {w, x, y, z}. */
    static double[] axisAngle(double[] axis, double angle) {
        double s = Math.sin(angle / 2.0);
        return new double[] {Math.cos(angle / 2.0), axis[0] * s, axis[1] * s, axis[2] * s};
    }

    /** {@code a} after {@code b}: the turn {@code b}, then {@code a}. */
    static double[] multiply(double[] a, double[] b) {
        return new double[] {a[0] * b[0] - a[1] * b[1] - a[2] * b[2] - a[3] * b[3], a[0] * b[1] + a[1] * b[0] + a[2] * b[3] - a[3] * b[2],
            a[0] * b[2] - a[1] * b[3] + a[2] * b[0] + a[3] * b[1], a[0] * b[3] + a[1] * b[2] - a[2] * b[1] + a[3] * b[0]};
    }

    /** {@code v} turned by the quaternion {@code q}. */
    public static double[] rotate(double[] q, double[] v) {
        double[] p = {0.0, v[0], v[1], v[2]};
        double[] conj = {q[0], -q[1], -q[2], -q[3]};
        double[] r = multiply(multiply(q, p), conj);
        return new double[] {r[1], r[2], r[3]};
    }

    /** Between two turns, the short way round. */
    static double[] nlerp(double[] a, double[] b, double t) {
        double sign = a[0] * b[0] + a[1] * b[1] + a[2] * b[2] + a[3] * b[3] < 0.0 ? -1.0 : 1.0;
        double[] q = new double[4];
        double l = 0.0;
        for (int i = 0; i < 4; i++) {
            q[i] = a[i] + (b[i] * sign - a[i]) * t;
            l += q[i] * q[i];
        }
        l = Math.sqrt(l);
        for (int i = 0; i < 4; i++) {
            q[i] /= l;
        }
        return q;
    }
}
