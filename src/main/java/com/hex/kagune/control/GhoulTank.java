package com.hex.kagune.control;

/**
 * 2.0 (the owner: "Mastery & Forms does not increase HP for the player, instead it basically just boosts resistence by a ton, the more
 * mastery, the more ghoul meat eaten, the better the form, etc all those factors makes the player a living tank, to make it more balanced
 * with the NPCS and pvp"): how much of every blow a ghoul player still takes (pure). Three parts, each cutting what is left of the last:
 * mastery (0..1000) up to {@link #MASTERY}, ghoul meat devoured up to {@link #MEAT} (half of it at {@link #MEAT_HALF} pieces, rising for ever,
 * every piece a little less than the last), and the form it is in now ({@link #form}): the kagune out, a kakuja, a final kakuja. A seasoned
 * ghoul in a final kakuja takes about a quarter of what a human would; a new one with its kagune out a little less than all of it.
 * Its health stays a player's 20.
 */
public final class GhoulTank {
    private GhoulTank() {
    }

    /** The share mastery 1000 takes off. */
    public static final double MASTERY = 0.35;
    /** The share all the ghoul meat in the world would take off, and how many pieces take off half of it. */
    public static final double MEAT = 0.25;
    public static final double MEAT_HALF = 20.0;
    /** The kagune out (at its first stage), and how much more each later stage of it takes off (up to {@link #STAGES} of them). */
    public static final double OUT = 0.10;
    public static final double STAGE = 0.04;
    public static final int STAGES = 3;
    /** A kakuja (any kind: rinkaku's, ukaku's, a graft's, the Owl), and a final kakuja (War, Peace, the Dragon, a path's final). */
    public static final double KAKUJA = 0.32;
    public static final double FINAL = 0.50;

    /** The share its form takes off: none with the kagune in, more as it grows, the most in a final kakuja. */
    public static double form(boolean out, int stage, boolean kakuja, boolean finalForm) {
        if (finalForm) {
            return FINAL;
        }
        if (kakuja) {
            return KAKUJA;
        }
        return out ? OUT + STAGE * Math.max(0, Math.min(STAGES, stage)) : 0.0;
    }

    /** How far {@code eaten} pieces have gone toward all of {@link #MEAT} (0 for none, a half at {@link #MEAT_HALF}). */
    public static double meat(int eaten) {
        double n = Math.max(0, eaten);
        return n / (n + MEAT_HALF);
    }

    /** What every blow is multiplied by: 1 for a new ghoul with its kagune in, about 0.24 at the most. */
    public static double taken(int mastery, int eaten, double form) {
        double m = Math.max(0, Math.min(1000, mastery)) / 1000.0;
        return (1.0 - MASTERY * m) * (1.0 - MEAT * meat(eaten)) * (1.0 - Math.max(0.0, Math.min(0.9, form)));
    }

    /** True for damage nothing may soften: the void, a kill command. */
    public static boolean bypasses(String msgId) {
        return "outOfWorld".equals(msgId) || "genericKill".equals(msgId);
    }
}
