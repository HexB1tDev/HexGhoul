package com.hex.kagune.control.npc;

/**
 * 1.3.3: the body of a one-of-a-kind NPC (Eto, and every one made after her): what every such body shares, whatever it is. It never
 * takes fall damage, it is the only one of its kind in the world ({@code UniqueNpcs}), and it belongs at its home, the place it was made
 * (its {@link #origin}): it comes back there a day after it dies, and goes back there by itself after a day away with no one about.
 */
public interface UniqueBody {
    /** Its key among the one-of-a-kind NPCs ({@code "eto"}...). */
    String uniqueKey();

    /** Where it belongs (a block: where it was first made, one up), or null before its first tick. */
    net.minecraft.core.BlockPos origin();

    /** It goes home: no longer following anyone or waiting anywhere, at its origin, and settled there. */
    void goHome();
}
