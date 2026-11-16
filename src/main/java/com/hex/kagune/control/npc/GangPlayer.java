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
public record GangPlayer(int gang, int ownKills, int banned, int aogiriStep, int clownStep, int gasStep, int anteikuStep, int generalStep) {
    public static final int NONE = 0;
    public static final int AOGIRI = 1;
    public static final int CLOWNS = 2;
    /** 4.1.54: the Gas Masks. */
    public static final int GAS_MASKS = 3;
    /** Killing more than this many of their own gets them banned (the owner: "over 10"). */
    public static final int BAN_OVER = 10;
    /**
     * 4.1.79 (the plan the owner approved: "the gangs' joining. Each chain 3 to 4 steps, each a different kind, a trial of its own, a scene
     * with the leader at the end"): Aogiri's way in, through Ayato: a spar with him ({@link #AOGIRI_SPAR}: a duel that ends at low health,
     * never a death), an ambush on a CCG patrol ({@link #AOGIRI_AMBUSH}), then back to him ({@link #AOGIRI_DONE}). Kept as its own number;
     * a player from before (4.1.50's "Kill 5 CCG" begun) is read as {@link #aogiriFrom}.
     */
    public static final int AOGIRI_NOT_YET = 0;
    public static final int AOGIRI_SPAR = 1;
    public static final int AOGIRI_AMBUSH = 2;
    public static final int AOGIRI_DONE = 3;
    /**
     * The Clowns' way in, through Roma: a joke that makes her laugh (4.1.50's, kept), then 4.1.79: a show in her ring ({@link #CLOWN_RING}),
     * a job done in a clown's face ({@link #CLOWN_JOB}: an investigator killed in one of the Clowns' masks), then the case off him brought
     * ({@link #CLOWN_CASE}). The saved numbers keep their places: 1 was "three Gas Masks" (now the ring), 2 "the quinque case" (now the job).
     */
    public static final int CLOWN_NOT_YET = 0;
    public static final int CLOWN_RING = 1;
    public static final int CLOWN_JOB = 2;
    public static final int CLOWN_CASE = 3;
    /**
     * 4.1.54: the Gas Masks' way in, through Banjou: something said that takes him seriously (kept), then 4.1.79: a walk back to him through
     * their gas ({@link #GAS_WALK}), the gate held with him against a Clown raid ({@link #GAS_RAID}), then back to him ({@link #GAS_DONE}).
     * The saved 1 was "three Clowns" (now the walk), 2 "a clown's mask" (now the raid).
     */
    public static final int GAS_NOT_YET = 0;
    public static final int GAS_WALK = 1;
    public static final int GAS_RAID = 2;
    public static final int GAS_DONE = 3;
    /**
     * 4.1.70 (the owner: "Anteikou - Joinable through Ken (quest)"): Anteiku, the café, through Ken: the answer that is Anteiku's (kept), then
     * 4.1.79: a shift at the café ({@link #ANTEIKU_SHIFT}: brew and serve), a CCG raid driven off with nobody killed ({@link #ANTEIKU_RAID}),
     * then back to him ({@link #ANTEIKU_DONE}). The saved 1 was "the coffee beans" (now the shift), 2 "three investigators" (now the raid).
     */
    public static final int ANTEIKU = 4;
    public static final int ANTEIKU_NOT_YET = 0;
    public static final int ANTEIKU_SHIFT = 1;
    public static final int ANTEIKU_RAID = 2;
    public static final int ANTEIKU_DONE = 3;
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
     * (the bureau's own answer, kept), then 4.1.79: the academy's test ({@link #CCG_EXAM}), a ghoul hunted with an assigned partner
     * ({@link #CCG_HUNT}), then he swears them in ({@link #CCG_DONE}). The saved 1 was "five ghouls" (now the test); 2 stays done, so the
     * hunt, new, is 3. Its ghoul division's ghouls are the CCG's ({@link #of}); its name cyan blue ({@link #CCG_CYAN}), a quinque case before it.
     */
    public static final int CCG = 5;
    public static final int CCG_NOT_YET = 0;
    public static final int CCG_EXAM = 1;
    public static final int CCG_DONE = 2;
    public static final int CCG_HUNT = 3;
    /** 4.1.71 (the owner: "Damage increased when fighting ghouls."): how much harder a member's blows land on a ghoul. */
    public static final float CCG_DAMAGE = 1.3F;
    /** 4.1.71 (the owner: "Cyan Blue"): the CCG's name colour (0xRRGGBB; plain text's nearest is aqua). */
    public static final int CCG_CYAN = 0x38B6F0;
    /** 4.1.71: the CCG's emblem, a quinque case (the owner's "brief cafe symbol": a briefcase). */
    public static final String BRIEFCASE = "\uE004";
    /** 4.1.70: Anteiku's coffee cup. */
    public static final String COFFEE = "\uE003";

    public static final GangPlayer NEW = new GangPlayer(NONE, 0, 0, AOGIRI_NOT_YET, CLOWN_NOT_YET, GAS_NOT_YET, ANTEIKU_NOT_YET, CCG_NOT_YET);

    /**
     * 4.1.79: Aogiri's step for a player saved before it had one: 4.1.50's quest ("Kill 5 CCG") done, back to Ayato; begun, the ambush (the
     * doves they killed for it counted as the spar's proof); never begun, nothing yet.
     */
    public static int aogiriFrom(boolean quest, int ccgKills) {
        return !quest ? AOGIRI_NOT_YET : ccgKills >= 5 ? AOGIRI_DONE : AOGIRI_AMBUSH;
    }

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

    /** 4.1.79: where {@code gang}'s way in stands for them (its own step numbers), or 0. */
    public int step(int gang) {
        return switch (gang) {
            case AOGIRI -> this.aogiriStep;
            case CLOWNS -> this.clownStep;
            case GAS_MASKS -> this.gasStep;
            case ANTEIKU -> this.anteikuStep;
            case CCG -> this.generalStep;
            default -> 0;
        };
    }

    /**
     * 4.1.79: {@code gang}'s way in moved on from {@code from} to {@code to}; unchanged if it is not at {@code from} (a trial won twice, or
     * one from another step, moves nothing), or if they already belong to it.
     */
    public GangPlayer advance(int gang, int from, int to) {
        if (this.member(gang) || this.step(gang) != from) {
            return this;
        }
        return this.with(gang, to);
    }

    private GangPlayer with(int gang, int step) {
        return new GangPlayer(this.gang, this.ownKills, this.banned, gang == AOGIRI ? step : this.aogiriStep, gang == CLOWNS ? step : this.clownStep,
            gang == GAS_MASKS ? step : this.gasStep, gang == ANTEIKU ? step : this.anteikuStep, gang == CCG ? step : this.generalStep);
    }

    /** They join {@code gang}: its member, their count of their own killed begun again, its way in done with. */
    public GangPlayer join(int gang) {
        GangPlayer in = new GangPlayer(gang, 0, this.banned, this.aogiriStep, this.clownStep, this.gasStep, this.anteikuStep, this.generalStep);
        return in.with(gang, 0);
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
            return new GangPlayer(NONE, 0, this.banned | bit(this.gang), AOGIRI_NOT_YET, CLOWN_NOT_YET, GAS_NOT_YET, ANTEIKU_NOT_YET, CCG_NOT_YET);
        }
        return new GangPlayer(this.gang, kills, this.banned, this.aogiriStep, this.clownStep, this.gasStep, this.anteikuStep, this.generalStep);
    }

    /**
     * 4.1.67 (the owner: "Add a new Quit the gang button to each leader whenever the player joins it."; "kicked from the gang if the player
     * marries another gang leader"): out of their gang, of their own accord or by a marriage to another gang's leader: no gang, their tag
     * gone, their count of their own killed forgotten; never banned for it (its way in begins again from the start).
     */
    public GangPlayer quit() {
        return this.gang == NONE ? this : new GangPlayer(NONE, 0, this.banned, AOGIRI_NOT_YET, CLOWN_NOT_YET, GAS_NOT_YET, ANTEIKU_NOT_YET,
            CCG_NOT_YET);
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
