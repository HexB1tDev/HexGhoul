package com.hex.kagune.control.npc;

import com.hex.kagune.control.king.KingForm;

/**
 * 1.3.65: which of the worst sights a player is ({@link Display#DREAD}, told apart; pure, never sent): the Dragon, War (the One-Eyed King,
 * feared most), Peace (as much awe as fear), Rinkaku's final kakuja (the centipede and the tentacles together), Ukaku's (needles, fire
 * and the half mask). Everyone near reacts to each its own way ({@code Sightings}); War and the Dragon are a terror: wild ghouls and
 * humans keep running while it is in sight, the CCG falters, a player's children hide from them.
 */
public enum FinalSight {
    NONE,
    WAR,
    PEACE,
    DRAGON,
    RINKAKU,
    UKAKU;

    /** Wild ghouls first see a terror and run this long (ticks), the other final sights this long; kept up while a terror is in sight. */
    public static final long TERROR_SCARES = 300L;
    public static final long SCARES = 240L;
    /** While a terror is in sight within {@link #KEEPS_RUNNING} blocks, a wild ghoul's fright is kept to at least this long from now. */
    public static final long STILL_SCARED = 40L;
    public static final double KEEPS_RUNNING = 28.0;
    /** How far a wild ghoul runs from a terror at a time (blocks; from anything else: 14). */
    public static final double TERROR_RUN = 20.0;
    /** A fleeing wild ghoul shouts about a terror still in sight at most this often (ticks, per player). */
    public static final long SHOUTS = 120L;

    /** A child stays hiding from a terror this long (ticks) after it was last near (kept up twice a second by {@code Sightings}). */
    public static final long HIDES = 30L;
    /** How far behind the one it hides behind a child stands (blocks past their middle), and how near that counts as there. */
    public static final double BEHIND = 0.95;
    public static final double THERE = 0.6;
    /** With no one to hide behind, how far off from a terror a child keeps. */
    public static final double KEEPS_OFF = 7.0;

    /**
     * Where a child hides from what it fears at ({@code fx}, {@code fz}) behind someone at ({@code sx}, {@code sz}): {@code gap} blocks past
     * them, straight away from it (x, z).
     */
    public static double[] behind(double sx, double sz, double fx, double fz, double gap) {
        double dx = sx - fx;
        double dz = sz - fz;
        double d = Math.sqrt(dx * dx + dz * dz);
        if (d < 1.0E-4) {
            dx = 1.0;
            dz = 0.0;
            d = 1.0;
        }
        return new double[] {sx + dx / d * gap, sz + dz / d * gap};
    }

    /** Which one, worst first: the Dragon, the final kakuja worn ({@code worn}), Rinkaku's final kakuja, Ukaku's; else NONE. */
    public static FinalSight of(boolean dragon, KingForm worn, boolean rinkaku, boolean ukaku) {
        if (dragon) {
            return DRAGON;
        }
        if (worn == KingForm.WAR) {
            return WAR;
        }
        if (worn == KingForm.PEACE) {
            return PEACE;
        }
        return rinkaku ? RINKAKU : ukaku ? UKAKU : NONE;
    }

    /** True for one of them. */
    public boolean any() {
        return this != NONE;
    }

    /** True for a terror (War, the Dragon): everyone runs, and keeps running while it is in sight. */
    public boolean terror() {
        return this == WAR || this == DRAGON;
    }

    /** How long a wild ghoul that first sees it runs (ticks). */
    public long scares() {
        return this.terror() ? TERROR_SCARES : this.any() ? SCARES : 0L;
    }
}
