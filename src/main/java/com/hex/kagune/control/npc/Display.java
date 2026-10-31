package com.hex.kagune.control.npc;

/**
 * 1.3.11: how much of what they are a player shows (pure), from nothing to the worst there is: their eyes (the kakugan), their kagune out,
 * a kakuja (any path's: Rinkaku's, Ukaku's, the Owl, Graft's), and dread (a final kakuja, the Dragon). NPCs notice when it rises
 * ({@code Sightings}) and react by who they are to them.
 */
public enum Display {
    NONE,
    EYES,
    KAGUNE,
    KAKUJA,
    DREAD;

    /** Whether going from {@code before} to this shows more than before. */
    public boolean rose(Display before) {
        return before == null ? this != NONE : this.ordinal() > before.ordinal();
    }

    /** Whether someone who doesn't know them should be afraid (a kakuja and worse). */
    public boolean frightening() {
        return this.ordinal() >= KAKUJA.ordinal();
    }
}
