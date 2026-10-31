package com.hex.kagune.control.quest;

import static com.hex.kagune.control.quest.Objective.of;

import com.hex.kagune.control.KagunePath;
import com.hex.kagune.control.quest.Quest.Line;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 7.0.1: every quest there is (pure, tested), the path quests that open each {@link Gate} and the ghoul's own. Most gates have
 * two ways in: a quest or a rival quest (some of them an {@code exclusive} pair, the side the ghoul takes: Anteiku's restraint or
 * the Pack's violence). Hunts ({@link Hunts}) and the NPCs' favours are made as they are offered and are not in here.
 */
public final class QuestBook {
    public static final String ANTEIKU = "anteiku";
    public static final String PACK = "pack";

    private static final Map<String, Quest> QUESTS = new LinkedHashMap<>();

    static {
        // ------------------------------------------------------------------ any ghoul: the ghoul's life, the eye-mode fight
        add(new Quest("first_meal", Line.GHOUL, null, "First Meal",
            "Human food turns to ash on your tongue now. Only flesh will do. Get used to it.", 0, null, null,
            List.of(of("Eat raw flesh", Signal.EAT, 10, Tags.FLESH)), Reward.mastery(15)));
        add(new Quest("light_feet", Line.GHOUL, null, "Light on Your Feet",
            "Without the kagune a ghoul still has its body. Hit fast, never stand still.", 150, null, null,
            List.of(of("Land blitz strikes, eyes open", Signal.STRIKE, 60), of("Flip", Signal.FLIP, 15)),
            Reward.mastery(25).opens(Gate.EYES_KICK)));
        add(new Quest("up_we_go", Line.GHOUL, null, "Up We Go!",
            "The One-Eyed Owl never touched the ground long. Fight in the air.", 350, List.of("light_feet"), null,
            List.of(of("Flip", Signal.FLIP, 80), of("Kill in mid-air, eyes open", Signal.KILL, 8, Tags.EYES, Tags.AIRBORNE)),
            Reward.mastery(40).opens(Gate.EYES_SOAR)));
        add(new Quest("witness_this", Line.GHOUL, null, "Witness This",
            "One punch is not enough. Make them watch the whole thing.", 550, List.of("light_feet"), null,
            List.of(of("Land full-charge Big Punches", Signal.PUNCH, 12, Tags.FULL),
                of("Kill an iron golem, eyes open", Signal.KILL, 1, Tags.mob("minecraft:iron_golem"), Tags.EYES)),
            Reward.mastery(50).opens(Gate.EYES_WITNESS)));
        add(new Quest("night_stalker", Line.GHOUL, null, "Night Stalker",
            "The city belongs to ghouls after dark. Take it back with your bare hands.", 100, null, null,
            List.of(of("Kill hostiles at night, eyes open", Signal.KILL, 25, Tags.HOSTILE, Tags.NIGHT, Tags.EYES)), Reward.mastery(35)));
        add(new Quest("the_brink", Line.GHOUL, null, "The Brink",
            "A ghoul's body only learns what it nearly lost.", 100, null, null,
            List.of(of("Live through the brink (below 1/5 health)", Signal.BRINK, 3)), Reward.mastery(30).meat(1)));
        add(new Quest("rampage", Line.GHOUL, null, "Rampage",
            "Let the hunger off its leash, just once.", 200, null, null,
            List.of(of("Kill 5 in a row, each within 10 s of the last", Signal.RAMPAGE, 5)), Reward.mastery(40)));
        add(new Quest("die_hard", Line.GHOUL, null, "Die Hard",
            "Death keeps coming for you. It keeps leaving disappointed.", 50, null, null,
            List.of(of("Die", Signal.DIE, 5)), Reward.mastery(10).points(1)));
        add(new Quest("cannibal", Line.GHOUL, null, "Cannibal",
            "Ghouls who eat ghouls grow something new inside them. Taste it.", 100, null, null,
            List.of(of("Eat Ghoul Meat", Signal.EAT, 3, Tags.GHOUL_MEAT)), Reward.mastery(20).points(1)));
        add(new Quest("ghoul_eater", Line.GHOUL, null, "Ghoul Eater",
            "Every ghoul you devour is RC you didn't have to earn.", 300, List.of("cannibal"), null,
            List.of(of("Kill ghouls", Signal.KILL, 5, Tags.GHOUL), of("Eat Ghoul Meat", Signal.EAT, 5, Tags.GHOUL_MEAT)),
            Reward.mastery(30).points(1).meat(2)));
        // 7.0.2: the ghouls of the night. Talk before you bite; earn a friend (a second way to the Speed Kick) or turn on one who
        // trusted you (the two close each other); run their errands; fight beside one.
        add(new Quest("among_our_own", Line.GHOUL, null, "Among Our Own",
            "You are not the only one out here at night. Find the others, and talk before you bite.", 0, null, null,
            List.of(of("Talk with ghouls you have not met", Signal.TALK, 3, Tags.FIRST)), Reward.mastery(15)));
        add(new Quest("a_friend", Line.GHOUL, null, "A Friend in the Dark",
            "Trust is rarer than flesh among ghouls. Earn some, and a friend shows you how they fight.", 100, List.of("among_our_own"), "trust",
            List.of(of("Befriend a ghoul", Signal.BEFRIEND, 1), of("Do a favour for a ghoul", Signal.FAVOUR, 1)),
            Reward.mastery(30).opens(Gate.EYES_KICK)));
        add(new Quest("cold_blood", Line.GHOUL, null, "Cold Blood",
            "Let one of them trust you. Then remind it what you are.", 100, List.of("among_our_own"), "trust",
            List.of(of("Turn on a ghoul who trusted you", Signal.BETRAY, 1)), Reward.mastery(20).points(1).meat(2)));
        add(new Quest("errands", Line.GHOUL, null, "Errands of the Night",
            "Every ghoul who owes you is one less ghoul hunting you.", 200, List.of("a_friend"), null,
            List.of(of("Do favours for ghouls", Signal.FAVOUR, 4)), Reward.mastery(40).points(1)));
        add(new Quest("side_by_side", Line.GHOUL, null, "Side by Side",
            "Two kagune are worse than one, for whoever stands in front of them.", 300, List.of("a_friend"), null,
            List.of(of("Have a ghoul walk with you", Signal.BEFRIEND, 1, Tags.ALLY), of("Kill hostiles with your ally near", Signal.KILL, 10, Tags.HOSTILE,
                Tags.ALLY)), Reward.mastery(50).meat(2)));

        // 1.3.0: Eto's lessons, a third way to the eye-mode fight's gates: each locked until she teaches it at the Library
        // (QuestLog.state, EtoRules.Lesson), in order.
        add(new Quest("eto_kick", Line.GHOUL, null, "Eto's Lesson: Light Feet",
            "Strike, strike, flip away, then all your weight through one kick. She'll want to see it.", 0, null, null,
            List.of(of("Land blitz strikes", Signal.STRIKE, 40), of("Flip", Signal.FLIP, 12)), Reward.mastery(25).opens(Gate.EYES_KICK)));
        add(new Quest("eto_soar", Line.GHOUL, null, "Eto's Lesson: Up We Go",
            "The ground is for people who are tired. Kill them before you land.", 0, List.of("eto_kick"), null,
            List.of(of("Land Speed Kicks", Signal.KICK, 12), of("Kill in mid-air, eyes open", Signal.KILL, 5, Tags.EYES, Tags.AIRBORNE)),
            Reward.mastery(40).opens(Gate.EYES_SOAR)));
        add(new Quest("eto_witness", Line.GHOUL, null, "Eto's Lesson: Witness This",
            "One punch is rude. Wind it all the way back, and make them watch.", 0, List.of("eto_soar"), null,
            List.of(of("Land full-charge Big Punches", Signal.PUNCH, 8, Tags.FULL), of("Flip", Signal.FLIP, 30)),
            Reward.mastery(50).opens(Gate.EYES_WITNESS)));
        // 1.3.2: her last lesson; done, she makes them one-eyed (EtoServer), whoever they are to her.
        add(new Quest("eto_one_eye", Line.GHOUL, null, "Eto's Lesson: One Eye",
            "Kill in the dark with your eye open until the dark feels like home. Bleed until it stops scaring you. Then come back and lie down.",
            0, List.of("eto_witness"), null,
            List.of(of("Kill at night, your eye open", Signal.KILL, 15, Tags.EYES, Tags.NIGHT), of("Live through the brink", Signal.BRINK, 3)),
            Reward.mastery(60)));

        // ------------------------------------------------------------------ Rinkaku: the one-eyed king
        add(new Quest("rizes_gift", Line.PATH, KagunePath.RINKAKU, "Rize's Gift",
            "Her kakuhou beats under your ribs. Let it out and let it feed.", 200, null, null,
            List.of(of("Kill with the kagune out", Signal.KILL, 15, Tags.KAGUNE), of("Eat raw flesh", Signal.EAT, 5, Tags.FLESH)),
            Reward.mastery(20).opens(Gate.RINKAKU_TWIN)));
        add(new Quest("say_no", Line.PATH, KagunePath.RINKAKU, "Learning to Say No",
            "The kagune wants. You decide. Prove it.", 200, null, null,
            List.of(of("Restrain the kagune's urge (crouch)", Signal.RESTRAIN, 5), of("Seconds with only the eyes open", Signal.TIME, 300, Tags.EYES)),
            Reward.mastery(20).opens(Gate.RINKAKU_TWIN)));
        add(new Quest("anteiku", Line.PATH, KagunePath.RINKAKU, "Anteiku's Way",
            "Coffee, restraint, and a hand that only kills when it must. The old man is watching.", 450, null, "rinkaku_four",
            List.of(of("Restrain the kagune's urge", Signal.RESTRAIN, 10), of("Live through the brink", Signal.BRINK, 2),
                of("Kill hostiles, eyes open", Signal.KILL, 20, Tags.HOSTILE, Tags.EYES)),
            Reward.mastery(40).opens(Gate.RINKAKU_FOUR).side(ANTEIKU)));
        add(new Quest("pack", Line.PATH, KagunePath.RINKAKU, "The Pack's Way",
            "The Pack takes the strong and eats the weak. Show them which you are.", 450, null, "rinkaku_four",
            List.of(of("Kill ghouls", Signal.KILL, 3, Tags.GHOUL), of("Kill with the kagune out", Signal.KILL, 30, Tags.KAGUNE, Tags.HOSTILE)),
            Reward.mastery(40).opens(Gate.RINKAKU_FOUR).side(PACK)));
        add(new Quest("thousand_minus_seven", Line.PATH, KagunePath.RINKAKU, "1000 Minus 7",
            "993. 986. 979... Count while it hurts. Something white grows out of the pain.", 700, null, null,
            List.of(of("Live through the brink", Signal.BRINK, 7), of("Die", Signal.DIE, 3)),
            Reward.mastery(50).opens(Gate.RINKAKU_CENTIPEDE)));
        add(new Quest("ghoul_eats_ghoul", Line.PATH, KagunePath.RINKAKU, "Ghoul Eats Ghoul",
            "A kakuja is what grows in a ghoul that eats its own kind.", 700, null, null,
            List.of(of("Eat Ghoul Meat", Signal.EAT, 8, Tags.GHOUL_MEAT), of("Kill ghouls", Signal.KILL, 5, Tags.GHOUL)),
            Reward.mastery(50).opens(Gate.RINKAKU_CENTIPEDE)));
        // 1.0.41: Graft's revive, the Rinkaku path's one passive now, behind the very quest Graft does for it.
        add(new Quest("rinkaku_not_that_easily", Line.PATH, KagunePath.RINKAKU, "You Thought You'd Get Rid of Me?",
            "Die, and die again, until dying stops working.", 450, null, null,
            List.of(of("Die", Signal.DIE, 5), of("Live through the brink", Signal.BRINK, 3)), Reward.mastery(20).opens(Gate.RINKAKU_NOT_THAT_EASILY)));
        add(new Quest("one_eyed_king", Line.PATH, KagunePath.RINKAKU, "The One-Eyed King",
            "Eat until there's nothing human left to eat. Then be king of what remains.", 950, null, null,
            List.of(of("Eat Ghoul Meat", Signal.EAT, 15, Tags.GHOUL_MEAT), of("Kill a boss in a kakuja", Signal.KILL, 1, Tags.BOSS, Tags.KAKUJA)),
            Reward.mastery(80).opens(Gate.RINKAKU_FULL_KAKUJA)));
        add(new Quest("centipede_feast", Line.PATH, KagunePath.RINKAKU, "The Centipede's Feast",
            "Stay in the kakuja until it stops feeling like a cage.", 950, null, null,
            List.of(of("Seconds in a kakuja", Signal.TIME, 600, Tags.KAKUJA), of("Kill ghouls in a kakuja", Signal.KILL, 10, Tags.GHOUL, Tags.KAKUJA)),
            Reward.mastery(80).opens(Gate.RINKAKU_FULL_KAKUJA)));
        // 1.0.4: Kakuja V2. Only kills in the Full Kakuja count, so it follows either way to it; done, the next death is the Dragon's.
        add(new Quest("the_dragon", Line.PATH, KagunePath.RINKAKU, "Kakuja V2: The Dragon",
            "Something enormous coils under your skin and waits. Feed it, bleed for it, and the next death will not be yours.", 1000, null, null,
            List.of(of("Kill in the Full Kakuja", Signal.KILL, 100, Tags.form(com.hex.kagune.gameplay.KaguneForm.FULL_KAKUJA.id())),
                of("Live through the brink in a kakuja", Signal.BRINK, 7, Tags.KAKUJA)),
            Reward.NONE.opens(Gate.RINKAKU_DRAGON)));

        // ------------------------------------------------------------------ Ukaku: the black rabbit
        add(new Quest("crystal_rain", Line.PATH, KagunePath.UKAKU, "Crystal Rain",
            "Ukaku burns out fast. Make every shard count.", 350, null, null,
            List.of(of("Kill with shots", Signal.KILL, 25, Tags.RANGED, Tags.KAGUNE)), Reward.mastery(25).opens(Gate.UKAKU_GATLING)));
        add(new Quest("black_rabbit", Line.PATH, KagunePath.UKAKU, "The Black Rabbit",
            "They never see a rabbit coming from above.", 350, null, null,
            List.of(of("Kill in mid-air, kagune out", Signal.KILL, 12, Tags.AIRBORNE, Tags.KAGUNE)), Reward.mastery(25).opens(Gate.UKAKU_GATLING)));
        add(new Quest("bursting", Line.PATH, KagunePath.UKAKU, "Bursting",
            "Shards that stick are shards wasted. Make them burst.", 400, null, null,
            List.of(of("Deal damage, kagune out", Signal.DEAL, 600, Tags.KAGUNE), of("Kill in a rampage of 4", Signal.RAMPAGE, 4)),
            Reward.mastery(20).opens(Gate.UKAKU_BURSTING_SHARDS)));
        // 1.0.41: Graft's revive as Ukaku's fifth mutation, behind the very quest Graft does for it.
        add(new Quest("ukaku_not_that_easily", Line.PATH, KagunePath.UKAKU, "You Thought You'd Get Rid of Me?",
            "Die, and die again, until dying stops working.", 450, null, null,
            List.of(of("Die", Signal.DIE, 5), of("Live through the brink", Signal.BRINK, 3)), Reward.mastery(20).opens(Gate.UKAKU_NOT_THAT_EASILY)));
        add(new Quest("packs_rabbit", Line.PATH, KagunePath.UKAKU, "The Pack's Rabbit",
            "The Pack gave you a place. Earn it in blood, theirs or anyone's.", 700, null, "ukaku_kakuja",
            List.of(of("Kill ghouls", Signal.KILL, 3, Tags.GHOUL), of("Eat Ghoul Meat", Signal.EAT, 5, Tags.GHOUL_MEAT)),
            Reward.mastery(50).opens(Gate.UKAKU_KAKUJA).side(PACK)));
        add(new Quest("sisters_shadow", Line.PATH, KagunePath.UKAKU, "Touka's Shadow",
            "Your sister never needed anyone. Neither do you. Survive like she did.", 700, null, "ukaku_kakuja",
            List.of(of("Live through the brink", Signal.BRINK, 5), of("Kill in a rampage of 4", Signal.RAMPAGE, 4)),
            Reward.mastery(50).opens(Gate.UKAKU_KAKUJA).side(ANTEIKU)));
        add(new Quest("rabbit_unbound", Line.PATH, KagunePath.UKAKU, "Rabbit Unbound",
            "The first kakuja was armour. The last one is a storm.", 950, null, null,
            List.of(of("Eat Ghoul Meat", Signal.EAT, 15, Tags.GHOUL_MEAT), of("Kill a boss in a kakuja", Signal.KILL, 1, Tags.BOSS, Tags.KAKUJA)),
            Reward.mastery(80).opens(Gate.UKAKU_FINAL_KAKUJA)));
        add(new Quest("storm", Line.PATH, KagunePath.UKAKU, "Eye of the Storm",
            "A hundred shots, a thousand feathers, ten minutes of fire.", 950, null, null,
            List.of(of("Kill with shots", Signal.KILL, 100, Tags.RANGED), of("Seconds in a kakuja", Signal.TIME, 600, Tags.KAKUJA)),
            Reward.mastery(80).opens(Gate.UKAKU_FINAL_KAKUJA)));

        // ------------------------------------------------------------------ Graft DNA: the Washuu experiment
        add(new Quest("rejection", Line.PATH, KagunePath.GRAFT, "Rejection",
            "Rize's kakuhou is fighting your body for control. Feed it, or let it kill you until it learns.", 200, null, null,
            List.of(of("Die", Signal.DIE, 2), of("Eat raw flesh", Signal.EAT, 10, Tags.FLESH)), Reward.mastery(20).opens(Gate.GRAFT_CENTIPEDE)));
        add(new Quest("clowns_game", Line.PATH, KagunePath.GRAFT, "The Clowns' Game",
            "Play with them in the dark, without the kagune. Clowns laugh at fair fights.", 200, null, null,
            List.of(of("Kill at night, eyes open", Signal.KILL, 15, Tags.NIGHT, Tags.EYES)), Reward.mastery(20).opens(Gate.GRAFT_CENTIPEDE)));
        add(new Quest("four_thorns", Line.PATH, KagunePath.GRAFT, "Four Thorns",
            "The centipede is a draft. Wear it out, and the thorns come next.", 400, null, null,
            List.of(of("Kill as the Centipede Rinkaku", Signal.KILL, 20, Tags.form("centipede")), of("Deal damage, kagune out", Signal.DEAL, 400,
                Tags.KAGUNE)),
            Reward.mastery(30).opens(Gate.GRAFT_THORNS)));
        add(new Quest("appetite", Line.PATH, KagunePath.GRAFT, "Rize's Appetite",
            "She was a binge eater. So are you now.", 450, null, null,
            List.of(of("Eat Ghoul Meat", Signal.EAT, 12, Tags.GHOUL_MEAT)), Reward.mastery(20).opens(Gate.GRAFT_APPETITE)));
        add(new Quest("not_that_easily", Line.PATH, KagunePath.GRAFT, "You Thought You'd Get Rid of Me?",
            "Die, and die again, until dying stops working.", 450, null, null,
            List.of(of("Die", Signal.DIE, 5), of("Live through the brink", Signal.BRINK, 3)), Reward.mastery(20).opens(Gate.GRAFT_NOT_THAT_EASILY)));
        add(new Quest("lets_play_house", Line.PATH, KagunePath.GRAFT, "Let's Play House",
            "Bring the family home. Eat the ones who won't come.", 650, null, null,
            List.of(of("Eat Ghoul Meat", Signal.EAT, 5, Tags.GHOUL_MEAT), of("Kill ghouls", Signal.KILL, 5, Tags.GHOUL)),
            Reward.mastery(40).opens(Gate.GRAFT_DRAGON)));
        add(new Quest("rizes_nursery", Line.PATH, KagunePath.GRAFT, "Rize's Nursery",
            "Grow the kagune out until the eyes open on it.", 650, null, null,
            List.of(of("Live through the brink", Signal.BRINK, 5), of("Seconds with the kagune out", Signal.TIME, 900, Tags.KAGUNE)),
            Reward.mastery(40).opens(Gate.GRAFT_DRAGON)));
        add(new Quest("dragon", Line.PATH, KagunePath.GRAFT, "The Dragon",
            "Washuu built you to become something that eats cities. Start with something big.", 850, null, null,
            List.of(of("Eat Ghoul Meat", Signal.EAT, 15, Tags.GHOUL_MEAT), of("Kill a boss", Signal.KILL, 1, Tags.BOSS)),
            Reward.mastery(70).opens(Gate.GRAFT_KAKUJA)));
        add(new Quest("washuu_heir", Line.PATH, KagunePath.GRAFT, "Washuu's Heir",
            "The clan's blood, the clan's methods. Cull the ghouls in your way.", 850, null, null,
            List.of(of("Kill ghouls as the Many-Eyed Kagune", Signal.KILL, 10, Tags.GHOUL, Tags.form("dragon")),
                of("Kill in a rampage of 6", Signal.RAMPAGE, 6)),
            Reward.mastery(70).opens(Gate.GRAFT_KAKUJA)));
    }

    private QuestBook() {
    }

    private static void add(Quest quest) {
        if (QUESTS.put(quest.id(), quest) != null) {
            throw new IllegalStateException("two quests named " + quest.id());
        }
    }

    /** Every quest in the book, in its order. */
    public static List<Quest> all() {
        return List.copyOf(QUESTS.values());
    }

    public static Quest get(String id) {
        return QUESTS.get(id);
    }

    /** The quests that open {@code gate} (any one is enough). */
    public static List<Quest> openers(Gate gate) {
        List<Quest> out = new ArrayList<>();
        for (Quest quest : QUESTS.values()) {
            if (quest.opens(gate)) {
                out.add(quest);
            }
        }
        return out;
    }

    /** "Rize's Gift or Learning to Say No": the quests that open {@code gate}, by title. */
    public static String openersText(Gate gate) {
        List<Quest> openers = openers(gate);
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < openers.size(); i++) {
            if (i > 0) {
                text.append(i == openers.size() - 1 ? " or " : ", ");
            }
            text.append(openers.get(i).title());
        }
        return text.toString();
    }
}
