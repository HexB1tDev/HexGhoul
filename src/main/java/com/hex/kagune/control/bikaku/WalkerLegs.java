package com.hex.kagune.control.bikaku;

import com.hex.kagune.control.graft.GraftMesh;

/**
 * 3.6.2 (the owner: "the fourth before the Kakuja should be like the player no longer even like walks with their own legs, like they get
 * like two separate appendages on either, like both sides emerging from the same spot as the worms, that when the player walks his body
 * doesn't move, but like he controls these tentacles that walk around adapt to terrain. Being able to climb anything, like Dr. Octopus
 * pretty much."): The Walker's two walking tentacles (pure). Where each one's foot is planted on the blocks round him (the ground, a
 * step, a wall he climbs), when it steps and where to, one at a time as he goes; and each tentacle's shape from the worms' root, arching
 * out over his side and down onto its foot, its mouth biting the block it stands on.
 */
public final class WalkerLegs {
    public static final int LEGS = 2;
    /** How far out to his side (blocks, from his middle) a foot is planted at rest, and how far ahead of him. */
    public static final double OUT = 1.35;
    public static final double AHEAD = 0.2;
    /** A stepping foot is put down this many ticks of his going ahead of him. */
    public static final double LEAD = 6.0;
    /** A planted foot this far from where it would be put now steps (one at a time); further than {@link #REACH} from the root, at once. */
    public static final double STEP = 0.85;
    public static final double REACH = 3.2;
    /** A step: how long (ticks) and how high its arc (off the face it leaves and lands on). */
    public static final double STEP_TICKS = 5.0;
    public static final double ARC = 0.55;
    /** How far below his feet and above them a foot looks for ground; how far out from the root for a wall. */
    static final double DOWN = 2.6;
    static final double UP = 1.3;
    static final double WALL = 2.4;
    /** How thick a walking tentacle is (the worm's is 1). */
    public static final double GIRTH = 0.72;

    private WalkerLegs() {
    }

    /** The blocks round him: whether the block at (x, y, z) is solid to stand on or grip. */
    public interface Blocks {
        boolean solid(int x, int y, int z);
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
        double e = GraftMesh.smooth(t);
        double[] lift = GraftMesh.normalize(GraftMesh.add(legs.fromFace[i], legs.face[i]));
        return GraftMesh.add(GraftMesh.lerp(legs.from[i], legs.foot[i], e), GraftMesh.mul(lift, ARC * Math.sin(Math.PI * t)));
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
        return GraftMesh.add(GraftMesh.add(feet, GraftMesh.mul(right, side * OUT * 0.8)), new double[] {0.0, -0.3, 0.0});
    }

    /**
     * Walking tentacle {@code i}'s shape (his body's frame: x right, y up, z ahead): out of the worms' root to its side, arching up and out
     * over his hip and down onto {@code foot}, its last part coming in along the face it stands on ({@code face}); its mouth shut on the
     * block ({@code open} while it steps).
     */
    public static MawMotion.Shape shape(int i, double[] foot, double[] face, boolean stepping, double time, int seed) {
        double side = i == 0 ? 1.0 : -1.0;
        double[] root = MawMotion.ROOT;
        double[] out = {root[0] + side * 0.28, root[1] + 0.08, root[2] - 0.2};
        double[] toFoot = GraftMesh.sub(foot, out);
        double reach = GraftMesh.length3(toFoot);
        double sway = 0.06 * Math.sin(time * 0.11 + seed * 1.7 + i * 2.1);
        double[] knee = GraftMesh.add(GraftMesh.lerp(out, foot, 0.42), new double[] {side * (0.42 + sway), 0.5 + 0.22 * reach, -0.12});
        double[] over = GraftMesh.add(foot, GraftMesh.mul(GraftMesh.normalize(face), 0.55 + 0.1 * reach));
        double[] down = GraftMesh.add(foot, GraftMesh.mul(GraftMesh.normalize(face), 0.18));
        double[][] path = {root.clone(), out, knee, GraftMesh.lerp(knee, over, 0.6), over, down, foot.clone()};
        // Its back faces out from him on the ground, up on a wall (never along the way it comes in).
        double[] dorsal = Math.abs(face[1]) > 0.5 ? new double[] {side, 0.0, 0.0} : new double[] {0.0, 1.0, 0.0};
        return new MawMotion.Shape(path, dorsal, stepping ? 0.55 : 0.04);
    }
}
