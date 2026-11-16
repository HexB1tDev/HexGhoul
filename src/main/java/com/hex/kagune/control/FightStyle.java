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
    SPEEDSTER("speedster", "Speedster", Gate.STYLE_SPEEDSTER),
    /**
     * 4.1.7 (the owner: "this combat style upon learned fully replaces the default M1 combos"): Kick Master's string on M1 (kicks and
     * punches: {@link com.hex.kagune.control.pale.PaleStrike.Style#KICKS}). The General teaches it, to a human or a ghoul behind a full mask;
     * a human fights with it whenever the main hand is empty. 4.1.8: its own right click, held and let go; 4.1.87, the dash kick (the owner:
     * "The right click should be a chargeable dash kick, not a teleport").
     */
    KICK_MASTER("kick_master", "Kick Master", Gate.STYLE_KICK_MASTER);

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

    /** True if an empty right click is this style's own move (never the game's). 4.1.8: Kick Master's (4.1.87: its dash kick). */
    public boolean ownUse() {
        return this != NONE;
    }

    /** 4.1.7: the string M1 throws in this style (the Speed Blitz, or Kick Master's kicks). */
    public com.hex.kagune.control.pale.PaleStrike.Style strikes() {
        return this == KICK_MASTER ? com.hex.kagune.control.pale.PaleStrike.Style.KICKS : com.hex.kagune.control.pale.PaleStrike.Style.BLITZ;
    }

    /**
     * The style M1 fights in (with only the eyes open; 4.1.7: a human's with the hand empty): the one learned ({@code open}: their gates), or
     * {@link #NONE}. 4.1.7: the one learned last in this list when there are more (Kick Master "fully replaces the default M1 combos").
     */
    public static FightStyle of(Predicate<Gate> open) {
        FightStyle[] all = values();
        for (int i = all.length - 1; i >= 0; i--) {
            if (all[i].gate != null && open.test(all[i].gate)) {
                return all[i];
            }
        }
        return NONE;
    }

    /** 4.1.7: the style whose right click is used: M1's style if it has its own (4.1.8: every style does), else Speedster's flips if learned, else {@link #NONE}. */
    public static FightStyle use(Predicate<Gate> open) {
        FightStyle m1 = of(open);
        if (m1.ownUse()) {
            return m1;
        }
        return open.test(Gate.STYLE_SPEEDSTER) ? SPEEDSTER : NONE;
    }
}
