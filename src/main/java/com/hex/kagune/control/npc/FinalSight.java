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

    // ---------------------------------------------------------------- 2.1.9: the king's final kakuja (Peace or War) seen in a fight

    /**
     * 2.1.9 (the owner: "Wild ghouls and CGG agents (ranks 0-4) run away from the final form of Rinkaku (Peace or War). Ranks (4-7) shake when
     * fighting the user and back away way more often"): how one holds its nerve before Peace or War: it runs (and keeps running while it is
     * in sight), it fights trembling and keeps backing off, or it stands (a Special Class investigator, the White Reaper).
     */
    public enum Nerve {
        RUN, SHAKE, STAND
    }

    /** True for Rinkaku's final kakuja, either one. */
    public boolean kingly() {
        return this == WAR || this == PEACE;
    }

    /** An investigator of Rank {@code rank}: 0 to 4 run, 5 to 7 shake, 8 and above stand. */
    public static Nerve investigator(int rank) {
        return rank <= 4 ? Nerve.RUN : rank <= 7 ? Nerve.SHAKE : Nerve.STAND;
    }

    /** A wild ghoul of rank {@code tier} (0 the weakest, {@code tiers} - 1 the strongest): all but the two strongest run; those shake. */
    public static Nerve ghoul(int tier, int tiers) {
        return tier < tiers - 2 ? Nerve.RUN : Nerve.SHAKE; // 4.0.0: an S and an SS shake, the rest run
    }

    /** How far off (blocks) Peace or War is felt; how far a runner gets before it stops to look back. */
    public static final double DREAD_RANGE = 32.0;
    public static final double RUNS_TO = 26.0;
    /** While it is in sight, a runner keeps running this long from now, a shaker keeps trembling this long (ticks; kept up twice a second). */
    public static final long STILL_RUNNING = 40L;
    public static final long STILL_SHAKING = 30L;
    /** A shaker's nerve gives every this many ticks or so (it backs off, then comes again), for this long. */
    public static final long BREAKS_EVERY = 90L;
    public static final long BACKS_OFF = 35L;

    /** True if, at {@code now}, a shaker ({@code id}: its own, so no two break together) loses its nerve and backs off. */
    public static boolean breaks(int id, long now) {
        return Math.floorMod(now + id * 37L, BREAKS_EVERY) < 10L;
    }

    /**
     * How far (radians) a trembling body is turned about its feet at {@code time} (ticks, with the partial tick): a fast, small, uneven
     * shiver, its own ({@code id}).
     */
    public static float tremble(int id, double time) {
        double t = time * 1.9 + id * 0.73;
        return (float)(0.035 * (Math.sin(t * 2.3) * 0.6 + Math.sin(t * 3.7 + 1.1) * 0.4));
    }

    /** How long a wild ghoul that first sees it runs (ticks). */
    public long scares() {
        return this.terror() ? TERROR_SCARES : this.any() ? SCARES : 0L;
    }
}
