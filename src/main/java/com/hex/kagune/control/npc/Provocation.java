package com.hex.kagune.control.npc;

/**
 * 4.3.1 (pure, tested; the owner, after 4.3.0: "they get pissed at you for no fucking reason sometimes, and they just start hitting you
 * non-stop. I mean, they shouldn't even, official NPCs should not attack you, unless you fucking get rid of 50 heart, 50 HP or something ...
 * they can get pissed at you, sure. You not talk to you if they get pissed at you, but starting to kill you while you're while you told them
 * to follow you and all is ridiculous"): when an official NPC (Eto, Touka, Ken, Ayato, Noro, the General, Roma, Banjou) fights a player back.
 *
 * <p>Before, every blow cost {@link NpcBonds#BLOW} (-30) and started a fight at once, and at {@link NpcBonds#ENEMY} (-40) she went for them
 * on sight for good: two stray hits from a friend (a kagune sweep at the CCG beside her in her own "Let me watch you") and she hunted them,
 * again after every respawn while she still walked with them. Now:
 * <ul>
 * <li>a blow while the player is fighting something else is an accident ({@link #accident}): no regard lost, nothing counted;</li>
 * <li>a blow on purpose stings her regard at most once every {@link #STING_EVERY} ticks (a three-tail swing is one blow) and counts its
 * harm ({@link #harm}), forgotten {@link #FORGETS} ticks after the last;</li>
 * <li>only when that harm reaches {@link #FIGHTS_BACK} health (the owner's 50) does she fight back ({@link #fightsBack}); she stops
 * walking with them then, and once they are dead it is over (no grudge kept past their death);</li>
 * <li>a poor regard (enemy) or an insult ends in her not talking to them, never in a fight.</li>
 * </ul>
 */
public final class Provocation {
    private Provocation() {
    }

    /** The harm (health points, the owner's "50 HP") a player must do her on purpose before she fights back. */
    public static final float FIGHTS_BACK = 50.0F;
    /** The harm is forgotten this long after the last blow on purpose (ticks: two minutes). */
    public static final long FORGETS = 2400L;
    /** A blow on purpose costs regard at most once in this many ticks (one swing's several hits are one blow). */
    public static final long STING_EVERY = 20L;
    /** What one blow on purpose costs her regard (it was {@link NpcBonds#BLOW}, -30, every hit). */
    public static final int STING = -10;
    /** How long an insult that turned her keeps her from talking to them (ticks: five minutes). Never a fight. */
    public static final long SNUBS = 6000L;

    /** True if a blow on her is an accident: the player struck something else no longer than {@code fighting} ticks before {@code now}. */
    public static boolean accident(long struckElsewhere, long now, long fighting) {
        return struckElsewhere != Long.MIN_VALUE && now >= struckElsewhere && now - struckElsewhere <= fighting;
    }

    /**
     * The harm counted after a blow of {@code amount}: {@code before} so far, the last blow on purpose at {@code last} (none: Long.MIN_VALUE;
     * forgotten after {@link #FORGETS}); an {@code accident} adds nothing.
     */
    public static float harm(float before, long last, long now, float amount, boolean accident) {
        float kept = last == Long.MIN_VALUE || now < last || now - last >= FORGETS ? 0.0F : Math.max(0.0F, before);
        return accident ? kept : kept + Math.max(0.0F, amount);
    }

    /** True if {@code harm} done her on purpose is enough for her to fight back. */
    public static boolean fightsBack(float harm) {
        return harm >= FIGHTS_BACK;
    }

    /** True if a blow at {@code now} costs her regard: on purpose, and the last that did was {@link #STING_EVERY} ticks ago or more. */
    public static boolean stings(long lastSting, long now, boolean accident) {
        return !accident && (lastSting == Long.MIN_VALUE || now < lastSting || now - lastSting >= STING_EVERY);
    }
}
