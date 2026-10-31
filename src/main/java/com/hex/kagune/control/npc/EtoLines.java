package com.hex.kagune.control.npc;

/**
 * 1.3.2: what Eto says outside a topic (pure, tested): her greetings, what she says when she comes looking for someone, the kiss and
 * the hug, a partner who hits her, her fight, the puppy, the letter, the ring, the night, the surgery, her lessons and a human's pact.
 * Every line is picked by who they are to her ({@link EtoVoice}), then her mood and the hour, then a seed so she rarely says the same
 * thing twice running. "{name}" is the player she talks to, "{partner}" the one she belongs to.
 */
public final class EtoLines {
    private EtoLines() {
    }

    static final String[] NONE = null;

    /** {@code text} with the names put in. */
    public static String fill(EtoTalk.Context c, String text) {
        return text.replace("{name}", c.name()).replace("{partner}", c.claimedBy() == null ? "someone" : c.claimedBy());
    }

    public static String one(int seed, String... lines) {
        return lines[Math.floorMod(seed, lines.length)];
    }

    // ---------------------------------------------------------------- hello

    public static String greeting(EtoTalk.Context c) {
        EtoVoice voice = EtoVoice.of(c);
        int seed = c.seed();
        String line = switch (voice) {
            case CLAIMED -> c.night() ? one(seed,
                "It's late. {partner} wouldn't like you hanging around me in the dark. Neither would I. What do you want?",
                "Lessons at this hour? ...Fine. Keep your distance. I bite, and I'm taken.")
                : one(seed,
                "Mm? Oh. You. If it's lessons, fine. If it's anything else, I'm {partner}'s. Don't get funny ideas.",
                "The library's open. I'm not. What do you want?",
                "If you're here to flirt, the exit is behind you. If you're here to learn, sit down.",
                "You again. {partner} told me to be nice to people. This is me being nice.");
            case WARY -> wary(c, seed);
            case FRIENDLY -> c.mood() == EtoMood.BORED ? one(seed,
                "Well, look who remembered the way. I'd started reading the dictionary out of spite.",
                "Oh! A face. I was about to start talking to the books. Again.")
                : c.night() ? one(seed,
                "You came at night. Good. I'm nicer at night. ...That was a lie, but come in.",
                "Couldn't sleep? Me neither. I never can. Sit.")
                : one(seed,
                "Oh, hello again! Don't mind the mess. The books only bite a little.",
                "Back so soon? I'll have to start charging rent.",
                "Hi, {name}. Wipe your feet. I just swept. ...I didn't sweep.");
            case CLOSE -> c.regard() >= NpcBonds.MOST ? one(seed,
                "My favourite visitor! I was just thinking about you. ...Don't let it go to your head.",
                "{name}! Perfect timing. I need someone to tell me my chapter isn't terrible. Lie to me.",
                "There you are. I saved you a seat. It's the one without the bloodstain.")
                : one(seed,
                "Back again? Careful, I'll start thinking you come for me and not the books.",
                "Hi, you. I found a book you'd hate. I put it on your chair.",
                "Oh good, it's you. Everyone else who comes in here screams.");
            case PARTNER -> partner(c, seed);
            case WIFE -> wife(c, seed);
        };
        return fill(c, line);
    }

    private static String wary(EtoTalk.Context c, int seed) {
        if (c.regard() < 0) {
            return one(seed, "Oh. It's you. Mind the shelves. And your manners, this time.",
                "You've got some nerve. Speak quickly, I'm in the middle of a chapter.",
                "Hm. You. I haven't decided what to do about you yet.");
        }
        if (c.night()) {
            return c.ghoul() ? one(seed, "Another one awake at this hour. Hunting, or hiding? Either way, not in here.",
                "Your eyes give you away, you know. Relax. I'm off duty.")
                : one(seed, "It's late for browsing, little human. Are you lost, or are you dinner?",
                "A human, in my library, in the dark. Bold. Stupid, but bold.");
        }
        return c.ghoul() ? one(seed, "Hm? You smell like one of us. Relax. It's a library, not a hunting ground. Mostly.",
            "One of us. Don't eat anyone between the shelves, it stains.")
            : one(seed, "A human, in my library? Bold. I like bold. Don't touch the red shelf.",
            "Oh. A visitor. We don't get many of those. We get a lot of former visitors.",
            "Shoes off the poetry. ...I'm kidding. Mostly. What do you want?");
    }

    private static String partner(EtoTalk.Context c, int seed) {
        return switch (c.mood()) {
            case GIDDY -> one(seed, "Stop looking at me like that. ...No, keep doing it.",
                "I can still feel it. The kiss. Don't make it a thing. It's a thing.");
            case LONELY -> one(seed, "Oh. So you are alive. I was about to start writing you a very dramatic obituary.",
                "You were gone " + c.daysAway() + " days. I counted. I don't count things, {name}. I counted.",
                "...Hi. No, I'm fine. I re-read every book in here twice and talked to a moth. I'm fine.");
            case CLINGY -> one(seed, "Don't move. I'm comfortable. You're my chair now.",
                "Come here. Closer. I have a rule about you being more than an arm away.");
            case AFFECTIONATE -> one(seed, "It's late, and you're here, and I'm done reading. ...Do the maths.",
                "Mm. You took your time. The candles are almost out. Good.");
            case SLEEPY -> one(seed, "Mm... hi. Is it morning? Make it not be morning.",
                "You're up early. Or I'm up late. Come be warm at me.");
            case JEALOUS -> one(seed, "Oh, hello. Was she nice? ...Don't. I can smell her on you.",
                "Hi. Who else did you say hi to today? Just curious. Very calmly curious.");
            case BLOODTHIRSTY -> one(seed, "You smell good tonight. That's a compliment and a warning.",
                "My eye won't close tonight. Stay where I can see you. For your sake.");
            case SULKY -> one(seed, "Hmph. You're here. Great. Wonderful. I'm reading.",
                "I'm still a little bit mad at you. Be extra nice and maybe I'll forget.");
            default -> c.night() ? one(seed, "There you are. The night's better with you in it. Don't repeat that.",
                "Hi, you. The stars came out. I didn't care until you did.")
                : one(seed, "There you are. I was starting to think you'd found another library.",
                "Hi, you. Come here. No, closer. ...Better.",
                "{name}! You're late. I've been rereading the same sentence for an hour.",
                "Hi. I missed you. That's all. Don't make a face.");
        };
    }

    private static String wife(EtoTalk.Context c, int seed) {
        return switch (c.mood()) {
            case GIDDY -> one(seed, "I keep looking at my ring. It's embarrassing. Say something so I stop.",
                "Hi. Married people are supposed to be boring. We're doing it wrong. Good.");
            case LONELY -> one(seed, "The bed was cold. The books were boring. I was a little bit miserable. You're never allowed to leave that long again.",
                c.daysAway() + " days, {name}. A wife notices. A wife sulks. A wife forgives you if you hug her right now.");
            case CLINGY -> one(seed, "Mine. Sorry, I was just reminding everyone.",
                "Hold still. I'm memorising you. For the next time you leave.");
            case AFFECTIONATE -> one(seed, "Lock the door. ...Libraries have doors, I checked.",
                "There's a bed upstairs, a candle, and a very patient wife. One of those is running out of patience.");
            case SLEEPY -> one(seed, "Five more minutes... with you, not the book.",
                "Morning, love. You're warm. Stay warm. Stay.");
            case JEALOUS -> one(seed, "I married you. You remember that, right? With a ring and everything?",
                "Hello, husband-or-whatever-I-call-you. Who were you smiling at today?");
            case BLOODTHIRSTY -> one(seed, "I'm starving tonight. Lucky for you I married you instead of eating you.",
                "My eye's open. Don't be scared. Be a little scared. It's cute.");
            case SULKY -> one(seed, "Oh, it's my spouse. The one who's in trouble.",
                "I'm not talking to you. ...This doesn't count.");
            default -> c.night() ? one(seed, "Welcome home, {name}. ...I'm still not used to saying that. I like it.",
                "There's my favourite person. Don't tell the books.")
                : one(seed, "Welcome home, love. I made the bed. Badly.",
                "There you are. My whole entire spouse. I've been waiting all day to be annoying at you.",
                "Hi. I'm married to you. Every day I remember and every day it's a good day.");
        };
    }

    /** What the screen says she is ({@code "the Library"}, "your date", "your wife", "taken"). */
    public static String title(EtoTalk.Context c) {
        return switch (EtoVoice.of(c)) {
            case WIFE -> "Your wife";
            case PARTNER -> "Your date";
            case CLAIMED -> "Taken";
            default -> "The Library";
        };
    }

    // ---------------------------------------------------------------- coming to them

    /** What she says when she comes looking for her partner (or a close friend) by herself. */
    public static String approach(EtoTalk.Context c) {
        EtoVoice voice = EtoVoice.of(c);
        int seed = c.seed();
        if (!voice.together()) {
            return fill(c, one(seed, "Oh! {name}. Come here, I found a book you'd hate.",
                "You. Yes, you. You looked bored. Come be bored near me.",
                "I was going to come find you. Then you were here. Convenient."));
        }
        String line = switch (c.mood()) {
            case LONELY -> one(seed, "You're back. You're back, you're back— ahem. Hello.",
                "Where were you? No. Don't answer. Just stand there and be here.");
            case AFFECTIONATE -> one(seed, "It's late, {name}. Come to bed.",
                "The candle's lit. The book's closed. Guess what that means.");
            case JEALOUS -> one(seed, "Who was that? ...Never mind. Don't tell me. Tell me.",
                "I'm not jealous. I'm a ghoul. Ghouls don't get jealous, they get even.");
            case BLOODTHIRSTY -> one(seed, "I'm hungry. Not for you. ...Probably.",
                "Walk with me tonight. If we see anything ugly, it's mine.");
            case SLEEPY -> one(seed, "Carry me. I'm too tired to be scary.",
                "Mm. Let me lean on you. Just... lean.");
            case GIDDY -> one(seed, "I keep smiling. It's your fault. Fix it. No, don't.",
                "Guess who's in a good mood. Wrong. It's me. Because of you.");
            case CLINGY -> one(seed, "Your arm. Give it. ...Thank you. I'm keeping it.",
                "Don't go anywhere. I've decided you live here now.");
            default -> c.married() ? one(seed, "Hey, spouse. Pay attention to me. ...That's it. That's the whole request.",
                "I wrote about you today. It was disgustingly sweet. I'm burning it.",
                "Come here a second. ...No reason. I just like you here.")
                : one(seed, "Hey. You didn't say hi. Say hi.",
                "Walk me somewhere. Anywhere. I've read everything twice.",
                "Pay attention to me. ...That's it. That's the whole request.",
                "Your hand. Give it. ...Thank you.");
        };
        return fill(c, line);
    }

    /** Her partner is back after a while away. */
    public static String welcome(EtoTalk.Context c) {
        return fill(c, c.married() ? one(c.seed(), "Welcome home! ...I didn't run. I walked very fast.",
            "There you are! The library was too quiet. I was about to burn something.",
            "You're home. Good. Hold me before you do anything else.")
            : one(c.seed(), "There you are! I was about to come find you. With teeth.",
            "You came back. Of course you came back. I knew. I wasn't worried. Hug me.",
            "Oh! It's you! I mean. Oh. It's you. Hello."));
    }

    // ---------------------------------------------------------------- kisses and hugs

    public static String kissed(int seed, boolean married) {
        return one(seed, "Mm. That'll do. For now.", "That's it? ...Cute.", "Quick. You'll pay for that with a long one later.",
            married ? "Hi, spouse. Hello, kiss." : "Your lips are cold. Fix that.");
    }

    public static String held(int seed) {
        return one(seed, "...You can't just do that. Do it again later.", "My face is hot. Don't look at my face.",
            "Okay. Okay. I'm fine. I'm completely fine.");
    }

    public static String pulled(int seed) {
        return one(seed, "...Where did you learn that? Don't answer. Come back here.", "My legs don't work. That's your fault.",
            "I'm not letting go next time. You've been warned.", "...Again. That was an order.");
    }

    public static String hanging(int seed) {
        return one(seed, "Unbelievable. I'm not asking again. ...Today.", "Oh, I see. Fine. FINE.",
            "I leaned in. I closed my eyes. And you just stood there. Wow.");
    }

    public static String hugged(int seed) {
        return one(seed, "Tighter. I won't break, I promise.", "Don't let go yet. Ten more seconds. Twenty.",
            "You're warm. That's the only reason I'm doing this. Shut up.");
    }

    /** 1.3.3: her hand in theirs. */
    public static String handTaken(int seed) {
        return one(seed, "Here. ...Don't make it weird. It's already weird. Walk.", "Your hand's warm. I'm keeping it.",
            "Lead on. If you let go, I'll know it was on purpose.", "Hold it properly. Like you mean it. ...There.");
    }

    /** 1.3.3: they let go. */
    public static String handLet(int seed) {
        return one(seed, "Fine. ...My hand's cold now. Thanks.", "Okay. But I'm taking it back later.", "Hmph. That was a good walk.",
            "You let go first. I'm writing that down.");
    }

    public static String clung(int seed) {
        return one(seed, "Come here. ...Don't move. This is my spot.", "I'm not clingy. I'm attached. There's a difference.",
            "Your arm's mine now. You can have it back later. Maybe.");
    }

    /** Her partner hit her: she never hits back. */
    public static String partnerHit(int seed, boolean married) {
        return married ? one(seed, "You hit your wife? ...Sleep on the floor tonight.", "...Ow. I'm not hitting back. I'm just going to remember it forever.")
            : one(seed, "...Did you just hit me? Wow. Okay. I'm not hitting back. I'm going to remember it forever, though.",
            "Ow! What was that for?! ...I'm not talking to you.");
    }

    // ---------------------------------------------------------------- her fight

    public static String serious(int seed) {
        return one(seed, "Oh. You're actually trying. Fine.", "Okay. No more playing.", "You made my eye open. That was a mistake.",
            "Let me show you what I keep in the back.");
    }

    public static String maniac(int seed) {
        return one(seed, "Ahahaha! Come on, come on, COME ON!", "You made me bleed on the books. You're going to pay for the books.",
            "This is my favourite chapter! Everybody dies in it!", "Stop moving. I want to see your face when it happens.");
    }

    public static String protect(EtoTalk.Context c) {
        return fill(c, one(c.seed(), "Hands off. That one's mine.", "You touched {name}. You picked the wrong person to touch.",
            "Nobody hurts {name} but me. And I don't.", "Get behind me, {name}. This won't take long."));
    }

    public static String won(int seed) {
        return one(seed, "...Look at the mess. Someone's cleaning that, and it isn't me.", "Hm. Shorter than I hoped.", "Next chapter.");
    }

    // ---------------------------------------------------------------- gifts and apologies

    /** A baby animal on a lead: {@code kind} its entity's path ({@code wolf}, {@code cat}...), {@code sorry} brought to make up. */
    public static String puppy(EtoTalk.Context c, String kind, boolean sorry) {
        int seed = c.seed();
        String line = switch (kind) {
            case "wolf" -> sorry ? one(seed, "...Is that a puppy? You brought me a— oh, he has a little face. Fine. FINE. You're forgiven.",
                "You think a puppy fixes everything? ...Look at his ears. It fixes everything.")
                : one(seed, "A puppy! For me? He's terrible. I love him. He's sleeping in the poetry section.",
                "Oh, you brought me a little monster. We'll get along.");
            case "cat" -> sorry ? one(seed, "A kitten. You're fighting dirty. ...It's working.", "Fine. You're forgiven. The kitten is forgiven more.")
                : one(seed, "A kitten! She's going to knock everything off every shelf. Perfect.", "She looked at me like I'm food. We're best friends now.");
            case "fox" -> one(seed, "Did you steal a fox for me? You absolute criminal. I love her.", "A fox. A real fox. You're forgiven for everything you'll ever do.");
            case "panda" -> one(seed, "Is that— where did you get a baby PANDA? Don't tell me. I don't want to be an accessory.");
            case "axolotl" -> one(seed, "A little pink... thing. It's smiling at me. Nobody smiles at me. I'm keeping it.");
            default -> one(seed, "...It's a baby " + kind.replace('_', ' ') + ". It's the thought that counts. It's a very strange thought.",
                "You brought me a baby " + kind.replace('_', ' ') + ". I... thank you? It's chewing my dress.");
        };
        return fill(c, line);
    }

    public static String letter(EtoTalk.Context c, boolean sorry) {
        return fill(c, sorry ? one(c.seed(), "You wrote me a letter. ...Your handwriting is awful. I forgive you. Don't tell anyone I cried.",
            "An apology, in writing? I collect those. ...Fine. You're forgiven. This time.")
            : one(c.seed(), "A letter? For me? I'll read it later. Alone. Don't watch me read it.",
            "You wrote something. For me. ...I'm putting it in the good drawer."));
    }

    public static String meat(EtoTalk.Context c) {
        return fill(c, EtoVoice.of(c).together() ? one(c.seed(), "You brought me dinner. ...That's the most romantic thing anyone's ever done.",
            "Still warm. You know me so well it's scary.")
            : one(c.seed(), "Ghoul meat. For me? ...Thank you. People usually bring me flowers. This is better.",
            "Mm. You're learning what I like."));
    }

    /** She won't talk: cross, or worse. */
    public static String snub(int seed, boolean enemy) {
        return enemy ? one(seed, "Eto bares her teeth at you.", "Eto doesn't want to hear it.")
            : one(seed, "Eto turns her nose up at you. Hmph.", "Eto pretends you don't exist.", "Eto is very busy reading the same page.");
    }

    // ---------------------------------------------------------------- the ring

    public static String ring(EtoTalk.Context c, Romance.Refusal refusal) {
        int seed = c.seed();
        if (refusal == null) {
            return fill(c, one(seed, "...Yes. Yes, obviously yes, you idiot, put it on me before I change my mind— I won't change my mind.",
                "Is that— are you— ...Yes. Yes. Ask me again so I can say yes again.",
                "I swore I'd never let anyone keep me. ...Keep me."));
        }
        return fill(c, switch (refusal) {
            case TOO_SOON -> "A ring? Already? ...Ask me again when we've been at this longer than a day. Keep it safe. Keep it very safe.";
            case NOT_CLOSE -> "You're sweet. But I'm not there yet. Make me be there.";
            case MARRIED_ELSEWHERE -> "You're already wearing someone else's ring. Bold. Rude. Get out.";
            case NOT_DATING -> "A ring? We haven't even been on a date. Slow down, romantic.";
            case ALREADY -> "We're already married, silly. You want to do it again? ...Actually, yes. Later.";
            default -> "No.";
        });
    }

    // ---------------------------------------------------------------- the night

    public static String nightAsk(EtoTalk.Context c) {
        if (!c.night()) {
            return one(c.seed(), "It's the middle of the day, you menace. Tonight.", "In daylight? With the books watching? Tonight.");
        }
        if (c.nightUsed()) {
            return one(c.seed(), "Again? ...Tomorrow night. I need to recover. And read.", "Greedy. Tomorrow.");
        }
        return one(c.seed(), "Finally. I thought you'd never ask.", "Took you long enough. Come on, then.", "Shh. Don't talk. Come here.");
    }

    /** What she says in the morning (the cutscene's last words). */
    public static String morning(EtoTalk.Context c) {
        return fill(c, one(c.seed(), "Morning, {name}. ...Don't look at me like that. Make me tea.",
            "Stay. Just tonight. ...Every night.", "You snore. I'm keeping you anyway.",
            c.married() ? "Good morning, spouse. I could get used to this. I have." : "...So. That happened. Good. Very good."));
    }

    /** 1.3.10: the morning after the night she conceived. */
    public static String conceived(EtoTalk.Context c) {
        return fill(c, one(c.seed(), "...Something changed last night. Don't ask me how I know. I know.",
            "Morning, {name}. ...I think we're going to need a smaller bookshelf. For small hands.",
            "Stay in bed a little longer. ...I want to remember this morning exactly."));
    }

    /** 1.3.10: what she says, unasked, while carrying their child, by how far along she is. */
    public static String pregnant(com.hex.kagune.control.family.Pregnancy.Stage stage, int seed) {
        return switch (stage) {
            case EARLY -> one(seed, "I can't stand the smell of ink this morning. Ink. Me.", "I'm starving. Feed me. Don't ask what I want, guess.",
                "...I keep putting my hand here. Habit. A new one.");
            case ALONG -> one(seed, "It kicked. Right in the middle of a sentence. Rude. Like you.", "Come here. Put your hand there. ...Feel it?",
                "I've started reading to it. It prefers poetry. We're doomed.");
            case LATE -> one(seed, "Help me up. Don't laugh.", "I can't see my feet. I've decided they're still there.",
                "Sit with me. The baby likes it when you're near.");
            case DUE -> one(seed, "It's soon. Don't go far.", "Stay. Today, stay.", "...Hold my hand. Just in case.");
        };
    }

    // ---------------------------------------------------------------- one eye

    public static String surgeryWords(EtoTalk.Context c) {
        if (EtoVoice.of(c).together()) {
            return fill(c, one(c.seed(), "hey are you awake honey?", "Hey. Hey, open your eyes. ...There you are. Look at us. Matching.",
                "Shh, it's done. You did so well. I'm right here."));
        }
        return fill(c, one(c.seed(), "Why are you crying? Stop being a baby. It's done.",
            "Get off my table. You're bleeding on the floor I didn't sweep.", "Stop whimpering. You wanted this. Congratulations."));
    }

    public static String surgeryOffer(EtoTalk.Context c) {
        if (EtoVoice.of(c).together()) {
            return c.ghoul() ? "Like me? One red eye, one of your own... It hurts. A lot. Lie down and close your eyes. Sure?"
                : "Like me? You'd stop being human, you know. For good. It hurts, and there's no going back. Still sure?";
        }
        return c.ghoul() ? "Lie down on the table. Don't move, don't scream, don't bleed on the books. Sure?"
            : "There's no going back to being human after this. I won't hold your hand. Lie down, or leave.";
    }

    // ---------------------------------------------------------------- following

    public static String follow(EtoTalk.Context c) {
        return fill(c, switch (EtoVoice.of(c)) {
            case WIFE -> one(c.seed(), "Lead the way. I'll be right behind you. Close behind. Very close.", "Anywhere. Hand.");
            case PARTNER -> one(c.seed(), "A date? Outside? ...Fine. Hold my hand or I'll get lost on purpose.", "Finally, some fresh air. Lead the way.");
            default -> one(c.seed(), "Fine. But if you get me lost, I'm eating you.", "A walk? Sure. Don't make it boring.");
        });
    }

    public static String stay(EtoTalk.Context c) {
        return fill(c, EtoVoice.of(c).together() ? one(c.seed(), "Here? With you? ...Yes. This is home.", "Here's good. Here has you in it.")
            : one(c.seed(), "Here? ...Okay. This is home now.", "Fine. But this place needs more books."));
    }
}
