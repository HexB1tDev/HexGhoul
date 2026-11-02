package com.hex.kagune.control.village;

/**
 * 1.3.75: the villagers' own skins (pure; written by {@code tools/human_skins.py}: do not edit by hand): the children's skins with their
 * hair, clothes and eyes turned to other colours, so no villager looks like a child of the player's.
 */
public final class HumanSkins {
    private HumanSkins() {
    }

    /** One skin: its texture (the mod's), girl or boy, slim arms or wide. */
    public record Skin(String path, boolean girl, boolean slim) {
    }

    public static final Skin[] ALL = {
        new Skin("textures/entity/human/human_00.png", true, true), // from daughter_21, hue +150
        new Skin("textures/entity/human/human_01.png", true, true), // from daughter_08, hue +220
        new Skin("textures/entity/human/human_02.png", true, false), // from daughter_18, hue +300
        new Skin("textures/entity/human/human_03.png", true, false), // from daughter_19, hue +90
        new Skin("textures/entity/human/human_04.png", true, false), // from daughter_13, hue +40
        new Skin("textures/entity/human/human_05.png", false, false), // from son_04, hue +200
        new Skin("textures/entity/human/human_06.png", false, false), // from son_08, hue +120
        new Skin("textures/entity/human/human_07.png", false, false), // from son_12, hue +260
        new Skin("textures/entity/human/human_08.png", false, false), // from son_06, hue +330
        new Skin("textures/entity/human/human_09.png", false, false), // from son_13, hue +60
    };
}
