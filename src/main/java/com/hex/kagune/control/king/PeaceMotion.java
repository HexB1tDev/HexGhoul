package com.hex.kagune.control.king;

import static com.hex.kagune.control.furuta.FurutaMesh.add;
import static com.hex.kagune.control.furuta.FurutaMesh.clamp01;
import static com.hex.kagune.control.furuta.FurutaMesh.cross;
import static com.hex.kagune.control.furuta.FurutaMesh.dot;
import static com.hex.kagune.control.furuta.FurutaMesh.lerp;
import static com.hex.kagune.control.furuta.FurutaMesh.mul;
import static com.hex.kagune.control.furuta.FurutaMesh.normalize;
import static com.hex.kagune.control.furuta.FurutaMesh.sub;
import static com.hex.kagune.control.king.WarModel.v;

/**
 * 1.0.18: Peace's six cross blades (pure): where each blade's heart is, which way its long point goes and which way its face looks, for
 * every move, so every client draws them and the server cuts along the very same paths. Three each side, each held on its own blade
 * arm from his upper back ({@link PeaceModel} draws the arm): UR, UL (the upper pair, over his shoulders), MR, ML (the great pair, at
 * his sides) and LR, LL (the lower pair, at his hips), right and left being his own. At rest every long point is turned ahead of him,
 * a little in toward what he faces; only a blade striking leaves that.
 *
 * <p>The choreography is written in his own frame, in blocks: x to his right, y up from his feet, z ahead (the spec's frame), and
 * handed out in body pixels as {@link PeaceModel} builds (x to his left, y up, z back). Every attack runs anticipation, acceleration,
 * impact and recovery; a move cut off by the next hands its blades over ({@link #HANDOFF}) instead of snapping them home.
 */
public final class PeaceMotion {
    public static final int UR = 0;
    public static final int UL = 1;
    public static final int MR = 2;
    public static final int ML = 3;
    public static final int LR = 4;
    public static final int LL = 5;
    public static final int BLADES = 6;
    /** The great blade: the Lance's, and Piercing Salvation's middle spear (through armour). */
    public static final int GREAT = MR;
    /** Blocks to body pixels. */
    public static final double BLOCK = 16.0 / 0.9375;
    /** Ticks over which the blades are handed from a move cut off to the next. */
    public static final double HANDOFF = 4.0;

    private PeaceMotion() {
    }

    /** Where a blade is: its heart (body pixels), the way its long point goes and the way its face looks (unit, square), its size. */
    public record Tip(double[] at, double[] dir, double[] face, double size, double fold) {
        /** 1.0.36: {@code fold} (0..1): its side and back points drawn in flat along its long one (the lance's cone); 0 as it always was. */
        public Tip(double[] at, double[] dir, double[] face, double size) {
            this(at, dir, face, size, 0.0);
        }

    }

    /**
     * What the blades are doing: {@code move} at {@code level}, {@code age} ticks in, the aim {@code pitch} radians up; {@code aims}: a
     * blade sent out on its own at a body (per blade {x, y, z body pixels, how far out 0..1}, or null; 1.0.34: and, if it has them, {the way
     * in x, y, z (body pixels; NaN: from its root), how far short of the point its heart stops, its size}); the move
     * cut off as this one began ({@code was} at {@code wasLevel}, {@code cutAge} ticks into it); and, drawn only, how far his turning
     * leaves the blades behind ({@code lag}, radians, + to his left), how fast he runs ({@code run}, 0..1) and how fast he rises
     * ({@code rise}, blocks a tick, + up); 1.0.25: and each arm alive as {@link PeaceArms} has it ({@code arms}: per blade its line,
     * {x, y, z, radius} body pixels, or null to draw it still).
     */
    public record State(KingMove move, int level, double age, double pitch, double[][] aims, KingMove was, int wasLevel, double cutAge, double lag,
                        double run, double rise, double[][][] arms) {
        public static final State REST = new State(KingMove.NONE, 0, 0.0, 0.0, null, KingMove.NONE, 0, 0.0, 0.0, 0.0, 0.0);

        public State(KingMove move, int level, double age, double pitch, double[][] aims, KingMove was, int wasLevel, double cutAge, double lag,
                     double run, double rise) {
            this(move, level, age, pitch, aims, was, wasLevel, cutAge, lag, run, rise, null);
        }

        /** 1.0.25: the same, its arms alive as {@code lines} have them. */
        public State withArms(double[][][] lines) {
            return new State(this.move, this.level, this.age, this.pitch, this.aims, this.was, this.wasLevel, this.cutAge, this.lag, this.run,
                this.rise, lines);
        }

        public State(KingMove move, int level, double age, double pitch, double[][] aims) {
            this(move, level, age, pitch, aims, KingMove.NONE, 0, 0.0, 0.0, 0.0, 0.0);
        }

        public State(KingMove move, int level, double age, double pitch, double[][] aims, KingMove was, int wasLevel, double cutAge, double lag,
                     double run) {
            this(move, level, age, pitch, aims, was, wasLevel, cutAge, lag, run, 0.0);
        }
    }

    /** His own frame (blocks: x right, y up, z ahead) to body pixels. */
    static double[] at(double x, double y, double z) {
        return v(-x * BLOCK, y * BLOCK, -z * BLOCK);
    }

    /** A direction in his own frame to body pixels' axes. */
    static double[] way(double x, double y, double z) {
        return normalize(v(-x, y, -z));
    }

    /** Which side a blade is on: +1 his right, -1 his left. */
    public static int right(int blade) {
        return blade % 2 == 0 ? 1 : -1;
    }

    /** Which pair a blade is of: 0 the upper, 1 the great (middle), 2 the lower. */
    public static int tier(int blade) {
        return blade / 2;
    }

    public static boolean upper(int blade) {
        return tier(blade) == 0;
    }

    public static boolean middle(int blade) {
        return tier(blade) == 1;
    }

    public static boolean lower(int blade) {
        return tier(blade) == 2;
    }

    /** Where blade {@code blade}'s arm roots on his upper back (body pixels). */
    public static double[] root(int blade) {
        int r = right(blade);
        return switch (tier(blade)) {
            case 0 -> at(0.11 * r, 1.36, -0.18);
            case 1 -> at(0.18 * r, 1.23, -0.18);
            default -> at(0.16 * r, 1.04, -0.17);
        };
    }

    /** The way its arm leaves his back: out to its side and back, the upper rising, the lower falling. */
    public static double[] rootOut(int blade) {
        int r = right(blade);
        return switch (tier(blade)) {
            case 0 -> way(0.7 * r, 0.45, -0.6);
            case 1 -> way(0.8 * r, 0.1, -0.6);
            default -> way(0.75 * r, -0.3, -0.6);
        };
    }

    /** How big each blade is (1: the great pair's). */
    public static double size(int blade) {
        return switch (tier(blade)) {
            case 0 -> 0.85;
            case 1 -> 1.15;
            default -> 0.92;
        };
    }

    /** The aim, in his frame. */
    static double[] aim(double pitch) {
        return way(0.0, Math.sin(pitch), Math.cos(pitch));
    }

    /** The aim turned {@code yaw} degrees to his right and {@code rise} degrees up. */
    static double[] aim(double pitch, double yaw, double rise) {
        double p = pitch + Math.toRadians(rise);
        double y = Math.toRadians(yaw);
        return way(Math.sin(y) * Math.cos(p), Math.sin(p), Math.cos(y) * Math.cos(p));
    }

    // ================================================================== the whole blade, at any moment

    /**
     * Blade {@code blade} at {@code time} (ticks), grown {@code g}, doing {@code s}: the move's pose, handed over from the one before it
     * over its first ticks, a blade sent out at a body on top, and (at rest) the life in it.
     */
    public static Tip tip(int blade, double time, double g, State s) {
        if (s == null) {
            s = State.REST;
        }
        Tip pose = pose(blade, time, s.move(), s.level(), s.age(), s.pitch(), s.lag(), s.run(), s.rise());
        if (s.was() != null && s.was() != KingMove.NONE && s.age() < HANDOFF && s.move() != s.was()) {
            Tip before = pose(blade, time, s.was(), s.wasLevel(), s.cutAge(), s.pitch(), s.lag(), s.run(), s.rise());
            pose = blend(before, pose, smooth(clamp01(s.age() / HANDOFF)));
        }
        double[][] aims = s.aims();
        if (aims != null && blade < aims.length && aims[blade] != null) {
            double[] aim = aims[blade];
            double[] target = v(aim[0], aim[1], aim[2]);
            double[] from = add(root(blade), mul(rootOut(blade), 10.0));
            // 1.0.34: an aim may give its own way in and how far short of the point its heart stops (driven through: past it).
            boolean own = aim.length >= 8 && !Double.isNaN(aim[4]) && aim[4] * aim[4] + aim[5] * aim[5] + aim[6] * aim[6] > 1.0E-8;
            double[] d = own ? normalize(v(aim[4], aim[5], aim[6])) : normalize(sub(target, from));
            double back = aim.length >= 8 ? aim[7] : 6.0;
            // 1.0.36: and its own face (a lance's blades each turned out round it, as a cone's petals).
            double[] face = level(d);
            if (aim.length >= 12 && !Double.isNaN(aim[9])) {
                double[] f = v(aim[9], aim[10], aim[11]);
                f = sub(f, mul(d, dot(f, d)));
                if (dot(f, f) > 1.0E-8) {
                    face = normalize(f);
                }
            }
            double fold = aim.length >= 13 && !Double.isNaN(aim[12]) ? clamp01(aim[12]) : 0.0;
            Tip lunge = new Tip(sub(target, mul(d, back)), d, face, size(blade) * (aim.length >= 9 ? aim[8] : 1.1), fold);
            pose = blend(pose, lunge, smooth(clamp01(aim[3])));
        }
        if (g < 1.0) {
            // Growing out of him: from his back to where it goes.
            return new Tip(lerp(add(root(blade), mul(rootOut(blade), 4.0)), pose.at(), g), pose.dir(), pose.face(), pose.size() * Math.max(0.05, g), pose.fold());
        }
        // 1.0.19: sinking back into his back as it leaves him, it is meant to go in.
        return s.move() == KingMove.RECEDE && s.age() > KingTransform.PEACE_FOLD ? pose : clear(blade, pose);
    }

    /** His body as the blades keep out of it (body pixels): this far either side of his middle, feet to over his head, chest to back. */
    static final double HALF = 9.6;
    static final double TOP = 34.0;
    static final double FRONT = -4.6;
    static final double BACK = 4.0;

    /**
     * A blade never inside him: if its heart or any of its points would be (a pose too close in), the whole of it is moved out to its
     * own side until all of it is clear. What every client draws and what the server cuts along both go by this.
     */
    public static Tip clear(int blade, Tip tip) {
        double size = tip.size();
        double[] n = tip.face();
        double[] across = cross(n, tip.dir());
        double side = PeaceModel.sidePoint(blade) * size;
        double[][] parts = {tip.at(), point(tip, blade), sub(tip.at(), mul(tip.dir(), PeaceModel.backPoint(blade) * size)), add(tip.at(), mul(across, side)),
            sub(tip.at(), mul(across, side))};
        double push = 0.0;
        double out = -right(blade); // his right is -x in body pixels
        for (double[] q : parts) {
            double r = 2.2;
            boolean alongside = q[1] > -1.0 - r && q[1] < TOP + r && q[2] > FRONT - r && q[2] < BACK + r;
            if (!alongside) {
                continue;
            }
            push = Math.max(push, (HALF + r) - q[0] * out);
        }
        return push <= 0.0 ? tip : new Tip(add(tip.at(), v(out * push, 0.0, 0.0)), tip.dir(), tip.face(), tip.size(), tip.fold());
    }

    /** Blade {@code blade} in {@code move} at {@code level}, {@code age} ticks in: what the server cuts along (no handing over). */
    public static Tip pose(int blade, double time, KingMove move, int level, double age, double pitch, double lag, double run) {
        return pose(blade, time, move, level, age, pitch, lag, run, 0.0);
    }

    /** As above, rising {@code rise} (drawn only). */
    public static Tip pose(int blade, double time, KingMove move, int level, double age, double pitch, double lag, double run, double rise) {
        Tip rest = rest(blade, time, lag, run, rise);
        // 1.0.25: this move's own arcs (keys): drawn from when it began, so every client and the server draw and cut the same ones.
        SEED.get()[0] = move == KingMove.NONE ? 0L : seed(Math.round(time - age), blade, move.ordinal() * 31 + level);
        return switch (move) {
            case BLADES -> strike(blade, rest, PeaceStrike.byOrdinal(level), age, pitch, time);
            case CHARGE -> charge(blade, rest, age, time);
            // 1.0.34: the reworked six: their blades go where their aims send them (into bodies, the lance, the ground); unsent, poised.
            case WALTZ -> poised(blade, rest, age, PeaceRules.WALTZ_TICKS, 0.5);
            case AEGIS, AEGIS_OPEN -> rest;
            case ASCENT -> ascent(blade, rest, age, time, lag, run);
            case DIVE -> dive(blade, age, pitch);
            case LANDING -> landing(blade, rest, age);
            case PIERCE -> poised(blade, rest, age, 0, 0.8);
            case REBIRTH -> poised(blade, rest, age, 0, 0.3);
            case HORIZON -> poised(blade, rest, age, PeaceRules.HORIZON_TICKS, 0.6);
            case EMERGE -> emerge(blade, rest, age, time);
            case RECEDE -> recede(blade, rest, age);
            default -> rest;
        };
    }

    // ================================================================== at rest: six blades framing him, every point ahead

    /**
     * Each pair at rest (his frame): {x out to its side, y, z ahead (blocks), how far its point turns in toward what he faces, how far
     * up (degrees), how far the blade is rocked on its point (degrees, the upper pair's tops out, the lower pair's feet out)}.
     */
    static final double[][] HOME = {{1.35, 2.45, 0.15, 24.0, -6.0, 26.0}, {2.05, 1.5, 0.35, 30.0, 0.0, 0.0}, {1.7, 0.6, 0.25, 27.0, 7.0, -26.0}};

    /**
     * At rest: three each side framing him, the upper pair over his shoulders, the great pair at his sides, the lower at his hips, every
     * long point turned ahead of him. Never still: each drifts on its own slow figure of eight, its point wandering a little, the blade
     * rocking on it; turning leaves them behind ({@code lag}), running streams them back ({@code run}), rising leaves them below and
     * falling lifts them ({@code rise}). Through all of it the points stay ahead.
     */
    static Tip rest(int blade, double time, double lag, double run, double rise) {
        int r = right(blade);
        int tier = tier(blade);
        double seed = blade * 1.9 + 0.7;
        double[] home = HOME[tier];
        // 1.0.21: more of it: a wider drift, and now and then a blade reaches out far ahead of him on its arm and is drawn back.
        double reach = Math.pow(Math.max(0.0, Math.sin(time * 0.031 + seed * 2.7)), 6.0);
        // 1.0.25: and now and then each does something of its own: looks somewhere else a while, flicks over on its point, darts out.
        double[] own = gesture(blade, time);
        double x = home[0] + 0.13 * Math.sin(time * 0.041 + seed) - 0.25 * reach + 0.1 * own[3];
        double y = home[1] + 0.14 * Math.sin(time * 0.063 + seed * 1.3);
        double z = home[2] + 0.12 * Math.sin(time * 0.082 + seed * 0.6) + 0.9 * reach + 0.65 * own[3];
        double yaw = home[3] + 8.0 * Math.sin(time * 0.047 + seed * 2.1) - 10.0 * reach + own[0];
        double pitch = home[4] + 5.0 * Math.sin(time * 0.071 + seed * 0.9) + own[1];
        double roll = home[5] + 18.0 * Math.sin(time * 0.039 + seed * 1.7) + own[2];
        // Running: streamed back and in, the points dipping.
        x *= 1.0 - 0.2 * run;
        z -= 0.9 * run;
        pitch -= 7.0 * run;
        // Rising leaves them below him, falling lifts them (the lower pair, farther from where they are held, most).
        double drag = Math.max(-1.0, Math.min(1.0, rise / 0.45));
        y -= (0.1 + 0.05 * tier) * drag;
        pitch -= 8.0 * drag;
        double ya = Math.toRadians(yaw);
        double pa = Math.toRadians(pitch);
        double[] heart = at(x * r, y, z);
        double[] dir = way(-r * Math.sin(ya) * Math.cos(pa), Math.sin(pa), Math.cos(ya) * Math.cos(pa));
        double[] face = roll(dir, faceNear(dir, way(0.0, 0.0, 1.0)), Math.toRadians(roll * r));
        // Turning: the blades swung round his vertical the way he came from, the points with them (less).
        double swing = -lag * (0.5 + 0.12 * tier);
        return new Tip(spin(heart, swing), spin(dir, swing * 0.6), spin(face, swing * 0.6), size(blade));
    }

    /**
     * 1.0.25: blade {@code blade}'s own gestures at rest at {@code time}, each now and then (the same on every client: by the clock):
     * {how far its point looks off to its side, and up (degrees), how far it has flicked over on its point (degrees, a whole turn),
     * how far it has darted out (0..1)}.
     */
    static double[] gesture(int blade, double time) {
        double[] out = new double[4];
        // A glance: its point turned off somewhere else a couple of seconds, and back.
        long w = Math.floorDiv((long)Math.floor(time) + blade * 37L, 90L);
        long g = seed(w, blade, 101);
        if (unit(g, 0) < 0.5) {
            double u = (time - (w * 90.0 - blade * 37.0 + unit(g, 1) * 40.0)) / 45.0;
            if (u > 0.0 && u < 1.0) {
                double e = Math.sin(Math.PI * u);
                e *= e;
                out[0] = (unit(g, 2) * 2.0 - 1.0) * 38.0 * e;
                out[1] = (unit(g, 3) * 2.0 - 1.0) * 22.0 * e;
            }
        }
        // A flick: a whole turn over on its point, quick.
        w = Math.floorDiv((long)Math.floor(time) + blade * 53L, 130L);
        g = seed(w, blade, 102);
        if (unit(g, 0) < 0.35) {
            double u = (time - (w * 130.0 - blade * 53.0 + unit(g, 1) * 110.0)) / 9.0;
            if (u > 0.0 && u < 1.0) {
                out[2] = 360.0 * smooth(u) * (unit(g, 2) < 0.5 ? -1.0 : 1.0);
            }
        }
        // A dart: out ahead and back, as if testing the air.
        w = Math.floorDiv((long)Math.floor(time) + blade * 29L, 75L);
        g = seed(w, blade, 103);
        if (unit(g, 0) < 0.4) {
            double u = (time - (w * 75.0 - blade * 29.0 + unit(g, 1) * 60.0)) / 8.0;
            if (u > 0.0 && u < 1.0) {
                out[3] = Math.sin(Math.PI * Math.pow(u, 0.6));
            }
        }
        return out;
    }

    /** {@code p} (body pixels or a direction) turned {@code a} radians round his vertical (+: ahead of him toward his left). */
    static double[] spin(double[] p, double a) {
        double c = Math.cos(a);
        double s = Math.sin(a);
        return v(p[0] * c + p[2] * s, p[1], -p[0] * s + p[2] * c);
    }

    /** {@code face} rocked {@code a} radians round {@code dir} (both unit, square). */
    static double[] roll(double[] dir, double[] face, double a) {
        return add(mul(face, Math.cos(a)), mul(cross(dir, face), Math.sin(a)));
    }

    // ================================================================== the tools

    static double smooth(double x) {
        return x * x * (3.0 - 2.0 * x);
    }

    /** Fast at first, settling at the end: a blow unfolding. */
    static double out(double x) {
        double k = 1.0 - x;
        return 1.0 - k * k * k;
    }

    /** Slow at first, fast at the end: a blow gathering speed. */
    static double in(double x) {
        return x * x * x;
    }

    /** A face for a point going along {@code dir}: level and square to it (the blade's flat stood upright along the way it goes). */
    static double[] level(double[] dir) {
        double[] f = cross(dir, v(0, 1, 0));
        return dot(f, f) < 1.0E-6 ? v(1, 0, 0) : normalize(f);
    }

    /** A face square to {@code dir} lying as near {@code wanted} as it can. */
    static double[] faceNear(double[] dir, double[] wanted) {
        double[] f = sub(wanted, mul(dir, dot(wanted, dir)));
        return dot(f, f) < 1.0E-6 ? level(dir) : normalize(f);
    }

    /** Between two poses: the heart along the way, the point and the face turned over (never flipped), the size. */
    public static Tip blend(Tip a, Tip b, double k) {
        if (k <= 0.0) {
            return a;
        }
        if (k >= 1.0) {
            return b;
        }
        double[] dir = lerp(a.dir(), b.dir(), k);
        dir = dot(dir, dir) < 1.0E-6 ? b.dir() : normalize(dir);
        double[] bf = dot(a.face(), b.face()) < 0.0 ? mul(b.face(), -1.0) : b.face();
        double[] face = faceNear(dir, lerp(a.face(), bf, k));
        return new Tip(lerp(a.at(), b.at(), k), dir, face, a.size() + (b.size() - a.size()) * k, a.fold() + (b.fold() - a.fold()) * k);
    }

    /** One key of a blade's path: at {@code t} ticks it is at {@code tip}, eased there by {@code ease} (0 smooth, 1 out, 2 in, 3 straight). */
    record Key(double t, Tip tip, int ease) {
    }

    static final int SMOOTH = 0;
    static final int OUT = 1;
    static final int IN = 2;
    static final int STRAIGHT = 3;

    /** 1.0.25: the arcs of the move being worked out on this thread (0: none, the keys go straight). */
    private static final ThreadLocal<long[]> SEED = ThreadLocal.withInitial(() -> new long[1]);
    /** How far a blade's way between two keys bows off the straight (of the way's length): a fast blow, a slower move. */
    static final double BOW_FAST = 0.28;
    static final double BOW = 0.34;
    /** How far a blade turns over on its point on the way between two keys (radians, at most). */
    static final double TWIST = 0.6;

    /** A seed for one move's arcs: begun at tick {@code start}, of blade {@code blade}, its kind {@code kind}. */
    static long seed(long start, int blade, int kind) {
        long h = start * 0x9E3779B97F4A7C15L + blade * 0xC2B2AE3D27D4EB4FL + kind * 0x165667B19E3779F9L + 0x27D4EB2F165667C5L;
        h ^= h >>> 33;
        h *= 0xFF51AFD7ED558CCDL;
        h ^= h >>> 33;
        return h == 0L ? 1L : h;
    }

    /** A number in [0, 1) from {@code seed} and {@code k}. */
    static double unit(long seed, int k) {
        long h = seed + k * 0xC4CEB9FE1A85EC53L;
        h ^= h >>> 31;
        h *= 0xFF51AFD7ED558CCDL;
        h ^= h >>> 29;
        return (h >>> 11) * 0x1.0p-53;
    }

    /**
     * 1.0.25: {@code tip}, {@code k} of the way from key {@code a} to key {@code b} (the {@code segment}th way of the move), bowed off the
     * straight along an arc of its own and turned over a little on its point, nothing at either key: no two blows of a blade ever go the
     * same way, and none goes in a straight line.
     */
    static Tip bowed(Tip tip, Tip a, Tip b, double k, int segment, int ease) {
        long seed = SEED.get()[0];
        if (seed == 0L || ease == STRAIGHT) {
            return tip;
        }
        double[] way = sub(b.at(), a.at());
        double length = Math.sqrt(dot(way, way));
        if (length < 3.0) {
            return tip;
        }
        double[] along = mul(way, 1.0 / length);
        double[] e1 = cross(along, v(0, 1, 0));
        e1 = dot(e1, e1) < 1.0E-6 ? v(1, 0, 0) : normalize(e1);
        double[] e2 = cross(along, e1);
        double angle = unit(seed, segment * 3) * Math.PI * 2.0;
        double[] off = add(mul(e1, Math.cos(angle)), mul(e2, Math.sin(angle)));
        double bend = Math.sin(Math.PI * k);
        double amount = (ease == SMOOTH ? BOW : BOW_FAST) * length * (0.55 + 0.45 * unit(seed, segment * 3 + 1)) * bend;
        double twist = TWIST * (unit(seed, segment * 3 + 2) * 2.0 - 1.0) * bend;
        return new Tip(add(tip.at(), mul(off, amount)), tip.dir(), roll(tip.dir(), tip.face(), twist), tip.size());
    }

    /** Where the keys have it at {@code age}. */
    static Tip keys(double age, Key... keys) {
        if (age <= keys[0].t()) {
            return keys[0].tip();
        }
        for (int i = 1; i < keys.length; i++) {
            if (age <= keys[i].t()) {
                double u = clamp01((age - keys[i - 1].t()) / Math.max(1.0E-6, keys[i].t() - keys[i - 1].t()));
                double k = switch (keys[i].ease()) {
                    case OUT -> out(u);
                    case IN -> in(u);
                    case STRAIGHT -> u;
                    default -> smooth(u);
                };
                return bowed(blend(keys[i - 1].tip(), keys[i].tip(), k), keys[i - 1].tip(), keys[i].tip(), k, i, keys[i].ease());
            }
        }
        return keys[keys.length - 1].tip();
    }

    /** A pose in his frame: the heart at (x, y, z), its point along {@code dir}, its face near {@code face}, {@code size}. */
    static Tip p(double x, double y, double z, double[] dir, double[] face, double size) {
        return new Tip(at(x, y, z), dir, faceNear(dir, face), size);
    }

    /** A pose with its heart at body pixels {@code heart}. */
    static Tip px(double[] heart, double[] dir, double[] face, double size) {
        return new Tip(heart, dir, faceNear(dir, face), size);
    }

    /** Round him at {@code azimuth} degrees (0 ahead, 90 to his right, 180 behind, 270 to his left), {@code radius} out, {@code height} up. */
    static double[] round(double azimuth, double radius, double height) {
        double a = Math.toRadians(azimuth);
        return at(radius * Math.sin(a), height, radius * Math.cos(a));
    }

    /** The tangent of a circle round him at {@code azimuth}, going {@code sense} (+1 clockwise from above: ahead to right). */
    static double[] tangent(double azimuth, double sense) {
        double a = Math.toRadians(azimuth);
        return way(sense * Math.cos(a), 0.0, -sense * Math.sin(a));
    }

    /** A blade wheeling round him at {@code azimuth}: its heart on the circle, its point along the way it goes, its flat level. */
    static Tip orbit(double azimuth, double radius, double height, double sense, double size) {
        return new Tip(round(azimuth, radius, height), tangent(azimuth, sense), UP, size);
    }

    /** Straight out from him at {@code azimuth}. */
    static double[] outward(double azimuth) {
        double a = Math.toRadians(azimuth);
        return way(Math.sin(a), 0.0, Math.cos(a));
    }

    static final double[] UP = {0.0, 1.0, 0.0};
    /** A face turned to what is before him. */
    static final double[] AHEAD = {0.0, 0.0, -1.0};

    /** Of the three pairs, {@code a} for the upper, {@code b} for the great, {@code c} for the lower. */
    static double by(int blade, double a, double b, double c) {
        return switch (tier(blade)) {
            case 0 -> a;
            case 1 -> b;
            default -> c;
        };
    }

    /**
     * 1.0.19: Peace leaving him: each blade drawn in to its side, its point turned down and back; folded tight to his back; sunk into it
     * at its root (the upper pair first).
     */
    static Tip recede(int blade, Tip rest, double age) {
        int r = right(blade);
        double s = size(blade);
        double t = age - tier(blade) * 1.0;
        Tip drawn = p(0.75 * r, by(blade, 2.0, 1.45, 0.9), -0.45, way(0.35 * r, by(blade, 0.4, -0.2, -0.6), -0.85), UP, s);
        double fold = KingTransform.PEACE_FOLD;
        Tip folded = p(0.3 * r, by(blade, 1.5, 1.3, 1.05), -0.3, way(0.25 * r, by(blade, 0.6, 0.0, -0.5), -0.6), UP, 0.45 * s);
        Tip sunk = new Tip(add(root(blade), mul(rootOut(blade), 2.0)), folded.dir(), folded.face(), 0.05);
        return keys(t, new Key(0.0, rest, SMOOTH), new Key(5.0, drawn, OUT), new Key(fold, folded, SMOOTH), new Key(fold + 8.0, sunk, IN),
            new Key(1.0E9, sunk, SMOOTH));
    }

    /** A slight shift of a rest pose (his frame, blocks): the others giving way to a blade striking. */
    static Tip nudge(Tip rest, double x, double y, double z) {
        return new Tip(add(rest.at(), at(x, y, z)), rest.dir(), rest.face(), rest.size());
    }

    /**
     * 1.0.17: Peace coming over him: each blade folded tight to his back, drawn up and out, unfurled as a wing (wide and high, the
     * upper pair first, beating slowly), held, and folded at last to its place at rest, its point turned ahead.
     */
    static Tip emerge(int blade, Tip rest, double age, double time) {
        int r = right(blade);
        int tier = tier(blade);
        double s = size(blade);
        double t = age - tier * 1.5 - (r > 0 ? 0.0 : 1.0);
        Tip folded = p(0.3 * r, by(blade, 1.5, 1.3, 1.05), -0.35, way(0.3 * r, by(blade, 0.7, 0.1, -0.5), -0.5), UP, 0.45 * s);
        Tip rising = p(0.75 * r, by(blade, 2.05, 1.5, 0.85), -0.7, way(0.55 * r, by(blade, 0.85, 0.2, -0.45), -0.45), UP, 0.85 * s);
        double beat = Math.sin(time * 0.16 + blade) * 0.08;
        Tip wide = p(by(blade, 2.0, 2.4, 1.85) * r, by(blade, 2.75 + beat, 1.7, 0.9 - beat), by(blade, -0.95, -1.0, -1.05),
            way(by(blade, 0.75, 0.9, 0.7) * r, by(blade, 0.55, 0.05, -0.3), by(blade, -0.45, -0.45, -0.65)), UP, 1.05 * s);
        Tip held = p(by(blade, 1.9, 2.25, 1.75) * r, by(blade, 2.65 - beat, 1.65, 0.95 + beat), by(blade, -1.05, -1.05, -1.1),
            way(by(blade, 0.7, 0.85, 0.65) * r, by(blade, 0.45, 0.05, -0.25), by(blade, -0.55, -0.5, -0.7)), UP, 1.02 * s);
        double from = KingTransform.PEACE_WINGS_FROM;
        return keys(t, new Key(from, folded, SMOOTH), new Key(from + 7.0, rising, OUT), new Key(from + 17.0, wide, OUT), new Key(from + 28.0, held, SMOOTH),
            new Key(KingTransform.PEACE_WINGS_TO, rest, SMOOTH), new Key(1.0E9, rest, SMOOTH));
    }

    // ================================================================== the M1

    static Tip strike(int blade, Tip rest, PeaceStrike blow, double age, double pitch, double time) {
        return switch (blow) {
            case LANCE -> lance(blade, rest, age, aim(pitch));
            case SWEEP -> sweep(blade, rest, age);
            case SCISSOR -> scissor(blade, rest, age);
            case CRUCIFIX -> crucifix(blade, rest, age, pitch);
            case ASCEND -> ascend(blade, rest, age);
            case JUDGMENT -> judgment(blade, rest, age);
            case CROSS_CLEAVE -> crossCleave(blade, rest, age);
            case SERAPH -> seraph(blade, age, pitch, time);
            case HALO -> halo(blade, rest, age);
        };
    }

    // ================================================================== 1.0.20: the fighting grammar's blows

    /**
     * The heavy charging: all six drawn back behind him, up and out, every point still ahead, quivering harder the longer it is held
     * (how far it is through the charge, {@link KingCombo#charge}).
     */
    static Tip charge(int blade, Tip rest, double age, double time) {
        return blend(rest, drawn(blade, age, time), smooth(clamp01(age / 5.0)));
    }

    /** Where blade {@code blade} is drawn back to, {@code age} ticks into the charge. */
    static Tip drawn(int blade, double age, double time) {
        int r = right(blade);
        double full = KingCombo.charge(age);
        double tremble = (0.012 + 0.035 * full) * Math.sin(time * 2.7 + blade * 1.9);
        return p(by(blade, 0.8, 1.15, 0.95) * r * (1.0 - 0.15 * full), by(blade, 2.15, 1.5, 0.8) + tremble, -0.55 - 0.3 * full + tremble,
            way(-0.15 * r, by(blade, -0.05, 0.0, 0.05), 1.0), way(r, 0.0, 0.0), size(blade) * (1.0 + 0.12 * full));
    }

    /** Ascend, the great pair's: down low before him, then scooped up through them and high; the others give way. */
    static Tip ascend(int blade, Tip rest, double age) {
        PeaceStrike blow = PeaceStrike.ASCEND;
        if (!middle(blade)) {
            return give(blade, rest, age, blow, 0.12);
        }
        int r = right(blade);
        double s = size(blade);
        Tip low = p(0.7 * r, 0.2, 2.2, way(-0.15 * r, -0.55, 1.0), way(r, 0.0, 0.0), s * 1.05);
        Tip lift = p(0.4 * r, 1.7, 2.8, way(-0.05 * r, 1.0, 0.45), way(r, 0.0, 0.0), s * 1.1);
        Tip high = p(0.7 * r, 3.1, 2.1, way(0.1 * r, 1.0, 0.1), way(r, 0.0, 0.0), s * 1.05);
        return keys(age, new Key(0.0, rest, SMOOTH), new Key(blow.impact - 2.5, low, SMOOTH), new Key(blow.impact, lift, IN),
            new Key(blow.impact + 2.0, high, OUT), new Key(blow.duration, rest, SMOOTH));
    }

    /** Judgment, the upper and great pairs': raised over him, then driven down through them before him; the lower pair give way. */
    static Tip judgment(int blade, Tip rest, double age) {
        PeaceStrike blow = PeaceStrike.JUDGMENT;
        if (lower(blade)) {
            return give(blade, rest, age, blow, 0.15);
        }
        int r = right(blade);
        boolean up = upper(blade);
        double s = size(blade) * 1.1;
        Tip raised = p((up ? 0.9 : 1.4) * r, up ? 3.2 : 2.8, 0.0, way(-0.15 * r, -0.4, 1.0), way(r, 0.0, 0.0), s);
        Tip down = p((up ? 0.3 : 0.6) * r, up ? 1.7 : 1.45, up ? 2.4 : 2.2, way(-0.05 * r, -0.8, 0.6), way(r, 0.0, 0.0), s);
        Tip after = p((up ? 0.36 : 0.72) * r, up ? 1.25 : 1.0, 2.3, way(0.0, -0.9, 0.4), way(r, 0.0, 0.0), s);
        return keys(age, new Key(0.0, rest, SMOOTH), new Key(blow.impact - 2.0, raised, SMOOTH), new Key(blow.impact, down, IN),
            new Key(blow.impact + 2.0, after, OUT), new Key(blow.duration, rest, SMOOTH));
    }

    /** Cross Cleave, the great pair's: raised wide either side, then crossed down through the front of him (a hair apart), and home. */
    static Tip crossCleave(int blade, Tip rest, double age) {
        PeaceStrike blow = PeaceStrike.CROSS_CLEAVE;
        if (!middle(blade)) {
            return give(blade, rest, age, blow, 0.15);
        }
        int r = right(blade);
        double depth = r > 0 ? 0.08 : -0.08;
        double s = size(blade);
        Tip raised = p(1.9 * r, 2.8, 0.6, way(-0.35 * r, 0.9, 0.3), AHEAD, s * 1.1);
        Tip crossed = p(0.0, 1.35, 2.7 + depth, way(-0.7 * r, -0.7, 0.2), AHEAD, s * 1.15);
        Tip past = p(-1.6 * r, 0.4, 2.1 + depth, way(-0.6 * r, -0.8, -0.1), AHEAD, s * 1.1);
        return keys(age, new Key(0.0, rest, SMOOTH), new Key(blow.impact - 3.0, raised, SMOOTH), new Key(blow.impact, crossed, IN),
            new Key(blow.impact + 2.0, past, OUT), new Key(blow.duration, rest, SMOOTH));
    }

    /** How far each blade goes in the Seraph Thrust (blocks): the great pair farthest. */
    public static double seraphReach(int blade) {
        return by(blade, 4.8, 6.0, 4.4);
    }

    /**
     * Seraph Thrust, all six: drawn back as the charge left them, tighter still, then driven out together along the aim in a column
     * (a ring of them round its middle, the great pair farthest), held, drawn back home.
     */
    static Tip seraph(int blade, double age, double pitch, double time) {
        PeaceStrike blow = PeaceStrike.SERAPH;
        double[] aim = aim(pitch);
        double a = Math.toRadians(hex(blade));
        double[] from = at(0.35 * Math.cos(a), 1.35 + 0.3 * Math.sin(a), 0.3);
        double reach = seraphReach(blade);
        Tip back = drawn(blade, KingCombo.CHARGE_TICKS, time);
        Tip tight = new Tip(add(back.at(), at(0.0, 0.0, -0.25)), back.dir(), back.face(), back.size());
        Tip thrust = px(add(from, mul(aim, reach * BLOCK)), aim, level(aim), size(blade) * 1.2);
        Tip held = px(add(from, mul(aim, (reach - 0.3) * BLOCK)), aim, level(aim), size(blade) * 1.15);
        Tip half = px(add(from, mul(aim, reach * 0.4 * BLOCK)), aim, level(aim), size(blade));
        return keys(age, new Key(0.0, back, SMOOTH), new Key(blow.impact - 2.0, tight, SMOOTH), new Key(blow.impact, thrust, OUT),
            new Key(blow.impact + 4.0, held, SMOOTH), new Key(blow.impact + 8.0, half, SMOOTH), new Key(blow.duration, rest(blade, time, 0.0, 0.0, 0.0),
                SMOOTH));
    }

    /**
     * Halo Rend: each side's three swept round its own half of him, from before him round to behind (the right's clockwise, the left's
     * the other way, so no arm ever winds round him), a ring of cuts all round; flung out at its end, and home.
     */
    static Tip halo(int blade, Tip rest, double age) {
        PeaceStrike blow = PeaceStrike.HALO;
        int r = right(blade);
        double radius = by(blade, 1.85, 2.15, 1.95);
        double h = by(blade, 2.2, 1.45, 0.75);
        double from = by(blade, 8.0, 22.0, 14.0) * r;
        double to = by(blade, 178.0, 168.0, 174.0) * r;
        double s = size(blade) * 1.1;
        double start = blow.impact - 3.0;
        double stop = blow.impact + 3.0;
        if (age >= start && age <= stop) {
            double u = (age - start) / (stop - start);
            return orbit(from + (to - from) * smooth(u), radius, h, r, s);
        }
        Tip begin = orbit(from, radius, h, r, s);
        if (age < start) {
            return keys(age, new Key(0.0, rest, SMOOTH), new Key(start, begin, SMOOTH));
        }
        double[] heart = round(to, radius + 0.5, h);
        Tip flung = new Tip(heart, outward(to), UP, s);
        return keys(age, new Key(stop, orbit(to, radius, h, r, s), SMOOTH), new Key(stop + 2.0, flung, OUT), new Key(blow.duration, rest, SMOOTH));
    }

    /** The blades not in {@code blow} giving way to those that are: out a little to their sides and back, and home after. */
    static Tip give(int blade, Tip rest, double age, PeaceStrike blow, double amount) {
        double give = smooth(clamp01(age / 3.0)) * (1.0 - smooth(clamp01((age - blow.impact - 2.0) / 3.0)));
        return nudge(rest, right(blade) * amount * give, by(blade, 0.1, 0.03, -0.06) * give, -0.1 * give);
    }

    /**
     * Lance, MR's: drawn back beside his right shoulder, coiled, the point turned to the target and held a breath; then driven nearly
     * straight from the shoulder to the target's middle; a pulse back through it; swung out to his side, the point still ahead, home.
     */
    static Tip lance(int blade, Tip rest, double age, double[] aim) {
        PeaceStrike blow = PeaceStrike.LANCE;
        if (blade != MR) {
            return give(blade, rest, age, blow, 0.12);
        }
        double[] shoulder = at(0.35, 1.45, 0.1);
        double[] hit = add(shoulder, mul(aim, 4.6 * BLOCK));
        double s = size(MR);
        Tip back = p(1.0, 1.7, -0.85, aim, UP, s);
        Tip held = p(0.95, 1.65, -1.0, aim, UP, s);
        Tip out = px(add(hit, mul(aim, 0.35 * BLOCK)), aim, UP, s * 1.1);
        Tip pulse = px(add(hit, mul(aim, -0.3 * BLOCK)), aim, UP, s);
        Tip after = p(2.0, 1.75, 1.0, way(-0.2, 0.2, 1.0), UP, s);
        return keys(age, new Key(0.0, rest, SMOOTH), new Key(2.2, back, SMOOTH), new Key(3.2, held, SMOOTH), new Key(blow.impact, out, OUT),
            new Key(blow.impact + 1.0, pulse, SMOOTH), new Key(blow.impact + 3.0, after, SMOOTH), new Key(blow.duration, rest, SMOOTH));
    }

    /** Sweep's arc: round a point behind him, out ahead of him (degrees: 0 ahead, + to his right). */
    static double[] sweepAt(double azimuth) {
        double a = Math.toRadians(azimuth);
        return at(3.1 * Math.sin(a), 1.45, -0.4 + 3.1 * Math.cos(a));
    }

    /**
     * Sweep, ML's: out to his left, its edge turned to the way it will go, its arm a great curve behind it; then flat across in front
     * of him, left to right, a scythe, the edge leading along the arc; on past, then curled back home.
     */
    static Tip sweep(int blade, Tip rest, double age) {
        PeaceStrike blow = PeaceStrike.SWEEP;
        if (blade != ML) {
            return give(blade, rest, age, blow, 0.1);
        }
        double start = blow.impact - 2.0;
        double stop = blow.impact + 2.0;
        if (age >= start && age <= stop) {
            double u = (age - start) / (stop - start);
            return sweepPose(-72.0 + 144.0 * (u < 0.5 ? 0.5 * out(u * 2.0) : 0.5 + 0.5 * smooth((u - 0.5) * 2.0)));
        }
        Tip wind = sweepPose(-72.0);
        if (age < start) {
            return keys(age, new Key(0.0, rest, SMOOTH), new Key(start, wind, SMOOTH));
        }
        Tip past = sweepPose(95.0);
        return keys(age, new Key(stop, sweepPose(72.0), SMOOTH), new Key(stop + 1.5, past, OUT), new Key(blow.duration, rest, SMOOTH));
    }

    /** Sweep's blade at {@code azimuth} on its arc: its point out from him, its flat level, its edge leading round. */
    static Tip sweepPose(double azimuth) {
        return new Tip(sweepAt(azimuth), outward(azimuth), UP, size(ML) * 1.05);
    }

    /**
     * Scissor, the upper and lower pairs': swung out wide either side, their points turned in to one spot (the upper pair high, the
     * lower low); then closed through the front of him, crossing (a hair apart in depth) a couple of blocks ahead, on past each other,
     * then apart and home. The great pair spread wide.
     */
    static Tip scissor(int blade, Tip rest, double age) {
        PeaceStrike blow = PeaceStrike.SCISSOR;
        if (middle(blade)) {
            double spread = smooth(clamp01(age / 3.0)) * (1.0 - smooth(clamp01((age - blow.impact - 2.0) / 4.0)));
            return nudge(rest, right(blade) * 0.25 * spread, 0.05 * spread, -0.1 * spread);
        }
        int r = right(blade);
        boolean up = upper(blade);
        double s = size(blade) * 1.2;
        double y = up ? (r > 0 ? 1.9 : 2.0) : (r > 0 ? 0.95 : 1.06);
        double[] cross = at(0.0, y, 2.8);
        double[] wide = at(2.2 * r, y, 0.7);
        double[] past = at(-0.8 * r, y, 3.0);
        double[] in = normalize(sub(cross, wide));
        double[] through = normalize(sub(past, cross));
        Tip open = px(wide, in, UP, s);
        Tip crossed = px(cross, normalize(add(in, through)), UP, s);
        Tip beyond = px(past, through, UP, s);
        Tip apart = p(1.25 * r, up ? 2.1 : 0.85, 1.0, way(-0.25 * r, up ? -0.1 : 0.1, 1.0), UP, size(blade));
        return keys(age, new Key(0.0, rest, SMOOTH), new Key(blow.impact - 2.5, open, SMOOTH), new Key(blow.impact, crossed, IN),
            new Key(blow.impact + 1.2, beyond, OUT), new Key(blow.impact + 3.2, apart, SMOOTH), new Key(blow.duration, rest, SMOOTH));
    }

    /** Where each blade stands round his front in the Crucifix and the Final Cross (degrees round, 0 to his right, 90 up, seen from behind). */
    static double hex(int blade) {
        return switch (blade) {
            case UR -> 58.0;
            case UL -> 122.0;
            case ML -> 180.0;
            case LL -> 238.0;
            case LR -> 302.0;
            default -> 0.0;
        };
    }

    /**
     * Crucifix, all six: out round his front like the points of a star, every point turned in on one spot (a moment: a great star of
     * blades round his front), then all driven in at once, impaling it (a little apart round it, never through each other), apart again,
     * and home round him.
     */
    static Tip crucifix(int blade, Tip rest, double age, double pitch) {
        PeaceStrike blow = PeaceStrike.CRUCIFIX;
        double a = Math.toRadians(hex(blade));
        double s = size(blade) * 1.1;
        double[] target = add(at(0.0, 1.35, 0.15), mul(aim(pitch), 3.3 * BLOCK));
        double[] quarter = at(2.0 * Math.cos(a), 1.35 + 1.35 * Math.sin(a), 1.1);
        double[] in = normalize(sub(target, quarter));
        double[] near = add(target, add(mul(in, -0.4 * BLOCK), at(0.14 * Math.cos(a), 0.12 * Math.sin(a), 0.0)));
        Tip spread = px(quarter, in, AHEAD, s);
        Tip impaled = px(near, in, AHEAD, s);
        Tip apart = px(add(quarter, at(0.0, 0.0, -0.3)), in, AHEAD, s * 0.95);
        return keys(age, new Key(0.0, rest, SMOOTH), new Key(blow.impact - 3.5, spread, SMOOTH), new Key(blow.impact - 2.0, spread, SMOOTH),
            new Key(blow.impact, impaled, IN), new Key(blow.impact + 1.5, impaled, SMOOTH), new Key(blow.impact + 3.5, apart, OUT),
            new Key(blow.duration, rest, SMOOTH));
    }

    // ================================================================== 1.0.34: the reworked six's own poses

    /**
     * Unsent through one of the reworked abilities: drawn a little ahead of him and up, points toward what he faces, ready, by
     * {@code ready} (0..1) of the way, eased in over its first ticks and out over its last ({@code ticks} long; 0: held, never out).
     */
    static Tip poised(int blade, Tip rest, double age, double ticks, double ready) {
        double in = smooth(clamp01(age / 6.0));
        double out = ticks > 0.0 ? 1.0 - smooth(clamp01((age - (ticks - 8.0)) / 8.0)) : 1.0;
        double k = in * out * ready;
        int r = right(blade);
        return nudge(rest, -0.15 * r * k, 0.12 * k, 0.45 * k);
    }

    // ================================================================== G: Ascent

    /** Blade {@code blade} as a feather of his wing in the air, {@code width} as wide as at full, {@code flex} up or down. */
    static Tip wing(int blade, double width, double flex) {
        int r = right(blade);
        return p(by(blade, 1.6, 2.05, 1.5) * width * r, by(blade, 2.25 + flex, 1.55 - flex * 0.5, 0.8 - flex), by(blade, -1.15, -1.25, -1.3),
            way(by(blade, 0.45, 0.4, 0.35) * r, by(blade, 0.25, 0.0, -0.15), -1.0), UP, size(blade));
    }

    /**
     * Up: drawn in close to his back, then out as one great living wing each side, the upper feather rising, the great one straight out,
     * the lower falling; held streamlined, the points trailing, banking with his turns (the inside wing drawn in), narrowing as he speeds.
     */
    static Tip ascent(int blade, Tip rest, double age, double time, double lag, double run) {
        int r = right(blade);
        double bank = Math.max(-1.0, Math.min(1.0, lag * 3.0)) * r;
        double width = (1.0 - 0.22 * bank) * (1.0 - 0.2 * run);
        Tip close = p(0.45 * r, by(blade, 1.6, 1.35, 1.0), -0.45, way(0.2 * r, by(blade, 0.6, 0.0, -0.5), -0.6), UP, size(blade) * 0.9);
        Tip wing = wing(blade, width, Math.sin(time * 0.17 + blade) * 0.06);
        return keys(age, new Key(0.0, rest, SMOOTH), new Key(2.5, close, SMOOTH), new Key(7.0, wing, OUT), new Key(1.0E9, wing, SMOOTH));
    }

    /** The dive: folded back close, then every point turned down along the dive before him. */
    static Tip dive(int blade, double age, double pitch) {
        int r = right(blade);
        double[] along = aim(Math.min(pitch, -0.5));
        Tip folded = p(0.5 * r, by(blade, 1.95, 1.4, 0.8), -0.6, way(0.2 * r, -0.2, -1.0), UP, size(blade));
        double[] from = at(by(blade, 0.4, 0.55, 0.42) * r, by(blade, 1.3, 1.05, 0.75), 0.15);
        Tip spear = px(add(from, mul(along, by(blade, 1.2, 1.35, 0.95) * BLOCK)), along, way(r, 0, 0), size(blade));
        return keys(age, new Key(0.0, wing(blade, 1.0, 0.0), SMOOTH), new Key(3.0, folded, SMOOTH), new Key(7.0, spear, OUT), new Key(1.0E9, spear, SMOOTH));
    }

    /** Landing: spread, points down, struck into the ground all round him (the upper pair before him, the great at his sides, the lower behind). */
    static Tip landing(int blade, Tip rest, double age) {
        int r = right(blade);
        double s = size(blade) * 1.1;
        double x = by(blade, 1.2, 1.65, 1.2) * r;
        double z = by(blade, 1.35, 0.1, -1.1);
        Tip spread = p(x * 1.05, by(blade, 1.5, 1.2, 0.9), z, way(0.15 * r, -1.0, 0.2 * Math.signum(z)), UP, s);
        Tip struck = p(x, -0.15, z, way(0.1 * r, -1.0, 0.15 * Math.signum(z)), UP, s);
        Tip bent = p(x * 0.97, -0.05, z * 0.95, way(0.1 * r, -1.0, 0.1 * Math.signum(z)), UP, s);
        Tip free = p(by(blade, 1.1, 1.5, 1.2) * r, by(blade, 2.1, 1.45, 0.6), 0.2, way(-0.2 * r, 0.1, 1.0), UP, size(blade));
        return keys(age, new Key(0.0, spread, SMOOTH), new Key(2.0, struck, IN), new Key(4.5, bent, SMOOTH), new Key(8.0, free, OUT),
            new Key(PeaceRules.LAND_TICKS, rest, SMOOTH));
    }

    // ================================================================== what cuts

    /**
     * True if blade {@code blade} cuts in {@code move} at {@code level} at {@code age}: what is in its way then is struck. Each move
     * cuts only on the ticks its blades are seen to strike.
     */
    public static boolean cutting(int blade, KingMove move, int level, double age) {
        // 1.0.34: only the M1's blows cut along their paths now: the reworked six strike what their blades are sent into, by the server.
        if (move == KingMove.BLADES) {
            PeaceStrike blow = PeaceStrike.byOrdinal(level);
            return blow.uses(blade) && blow.cutting(age);
        }
        return false;
    }

    /** The far end of blade {@code blade}'s long point, from its heart (body pixels): what cuts reaches this far along its way. */
    public static double[] point(Tip tip, int blade) {
        return add(tip.at(), mul(tip.dir(), PeaceModel.longPoint(blade) * tip.size()));
    }
}
