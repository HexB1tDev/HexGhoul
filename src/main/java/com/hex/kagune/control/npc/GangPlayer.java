package com.hex.kagune.control.npc;

/**
 * 4.1.50 (the owner: "Players are now able to join gangs as well, dialog with Ayato and with a quest you can join Aogiri tree, the
 * player's name turns dark red with a symbol on the left (symbol) (space) (name)"; the quest "Kill 5 CCG"; the Clowns through Roma, "A
 * short chain": make her laugh, kill 3 Gas Masks, bring her an investigator's quinque case; the Clowns' name blue with a clown face; and
 * for every gang: "the gang will never attack the player again, even when hit, they will however help the player and defend him against
 * hostiles. IF the player kills over 10 members of his own gang, he will be banned from joining, tag removed and will be attacked on
 * sight."): a player's gang (pure, tested). Which one they belong to ({@link #NONE}, {@link #AOGIRI}, {@link #CLOWNS}), how many of their
 * own they have killed since joining, the gangs that have banned them, and where they stand on each way in.
 *
 * <p>4.1.54 (the owner: "New gang: / Gas Masks - Toxic logo, green name. / Leader - Banjou"; joined, asked, by "Short chain, like Roma"):
 * {@link #GAS_MASKS}, through Banjou: say something to him that takes him seriously ({@link #takenSeriously}), kill {@link #GAS_CLOWNS}
 * Clowns ({@link #killedClown}), bring him a clown's mask; its emblem the toxic trefoil, its name green.
 */
public record GangPlayer(int gang, int ownKills, int banned, boolean aogiriQuest, int ccgKills, int clownStep, int gasKills, int gasStep,
                         int clownKills, int anteikuStep, int doveKills, int generalStep, int ghoulKills) {
    public static final int NONE = 0;
    public static final int AOGIRI = 1;
    public static final int CLOWNS = 2;
    /** 4.1.54: the Gas Masks. */
    public static final int GAS_MASKS = 3;
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
    /** 4.1.54: the Gas Masks' chain: nothing yet; Banjou taken seriously (the Clowns next); the Clowns done (a clown mask next). */
    public static final int GAS_NOT_YET = 0;
    public static final int GAS_CLOWNS = 3;
    public static final int GAS_CLOWNS_NEXT = 1;
    public static final int GAS_MASK_NEXT = 2;
    /**
     * 4.1.70 (the owner: "Anteikou - Joinable through Ken (quest)"): Anteiku, the café, through Ken. His way in, a short chain as the others':
     * the answer that is Anteiku's (why they want in: {@link #answered}), a handful of coffee beans brought ({@link #ANTEIKU_BEAN_COUNT}
     * cocoa beans in the hand: {@link #beansBrought}), then {@link #ANTEIKU_CCG} investigators who came sniffing round the café killed
     * ({@link #droveDove}); then he takes them in.
     */
    public static final int ANTEIKU = 4;
    public static final int ANTEIKU_NOT_YET = 0;
    public static final int ANTEIKU_BEANS = 1;
    public static final int ANTEIKU_DOVES = 2;
    public static final int ANTEIKU_DONE = 3;
    public static final int ANTEIKU_CCG = 3;
    public static final int ANTEIKU_BEAN_COUNT = 8;
    /**
     * 4.1.70 (the owner: "Enegy bar increases size and increases maximum capacity."): what one of Anteiku adds to how many times the energy
     * their bar holds (1 to 2; Potential Unlock's 3 to 4), and how much longer the bar is drawn ({@code ReserveBarView.ANTEIKU_TAIL}).
     */
    public static final int ANTEIKU_CAPACITY = 1;
    /** 4.1.70 (the owner: "Light brown name tag with a coffee symbol"): Anteiku's light brown, a latte's (0xRRGGBB). */
    public static final int ANTEIKU_BROWN = 0xC89B6D;
    /** The emblems (assets/minecraft/font/default.json's private characters: tools/gang_emblems.py). */
    public static final String TREE = "";
    public static final String CLOWN = "";
    /** 4.1.54: the Gas Masks' toxic trefoil. */
    public static final String TOXIC = "";
    /**
     * 4.1.71 (the owner: "CCG Faction: / Join through the general (quest) and now daily quests as well. / Traits: / CCG no longer attacks the
     * player. / All ghouls but official NPCS are hostile towards the player. / Damage increased when fighting ghouls. / Looks:  Cyan Blue with
     * a brief cafe symbol."; the symbol, their answer: "A briefcase (Recommended)"): the CCG, through the General. Why they want to serve
     * ({@link #sworn}: the bureau's own answer), then {@link #CCG_GHOUL_COUNT} ghouls killed ({@link #killedGhoul}); then he swears them in.
     * Its ghoul division's ghouls are the CCG's ({@link #of}); its name cyan blue ({@link #CCG_CYAN}), a quinque case before it.
     */
    public static final int CCG = 5;
    public static final int CCG_NOT_YET = 0;
    public static final int CCG_GHOULS = 1;
    public static final int CCG_DONE = 2;
    public static final int CCG_GHOUL_COUNT = 5;
    /** 4.1.71 (the owner: "Damage increased when fighting ghouls."): how much harder a member's blows land on a ghoul. */
    public static final float CCG_DAMAGE = 1.3F;
    /** 4.1.71 (the owner: "Cyan Blue"): the CCG's name colour (0xRRGGBB; plain text's nearest is aqua). */
    public static final int CCG_CYAN = 0x38B6F0;
    /** 4.1.71: the CCG's emblem, a quinque case (the owner's "brief cafe symbol": a briefcase). */
    public static final String BRIEFCASE = "\uE004";
    /** 4.1.70: Anteiku's coffee cup. */
    public static final String COFFEE = "\uE003";

    public static final GangPlayer NEW = new GangPlayer(NONE, 0, 0, false, 0, CLOWN_NOT_YET, 0, GAS_NOT_YET, 0, ANTEIKU_NOT_YET, 0, CCG_NOT_YET, 0);

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
            gang == CLOWNS ? CLOWN_NOT_YET : this.clownStep, gang == CLOWNS ? 0 : this.gasKills, gang == GAS_MASKS ? GAS_NOT_YET : this.gasStep,
            gang == GAS_MASKS ? 0 : this.clownKills, gang == ANTEIKU ? ANTEIKU_NOT_YET : this.anteikuStep, gang == ANTEIKU ? 0 : this.doveKills,
            gang == CCG ? CCG_NOT_YET : this.generalStep, gang == CCG ? 0 : this.ghoulKills);
    }

    /** Aogiri's quest begun (Ayato's "Kill 5 CCG"). */
    public GangPlayer startAogiri() {
        return new GangPlayer(this.gang, this.ownKills, this.banned, true, 0, this.clownStep, this.gasKills, this.gasStep, this.clownKills,
            this.anteikuStep, this.doveKills, this.generalStep, this.ghoulKills);
    }

    /** True once Aogiri's quest is done (five of the CCG killed while it was on). */
    public boolean aogiriDone() {
        return this.aogiriQuest && this.ccgKills >= AOGIRI_CCG;
    }

    /** One of the CCG killed: counted while Aogiri's quest is on. */
    public GangPlayer killedDove() {
        return this.aogiriQuest && !this.member(AOGIRI) ? new GangPlayer(this.gang, this.ownKills, this.banned, true, this.ccgKills + 1, this.clownStep,
            this.gasKills, this.gasStep, this.clownKills, this.anteikuStep, this.doveKills, this.generalStep, this.ghoulKills) : this;
    }

    /** Roma laughed: the Gas Masks next. */
    public GangPlayer laughed() {
        return this.clownStep == CLOWN_NOT_YET ? new GangPlayer(this.gang, this.ownKills, this.banned, this.aogiriQuest, this.ccgKills, CLOWN_GAS_MASKS, 0,
            this.gasStep, this.clownKills, this.anteikuStep, this.doveKills, this.generalStep, this.ghoulKills) : this;
    }

    /** One of the Gas Masks killed: counted while the Clowns' chain asks for them; the third moves it on to the case. */
    public GangPlayer killedGasMask() {
        if (this.clownStep != CLOWN_GAS_MASKS) {
            return this;
        }
        int kills = this.gasKills + 1;
        return new GangPlayer(this.gang, this.ownKills, this.banned, this.aogiriQuest, this.ccgKills, kills >= CLOWN_GAS ? CLOWN_CASE : CLOWN_GAS_MASKS,
            kills, this.gasStep, this.clownKills, this.anteikuStep, this.doveKills,
            this.generalStep, this.ghoulKills);
    }

    /** 4.1.54: Banjou taken seriously (the answer that does it): the Clowns next. */
    public GangPlayer takenSeriously() {
        return this.gasStep == GAS_NOT_YET ? new GangPlayer(this.gang, this.ownKills, this.banned, this.aogiriQuest, this.ccgKills, this.clownStep,
            this.gasKills, GAS_CLOWNS_NEXT, 0, this.anteikuStep, this.doveKills, this.generalStep, this.ghoulKills) : this;
    }

    /** 4.1.54: one of the Clowns killed: counted while the Gas Masks' chain asks for them; the third moves it on to the clown mask. */
    public GangPlayer killedClown() {
        if (this.gasStep != GAS_CLOWNS_NEXT) {
            return this;
        }
        int kills = this.clownKills + 1;
        return new GangPlayer(this.gang, this.ownKills, this.banned, this.aogiriQuest, this.ccgKills, this.clownStep, this.gasKills,
            kills >= GAS_CLOWNS ? GAS_MASK_NEXT : GAS_CLOWNS_NEXT, kills, this.anteikuStep, this.doveKills,
            this.generalStep, this.ghoulKills);
    }

    /** 4.1.70: Ken's question answered as Anteiku would (coffee beans next). */
    public GangPlayer answered() {
        return this.anteikuStep == ANTEIKU_NOT_YET ? this.anteiku(ANTEIKU_BEANS, 0) : this;
    }

    /** 4.1.70: the coffee beans brought (the investigators next). */
    public GangPlayer beansBrought() {
        return this.anteikuStep == ANTEIKU_BEANS ? this.anteiku(ANTEIKU_DOVES, 0) : this;
    }

    /** 4.1.70: one of the CCG killed: counted while Ken's chain asks for them; the third finishes it. */
    public GangPlayer droveDove() {
        if (this.anteikuStep != ANTEIKU_DOVES) {
            return this;
        }
        int kills = this.doveKills + 1;
        return this.anteiku(kills >= ANTEIKU_CCG ? ANTEIKU_DONE : ANTEIKU_DOVES, kills);
    }

    /** 4.1.71: the General's question answered as the bureau would (ghouls next). */
    public GangPlayer sworn() {
        return this.generalStep == CCG_NOT_YET ? this.general(CCG_GHOULS, 0) : this;
    }

    /** 4.1.71: a ghoul killed (not the CCG's own): counted while the General's chain asks for them; the fifth finishes it. */
    public GangPlayer killedGhoul() {
        if (this.generalStep != CCG_GHOULS) {
            return this;
        }
        int kills = this.ghoulKills + 1;
        return this.general(kills >= CCG_GHOUL_COUNT ? CCG_DONE : CCG_GHOULS, kills);
    }

    /** 4.1.71: true once the General's chain is done (he swears them in when they next talk to him). */
    public boolean ccgDone() {
        return this.generalStep == CCG_DONE;
    }

    private GangPlayer general(int step, int kills) {
        return new GangPlayer(this.gang, this.ownKills, this.banned, this.aogiriQuest, this.ccgKills, this.clownStep, this.gasKills, this.gasStep,
            this.clownKills, this.anteikuStep, this.doveKills, step, kills);
    }

    /** 4.1.70: true once Ken's chain is done (he takes them in when they next talk to him). */
    public boolean anteikuDone() {
        return this.anteikuStep == ANTEIKU_DONE;
    }

    private GangPlayer anteiku(int step, int kills) {
        return new GangPlayer(this.gang, this.ownKills, this.banned, this.aogiriQuest, this.ccgKills, this.clownStep, this.gasKills, this.gasStep,
            this.clownKills, step, kills, this.generalStep, this.ghoulKills);
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
            return new GangPlayer(NONE, 0, this.banned | bit(this.gang), false, 0, CLOWN_NOT_YET, 0, GAS_NOT_YET, 0, ANTEIKU_NOT_YET, 0,
                CCG_NOT_YET, 0);
        }
        return new GangPlayer(this.gang, kills, this.banned, this.aogiriQuest, this.ccgKills, this.clownStep, this.gasKills, this.gasStep,
            this.clownKills, this.anteikuStep, this.doveKills,
            this.generalStep, this.ghoulKills);
    }

    /**
     * 4.1.67 (the owner: "Add a new Quit the gang button to each leader whenever the player joins it."; "kicked from the gang if the player
     * marries another gang leader"): out of their gang, of their own accord or by a marriage to another gang's leader: no gang, their tag
     * gone, their count of their own killed forgotten; never banned for it (its way in begins again from the start).
     */
    public GangPlayer quit() {
        return this.gang == NONE ? this : new GangPlayer(NONE, 0, this.banned, false, 0, CLOWN_NOT_YET, 0, GAS_NOT_YET, 0, ANTEIKU_NOT_YET, 0,
            CCG_NOT_YET, 0);
    }

    /**
     * Their name as everyone sees it (chat, the tag over their head): the gang's emblem in its own colours, a space, the name in the gang's
     * colour (the owner: "(symbol) (space) (name)": Aogiri's dark red, the Clowns' blue; 4.1.54, the Gas Masks: "Toxic logo, green name");
     * a gangless name as it is.
     */
    public static String named(int gang, String name) {
        return switch (gang) {
            case AOGIRI -> "§f" + TREE + " §4" + name + "§r";
            case CLOWNS -> "§f" + CLOWN + " §9" + name + "§r";
            case GAS_MASKS -> "§f" + TOXIC + " §a" + name + "§r";
            // 4.1.70: light brown is none of the sixteen; GangServer.nameTag gives it (ANTEIKU_BROWN); here, plain text's nearest: gold.
            case ANTEIKU -> "§f" + COFFEE + " §6" + name + "§r";
            // 4.1.71: the CCG's cyan blue the same way (GangServer.nameTag, CCG_CYAN); here, plain text's nearest: aqua.
            case CCG -> "§f" + BRIEFCASE + " §b" + name + "§r";
            default -> name;
        };
    }

    /** The gang's name. */
    public static String title(int gang) {
        return switch (gang) {
            case AOGIRI -> "Aogiri Tree";
            case CLOWNS -> "the Clowns";
            case GAS_MASKS -> "the Gas Masks";
            case ANTEIKU -> "Anteiku";
            case CCG -> "the CCG";
            default -> "no gang";
        };
    }

    /**
     * The gang a ghoul NPC of {@code who} belongs to, as a player would join it (Aogiri's members, the Clowns, the Gas Masks; 4.1.71: the CCG's
     * ghoul division the CCG), or {@link #NONE}.
     */
    public static int of(NpcIdentity who) {
        if (who == null) {
            return NONE;
        }
        if (who.aogiri()) {
            return AOGIRI;
        }
        if (who.ccg()) {
            return CCG; // 4.1.71: the CCG's ghoul division
        }
        return who.gangOf() == Gang.CLOWNS ? CLOWNS : who.gangOf() == Gang.GAS_MASKS ? GAS_MASKS : NONE;
    }
}
