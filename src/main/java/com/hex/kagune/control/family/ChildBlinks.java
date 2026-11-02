package com.hex.kagune.control.family;

/**
 * 1.3.76: the children's skins that can shut their eyes (pure; written by {@code tools/blink_skins.py}: do not edit by hand): each has
 * {@code <key>_closed.png} beside it. A skin whose eyes are already shut, or hidden by its hair, has none.
 */
public final class ChildBlinks {
    private ChildBlinks() {
    }

    private static final java.util.Set<String> KEYS = java.util.Set.of(
        "daughter_01",
        "daughter_03",
        "daughter_05",
        "daughter_08",
        "daughter_09",
        "daughter_13",
        "daughter_14",
        "daughter_15",
        "daughter_16",
        "daughter_17",
        "daughter_18",
        "daughter_19",
        "daughter_23",
        "son_01",
        "son_02",
        "son_03",
        "son_04",
        "son_05",
        "son_06",
        "son_07",
        "son_08",
        "son_09",
        "son_10",
        "son_11",
        "son_12",
        "son_13",
        "son_15",
        "son_16");

    /** True if skin {@code key} has its eyes-shut overlay. */
    public static boolean has(String key) {
        return key != null && KEYS.contains(key);
    }

    /** The overlay's texture path (the mod's), for a skin that {@link #has} one. */
    public static String closed(String key) {
        return "textures/entity/child/skins/" + key + "_closed.png";
    }
}
