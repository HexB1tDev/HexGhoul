package com.hex.kagune.control.king;

/**
 * 1.0.5: the final kakujas' rules (pure, tested): what a ghoul's saved crown says, what each form gives, how fast it grows over
 * him, and how dark the night is for War.
 *
 * <p>Saved as one string in the profile: empty (the Dragon has not been through him), {@code awake} (it has: a crown waits to be
 * chosen), or the chosen form's id.
 */
public final class KingRules {
    public static final String AWAKE = "awake";
    /** Ticks a final kakuja takes to grow over him (and to go). */
    public static final int GROW_TICKS = 24;
    /**
     * 1.0.23: the RC a worn final kakuja burns each tick (Furuta's kakuja's rate: a full bar lasts about two minutes); none comes back
     * while it is worn, and run dry it falls away from him. It takes this much to call one out (the original's to bring out the kagune).
     */
    public static final float UPKEEP = 0.04F;
    public static final float CALL_ENERGY = 5.0F;

    /** War's numbers from the spec: melee, move speed, attack speed, max health (shares added to the base) and damage taken off. */
    public static final double WAR_MELEE = 0.45;
    public static final double WAR_SPEED = 0.35;
    public static final double WAR_ATTACK_SPEED = 0.30;
    public static final double WAR_HEALTH = 0.80;
    public static final double WAR_REDUCTION = 0.25;
    /** Peace's, for when it is built. */
    public static final double PEACE_MELEE = 0.30;
    public static final double PEACE_SPEED = 0.25;
    public static final double PEACE_ATTACK_SPEED = 0.40;
    public static final double PEACE_HEALTH = 1.00;
    public static final double PEACE_REDUCTION = 0.35;

    private KingRules() {
    }

    /** The saved crown: has the Dragon been through him. */
    public static boolean awake(String saved) {
        return saved != null && !saved.isEmpty();
    }

    /** The saved crown: the form chosen, or {@link KingForm#NONE}. */
    public static KingForm chosen(String saved) {
        return saved == null || saved.isEmpty() || AWAKE.equals(saved) ? KingForm.NONE : KingForm.byId(saved);
    }

    /** The crown saved after {@code form} is chosen (only from {@link #AWAKE}, only a built form). */
    public static String choose(String saved, KingForm form) {
        if (!awake(saved) || chosen(saved) != KingForm.NONE || form == null || !form.built()) {
            return saved;
        }
        return form.id();
    }

    /** Melee, move speed, attack speed and max health shares {@code form} adds. */
    /** 1.0.26: how much faster again each is while it sprints (a share of the base speed, on top of its own boost and vanilla's sprint). */
    public static final double WAR_SPRINT = 1.3; // 1.0.33: was 0.6 (still too slow)
    public static final double PEACE_SPRINT = 1.8; // 1.0.33: was 0.75

    /** 1.0.26: {@code form}'s sprint ({@link #WAR_SPRINT}, {@link #PEACE_SPRINT}); none for anything else. */
    public static double sprint(KingForm form) {
        return form == KingForm.WAR ? WAR_SPRINT : form == KingForm.PEACE ? PEACE_SPRINT : 0.0;
    }

    public static double[] boosts(KingForm form) {
        return switch (form) {
            case WAR -> new double[] {WAR_MELEE, WAR_SPEED, WAR_ATTACK_SPEED, WAR_HEALTH};
            case PEACE -> new double[] {PEACE_MELEE, PEACE_SPEED, PEACE_ATTACK_SPEED, PEACE_HEALTH};
            default -> new double[4];
        };
    }

    /** The share of damage {@code form} takes off. */
    public static double reduction(KingForm form) {
        return switch (form) {
            case WAR -> WAR_REDUCTION;
            case PEACE -> PEACE_REDUCTION;
            default -> 0.0;
        };
    }

    /** How far grown over him a final kakuja is, {@code age} ticks after it came ({@code on}) or went: 0..1. */
    public static double grow(boolean on, double age) {
        double t = Math.max(0.0, Math.min(1.0, age / GROW_TICKS));
        double s = t * t * (3.0 - 2.0 * t);
        return on ? s : 1.0 - s;
    }

    /**
     * How deep in the night {@code dayTime} is (0 by day, 1 in the dead of night), eased through dusk (12500..13500) and dawn
     * (22500..23500): War's body goes dark with it and its eye burns brighter.
     */
    public static double night(long dayTime) {
        long t = Math.floorMod(dayTime, 24000L);
        double x;
        if (t < 12500L || t >= 23500L) {
            x = 0.0;
        } else if (t < 13500L) {
            x = (t - 12500L) / 1000.0;
        } else if (t < 22500L) {
            x = 1.0;
        } else {
            x = (23500L - t) / 1000.0;
        }
        return x * x * (3.0 - 2.0 * x);
    }
}
