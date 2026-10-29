package com.hex.kagune.control.quest;

/**
 * 7.0.1: what the server tells the quest log happened (pure). Each {@link QuestEvent} is one of these, an amount and tags that
 * say how and where ({@link Tags}); an {@link Objective} counts the ones it matches.
 */
public enum Signal {
    /** A kill of the player's (1 each): what died ({@link Tags#mob}, hostile, ghoul, boss) and how the player was. */
    KILL,
    /** The player died (1 each): what killed them and how they were. */
    DIE,
    /** Something eaten (1 each): {@link Tags#item}, {@code flesh} (ghoul food), {@code ghoul_meat}. */
    EAT,
    /** Brought below a fifth of their health, and lived through it (1 each). */
    BRINK,
    /** Damage dealt (the amount, rounded down). */
    DEAL,
    /** Damage taken and survived (the amount, rounded down); {@code ranged} from a projectile. */
    TAKE,
    /** Kills in a row, each within {@link QuestLog#RAMPAGE_TICKS} of the last: the streak's length (an objective keeps its best). */
    RAMPAGE,
    /** A second spent with the kagune out ({@code kakuja} in a kakuja), or (eyes) with only the eyes open. */
    TIME,
    /** Kaneki: the kagune's urge restrained (crouching through it). */
    RESTRAIN,
    /** The shared eye-mode fight: a blitz strike landed (1 for each body hit); {@code finisher} for the twin palm. */
    STRIKE,
    /** The Big Punch landed; {@code full} at the full charge. */
    PUNCH,
    /** The Speed Kick landed. */
    KICK,
    /** A flip. */
    FLIP,
    /** Mastery: the player's mastery now (an objective keeps its best). */
    MASTERY,
    /** 7.0.1 (NPCs): a word with a ghoul ({@code friend}, {@code ally}), a favour done, a ghoul turned on. */
    TALK,
    BEFRIEND,
    FAVOUR,
    BETRAY
}
