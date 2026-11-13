package com.hex.kagune.control.npc;

import com.hex.kagune.control.quest.QuestLog;
import java.util.List;
import java.util.Set;

/**
 * 2.1.2 (pure, tested; the owner: "adapt the Ken, Eto and Touka quests for romance better, so that it's more immersive and less
 * repetitive, it's super boring at it stands"): each companion's own courtship, in place of the three errands they all shared (books,
 * ten kills at night, a flower). Five chapters each, every one asking for something only that one would ask. 2.1.5 (the owner: "the kill
 * doves quest shouldn't be monsters, considering we now have CCG agents ... we have humans, ccg, etc why are the quests so boring"): out
 * of the mod's own world: Eto wants to walk among the humans unseen, a human who cares for you, Doves killed while she watches, a human's
 * secret, the Library at three in the morning; Touka a cup of the cafe's blend you brewed, Doves cleared from the ward, somewhere high,
 * the ghoul stalking her human friend's village, the ranked investigator hunting the rabbit; Ken ghoul meat for the ghouls who won't kill,
 * a lonely wild ghoul made a friend, a Pack ghoul stopped, the Dove hunting him fought side by side, one sunrise not alone. Every one is
 * closed by its moment: a scene of its own in the companion's script
 * ({@code moment <chapter id>} in {@code data/hexkagune/talk/<key>.txt}) that plays as soon as the task is done. After the fifth, they
 * may ask the companion out.
 *
 * <p>Kept in the bond as the errands were: {@link EtoRules#LINE_ON} once begun, the chapters closed in three bits of its flags
 * ({@link #SHIFT}), the task's progress in its favour count's low byte ({@link EtoRules#count}). A bond from before (the errands' flags)
 * is read as so many chapters closed, and the three errands all done as all five ({@link #migrate}).
 */
public final class Courtship {
    private Courtship() {
    }

    /** Every companion's courtship has this many chapters. */
    public static final int CHAPTERS = 5;
    /** It may begin once they are this close (a friend: the courtship carries them the rest of the way). */
    public static final int BEGIN_AT = NpcBonds.FRIEND;
    /** The chapters closed, in the bond's flags: three bits from here. */
    public static final int SHIFT = 13;
    static final int BITS = 7 << SHIFT;
    /** The errands' flags (before 2.1.2), read once and cleared. */
    static final int LEGACY = EtoRules.BOOKS_DONE | EtoRules.HUNT_DONE | EtoRules.FLOWER_DONE;
    /** A walk or a sit is counted in steps of this many seconds. */
    public static final int STEP_SECONDS = 10;
    /** High enough (block height) for "somewhere high". */
    public static final int HIGH = 100;
    /** 2.1.5: how warm a village human must be to count as caring for them. */
    public static final int HUMAN_CLOSE = 60;
    /** 2.1.5: a bond's flag: its courtship is the 2.1.5 one (a count kept from before it meant something else, so it starts again). */
    public static final int FROM_215 = 1 << 16;
    /** Near enough (blocks) to be "with" them at dawn or in the night; to be sitting with them. */
    public static final double WITH = 10.0;
    public static final double SITTING = 6.0;
    /**
     * 4.1.80: a bond's flag: its courtship is 4.1.80's (a chapter on from before whose task changed starts again, or stays done:
     * {@link #migrate}).
     */
    public static final int FROM_480 = 1 << 17;
    /** 4.1.80: how near (blocks) the Library's heart a reading is; the tower's middle the roof; the companion fireworks are watched from. */
    public static final double LIBRARY_NEAR = 30.0;
    public static final double ROOF_REACH = 24.0;
    public static final double SHOW_NEAR = 24.0;
    /** 4.1.80: how near (blocks) their home a garden is planted; how near they must be to see it. */
    public static final double GARDEN_NEAR = 40.0;
    public static final double WATCHING = 16.0;
    /** 4.1.80: how many of {@link #HOMEWORK}'s questions are asked at a sitting (all right in a row). */
    public static final int ASKED = 3;

    /** What a chapter asks for. */
    public enum Task {
        /** Something handed over, {@code need} of it (held out on the talk screen: all of it at once). */
        GIVE,
        /** Monsters killed at night, anywhere. */
        KILLS,
        /** Monsters killed at night while the companion walks with them. */
        KILLS_WITH,
        /** Wild ghouls killed. */
        GHOUL,
        /** Walking with the companion following (steps of {@link #STEP_SECONDS}), after dark. */
        WALK_NIGHT,
        /** Sitting with them at their own place (near, not following them; steps of {@link #STEP_SECONDS}). */
        SIT,
        /** The companion following them up somewhere high ({@link #HIGH}). */
        HIGH,
        /** With them at dawn. */
        DAWN,
        /** With them at their own place in the dead of night. */
        DEEP,
        // 2.1.5 (the owner: "we have humans, ccg, etc. why are the quests so boring"): what the mod's own world holds.
        /** Doves (CCG investigators) killed, of at least the chapter's {@link Chapter#rank}. */
        DOVES,
        /** The same, with the companion walking with them. */
        DOVES_WITH,
        /** Wild ghouls of the Pack (the ones that kill for the joy of it) killed. */
        GHOUL_PACK,
        /** Wild ghouls killed where the humans of a village are (preying on them). */
        VILLAGE_GHOUL,
        /** A wild ghoul made their ally (it walks with them). */
        ALLY,
        /** A village human brought to care for them (its regard at {@link #HUMAN_CLOSE}). */
        HUMAN_FRIEND,
        /** A village human brought to tell them its secret. */
        HUMAN_SECRET,
        /** Walking through a human village after dark with the companion following (steps of {@link #STEP_SECONDS}). */
        VILLAGE_NIGHT,
        // 4.1.80 (the plan: "swap the repeated tasks so each companion's five differ"): each asked by one companion alone.
        /** A spar with the companion, won: them at {@link JoinTrial#SPAR_WIN} of their health before the player is at {@link JoinTrial#SPAR_FLOOR}. */
        SPAR,
        /** Reading with them at the Library at night, them following (steps of {@link #STEP_SECONDS}). */
        READING,
        /** Up on the CCG tower's roof with them following. */
        ROOF,
        /** Pack ghouls killed with a quinque in the hand. */
        QUINQUE,
        /** Walking through a human village in daylight with them following (steps of {@link #STEP_SECONDS}). */
        VILLAGE_DAY,
        /** Fireworks set off at night, near them, as they watch. */
        SHOW,
        /** Village humans' favours done. */
        ERRAND,
        /** Their homework: {@link #ASKED} of {@link #HOMEWORK}'s questions right in a row on the talk screen (one wrong: tomorrow). */
        QUIZ,
        /** Flowers or saplings planted in their garden as they watch. */
        GARDEN,
        /** A bowl of the cookbook's handed to each of their own people (a different one each time). */
        SERVE;

        /** True if counted in steps of time (shown in minutes). */
        public boolean timed() {
            return this == WALK_NIGHT || this == SIT || this == VILLAGE_NIGHT || this == READING || this == VILLAGE_DAY;
        }

        /** 4.1.80: true if done in one go (asked the same way until it is: a place reached, a time kept, a spar won, a test passed). */
        public boolean once() {
            return this == HIGH || this == DAWN || this == DEEP || this == SPAR || this == ROOF || this == QUIZ;
        }

        /** 4.1.80: true if begun from the talk screen ("Let's go."): the spar, the homework. */
        public boolean begun() {
            return this == SPAR || this == QUIZ;
        }
    }

    /**
     * A chapter: its id (its moment's scene in the script), its title, what it asks for and how much ({@code items} the item ids a
     * {@link Task#GIVE} takes, {@code itemName} what to call them on the talk screen), and its words: {@code ask} as it begins (and again if
     * asked before anything is done), {@code progress} while it is under way, {@code journal} the task as the journal says it. In the words,
     * {@code {n}} is how much is done, {@code {need}} how much is needed ({@code {m}}, {@code {mm}}: the same in minutes).
     */
    public record Chapter(String id, String title, Task task, int need, Set<String> items, String itemName, String ask, String progress,
                          String journal, int rank) {
        /** 2.1.5: as 2.1.2 made one: any Dove will do. */
        public Chapter(String id, String title, Task task, int need, Set<String> items, String itemName, String ask, String progress, String journal) {
            this(id, title, task, need, items, itemName, ask, progress, journal, 0);
        }
    }

    // ---------------------------------------------------------------- the chapters

    /** Any drink of her cookbook's (the blend wants human meat; the tonic and the shot ghoul meat). */
    static final Set<String> BLEND = Set.of("hexkagune:anteiku_blend", "hexkagune:blood_latte", "hexkagune:crimson_tea", "hexkagune:rc_tonic",
        "hexkagune:kakuja_shot");
    static final Set<String> GHOUL_MEAT = Set.of("hexkagune:ghoul_meat");
    /** 4.1.80: Banjou's boys' supper: any bowl of the cookbook's (its bowls chapter's, and the broth). */
    public static final Set<String> SUPPER = supper();

    private static Set<String> supper() {
        java.util.Set<String> out = new java.util.TreeSet<>();
        for (com.hex.kagune.control.kitchen.Dish dish : com.hex.kagune.control.kitchen.Menu.DISHES) {
            if (dish.kind() == com.hex.kagune.control.kitchen.Dish.Kind.BOWL) {
                out.add("hexkagune:" + dish.id());
            }
        }
        return java.util.Set.copyOf(out);
    }

    /** 4.1.80: what a garden takes: a sapling, a flower (any of Minecraft's, the tall ones too), an azalea. */
    private static final String[] FLOWERS = {"dandelion", "poppy", "blue_orchid", "allium", "azure_bluet", "_tulip", "oxeye_daisy", "cornflower",
        "lily_of_the_valley", "sunflower", "lilac", "rose_bush", "peony", "torchflower", "pitcher_plant", "pink_petals", "wither_rose"};

    /** 4.1.80: true if {@code id} (a block's or an item's, "minecraft:poppy") is something planted in a garden. */
    public static boolean plant(String id) {
        if (id == null || !id.startsWith("minecraft:")) {
            return false;
        }
        String name = id.substring(10);
        if (name.startsWith("potted_")) {
            return false; // a pot is set down, not planted
        }
        if (name.endsWith("_sapling") || name.equals("azalea") || name.equals("flowering_azalea") || name.equals("mangrove_propagule")) {
            return true;
        }
        for (String flower : FLOWERS) {
            if (flower.startsWith("_") ? name.endsWith(flower) : name.equals(flower)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 2.1.5: hers: walking among the humans unseen, a human who cares for you, Doves dying while she watches, a human's secret, then the
     * Library at three in the morning.
     */
    static final List<Chapter> ETO = List.of(
        new Chapter("eto_village_night", "A walk among them", Task.VILLAGE_NIGHT, 12, Set.of(), "",
            "Take me to the humans' village. At night. I want to walk down their street while they sleep, and nobody knows what's walking past "
                + "their windows. Ask me to follow you, and walk me through it.",
            "{m} of {mm} minutes among them. Slowly. I'm reading their windows.",
            "After dark, with Eto following you, walk through a human village: {m} of {mm} minutes"),
        new Chapter("eto_human_friend", "Make one of them love you", Task.HUMAN_FRIEND, 1, Set.of(), "",
            "Here's what I want. Go into that village and make one of them care about you. Really care. Talk, give them things, listen to their "
                + "boring little lives. I want to know if they can love something like us without knowing.",
            "Not yet? They're slow. Humans are always slow. Keep at it.",
            "Make a village human care about you (their regard " + HUMAN_CLOSE + ")"),
        new Chapter("eto_watch", "Let me watch you", Task.DOVES_WITH, 3, Set.of(), "",
            "I want to watch you kill Doves. Not because they deserve it. Because people show you who they are with blood on their hands. Take "
                + "me with you, and kill three of them while I watch.",
            "{n} of three. Keep going. I'm taking notes. In my head. Mostly.",
            "With Eto following you, kill CCG investigators while she watches: {n} of {need}"),
        new Chapter("eto_secret", "A human's secret", Task.HUMAN_SECRET, 1, Set.of(), "",
            "Humans are dull until you know their secrets. Then they're the best books there are. Go and get one of them to tell you a secret. "
                + "A real one. Then come back and don't tell me what it was. I'll guess.",
            "No secret yet? They only tell people they trust. Be trusted. It's easy. I hear.",
            "Get a village human to tell you a secret"),
        new Chapter("eto_deep", "The Library at three in the morning", Task.DEEP, 1, Set.of(), "",
            "Last thing. Come to the Library in the dead of night, when even the books are asleep. Stay with me there. I want to show you "
                + "something no one's ever seen.",
            "The dead of night. The Library. Me. You know where. Don't be late. I hate waiting almost as much as I hate being early.",
            "In the dead of night, be with Eto at the Library"));

    /**
     * 2.1.5: hers: a cup of the cafe's own blend you brewed yourself, Doves in the ward, somewhere high, the ghoul stalking her human friend's
     * village, and the investigator hunting the rabbit.
     */
    static final List<Chapter> TOUKA = List.of(
        new Chapter("touka_blend", "Brew it yourself", Task.GIVE, 1, BLEND, "the drink you made",
            "...Fine. Since you won't leave me alone. Make me something from my cookbook. A drink. The cafe's blend if you've got the stomach for "
                + "it, a tonic if you don't. Make it yourself, bring it here, and don't you dare buy it from someone.",
            "Still nothing? It's in the book. Drinks chapter. It's not hard. ...It's a little hard.",
            "Make Touka a drink from her cookbook (the drinks chapter: the Anteiku Blend, an RC Tonic...) and hand it to her"),
        // 4.1.80: was four Doves killed (Doves were three companions' twice over): her exams, which she studies for every night.
        new Chapter("touka_patrol", "Help me study", Task.QUIZ, 1, Set.of(), "",
            "I've got exams next week and nothing's sticking. ...Help me study. I'll read you my homework, you answer. Three right in a row and "
                + "you can explain them to me after. One wrong and that's it for today. ...Don't laugh.",
            "Again? Fine. Same rules. Three in a row. ...I've been practising. Have you?",
            "Help Touka with her homework: three of her questions right in a row (one wrong: try again tomorrow)"),
        new Chapter("touka_roof", "Somewhere high", Task.HIGH, 1, Set.of(), "",
            "Take me somewhere high. Higher than the rooftops. A mountain, a tower, I don't care. Somewhere the whole world's under us and "
                + "nobody's looking up. Ask me to follow you, and climb.",
            "Still on the ground. Higher. Way higher. Somewhere the clouds are close.",
            "With Touka following you, climb somewhere high (height " + HIGH + " or more)"),
        new Chapter("touka_friend", "My human friend", Task.VILLAGE_GHOUL, 1, Set.of(), "",
            "I've got a human friend. She doesn't know what I am. She walks home past the woods by a village like hers, and something's been "
                + "hunting there. One of us. Find the ghoul preying on those humans, and kill it.",
            "It's still out there. Near the humans. Find it.",
            "Kill a wild ghoul preying on a human village"),
        new Chapter("touka_rank", "Hunting rabbits", Task.DOVES, 1, Set.of(), "",
            "Last thing. The Doves sent someone serious. A ranked investigator, Rank 4 or worse, and he's hunting rabbits. Me. Kill him before he "
                + "finds the cafe. ...And come back. That part's not optional.",
            "He's still out there. Rank 4 or above. You'll know him.",
            "Kill a CCG investigator of Rank 4 or above", 4));

    /**
     * 2.1.5: his: meat for the ghouls who won't kill, a lonely ghoul made a friend, a Pack ghoul stopped, the Dove hunting him fought side by
     * side, then one sunrise not alone.
     */
    static final List<Chapter> KEN = List.of(
        new Chapter("ken_meat", "The ones who won't kill", Task.GIVE, 3, GHOUL_MEAT, "the ghoul meat",
            "There are ghouls who won't hunt people. They starve instead. I feed the ones I can. ...Bring me meat for them. Ghoul meat. Three. "
                + "I know what I'm asking. I'm sorry.",
            "{n} of three. Every one is someone who doesn't have to kill tonight.",
            "Bring Ken ghoul meat for the ghouls who won't kill: {n} of {need}"),
        new Chapter("ken_ally", "Nobody should be alone", Task.ALLY, 1, Set.of(), "",
            "Out there, there are ghouls who've never had anyone. I was one. Find one of them. Talk to them. Walk with them. Make one of them your "
                + "friend, so they're not alone anymore.",
            "Still alone, all of them? ...Try a gentle one. They're the loneliest.",
            "Make a wild ghoul your ally (it walks with you)"),
        new Chapter("ken_ghoul", "The ones who enjoy it", Task.GHOUL_PACK, 1, Set.of(), "",
            "There are ghouls who kill because they like it. The Pack. They eat people who can't fight back, and laugh. I can't be everywhere. "
                + "...Stop one. Please. And come back alive.",
            "Not yet? ...Be careful. A Pack ghoul. You'll know them by how they smile.",
            "Kill a wild ghoul of the Pack"),
        // 4.1.80: was a Rank 5 Dove killed with him along (Ayato asks the same): a night at the Library, reading, the one place he rests.
        new Chapter("ken_doves", "Nights at the Library", Task.READING, 12, Set.of(), "",
            "I don't sleep. You know that. When it's bad I go to the Library, the old one, and read until the words stop moving. ...Would you come "
                + "with me? At night. Just to read. A book each, nobody talking. Ask me to follow you there.",
            "{m} of {mm} minutes. ...Don't stop reading on my account. I like the sound of the pages.",
            "At night, with Ken following you, read with him at the Library: {m} of {mm} minutes"),
        new Chapter("ken_sunrise", "One sunrise", Task.DAWN, 1, Set.of(), "",
            "I don't sleep well. I haven't, for a long time. Stay up with me, once. Until the sun comes up. I'd like to see one sunrise with "
                + "someone, instead of alone.",
            "Dawn. Be with me when the sun comes up. That's all.",
            "Be with Ken when the sun comes up (at dawn)"));

    /**
     * 4.0.0 (the owner: his romance "should not suddenly become affectionate ... his dialogue and behavior should gradually change as the
     * relationship develops, revealing his more protective, jealous, teasing, and vulnerable sides"): his. Feathers he'll never admit he
     * collects; a night's fight at his side; the rooftops where the Doves can't see; the Dove hunting the black rabbit, fought together; and
     * the hideout in the dead of night, where he says the one thing he won't say twice.
     */
    static final List<Chapter> AYATO = List.of(
        new Chapter("ayato_meal", "Something that isn't garbage", Task.GIVE, 3, Set.of("hexkagune:ghouls_pie"), "the pies",
            "You want something to do? Feed me. Three ghoul's pies. Made right, not that slop they sell. ...Don't ask why. And don't tell my sister.",
            "{n} of three. You're slow.",
            "Bring Ayato ghoul's pies: {n} of {need}"),
        // 4.1.80: was three wild ghouls killed: a spar with him (the plan: "Ayato's a spar at the hideout"); then not just anywhere high
        // (three others asked that) but the Doves' own roof.
        new Chapter("ayato_ghouls", "Hit me like you mean it", Task.SPAR, 1, Set.of(), "",
            "You want something to do? Fight me. Here. Now. Not to the death, I'm not an idiot. Get me to half before I get you under a quarter. "
                + "...If you can. Say when.",
            "Again? Fine. Same rules. Half of me before a quarter of you.",
            "Spar with Ayato: get him to half his health before he gets you under a quarter of yours"),
        new Chapter("ayato_roof", "Where they can't see", Task.ROOF, 1, Set.of(), "",
            "...Take me up on the Doves' roof. Their tower. Hundreds of them in there, and not one of them ever looks up. I want to sit up there "
                + "with you. Shut up, that's not weird.",
            "Not there yet. Their roof. The very top. Where they can't see.",
            "Take Ayato up on the CCG tower's roof (him following you)"),
        new Chapter("ayato_rabbit", "Hunting the black rabbit", Task.DOVES_WITH, 1, Set.of(), "",
            "There's a Dove who's been hunting the black rabbit. Me. Rank 5 or worse. He'll come. When he does, you stand next to me. Not "
                + "behind. Next to.",
            "He hasn't come yet. Stay close. Rank 5 or above.",
            "With Ayato following you, kill a CCG investigator of Rank 5 or above", 5),
        new Chapter("ayato_deep", "The dead of night", Task.DEEP, 1, Set.of(), "",
            "Come to the hideout. Late. After everyone's asleep. ...I've got something to say and I'm only saying it once. Don't be late.",
            "Late. The hideout. When it's dead quiet. Don't make me wait.",
            "Be with Ayato at the hideout in the dead of night"));

    /**
     * 4.1.5 (the owner: "proud of the CCG, has to be convinced to love a ghoul"): the General's: the ghouls preying on a village put down; a villager who will vouch
     * for you; the Pack's killers stopped; his district seen from somewhere high, off the map on his wall; and the sun coming up with him
     * after the night shift. Each one is a reason to believe what he sees of you.
     */
    static final List<Chapter> GENERAL = List.of(
        new Chapter("general_village", "Where the people are", Task.VILLAGE_GHOUL, 3, Set.of(), "",
            "There are ghouls feeding on a village in my district, and my patrols keep arriving late. Three of them. Stop them where the "
                + "people are. Show me whose side you stand on when nobody's ordering you.",
            "{n} of three, at a village. The people there can't wait on my paperwork.",
            "Kill three wild ghouls preying on a village: {n} of {need}"),
        // 4.1.80: was a villager who cares for you (Eto asks that): villagers' favours done, the people's work his officers have no time
        // for; the Pack put down with one of the bureau's quinques (Ken and Roma ask for the Pack too); his district walked by day, not
        // seen from somewhere high.
        new Chapter("general_vouch", "Someone to vouch for you", Task.ERRAND, 2, Set.of(), "",
            "I've read your file. I'd rather hear it from people. There are villagers in my district with problems my officers never have time "
                + "for. Help two of them, whatever they ask. Then we'll see who writes to me.",
            "{n} of {need}. People talk. The good things travel slower, but they travel.",
            "Do favours for villagers (the ones they ask of you): {n} of {need}"),
        new Chapter("general_pack", "The ones who enjoy it", Task.QUINQUE, 3, Set.of(), "",
            "The Pack. The ones who kill for the joy of it. I've lost eleven investigators to them this year. Three. And do it with one of ours: "
                + "a quinque in your hand. I want them to see what's coming for them.",
            "{n} of three of the Pack, with a quinque. Be careful. That's not an order. ...It's an order.",
            "Kill three ghouls of the Pack with a quinque in your hand: {n} of {need}"),
        new Chapter("general_high", "The district, not the map", Task.VILLAGE_DAY, 12, Set.of(), "",
            "I've run this district for ten years from a map on a wall. Walk me through a village. In daylight, on foot, like a person. I want "
                + "to see it once the way it really is. ...With you. That part isn't in the report.",
            "{m} of {mm} minutes. Slower. I want to read the shop signs.",
            "In daylight, with General Kuroda following you, walk through a human village: {m} of {mm} minutes"),
        new Chapter("general_dawn", "The end of the night shift", Task.DAWN, 1, Set.of(), "",
            "Be here at dawn. When the night shift comes in and I count them. ...I've counted them alone for thirty years. I'd like, once, "
                + "not to.",
            "Dawn. Here. I'll be at the window, counting.",
            "Be with General Kuroda at dawn"));

    /**
     * 4.1.49 (the owner: Roma "has a messed up sense of humor but can be romatic when comes to it"): Roma's: a show for her (the CCG's
     * investigators put down, three, her idea of a joke), the night walked with her, her tent sat in after dark, the Pack's killers stopped
     * (the only thing she won't laugh at), and the sun coming up with her, the one time she drops the act.
     */
    static final List<Chapter> ROMA = List.of(
        // 4.1.80: was three Doves killed (the plan: "Roma's 'kill 3 investigators' a circus show for her"): fireworks over her ring.
        new Chapter("roma_show", "A show for the ringmaster", Task.SHOW, 8, Set.of(), "",
            "You want to date the ringmaster? Then put on a show! Fireworks. Eight of them, over my ring, at night, where I can see. ...What? "
                + "Everybody else brings flowers. Flowers don't EXPLODE.",
            "{n} of eight! The crowd's warming up! The crowd is me.",
            "At night, set off fireworks by Roma's ring while she watches: {n} of {need}"),
        // 4.1.80: its count was 3 steps (half a minute) said as "0.5 of 3 minutes": three minutes now, said as minutes.
        new Chapter("roma_walk", "Walk me past the windows", Task.VILLAGE_NIGHT, 18, Set.of(), "",
            "Walk me through a village. At night. The long way, past all the windows with the lights off. I like to guess which ones are "
                + "already dead. ...Kidding! Mostly. Walk with me anyway.",
            "{m} minutes of {mm}... keep walking, you're doing great, you haven't screamed once.",
            "Walk Roma through a human village after dark: {m} of {mm} minutes"),
        new Chapter("roma_tent", "After the lights go out", Task.DEEP, 1, Set.of(), "",
            "Come to the tent in the dead of night. No show. No masks. Just the two of us and the echo. ...It's very spooky. You'll love it.",
            "The dead of night. In the tent. Shh. Hear that? That's nothing. That's the point.",
            "Be with Roma in her tent in the dead of night"),
        new Chapter("roma_pack", "Not that kind of funny", Task.GHOUL_PACK, 3, Set.of(), "",
            "...There's a kind of ghoul who kills for fun. The Pack. And I know, I KNOW, I joke about death all day. But they're not "
                + "joking. They're just cruel. Three of them. Please. That one's not a bit.",
            "{n} of three. ...Thank you. I mean it. Don't tell anyone I meant something.",
            "Kill three ghouls of the Pack: {n} of {need}"),
        new Chapter("roma_dawn", "When the circus sleeps", Task.DAWN, 1, Set.of(), "",
            "Be with me at dawn. That's when the Clowns sleep and nobody needs me to be funny. ...I want to see what I'm like then. With you.",
            "Dawn. Here. I'll be the one not wearing a mask.",
            "Be with Roma at dawn"));

    /**
     * 4.1.54 (the owner: Banjou "wants to be tough but truly isnt, is aware of how weak he truly is", "Romanceable (Only females)"): Banjou's:
     * supper for his hungry men (he is the one supposed to feed them, and he can't hunt), the investigators sniffing round his hideout that he cannot see off
     * alone, somewhere high where he can feel big for once, a wild ghoul made a friend (he has tried to recruit for years and they laugh at
     * him), and the cottage in the dead of night, the mask off.
     */
    static final List<Chapter> BANJOU = List.of(
        // 4.1.80: was four Ghoul Meat handed to him (Ken asks for Ghoul Meat too): supper cooked and handed to the boys themselves; then,
        // was three Doves killed: the dead garden behind his cottage planted.
        new Chapter("banjou_supper", "Supper for the boys", Task.SERVE, 3, SUPPER, "a bowl",
            "You want to... help? Me? Okay. Okay. The boys are hungry and I'm the one who's supposed to feed them. Cook them something hot. "
                + "Bowls, from the cookbook. Hand one to three of them yourself. ...I'd do it. I'd burn it. I always do.",
            "{n} of three fed. The boys eat before I do. They always do.",
            "Cook bowls from the cookbook and hand one each to three of Banjou's Gas Masks (right-click them with it): {n} of {need}"),
        new Chapter("banjou_doves", "Something that grows", Task.GARDEN, 6, Set.of(), "",
            "There's a garden at the den. Was. Nothing grows for me. I planted everything last spring and it all died, and the boys laughed for a "
                + "week. ...Would you plant something with me? Flowers. Saplings. Six. I'll watch. I'll hold the... I'll watch.",
            "{n} of six in the ground. Careful with that one. ...They're all that one.",
            "Plant flowers or saplings at the Gas Masks' den with Banjou there: {n} of {need}"),
        new Chapter("banjou_high", "Somewhere I look big", Task.HIGH, 1, Set.of(), "",
            "Take me somewhere high? Really high. Where everything looks small. ...I'd like to feel big, just once, next to somebody. Is that stupid?",
            "Higher. I can still see things that are bigger than me.",
            "Take Banjou somewhere high (him following you)"),
        new Chapter("banjou_recruit", "Show me how it's done", Task.ALLY, 1, Set.of(), "",
            "I've been trying to bring wild ghouls into the Gas Masks for years. They laugh at me. Every time. ...Make one of them a friend. "
                + "Show me how you do it. I'll take notes.",
            "Still working on it? They laughed at me too. Don't let it get to you.",
            "Make a wild ghoul your ally (it walks with you)"),
        new Chapter("banjou_deep", "The mask comes off", Task.DEEP, 1, Set.of(), "",
            "Come to the cottage tonight. Late. When the boys are asleep and I've got the mask on. ...I want you to see me take it off. I don't "
                + "do that in front of anyone.",
            "The dead of night. The cottage. I'll leave the lantern lit.",
            "Be with Banjou at his cottage in the dead of night"));

    /** {@code key}'s chapters, in order (none for one who has none). */
    public static List<Chapter> of(String key) {
        return switch (key == null ? "" : key) {
            case "eto" -> ETO;
            case "touka" -> TOUKA;
            case "ken" -> KEN;
            case "ayato" -> AYATO;
            case "general" -> GENERAL;
            case "roma" -> ROMA;
            case "banjou" -> BANJOU;
            default -> List.of();
        };
    }

    // ---------------------------------------------------------------- where they are

    /** How many chapters are closed (0 to all of them) in {@code bond}, for a courtship of {@code total} chapters. */
    public static int closed(QuestLog.Bond bond, int total) {
        int bits = (bond.flags() & BITS) >>> SHIFT;
        if (bits == 0 && (bond.flags() & LEGACY) != 0) {
            // The errands of before: all three done, they were waiting to ask; else one chapter for each done.
            if (bond.has(EtoRules.FLOWER_DONE)) {
                return total;
            }
            bits = (bond.has(EtoRules.BOOKS_DONE) ? 1 : 0) + (bond.has(EtoRules.HUNT_DONE) ? 1 : 0);
        }
        return Math.max(0, Math.min(total, bits));
    }

    /**
     * 4.1.80: the chapters whose task changed in 4.1.80, and what each needed before: a chapter on from before it changed starts again
     * (its count counted something else), unless it was already done, waiting for its moment (then it stays done).
     */
    static final java.util.Map<String, Integer> CHANGED_480 = java.util.Map.of("touka_patrol", 4, "ken_doves", 1, "ayato_ghouls", 3,
        "ayato_roof", 1, "general_vouch", 1, "general_pack", 3, "general_high", 1, "roma_show", 3, "roma_walk", 3, "banjou_supper", 4);
    /** Banjou's second: its old need (Map.of takes ten pairs at most). */
    static final int BANJOU_DOVES_BEFORE = 3;

    /** 4.1.80: what {@code id} needed before 4.1.80 changed it (0: it didn't change). */
    static int before480(String id) {
        return "banjou_doves".equals(id) ? BANJOU_DOVES_BEFORE : CHANGED_480.getOrDefault(id, 0);
    }

    /**
     * 4.1.80: {@code bond} (of {@code key}'s courtship, after the 2.1.5 step) as 4.1.80 keeps it: a chapter on whose task changed starts
     * again, or stays done if it was ({@link #before480}); marked {@link #FROM_480} once looked at (begun or not).
     */
    static QuestLog.Bond migrate480(String key, QuestLog.Bond bond) {
        if (bond.has(FROM_480)) {
            return bond;
        }
        QuestLog.Bond marked = new QuestLog.Bond(bond.regard(), bond.chatted(), bond.flags() | FROM_480, bond.favours());
        Chapter chapter = chapter(key, bond);
        int was = chapter == null ? 0 : before480(chapter.id());
        if (was == 0) {
            return marked;
        }
        return EtoRules.count(marked, EtoRules.count(bond) >= was ? chapter.need() : 0);
    }

    /** 4.1.80: {@code bond} as this courtship keeps it for {@code key} (every step: the errands', 2.1.5's, 4.1.80's). */
    public static QuestLog.Bond migrate(String key, QuestLog.Bond bond, int total) {
        return migrate480(key, migrate(bond, total));
    }

    /** {@code bond} as this courtship keeps it: the errands' flags read as chapters (their count, meaningless now, from 0). */
    public static QuestLog.Bond migrate(QuestLog.Bond bond, int total) {
        if ((bond.flags() & LEGACY) == 0) {
            // 2.1.5: a chapter on from before it changed what each asks: its count starts again (it counted something else).
            if (bond.has(EtoRules.LINE_ON) && !bond.has(FROM_215)) {
                return EtoRules.count(new QuestLog.Bond(bond.regard(), bond.chatted(), bond.flags() | FROM_215, bond.favours()), 0);
            }
            return bond;
        }
        int closed = closed(bond, total);
        QuestLog.Bond cleared = new QuestLog.Bond(bond.regard(), bond.chatted(), bond.flags() & ~LEGACY & ~BITS | Math.min(7, closed) << SHIFT | FROM_215,
            bond.favours());
        return EtoRules.count(cleared, 0);
    }

    /** True once begun and not every chapter is closed. */
    public static boolean on(QuestLog.Bond bond, int total) {
        return bond.has(EtoRules.LINE_ON) && !bond.has(EtoRules.DATING) && closed(bond, total) < total;
    }

    /** The chapter on for {@code key} (null: not begun, or all closed). */
    public static Chapter chapter(String key, QuestLog.Bond bond) {
        List<Chapter> all = of(key);
        if (!on(bond, all.size())) {
            return null;
        }
        return all.get(closed(bond, all.size()));
    }

    /** True if the chapter on is done and its moment waits to be played. */
    public static boolean ready(String key, QuestLog.Bond bond) {
        Chapter chapter = chapter(key, bond);
        return chapter != null && EtoRules.count(bond) >= chapter.need();
    }

    /** Begun: the first chapter on, nothing done of it. */
    public static QuestLog.Bond begin(QuestLog.Bond bond) {
        return EtoRules.count(new QuestLog.Bond(bond.regard(), bond.chatted(), (bond.flags() | EtoRules.LINE_ON | FROM_215 | FROM_480) & ~LEGACY
            & ~BITS, bond.favours()), 0);
    }

    /** {@code amount} more done on the chapter on for {@code key}, if it is a {@code task} (never past what it needs). */
    public static QuestLog.Bond progress(String key, QuestLog.Bond bond, Task task, int amount) {
        return progress(key, bond, task, amount, Integer.MAX_VALUE);
    }

    /** 2.1.5: the same, for a Dove of rank {@code rank} (a chapter wanting a higher one doesn't count it). */
    public static QuestLog.Bond progress(String key, QuestLog.Bond bond, Task task, int amount, int rank) {
        Chapter chapter = chapter(key, bond);
        if (chapter == null || chapter.task() != task || amount <= 0 || rank < chapter.rank()) {
            return bond;
        }
        return EtoRules.count(bond, Math.min(chapter.need(), EtoRules.count(bond) + amount));
    }

    /** Its moment played: the chapter closed, the next on (nothing done of it). Only a ready chapter closes. */
    public static QuestLog.Bond close(String key, QuestLog.Bond bond) {
        List<Chapter> all = of(key);
        if (!ready(key, bond)) {
            return bond;
        }
        int closed = closed(bond, all.size()) + 1;
        return EtoRules.count(new QuestLog.Bond(bond.regard(), bond.chatted(), bond.flags() & ~LEGACY & ~BITS | Math.min(7, closed) << SHIFT,
            bond.favours()), 0);
    }

    /**
     * 2.2.7 (the owner: "a command to fully marry ... everything about that person's relationship is instantly maxed, categorized as married,
     * and everything's full"): {@code bond} as a marriage leaves it, all at once: regard at the most, met and befriended, the courtship
     * begun and every chapter of it closed, dating (the stage the world keeps, married, is set beside it), nothing left owing on a chapter.
     * What was taught stays taught; the day of the last gift is kept.
     */
    public static QuestLog.Bond wed(QuestLog.Bond bond) {
        int flags = bond.flags() & ~LEGACY & ~BITS | NpcBonds.MET | NpcBonds.BEFRIENDED | EtoRules.LINE_ON | EtoRules.DATING | FROM_215
            | FROM_480 | Math.min(7, CHAPTERS) << SHIFT;
        return EtoRules.count(new QuestLog.Bond(NpcBonds.MOST, bond.chatted(), flags, bond.favours()), 0);
    }

    /** How many of a {@link Task#GIVE}'s items a hand-over of {@code held} takes now (0: it isn't one, or it is done). */
    public static int takes(String key, QuestLog.Bond bond, int held) {
        Chapter chapter = chapter(key, bond);
        if (chapter == null || chapter.task() != Task.GIVE || held <= 0) {
            return 0;
        }
        return Math.max(0, Math.min(held, chapter.need() - EtoRules.count(bond)));
    }

    /** True if {@code item} (an item id) is what the chapter on for {@code key} asks to be handed over. */
    public static boolean wants(String key, QuestLog.Bond bond, String item) {
        Chapter chapter = chapter(key, bond);
        return chapter != null && chapter.task() == Task.GIVE && item != null && chapter.items().contains(item);
    }

    /**
     * 4.1.80: true if planting {@code what} went into the ground: one of the clicked block and its sides was something else
     * ({@code before}) and is it now ({@code after}, the same places in the same order).
     */
    public static boolean planted(String what, String[] before, String[] after) {
        if (!plant(what) || before == null || after == null || before.length != after.length) {
            return false;
        }
        for (int i = 0; i < after.length; i++) {
            if (what.equals(after[i]) && !what.equals(before[i])) {
                return true;
            }
        }
        return false;
    }

    // ---------------------------------------------------------------- 4.1.80: where and when

    /** How far over the General's floor the tower's top must be (blocks); how near its top height one on its roof stands. */
    public static final int ROOF_OVER = 6;
    public static final double ROOF_SLACK = 4.0;

    /**
     * True if one standing at {@code y} is up on the CCG tower's roof: nothing over them ({@code topHere}: the first open block over
     * their column, as the world's height map has it), about as high as the tower's top over the General's spot ({@code topTower}), which
     * is a building standing over his floor ({@code homeY}).
     */
    public static boolean onRoof(double y, int topHere, int topTower, int homeY) {
        return topTower >= homeY + ROOF_OVER && y >= topHere - 0.5 && Math.abs(y - topTower) <= ROOF_SLACK;
    }

    /** True if it is daylight at {@code time} (the day's ticks): the hours a village is up and about (a walk by day). */
    public static boolean daylight(long time) {
        long t = Math.floorMod(time, 24000L);
        return t >= 1000L && t < 12000L;
    }

    // ---------------------------------------------------------------- 4.1.80: Touka's homework

    /**
     * Her homework (she studies every night to stay in her human school): school questions, the right answer first (shown turned round,
     * as the academy's test is). Append only (asked by index).
     */
    public static final JoinTrial.Question[] HOMEWORK = {
        new JoinTrial.Question("The cafe sells three cups for 450 yen. How much are seven?", "1,050 yen.", "1,350 yen.", "950 yen."),
        new JoinTrial.Question("Solve for x: 3x + 7 = 22.", "x = 5.", "x = 7.", "x = 3."),
        new JoinTrial.Question("A right triangle's two short sides are 3 and 4. The long side is...", "5.", "7.", "6."),
        new JoinTrial.Question("What's the square root of 144?", "12.", "14.", "16."),
        new JoinTrial.Question("What's 15% of 200?", "30.", "15.", "20."),
        new JoinTrial.Question("Which gas do plants take in to make their food?", "Carbon dioxide.", "Oxygen.", "Nitrogen."),
        new JoinTrial.Question("At sea level, water boils at...", "100 degrees Celsius.", "90 degrees Celsius.", "120 degrees Celsius."),
        new JoinTrial.Question("The largest planet in the solar system is...", "Jupiter.", "Saturn.", "Neptune."),
        new JoinTrial.Question("The past tense of \"run\" is...", "Ran.", "Runned.", "Running.")};

    /** The {@link #ASKED} homework questions asked on {@code day} of a player of {@code mine} (indexes, all different). */
    public static int[] homework(long day, long mine) {
        return JoinTrial.draw(HOMEWORK.length, ASKED, day, mine);
    }

    /** True if the homework may be tried on {@code day}: never on the day of a wrong answer ({@code failedOn}; -1 for none). */
    public static boolean mayStudy(long day, long failedOn) {
        return day != failedOn;
    }

    // ---------------------------------------------------------------- its words

    /** {@code text} with the chapter's counts in it ({@code done} so far). */
    public static String fill(String text, Chapter chapter, int done) {
        int step = STEP_SECONDS;
        return text.replace("{n}", Integer.toString(done)).replace("{need}", Integer.toString(chapter.need()))
            .replace("{mm}", minutes(chapter.need() * step)).replace("{m}", minutes(done * step));
    }

    /** Seconds as minutes ("1", "1.5", "2"). */
    static String minutes(int seconds) {
        int halves = Math.round(seconds / 30.0F);
        return halves % 2 == 0 ? Integer.toString(halves / 2) : (halves / 2) + ".5";
    }

    /** What the companion says asked about the chapter on: its ask if nothing is done yet, else how far along. */
    public static String said(Chapter chapter, int done) {
        return fill(done <= 0 || chapter.task().once() ? chapter.ask() : chapter.progress(), chapter, done);
    }

    /** The journal's name for the chapter on: "Chapter 2 of 5: A walk after dark" ("" past the last). */
    public static String journal(String key, int closed, int done) {
        List<Chapter> all = of(key);
        if (closed < 0 || closed >= all.size()) {
            return "";
        }
        Chapter chapter = all.get(closed);
        return "Chapter " + (closed + 1) + " of " + all.size() + ": " + chapter.title() + (done >= chapter.need() ? " (done)" : "");
    }

    /** The journal's line for what the chapter on asks, done so far (and once it's done, to go and see them). */
    public static String task(String key, int closed, int done) {
        List<Chapter> all = of(key);
        if (closed < 0 || closed >= all.size()) {
            return "";
        }
        Chapter chapter = all.get(closed);
        return done >= chapter.need() ? "Done: go and see " + Persona.of(key).her() + "." : fill(chapter.journal(), chapter, done) + ".";
    }

    /** 4.1.80: the button that begins a chapter begun from the talk ({@link Task#begun}). */
    public static String begin(Task task) {
        return task == Task.SPAR ? "Let's fight. (the spar)" : task == Task.QUIZ ? "Okay. Read me the first one." : "";
    }

    /** The talk screen's words for handing over the chapter's items. */
    public static String handOver(Chapter chapter) {
        return "Here. What you asked for. (" + chapter.itemName() + " in your hand)";
    }

    // ---------------------------------------------------------------- dates (once they are together)

    /** The sweet page's answer that opens the dates. */
    public static final String DATES = "Let's go somewhere. (a date)";

    /** What {@code key} says offering where to go. */
    public static String datesAsked(String key, int seed) {
        String[] lines = switch (key == null ? "" : key) {
            case "eto" -> new String[] {"A date? With me? ...Where are you taking me, then? Choose well. I'll write about it.",
                "Out? Together? In public? How scandalous. Pick somewhere.", "Mm. Somewhere with you. Anywhere's fine. Pick, before I pick for us."};
            case "touka" -> new String[] {"A date. Right. Fine. Where? ...Don't say the cafe, I work there.",
                "You want to go out? With me? ...Okay. Where. Quickly, before I change my mind.", "Fine. One date. Where are we going?"};
            case "ken" -> new String[] {"A date? I'd love that. Where would you like to go?", "Somewhere with you? ...Yes. Anywhere. You choose.",
                "I'm not very good at dates. But I'd like to try. With you."};
            case "ayato" -> new String[] {"A date. ...Fine. Where. Somewhere without people.", "You want to go out? With me? ...Yeah. Pick. Fast.",
                "Don't call it a date. ...Okay, it's a date. Where?"};
            case "general" -> new String[] {"An evening off. ...I haven't taken one in years. Where shall we go?",
                "Out. With you. ...Yes. I'll leave the coat. No, I'll keep the coat. Where?", "I'll clear my evening. Name the place."};
            case "roma" -> new String[] {"A date! Oh, I LOVE dates. Somewhere dramatic. Somewhere we could plausibly both die. Pick!",
                "Out? With the ringmaster? Bold. Very bold. I'll wear my good face. Where?", "Yes yes yes. Where? Don't say a graveyard. ...Do say a graveyard."};
            case "banjou" -> new String[] {"A date? With me? ...Okay. Okay! Where? Somewhere quiet. Somewhere I won't trip over anything.",
                "Out? Together? I'll wash the beard. Where are we going?", "Yes. Yes. Pick somewhere. I'll follow you anywhere. Literally, I'm bad with maps."};
            default -> new String[] {"Where to?"};
        };
        return lines[Math.floorMod(seed, lines.length)];
    }

    /** What {@code key} says asked for a second date in a day. */
    public static String datedToday(String key, int seed) {
        String[] lines = switch (key == null ? "" : key) {
            case "eto" -> new String[] {"We already went out today. Greedy. ...Tomorrow. I'll think of somewhere worse.",
                "One date a day, or I'll run out of things to write about you."};
            case "touka" -> new String[] {"We went out already. Twice in one day? People will talk. ...Tomorrow.", "Tomorrow. I have a shift. And homework. And you."};
            case "ken" -> new String[] {"We already went out today. ...Tomorrow? I'll be looking forward to it all night.",
                "Tomorrow. I want to have something to look forward to."};
            case "ayato" -> new String[] {"Twice in one day? Greedy. ...Tomorrow.", "Tomorrow. Don't forget. I won't."};
            case "general" -> new String[] {"Twice in a day and the bureau will notice. ...Tomorrow.", "Tomorrow. I'll be watching the clock."};
            case "roma" -> new String[] {"Twice in one day? Greedy! I love it. No. Tomorrow. Leave them wanting more, that's showbiz.",
                "Tomorrow, sweetheart. The ringmaster needs her beauty sleep. In a coffin. Kidding. It's a hammock."};
            case "banjou" -> new String[] {"We already went out today. ...Tomorrow? I'll be counting the hours. Don't tell anyone that.",
                "Tomorrow. I need tonight to calm down from the last one."};
            default -> new String[] {"Tomorrow."};
        };
        return lines[Math.floorMod(seed, lines.length)];
    }

    // ---------------------------------------------------------------- on the talk screen

    /**
     * The first page's courtship answers for {@code key} ({@code romantic}: whether it would date them at all): before it begins, once
     * they are close enough, "Need anything done?"; on a chapter, what they can say about it (the items held out, the task done, or how
     * it's going); every chapter closed, "Go out with me."
     */
    public static void options(String key, EtoTalk.Context c, boolean romantic, List<EtoTalk.Option> out) {
        if (EtoVoice.of(c).together() || !c.mayDate() || !romantic) {
            return;
        }
        switch (c.step()) {
            case NONE -> {
                if (c.regard() >= BEGIN_AT) {
                    out.add(EtoTalk.Option.of(NpcTalk.Choice.FAVOUR));
                }
            }
            case COURTING -> {
                Chapter chapter = c.chapter(key);
                if (chapter != null) {
                    int done = c.count();
                    String label = done >= chapter.need() ? "I did what you asked."
                        : chapter.task() == Task.GIVE && c.held() > 0 ? handOver(chapter) : done > 0 ? "About what you asked..." : "";
                    out.add(new EtoTalk.Option(NpcTalk.Choice.FAVOUR, label, ""));
                }
            }
            case ASK -> out.add(EtoTalk.Option.of(NpcTalk.Choice.ASK_OUT));
            default -> {
            }
        }
    }

    /**
     * Its answer to "Need anything done?" (and what is said about a chapter after): the first chapter's ask as the courtship begins
     * ({@link EtoTalk.Effect#BEGIN}); the items held out taken, or the task done, and its moment played ({@link EtoTalk.Effect#COURT}: the
     * line here is what it says if the hand-over leaves some still to bring); how it's going; the rest in its own words ({@code notYet}
     * before they are close enough, {@code asking} once every chapter is closed, {@code dating}, {@code taken} by someone else,
     * {@code married} to someone else).
     */
    public static EtoTalk.Reply favour(String key, EtoTalk.Context c, String notYet, String asking, String dating, String taken, String married) {
        if (!c.mayDate()) {
            return new EtoTalk.Reply(c.claimed() ? EtoLines.fill(c, taken) : married, 0, EtoTalk.Effect.NONE);
        }
        List<Chapter> all = of(key);
        return switch (c.step()) {
            case NONE -> c.regard() >= BEGIN_AT && !all.isEmpty() ? new EtoTalk.Reply(fill(all.get(0).ask(), all.get(0), 0), 0, EtoTalk.Effect.BEGIN)
                : new EtoTalk.Reply(notYet, 0, EtoTalk.Effect.NONE);
            case ASK -> new EtoTalk.Reply(asking, 0, EtoTalk.Effect.NONE);
            case DATING -> new EtoTalk.Reply(dating, 0, EtoTalk.Effect.NONE);
            default -> {
                Chapter chapter = c.chapter(key);
                if (chapter == null) {
                    yield new EtoTalk.Reply(asking, 0, EtoTalk.Effect.NONE);
                }
                int done = c.count();
                if (done >= chapter.need()) {
                    yield new EtoTalk.Reply("", 0, EtoTalk.Effect.COURT);
                }
                if (chapter.task() == Task.GIVE && c.held() > 0) {
                    int after = done + Math.min(c.held(), chapter.need() - done);
                    yield new EtoTalk.Reply(fill(chapter.progress(), chapter, after), 0, EtoTalk.Effect.COURT);
                }
                yield new EtoTalk.Reply(said(chapter, done), 0, EtoTalk.Effect.NONE);
            }
        };
    }
}
