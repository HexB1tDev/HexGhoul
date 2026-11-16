package com.hex.kagune.control;

import com.hex.kagune.control.ukaku.UkakuSounds;

/**
 * 4.1.93: the original kagune's own sound events (the jar's {@code HexKaguneSounds}, played through {@code KaguneServerController.playAt}
 * by the jar's own moves and by many of the mod's) as each kagune plays them (pure): a Rinkaku's ({@link RinkakuSounds}), a Bikaku's
 * ({@link BikakuSounds}), an Ukaku's ({@link UkakuSounds}), all the game's own; null for the paths that keep the pack's (Owl DNA, Graft DNA,
 * none), and for an event that is not one of the kagune's.
 */
public final class KaguneVoices {
    /** The original's sixteen kagune events, by their sound event names. */
    public static final String[] EVENTS = {"manifest_rip", "kagune_reshape", "kagune_dismiss", "kagune_whip", "flesh_impact", "bone_crack", "bone_crush",
        "kagune_grab", "kagune_strain", "kagune_slam", "kagune_guard", "kagune_anchor", "centipede_click", "shard_launch", "kagune_idle", "crush_kill"};

    private KaguneVoices() {
    }

    /** What {@code event} (one of {@link #EVENTS}) plays as for a kagune of {@code path}; null: as it was. */
    public static String of(KagunePath path, String event) {
        if (path == null || event == null) {
            return null;
        }
        return switch (path) {
            case RINKAKU -> rinkaku(event);
            case BIKAKU -> bikaku(event);
            case UKAKU -> ukaku(event);
            default -> null;
        };
    }

    static String rinkaku(String event) {
        return switch (event) {
            case "manifest_rip" -> RinkakuSounds.MANIFEST;
            case "kagune_reshape" -> RinkakuSounds.RESHAPE;
            case "kagune_dismiss" -> RinkakuSounds.DISMISS;
            case "kagune_whip" -> RinkakuSounds.WHIP;
            case "flesh_impact" -> RinkakuSounds.IMPACT;
            case "bone_crack" -> RinkakuSounds.BONE_CRACK;
            case "bone_crush" -> RinkakuSounds.BONE_CRUSH;
            case "kagune_grab" -> RinkakuSounds.GRAB;
            case "kagune_strain" -> RinkakuSounds.STRAIN;
            case "kagune_slam" -> RinkakuSounds.SLAM;
            case "kagune_guard" -> RinkakuSounds.GUARD;
            case "kagune_anchor" -> RinkakuSounds.ANCHOR;
            case "centipede_click" -> RinkakuSounds.CHITTER;
            case "shard_launch" -> RinkakuSounds.HURL;
            case "kagune_idle" -> RinkakuSounds.WRITHE;
            case "crush_kill" -> RinkakuSounds.CRUSH_KILL;
            default -> null;
        };
    }

    static String bikaku(String event) {
        return switch (event) {
            case "manifest_rip", "kagune_reshape", "kagune_dismiss" -> BikakuSounds.RESHAPE;
            case "kagune_whip", "shard_launch" -> BikakuSounds.LUNGE;
            case "flesh_impact" -> BikakuSounds.IMPACT;
            case "bone_crack", "bone_crush" -> BikakuSounds.BONE_CRUSH;
            case "kagune_grab" -> BikakuSounds.SNAP;
            case "kagune_strain", "centipede_click", "kagune_idle" -> BikakuSounds.WRITHE;
            case "kagune_slam" -> BikakuSounds.SLAM;
            case "kagune_guard" -> BikakuSounds.ARMOUR;
            case "kagune_anchor" -> BikakuSounds.ANCHOR;
            case "crush_kill" -> BikakuSounds.SQUELCH;
            default -> null;
        };
    }

    /** 4.1.93: the Ukaku's too (4.1.91 missed what the original plays for every path: the wings put away, reshaped). */
    static String ukaku(String event) {
        return switch (event) {
            case "manifest_rip" -> UkakuSounds.MANIFEST;
            case "kagune_reshape", "kagune_strain", "kagune_anchor" -> UkakuSounds.WING_FLAP;
            case "kagune_dismiss" -> UkakuSounds.SNUFF;
            case "kagune_whip" -> UkakuSounds.WING_CUT;
            case "flesh_impact", "kagune_grab" -> UkakuSounds.SLAP;
            case "bone_crack", "bone_crush" -> UkakuSounds.SLAP_HEAVY;
            case "kagune_slam", "crush_kill" -> UkakuSounds.SLAM;
            case "kagune_guard" -> UkakuSounds.CRYSTAL_HIT;
            case "centipede_click", "kagune_idle" -> UkakuSounds.WING_FLICK;
            case "shard_launch" -> UkakuSounds.FLAME_BURST;
            default -> null;
        };
    }
}
