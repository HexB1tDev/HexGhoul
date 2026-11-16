package com.hex.kagune.control.npc;

/**
 * 2.0.49 (pure, tested; the owner: "I want [the White Reaper] to be a little tech, a bit more of a tactician ... Make him be able to
 * backflip with the mutation thing that makes him backflip higher ... midair, start shooting lightning balls. Like, get close when he
 * sees an opportunity. It should be the pinnacle of combat in this mod"): what the White Reaper reads in a fight and the play he makes
 * on it, ahead of the moves every investigator has ({@link #choose}):
 *
 * <ul>
 *   <li>{@link Play#VAULT}: his quarry guarding in front of him (a Rinkaku's block, a shield) and close: a front flip clean over them,
 *       landing behind the guard, his string at once.</li>
 *   <li>{@link Play#SKY_SHOT}: his quarry in the air (a jump, his kick's launch): a quick shot leading its fall (it can't step aside up
 *       there), the volley from far enough off.</li>
 *   <li>{@link Play#PUNISH}: an opening (stunned, a blow of theirs just spent on nothing, or nearly dead): in at once (a flip from far,
 *       dashes from near) and his string the moment he is in reach.</li>
 *   <li>{@link Play#SOAR}: a backflip as Up We Go launches one ({@link #soar}: built up over a few ticks, about two and a half times a
 *       flip's height), then, from the top of it, hanging there a moment, the volley down at his quarry. After his string lands, when he is
 *       hurt and pressed close, and now and then to keep them guessing.</li>
 * </ul>
 */
public final class ReaperTactics {
    private ReaperTactics() {
    }

    public enum Play {
        NONE, VAULT, SKY_SHOT, PUNISH, SOAR
    }

    /**
     * What he reads this tick: how far off his quarry is and his reach; whether he sees it; whether it is in the air, stunned, has just
     * spent a blow on nothing, is guarding in front of him; its health and his (0..1); whether his string just landed; whether he is on
     * his feet; what is ready (a flip, a dash, a shot, a soar); whether he is in the middle of something (a blow, a kick, an aim, his
     * lance in the ground).
     */
    public record Read(double distance, double reach, boolean sees, boolean preyAirborne, boolean preyStunned, boolean preyWhiffed,
                       boolean preyGuarding, double preyHealth, double ownHealth, boolean afterString, boolean grounded, boolean flipReady,
                       boolean dashReady, boolean shotReady, boolean soarReady, boolean busy) {
    }

    /** The closest a guard is vaulted from, and the farthest. */
    public static final double VAULT_NEAREST = 1.2;
    public static final double VAULT_FARTHEST = 6.0;
    /** How far past the quarry a vault lands (blocks), and how much higher than a flip it goes. */
    public static final double VAULT_PAST = 2.0;
    public static final double VAULT_LIFT = 1.25;
    /** A quarry in the air is shot from this near to this far, this share of the time. */
    public static final double SKY_NEAREST = 5.0;
    public static final double SKY_FARTHEST = 22.0;
    public static final double SKY_CHANCE = 0.6;
    /** An opening is taken from this far off at most. */
    public static final double PUNISH_FARTHEST = 12.0;
    /** Past this far off he closes with a flip rather than dashes. */
    public static final double PUNISH_FLIP = 6.0;
    /** Below this share of his health, pressed this close, he soars clear. */
    public static final double HURT = 0.4;
    public static final double PRESSED = 5.0;
    /** His string landed: this share of the time he soars after it. */
    public static final double AFTER_STRING_CHANCE = 0.6;
    /** Nearly dead (this share of its health): he goes in for it. */
    public static final double FINISH = 0.3;
    public static final double FINISH_FARTHEST = 10.0;
    /** Now and then, from this near to this far, this share of the times he reads it, a soar to keep them guessing. */
    public static final double MIX_NEAREST = 3.5;
    public static final double MIX_FARTHEST = 9.0;
    public static final double MIX_CHANCE = 0.05;

    /** The play for {@code read}, {@code roll} a draw in 0..1 for the ones made only some of the time. */
    public static Play choose(Read read, double roll) {
        if (read.busy() || !read.grounded() || !read.sees()) {
            return Play.NONE;
        }
        double d = read.distance();
        boolean outOfReach = d > read.reach() + 0.5;
        boolean closes = read.dashReady() || read.flipReady();
        if (read.preyGuarding() && d >= VAULT_NEAREST && d <= VAULT_FARTHEST && read.flipReady()) {
            return Play.VAULT;
        }
        if (read.preyAirborne() && d >= SKY_NEAREST && d <= SKY_FARTHEST && read.shotReady() && roll < SKY_CHANCE) {
            return Play.SKY_SHOT;
        }
        if ((read.preyStunned() || read.preyWhiffed() || read.preyAirborne()) && outOfReach && d <= PUNISH_FARTHEST && closes) {
            return Play.PUNISH;
        }
        boolean soars = read.soarReady() && read.flipReady();
        if (soars && read.ownHealth() < HURT && d < PRESSED) {
            return Play.SOAR;
        }
        if (soars && read.afterString() && roll < AFTER_STRING_CHANCE) {
            return Play.SOAR;
        }
        if (read.preyHealth() < FINISH && outOfReach && d <= FINISH_FARTHEST && closes) {
            return Play.PUNISH;
        }
        if (soars && d >= MIX_NEAREST && d <= MIX_FARTHEST && roll < MIX_CHANCE) {
            return Play.SOAR;
        }
        return Play.NONE;
    }

    // ---------------------------------------------------------------- the soar

    /** How many ticks the soar's launch is built up over, and how fast back and up it goes at the top of it (blocks a tick). */
    public static final int RAMP = 5;
    public static final double SOAR_BACK = 0.62;
    public static final double SOAR_UP = 0.68;
    private static final double[] RAMP_SPEED = {0.42, 0.66, 0.84, 0.95, 1.0};
    /** Rising slower than this (blocks a tick) he is at the top: he aims there. */
    public static final double APEX = 0.05;
    /** How long he aims from up there (ticks: quicker than from the ground), how slowly he sinks meanwhile, how his drift is damped. */
    public static final int AIR_AIM = 6;
    public static final double HANG_FALL = -0.02;
    public static final double HANG_DRIFT = 0.6;
    /** A soar ends this long after it began at the latest, whatever happens. */
    public static final int LONGEST = 80;
    /** How long before he soars again. */
    public static final int SOAR_EVERY = 100;
    /** A soar's flip as every client draws it: turning over from this tick, for this many (done by the top, where he aims). */
    public static final double SPIN_FROM = 3.0;
    public static final double SPIN_TICKS = 11.0;
    /** A shot at a quarry in the air: aimed this quickly, and ready again this soon. */
    public static final int SKY_AIM = 6;
    public static final int SKY_EVERY = 46;
    /** How much of a moving quarry's way he leads a shot by (of the time the ball takes to get there). */
    public static final double LEAD = 0.6;

    /** The soar's launch on ramp tick {@code tick} (0 first): {back along his way, up} (blocks a tick). */
    public static double[] soar(int tick) {
        double k = RAMP_SPEED[Math.max(0, Math.min(RAMP_SPEED.length - 1, tick))];
        return new double[] {SOAR_BACK * k, SOAR_UP * k};
    }

    /**
     * Where to aim at a quarry at {@code at} moving {@code velocity} (blocks a tick), {@code distance} off, the ball flying {@code speed}
     * a tick: ahead of it along its way by {@link #LEAD} of the time the ball takes.
     */
    public static double[] lead(double[] at, double[] velocity, double distance, double speed) {
        double t = LEAD * Math.max(0.0, distance) / Math.max(0.1, speed);
        return new double[] {at[0] + velocity[0] * t, at[1] + velocity[1] * t, at[2] + velocity[2] * t};
    }
}
