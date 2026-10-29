package com.hex.kagune.control.npc;

/**
 * 7.0.2: what ghoul NPCs are called (pure): a given name, and what they are ("Ren the Drifter", "Mio of Anteiku"). 1.0.41: a boy's
 * name for a boy, a girl's for a girl ({@link NpcLook#girl}).
 */
public final class NpcNames {
    private static final String[] BOYS = {"Akio", "Asahi", "Daichi", "Eiji", "Fumio", "Haru", "Hikaru", "Hiro", "Itsuki", "Jun", "Kaito", "Kazu",
        "Kei", "Kenta", "Kiyoshi", "Kyo", "Makoto", "Masa", "Minato", "Raku", "Ren", "Riku", "Ryo", "Shin", "Sora", "Sou", "Tadashi", "Taiga",
        "Tsubasa", "Yuma"};
    private static final String[] GIRLS = {"Aoi", "Chiaki", "Emi", "Hotaru", "Ichika", "Izumi", "Kaede", "Kana", "Mai", "Mei", "Michi", "Mio", "Nagi",
        "Nao", "Natsu", "Noa", "Rin", "Saki", "Tomo", "Yoru", "Yuki", "Yuu", "Hina", "Sakura", "Rei", "Ami", "Akane", "Haruka", "Kotone", "Yui"};

    private NpcNames() {
    }

    /** How many names each kind has (a saved name's number is taken round it). */
    public static int count() {
        return Math.min(BOYS.length, GIRLS.length);
    }

    /** Given name {@code index}: a girl's or a boy's. */
    public static String given(int index, boolean girl) {
        String[] names = girl ? GIRLS : BOYS;
        return names[Math.floorMod(index, count())];
    }

    /** The name it goes by: its given name and what it is. */
    public static String name(int index, NpcTemper temper, boolean girl) {
        String given = given(index, girl);
        return switch (temper) {
            case ANTEIKU -> given + " of Anteiku";
            case PACK -> given + " of the Pack";
            default -> given + " the " + temper.title();
        };
    }
}
