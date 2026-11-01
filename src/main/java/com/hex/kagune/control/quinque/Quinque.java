package com.hex.kagune.control.quinque;

/**
 * 1.3.25: the CCG's quinques (pure): weapons made from a ghoul's kakuhou, carried shut in an attaché case and released from its
 * handle, the case unfolding into the weapon in the investigator's hand (and folding back). Each takes after the kagune it was made
 * from: a Bikaku blade (balanced, a sword), a Koukaku spear (hard and heavy, a long reach). A quinque cuts a ghoul as nothing else
 * does ({@link #ghoul}: more damage) and holds its regeneration back a while ({@link #suppress}: RC suppression).
 *
 * <p>In the hand: a right click on the case opens it (the case item becomes the weapon item); Shift + right click on the weapon shuts
 * it. The stack carries {@link #AT}: when it last opened or shut, so its model is drawn moving between the two for {@link #UNFOLD}
 * ticks ({@link #shown}), and it can't be turned again until it is done.
 */
public enum Quinque {
    /** Bikaku: a long segmented blade, one-handed (8 damage, 1.6 attacks a second). */
    BLADE("bikaku", "bikaku", "quinque_case_bikaku", "quinque_bikaku_blade", 3, -2.4F, 0.0, 1.5F, 60, false, false, 7),
    /**
     * Koukaku: a great spear, three blocks long, gripped in the middle with both hands: agile (8 damage, 1.8 attacks a second, two
     * blocks more reach, a little quicker on its feet). 1.3.26: the owner: "a spear has to be bigger... in combat it should be agile".
     */
    SPEAR("koukaku", "koukaku", "quinque_case_koukaku", "quinque_koukaku_spear", 3, -2.2F, 2.0, 1.5F, 70, true, false, 6),
    /** 1.3.26: Bikaku: a katana, held in both hands (9 damage, 1.7 attacks a second, half a block more reach). */
    KATANA("bikaku", "katana", "quinque_case_katana", "quinque_katana", 4, -2.3F, 0.5, 1.6F, 60, true, false, 8),
    /** 1.3.26: Rinkaku: a flesh mace, a long handle in both hands, a great lump of flesh for a head (14 damage, 0.8 a second). */
    MACE("rinkaku", "mace", "quinque_case_mace", "quinque_flesh_mace", 9, -3.2F, 0.75, 1.7F, 100, true, false, 12),
    /** 1.3.26: Ukaku: a launcher firing crystal shards (hold use); a poor club (4 damage). */
    LAUNCHER("ukaku", "launcher", "quinque_case_launcher", "quinque_crystal_launcher", -1, -2.4F, 0.0, 1.4F, 40, true, true, 7);

    /** How long it takes to open or shut (ticks). */
    public static final int UNFOLD = 12;
    /** The stack's tag: the game time it last opened or shut. */
    public static final String AT = "QuinqueAt";
    /** The model property its frames are chosen by (0 the case shut .. 1 the weapon out). */
    public static final String PROPERTY = "deploy";

    /** The kagune it is made of. */
    public final String type;
    /** Its models' and textures' name (tools/quinque_models.py). */
    public final String model;
    /** Its case's item id and its weapon's. */
    public final String caseId;
    public final String weaponId;
    /** The weapon's attack damage and speed on a sword's terms (SwordItem: on top of its tier's). */
    public final int damage;
    public final float speed;
    /** Reach it adds while it is out (blocks). */
    public final double reach;
    /** How much harder it cuts a ghoul. */
    public final float ghoul;
    /** How long a cut holds a ghoul's regeneration back (ticks). */
    public final int suppress;
    /** 1.3.26: held in both hands (its stance: QuinquePose). */
    public final boolean twoHanded;
    /** 1.3.26: fires (hold use) rather than cuts. */
    public final boolean ranged;
    /** 1.3.26: how long its swing is drawn (ticks): a heavy one slower. */
    public final int swing;

    Quinque(String type, String model, String caseId, String weaponId, int damage, float speed, double reach, float ghoul, int suppress,
            boolean twoHanded, boolean ranged, int swing) {
        this.type = type;
        this.model = model;
        this.twoHanded = twoHanded;
        this.ranged = ranged;
        this.swing = swing;
        this.caseId = caseId;
        this.weaponId = weaponId;
        this.damage = damage;
        this.speed = speed;
        this.reach = reach;
        this.ghoul = ghoul;
        this.suppress = suppress;
    }

    /** How far it has come since it last opened or shut (0 .. 1): Long.MIN_VALUE (never) is done. Never overflows. */
    public static float progress(long now, long at) {
        if (at == Long.MIN_VALUE || now >= at + UNFOLD || now - at >= UNFOLD) {
            return 1.0F;
        }
        return now <= at ? 0.0F : (float)(now - at) / UNFOLD;
    }

    /** True while it is still opening or shutting (it can't be turned again yet). */
    public static boolean moving(long now, long at) {
        return progress(now, at) < 1.0F;
    }

    /**
     * How far out it is drawn (its model's {@link #PROPERTY}): a weapon opening out of its case (0 .. 1), a case shutting round its
     * weapon (1 .. 0); still, a weapon is all the way out (1), a case shut (0).
     */
    public static float shown(boolean weapon, long now, long at) {
        float p = progress(now, at);
        return weapon ? p : 1.0F - p;
    }

    /** The attack speed it has (a player's 4 and its own). */
    public float attacksPerSecond() {
        return 4.0F + this.speed;
    }

    /** The damage it deals (1, netherite's 4 and its own). */
    public float attackDamage() {
        return 1.0F + 4.0F + this.damage;
    }

    /** The quinque whose case or weapon has {@code id} ("hexkagune:..." or bare), or null. */
    public static Quinque byId(String id) {
        if (id == null) {
            return null;
        }
        String bare = id.startsWith("hexkagune:") ? id.substring("hexkagune:".length()) : id;
        for (Quinque q : values()) {
            if (q.caseId.equals(bare) || q.weaponId.equals(bare)) {
                return q;
            }
        }
        return null;
    }

    // ---------------------------------------------------------------- 1.3.26: its M1s, three to a string

    /** A string of M1s: a blow within this long of the last goes on to the next of the three (ticks); later, it starts again. */
    public static final long COMBO = 26L;
    /** Its third blow: the finisher, harder. */
    public static final int FINISHER = 2;

    /** Which of its three blows comes next: {@code last} the blow before ({@code -1} none), {@code since} ticks since it. */
    public static int next(int last, long since) {
        return last < 0 || since < 0L || since > COMBO ? 0 : (last + 1) % 3;
    }

    /** How much harder its {@code step}th blow lands (its finisher's weight). */
    public float weight(int step) {
        if (step != FINISHER) {
            return 1.0F;
        }
        return switch (this) {
            case BLADE -> 1.4F;
            case SPEAR -> 1.3F;
            case KATANA -> 1.6F;
            case MACE -> 1.25F;
            case LAUNCHER -> 1.0F;
        };
    }

    /** How far round its finisher reaches others (blocks; 0 none): the katana's wide cut, the mace's slam, the spear's thrust's line. */
    public double sweep(int step) {
        if (step != FINISHER) {
            return 0.0;
        }
        return switch (this) {
            case KATANA -> 3.5;
            case MACE -> 3.0;
            case SPEAR -> 5.0;
            default -> 0.0;
        };
    }

    /** The launcher: a shard each this many ticks held (use repeats every four), this much each, this fast, this far. */
    public static final float SHARD_DAMAGE = 3.5F;
    public static final double SHARD_SPEED = 2.2;
    public static final double SHARD_RANGE = 40.0;
    /**
     * It heats a shard at a time and cools one every {@link #COOLS_EVERY} ticks; at {@link #HEAT_MOST} it overheats and won't fire for
     * {@link #COOL} ticks: held down (a shard every four ticks) about six seconds of fire, then three to cool; bursts never overheat.
     */
    public static final int HEAT_MOST = 30;
    public static final long COOL = 60L;
    public static final long COOLS_EVERY = 8L;

    /** The launcher's heat after a shot: {@code heat} as it was {@code since} ticks after the shot before (cooled since), plus one. */
    public static int heated(int heat, long since) {
        long cooled = Math.max(0L, since) / COOLS_EVERY;
        return (int)Math.max(0L, heat - Math.min(cooled, (long)Integer.MAX_VALUE)) + 1;
    }
}
