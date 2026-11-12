package com.hex.kagune.control.npc;

import java.util.List;

/**
 * 4.1.54: Banjou's words (pure, tested). The owner: "Personality - wants to be tough but truly isnt, is aware of how weak he truly is.";
 * "Dialog - Daily quest giver, Chat, Romanceable (Only females)". The Gas Masks' leader: a big bearded man who opens every sentence like a
 * gang boss and finishes it like someone apologising. He knows he is the weakest ghoul in his own gang, and says so before anyone else
 * can; his men follow him because he is the one who stays, patches them up and keeps the filters stocked. With the woman he loves he stops
 * trying to sound tough at all, and is better for it. Only women (and him a man: {@code mixed}).
 */
final class BanjouScript extends CompanionScript {
    @Override
    protected String key() {
        return "banjou";
    }

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
        return "I brought you charcoal. (the charcoal in your hand)";
    }

    @Override
    protected String flowerLabel() {
        return "I picked you a blue orchid. (the one in your hand)";
    }

    @Override
    protected String lessonLabel(EtoTalk.Context c) {
        return "Teach me something.";
    }

    @Override
    public String title(EtoTalk.Context c) {
        return switch (EtoVoice.of(c)) {
            case WIFE -> "Your husband (the Gas Masks' boss)";
            case PARTNER -> "Yours (he told every one of his men)";
            case CLAIMED -> "Spoken for";
            case CLOSE -> "Banjou";
            default -> "The Gas Masks' leader";
        };
    }

    @Override
    public String greeting(EtoTalk.Context c) {
        int seed = c.seed();
        String line = switch (EtoVoice.of(c)) {
            case CLAIMED -> one(seed, "Oh. Hey. You're {partner}'s, right? Good for you. Really. ...Need something?",
                "You belong to {partner}. I'm not going to make trouble. I'm not much good at trouble.", "Hey. {partner}'s, aren't you. ...Tea?");
            case WARY -> c.night() ? one(seed, "*his voice comes out muffled through the mask* Halt! Who goes there? ...That came out wrong. Who are you?",
                "Hold it right there! This is Gas Mask ground. ...Please don't make me prove it.",
                "*he steps in front of the door, filling it* You're on our turf. At night. That's... that's very bold of you.")
                : one(seed, "Stop right there! ...Okay, you stopped. Good. I wasn't sure that would work. Who are you?",
                    "This is the Gas Masks' place. I'm the boss. Yeah. Me. ...Don't laugh.", "Hey! You! State your business. ...Please.",
                    "*crosses his arms and tries very hard to look dangerous* What do you want?");
            case FRIENDLY -> one(seed, "Oh, it's you! Hey. I mean- hey. *nods, very cool about it*", "{name}! Come in, come in. Mind the pots. I water "
                + "them. Don't tell anyone.", "You came back. Huh. Most people come once and decide I'm not worth the walk.",
                "Hey there. The boys said you were coming. I told them to stand up straight. They didn't.");
            case CLOSE -> one(seed, "{name}! I was hoping it'd be you. Sit. I'll put the kettle on.", "There you are. It's quieter when you're "
                + "not here. I don't like it as much as I thought I would.", "Hey. You look tired. Sit down a minute. I'll keep watch.",
                "Good, it's you. I've been practising a speech about how tough I am. You want to hear it? It's not very good.");
            case PARTNER -> c.mood() == EtoMood.SULKY ? one(seed, "Oh. Hi. ...I'm not sulking. I'm thinking. Hard. About things.",
                "Hey. I'm a little upset. I'll be fine. I always am.")
                : c.daysAway() >= 3 ? one(seed, "Three days! I had the boys out looking for you! I went out looking! ...I got lost. Come here.",
                    "You're back. You're okay. *lets out a breath he's been holding for days* ...Good.")
                : one(seed, "Hey, you. *goes red to the ears* ...Still not used to saying that.", "There she is. *grins like an idiot* Sorry. Just happy.",
                    "Hi. I made tea. I made too much tea. I always make too much when you're coming.", "You came! Look, everybody, she came. ...They're "
                        + "wearing masks, you can't tell, but they're happy too.");
            case WIFE -> c.pregnant() ? one(seed, "Sit! Sit down. Feet up. I'll do everything. I'll do it badly, but I'll do it.",
                "How are you feeling? Do you need anything? Water? A pillow? A smaller husband? I can't help that one.")
                : one(seed, "Welcome home. *takes your coat, hangs it up, straightens it twice* ...Home. Ours. Still gets me.", "You're home. "
                    + "Good. Now the cottage feels like a house.", "There's my wife. *stands up a little taller* The boys are jealous. I don't blame them.",
                    "Hey, love. Dinner's almost ready. It's almost edible.");
        };
        return EtoLines.fill(c, line);
    }

    @Override
    public EtoTalk.Reply answer(EtoTalk.Context c, NpcTalk.Choice choice) {
        EtoVoice voice = EtoVoice.of(c);
        int seed = c.seed();
        return switch (choice) {
            case WHO -> new EtoTalk.Reply(voice == EtoVoice.CLAIMED ? EtoLines.fill(c, "Banjou. I run the Gas Masks. You're {partner}'s, so... "
                + "nice to meet you, I suppose.") : "Banjou. I lead the Gas Masks. That's them out there, in the masks. They look after each other, they "
                + "start nothing, and if somebody hurts one of them, all of us come. ...Not because of me. I'm the weakest one here. Everyone knows it. "
                + "But I'm the one who stays, so I'm the boss.", c.has(NpcBonds.ASKED) ? 0 : EtoRules.WHO, EtoTalk.Effect.NONE);
            case CHAT -> new EtoTalk.Reply(EtoLines.fill(c, voice.pick(seed,
                s("Quick, before {partner} thinks I'm stealing you. I couldn't steal a biscuit."),
                s("Talk? Sure. Just... don't tell the boys I chat. I'm supposed to be scary.", "Okay. What do you want to talk about? Not fighting. "
                    + "I'm bad at fighting."),
                s("Sit down. Tell me something. I'll make tea. I make tea when I'm nervous. I make a lot of tea.", "Talk to me. It's nice having "
                    + "someone to talk to who isn't wearing a filter."),
                s("Come talk. I saved the good chair for you. It's the one that doesn't wobble.", "Tell me everything. I'm a good listener. It's "
                    + "about the only thing I'm good at."),
                s("Talk to me. ...Sorry, I'm staring. You're just nice to look at.", "Sit with me. Closer. I don't bite. Well. Not you."),
                s("Tell me about your day, love. All of it. Even the boring parts.", "Come here and talk. I've been saving things up to tell you all day."))),
                c.chattedToday() ? 0 : EtoRules.CHAT, EtoTalk.Effect.TALK);
            case BOOK -> errandItem(c, "Charcoal? You've got nothing in your hands. ...Was that a test? Did I pass?",
                "One lot a day is plenty. We've got enough filters for a week now. Thanks to you.", "Charcoal! For the filters! You thought of the "
                    + "filters. Nobody thinks of the filters. *hugs the lump to his chest* ...Thank you, love.", "Charcoal. For the masks. That's... "
                    + "that's really thoughtful. I'll make sure every one of the boys knows it came from you.", EtoGesture.SHY);
            case FLOWER -> flowerGiven(c, "A flower? Where? ...Oh. I thought- never mind.",
                "One flower a day. The cottage is already full of them. I'm running out of pots.", "A blue orchid. *carefully, like it might break* "
                    + "I'll put it by the window. Where I can see it. ...You remembered I liked those.", "Oh. A blue orchid. For me? ...Nobody "
                    + "gives flowers to a man like me. *goes red* Thank you.", EtoGesture.SHY);
            case GIFT -> c.meat() ? r(c.ghoul() ? "Dinner? For me? ...I'll share it with the boys. They eat first. They always eat first. Thank you."
                : "Where did a human get THAT? ...I'm not going to ask. Thank you. Really.", 3)
                : r("Your hands are empty. That's okay. I didn't expect anything.", 0);
            case PUPPY -> c.puppy() == null ? r("A puppy? Where? ...Oh. There isn't one. That's fine. I wasn't excited.", 0)
                : new EtoTalk.Reply(this.puppy(c, c.puppy(), false), 8, EtoTalk.Effect.PUPPY, EtoGesture.PET);
            case LETTER -> c.letter() ? new EtoTalk.Reply(this.letter(c, false), c.giftedToday() ? 0 : 4, EtoTalk.Effect.LETTER, EtoGesture.READ)
                : r("A letter? I don't see one. ...I'd have liked a letter.", 0);
            case LESSON -> lesson(c, LESSONS);
            case FAVOUR -> !c.mixed() ? r("You don't owe me anything. You're a friend. I don't have so many I can afford to lose one over a favour.", 0)
                : errand(c, this.key(),
                "A favour? From me? I... don't know you well enough yet to ask. Come back. Please.",
                "That's everything I asked for. All of it. ...Was there something you wanted to ask me? You've got that look.",
                "I don't need anything else. I've got you. That's more than a man like me gets to have.",
                "{partner} looks after you. I don't want to get in the way of that.", "You're married. Go home to them. They're lucky.");
            case ASK_OUT -> !c.mixed() ? r("...I'm flattered. Honestly. But not like that. Friends, though? I could use one.", 0)
                : !c.mayDate() ? r(c.claimed() ? EtoLines.fill(c, "I'm {partner}'s. ...I still can't believe that.")
                    : "You've got someone. I'm not going to be the reason that goes wrong.", 0)
                : c.step() != EtoRules.Step.ASK ? r("Ask me what? ...You lost me. That happens a lot.", 0)
                : new EtoTalk.Reply("...Me? You want to go out with me? *He looks behind himself to check you mean him.* ...Yes. Yes! Sorry. Yes. "
                    + "I'm going to tell the boys and none of them are going to believe me.", 0, EtoTalk.Effect.DATE, EtoGesture.SHY);
            case KISS -> c.dating() ? new EtoTalk.Reply(one(seed, "A kiss? Here? In front of the boys? ...Okay. Yes. Let them look.",
                "*he bends down, slowly, like he's afraid you'll change your mind* ...Okay."), 0, EtoTalk.Effect.KISS)
                : r("A kiss? *goes red to the ears* We're not- I mean- not yet. Not yet.", 0);
            case HUG -> c.dating() ? new EtoTalk.Reply(this.hugged(seed), 0, EtoTalk.Effect.HUG) : r("A hug? Oh. Uh. Maybe later. When nobody's watching.", 0);
            case SWEET -> c.dating() ? new EtoTalk.Reply(EtoLines.fill(c, one(seed, "Say something nice? About me? ...There's not a lot to work with, "
                + "but go on.", "I'm listening. I'm going to go red. I'm already going red.", "Go on. I'll try not to hide behind my mask.")), 0,
                EtoTalk.Effect.SWEET) : r("Compliments? For me? I wouldn't know what to do with them.", 0);
            case NIGHT -> !c.dating() || !c.mixed() ? r("*goes completely red* I- no. No. That's not- we're not- no.", -1)
                : new EtoTalk.Reply(c.night() && !c.nightUsed()
                    ? one(seed, "Stay the night? In the cottage? ...Yes. I'll tell the boys to guard the far side of the garden.",
                        "...Yes. Stay. I sleep badly. I hear every sound. Not with you here.")
                    : one(seed, "It's the middle of the day! ...Tonight. Ask me tonight.", "Not tonight. I'm on watch. ...Stay close anyway."), 0,
                    c.night() && !c.nightUsed() ? EtoTalk.Effect.NIGHT : EtoTalk.Effect.NONE);
            case PROPOSE -> new EtoTalk.Reply("", 0, EtoTalk.Effect.PROPOSE);
            case HOME -> c.stage() == Romance.Stage.MARRIED ? new EtoTalk.Reply(one(seed, "Ours? A house of our own? ...I'll bring the flowers. "
                + "All of them.", "Home. With you. ...I never thought I'd get one of those."), 2, EtoTalk.Effect.HOME)
                : new EtoTalk.Reply("Ours? We're not married. ...Yet. I mean- not yet.", 0, EtoTalk.Effect.NONE);
            case FOLLOW -> new EtoTalk.Reply(voice.together() ? one(seed, "Lead the way, love. I'll walk on the side the trouble comes from.",
                "With you? Anywhere. I'll be right behind you. ...Or in front, if something's coming.")
                : one(seed, "Okay. I'll come. Don't expect much if there's a fight.", "Sure. Lead on. I'll try not to slow you down."), 0, EtoTalk.Effect.FOLLOW);
            case STAY -> new EtoTalk.Reply(voice.together() ? one(seed, "I'll wait right here. Come back, okay? Come back.",
                "Here? Okay. I'll keep watch. I'm good at keeping watch.") : one(seed, "Staying. Got it. I'll stand here and look tough.",
                "I'll wait. I'm very good at waiting."), 0, EtoTalk.Effect.STAY);
            case DECLINE -> r("Oh. Okay. That's fine.", 0);
            case TEASE -> new EtoTalk.Reply(one(seed, "Hey! That's- okay, that's fair. That's very fair.", "You're mean. ...You're right, but you're mean.",
                "*laughs, red to the ears* Okay, okay. You got me.", "I'm going to pretend I didn't hear that. With great dignity."),
                1, EtoTalk.Effect.NONE, EtoGesture.LAUGH);
            case INSULT -> NpcBonds.clamp(c.regard() + EtoRules.INSULT) <= NpcBonds.ENEMY
                ? new EtoTalk.Reply("*For a moment he just looks at you.* ...I know what I am. I don't need you to tell me. Get out. The boys won't "
                    + "be as polite as I am.", EtoRules.INSULT, EtoTalk.Effect.ANGER, EtoGesture.ANGRY)
                : new EtoTalk.Reply(voice == EtoVoice.CLAIMED ? EtoLines.fill(c, "...I'm sure {partner} would love to hear you say that. Go.")
                : one(seed, "...Yeah. I've heard that one before. Go on, then.", "That's not true. ...It's a little true. Go away."), EtoRules.INSULT,
                EtoTalk.Effect.CLOSE, EtoGesture.SCOLD);
            case BACK -> new EtoTalk.Reply(EtoLines.fill(c, voice.pick(seed, s("Anything else?"), s("What else? I'm listening."),
                s("Hm? Go on."), null, s("What is it, love?", "Go on. I'm right here."))), 0, EtoTalk.Effect.MAIN);
            case LEAVE -> new EtoTalk.Reply("", 0, EtoTalk.Effect.CLOSE);
            default -> r("Hm? Sorry. Say that again?", 0);
        };
    }

    /** He teaches nothing: what he knows is how to get back up, and he doesn't think that's worth teaching. */
    static final LessonWords LESSONS = new LessonWords() {
        @Override
        public String teach(Teaching.Lesson lesson, EtoTalk.Context c) {
            return "Okay. Watch. ...I'll try to do it right this time. (" + lesson.name() + ")";
        }

        @Override
        public String refuse(Teaching.Refusal refusal, Teaching.View view, EtoTalk.Context c) {
            return one(c.seed(), "Teach you? Me? ...You'd learn more watching any one of the boys. I'm not being modest. I've seen me fight.",
                "The only thing I'm good at is getting knocked down and getting back up. I don't think you can teach that. You just have to "
                    + "keep doing it.", "I'd teach you if I knew anything worth knowing. Ask someone stronger. Everyone's stronger.");
        }
    };

    @Override
    protected String[][] sweetSays() {
        return new String[][] {{"You're strong.", "strong"}, {"I love you.", "love"}, {"I missed you.", "missed"}, {"You're handsome.", "pretty"},
            {"I like your beard.", "beard"}, {"Lean on me.", "lean"}, {"You don't have to be tough with me.", "soft"}, {"Make me some tea.", "tea"},
            {"I'm proud of you.", "proud"}, {"Why me?", "why"}};
    }

    @Override
    public EtoTalk.Reply sweetly(EtoTalk.Context c, String what) {
        int seed = c.seed();
        boolean married = c.married();
        EtoTalk.Reply reply = switch (what) {
            case "cling" -> new EtoTalk.Reply(one(seed, "Like this? *very carefully offers you his arm* ...Okay. Okay. This is good.",
                "*lets you hang off his arm and doesn't move a muscle* I'm a good post. I'm sturdy."), 2, EtoTalk.Effect.CLING, EtoGesture.NONE);
            case "hand" -> new EtoTalk.Reply(one(seed, "Hand? *his swallows yours completely* ...Sorry. Big hands. Is that okay?",
                "*holds it like he's been given something he might drop* ...Okay."), 1, EtoTalk.Effect.HAND, EtoGesture.NONE);
            case "strong" -> r(one(seed, "Strong? Me? ...Nobody's ever- I'm not, though. You know I'm not.", "*stands up a little taller* ...Say that "
                + "again. Slowly. I want the boys to hear it."), 3, EtoGesture.SHY);
            case "love" -> married ? r(one(seed, "I love you too. Every day. I still don't know what I did to deserve it.",
                "...I love you. That's the one thing I'm sure I'm good at."), 3, EtoTalk.Effect.NONE, EtoGesture.HUG)
                : r(one(seed, "...You love me? *he has to sit down* ...Okay. Okay. I love you too. I have for a while. I didn't think I was allowed.",
                    "*goes absolutely still* Say it again. I want to make sure I heard it right."), 3, EtoTalk.Effect.NONE, EtoGesture.SHY);
            case "missed" -> r(one(seed, "I missed you more. I kept walking to the door to check. The boys started a betting pool.",
                "Good. I thought it was only me."), 2, EtoTalk.Effect.CLING, EtoGesture.CLING);
            case "pretty" -> r(one(seed, "Handsome? *rubs his beard* ...You need your eyes checked. Don't get them checked.", "*turns red from the "
                + "collar up* I- thank you. Nobody says that."), 2, EtoGesture.SHY);
            case "beard" -> r(one(seed, "You like it? I keep it because the mask fits better. ...And because I look less soft with it.",
                "*strokes it, pleased* I trim it myself. Badly. Thank you."), 2, EtoGesture.SHY);
            case "lean" -> r(one(seed, "Lean on you? I'm twice your size. ...Okay. Just for a minute.", "...Okay. Don't tell the boys their boss gets "
                + "tired."), 2, EtoTalk.Effect.CLING, EtoGesture.CLING);
            case "soft" -> r(one(seed, "...I don't know how not to try. If I stop, I'm just a big weak man in a cottage. ...But with you that's okay, "
                + "isn't it.", "*his shoulders come down for the first time all day* ...Thank you. You don't know how tired I am of pretending."), 4,
                EtoTalk.Effect.HUG, EtoGesture.HUG);
            case "tea" -> r(one(seed, "Tea! Yes. I can do tea. Tea is the one thing I can do. *already halfway to the kettle*",
                "*comes back with two cups and a lot of spilled tea* ...Most of it made it."), 2, EtoGesture.HUM);
            case "proud" -> r(one(seed, "Proud? Of me? ...What did I do? I didn't do anything.", "...Stop it. I'm going to cry, and then I'm going "
                + "to have to put the mask on so nobody sees."), 3, EtoTalk.Effect.NONE, EtoGesture.SHY);
            case "why" -> r(one(seed, "I've asked myself that every day. ...I think it's because I stay. I'm not strong. But I don't leave. Ever.",
                "Because you saw what I am and you stayed anyway. Nobody stays."), 4, EtoTalk.Effect.HUG, EtoGesture.HUG);
            case "baby", "tonight" -> EtoTalk.babyAnswer(c, what, s("A baby? Ours? ...I'd be a terrible father. I'd try so hard. Tonight.",
                    "A family. Me. A dad. ...I want that more than I've ever wanted anything. Tonight.",
                    "...I always thought I'd die before I got to want something like this. Tonight."),
                s("Another one? Another little one? ...Yes. Tonight.", "Two of them. I'm going to need a bigger cottage. Tonight.",
                    "...You're sure? Then yes. Tonight."),
                days -> "Already? You need to rest first. I'll be right here. Ask me in " + EtoTalk.days(days) + ". It's a yes. It'll always be a yes.",
                s("I haven't forgotten. I haven't thought about anything else. Tonight.", "Tonight. I'll send the boys out for the night.",
                    "Still yes. Tonight."),
                EtoGesture.HUG, EtoGesture.SHY);
            default -> r("Hm?", 0);
        };
        return new EtoTalk.Reply(EtoLines.fill(c, reply.line()), reply.regard(), reply.effect(), reply.gesture());
    }

    @Override
    public String giftsAsked(int seed) {
        return one(seed, "For me? You didn't have to. ...What is it?", "Presents? For me? Show me.");
    }

    @Override
    public String topicsAgain(int seed) {
        return one(seed, "What else?", "Go on.", "Something else?");
    }

    @Override
    public String busy() {
        return "Banjou is fighting. (Trying to.)";
    }

    @Override
    public String approach(EtoTalk.Context c) {
        int seed = c.seed();
        if (c.pregnant() && seed % 2 == 0) {
            return this.pregnant(c.family().stage(), seed);
        }
        return EtoLines.fill(c, switch (c.mood()) {
            case CLINGY, LONELY -> one(seed, "Hey. ...No reason. I just wanted to be next to you for a bit.", "You were gone all day. The cottage "
                + "was too big.");
            case AFFECTIONATE -> one(seed, "Come here a second. *hugs you, very gently, like you might break* ...Okay. That's all.",
                "I was thinking about you on watch. I walked into the gate.");
            case SULKY, JEALOUS -> one(seed, "Who was that? ...It's fine. It's none of my business. It's a little my business.",
                "I saw. It's fine. I'm going to go water the plants very angrily.");
            case GIDDY -> one(seed, "Guess what! The boys and me ran off three Clowns! ...They ran off three Clowns. I helped!",
                "Good day! Nobody got hurt! That almost never happens!");
            case SLEEPY -> one(seed, "*yawns enormously* Sit with me? If I fall asleep, just leave me. I'm heavy.", "Long night. Stay a while?");
            case BLOODTHIRSTY -> one(seed, "*his kakugan lit, embarrassed about it* I'm hungry. Stay where I can see you. Not because- I'm just hungry.",
                "Give me a minute. I don't want to be like this in front of you.");
            default -> one(seed, "Walk with me? I'm doing the rounds. It's mostly checking on flowers.", "{name}! Hey. I've got an hour. Want to "
                + "sit outside?", "You look like you could use a cup of tea.");
        });
    }

    @Override
    public String welcome(EtoTalk.Context c) {
        return one(c.seed(), "You're back! I put the kettle on the minute I saw you.", "Home. Come in. Mind the pots.",
            "There you are. I kept your chair.");
    }

    @Override
    public String kissed(int seed, boolean married) {
        return married ? one(seed, "...Still gets me. Every time.", "Again? ...Again.", "Mine. *very quietly* ...Still can't believe it.")
            : one(seed, "...Oh. Oh wow. I forgot what I was going to say.", "*pulls back, red from the beard up* ...That was- was that okay?",
                "Okay. Wow. ...Don't tell the boys. They'll never let me hear the end of it.");
    }

    @Override
    public String peck(int seed) {
        return one(seed, "*a quick, clumsy kiss on your cheek, then he looks away fast*", "*bends down and kisses the top of your head* ...Sorry. Couldn't help it.");
    }

    @Override
    public String stolen(int seed) {
        return one(seed, "*kisses you before he can lose his nerve, then stares at you, amazed* ...I did it.",
            "*steals a kiss and immediately goes red* ...That was me being bold. How was it?");
    }

    @Override
    public String kissWaiting(String name) {
        return name + " bends down, closes his eyes, and waits, holding very still...";
    }

    @Override
    public String kissHint(int seed) {
        return one(seed, "...Um. If you wanted to. You could.", "*clears his throat and looks very hard at your mouth, then away*");
    }

    @Override
    public String babyNight(EtoTalk.Context c) {
        return one(c.seed(), "It's dark. ...You still want this? ...Me too. More than anything.", "I sent the boys out. All of them. ...Come here.");
    }

    @Override
    public String held(int seed) {
        return one(seed, "Don't let go. ...Please.", "A little longer.", "*holds on, very carefully* ...Okay.");
    }

    @Override
    public String pulled(int seed) {
        return one(seed, "Come back here. I wasn't done.", "Closer. ...There.", "Come here, you.");
    }

    @Override
    public String hanging(int seed) {
        return one(seed, "...Oh. Okay.", "Fine. That's fine.", "Never mind.");
    }

    @Override
    public String hugged(int seed) {
        return one(seed, "*wraps you up completely, very gently* ...Am I squeezing too hard? Tell me if I'm squeezing too hard.",
            "*his blue wing curls round you too before he can stop it* ...Sorry. It does that.", "...Oh. That's nice. That's really nice. Don't stop.");
    }

    @Override
    public String handTaken(int seed) {
        return one(seed, "*his hand swallows yours* ...Hi.", "Holding hands. In front of everyone. Okay. I can do this.", "*squeezes, carefully*");
    }

    @Override
    public String handLet(int seed) {
        return one(seed, "Oh. Okay.", "...I'll just hold my other hand, then.");
    }

    @Override
    public String partnerHit(int seed, boolean married) {
        return married ? one(seed, "Ow! ...What was that for? Did I do something?", "Hey. Not you. You don't get to do that.")
            : one(seed, "Ow! Okay, I deserved that. Probably.", "Hey! I bruise, you know.");
    }

    @Override
    public String serious(int seed) {
        return one(seed, "Okay. I'm scared. I'm still not moving.", "Right. Right. Stand your ground. You can do this.",
            "*his voice shakes, and he stays where he is anyway* ...Last chance to walk away.");
    }

    @Override
    public String maniac(int seed) {
        return one(seed, "I'm still standing! I'm still standing!", "That hurt! ...I'm not going down!", "Come on, then! I've been hit harder! ...I haven't.");
    }

    @Override
    public String protect(EtoTalk.Context c) {
        return one(c.seed(), "Get behind me. ...I'm big. I'm a good wall.", "Don't you touch them!", "You'll have to go through me. ...It won't be hard, "
            + "but you'll have to.");
    }

    @Override
    public String won(int seed) {
        return one(seed, "...Did I win? I won!", "Phew. Okay. That's done.", "And stay down! ...Please stay down.");
    }

    @Override
    public String puppy(EtoTalk.Context c, String kind, boolean sorry) {
        String what = kind == null ? "that" : "a " + kind;
        return sorry ? one(c.seed(), "You brought " + what + " to say sorry? ...That's not fair. It's working. Forgiven.",
            "An apology with fur. ...Forgiven. Completely.")
            : one(c.seed(), "Is that " + what + "?! For us? The boys are going to lose their minds. I'm losing mine.",
                "Oh. Oh no. It's perfect. It's staying. It's sleeping in the cottage. Don't argue.");
    }

    @Override
    public String letter(EtoTalk.Context c, boolean sorry) {
        return sorry ? one(c.seed(), "A sorry letter. ...You wrote it out and everything. Okay. Forgiven.", "...Thank you. Come here.")
            : one(c.seed(), "A letter? For me? *reads it, then reads it again, slower* ...I'm keeping this one in my coat. Over my heart. Is that stupid?",
                "Nobody's ever written to me. ...I'm going to read this every night. Don't watch me read it.");
    }

    @Override
    public String snub(int seed, boolean enemy) {
        return enemy ? one(seed, "Banjou stands up very slowly. Behind him, every Gas Mask in the garden turns to look at you.",
            "Banjou doesn't say anything. He just doesn't take his eyes off you.")
            : one(seed, "Banjou is busy watering the pots. He doesn't look up.", "Banjou gives you a stiff nod and goes back to what he was doing.");
    }

    @Override
    public String ring(EtoTalk.Context c, Romance.Refusal refusal) {
        if (refusal == null) {
            return one(c.seed(), "...A ring. For me. *He has to sit down on the step.* ...Yes. Yes. Of course yes. I just- give me a second. "
                + "I'm not crying. It's the filters.",
                "I spent my whole life thinking nobody would ever pick me. ...Yes. A thousand times yes.");
        }
        return switch (refusal) {
            case NOT_DATING -> "A ring? We haven't even- we're not even- put that away before I faint.";
            case CLAIMED -> EtoLines.fill(c, "I'm {partner}'s. ...It's a very nice ring, though.");
            case MARRIED_ELSEWHERE -> "You're married! Go home to them.";
            case TOO_SOON -> "Too soon. That's not a no. I just want to be sure you're sure. About me. Of all people.";
            case NOT_CLOSE -> "Not yet. I don't want you to regret it. I'm not much of a catch.";
            case ALREADY -> "We're already married! I was there! I cried the whole time!";
        };
    }

    @Override
    public String morning(EtoTalk.Context c) {
        return one(c.seed(), "Banjou is already up, sitting on the edge of the bed with two cups of tea, one for each of you.\n\"Morning. ...I "
            + "watched you sleep a bit. Is that creepy? That's creepy. Sorry. Tea?\"", "Banjou is asleep on his back, one big arm thrown over you, his "
            + "gas mask on the bedpost.\n\"...Mm. Five more minutes. The boys can guard themselves.\"");
    }

    @Override
    public String conceived(EtoTalk.Context c) {
        return one(c.seed(), "Banjou keeps looking at you like you might break. He's reorganised the whole kitchen twice before breakfast.\n\"...I'm "
            + "fine! I'm fine. Are you fine?\"", "Banjou has built a cradle. It's a little lopsided. He looks very proud of it.\n\"No reason!\"");
    }

    @Override
    public String pregnant(com.hex.kagune.control.family.Pregnancy.Stage stage, int seed) {
        return switch (stage) {
            case EARLY -> one(seed, "I'm not panicking. I'm not. Do you need anything? Anything at all?", "I read a book about it. I didn't understand "
                + "most of it.");
            case ALONG -> one(seed, "It kicked! It kicked my hand! It's stronger than me already!", "I talk to it at night. I tell it I'll try "
                + "really hard.");
            case LATE -> one(seed, "Sit. Rest. I'll carry anything. I'll carry you.", "Not long now. ...I'm scared. I'm happy. Both. Mostly scared.");
            case DUE -> one(seed, "Any day now. I've got the boys on watch in shifts.", "Stay close. Please. Right here.");
        };
    }

    @Override
    public String saw(Display shown, EtoVoice voice, boolean home, int seed) {
        boolean ours = voice.together();
        return switch (shown) {
            case NONE -> "";
            case EYES -> ours ? one(seed, "Your eyes. ...Still pretty. Scary pretty. I'm not scared. I'm a little scared.", "Red eyes. Matching. "
                + "That's nice.") : one(seed, "A ghoul. ...Okay. So am I. Let's both stay calm.", "Kakugan. Right. Easy. Nobody needs to do anything.");
            case KAGUNE -> ours ? one(seed, "Wow. Look at that. ...Mine's very small. Don't compare them.", "That's beautiful. Put it away before "
                + "the boys get nervous.") : one(seed, "Kagune out? Here? ...That's- okay. I'm not backing down. I'm stepping to the side.",
                "Easy. Easy. Nobody needs to fight.");
            case KAKUJA -> ours ? one(seed, "Hey. Hey, it's me. Come back. Follow my voice. I'm not going anywhere.", "Come back to me. Please.")
                : one(seed, "Kakuja. ...Everyone inside. Now.", "Okay. Okay. Nobody move. Especially me.");
            case DREAD -> ours ? one(seed, "...What is that? Come back. Please come back. I'm right here.", "Not you. I won't lose you to that.")
                : one(seed, "Run! Everyone run! That's not a fight, that's the end!", "Boys! Inside! Masks on! Don't breathe!");
        };
    }

    @Override
    public String dread(FinalSight sight, EtoVoice voice, boolean home, int seed) {
        boolean ours = voice.together();
        return switch (sight) {
            case WAR -> ours ? one(seed, "Take it off. That's not you. Please.", "Come back before it keeps you.")
                : one(seed, "That's the end of the world. Everyone out the back.", "I can't stand in front of that. Nobody can.");
            case PEACE -> ours ? one(seed, "...That's the king's. And it's you. ...And you let me hold your hand.", "You carry that and you still "
                + "come home to my cottage?") : one(seed, "Nobody move. Nobody breathe.", "...Oh.");
            case DRAGON -> ours ? one(seed, "COME BACK. I'll drag you out of there myself.", "Not you. Not to that.")
                : one(seed, "Boys! RUN! That eats cities!", "That's not a fight. RUN.");
            case RINKAKU -> ours ? one(seed, "Easy. Easy. Come back down. I'm right here.", "Breathe. I've got you.")
                : one(seed, "Rinkaku kakuja! Get back, all of you!", "Big tendrils. Keep your distance. Keep MY distance.");
            case UKAKU -> ours ? one(seed, "Like a storm with your face. ...Come down, love.", "I see you in there. Come back.")
                : one(seed, "Ukaku kakuja! Down!", "Everyone behind the wall!");
            case NONE -> "";
        };
    }

    @Override
    public EtoGesture dreadGesture(FinalSight sight, boolean ours) {
        return switch (sight) {
            case WAR, DRAGON -> EtoGesture.FLINCH;
            case PEACE -> EtoGesture.AWE;
            case RINKAKU, UKAKU -> ours ? EtoGesture.AWE : EtoGesture.FLINCH;
            case NONE -> EtoGesture.NONE;
        };
    }

    @Override
    public String initiative(EtoInitiative.Act act, int seed) {
        return switch (act) {
            case HAND -> one(seed, "Can I- your hand? ...Thanks.", "Hold my hand? I'm nervous. I'm always nervous.");
            case CLING -> one(seed, "*offers you his arm, very formally* ...Here.", "Stay by me. Closer. The boys are watching. Let them.");
            case HUG -> one(seed, "Come here. I need a hug. Don't make it a big deal.", "Hug? ...Hug.");
            case LEAN -> one(seed, "Lean on me. I'm good at being leaned on. It's the size.", "Rest. I'll keep watch.");
            case HUM -> one(seed, "*hums something low and off-key, then notices you listening and stops* ...What.", "*hums while he waters the pots*");
            case READ -> one(seed, "Read to me? Something calm. Something where nobody gets hurt.", "I like listening to you read.");
            case DANCE -> one(seed, "Dance? Me? I'll step on you. ...Okay. One dance.", "Come on. Nobody's watching. Everybody's watching. Dance anyway.");
            default -> "";
        };
    }

    @Override
    public String moment(EtoInitiative.Moment moment, boolean together, int seed) {
        return switch (moment) {
            case KILL -> together ? one(seed, "Nice one! You're amazing.", "Remind me never to make you angry.") : one(seed, "Good.", "That's done.");
            case HURT -> together ? one(seed, "You're bleeding! Who did that? Point. I'll- I'll try. Point anyway.", "Hey. Look at me. You're okay. I've got "
                + "you.") : one(seed, "You're hurt. Sit down. I've got bandages.", "That looks bad. Let me see.");
            case RAIN -> together ? one(seed, "Rain! Get under my coat. Both of us won't fit. You get under it.", "Let's wait it out inside. I'll make tea.")
                : one(seed, "Rain. Good for the flowers.", "Ugh. Rain.");
            case DUSK -> together ? one(seed, "Getting dark. I'll put the mask on soon. Stay near me.", "The sun's going. I like this bit with you.")
                : one(seed, "Getting dark. Masks soon.", "Dusk.");
            case DAWN -> together ? one(seed, "Morning. We made it through another one. ...Hi.", "Dawn. The mask comes off. It's just me.")
                : one(seed, "Morning.", "Dawn. Mask off.");
        };
    }

    static final List<EtoTopics.Topic> TOPICS = List.of(
        new EtoTopics.Topic("gasmasks", c -> true, c -> "Who are the Gas Masks?",
            c -> EtoVoice.of(c).pick(c.seed(),
                s("My boys. {partner}'s friend now too, I suppose."),
                s("My gang. Ghouls nobody else wanted. We wear the masks so nobody knows our faces, we look after each other, and we start nothing. "
                    + "That's the whole rule.", "My boys. Hurt one and you've hurt all of us. ...They'd be fine without me. I wouldn't be fine without them."),
                s("They're my family. Every one of them came here with nothing. I gave them a mask and a place to sleep. That's all I had to give."),
                null,
                s("My family. And you. The family got bigger. I'm terrified. It's wonderful."),
                s("Still my family. You're the part of it I come home to.")),
            List.of(
                new EtoTopics.Follow("Can I join?", c -> r(together(c) ? "You're already one of us, love. ...Officially, you'd have to earn the mask. "
                    + "Ask me properly sometime." : "Join? You? ...Maybe. Ask me properly. I'll take you seriously. Somebody should.", 1, EtoGesture.LOOK)),
                new EtoTopics.Follow("And the Clowns?", c -> r("The Clowns. Laughing at everything. Laughing at us. They pick fights with my boys for "
                    + "fun. We fight them on sight now. I don't like it. I like them hurting my boys less.", 2, EtoGesture.HUFF)),
                new EtoTopics.Follow("Why gas masks?", c -> r("Filters. If you can't smell the blood, it's easier not to want it. Mostly. ...And "
                    + "nobody can see your face when you're scared.", 3, EtoGesture.LOOK)))),

        new EtoTopics.Topic("weak", c -> c.regard() >= NpcBonds.FRIENDLY, c -> "Are you really their leader?",
            c -> one(c.seed(), "Yeah. I know. I'm the weakest ghoul in my own gang. Everyone knows. I'd rather say it first than hear it.",
                "I'm not the strongest. I'm not the smartest. I'm the one who doesn't leave. Turns out that counts for something."),
            List.of(
                new EtoTopics.Follow("That's what makes a leader.", c -> r("...You think so? Huh. Nobody's ever put it like that.", 3, EtoGesture.SHY)),
                new EtoTopics.Follow("You should train.", c -> r("I train every day. Every single day. This is me after the training.", 2, EtoGesture.LAUGH)),
                new EtoTopics.Follow("Come here.", c -> together(c), c -> r("...Okay. *leans down into it* Thanks.", 4, EtoTalk.Effect.HUG,
                    EtoGesture.HUG)))),

        new EtoTopics.Topic("fight", c -> true, c -> "How do you fight?",
            c -> one(c.seed(), "Badly. ...I've got one wing. It's small. It's blue. It doesn't shoot anything. I just hit people with it.",
                "I slap them with my wing until they get bored. It works more often than you'd think."),
            List.of(
                new EtoTopics.Follow("Just one wing?", c -> r("Just the one. And it's tiny. The other one never came in. ...Don't laugh. Everyone laughs.",
                    2, EtoGesture.SHY)),
                new EtoTopics.Follow("Teach me.", c -> r("Teach you to fight? Ask anyone but me. I mean that kindly.", 0, EtoGesture.SHRUG)))),

        new EtoTopics.Topic("mask", c -> c.regard() >= NpcBonds.FRIEND, c -> "Why the mask at night?",
            c -> one(c.seed(), "At night the hunger's worse. The filter helps. And at night people can't see I'm scared.",
                "It's a habit. Night comes, mask goes on. You feel braver behind it. Not stronger. Just braver."),
            List.of(
                new EtoTopics.Follow("You don't need it with me.", c -> r("...I'll try. For you. It might take a while.", 3, EtoGesture.SHY)),
                new EtoTopics.Follow("Can I wear one?", c -> r(together(c) ? "*hands you his spare, very seriously* There. Now you're a Gas Mask. "
                    + "Unofficially." : "Ha. You'd have to join first. ...Or ask very nicely.", 2, EtoGesture.LAUGH)))),

        new EtoTopics.Topic("flowers", c -> c.regard() >= NpcBonds.FRIENDLY, c -> "Who waters all these flowers?",
            c -> one(c.seed(), "...One of the boys. ...Me. It's me. Don't tell anyone.", "Somebody has to. The cottage looked so sad when we found it."),
            List.of(
                new EtoTopics.Follow("It's sweet.", c -> r("It's not sweet. It's- okay, it's a little sweet. Keep it to yourself.", 3, EtoGesture.SHY)),
                new EtoTopics.Follow("Which are your favourite?", c -> r("The blue orchids. They grow where nothing else will. I like that about them.",
                    2, EtoGesture.HUM)))),

        new EtoTopics.Topic("serious", c -> together(c), c -> "Why do you try so hard to look tough?",
            c -> one(c.seed(), "*long pause* ...Because if they knew how scared I was, they'd be scared too.", "Because somebody has to look like they "
                + "know what they're doing. Even if it's the wrong somebody."),
            List.of(
                new EtoTopics.Follow("You can be scared with me.", c -> r("...Don't say things like that. I'll believe you.", 4, EtoGesture.SHY)),
                new EtoTopics.Follow("I think you're brave.", c -> r("Brave's when you're scared and you do it anyway. ...Huh. Okay. Maybe I am.", 4,
                    EtoGesture.SHY)))),

        new EtoTopics.Topic("baby", EtoTalk.Context::pregnant, c -> "How's our baby?",
            c -> switch (c.family().stage()) {
                case EARLY -> "Small. Very small. I've already childproofed the cottage twice.";
                case ALONG -> "Kicking! Strong kicks. Gets that from you.";
                case LATE -> "Big. You're doing amazing. I'm doing nothing. I'm making tea.";
                case DUE -> "Any day now. ...Stay close. Please.";
            },
            List.of(
                new EtoTopics.Follow("Will they be strong?", c -> r("Stronger than me. That's not hard. ...Kind, I hope. Like you.", 4, EtoGesture.HUG)),
                new EtoTopics.Follow("Hold me.", c -> r("...Come here. Both of you.", 4, EtoTalk.Effect.CLING, EtoGesture.CLING)))),

        new EtoTopics.Topic("family", c -> c.married() && c.mixed() && !c.pregnant() && c.family().children() == 0, c -> "Do you want a family?",
            c -> c.family().wantsBaby() ? "Tonight. ...If you still want to. I do. So much."
                : "A family? Me? ...I've got a garden full of masked ghouls. With you, I'd want a real one. Little masks. Tiny ones.",
            List.of(
                new EtoTopics.Follow("Someday.", c -> r("Someday. I'll start building the cradle now. Just in case.", 2, EtoGesture.SHY)))));

    @Override
    protected List<EtoTopics.Topic> topics() {
        return TOPICS;
    }
}
