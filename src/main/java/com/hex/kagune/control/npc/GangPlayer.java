package com.hex.kagune.control.npc;

/**
 * 4.1.50 (the owner: "Players are now able to join gangs as well, dialog with Ayato and with a quest you can join Aogiri tree, the
 * player's name turns dark red with a symbol on the left (symbol) (space) (name)"; the quest "Kill 5 CCG"; the Clowns through Roma, "A
 * short chain": make her laugh, kill 3 Gas Masks, bring her an investigator's quinque case; the Clowns' name blue with a clown face; and
 * for every gang: "the gang will never attack the player again, even when hit, they will however help the player and defend him against
 * hostiles. IF the player kills over 10 members of his own gang, he will be banned from joining, tag removed and will be attacked on
 * sight."): a player's gang (pure, tested). Which one they belong to ({@link #NONE}, {@link #AOGIRI}, {@link #CLOWNS}), how many of their
 * own they have killed since joining, the gangs that have banned them, and where they stand on each way in.
 */
public record GangPlayer(int gang, int ownKills, int banned, boolean aogiriQuest, int ccgKills, int clownStep, int gasKills) {
    public static final int NONE = 0;
    public static final int AOGIRI = 1;
    public static final int CLOWNS = 2;
    /** Aogiri's quest: this many of the CCG killed (the owner: "Kill 5 CCG"). */
    public static final int AOGIRI_CCG = 5;
    /** The Clowns' chain: this many Gas Masks killed. */
    public static final int CLOWN_GAS = 3;
    /** Killing more than this many of their own gets them banned (the owner: "over 10"). */
    public static final int BAN_OVER = 10;
    /** The Clowns' chain: nothing yet; she laughed (the Gas Masks next); the Gas Masks done (a quinque case next). */
    public static final int CLOWN_NOT_YET = 0;
    public static final int CLOWN_GAS_MASKS = 1;
    public static final int CLOWN_CASE = 2;
    /** The emblems (assets/minecraft/font/default.json's private characters: tools/gang_emblems.py). */
    public static final String TREE = "";
    public static final String CLOWN = "";

    public static final GangPlayer NEW = new GangPlayer(NONE, 0, 0, false, 0, CLOWN_NOT_YET, 0);

    /** True if they belong to {@code gang}. */
    public boolean member(int gang) {
        return gang != NONE && this.gang == gang;
    }

    /** True if {@code gang} has banned them. */
    public boolean banned(int gang) {
        return gang != NONE && (this.banned & bit(gang)) != 0;
    }

    static int bit(int gang) {
        return 1 << gang;
    }

    /** They join {@code gang}: its member, their count of their own killed begun again, its way in done with. */
    public GangPlayer join(int gang) {
        return new GangPlayer(gang, 0, this.banned, gang == AOGIRI ? false : this.aogiriQuest, gang == AOGIRI ? 0 : this.ccgKills,
            gang == CLOWNS ? CLOWN_NOT_YET : this.clownStep, gang == CLOWNS ? 0 : this.gasKills);
    }

    /** Aogiri's quest begun (Ayato's "Kill 5 CCG"). */
    public GangPlayer startAogiri() {
        return new GangPlayer(this.gang, this.ownKills, this.banned, true, 0, this.clownStep, this.gasKills);
    }

    /** True once Aogiri's quest is done (five of the CCG killed while it was on). */
    public boolean aogiriDone() {
        return this.aogiriQuest && this.ccgKills >= AOGIRI_CCG;
    }

    /** One of the CCG killed: counted while Aogiri's quest is on. */
    public GangPlayer killedDove() {
        return this.aogiriQuest && !this.member(AOGIRI) ? new GangPlayer(this.gang, this.ownKills, this.banned, true, this.ccgKills + 1, this.clownStep,
            this.gasKills) : this;
    }

    /** Roma laughed: the Gas Masks next. */
    public GangPlayer laughed() {
        return this.clownStep == CLOWN_NOT_YET ? new GangPlayer(this.gang, this.ownKills, this.banned, this.aogiriQuest, this.ccgKills, CLOWN_GAS_MASKS, 0)
            : this;
    }

    /** One of the Gas Masks killed: counted while the Clowns' chain asks for them; the third moves it on to the case. */
    public GangPlayer killedGasMask() {
        if (this.clownStep != CLOWN_GAS_MASKS) {
            return this;
        }
        int kills = this.gasKills + 1;
        return new GangPlayer(this.gang, this.ownKills, this.banned, this.aogiriQuest, this.ccgKills, kills >= CLOWN_GAS ? CLOWN_CASE : CLOWN_GAS_MASKS,
            kills);
    }

    /**
     * One of their own gang killed by them: counted; past {@link #BAN_OVER} they are banned from it, out of it, their tag gone (the gang
     * goes for them on sight from then on).
     */
    public GangPlayer killedOwn() {
        if (this.gang == NONE) {
            return this;
        }
        int kills = this.ownKills + 1;
        if (kills > BAN_OVER) {
            return new GangPlayer(NONE, 0, this.banned | bit(this.gang), false, 0, CLOWN_NOT_YET, 0);
        }
        return new GangPlayer(this.gang, kills, this.banned, this.aogiriQuest, this.ccgKills, this.clownStep, this.gasKills);
    }

    /**
     * Their name as everyone sees it (chat, the tag over their head): the gang's emblem in its own colours, a space, the name in the gang's
     * colour (the owner: "(symbol) (space) (name)": Aogiri's dark red, the Clowns' blue); a gangless name as it is.
     */
    public static String named(int gang, String name) {
        return switch (gang) {
            case AOGIRI -> "§f" + TREE + " §4" + name + "§r";
            case CLOWNS -> "§f" + CLOWN + " §9" + name + "§r";
            default -> name;
        };
    }

    /** The gang's name. */
    public static String title(int gang) {
        return switch (gang) {
            case AOGIRI -> "Aogiri Tree";
            case CLOWNS -> "the Clowns";
            default -> "no gang";
        };
    }

    /** The gang a ghoul NPC of {@code who} belongs to, as a player would join it (Aogiri's members, the Clowns), or {@link #NONE}. */
    public static int of(NpcIdentity who) {
        if (who == null) {
            return NONE;
        }
        if (who.aogiri()) {
            return AOGIRI;
        }
        return who.gangOf() == Gang.CLOWNS ? CLOWNS : NONE;
    }
}
