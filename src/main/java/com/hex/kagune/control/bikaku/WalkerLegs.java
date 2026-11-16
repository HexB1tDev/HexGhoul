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
 *
 * <p>4.0.1 (the owner: "the walking is way too weird, bro. It's too robotic, too slow. Like the steps should be like they should vary in
 * length ... it should always be random and like feel like more of a wobble, like a longer distance than the other ... it still feels like
 * it's two sticks walking"): every step its own, by chance ({@link #roll}): how far the foot lets itself be left before it goes (some
 * short, some long), where it lands (a little ahead or behind, in or out), how high it is lifted and how fast it goes (quicker the
 * shorter); the feet in turn, one stride longer than the other as it falls; the middle of each leg on a spring ({@link #wobble}), lagging
 * behind as he goes and shaken as a foot lifts and lands; each leg a tentacle's S, out of his hip up to a high knee and bowing out and
 * down onto its foot; his body swaying over the foot it stands on and dipping as the other goes ({@link #body}).
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
    /**
     * 4.0.1 (the owner: "One leg at a time, not two in front at once ... They don't go back like further back enough or forward enough ...
     * you can always tell where the next step is gonna be, which is what I don't want"): walking, a true stride, one leg at a time: a foot
     * is put down well ahead of him, stays where it is as he goes over it, and only lifts once it is well behind him; the other goes
     * meanwhile. How long a stride is ({@link #STRIDE_BASE}, and {@link #STRIDE_PER_SPEED} more for each block a tick he goes), and each
     * step's by chance: mostly {@link #STRIDE_LEAST} to {@link #STRIDE_MOST} of that, now and then a stutter ({@link #STUTTER}) or a
     * lunge ({@link #LUNGE}); how far behind him it lets itself go before it lifts (half its stride, give or take {@link #LEAVE_SPREAD});
     * how far in or out it lands ({@link #LAND_OUT}); how long a step takes (ticks: {@link #STEP_BASE} and {@link #STEP_PER_BLOCK} for each
     * block it swings, between {@link #STEP_LEAST} and {@link #STEP_MOST}, give or take a little).
     */
    public static final double STRIDE_BASE = 1.6;
    public static final double STRIDE_PER_SPEED = 8.0;
    public static final double STRIDE_LEAST = 0.7;
    public static final double STRIDE_MOST = 1.3;
    public static final double STUTTER = 0.45;
    public static final double LUNGE = 1.55;
    public static final double LEAVE_SPREAD = 0.3;
    public static final double LAND_OUT = 0.45;
    public static final double STEP_BASE = 3.0;
    public static final double STEP_PER_BLOCK = 0.9;
    public static final double STEP_LEAST = 3.5;
    public static final double STEP_MOST = 9.0;
    /** 4.0.1: slower than this (blocks a tick) he is standing: a foot steps only to put itself back beside him, once this far off. */
    public static final double STANDING = 0.02;
    public static final double SETTLE = 0.9;
    /** 4.0.1: the legs' middles' spring: how stiff, how damped (a tick), how far it lags behind his going, the most it swings (blocks). */
    static final double WOBBLE_STIFF = 0.2;
    static final double WOBBLE_DAMP = 0.32;
    static final double WOBBLE_LAG = 3.2;
    static final double WOBBLE_MOST = 1.1;
    /** How far below his feet and above them a foot looks for ground; how far out from the root for a wall. */
    static final double DOWN = 2.6 + 3.5;
    static final double UP = 1.3;
    static final double WALL = 3.0;
    /** How thick a walking tentacle is (the worm's is 1). */
    public static final double GIRTH = 1.0; // 4.0.1: thicker (the owner: "two sticks")

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
        /** 4.0.1: each foot's step: how long it takes, how high it goes (and the body dips), how far behind him it lets itself go next. */
        final double[] stepTicks = {STEP_TICKS, STEP_TICKS};
        final double[] lift = {1.0, 1.0};
        final double[] stride = {STEP, STEP};
        /** 4.0.1: each leg's middle on its spring (the world): how far it is swung, how fast. */
        final double[][] swing = {{0.0, 0.0, 0.0}, {0.0, 0.0, 0.0}};
        final double[][] swingSpeed = {{0.0, 0.0, 0.0}, {0.0, 0.0, 0.0}};
        /** 4.0.1: how many steps so far (each one's chances from it and the seed); which foot went last. */
        int steps;
        int lastFoot = -1;
        long seed;

        public Legs() {
        }

        /** 4.0.1: one whose chances are its own ({@code seed}: its player's). */
        public Legs(long seed) {
            this.seed = seed;
        }
    }

    /** 4.0.1: a number in [0, 1) for step {@code n}'s chance {@code k} on {@code legs} (the same every time it is asked). */
    static double roll(Legs legs, int n, int k) {
        long z = legs.seed * 0x9E3779B97F4A7C15L + n * 0xBF58476D1CE4E5B9L + k * 0x94D049BB133111EBL + 0x2545F4914F6CDD1DL;
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        z ^= z >>> 31;
        return (z >>> 11) * 0x1.0p-53;
    }

    /** A foothold: where, and the face's outward normal (up for the ground, out of a wall for a wall). */
    public record Hold(double[] at, double[] face) {
    }

    /**
     * Moves his feet on to {@code time} (ticks): his feet at {@code feet} (the world), facing {@code ahead} with {@code right} to his right
     * (both level, unit), going {@code going} a tick; {@code climbing} when he is going up a wall before him.
     */
    public static void update(Legs legs, double[] feet, double[] right, double[] ahead, double[] going, boolean climbing, double time, Blocks blocks) {
        double before = legs.last;
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
                legs.swing[i] = new double[3];
                legs.swingSpeed[i] = new double[3];
            }
            legs.lastFoot = -1;
            return;
        }
        double dt = Math.max(0.0, Math.min(2.0, time - before));
        for (int i = 0; i < LEGS; i++) {
            if (!Double.isNaN(legs.stepAt[i]) && time >= legs.stepAt[i] + legs.stepTicks[i]) {
                legs.stepAt[i] = Double.NaN;
                // 4.0.1: it lands: the leg's middle is thrown down and out by it.
                double side = i == 0 ? 1.0 : -1.0;
                legs.swingSpeed[i] = GraftMesh.add(legs.swingSpeed[i], GraftMesh.add(new double[] {0.0, -0.2 * legs.lift[i], 0.0},
                    GraftMesh.mul(right, side * 0.08 * legs.lift[i])));
            }
        }
        // 4.0.1: each leg's middle on its spring: lagging behind as he goes, swinging back past where it hangs, settling.
        for (int i = 0; i < LEGS; i++) {
            double[] target = GraftMesh.mul(going, -WOBBLE_LAG);
            double[] pull = GraftMesh.sub(GraftMesh.mul(GraftMesh.sub(target, legs.swing[i]), WOBBLE_STIFF), GraftMesh.mul(legs.swingSpeed[i], WOBBLE_DAMP));
            legs.swingSpeed[i] = GraftMesh.add(legs.swingSpeed[i], GraftMesh.mul(pull, dt));
            legs.swing[i] = GraftMesh.add(legs.swing[i], GraftMesh.mul(legs.swingSpeed[i], dt));
            double length = GraftMesh.length3(legs.swing[i]);
            if (length > WOBBLE_MOST) {
                legs.swing[i] = GraftMesh.mul(legs.swing[i], WOBBLE_MOST / length);
            }
        }
        double[] off = new double[LEGS];
        Hold[] holds = new Hold[LEGS];
        for (int i = 0; i < LEGS; i++) {
            holds[i] = hold(i, feet, right, ahead, going, climbing, blocks);
            double[] wanted = holds[i] == null ? dangle(i, feet, right) : holds[i].at();
            off[i] = GraftMesh.length3(GraftMesh.sub(legs.foot[i], wanted));
        }
        double speed = Math.hypot(going[0], going[2]);
        double[] way = speed > 1.0E-6 ? new double[] {going[0] / speed, 0.0, going[2] / speed} : ahead;
        // How far ahead of him each foot is, the way he goes (behind: less than nothing).
        double[] along = new double[LEGS];
        for (int i = 0; i < LEGS; i++) {
            along[i] = (legs.foot[i][0] - feet[0]) * way[0] + (legs.foot[i][2] - feet[2]) * way[2];
        }
        // One at a time: the foot further behind goes first (the other is the one he stands on); never both, unless one is stretched.
        int first = along[0] < along[1] ? 0 : along[1] < along[0] ? 1 : legs.lastFoot >= 0 ? 1 - legs.lastFoot : 0;
        for (int n = 0; n < LEGS; n++) {
            int i = n == 0 ? first : 1 - first;
            if (!Double.isNaN(legs.stepAt[i])) {
                continue;
            }
            boolean stretched = GraftMesh.length3(GraftMesh.sub(legs.foot[i], hip)) > REACH;
            boolean otherStepping = !Double.isNaN(legs.stepAt[1 - i]);
            if (otherStepping && !stretched) {
                continue;
            }
            boolean due;
            if (climbing || speed < STANDING) {
                // On a wall, or standing: a foot goes back beside him once it is well off where it would be (the one further off first).
                due = off[i] > (climbing ? STEP : SETTLE) && (n == 0 || off[i] >= off[1 - i] || stretched);
            } else {
                // Walking: the rear foot, once it is as far behind him as it lets itself go this time (or far off its line: he turned).
                due = (i == first && -along[i] > legs.stride[i]) || off[i] > REACH * 0.6 || stretched;
            }
            if (due) {
                step(legs, i, holds[i], feet, right, ahead, going, climbing, time, blocks);
                break;
            }
        }
    }

    /**
     * 4.0.1: foot {@code i} steps now, this step its own by chance: walking, how long a stride (now and then a stutter or a lunge), where
     * it lands (half that ahead of where he will be when it does, in or out a little), how high and fast it goes, how far behind him it
     * lets itself go before its next; on a wall or standing, back to where it would be.
     */
    private static void step(Legs legs, int i, Hold plain, double[] feet, double[] right, double[] ahead, double[] going, boolean climbing, double time,
                             Blocks blocks) {
        int n = legs.steps++;
        double side = i == 0 ? 1.0 : -1.0;
        double speed = Math.hypot(going[0], going[2]);
        double[] hip = GraftMesh.add(feet, new double[] {0.0, MawMotion.ROOT[1], 0.0});
        double[] from = foot(legs, i, time);
        Hold hold = plain;
        double ticks;
        double chance = roll(legs, n, 0);
        double stride = (STRIDE_BASE + STRIDE_PER_SPEED * speed)
            * (chance < 0.1 ? STUTTER : chance > 0.9 ? LUNGE : STRIDE_LEAST + (STRIDE_MOST - STRIDE_LEAST) * roll(legs, n, 1));
        if (!climbing && speed >= STANDING) {
            double[] way = {going[0] / speed, 0.0, going[2] / speed};
            double behind = Math.max(0.0, -((from[0] - feet[0]) * way[0] + (from[2] - feet[2]) * way[2]));
            // Its swing's length (ticks) for the way it has to go, worked out with how far he goes meanwhile.
            ticks = Math.max(STEP_LEAST, Math.min(STEP_MOST, (STEP_BASE + STEP_PER_BLOCK * (stride / 2.0 + behind)) / Math.max(0.2, 1.0 - STEP_PER_BLOCK * speed)));
            double reachAhead = speed * ticks + stride / 2.0;
            double out = side * (roll(legs, n, 2) * 2.0 - 1.0) * LAND_OUT;
            // Never further than the leg reaches (from where his hip will be when it lands).
            for (int k = 0; k < 6; k++) {
                double[] spot = GraftMesh.add(GraftMesh.add(feet, GraftMesh.mul(way, reachAhead)), GraftMesh.mul(right, side * OUT + out));
                double[] lands = GraftMesh.add(hip, GraftMesh.mul(way, speed * ticks));
                double[] nudge = GraftMesh.sub(spot, GraftMesh.add(GraftMesh.add(feet, GraftMesh.mul(right, side * OUT)),
                    GraftMesh.add(GraftMesh.mul(ahead, AHEAD), GraftMesh.mul(new double[] {going[0], 0.0, going[2]}, LEAD))));
                Hold there = hold(i, feet, right, ahead, going, false, blocks, nudge);
                if (there != null && GraftMesh.length3(GraftMesh.sub(there.at(), lands)) <= REACH - 0.4
                    && GraftMesh.length3(GraftMesh.sub(there.at(), hip)) <= REACH) {
                    hold = there;
                    break;
                }
                reachAhead *= 0.75;
            }
        } else {
            ticks = STEP_BASE + STEP_PER_BLOCK * (hold == null ? 1.0 : GraftMesh.length3(GraftMesh.sub(hold.at(), from)));
        }
        legs.from[i] = from;
        legs.fromFace[i] = legs.face[i];
        legs.foot[i] = hold == null ? dangle(i, feet, right) : hold.at();
        legs.face[i] = hold == null ? new double[] {0.0, 1.0, 0.0} : hold.face();
        legs.stepTicks[i] = Math.max(STEP_LEAST, Math.min(STEP_MOST, ticks * (0.88 + 0.24 * roll(legs, n, 3))));
        legs.lift[i] = 0.7 + 0.65 * roll(legs, n, 4);
        // How far behind him it lets itself go before it lifts next: about half this stride, give or take, by chance.
        legs.stride[i] = Math.max(0.3, stride / 2.0 * (1.0 + LEAVE_SPREAD * (roll(legs, n, 5) * 2.0 - 1.0)));
        legs.stepAt[i] = time;
        legs.lastFoot = i;
        // Lifting off, the leg's middle is thrown up and on.
        legs.swingSpeed[i] = GraftMesh.add(legs.swingSpeed[i], GraftMesh.add(new double[] {0.0, 0.14 * legs.lift[i], 0.0}, GraftMesh.mul(ahead, 0.06)));
    }

    /** 4.0.1: how far leg {@code i}'s middle is swung on its spring (the world, blocks). */
    public static double[] wobble(Legs legs, int i) {
        return legs.swing[i].clone();
    }

    /**
     * 4.0.1: how his body rides his steps at {@code time}: {roll (radians, + to his left: over his left foot while his right is up), drop
     * (model pixels, + down: dipping while a foot is up)}, a slow sway on top.
     */
    public static double[] body(Legs legs, double time) {
        double roll = 0.025 * Math.sin(time * 0.13 + (legs.seed & 63L));
        double drop = 0.0;
        for (int i = 0; i < LEGS; i++) {
            double p = progress(legs, i, time);
            if (!Double.isNaN(p)) {
                double s = Math.sin(Math.PI * p) * legs.lift[i];
                roll += (i == 0 ? 1.0 : -1.0) * 0.11 * s;
                drop += 1.2 * s;
            }
        }
        return new double[] {roll, drop};
    }

    /** True while foot {@code i} is in the air, stepping. */
    public static boolean stepping(Legs legs, int i, double time) {
        return !Double.isNaN(legs.stepAt[i]) && time < legs.stepAt[i] + legs.stepTicks[i];
    }

    /** Where foot {@code i} is at {@code time} (the world): planted, or on its arc from the last hold to the next. */
    public static double[] foot(Legs legs, int i, double time) {
        if (legs.foot[i] == null) {
            return null;
        }
        if (!stepping(legs, i, time) || legs.from[i] == null) {
            return legs.foot[i].clone();
        }
        double t = GraftMesh.clamp01((time - legs.stepAt[i]) / legs.stepTicks[i]);
        double[] lift = GraftMesh.normalize(GraftMesh.add(legs.fromFace[i], legs.face[i]));
        double stride = GraftMesh.length3(GraftMesh.sub(legs.foot[i], legs.from[i]));
        return GraftMesh.add(GraftMesh.lerp(legs.from[i], legs.foot[i], carried(t)),
            GraftMesh.mul(lift, (ARC + ARC_PER_BLOCK * Math.min(3.0, stride)) * legs.lift[i] * raised(t)));
    }

    /** 4.0.0: how far along its way a stepping foot is, {@code t} of the way through the step: eased in and out (quintic), never snapping. */
    public static double carried(double t) {
        double x = GraftMesh.clamp01(t);
        return x * x * x * (x * (x * 6.0 - 15.0) + 10.0);
    }

    /** 4.0.0: how high it is off the faces (0..1): up quickly, set down softly (the top a little before halfway; never a snap). */
    public static double raised(double t) {
        double x = GraftMesh.clamp01(t);
        return Math.sin(Math.PI * x * (1.25 - 0.25 * x));
    }

    /** 4.0.0: how far through its step foot {@code i} is at {@code time} (0..1), or NaN while it is planted. */
    public static double progress(Legs legs, int i, double time) {
        return stepping(legs, i, time) ? GraftMesh.clamp01((time - legs.stepAt[i]) / legs.stepTicks[i]) : Double.NaN;
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
        return hold(i, feet, right, ahead, going, climbing, blocks, new double[3]);
    }

    /** As {@link #hold(int, double[], double[], double[], double[], boolean, Blocks)}, its spot on the ground put {@code nudge} off (the world). */
    public static Hold hold(int i, double[] feet, double[] right, double[] ahead, double[] going, boolean climbing, Blocks blocks, double[] nudge) {
        double side = i == 0 ? 1.0 : -1.0;
        double[] level = {going[0], 0.0, going[2]};
        double[] lead = GraftMesh.mul(level, LEAD);
        double[] home = GraftMesh.add(GraftMesh.add(GraftMesh.add(feet, GraftMesh.mul(right, side * OUT)), GraftMesh.add(GraftMesh.mul(ahead, AHEAD), lead)),
            nudge);
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

    /** As {@link #shape(int, double[], double[], double, double, int, double[])}, its middle not swung. */
    public static MawMotion.Shape shape(int i, double[] foot, double[] face, double step, double time, int seed) {
        return shape(i, foot, face, step, time, seed, new double[3]);
    }

    /**
     * Walking tentacle {@code i}'s shape (his body's frame: x right, y up, z ahead), its foot {@code step} of the way through a step (NaN:
     * planted), its middle swung {@code wobble} on its spring (his body's frame). 4.0.1 (the owner: "it still feels like it's two sticks
     * walking"): a tentacle's S: out of the worms' root up and out over his hip to a high knee (leaning the way its foot is, higher the
     * further the foot and while it is up), on out from there and bowing down onto {@code foot}, its last stretch along the face it
     * stands on ({@code face}); swung in the middle, a slow wave running down it; its mouth opening as the foot lifts, shutting as it lands.
     */
    public static MawMotion.Shape shape(int i, double[] foot, double[] face, double step, double time, int seed, double[] wobble) {
        double side = i == 0 ? 1.0 : -1.0;
        double[] root = MawMotion.ROOT;
        double[] out = {root[0] + side * 0.3, root[1] + 0.06, root[2] - 0.22};
        double[] normal = GraftMesh.normalize(face);
        double reach = GraftMesh.length3(GraftMesh.sub(foot, out));
        double lifted = Double.isNaN(step) ? 0.0 : Math.sin(Math.PI * GraftMesh.clamp01(step));
        double sway = Math.sin(time * 0.11 + seed * 1.7 + i * 2.1);
        double[] knee = GraftMesh.add(out, new double[] {side * (0.4 + 0.16 * reach + 0.05 * sway), 0.4 + 0.2 * reach + 0.45 * lifted,
            0.3 * (foot[2] - out[2]) - 0.1});
        // Leaving the hip up and out; level through the knee, going on out (and toward its foot); coming down onto the foot from over it.
        double[] leave = GraftMesh.normalize(new double[] {side * 0.75, 1.0, -0.2});
        double[] across = GraftMesh.sub(foot, knee);
        double flat = Math.max(1.0E-6, Math.hypot(across[0], across[2]));
        double[] through = GraftMesh.normalize(new double[] {side * 0.7 + 0.5 * across[0] / flat, 0.05, 0.5 * across[2] / flat});
        double up = GraftMesh.length3(GraftMesh.sub(knee, out));
        double down = GraftMesh.length3(across);
        double[][] a = {out, GraftMesh.add(out, GraftMesh.mul(leave, 0.45 * up)), GraftMesh.sub(knee, GraftMesh.mul(through, 0.4 * up)), knee};
        double[] over = GraftMesh.add(foot, GraftMesh.mul(normal, 0.3 * down + 0.25 * lifted));
        over = GraftMesh.add(over, new double[] {side * 0.12 * down * Math.abs(normal[1]), 0.0, 0.0});
        double[][] b = {knee, GraftMesh.add(knee, GraftMesh.mul(through, 0.45 * down)), over, foot};
        int n = 7;
        double[][] path = new double[2 * n + 2][];
        path[0] = root.clone();
        for (int k = 0; k <= 2 * n; k++) {
            double[][] seg = k <= n ? a : b;
            double u = k <= n ? k / (double)n : (k - n) / (double)n;
            double v = 1.0 - u;
            double[] p = GraftMesh.add(GraftMesh.add(GraftMesh.mul(seg[0], v * v * v), GraftMesh.mul(seg[1], 3.0 * v * v * u)),
                GraftMesh.add(GraftMesh.mul(seg[2], 3.0 * v * u * u), GraftMesh.mul(seg[3], u * u * u)));
            double g = k / (2.0 * n); // along the whole leg: none at the hip or the foot, the most in the middle
            double hang = Math.sin(Math.PI * g);
            double wave = 0.08 * Math.sin(time * 0.17 + seed * 1.3 + i * 2.0 - g * 6.0) * hang * (1.0 - 0.5 * lifted);
            p = GraftMesh.add(p, GraftMesh.add(GraftMesh.mul(wobble, hang), new double[] {side * 0.45 * wave, 0.85 * wave, 0.3 * wave}));
            path[k + 1] = k == 2 * n ? foot.clone() : p;
        }
        // Its back faces out from him on the ground, up on a wall (never along the way it comes in).
        double[] dorsal = Math.abs(face[1]) > 0.5 ? new double[] {side, 0.0, 0.0} : new double[] {0.0, 1.0, 0.0};
        double open = Double.isNaN(step) ? 0.04 : 0.04 + 0.5 * Math.sin(Math.PI * Math.min(1.0, GraftMesh.clamp01(step) * 1.15));
        return new MawMotion.Shape(path, dorsal, open);
    }
}
