package com.hex.kagune.control.npc;

/**
 * 3.2.2: one at a time (pure, tested; the world's side is {@code Engaged}), the owner: "we also need to minimize the interactions. In terms
 * of like not reduce the amount. It's just like if I'm being already interacted by someone giving me a hug, for example, like another one
 * shouldn't be coming to do the same thing." Nobody starts something with a player by themselves (walking up for a hug, a kiss, an arm, a
 * hand, coming over with something to say, a child's own hug or wave) while someone else already has them (walking up to them, in a hug or
 * a kiss, holding on to them, in a moment, a card waiting on their answer, a talk open), nor for {@link #GAP} after that one let go: not
 * fewer of them, just one at a time. What the player asks for themselves is never held back.
 */
public final class Turns {
    private Turns() {
    }

    /** How long after someone lets go of a player before the next may start something with them (ticks). */
    public static final long GAP = 80L;

    /**
     * True if {@code me} (an entity id) must wait its turn: {@code who} (0: nobody) has the player now, or {@code last} had them
     * {@code ago} ticks back (negative: never seen) and that was less than {@link #GAP} ago ({@code recent} false: only now counts, for a
     * moment that can't wait, such as walking in through the door with a child).
     */
    public static boolean waits(int me, int who, int last, long ago, boolean recent) {
        if (who != 0 && who != me) {
            return true;
        }
        return recent && last != 0 && last != me && ago >= 0L && ago < GAP;
    }
}
