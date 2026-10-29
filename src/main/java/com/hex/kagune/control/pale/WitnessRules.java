package com.hex.kagune.control.pale;

/**
 * 6.2.0, Witness This ({@link PaleMutation#WITNESS_THIS}; pure, tested): what a fully charged Big Punch that lands goes on into. The
 * blow drives it back and holds it there; she is gone and behind it, crouched, and her uppercut throws it {@link #RISE} blocks up,
 * tumbling like a rag doll from then on (as the Speed Kick's do); she is after it at once, while it still rises, and blitzes it in
 * the air: a storm of {@link #HITS} punches and kicks, one a tick, each from another point of a sphere round it (above it, below
 * it, every side), each from about the far side of the last, every one knocking it {@link #ACROSS} blocks the way it was struck, so
 * it is batted all over the sky (as Rock Lee does Gaara); then she is right above it, tucked, and stomps it straight down out of the
 * air, riding it down into the ground: a crater, and it lies there before it gets up. It is frozen all through (it can do nothing;
 * a player can only look round).
 *
 * <p>Ticks from the punch's blow; blocks in the move's frame: x to her right as she threw the punch, y up from the ground it stood on
 * (its feet then), z the way the punch went, from where it stood when it was hit. Every client and the server work out every place
 * from here alone.
 */
public final class WitnessRules {
    /** The punch drives it back this far over {@link #KNOCK} ticks; it is held there. */
    public static final double KNOCK_BACK = 3.0;
    public static final int KNOCK = 4;
    /** She is behind it ({@link #BEHIND}), the uppercut lands ({@link #UPPER}) and throws it {@link #RISE} up over {@link #CLIMB} ticks. */
    public static final int BEHIND = 4;
    public static final int UPPER = 8;
    public static final double RISE = 17.0;
    public static final int CLIMB = 11;
    /**
     * The blitz: {@link #HITS} blows from {@link #FIRST_HIT} (straight after the uppercut, while it still rises: no pause). 6.2.3:
     * never the same pace twice running ({@link #GAPS}, ticks from each blow to the next): flurries of quick jabs, then a slow,
     * heavy kick that sends it much further; she is there {@link #windUp} before each lands, longer for the slower ones, and every
     * blow stops dead on contact a moment ({@link #hitStop}) before it flies.
     */
    public static final int HITS = 20;
    public static final int FIRST_HIT = UPPER + 3;
    static final int[] GAPS = {5, 3, 2, 7, 4, 6, 2, 4, 8, 5, 3, 7, 4, 2, 6, 9, 3, 2, 5, 7};
    /** The tick each blow lands. */
    static final int[] AT = new int[HITS];
    /** Above it, tucked, winding up ({@link #ABOVE}); the stomp lands ({@link #DROP}); it hits the ground ({@link #IMPACT}). */
    public static final int ABOVE = blitzEnd();
    public static final int DROP = ABOVE + 5;
    public static final int IMPACT = DROP + 4;
    /**
     * 6.2.1: she kicks off it as it goes down and is her own again from {@link #FREE}, high in the air: she falls out of the sky
     * (no fall damage until she is down). The move lasts at least {@link #TICKS}, and on until she is on the ground.
     */
    public static final int FREE = DROP + 2;
    public static final int TICKS = IMPACT + 12;
    /** And at most this long, however far she has to fall. */
    public static final int LONGEST = TICKS + 100;
    /** How far (blocks) she springs up off it, over {@link #DROP} to {@link #FREE}. */
    public static final double REBOUND = 0.9;
    /**
     * How far each blitz blow knocks it (blocks, the way it was struck, times its {@link #power}; there in {@link #fly} ticks, fastest off the blow), and
     * how much of the way back to the middle of the storm it is drawn each time (so it is batted all over a sphere some blocks
     * across, never off out of it); how far from its middle she strikes from; where its middle is.
     */
    public static final double ACROSS = 15.0;
    public static final double CENTRE = 0.3;
    /** 6.2.3: how much of each knock is up or down (the sphere is wider than it is tall, so it never comes near the ground). */
    public static final double VERTICAL = 0.7;
    public static final double STAND = 1.4;
    public static final double MID = 1.0;
    /** How far above its feet she is for the stomp (her foot on its head). */
    public static final double OVER = 2.0;

    /**
     * The blows of the blitz: her punches; the Speed Kick's flying side kick, a stomp straight down from above; 6.2.3: a high
     * roundhouse, a spinning heel kick (a whole turn into it), an axe kick down from above and a flying knee.
     */
    public enum Blow {
        CROSS, JAB, HOOK, LIVER, UPPERCUT, TWIN_PALM, SIDE_KICK, STOMP, ROUNDHOUSE, SPIN_KICK, AXE_KICK, FLYING_KNEE;

        /** How high (blocks above her feet, upright) what she strikes with meets it. */
        public double reach() {
            return switch (this) {
                case LIVER, FLYING_KNEE -> 0.95;
                case UPPERCUT -> 1.55;
                case TWIN_PALM -> 1.1;
                case SIDE_KICK, SPIN_KICK -> PaleRules.KICK_FOOT_Y;
                case ROUNDHOUSE -> 1.45;
                case AXE_KICK -> 0.35;
                case STOMP -> 0.0;
                default -> 1.3;
            };
        }

        /** Whether her whole body is tipped to aim it at where it is (not the uppercut, the stomp and the axe kick: they aim themselves). */
        public boolean aimed() {
            return this != UPPERCUT && this != STOMP && this != AXE_KICK;
        }

        /** 6.2.3: whether it is one of her kicks (a leg, not a fist). */
        public boolean kick() {
            return this == SIDE_KICK || this == STOMP || this == ROUNDHOUSE || this == SPIN_KICK || this == AXE_KICK || this == FLYING_KNEE;
        }
    }

    /** How high (degrees above its middle) she is for each blow, in turn: a stomp from high above, an uppercut from low below. */
    static final double[] ELEVATIONS = {0, 20, -25, 55, -55, 10, 30, -15, 60, -60, 5, -20, 40, 15, 25, -30, 55, -55, 20, 0};
    /** Which of her side blows she throws in turn, where it is neither from above nor below (6.2.3: as many kicks as punches)... */
    static final Blow[] SIDE_BLOWS = {Blow.CROSS, Blow.FLYING_KNEE, Blow.HOOK, Blow.ROUNDHOUSE, Blow.JAB, Blow.SIDE_KICK, Blow.LIVER, Blow.SPIN_KICK};
    /** ...the quick ones always fists, the slow heavy ones always her big kicks. */
    static final Blow[] QUICK_BLOWS = {Blow.JAB, Blow.CROSS, Blow.HOOK};
    static final Blow[] HEAVY_BLOWS = {Blow.ROUNDHOUSE, Blow.SPIN_KICK, Blow.SIDE_KICK};
    /** Each blow, her bearing round it (radians, + toward her right), and which way from its middle she is (unit). */
    static final Blow[] BLOWS = new Blow[HITS];
    static final double[] BEARINGS = new double[HITS];
    static final double[][] AROUND = new double[HITS][];
    /** Where each blow sends it (its feet, against the top of its climb), the last the last blow's. */
    static final double[][] OFFSETS = new double[HITS + 1][];

    static {
        double b = Math.PI * 0.5;
        int side = 0;
        int quick = 0;
        int heavy = 0;
        int high = 0;
        OFFSETS[0] = new double[] {0.0, 0.0, 0.0};
        for (int i = 0; i < HITS; i++) {
            double e = Math.toRadians(ELEVATIONS[i]);
            BEARINGS[i] = b;
            AROUND[i] = new double[] {Math.sin(b) * Math.cos(e), Math.sin(e), Math.cos(b) * Math.cos(e)};
            BLOWS[i] = i == HITS - 1 ? Blow.TWIN_PALM : ELEVATIONS[i] >= 45.0 ? (high++ % 2 == 0 ? Blow.AXE_KICK : Blow.STOMP)
                : ELEVATIONS[i] <= -45.0 ? Blow.UPPERCUT : GAPS[i] >= 7 ? HEAVY_BLOWS[heavy++ % HEAVY_BLOWS.length]
                : GAPS[i] <= 2 ? QUICK_BLOWS[quick++ % QUICK_BLOWS.length] : SIDE_BLOWS[side++ % SIDE_BLOWS.length];
            // Knocked away from her the way it was struck (the slower, heavier blows much further), then drawn a little back toward
            // the middle of the storm.
            double[] o = OFFSETS[i];
            double across = ACROSS * power(i);
            double[] hit = {o[0] - AROUND[i][0] * across, o[1] - AROUND[i][1] * across * VERTICAL, o[2] - AROUND[i][2] * across};
            OFFSETS[i + 1] = new double[] {hit[0] * (1.0 - CENTRE), hit[1] * (1.0 - CENTRE), hit[2] * (1.0 - CENTRE)};
            // The next from about the far side, swung a little round each time.
            b += Math.PI + (i % 2 == 0 ? 0.87 : -0.61);
        }
    }

    private WitnessRules() {
    }

    /** The blow {@code hit} is. */
    public static Blow blow(int hit) {
        return BLOWS[Math.floorMod(hit, HITS)];
    }

    /** Which way (unit, {x, y, z} in the move's frame) from its middle she is for blow {@code hit}. */
    public static double[] around(int hit) {
        return AROUND[Math.floorMod(hit, HITS)].clone();
    }

    /** The way blow {@code hit} knocks it (unit): from her, through it. */
    public static double[] knock(int hit) {
        double[] a = around(hit);
        return new double[] {-a[0], -a[1], -a[2]};
    }

    /** Her bearing round it for blow {@code hit} (radians). */
    public static double bearing(int hit) {
        return BEARINGS[Math.floorMod(hit, HITS)];
    }

    /** The tick blitz blow {@code hit} lands. */
    public static int hitAt(int hit) {
        return AT[Math.max(0, Math.min(HITS - 1, hit))];
    }

    /** The tick the blitz is over (she is above it): after the last blow's own gap. */
    private static int blitzEnd() {
        int at = FIRST_HIT;
        for (int i = 0; i < HITS; i++) {
            AT[i] = at;
            at += GAPS[i];
        }
        return at;
    }

    /** 6.2.3: the ticks from blow {@code hit} to the next (the last: to {@link #ABOVE}). */
    public static int gap(int hit) {
        return GAPS[Math.max(0, Math.min(HITS - 1, hit))];
    }

    /** 6.2.3: how long (ticks) before blow {@code hit} lands she is there for it: longer the more time there was since the last. */
    public static double windUp(int hit) {
        double before = hit <= 0 ? FIRST_HIT - UPPER : gap(hit - 1);
        return Math.max(0.8, Math.min(2.0, 0.42 * before));
    }

    /** 6.2.3: how long (ticks) blow {@code hit} stops dead on contact before it flies: the heavy ones longest. */
    public static double hitStop(int hit) {
        return heavy(hit) ? 1.5 : gap(hit) <= 2 ? 0.5 : 0.8;
    }

    /** 6.2.3: whether blow {@code hit} is one of the slow, heavy ones (a big kick, the last twin palm). */
    public static boolean heavy(int hit) {
        return gap(hit) >= 6 || hit == HITS - 1;
    }

    /** 6.2.3: how hard blow {@code hit} is (1 an average blow): how far it knocks it, how big the flash. */
    public static double power(int hit) {
        return 0.2 + 0.15 * gap(hit);
    }

    /** 6.2.3: how long (ticks) it flies off blow {@code hit}, after the hit stop: there before she is there for the next. */
    static double fly(int hit) {
        return Math.max(1.0, (gap(hit) - hitStop(hit)) * 0.85);
    }

    /** The blitz blow she is on {@code age} ticks in (from its {@link #windUp} before it lands until the next's), or -1. */
    public static int hit(double age) {
        if (age < hitAt(0) - windUp(0) || age >= ABOVE - 1.0) {
            return -1;
        }
        for (int i = HITS - 1; i > 0; i--) {
            if (age >= hitAt(i) - windUp(i)) {
                return i;
            }
        }
        return 0;
    }

    /** The top of its climb {@code age} ticks in (its feet): driven back by the punch, held, then thrown up by the uppercut. */
    static double[] climb(double age) {
        if (age < KNOCK) {
            double u = clamp01(age / KNOCK);
            return new double[] {0.0, 0.0, KNOCK_BACK * (1.0 - (1.0 - u) * (1.0 - u))};
        }
        double u = clamp01((age - UPPER) / CLIMB);
        return new double[] {0.0, RISE * (1.0 - (1.0 - u) * (1.0 - u) * (1.0 - u)), KNOCK_BACK};
    }

    /** Where the blitz has knocked it to (against its climb) {@code age} ticks in: each blow sends it on, fastest off the blow. */
    static double[] knocked(double age) {
        int last = -1;
        for (int i = 0; i < HITS; i++) {
            if (age >= hitAt(i)) {
                last = i;
            }
        }
        if (last < 0) {
            return OFFSETS[0].clone();
        }
        double[] from = OFFSETS[last];
        double[] to = OFFSETS[last + 1];
        // Stopped dead on the blow a moment, then gone, fastest off it.
        double u = clamp01((age - hitAt(last) - hitStop(last)) / fly(last));
        double e = 1.0 - (1.0 - u) * (1.0 - u);
        return new double[] {from[0] + (to[0] - from[0]) * e, from[1] + (to[1] - from[1]) * e, from[2] + (to[2] - from[2]) * e};
    }

    /** Where its feet are {@code age} ticks in, the ground {@code ground} (blocks, against where it stood): {x, y, z}. */
    public static double[] target(double age, double ground) {
        if (age < DROP) {
            double[] c = climb(age);
            double[] k = knocked(age);
            return new double[] {c[0] + k[0], c[1] + k[1], c[2] + k[2]};
        }
        double[] top = target(DROP - 1.0E-6, ground);
        double u = clamp01((age - DROP) / (IMPACT - DROP));
        // Driven straight down, faster and faster.
        return new double[] {top[0], top[1] + (ground - top[1]) * u * (0.35 + 0.65 * u), top[2]};
    }

    /** True while it is held frozen (from the blow until it is down in the crater). */
    public static boolean frozen(double age) {
        return age >= 0.0 && age < IMPACT;
    }

    /**
     * Where her feet are {@code age} ticks in, the ground {@code ground}: {x, y, z}; null before she is gone from where she threw the
     * punch, and once she has kicked off it, high up ({@link #FREE}: she is her own again, falling).
     */
    public static double[] her(double age, double ground) {
        if (age < BEHIND || age >= FREE) {
            return null;
        }
        if (age < UPPER + 0.5) {
            // Behind it (the far side), on the ground, crouched for the uppercut.
            double[] at = target(UPPER, ground);
            return new double[] {at[0], Math.min(0.0, ground), at[2] + STAND};
        }
        if (age < hitAt(0) - windUp(0)) {
            // Up after it at once.
            double[] at = target(age, ground);
            return new double[] {at[0], Math.max(Math.min(0.0, ground), at[1] - 0.6), at[2] + STAND};
        }
        int hit = hit(age);
        if (hit >= 0) {
            // Where what she strikes with meets its middle, from her side of it.
            double[] at = target(hitAt(hit) - 1.0E-6, ground);
            double[] a = AROUND[hit];
            return new double[] {at[0] + a[0] * STAND, at[1] + MID + a[1] * STAND - BLOWS[hit].reach(), at[2] + a[2] * STAND};
        }
        // Right above it, winding up; then, the stomp landed, springing up off it as it goes down.
        double[] at = target(Math.min(Math.max(age, ABOVE), DROP - 1.0E-6), ground);
        double up = age < DROP ? 0.0 : REBOUND * smooth((age - DROP) / (FREE - DROP));
        return new double[] {at[0], at[1] + OVER + up, at[2]};
    }

    /**
     * How far her whole body is tipped {@code age} ticks in (radians, + back: her front up) to aim a blow at it from above or below
     * (the uppercut and the stomp aim themselves).
     */
    public static double tilt(double age) {
        int hit = hit(age);
        if (hit < 0 || !BLOWS[hit].aimed()) {
            return 0.0;
        }
        return -Math.toRadians(ELEVATIONS[hit]) * 0.85;
    }

    /** Which way she faces {@code age} ticks in: her turn from the way the punch went (radians, + to her left), always at it. */
    public static double facing(double age) {
        if (age < BEHIND) {
            return 0.0;
        }
        int hit = hit(age);
        if (hit >= 0) {
            return Math.PI - BEARINGS[hit];
        }
        if (age < FIRST_HIT) {
            return Math.PI;
        }
        return Math.PI - BEARINGS[HITS - 1];
    }

    /** True on the ticks she blinks to a new place (gone from one, there at the next): behind it, each blitz blow, above it. */
    public static boolean blinks(int tick) {
        if (tick == BEHIND || tick == ABOVE) {
            return true;
        }
        for (int i = 0; i < HITS; i++) {
            if (tick == (int)Math.ceil(hitAt(i) - windUp(i))) {
                return true;
            }
        }
        return false;
    }

    /** Damage, times a fist: the uppercut, each blitz blow, the stomp down; and what the shock of the crater does round it. */
    public static final float UPPER_DAMAGE = 1.4F;
    public static final float HIT_DAMAGE = 0.3F;
    public static final float DROP_DAMAGE = 2.5F;
    public static final float SHOCK_DAMAGE = 1.2F;
    /**
     * 6.2.3: how long (ticks) it lies stunned in the crater before it gets up (a Speed Kick's lie is PaleRules.KICK_DOWN_TICKS): it
     * can do nothing until it is up.
     */
    public static final int DOWN_TICKS = 70;
    /** The crater (blocks, radius) and how far round it the shock throws everything up and away. */
    public static final double CRATER = 3.6;
    public static final double SHOCK = 8.0;

    static double smooth(double u) {
        double c = clamp01(u);
        return c * c * (3.0 - 2.0 * c);
    }

    static double clamp01(double v) {
        return Math.max(0.0, Math.min(1.0, v));
    }
}
