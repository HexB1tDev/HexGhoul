package com.hex.kagune.control.npc;

/**
 * 1.3.11: how much of what they are a player shows (pure), from nothing to the worst there is: their eyes (the kakugan), their kagune out,
 * a kakuja (any path's: Rinkaku's, Ukaku's, the Owl, Graft's), and dread (a final kakuja, the Dragon). NPCs notice when it rises
 * ({@code Sightings}) and react by who they are to them. 1.3.64: dread is what they wear now ({@link #of}), never what they once were.
 */
public enum Display {
    NONE,
    EYES,
    KAGUNE,
    KAKUJA,
    DREAD;

    /**
     * 1.3.64: what a player shows now, the worst first: dread while the Dragon has them ({@code dragon}), while they wear War or Peace
     * ({@code king}: worn now, not the preview, never the save that the Dragon once went through them) or in Rinkaku's or Ukaku's final
     * kakuja ({@code finalKakuja}); a kakuja (any path's first one, the Owl, Graft's); their kagune out; their eyes; nothing.
     */
    public static Display of(boolean dragon, boolean king, boolean finalKakuja, boolean kakuja, boolean kagune, boolean eyes) {
        if (dragon || king || finalKakuja) {
            return DREAD;
        }
        return kakuja ? KAKUJA : kagune ? KAGUNE : eyes ? EYES : NONE;
    }

    /** Whether going from {@code before} to this shows more than before. */
    public boolean rose(Display before) {
        return before == null ? this != NONE : this.ordinal() > before.ordinal();
    }

    /** Whether someone who doesn't know them should be afraid (a kakuja and worse). */
    public boolean frightening() {
        return this.ordinal() >= KAKUJA.ordinal();
    }
}
