package com.hex.kagune.control.npc;

/**
 * 7.0.2: the ghoul NPCs' numbers (pure, tested): when and where they turn up, how strong each tier is, how they fight.
 *
 * <p>Spawning: only at night ({@link #night}), only in the overworld (any biome), now and then near a player ({@link #SPAWN_EVERY},
 * {@link #SPAWN_CHANCE}: about one or two a night near someone out in it), {@link #SPAWN_NEAREST} to {@link #SPAWN_FARTHEST} blocks
 * out on the surface, never near another one ({@link #spaced}). They go as monsters do when no one is near, and slip away at dawn
 * when no one is watching.
 */
public final class NpcRules {
    /** A try near each player this often (ticks), at this chance. */
    public static final int SPAWN_EVERY = 200;
    public static final double SPAWN_CHANCE = 0.05;
    /** How far from the player it turns up (blocks). */
    public static final double SPAWN_NEAREST = 24.0;
    public static final double SPAWN_FARTHEST = 48.0;
    /** No player as close as this to where it turns up. */
    public static final double CLEAR_OF_PLAYERS = 20.0;
    /** No other ghoul NPC within this of where it turns up, and at most {@link #CROWD_MOST} others within {@link #CROWD}. */
    public static final double ALONE = 72.0;
    public static final double CROWD = 128.0;
    public static final int CROWD_MOST = 1;
    /** The night (day time, ticks into the day): from dusk to just before dawn. */
    public static final long NIGHT_FROM = 13000L;
    public static final long NIGHT_TO = 23000L;
    /** At dawn one with no player within this slips away (unless it is kept: an ally). */
    public static final double DAWN_WATCHED = 24.0;

    /** How far it sees someone to go for (blocks), and how far it chases before it gives up. */
    public static final double SIGHT = 20.0;
    public static final double LEASH = 40.0;
    /** A temper that keeps its distance (the scavenger) backs off a ghoul with its kagune out within this. */
    public static final double SHY = 8.0;
    /** A coward runs below this share of its health. */
    public static final double FLEE_BELOW = 0.4;

    /** The kagune: grown out over this many ticks; put away this long after its last fight. */
    public static final int DEPLOY_TICKS = 12;
    public static final int CALM_TICKS = 200;
    /** Between strings (or volleys) it circles and sizes its prey up for this long, more or less (ticks). */
    public static final int PAUSE_LEAST = 18;
    public static final int PAUSE_MOST = 40;

    /** Ukaku: it keeps this far from its prey and shoots this often; the shots fly this fast, this far, this wide. */
    public static final double SHOOT_NEAREST = 5.0;
    public static final double SHOOT_FARTHEST = 12.0;
    public static final int SHOT_EVERY = 9;
    public static final double SHOT_SPEED = 1.5;
    public static final double SHOT_RANGE = 18.0;
    public static final float SHOT_RADIUS = 0.35F;
    public static final int SHOT_BURN = 2;

    /**
     * 1.0.41: flips, a player's base ones ({@code PaleRules.Flip}, never Up We Go's soaring launch), a little shorter: a front flip in on
     * prey too far off (as fast along as it needs to land where it wants to be, within these), a back flip clear of prey too close.
     */
    public static final double FLIP_FORWARD_LEAST = 0.45;
    public static final double FLIP_FORWARD_MOST = 1.0;
    public static final double FLIP_FORWARD_UP = 0.62;
    public static final double FLIP_BACK_SPEED = 0.62;
    public static final double FLIP_BACK_UP = 0.6;
    /** How far a flip carries for each block a tick it sets out at (the air's drag over its flight). */
    public static final double FLIP_CARRY = 8.4;
    /** Ticks it is in a flip at least (its body turning over: no blow, no shot), and how long one is drawn (the turn and the landing). */
    public static final int FLIP_BUSY = 12;
    public static final int FLIP_POSED = 26;
    /** The chance a tick that it takes a flip it may take (so it does not flip the moment it can). */
    public static final double FLIP_CHANCE = 0.12;
    /** A rinkaku backs off prey within this; it flips in on prey {@link #CLOSE_IN} farther off than it wants, up to {@link #CLOSE_IN_MOST}. */
    public static final double CROWDED = 2.2;
    public static final double CLOSE_IN = 3.5;
    public static final double CLOSE_IN_MOST = 14.0;
    /** The heavies, its right click: Owl's Execution (rinkaku, prey this near) and the feather storm (ukaku: shots, how wide the fan). */
    public static final double HEAVY_REACH = 4.2;
    public static final int STORM_SHOTS = 7;
    public static final double STORM_SPREAD = 26.0;
    /** Snipers keep this far off, shoot this far, this fast, this often, this much harder. */
    public static final double SNIPE_NEAREST = 10.0;
    public static final double SNIPE_FARTHEST = 20.0;
    public static final double SNIPE_RANGE = 30.0;
    public static final double SNIPE_SPEED = 2.2;
    public static final int SNIPE_EVERY = 16;
    public static final float SNIPE_DAMAGE = 1.35F;

    /** Ghouls heal fast: unhurt for {@link #REGEN_AFTER} ticks, it gets one health back every {@link #REGEN_EVERY}. */
    public static final int REGEN_AFTER = 80;
    public static final int REGEN_EVERY = 30;

    private NpcRules() {
    }

    /** True at night (from dusk until just before dawn). */
    public static boolean night(long dayTime) {
        long time = Math.floorMod(dayTime, 24000L);
        return time >= NIGHT_FROM && time < NIGHT_TO;
    }

    /** True if a new one may turn up: the nearest other ghoul NPC {@code nearest} away, {@code crowd} others within {@link #CROWD}. */
    public static boolean spaced(double nearest, int crowd) {
        return nearest >= ALONE && crowd <= CROWD_MOST;
    }

    /** The tier a ghoul met by someone at {@code mastery} has, for a roll {@code r} in 0..1: the stronger they are, the stronger it is. */
    public static int tier(int mastery, double r) {
        double m = Math.max(0, Math.min(1000, mastery)) / 1000.0;
        double strong = 0.05 + 0.35 * m;
        double middling = 0.30 + 0.20 * m;
        return r < strong ? 2 : r < strong + middling ? 1 : 0;
    }

    /** Health, armor, speed (the mob's movement attribute) by tier. */
    public static float health(int tier) {
        return switch (clamp(tier)) {
            case 0 -> 40.0F;
            case 1 -> 60.0F;
            default -> 85.0F;
        };
    }

    public static double armor(int tier) {
        return 4.0 + 2.0 * clamp(tier);
    }

    public static double speed(int tier) {
        return 0.30 + 0.02 * clamp(tier);
    }

    /** Damage for each unit of a lash's power, or of one shot (before the difficulty's scaling, as every mob's). */
    public static float damage(int tier) {
        return 3.0F + clamp(tier);
    }

    /** The mastery its tails are drawn at: one at the lowest tier, two above it. */
    public static int tailMastery(int tier) {
        return switch (clamp(tier)) {
            case 0 -> 0;
            case 1 -> 150;
            default -> 240;
        };
    }

    /** The mastery its wings are drawn at. */
    public static int wingMastery(int tier) {
        return switch (clamp(tier)) {
            case 0 -> 100;
            case 1 -> 200;
            default -> 350;
        };
    }

    /** How many lashes of the string it throws before it pauses (the stronger, the longer), or shots in a volley. */
    public static int string(int tier) {
        return 3 + clamp(tier);
    }

    /** 1.0.41: ticks between its flips (the stronger, the more often). */
    public static int flipEvery(int tier) {
        return switch (clamp(tier)) {
            case 0 -> 80;
            case 1 -> 60;
            default -> 45;
        };
    }

    /** 1.0.41: how likely it meets a blow coming at it with its own (a rinkaku with the blow's owner in reach), or flips clear of it. */
    public static double counter(int tier) {
        return 0.25 + 0.2 * clamp(tier);
    }

    public static double dodge(int tier) {
        return 0.2 + 0.15 * clamp(tier);
    }

    /** 1.0.41: ticks between its heavies. */
    public static int heavyEvery(int tier) {
        return switch (clamp(tier)) {
            case 0 -> 260;
            case 1 -> 200;
            default -> 150;
        };
    }

    /** 1.0.41: its way of fighting, from its seed: a rinkaku lancer opens from the full length of its tails; an ukaku sniper keeps far off. */
    public static boolean lancer(int seed) {
        return (seed & 1) == 0;
    }

    public static boolean sniper(int seed) {
        return Math.floorMod(seed, 3) == 0;
    }

    /** 1.0.41: how fast along a front flip sets out to carry it {@code gap} blocks (no slower or faster than a flip goes). */
    public static double flipSpeed(double gap) {
        return Math.max(FLIP_FORWARD_LEAST, Math.min(FLIP_FORWARD_MOST, gap / FLIP_CARRY));
    }

    /** Pieces of Ghoul Meat it leaves, for a roll {@code r} in 0..1: one, up to two, two or three. */
    public static int meat(int tier, double r) {
        return switch (clamp(tier)) {
            case 0 -> 1;
            case 1 -> r < 0.5 ? 1 : 2;
            default -> r < 0.5 ? 2 : 3;
        };
    }

    /** Where around a player it turns up, for rolls {@code a} and {@code b} in 0..1: {x, z} offsets in blocks. */
    public static double[] around(double a, double b) {
        double angle = a * Math.PI * 2.0;
        double distance = SPAWN_NEAREST + (SPAWN_FARTHEST - SPAWN_NEAREST) * b;
        return new double[] {Math.cos(angle) * distance, Math.sin(angle) * distance};
    }

    private static int clamp(int tier) {
        return Math.max(0, Math.min(NpcIdentity.TIERS - 1, tier));
    }
}
