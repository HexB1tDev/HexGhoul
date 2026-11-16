package com.hex.kagune.control;

/**
 * 2.0.42 (pure, tested; the owner: "right click with the Rinkaku ... It's going to be a new move called blocking. While holding right
 * click, your kagune is going to go in front of you. You're going to assume a defensive position with your hands ... You block literally
 * all moves with that, but if you get too many hits, your kagune will like be pushed back and the move will go on cooldown. Perfectly
 * timed blocks ... cause the enemy to stagger ... it has to be extremely perfectly timed ... you don't get knocked back, and you get a free
 * shot at the enemy"): Rinkaku's block, held on the right click while the tails are out.
 *
 * <ul>
 *   <li>While it is up every blow from in front ({@link #covers}: anything but from behind) is stopped whole: no damage, and only a
 *       little push back ({@link #BLOCK_PUSH}). What has nothing behind it (a fall, fire, hunger, the void) still hurts.</li>
 *   <li>Each blow stopped weighs on the tails ({@link #weight}: one for a light blow, more for a heavy one, counted at most once every
 *       {@link #HIT_GAP} ticks); at {@link #BREAK_AT} the guard breaks: the tails are thrown back, he is pushed back hard
 *       ({@link #BREAK_PUSH}) and the block can't be raised again for {@link #BREAK_COOLDOWN}. The weight drains away once he has gone
 *       {@link #RECOVER_DELAY} without a blow ({@link #settle}).</li>
 *   <li>A blow that lands within {@link #PERFECT_TICKS} of the block going up ({@link #perfect}: three ticks, 0.15 s) is a perfect
 *       block: an impact, no push at all, and the one who struck it (if close: {@link #STAGGER_RANGE}) is staggered
 *       ({@link #STAGGER_TICKS}): stunned, pushed off, and can't hurt anything meanwhile. Only a fresh press opens that window, and only
 *       {@link #FRESH_AFTER} after the last one ({@link #opensWindow}): mashing the button gives none.</li>
 * </ul>
 *
 * <p>Its look: the tails swing up off the back, round each shoulder and across in front of him, crossing (an X a pair, one pair over
 * the next: {@link #pose}), while he holds his forearms up over his face. A blow presses them back ({@link #recoil}), a perfect block
 * snaps them out, a break throws them back over him ({@link #flung}). Points are in the owner's own frame from his feet (x right, y up,
 * z forward, blocks), as {@link RinkakuRoot}'s.
 */
public final class RinkakuBlock {
    private RinkakuBlock() {
    }

    /** Ticks the tails take to swing into the guard, and to fold back out of it. */
    public static final int RAISE_TICKS = 3;
    public static final int LOWER_TICKS = 6;
    /** A blow landing this many ticks or fewer after a fresh press (0, 1, 2) is a perfect block. */
    public static final int PERFECT_TICKS = 3;
    /** A press opens the perfect window only this long after the last press that did. */
    public static final int FRESH_AFTER = 16;
    /** While the button is held the client says so this often; the server lets the block down if it hears nothing for {@link #HELD_TIMEOUT}. */
    public static final int RESEND_TICKS = 4;
    public static final int HELD_TIMEOUT = 14;
    /** The guard breaks at this much weight. */
    public static final float BREAK_AT = 6.0F;
    /** A blow up to this hard weighs one; harder weighs more, up to {@link #MOST_WEIGHT}. */
    public static final float LIGHT_BLOW = 6.0F;
    public static final float MOST_WEIGHT = 3.0F;
    /** Blows stopped within this many ticks of the last counted one weigh nothing more (a stream of hits counts as a few). */
    public static final int HIT_GAP = 5;
    /** The weight starts draining this long after the last blow, this much a tick. */
    public static final int RECOVER_DELAY = 30;
    public static final float RECOVER_PER_TICK = 0.04F;
    /** A broken guard can't be raised again for this long (5 s). */
    public static final int BREAK_COOLDOWN = 100;
    /** A perfect block's stagger: how long, how close the striker must be, and how hard it is pushed off. */
    public static final int STAGGER_TICKS = 24;
    public static final double STAGGER_RANGE = 7.0;
    public static final double STAGGER_PUSH = 0.6;
    /** The push back a blocked blow gives him, and a breaking one. */
    public static final double BLOCK_PUSH = 0.28;
    public static final double BREAK_PUSH = 0.9;
    /** Blows from within this many degrees of where he faces are stopped (anything but from behind). */
    public static final double COVER_DEGREES = 115.0;
    /** His walking, guard up: Slowness at this amplifier (0: I). */
    public static final int SLOW_AMPLIFIER = 0;
    /** In his own first-person view the guard crosses this much lower (blocks, at a player's size). */
    public static final double OWN_DROP = 0.3;
    /** The look: a blow's press back on the tails, a perfect block's snap out, a break's throw (ticks). */
    public static final double RECOIL_TICKS = 6.0;
    public static final double SNAP_TICKS = 8.0;
    public static final double THROW_TICKS = 3.0;
    public static final double THROWN_TICKS = 14.0;

    /** True when a blow at {@code now} meets a block raised at {@code raisedAt} in its perfect window ({@code fresh}: a press that opened one). */
    public static boolean perfect(long raisedAt, boolean fresh, long now) {
        long since = now - raisedAt;
        return fresh && since >= 0 && since < PERFECT_TICKS;
    }

    /** True when a press at {@code now} opens the perfect window: the last one that did was {@link #FRESH_AFTER} or more ago. */
    public static boolean opensWindow(long lastWindow, long now) {
        return now - lastWindow >= FRESH_AFTER || now < lastWindow;
    }

    /** What a stopped blow of {@code damage} weighs on the guard. */
    public static float weight(float damage) {
        if (!(damage > 0.0F)) {
            return 1.0F;
        }
        return Math.max(1.0F, Math.min(MOST_WEIGHT, damage / LIGHT_BLOW));
    }

    /** True when a blow at {@code now} adds its weight: the last counted one was {@link #HIT_GAP} or more before. */
    public static boolean counts(long lastCounted, long now) {
        return now - lastCounted >= HIT_GAP || now < lastCounted;
    }

    /** True when the guard breaks under {@code weight}. */
    public static boolean breaks(float weight) {
        return weight >= BREAK_AT;
    }

    /** The weight after one more tick, {@code sinceHit} ticks after the last blow. */
    public static float settle(float weight, long sinceHit) {
        if (sinceHit < RECOVER_DELAY) {
            return weight;
        }
        return Math.max(0.0F, weight - RECOVER_PER_TICK);
    }

    /**
     * True when a blow from {@code toSource} (from him toward where it came from) is in front of {@code facing} (where he looks): within
     * {@link #COVER_DEGREES} across the ground. One from straight above or below (his own feet, a ceiling) counts as in front.
     */
    public static boolean covers(double[] facing, double[] toSource) {
        double fx = facing[0];
        double fz = facing[2];
        double sx = toSource[0];
        double sz = toSource[2];
        double f = Math.sqrt(fx * fx + fz * fz);
        double s = Math.sqrt(sx * sx + sz * sz);
        if (f < 1.0E-6 || s < 0.15) {
            return true;
        }
        double cos = (fx * sx + fz * sz) / (f * s);
        return cos >= Math.cos(Math.toRadians(COVER_DEGREES));
    }

    /**
     * How far the tails are in the guard (0..1): swinging in over {@link #RAISE_TICKS} from {@code sinceRaised}, folding back over
     * {@link #LOWER_TICKS} from {@code sinceLowered} (NaN while it is still up).
     */
    public static float amount(double sinceRaised, double sinceLowered) {
        double up = smooth(sinceRaised / RAISE_TICKS);
        if (Double.isNaN(sinceLowered)) {
            return (float)up;
        }
        return (float)(up * (1.0 - smooth(sinceLowered / LOWER_TICKS)));
    }

    /**
     * How far the guard's front is pushed along the way he faces ({@code +} out, {@code -} back toward him, blocks), {@code since} ticks
     * after a blow ({@code perfect}: a perfect block's snap out; otherwise pressed back, shaking).
     */
    public static double recoil(double since, boolean perfect) {
        if (!(since >= 0.0)) {
            return 0.0;
        }
        if (perfect) {
            if (since >= SNAP_TICKS) {
                return 0.0;
            }
            double out = since < 1.0 ? smooth(since) : 1.0 - smooth((since - 1.0) / (SNAP_TICKS - 1.0));
            return 0.26 * out;
        }
        if (since >= RECOIL_TICKS) {
            return 0.0;
        }
        double press = since < 1.0 ? smooth(since) : 1.0 - smooth((since - 1.0) / (RECOIL_TICKS - 1.0));
        return -0.16 * press + 0.025 * Math.sin(since * 2.6) * (1.0 - since / RECOIL_TICKS);
    }

    /**
     * How far a broken guard's tails are thrown back (0..1, of {@link #flung}), and how much of the guard's hold is left on them at all,
     * {@code since} ticks after it broke: {thrown, hold}.
     */
    public static double[] thrown(double since) {
        if (!(since >= 0.0)) {
            return new double[] {0.0, 0.0};
        }
        double thrown = smooth(since / THROW_TICKS);
        double hold = 1.0 - smooth((since - THROW_TICKS) / THROWN_TICKS);
        return new double[] {thrown, Math.max(0.0, hold)};
    }

    /** How many lanes (pairs, one over the next) {@code tails} tails make. */
    public static int lanes(int tails) {
        return Math.max(1, Math.min(3, (Math.max(1, tails) + 1) / 2));
    }

    /** The height of lane {@code lane}'s crossing (of a body {@code height} tall) when there are {@code lanes}: face and chest, then the belly. */
    static double laneY(int lane, int lanes, double height) {
        double scale = height / 1.8;
        double[] at = switch (lanes) {
            case 1 -> new double[] {1.36};
            case 2 -> new double[] {1.5, 1.06};
            default -> new double[] {1.56, 1.22, 0.88};
        };
        return at[Math.max(0, Math.min(at.length - 1, lane))] * scale;
    }

    /**
     * Tail {@code limb} of {@code tails} in its guard, {@code n} points from its root (owner's frame): out of the lower back, up round its
     * own shoulder outside the raised arm, then across in front of him from high on its side to low on the other, its tip curling round
     * the far side; the pair of a lane cross in the middle (one a little in front of the other), each lane a little further out than the
     * one above. A single tail lies across his face and chest. Points are evenly spaced along it.
     */
    public static double[][] pose(int limb, int tails, double fanSide, int n, double height) {
        return pose(limb, tails, fanSide, n, height, false);
    }

    /**
     * As {@link #pose(int, int, double, int, double)}; {@code own} for his own eyes in first person: the crossing let down by
     * {@link #OWN_DROP} and a little further out, so the guard shows at the bottom of his view without hiding what is in front of him.
     */
    public static double[][] pose(int limb, int tails, double fanSide, int n, double height, boolean own) {
        double scale = height / 1.8;
        double side = RinkakuRoot.side(limb, fanSide);
        double[] root = RinkakuRoot.root(limb, side, height);
        double[] way = RinkakuRoot.direction(limb, side);
        int lanes = lanes(tails);
        int lane = Math.min(lanes - 1, Math.max(0, limb) / 2);
        double y = laneY(lane, lanes, height) - (own ? OWN_DROP * scale : 0.0);
        double front = (0.62 + 0.08 * lane + (own ? 0.12 : 0.0)) * scale;
        boolean alone = limb % 2 == 0 && limb + 1 >= tails;
        double drop = alone ? 0.22 : 0.32;
        double cross = (limb % 2 == 0 ? 0.06 : -0.06) * scale;
        double[][] keys = {
            root,
            {root[0] + way[0] * 0.38 * scale, root[1] + way[1] * 0.38 * scale + 0.04 * scale, root[2] + way[2] * 0.38 * scale},
            {side * 0.46 * scale, (root[1] + y) * 0.5 + 0.24 * scale, -0.36 * scale},
            {side * 0.7 * scale, y + (drop + 0.04) * scale, 0.02 * scale},
            {side * 0.54 * scale, y + drop * scale, front - 0.05 * scale},
            {0.0, y, front + 0.05 * scale + cross},
            {-side * 0.56 * scale, y - drop * scale, front},
            {-side * 0.8 * scale, y - (drop + 0.1) * scale, front - 0.3 * scale}};
        return resample(spline(keys, 12), n);
    }

    /**
     * Tail {@code limb} thrown back over him (a broken guard), {@code n} points from its root: out of the back, up behind his head on its
     * own side and back, its tip high behind.
     */
    public static double[][] flung(int limb, double fanSide, int n, double height) {
        double scale = height / 1.8;
        double side = RinkakuRoot.side(limb, fanSide);
        double[] root = RinkakuRoot.root(limb, side, height);
        double[] way = RinkakuRoot.direction(limb, side);
        int pair = Math.max(0, limb) / 2;
        double spread = (0.5 + 0.22 * pair) * side * scale;
        double[][] keys = {
            root,
            {root[0] + way[0] * 0.38 * scale, root[1] + way[1] * 0.38 * scale + 0.06 * scale, root[2] + way[2] * 0.38 * scale},
            {spread * 0.8, 1.55 * scale, -0.75 * scale},
            {spread * 1.2, 2.25 * scale - 0.12 * pair * scale, -1.25 * scale},
            {spread * 1.5, 2.55 * scale - 0.2 * pair * scale, -1.95 * scale}};
        return resample(spline(keys, 12), n);
    }

    /**
     * {@code path} moved along {@code forward} (unit, the same frame) by {@code by} blocks, the more the further along it is (none by
     * its root's half, all of it from two thirds on). A new array.
     */
    public static double[][] push(double[][] path, double[] forward, double by) {
        int n = path.length;
        double[][] out = new double[n][];
        for (int i = 0; i < n; i++) {
            double u = n < 2 ? 1.0 : i / (double)(n - 1);
            double k = smooth((u - 0.4) / 0.28) * by;
            out[i] = new double[] {path[i][0] + forward[0] * k, path[i][1] + forward[1] * k, path[i][2] + forward[2] * k};
        }
        return out;
    }

    /** {@code a} moved {@code amount} (0..1) of the way to {@code b}, point by point (same length). A new array. */
    public static double[][] blend(double[][] a, double[][] b, double amount) {
        double t = Math.max(0.0, Math.min(1.0, amount));
        double[][] out = new double[a.length][];
        for (int i = 0; i < a.length; i++) {
            out[i] = new double[] {a[i][0] + (b[i][0] - a[i][0]) * t, a[i][1] + (b[i][1] - a[i][1]) * t, a[i][2] + (b[i][2] - a[i][2]) * t};
        }
        return out;
    }

    /** A Catmull-Rom curve through {@code keys}, {@code per} points between each two (the ends kept). */
    static double[][] spline(double[][] keys, int per) {
        int k = keys.length;
        double[][] out = new double[(k - 1) * per + 1][];
        int o = 0;
        for (int s = 0; s < k - 1; s++) {
            double[] p0 = keys[Math.max(0, s - 1)];
            double[] p1 = keys[s];
            double[] p2 = keys[s + 1];
            double[] p3 = keys[Math.min(k - 1, s + 2)];
            for (int j = 0; j < per; j++) {
                double t = j / (double)per;
                double t2 = t * t;
                double t3 = t2 * t;
                double[] p = new double[3];
                for (int c = 0; c < 3; c++) {
                    p[c] = 0.5 * (2.0 * p1[c] + (-p0[c] + p2[c]) * t + (2.0 * p0[c] - 5.0 * p1[c] + 4.0 * p2[c] - p3[c]) * t2
                        + (-p0[c] + 3.0 * p1[c] - 3.0 * p2[c] + p3[c]) * t3);
                }
                out[o++] = p;
            }
        }
        out[o] = keys[k - 1].clone();
        return out;
    }

    /** {@code n} points evenly spaced along {@code line}. */
    static double[][] resample(double[][] line, int n) {
        int m = line.length;
        double[] arc = new double[m];
        for (int i = 1; i < m; i++) {
            arc[i] = arc[i - 1] + RinkakuRoot.distance(line[i], line[i - 1]);
        }
        double total = arc[m - 1];
        double[][] out = new double[Math.max(0, n)][];
        int j = 1;
        for (int i = 0; i < n; i++) {
            double at = n < 2 ? 0.0 : total * i / (n - 1);
            while (j < m - 1 && arc[j] < at) {
                j++;
            }
            double span = arc[j] - arc[j - 1];
            double t = span < 1.0E-9 ? 0.0 : Math.max(0.0, Math.min(1.0, (at - arc[j - 1]) / span));
            double[] a = line[j - 1];
            double[] b = line[j];
            out[i] = new double[] {a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t, a[2] + (b[2] - a[2]) * t};
        }
        return out;
    }

    static double smooth(double value) {
        double t = Double.isFinite(value) ? Math.max(0.0, Math.min(1.0, value)) : 0.0;
        return t * t * (3.0 - 2.0 * t);
    }
}
