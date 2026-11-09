package com.hex.kagune.control.family;

import java.util.Random;

/**
 * 1.3.43: the kagune a ghoul child inherits (pure, tested), decided the night it is conceived with the rest of its {@link Genome}: the kinds,
 * as bits, from both parents, and its colour, from one of them. Each parent passes on what they have (a player their path: Rinkaku tails
 * or the Ukaku pair; Touka her wings, Ken his tails, Eto her Owl kagune); a child of two of the same kind has it whole, a child of two
 * kinds has both, its wings down to one (the owner: a Rinkaku father and an Ukaku mother, the tails and one wing, with the moves of both).
 * 2.0.6: of two kinds, a chance: 40% the mother's alone, 40% the father's alone, 20% both ({@link #inherit(int, int, double)}).
 *
 * <p>4.0.0 (the owner: "Make sure The child of a Bikaku user with all the other combinations matches perfectly. Children: Kids work with
 * chances now, depending on the parents, assuming for example a Bikaku user and a Ukaku user, the child has a chance to come out with only
 * 1 kagune type like just 2 ukaku wings, just Bikaku worm tendrils, or both"): a Bikaku parent passes on its worm ({@link #WORM}; two
 * Bikaku parents, a worm from each, {@link #WORM_TWIN}); of two kinds, a third of children take their mother's whole, a third their
 * father's whole, a third both (the wings down to one beside anything else).
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
    /**
     * 2.3.7 (the owner: "they're going to have one of each, both parents. If they're both ghouls, you get both halves"): a second tentacle,
     * one from each Rinkaku parent (only with {@link #TAILS}).
     */
    public static final int TWIN = 16;
    /** 4.0.0: a Bikaku worm (its mouth, its bites). */
    public static final int WORM = 32;
    /** 4.0.0: a second worm, one from each Bikaku parent (only with {@link #WORM}). */
    public static final int WORM_TWIN = 64;
    private static final int ALL = TAILS | WINGS | OWL | TWIN | WORM | WORM_TWIN;

    private KaguneGenes() {
    }

    /**
     * What a parent on {@code path} (a {@code KagunePath} id: "rinkaku", "ukaku"...) passes on; a ghoul still choosing passes on the tails
     * (the original's Rinkaku). 4.0.0: a Bikaku ghoul its worm.
     */
    public static int ofPath(String path, boolean ghoul) {
        if (!ghoul) {
            return NONE;
        }
        return "ukaku".equals(path) ? WINGS : "bikaku".equals(path) ? WORM : TAILS;
    }

    /**
     * The child's from its mother's and father's: the same kind from both, that kind whole; one parent with none, the other's whole; two
     * kinds, all of them, the wings down to one.
     */
    public static int inherit(int mother, int father) {
        int m = clean(mother);
        int f = clean(father);
        // 2.3.7: two Rinkaku parents, a tentacle from each. 4.0.0: two Bikaku parents, a worm from each.
        int twin = ((m & TAILS) != 0 && (f & TAILS) != 0 ? TWIN : 0) | ((m & WORM) != 0 && (f & WORM) != 0 ? WORM_TWIN : 0);
        if (m == NONE || m == f) {
            return f == NONE ? NONE : f | twin;
        }
        if (f == NONE) {
            return m;
        }
        int both = m | f | twin;
        if ((both & WING) != 0 && (both & (TAILS | OWL | WORM)) != 0) {
            both &= ~PAIR;
        }
        return both;
    }

    /**
     * 2.0.6: the share of children of two kinds who take only their mother's kind (as many take only their father's; the rest both). 2.3.7:
     * none (both always). 4.0.0 (the owner: "Kids work with chances now"): a third.
     */
    public static final double ONE_KIND = 1.0 / 3.0;

    /**
     * 2.0.6 (the owner: kids should "have a chance to enherit both kagunes"; chosen 40/40/20): of two different kinds, {@code roll} in
     * [0, 1) below {@link #ONE_KIND} the mother's kind alone, below twice that the father's alone, else both ({@link #inherit(int, int)}: the
     * wings down to one). The same kind from both, or one parent with none: as before, whatever the roll.
     */
    public static int inherit(int mother, int father, double roll) {
        int m = clean(mother);
        int f = clean(father);
        if (m == NONE || f == NONE || m == f) {
            return inherit(mother, father);
        }
        return roll < ONE_KIND ? m : roll < 2.0 * ONE_KIND ? f : inherit(mother, father);
    }

    /**
     * 2.0.6: its colour once its kind is known: a child that took one parent's kind alone has that parent's colour; otherwise {@code rolled}
     * (one parent's, {@link #colour(int, int, int, int, double)}).
     */
    public static int colourOf(int bits, int motherBits, int motherColour, int fatherBits, int fatherColour, int rolled) {
        int m = clean(motherBits);
        int f = clean(fatherBits);
        int own = clean(bits);
        if (own == NONE || m == f || m == NONE || f == NONE) {
            return rolled;
        }
        return own == m ? motherColour & 0xFFFFFF : own == f ? fatherColour & 0xFFFFFF : rolled;
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
        if ((out & TAILS) == 0) {
            out &= ~TWIN;
        }
        if ((out & WORM) == 0) {
            out &= ~WORM_TWIN;
        }
        return out;
    }

    /** 4.0.0: how many Bikaku worms: 0, 1, or 2 (one from each Bikaku parent). */
    public static int worms(int bits) {
        return bits <= 0 || (bits & WORM) == 0 ? 0 : (bits & WORM_TWIN) != 0 ? 2 : 1;
    }

    public static boolean tails(int bits) {
        return bits > 0 && (bits & TAILS) != 0;
    }

    /** 2.3.7: how many Rinkaku tentacles: 0, 1, or 2 (one from each Rinkaku parent). */
    public static int tentacles(int bits) {
        return !tails(bits) ? 0 : (bits & TWIN) != 0 ? 2 : 1;
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
            out.append(out.length() == 0 ? "" : " and ").append("Eto's flesh");
        }
        int worms = worms(bits);
        if (worms > 0) {
            out.append(out.length() == 0 ? "" : " and ").append(worms == 2 ? "two Bikaku worms" : "a Bikaku worm");
        }
        return out.length() == 0 ? "none" : out.toString();
    }
}
