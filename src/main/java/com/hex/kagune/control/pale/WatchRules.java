package com.hex.kagune.control.pale;

/**
 * 6.2.0, Watch This (B; pure, tested): Pale throws her arms out into a cross and two tentacles of her pale flesh burst out of her
 * upper back, one over each shoulder, each ending in a long beak of bone. For exactly {@link #ACTIVE} ticks they hunt on their
 * own: each picks something near her that is hostile to her, rears back and strikes, the beak driven through its chest; lifts it
 * on the beak in a great sweep up over her and out to its own side ({@link #carry}), and throws it away that way, the left
 * tentacle to her left, the right to her right, at the speed the sweep had; and goes back to hunting. Then both draw back into
 * her back.
 *
 * <p>Points are in her frame: blocks to her right (x), up from her feet (y), ahead of her (z). A tentacle's {@code side} is 1 for
 * the one over her right shoulder, -1 for the left.
 */
public final class WatchRules {
    /** What a tentacle is doing (sent by ordinal: new ones go at the end). */
    public enum Phase {
        /** Not out. */
        NONE,
        /** Bursting out of her back. */
        OUT,
        /** Poised over her shoulder, hunting. */
        IDLE,
        /** Rearing back ({@link #REAR} ticks), then the lunge at its target. */
        STRIKE,
        /** The beak through its target's chest: the stab held, the lift, the throw at its end. */
        CARRY,
        /** Following through after the throw, then back to its place. */
        RECOVER,
        /** The lunge found nothing there: back to its place. */
        MISS,
        /** Drawing back into her back. */
        IN;

        public static Phase byOrdinal(int ordinal) {
            Phase[] all = values();
            return ordinal >= 0 && ordinal < all.length ? all[ordinal] : NONE;
        }
    }

    // ------------------------------------------------------------------ timings (ticks)
    /** Her cross: the body's move, all of it. */
    public static final int POSE_TICKS = 26;
    /** The tentacles burst out this far into it, and take this long to be all out. */
    public static final int ERUPT_AT = 4;
    public static final int ERUPT = 6;
    /** They are out exactly ten seconds (from the burst to the moment they start back in), and take this long to go back in. */
    public static final int ACTIVE = 200;
    public static final int RETRACT = 14;
    /** A strike: the rear back, and its lunge ({@link #lungeTicks}). */
    public static final int REAR = 4;
    /** The beak through the body: held a moment, then the lift, thrown at its end. */
    public static final int IMPALE_HOLD = 3;
    public static final int LIFT = 12;
    /** After a throw (the follow-through and back), and after a miss. */
    public static final int RECOVER = 9;
    public static final int MISS_RECOVER = 7;

    // ------------------------------------------------------------------ reach, damage, cost
    /** What is this near her (blocks, chest to chest) can be struck. */
    public static final double RANGE = 9.0;
    /** The furthest (blocks from its {@link #pivot}) a beak reaches: what has got further by the end of the lunge is missed. */
    public static final double REACH = 11.0;
    /** The beak's damage, times a fist's. */
    public static final float DAMAGE = 2.6F;
    public static final float COST = 18.0F;
    /** Ticks after they have gone back in before she can call them again. */
    public static final int COOLDOWN = 300;
    /** A body just thrown is not caught again for this long (ticks): it is let fly. */
    public static final int THROWN_FREE = 30;
    /** A body that hurt her is hostile to her this long after (ticks). */
    public static final int GRUDGE = 200;

    // ------------------------------------------------------------------ the tentacle
    /** The beak: its length (blocks), and how far it comes out past the middle of a body it has gone through. */
    public static final double BEAK = 1.3;
    public static final double THROUGH = 0.55;
    /** Where on a body the beak goes in (a share of its height up from its feet: the chest). */
    public static final double CHEST = 0.62;

    /** The lunge at something {@code distance} blocks from the tentacle's pivot: the further, the longer (ticks). */
    public static int lungeTicks(double distance) {
        return (int)Math.max(2, Math.min(5, Math.round(1.4 + distance * 0.34)));
    }

    /** Ticks from the start of a strike to the beak going in. */
    public static int strikeTicks(int lunge) {
        return REAR + lunge;
    }

    /** The point a tentacle swings what it carries about: above and outside her shoulder, a little back, where it rears from. */
    public static double[] pivot(int side) {
        return new double[] {side * 0.55, 2.3, -0.3};
    }

    // ------------------------------------------------------------------ the sweep
    /** The sweep's end, as seen from the pivot: out to the tentacle's side and a little behind her (radians from ahead). */
    static final double END_TURN = 1.6;
    /** How high it carries the body over the top (radians above level), and where it lets go (still well up). */
    static final double TOP = 1.32;
    static final double LET_GO = 0.66;
    /** How fast (radians per sweep) it is still coming down where it lets go: the throw goes out, not down. */
    static final double LET_GO_DROP = -1.15;
    /** How far from the pivot (blocks) it hauls the body in over the top, and the length it whips out to at the throw. */
    static final double HAULED = 3.0;
    static final double FLUNG = 5.6;
    /** Where in the sweep it is over the top. */
    static final double OVER = 0.5;

    /**
     * Where a body impaled at {@code start} (her frame when it went in) is {@code u} of the way through the sweep (0 the stab, 1 the
     * throw; past 1 the empty tentacle's follow-through), carried by the tentacle on {@code side}: hauled in and lifted up over her,
     * swung round and out to that side, whipped out long at the end. About the {@link #pivot}: from level with the body (or below
     * it) up over the top and down to {@link #LET_GO} above level on the far side, nearly half a turn.
     */
    public static double[] carry(double u, double[] start, int side) {
        double[] p = pivot(side);
        double dx = start[0] - p[0];
        double dy = start[1] - p[1];
        double dz = start[2] - p[2];
        double r0 = Math.max(0.6, Math.sqrt(dx * dx + dy * dy + dz * dz));
        double turn0 = Math.atan2(dx, dz);
        double rise0 = Math.asin(Math.max(-1.0, Math.min(1.0, dy / r0)));
        double turn1 = side * END_TURN;
        double around = wrap(turn1 - turn0);
        double turn;
        double rise;
        double r;
        if (u <= 1.0) {
            turn = turn0 + around * smooth(u);
            rise = u < OVER ? hermite(rise0, 0.6, TOP, 0.0, u / OVER, OVER) : hermite(TOP, 0.0, LET_GO, LET_GO_DROP, (u - OVER) / (1.0 - OVER), 1.0 - OVER);
            r = u < OVER ? r0 + (HAULED - r0) * smooth(u / OVER) : HAULED + (FLUNG - HAULED) * Math.pow((u - OVER) / (1.0 - OVER), 2.0);
        } else {
            // Empty, it swings on through and down, drawing in.
            double past = Math.min(1.0, (u - 1.0) / 0.4);
            turn = turn1 + side * 0.25 * Math.sin(past * Math.PI * 0.5);
            rise = LET_GO + LET_GO_DROP * 0.4 * (past - 0.5 * past * past) * 1.3;
            r = FLUNG - (FLUNG - HAULED * 1.1) * smooth(past);
        }
        double level = Math.cos(rise) * r;
        return new double[] {p[0] + Math.sin(turn) * level, p[1] + Math.sin(rise) * r, p[2] + Math.cos(turn) * level};
    }

    /** How far through the sweep it is {@code ticks} after the lift began (0 at the start, 1 at the throw): heavy, then whipped. */
    public static double sweep(double ticks) {
        double t = Math.max(0.0, Math.min(1.0, ticks / LIFT));
        return Math.pow(t, 2.0);
    }

    /** The body's speed (blocks a tick, her frame) as it is thrown: the sweep's own, at the end. */
    public static double[] throwVelocity(double[] start, int side) {
        double h = 0.02;
        double[] a = carry(sweep(LIFT - h), start, side);
        double[] b = carry(sweep(LIFT), start, side);
        return new double[] {(b[0] - a[0]) / h, (b[1] - a[1]) / h, (b[2] - a[2]) / h};
    }

    // ------------------------------------------------------------------ helpers

    /** A cubic from {@code a} (slope {@code sa} per unit of the whole) to {@code b} (slope {@code sb}), {@code t} 0..1 over a span {@code span} long. */
    static double hermite(double a, double sa, double b, double sb, double t, double span) {
        double c = Math.max(0.0, Math.min(1.0, t));
        double c2 = c * c;
        double c3 = c2 * c;
        return (2 * c3 - 3 * c2 + 1) * a + (c3 - 2 * c2 + c) * sa * span + (-2 * c3 + 3 * c2) * b + (c3 - c2) * sb * span;
    }

    static double wrap(double radians) {
        return radians - 2.0 * Math.PI * Math.floor((radians + Math.PI) / (2.0 * Math.PI));
    }

    static double smooth(double u) {
        double c = Math.max(0.0, Math.min(1.0, u));
        return c * c * (3.0 - 2.0 * c);
    }

    private WatchRules() {
    }
}
