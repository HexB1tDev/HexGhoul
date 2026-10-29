package com.hex.kagune.control.npc;

/**
 * 7.0.2: how a ghoul NPC stands with a player (pure, tested): a regard from {@link #LEAST} to {@link #MOST}, kept per player and per
 * NPC in the player's quest log. Talking, gifts and favours raise it; insults and blows lower it. It decides how the NPC takes to
 * them over its temper: at {@link #ENEMY} or below it goes for them on sight, from {@link #FRIENDLY} it will not fight them first,
 * from {@link #FRIEND} it can be asked to walk with them (an ally).
 */
public final class NpcBonds {
    public static final int LEAST = -100;
    public static final int MOST = 100;
    public static final int ENEMY = -40;
    public static final int FRIENDLY = 20;
    public static final int FRIEND = 60;

    /** What each thing does to the regard. */
    public static final int WHO = 3;
    public static final int CHAT = 4;
    public static final int GIFT = 12;
    public static final int GIFT_GOURMET = 20;
    public static final int ACCEPT = 2;
    public static final int FAVOUR_DONE = 25;
    public static final int INSULT = -25;
    public static final int BLOW = -30;
    /** Turned on: a player who hurts a ghoul that trusted them (a friend) drops to this at once. */
    public static final int BETRAYED = -60;

    /** Allies a player can have at once, favours on at once, bonds remembered (the faintest forgotten first). */
    public static final int MOST_ALLIES = 2;
    public static final int MOST_FAVOURS = 3;
    public static final int REMEMBERED = 48;

    /** Flags kept with a bond. */
    public static final int MET = 1;
    public static final int BEFRIENDED = 2;
    public static final int ASKED = 4;
    public static final int ALLY = 8;

    /** How the bond reads, for the talk and the screen. */
    public enum Band {
        ENEMY("Enemy"), COLD("Cold"), STRANGER("Stranger"), FRIENDLY("Friendly"), FRIEND("Friend");

        private final String label;

        Band(String label) {
            this.label = label;
        }

        public String label() {
            return this.label;
        }
    }

    private NpcBonds() {
    }

    public static int clamp(int regard) {
        return Math.max(LEAST, Math.min(MOST, regard));
    }

    public static Band band(int regard) {
        return regard <= ENEMY ? Band.ENEMY : regard < 0 ? Band.COLD : regard < FRIENDLY ? Band.STRANGER : regard < FRIEND ? Band.FRIENDLY : Band.FRIEND;
    }

    /** How {@code temper} takes to a player, its regard for them {@code regard} over what its temper says ({@code base}). */
    public static NpcTemper.Attitude attitude(NpcTemper.Attitude base, int regard) {
        if (regard <= ENEMY) {
            return NpcTemper.Attitude.HOSTILE;
        }
        if (regard >= FRIENDLY && base != NpcTemper.Attitude.HOSTILE) {
            return NpcTemper.Attitude.FRIENDLY;
        }
        return base;
    }

    /**
     * A blow from a player: what the regard becomes. A friend forgives one stray blow (it only cools); the blow that turns a
     * ghoul who trusted them (from {@link #FRIENDLY}) against them is a betrayal: down to {@link #BETRAYED} at once.
     */
    public static int struck(int regard) {
        int after = clamp(regard + BLOW);
        return betrayal(regard) ? BETRAYED : after;
    }

    /** True if a blow at {@code before} turns a ghoul that trusted the player against them. */
    public static boolean betrayal(int before) {
        return before >= FRIENDLY && before + BLOW < 0;
    }

    /** True if {@code temper} would walk with a player on {@code side} at all. */
    public static boolean joins(NpcTemper temper, String side) {
        return switch (temper) {
            case DRIFTER, SCAVENGER -> true;
            case ANTEIKU -> !"pack".equals(side);
            case PACK -> "pack".equals(side);
            default -> false;
        };
    }
}
