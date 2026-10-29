package com.hex.kagune.control.npc;

/**
 * 7.0.2: a ghoul NPC's personality (pure, tested): who it goes for on sight, who it leaves be, who it never touches first, and
 * what it does when it is hurt. A player's standing with it comes from what the player is ({@code ghoul}: a ghoul or still human)
 * and the side they took in the quests ({@code side}: {@code "anteiku"}, {@code "pack"} or null).
 * <ul>
 *   <li>Feral: starving, past reason; goes for anyone it sees.</li>
 *   <li>Gourmet: eats its own kind; goes for ghouls (players and NPCs), leaves humans be.</li>
 *   <li>Pack: the violent side; goes for humans and for Anteiku's ghouls, sizes up the rest, never touches its own.</li>
 *   <li>Drifter: keeps to itself; only fights back.</li>
 *   <li>Scavenger: lives off what others leave; keeps its distance, fights back only when cornered, runs when it is losing.</li>
 *   <li>Anteiku: lives quietly among humans and protects them; never strikes first, except at a feral ghoul.</li>
 * </ul>
 */
public enum NpcTemper {
    FERAL("Feral", 24),
    GOURMET("Gourmet", 10),
    PACK("Pack", 16),
    DRIFTER("Drifter", 20),
    SCAVENGER("Scavenger", 14),
    ANTEIKU("Anteiku", 16);

    /** How it takes to someone: goes for them on sight, leaves them be unless they start it, or will not fight them first at all. */
    public enum Attitude {
        HOSTILE, WARY, FRIENDLY
    }

    private final String title;
    private final int weight;

    NpcTemper(String title, int weight) {
        this.title = title;
        this.weight = weight;
    }

    public String title() {
        return this.title;
    }

    /** How often it turns up (out of the weights' sum). */
    public int weight() {
        return this.weight;
    }

    /** How it takes to a player: {@code ghoul} if they are one, {@code side} the side they took ("anteiku", "pack", null). */
    public Attitude toward(boolean ghoul, String side) {
        boolean anteiku = "anteiku".equals(side);
        boolean pack = "pack".equals(side);
        return switch (this) {
            case FERAL -> Attitude.HOSTILE;
            case GOURMET -> ghoul ? Attitude.HOSTILE : Attitude.WARY;
            case PACK -> pack ? Attitude.FRIENDLY : !ghoul || anteiku ? Attitude.HOSTILE : Attitude.WARY;
            case DRIFTER, SCAVENGER -> Attitude.WARY;
            case ANTEIKU -> pack ? Attitude.WARY : Attitude.FRIENDLY;
        };
    }

    /** True if it goes for another ghoul NPC of temper {@code other} on sight. */
    public boolean hunts(NpcTemper other) {
        return switch (this) {
            case GOURMET -> other != GOURMET;
            case FERAL -> other != FERAL;
            case PACK -> other == ANTEIKU;
            case ANTEIKU -> other == FERAL;
            default -> false;
        };
    }

    /** True if it goes for any player it sees (it has someone to be hostile to). */
    public boolean hostile() {
        return this == FERAL || this == GOURMET || this == PACK;
    }

    /** True if it runs when it is losing (and keeps away from a ghoul with its kagune out). */
    public boolean coward() {
        return this == SCAVENGER;
    }

    /** How long it holds a grudge against whoever hurt it (ticks): the hostile ones never let go while it lives. */
    public int grudge() {
        return switch (this) {
            case FERAL, GOURMET, PACK -> Integer.MAX_VALUE;
            case DRIFTER -> 2400;
            case SCAVENGER -> 600;
            case ANTEIKU -> 1200;
        };
    }

    public static NpcTemper byOrdinal(int ordinal) {
        NpcTemper[] all = values();
        return ordinal >= 0 && ordinal < all.length ? all[ordinal] : DRIFTER;
    }

    /** The temper a roll of {@code r} (0 inclusive .. 1 exclusive) lands on, by weight. */
    public static NpcTemper pick(double r) {
        int total = 0;
        for (NpcTemper temper : values()) {
            total += temper.weight;
        }
        double at = Math.max(0.0, Math.min(0.999999, r)) * total;
        for (NpcTemper temper : values()) {
            at -= temper.weight;
            if (at < 0.0) {
                return temper;
            }
        }
        return DRIFTER;
    }
}
