package com.hex.kagune.control.pale;

import com.hex.kagune.control.ClashRules;

/**
 * 1.3.63: the same big move meeting itself (pure, tested; the world's side is {@code MoveClash}), the owner: "if both use the same move
 * like big punch, speed kick or any of the sort it should cancel both out and cause a big impact". Pale's kit's big moves come in
 * {@link Family families} (a Big Punch and the Owl's Big Punch are both big punches; a fist still winding up is one coming): one let go
 * ({@link #released}) is on its way until it would land ({@link #window}); if what it goes for is doing the same kind of move back at
 * it, within {@link #reach}, both are cancelled with a big impact ({@link #power}: never less than {@link #BIG}), both thrown apart and
 * stunned a moment ({@link #STAGGER}).
 */
public final class MoveClashRules {
    /** The kinds of big move that cancel each other (never sent: order free). */
    public enum Family {
        PUNCH, KICK, SLAM
    }

    /** The least a same-move clash is (a big one: the ground always shakes), and how long both are stunned after it (ticks). */
    public static final float BIG = 2.4F;
    public static final int STAGGER = 14;

    private MoveClashRules() {
    }

    /** {@code move}'s family, winding up or let go; null for a move that doesn't meet itself this way. */
    public static Family family(PaleMove move) {
        if (move == null) {
            return null;
        }
        return switch (move) {
            case PUNCH_CHARGE, PUNCH, OWL_PUNCH_CHARGE, OWL_PUNCH -> Family.PUNCH;
            case KICK, OWL_KICK -> Family.KICK;
            case HAMMER_CHARGE, HAMMER, OWL_FISSURE_CHARGE, OWL_FISSURE -> Family.SLAM;
            default -> null;
        };
    }

    /** True for a big move let go (on its way at something), not one still winding up. */
    public static boolean released(PaleMove move) {
        return move == PaleMove.PUNCH || move == PaleMove.OWL_PUNCH || move == PaleMove.KICK || move == PaleMove.OWL_KICK || move == PaleMove.HAMMER
            || move == PaleMove.OWL_FISSURE;
    }

    /** How long (ticks) a released {@code move} is on its way before it lands ({@code detail}: a speed kick's dash ticks). */
    public static int window(PaleMove move, int detail) {
        return switch (move) {
            case PUNCH, OWL_PUNCH -> PaleRules.PUNCH_IMPACT + 1;
            case KICK -> PaleRules.KICK_BURST + Math.max(0, detail) + PaleRules.KICK_CONTACT + 1;
            case OWL_KICK -> Math.max(0, detail) + OwlRules.KICK_SKEWER + 1;
            case HAMMER -> PaleRules.HAMMER_IMPACT + 1;
            case OWL_FISSURE -> OwlRules.FISSURE_IMPACT + 1;
            default -> 0;
        };
    }

    /** How near (blocks) the two must be for the same moves to meet. */
    public static double reach(Family family) {
        return switch (family) {
            case PUNCH -> 4.5;
            case KICK -> 4.0;
            case SLAM -> 5.5;
        };
    }

    /** How big the impact is: as big as the two were hard ({@link ClashRules#impact}), never less than {@link #BIG}. */
    public static float power(double hitA, double hitB) {
        return Math.max(BIG, ClashRules.impact(hitA, hitB));
    }
}
