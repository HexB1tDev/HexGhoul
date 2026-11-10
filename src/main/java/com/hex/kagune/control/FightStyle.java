package com.hex.kagune.control;

import com.hex.kagune.control.quest.Gate;
import java.util.function.Predicate;

/**
 * 4.1.3 (the owner: "until a fighting style is unlocked, the player can only use default punches in kakugan mode, right click is also
 * default, the previous M1 and M2 flips are now all part of a fighting style called Speedster"): how a ghoul fights with only the kakugan
 * open (pure, tested). A style is what M1 and the right click do there; each is learned (its {@link #gate}). With none, M1 is the game's own
 * punch and the right click the game's own. The keys' moves (the Big Punch, the Speed Kick) are not a style's: they stay as they were.
 */
public enum FightStyle {
    /** No style learned: the game's own punch, the game's own right click. */
    NONE("none", "Bare hands", null),
    /** The Speed Blitz on M1 and the flips on the right click (the eye-mode fight as it was before 4.1.3). Ayato teaches it. */
    SPEEDSTER("speedster", "Speedster", Gate.STYLE_SPEEDSTER);

    private final String id;
    private final String displayName;
    private final Gate gate;

    FightStyle(String id, String displayName, Gate gate) {
        this.id = id;
        this.displayName = displayName;
        this.gate = gate;
    }

    public String id() {
        return this.id;
    }

    public String displayName() {
        return this.displayName;
    }

    /** The gate that has to be open for it (null for {@link #NONE}). */
    public Gate gate() {
        return this.gate;
    }

    /** True if M1 is this style's own string (never the game's punch). */
    public boolean ownAttack() {
        return this != NONE;
    }

    /** True if an empty right click is this style's own move (never the game's). */
    public boolean ownUse() {
        return this != NONE;
    }

    /** The style a ghoul fights in with only the eyes open: the one learned ({@code open}: their gates), or {@link #NONE}. */
    public static FightStyle of(Predicate<Gate> open) {
        for (FightStyle style : values()) {
            if (style.gate != null && open.test(style.gate)) {
                return style;
            }
        }
        return NONE;
    }
}
