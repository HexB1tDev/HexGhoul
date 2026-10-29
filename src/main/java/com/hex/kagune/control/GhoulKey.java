package com.hex.kagune.control;

/**
 * What the kagune key does (3.0.0; pure, tested). 1.2.0: R alone is kakugan mode: it opens or closes the kakugan without the
 * kagune (the eyes' own fight: Big Punch, flips, the blitz and the kick), and with the kagune out it puts only the kagune away,
 * the eyes staying open, so the fight goes on in kakugan mode. Ctrl + R (the Kagune key, rebindable) brings the kagune out, the
 * eyes with it, and with the kagune out puts everything away, eyes and all.
 *
 * @param toggleKagune bring the kagune out or put it in (the original's toggle)
 * @param ghoul        ghoul mode after the press
 */
public record GhoulKey(boolean toggleKagune, boolean ghoul) {
    /** The press: {@code kaguneOut} and {@code ghoul} before it, {@code unleash} if the Kagune key (Ctrl) was held. */
    public static GhoulKey press(boolean kaguneOut, boolean ghoul, boolean unleash) {
        if (unleash) {
            return kaguneOut ? new GhoulKey(true, false) : new GhoulKey(true, true);
        }
        return kaguneOut ? new GhoulKey(true, true) : new GhoulKey(false, !ghoul);
    }
}
