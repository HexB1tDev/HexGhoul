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
    /** Bikaku: a long segmented blade (netherite's 4 + 3 + 1: 8 damage, 1.6 attacks a second). */
    BLADE("bikaku", "quinque_case_bikaku", "quinque_bikaku_blade", 3, -2.4F, 0.0, 1.5F, 60),
    /** Koukaku: a heavy lance (10 damage, 1.1 attacks a second, a block and a half more reach). */
    SPEAR("koukaku", "quinque_case_koukaku", "quinque_koukaku_spear", 5, -2.9F, 1.5, 1.6F, 80);

    /** How long it takes to open or shut (ticks). */
    public static final int UNFOLD = 12;
    /** The stack's tag: the game time it last opened or shut. */
    public static final String AT = "QuinqueAt";
    /** The model property its frames are chosen by (0 the case shut .. 1 the weapon out). */
    public static final String PROPERTY = "deploy";

    /** The kagune it is made of. */
    public final String type;
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

    Quinque(String type, String caseId, String weaponId, int damage, float speed, double reach, float ghoul, int suppress) {
        this.type = type;
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
}
