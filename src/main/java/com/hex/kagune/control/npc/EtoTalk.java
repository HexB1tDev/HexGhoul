package com.hex.kagune.control.npc;

import java.util.ArrayList;
import java.util.List;

/**
 * 1.3.0: talking with Eto (pure, tested), on the ghoul NPCs' talk screen ({@link NpcTalk.Choice}, the new ones at its end). She talks
 * to anyone, ghoul or human, who has not made an enemy of her: chats, takes a book or a flower, teaches a ghoul her lessons
 * ({@link EtoRules.Lesson}), at her highest regard sends them on her errands ({@link EtoRules.Step}), and once they are dating: kisses,
 * hugs, and the surgery that makes them like her. Every answer is a {@link Reply}: her words, what it does to her regard, what the server
 * then does ({@link Effect}).
 *
 * <p>1.3.2: a conversation is pages ({@link Page}). The first ({@link #main}) depends on who the player is to her ({@link EtoVoice}): her
 * partner gets her sweet side (its own page, {@link #sweet}: kisses, holding, the ring, the night), someone else's partner gets lessons
 * and a cold shoulder and nothing more, anyone else what they've earned. "Talk a while" opens the topics ({@link EtoTopics}, {@link #talk}),
 * each with her answer and what can be said back ({@link #topic}); gifts ({@link #gifts}) are a page of their own when there are many.
 */
public final class EtoTalk {
    /** What the server does after an answer. */
    public enum Effect {
        NONE, CLOSE, BOOK, FLOWER, TEACH, BEGIN, DATE, KISS, HUG, OFFER, SURGERY, ANGER, FOLLOW, STAY,
        // 1.3.2
        TALK, SWEET, MAIN, GIFTS, PUPPY, PROPOSE, NIGHT, PACT, MEAT, LETTER, CLING, FORGIVE, TRIAL, HAND
    }

    /**
     * Everything a page depends on: her regard for them and her bond's flags ({@code bond}: the step and count of her errands are read
     * from it), a ghoul or not, chatted and gifted today, a book or a flower in hand, the lesson she would teach next and whether the one
     * before it is practised (its quest done), whether she has already made them like her, a seed for her lines. 1.3.2: who they are to
     * her ({@code stage}; {@code claimedBy} the name of the player she belongs to, if not them), whether she follows them, the day's
     * ticks, her mood, days together and days since they were last with her, what they bring (a baby animal on a lead: its kind; a
     * ring, ghoul meat, a letter), a human's pact with her and how far along, her last lesson done (a ghoul's), the night already spent
     * together, their name, whether they are married to another, and whether she could date them at all.
     */
    public record Context(int regard, int flags, int count, boolean ghoul, boolean chattedToday, boolean giftedToday, boolean book, boolean flower,
                          EtoRules.Lesson next, boolean beforeDone, boolean granted, int seed,
                          Romance.Stage stage, String claimedBy, boolean following, long dayTime, EtoMood mood, long daysTogether, long daysAway,
                          String puppy, boolean ring, boolean meat, boolean letter, EtoRules.Pact pact, int pactCount, boolean trialDone,
                          boolean nightUsed, String name, boolean spouseElsewhere) {
        /** As 1.3.0 made one: dating read from the bond's flag, daytime, nothing in hand. */
        public Context(int regard, int flags, int count, boolean ghoul, boolean chattedToday, boolean giftedToday, boolean book, boolean flower,
                       EtoRules.Lesson next, boolean beforeDone, boolean granted, int seed) {
            this(regard, flags, count, ghoul, chattedToday, giftedToday, book, flower, next, beforeDone, granted, seed,
                (flags & EtoRules.DATING) != 0 ? Romance.Stage.DATING : Romance.Stage.NONE, null, false, 6000L, EtoMood.HAPPY, 0L, 0L, null, false, false,
                false, EtoRules.Pact.NONE, 0, false, false, "you", false);
        }

        boolean has(int flag) {
            return (this.flags & flag) != 0;
        }

        EtoRules.Step step() {
            return EtoRules.step(new com.hex.kagune.control.quest.QuestLog.Bond(this.regard, 0L, this.flags, this.count));
        }

        public boolean dating() {
            return this.stage.together();
        }

        public boolean married() {
            return this.stage == Romance.Stage.MARRIED;
        }

        /** She belongs to another player. */
        public boolean claimed() {
            return this.claimedBy != null;
        }

        public boolean night() {
            return EtoRules.night(this.dayTime);
        }

        /** She could still begin dating them: free, and they are married to no one. */
        public boolean mayDate() {
            return Romance.mayDate(this.claimed(), this.spouseElsewhere);
        }

        /** The same, with another seed (another of her lines). */
        public Context reseed(int seed) {
            return new Context(this.regard, this.flags, this.count, this.ghoul, this.chattedToday, this.giftedToday, this.book, this.flower, this.next,
                this.beforeDone, this.granted, seed, this.stage, this.claimedBy, this.following, this.dayTime, this.mood, this.daysTogether, this.daysAway,
                this.puppy, this.ring, this.meat, this.letter, this.pact, this.pactCount, this.trialDone, this.nightUsed, this.name, this.spouseElsewhere);
        }
    }

    /** What an answer brings: her words, the change in her regard, what happens, what she does (1.3.2). */
    public record Reply(String line, int regard, Effect effect, EtoGesture gesture) {
        public Reply(String line, int regard, Effect effect) {
            this(line, regard, effect, EtoGesture.NONE);
        }
    }

    /** One answer on a page: the choice sent, its words ("" for its own), and what it means to the server (a topic, a reply...). */
    public record Option(NpcTalk.Choice choice, String label, String key) {
        static Option of(NpcTalk.Choice choice) {
            return new Option(choice, "", "");
        }
    }

    /** A page: what she says, and the answers. */
    public record Page(String line, List<Option> options) {
        public List<NpcTalk.Choice> choices() {
            List<NpcTalk.Choice> out = new ArrayList<>();
            for (Option option : this.options) {
                out.add(option.choice());
            }
            return out;
        }
    }

    private EtoTalk() {
    }

    /** True if she talks to them at all (an enemy of hers is fought, not talked to). */
    public static boolean talks(int regard) {
        return regard > NpcBonds.ENEMY;
    }

    // ---------------------------------------------------------------- the first page

    /** The answers she allows now, on the first page. */
    public static List<NpcTalk.Choice> choices(Context c) {
        return main(c, "").choices();
    }

    /** The first page: {@code line} hers, and what can be said, by who they are to her. */
    public static Page main(Context c, String line) {
        List<Option> out = new ArrayList<>();
        EtoVoice voice = EtoVoice.of(c);
        if (!c.has(NpcBonds.ASKED)) {
            out.add(Option.of(NpcTalk.Choice.WHO));
        }
        out.add(new Option(NpcTalk.Choice.CHAT, voice.together() ? "Talk to me." : "", ""));
        if (voice == EtoVoice.CLAIMED) {
            // Someone else's: lessons, a word, and the door. No gifts, no sweetness.
            student(c, out);
            out.add(Option.of(NpcTalk.Choice.INSULT));
            out.add(Option.of(NpcTalk.Choice.LEAVE));
            return new Page(line, out);
        }
        if (voice.together()) {
            out.add(new Option(NpcTalk.Choice.SWEET, c.married() ? "Be sweet to your wife." : "", ""));
        }
        List<Option> gifts = giftOptions(c);
        if (gifts.size() > 2) {
            out.add(new Option(NpcTalk.Choice.SAY_8, "I brought you something...", "gifts"));
        } else {
            out.addAll(gifts);
        }
        student(c, out);
        EtoRules.Step step = c.step();
        if (!voice.together() && c.mayDate()) {
            if (step != EtoRules.Step.NONE && step != EtoRules.Step.DATING || step == EtoRules.Step.NONE && c.regard() >= NpcBonds.MOST) {
                out.add(Option.of(NpcTalk.Choice.FAVOUR));
            }
            if (step == EtoRules.Step.ASK) {
                out.add(Option.of(NpcTalk.Choice.ASK_OUT));
            }
        }
        if (voice.together() || c.regard() >= NpcBonds.FRIENDLY) {
            out.add(Option.of(c.following() ? NpcTalk.Choice.STAY : NpcTalk.Choice.FOLLOW));
        }
        out.add(Option.of(voice.together() ? NpcTalk.Choice.TEASE : NpcTalk.Choice.INSULT));
        out.add(Option.of(NpcTalk.Choice.LEAVE));
        return new Page(line, out);
    }

    /** Her lessons (a ghoul's), a human's pact, and her last step: the one eye. Anyone she talks to may learn. */
    private static void student(Context c, List<Option> out) {
        if (c.granted()) {
            if (c.ghoul() && c.next() != null) {
                out.add(Option.of(NpcTalk.Choice.LESSON));
            }
            return;
        }
        if (c.ghoul()) {
            if (c.next() != null) {
                out.add(Option.of(NpcTalk.Choice.LESSON));
            } else if (c.trialDone()) {
                out.add(Option.of(NpcTalk.Choice.TRIAL));
            }
        } else if (c.pact() == EtoRules.Pact.READY) {
            out.add(Option.of(NpcTalk.Choice.TRIAL));
        } else {
            out.add(new Option(NpcTalk.Choice.LESSON, c.pact() == EtoRules.Pact.NONE ? "Teach me to be like you." : "How am I doing?", ""));
        }
    }

    private static List<Option> giftOptions(Context c) {
        List<Option> out = new ArrayList<>();
        if (c.puppy() != null) {
            out.add(Option.of(NpcTalk.Choice.PUPPY));
        }
        if (c.book()) {
            out.add(Option.of(NpcTalk.Choice.BOOK));
        }
        if (c.flower()) {
            out.add(Option.of(NpcTalk.Choice.FLOWER));
        }
        if (c.meat()) {
            out.add(new Option(NpcTalk.Choice.GIFT, "I brought you dinner. (the Ghoul Meat in your hand)", ""));
        }
        if (c.letter()) {
            out.add(Option.of(NpcTalk.Choice.LETTER));
        }
        return out;
    }

    /** The gifts page: everything they could give her. */
    public static Page gifts(Context c, String line) {
        List<Option> out = new ArrayList<>(giftOptions(c));
        out.add(Option.of(NpcTalk.Choice.BACK));
        return new Page(line, out);
    }

    /** The answers to her offer of the surgery. */
    public static List<NpcTalk.Choice> offer() {
        return List.of(NpcTalk.Choice.ACCEPT, NpcTalk.Choice.DECLINE);
    }

    /** Her first words. */
    public static String greeting(Context c) {
        return EtoLines.greeting(c);
    }

    // ---------------------------------------------------------------- talking a while

    /** The topics page: what they could bring up (at most {@link NpcTalk.Choice#SAYS}'s worth), a different handful each time. */
    public static Page talk(Context c, String line) {
        List<EtoTopics.Topic> open = EtoTopics.open(c);
        List<Option> out = new ArrayList<>();
        int room = NpcTalk.Choice.SAYS.length;
        int start = open.isEmpty() ? 0 : Math.floorMod(c.seed(), open.size());
        for (int i = 0; i < Math.min(room, open.size()); i++) {
            EtoTopics.Topic topic = open.get((start + i) % open.size());
            out.add(new Option(NpcTalk.Choice.SAYS[i], topic.prompt().apply(c), "topic:" + topic.id()));
        }
        out.add(Option.of(NpcTalk.Choice.BACK));
        return new Page(line, out);
    }

    /** A topic brought up: her answer, and what can be said back. */
    public static Page topic(Context c, EtoTopics.Topic topic) {
        List<Option> out = new ArrayList<>();
        List<EtoTopics.Follow> follows = topic.open(c);
        for (int i = 0; i < Math.min(NpcTalk.Choice.SAYS.length, follows.size()); i++) {
            out.add(new Option(NpcTalk.Choice.SAYS[i], follows.get(i).prompt(), "follow:" + topic.id() + ":" + i));
        }
        out.add(Option.of(NpcTalk.Choice.BACK));
        return new Page(EtoLines.fill(c, topic.answer().apply(c)), out);
    }

    /** What was said back on {@code topic} ({@code index} among its open replies): her reply, names put in. */
    public static Reply follow(Context c, EtoTopics.Topic topic, int index) {
        List<EtoTopics.Follow> follows = topic.open(c);
        if (index < 0 || index >= follows.size()) {
            return new Reply("Hm? Say that again.", 0, Effect.NONE);
        }
        Reply reply = follows.get(index).reply().apply(c);
        return new Reply(EtoLines.fill(c, reply.line()), reply.regard(), reply.effect(), reply.gesture());
    }

    // ---------------------------------------------------------------- being sweet (her partner's)

    /** What sweet things can be said, picked by {@code seed} (three a time). */
    public static final String[][] SWEET_SAYS = {
        {"You're beautiful.", "hold"}, {"I love you.", "love"}, {"I missed you.", "missed"}, {"Dance with me.", "dance"}, {"Read to me.", "read"},
        {"Play with your hair.", "hair"}, {"Lean on me.", "lean"}, {"You smell nice.", "smell"}};

    /** The sweet page: kisses, a hug, holding her, sweet things, the night, the ring, the surgery. */
    public static Page sweet(Context c, String line) {
        List<Option> out = new ArrayList<>();
        out.add(Option.of(NpcTalk.Choice.KISS));
        out.add(Option.of(NpcTalk.Choice.HUG));
        out.add(new Option(NpcTalk.Choice.SAY_1, "Hold me.", "sweet:cling"));
        out.add(new Option(NpcTalk.Choice.SAY_2, "Hold my hand.", "sweet:hand"));
        int start = Math.floorMod(c.seed(), SWEET_SAYS.length);
        for (int i = 0; i < 3; i++) {
            String[] say = SWEET_SAYS[(start + i) % SWEET_SAYS.length];
            out.add(new Option(NpcTalk.Choice.SAYS[i + 2], say[0], "sweet:" + say[1]));
        }
        if (c.night()) {
            out.add(Option.of(NpcTalk.Choice.NIGHT));
        }
        if (c.ring() && !c.married()) {
            out.add(Option.of(NpcTalk.Choice.PROPOSE));
        }
        if (!c.granted()) {
            out.add(Option.of(NpcTalk.Choice.SURGERY));
        }
        out.add(Option.of(NpcTalk.Choice.BACK));
        return new Page(line, out);
    }

    /** Her answer to a sweet thing ({@code what}: its key's word). */
    public static Reply sweetly(Context c, String what) {
        int seed = c.seed();
        boolean married = c.married();
        Reply reply = switch (what) {
            case "cling" -> new Reply(EtoLines.clung(seed), 2, Effect.CLING, EtoGesture.NONE);
            case "hand" -> new Reply(EtoLines.one(seed, "My hand? ...Fine. Come here.", "Finally. I thought you'd never ask."), 1, Effect.HAND, EtoGesture.NONE);
            case "hold" -> new Reply(EtoLines.one(seed, "...Say it slower.", "I know. Say it again anyway.", "Stop it. ...Don't stop it."), 2, Effect.NONE,
                EtoGesture.HAIR);
            case "love" -> married ? new Reply(EtoLines.one(seed, "I know. I love you more. Don't argue, I've read more books.",
                "I love you too. Every day. Even the days you're annoying. Especially those."), 3, Effect.NONE, EtoGesture.HUG)
                : new Reply(EtoLines.one(seed, "...You can't just— ...I love you too. Idiot.", "Shh. Say it again. Quieter. Just for me."), 3, Effect.NONE,
                EtoGesture.HAIR);
            case "missed" -> new Reply(EtoLines.one(seed, "Good. Miss me more. Then come back faster.", "I missed you too. I read the same book four times."),
                2, Effect.CLING, EtoGesture.CLING);
            case "dance" -> new Reply(EtoLines.one(seed, "There's no music. ...Hum, then.", "In a library? ...Fine. Don't step on my feet."), 2, Effect.NONE,
                EtoGesture.DANCE);
            case "read" -> new Reply(EtoLines.one(seed, "Sit. '...And the girl in the tower opened the window, and the dark came in like a friend.' "
                + "That's my favourite line. Don't tell anyone.", "'Once there was a monster who fell in love, and it was terrible for everyone, '"
                + "'and wonderful for the monster.' ...I wrote that one."), 2, Effect.NONE, EtoGesture.READ);
            case "hair" -> new Reply(EtoLines.one(seed, "Mm. Gently. ...Okay, you can keep doing that forever.", "You're messing it up. Keep messing it up."),
                2, Effect.NONE, EtoGesture.HAIR);
            case "lean" -> new Reply(EtoLines.one(seed, "On you? ...Fine. You're comfier than you look.", "If I fall asleep, carry me. Don't drop me."),
                2, Effect.CLING, EtoGesture.CLING);
            case "smell" -> new Reply(EtoLines.one(seed, "It's old books and a little bit of blood. Thank you.", "You'd know. You're always sniffing me. ...I like it."),
                1, Effect.NONE, EtoGesture.GIGGLE);
            default -> new Reply("Hm?", 0, Effect.NONE);
        };
        return new Reply(EtoLines.fill(c, reply.line()), reply.regard(), reply.effect(), reply.gesture());
    }

    // ---------------------------------------------------------------- answers

    /** What she says back to {@code choice}. */
    public static Reply answer(Context c, NpcTalk.Choice choice) {
        EtoVoice voice = EtoVoice.of(c);
        int seed = c.seed();
        return switch (choice) {
            case WHO -> new Reply(voice == EtoVoice.CLAIMED ? EtoLines.fill(c, "Eto. I live here. I'm {partner}'s. That's everything you need to know.")
                : "Eto. I live here, with the books. I write a little too, under another name. Don't ask which, you'd only want an autograph.",
                c.has(NpcBonds.ASKED) ? 0 : EtoRules.WHO, Effect.NONE);
            case CHAT -> new Reply(EtoLines.fill(c, voice.pick(seed,
                new String[] {"Talk? Keep it short. {partner} gets the long conversations."},
                new String[] {"Talk? Fine. About what?", "You want to talk? To me? Brave. Go on."},
                new String[] {"Sure! Pull up a chair. Not that one, it bites."},
                new String[] {"Always. What's on your mind?", "Talk to me. Tell me something I haven't read."},
                new String[] {"About anything. Your voice is nice. Keep using it.", "Yes. Sit. Closer. Now talk."},
                new String[] {"Talk to me, love. About anything. About nothing.", "Mm. Come here and talk, then."})),
                c.chattedToday() ? 0 : EtoRules.CHAT, Effect.TALK);
            case BOOK -> book(c);
            case FLOWER -> flower(c);
            case GIFT -> c.meat() ? new Reply(EtoLines.meat(c), c.giftedToday() ? 0 : EtoRules.GIFT, Effect.MEAT, EtoGesture.LICK)
                : new Reply("Dinner? Where?", 0, Effect.NONE);
            case PUPPY -> c.puppy() == null ? new Reply("A what? Where?", 0, Effect.NONE)
                : new Reply(EtoLines.puppy(c, c.puppy(), false), 8, Effect.PUPPY, EtoGesture.PET);
            case LETTER -> c.letter() ? new Reply(EtoLines.letter(c, false), c.giftedToday() ? 0 : 4, Effect.LETTER, EtoGesture.READ)
                : new Reply("A letter? Where?", 0, Effect.NONE);
            case LESSON -> lesson(c);
            case TRIAL -> trial(c);
            case FAVOUR -> favour(c);
            case ASK_OUT -> !c.mayDate() ? new Reply(c.claimed() ? EtoLines.fill(c, "I'm {partner}'s. Ask someone else.")
                : "Go out with you? You're married. Go home.", 0, Effect.NONE)
                : c.step() == EtoRules.Step.ASK ? new Reply("...Took you long enough. Yes. Obviously, yes.", 0, Effect.DATE, EtoGesture.GIGGLE)
                : new Reply("Ask me what, exactly?", 0, Effect.NONE);
            case KISS -> c.dating() ? new Reply(EtoLines.one(seed, "Mm. Come here, then.", "Finally. I was about to ask myself."), 0, Effect.KISS)
                : new Reply("Bold. Buy me a book first.", 0, Effect.NONE);
            case HUG -> c.dating() ? new Reply(EtoLines.hugged(seed), 0, Effect.HUG) : new Reply("Hands where I can see them.", 0, Effect.NONE);
            case SWEET -> c.dating() ? new Reply(EtoLines.fill(c, EtoLines.one(seed, "Oh? Go on, then. Be sweet.", "Mm. I'm listening.",
                "Careful. I might start expecting this.")), 0, Effect.SWEET) : new Reply("Sweet? To me? Earn it.", 0, Effect.NONE);
            case NIGHT -> !c.dating() ? new Reply("Excuse me?", -2, Effect.NONE)
                : new Reply(EtoLines.nightAsk(c), 0, c.night() && !c.nightUsed() ? Effect.NIGHT : Effect.NONE);
            case PROPOSE -> new Reply("", 0, Effect.PROPOSE);
            case SURGERY -> surgery(c);
            case FOLLOW -> new Reply(EtoLines.follow(c), 0, Effect.FOLLOW);
            case STAY -> new Reply(EtoLines.stay(c), 0, Effect.STAY);
            case ACCEPT -> new Reply("Shh. Close your eyes. I'll be right here when you wake up.", 0, Effect.SURGERY);
            case DECLINE -> new Reply(voice.together() ? "Another time, then. The offer stands. It always will." : "Then stop wasting my time.", 0, Effect.NONE);
            case TEASE -> new Reply(EtoLines.one(seed, "I'm not grumpy. I'm literary.", "Say that again and I'll bite you. ...That wasn't a no.",
                "You're lucky you're cute.", "Oh, you think you're funny. You're a little funny."), 1, Effect.NONE, EtoGesture.POUT);
            case INSULT -> NpcBonds.clamp(c.regard() + EtoRules.INSULT) <= NpcBonds.ENEMY
                ? new Reply("Oh, you want to play rough? Fine. Let's play.", EtoRules.INSULT, Effect.ANGER)
                : new Reply(voice == EtoVoice.CLAIMED ? EtoLines.fill(c, "Charming. {partner} will hear about this. Out.")
                : "Wow. Rude. Get out of my library before I shelve you.", EtoRules.INSULT, Effect.CLOSE);
            case BACK -> new Reply(EtoLines.fill(c, voice.pick(seed, new String[] {"Anything else? Make it quick."}, new String[] {"Anything else?"},
                new String[] {"What else?"}, null, new String[] {"What else, you?", "Mm? What else?"})), 0, Effect.MAIN);
            case LEAVE -> new Reply("", 0, Effect.CLOSE);
            default -> new Reply("Hm? Say that again.", 0, Effect.NONE);
        };
    }

    private static Reply book(Context c) {
        if (!c.book()) {
            return new Reply("A book? Where?", 0, Effect.NONE);
        }
        if (c.step() == EtoRules.Step.BOOKS) {
            int now = c.count() + 1;
            int regard = c.giftedToday() ? 0 : EtoRules.GIFT;
            return now >= EtoRules.Step.BOOKS.need
                ? new Reply("Three! Oh, these are perfect. Now, something less bookish: ask me again.", regard, Effect.BOOK, EtoGesture.READ)
                : new Reply("One more for the pile. That's " + now + " of three.", regard, Effect.BOOK, EtoGesture.READ);
        }
        if (c.giftedToday()) {
            return new Reply("You're spoiling me. One a day, or I'll never finish them.", 0, Effect.NONE);
        }
        return new Reply(EtoVoice.of(c).together() ? "Another one? You know exactly how to keep me. I'll read it to you later."
            : "A book! For me? You really do know the way to a girl's heart.", EtoRules.GIFT, Effect.BOOK, EtoGesture.READ);
    }

    private static Reply flower(Context c) {
        if (!c.flower()) {
            return new Reply("A flower? Where?", 0, Effect.NONE);
        }
        if (c.step() == EtoRules.Step.FLOWER) {
            return new Reply("...You picked this yourself. I can tell. Thank you. Was there something you wanted to ask me?", 0, Effect.FLOWER, EtoGesture.HAIR);
        }
        if (c.giftedToday()) {
            return new Reply("I've still got the last one. Spoil me tomorrow.", 0, Effect.NONE);
        }
        return new Reply(EtoVoice.of(c).together() ? "For me? Put it in my hair. ...Gently. There. How do I look?"
            : "For me? I'll press it in my favourite book.", EtoRules.GIFT, Effect.FLOWER, EtoGesture.HAIR);
    }

    private static Reply lesson(Context c) {
        boolean cold = EtoVoice.of(c) == EtoVoice.CLAIMED;
        if (!c.ghoul()) {
            return pact(c);
        }
        EtoRules.Refusal refusal = EtoRules.refusal(c.next(), c.ghoul(), c.beforeDone());
        if (refusal != null) {
            return new Reply(switch (refusal) {
                case ALL_TAUGHT -> "I've taught you everything I know. The rest you learn by bleeding.";
                case HUMAN -> "Teach a human to fight like a ghoul? Cute. No.";
                default -> cold ? "Practise what I showed you first. I'm not repeating myself." : "Practise what I showed you first. Then come back and show off.";
            }, 0, Effect.NONE);
        }
        String line = switch (c.next()) {
            case KICK -> cold ? "Watch my feet, not my face. Strike, strike, flip away, then all your weight through one kick. Go. (Her lesson is in your journal.)"
                : "Watch my feet. Strike, strike, flip away, then all your weight through one kick. Go on, try it. (Her lesson is in your journal.)";
            case SOAR -> "The ground is for people who are tired. Flip, and keep going up. Kill them before you land. (Her lesson is in your journal.)";
            case WITNESS -> "One punch is rude. Wind it all the way back and make them watch. Then do it again. (Her lesson is in your journal.)";
            default -> cold ? "Last one. Kill in the dark with your eye open until it feels like home. Bleed until it stops scaring you. Then I'll make you "
                + "one-eyed, and we never speak again. (Her lesson is in your journal.)"
                : "Last one, and it's the real one. Kill in the dark with your eye open until it feels like home. Bleed until it stops scaring you. "
                + "Then come back, and lie down. (Her lesson is in your journal.)";
        };
        return new Reply(line, 0, Effect.TEACH);
    }

    /** A human asks to learn: the pact, its price and how far along they are. */
    private static Reply pact(Context c) {
        boolean warm = EtoVoice.of(c).together();
        return switch (c.pact()) {
            case NONE -> new Reply("Teach a human? There's one lesson for humans, and it's 'stop being one'. Fine. The price: kill twelve things at night. "
                + "Then kill one of us. Then bring me its meat. Then lie down on my table.", 0, Effect.PACT);
            case HUNT -> new Reply(c.pactCount() + " of twelve. The night isn't going to hunt itself.", 0, Effect.NONE);
            case GHOUL -> new Reply("Now one of us. A ghoul. Any ghoul. They'll fight back. Good.", 0, Effect.NONE);
            case MEAT -> new Reply("Bring me what's left of one. Ghoul meat. Don't eat it, you'd throw up on my floor.", 0, Effect.NONE);
            default -> new Reply(warm ? "You did all of it. For me. ...Come here when you're ready, and I'll make you like me."
                : "You actually did it. Come back when you're ready to stop being human.", 0, Effect.NONE);
        };
    }

    /** Her last lesson done (or a human's pact): she offers the surgery, warm or cold. */
    private static Reply trial(Context c) {
        if (c.granted()) {
            return new Reply("You already are. Look in a mirror at night.", 0, Effect.NONE);
        }
        boolean ready = c.ghoul() ? c.trialDone() : c.pact() == EtoRules.Pact.READY;
        if (!ready) {
            return new Reply("Not yet. You know what's left.", 0, Effect.NONE);
        }
        return new Reply(EtoLines.surgeryOffer(c), 0, Effect.OFFER);
    }

    private static Reply favour(Context c) {
        if (!c.mayDate()) {
            return new Reply(c.claimed() ? EtoLines.fill(c, "I don't need anything from you. {partner} takes care of me.") : "Nothing. Go home to your spouse.",
                0, Effect.NONE);
        }
        return switch (c.step()) {
            case NONE -> c.regard() >= NpcBonds.MOST
                ? new Reply("Since you asked so nicely... Bring me three books I haven't read. Then I'll think of something else.", 0, Effect.BEGIN)
                : new Reply("Nothing I'd ask of you. Yet.", 0, Effect.NONE);
            case BOOKS -> new Reply("Books. Three of them, ones I haven't read. You've brought " + c.count() + ".", 0, Effect.NONE);
            case HUNT -> new Reply("The night's crawling with ugly things. Kill ten of them for me, after dark. " + c.count() + " so far.", 0, Effect.NONE);
            case FLOWER -> new Reply("Last thing. A flower. Pick it yourself, I'll know.", 0, Effect.NONE);
            case ASK -> new Reply("That's everything. ...Was there something you wanted to ask me?", 0, Effect.NONE);
            case DATING -> new Reply("You don't have to do anything for me now, silly. Just come back.", 0, Effect.NONE);
        };
    }

    private static Reply surgery(Context c) {
        if (!c.dating()) {
            return new Reply("Like me? You don't even know what I am.", 0, Effect.NONE);
        }
        if (c.granted()) {
            return new Reply("You already are, silly. Look in a mirror at night.", 0, Effect.NONE);
        }
        return new Reply(EtoLines.surgeryOffer(c), 0, Effect.OFFER);
    }

    static String pick(String[] lines, int seed) {
        return lines[Math.floorMod(seed, lines.length)];
    }
}
