package com.hex.kagune.control;

import com.hex.kagune.control.npc.AgentRules;

/**
 * 3.2.4: how much a fighter carries into a clash (pure, tested), the owner: "I keep getting pushed back on the clash ... CCG agents, even at
 * low rank, are pushing back ... I'm at like maximum level, maximum mastery, maximum quests done, maximum ghoul meat eaten ... at max level,
 * it should be able to be matching Arima." A clash threw the weaker blow further by the gap in damage ({@link ClashRules#ratio}), and the two
 * sides weren't measured alike: an agent's blow was its quinque's raw damage times its rank's power, a player's the kagune's damage after
 * {@code DamageBalance} had scaled it down (to 0.55 at full mastery), with neither the quests done nor the ghoul meat eaten in it at all.
 * A rank 4 investigator with a mace out-hit a mastered ghoul; the Reaper flung one like a kick.
 *
 * <p>Now both stand on the CCG's own scale: an ordinary blow ({@link #BLOW}, the Reaper's Dawn) times the fighter's power, times the root of
 * how heavy this blow is next to an ordinary one (a finisher, a mace's weight: they count, but less than who swings them). An agent's power
 * is its rank's; a player's runs from a fresh Rank 0's to the Reaper's ({@link #power}) with how far they have come ({@link #progress}:
 * mastery, the gates of their path opened by quests and lessons, the ghoul meat in them), and their form and mutations on top
 * ({@link #build}). At the top a player meets the Reaper as an equal; against a low rank, the low rank is the one sent flying.
 */
public final class ClashPower {
    private ClashPower() {
    }

    /** An ordinary blow on the CCG's scale: Dawn's (its damage, 9, as an agent swings it, 0.85 of it). */
    public static final double BLOW = 9.0 * 0.85;
    /** How the three count toward a player's progress (they add to 1), and the ghoul meat that counts as all of it. */
    public static final double MASTERY_SHARE = 0.55;
    public static final double GATES_SHARE = 0.30;
    public static final double MEAT_SHARE = 0.15;
    public static final int MEAT_FULL = 150;
    /** The most a player's form and mutations add (times). */
    public static final double BUILD_MOST = 1.6;
    /**
     * 3.4.3 (the owner: "Even at max mastery, one-eyed, and with max ghoul meat eaten, he still ignores that"): what a one-eyed ghoul's
     * kakugan adds (times): a maxed one-eyed ghoul a little past the Reaper.
     */
    public static final double ONE_EYED = 1.15;

    /**
     * How far a player has come, 0 to 1: {@code mastery} (of 1000), {@code gates} (the share of their path's gates open: quests and
     * lessons done) and {@code eaten} (pieces of ghoul meat; {@link #MEAT_FULL} or more is all of its share).
     */
    public static double progress(int mastery, double gates, int eaten) {
        double m = clamp(mastery / 1000.0);
        double g = clamp(gates);
        double meat = clamp(GhoulMeat.growth(eaten) / GhoulMeat.growth(MEAT_FULL));
        return clamp(MASTERY_SHARE * m + GATES_SHARE * g + MEAT_SHARE * meat);
    }

    /** A player's power at {@code progress}: a fresh Rank 0's at 0, the Reaper's (Rank 10's) at 1. */
    public static float power(double progress) {
        float low = AgentRules.Rank.RANK_0.power;
        float high = AgentRules.Rank.RANK_10.power;
        return (float)(low + (high - low) * clamp(progress));
    }

    /**
     * What a player's form and mutations add: {@code factor} is how much harder they make the kagune hit than its bare mastery would
     * (the original's swipe over its base), counted by its root, from 1 to {@link #BUILD_MOST}.
     */
    public static double build(double factor) {
        return Math.max(1.0, Math.min(BUILD_MOST, Math.sqrt(Math.max(0.0, factor))));
    }

    /** A player's blow in a clash: {@code progress}, {@code build}, {@code weight} (how heavy this blow is next to an ordinary one: 1). */
    public static double player(double progress, double build, double weight) {
        return player(progress, build, weight, false);
    }

    /** 3.4.3: as above, {@code oneEyed} counted ({@link #ONE_EYED}). */
    public static double player(double progress, double build, double weight, boolean oneEyed) {
        return BLOW * power(progress) * build * heft(weight) * (oneEyed ? ONE_EYED : 1.0);
    }

    /** An agent's blow in a clash: {@code rankPower} (its rank's, a superior's included), {@code blow} its weapon's own damage for it. */
    public static double agent(float rankPower, double blow) {
        return BLOW * rankPower * heft(Math.max(0.1, blow) / BLOW);
    }

    /** How much a blow {@code weight} times an ordinary one counts: its root (0.55 to 1.7). */
    public static double heft(double weight) {
        return Math.sqrt(Math.max(0.3, Math.min(3.0, weight)));
    }

    private static double clamp(double v) {
        return Math.max(0.0, Math.min(1.0, v));
    }
}
