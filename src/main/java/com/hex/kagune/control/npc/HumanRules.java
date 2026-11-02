package com.hex.kagune.control.npc;

/**
 * Home Living (1.3.40): The Human (pure, tested): an ordinary person, made only by command for now ({@code /summon hexkagune:human}). It
 * wanders; it knows a ghoul when one shows itself ({@link #NOTICE} blocks off with its kagune out, or {@link #CLOSE} with it hidden, or
 * anyone who has hurt it) and runs, at a person's pace: about a player's walk ({@link #SPEED} times {@link #RUN}), so a ghoul who
 * sprints catches it. Its looks are vanilla's own default skins ({@link #LOOKS}); its arms go up as it runs. It leaves
 * {@link #meat} pieces of Human Meat.
 */
public final class HumanRules {
    public static final double HEALTH = 20.0;
    /** Its movement speed attribute: a mob's ground speed goes as the square of it (0.3 at {@link #RUN} is a player's walk). */
    public static final double SPEED = 0.3;
    public static final double WALK = 0.6;
    public static final double RUN = 1.05;
    /** How far off it sees a ghoul with its kagune out, and how near a hidden one has to come before it feels it. */
    public static final double NOTICE = 16.0;
    public static final double CLOSE = 6.0;
    /** How far it runs before it stops to look back, and how long (ticks) it stays frightened after. */
    public static final double SAFE = 24.0;
    public static final int SHAKEN = 80;
    /** How long (ticks) it keeps running from whoever hurt it. */
    public static final int HURT_MEMORY = 600;

    /**
     * 1.3.65: the worst sights ({@link FinalSight}) are seen from much further and run from harder: a final kakuja from {@link #DREAD_NOTICE},
     * War and the Dragon (a terror) from {@link #TERROR_NOTICE}, in its sight; a terror at a sprint ({@link #TERROR_RUN}), and remembered
     * {@link #TERROR_MEMORY} ticks out of sight (it keeps running till it is {@link #TERROR_SAFE} off). It screams as it starts to run, and
     * from a terror every {@link #SCREAMS} ticks or so.
     */
    public static final double DREAD_NOTICE = 28.0;
    public static final double TERROR_NOTICE = 40.0;
    public static final double TERROR_SAFE = 52.0;
    public static final double TERROR_RUN = 1.3;
    public static final int TERROR_MEMORY = 100;
    public static final int SCREAMS = 70;
    /** How far its scream is heard (blocks: shown to the players who see it within this). */
    public static final double HEARD = 32.0;

    /** Vanilla's default skins: (path, slim arms). */
    public static final String[] LOOKS = {"steve", "alex", "ari", "efe", "kai", "makena", "noor", "sunny", "zuri"};

    private HumanRules() {
    }

    /** 1.3.75: the villagers' own looks (HumanSkins) are numbered from this; a new human is given one of them. */
    public static final int CUTE = 1000;

    /** 1.3.75: a new human's look ({@code roll} any int): one of the villagers' own skins. */
    public static int pick(int roll) {
        return CUTE + Math.floorMod(roll, com.hex.kagune.control.village.HumanSkins.ALL.length);
    }

    /**
     * 2.0 (the owner: "make sure there are no 2 of the same look per village"): the village at ({@code midX}, {@code midZ})'s own shuffle of
     * the villagers' looks, at slot {@code slot} (each of a village's people has its own slot): no two slots below the number of looks
     * share one.
     */
    public static int villageLook(int midX, int midZ, int slot) {
        int n = com.hex.kagune.control.village.HumanSkins.ALL.length;
        return CUTE + shuffled(midX, midZ, 0x5EEDL, n)[Math.floorMod(slot, n)];
    }

    /** 2.0: the same for its names ({@code names} of them): no two of a village's people share one. */
    public static int villageName(int midX, int midZ, int slot, int names) {
        return shuffled(midX, midZ, 0x4A3EL, names)[Math.floorMod(slot, names)];
    }

    /** 0 .. n-1 in the order the village at ({@code x}, {@code z}) shuffles them ({@code salt}: which shuffle). */
    static int[] shuffled(int x, int z, long salt, int n) {
        int[] order = new int[n];
        for (int i = 0; i < n; i++) {
            order[i] = i;
        }
        long s = (x * 341873128712L) ^ (z * 132897987541L) ^ (salt * 0x9E3779B97F4A7C15L);
        for (int i = n - 1; i > 0; i--) {
            s = s * 6364136223846793005L + 1442695040888963407L;
            int j = (int)Math.floorMod(s >>> 33, (long)(i + 1));
            int k = order[i];
            order[i] = order[j];
            order[j] = k;
        }
        return order;
    }

    /** 1.3.75: whose texture {@code look} is: the mod's (a villager's own skin) or vanilla's (a default skin). */
    public static String namespace(int look) {
        return look >= CUTE ? "hexkagune" : "minecraft";
    }

    /** The skin's texture path for look {@code look} (0 .. 2 * LOOKS.length - 1: the wide ones, then the slim; 1.3.75: CUTE on, HumanSkins). */
    public static String skin(int look) {
        if (look >= CUTE) {
            return com.hex.kagune.control.village.HumanSkins.ALL[Math.floorMod(look - CUTE, com.hex.kagune.control.village.HumanSkins.ALL.length)].path();
        }
        int n = Math.floorMod(look, LOOKS.length * 2);
        return "textures/entity/player/" + (slim(n) ? "slim/" : "wide/") + LOOKS[n % LOOKS.length] + ".png";
    }

    /**
     * 1.3.76: the texture (the mod's) of look {@code look}'s shut eyes for a blink, or null: a villager's own skin blinks with the shut eyes
     * of the child's skin it was made from (the eyes in the same place, the skin's tone kept); vanilla's default skins have none.
     */
    public static String closed(int look) {
        if (look < CUTE) {
            return null;
        }
        String child = com.hex.kagune.control.village.HumanSkins.ALL[Math.floorMod(look - CUTE, com.hex.kagune.control.village.HumanSkins.ALL.length)].child();
        return com.hex.kagune.control.family.ChildBlinks.has(child) ? com.hex.kagune.control.family.ChildBlinks.closed(child) : null;
    }

    /** 1.3.77: the texture (the mod's) of look {@code look}'s blush (its child skin's), or null (vanilla's looks, or no eyes boxed). */
    public static String blush(int look) {
        if (look < CUTE) {
            return null;
        }
        String child = com.hex.kagune.control.village.HumanSkins.ALL[Math.floorMod(look - CUTE, com.hex.kagune.control.village.HumanSkins.ALL.length)].child();
        return com.hex.kagune.control.family.ChildBlinks.has(child) ? com.hex.kagune.control.family.ChildBlinks.blush(child) : null;
    }

    public static boolean slim(int look) {
        if (look >= CUTE) {
            return com.hex.kagune.control.village.HumanSkins.ALL[Math.floorMod(look - CUTE, com.hex.kagune.control.village.HumanSkins.ALL.length)].slim();
        }
        return Math.floorMod(look, LOOKS.length * 2) >= LOOKS.length;
    }

    /** How many pieces of Human Meat it leaves ({@code roll} 0..1, the killer's {@code looting}): one to three, one more a level. */
    public static int meat(double roll, int looting) {
        return 1 + (int)Math.floor(Math.max(0.0, Math.min(0.999, roll)) * 3.0) + Math.max(0, looting);
    }

    /**
     * True if it sees a ghoul {@code distance} blocks off as one: with its kagune out within {@link #NOTICE}, hidden only within
     * {@link #CLOSE}.
     */
    public static boolean notices(double distance, boolean kaguneOut) {
        return distance <= (kaguneOut ? NOTICE : CLOSE);
    }

    /**
     * 1.3.65: how far off it sees a ghoul player as one: a terror from {@link #TERROR_NOTICE}, another final sight from {@link #DREAD_NOTICE},
     * their eyes on or their kagune out (or a kakuja) from {@link #NOTICE}. 1.3.68 (the owner: "they run from anyone with eyes on, or kagune
     * out"): one who hides it all is just a person to it (it was felt from {@link #CLOSE}).
     */
    public static double notice(FinalSight sight, boolean shown) {
        return sight.terror() ? TERROR_NOTICE : sight.any() ? DREAD_NOTICE : shown ? NOTICE : 0.0;
    }

    /** 1.3.65: how fast it runs from {@code sight} (a terror: a sprint). */
    public static double pace(FinalSight sight) {
        return sight.terror() ? TERROR_RUN : RUN;
    }

    /** Where it runs to: {@code away} (x, z) from the danger, turned by {@code turn} radians (when the straight way is blocked). */
    public static double[] fleeStep(double fromX, double fromZ, double dangerX, double dangerZ, double turn, double length) {
        double dx = fromX - dangerX;
        double dz = fromZ - dangerZ;
        double d = Math.sqrt(dx * dx + dz * dz);
        if (d < 1.0E-4) {
            dx = 1.0;
            dz = 0.0;
            d = 1.0;
        }
        dx /= d;
        dz /= d;
        double c = Math.cos(turn);
        double s = Math.sin(turn);
        return new double[] {fromX + (dx * c - dz * s) * length, fromZ + (dx * s + dz * c) * length};
    }
}
