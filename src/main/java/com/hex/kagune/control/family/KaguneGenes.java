package com.hex.kagune.control.family;

import java.util.Random;

/**
 * 1.3.43: the kagune a ghoul child inherits (pure, tested), decided the night it is conceived with the rest of its {@link Genome}: the kinds,
 * as bits, from both parents, and its colour, from one of them. Each parent passes on what they have (a player their path: Rinkaku tails
 * or the Ukaku pair; Touka her wings, Ken his tails, Eto her Owl kagune); a child of two of the same kind has it whole, a child of two
 * kinds has both, its wings down to one (the owner: a Rinkaku father and an Ukaku mother, the tails and one wing, with the moves of both).
 */
public final class KaguneGenes {
    /** Not known (a child from before this: worked out from its parents when it next thinks). */
    public static final int UNKNOWN = -1;
    /** None (a human child, or a parent with no kagune). */
    public static final int NONE = 0;
    /** Rinkaku tails. */
    public static final int TAILS = 1;
    /** An Ukaku wing: one alone, or the pair with {@link #PAIR}. */
    public static final int WING = 2;
    /** The second Ukaku wing (only ever with {@link #WING}). */
    public static final int PAIR = 4;
    /** Eto's Owl kagune: her flesh, her moves, the Owl at the end. */
    public static final int OWL = 8;
    /** The Ukaku pair. */
    public static final int WINGS = WING | PAIR;
    private static final int ALL = TAILS | WINGS | OWL;

    private KaguneGenes() {
    }

    /** What a parent on {@code path} (a {@code KagunePath} id: "rinkaku", "ukaku"...) passes on; a ghoul still choosing passes on the tails (the original's Rinkaku). */
    public static int ofPath(String path, boolean ghoul) {
        if (!ghoul) {
            return NONE;
        }
        return "ukaku".equals(path) ? WINGS : TAILS;
    }

    /**
     * The child's from its mother's and father's: the same kind from both, that kind whole; one parent with none, the other's whole; two
     * kinds, all of them, the wings down to one.
     */
    public static int inherit(int mother, int father) {
        int m = clean(mother);
        int f = clean(father);
        if (m == NONE || m == f) {
            return f;
        }
        if (f == NONE) {
            return m;
        }
        int both = m | f;
        if ((both & WING) != 0 && (both & (TAILS | OWL)) != 0) {
            both &= ~PAIR;
        }
        return both;
    }

    /** {@code bits} with anything that isn't a kind dropped, and a lone second wing made a wing ({@link #UNKNOWN} and bad values: none). */
    public static int clean(int bits) {
        if (bits <= 0) {
            return NONE;
        }
        int out = bits & ALL;
        if ((out & PAIR) != 0) {
            out |= WING;
        }
        return out;
    }

    public static boolean tails(int bits) {
        return bits > 0 && (bits & TAILS) != 0;
    }

    /** How many Ukaku wings: 0, 1 or 2. */
    public static int wings(int bits) {
        if (bits <= 0 || (bits & WING) == 0) {
            return 0;
        }
        return (bits & PAIR) != 0 ? 2 : 1;
    }

    public static boolean owl(int bits) {
        return bits > 0 && (bits & OWL) != 0;
    }

    /** True if it has any kagune to fight with. */
    public static boolean any(int bits) {
        return clean(bits) != NONE;
    }

    /** The side its one wing grows on (1 its right, -1 its left), the same every time for its {@code seed}. */
    public static int side(long seed) {
        return (seed >>> 17 & 1L) == 0L ? 1 : -1;
    }

    /**
     * Its colour: one parent's ({@code roll} in 0..1 picks, the mother's below a half), only ever one who has a kagune to give it; -1 if
     * neither has (a human child).
     */
    public static int colour(int motherBits, int motherColour, int fatherBits, int fatherColour, double roll) {
        boolean m = any(motherBits);
        boolean f = any(fatherBits);
        if (!m && !f) {
            return -1;
        }
        boolean mother = m && (!f || roll < 0.5);
        return (mother ? motherColour : fatherColour) & 0xFFFFFF;
    }

    /** The same, rolled from {@code random}. */
    public static int colour(int motherBits, int motherColour, int fatherBits, int fatherColour, Random random) {
        return colour(motherBits, motherColour, fatherBits, fatherColour, random.nextDouble());
    }

    /** What it has, in words ("a Rinkaku tentacle and an Ukaku wing"), for {@code /kagune child} and the like. */
    public static String describe(int bits) {
        if (bits == UNKNOWN) {
            return "not known yet";
        }
        StringBuilder out = new StringBuilder();
        if (tails(bits)) {
            out.append("a Rinkaku tentacle"); // 1.3.44: one
        }
        int wings = wings(bits);
        if (wings > 0) {
            out.append(out.length() == 0 ? "" : " and ").append(wings == 2 ? "Ukaku wings" : "an Ukaku wing");
        }
        if (owl(bits)) {
            out.append(out.length() == 0 ? "" : " and ").append("the Owl's flesh");
        }
        return out.length() == 0 ? "none" : out.toString();
    }
}
