package com.hex.kagune.control.npc;

import com.hex.kagune.control.kakugan.KakuganEyes;

/**
 * 7.0.2: what a ghoul NPC looks like (pure): a human with the kakugan on its eyes. 1.0.41: in one of its own skins (painted by
 * {@code tools/ghoul_skins.py}), twelve boys and twelve girls (the girls on the slim arms), every one with its eyes where the kakugan
 * is drawn: the right eye on columns 1 and 2 of the face, the left on 5 and 6, from row 4 (one row tall for the boys, two for the
 * girls), the iris toward the nose, nothing over them; one boy wears a medical eyepatch over his left, and only his right turns.
 */
public final class NpcLook {
    /** Its skins, by the name of their texture under {@code textures/entity/ghoul/} (the boys first). */
    public static final String[] SKINS = {"boy_01", "boy_02", "boy_03", "boy_04", "boy_05", "boy_06", "boy_07", "boy_08", "boy_09", "boy_10", "boy_11",
        "boy_12", "girl_01", "girl_02", "girl_03", "girl_04", "girl_05", "girl_06", "girl_07", "girl_08", "girl_09", "girl_10", "girl_11", "girl_12"};
    /** How many of {@link #SKINS} are boys (the rest are girls). */
    public static final int BOYS = 12;
    /** The boy in the eyepatch. */
    public static final int PATCHED = 0;
    /** A boy's eyes (one row) and a girl's (two). */
    public static final KakuganEyes EYES = new KakuganEyes(1, 4, 5, 4, 1, true, false);
    public static final KakuganEyes GIRL_EYES = new KakuganEyes(1, 4, 5, 4, 2, true, false);

    private NpcLook() {
    }

    /** True if skin {@code index} is a girl's (drawn on the slim arms, given a girl's name). */
    public static boolean girl(int index) {
        return Math.floorMod(index, SKINS.length) >= BOYS;
    }

    /** Where the kakugan sits on skin {@code index}, and which eyes turn. */
    public static KakuganEyes eyes(int index) {
        int skin = Math.floorMod(index, SKINS.length);
        if (skin == PATCHED) {
            return EYES.withGhoul(KakuganEyes.RIGHT);
        }
        return girl(skin) ? GIRL_EYES : EYES;
    }

    /** The texture path of skin {@code index} (in the hexkagune namespace). */
    public static String texture(int index) {
        return "textures/entity/ghoul/" + SKINS[Math.floorMod(index, SKINS.length)] + ".png";
    }
}
