package com.hex.kagune.control.bikaku;

import com.hex.kagune.control.graft.GraftMesh;

/**
 * 3.6.2 (the owner: "the fourth before the Kakuja should be like the player no longer even like walks with their own legs, like they get
 * like two separate appendages on either, like both sides emerging from the same spot as the worms, that when the player walks his body
 * doesn't move, but like he controls these tentacles that walk around adapt to terrain. Being able to climb anything, like Dr. Octopus
 * pretty much."): The Walker's two walking tentacles (pure). Where each one's foot is planted on the blocks round him (the ground, a
 * step, a wall he climbs), when it steps and where to, one at a time as he goes; and each tentacle's shape from the worms' root, arching
 * out over his side and down onto its foot, its mouth biting the block it stands on.
 *
 * <p>3.7.1 (the owner: "the walker should lift the player way higher up (like 3-4 blocks up)  and adapt better to literally any terrain";
 * "the walker is the first tap K half kakuja, the full kakuja is the hold K"): the legs carry him {@link #HEIGHT} blocks up, standing wide
 * and reaching far; how high he is carried over the ground round him ({@link #ride}), how fast he is brought there ({@link #lift}), and
 * what he climbs ({@link #climbable}: a wall, never the underside of a roof, which he is let down under).
 *
 * <p>4.0.0 (the owner: "The walker legs needs more fluidity and better arching, it walks super robotic, sure it works but it's super ugly
 * to see, the legs look like sticks, and dont move naturally"): a step takes longer and is eased in and out (the foot lifting quickly,
 * carried, set down softly, higher the longer the stride), the next foot going the moment the last lands; each leg one smooth arch (up and
 * out of his hip, over, and down onto its foot along the face it stands on), lifting higher while its foot is in the air, a slow wave
 * running down it; drawn thick at the hip, tapering to the foot ({@code MawWorm}'s leg).
 */
public final class WalkerLegs {
    public static final int LEGS = 2;
    /** How far out to his side (blocks, from his middle) a foot is planted at rest, and how far ahead of him. */
    public static final double OUT = 2.1;
    public static final double AHEAD = 0.35;
    /** A stepping foot is put down this many ticks of his going ahead of him. */
    public static final double LEAD = 6.0;
    /** A planted foot this far from where it would be put now steps (one at a time); further than {@link #REACH} from the root, at once. */
    public static final double STEP = 1.1;
    public static final double REACH = 6.6;
    /** A step: how long (ticks) and how high its arc (off the face it leaves and lands on), and how much higher for each block of stride. */
    public static final double STEP_TICKS = 8.0;
    public static final double ARC = 0.7;
    public static final double ARC_PER_BLOCK = 0.22;
    /** How far below his feet and above them a foot looks for ground; how far out from the root for a wall. */
    static final double DOWN = 2.6 + 3.5;
    static final double UP = 1.3;
    static final double WALL = 3.0;
    /** How thick a walking tentacle is (the worm's is 1). */
    public static final double GIRTH = 0.85;

    /**
     * 3.7.1 (the owner: "the walker should lift the player way higher up (like 3-4 blocks up)  and adapt better to literally any terrain"):
     * how high his feet are carried over the ground (blocks); how far round him the ground he is carried over is felt (blocks, from his
     * middle); how far above his feet ground still counts (he is carried up onto it; higher, he climbs it); the room kept over his head.
     */
    public static final double HEIGHT = 3.5;
    static final double FEEL = 1.2;
    static final double RIDE_UP = 2.5;
    static final double HEADROOM = 2.0;

    private WalkerLegs() {
    }

    /** The blocks round him: whether the block at (x, y, z) is solid to stand on or grip. */
    public interface Blocks {
        boolean solid(int x, int y, int z);
    }

    /**
     * 3.7.1: how high his feet should be carried now (the world's y): {@link #HEIGHT} over the highest ground under and round him (within
     * {@link #FEEL}: a step, a rock, a ledge, a roof, the top of anything up to {@link #RIDE_UP} above his feet; down to {@link #DOWN} below
     * them), lowered so his head keeps {@link #HEADROOM} under a ceiling (never below that ground); NaN with no ground in the legs' reach
     * (he falls).
     */
    public static double ride(double[] feet, Blocks blocks) {
        // A roof right over his own head (a solid block with open air under it): nothing above it can be stood on from here.
        int x0 = (int)Math.floor(feet[0]);
        int z0 = (int)Math.floor(feet[2]);
        double over = Double.POSITIVE_INFINITY;
        for (int y = (int)Math.floor(feet[1]) + 1; y <= (int)Math.floor(feet[1] + RIDE_UP + HEIGHT + HEADROOM); y++) {
            if (blocks.solid(x0, y, z0)) {
                if (!blocks.solid(x0, y - 1, z0)) {
                    over = y;
                }
                break;
            }
        }
        // Each of the nine columns felt: the first block from RIDE_UP above his feet down to DOWN below with open air on top (and under
        // that roof); the highest of them is what he is carried over.
        double[] tops = new double[9];
        double top = Double.NEGATIVE_INFINITY;
        int high = (int)Math.floor(feet[1] + RIDE_UP);
        int low = (int)Math.floor(feet[1] - DOWN);
        for (int c = 0; c < 9; c++) {
            int x = (int)Math.floor(feet[0] + (c / 3 - 1) * FEEL);
            int z = (int)Math.floor(feet[2] + (c % 3 - 1) * FEEL);
            tops[c] = Double.NEGATIVE_INFINITY;
            for (int y = high; y >= low; y--) {
                if (blocks.solid(x, y, z) && !blocks.solid(x, y + 1, z) && y + 1.0 <= over) {
                    tops[c] = y + 1.0;
                    top = Math.max(top, y + 1.0);
                    break;
                }
            }
        }
        if (top == Double.NEGATIVE_INFINITY) {
            return Double.NaN;
        }
        // A ceiling over him or about to be (a solid block with open air under it, above his feet, in any of the columns felt), lowering
        // him; a wall or a trunk beside him (solid on down) is not one, nor what a column was found to be stood on.
        double carried = top + HEIGHT;
        int roof = (int)Math.floor(carried + HEADROOM);
        for (int c = 0; c < 9; c++) {
            int x = (int)Math.floor(feet[0] + (c / 3 - 1) * FEEL);
            int z = (int)Math.floor(feet[2] + (c % 3 - 1) * FEEL);
            for (int y = (int)Math.floor(Math.min(feet[1], carried)) + 1; y <= roof; y++) {
                if (blocks.solid(x, y, z)) {
                    if (!blocks.solid(x, y - 1, z) && tops[c] != y + 1.0) {
                        carried = Math.min(carried, y - HEADROOM);
                    }
                    break;
                }
            }
        }
        return Math.max(top, carried);
    }

    /** 3.7.1: how fast (blocks a tick) he is brought to the height he is carried at: a spring, no faster up than this, nor down. */
    static final double SPRING = 0.3;
    public static final double RISE = 0.4;
    public static final double SINK = 0.45;
    /** 3.7.1: how long (ticks) the walking tentacles take to grow out of him (and go back in). */
    public static final double GROW = 14.0;

    /** 3.7.1: his upward speed (blocks a tick) to be carried from {@code y} toward {@code ride} ({@link #ride}). */
    public static double lift(double y, double ride) {
        return Math.max(-SINK, Math.min(RISE, (ride - y) * SPRING));
    }

    /**
     * 3.7.1: a block before his body he climbs: solid, with solid under it (a wall going down to the ground). The underside of a roof (open
     * under it) is not: he is let down under it ({@link #ride}).
     */
    public static boolean climbable(Blocks blocks, int x, int y, int z) {
        return blocks.solid(x, y, z) && blocks.solid(x, y - 1, z);
    }

    /** Where his two feet are (the world), stepping or planted; one per player, kept by whoever draws him. */
    public static final class Legs {
        final double[][] foot = new double[LEGS][];
        final double[][] face = new double[LEGS][];
        final double[][] from = new double[LEGS][];
        final double[][] fromFace = new double[LEGS][];
        final double[] stepAt = {Double.NaN, Double.NaN};
        double[] lastFeet;
        double last = Double.NaN;
    }

    /** A foothold: where, and the face's outward normal (up for the ground, out of a wall for a wall). */
    public record Hold(double[] at, double[] face) {
    }

    /**
     * Moves his feet on to {@code time} (ticks): his feet at {@code feet} (the world), facing {@code ahead} with {@code right} to his right
     * (both level, unit), going {@code going} a tick; {@code climbing} when he is going up a wall before him.
     */
    public static void update(Legs legs, double[] feet, double[] right, double[] ahead, double[] going, boolean climbing, double time, Blocks blocks) {
        boolean fresh = Double.isNaN(legs.last) || time < legs.last || time - legs.last > 40.0
            || legs.lastFeet == null || GraftMesh.length3(GraftMesh.sub(feet, legs.lastFeet)) > 6.0;
        legs.last = time;
        legs.lastFeet = feet.clone();
        double[] hip = GraftMesh.add(feet, new double[] {0.0, MawMotion.ROOT[1], 0.0});
        if (fresh) {
            for (int i = 0; i < LEGS; i++) {
                Hold hold = hold(i, feet, right, ahead, new double[3], climbing, blocks);
                legs.foot[i] = hold == null ? dangle(i, feet, right) : hold.at();
                legs.face[i] = hold == null ? new double[] {0.0, 1.0, 0.0} : hold.face();
                legs.stepAt[i] = Double.NaN;
            }
            return;
        }
        for (int i = 0; i < LEGS; i++) {
            if (!Double.isNaN(legs.stepAt[i]) && time >= legs.stepAt[i] + STEP_TICKS) {
                legs.stepAt[i] = Double.NaN;
            }
        }
        // The foot furthest from where it would be put now steps first; the other waits for it to land (unless it is stretched too far).
        double[] off = new double[LEGS];
        Hold[] holds = new Hold[LEGS];
        for (int i = 0; i < LEGS; i++) {
            holds[i] = hold(i, feet, right, ahead, going, climbing, blocks);
            double[] wanted = holds[i] == null ? dangle(i, feet, right) : holds[i].at();
            off[i] = GraftMesh.length3(GraftMesh.sub(legs.foot[i], wanted));
        }
        int first = off[1] > off[0] ? 1 : 0;
        for (int n = 0; n < LEGS; n++) {
            int i = n == 0 ? first : 1 - first;
            if (!Double.isNaN(legs.stepAt[i])) {
                continue;
            }
            boolean stretched = GraftMesh.length3(GraftMesh.sub(legs.foot[i], hip)) > REACH;
            boolean otherStepping = !Double.isNaN(legs.stepAt[1 - i]);
            if (off[i] > STEP && (!otherStepping || stretched)) {
                legs.from[i] = foot(legs, i, time);
                legs.fromFace[i] = legs.face[i];
                legs.foot[i] = holds[i] == null ? dangle(i, feet, right) : holds[i].at();
                legs.face[i] = holds[i] == null ? new double[] {0.0, 1.0, 0.0} : holds[i].face();
                legs.stepAt[i] = time;
            }
        }
    }

    /** True while foot {@code i} is in the air, stepping. */
    public static boolean stepping(Legs legs, int i, double time) {
        return !Double.isNaN(legs.stepAt[i]) && time < legs.stepAt[i] + STEP_TICKS;
    }

    /** Where foot {@code i} is at {@code time} (the world): planted, or on its arc from the last hold to the next. */
    public static double[] foot(Legs legs, int i, double time) {
        if (legs.foot[i] == null) {
            return null;
        }
        if (!stepping(legs, i, time) || legs.from[i] == null) {
            return legs.foot[i].clone();
        }
        double t = GraftMesh.clamp01((time - legs.stepAt[i]) / STEP_TICKS);
        double[] lift = GraftMesh.normalize(GraftMesh.add(legs.fromFace[i], legs.face[i]));
        double stride = GraftMesh.length3(GraftMesh.sub(legs.foot[i], legs.from[i]));
        return GraftMesh.add(GraftMesh.lerp(legs.from[i], legs.foot[i], carried(t)), GraftMesh.mul(lift, (ARC + ARC_PER_BLOCK * Math.min(3.0, stride)) * raised(t)));
    }

    /** 4.0.0: how far along its way a stepping foot is, {@code t} of the way through the step: eased in and out (quintic), never snapping. */
    public static double carried(double t) {
        double x = GraftMesh.clamp01(t);
        return x * x * x * (x * (x * 6.0 - 15.0) + 10.0);
    }

    /** 4.0.0: how high it is off the faces (0..1): up quickly, held, set down softly (the top a little before halfway). */
    public static double raised(double t) {
        return Math.sin(Math.PI * Math.pow(GraftMesh.clamp01(t), 0.8));
    }

    /** 4.0.0: how far through its step foot {@code i} is at {@code time} (0..1), or NaN while it is planted. */
    public static double progress(Legs legs, int i, double time) {
        return stepping(legs, i, time) ? GraftMesh.clamp01((time - legs.stepAt[i]) / STEP_TICKS) : Double.NaN;
    }

    /** The face foot {@code i} stands on (or is stepping to). */
    public static double[] face(Legs legs, int i) {
        return legs.face[i] == null ? new double[] {0.0, 1.0, 0.0} : legs.face[i].clone();
    }

    /**
     * Where foot {@code i} (0 out to his right, 1 to his left) would be put now: on the ground out to its side and a little ahead (ahead of
     * him as he goes), up a step or down off an edge; failing that, on a wall out that way; climbing, on the wall before him first.
     */
    public static Hold hold(int i, double[] feet, double[] right, double[] ahead, double[] going, boolean climbing, Blocks blocks) {
        double side = i == 0 ? 1.0 : -1.0;
        double[] level = {going[0], 0.0, going[2]};
        double[] lead = GraftMesh.mul(level, LEAD);
        double[] home = GraftMesh.add(GraftMesh.add(feet, GraftMesh.mul(right, side * OUT)), GraftMesh.add(GraftMesh.mul(ahead, AHEAD), lead));
        double[] hip = GraftMesh.add(feet, new double[] {0.0, MawMotion.ROOT[1], 0.0});
        if (climbing) {
            double[] from = GraftMesh.add(hip, new double[] {0.0, 0.45 + Math.max(0.0, going[1]) * LEAD, 0.0});
            Hold wall = wall(from, GraftMesh.add(GraftMesh.mul(ahead, 0.9), GraftMesh.mul(right, side * 0.5)), blocks);
            if (wall != null) {
                return wall;
            }
        }
        Hold ground = ground(home, blocks);
        if (ground != null && GraftMesh.length3(GraftMesh.sub(ground.at(), hip)) <= REACH) {
            return ground;
        }
        Hold wall = wall(hip, GraftMesh.add(GraftMesh.mul(right, side), GraftMesh.add(GraftMesh.mul(ahead, 0.3), GraftMesh.mul(level, 2.0))), blocks);
        if (wall != null) {
            return wall;
        }
        return ground;
    }

    /** The top of the highest block to stand on under {@code home}, within {@link #UP} above it and {@link #DOWN} below. */
    static Hold ground(double[] home, Blocks blocks) {
        int x = (int)Math.floor(home[0]);
        int z = (int)Math.floor(home[2]);
        int top = (int)Math.floor(home[1] + UP);
        int bottom = (int)Math.floor(home[1] - DOWN);
        for (int y = top; y >= bottom; y--) {
            if (blocks.solid(x, y, z) && !blocks.solid(x, y + 1, z)) {
                return new Hold(new double[] {home[0], y + 1.0, home[2]}, new double[] {0.0, 1.0, 0.0});
            }
        }
        return null;
    }

    /** The first block face met going from {@code from} along {@code way} (levelled) as far as {@link #WALL}: the point on it, its normal. */
    static Hold wall(double[] from, double[] way, Blocks blocks) {
        double[] dir = GraftMesh.normalize(new double[] {way[0], 0.0, way[2]});
        if (GraftMesh.length3(new double[] {way[0], 0.0, way[2]}) < 1.0E-6) {
            return null;
        }
        double[] last = from;
        int[] lastCell = cell(from);
        if (blocks.solid(lastCell[0], lastCell[1], lastCell[2])) {
            return null;
        }
        for (double d = 0.1; d <= WALL + 1.0E-9; d += 0.1) {
            double[] p = GraftMesh.add(from, GraftMesh.mul(dir, d));
            int[] c = cell(p);
            if (blocks.solid(c[0], c[1], c[2])) {
                double[] normal = {lastCell[0] - c[0], lastCell[1] - c[1], lastCell[2] - c[2]};
                if (Math.abs(normal[0]) + Math.abs(normal[1]) + Math.abs(normal[2]) != 1.0) {
                    normal = new double[] {-Math.signum(dir[0]) * (Math.abs(dir[0]) >= Math.abs(dir[2]) ? 1 : 0), 0.0,
                        -Math.signum(dir[2]) * (Math.abs(dir[2]) > Math.abs(dir[0]) ? 1 : 0)};
                }
                // On the face itself (the boundary between the last open cell and this one).
                double[] at = last.clone();
                for (int k = 0; k < 3; k++) {
                    if (normal[k] > 0.5) {
                        at[k] = c[k] + 1.0;
                    } else if (normal[k] < -0.5) {
                        at[k] = c[k];
                    }
                }
                return new Hold(at, normal);
            }
            last = p;
            lastCell = c;
        }
        return null;
    }

    private static int[] cell(double[] p) {
        return new int[] {(int)Math.floor(p[0]), (int)Math.floor(p[1]), (int)Math.floor(p[2])};
    }

    /** Nothing to stand on: the foot hangs out to its side, below his root. */
    static double[] dangle(int i, double[] feet, double[] right) {
        double side = i == 0 ? 1.0 : -1.0;
        return GraftMesh.add(GraftMesh.add(feet, GraftMesh.mul(right, side * OUT * 0.8)), new double[] {0.0, -1.6, 0.0});
    }

    /** Walking tentacle {@code i}'s shape, its foot in the air ({@code stepping}: halfway through the step) or planted. */
    public static MawMotion.Shape shape(int i, double[] foot, double[] face, boolean stepping, double time, int seed) {
        return shape(i, foot, face, stepping ? 0.5 : Double.NaN, time, seed);
    }

    /**
     * Walking tentacle {@code i}'s shape (his body's frame: x right, y up, z ahead), its foot {@code step} of the way through a step (NaN:
     * planted). 4.0.0: one smooth arch out of the worms' root: up and out over his hip, over, and down onto {@code foot}, its last stretch
     * coming in along the face it stands on ({@code face}); the further the foot, the higher and wider it arches, higher still while the
     * foot is in the air; a slow wave running down it; its mouth opening as the foot lifts and shutting on the block as it lands.
     */
    public static MawMotion.Shape shape(int i, double[] foot, double[] face, double step, double time, int seed) {
        double side = i == 0 ? 1.0 : -1.0;
        double[] root = MawMotion.ROOT;
        double[] out = {root[0] + side * 0.3, root[1] + 0.06, root[2] - 0.22};
        double[] normal = GraftMesh.normalize(face);
        double reach = GraftMesh.length3(GraftMesh.sub(foot, out));
        double lifted = Double.isNaN(step) ? 0.0 : Math.sin(Math.PI * GraftMesh.clamp01(step));
        double sway = Math.sin(time * 0.11 + seed * 1.7 + i * 2.1);
        // Leaving the hip up and out (a little back), as a spider's leg stands; coming down onto the foot from over it, a little outside it
        // on the ground (the arch's top out past the foot), straight in along a wall's face.
        double rise = 0.55 + 0.3 * reach + 0.4 * lifted;
        double[] p1 = GraftMesh.add(out, GraftMesh.mul(GraftMesh.normalize(new double[] {side * (0.95 + 0.06 * sway), 1.0, -0.22}), rise));
        double drop = 0.5 + 0.36 * reach + 0.3 * lifted;
        double[] p2 = GraftMesh.add(foot, GraftMesh.mul(normal, drop));
        p2 = GraftMesh.add(p2, new double[] {side * 0.16 * reach * Math.abs(normal[1]), 0.0, 0.0});
        int n = 10;
        double[][] path = new double[n + 2][];
        path[0] = root.clone();
        for (int k = 0; k <= n; k++) {
            double u = k / (double)n;
            double a = 1.0 - u;
            double[] p = GraftMesh.add(GraftMesh.add(GraftMesh.mul(out, a * a * a), GraftMesh.mul(p1, 3.0 * a * a * u)),
                GraftMesh.add(GraftMesh.mul(p2, 3.0 * a * u * u), GraftMesh.mul(foot, u * u * u)));
            // A slow wave down it (none at its ends: they stay on the hip and the foot).
            double wave = 0.07 * Math.sin(time * 0.16 + seed * 1.3 + i * 2.0 - u * 5.5) * 4.0 * u * a * (1.0 - 0.6 * lifted);
            p = GraftMesh.add(p, new double[] {side * 0.45 * wave, 0.85 * wave, 0.3 * wave});
            path[k + 1] = k == n ? foot.clone() : p;
        }
        // Its back faces out from him on the ground, up on a wall (never along the way it comes in).
        double[] dorsal = Math.abs(face[1]) > 0.5 ? new double[] {side, 0.0, 0.0} : new double[] {0.0, 1.0, 0.0};
        double open = Double.isNaN(step) ? 0.04 : 0.04 + 0.5 * Math.sin(Math.PI * Math.min(1.0, GraftMesh.clamp01(step) * 1.15));
        return new MawMotion.Shape(path, dorsal, open);
    }
}
