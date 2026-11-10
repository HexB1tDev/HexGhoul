package com.hex.kagune.control.npc;

import java.util.List;

/**
 * 4.1.5: the General's words (pure, tested). The owner: "Attitude: Tough, softens up when romanced, embarrased easily, proud of the CCG,
 * has to be convinced to love a ghoul, Only interested in woman players". The CCG's General, a human, thirty years in the bureau: clipped,
 * dry, sure of himself and of the people under him. A stranger gets orders; a friend gets the same orders with a chair pulled out first;
 * the woman he loves gets a man who goes red to the ears at a kind word and pretends to read a report. A ghoul he will talk to, even like;
 * to love one he has to be convinced ({@link #convinced}): she has to keep coming back until he believes what he sees over what he was
 * taught.
 */
final class GeneralScript extends CompanionScript {
    /** How well he must think of a ghoul before he will be asked out by one (a human needs only her courtship done). */
    static final int CONVINCED = 90;

    /** True if he would say yes to her asking him out: a human; or a ghoul he has come to think this well of. */
    static boolean convinced(EtoTalk.Context c) {
        return !c.ghoul() || c.regard() >= CONVINCED;
    }

    @Override
    protected String key() {
        return "general";
    }

    @Override
    protected String her() {
        return "him";
    }

    @Override
    protected String spouse() {
        return "husband";
    }

    @Override
    protected String errandLabel() {
        return "I brought you paper. (the paper in your hand)";
    }

    @Override
    protected String flowerLabel() {
        return "I picked you a cornflower. (the one in your hand)";
    }

    @Override
    protected String lessonLabel(EtoTalk.Context c) {
        return "Teach me to fight.";
    }

    /** Only a woman (and him a man: {@code mixed}). */
    @Override
    protected boolean romantic(EtoTalk.Context c) {
        return c.mixed();
    }

    @Override
    public String title(EtoTalk.Context c) {
        return switch (EtoVoice.of(c)) {
            case WIFE -> "Your husband (off duty)";
            case PARTNER -> "Yours (he hasn't told the bureau)";
            case CLAIMED -> "Spoken for";
            case CLOSE -> "Kuroda";
            default -> "The General";
        };
    }

    @Override
    public String greeting(EtoTalk.Context c) {
        int seed = c.seed();
        String line = switch (EtoVoice.of(c)) {
            case CLAIMED -> one(seed, "Bureau business only. Anything else, {partner} hears about it.", "I'm spoken for. State your business.",
                "You again. Keep it professional.");
            case WARY -> c.ghoul() ? one(seed, "A ghoul. In my building. ...You walked in the front door, so I'll hear you out. Briefly.",
                "I know what you are. My people know what you are. Say what you came to say.", "Hands where I can see them. Now talk.")
                : c.night() ? one(seed, "This building is closed to civilians after dark. Who let you up?", "State your name and your business.",
                    "It's late. If this isn't urgent, it can wait for morning.")
                : one(seed, "This floor is restricted. Who are you?", "You have two minutes. Use them.", "Speak. I have reports to sign.",
                    "Civilians wait downstairs. ...Fine. What is it?");
            case FRIENDLY -> one(seed, "You. Back again. ...Sit down, then.", "{name}. You're persistent. I can respect that.",
                "I had a feeling that was your knock.", "Still in one piece. Good. Report.");
            case CLOSE -> one(seed, "{name}. Close the door. Sit. I've had a long day and you're the first good thing in it.",
                "There you are. I told the front desk to stop asking for your papers.", "You look tired. Who's been working you? Sit.",
                "Good timing. I was about to pretend to read this for another hour.");
            case PARTNER -> c.mood() == EtoMood.SULKY ? one(seed, "...I'm busy. ...I'm not busy. I'm annoyed. Sit down anyway.",
                "Oh. You remembered where my office is.")
                : c.daysAway() >= 3 ? one(seed, "Three days. I had a search order half written. ...Don't tell anyone that.",
                    "Where were you? No. Don't answer. Just stand there a moment where I can see you.")
                : one(seed, "{name}. ...Close the door. I don't smile on duty and I'm about to.", "You're here. Good. ...That's all. Good.",
                    "I heard your step on the stairs. I've signed nothing since.", "Come in. Mind the paperwork. ...I missed you. Carry on.");
            case WIFE -> c.pregnant() ? one(seed, "Sit. That's an order. ...I outrank you in exactly one thing and it's this.",
                "You took the stairs? In your state? ...I'm having a chair put on every landing.")
                : one(seed, "Welcome home. ...I've been waiting to say that all day.", "There she is. Come here. Off duty. Finally.",
                    "You're home. Good. The day can end now.", "I made tea. It's bad. Drink it anyway.");
        };
        return EtoLines.fill(c, line);
    }

    @Override
    public EtoTalk.Reply answer(EtoTalk.Context c, NpcTalk.Choice choice) {
        EtoVoice voice = EtoVoice.of(c);
        int seed = c.seed();
        return switch (choice) {
            case WHO -> new EtoTalk.Reply(voice == EtoVoice.CLAIMED ? EtoLines.fill(c, "Kuroda. General, CCG. {partner}'s. In that order of importance, "
                + "reversed.") : "Kuroda. General of this branch of the CCG. Thirty years in the bureau. Every investigator in this building answers "
                + "to me, and I answer for every one of them. That's all you need.", c.has(NpcBonds.ASKED) ? 0 : EtoRules.WHO, EtoTalk.Effect.NONE);
            case CHAT -> new EtoTalk.Reply(EtoLines.fill(c, voice.pick(seed,
                s("Keep it short. {partner} is expecting me."),
                s("Talk. I'll keep signing.", "You have until this report is done."),
                s("Sit. Go on. I'm listening.", "Tell me something that isn't in a file."),
                s("Close the door and talk to me. The reports can wait. They always can.", "Go on. I like how you tell it."),
                s("Talk to me. ...No, keep going. I was listening. I was also looking. Carry on.", "Sit here. Closer. ...That's an order. A soft one."),
                s("Tell me everything. Start with who annoyed you. I have a whole bureau.", "Come and talk. I've been saving the quiet for you."))),
                c.chattedToday() ? 0 : EtoRules.CHAT, EtoTalk.Effect.TALK);
            case BOOK -> errandItem(c, "Paper? Your hands are empty.",
                "That's enough for today. The clerks will think I'm hoarding.", "...You noticed we were short again. You notice everything. Thank you.",
                "Paper. ...We are short, as it happens. Leave it on the desk.", EtoGesture.WRITE);
            case FLOWER -> flowerGiven(c, "A flower? I see no flower.",
                "One a day is plenty. I have a reputation.", "A cornflower. ...The bureau's old colour. You looked that up. ...I'm keeping it in my coat.",
                "A flower. For a general. ...Put it on the desk. Not a word to anyone.", EtoGesture.SHY);
            case GIFT -> c.meat() ? r(c.ghoul() ? "...That is meat. I am not going to ask what kind. Take it away, and we'll both forget this."
                : "Where did you get that? No. Take it out of my building.", -2)
                : r("You're holding nothing.", 0);
            case PUPPY -> c.puppy() == null ? r("A what? There's nothing there.", 0)
                : new EtoTalk.Reply(this.puppy(c, c.puppy(), false), 8, EtoTalk.Effect.PUPPY, EtoGesture.PET);
            case LETTER -> c.letter() ? new EtoTalk.Reply(this.letter(c, false), c.giftedToday() ? 0 : 4, EtoTalk.Effect.LETTER, EtoGesture.READ)
                : r("What letter? You've brought nothing.", 0);
            case LESSON -> lesson(c, LESSONS);
            case FAVOUR -> errand(c, this.key(),
                "A favour? I don't take favours from people I don't know. Come back when I know you.",
                "That's everything I had for you. ...Was there something you wanted to ask me?",
                "I need nothing from you. ...Stay a while. That's what I need.",
                "{partner} sees to that. Thank you.", "You're married. Go home.");
            case ASK_OUT -> !c.mixed() ? r("...No. Not like that. You're good company, and I mean to keep you as that.", 0)
                : !c.mayDate() ? r(c.claimed() ? EtoLines.fill(c, "I'm {partner}'s. That's the end of it.")
                    : "You have someone. I don't take what's another's. Go home.", 0)
                : c.step() != EtoRules.Step.ASK ? r("Ask me what? Speak plainly.", 0)
                : !convinced(c) ? r(one(seed, "...No. Not because of you. Because of what you are, and what I am, and thirty years of funerals. "
                    + "...I didn't say never. Keep coming back. Convince me.",
                    "I've signed the order on a hundred of your kind. And you stand there asking me to dinner. ...Not yet. Ask me again when "
                        + "I've had longer to be wrong about you.",
                    "A general of the CCG and a ghoul. ...I can't. Not today. ...Come back tomorrow. And the day after."), 1, EtoGesture.LOOK)
                : new EtoTalk.Reply(c.ghoul() ? "...Thirty years I've known exactly what a ghoul is. Then you kept walking in here. ...Yes. Damn "
                    + "the bureau, yes. I'd like that. ...Stop smiling, someone will see."
                    : "...You're asking me. To dinner. ...I haven't been asked anything I couldn't answer in thirty years. ...Yes. Yes. "
                    + "Not a word to the front desk.", 0, EtoTalk.Effect.DATE, EtoGesture.SHY);
            case KISS -> c.dating() ? new EtoTalk.Reply(one(seed, "...Here? ...The door's closed. Yes. Come here.", "I was hoping you'd ask. "
                + "I was never going to."), 0, EtoTalk.Effect.KISS) : r("Step back. That's not a request.", 0);
            case HUG -> c.dating() ? new EtoTalk.Reply(this.hugged(seed), 0, EtoTalk.Effect.HUG) : r("I don't do that. Stand easy.", 0);
            case SWEET -> c.dating() ? new EtoTalk.Reply(EtoLines.fill(c, one(seed, "...Careful. I go red and the whole floor finds out.",
                "Go on. ...I'm reading this report. I'm not reading this report.", "Say it quietly. I embarrass easily. You've noticed.")), 0,
                EtoTalk.Effect.SWEET) : r("Flattery. Save it for someone it works on.", 0);
            case NIGHT -> !c.dating() ? r("You forget yourself. Out.", -2) : new EtoTalk.Reply(c.night() && !c.nightUsed()
                ? one(seed, "...Stay. I'll have the guard changed on this floor. ...That's the most romantic thing I know how to do.",
                    "Yes. Stay. I sleep badly. Not when you're here.")
                : one(seed, "It's the middle of the day. I have a bureau to run. ...Tonight.", "Not tonight. ...Stay close anyway."), 0,
                c.night() && !c.nightUsed() ? EtoTalk.Effect.NIGHT : EtoTalk.Effect.NONE);
            case PROPOSE -> new EtoTalk.Reply("", 0, EtoTalk.Effect.PROPOSE);
            case HOME -> c.stage() == Romance.Stage.MARRIED ? new EtoTalk.Reply(one(seed, "Ours. ...I've lived in barracks and offices for thirty "
                + "years. Ours. Yes.", "Home. I'll post no guards. I'll be the guard."), 2, EtoTalk.Effect.HOME)
                : new EtoTalk.Reply("Ours? We aren't married. One thing at a time.", 0, EtoTalk.Effect.NONE);
            case FOLLOW -> new EtoTalk.Reply(voice.together() ? one(seed, "Lead on. I'll take the rear. Old habit.", "With you. The bureau "
                + "can run itself for an afternoon.") : one(seed, "I'll walk with you. I should see the district on foot more.",
                "Very well. Lead. I'll keep up."), 0, EtoTalk.Effect.FOLLOW);
            case STAY -> new EtoTalk.Reply(voice.together() ? one(seed, "I'll hold here. Come back to me.", "Here. Understood. ...Don't be long.")
                : one(seed, "Holding position.", "I'll wait here."), 0, EtoTalk.Effect.STAY);
            case DECLINE -> r("As you like.", 0);
            case TEASE -> new EtoTalk.Reply(one(seed, "...I am not blushing. It's warm in here. Open a window.",
                "Thirty years, and nobody has ever spoken to me like that. ...Do it again.", "Insubordination. ...I'll allow it.",
                "You'll undo my whole reputation. ...Worth it."), 1, EtoTalk.Effect.NONE, EtoGesture.SHY);
            case INSULT -> NpcBonds.clamp(c.regard() + EtoRules.INSULT) <= NpcBonds.ENEMY
                ? new EtoTalk.Reply("That's enough. You're done here. Next time you're in my building, you're a threat.", EtoRules.INSULT,
                EtoTalk.Effect.ANGER, EtoGesture.ANGRY)
                : new EtoTalk.Reply(voice == EtoVoice.CLAIMED ? EtoLines.fill(c, "{partner} will hear every word of that. Out.")
                : one(seed, "I've been insulted by better. Out of my office.", "Say that to my investigators. See how far you get."), EtoRules.INSULT,
                EtoTalk.Effect.CLOSE, EtoGesture.SCOLD);
            case BACK -> new EtoTalk.Reply(EtoLines.fill(c, voice.pick(seed, s("Anything else?"), s("Is that all?"), s("What else?"), null,
                s("Hm? Go on. I'm listening.", "What is it?"))), 0, EtoTalk.Effect.MAIN);
            case LEAVE -> new EtoTalk.Reply("", 0, EtoTalk.Effect.CLOSE);
            default -> r("Speak up.", 0);
        };
    }

    /** How he answers someone asking to be taught: nothing yet to teach (his own way of fighting is not one he hands out lightly). */
    static final LessonWords LESSONS = new LessonWords() {
        @Override
        public String teach(Teaching.Lesson lesson, EtoTalk.Context c) {
            return "Watch. Then do it until your legs give out. Then again. (" + lesson.name() + ")";
        }

        @Override
        public String refuse(Teaching.Refusal refusal, Teaching.View view, EtoTalk.Context c) {
            return switch (refusal) {
                case WEAK -> "You're not ready. Come back stronger. (" + view.next().name() + ": mastery " + view.need() + ")";
                case BEFORE -> "First things first. You don't have " + (view.before() == null ? "the basics" : view.before().displayName()) + ".";
                case ALL_TAUGHT -> "I've taught you what I have. The rest is ten thousand repetitions.";
                default -> "How I fight took me thirty years, and I don't hand it to people I've only just met. ...Ask me another day.";
            };
        }
    };

    @Override
    protected String[][] sweetSays() {
        return new String[][] {{"You look good in that coat.", "coat"}, {"I love you.", "love"}, {"I missed you.", "missed"},
            {"You're strong.", "strong"}, {"I like your beard.", "beard"}, {"Lean on me.", "lean"}, {"You can be off duty with me.", "soft"},
            {"Smile for me.", "smile"}, {"I'm proud of you.", "proud"}, {"Why me?", "why"}};
    }

    @Override
    public EtoTalk.Reply sweetly(EtoTalk.Context c, String what) {
        int seed = c.seed();
        boolean married = c.married();
        EtoTalk.Reply reply = switch (what) {
            case "cling" -> new EtoTalk.Reply(one(seed, "...Like this? ...Yes. Stay there.", "I have you. ...Nobody's watching. I checked."), 2,
                EtoTalk.Effect.CLING, EtoGesture.NONE);
            case "hand" -> new EtoTalk.Reply(one(seed, "My hand. ...In the corridor? ...Yes. Let them look.", "...Here. It's rough. Sorry."), 1,
                EtoTalk.Effect.HAND, EtoGesture.NONE);
            case "coat" -> r(one(seed, "It's regulation. ...You think so? ...I'll wear it more.", "It's just the coat. ...Say it again."), 2,
                EtoGesture.SHY);
            case "love" -> married ? r(one(seed, "...And I you. Every day. I say it badly. I mean it well.", "I love you. There. On the record."),
                3, EtoTalk.Effect.NONE, EtoGesture.HUG)
                : r(one(seed, "...You can't just say that. I have a meeting in ten minutes and now I'll be useless in it. ...I love you too.",
                    "...Say that again when I'm sitting down. ...I love you. There."), 3, EtoTalk.Effect.NONE, EtoGesture.SHY);
            case "missed" -> r(one(seed, "...I counted the days. I'm a general. I count things. ...I missed you.", "Good. I'd hate to have "
                + "been the only one."), 2, EtoTalk.Effect.CLING, EtoGesture.CLING);
            case "strong" -> r(one(seed, "I'm old and I'm stubborn. That passes for strong. ...Thank you.", "Strong enough to stand between you "
                + "and anything. That's what it's for."), 2, EtoGesture.CROSS);
            case "beard" -> r(one(seed, "...It's not regulation. Nobody dares tell me. ...You like it? ...It stays.", "Don't— ...all right. "
                + "You may. Briefly."), 2, EtoGesture.SHY);
            case "lean" -> r(one(seed, "A general doesn't lean. ...One minute.", "...All right. Just until the hour."), 2, EtoTalk.Effect.CLING,
                EtoGesture.CLING);
            case "soft" -> r(one(seed, "...I don't know how to be off duty. Thirty years. ...Show me. Slowly.",
                "If I take the coat off, there's only a tired man under it. ...You'd still want him? ...All right."), 4, EtoTalk.Effect.HUG,
                EtoGesture.HUG);
            case "smile" -> r(one(seed, "I don't— ...there. That's all you get. Don't tell the investigators.", "On duty? ...Damn. You saw that."), 2,
                EtoGesture.SHY);
            case "proud" -> r(one(seed, "...Nobody's said that to me since my first commander. ...Excuse me. Something in my eye.",
                "Of me? ...I'm proud of them. The ones in this building. But I'll keep that. Thank you."), 3, EtoTalk.Effect.NONE, EtoGesture.SHY);
            case "why" -> r(one(seed, c.ghoul() ? "Because you made a liar of everything I was sure of, and I've never been so glad to be wrong."
                : "Because you walked into a general's office and treated me like a man. Nobody's done that in thirty years.",
                "Because you kept coming back. I gave you every reason not to."), 4, EtoTalk.Effect.HUG, EtoGesture.HUG);
            case "baby", "tonight" -> EtoTalk.babyAnswer(c, what, s("...A child. Ours. ...I've buried too many young ones. I'd like to raise one. Tonight.",
                    "A family. At my age. ...Yes. Tonight. I'll be terrible at it and I'll try anyway.",
                    "I never let myself want that. ...I want it. Tonight."),
                s("Another. ...Yes. A house with noise in it. Tonight.", "Two. I've commanded a bureau. I can manage two. ...Tonight.",
                    "...You're certain? You were worn out last time. ...Then yes. Tonight."),
                days -> "So soon? You need rest, and that's an order. ...Ask me in " + EtoTalk.days(days) + ". The answer will be yes.",
                s("I haven't forgotten. I've thought of nothing else. Tonight.", "Tonight. I've cleared my evening.", "Still yes. Tonight."),
                EtoGesture.HUG, EtoGesture.SHY);
            default -> r("Hm?", 0);
        };
        return new EtoTalk.Reply(EtoLines.fill(c, reply.line()), reply.regard(), reply.effect(), reply.gesture());
    }

    @Override
    public String giftsAsked(int seed) {
        return one(seed, "You brought something? ...Let me see.", "For me? ...Show me.");
    }

    @Override
    public String topicsAgain(int seed) {
        return one(seed, "What else?", "Go on.", "Anything further?");
    }

    @Override
    public String busy() {
        return "The General is occupied.";
    }

    @Override
    public String approach(EtoTalk.Context c) {
        int seed = c.seed();
        if (c.pregnant() && seed % 2 == 0) {
            return this.pregnant(c.family().stage(), seed);
        }
        return EtoLines.fill(c, switch (c.mood()) {
            case CLINGY, LONELY -> one(seed, "...I had nothing to say. I only wanted to stand near you.", "You've been gone all day. I noticed.");
            case AFFECTIONATE -> one(seed, "Come here. ...No reason. Do I need one?", "I was thinking about you in a briefing. I lost my place.");
            case SULKY, JEALOUS -> one(seed, "Who was that you were talking to? ...I'm asking as a general. ...No, I'm not.",
                "I saw. ...It's nothing. I'm going to go and sign things, hard.");
            case GIDDY -> one(seed, "Not one casualty this week. Not one. ...I wanted you to know first.", "A good day. I don't get many. Stay in it with me.");
            case SLEEPY -> one(seed, "I've been up two days. ...Sit with me. I'll close my eyes a minute.", "Wake me if the bureau burns down.");
            case BLOODTHIRSTY -> one(seed, "Give me a moment. A bad report. I'm not fit company.", "I lost someone today. Stay, but don't talk.");
            default -> one(seed, "Walk with me. I think better on my feet.", "{name}. I have an hour. I'd like to spend it badly, with you.",
                "You look like you need air. So do I. Come.");
        });
    }

    @Override
    public String welcome(EtoTalk.Context c) {
        return one(c.seed(), "You're back. ...Good. I can stop watching the door.", "Home. ...Come here. Off duty.",
            "There you are. I kept your tea warm. It's still bad.");
    }

    @Override
    public String kissed(int seed, boolean married) {
        return married ? one(seed, "...Thirty years of composure, and still. Every time.", "Again. ...That's an order.", "Mine. ...I never say that. Mine.")
            : one(seed, "...I— hm. Yes. ...Give me a moment. I'm red to the ears.", "That was— carry on. Later. Again later.",
                "...Not a word to the investigators. ...Again.");
    }

    @Override
    public String peck(int seed) {
        return one(seed, "...I had the nerve for once. Don't make a thing of it.", "There. ...I'm going back to my desk.");
    }

    @Override
    public String kissWaiting(String name) {
        return name + " clears his throat, looks at the ceiling, at the door, and finally at you...";
    }

    @Override
    public String kissHint(int seed) {
        return one(seed, "...The door's closed.", "You're looking at me. ...I'm not looking away.");
    }

    @Override
    public String babyNight(EtoTalk.Context c) {
        return one(c.seed(), "It's dark. ...You still want this? ...So do I.", "I've sent the guard off this floor. ...Come here.");
    }

    @Override
    public String held(int seed) {
        return one(seed, "...A little longer. That's all I ask.", "Hold. ...Hold.", "Don't let go yet.");
    }

    @Override
    public String pulled(int seed) {
        return one(seed, "Come here. I'm not finished.", "Closer. ...There.", "Stay. That's an order. A soft one.");
    }

    @Override
    public String hanging(int seed) {
        return one(seed, "...As you were.", "Hm. Another time.", "Understood.");
    }

    @Override
    public String hugged(int seed) {
        return one(seed, "...Oh. ...Yes. All right. ...Don't stop.", "I'm— this isn't regulation. ...Carry on.", "...Not a word of this downstairs.");
    }

    @Override
    public String handTaken(int seed) {
        return one(seed, "In front of the whole bureau. ...Good. Let them see.", "Your hand's cold. ...Keep it there.", "Walk. I have you.");
    }

    @Override
    public String handLet(int seed) {
        return one(seed, "...Right.", "As you like.");
    }

    @Override
    public String partnerHit(int seed, boolean married) {
        return married ? one(seed, "...What was that for?", "Don't. Not you.") : one(seed, "Stand down! ...What's got into you?",
            "If you want to spar, ask.");
    }

    @Override
    public String serious(int seed) {
        return one(seed, "You've made a mistake.", "Last warning. Walk away.", "I've put down worse than you before breakfast.");
    }

    @Override
    public String maniac(int seed) {
        return one(seed, "Is that all? On your feet!", "Thirty years! You think that hurts?", "Again! Come on!");
    }

    @Override
    public String protect(EtoTalk.Context c) {
        return one(c.seed(), "Behind me. Now.", "You'll go through me first.", "Hands off. That one's under my protection.");
    }

    @Override
    public String won(int seed) {
        return one(seed, "Done.", "File it.", "Sloppy. Still done.");
    }

    @Override
    public String puppy(EtoTalk.Context c, String kind, boolean sorry) {
        String what = kind == null ? "that" : "a " + kind;
        return sorry ? one(c.seed(), "You brought " + what + " to apologise. ...That's unfair. It worked.",
            "An apology with fur. ...Accepted.")
            : one(c.seed(), "Is that " + what + "? In a government building? ...It can stay under the desk. Don't tell anyone.",
                "I can't keep " + what + ". ...It's looking at me. ...It's staying.");
    }

    @Override
    public String letter(EtoTalk.Context c, boolean sorry) {
        return sorry ? one(c.seed(), "You wrote this. ...All right. It's forgotten.", "...Accepted. Come here.")
            : one(c.seed(), "A letter. ...I'll read it when the door's closed. I don't trust my face.",
                "By hand. ...I get a hundred reports a day and none of them are for me. Thank you.");
    }

    @Override
    public String snub(int seed, boolean enemy) {
        return enemy ? one(seed, "The General's hand goes to his collar, loosening it. He doesn't take his eyes off you.",
            "The General turns his back on you and says one word to the guard at the door.")
            : one(seed, "The General keeps signing. He doesn't look up.", "The General has no time for you just now.");
    }

    @Override
    public String ring(EtoTalk.Context c, Romance.Refusal refusal) {
        if (refusal == null) {
            return one(c.seed(), "...You're on one knee. In my office. ...Get up. No— stay. ...Yes. Yes. I'm red to the ears. YES.",
                c.ghoul() ? "A general of the CCG, married to a ghoul. They'll have my rank for it. ...Let them. Yes."
                    : "I've given this bureau thirty years. The rest are yours. Yes.");
        }
        return switch (refusal) {
            case NOT_DATING -> "A ring? We haven't so much as had dinner. Put that away.";
            case CLAIMED -> EtoLines.fill(c, "I'm {partner}'s. Put it away.");
            case MARRIED_ELSEWHERE -> "You're married. Go home.";
            case TOO_SOON -> "Too soon. That isn't a no. Ask me when we've had longer.";
            case NOT_CLOSE -> "Not yet. I don't say yes to things I'm not sure of.";
            case ALREADY -> "We're married. I was there. I wore the coat.";
        };
    }

    @Override
    public String morning(EtoTalk.Context c) {
        return one(c.seed(), "The General is already dressed, coat buttoned, looking at you instead of the window.\n\"Good morning. ...I've been "
            + "awake an hour. I didn't want to move.\"",
            "The General pretends to be reading a report. It's upside down.\n\"...Morning. You talk in your sleep. I won't say what.\"");
    }

    @Override
    public String conceived(EtoTalk.Context c) {
        return one(c.seed(), "The General pours your tea before his own, which he has never done.\n\"...How do you feel? ...No reason.\"",
            "The General stands in the doorway a while.\n\"I'm having the guard doubled on this floor. Don't ask me why.\"");
    }

    @Override
    public String pregnant(com.hex.kagune.control.family.Pregnancy.Stage stage, int seed) {
        return switch (stage) {
            case EARLY -> one(seed, "Sit. Eat. I've had the kitchen send something up.", "If you feel unwell, tell me at once. That's an order.");
            case ALONG -> one(seed, "...It moved. I felt it. ...Excuse me. I need a moment.", "I read to it when you sleep. Field manuals. It's all I have.");
            case LATE -> one(seed, "You're not carrying that. I'll carry it. I'll carry you.", "Not long. ...I've faced worse. I've never been so afraid.");
            case DUE -> one(seed, "Any day. I've cleared my desk. The bureau can wait.", "I'm here. I'm not leaving this room.");
        };
    }

    @Override
    public String saw(Display shown, EtoVoice voice, boolean home, int seed) {
        boolean ours = voice.together();
        return switch (shown) {
            case NONE -> "";
            case EYES -> ours ? one(seed, "...Your eyes. ...I've seen ten thousand like them. Never once thought them beautiful. Until now.",
                "Put them away before my people see. ...Not for my sake. For yours.")
                : one(seed, "Those eyes. Close them, or my investigators will close them for you.", "Ghoul. ...I'm giving you one chance to walk.");
            case KAGUNE -> ours ? one(seed, "Put it away. Please. I can order them not to shoot, but I can't order them not to be afraid.",
                "...It's part of you. I'm learning that. Put it away all the same.")
                : one(seed, "Kagune! Stand back, all of you!", "That's a mistake, showing me that.");
            case KAKUJA -> ours ? one(seed, "Come back. That's not an order. I'm asking.", "Listen to my voice. Only mine. Come back to me.")
                : one(seed, "Kakuja. Clear the floor. NOW.", "Every investigator to me!");
            case DREAD -> ours ? one(seed, "...What is that? ...Come back. Whatever it takes. Come back.", "I won't lose you to that. I won't.")
                : one(seed, "Fall back! All units, fall back!", "That's not ours to fight. Get everyone out.");
        };
    }

    @Override
    public String dread(FinalSight sight, EtoVoice voice, boolean home, int seed) {
        boolean ours = voice.together();
        return switch (sight) {
            case WAR -> ours ? one(seed, "Take it off. Please. That isn't you.", "I've read the file on that. Come back before it keeps you.")
                : one(seed, "All units, withdraw. That's an order.", "We don't have a quinque for that.");
            case PEACE -> ours ? one(seed, "...That's the one from the old reports. And it's you. ...I'm not afraid. I'm in awe.",
                "You carry that, and you still come home to me.") : one(seed, "The king's own. Nobody move.", "...Stand down. All of you. Stand down.");
            case DRAGON -> ours ? one(seed, "COME BACK. I'll walk in there myself.", "Not you. Not to that.")
                : one(seed, "Evacuate the ward. Everyone. Go.", "That thing ends cities. Move!");
            case RINKAKU -> ours ? one(seed, "Easy. Easy. I'm here. Breathe.", "Come back down to me.")
                : one(seed, "Rinkaku kakuja. Keep your distance, all of you.", "Hold the line. Don't close with it.");
            case UKAKU -> ours ? one(seed, "...Like a storm with your face. Come down from there.", "I see you in there. Come back.")
                : one(seed, "Ukaku kakuja. Shields up. Don't look away from it.", "Cover! All of you!");
            case NONE -> "";
        };
    }

    @Override
    public EtoGesture dreadGesture(FinalSight sight, boolean ours) {
        return switch (sight) {
            case WAR, DRAGON -> EtoGesture.FLINCH;
            case PEACE -> EtoGesture.AWE;
            case RINKAKU, UKAKU -> ours ? EtoGesture.AWE : EtoGesture.CROSS;
            case NONE -> EtoGesture.NONE;
        };
    }

    @Override
    public String initiative(EtoInitiative.Act act, int seed) {
        return switch (act) {
            case HAND -> one(seed, "...Give me your hand. I've decided I don't care who sees.", "Your hand. Please.");
            case CLING -> one(seed, "Stay by me. ...Closer than that.", "At my side. That's your post.");
            case HUG -> one(seed, "...Come here. Don't say anything, I'll lose my nerve.", "A moment. Just— stand still.");
            case LEAN -> one(seed, "Lean on me. I've held up worse.", "Rest. I have the watch.");
            case HUM -> one(seed, "*hums an old marching tune, badly, and stops when you look* ...Hm.", "...The cadets sang that in my year. Ignore me.");
            case READ -> one(seed, "Read to me. Anything that isn't a report.", "Read something. I like your voice in this room.");
            case DANCE -> one(seed, "...I learned for a bureau ball, once. I never used it. May I?", "Dance with me. Nobody's here. I checked twice.");
            default -> "";
        };
    }

    @Override
    public String moment(EtoInitiative.Moment moment, boolean together, int seed) {
        return switch (moment) {
            case KILL -> together ? one(seed, "Clean. ...Well done.", "I'd have you in my bureau for that.") : one(seed, "Hm.", "Noted.");
            case HURT -> together ? one(seed, "You're bleeding. Who? ...Sit. Let me see it.", "Medic! ...Stay with me. Look at me.")
                : one(seed, "You're hurt. Get that seen to.", "Fall back and patch that.");
            case RAIN -> together ? one(seed, "Rain. ...Under my coat. Come on.", "Stay in with me till it passes.") : one(seed, "Rain.", "Hm. Rain.");
            case DUSK -> together ? one(seed, "Night shift's coming on. Stay near me.", "Sundown. ...I like it better with you here.")
                : one(seed, "Night shift.", "Sundown. Look sharp.");
            case DAWN -> together ? one(seed, "Morning. Everyone came back. ...And you're here.", "Dawn. We made it through another.")
                : one(seed, "Morning.", "Dawn. Shift change.");
        };
    }

    static final List<EtoTopics.Topic> TOPICS = List.of(
        new EtoTopics.Topic("ccg", c -> true, c -> "What does the CCG do?",
            c -> EtoVoice.of(c).pick(c.seed(),
                s("We keep people alive. That's all of it."),
                s("We stand between this city and the things that eat it. Every night. Most of the city never knows.",
                    "We bury our own and go back out. That's what we do."),
                s("We hold a line. Good men and women, a coat and a case each, against things that don't die easily. I've never been prouder "
                    + "of anything than of them."),
                null,
                s("It's my life's work. And then you walked in, and it turned out a life can hold two things."),
                s("It's still the best thing I ever built. You're the best thing I didn't have to build.")),
            List.of(
                new EtoTopics.Follow("You're proud of them.", c -> r("Of every one. I know all their names. I've written to too many of their "
                    + "mothers.", 2, EtoGesture.CROSS)),
                new EtoTopics.Follow("Do you hate ghouls?", c -> r(c.ghoul() ? "...I hated what they did. I thought that was the same thing. "
                    + "Then one of them started bringing me paper." : "I hate funerals. I've been to hundreds. Ask me again in a quieter year.",
                    c.ghoul() ? 3 : 1, EtoGesture.LOOK)),
                new EtoTopics.Follow("Can I join?", c -> r(c.ghoul() ? "A ghoul in the bureau. ...Stranger things are in the files. Not today."
                    : "We always need good people. Prove you are one. Then come and see me.", 1, EtoGesture.CHIN)))),

        new EtoTopics.Topic("years", c -> c.regard() >= NpcBonds.FRIENDLY, c -> "Thirty years?",
            c -> one(c.seed(), "I came in at nineteen with a borrowed coat. I've outlived three partners and two generals. I intend to be the "
                + "last one I outlive.", "Thirty years. I remember every name on the wall downstairs. I put some of them there by giving the order."),
            List.of(
                new EtoTopics.Follow("That's a lot to carry.", c -> r("...It is. Nobody says so. Thank you.", 3, EtoGesture.LOOK)),
                new EtoTopics.Follow("Would you do it again?", c -> r("Every day of it. ...Perhaps with fewer meetings.", 2, EtoGesture.CHIN)),
                new EtoTopics.Follow("Come here.", c -> together(c), c -> r("...A moment. Just a moment. The door's closed?", 4, EtoTalk.Effect.HUG,
                    EtoGesture.HUG)))),

        new EtoTopics.Topic("fight", c -> true, c -> "How do you fight?",
            c -> one(c.seed(), "With my legs. A quinque breaks. A case gets left behind. My legs come with me.",
                "Kicks. A side kick will stop anything with a spine. A spinning one will stop most things without."),
            List.of(
                new EtoTopics.Follow("Teach me.", c -> r("Not yet. I don't hand thirty years to someone I've only just met. Ask me another day.", 0,
                    EtoGesture.CROSS)),
                new EtoTopics.Follow("No quinque?", c -> r("I have one. It's in a drawer. I've never needed to open the drawer.", 2, EtoGesture.CHIN)))),

        new EtoTopics.Topic("ghoul", EtoTalk.Context::ghoul, c -> "I'm a ghoul.",
            c -> together(c) ? one(c.seed(), "I know what you are. I knew when I said yes. I'd say it again.",
                "You are. And I love you. I've stopped trying to make those two fit. They just do.")
                : one(c.seed(), "I know. I've known since you walked in. I've been deciding what to do about it ever since.",
                    "You are. And you're standing in a general's office telling him so. That's either courage or madness."),
            List.of(
                new EtoTopics.Follow("I've never hurt anyone who didn't deserve it.", c -> r("...I want to believe that. Give me time to.", 3,
                    EtoGesture.LOOK)),
                new EtoTopics.Follow("Does it change things?", c -> r(together(c) ? "It changed everything. For the better. I'm a slow man, but I "
                    + "get there." : "It makes everything harder. ...I didn't say impossible.", together(c) ? 4 : 2, EtoGesture.CHIN)),
                new EtoTopics.Follow("Arrest me, then.", c -> r("...Don't tempt me. ...No. Sit down. I'm not finished talking to you.", 2,
                    EtoGesture.HUFF)))),

        new EtoTopics.Topic("coat", c -> c.regard() >= NpcBonds.FRIEND, c -> "You never wear the cap.",
            c -> one(c.seed(), "I lost it my first week. I was too proud to ask for another. Thirty years on, it's a tradition.",
                "A cap hides a man's eyes. My people should see mine."),
            List.of(
                new EtoTopics.Follow("It suits you.", c -> r(together(c) ? "...You'll make me red again. ...Thank you." : "Hm. So I'm told.",
                    together(c) ? 3 : 1, EtoGesture.SHY)),
                new EtoTopics.Follow("I'll find you one.", c -> r("Don't you dare.", 1, EtoGesture.HUFF)))),

        new EtoTopics.Topic("blush", c -> together(c), c -> "You're blushing.",
            c -> one(c.seed(), "I am not. It's the collar. It's tight. ...Stop looking.", "A general does not blush. ...This is a medical condition."),
            List.of(
                new EtoTopics.Follow("It's sweet.", c -> r("...Not a word of it leaves this room.", 3, EtoGesture.SHY)),
                new EtoTopics.Follow("Only for me?", c -> r("Only ever for you. Thirty years of a straight face. You undid it in a week.", 4,
                    EtoGesture.SHY)))),

        new EtoTopics.Topic("baby", EtoTalk.Context::pregnant, c -> "How's our baby?",
            c -> switch (c.family().stage()) {
                case EARLY -> "Small. Well. Rest more. I've had your duties reassigned. You have no duties. That's the point.";
                case ALONG -> "Strong. Kicks like its father. I'm insufferably proud.";
                case LATE -> "Soon. I have the best doctor in the bureau on call and two more she doesn't know about.";
                case DUE -> "Any day. I'm not leaving your side. The bureau has been told.";
            },
            List.of(
                new EtoTopics.Follow("Will they wear the coat?", c -> r("If they want to. ...Only if they want to. I'll be proud either way.", 4,
                    EtoGesture.HUG)),
                new EtoTopics.Follow("Hold me.", c -> r("...Come here. Both of you.", 4, EtoTalk.Effect.CLING, EtoGesture.CLING)))),

        new EtoTopics.Topic("family", c -> c.married() && !c.pregnant() && c.family().children() == 0, c -> "Do you want a family?",
            c -> c.family().wantsBaby() ? "Tonight. ...If you still want to. I do."
                : "I had the bureau instead. ...With you, I'd like to find out what the other thing is.",
            List.of(
                new EtoTopics.Follow("Someday.", c -> r("Someday. I'm not a young man. Not too far off.", 2, EtoGesture.SHY)))));

    @Override
    protected List<EtoTopics.Topic> topics() {
        return TOPICS;
    }
}
