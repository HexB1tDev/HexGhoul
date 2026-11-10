package com.hex.kagune.control.npc;

import java.util.List;

/**
 * 4.0.0 (The Great Exploration): Ayato's words (pure, tested). The owner: "edgy, arrogant, confrontational, confident, sarcastic, and
 * occasionally aggressive. His dialogue should reflect his rebellious attitude, his strength, and his unwillingness to openly express
 * vulnerability"; his romance "should not suddenly become affectionate or overly friendly simply because the player begins interacting
 * with him. Instead, his dialogue and behavior should gradually change as the relationship develops, revealing his more protective,
 * jealous, teasing, and vulnerable sides while preserving his usual personality" (and "quite similar to kaneki, animations can be the
 * same"). Touka's little brother, Aogiri Tree's; an Ukaku who calls everyone weak and checks they got home. Strangers get contempt;
 * friends get insults with something under them; his partner gets the same insults, and the something said out loud, once, quietly.
 */
final class AyatoScript extends CompanionScript {
    @Override
    protected String key() {
        return "ayato";
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
        return "I brought you feathers. (the feathers in your hand)";
    }

    @Override
    protected String flowerLabel() {
        return "I got you a wither rose. (the one in your hand)";
    }

    @Override
    protected String lessonLabel(EtoTalk.Context c) {
        return c.teaching().refusal() == Teaching.Refusal.HUMAN ? "Teach me to fight." : "Teach me. (your wings, your kakuja)";
    }

    @Override
    public String title(EtoTalk.Context c) {
        return switch (EtoVoice.of(c)) {
            case WIFE -> "Your husband (he'll deny it)";
            case PARTNER -> "Your boyfriend (don't say it out loud)";
            case CLAIMED -> "Taken";
            case CLOSE -> "Ayato";
            default -> "The black rabbit";
        };
    }

    @Override
    public String greeting(EtoTalk.Context c) {
        int seed = c.seed();
        String line = switch (EtoVoice.of(c)) {
            case CLAIMED -> one(seed, "Lessons, then. Anything else, take it up with {partner}. I'm theirs.", "What. I'm taken. Talk fast.",
                "You again. If you're here to flirt, I'll throw you off the roof.");
            case WARY -> c.night() ? one(seed, "Out at this hour? You've got a death wish or you're stupid. Which.",
                "Keep walking. Before I decide you're a Dove.", "Tch. You smell weak. What do you want?")
                : one(seed, "What are you looking at?", "Who let you in here?", "If you're lost, get lost somewhere else.",
                    "Talk. Or leave. I don't care which.");
            case FRIENDLY -> one(seed, "Oh. You. ...Still alive, huh.", "You came back. Bold. Or dumb. Probably dumb.",
                "{name}. Don't look at me like that, I didn't miss you.", "Hey. You get stronger yet, or are you still dead weight?");
            case CLOSE -> one(seed, "Took you long enough. ...I wasn't waiting.", "{name}. Sit. Not there. ...Fine, there.",
                "You look terrible. Who hit you? Give me a name.", "There you are. I was about to come drag you back myself.");
            case PARTNER -> c.mood() == EtoMood.SULKY ? one(seed, "Don't. I'm not talking to you. ...What.", "Oh, now you remember I exist.")
                : c.daysAway() >= 3 ? one(seed, "Three days. Three. Don't do that again. ...I wasn't worried. I was annoyed.",
                    "Where were you? No, shut up, don't answer. Just stay here a minute.")
                : one(seed, "Hey. ...Come here. Closer. I'm not saying it twice.", "You're late. I almost went looking. Almost.",
                    "Took you long enough. ...Missed you. Whatever. Forget I said that.", "{name}. Good. Now the day doesn't suck.");
            case WIFE -> c.pregnant() ? one(seed, "Sit down. I said sit. You're carrying my kid, you don't get to argue.",
                "If anyone so much as looks at you wrong, I'll tear their wings off. Their arms. Whatever they've got.")
                : one(seed, "Welcome home. ...Yeah, I said it. Don't make it a thing.", "There you are. Come here, idiot.",
                    "Took you long enough. Dinner's raw. Deal with it.", "You're home. Good. I hate it when you're not.");
        };
        return EtoLines.fill(c, line);
    }

    @Override
    public EtoTalk.Reply answer(EtoTalk.Context c, NpcTalk.Choice choice) {
        EtoVoice voice = EtoVoice.of(c);
        int seed = c.seed();
        return switch (choice) {
            case WHO -> new EtoTalk.Reply(voice == EtoVoice.CLAIMED ? EtoLines.fill(c, "Ayato. I'm {partner}'s. That's all you need to know.")
                : "Ayato Kirishima. Aogiri Tree. The one they send when they want it done. ...If you've met my sister, don't believe a word she says "
                + "about me.", c.has(NpcBonds.ASKED) ? 0 : EtoRules.WHO, EtoTalk.Effect.NONE);
            case CHAT -> new EtoTalk.Reply(EtoLines.fill(c, voice.pick(seed,
                s("Make it quick. {partner}'s got eyes everywhere."),
                s("Talk? To you? ...Fine. Thirty seconds.", "I'm listening. Barely."),
                s("Fine. Sit. Don't get comfortable.", "Go on. Bore me."),
                s("Yeah. Sit. ...Tell me something that isn't stupid.", "Talk. I've got nothing better to do. That's not a compliment."),
                s("Talk to me. ...Yeah. I want to. Don't make that face.", "Sit closer. I can't hear you from over there. Liar? Maybe."),
                s("Tell me everything. Who bothered you. I'll handle it.", "Come here and talk. ...I like your voice. Shut up."))),
                c.chattedToday() ? 0 : EtoRules.CHAT, EtoTalk.Effect.TALK);
            case BOOK -> errandItem(c, "Feathers? Where? You're empty-handed.",
                "I've got enough for today. ...Keep them for tomorrow.", "...You kept picking these up for me. Idiot. ...Thanks.",
                "Feathers. Why would I want— ...Black ones. Huh. Fine. I'm keeping them.", EtoGesture.HAIR);
            case FLOWER -> flowerGiven(c, "A flower? You're hallucinating.",
                "You already gave me one today. Don't overdo it.", "A wither rose. ...You actually get me. That's annoying.",
                "A flower for me? Do I look like— ...Wither rose. Okay. That one's fine.", EtoGesture.CROSS);
            case GIFT -> c.meat() ? new EtoTalk.Reply(one(seed, "Meat? For me? ...I could've hunted my own. But fine. Thanks.",
                "Huh. You're not completely useless."), c.giftedToday() ? 0 : EtoRules.GIFT, EtoTalk.Effect.MEAT, EtoGesture.HUFF)
                : r("What dinner? You've got nothing.", 0);
            case PUPPY -> c.puppy() == null ? r("A what? You're seeing things.", 0)
                : new EtoTalk.Reply(this.puppy(c, c.puppy(), false), 8, EtoTalk.Effect.PUPPY, EtoGesture.PET);
            case LETTER -> c.letter() ? new EtoTalk.Reply(this.letter(c, false), c.giftedToday() ? 0 : 4, EtoTalk.Effect.LETTER, EtoGesture.READ)
                : r("What letter? Your hands are empty.", 0);
            case LESSON -> lesson(c, LESSONS);
            case FAVOUR -> errand(c, this.key(),
                "Favours? I don't do favours. ...You want to be useful? Then prove it. Ask me again when you've got the guts.",
                "That's everything I wanted. ...What, you want something from me now? Spit it out.",
                "I don't need anything from you. ...Just keep showing up. That's enough.",
                "{partner} handles that. Back off.", "You're married. Go home to them before I make you.");
            case ASK_OUT -> !c.mayDate() ? r(c.claimed() ? EtoLines.fill(c, "I'm {partner}'s. Don't ask again.")
                : "You've got someone. I don't share and I don't steal. Get lost.", 0)
                : c.step() == EtoRules.Step.ASK ? new EtoTalk.Reply("...You're asking me out. Me. ...You've got terrible taste. ...Yeah. Okay. Yes. "
                    + "But if you tell my sister, I'll kill you.", 0, EtoTalk.Effect.DATE, EtoGesture.HUFF)
                : r("Ask me what? Use your words.", 0);
            case KISS -> c.dating() ? new EtoTalk.Reply(one(seed, "...Fine. Come here. Don't look at me like that.", "Took you long enough to ask."), 0,
                EtoTalk.Effect.KISS) : r("Try that and you lose your face.", 0);
            case HUG -> c.dating() ? new EtoTalk.Reply(this.hugged(seed), 0, EtoTalk.Effect.HUG) : r("Don't touch me.", 0);
            case SWEET -> c.dating() ? new EtoTalk.Reply(EtoLines.fill(c, one(seed, "...Don't. You'll make me say something stupid.", "Go on. I'm listening. "
                + "Not because I like it.", "Careful. I bite.")), 0, EtoTalk.Effect.SWEET) : r("Sweet? Me? Get your head checked.", 0);
            case NIGHT -> !c.dating() ? r("In your dreams. Get out.", -2) : new EtoTalk.Reply(c.night() && !c.nightUsed()
                ? one(seed, "...Yeah. Stay. Not because I'm asking. ...I'm asking.", "Fine. The door locks. Nobody comes in. Nobody.")
                : one(seed, "It's still daylight, idiot. Later.", "Not tonight. ...Stay close anyway."), 0,
                c.night() && !c.nightUsed() ? EtoTalk.Effect.NIGHT : EtoTalk.Effect.NONE);
            case PROPOSE -> new EtoTalk.Reply("", 0, EtoTalk.Effect.PROPOSE);
            case HOME -> c.stage() == Romance.Stage.MARRIED ? new EtoTalk.Reply(one(seed, "Ours. ...Yeah. Ours. Anyone comes near it, they die.",
                "This is home now. Don't make me say it nicer than that."), 2, EtoTalk.Effect.HOME)
                : new EtoTalk.Reply("Ours? We're not married. Slow down.", 0, EtoTalk.Effect.NONE);
            case FOLLOW -> new EtoTalk.Reply(voice.together() ? one(seed, "Yeah, yeah. Someone has to keep you alive.", "Lead. I'll be behind you. "
                + "Anyone tries anything, they're dead.") : one(seed, "Fine. I'm bored anyway. Don't slow me down.", "I'll come. Not for you. For the fight."),
                0, EtoTalk.Effect.FOLLOW);
            case STAY -> new EtoTalk.Reply(voice.together() ? one(seed, "Here? ...Fine. Come back. Soon.", "I'll wait. Hurry up.")
                : one(seed, "Whatever. I'll be here.", "Don't take forever."), 0, EtoTalk.Effect.STAY);
            case DECLINE -> r("Tch. Your loss.", 0);
            case TEASE -> new EtoTalk.Reply(one(seed, "Say that again and see what happens. ...Okay, that was a little funny.",
                "I'm not short. You're tall. There's a difference.", "Keep it up. I know where you sleep.", "...You're lucky I like you. Don't repeat that."),
                1, EtoTalk.Effect.NONE, EtoGesture.HUFF);
            case INSULT -> NpcBonds.clamp(c.regard() + EtoRules.INSULT) <= NpcBonds.ENEMY
                ? new EtoTalk.Reply("That's it. You're done. Next time I see you, I'm not holding back.", EtoRules.INSULT, EtoTalk.Effect.ANGER,
                EtoGesture.ANGRY)
                : new EtoTalk.Reply(voice == EtoVoice.CLAIMED ? EtoLines.fill(c, "Keep talking. {partner} will love hearing it. Get out.")
                : one(seed, "Big words from someone that weak. Get out of my sight.", "Say that again. I dare you."), EtoRules.INSULT, EtoTalk.Effect.CLOSE,
                EtoGesture.SCOLD);
            case BACK -> new EtoTalk.Reply(EtoLines.fill(c, voice.pick(seed, s("What else?"), s("Anything else? No?"), s("What now?"), null,
                s("What. ...What is it?", "Mm? Go on."))), 0, EtoTalk.Effect.MAIN);
            case LEAVE -> new EtoTalk.Reply("", 0, EtoTalk.Effect.CLOSE);
            default -> r("Huh?", 0);
        };
    }

    /** How he teaches: impatiently, and only the ones who earned it. */
    static final LessonWords LESSONS = new LessonWords() {
        @Override
        public String teach(Teaching.Lesson lesson, EtoTalk.Context c) {
            return switch (lesson.gate()) {
                case UKAKU_AYATO_DOVES -> "Three Doves. Not bad. ...Don't let it go to your head. Doves are slow. Next time I want to see you fight "
                    + "something that fights back. (Ayato's respect: the first step.)";
                case UKAKU_AYATO_HUNT -> "A-ranks. All three. And you're still standing. ...Okay. Maybe you're not weak. Maybe. One more and I'll show "
                    + "you what your wings really are. (Ayato's respect: the second step.)";
                case UKAKU_KAKUJA -> "You killed an S-rank. ...Fine. You earned this. Sit down. Don't scream. Actually, scream if you want. It "
                    + "doesn't change anything. Your wings are going to eat you from the inside, and you're going to let them. Ready? Doesn't matter.";
                // 4.1.3: Speedster.
                case STYLE_SPEEDSTER -> "Six. With your hands. ...Your knuckles look terrible. Good. Now forget all of it. Don't plant your "
                    + "feet. Don't wind up. Hit, and be gone before it lands. Hit again from where they aren't looking. When they swing, you're "
                    + "already over their head. That's it. That's the whole thing. Nobody hits what they can't keep in front of them. (Speedster: "
                    + "with your eyes open and your kagune in, M1 is the Speed Blitz and right click flips.)";
                default -> "Watch. I'm not doing it twice. (" + lesson.name() + ")";
            };
        }

        @Override
        public String refuse(Teaching.Refusal refusal, Teaching.View view, EtoTalk.Context c) {
            return switch (refusal) {
                case HUMAN -> "You're human. I don't teach humans. I eat them. ...Go home.";
                case WRONG_PATH -> "That's not an Ukaku. I don't know anything about whatever crawls out of your back. Ask someone who cares.";
                case WEAK -> "You? Not a chance. You'd die and I'd have to clean it up. Get stronger. (" + view.next().name() + ": mastery " + view.need() + ")";
                case BEFORE -> "You don't even have " + (view.before() == null ? "the basics" : view.before().displayName()) + ". My sister can "
                    + "teach you that. Go bother her.";
                case ALL_TAUGHT, GHOUL -> "I've got nothing left to teach you. ...Don't make it weird. Go find something stronger to fight.";
            };
        }
    };

    @Override
    public String trialWords(Teaching.Lesson lesson, EtoTalk.Context c) {
        return together(c) ? "Ayato is holding you up by the collar when the world comes back.\n\"Hey. Hey. Look at me. ...You're still you. Good. "
            + "Don't scare me like that again.\""
            : "Ayato is sitting on the crate across from you, watching, when the world comes back.\n\"Took you long enough. ...Not bad. "
            + "Your wings are yours now. All of them.\"";
    }

    @Override
    protected String[][] sweetSays() {
        return new String[][] {{"You're cute when you scowl.", "cute"}, {"I love you.", "love"}, {"I missed you.", "missed"},
            {"You're strong.", "strong"}, {"Your hair's soft.", "hair"}, {"Lean on me.", "lean"}, {"You don't have to be tough with me.", "soft"},
            {"Smile for once.", "smile"}, {"Protect me.", "protect"}, {"Why me?", "why"}};
    }

    @Override
    public EtoTalk.Reply sweetly(EtoTalk.Context c, String what) {
        int seed = c.seed();
        boolean married = c.married();
        EtoTalk.Reply reply = switch (what) {
            case "cling" -> new EtoTalk.Reply(one(seed, "Like this? ...Yeah. Don't move.", "I've got you. Don't make it weird."), 2, EtoTalk.Effect.CLING,
                EtoGesture.NONE);
            case "hand" -> new EtoTalk.Reply(one(seed, "...Fine. Here. If anyone sees, I'm dragging you.", "My hand? ...Whatever. Take it."), 1,
                EtoTalk.Effect.HAND, EtoGesture.NONE);
            case "cute" -> r(one(seed, "I'm not cute. I'm terrifying. ...Stop smiling.", "Say that again and I'll— ...Shut up. Okay. Thanks."), 2,
                EtoGesture.HUFF);
            case "love" -> married ? r(one(seed, "...I know. Me too. More than I'll ever say again, so remember it.", "Yeah. I love you. There. Happy?"),
                3, EtoTalk.Effect.NONE, EtoGesture.HUG)
                : r(one(seed, "...Don't say that so easy. ...I— yeah. Me too. Don't make me say it.", "You're an idiot. ...I love you too. Shut up."), 3,
                    EtoTalk.Effect.NONE, EtoGesture.SHY);
            case "missed" -> r(one(seed, "...Yeah. Me too. Don't leave that long again. That's an order.", "I didn't miss you. ...I missed you."), 2,
                EtoTalk.Effect.CLING, EtoGesture.CLING);
            case "strong" -> r(one(seed, "Obviously. ...But it sounds better from you.", "Stronger than anyone. Strong enough for both of us. Remember that."), 2,
                EtoGesture.CROSS);
            case "hair" -> r(one(seed, "Don't— ...okay. You can keep doing that. Just for a minute.", "My sister used to do that. ...Don't stop."), 2,
                EtoGesture.HAIR);
            case "lean" -> r(one(seed, "On you? I'm not tired. ...Five minutes.", "...Yeah. Okay. Just this once."), 2, EtoTalk.Effect.CLING, EtoGesture.CLING);
            case "soft" -> r(one(seed, "...Don't. If I stop being tough, everything I've got falls apart. ...With you it doesn't. That's the scary part.",
                "You don't get it. Everyone I let in either leaves or dies. ...You'd better not. Either."), 4, EtoTalk.Effect.HUG, EtoGesture.HUG);
            case "smile" -> r(one(seed, "I don't smile. ...That wasn't a smile. Shut up.", "Make me. ...Damn it."), 2, EtoGesture.SHY);
            case "protect" -> r(one(seed, "Always. Anyone touches you, I'll tear their wings off and make them eat them.", "You didn't have to ask. "
                + "I've been doing it the whole time."), 3, EtoTalk.Effect.NONE, EtoGesture.CROSS);
            case "why" -> r(one(seed, "Because you came back. Everyone else ran. You kept coming back, every time, like an idiot. ...My idiot.",
                "You saw me at my worst and you didn't flinch. Nobody does that. Nobody. ...So don't ever stop."), 4, EtoTalk.Effect.HUG, EtoGesture.HUG);
            case "baby", "tonight" -> EtoTalk.babyAnswer(c, what, s("...A kid. With me. ...Yeah. Tonight. I'll protect it with everything.",
                    "You want a family. With me. ...Okay. Okay. Tonight. Don't tell my sister yet.",
                    "Yeah. I want that too. I never said it because I didn't think I could. Tonight.",
                    "A kid who's half you. ...Tonight. And nobody's ever touching them. Ever."),
                s("Another one? ...Yeah. A sibling. Everyone needs someone to fight with. Tonight.",
                    "Two of them to protect. ...Fine. Tonight. I can handle it.",
                    "...You sure? You were exhausted last time. ...You're sure. Okay. Tonight. I'll be careful."),
                days -> "Already? You need to rest. ...Ask me in " + EtoTalk.days(days) + ". I'll still say yes. Don't tell anyone.",
                s("I didn't forget. I've been thinking about it all day. ...Tonight.", "Tonight. Lock the door. I mean it.",
                    "Still yes. Stop asking. ...Tonight."),
                EtoGesture.HUG, EtoGesture.SHY);
            default -> r("What?", 0);
        };
        return new EtoTalk.Reply(EtoLines.fill(c, reply.line()), reply.regard(), reply.effect(), reply.gesture());
    }

    @Override
    public String giftsAsked(int seed) {
        return one(seed, "What, you brought something? Let's see.", "For me? ...Show me.");
    }

    @Override
    public String topicsAgain(int seed) {
        return one(seed, "What else?", "Anything else, or are we done?", "Go on.");
    }

    @Override
    public String busy() {
        return "Ayato is busy. His wings are busier.";
    }

    @Override
    public String approach(EtoTalk.Context c) {
        int seed = c.seed();
        if (c.pregnant() && seed % 2 == 0) {
            return this.pregnant(c.family().stage(), seed);
        }
        return EtoLines.fill(c, switch (c.mood()) {
            case CLINGY, LONELY -> one(seed, "...Hey. Nothing. I was bored. Stay here.", "You've been gone all day. Don't do that.");
            case AFFECTIONATE -> one(seed, "Come here. ...No reason.", "I just wanted to see your stupid face.");
            case SULKY, JEALOUS -> one(seed, "Who was that? Don't lie to me.", "I saw you. With them. ...I don't care. I'm going for a walk.");
            case GIDDY -> one(seed, "I took down three Doves before breakfast. Be impressed.", "Good day. Don't ruin it.");
            case SLEEPY -> one(seed, "I didn't sleep. Stay. ...Shut up, I'm not tired.", "Wake me if anyone comes.");
            case BLOODTHIRSTY -> one(seed, "Stay back. I'm hungry and I don't trust me right now.", "My wings won't settle. Give me space.");
            default -> one(seed, "Hey. Walk with me.", "{name}. I've got nothing to do. Entertain me.", "You look bored. So am I. Come on.");
        });
    }

    @Override
    public String welcome(EtoTalk.Context c) {
        return one(c.seed(), "Finally. ...Come here. Don't say anything.", "You're back. Good. Now I can stop pacing.",
            "Took you long enough. I kept your side warm. Shut up.");
    }

    @Override
    public String kissed(int seed, boolean married) {
        return married ? one(seed, "...Still not used to that. Good.", "Again. ...I said again.", "Mine. Got it? Mine.")
            : one(seed, "...Don't look at me. I'm— shut up.", "That was— fine. That was fine. Do it again.", "If you tell anyone, I'll kill you. ...Again.");
    }

    @Override
    public String peck(int seed) {
        return one(seed, "What? I felt like it.", "Don't make a thing of it.", "You had something on your face. ...My mouth.");
    }

    @Override
    public String kissWaiting(String name) {
        return name + " grabs your collar, pulls you down to his height, and doesn't look away...";
    }

    @Override
    public String kissHint(int seed) {
        return one(seed, "...Come here a sec.", "You're staring. So am I. Hurry up.");
    }

    @Override
    public String babyNight(EtoTalk.Context c) {
        return one(c.seed(), "It's dark. ...You still want this? Good. Me too.", "Lock the door. I've been thinking about it all day.");
    }

    @Override
    public String held(int seed) {
        return one(seed, "Don't let go yet. ...That's not a request.", "Five more seconds. ...Ten.", "Stay. Just like this.");
    }

    @Override
    public String pulled(int seed) {
        return one(seed, "Come here. I'm not done with you.", "You're not going anywhere.", "Closer. ...There.");
    }

    @Override
    public String hanging(int seed) {
        return one(seed, "...Fine. Whatever.", "Tch. Your loss.", "Forget it. I didn't want to anyway.");
    }

    @Override
    public String hugged(int seed) {
        return one(seed, "...You're squeezing too hard. Don't stop.", "Ugh. Fine. One hug. ...Two.", "Don't tell anyone about this.");
    }

    @Override
    public String handTaken(int seed) {
        return one(seed, "Hold it properly. People are watching. Let them.", "Your hand's freezing. ...Keep it there.", "Walk. I'll keep up.");
    }

    @Override
    public String handLet(int seed) {
        return one(seed, "...Okay.", "Whatever.");
    }

    @Override
    public String partnerHit(int seed, boolean married) {
        return married ? one(seed, "...What was that for?", "Do that again and we're going to have a problem.")
            : one(seed, "Hey! What the hell?", "You want to fight? Fine. Not like this.");
    }

    @Override
    public String serious(int seed) {
        return one(seed, "You picked the wrong guy.", "Last warning.", "You're already dead. You just don't know it yet.");
    }

    @Override
    public String maniac(int seed) {
        return one(seed, "Is that all you've got?!", "Come on! Hit me! HIT ME!", "Pathetic. You're all pathetic!");
    }

    @Override
    public String protect(EtoTalk.Context c) {
        return one(c.seed(), "Touch them again and I'll rip your wings off.", "Get behind me. NOW.", "Nobody hurts what's mine.");
    }

    @Override
    public String won(int seed) {
        return one(seed, "Too easy.", "Weak.", "That all? Boring.");
    }

    @Override
    public String puppy(EtoTalk.Context c, String kind, boolean sorry) {
        String what = kind == null ? "that" : "a " + kind;
        return sorry ? one(c.seed(), "You think " + what + " fixes it? ...It helps. Fine. You're forgiven.",
            "Bringing me " + what + " to say sorry is cheating. ...It worked.")
            : one(c.seed(), "Is that " + what + "? ...I'm not keeping it. ...It's sleeping on me. I guess I'm keeping it.",
                "Why would I want " + what + "? ...Fine. It stays. It's not cute.");
    }

    @Override
    public String letter(EtoTalk.Context c, boolean sorry) {
        return sorry ? one(c.seed(), "You wrote this? ...Fine. I'm not mad anymore. Don't make me read it out loud.", "Idiot. ...Apology accepted.")
            : one(c.seed(), "A letter. For me. ...I'll read it later. Alone. Don't watch me read it.", "Who writes letters anymore? ...Thanks.");
    }

    @Override
    public String snub(int seed, boolean enemy) {
        return enemy ? one(seed, "Ayato's eyes go red. His wings flicker behind him.", "Ayato turns his back on you. On purpose.")
            : one(seed, "Ayato doesn't feel like talking right now.", "Ayato ignores you. Loudly.");
    }

    @Override
    public String ring(EtoTalk.Context c, Romance.Refusal refusal) {
        if (refusal == null) {
            return one(c.seed(), "...You're serious. You're actually— ...Yeah. YES. Idiot. Give me that. ...Don't cry, then I'll— shut up.",
                "Marry you? ...Yeah. Forever. And anyone who tries to take you away from me dies. That's my vow.");
        }
        return switch (refusal) {
            case NOT_DATING -> "A ring? We're not even— put that away before someone sees.";
            case CLAIMED -> EtoLines.fill(c, "I'm {partner}'s. Back off.");
            case MARRIED_ELSEWHERE -> "You're already married. Go home.";
            case TOO_SOON -> "Too fast. Not no. Ask me again when I believe you mean it.";
            case NOT_CLOSE -> "Not yet. You haven't earned that. Yet.";
            case ALREADY -> "We're already married. Did you hit your head?";
        };
    }

    @Override
    public String morning(EtoTalk.Context c) {
        return one(c.seed(), "Ayato is already up, arms crossed, watching the door.\n\"Morning. ...Nobody came. I checked. Twice.\"",
            "Ayato pretends to be asleep when you look at him.\n\"...I wasn't watching you sleep. Shut up.\"");
    }

    @Override
    public String conceived(EtoTalk.Context c) {
        return one(c.seed(), "Ayato keeps glancing at you over breakfast.\n\"...Feel different? ...Me neither. ...Maybe.\"",
            "Ayato stands at the door longer than he needs to.\n\"Nobody gets near you now. Nobody.\"");
    }

    @Override
    public String pregnant(com.hex.kagune.control.family.Pregnancy.Stage stage, int seed) {
        return switch (stage) {
            case EARLY -> one(seed, "Sit. Eat. I hunted. Don't argue.", "If you feel sick, tell me. I'll... figure something out.");
            case ALONG -> one(seed, "...It kicked. It KICKED. That's a fighter. That's my kid.", "I talk to it when you're asleep. Don't tell anyone.");
            case LATE -> one(seed, "Don't carry that. Don't carry anything. I'll carry you.", "Not long now. ...I'm not scared. Shut up.");
            case DUE -> one(seed, "Any day. I'm not leaving. Doves can wait.", "Breathe. I'm here. I'm not going anywhere.");
        };
    }

    @Override
    public String saw(Display shown, EtoVoice voice, boolean home, int seed) {
        boolean ours = voice.together();
        return switch (shown) {
            case NONE -> "";
            case EYES -> ours ? one(seed, "Red suits you. Don't let anyone else see.", "Your eyes. ...Good. Now you look like you mean it.")
                : one(seed, "Put those away before a Dove sees.", "Oh, you're one of us. Whatever.");
            case KAGUNE -> ours ? one(seed, "That's it? ...It's fine. It'll get bigger.", "Not bad. Mine's better.")
                : one(seed, "You want to fight? Fine.", "Put it away unless you plan to use it.");
            case KAKUJA -> ours ? one(seed, "Hey. HEY. Come back. You're still in there.", "Listen to me. Only me. Come back.")
                : one(seed, "Don't lose yourself in there. Nobody comes to get you.", "...Careful. That thing eats people.");
            case DREAD -> ours ? one(seed, "What the hell is that? ...Come back. NOW.", "I'm not losing you to that.")
                : one(seed, "...Okay. That's a problem. Run.", "Even I'm not stupid enough to fight that.");
        };
    }

    @Override
    public String dread(FinalSight sight, EtoVoice voice, boolean home, int seed) {
        boolean ours = voice.together();
        return switch (sight) {
            case WAR -> ours ? one(seed, "Take that off. Now. ...Please.", "That's not you. Come back before it's all that's left.")
                : one(seed, "...Even I know when to run.", "That's not a fight. That's a funeral.");
            case PEACE -> ours ? one(seed, "...Since when can you do that? ...Okay. I'm a little impressed. A little.", "That's the one-eyed king's. "
                + "And it's yours now. Huh.") : one(seed, "That's not yours. Where did you get that?", "...I'm not fighting that today.");
            case DRAGON -> ours ? one(seed, "COME BACK. I'll drag you out of that thing myself.", "I'm not losing you. Not to that.")
                : one(seed, "Run. Everyone run.", "That thing eats cities. Move.");
            case RINKAKU -> ours ? one(seed, "That's a lot of legs. ...Come back to me.", "Easy. Easy. Breathe.")
                : one(seed, "Tch. Rinkaku freaks. Don't get close to me.", "Keep that thing away from me.");
            case UKAKU -> ours ? one(seed, "...Okay. That's beautiful. Don't tell anyone I said that.", "You took it all the way. ...Show-off.")
                : one(seed, "Ukaku, all the way? ...Not bad. Still not better than me.", "Huh. You're not weak.");
            case NONE -> "";
        };
    }

    @Override
    public EtoGesture dreadGesture(FinalSight sight, boolean ours) {
        return switch (sight) {
            case WAR, DRAGON -> EtoGesture.FLINCH;
            case PEACE -> EtoGesture.AWE;
            case RINKAKU -> EtoGesture.CROSS;
            case UKAKU -> ours ? EtoGesture.AWE : EtoGesture.HUFF;
            case NONE -> EtoGesture.NONE;
        };
    }

    @Override
    public String initiative(EtoInitiative.Act act, int seed) {
        return switch (act) {
            case HAND -> one(seed, "Give me your hand. ...So nobody gets the wrong idea about who you're with.", "Hand. Now. Don't argue.");
            case CLING -> one(seed, "Stay close. Closer. That's your spot.", "Over here. Where I can see you.");
            case HUG -> one(seed, "...C'mere. Don't say anything.", "Just— stay still a second.", "I'm not hugging you. I'm keeping you from running.");
            case LEAN -> one(seed, "Lean on me. I won't break.", "Rest. I'm on watch.");
            case HUM -> one(seed, "*hums something under his breath, then stops when you look* ...What.", "...My sister used to hum this. Don't tell her.");
            case READ -> one(seed, "Read to me. ...I like your voice. Don't make it weird.", "Got anything to read? I'm bored.");
            case DANCE -> one(seed, "Dance? ...Fine. Once. If you step on me, I bite.", "Come here. No music. Doesn't matter.");
            default -> "";
        };
    }

    @Override
    public String moment(EtoInitiative.Moment moment, boolean together, int seed) {
        return switch (moment) {
            case KILL -> together ? one(seed, "Clean. ...You're getting good.", "Not bad. I'd have done it faster.") : one(seed, "Hmph.", "Finally.");
            case HURT -> together ? one(seed, "You're bleeding. WHO did this?", "Let me see. ...I'll kill them.", "Don't you dare die on me.")
                : one(seed, "You're bleeding. Idiot.", "Get somewhere safe before you embarrass yourself.");
            case RAIN -> together ? one(seed, "Rain. ...Stay here with me till it stops.", "My wings hate the rain. You don't. Stay.")
                : one(seed, "Rain. Great.", "Tch. Rain.");
            case DUSK -> together ? one(seed, "Night's coming. Stay close. Doves hunt in the dark.", "Sunset. ...It's fine. You're here.")
                : one(seed, "Night. Finally.", "The good part of the day.");
            case DAWN -> together ? one(seed, "Morning. ...You stayed. Good.", "Sun's up. We made it. Again.") : one(seed, "Morning.", "Tch. Daylight.");
        };
    }

    static final List<EtoTopics.Topic> TOPICS = List.of(
        new EtoTopics.Topic("aogiri", c -> true, c -> "What's Aogiri Tree?",
            c -> EtoVoice.of(c).pick(c.seed(),
                s("None of your business."),
                s("The ones who don't hide. We don't serve coffee and pretend. We take what's ours.", "The strong ones. You wouldn't last a day."),
                s("Family. The kind you pick. ...The kind that doesn't run when it gets bad.", "Ghouls who stopped hiding."),
                null,
                s("...The place I went when I didn't have anywhere else. Don't worry. I'd leave it for you."),
                s("Old news. You're my family now. They know that.")),
            List.of(
                new EtoTopics.Follow("Can I join?", c -> r("You? Prove you're not weak first. Then we'll talk.", 1, EtoGesture.CROSS)),
                new EtoTopics.Follow("Why did you join?", c -> r("Because the other way was waiting to die quietly in a cafe. I'm not my sister. "
                    + "I don't wait.", 2, EtoGesture.HUFF)),
                new EtoTopics.Follow("Do you trust them?", c -> r(together(c) ? "...Not like I trust you. Nobody gets that." : "Trust is for people "
                    + "who want to die. I trust what I can see.", together(c) ? 4 : 1, EtoGesture.LOOK)))),

        new EtoTopics.Topic("sister", c -> c.regard() >= NpcBonds.FRIENDLY, c -> "You have a sister?",
            c -> one(c.seed(), "Touka. She works at that cafe with the humans. Pretending. ...She's an idiot.",
                "Yeah. Touka. Don't bring her up. ...Is she eating? Never mind. Forget it."),
            List.of(
                new EtoTopics.Follow("You care about her.", c -> r("I don't. ...Shut up. Okay. A little. If you tell her, I'll kill you.", 3, EtoGesture.HUFF)),
                new EtoTopics.Follow("Why don't you talk?", c -> r("Because she chose them. The humans. And I chose this. ...Some fights you don't "
                    + "win by talking.", 2, EtoGesture.CROSS)),
                new EtoTopics.Follow("She misses you.", c -> r("...She said that? ...She didn't say that. You're lying. ...Did she really?", 3,
                    EtoGesture.SHY)))),

        new EtoTopics.Topic("father", c -> c.regard() >= NpcBonds.FRIEND, c -> "What happened to your father?",
            c -> "The Doves took him. When we were kids. He told us to hide, to live quietly, to be good. ...Look where that got him.",
            List.of(
                new EtoTopics.Follow("Is that why you fight?", c -> r("Someone has to. If being quiet gets you killed anyway, you might as well be loud.", 3,
                    EtoGesture.POSE)),
                new EtoTopics.Follow("I'm sorry.", c -> r("Don't be. Sorry doesn't bring anyone back. ...But. Thanks.", 3, EtoGesture.LOOK)),
                new EtoTopics.Follow("Come here.", c -> together(c), c -> r("...Just for a minute. Don't look at me.", 4, EtoTalk.Effect.HUG,
                    EtoGesture.HUG)))),

        new EtoTopics.Topic("wings", c -> true, c -> "Your wings...",
            c -> one(c.seed(), "Ukaku. Fast. Ranged. Burns when it runs hot. Better than anything else crawling out of anyone's back.",
                "Crystal when I need it, fire when I don't. Want to see? ...No. Not until you earn it."),
            List.of(
                new EtoTopics.Follow("Teach me.", c -> r("Earn it. Ask me properly and do what I tell you. I don't teach the weak.", 0, EtoGesture.CROSS)),
                new EtoTopics.Follow("They're beautiful.", c -> r(together(c) ? "...Yeah? ...You're the only one who's ever said that like you meant it."
                    : "Beautiful? They're weapons. ...But yeah. They are.", together(c) ? 4 : 2, EtoGesture.SHY)),
                new EtoTopics.Follow("Does it hurt?", c -> r("Every time. You get used to it. That's the bad part.", 2, EtoGesture.LOOK)))),

        new EtoTopics.Topic("height", c -> c.regard() >= NpcBonds.FRIEND, c -> "You're kind of short.",
            c -> one(c.seed(), "Say that again. I dare you.", "I'm not short. Everyone else is unnecessarily tall."),
            List.of(
                new EtoTopics.Follow("It's cute.", c -> r(together(c) ? "...Fine. You can say it. Only you." : "Cute? I'll show you cute.", together(c) ? 3 : -1,
                    EtoGesture.HUFF)),
                new EtoTopics.Follow("Sorry.", c -> r("You'd better be.", 1, EtoGesture.CROSS)))),

        new EtoTopics.Topic("jealous", c -> together(c), c -> "Are you jealous?",
            c -> one(c.seed(), "Of who? That idiot? ...Yes. Obviously. Stop talking to them.", "I'm not jealous. I just think everyone you smile at "
                + "should be thrown off a roof."),
            List.of(
                new EtoTopics.Follow("Only you, Ayato.", c -> r("...Good. Say it again. ...Okay. Okay.", 4, EtoGesture.SHY)),
                new EtoTopics.Follow("That's sweet.", c -> r("It's not sweet. It's a warning.", 2, EtoGesture.HUFF)))),

        new EtoTopics.Topic("kagune", c -> c.ghoul(), c -> "How did you get so strong?",
            c -> one(c.seed(), "I never stopped. Every fight, every Dove, every ghoul who thought I was a kid. I ate them all.",
                "By not having anything to lose. ...That changed. Doesn't matter. I'm still stronger."),
            List.of(
                new EtoTopics.Follow("Teach me the kakuja.", c -> r("Earn it first. My respect isn't free. Ask me properly.", 0, EtoGesture.CROSS)),
                new EtoTopics.Follow("Changed how?", c -> r(together(c) ? "...You. Obviously. Now I've got something to lose. I hate it." : "None of your business.",
                    together(c) ? 4 : 0, together(c) ? EtoGesture.SHY : EtoGesture.HUFF)))),

        new EtoTopics.Topic("baby", EtoTalk.Context::pregnant, c -> "How's our baby?",
            c -> switch (c.family().stage()) {
                case EARLY -> "Small. Fine. Sleep more. That's an order.";
                case ALONG -> "Kicking. Strong. Obviously. It's mine.";
                case LATE -> "Soon. I cleared the hideout of anyone who breathes too loud.";
                case DUE -> "Any day. Nobody gets through that door. Nobody.";
            },
            List.of(
                new EtoTopics.Follow("Will they have wings?", c -> r("They'd better. ...Doesn't matter. I'll protect them either way.", 4, EtoGesture.HUG)),
                new EtoTopics.Follow("Hold me.", c -> r("...Come here. Both of you.", 4, EtoTalk.Effect.CLING, EtoGesture.CLING)))),

        new EtoTopics.Topic("family", c -> c.married() && !c.pregnant() && c.family().children() == 0, c -> "Do you want a family?",
            c -> c.family().wantsBaby() ? "Tonight. ...If you still want to. I do."
                : "I had one. They took it. ...With you, I'd build one nobody could take.",
            List.of(
                new EtoTopics.Follow("Someday.", c -> r("Someday. ...Don't make me wait too long.", 2, EtoGesture.SHY)))));

    @Override
    protected List<EtoTopics.Topic> topics() {
        return TOPICS;
    }
}
