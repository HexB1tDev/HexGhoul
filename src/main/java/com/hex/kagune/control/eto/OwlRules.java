package com.hex.kagune.control.eto;

/**
 * 6.2.0, Eto's first kakuja, the Owl, in the game (K tap; pure, tested): she doubles over, her raw pale flesh bursts out of her
 * shoulders and the kakuja hardens over it (the red giant arms and their white blades, the mask, the spikes, her body bulked up
 * under it); K again and it draws back into her. In it, her moves are its own, one by one (its Big Punch: {@link #punchDistance}).
 */
public final class OwlRules {
    public static final int MASTERY = EtoRules.KAKUJA_V1_MASTERY;
    /** Growing it (ticks), and the tick the flesh bursts out of her (doubled over until then). */
    public static final int GROW_TICKS = 34;
    public static final int BURST = 9;
    /** Turning back (ticks). */
    public static final int SHED_TICKS = 22;
    /** RC to grow it; ticks before K is heard again after either. */
    public static final float COST = 15.0F;
    public static final int COOLDOWN = 20;

    /** 6.2.1: in it, all grown out, she takes only this share of any hurt (the kakuja's hide shrugs off the rest): a fortress. */
    public static final float TAKEN = 0.15F;

    /** 6.2.1: what of {@code amount} reaches her through the Owl. */
    public static float taken(float amount) {
        return amount * TAKEN;
    }

    private OwlRules() {
    }

    // ------------------------------------------------------------------ its Big Punch (Z held, let go)

    /** How far (blocks) the body it strikes flies, dead level along the blow: at the least charge, and at full. */
    public static final double PUNCH_NEAR = 16.0;
    public static final double PUNCH_FAR = 56.0;
    /** Its speed off the blow (blocks a tick), least and full: the blow's force, spent evenly over the flight. */
    public static final double PUNCH_SPEED_NEAR = 2.4;
    public static final double PUNCH_SPEED_FAR = 4.2;
    /** How far along its flight (blocks from where it was struck) it breaks through whatever is in its way; past it, what holds stops it. */
    public static final double PUNCH_SMASH_REACH = 10.0;
    /** Nothing tougher than this breaks (destroy time: stone 1.5, deepslate 3, an iron block 5; obsidian, 50, holds). */
    public static final float PUNCH_HARDEST = 10.0F;
    /** Each block it breaks takes this much of its speed (never its line). */
    public static final double PUNCH_PER_BLOCK = 0.97;
    /** At most this many blocks broken in one flight. */
    public static final int PUNCH_MOST_BLOCKS = 220;
    /** The Owl's blow over the Big Punch's own damage. */
    public static final float PUNCH_DAMAGE = 1.6F;
    /** Knockback resistance takes at most this much of the distance (never of its line or its speed off the blow). */
    public static final double PUNCH_RESIST = 0.4;
    /** What it slams into on the way is hurt this much of the blow and thrown aside. */
    public static final float PUNCH_BOWLED = 0.4F;
    /** A flight is watched at most this long (ticks). */
    public static final int PUNCH_FLIGHT_TICKS = 60;

    // ------------------------------------------------------------------ its Speed Kick (V tapped)

    /**
     * How far in front of what it kicks she plants (blocks, from its middle: the right giant arm's reach through it), and how fast the
     * dash there is on average (blocks a tick: near invisible), between {@link #KICK_DASH_MIN} and {@link #KICK_DASH_MAX} ticks; its
     * fastest tick {@link #KICK_DASH_PEAK} times the average (under the 10 blocks a tick a server lets a player move).
     */
    public static final double KICK_STAND = 2.0;
    /** 6.2.0: how far off (blocks) what the Owl kicks may be (the base form's Speed Kick reaches only {@link EtoRules#KICK_RANGE}). */
    public static final double KICK_RANGE = 40.0;
    public static final double KICK_DASH_SPEED = 6.0;
    public static final double KICK_DASH_PEAK = 1.4;
    public static final int KICK_DASH_MIN = 2;
    public static final int KICK_DASH_MAX = 7;
    /**
     * After the dash: the right giant arm's blade through it ({@link #KICK_SKEWER}, held on it), the left one's across its neck
     * ({@link #KICK_SLASH}), the right drawn back out of it ({@link #KICK_PULL}), the spinning heel into it ({@link #KICK_BLOW}),
     * and she is herself again by {@link #KICK_TICKS}.
     */
    public static final int KICK_SKEWER = 2;
    public static final int KICK_SLASH = 5;
    public static final int KICK_PULL = 7;
    public static final int KICK_BLOW = 10;
    public static final int KICK_TICKS = 18;
    /** Each blow's damage, in her fist's: the skewer, the slash, the heel. */
    public static final float KICK_SKEWER_DAMAGE = 1.2F;
    public static final float KICK_SLASH_DAMAGE = 1.0F;
    public static final float KICK_BLOW_DAMAGE = 2.8F;
    /** The heel sends it flying away from her (blocks a tick) and up; knockback resistance takes at most this much of it. */
    public static final double KICK_FLIGHT = 2.6;
    public static final double KICK_UP = 0.45;
    public static final double KICK_RESIST = 0.15;
    /** Too far from where she planted (she was moved off it) by the skewer's tick: nothing lands. */
    public static final double KICK_THERE = 2.0;
    /** Through the spin (from the blade drawn out to the heel) she steps in this far at it: her heel reaches what her blade did. */
    public static final double KICK_STEP = 1.05;

    /** The dash's ticks over {@code distance} blocks: quick, never fewer than {@link #KICK_DASH_MIN}, never more than the most. */
    public static int kickDash(double distance) {
        return (int)Math.max(KICK_DASH_MIN, Math.min(KICK_DASH_MAX, Math.ceil(distance / KICK_DASH_SPEED)));
    }

    /**
     * Where her feet are {@code age} ticks into the kick (the client moves her there; blocks from where she was when it began),
     * {@code to} where she plants, the kick facing {@code yaw} (radians, as the game turns): the dash along the ground, fastest off the
     * mark and slowing as she gets there; held there through the skewer and the slash; stepping in at it through the spin
     * ({@link #kickStep}). Null once it is over (she is her own again).
     */
    public static double[] kickPlace(double[] to, int dash, double yaw, double age) {
        int ticks = Math.max(1, dash);
        if (age >= ticks + KICK_TICKS) {
            return null;
        }
        if (age < ticks) {
            double s = Math.max(0.0, age) / ticks;
            double u = s * (KICK_DASH_PEAK - (KICK_DASH_PEAK - 1.0) * s);
            return new double[] {to[0] * u, to[1] * u, to[2] * u};
        }
        double step = kickStep(age - ticks);
        return new double[] {to[0] - Math.sin(yaw) * step, to[1], to[2] + Math.cos(yaw) * step};
    }

    /** How far (blocks) she has stepped in at it {@code since} ticks after the dash: none until the blade is drawn out, all of it by the heel. */
    public static double kickStep(double since) {
        return KICK_STEP * smooth((since - KICK_PULL) / (KICK_BLOW - KICK_PULL));
    }

    // ------------------------------------------------------------------ its Rend (M1)

    /**
     * The Owl's M1, its Rend (6.2.4: its maul): four blows click by click (its right claw raked across, its left claw back across, its
     * kagune's points driven in, its jaws snapped shut on it), each with the whole creature behind it. Each stage's length, the tick
     * its blow lands, and the tick a click kept for the next one throws it.
     */
    public static final int REND_STAGES = 4;
    public static final int[] REND_TICKS = {9, 9, 10, 15};
    public static final int[] REND_IMPACT = {4, 4, 5, 7};
    public static final int[] REND_CHAIN = {6, 6, 7, Integer.MAX_VALUE};
    /** How far (blocks, from her eyes to its near side) each cut reaches, and how wide (degrees either side of where she faces). */
    public static final double[] REND_REACH = {3.9, 3.9, 3.6, 4.3};
    public static final double[] REND_ARC = {75.0, 75.0, 60.0, 45.0};
    /** Each cut's hurt (in her fist's) and RC. */
    public static final float[] REND_DAMAGE = {1.4F, 1.4F, 1.6F, 2.6F};
    public static final float[] REND_COST = {2.5F, 2.5F, 3.0F, 5.0F};
    /** Where each cut throws what it cuts (blocks a tick: back from her, to her right, up): the way the blade went. */
    public static final double[][] REND_KNOCK = {{0.5, -0.55, 0.15}, {0.5, 0.55, 0.15}, {0.95, 0.0, 0.2}, {0.55, 0.0, -0.2}};
    /** She steps in through each cut's wind-up until this far (blocks) from what she cuts: the blades' reach. */
    public static final double REND_STAND = 2.4;
    /** The finisher's jaws close this far ahead (blocks); its Pounce's two hands crater the ground this far either side. */
    public static final double REND_BITE_AHEAD = 2.3;
    public static final double REND_BITE_SIDE = 0.5;

    /** The cut after {@code previous} (-1: none): on through the four, then the first again. */
    public static int nextRend(int previous) {
        return previous < 0 || previous >= REND_STAGES - 1 ? 0 : previous + 1;
    }

    // ------------------------------------------------------------------ its Pounce (M2)

    /**
     * The Owl's M2, its Pounce: sunk down with the blades reared up behind her for {@link #LEAP_CROUCH} ticks, then a great bound
     * (the flip's way, further along and flatter than a flip: {@link #LEAP_ALONG}, {@link #LEAP_UP}; with Up We Go higher and further
     * still), and as she lands both blades driven down into the ground ahead of her: the slam.
     */
    public static final int LEAP_CROUCH = 3;
    public static final double LEAP_ALONG = 1.15;
    public static final double LEAP_UP = 0.9;
    public static final double LEAP_SOAR_ALONG = 1.3;
    public static final double LEAP_SOAR_UP = 1.35;
    /** Still in the air this long (ticks) and it is given up (no slam); after the slam she is herself again this long after. */
    public static final int LEAP_MOST = 60;
    public static final int LEAP_RECOVER = 14;
    /** The blades come down this many ticks after she touches down: the slam lands then. */
    public static final int LEAP_SLAM_AFTER = 2;
    /** The slam: this far ahead of her feet (blocks), reaching this far round it (blocks), its hurt (her fist's), its craters. */
    public static final double LEAP_SLAM_AHEAD = 2.2;
    public static final double LEAP_SLAM_REACH = 3.2;
    public static final float LEAP_DAMAGE = 2.2F;
    public static final double LEAP_CRATER = 1.1;
    public static final int LEAP_COOLDOWN = 30;
    public static final float LEAP_COST = 4.0F;

    /** The pounce's launch off the ground ({along her way, up}, blocks a tick) for {@code flip}'s way, soaring or not. */
    public static double[] leap(EtoRules.Flip flip, boolean soaring) {
        double along = flip.speed * LEAP_ALONG * (soaring ? LEAP_SOAR_ALONG : 1.0);
        double up = flip.up * LEAP_UP * (soaring ? LEAP_SOAR_UP : 1.0);
        return new double[] {along, up};
    }

    /** A pounce's detail: which way it went and whether it soars (sent with it). */
    public static int leapDetail(EtoRules.Flip flip, boolean soaring) {
        return flip.ordinal() | (soaring ? 16 : 0);
    }

    // ------------------------------------------------------------------ its Fissure (V held)

    /**
     * The Owl's V held, its Fissure: held, she rears up onto her toes, both giant arms raised high, charging as the Flesh
     * Hammer does ({@link EtoRules#hammerCharge}); let go, both blades and all her weight come down on one place
     * {@link #FISSURE_AHEAD} ahead ({@link #FISSURE_IMPACT} ticks after), and the ground splits from there on along the way she faces,
     * {@link #fissureLength} blocks, running out {@link #FISSURE_SPEED} blocks a tick: knocked out of the ground (put back later), and
     * everything along it thrown up into the air.
     */
    public static final int FISSURE_IMPACT = 6;
    public static final int FISSURE_TICKS = 22;
    public static final double FISSURE_AHEAD = 2.4;
    public static final double FISSURE_SPEED = 2.0;
    /** How far either side of its line it throws what stands there (blocks), how high (blocks a tick), its hurt (her fist's). */
    public static final double FISSURE_WIDTH = 1.4;
    public static final double FISSURE_UP = 0.95;
    public static final float FISSURE_DAMAGE = 2.4F;
    /** The craters along it: one every so far (blocks), this wide, wider where it begins. */
    public static final double FISSURE_STEP = 1.0;
    public static final double FISSURE_CRATER = 0.85;

    /** How far (blocks) the ground splits from where the blades come down, at {@code charge}. */
    public static double fissureLength(float charge) {
        return 5.0 + 6.0 * clamp01(charge);
    }

    /** How far (blocks) the split has run {@code age} ticks into the slam (nothing before the blades come down). */
    public static double fissureRun(double age, float charge) {
        return age < FISSURE_IMPACT ? -1.0 : Math.min(fissureLength(charge), (age - FISSURE_IMPACT) * FISSURE_SPEED);
    }

    // ------------------------------------------------------------------ its Watch This (B): the lances

    /**
     * The Owl's B, its Watch This ({@link OwlLances}): she hunches, then rears up with the giant arms flung wide as six lances of red
     * muscle tipped with barbed bone burst out of her back into a crown over her. For {@link #LANCES_ACTIVE} ticks they hunt on their
     * own, each going for something hostile to her on its own side of her: it rears high and dives onto it from above, the bone driven
     * through it and on into the ground; pinned there a moment, it is hoisted up over her on the barbs and slammed into the ground on
     * her far side; the lance tears free and hunts on. Then they draw back into her. On Watch This's own cost and cooldown.
     */
    public static final int LANCES_POSE_TICKS = 24;
    /** The lances burst out this far into her pose, and take this long to be all out. */
    public static final int LANCES_ERUPT_AT = 6;
    public static final int LANCES_ERUPT = 5;
    /** Out exactly ten seconds (from the burst to the moment they start back in), as Watch This's tentacles are; this long going in. */
    public static final int LANCES_ACTIVE = 200;
    public static final int LANCES_RETRACT = 12;
    /** A strike: rearing up high, then the dive ({@link OwlLances#diveTicks}). */
    public static final int LANCE_REAR = 3;
    /** Pinned to the ground on it; then the hoist up over her and the slam ({@link OwlLances#hoist}). */
    public static final int LANCE_PIN = 6;
    public static final int LANCE_HOIST = 11;
    /** Torn free after the slam and back into the crown; after a miss. */
    public static final int LANCE_RECOVER = 8;
    public static final int LANCE_MISS = 6;
    /** What is this near her (blocks, chest to chest) is hunted; the furthest a lance reaches from where it rears ({@link OwlLances#pivot}). */
    public static final double LANCE_RANGE = 11.0;
    public static final double LANCE_REACH = 13.5;
    /** The dive's hurt and the slam's, times a fist's. */
    public static final float LANCE_DAMAGE = 2.2F;
    public static final float LANCE_SLAM_DAMAGE = 1.6F;
    /** A body just slammed is not hunted again for this long (ticks). */
    public static final int LANCE_SLAMMED_FREE = 24;
    /** The crater where a body is slammed down (its radius, blocks). */
    public static final double LANCE_CRATER = 0.9;

    // ------------------------------------------------------------------ its Reaping Whirl (J tapped)

    /**
     * The Owl's J tapped, its Reaping Whirl: coiled up to her right ({@link #WHIRL_WIND} ticks), then a whole turn round to her left
     * with both giant arms out at full reach (from {@link #WHIRL_START} to {@link #WHIRL_END}, speeding up into it and slowing out of
     * it: {@link #whirlTurn}), the blades reaping everything round her; on round a little, and back into her stance by
     * {@link #WHIRL_TICKS}.
     */
    public static final int WHIRL_WIND = 3;
    public static final int WHIRL_START = 3;
    public static final int WHIRL_END = 13;
    public static final int WHIRL_TICKS = 20;
    /** What the blades reach (blocks from her middle: from close in to their points), and how high round her. */
    public static final double WHIRL_NEAR = 0.6;
    public static final double WHIRL_REACH = 4.6;
    public static final float WHIRL_DAMAGE = 1.8F;
    public static final float WHIRL_COST = 10.0F;
    /** What it reaps is thrown out from her (blocks a tick) and along the way the blade went, a little up. */
    public static final double WHIRL_OUT = 0.9;
    public static final double WHIRL_ALONG = 0.7;
    public static final double WHIRL_UP = 0.3;

    /**
     * How far round (radians, + to her left) she has turned {@code age} ticks into the whirl: none through the coil, then the whole
     * turn, speeding up into its first half and slowing out of its second (the same eases her body is drawn with), all of it after.
     */
    public static double whirlTurn(double age) {
        if (age <= WHIRL_START) {
            return 0.0;
        }
        double half = (WHIRL_END - WHIRL_START) * 0.5;
        if (age < WHIRL_START + half) {
            return Math.PI * Math.pow((age - WHIRL_START) / half, 2.2);
        }
        if (age < WHIRL_END) {
            double u = (age - WHIRL_START - half) / half;
            return Math.PI + Math.PI * (1.0 - (1.0 - u) * (1.0 - u));
        }
        return Math.PI * 2.0;
    }

    // ------------------------------------------------------------------ its bone spikes (J held)

    /** Ticks she hunches before the first spike bursts out of her ({@link OwlSpikes#FIRST}). */
    public static final double SPIKES_HUNCH = 4.0;
    /** Held out at most this long (ticks) once they are all out; then they go back in whether J is held or not. */
    public static final int SPIKES_HOLD = 40;
    /** RC for the burst, and the hurt of a spike (in her fist's). */
    public static final float SPIKES_COST = 14.0F;
    public static final float SPIKES_DAMAGE = 1.9F;
    /** A body this close (blocks) to a spike's line is hit by it; its middle this close (in its half widths), it is run through. */
    public static final double SPIKES_REACH = 0.25;
    public static final double SPIKES_THROUGH = 0.85;

    // ------------------------------------------------------------------ 6.2.4, its great hand (N)

    /**
     * How far (blocks from her eyes) its great hand reaches for what she looks at; how fast it gets there (blocks a tick) and the
     * fewest ticks it takes; the longest it holds what it seized (ticks), and how often it worries at it there (ticks), each time this
     * much of a fist.
     */
    public static final double GRAB_RANGE = 7.0;
    public static final double GRAB_SPEED = 1.6;
    public static final int GRAB_REACH_LEAST = 4;
    public static final int GRAB_HOLD_MAX = 100;
    public static final int GRAB_CRUSH_EVERY = 10;
    public static final float GRAB_CRUSH_DAMAGE = 0.45F;
    /** Let go: slammed down on its impact tick, this much of a fist, a crater there; the move over at its last. */
    public static final int GRAB_SLAM_IMPACT = 6;
    public static final int GRAB_SLAM_TICKS = 14;
    public static final float GRAB_SLAM_DAMAGE = 2.6F;
    public static final double GRAB_SLAM_CRATER = 1.0;
    /** Its reach closing on nothing (ticks); RC to reach, RC a tick to hold, ticks (after the slam) before it reaches again. */
    public static final int GRAB_MISS_TICKS = 12;
    public static final float GRAB_COST = 12.0F;
    public static final float GRAB_UPKEEP = 0.15F;
    public static final int GRAB_COOLDOWN = 80;
    /**
     * Where it holds what it seized and where it slams it (her frame, blocks: to her right, up from her feet, ahead): the creature's
     * {@link OwlActs#HOLD}, before its jaws, and the ground under its hand ahead.
     */
    public static final double[] GRAB_HOLD = {-OwlActs.HOLD[0] / 16.0, OwlActs.HOLD[1] / 16.0, -OwlActs.HOLD[2] / 16.0};
    public static final double[] GRAB_SLAM = {0.5, 0.0, 3.1};

    /** The ticks its great hand takes to reach something {@code distance} blocks away. */
    public static int grabReachTicks(double distance) {
        return Math.max(GRAB_REACH_LEAST, (int)Math.ceil(distance / GRAB_SPEED));
    }

    /** How far (blocks) the struck body flies at {@code charge} (0..1), before knockback resistance. */
    public static double punchDistance(float charge) {
        return PUNCH_NEAR + (PUNCH_FAR - PUNCH_NEAR) * clamp01(charge);
    }

    /** Its speed off the blow (blocks a tick) at {@code charge}. */
    public static double punchSpeed(float charge) {
        return PUNCH_SPEED_NEAR + (PUNCH_SPEED_FAR - PUNCH_SPEED_NEAR) * clamp01(charge);
    }

    /** How much speed it loses each tick (blocks a tick, a tick) so that, flying off at {@code speed}, it stops {@code distance} on. */
    public static double punchDecel(double speed, double distance) {
        return distance <= 0.0 ? speed : speed * speed / (2.0 * distance);
    }

    /** Its speed a tick on from {@code speed}, slowing by {@code decel}: evenly, as anything sliding to a stop. */
    public static double slowed(double speed, double decel) {
        return Math.sqrt(Math.max(0.0, speed * speed - 2.0 * decel));
    }

    /** How far (blocks) it goes this tick, flying at {@code speed}, {@code travelled} of {@code distance} gone: never past the end. */
    public static double punchStep(double speed, double travelled, double distance) {
        return Math.max(0.0, Math.min(speed, distance - travelled));
    }

    /** How far it has grown (0..1) {@code age} ticks into growing: nothing while she doubles over, then it bursts out and fills in. */
    public static double grown(double age) {
        if (age < BURST) {
            return 0.0;
        }
        double u = clamp01((age - BURST) / (GROW_TICKS - BURST));
        return 1.0 - Math.pow(1.0 - u, 2.4);
    }

    /** How much of it is left (1..0) {@code age} ticks into turning back: drawn in, slow at first, then all at once. */
    public static double shed(double age) {
        double u = clamp01(age / SHED_TICKS);
        return 1.0 - u * u * (3.0 - 2.0 * u);
    }

    /** How grown it is, {@code age} ticks after it began growing ({@code on}) or turning back. */
    public static double grown(boolean on, double age) {
        return on ? grown(age) : shed(age);
    }

    /** How bulked up her body is under it (0..1) at {@code grown}: once the kakuja is well out, then all of it. */
    public static double bulk(double grown) {
        return smooth((grown - 0.25) / 0.75);
    }

    /** Whether it is still growing or turning back (nothing else may start): {@code age} ticks in. */
    public static boolean changing(boolean on, double age) {
        return age < (on ? GROW_TICKS : SHED_TICKS);
    }

    static double clamp01(double u) {
        return Math.max(0.0, Math.min(1.0, u));
    }

    static double smooth(double u) {
        double c = clamp01(u);
        return c * c * (3.0 - 2.0 * c);
    }
}
