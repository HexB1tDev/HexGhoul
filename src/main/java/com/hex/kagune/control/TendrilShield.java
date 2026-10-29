package com.hex.kagune.control;

/**
 * Auto Projectile as a shield (5.0.14; pure, tested). The original mutation swung one random tentacle at where an incoming
 * projectile had been, which never looked like it touched it. Now a tentacle swings out and holds itself across the line the
 * shots come in on, between them and its owner, and the shots stop on it: one tentacle for each direction they come from
 * ({@link #covers}: within {@link #COVER_DEGREES} of a raised one it is already covered).
 *
 * <p>It is a guard, not a wall, so Ayato keeps every answer to it:
 * <ul>
 *   <li>a tentacle needs {@link #RAISE_TICKS} to swing into place before it stops anything: spikes and needles fired from close
 *       up arrive first;</li>
 *   <li>each tentacle's guard holds {@link #guardHealth} damage; what a shot has beyond that goes through, and a broken guard is
 *       knocked aside for {@link #BREAK_TICKS} (the Gatling's stream breaks one in about a second);</li>
 *   <li>no more than {@link #MAX_GUARDS} at once (a server setting), so shots from several sides, from above or crossing get
 *       through;</li>
 *   <li>the guards drop whenever he attacks, grabs or moves with his tentacles (they are needed), and every stopped shot costs RC
 *       ({@link #cost}).</li>
 * </ul>
 *
 * <p>Directions are world unit vectors pointing from the defender toward where the shots come from. Points are world
 * coordinates in blocks.
 */
public final class TendrilShield {
    /**
     * Projectile entities (arrows, tridents, thrown things, fireballs): seen this far away and followed along their own flight this
     * many ticks ahead ({@link #incoming(double[], double[], double, double, double[], double[], int, double)}). 6.0.9: until now
     * they were seen 8 blocks out along a straight line, as the original: under three ticks for a drawn bow's arrow, less than the
     * {@link #RAISE_TICKS} a tentacle takes to swing into place, and a straight line missed arrows shot on an arc.
     */
    public static final double FLIGHT_SCAN = 48.0;
    public static final int FLIGHT_LOOKAHEAD = 20;
    /** Vanilla's flight a tick: an arrow (or a trident) falls 0.05, a thrown thing 0.03, and each keeps 0.99 of its speed. */
    public static final double ARROW_GRAVITY = 0.05;
    public static final double THROWN_GRAVITY = 0.03;
    public static final double AIR_DRAG = 0.99;
    /**
     * 6.0.9: a shot loosed from close up lands before a tentacle could swing in, so the guard goes up against the shooter before
     * he looses: a ranged mob after him within {@link #MOB_RANGE}, or a player drawing (or holding a loaded crossbow) within
     * {@link #AIM_RANGE} and aimed within {@link #AIM_DEGREES} of him.
     */
    public static final double MOB_RANGE = 24.0;
    public static final double AIM_RANGE = 48.0;
    public static final double AIM_DEGREES = 15.0;
    /** Ayato's shots (simulated, not entities): seen this many ticks before they would hit, and this far away at most. */
    public static final double SHOT_LOOKAHEAD = 10.0;
    public static final double SHOT_SCAN = 64.0;
    /** A shot passing this close to the body's middle (plus its own radius) counts as incoming, as the original. */
    public static final double INTERCEPT = 1.35;
    /** A raised tentacle covers shots coming in within this angle of its own direction. */
    public static final double COVER_DEGREES = 50.0;
    /** A tentacle stays up this long after the last shot it covered or stopped. */
    public static final int HOLD_TICKS = 14;
    /** Ticks a tentacle takes to swing into place (it stops nothing before), and to fold back. */
    public static final int RAISE_TICKS = 4;
    public static final int LOWER_TICKS = 5;
    /** Guards up at once, unless the server sets otherwise. */
    public static final int MAX_GUARDS = 2;
    /** A broken guard: the tentacle is knocked aside for this long before it can guard again (3 s). */
    public static final int BREAK_TICKS = 60;
    /** A guard's damage comes back this much a tick while it isn't broken (2 a second). */
    public static final float REGEN_PER_TICK = 0.1F;
    /** A projectile entity (an arrow, a fireball) counts as this much damage against a guard. */
    public static final float ENTITY_WEIGHT = 4.0F;
    /** Where the guard stands: this far out from the body's middle toward the shots (blocks), and how wide it spans across. */
    public static final double DISTANCE = 0.85;
    public static final double HALF_WIDTH = 0.8;
    /** A projectile entity bounced off a tentacle isn't stopped again by the same player for this many ticks. */
    public static final int REBLOCK_TICKS = 10;
    /** RC: a projectile entity costs what the original's deflection did; Ayato's shots by how much the guard took. */
    public static final float ARROW_COST = 2.0F;
    public static final float SHOT_COST = 0.15F;
    public static final float SHOT_COST_PER_DAMAGE = 0.25F;
    /** Projectile Reflex Organ: every block costs this share (the original's 1.36 against 2). */
    public static final float ORGAN_COST = 0.68F;
    /** The approach (off the back and round) takes this share of the tentacle; the rest lies across the shots. */
    static final double SPLIT = 0.5;

    private TendrilShield() {
    }

    /** RC one stopped shot costs: a projectile entity, or one of Ayato's shots the guard took {@code absorbed} of. */
    public static float cost(float absorbed, boolean entity, boolean organ) {
        float base = entity ? ARROW_COST : Math.max(SHOT_COST, SHOT_COST_PER_DAMAGE * Math.max(0.0F, absorbed));
        return base * (organ ? ORGAN_COST : 1.0F);
    }

    /**
     * How much damage one tentacle's guard holds before it breaks: 6 at mastery 0 to 12 at 1000 (about three of Ayato's shots
     * at the same mastery), half as much again with the Projectile Reflex Organ, times the server's {@code strength}.
     */
    public static float guardHealth(int mastery, boolean organ, double strength) {
        float base = 6.0F + 6.0F * Math.max(0, Math.min(1000, mastery)) / 1000.0F;
        return (float)(base * (organ ? 1.5F : 1.0F) * Math.max(0.0, strength));
    }

    /**
     * A shot of {@code damage} against a guard with {@code health} left: {absorbed, through}. The guard takes what it can; the
     * rest goes on into the body.
     */
    public static float[] absorb(float damage, float health) {
        float taken = Math.max(0.0F, Math.min(damage, health));
        return new float[] {taken, Math.max(0.0F, damage - taken)};
    }

    /** True once a guard raised at {@code raisedAt} is in place at {@code now} (it stops nothing while still swinging in). */
    public static boolean inPlace(long raisedAt, long now) {
        return now - raisedAt >= RAISE_TICKS;
    }

    /**
     * True when a shot at {@code p} moving {@code v} a tick (relative to the defender) passes within {@code reach} of
     * {@code centre} in the next {@code lookahead} ticks, still coming in.
     */
    public static boolean incoming(double[] p, double[] v, double[] centre, double lookahead, double reach) {
        double speed = dot(v, v);
        if (speed < 0.0025) {
            return false;
        }
        double[] toCentre = sub(centre, p);
        double t = dot(toCentre, v) / speed;
        if (t < 0.0 || t > lookahead) {
            return false;
        }
        double[] miss = sub(toCentre, scale(v, t));
        return dot(miss, miss) <= reach * reach;
    }

    /**
     * 6.0.9: true when a projectile at {@code p} moving {@code v} a tick, falling {@code gravity} a tick and keeping {@code drag} of
     * its speed (vanilla's order: it moves, then slows, then falls), passes within {@code reach} of {@code centre}, itself moving
     * {@code drift} a tick, in the next {@code lookahead} ticks: along its real flight, arc and all, closest approach checked
     * along each tick's stretch of it.
     */
    public static boolean incoming(double[] p, double[] v, double gravity, double drag, double[] centre, double[] drift, int lookahead,
                                   double reach) {
        if (dot(v, v) < 0.0025) {
            return false;
        }
        double[] at = p.clone();
        double[] speed = v.clone();
        double[] target = centre.clone();
        for (int tick = 0; tick < lookahead; tick++) {
            double[] next = add(at, speed);
            // Closest approach to the defender over this tick, both moving.
            double[] rel = sub(at, target);
            double[] step = sub(speed, drift);
            double along = dot(step, step) < 1.0E-12 ? 0.0 : Math.max(0.0, Math.min(1.0, -dot(rel, step) / dot(step, step)));
            double[] miss = add(rel, scale(step, along));
            if (dot(miss, miss) <= reach * reach) {
                return true;
            }
            at = next;
            target = add(target, drift);
            speed = new double[] {speed[0] * drag, speed[1] * drag - gravity, speed[2] * drag};
        }
        return false;
    }

    /** 6.0.9: true when someone at {@code eye} looking along {@code look} aims within {@link #AIM_DEGREES} of {@code target}, in range. */
    public static boolean aimedAt(double[] eye, double[] look, double[] target) {
        double[] to = sub(target, eye);
        double distance = Math.sqrt(dot(to, to));
        double lookLength = Math.sqrt(dot(look, look));
        if (distance < 1.0E-6 || distance > AIM_RANGE || lookLength < 1.0E-6) {
            return distance < 1.0E-6;
        }
        return dot(to, look) / (distance * lookLength) >= Math.cos(Math.toRadians(AIM_DEGREES));
    }

    /** The direction a shot comes from, seen from {@code centre}: back along its flight, or toward it when it barely moves. */
    public static double[] from(double[] p, double[] v, double[] centre) {
        double[] back = scale(v, -1.0);
        if (dot(back, back) < 1.0E-8) {
            back = sub(p, centre);
        }
        double length = Math.sqrt(dot(back, back));
        return length < 1.0E-8 ? new double[] {0.0, 0.0, 1.0} : scale(back, 1.0 / length);
    }

    /** True when a tentacle raised toward {@code shield} also covers shots from {@code threat} (unit vectors). */
    public static boolean covers(double[] shield, double[] threat) {
        return dot(shield, threat) >= Math.cos(Math.toRadians(COVER_DEGREES));
    }

    /**
     * Where a shot stops on a tentacle raised toward {@code dir} in front of {@code centre}: where its line, through {@code p}
     * along {@code v}, crosses the guard's plane (no further than {@code hit}, where it reached the body), or {@code hit}.
     */
    public static double[] stopPoint(double[] p, double[] v, double[] hit, double[] centre, double[] dir) {
        double[] plane = add(centre, scale(dir, DISTANCE));
        double along = dot(v, dir);
        if (Math.abs(along) < 1.0E-6) {
            return hit.clone();
        }
        double t = dot(sub(plane, p), dir) / along;
        double[] at = add(p, scale(v, t));
        double reached = dot(sub(hit, p), v);
        return t < 0.0 || dot(sub(at, p), v) > reached ? hit.clone() : at;
    }

    /** World yaw and pitch (degrees, Minecraft's convention) of a direction, and back. */
    public static float yaw(double[] dir) {
        return (float)Math.toDegrees(Math.atan2(-dir[0], dir[2]));
    }

    public static float pitch(double[] dir) {
        return (float)Math.toDegrees(-Math.asin(Math.max(-1.0, Math.min(1.0, dir[1]))));
    }

    public static double[] direction(float yaw, float pitch) {
        double y = Math.toRadians(yaw);
        double p = Math.toRadians(pitch);
        return new double[] {-Math.sin(y) * Math.cos(p), -Math.sin(p), Math.cos(y) * Math.cos(p)};
    }

    /**
     * How far a tentacle is in its guard (0..1): swinging in over {@link #RAISE_TICKS} from {@code sinceRaised}, and folding
     * back over {@link #LOWER_TICKS} from {@code sinceLowered} (NaN while it is still up).
     */
    public static float amount(double sinceRaised, double sinceLowered) {
        double up = smooth(sinceRaised / RAISE_TICKS);
        if (Double.isNaN(sinceLowered)) {
            return (float)up;
        }
        return (float)(up * (1.0 - smooth(sinceLowered / LOWER_TICKS)));
    }

    /**
     * One tentacle's guard, {@code n} points from its {@code root}: up off the back and round the body on its own side, then
     * held across {@code dir} in front of {@code centre}, bowed a little toward the shots. Shots from high up (rain) put it
     * over his head. {@code forward} is the body's horizontal facing (used when the shots come from straight above or below).
     */
    public static double[][] pose(double[] root, double[] centre, double[] dir, double[] forward, int n) {
        double rise = Math.max(-0.6, Math.min(0.95, dir[1]));
        double[] flat = {dir[0], 0.0, dir[2]};
        double flatLength = Math.sqrt(dot(flat, flat));
        flat = flatLength < 0.2 ? normalize(new double[] {forward[0], 0.0, forward[2]}) : scale(flat, 1.0 / flatLength);
        double out = DISTANCE * (1.0 - 0.45 * Math.abs(rise));
        double[] middle = {centre[0] + flat[0] * out, centre[1] + 0.15 + rise * 0.9, centre[2] + flat[2] * out};
        // Across the shots, starting on the side the tentacle grows from.
        double[] across = {-flat[2], 0.0, flat[0]};
        double[] rootSide = {root[0] - centre[0], 0.0, root[2] - centre[2]};
        if (dot(across, rootSide) < 0.0) {
            across = scale(across, -1.0);
        }
        double[] start = add(middle, scale(across, HALF_WIDTH));
        double[] tip = add(middle, scale(across, -HALF_WIDTH));
        double[] bow = add(middle, scale(dir, 0.15));
        double[] away = dot(rootSide, rootSide) < 1.0E-8 ? new double[3] : normalize(rootSide);
        double[] lift = {root[0] + away[0] * 0.35, root[1] + 0.3, root[2] + away[2] * 0.35};
        double[] reach = {start[0] - flat[0] * 0.6 + across[0] * 0.45, start[1] + 0.15, start[2] - flat[2] * 0.6 + across[2] * 0.45};
        double[][] points = new double[n][];
        for (int i = 0; i < n; i++) {
            double u = n < 2 ? 1.0 : i / (double)(n - 1);
            points[i] = u <= SPLIT ? cubic(root, lift, reach, start, u / SPLIT) : quadratic(start, bow, tip, (u - SPLIT) / (1.0 - SPLIT));
        }
        return points;
    }

    /** {@code path} moved {@code amount} (0..1) of the way to {@code guard}, point by point (same length). */
    public static double[][] blend(double[][] path, double[][] guard, double amount) {
        double a = Math.max(0.0, Math.min(1.0, amount));
        double[][] out = new double[path.length][];
        for (int i = 0; i < path.length; i++) {
            out[i] = new double[] {path[i][0] + (guard[i][0] - path[i][0]) * a, path[i][1] + (guard[i][1] - path[i][1]) * a,
                path[i][2] + (guard[i][2] - path[i][2]) * a};
        }
        return out;
    }

    private static double[] cubic(double[] a, double[] b, double[] c, double[] d, double t) {
        double s = 1.0 - t;
        double[] out = new double[3];
        for (int k = 0; k < 3; k++) {
            out[k] = s * s * s * a[k] + 3.0 * s * s * t * b[k] + 3.0 * s * t * t * c[k] + t * t * t * d[k];
        }
        return out;
    }

    private static double[] quadratic(double[] a, double[] b, double[] c, double t) {
        double s = 1.0 - t;
        double[] out = new double[3];
        for (int k = 0; k < 3; k++) {
            out[k] = s * s * a[k] + 2.0 * s * t * b[k] + t * t * c[k];
        }
        return out;
    }

    static double smooth(double value) {
        double t = Double.isFinite(value) ? Math.max(0.0, Math.min(1.0, value)) : 0.0;
        return t * t * (3.0 - 2.0 * t);
    }

    static double dot(double[] a, double[] b) {
        return a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
    }

    static double[] add(double[] a, double[] b) {
        return new double[] {a[0] + b[0], a[1] + b[1], a[2] + b[2]};
    }

    static double[] sub(double[] a, double[] b) {
        return new double[] {a[0] - b[0], a[1] - b[1], a[2] - b[2]};
    }

    static double[] scale(double[] a, double s) {
        return new double[] {a[0] * s, a[1] * s, a[2] * s};
    }

    static double[] normalize(double[] a) {
        double length = Math.sqrt(dot(a, a));
        return length < 1.0E-8 ? new double[] {0.0, 0.0, 1.0} : scale(a, 1.0 / length);
    }
}
