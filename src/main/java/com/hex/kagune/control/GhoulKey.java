package com.hex.kagune.control;

/**
 * What the kagune key does (3.0.0; pure, tested): R alone opens or closes the kakugan without the kagune (ghoul mode), or with
 * the kagune out puts everything away; Alt + R unleashes the kagune, the eyes with it.
 *
 * @param toggleKagune bring the kagune out or put it in (the original's toggle)
 * @param ghoul        ghoul mode after the press
 */
public record GhoulKey(boolean toggleKagune, boolean ghoul) {
    /** The press: {@code kaguneOut} and {@code ghoul} before it, {@code unleash} if Alt was held. */
    public static GhoulKey press(boolean kaguneOut, boolean ghoul, boolean unleash) {
        if (unleash) {
            return kaguneOut ? new GhoulKey(false, ghoul) : new GhoulKey(true, true);
        }
        return kaguneOut ? new GhoulKey(true, false) : new GhoulKey(false, !ghoul);
    }
}
