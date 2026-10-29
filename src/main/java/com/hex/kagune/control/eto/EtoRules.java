package com.hex.kagune.control.eto;

/**
 * Every number of Eto's kit (6.1.0; pure, tested): mastery gates, timings, reach, damage, RC costs and cooldowns. The server
 * runs her moves by these and every client animates them by the same timings, so a strike lands on the frame its fist does.
 * Times are game ticks from a move's start; distances in blocks; angles in degrees (a positive bearing is to her right).
 *
 * <p>Her base form is all speed and play: the fists (M1, {@link EtoStrike}), the flips (right click), Big Punch (Z held), and
 * from the upper back a mass of pale living flesh that sweeps (J tapped), grabs and throws (J held) and grows round her leg into a
 * club (V held); V tapped is a speed kick. Her kakuja is the Owl ({@link OwlRules}).
 */
public final class EtoRules {
    // ------------------------------------------------------------------ mastery gates
    public static final int BLITZ_BLINK_MASTERY = 100;
    public static final int SWIPE_MASTERY = 150;
    public static final int KICK_MASTERY = 250;
    public static final int BLITZ_FINISH_MASTERY = 350;
    public static final int GRAB_MASTERY = 450;
    public static final int HAMMER_MASTERY = 600;
    /** 6.2.0: Watch This (B), the two impaling tentacles ({@link WatchRules}). */
    public static final int WATCH_MASTERY = 650;
    /** Her kakuja, the Owl ({@link OwlRules#MASTERY}); 6.2.0: her only one (the sealed V2 is gone). */
    public static final int KAKUJA_V1_MASTERY = 750;

    /** Ticks Z, J or V must be held before it counts as held rather than tapped. */
    public static final int HOLD_TICKS = 6;
    /** While a key is held the client reminds the server this often; a hold not heard of for {@link #HOLD_TIMEOUT} ticks is let go. */
    public static final int HOLD_RESEND_TICKS = 4;
    public static final int HOLD_TIMEOUT = 12;

    // ------------------------------------------------------------------ the fists
    /** A fist's damage before a strike's multiplier (6.2.1: cut well down, it was killing iron golems in a blitz): 1.8 at mastery 0, 4.5 at 1000. */
    public static float fist(int mastery) {
        return 1.8F + 0.0027F * clampMastery(mastery);
    }

    /** A strike may begin this soon after the last one ended and still chain on; later the blitz starts over. */
    public static final int CHAIN_WINDOW = 12;
    /** A click this many ticks before the strike in progress ends waits for it instead of being dropped. */
    public static final int CLICK_BUFFER = 5;
    /** After the finisher she needs this long before the next blitz. */
    public static final int FINISH_RECOVERY = 8;
    /** A target further than a strike's reach but within this steps her in (the lunge's top speed, blocks a tick). */
    public static final double STEP_IN_RANGE = 6.0;
    public static final double STEP_IN_SPEED = 1.15;
    /**
     * How far from a body's centre (blocks, level) she stands when her fist lands on it: her blows reach about a block out in front
     * of her (the lunge carries the hips forward), so she stops a block short of its near side.
     */
    public static double strikeStand(double targetWidth) {
        return 1.05 + targetWidth * 0.5;
    }

    /** A blink needs a target this close. */
    public static final double BLINK_RANGE = 8.0;

    // ------------------------------------------------------------------ flips (right click)
    /** The four flips, by the movement key held (no key: forward). */
    public enum Flip {
        FORWARD(0.0, 1.18, 0.78), BACK(180.0, 0.98, 0.86), LEFT(-90.0, 1.02, 0.72), RIGHT(90.0, 1.02, 0.72);

        /** Direction from where she looks (degrees, positive to her right), launch speed along it and up (blocks a tick). */
        public final double bearing;
        public final double speed;
        public final double up;

        Flip(double bearing, double speed, double up) {
            this.bearing = bearing;
            this.speed = speed;
            this.up = up;
        }

        public static Flip byOrdinal(int ordinal) {
            Flip[] all = values();
            return ordinal >= 0 && ordinal < all.length ? all[ordinal] : FORWARD;
        }

        /** From the movement input: forward/back {@code forward} (+1 W, -1 S), sideways {@code strafe} (+1 A, -1 D). */
        public static Flip from(float forward, float strafe) {
            if (Math.abs(strafe) > Math.abs(forward) + 0.05F) {
                return strafe > 0.0F ? LEFT : RIGHT;
            }
            return forward < -0.05F ? BACK : FORWARD;
        }
    }

    /** The push is renewed for this many ticks so the ground's grip doesn't eat the first one. */
    public static final int FLIP_PUSH_TICKS = 2;
    /** The body turns over once in this many ticks (then holds upright until it lands). */
    public static final int FLIP_SPIN_TICKS = 17;
    /** Crouch before the takeoff (animation only: the push is at once, the legs are already driving). */
    public static final int FLIP_COOLDOWN = 14;
    public static final float FLIP_COST = 1.4F;
    /** A flip's move ends this many ticks after touchdown (the knees soak it up), or after the longest flight anyway. */
    public static final int FLIP_LAND_TICKS = 4;
    public static final int FLIP_MAX_TICKS = 60;

    /**
     * 6.2.0, Up We Go (her second mutation): a flip's launch built up over {@link #SOAR_RAMP} ticks, each setting her speed a little
     * higher ({@link #soarRamp}), to {@link #SOAR_UP} times the flip's own lift upward and {@link #SOAR_ALONG} times its speed along
     * its way: she rises the whole time, faster and faster, then flies on up on her own momentum, about two and a half times as high
     * as a flip. She turns over across the longer climb ({@link #SOAR_SPIN_TICKS}, from the top of the ramp).
     */
    public static final int SOAR_RAMP = 5;
    public static final double SOAR_UP = 1.5;
    public static final double SOAR_ALONG = 1.15;
    public static final int SOAR_SPIN_TICKS = 24;
    public static final int SOAR_MAX_TICKS = 110;
    /** How much of its full speed the launch has on each tick of the ramp: smoothly up to all of it. */
    private static final double[] SOAR_RAMP_SPEED = {0.42, 0.66, 0.84, 0.95, 1.0};

    /** How much (0..1) of an Up We Go launch's speed she has on ramp tick {@code tick} (0 first). */
    public static double soarRamp(int tick) {
        return SOAR_RAMP_SPEED[Math.max(0, Math.min(SOAR_RAMP_SPEED.length - 1, tick))];
    }

    /** An Up We Go launch on ramp tick {@code tick}: {along her way, up} (blocks a tick). */
    public static double[] soar(Flip flip, int tick) {
        double k = soarRamp(tick);
        return new double[] {flip.speed * SOAR_ALONG * k, flip.up * SOAR_UP * k};
    }

    // ------------------------------------------------------------------ Big Punch (Z held, released)
    public static final int PUNCH_CHARGE_TICKS = 30;
    /** Shorter holds still punch, at the least charge; holding past this lets it go by itself. */
    public static final int PUNCH_MAX_HOLD = 100;
    /** Released: she blinks in front of the target on this tick, already coiled, and the fist lands on the next. */
    public static final int PUNCH_BLINK = 1;
    public static final int PUNCH_IMPACT = 4;
    /** The punch's whole length (its extension held a moment, as everything round it is thrown), and the launched body watched. */
    public static final int PUNCH_TICKS = 18;
    public static final int PUNCH_FLIGHT = 40;
    public static final int PUNCH_COOLDOWN = 60;
    /** Where she reappears: this far in front of the target, toward where she came from. */
    public static final double PUNCH_ARRIVE = 1.25;

    /** 0..1 charge after holding {@code held} ticks (a floor of 0.15 for a quick release). */
    public static float punchCharge(double held) {
        return (float)Math.max(0.15, Math.min(1.0, held / PUNCH_CHARGE_TICKS));
    }

    /** How far she can blitz to a target at {@code charge}. */
    public static double punchRange(float charge) {
        return 8.0 + 16.0 * clamp01(charge);
    }

    public static float punchDamage(int mastery, float charge) {
        return fist(mastery) * (1.5F + 2.0F * clamp01(charge));
    }

    /** The launch: speed along her facing and up (blocks a tick). */
    public static double punchLaunch(float charge) {
        return 1.8 + 2.6 * clamp01(charge);
    }

    public static double punchLift(float charge) {
        return 0.25 + 0.2 * clamp01(charge);
    }

    public static float punchCost(float charge) {
        return 5.0F + 9.0F * clamp01(charge);
    }

    // ------------------------------------------------------------------ Flesh Swipe (J tapped)
    public static final int SWIPE_ERUPT = 5;
    public static final int SWIPE_SWEEP_END = 12;
    public static final int SWIPE_TICKS = 24;
    public static final double SWIPE_REACH = 7.0;
    /**
     * 6.2.0: how far (ticks) the blade at its end trails the flesh as it whips round (its weight: FleshPhysics.WHIP), so what it
     * cuts is cut as the bone gets there, not as the flesh's path does.
     */
    public static final double SWIPE_LAG = 1.5;
    /** The sweep runs from this far to her right round to the same to her left. */
    public static final double SWIPE_HALF_ANGLE = 78.0;
    public static final int SWIPE_COOLDOWN = 50;
    public static final float SWIPE_COST = 8.0F;
    public static final float SWIPE_DAMAGE = 2.2F;

    /** The sweep's leading edge (bearing, degrees) at {@code age}: held right while it erupts, swung across, held left after. */
    public static double swipeBearing(double age) {
        if (age <= SWIPE_ERUPT) {
            return SWIPE_HALF_ANGLE;
        }
        if (age >= SWIPE_SWEEP_END) {
            return -SWIPE_HALF_ANGLE;
        }
        double t = (age - SWIPE_ERUPT) / (SWIPE_SWEEP_END - SWIPE_ERUPT);
        // Heavy: it starts slow, whips through the middle and drags at the end.
        double eased = t * t * (3.0 - 2.0 * t);
        eased = 0.35 * t + 0.65 * eased;
        return SWIPE_HALF_ANGLE - 2.0 * SWIPE_HALF_ANGLE * eased;
    }

    /** How far out the flesh reaches at {@code age} (0..1 of {@link #SWIPE_REACH}): it erupts, sweeps at full length, contracts. */
    public static double swipeExtent(double age) {
        if (age < SWIPE_ERUPT) {
            return smooth(age / SWIPE_ERUPT) * 0.92;
        }
        if (age < SWIPE_SWEEP_END + 1.0) {
            return 0.92 + 0.08 * Math.sin(Math.PI * Math.min(1.0, (age - SWIPE_ERUPT) / (SWIPE_SWEEP_END + 1.0 - SWIPE_ERUPT)));
        }
        return 0.92 * (1.0 - smooth((age - SWIPE_SWEEP_END - 1.0) / (SWIPE_TICKS - SWIPE_SWEEP_END - 1.0)));
    }

    // ------------------------------------------------------------------ Flesh Grab and Throw (J held)
    public static final double GRAB_RANGE = 14.0;
    public static final int GRAB_HOLD_MAX = 100;
    /**
     * The throw: hauled up over her shoulder and all the way back ({@link #GRAB_PULL} ticks), hung there a moment, straining
     * ({@link #GRAB_HANG}), then whipped up over her and down out ahead, faster and faster ({@link #GRAB_WHIP}), and let go at
     * its fastest ({@link #grabRelease}); the flesh then draws back in ({@link #GRAB_RETRACT}).
     */
    public static final int GRAB_PULL = 7;
    public static final int GRAB_HANG = 2;
    public static final int GRAB_WHIP = 3;
    public static final int GRAB_RETRACT = 12;
    public static final int GRAB_MISS_TICKS = 14;
    public static final int GRAB_COOLDOWN = 120;
    public static final float GRAB_COST = 10.0F;
    /** RC a tick while she holds something. */
    public static final float GRAB_UPKEEP = 0.12F;
    public static final float GRAB_THROW_DAMAGE = 1.5F;
    public static final double GRAB_THROW_SPEED = 4.6;
    /** Where she holds what she caught: this far ahead of her and up from her feet. */
    public static final double GRAB_HOLD_AHEAD = 4.2;
    public static final double GRAB_HOLD_UP = 2.3;
    /**
     * Where the throw carries the body (blocks: to her right, up from her feet, ahead): over her right shoulder, high
     * ({@link #GRAB_HAUL_OVER}), to far behind her ({@link #GRAB_BACK}), a little further while it hangs ({@link #GRAB_STRAIN}), up
     * over her again ({@link #GRAB_WHIP_OVER}) and down to where it is let go ({@link #GRAB_FLING}).
     */
    public static final double[] GRAB_HAUL_OVER = {0.7, 6.4, 1.2};
    public static final double[] GRAB_BACK = {0.9, 3.4, -3.6};
    public static final double[] GRAB_STRAIN = {1.0, 3.1, -4.0};
    public static final double[] GRAB_WHIP_OVER = {0.6, 7.2, -0.6};
    public static final double[] GRAB_FLING = {0.3, 1.6, 3.6};

    /** Ticks into the throw when the body is let go. */
    public static int grabRelease() {
        return GRAB_PULL + GRAB_HANG + GRAB_WHIP;
    }

    /**
     * Where the thrown body's middle is {@code age} ticks into the throw (blocks: to her right, up from her feet, ahead of her),
     * from where it was held when the throw began ({@code from}, the same way).
     */
    public static double[] throwAt(double age, double[] from) {
        if (age < GRAB_PULL) {
            return bezier(from, GRAB_HAUL_OVER, GRAB_BACK, smooth(age / GRAB_PULL));
        }
        if (age < GRAB_PULL + GRAB_HANG) {
            return bezier(GRAB_BACK, lerp3(GRAB_BACK, GRAB_STRAIN, 0.5), GRAB_STRAIN, smooth((age - GRAB_PULL) / GRAB_HANG));
        }
        double u = Math.min(1.0, (age - GRAB_PULL - GRAB_HANG) / GRAB_WHIP);
        // Faster and faster: at its fastest when it is let go.
        return bezier(GRAB_STRAIN, GRAB_WHIP_OVER, GRAB_FLING, u * u);
    }

    static double[] bezier(double[] a, double[] b, double[] c, double t) {
        double[] out = new double[3];
        for (int k = 0; k < 3; k++) {
            out[k] = (1.0 - t) * (1.0 - t) * a[k] + 2.0 * t * (1.0 - t) * b[k] + t * t * c[k];
        }
        return out;
    }

    static double[] lerp3(double[] a, double[] b, double t) {
        return new double[] {a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t, a[2] + (b[2] - a[2]) * t};
    }

    /** Ticks the flesh takes to reach something {@code distance} away. */
    public static int grabReachTicks(double distance) {
        return (int)Math.max(4, Math.min(11, Math.round(3.0 + distance * 0.55)));
    }

    // ------------------------------------------------------------------ Speed Kick (V tapped)
    /*
     * 6.2.0, the Speed Kick (Kizaru's kick, with everything behind it): V tapped picks what is closest to her crosshair, in front of
     * her and in her sight, within KICK_RANGE (with nothing there, only "No target found": no move, no cost, no cooldown). She throws
     * herself forward into a sprinter's lunge (KICK_BURST: leaned far over, the right arm reaching out ahead, the left swept back with
     * its forearm hanging, the left knee driven up under her, the right leg stretched out far behind: the user's first stick figure)
     * and is gone: all through the dash nothing of her is there to see (not her body, not her shadow; only the ground torn up along
     * the way and the wind) and nothing can hurt her. She is there in front of it (kickArrive), already thrown back into the kick
     * with her right knee drawn up hard, and drives the leg out straight into its middle (KICK_CONTACT), her whole body thrown out
     * flat in the air behind it, a flying side kick (the user's second stick figure): its gut, or its backside if it is turned away.
     * It is launched straight away from her, low and very far (KICK_SPEED, carried: no air drag), smashing through anything up to
     * iron on the way (KICK_HARDEST) and bowling over what it flies into, tumbling (KickTumble). She holds the leg out flat a moment
     * (KICK_HOLD), rebounds off it in a backflip (KICK_TUCK, KICK_OPEN, thrown up KICK_REBOUND), lands in a crouch (KICK_LAND) and
     * comes up into her guard (KICK_TICKS). What she kicks is held where it is, too slow to react, until the blow.
     */
    public static final double KICK_RANGE = 20.0;
    /** How far off her crosshair (degrees) what she kicks may be. */
    public static final double KICK_CONE = 40.0;
    /** Ticks she is seen leaning into the dash before she is gone. */
    public static final int KICK_BURST = 2;
    /**
     * The dash (she is gone): on average no faster than this (blocks a tick), between {@link #KICK_DASH_MIN} and {@link #KICK_DASH}
     * ticks; its fastest tick is {@link #KICK_DASH_PEAK} times the average, under the 10 blocks a tick a server lets a player move.
     * If she must be up off the ground to reach its middle, she rises over the last {@link #KICK_SPRING} of it.
     */
    public static final double KICK_DASH_SPEED = 5.0;
    public static final double KICK_DASH_PEAK = 1.4;
    public static final int KICK_DASH = 4;
    public static final int KICK_DASH_MIN = 2;
    public static final double KICK_SPRING = 0.4;
    /**
     * Ticks after she is there: the blow (the leg driven out of the chamber into its middle, her body thrown out flat behind it),
     * held out flat; the rebound off it, a backflip, upside down and tucked at {@link #KICK_TUCK}, opening out at {@link #KICK_OPEN},
     * down in a crouch at {@link #KICK_LAND}; and done. The rebound throws her up {@link #KICK_REBOUND} blocks at its height.
     */
    public static final int KICK_CONTACT = 1;
    public static final double KICK_HOLD = 3.0;
    public static final double KICK_TUCK = 4.8;
    public static final double KICK_OPEN = 6.6;
    public static final double KICK_LAND = 8.0;
    public static final int KICK_TICKS = 13;
    public static final double KICK_REBOUND = 1.2;
    public static final int KICK_COOLDOWN = 60;
    public static final float KICK_COST = 9.0F;
    public static final float KICK_DAMAGE = 3.2F;
    /**
     * Where the sole of her right foot is at the blow (blocks; x to her right, y up from her feet, z ahead, as she faced the target
     * when the kick began): her body thrown out flat behind the leg, the leg straight and level ahead of her (measured off the pose,
     * and tested). She is up off the ground by however much more it takes for it to be at the middle of what she kicks.
     */
    public static final double KICK_FOOT_X = 0.05;
    public static final double KICK_FOOT_Y = 0.82;
    public static final double KICK_FOOT_Z = 0.70;
    public static final double KICK_LIFT_MAX = 1.6;
    /** Where on what she kicks the foot goes: this far up its height (its middle: the gut, or the backside of a body turned away). */
    public static final double KICK_AIM = 0.5;
    /**
     * How hard the body flies: off at {@link #KICK_SPEED} (blocks a tick) straight away from her and {@link #KICK_UP} up; the kick
     * carries it (no air drag), keeping {@link #KICK_CARRY} of its speed a tick until it comes down, for at most
     * {@link #KICK_FLIGHT_TICKS}: about 55 blocks on open ground.
     */
    public static final double KICK_SPEED = 4.6;
    public static final double KICK_UP = 0.5;
    public static final double KICK_CARRY = 0.98;
    public static final int KICK_FLIGHT_TICKS = 60;
    /**
     * What it smashes through on the way: anything up to this hard (stone, deepslate, ores, bricks, wood, dirt, glass, even iron and
     * diamond blocks; never obsidian or anything unbreakable), each block taking {@link #KICK_PER_BLOCK} of its speed, at most
     * {@link #KICK_MOST_BLOCKS} a flight (each put back exactly later, as every smashed block is). What it flies into is hurt
     * ({@link #KICK_BOWLED} of the blow) and thrown aside.
     */
    public static final float KICK_HARDEST = 5.0F;
    public static final double KICK_PER_BLOCK = 0.975;
    public static final int KICK_MOST_BLOCKS = 240;
    public static final float KICK_BOWLED = 0.5F;
    /** How little its weight holds it back (an iron golem flies too). */
    public static final double KICK_RESIST = 0.15;
    /** Once it comes down: how long (ticks) it lies there, held down, and how long it takes to get up. */
    public static final int KICK_DOWN_TICKS = 28;
    public static final int KICK_GETUP_TICKS = 12;
    /** How far from where she meant to be (blocks) she may be at the blow (the server hears where she is a little late). */
    public static final double KICK_THERE = 2.0;
    /** How far (blocks) off the front of its body the foot may be and still land. */
    public static final double KICK_SLACK = 0.9;

    /** How far off the ground she is to put her foot at a middle {@code middle} blocks above the ground she stands on. */
    public static double kickLift(double middle) {
        return Math.max(0.0, Math.min(KICK_LIFT_MAX, middle - KICK_FOOT_Y));
    }

    /**
     * How many ticks the dash takes over {@code distance} blocks: never fewer than {@link #KICK_DASH_MIN}, never more than
     * {@link #KICK_DASH}.
     */
    public static int kickDash(double distance) {
        return (int)Math.max(KICK_DASH_MIN, Math.min(KICK_DASH, Math.ceil(distance / KICK_DASH_SPEED)));
    }

    /** The tick she is there in front of it (the dash {@code dash} ticks): the lunge, then the dash. */
    public static int kickArrive(int dash) {
        return KICK_BURST + Math.max(1, dash);
    }

    /** True while she is gone ({@code age} ticks in, the dash {@code dash} ticks): nothing of her is drawn. */
    public static boolean kickGone(int dash, double age) {
        return age >= KICK_BURST && age < kickArrive(dash);
    }

    /** True while nothing can hurt her: from the lunge until she is there. */
    public static boolean kickUntouchable(int dash, double age) {
        return age >= 0.0 && age < kickArrive(dash);
    }

    /**
     * How far up (0..1 of her lift) she is {@code age} ticks into the kick, the dash {@code dash} ticks: on the ground in the lunge
     * and most of the dash, rising over its last {@link #KICK_SPRING} (unseen); up there through the blow and the hold; then
     * dropping, down by {@link #KICK_LAND} after she was there.
     */
    public static double kickRise(int dash, double age) {
        int ticks = Math.max(1, dash);
        double since = age - kickArrive(dash);
        if (since >= KICK_LAND) {
            return 0.0;
        }
        if (since < 0.0) {
            double s = ((age - KICK_BURST) / ticks - (1.0 - KICK_SPRING)) / KICK_SPRING;
            return s <= 0.0 ? 0.0 : s >= 1.0 ? 1.0 : s * s * (3.0 - 2.0 * s);
        }
        if (since < KICK_HOLD) {
            return 1.0;
        }
        double u = (since - KICK_HOLD) / (KICK_LAND - KICK_HOLD);
        return 1.0 - u * u;
    }

    /** How far (blocks) the rebound off it throws her up {@code age} ticks in: a hop from the hold to the landing, highest midway. */
    public static double kickHop(int dash, double age) {
        double u = (age - kickArrive(dash) - KICK_HOLD) / (KICK_LAND - KICK_HOLD);
        return u <= 0.0 || u >= 1.0 ? 0.0 : KICK_REBOUND * 4.0 * u * (1.0 - u);
    }

    /** How far up (blocks) her feet are {@code age} ticks in, her lift {@code lift}: risen to the blow, and the rebound's hop. */
    public static double kickHeight(int dash, double lift, double age) {
        return lift * kickRise(dash, age) + kickHop(dash, age);
    }

    /**
     * How much of her (0..1) is in the air {@code age} ticks in: none while she stands (the lunge, the chamber, the crouch), all of her
     * from the blow until her feet come back under her ({@link #KICK_OPEN}); her body is held at its own height there rather than set
     * down on her left foot.
     */
    public static double kickAirborne(int dash, double age) {
        double since = age - kickArrive(dash);
        if (since <= 0.0 || since >= KICK_LAND) {
            return 0.0;
        }
        if (since < KICK_CONTACT) {
            double u = since / KICK_CONTACT;
            return u * u * (3.0 - 2.0 * u);
        }
        if (since <= KICK_OPEN) {
            return 1.0;
        }
        double u = (since - KICK_OPEN) / (KICK_LAND - KICK_OPEN);
        return 1.0 - u * u * (3.0 - 2.0 * u);
    }

    /** True while the dash is on (the lunge done, she is shooting at it unseen): {@code age} ticks in, the dash {@code dash} ticks. */
    public static boolean kickDashing(int dash, double age) {
        return kickGone(dash, age);
    }

    /**
     * Where her feet are {@code age} ticks into the kick (the client moves her there; blocks, relative to where she was when it began),
     * {@code to} where she kicks from (relative to the same, on the ground), the dash {@code dash} ticks, her lift {@code lift}: where
     * she was through the lunge; the dash, fastest off the mark and slowing as she gets there, rising if she must; held up there
     * through the blow; then the rebound's hop, coming down where she kicked from. Null once she has landed (she is her own again).
     */
    public static double[] kickPlace(double[] to, int dash, double lift, double age) {
        int ticks = Math.max(1, dash);
        int arrive = kickArrive(dash);
        if (age >= arrive + KICK_LAND) {
            return null;
        }
        double rise = kickHeight(dash, lift, age);
        if (age < arrive) {
            // Fastest off the mark, slowing as she gets there.
            double s = Math.max(0.0, age - KICK_BURST) / ticks;
            double u = s * (KICK_DASH_PEAK - (KICK_DASH_PEAK - 1.0) * s);
            return new double[] {to[0] * u, to[1] * u + rise, to[2] * u};
        }
        return new double[] {to[0], to[1] + rise, to[2]};
    }

    /**
     * Where she kicks from to put her foot on the front of a body {@code half} wide (half its width) from its centre line (blocks,
     * {x to her right, z ahead}, as she faced it): straight in front of it, the leg's reach and its half width off.
     */
    public static double[] kickStand(double half) {
        return new double[] {-KICK_FOOT_X, -(KICK_FOOT_Z + half)};
    }

    /** The way the kicked body flies (a level unit vector, {x to her right, z ahead}): straight away from her, the way the foot drove. */
    public static double[] kickFlight() {
        return new double[] {0.0, 1.0};
    }

    /**
     * How fast (blocks a tick) a body flies {@code ticks} after the kick, off at {@code speed}, through {@code blocks} blocks so far:
     * carried, slowing a little each tick and each block it breaks.
     */
    public static double kickCarried(double speed, int ticks, int blocks) {
        return speed * Math.pow(KICK_CARRY, Math.max(0, ticks)) * Math.pow(KICK_PER_BLOCK, Math.max(0, blocks));
    }

    // ------------------------------------------------------------------ Flesh Hammer (V held, released)
    public static final int HAMMER_RAISE_TICKS = 10;
    public static final int HAMMER_GROW_TICKS = 30;
    public static final int HAMMER_MAX_HOLD = 80;
    /** The slam: the last of the wind-up (the leg cocked furthest back) this many ticks after it is let go, the blow at impact. */
    public static final int HAMMER_COCK = 2;
    public static final int HAMMER_IMPACT = 6;
    public static final int HAMMER_TICKS = 30;
    public static final int HAMMER_COOLDOWN = 100;
    /**
     * The club sprouts out of her foot this many ticks into the raise (once the leg is up: it grows on up into the sky, never
     * down at the ground); after the blow it lies on the ground until {@link #HAMMER_RETRACT_AT}, then draws back into her leg
     * over {@link #HAMMER_RETRACT} ticks.
     */
    public static final int HAMMER_SPROUT = 5;
    public static final int HAMMER_RETRACT_AT = 16;
    public static final int HAMMER_RETRACT = 10;
    /** RC the charge needs to start (the rest is paid as it is let go). */
    public static final float HAMMER_START_COST = 4.0F;

    public static float hammerCharge(double held) {
        return (float)Math.max(0.1, Math.min(1.0, held / HAMMER_GROW_TICKS));
    }

    /**
     * 6.2.0, True Anonymity (a mutation): the club grows on past its full size, {@link #HAMMER_HUGE} times as far at the full charge:
     * longer, thicker, reaching further, its shockwave wider, its crater bigger, its blow harder; bone spikes all over it, and what
     * the blow catches bleeds. Its charge then runs up to {@link #HAMMER_HUGE} (everything the club's size is worked out from takes
     * it as it is).
     */
    public static final float HAMMER_HUGE = 1.7F;

    public static float hammerCharge(double held, boolean huge) {
        return hammerCharge(held) * (huge ? HAMMER_HUGE : 1.0F);
    }

    /** How grown the club is at {@code charge}: 0..1 as ever, on to {@link #HAMMER_HUGE} with True Anonymity. */
    static double grown(float charge) {
        return Math.max(0.0, Math.min(HAMMER_HUGE, charge));
    }

    /** The club's length past the foot (blocks) at {@code charge}. */
    public static double hammerLength(float charge) {
        return 0.9 + 2.3 * grown(charge);
    }

    /** The club's thickest radius (blocks). */
    public static double hammerGirth(float charge) {
        return 0.2 + 0.3 * grown(charge);
    }

    public static double hammerRadius(float charge) {
        return 3.0 + 2.5 * grown(charge);
    }

    public static float hammerDamage(int mastery, float charge) {
        return fist(mastery) * (1.6F + 2.4F * (float)grown(charge));
    }

    public static double hammerLaunch(float charge) {
        return 1.0 + 1.6 * grown(charge);
    }

    /**
     * How far ahead of her feet the club's head comes down (blocks), {@code charge} grown: her foot out ahead of her, and most of the
     * club's length past it (as the slam has it, EtoMotions.clubHeadAt).
     */
    public static double hammerReach(float charge) {
        return HAMMER_FOOT_AHEAD + HAMMER_HEAD_ALONG * hammerLength(charge);
    }

    static final double HAMMER_FOOT_AHEAD = 0.62;
    static final double HAMMER_HEAD_ALONG = 0.85;

    /** How big the crater (blocks across, radius) the blow knocks into the ground. */
    public static double hammerCrater(float charge) {
        return 0.9 + 1.3 * grown(charge);
    }

    public static float hammerCost(float charge) {
        return 10.0F + 10.0F * clamp01(charge);
    }

    // ------------------------------------------------------------------ RC

    /** RC a tick back while her form is out: a little for a new ghoul, 2.4 a second at mastery 1000. */
    public static float formRegen(int mastery) {
        return 0.03F + 0.09F * clampMastery(mastery) / 1000.0F;
    }

    // ------------------------------------------------------------------ helpers

    static int clampMastery(int mastery) {
        return Math.max(0, Math.min(1000, mastery));
    }

    public static float clamp01(float value) {
        return Float.isFinite(value) ? Math.max(0.0F, Math.min(1.0F, value)) : 0.0F;
    }

    public static double smooth(double value) {
        double t = Math.max(0.0, Math.min(1.0, value));
        return t * t * (3.0 - 2.0 * t);
    }

    private EtoRules() {
    }
}
