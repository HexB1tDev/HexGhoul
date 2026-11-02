package com.hex.kagune.control.family;

import java.util.Map;

/**
 * 1.3.79: each child skin's looks (pure; written by {@code tools/child_variants.py}: do not edit by hand): the skin as it is, and
 * versions of it with other hair and eye colours ({@code <key>_vN.png}). A child wears its skin ({@link ChildSkins#pick}) in the look
 * whose hair and eyes are nearest the ones its genes gave it, its parents' ({@link #pick}). The versions share the skin's overlays
 * (kakugan, shut eyes, blush): the eyes are where they were.
 */
public final class ChildLooks {
    private ChildLooks() {
    }

    /** One look: its texture's key, its hair colour and its eye colour (RGB; 0 for eyes not boxed). */
    public record Look(String texture, int hair, int eyes) {
    }

    static final Map<String, Look[]> LOOKS = Map.ofEntries(
        Map.entry("daughter_01", new Look[] {new Look("daughter_01", 0xC8DCD9, 0x506E77), new Look("daughter_01_v1", 0x1E1A1E, 0x4682DC), new Look("daughter_01_v2", 0xDEBC70, 0x785032), new Look("daughter_01_v3", 0x9C7ED2, 0x965AD2), new Look("daughter_01_v4", 0x8C3A24, 0x3CA05A)}),
        Map.entry("daughter_02", new Look[] {new Look("daughter_02", 0xE0D3C4, 0x000000)}),
        Map.entry("daughter_03", new Look[] {new Look("daughter_03", 0x4E4645, 0x727477), new Look("daughter_03_v1", 0xCED2DC, 0x3CA05A), new Look("daughter_03_v2", 0x1E1A1E, 0x28AAB4), new Look("daughter_03_v3", 0xDEBC70, 0xD2A028), new Look("daughter_03_v4", 0x9C7ED2, 0x4682DC)}),
        Map.entry("daughter_04", new Look[] {new Look("daughter_04", 0xCDB6AD, 0x000000)}),
        Map.entry("daughter_05", new Look[] {new Look("daughter_05", 0xB08D80, 0xC3B4AF), new Look("daughter_05_v1", 0xB08D80, 0xD2A028), new Look("daughter_05_v2", 0xB08D80, 0x4682DC), new Look("daughter_05_v3", 0xB08D80, 0x785032), new Look("daughter_05_v4", 0xB08D80, 0x965AD2)}),
        Map.entry("daughter_06", new Look[] {new Look("daughter_06", 0x9C8984, 0x000000), new Look("daughter_06_v1", 0x5070CE, 0x000000), new Look("daughter_06_v2", 0x6E482E, 0x000000), new Look("daughter_06_v3", 0xCED2DC, 0x000000), new Look("daughter_06_v4", 0x1E1A1E, 0x000000)}),
        Map.entry("daughter_07", new Look[] {new Look("daughter_07", 0x474343, 0x000000)}),
        Map.entry("daughter_08", new Look[] {new Look("daughter_08", 0x918B84, 0xB0A4B9), new Look("daughter_08_v1", 0x918B84, 0x4682DC), new Look("daughter_08_v2", 0x918B84, 0x785032), new Look("daughter_08_v3", 0x918B84, 0x965AD2), new Look("daughter_08_v4", 0x918B84, 0x3CA05A)}),
        Map.entry("daughter_09", new Look[] {new Look("daughter_09", 0xCDA898, 0x908E8D), new Look("daughter_09_v1", 0xCDA898, 0x28AAB4), new Look("daughter_09_v2", 0xCDA898, 0xD2A028), new Look("daughter_09_v3", 0xCDA898, 0x4682DC), new Look("daughter_09_v4", 0xCDA898, 0x785032)}),
        Map.entry("daughter_10", new Look[] {new Look("daughter_10", 0xC3C5CA, 0x000000)}),
        Map.entry("daughter_11", new Look[] {new Look("daughter_11", 0x71625B, 0x000000), new Look("daughter_11_v1", 0x5AA48A, 0x000000), new Look("daughter_11_v2", 0xC86C32, 0x000000), new Look("daughter_11_v3", 0x5070CE, 0x000000), new Look("daughter_11_v4", 0xCED2DC, 0x000000)}),
        Map.entry("daughter_12", new Look[] {new Look("daughter_12", 0x0F0E0E, 0x000000), new Look("daughter_12_v1", 0xECE4CE, 0x000000), new Look("daughter_12_v2", 0x5AA48A, 0x000000), new Look("daughter_12_v3", 0xC86C32, 0x000000), new Look("daughter_12_v4", 0x5070CE, 0x000000)}),
        Map.entry("daughter_13", new Look[] {new Look("daughter_13", 0xF58FA6, 0xA76D66), new Look("daughter_13_v1", 0xECE4CE, 0x788CA0), new Look("daughter_13_v2", 0x5AA48A, 0x28AAB4), new Look("daughter_13_v3", 0xC86C32, 0xD2A028), new Look("daughter_13_v4", 0x5070CE, 0x4682DC)}),
        Map.entry("daughter_14", new Look[] {new Look("daughter_14", 0xEBD6D2, 0xA28882), new Look("daughter_14_v1", 0xEBD6D2, 0x965AD2), new Look("daughter_14_v2", 0xEBD6D2, 0x3CA05A), new Look("daughter_14_v3", 0xEBD6D2, 0x28AAB4), new Look("daughter_14_v4", 0xEBD6D2, 0xD2A028)}),
        Map.entry("daughter_15", new Look[] {new Look("daughter_15", 0xEDAEC3, 0xB9616A), new Look("daughter_15_v1", 0x5AA48A, 0x4682DC), new Look("daughter_15_v2", 0xC86C32, 0x785032), new Look("daughter_15_v3", 0x5070CE, 0x965AD2), new Look("daughter_15_v4", 0x6E482E, 0x3CA05A)}),
        Map.entry("daughter_16", new Look[] {new Look("daughter_16", 0x5764A2, 0x62739D), new Look("daughter_16_v1", 0xE88CB0, 0x28AAB4), new Look("daughter_16_v2", 0x3E2A20, 0xD2A028), new Look("daughter_16_v3", 0xECE4CE, 0x4682DC), new Look("daughter_16_v4", 0xC86C32, 0x785032)}),
        Map.entry("daughter_17", new Look[] {new Look("daughter_17", 0x876759, 0x8E554C), new Look("daughter_17_v1", 0x876759, 0x3CA05A), new Look("daughter_17_v2", 0x876759, 0x788CA0), new Look("daughter_17_v3", 0x876759, 0x28AAB4), new Look("daughter_17_v4", 0x876759, 0xD2A028)}),
        Map.entry("daughter_18", new Look[] {new Look("daughter_18", 0xBB9157, 0x876D3C), new Look("daughter_18_v1", 0xE88CB0, 0x965AD2), new Look("daughter_18_v2", 0x3E2A20, 0x3CA05A), new Look("daughter_18_v3", 0xECE4CE, 0x788CA0), new Look("daughter_18_v4", 0x5AA48A, 0x28AAB4)}),
        Map.entry("daughter_19", new Look[] {new Look("daughter_19", 0x463D42, 0x705934), new Look("daughter_19_v1", 0x9C7ED2, 0xD2A028), new Look("daughter_19_v2", 0x8C3A24, 0x4682DC), new Look("daughter_19_v3", 0xE88CB0, 0x965AD2), new Look("daughter_19_v4", 0xECE4CE, 0x3CA05A)}),
        Map.entry("daughter_20", new Look[] {new Look("daughter_20", 0x4F4948, 0x000000), new Look("daughter_20_v1", 0x9C7ED2, 0x000000), new Look("daughter_20_v2", 0x8C3A24, 0x000000), new Look("daughter_20_v3", 0xE88CB0, 0x000000), new Look("daughter_20_v4", 0xECE4CE, 0x000000)}),
        Map.entry("daughter_21", new Look[] {new Look("daughter_21", 0x422B2A, 0x000000), new Look("daughter_21_v1", 0x9C7ED2, 0x000000), new Look("daughter_21_v2", 0x8C3A24, 0x000000), new Look("daughter_21_v3", 0xE88CB0, 0x000000), new Look("daughter_21_v4", 0xECE4CE, 0x000000)}),
        Map.entry("daughter_22", new Look[] {new Look("daughter_22", 0x4A384C, 0x000000), new Look("daughter_22_v1", 0x9C7ED2, 0x000000), new Look("daughter_22_v2", 0x8C3A24, 0x000000), new Look("daughter_22_v3", 0xE88CB0, 0x000000), new Look("daughter_22_v4", 0xECE4CE, 0x000000)}),
        Map.entry("daughter_23", new Look[] {new Look("daughter_23", 0xE1ECED, 0x7A969C), new Look("daughter_23_v1", 0xE1ECED, 0x28AAB4), new Look("daughter_23_v2", 0xE1ECED, 0xD2A028), new Look("daughter_23_v3", 0xE1ECED, 0x4682DC), new Look("daughter_23_v4", 0xE1ECED, 0x785032)}),
        Map.entry("son_01", new Look[] {new Look("son_01", 0x483F40, 0x2E4869), new Look("son_01_v1", 0xDEBC70, 0x3CA05A), new Look("son_01_v2", 0x9C7ED2, 0x788CA0), new Look("son_01_v3", 0x8C3A24, 0x28AAB4), new Look("son_01_v4", 0xE88CB0, 0xD2A028)}),
        Map.entry("son_02", new Look[] {new Look("son_02", 0x0C0B0B, 0x2F3D2E), new Look("son_02_v1", 0xDEBC70, 0x785032), new Look("son_02_v2", 0x9C7ED2, 0x965AD2), new Look("son_02_v3", 0x8C3A24, 0x3CA05A), new Look("son_02_v4", 0xE88CB0, 0x788CA0)}),
        Map.entry("son_03", new Look[] {new Look("son_03", 0x0F0F0F, 0x486E30), new Look("son_03_v1", 0xCED2DC, 0xD2A028), new Look("son_03_v2", 0xDEBC70, 0x4682DC), new Look("son_03_v3", 0x9C7ED2, 0x965AD2), new Look("son_03_v4", 0x8C3A24, 0x3CA05A)}),
        Map.entry("son_04", new Look[] {new Look("son_04", 0x28190F, 0x023A53), new Look("son_04_v1", 0xCED2DC, 0x788CA0), new Look("son_04_v2", 0xDEBC70, 0x28AAB4), new Look("son_04_v3", 0x9C7ED2, 0xD2A028), new Look("son_04_v4", 0x8C3A24, 0x4682DC)}),
        Map.entry("son_05", new Look[] {new Look("son_05", 0x1B1818, 0x515158), new Look("son_05_v1", 0xCED2DC, 0x965AD2), new Look("son_05_v2", 0xDEBC70, 0x3CA05A), new Look("son_05_v3", 0x9C7ED2, 0x788CA0), new Look("son_05_v4", 0x8C3A24, 0x28AAB4)}),
        Map.entry("son_06", new Look[] {new Look("son_06", 0x1E1E1E, 0xB9B928), new Look("son_06_v1", 0xCED2DC, 0x4682DC), new Look("son_06_v2", 0xDEBC70, 0x785032), new Look("son_06_v3", 0x9C7ED2, 0x965AD2), new Look("son_06_v4", 0x8C3A24, 0x3CA05A)}),
        Map.entry("son_07", new Look[] {new Look("son_07", 0x544040, 0x1B649C), new Look("son_07_v1", 0x5070CE, 0x28AAB4), new Look("son_07_v2", 0xCED2DC, 0xD2A028), new Look("son_07_v3", 0x1E1A1E, 0x4682DC), new Look("son_07_v4", 0xDEBC70, 0x785032)}),
        Map.entry("son_08", new Look[] {new Look("son_08", 0x28190F, 0x023A53), new Look("son_08_v1", 0x5070CE, 0x3CA05A), new Look("son_08_v2", 0x6E482E, 0x788CA0), new Look("son_08_v3", 0xCED2DC, 0x28AAB4), new Look("son_08_v4", 0xDEBC70, 0xD2A028)}),
        Map.entry("son_09", new Look[] {new Look("son_09", 0x1B1818, 0x515158), new Look("son_09_v1", 0x5070CE, 0x965AD2), new Look("son_09_v2", 0x6E482E, 0x3CA05A), new Look("son_09_v3", 0xCED2DC, 0x788CA0), new Look("son_09_v4", 0xDEBC70, 0x28AAB4)}),
        Map.entry("son_10", new Look[] {new Look("son_10", 0x4F4645, 0x2E4B6E), new Look("son_10_v1", 0x5AA48A, 0xD2A028), new Look("son_10_v2", 0xC86C32, 0x4682DC), new Look("son_10_v3", 0x5070CE, 0x785032), new Look("son_10_v4", 0xCED2DC, 0x965AD2)}),
        Map.entry("son_11", new Look[] {new Look("son_11", 0xC9B6A5, 0x9A8473), new Look("son_11_v1", 0x5AA48A, 0x28AAB4), new Look("son_11_v2", 0xC86C32, 0xD2A028), new Look("son_11_v3", 0x5070CE, 0x4682DC), new Look("son_11_v4", 0x6E482E, 0x785032)}),
        Map.entry("son_12", new Look[] {new Look("son_12", 0x5E3A40, 0x837A92), new Look("son_12_v1", 0x5AA48A, 0x965AD2), new Look("son_12_v2", 0xC86C32, 0x3CA05A), new Look("son_12_v3", 0x5070CE, 0x28AAB4), new Look("son_12_v4", 0xCED2DC, 0xD2A028)}),
        Map.entry("son_13", new Look[] {new Look("son_13", 0x49321B, 0x30CD00), new Look("son_13_v1", 0x5AA48A, 0x4682DC), new Look("son_13_v2", 0xC86C32, 0x785032), new Look("son_13_v3", 0x5070CE, 0x965AD2), new Look("son_13_v4", 0xCED2DC, 0x3CA05A)}),
        Map.entry("son_14", new Look[] {new Look("son_14", 0xB49C96, 0x000000)}),
        Map.entry("son_15", new Look[] {new Look("son_15", 0x141111, 0x515158), new Look("son_15_v1", 0xECE4CE, 0x3CA05A), new Look("son_15_v2", 0x5AA48A, 0x788CA0), new Look("son_15_v3", 0xC86C32, 0x28AAB4), new Look("son_15_v4", 0x5070CE, 0xD2A028)}),
        Map.entry("son_16", new Look[] {new Look("son_16", 0x1F1611, 0x27311F), new Look("son_16_v1", 0xECE4CE, 0x785032), new Look("son_16_v2", 0x5AA48A, 0x965AD2), new Look("son_16_v3", 0xC86C32, 0x3CA05A), new Look("son_16_v4", 0x5070CE, 0x788CA0)}));

    /** Every look of skin {@code key} (its own first); just its own if it has no versions. */
    public static Look[] of(String key) {
        Look[] looks = LOOKS.get(key);
        return looks != null ? looks : new Look[] {new Look(key, 0, 0)};
    }

    /** The look of skin {@code key} nearest hair {@code hair} and eyes {@code eyes} (RGB): hair counts twice as much as eyes. */
    public static Look pick(String key, int hair, int eyes) {
        Look best = null;
        double bestScore = Double.MAX_VALUE;
        for (Look look : of(key)) {
            double score = 2.0 * ChildSkins.distance(look.hair(), hair) + (look.eyes() == 0 ? 0.0 : ChildSkins.distance(look.eyes(), eyes));
            if (score < bestScore) {
                bestScore = score;
                best = look;
            }
        }
        return best;
    }
}
