package com.hex.kagune.control.npc;

import java.util.List;

/**
 * 1.3.14: Touka's words (pure, tested). The rabbit: an Ukaku, sharp-tongued, serious, quick to pout and quicker to kick, who would
 * rather die than say she cares and then does anyway, badly. She works a cafe, studies for exams she swears she doesn't care about,
 * hates Doves, worries about a brother she won't name, and teaches the wings: crystallizing them, the Gatling, the first kakuja.
 * Strangers get "what"; friends get insults that mean something else; her partner gets the same insults, blushing.
 */
final class ToukaScript extends CompanionScript {
    // ---------------------------------------------------------------- who she is to them

    @Override
    protected String her() {
        return "her";
    }

    @Override
    protected String spouse() {
        return "wife";
    }

    @Override
    protected String errandLabel() {
        return "I brought coffee beans. (the cocoa beans in your hand)";
    }

    @Override
    protected String flowerLabel() {
        return "I got you a rabbit's foot. (the one in your hand)";
    }

    @Override
    protected String lessonLabel(EtoTalk.Context c) {
        return c.teaching().refusal() == Teaching.Refusal.HUMAN ? "Teach me to fight." : "Teach me. (your wings)";
    }

    @Override
    public String title(EtoTalk.Context c) {
        return switch (EtoVoice.of(c)) {
            case WIFE -> "Your wife";
            case PARTNER -> "Your girlfriend (don't call her that)";
            case CLAIMED -> "Taken";
            case CLOSE -> "Touka";
            default -> "The rabbit";
        };
    }

    @Override
    public String greeting(EtoTalk.Context c) {
        int seed = c.seed();
        String line = switch (EtoVoice.of(c)) {
            case CLAIMED -> one(seed, "If it's about your wings, fine. If it's anything else, I'm {partner}'s. Don't make it weird.",
                "What. ...Lessons only. I have a partner and you're not them.", "You again. Make it quick, {partner}'s waiting.");
            case WARY -> c.night() ? one(seed, "It's late. What do you want.", "Out hunting at this hour? Idiot. What.",
                "If you're a Dove, you're already dead. If you're not, talk.")
                : one(seed, "What.", "...Can I help you? No? Then move.", "You're staring. Stop it.", "I'm busy. Make it quick.");
            case FRIENDLY -> one(seed, "Oh. It's you. ...Hi, I guess.", "You're back. Don't make a habit of it. ...Or do. Whatever.",
                "Coffee's on. Don't touch the good beans.", "{name}. You look terrible. Sit down before you fall down.");
            case CLOSE -> one(seed, "Took you long enough. ...I wasn't waiting.", "Hey. Sit. I saved you the seat by the window. Don't read into it.",
                "There you are, idiot. I made too much coffee. You're drinking it.", "{name}. You didn't get yourself killed. Good. ...I mean, fine.");
            case PARTNER -> c.mood() == EtoMood.SULKY ? one(seed, "Oh, now you show up.", "Hmph. Don't talk to me. ...Okay, talk to me.")
                : c.daysAway() >= 3 ? one(seed, "Three days. You don't text, you don't visit. I wasn't worried. Idiot.",
                    "Where have you been? ...No. Don't explain. Just stay a bit.")
                : one(seed, "You're here. ...Good. Don't look so pleased with yourself.", "Hey, you. Come here. ...Closer. Not that close. Okay, that close.",
                    "I was just thinking about you. Annoying. You're annoying.", "Your hair's a mess. Hold still. ...There.");
            case WIFE -> c.pregnant() ? one(seed, "Don't hover. I'm pregnant, not made of glass. ...Okay, hover a little.",
                "Your kid kicked me all night. Your kid. Definitely yours.")
                : one(seed, "Welcome home. ...Don't make that face, it's just a word.", "Hey. Dinner's coffee again. Deal with it.",
                    "There's my idiot. Come here.", "You're late. I kept your cup warm. Twice.");
        };
        return EtoLines.fill(c, line);
    }

    // ---------------------------------------------------------------- answers

    @Override
    public EtoTalk.Reply answer(EtoTalk.Context c, NpcTalk.Choice choice) {
        EtoVoice voice = EtoVoice.of(c);
        int seed = c.seed();
        return switch (choice) {
            case WHO -> new EtoTalk.Reply(voice == EtoVoice.CLAIMED ? EtoLines.fill(c, "Touka. I'm {partner}'s. That's all you need.")
                : "Touka. I work at a cafe, I go to school, and I kick people who ask too many questions. ...Wings. Ukaku. If you have them, maybe "
                + "I'll show you how to use them properly.", c.has(NpcBonds.ASKED) ? 0 : EtoRules.WHO, EtoTalk.Effect.NONE);
            case CHAT -> new EtoTalk.Reply(EtoLines.fill(c, voice.pick(seed,
                s("Short. {partner} gets the long ones."),
                s("Talk? About what. ...Fine. Five minutes.", "You want to talk. To me. ...Okay. Go."),
                s("Sure. I'm on break anyway.", "Fine. Sit. Don't slurp."),
                s("Yeah. Talk to me. I've had a long day.", "What's on your mind? ...Don't say 'you'."),
                s("Talk. I like your voice. ...Forget I said that.", "Sit by me. Talk. About anything."),
                s("Talk to me, idiot. I missed you today.", "Come here and tell me everything. Leave out the gross parts."))),
                c.chattedToday() ? 0 : EtoRules.CHAT, EtoTalk.Effect.TALK);
            case BOOK -> errandItem(c, "Coffee beans? Where? You're holding air.",
                "Good beans. That's {n} of three. Keep going.", "Three. That's enough to keep the cafe going a week. ...Thanks. "
                    + "Now something else. Ask me again.",
                "We're stocked. Bring them tomorrow.", "Beans again? You're trying to make me like you. ...It's working. Shut up.",
                "Oh. Good beans, actually. The cafe's always running low. Thanks.", EtoGesture.SIP);
            case FLOWER -> flowerGiven(c, "A rabbit's foot? Where?",
                "...A rabbit's foot. Is this supposed to be a joke? Because it's not funny. ...I'm keeping it. Was there something you wanted to ask me?",
                "I already have one. From you. ...I keep it in my pocket. Don't tell anyone.",
                "Another one? You're so weird. ...Thanks.", "Is this a joke about my mask? ...It's kind of funny. I'm keeping it.", EtoGesture.SHY);
            case GIFT -> c.meat() ? new EtoTalk.Reply(one(seed, "...You brought me dinner. Who taught you to be thoughtful? Gross. Thank you.",
                "I haven't eaten in three days. Don't tell anyone. ...Thank you."), c.giftedToday() ? 0 : EtoRules.GIFT, EtoTalk.Effect.MEAT, EtoGesture.SHY)
                : r("Dinner? Where?", 0);
            case PUPPY -> c.puppy() == null ? r("A what? Where?", 0)
                : new EtoTalk.Reply(this.puppy(c, c.puppy(), false), 8, EtoTalk.Effect.PUPPY, EtoGesture.PET);
            case LETTER -> c.letter() ? new EtoTalk.Reply(this.letter(c, false), c.giftedToday() ? 0 : 4, EtoTalk.Effect.LETTER, EtoGesture.READ)
                : r("A letter? Where?", 0);
            case LESSON -> lesson(c, LESSONS);
            case FAVOUR -> errand(c,
                "...Fine. Since you won't leave me alone. The cafe's out of good beans. Bring me three bags. Cocoa beans, the brown pods. Don't ask.",
                "I don't need anything from you. Yet.",
                "Beans. Three. You've brought {n}. It's not hard.",
                "Doves have been sniffing round the ward. Ugly things crawl out at night and draw them in. Kill ten of them after dark. {n} so far.",
                "Last thing. Bring me a rabbit's foot. ...Don't ask why. Just do it.",
                "That's everything. ...Was there something you wanted to say? Say it before I change my mind.",
                "You don't have to do chores for me anymore. Just... come by.",
                "I don't need anything. {partner} takes care of it.", "Go home to your spouse, idiot.");
            case ASK_OUT -> !c.mayDate() ? r(c.claimed() ? EtoLines.fill(c, "I'm {partner}'s. Are you deaf?") : "You're married. Go home.", 0)
                : c.step() == EtoRules.Step.ASK ? new EtoTalk.Reply("...Took you long enough. Yes. Fine. Yes. Don't make it weird. ...You're making it weird.",
                    0, EtoTalk.Effect.DATE, EtoGesture.SHY)
                : r("Ask me what?", 0);
            case KISS -> c.dating() ? new EtoTalk.Reply(one(seed, "...Fine. Close your eyes. I said close them.", "Here? Now? ...Okay. Come here."), 0,
                EtoTalk.Effect.KISS) : r("Try it and lose your teeth.", -2);
            case HUG -> c.dating() ? new EtoTalk.Reply(this.hugged(seed), 0, EtoTalk.Effect.HUG) : r("Don't touch me.", 0);
            case SWEET -> c.dating() ? new EtoTalk.Reply(EtoLines.fill(c, one(seed, "Sweet? ...Fine. Go on. I'm not blushing.",
                "What, you're going to be nice to me now? ...Okay.", "Make it quick before someone sees.")), 0, EtoTalk.Effect.SWEET)
                : r("Sweet to me? Earn it.", 0);
            case NIGHT -> !c.dating() ? r("EXCUSE me?", -3) : new EtoTalk.Reply(c.night() && !c.nightUsed() ? one(seed,
                "...Bed. Fine. Lights off. And if you snore I'm kicking you out.", "Come on, then. Don't make me say it twice.")
                : one(seed, "Not tonight. I'm tired. Hold my hand instead.", "It's not even dark, idiot."), 0,
                c.night() && !c.nightUsed() ? EtoTalk.Effect.NIGHT : EtoTalk.Effect.NONE);
            case PROPOSE -> new EtoTalk.Reply("", 0, EtoTalk.Effect.PROPOSE);
            case FOLLOW -> new EtoTalk.Reply(voice.together() ? one(seed, "Fine. Walk slower. ...Not that slow.", "Where are we going? ...Okay. Lead.")
                : one(seed, "Fine. Don't get us killed.", "I'll come. Not because you asked."), 0, EtoTalk.Effect.FOLLOW);
            case STAY -> new EtoTalk.Reply(voice.together() ? one(seed, "Here? Fine. I'll make this place less ugly.", "Okay. This is home now. Don't take forever.")
                : one(seed, "Fine. I'll wait. Don't make me wait long.", "Here? Whatever."), 0, EtoTalk.Effect.STAY);
            case DECLINE -> r("Your loss.", 0);
            case TEASE -> new EtoTalk.Reply(one(seed, "Say that again and I'll kick you into next week.", "You think you're funny. You're not. ...You're a little funny.",
                "Shut up. ...Shut UP.", "Ugh. Why do I like you."), 1, EtoTalk.Effect.NONE, EtoGesture.HUFF);
            case INSULT -> NpcBonds.clamp(c.regard() + EtoRules.INSULT) <= NpcBonds.ENEMY
                ? new EtoTalk.Reply("Okay. You want to die. I can do that.", EtoRules.INSULT, EtoTalk.Effect.ANGER)
                : new EtoTalk.Reply(voice == EtoVoice.CLAIMED ? EtoLines.fill(c, "Charming. {partner}'s going to hear about this. Get lost.")
                : one(seed, "Get out before I kick you out.", "Wow. Okay. Leave."), EtoRules.INSULT, EtoTalk.Effect.CLOSE, EtoGesture.HUFF);
            case BACK -> new EtoTalk.Reply(EtoLines.fill(c, voice.pick(seed, s("Anything else? No? Good."), s("What else."), s("Something else?"), null,
                s("What else, idiot?", "Mm? What?"))), 0, EtoTalk.Effect.MAIN);
            case LEAVE -> new EtoTalk.Reply("", 0, EtoTalk.Effect.CLOSE);
            default -> r("What?", 0);
        };
    }

    /** How she teaches: shown once, then "again", until it sticks. */
    static final LessonWords LESSONS = new LessonWords() {
        @Override
        public String teach(Teaching.Lesson lesson, EtoTalk.Context c) {
            return switch (lesson.gate()) {
                case UKAKU_CRYSTAL -> "Your wings burn because you let them. Breathe in. Hold it in your back, not your chest. Now make it cold. Hard. "
                    + "Like glass... There. Crystal. Don't shatter it the first time, idiot. (Crystal Wings: V with your wings out.)";
                case UKAKU_GATLING -> "One shard at a time? Cute. Stop aiming every one. Pick a direction and empty yourself into it. Again. Faster. "
                    + "...Good. That's a Gatling. Don't burn out. (Gatling: J in crystal mode.)";
                case UKAKU_KAKUJA -> "You want more. ...It costs. Eat your own kind long enough and it grows over you like armour. It doesn't feel like "
                    + "you anymore. I'm not proud of mine. Don't you be proud of yours. (First kakuja: tap K.)";
                default -> "Watch. Once. Then you do it. (" + lesson.name() + ")";
            };
        }

        @Override
        public String refuse(Teaching.Refusal refusal, Teaching.View view, EtoTalk.Context c) {
            return switch (refusal) {
                case HUMAN -> "You don't have wings. You don't have anything. Go home, human, before something eats you.";
                case WRONG_PATH -> "Those aren't wings. I don't know what to do with tails. Ask someone who has them. ...Ask Ken.";
                case WEAK -> "Not yet. You'd tear yourself apart. Come back stronger. (" + view.next().name() + ": mastery " + view.need() + ")";
                case BEFORE -> "You skipped a step. Get " + (view.before() == null ? "the basics" : view.before().displayName()) + " first.";
                case ALL_TAUGHT -> "I've taught you everything I know. The last one... you'd have to go through hell for. Ken knows the way. "
                    + "I wish he didn't.";
            };
        }
    };

    // ---------------------------------------------------------------- her partner's sweet page

    @Override
    protected String[][] sweetSays() {
        return new String[][] {{"You're cute.", "cute"}, {"I love you.", "love"}, {"I missed you.", "missed"}, {"Make me coffee?", "coffee"},
            {"Let me fix your hair.", "hair"}, {"Lean on me.", "lean"}, {"Smile for me.", "smile"}, {"You're the strongest person I know.", "strong"}};
    }

    @Override
    public EtoTalk.Reply sweetly(EtoTalk.Context c, String what) {
        int seed = c.seed();
        boolean married = c.married();
        EtoTalk.Reply reply = switch (what) {
            case "cling" -> new EtoTalk.Reply(one(seed, "...Fine. Just for a second. ...Okay, a minute.", "Don't let go yet."), 2, EtoTalk.Effect.CLING, EtoGesture.NONE);
            case "hand" -> new EtoTalk.Reply(one(seed, "My hand? ...Here. Don't swing it. ...Okay, swing it a little.",
                "Fine. But if anyone sees, you grabbed mine."), 1, EtoTalk.Effect.HAND, EtoGesture.NONE);
            case "cute" -> r(one(seed, "I'm NOT cute. ...Say it again.", "Shut up. I'm terrifying.", "...You're blind. Thanks."), 2, EtoGesture.SHY);
            case "love" -> married ? r(one(seed, "I know, idiot. I married you. ...I love you too.", "Every day? Okay. I love you too. Every day. Happy?"),
                3, EtoTalk.Effect.NONE, EtoGesture.SHY)
                : r(one(seed, "You can't just SAY that. ...I love you too. Shut up.", "...Don't. Don't say it like that. ...I mean it back."), 3,
                    EtoTalk.Effect.NONE, EtoGesture.SHY);
            case "missed" -> r(one(seed, "I didn't miss you. ...I missed you a little.", "Good. Then don't leave so long."), 2, EtoTalk.Effect.CLING, EtoGesture.CLING);
            case "coffee" -> r(one(seed, "You don't deserve my coffee. ...Sit. One cup.", "Fine. Watch. It's not hard. You're still not touching the machine."),
                2, EtoGesture.SIP);
            case "hair" -> r(one(seed, "Don't— ...okay. Gently.", "If you mess up my bangs I'll end you. ...That's nice."), 2, EtoGesture.HAIR);
            case "lean" -> r(one(seed, "Only because I'm tired.", "...You're comfortable. I hate it."), 2, EtoTalk.Effect.CLING, EtoGesture.CLING);
            case "smile" -> r(one(seed, "No. ...Stop it. Stop making faces. ...Ugh. There. Happy?", "I'm smiling. Inside. Deep inside."), 2,
                EtoGesture.SHY);
            case "strong" -> r(one(seed, "...Where'd that come from? ...Thanks. Nobody says that.", "I'm not. But thanks for thinking it."), 3,
                EtoTalk.Effect.HUG, EtoGesture.HUG);
            default -> r("What?", 0);
        };
        return new EtoTalk.Reply(EtoLines.fill(c, reply.line()), reply.regard(), reply.effect(), reply.gesture());
    }

    // ---------------------------------------------------------------- what she says unasked

    @Override
    public String giftsAsked(int seed) {
        return one(seed, "You brought me something? ...What is it.", "For me? ...Show me.");
    }

    @Override
    public String topicsAgain(int seed) {
        return one(seed, "What else.", "Something else?", "Go on.");
    }

    @Override
    public String busy() {
        return "Touka's busy kicking something's head in.";
    }

    @Override
    public String approach(EtoTalk.Context c) {
        int seed = c.seed();
        if (c.pregnant() && seed % 2 == 0) {
            return this.pregnant(c.family().stage(), seed);
        }
        return EtoLines.fill(c, switch (c.mood()) {
            case CLINGY, LONELY -> one(seed, "Hey. ...No reason. I just wanted to see you.", "You're here. Good. Stay where I can see you.",
                "Don't go anywhere for a bit, okay?");
            case AFFECTIONATE -> one(seed, "Come here. ...I said come here.", "You. Me. Coffee. Now.", "I like it when you're around. Don't make it a thing.");
            case SULKY, JEALOUS -> one(seed, "So. Who was that.", "I'm not mad. I'm just standing here. Being not mad.");
            case GIDDY -> one(seed, "Hey! ...Ahem. Hey.", "I had a good day. Tell me yours.");
            case SLEEPY -> one(seed, "I'm so tired. Be quiet with me.", "Exams. Coffee. More exams. Hold me up.");
            default -> one(seed, "Hey. Got a minute?", "There you are.", "You look like you need coffee. You always look like you need coffee.");
        });
    }

    @Override
    public String welcome(EtoTalk.Context c) {
        return one(c.seed(), "You're back. ...About time.", "Welcome back. Don't look so happy, I just happened to be here.",
            "There you are. I was about to come looking. Not that I was worried.");
    }

    @Override
    public String kissed(int seed, boolean married) {
        return married ? one(seed, "...Mm. Okay. Again later.", "Idiot. ...I love you.") : one(seed, "...Don't tell anyone.", "That was... fine. It was fine.",
            "Okay. Okay. Stop smiling.");
    }

    @Override
    public String peck(int seed) {
        return one(seed, "...That didn't happen. Don't look at me.", "Shut up. It was a tiny one. It doesn't count.", "Hmph. You looked like you needed it.",
            "Don't make that face. ...I'll do it again.");
    }

    @Override
    public String kissWaiting(String name) {
        return name + " goes red to the ears, squeezes her eyes shut and leans in...";
    }

    @Override
    public String kissHint(int seed) {
        return one(seed, "...You're really going to make me ask.", "Hey. Come here. ...No, closer. Ugh.", "I'm not going to say it.");
    }

    @Override
    public String babyNight(EtoTalk.Context c) {
        return one(c.seed(), "...It's night. You asked. I said yes. Don't make me come and get you.", "Bed. Now. You know why. Don't smirk.");
    }

    @Override
    public String held(int seed) {
        return one(seed, "...You held that one. I noticed.", "Okay, that was nice. That was really nice.");
    }

    @Override
    public String pulled(int seed) {
        return one(seed, "Come HERE. ...There. Now shut up.", "Stop being so slow.");
    }

    @Override
    public String hanging(int seed) {
        return one(seed, "...You know what? Forget it. FORGET IT.", "I can't believe you. Don't talk to me.",
            "Wow. Okay. Great. Enjoy kissing nobody, idiot.");
    }

    @Override
    public String hugged(int seed) {
        return one(seed, "...Fine. Hug me. Don't squeeze.", "Ugh. Okay. Come here.", "You're warm. Shut up.");
    }

    @Override
    public String handTaken(int seed) {
        return one(seed, "My hand. Don't make a big deal.", "...Okay. We're holding hands. It's fine. This is fine.",
            "If anyone asks, you grabbed mine.");
    }

    @Override
    public String handLet(int seed) {
        return one(seed, "...Okay.", "Fine. Whatever.", "Hmph. Your hand was sweaty anyway.");
    }

    @Override
    public String partnerHit(int seed, boolean married) {
        return married ? one(seed, "Did you just— Are you serious right now?", "Hit me again and you're sleeping outside.")
            : one(seed, "OW. What is WRONG with you?", "You hit me. You actually hit me.", "Do that again and I'll break your leg.");
    }

    @Override
    public String serious(int seed) {
        return one(seed, "Okay. Now I'm annoyed.", "You picked the wrong rabbit.", "Stay down.");
    }

    @Override
    public String maniac(int seed) {
        return one(seed, "I'm going to kill you. I'm going to KILL you.", "You want to see what a rabbit does when it's cornered?",
            "Don't you DARE.");
    }

    @Override
    public String protect(EtoTalk.Context c) {
        return one(c.seed(), "Hey! Get away from them!", "Touch them again. I dare you.", "Stay behind me, idiot.");
    }

    @Override
    public String won(int seed) {
        return one(seed, "...Done. Tch.", "Stay down this time.", "Waste of my time.");
    }

    @Override
    public String puppy(EtoTalk.Context c, String kind, boolean sorry) {
        String what = kind == null ? "it" : kind.equals("rabbit") ? "a rabbit" : "a little " + kind;
        return sorry ? one(c.seed(), "...You brought me " + what + " to say sorry. That's so cheap. ...It's working.",
            "Ugh. Fine. I forgive you. Not because of " + what + ". ...A little because of " + what + ".")
            : one(c.seed(), "Is that " + what + "? For me? ...I'm not crying. Shut up.", "Oh. Oh, look at it. ...Don't look at me looking at it.");
    }

    @Override
    public String letter(EtoTalk.Context c, boolean sorry) {
        return sorry ? one(c.seed(), "...You wrote this. By hand. ...Okay. You're forgiven. Don't do it again.", "Your handwriting's terrible. ...I'll keep it.")
            : one(c.seed(), "A letter? Who writes letters? ...I'll read it later. Alone.", "You wrote me something? ...Don't watch me read it.");
    }

    @Override
    public String snub(int seed, boolean enemy) {
        return enemy ? one(seed, "Touka won't even look at you.", "Touka's hand drifts toward her kagune.")
            : one(seed, "Touka turns her back on you. Hmph.", "Touka is ignoring you. Loudly.");
    }

    @Override
    public String ring(EtoTalk.Context c, Romance.Refusal refusal) {
        if (refusal == null) {
            return one(c.seed(), "Is that— ...You idiot. You absolute idiot. ...Yes. YES. Put it on before I change my mind.",
                "...You're serious. You're actually serious. ...Okay. Yes. Don't cry, you'll make ME cry.");
        }
        return switch (refusal) {
            case NOT_DATING -> "A ring? We're not even— Put that away.";
            case CLAIMED -> EtoLines.fill(c, "I'm {partner}'s. Put that away.");
            case MARRIED_ELSEWHERE -> "You're already married. Are you insane?";
            case TOO_SOON -> "We've barely started. Ask me again in a few days. ...I'm not saying no. Just not yet.";
            case NOT_CLOSE -> "Not yet. ...Make me sure first.";
            case ALREADY -> "We're already married, idiot. I'm wearing it. Look.";
        };
    }

    @Override
    public String morning(EtoTalk.Context c) {
        return one(c.seed(), "Touka, half asleep, steals the whole blanket.\n\"...Morning. Coffee. Now.\"",
            "Touka is already up, two cups on the table.\n\"Don't get used to this.\"");
    }

    @Override
    public String conceived(EtoTalk.Context c) {
        return one(c.seed(), "Touka lies awake, a hand on her stomach.\n\"...I think something's different. Don't ask. Not yet.\"",
            "In the morning Touka is quiet a long time.\n\"...We're going to need a bigger place.\"");
    }

    @Override
    public String pregnant(com.hex.kagune.control.family.Pregnancy.Stage stage, int seed) {
        return switch (stage) {
            case EARLY -> one(seed, "I can't even smell coffee right now. COFFEE.", "Everything makes me want to throw up. Including you. ...Stay anyway.");
            case ALONG -> one(seed, "It kicks. Like me. Of course it kicks.", "Feel. ...There. That's our kid. Trying to get out already.");
            case LATE -> one(seed, "I'm huge. Don't agree with me.", "My back hurts, my feet hurt, I'm hungry. Rub something.");
            case DUE -> one(seed, "Any day now. Stay close. ...Please.", "If you wander off now, I'll find you. I'll find you.");
        };
    }

    @Override
    public String saw(Display shown, EtoVoice voice, boolean home, int seed) {
        boolean ours = voice.together();
        return switch (shown) {
            case NONE -> "";
            case EYES -> ours ? one(seed, "...Your eyes. Put them away before someone sees. ...They're pretty, though.", "Red suits you. Don't let it go to your head.")
                : one(seed, "Put those away. Doves are everywhere.", "Eyes. Great. Are you trying to get us found?");
            case KAGUNE -> ours ? one(seed, "Show-off. ...Your form's sloppy. I'll fix it later.", "Not bad. Mine are prettier.")
                : one(seed, "Put that away before I take it off you.", "Wings out in the open? Idiot.");
            case KAKUJA -> ours ? one(seed, "...Hey. Hey. Come back. Look at me. Come back.", "Don't let it eat you. You hear me? Don't.")
                : one(seed, "...What ARE you.", "Stay away from me.");
            case DREAD -> ours ? one(seed, "No. No no no. Come back. COME BACK.", "Please. Whatever's left of you in there. Please.")
                : one(seed, "Run. Everyone run.", "That's... that's not a ghoul anymore.");
        };
    }

    @Override
    public String initiative(EtoInitiative.Act act, int seed) {
        return switch (act) {
            case HAND -> one(seed, "Give me your hand. ...Don't ask why.", "Hand. Now. It's cold.", "My hand's free. Fix that.");
            case CLING -> one(seed, "Don't move. I'm comfortable.", "...Just for a minute.");
            case HUG -> one(seed, "C-come here. No reason.", "Shut up and hug me back.");
            case LEAN -> one(seed, "I'm just resting. On you. Coincidence.", "You're a good height for this. Annoying.");
            case HUM -> one(seed, "*hums something under her breath, glaring if you notice*", "Don't listen. I'm not singing.");
            case READ -> one(seed, "Quiz me. I have an exam. Don't make that face.", "Read this. Is it wrong? It's wrong, isn't it.");
            case DANCE -> one(seed, "...Don't laugh. Just. Come here. One song.", "Nobody's looking. One dance. Then we never talk about it.");
            default -> "";
        };
    }

    @Override
    public String moment(EtoInitiative.Moment moment, boolean together, int seed) {
        return switch (moment) {
            case KILL -> together ? one(seed, "Clean. ...You're getting better.", "Sloppy. You telegraph your left. I'll fix it.") : one(seed, "Tch.", "Messy.");
            case HURT -> together ? one(seed, "You're bleeding. Sit. SIT.", "Who did that? Point.", "Idiot. Don't die on me.") : one(seed, "You're hurt. Careful.",
                "Patch yourself up before you drop.");
            case RAIN -> together ? one(seed, "Rain. Good. Nobody hunts in the rain. Stay in with me.", "Ugh, my hair. ...Share your hood.")
                : one(seed, "Rain. Great.", "Tch. Rain.");
            case DUSK -> together ? one(seed, "Sun's going down. Stay close tonight.", "Night shift. ...Walk me home after?") : one(seed, "Dark soon. Go home.",
                "Night. Doves come out at night. Be careful.");
            case DAWN -> together ? one(seed, "Morning already? We didn't sleep. Your fault.", "Sunrise. ...It's pretty. Don't tell anyone I said that.")
                : one(seed, "Morning. I have school.", "Ugh. Sun.");
        };
    }

    // ---------------------------------------------------------------- what can be talked about

    static final List<EtoTopics.Topic> TOPICS = List.of(
        new EtoTopics.Topic("cafe", c -> true, c -> "How's the cafe?",
            c -> EtoVoice.of(c).pick(c.seed(),
                s("Busy. Like me."),
                s("Fine. People drink coffee. I serve it. Riveting."),
                s("It's quiet. The manager says a cafe should be a place where anyone can rest. Even us.",
                    "We burned the milk twice today. Not me. ...Mostly not me."),
                null,
                s("Better when you come by. Not that I watch the door.", "I saved you the corner table. Again."),
                s("Ours now, kind of. I want a cafe of my own someday. You'll wash cups.")),
            List.of(
                new EtoTopics.Follow("Teach me to make coffee.", c -> r(together(c) ? "Fine. Stand behind me. Hands here. ...Not THERE. Here. Slow pour."
                    : "Pour slow. Like you mean it. ...You don't mean it. Again.", 2, EtoGesture.SIP)),
                new EtoTopics.Follow("Why coffee, of all things?", c -> r("It's the one human thing we can taste. Everything else is like eating dirt. "
                    + "Coffee's... ours. A little bit of normal.", 2, EtoGesture.SIP)),
                new EtoTopics.Follow("I'd come every day.", c -> r(together(c) ? "...Then come every day. Idiot." : "Then pay for your coffee every day.", 1,
                    EtoGesture.SHY)))),

        new EtoTopics.Topic("school", c -> !c.claimed(), c -> "How's school?",
            c -> one(c.seed(), "Exams. I'm going to fail chemistry. Don't laugh.", "My friend made me lunch again. Human lunch. I ate all of it.",
                "Studying. It's stupid. ...I want to go to university. Don't tell anyone."),
            List.of(
                new EtoTopics.Follow("You ate human food? For her?", c -> r("She worked hard on it. Her face when I said it was good... "
                    + "I threw up behind the gym after. Worth it.", 3, EtoGesture.SHY)),
                new EtoTopics.Follow("Want me to quiz you?", c -> r(one(c.seed(), "...Yeah. Okay. Don't be smug when I get them wrong.",
                    "Fine. Start with the hard ones."), 2, EtoGesture.READ)),
                new EtoTopics.Follow("You'll get in. You're smart.", c -> r("...Shut up. ...You think so?", 3, EtoGesture.SHY)))),

        new EtoTopics.Topic("doves", c -> true, c -> "Who are the Doves?",
            c -> one(c.seed(), "Investigators. White coats, suitcases full of kagune. Ours. They cut us up and make weapons out of us.",
                "The people who killed my father. Next question."),
            List.of(
                new EtoTopics.Follow("Have you fought them?", c -> r("Yes. I won. Mostly. One of them nearly took my wing off. I still dream about him.", 1)),
                new EtoTopics.Follow("I'm sorry about your father.", c -> r("...Don't be. Just don't get caught. That's how you can be sorry.", 3,
                    EtoGesture.CROSS)),
                new EtoTopics.Follow("I'll keep you safe.", c -> r(together(c) ? "...You can't. But... thanks for wanting to." : "I keep myself safe. "
                    + "Worry about you.", 2, EtoGesture.SHY)))),

        new EtoTopics.Topic("brother", c -> c.regard() >= NpcBonds.FRIENDLY, c -> "Do you have family?",
            c -> one(c.seed(), "A little brother. He's an idiot. He joined people I hate and he thinks I'm weak. ...I still worry about him.",
                "A brother. We don't talk. He's alive. That's enough."),
            List.of(
                new EtoTopics.Follow("Why do you fight?", c -> r("He thinks being strong means being cruel. I want to prove him wrong. I want to be strong "
                    + "and still be... me.", 3)),
                new EtoTopics.Follow("He'll come around.", c -> r("...Maybe. If someone doesn't kill him first. Probably me.", 2, EtoGesture.HUFF)))),

        new EtoTopics.Topic("rabbit", c -> true, c -> "Why do they call you the rabbit?",
            c -> one(c.seed(), "My mask. Rabbit ears. Don't laugh. ...You laughed.",
                "Because I'm fast, I'm small, and I go for the throat. Any more questions?"),
            List.of(
                new EtoTopics.Follow("Rabbits die of loneliness, right?", c -> r(together(c) ? "...That's a myth. ...Don't test it. Stay."
                    : "That's a myth. And I'm not lonely. Shut up.", 2, EtoGesture.SHY)),
                new EtoTopics.Follow("It's a cute mask.", c -> r("It's not supposed to be cute. It's supposed to be scary. ...It's a little cute.", 2,
                    EtoGesture.HUFF)))),

        new EtoTopics.Topic("wings", c -> c.ghoul(), c -> "How do you fight so well?",
            c -> one(c.seed(), "Practice. And being angry for twenty years. Ukaku burns fast, so I end things fast.",
                "Keep moving. Never stand still. If they're still breathing, you weren't fast enough."),
            List.of(
                new EtoTopics.Follow("Spar with me?", c -> r("Sure. ...Don't cry when I win.", 1, EtoGesture.KICKS)),
                new EtoTopics.Follow("Your kicks are scary.", c -> r("Good. That's the point. Want a demonstration?", 1, EtoGesture.KICKS)))),

        new EtoTopics.Topic("ken", c -> c.regard() >= NpcBonds.FRIENDLY, c -> "Do you know Ken?",
            c -> one(c.seed(), "That idiot? ...Yeah. I know him. He used to be soft. Then they broke him and he came back white.",
                "Ken. He reads too much and he never eats and he thinks he has to carry everyone. I hate it."),
            List.of(
                new EtoTopics.Follow("Do you like him?", c -> together(c) ? r("I'm with YOU. Idiot. ...He's family. Kind of. Don't make it weird.", 2,
                    EtoGesture.HUFF) : r("Shut up. ...He's important. That's all.", 1, EtoGesture.SHY)),
                new EtoTopics.Follow("What happened to him?", c -> r("Someone took him for ten days and did things to him. When he came back he counted "
                    + "in his sleep. If you want the kakuja, he'll show you. ...Don't ask him.", 1)))),

        new EtoTopics.Topic("writer", c -> c.regard() >= NpcBonds.FRIEND, c -> "What do you think of Eto?",
            c -> "The writer in the library? Stay away from her. She smiles like she already knows how you die.",
            List.of(
                new EtoTopics.Follow("She's not that bad.", c -> r("Everyone says that. Right up until.", 0, EtoGesture.CROSS)),
                new EtoTopics.Follow("Are you jealous?", c -> r("JEALOUS? Of— ...Get out.", together(c) ? 2 : -1, EtoGesture.HUFF)))),

        // A family of their own, if they have one coming.
        new EtoTopics.Topic("baby", EtoTalk.Context::pregnant, c -> "How's the baby?",
            c -> switch (c.family().stage()) {
                case EARLY -> "Small. Annoying. Like its father. ...I'm fine. I'm fine.";
                case ALONG -> "It kicks like me. I'm so proud and so sore.";
                case LATE -> "Huge. I'm huge. Don't say anything.";
                case DUE -> "Soon. Stay where I can reach you.";
            },
            List.of(
                new EtoTopics.Follow("Can I feel it?", c -> c.family().stage() != com.hex.kagune.control.family.Pregnancy.Stage.EARLY,
                    c -> r("...Here. Gently. ...There. That's ours.", 5, EtoTalk.Effect.HUG, EtoGesture.HUG)),
                new EtoTopics.Follow("Do you need anything?", c -> r("Coffee. I can't have coffee. Do you know what that's like? Rub my back.", 3, EtoGesture.HAIR)),
                new EtoTopics.Follow("Are you scared?", c -> r("...Yeah. Don't leave.", 4, EtoTalk.Effect.CLING, EtoGesture.CLING)))),

        new EtoTopics.Topic("family", c -> c.married() && c.mixed() && !c.pregnant(), c -> "Do you think about a family?",
            c -> c.family().wantsBaby() ? "Tonight. ...You asked. I said yes. Don't make me say it again."
                : "...Sometimes. A small place. A cafe. A kid who kicks. Why?",
            List.of(
                new EtoTopics.Follow("Let's have a baby.", c -> c.babyOpen(),
                    c -> r("...You mean it. ...Okay. Yes. Tonight. Don't you dare make a joke right now.", 6, EtoTalk.Effect.BABY, EtoGesture.SHY)),
                new EtoTopics.Follow("Someday.", c -> r("Someday. Okay. I'll hold you to it.", 2, EtoGesture.SHY)))));

    @Override
    protected List<EtoTopics.Topic> topics() {
        return TOPICS;
    }
}
