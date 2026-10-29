package com.hex.kagune.control.quest;

/**
 * 7.0.1: the words a {@link QuestEvent} is tagged with (pure). Context of the player: {@link #EYES} (only the eyes open),
 * {@link #KAGUNE} (the kagune out), {@link #KAKUJA}, {@link #NIGHT}, {@link #AIRBORNE}, {@link #LOW} (at a third of their
 * health or less), {@link #path}, the form ({@link #form}: a Kaneki form, an Ayato wing form, a Furuta stage). Of what was
 * hit or killed: {@link #mob}, {@link #HOSTILE}, {@link #GHOUL} (a ghoul NPC or a ghoul player), {@link #NPC}, {@link #PLAYER},
 * {@link #BOSS}, a ghoul NPC's {@link #temper}; of how: {@link #RANGED} (a projectile). Of what was eaten: {@link #item}, {@link #FLESH},
 * {@link #GHOUL_MEAT}. 7.0.2, with ghoul NPCs: {@link #FIRST} (a first word), {@link #ALLY} (one walks with the player, or is near).
 */
public final class Tags {
    public static final String EYES = "eyes";
    public static final String KAGUNE = "kagune";
    public static final String KAKUJA = "kakuja";
    public static final String NIGHT = "night";
    public static final String AIRBORNE = "airborne";
    public static final String LOW = "low";
    public static final String HOSTILE = "hostile";
    public static final String GHOUL = "ghoul";
    public static final String NPC = "npc";
    public static final String PLAYER = "player";
    public static final String BOSS = "boss";
    public static final String RANGED = "ranged";
    public static final String FLESH = "flesh";
    public static final String GHOUL_MEAT = "ghoul_meat";
    public static final String FULL = "full";
    public static final String FINISHER = "finisher";
    public static final String FRIEND = "friend";
    public static final String ALLY = "ally";

    private Tags() {
    }

    /** What was killed or hit, or what killed the player: {@code mob:minecraft:zombie}. */
    public static String mob(String id) {
        return "mob:" + id;
    }

    public static String item(String id) {
        return "item:" + id;
    }

    public static String path(String id) {
        return "path:" + id;
    }

    public static String form(String id) {
        return "form:" + id;
    }

    /** 7.0.2: a ghoul NPC's temper (its kill, a word with it, a favour done for it): {@code temper:feral}. */
    public static String temper(String id) {
        return "temper:" + id;
    }

    /** 7.0.2: the first word with a ghoul NPC. */
    public static final String FIRST = "first";
}
