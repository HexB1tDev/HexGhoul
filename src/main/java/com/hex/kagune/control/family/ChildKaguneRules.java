package com.hex.kagune.control.family;

/**
 * 1.3.43: how a ghoul child fights with the kagune it inherited (pure, tested; the world's side is {@code ChildKagune}, the drawing the
 * wild ghouls' renderers at its size). Its tails lash as a wild ghoul's do (the transplant's string, two tails from the start: both
 * tentacles), its wing or wings shoot as theirs do (one wing: every shot from that side), all of it at its body's size; with tails and a
 * wing it uses both, the tails up close, the wing from further off. It only ever fights what threatens its family (as before).
 */
public final class ChildKaguneRules {
    /** How close it keeps when it only shoots, how far it lets its prey get, how far a shot flies, ticks between shots. */
    public static final double SHOOT_NEAREST = 4.0;
    public static final double SHOOT_FARTHEST = 10.0;
    public static final double SHOT_RANGE = 14.0;
    public static final int SHOT_EVERY = 10;
    public static final double SHOT_SPEED = 1.3;
    /** How long it waits between strings and volleys (ticks), at least and at most. */
    public static final int PAUSE_LEAST = 20;
    public static final int PAUSE_MOST = 42;
    /** A lash or a shot's damage against its punch's (a child's kagune is lighter than a grown ghoul's). */
    public static final float KAGUNE_SHARE = 0.45F;

    /** What it does this tick: lash with its tails, shoot with its wing(s), or close in (it has nothing that reaches yet). */
    public enum Use {
        TAILS, WINGS, CLOSE_IN, NONE
    }

    private ChildKaguneRules() {
    }

    /** How big its kagune is against a grown ghoul's: as big as it is ({@link ChildStage#draw}). */
    public static double scale(ChildStage stage) {
        return stage == null ? 1.0 : stage.draw;
    }

    /**
     * The mastery its tails are drawn and strike at: always two (both tentacles), a teen's longer. By its stage alone: its clients draw it
     * from what they know of it, and the server strikes from the same.
     */
    public static int tailMastery(ChildStage stage) {
        return stage == ChildStage.TEEN ? 200 : 100;
    }

    /** The mastery its wing or wings are drawn at (and shoot from the tip of): a child's small, a teen's grown; by its stage alone. */
    public static int wingMastery(ChildStage stage) {
        return stage == ChildStage.TEEN ? 220 : 110;
    }

    /** One unit of a lash's power, or one shot: a share of its punch ({@link ChildRules#damage}), as strong as it was born. */
    public static float damage(ChildStage stage, int skill, float might) {
        return ChildRules.damage(stage, skill, true, might) * KAGUNE_SHARE;
    }

    /** How many lashes a string, or shots a volley, before it pauses: two, up to four the better it was taught. */
    public static int string(int skill) {
        return 2 + Math.max(0, Math.min(2, skill / 2));
    }

    /**
     * What it does with kagune {@code bits} against prey {@code distance} away that it {@code sees}: its tails within their reach
     * ({@code tailReach}, already at its size); its wing(s) beyond it (or always, with no tails); closer if it can't reach at all.
     */
    public static Use use(int bits, double distance, boolean sees, double tailReach) {
        boolean tails = KaguneGenes.tails(bits);
        boolean wings = KaguneGenes.wings(bits) > 0;
        if (!tails && !wings) {
            return Use.NONE;
        }
        if (tails && distance <= tailReach - 0.3 && sees) {
            return Use.TAILS;
        }
        if (wings && sees && distance <= SHOT_RANGE) {
            return tails && distance <= tailReach + 1.5 ? Use.CLOSE_IN : Use.WINGS;
        }
        return Use.CLOSE_IN;
    }

    /** The wing its next shot leaves from (1 its right, -1 its left): its one wing's side, or turn about with the pair ({@code last}). */
    public static int side(int bits, long seed, int last) {
        if (KaguneGenes.wings(bits) == 1) {
            return KaguneGenes.side(seed);
        }
        return last >= 0 ? -1 : 1;
    }

    /** Its pause after a string or volley ({@code roll} in 0..1). */
    public static int pause(double roll) {
        return PAUSE_LEAST + (int)Math.round(Math.max(0.0, Math.min(1.0, roll)) * (PAUSE_MOST - PAUSE_LEAST));
    }
}
