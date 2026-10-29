package com.hex.kagune.control;

import com.hex.kagune.gameplay.MutationNode;

/**
 * Rinkaku's mutations (pure, tested). 3.0.3: the user kept five of the original fifteen. 1.0.41: none: the Rinkaku path has no
 * mutations at all (its passive, You Thought You'd Get Rid of Me That Easily?, comes from its quest instead). Every one stays an
 * enum constant (the profile stores mutations as bits by ordinal) but never unlocks, never counts when a save still carries it (its
 * bit is stripped from the profile), and is never shown. The guard (C) that Guard Membrane gated is everyone's.
 */
public final class RinkakuMutations {
    /** The ones that stay, in board order (1.0.41: none). */
    private static final MutationNode[] KEPT = {};

    private RinkakuMutations() {
    }

    /** The mutations a player can have, in board order (the patched menu and identity panel list these). */
    public static MutationNode[] nodes() {
        return KEPT.clone();
    }

    /** True for one that was cut (1.0.41: every one). */
    public static boolean removed(MutationNode node) {
        for (MutationNode kept : KEPT) {
            if (kept == node) {
                return false;
            }
        }
        return node != null;
    }

    /** The bits of every cut mutation. */
    public static long removedMask() {
        long mask = 0L;
        for (MutationNode node : MutationNode.values()) {
            mask |= removed(node) ? node.bit() : 0L;
        }
        return mask;
    }

    /** A stored mutation mask with the cut ones taken out. */
    public static long strip(long mask) {
        return mask & ~removedMask();
    }

    /**
     * Whether a player with stored mutations {@code mask} has {@code node} as far as the game is concerned: a cut one never
     * counts, except Guard Membrane, which everyone has (the guard is no longer gated).
     */
    public static boolean has(long mask, MutationNode node) {
        if (node == MutationNode.GUARD_MEMBRANE) {
            return true;
        }
        return node != null && !removed(node) && (mask & node.bit()) != 0L;
    }
}
