package com.hex.kagune.control.npc;

/**
 * 4.1.52 (the owner: "New Reputation bar when talking to fellow gang members, high reputation means you're feared, they react to it, some
 * stepping back to let you pass (just a little and not the leaders, they do not step back this also applies for official NPCS). / When
 * the reputation bar reaches 0 the player is warned to leave the area (if within the hideout), the bar starts at 50%."; their answers:
 * raised by daily quests and by killing the CCG and rivals, lowered by hitting your own (a kill costs a lot; the ban past 10 kills stays),
 * back toward half over the days nothing happens; at 0 "They attack after a warning"): a member's standing in their gang (pure, tested).
 * {@link #reputation} 0 .. {@link #MOST}; {@link #day} the game day it was last changed (-1: never); {@link #warnedAt} the game time they
 * were told to leave the hideout (-1: not told).
 */
public record GangStanding(int reputation, long day, long warnedAt) {
    /** The bar's top, and where it starts (the owner: "the bar starts at 50%"). */
    public static final int MOST = 100;
    public static final int START = 50;
    /** From here up they are feared: some of the gang step back to let them pass (never its leaders, never an official NPC). */
    public static final int FEARED = 80;
    /** A killed investigator (or one of the CCG's ghoul division); a killed member of another gang. */
    public static final int KILL_CCG = 3;
    public static final int KILL_RIVAL = 4;
    /** A blow on one of their own (at most one counted every {@link #HIT_REST} ticks); one of their own killed: a lot. */
    public static final int HIT_OWN = 2;
    public static final int KILL_OWN = 20;
    public static final long HIT_REST = 40L;
    /** A daily quest done (4.1.53's, Noro's and Roma's). */
    public static final int DAILY = 10;
    /** How far it goes back toward {@link #START} for each game day nothing changed it. */
    public static final int DRIFT = 5;
    /** How long (ticks) after the warning the gang goes for them, if they are still in the hideout at 0 (the owner: "They attack after a warning"; 30 s). */
    public static final long GRACE = 600L;
    /** A game day (ticks). */
    public static final long DAY = 24000L;

    public static final GangStanding NEW = new GangStanding(START, -1L, -1L);

    /** A new member's standing, begun on game day {@code today}. */
    public static GangStanding joined(long today) {
        return new GangStanding(START, today, -1L);
    }

    /** As it stands on game day {@code today}: back toward {@link #START} by {@link #DRIFT} for each whole day since it last changed. */
    public GangStanding drifted(long today) {
        if (this.day < 0L || today <= this.day) {
            return this.day < 0L ? new GangStanding(this.reputation, today, this.warnedAt) : this;
        }
        long steps = Math.min((today - this.day) * DRIFT, MOST);
        int rep = this.reputation > START ? (int)Math.max(START, this.reputation - steps) : (int)Math.min(START, this.reputation + steps);
        return new GangStanding(rep, today, rep > 0 ? -1L : this.warnedAt);
    }

    /** {@code by} added (taken, if less than 0) on game day {@code today}, within 0 .. {@link #MOST}; above 0 any warning is over. */
    public GangStanding plus(int by, long today) {
        GangStanding d = drifted(today);
        int rep = Math.max(0, Math.min(MOST, d.reputation + by));
        return new GangStanding(rep, today, rep > 0 ? -1L : d.warnedAt);
    }

    /** True if they are feared (some of the gang step back to let them pass). */
    public boolean feared() {
        return this.reputation >= FEARED;
    }

    /** True if the bar is empty (in the hideout they are told to leave). */
    public boolean empty() {
        return this.reputation <= 0;
    }

    /** Told to leave at game time {@code now} (once: the first warning's time is kept). */
    public GangStanding warn(long now) {
        return this.warnedAt >= 0L ? this : new GangStanding(this.reputation, this.day, now);
    }

    /** The warning over (they left the hideout, or the bar rose). */
    public GangStanding calm() {
        return this.warnedAt < 0L ? this : new GangStanding(this.reputation, this.day, -1L);
    }

    /** True if the gang goes for them now: the bar empty, still in the hideout, warned {@link #GRACE} or more ago. */
    public boolean turned(long now, boolean inHideout) {
        return empty() && inHideout && this.warnedAt >= 0L && now - this.warnedAt >= GRACE;
    }

    /** Seconds left of the warning at {@code now} (0 once it has run out; {@link #GRACE} / 20 if not warned). */
    public int secondsLeft(long now) {
        if (this.warnedAt < 0L) {
            return (int)(GRACE / 20L);
        }
        return (int)Math.max(0L, (GRACE - (now - this.warnedAt) + 19L) / 20L);
    }

    /** Of every ten of a gang, this many step back for a feared member (the owner: "some stepping back"). */
    public static final int YIELDING = 6;

    /** True if one of a gang with seed {@code seed} is one who steps back for a feared member (always the same ones). */
    public static boolean yields(int seed) {
        return Math.floorMod(seed * 31 + 7, 10) < YIELDING;
    }

    /** The word for where they stand (shown under the bar). */
    public static String band(int reputation) {
        if (reputation <= 0) {
            return "Unwanted";
        }
        if (reputation >= FEARED) {
            return "Feared";
        }
        if (reputation >= 60) {
            return "Respected";
        }
        if (reputation >= 40) {
            return "Known";
        }
        return reputation >= 20 ? "Doubted" : "Despised";
    }
}
