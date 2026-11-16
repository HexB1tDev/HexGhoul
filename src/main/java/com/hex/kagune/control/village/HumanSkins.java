package com.hex.kagune.control.village;

/**
 * 1.3.75: the villagers' own skins (pure; written by {@code tools/human_skins.py}: do not edit by hand): the children's skins with their
 * hair, clothes and eyes turned to other colours, so no villager looks like a child of the player's.
 */
public final class HumanSkins {
    private HumanSkins() {
    }

    /**
     * One skin: its texture (the mod's), girl or boy, slim arms or wide, and the child's skin it was made from (1.3.76: its eyes are in
     * the same place, so it blinks with that skin's shut eyes, {@code ChildBlinks}).
     */
    public record Skin(String path, boolean girl, boolean slim, String child) {
    }

    public static final Skin[] ALL = {
        new Skin("textures/entity/human/human_00.png", true, true, "daughter_21"), // hue +150
        new Skin("textures/entity/human/human_01.png", true, true, "daughter_08"), // hue +220
        new Skin("textures/entity/human/human_02.png", true, false, "daughter_18"), // hue +300
        new Skin("textures/entity/human/human_03.png", true, false, "daughter_19"), // hue +90
        new Skin("textures/entity/human/human_04.png", true, false, "daughter_13"), // hue +40
        new Skin("textures/entity/human/human_05.png", false, false, "son_04"), // hue +200
        new Skin("textures/entity/human/human_06.png", false, false, "son_08"), // hue +120
        new Skin("textures/entity/human/human_07.png", false, false, "son_12"), // hue +260
        new Skin("textures/entity/human/human_08.png", false, false, "son_06"), // hue +330
        new Skin("textures/entity/human/human_09.png", false, false, "son_13"), // hue +60
        new Skin("textures/entity/human/human_10.png", true, false, "daughter_01"), // hue +97
        new Skin("textures/entity/human/human_11.png", true, false, "daughter_13"), // hue +192
        new Skin("textures/entity/human/human_12.png", true, true, "daughter_15"), // hue +164
        new Skin("textures/entity/human/human_13.png", true, false, "daughter_16"), // hue +225
        new Skin("textures/entity/human/human_14.png", true, false, "daughter_16"), // hue +105
        new Skin("textures/entity/human/human_15.png", true, false, "daughter_18"), // hue +77
        new Skin("textures/entity/human/human_16.png", true, false, "daughter_18"), // hue +137
        new Skin("textures/entity/human/human_17.png", false, false, "son_04"), // hue +83
        new Skin("textures/entity/human/human_18.png", false, false, "son_04"), // hue +143
        new Skin("textures/entity/human/human_19.png", false, false, "son_05"), // hue +54
        new Skin("textures/entity/human/human_20.png", false, false, "son_06"), // hue +115
        new Skin("textures/entity/human/human_21.png", false, false, "son_06"), // hue +265
        new Skin("textures/entity/human/human_22.png", false, false, "son_07"), // hue +176
        new Skin("textures/entity/human/human_23.png", false, false, "son_10"), // hue +149
        new Skin("textures/entity/human/human_24.png", false, false, "son_13"), // hue +182
        new Skin("textures/entity/human/human_25.png", false, false, "son_15"), // hue +123
    };
}
