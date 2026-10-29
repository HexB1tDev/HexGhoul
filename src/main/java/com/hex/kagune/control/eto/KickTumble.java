package com.hex.kagune.control.eto;

/**
 * 6.2.0: how a body the Speed Kick lands on goes (pure, tested): only how it is drawn, every client alike (where it flies is the
 * server's launch). The head snaps the way the foot drove it on the very frame of the blow; the body follows it a beat later,
 * twisting round (the blow was to the side of its head) and tipping over head first the way it flies, tumbling; it comes down
 * flat with a thump, lies there a moment ({@link EtoRules#KICK_DOWN_TICKS}) and gets up ({@link EtoRules#KICK_GETUP_TICKS}).
 *
 * <p>Angles in radians: {@code head} the head's turn the way it was driven, {@code pitch} its chin thrown up, {@code twist} the
 * whole body's turn about its own length the same way, {@code tumble} its tip over about the level line square to its flight (+
 * the top goes the way it flies; a quarter turn lies it flat).
 */
public final class KickTumble {
    /** How far the head snaps round, and the chin up. */
    public static final double HEAD_SNAP = 1.15;
    static final double HEAD_PITCH = 0.5;
    /** How quickly (ticks) the head gets there: it is the first thing to go. */
    static final double SNAP_TICKS = 0.7;
    /** How far the body turns after it (the neck, the shoulders, the chest), how long after, and how quickly. */
    static final double BODY_TURN = 0.95;
    static final double BODY_LAG = 0.35;
    static final double BODY_TICKS = 2.2;
    /** Then on round in the air (radians a tick), and over head first. */
    static final double SPIN = 0.24;
    static final double TUMBLE = 0.42;
    /** The tumble starts after the body has begun to turn, and takes this long (ticks) to get going. */
    static final double TUMBLE_LAG = 0.7;
    static final double TUMBLE_RAMP = 1.4;
    /** Ticks to come down flat once it lands, the bounce in it. */
    public static final double THUMP = 3.0;
    static final double BOUNCE = 0.18;
    /** In the air at most this long (ticks): then it is down wherever it is (6.2.2: long enough for all of Witness This). */
    public static final double AIRBORNE_MAX = 140.0;
    /** 6.2.2: a body juggled in Witness This turns over this much slower (so you can follow it). */
    public static final double WITNESS_SLOW = 0.45;

    private KickTumble() {
    }

    /** One frame: the head's turn and pitch, the body's twist and tumble, and how flat it lies (0 upright, 1 flat). */
    public record Frame(double head, double pitch, double twist, double tumble, double flat) {
        public static final Frame NONE = new Frame(0.0, 0.0, 0.0, 0.0, 0.0);
    }

    /** {@code t} ticks after the blow; it landed {@code landed} ticks after the blow (NaN: still in the air). */
    public static Frame at(double t, double landed) {
        return at(t, landed, EtoRules.KICK_DOWN_TICKS);
    }

    /** 6.2.3: ...lying there {@code lieTicks} once down (Witness This's crater: longer). */
    public static Frame at(double t, double landed, double lieTicks) {
        if (t < 0.0) {
            return Frame.NONE;
        }
        boolean down = !Double.isNaN(landed) && t >= landed;
        double air = down ? landed : t;
        double head = HEAD_SNAP * snap(air / SNAP_TICKS);
        double pitch = HEAD_PITCH * snap(air / SNAP_TICKS);
        double twist = twist(air);
        double tumble = tumble(air);
        if (!down) {
            return new Frame(head, pitch, twist, tumble, Math.abs(Math.sin(tumble)));
        }
        // Down: from however it was turned over, flat (head first the way it flew, or the other way, whichever is nearer), with a
        // thump; lying there; then up onto its feet.
        double since = t - landed;
        double lie = lying(tumble);
        double up = upright(lie);
        double spun = Math.round(twist / (Math.PI * 2.0)) * Math.PI * 2.0;
        double relax = 1.0 - smooth(since / 4.0);
        if (since < THUMP) {
            double u = since / THUMP;
            double eased = 1.0 - Math.pow(1.0 - u, 3.0) + BOUNCE * Math.sin(Math.PI * u) * (1.0 - u);
            double angle = tumble + (lie - tumble) * eased;
            return new Frame(head * relax, pitch * relax, twist, angle, Math.abs(Math.sin(angle)));
        }
        double rising = (since - lieTicks) / EtoRules.KICK_GETUP_TICKS;
        if (rising <= 0.0) {
            return new Frame(head * relax, pitch * relax, twist, lie, 1.0);
        }
        double u = smooth(Math.min(1.0, rising));
        double angle = lie + (up - lie) * u;
        return new Frame(head * relax * (1.0 - u), pitch * relax * (1.0 - u), twist + (spun - twist) * u, angle, Math.abs(Math.sin(angle)));
    }

    /** True once it is back on its feet, {@code t} ticks after the blow, having landed {@code landed} after it. */
    public static boolean over(double t, double landed) {
        return over(t, landed, EtoRules.KICK_DOWN_TICKS);
    }

    /** 6.2.3: ...having lain there {@code lie} ticks. */
    public static boolean over(double t, double landed, double lie) {
        return !Double.isNaN(landed) && t - landed >= lie + EtoRules.KICK_GETUP_TICKS;
    }

    /** The body's turn about its own length {@code t} ticks into its flight: after the head, then on round. */
    static double twist(double t) {
        double follow = BODY_TURN * smooth((t - BODY_LAG) / BODY_TICKS);
        double on = t - BODY_LAG - BODY_TICKS;
        return follow + (on > 0.0 ? SPIN * on : 0.0);
    }

    /** Its tip over {@code t} ticks into its flight: getting going, then over and over. */
    static double tumble(double t) {
        double x = t - TUMBLE_LAG;
        if (x <= 0.0) {
            return 0.0;
        }
        return TUMBLE * (x < TUMBLE_RAMP ? x * x / (2.0 * TUMBLE_RAMP) : x - TUMBLE_RAMP / 2.0);
    }

    /** The flat angle nearest {@code tumble} (a quarter turn, three quarters, and so on round); at least a quarter turn over. */
    static double lying(double tumble) {
        double k = Math.round((tumble - Math.PI / 2.0) / Math.PI);
        return Math.max(Math.PI / 2.0, Math.PI / 2.0 + k * Math.PI);
    }

    /** Up from lying at {@code lie}: the nearer of a quarter turn back or on that is a whole number of turns. */
    static double upright(double lie) {
        double back = lie - Math.PI / 2.0;
        double turns = back / (Math.PI * 2.0);
        return Math.abs(turns - Math.round(turns)) < 1.0E-6 ? back : lie + Math.PI / 2.0;
    }

    /**
     * 6.2.2: how hard a Witness This blow jolts the body it lands on {@code since} ticks after it (0..1): snapped the way it was
     * struck in half a tick, held there through the blow's hit stop ({@code stop} ticks, 6.2.3), then easing back.
     */
    public static double jolt(double since, double stop) {
        if (since < 0.0) {
            return 0.0;
        }
        double held = Math.max(0.5, stop);
        return since < 0.5 ? snap(since / 0.5) : since < held ? 1.0 : Math.exp(-(since - held) / 1.4);
    }

    /**
     * 6.2.3: how squashed a body is along the line of a Witness This blow {@code since} ticks after it lands (+ flattened by it, -
     * stretched out along it): crushed on contact through the hit stop ({@code stop}), then flung out long as it goes, and back.
     */
    public static double squash(double since, double stop) {
        if (since < 0.0) {
            return 0.0;
        }
        if (since < stop) {
            return SQUASH * snap(since / 0.35);
        }
        double u = since - stop;
        if (u < 1.0) {
            double e = smooth(u);
            return SQUASH + (-STRETCH - SQUASH) * e;
        }
        return -STRETCH * Math.exp(-(u - 1.0) / 1.2);
    }

    /** How far a blow crushes a body (of its length along the blow), and how far it is flung out long after. */
    static final double SQUASH = 0.24;
    static final double STRETCH = 0.14;

    /** Fast out of the blocks, easing in: how the head snaps. */
    static double snap(double u) {
        double c = Math.max(0.0, Math.min(1.0, u));
        return 1.0 - (1.0 - c) * (1.0 - c) * (1.0 - c);
    }

    static double smooth(double u) {
        double c = Math.max(0.0, Math.min(1.0, u));
        return c * c * (3.0 - 2.0 * c);
    }
}
