package com.hex.kagune.control.client.animation;

import com.hex.kagune.control.bikaku.PotentialRules;
import java.util.SplittableRandom;

/**
 * 3.7.3, Potential Unlock (pure; the owner's words in {@code docs/handoff/owner-words-mutations.md}): a part of his body torn off whole by a
 * blow (an arm at the shoulder, a leg at the hip, the head at the neck), cut from his own skin as Pale's revive cuts hers ({@link Remains}:
 * the same pieces, moved as one, their torn ends raw); thrown off the way the blow went, tumbling, coming down and lying where it fell;
 * then strands of his kagune, in his kagune colour, coming out of the wound, stretching and twisting to it each on its own, taking hold
 * of its torn end and wrapping round it, tightening and hauling it up off the ground and back through the air, turning it upright as it
 * comes, tissue growing between the two as it closes; in place, the flesh closes round it and the strands sink into him. About five
 * seconds ({@link #WHOLE}) for each part, each on its own.
 *
 * <p>Rig space throughout (blocks; x to his right, y up from his feet, z the way his body faced, the ground {@code ground} below his
 * feet); a part's own frame is rig space as it sits in his standing body, about its middle.
 */
public final class Reattach {
    /** The pieces of Pale's revive {@code part} is made of (moved as one). */
    static Remains.Piece[] pieces(PotentialRules.Part part) {
        return switch (part) {
            case HEAD -> new Remains.Piece[] {Remains.Piece.HEAD};
            case RIGHT_ARM -> new Remains.Piece[] {Remains.Piece.RIGHT_UPPER_ARM, Remains.Piece.RIGHT_FOREARM};
            case LEFT_ARM -> new Remains.Piece[] {Remains.Piece.LEFT_UPPER_ARM, Remains.Piece.LEFT_FOREARM};
            case RIGHT_LEG -> new Remains.Piece[] {Remains.Piece.RIGHT_THIGH, Remains.Piece.RIGHT_SHIN};
            case LEFT_LEG -> new Remains.Piece[] {Remains.Piece.LEFT_THIGH, Remains.Piece.LEFT_SHIN};
        };
    }

    /** The pieces of {@code part}, for drawing. */
    public static Remains.Piece[] piecesOf(PotentialRules.Part part) {
        return pieces(part);
    }

    /** Its torn end is its piece's top (an arm at the shoulder, a leg at the hip) or bottom (the head at the neck). */
    static boolean tornAtTop(PotentialRules.Part part) {
        return part != PotentialRules.Part.HEAD;
    }

    /** The side it flies off to (blocks a tick, rig space), besides the blow: an arm out its side, a leg out a little, the head up. */
    static double[] away(PotentialRules.Part part) {
        return switch (part) {
            case HEAD -> new double[] {0.0, 0.08, -0.02};
            case RIGHT_ARM -> new double[] {0.06, 0.04, 0.0};
            case LEFT_ARM -> new double[] {-0.06, 0.04, 0.0};
            case RIGHT_LEG -> new double[] {0.07, 0.02, 0.02};
            case LEFT_LEG -> new double[] {-0.07, 0.02, 0.02};
        };
    }

    /** The timeline (ticks from the blow): {@link PotentialRules}. */
    static final int REACH_AT = PotentialRules.REACH_AT;
    static final int GRIP_AT = PotentialRules.GRIP_AT;
    static final int HAUL_AT = PotentialRules.HAUL_AT;
    static final int HOME_AT = PotentialRules.HOME_AT;
    static final int WHOLE = PotentialRules.WHOLE;
    /** How many strands come out of each wound, how many points each is drawn through, how thick they are (blocks). */
    public static final int STRANDS = 6;
    public static final int POINTS = 16;
    public static final double STRAND_RADIUS = 0.022;
    /** How the part is thrown and falls: speed off the blow, up, spin (radians a tick), gravity, air, bounce. */
    static final double SPEED = 0.16;
    static final double UP = 0.16;
    static final double SPIN = 0.35;
    static final double GRAVITY = 0.06;
    static final double AIR = 0.97;
    static final double BOUNCE = 0.3;
    static final double SLIDE = 0.4;
    /** How high it is lifted off the ground on its way back (blocks over the straight line). */
    static final double ARC = 0.9;

    private Reattach() {
    }

    // ------------------------------------------------------------------ where it belongs

    /** The middle of {@code part} where it belongs in his standing body. */
    public static double[] centre(PotentialRules.Part part, boolean slim) {
        double[] sum = new double[3];
        for (Remains.Piece piece : pieces(part)) {
            sum = Remains.add(sum, Remains.centre(piece, slim));
        }
        return Remains.mul(sum, 1.0 / pieces(part).length);
    }

    /** A point of {@code piece}'s own frame in {@code part}'s own frame. */
    public static double[] local(PotentialRules.Part part, Remains.Piece piece, double[] own, boolean slim) {
        return Remains.sub(Remains.add(Remains.centre(piece, slim), own), centre(part, slim));
    }

    /** Its torn end in its own frame: the middle of it and the way out of it (unit; toward him, where it belongs). */
    public static double[][] end(PotentialRules.Part part, boolean slim) {
        Remains.Piece piece = pieces(part)[0];
        double[][][] openings = Remains.openings(piece, slim);
        double[][] torn = tornAtTop(part) ? openings[0] : openings[openings.length - 1];
        return new double[][] {local(part, piece, torn[0], slim), torn[1]};
    }

    /** The wound on him (rig space): where its torn end sits in his standing body, and the way out of the wound (toward where it was). */
    public static double[][] wound(PotentialRules.Part part, boolean slim) {
        double[][] end = end(part, slim);
        return new double[][] {Remains.add(centre(part, slim), end[0]), Remains.mul(end[1], -1.0)};
    }

    /** How wide its torn end is (blocks, about): an arm's or a leg's 2 to 4 pixels, the neck's. */
    static double rim(PotentialRules.Part part) {
        return part == PotentialRules.Part.HEAD ? 0.14 : part == PotentialRules.Part.RIGHT_LEG || part == PotentialRules.Part.LEFT_LEG ? 0.12 : 0.1;
    }

    // ------------------------------------------------------------------ thrown off

    /** Its way off him: its middle and turn ({w, x, y, z}) at each tick from the blow until it is hauled ({@link #HAUL_AT}). */
    public record Flight(double[][] at, double[][] turn) {
    }

    /**
     * Thrown off the way the blow went ({@code blow}, rig space, about unit), out to its own side, tumbling; it comes down and bounces once
     * off {@code ground} (below his feet), slides and lies there. The same seed throws it the same way on every client.
     */
    public static Flight thrown(PotentialRules.Part part, long seed, double[] blow, double ground, boolean slim) {
        SplittableRandom random = new SplittableRandom(seed * 0x9E3779B97F4A7C15L + part.ordinal() * 0x632BE59BD9B4E019L);
        double[][] corners = corners(part, slim);
        double[] at = centre(part, slim);
        double[] flat = {blow[0], 0.0, blow[2]};
        double[] way = Remains.length(flat) < 1.0E-6 ? new double[] {0.0, 0.0, -1.0} : Remains.normalize(flat);
        double[] v = Remains.add(Remains.add(Remains.mul(way, SPEED * (0.85 + 0.3 * random.nextDouble())), away(part)),
            new double[] {(random.nextDouble() - 0.5) * 0.06, UP * (0.8 + 0.4 * random.nextDouble()), (random.nextDouble() - 0.5) * 0.06});
        double[] axis = Remains.normalize(new double[] {random.nextDouble() - 0.5, random.nextDouble() - 0.5, random.nextDouble() - 0.5});
        double spin = SPIN * (0.7 + 0.6 * random.nextDouble()) * (random.nextBoolean() ? 1.0 : -1.0);
        double[] turn = {1.0, 0.0, 0.0, 0.0};
        double[][] ats = new double[HAUL_AT + 1][];
        double[][] turns = new double[HAUL_AT + 1][];
        ats[0] = at.clone();
        turns[0] = turn.clone();
        boolean bounced = false;
        for (int tick = 1; tick <= HAUL_AT; tick++) {
            v[1] -= GRAVITY;
            v = Remains.mul(v, AIR);
            at = Remains.add(at, v);
            turn = normalize4(Remains.multiply(Remains.axisAngle(axis, spin), turn));
            double lowest = Double.MAX_VALUE;
            for (double[] c : corners) {
                lowest = Math.min(lowest, at[1] + Remains.rotate(turn, c)[1]);
            }
            if (lowest < ground) {
                at[1] += ground - lowest;
                if (v[1] < 0.0) {
                    v[1] = bounced ? 0.0 : -v[1] * BOUNCE;
                    bounced = true;
                }
                v[0] *= SLIDE;
                v[2] *= SLIDE;
                spin *= SLIDE;
            }
            ats[tick] = at.clone();
            turns[tick] = turn.clone();
        }
        return new Flight(ats, turns);
    }

    /** The corners of its pieces in its own frame. */
    static double[][] corners(PotentialRules.Part part, boolean slim) {
        double[][] out = new double[8 * pieces(part).length][];
        for (int p = 0; p < pieces(part).length; p++) {
            double[][] own = Remains.corners(pieces(part)[p], slim);
            for (int i = 0; i < 8; i++) {
                out[p * 8 + i] = local(part, pieces(part)[p], own[i], slim);
            }
        }
        return out;
    }

    // ------------------------------------------------------------------ where it is

    /**
     * Where it is {@code age} ticks after the blow: flying, lying where it fell; from {@link #HAUL_AT} hauled up off the ground in an arc
     * and back to where it belongs, slowly at first and faster as the strands tighten, turned upright on the way so it comes in square;
     * in place from {@link #HOME_AT}.
     */
    public static Remains.Placed place(PotentialRules.Part part, Flight flight, double age, boolean slim) {
        return place(part, flight, age, slim, Anchor.HERE);
    }

    /**
     * Where the part lies relative to him now (3.7.3): it was thrown from where he stood when the blow came ({@code shift}: that spot from
     * his feet now, in his body's frame now) and his body has turned {@code turn} radians since (his body's yaw then less now). He can
     * move while it lies there: it lies where it fell, and it is hauled to wherever he is.
     */
    public record Anchor(double[] shift, double turn) {
        public static final Anchor HERE = new Anchor(new double[3], 0.0);
    }

    /** {@code placed} (where he stood when the blow came) where it is from him now. */
    static Remains.Placed anchored(Remains.Placed placed, Anchor anchor) {
        if (anchor.turn() == 0.0 && anchor.shift()[0] == 0.0 && anchor.shift()[1] == 0.0 && anchor.shift()[2] == 0.0) {
            return placed;
        }
        double[] q = Remains.axisAngle(new double[] {0.0, 1.0, 0.0}, anchor.turn());
        return new Remains.Placed(Remains.add(anchor.shift(), Remains.rotate(q, placed.at())), normalize4(Remains.multiply(q, placed.turn())));
    }

    /** As {@link #place(PotentialRules.Part, Flight, double, boolean)}, from where he is now ({@link Anchor}). */
    public static Remains.Placed place(PotentialRules.Part part, Flight flight, double age, boolean slim, Anchor anchor) {
        double[] home = centre(part, slim);
        if (age >= HOME_AT) {
            return new Remains.Placed(home, new double[] {1.0, 0.0, 0.0, 0.0});
        }
        Remains.Placed lying = anchored(sample(flight, Math.min(age, HAUL_AT)), anchor);
        if (age <= HAUL_AT) {
            return lying;
        }
        double t = (age - HAUL_AT) / (HOME_AT - HAUL_AT);
        double e = smooth(Math.pow(t, 1.3)); // slow to lift it off, faster as they tighten
        double[] up = Remains.add(Remains.lerp(lying.at(), home, 0.5), new double[] {0.0, ARC + Math.max(0.0, home[1] - lying.at()[1]) * 0.35, 0.0});
        double[] a = Remains.lerp(lying.at(), up, e);
        double[] b = Remains.lerp(up, home, e);
        double[] at = Remains.lerp(a, b, e);
        double upright = smooth(Math.min(1.0, t * 1.2));
        return new Remains.Placed(at, Remains.nlerp(lying.turn(), new double[] {1.0, 0.0, 0.0, 0.0}, upright));
    }

    /** True once it is back on him (drawn on him, not where it was); whole (the small heal) at {@link #WHOLE}. */
    public static boolean onHim(double age) {
        return age >= HOME_AT;
    }

    static Remains.Placed sample(Flight flight, double age) {
        double t = Math.max(0.0, Math.min(HAUL_AT, age));
        int a = (int)Math.floor(t);
        int b = Math.min(HAUL_AT, a + 1);
        double f = t - a;
        return new Remains.Placed(Remains.lerp(flight.at[a], flight.at[b], f), Remains.nlerp(flight.turn[a], flight.turn[b], f));
    }

    // ------------------------------------------------------------------ the strands

    /**
     * Its strands {@code age} ticks after the blow ({@code time} his clock, for their writhing): each a line of {@link #POINTS} points (rig
     * space) from round the wound on him. Reaching ({@link #REACH_AT} to {@link #GRIP_AT}), each comes out of the wound and stretches to the
     * part, twisting and searching, at its own pace; gripping (to {@link #HAUL_AT}), its tip wraps round the torn end; hauling (to
     * {@link #HOME_AT}), they tighten, pulled straight, shortening as the part comes in; knitting (to {@link #WHOLE}) they sink into him. Null
     * when there are none.
     */
    public static double[][][] strands(PotentialRules.Part part, long seed, Flight flight, double age, double time, boolean slim) {
        return strands(part, seed, flight, age, time, slim, Anchor.HERE);
    }

    /** As {@link #strands(PotentialRules.Part, long, Flight, double, double, boolean)}, from where he is now. */
    public static double[][][] strands(PotentialRules.Part part, long seed, Flight flight, double age, double time, boolean slim, Anchor anchor) {
        if (age < REACH_AT || age >= WHOLE) {
            return null;
        }
        Remains.Placed placed = place(part, flight, age, slim, anchor);
        double[][] end = end(part, slim);
        double[][] wound = wound(part, slim);
        double[] tornAt = Remains.add(placed.at(), Remains.rotate(placed.turn(), end[0]));
        double[] tornOut = Remains.rotate(placed.turn(), end[1]);
        double[] out = wound[1];
        double rim = rim(part);
        SplittableRandom random = new SplittableRandom(seed * 0x2545F4914F6CDD1DL + part.ordinal() * 977L);
        double[][][] lines = new double[STRANDS][][];
        for (int s = 0; s < STRANDS; s++) {
            double angle = Math.PI * 2.0 * s / STRANDS + (random.nextDouble() - 0.5) * 0.6;
            double pace = 0.8 + 0.4 * random.nextDouble();
            double phase = random.nextDouble() * Math.PI * 2.0;
            double[] root = Remains.add(wound[0], ring(out, angle, rim * 0.7));
            double[] grip = Remains.add(tornAt, ring(tornOut, -angle, rim * 1.1));
            double reach = Math.max(0.0, Math.min(1.0, (age - REACH_AT) / (GRIP_AT - REACH_AT) * pace));
            reach = smooth(reach);
            double tension = age <= HAUL_AT ? 0.0 : smooth(Math.min(1.0, (age - HAUL_AT) / 10.0));
            double sink = age <= HOME_AT ? 0.0 : (age - HOME_AT) / (WHOLE - HOME_AT);
            double[] tip = Remains.lerp(root, grip, reach);
            double length = Remains.length(Remains.sub(tip, root));
            double[] along = length < 1.0E-6 ? out : Remains.normalize(Remains.sub(tip, root));
            double[] side = Remains.normalize(cross(along, Math.abs(along[1]) > 0.9 ? new double[] {1.0, 0.0, 0.0} : new double[] {0.0, 1.0, 0.0}));
            double[] up = cross(side, along);
            double[][] line = new double[POINTS][];
            for (int k = 0; k < POINTS; k++) {
                double u = k / (POINTS - 1.0);
                // Out of the wound first, then bending round to its tip (a cubic: the wound's way out, then the line to the tip).
                double[] c1 = Remains.add(root, Remains.mul(out, 0.35 * length));
                double[] p = cubic(root, c1, Remains.lerp(c1, tip, 0.6), tip, u);
                // Twisting and searching while it reaches (less as it takes hold); hanging a little while slack; pulsing as it hauls.
                double slack = 1.0 - tension;
                double writhe = (0.06 + 0.12 * (1.0 - reach)) * slack * Math.sin(Math.PI * u) * length;
                p = Remains.add(p, Remains.mul(side, writhe * Math.sin(time * 0.35 + phase + u * 5.0)));
                p = Remains.add(p, Remains.mul(up, writhe * 0.7 * Math.cos(time * 0.29 + phase * 1.3 + u * 4.0)));
                p[1] -= 0.12 * length * slack * reach * Math.sin(Math.PI * u);
                line[k] = p;
            }
            // Taking hold: its last points wrap round the torn end.
            double wrap = age < GRIP_AT ? 0.0 : smooth(Math.min(1.0, (age - GRIP_AT) / (HAUL_AT - GRIP_AT)));
            int wrapped = (int)Math.round(wrap * 4.0);
            for (int j = 0; j < wrapped; j++) {
                int k = POINTS - 1 - (wrapped - 1 - j);
                double turnAngle = -angle + (j + 1) * 1.1;
                line[k] = Remains.add(Remains.add(tornAt, ring(tornOut, turnAngle, rim * 1.2)), Remains.mul(tornOut, -0.04 * (wrapped - j)));
            }
            // Knitting: they sink into him, the line drawn back into the wound.
            if (sink > 0.0) {
                int keep = Math.max(2, (int)Math.round(POINTS * (1.0 - sink)));
                double[][] shorter = new double[keep][];
                System.arraycopy(line, 0, shorter, 0, keep);
                line = shorter;
            }
            lines[s] = line;
        }
        return lines;
    }

    /** 0 .. 1: how thick strand {@code s} is now (a pulse running down it as it hauls), times {@link #STRAND_RADIUS}. */
    public static double strandWidth(int s, double age, double time) {
        double hauling = age < HAUL_AT ? 0.0 : Math.min(1.0, (age - HAUL_AT) / 10.0);
        double fade = age < HOME_AT ? 1.0 : 1.0 - (age - HOME_AT) / (WHOLE - HOME_AT);
        return (0.85 + 0.15 * Math.sin(time * 0.9 + s * 1.7) + 0.3 * hauling) * Math.max(0.0, fade);
    }

    /**
     * The tissue growing between the wound and the torn end as they come together (rig space, from the wound to the end), and how thick
     * (0 .. 1 of the rim); null when there is none.
     */
    public static double[][] tissue(PotentialRules.Part part, Flight flight, double age, boolean slim) {
        return tissue(part, flight, age, slim, Anchor.HERE);
    }

    /** As {@link #tissue(PotentialRules.Part, Flight, double, boolean)}, from where he is now. */
    public static double[][] tissue(PotentialRules.Part part, Flight flight, double age, boolean slim, Anchor anchor) {
        double from = HAUL_AT + (HOME_AT - HAUL_AT) * 0.6;
        if (age < from || age >= WHOLE) {
            return null;
        }
        Remains.Placed placed = place(part, flight, age, slim, anchor);
        double[][] end = end(part, slim);
        double[] tornAt = Remains.add(placed.at(), Remains.rotate(placed.turn(), end[0]));
        double grown = smooth(Math.min(1.0, (age - from) / (HOME_AT - from)));
        double closing = age < HOME_AT ? 1.0 : 1.0 - smooth((age - HOME_AT) / (WHOLE - HOME_AT));
        return new double[][] {wound(part, slim)[0], tornAt, {grown * closing}};
    }

    // ------------------------------------------------------------------ small sums

    static double[] ring(double[] axis, double angle, double radius) {
        double[] a = Remains.normalize(axis);
        double[] side = Remains.normalize(cross(a, Math.abs(a[1]) > 0.9 ? new double[] {1.0, 0.0, 0.0} : new double[] {0.0, 1.0, 0.0}));
        double[] up = cross(side, a);
        return Remains.add(Remains.mul(side, Math.cos(angle) * radius), Remains.mul(up, Math.sin(angle) * radius));
    }

    static double[] cubic(double[] a, double[] b, double[] c, double[] d, double t) {
        double s = 1.0 - t;
        return new double[] {s * s * s * a[0] + 3 * s * s * t * b[0] + 3 * s * t * t * c[0] + t * t * t * d[0],
            s * s * s * a[1] + 3 * s * s * t * b[1] + 3 * s * t * t * c[1] + t * t * t * d[1],
            s * s * s * a[2] + 3 * s * s * t * b[2] + 3 * s * t * t * c[2] + t * t * t * d[2]};
    }

    static double[] cross(double[] a, double[] b) {
        return new double[] {a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]};
    }

    static double[] normalize4(double[] q) {
        double l = Math.sqrt(q[0] * q[0] + q[1] * q[1] + q[2] * q[2] + q[3] * q[3]);
        return l < 1.0E-12 ? new double[] {1.0, 0.0, 0.0, 0.0} : new double[] {q[0] / l, q[1] / l, q[2] / l, q[3] / l};
    }

    static double smooth(double t) {
        double c = Math.max(0.0, Math.min(1.0, t));
        return c * c * (3.0 - 2.0 * c);
    }
}
