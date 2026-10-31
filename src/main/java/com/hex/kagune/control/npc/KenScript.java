package com.hex.kagune.control.npc;

import java.util.List;

/**
 * 1.3.14: Ken's words (pure, tested). The one-eyed king: a Rinkaku made, not born, who was a quiet boy who read too much until someone
 * took him for ten days. Polite, gentle, apologetic, a little sad, fond of coffee and of books (one writer's most of all, without
 * knowing who she is); calm in a way that frightens people when he talks about pain, and a finger cracked under his thumb when he
 * is thinking about it. He teaches what it costs to go further: the centipede and the Ukaku's final form through what was done to him,
 * the Full Kakuja, and the Dragon. Strangers get courtesy; friends get honesty; his partner gets all of him.
 */
final class KenScript extends CompanionScript {
    @Override
    protected String her() {
        return "him";
    }

    @Override
    protected String spouse() {
        return "husband";
    }

    /** Only a woman (and him a man: {@code mixed}). */
    @Override
    protected boolean romantic(EtoTalk.Context c) {
        return c.mixed();
    }

    @Override
    protected String errandLabel() {
        return "I brought you a book. (the book in your hand)";
    }

    @Override
    protected String flowerLabel() {
        return "I picked you a white flower. (the one in your hand)";
    }

    @Override
    protected String lessonLabel(EtoTalk.Context c) {
        return c.teaching().refusal() == Teaching.Refusal.HUMAN ? "Teach me to fight." : "Teach me what you know.";
    }

    @Override
    public String title(EtoTalk.Context c) {
        return switch (EtoVoice.of(c)) {
            case WIFE -> "Your husband";
            case PARTNER -> "Your boyfriend";
            case CLAIMED -> "Taken";
            case CLOSE -> "Ken";
            default -> "The one-eyed king";
        };
    }

    @Override
    public String greeting(EtoTalk.Context c) {
        int seed = c.seed();
        String line = switch (EtoVoice.of(c)) {
            case CLAIMED -> one(seed, "Hello. If it's about your kagune, I'll help. Anything else... I'm {partner}'s. I'm sorry.",
                "Ah. Lessons? Of course. Please keep it to that.");
            case WARY -> c.night() ? one(seed, "It's late to be walking around. Are you lost?", "...You smell like a ghoul. Relax. So do I.",
                "Good evening. Please don't make me hurt you.")
                : one(seed, "Hello. Can I help you?", "Oh, sorry, I was reading. Did you need something?", "Hello. You look like you have a question.");
            case FRIENDLY -> one(seed, "Ah, {name}. Hello again. Coffee?", "You came back. That's... nice. Sit, if you like.",
                "I just finished a chapter. Perfect timing.", "{name}. You look tired. Did you eat?");
            case CLOSE -> one(seed, "{name}. I was hoping you'd come.", "Hey. I kept the good beans for you.", "There you are. I was starting to worry. "
                + "I always start to worry.", "Sit with me. I don't want to read alone today.");
            case PARTNER -> c.mood() == EtoMood.SULKY ? one(seed, "...Oh. Hi.", "I'm fine. I'm just... I'm fine.")
                : c.daysAway() >= 3 ? one(seed, "You were gone three days. I counted. ...I'm sorry, that's a bad habit of mine.",
                    "You're back. I'm so glad you're back.")
                : one(seed, "Hey, you. Come here.", "{name}. ...Sorry. I just like saying your name.", "I made coffee. Two cups. I hoped.",
                    "You're here. Everything's quieter when you're here.");
            case WIFE -> c.pregnant() ? one(seed, "How are you feeling? Sit. I'll get you anything. Everything.",
                "I read four books about babies today. I'm more scared than before.")
                : one(seed, "Welcome home.", "There you are. I missed you. I always miss you.", "Hey. Dinner's ready. It's coffee. I'm sorry.",
                    "You're home. I love that word now.");
        };
        return EtoLines.fill(c, line);
    }

    @Override
    public EtoTalk.Reply answer(EtoTalk.Context c, NpcTalk.Choice choice) {
        EtoVoice voice = EtoVoice.of(c);
        int seed = c.seed();
        return switch (choice) {
            case WHO -> new EtoTalk.Reply(voice == EtoVoice.CLAIMED ? EtoLines.fill(c, "Ken. I belong to {partner}. I'll still help you, if I can.")
                : "Ken. I was a student. I liked books and coffee. Then I was a ghoul. Now I'm... whatever this is. People call me a king. "
                + "I'd rather they didn't.", c.has(NpcBonds.ASKED) ? 0 : EtoRules.WHO, EtoTalk.Effect.NONE);
            case CHAT -> new EtoTalk.Reply(EtoLines.fill(c, voice.pick(seed,
                s("Briefly. I'm sorry, {partner} worries."),
                s("Of course. What about?", "I don't talk much. But I listen well."),
                s("Sure. Sit. I'll put the book down.", "I'd like that."),
                s("Always. Tell me something good. I collect them.", "Talk to me. Please."),
                s("Talk to me. I could listen to you all day.", "Sit closer. ...Sorry. Is that okay?"),
                s("Tell me about your day. All of it.", "Come here and talk. I've been waiting."))),
                c.chattedToday() ? 0 : EtoRules.CHAT, EtoTalk.Effect.TALK);
            case BOOK -> errandItem(c, "A book? I don't see one.",
                "Oh, I haven't read this one. That's {n} of three. Thank you.", "Three. I'll read them tonight. All of them. ...Was there something "
                    + "else? You can ask me again.",
                "I haven't finished the last one yet. Tomorrow?", "Another one? You know exactly how to make me happy.",
                "A book... for me? I don't know what to say. Thank you. Really.", EtoGesture.READ);
            case FLOWER -> flowerGiven(c, "A flower? Where?",
                "A white one. ...I used to dream of flowers, back then. Red ones. White suits me better now, I think. ...Was there something you "
                    + "wanted to ask?",
                "I still have the last one, pressed in a book.", "Thank you. I'll keep this one too.", "For me? ...People don't usually give me things. "
                    + "Thank you.", EtoGesture.BOW);
            case GIFT -> c.meat() ? new EtoTalk.Reply(one(seed, "...You brought me something to eat. I'm always hungry. I never say. Thank you.",
                "You noticed I haven't been eating. ...Thank you."), c.giftedToday() ? 0 : EtoRules.GIFT, EtoTalk.Effect.MEAT, EtoGesture.BOW)
                : r("Dinner? Where?", 0);
            case PUPPY -> c.puppy() == null ? r("A what? I'm sorry, I don't see one.", 0)
                : new EtoTalk.Reply(this.puppy(c, c.puppy(), false), 8, EtoTalk.Effect.PUPPY, EtoGesture.PET);
            case LETTER -> c.letter() ? new EtoTalk.Reply(this.letter(c, false), c.giftedToday() ? 0 : 4, EtoTalk.Effect.LETTER, EtoGesture.READ)
                : r("A letter? I don't see one.", 0);
            case LESSON -> lesson(c, LESSONS);
            case FAVOUR -> !c.mixed() ? r("You don't owe me anything. You're a friend. That's already a lot, for me.", 0) : errand(c,
                "You'd do something for me? ...Then bring me books. Three I haven't read. That's all I've ever really wanted.",
                "Nothing, really. You don't owe me anything.",
                "Three books. You've brought {n}. Take your time.",
                "There are things in the dark that hunt people who can't fight back. Kill ten of them at night. {n} so far. Please be careful.",
                "One more thing. A white flower. ...It's a silly thing. Humour me.",
                "That's everything. ...Was there something you wanted to ask me? I'll listen.",
                "You don't need to do anything for me. Just be here.",
                "{partner} takes care of me. But thank you.", "You're married. Go home to them. Please.");
            case ASK_OUT -> !c.mixed() ? r("...I'm flattered. Really. But not like that. Friends? I don't have many.", 0) : !c.mayDate() ? r(c.claimed() ? EtoLines.fill(c, "I'm {partner}'s. I'm sorry.") : "You're married. I can't.", 0)
                : c.step() == EtoRules.Step.ASK ? new EtoTalk.Reply("...Me? You want... me? Even knowing what I am? ...Yes. Yes, I'd like that. "
                    + "Very much.", 0, EtoTalk.Effect.DATE, EtoGesture.BOW)
                : r("Ask me what?", 0);
            case KISS -> c.dating() ? new EtoTalk.Reply(one(seed, "...Okay. Come here.", "I was hoping you'd ask."), 0, EtoTalk.Effect.KISS)
                : r("I... don't think so. I'm sorry.", 0);
            case HUG -> c.dating() ? new EtoTalk.Reply(this.hugged(seed), 0, EtoTalk.Effect.HUG) : r("Oh. I'm... not good with that. Sorry.", 0);
            case SWEET -> c.dating() ? new EtoTalk.Reply(EtoLines.fill(c, one(seed, "...You're going to make me blush.", "Go on. I'm listening.",
                "Careful. I'll start believing you.")), 0, EtoTalk.Effect.SWEET) : r("Sweet? To me? I don't think I deserve that.", 0);
            case NIGHT -> !c.dating() ? r("...I think you have the wrong idea. I'm sorry.", -2) : new EtoTalk.Reply(c.night() && !c.nightUsed()
                ? one(seed, "...Okay. Stay with me tonight. I sleep better when you're there.", "Come on. I'll leave the lamp on, if you want.")
                : one(seed, "It's still light out. Read with me instead?", "Not tonight. Hold my hand?"), 0,
                c.night() && !c.nightUsed() ? EtoTalk.Effect.NIGHT : EtoTalk.Effect.NONE);
            case PROPOSE -> new EtoTalk.Reply("", 0, EtoTalk.Effect.PROPOSE);
            case FOLLOW -> new EtoTalk.Reply(voice.together() ? one(seed, "Wherever you're going.", "Lead the way. I'll keep up.")
                : one(seed, "Alright. I'll watch your back.", "Okay. Stay where I can see you."), 0, EtoTalk.Effect.FOLLOW);
            case STAY -> new EtoTalk.Reply(voice.together() ? one(seed, "Here? ...It's nice. I'll make it ours.", "Okay. I'll be right here. Come back.")
                : one(seed, "I'll wait here.", "Alright. Don't be long."), 0, EtoTalk.Effect.STAY);
            case DECLINE -> r("That's alright. Whenever you're ready.", 0);
            case TEASE -> new EtoTalk.Reply(one(seed, "...Was that a joke? Sorry. I'll laugh. Ha.", "You're mean. ...I like it.",
                "Keep that up and I'll read you poetry until you beg.", "I'm not touching my chin. ...I'm not."), 1, EtoTalk.Effect.NONE, EtoGesture.CHIN);
            case INSULT -> NpcBonds.clamp(c.regard() + EtoRules.INSULT) <= NpcBonds.ENEMY
                ? new EtoTalk.Reply("...I see. I'm sorry it has to be like this.", EtoRules.INSULT, EtoTalk.Effect.ANGER, EtoGesture.CRACK)
                : new EtoTalk.Reply(voice == EtoVoice.CLAIMED ? EtoLines.fill(c, "That's unkind. {partner} won't like hearing it. Please go.")
                : one(seed, "...Okay. I'll go back to my book.", "I've heard worse. From worse people. Please leave."), EtoRules.INSULT, EtoTalk.Effect.CLOSE,
                EtoGesture.CRACK);
            case BACK -> new EtoTalk.Reply(EtoLines.fill(c, voice.pick(seed, s("Anything else?"), s("Something else?"), s("What else?"), null,
                s("What else, love?", "Mm? What is it?"))), 0, EtoTalk.Effect.MAIN);
            case LEAVE -> new EtoTalk.Reply("", 0, EtoTalk.Effect.CLOSE);
            default -> r("I'm sorry, what?", 0);
        };
    }

    /** How he teaches: quietly, and with a warning first. */
    static final LessonWords LESSONS = new LessonWords() {
        @Override
        public String teach(Teaching.Lesson lesson, EtoTalk.Context c) {
            return switch (lesson.gate()) {
                case RINKAKU_CENTIPEDE -> "You want the centipede. ...It doesn't come from strength. It comes from what's left when everything else "
                    + "is gone. I can't tell you. I can only show you. Sit down. It took me ten days. For you, a minute. I'm sorry.";
                case UKAKU_FINAL_KAKUJA -> "Your wings can go further. Touka can't take you there, and she shouldn't. It's not something you learn. It's "
                    + "something you survive. Sit down. ...I'm sorry. I'll be here when it's over.";
                case RINKAKU_FULL_KAKUJA -> "The centipede is a cage. The Full Kakuja is when you stop rattling it. Eat your own until nothing human "
                    + "is left to complain. Then put the armour on and keep your mind. That's the hard part. Keep your mind. (Full Kakuja.)";
                case RINKAKU_DRAGON -> "...You want the last one. The Dragon. I'll tell you what it costs, and you'll do it anyway, because that's what "
                    + "we do. The next time you die, you won't. Something enormous will crawl out of you instead and it will eat the city. Come "
                    + "back. Whatever happens, come back. (The Dragon: your next death wakes it.)";
                default -> "Watch carefully. (" + lesson.name() + ")";
            };
        }

        @Override
        public String refuse(Teaching.Refusal refusal, Teaching.View view, EtoTalk.Context c) {
            return switch (refusal) {
                case HUMAN -> "You're human. ...Stay that way. Please. I'd give anything to.";
                case WRONG_PATH -> "I don't know your kagune. I only know what it costs to be more than you are. Come back if you ever have tails, "
                    + "or wings.";
                case WEAK -> "Not yet. It would kill you, and I'd have to watch. Get stronger. (" + view.next().name() + ": mastery " + view.need() + ")";
                case BEFORE -> "You're not there yet. Get " + (view.before() == null ? "what comes before" : view.before().displayName())
                    + " first. " + (view.before() != null && view.before().path() == com.hex.kagune.control.KagunePath.UKAKU
                    ? "Touka can teach you that." : "Your tails will tell you how.");
                case ALL_TAUGHT -> "There's nothing left I can teach you. ...I'm sorry. I wish there was less.";
            };
        }
    };

    @Override
    public String trialWords(Teaching.Lesson lesson, EtoTalk.Context c) {
        boolean ours = together(c);
        return switch (lesson.gate()) {
            case UKAKU_FINAL_KAKUJA -> ours ? "Ken holds you until you stop shaking.\n\"It's over. I'm here. ...Your wings will remember, not you.\""
                : "Ken waits until you can stand.\n\"It's over. The storm is yours now. I'm sorry it had to be this.\"";
            default -> ours ? "Ken's hand on your hair, shaking more than you are.\n\"You're still here. That's all it takes. Not strength. Staying.\""
                : "Ken is sitting beside you when you open your eyes.\n\"You're still here. Good. ...The centipede is yours now.\"";
        };
    }

    @Override
    protected String[][] sweetSays() {
        return new String[][] {{"You're handsome.", "handsome"}, {"I love you.", "love"}, {"I missed you.", "missed"}, {"Read to me.", "read"},
            {"Your hair's soft.", "hair"}, {"Lean on me.", "lean"}, {"You're not a monster.", "monster"}, {"Smile for me.", "smile"},
            {"Kiss my forehead.", "forehead"}, {"Tell me what you love about me.", "why"}};
    }

    @Override
    public EtoTalk.Reply sweetly(EtoTalk.Context c, String what) {
        int seed = c.seed();
        boolean married = c.married();
        EtoTalk.Reply reply = switch (what) {
            case "cling" -> new EtoTalk.Reply(one(seed, "Like this? ...Don't let go.", "I've got you."), 2, EtoTalk.Effect.CLING, EtoGesture.NONE);
            case "hand" -> new EtoTalk.Reply(one(seed, "My hand? ...Here. It's cold, sorry.", "Of course."), 1, EtoTalk.Effect.HAND, EtoGesture.NONE);
            case "handsome" -> r(one(seed, "Me? ...You need glasses. Thank you, though.", "I— um. Thank you. I don't know where to look."), 2, EtoGesture.CHIN);
            case "love" -> married ? r(one(seed, "I love you too. More than every book I've ever read.", "I know. I still can't believe it. I love you."),
                3, EtoTalk.Effect.NONE, EtoGesture.HUG)
                : r(one(seed, "...I love you too. I've been scared to say it.", "Say that again. I want to remember exactly how it sounded."), 3,
                    EtoTalk.Effect.NONE, EtoGesture.BOW);
            case "missed" -> r(one(seed, "I missed you too. I read the same page twenty times.", "Don't go so long again. Please."), 2, EtoTalk.Effect.CLING,
                EtoGesture.CLING);
            case "read" -> r(one(seed, "Lie down. 'The black goat's egg was cold, and the boy held it anyway...' My favourite author. Nobody knows who "
                + "she really is.", "'Nobody is born a monster.' ...I think about that line a lot."), 2, EtoGesture.READ);
            case "hair" -> r(one(seed, "It used to be black. ...You can keep doing that.", "Mm. That's nice."), 2, EtoGesture.HAIR);
            case "lean" -> r(one(seed, "On you? ...Okay. Just for a while.", "You're warm. I'm always cold now."), 2, EtoTalk.Effect.CLING, EtoGesture.CLING);
            case "monster" -> r(one(seed, "...You really think so? ...Thank you. I needed that more than you know.", "Stay with me long enough to prove it."),
                4, EtoTalk.Effect.HUG, EtoGesture.HUG);
            case "smile" -> r(one(seed, "Like this? ...Sorry, I'm out of practice.", "You make it easy."), 2, EtoGesture.HUM);
            case "forehead" -> r(one(seed, "Come here. ...There. That one means 'I'm proud of you.'", "Here. And here. And— okay, one more."), 3,
                EtoTalk.Effect.NONE, EtoGesture.PECK);
            case "why" -> r(one(seed, "The way you laugh when you're trying not to. How you always come back. That you've never once looked at me "
                + "like I'm a monster. ...I could keep going. I'd like to.", "Everything. ...Okay. Your hands. Your stubbornness. How you look at me "
                + "when you think I'm not looking. I'm always looking."), 4, EtoTalk.Effect.HUG, EtoGesture.HUG);
            default -> r("Hm?", 0);
        };
        return new EtoTalk.Reply(EtoLines.fill(c, reply.line()), reply.regard(), reply.effect(), reply.gesture());
    }

    @Override
    public String giftsAsked(int seed) {
        return one(seed, "You brought something? For me?", "Oh, what is it?");
    }

    @Override
    public String topicsAgain(int seed) {
        return one(seed, "What else?", "Something else?", "Go on, I'm listening.");
    }

    @Override
    public String busy() {
        return "Ken is busy. His tails are busier.";
    }

    @Override
    public String approach(EtoTalk.Context c) {
        int seed = c.seed();
        if (c.pregnant() && seed % 2 == 0) {
            return this.pregnant(c.family().stage(), seed);
        }
        return EtoLines.fill(c, switch (c.mood()) {
            case CLINGY, LONELY -> one(seed, "Can I stay with you for a bit? I don't want to be alone today.", "There you are. I was looking for you.");
            case AFFECTIONATE -> one(seed, "I just wanted to see you.", "Come here. I want to hold you.");
            case SULKY, JEALOUS -> one(seed, "...Who was that? Sorry. It's none of my business. ...Who was that?", "I'm fine. I'm just thinking.");
            case GIDDY -> one(seed, "I found a first edition today. Look. LOOK.", "I'm in a good mood. Don't ruin it. ...You won't.");
            case SLEEPY -> one(seed, "I didn't sleep. Stay with me while I close my eyes?", "Read to me? Your voice puts me to sleep. In a good way.");
            case BLOODTHIRSTY -> one(seed, "...Sorry. I'm hungry. Don't stand too close for a while.", "My eye won't close today.");
            default -> one(seed, "Hey. Coffee?", "I just finished a book. Want to hear about it?", "{name}. Walk with me?");
        });
    }

    @Override
    public String welcome(EtoTalk.Context c) {
        return one(c.seed(), "There you are. ...Come here. I missed you more than I should admit.", "You're home. Good. Now I can breathe.",
            "I kept your coffee warm. And your side of the bed.");
    }

    @Override
    public String kissed(int seed, boolean married) {
        return married ? one(seed, "Every time still feels like the first one.", "I love you. ...Come back here, I wasn't finished.",
            "Mm. That's my favourite thing in the world. You're my favourite thing in the world.")
            : one(seed, "...I've wanted to do that since the first time you smiled at me.", "Again. ...Please.", "You taste like coffee. I'm keeping that.");
    }

    @Override
    public String peck(int seed) {
        return one(seed, "...Sorry. I just wanted to.", "That one was for later. I couldn't wait.", "You had something on your cheek. ...My lips. Sorry.");
    }

    @Override
    public String kissWaiting(String name) {
        return name + " tips your chin up with one finger, a hand at your waist, and leans down...";
    }

    @Override
    public String kissHint(int seed) {
        return one(seed, "...Can I? Is that okay?", "I've been wanting to all day.");
    }

    @Override
    public String babyNight(EtoTalk.Context c) {
        return one(c.seed(), "It's late. ...Do you still want to? I do.", "Come to bed. I've been thinking about what you asked all day.");
    }

    @Override
    public String held(int seed) {
        return one(seed, "Don't pull away yet. Stay right here.", "Mm. Five more seconds. ...Ten. A hundred.", "I could stay like this until the sun comes up.");
    }

    @Override
    public String pulled(int seed) {
        return one(seed, "Come here. I'm not done with you.", "I've got you. I've always got you.", "Closer. ...There.");
    }

    @Override
    public String hanging(int seed) {
        return one(seed, "...Oh. Okay. I'm sorry, I misread that.", "...Right. Of course. Sorry.", "That's fine. That's... fine.");
    }

    @Override
    public String hugged(int seed) {
        return one(seed, "Come here. ...You fit perfectly. I noticed that a long time ago.", "I've got you. Rest.", "Don't let go yet. I'm not ready.");
    }

    @Override
    public String handTaken(int seed) {
        return one(seed, "Your hand. ...I'll never get tired of this.", "Walk with me. Slowly. I want this to last.", "Your hand's small in mine. I like that.");
    }

    @Override
    public String handLet(int seed) {
        return one(seed, "...Okay.", "Thanks for that.");
    }

    @Override
    public String partnerHit(int seed, boolean married) {
        return married ? one(seed, "...That hurt. Not the hit.", "Why? ...Tell me why.") : one(seed, "Ow. ...Did I do something wrong?",
            "Please don't do that.");
    }

    @Override
    public String serious(int seed) {
        return one(seed, "I don't want to do this.", "Please stop. I'm asking once.", "...I'm sorry.");
    }

    @Override
    public String maniac(int seed) {
        return one(seed, "What's a thousand minus seven?", "Nine hundred ninety-three. Nine hundred eighty-six...", "I'm a ghoul.");
    }

    @Override
    public String protect(EtoTalk.Context c) {
        return one(c.seed(), "Don't touch them.", "Get away from them. Now.", "I won't lose anyone else.");
    }

    @Override
    public String won(int seed) {
        return one(seed, "...I'm sorry.", "It's over.", "I didn't want to do that.");
    }

    @Override
    public String puppy(EtoTalk.Context c, String kind, boolean sorry) {
        String what = kind == null ? "it" : "a little " + kind;
        return sorry ? one(c.seed(), "You brought me " + what + " to apologize. ...That's very sweet. I forgive you.",
            "How could I stay upset with " + what + " looking at me?")
            : one(c.seed(), "Is that " + what + "? For me? ...I'll take care of it. I promise.", "Oh. It's so small. I'll be careful.");
    }

    @Override
    public String letter(EtoTalk.Context c, boolean sorry) {
        return sorry ? one(c.seed(), "You wrote this for me. ...Of course I forgive you.", "I'll keep it in my favourite book.")
            : one(c.seed(), "A letter? I'll read it slowly. I want it to last.", "You wrote me something. ...Thank you.");
    }

    @Override
    public String snub(int seed, boolean enemy) {
        return enemy ? one(seed, "Ken's left eye turns red. He doesn't say anything.", "Ken cracks a finger and looks away.")
            : one(seed, "Ken is quiet. He doesn't want to talk right now.", "Ken keeps reading. He's hurt.");
    }

    @Override
    public String ring(EtoTalk.Context c, Romance.Refusal refusal) {
        if (refusal == null) {
            return one(c.seed(), "...Is this real? I've lost so much. ...Yes. Yes, of course. I'll never let go.",
                "You'd marry me? Me? ...Yes. A thousand times yes. Minus nothing.");
        }
        return switch (refusal) {
            case NOT_DATING -> "A ring? We haven't even... I'm flattered. Put it away.";
            case CLAIMED -> EtoLines.fill(c, "I'm with {partner}. I'm sorry.");
            case MARRIED_ELSEWHERE -> "You're already married. I can't.";
            case TOO_SOON -> "It's too soon. Not no. Just... ask me again when we're sure.";
            case NOT_CLOSE -> "Not yet. Give me time to believe this is real.";
            case ALREADY -> "We're already married. I wear it every day.";
        };
    }

    @Override
    public String morning(EtoTalk.Context c) {
        return one(c.seed(), "Ken is awake first, reading by the window.\n\"Morning. ...I didn't count once last night.\"",
            "Ken makes two cups of coffee and burns one.\n\"Sorry. Good morning.\"");
    }

    @Override
    public String conceived(EtoTalk.Context c) {
        return one(c.seed(), "Ken can't stop looking at you in the morning.\n\"...Do you feel different? I think I do.\"",
            "Ken holds your hand a long time at breakfast.\n\"Whatever happens, I'll be there. Every day.\"");
    }

    @Override
    public String pregnant(com.hex.kagune.control.family.Pregnancy.Stage stage, int seed) {
        return switch (stage) {
            case EARLY -> one(seed, "How are you feeling? I read that this part is the worst.", "Sit. I'll make you something. Anything.");
            case ALONG -> one(seed, "Can I— can I feel? ...Oh. Oh.", "I talk to the baby when you're asleep. Is that weird?");
            case LATE -> one(seed, "Let me carry that. And that. And you.", "Not long now. I'm terrified. I'm so happy.");
            case DUE -> one(seed, "Any day now. I'm not leaving your side.", "Breathe. I'm here. I'm here.");
        };
    }

    @Override
    public String saw(Display shown, EtoVoice voice, boolean home, int seed) {
        boolean ours = voice.together();
        return switch (shown) {
            case NONE -> "";
            case EYES -> ours ? one(seed, "Your eyes... they're beautiful. Mine never were.", "Red suits you.")
                : one(seed, "Careful. People see those, people get scared.", "We match. Sort of.");
            case KAGUNE -> ours ? one(seed, "It's strong. Don't let it think for you.", "Beautiful. Dangerous. Like you.")
                : one(seed, "Put it away, please. Unless you mean to use it.", "Easy. I'm not your enemy.");
            case KAKUJA -> ours ? one(seed, "Come back to me. You're still in there. I know you are.", "Listen to my voice. Only my voice.")
                : one(seed, "I know what that feels like. Don't let it keep you.", "Stay in control. Please.");
            case DREAD -> ours ? one(seed, "...I've been there. Come back. COME BACK.", "I'll pull you out. Whatever it takes.")
                : one(seed, "That's... no. I know what that is. Run.", "That's what I became. Don't become it.");
        };
    }

    @Override
    public String initiative(EtoInitiative.Act act, int seed) {
        return switch (act) {
            case HAND -> one(seed, "Give me your hand. I want everyone to see who I'm with.", "Your hand. ...Just because.", "Walk with me. Here, take my hand.");
            case CLING -> one(seed, "Come here. Under my arm. ...There. That's your spot.", "Stay close. I like you right here.");
            case HUG -> one(seed, "Come here. I just wanted to hold you.", "I missed you. Since this morning. That's how bad it is.",
                "Don't say anything. Just let me hold you a minute.");
            case LEAN -> one(seed, "Lean on me. That's what I'm here for.", "Rest your head here. I've got you.");
            case HUM -> one(seed, "*hums the old song from the cafe, slow, just for you*", "Do you know this one? I'll teach you. I want it to be ours.");
            case READ -> one(seed, "Listen. '...and he loved her the way you love a lamp in the dark.' ...That's how I feel. Sorry. That was a lot.",
                "Sit with me. I'll read to you. You can fall asleep. I'll carry you.");
            case DANCE -> one(seed, "Dance with me. I'll lead. I've been practising. Secretly.", "No music? Then I'll hum. Come here.");
            default -> "";
        };
    }

    @Override
    public String moment(EtoInitiative.Moment moment, boolean together, int seed) {
        return switch (moment) {
            case KILL -> together ? one(seed, "...Are you okay? It gets easier. That's the worst part.", "Clean. ...Don't enjoy it too much.")
                : one(seed, "...", "I'm sorry it had to be that.");
            case HURT -> together ? one(seed, "You're hurt. Let me see. Let me SEE.", "Who did this?", "Don't you dare die. I can't do that again.")
                : one(seed, "You're bleeding. Be careful.", "Get somewhere safe.");
            case RAIN -> together ? one(seed, "Rain. Good reading weather. Stay in with me.", "I like the sound. It's the only time my head's quiet.")
                : one(seed, "Rain.", "Good reading weather.");
            case DUSK -> together ? one(seed, "Night's coming. Stay close.", "My eye always itches at sunset. Don't worry.")
                : one(seed, "Night's coming. Be careful out there.", "Dusk. Go home, if you have one.");
            case DAWN -> together ? one(seed, "Morning. You stayed. You always stay.", "The sun's up. ...We made it through another one.")
                : one(seed, "Morning.", "Another day.");
        };
    }

    static final List<EtoTopics.Topic> TOPICS = List.of(
        new EtoTopics.Topic("books", c -> true, c -> "What are you reading?",
            c -> EtoVoice.of(c).pick(c.seed(),
                s("A novel. Nothing important."),
                s("'The Black Goat's Egg.' My favourite author. It's about a monster who doesn't know he's one.",
                    "A book of poems. They're sad. I like sad."),
                s("The same book I always read. I like knowing how it ends.", "A mystery. I already know who did it. I keep reading anyway."),
                null,
                s("I can't concentrate. I keep looking at you.", "A romance. I'm learning."),
                s("Something for the two of us. I'll read it to you tonight.")),
            List.of(
                new EtoTopics.Follow("Who wrote it?", c -> r("A writer nobody's ever seen. Her books feel like... she knows exactly what we are. "
                    + "Sometimes I think she's one of us.", 2, EtoGesture.READ)),
                new EtoTopics.Follow("Read some to me?", c -> r(together(c) ? "Lie here. '...And the monster thought, if I am loved, am I still a monster?' "
                    + "...What do you think?" : "'Nobody is born a monster.' ...That's my favourite line.", 2, EtoGesture.READ)),
                new EtoTopics.Follow("Why do you like sad books?", c -> r("Because they're honest. Happy endings feel like lies. ...You might change my mind.",
                    3, EtoGesture.CHIN)))),

        new EtoTopics.Topic("past", c -> c.regard() >= NpcBonds.FRIENDLY, c -> "What happened to you?",
            c -> one(c.seed(), "I was human. A girl I liked was a ghoul. She tried to eat me. A steel beam fell on her. They put her inside me. "
                + "...It's a long story. It doesn't end well.", "I was a student. Then I wasn't."),
            List.of(
                new EtoTopics.Follow("Your hair. It was black?", c -> r("Someone kept me for ten days. When it was over, it was white. So was everything "
                    + "else.", 2, EtoGesture.CRACK)),
                new EtoTopics.Follow("Do you miss being human?", c -> r("Every day. ...Less, lately.", together(c) ? 4 : 2, EtoGesture.CHIN)),
                new EtoTopics.Follow("You can tell me.", c -> r("...Thank you. Not today. But thank you.", 3, EtoGesture.BOW)))),

        new EtoTopics.Topic("thousand", c -> c.regard() >= NpcBonds.FRIEND, c -> "Why do you crack your fingers?",
            c -> "Do I? ...I didn't notice. It started back then. Every time it got bad, I counted. A thousand minus seven. Nine hundred ninety-three. "
                + "My hands remember even when I don't.",
            List.of(
                new EtoTopics.Follow("What's a thousand minus seven?", c -> r("...Nine hundred ninety-three. ...Please don't ask me that again.", -1,
                    EtoGesture.CRACK)),
                new EtoTopics.Follow("I'm sorry.", c -> r("It's not your fault. It's not even his anymore. It's just mine now.", 3, EtoGesture.BOW)),
                new EtoTopics.Follow("Hold my hand instead.", c -> together(c), c -> r("...Okay. That's better. That's much better.", 4, EtoTalk.Effect.HAND,
                    EtoGesture.NONE)))),

        new EtoTopics.Topic("coffee", c -> true, c -> "Coffee?",
            c -> one(c.seed(), "Always. It's the only thing that still tastes right.", "The manager taught me. Grind it fresh. Pour it slow. Be patient."),
            List.of(
                new EtoTopics.Follow("Make me one?", c -> r("Of course. Sit. ...Here. Careful, it's hot.", 2, EtoGesture.SIP)),
                new EtoTopics.Follow("Touka makes it better.", c -> r("She does. Don't tell her I said that. She'll be unbearable.", 2, EtoGesture.CHIN)))),

        new EtoTopics.Topic("touka", c -> c.regard() >= NpcBonds.FRIENDLY, c -> "Do you know Touka?",
            c -> one(c.seed(), "Touka-chan? She kicked me the day we met. And the day after. She taught me how to be a ghoul when I didn't want "
                + "to be one.", "Touka. She's the strongest person I know. Don't tell her that either."),
            List.of(
                new EtoTopics.Follow("Do you love her?", c -> together(c) ? r("I love YOU. ...She's family. That's different. Please don't be jealous.", 2,
                    EtoGesture.CHIN) : r("...That's a complicated question. Next one.", 0, EtoGesture.CHIN)),
                new EtoTopics.Follow("She told me you never eat.", c -> r("She worries. ...I'll eat. Promise.", 1)))),

        new EtoTopics.Topic("kagune", c -> c.ghoul(), c -> "How did you get so strong?",
            c -> one(c.seed(), "By losing. Over and over. Every time I lost something, I ate something. It adds up.",
                "I ate a lot of people who deserved it. I'm still not sure that makes it okay."),
            List.of(
                new EtoTopics.Follow("Teach me the kakuja.", c -> r("...If you're sure. Ask me properly. And be sure.", 0, EtoGesture.CRACK)),
                new EtoTopics.Follow("Does it hurt?", c -> r("Every time. You stop noticing. That's what scares me.", 2, EtoGesture.CHIN)))),

        new EtoTopics.Topic("hide", c -> c.regard() >= NpcBonds.FRIEND, c -> "Who was your best friend?",
            c -> "Hide. He knew. The whole time, he knew what I was, and he stayed. ...He was the bravest person I ever met.",
            List.of(
                new EtoTopics.Follow("Where is he now?", c -> r("...Let's talk about something else.", 0, EtoGesture.CHIN)),
                new EtoTopics.Follow("I'll stay too.", c -> r("...Don't say that unless you mean it. ...You mean it. Okay.", together(c) ? 5 : 3, EtoGesture.BOW)))),

        new EtoTopics.Topic("baby", EtoTalk.Context::pregnant, c -> "How's our baby?",
            c -> switch (c.family().stage()) {
                case EARLY -> "I've read everything. I know nothing. How are YOU?";
                case ALONG -> "Growing. I put my ear to you last night and heard a heartbeat that wasn't yours. I cried. Don't tell anyone.";
                case LATE -> "Soon. I've built a crib. It's crooked. I'll fix it.";
                case DUE -> "Any day. I'm not going anywhere.";
            },
            List.of(
                new EtoTopics.Follow("Will they be like us?", c -> r("Maybe. If they are, we'll teach them to be gentle. That's all I want for them.", 4,
                    EtoGesture.HUG)),
                new EtoTopics.Follow("Hold me.", c -> r("Come here. Both of you.", 4, EtoTalk.Effect.CLING, EtoGesture.CLING)))),

        new EtoTopics.Topic("family", c -> c.married() && c.mixed() && !c.pregnant(), c -> "Do you think about a family?",
            c -> c.family().wantsBaby() ? "Tonight. ...If you still want to. I do."
                : "I never let myself. A family is something you can lose. ...With you, I think about it.",
            List.of(
                new EtoTopics.Follow("Let's have a baby.", c -> c.babyOpen(),
                    c -> r("...Yes. I'd like that more than anything. Tonight, then.", 6, EtoTalk.Effect.BABY, EtoGesture.HUG)),
                new EtoTopics.Follow("Someday.", c -> r("Someday. I'll wait. I'm good at waiting.", 2, EtoGesture.BOW)))));

    @Override
    protected List<EtoTopics.Topic> topics() {
        return TOPICS;
    }
}
