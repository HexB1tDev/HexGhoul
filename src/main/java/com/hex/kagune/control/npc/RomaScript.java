package com.hex.kagune.control.npc;

import java.util.List;

/**
 * 4.1.49: Roma's words (pure, tested). The owner: "she is the leader of the clowns, can be romanced, has a messed up sense of humor but can
 * be romatic when comes to it". The ringmaster of the Clowns' circus tent: every joke she makes is about death, she is delighted by
 * everything and embarrassed by nothing, and her own people would follow her off a roof for the punchline. Under it, a girl who laughs so
 * nobody asks why; with the one she loves the act drops, all at once, and she means every word. She dates anyone.
 */
final class RomaScript extends CompanionScript {
    @Override
    protected String key() {
        return "roma";
    }

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
        return "I brought you fireworks. (the rocket in your hand)";
    }

    @Override
    protected String flowerLabel() {
        return "I picked you an allium. (the one in your hand)";
    }

    @Override
    protected String lessonLabel(EtoTalk.Context c) {
        return "Teach me something.";
    }

    @Override
    public String title(EtoTalk.Context c) {
        return switch (EtoVoice.of(c)) {
            case WIFE -> "Your wife (the ringmaster)";
            case PARTNER -> "Yours (she told the whole circus)";
            case CLAIMED -> "Spoken for";
            case CLOSE -> "Roma";
            default -> "The Clowns' ringmaster";
        };
    }

    @Override
    public String greeting(EtoTalk.Context c) {
        int seed = c.seed();
        String line = switch (EtoVoice.of(c)) {
            case CLAIMED -> one(seed, "Ooh, it's you! {partner}'s, by the way. I'm taken. Tragically. Hilariously.",
                "Hi hi! I belong to {partner}. Don't make it weird. Make it funny.", "You again! I'm spoken for, but I'm still a great audience.");
            case WARY -> c.night() ? one(seed, "Well well WELL. A visitor. At night. In a tent full of masked ghouls. That's either very brave or very "
                + "delicious.", "Shh! The Clowns are sleeping. ...No they're not. They're right behind you. Kidding! ...Look anyway.",
                "Welcome to the show! Tickets are free. Leaving costs extra.")
                : one(seed, "A new face! Ooh. Can I keep it? Kidding. Mostly. Who are you?", "Step right up! Nobody ever steps right up. Why are you "
                    + "stepping right up?", "Hello hello! You look like someone who's about to make a terrible decision. I love that.",
                    "Oh! A guest! Nobody comes in here on purpose. Are you lost or are you interesting?");
            case FRIENDLY -> one(seed, "You came back! Most people don't. Most people can't. Ha!", "{name}! My favourite audience member! My ONLY "
                + "audience member.", "Look who it is! Sit, sit. Mind the bloodstain. It's paint. It's mostly paint.",
                "Ooh, perfect timing. I was just about to do something stupid. Watch!");
            case CLOSE -> one(seed, "{name}! Come here, come here, I saved you the good crate.", "There you are. The tent was too quiet. I don't "
                + "like quiet. Quiet is when the jokes run out.", "You look tired. Want to hear about the time I got stabbed? It's a funny one.",
                "Hiii. I made the Clowns do a drumroll when they saw you coming. They refused. Cowards.");
            case PARTNER -> c.mood() == EtoMood.SULKY ? one(seed, "Oh. You. ...I'm not sulking. I'm doing a very serious mime about being "
                + "ignored.", "Hmph. I wrote a joke about you. It's mean. I'll tell it later.")
                : c.daysAway() >= 3 ? one(seed, "THREE DAYS. I had the Clowns drawing up your obituary. It was a comedy. ...I hated it. Come here.",
                    "You're alive! I bet against you. I lost. Best money I ever lost.")
                : one(seed, "Sweetheart! You came! Everybody look, my sweetheart came!", "There's my favourite. ...No joke. You're just my favourite.",
                    "Ta-daaa! You! The best act in the whole show!", "Hi, you. I've been grinning at nothing for an hour. The Clowns are concerned.");
            case WIFE -> c.pregnant() ? one(seed, "Sit down! Feet up! The ringmaster's wife does NOT do stunts. Ringmaster's orders.",
                "Careful! There's a little clown in there. ...Okay, that was cute. I'm allowed one cute.")
                : one(seed, "Welcome home, wife of the ringmaster! Big round of applause! ...The Clowns aren't clapping. Clap!", "You're home. "
                    + "Good. The tent feels like a tent again.", "There she is! Or he is! The love of my life and also my favourite target.",
                    "Hi, love. I kept your seat warm. I sat on it. That's how.");
        };
        return EtoLines.fill(c, line);
    }

    @Override
    public EtoTalk.Reply answer(EtoTalk.Context c, NpcTalk.Choice choice) {
        EtoVoice voice = EtoVoice.of(c);
        int seed = c.seed();
        return switch (choice) {
            case WHO -> new EtoTalk.Reply(voice == EtoVoice.CLAIMED ? EtoLines.fill(c, "Roma! Ringmaster of the Clowns. Taken, by {partner}. "
                + "Ta-da.") : "Roma! Ringmaster of the Clowns, keeper of the tent, painter of the masks, teller of the jokes nobody laughs at. "
                + "That's my gang out there, in the faces. They protect each other, they start nothing, and if you hurt one of them... well. "
                + "It'd be funny for me. Less for you.", c.has(NpcBonds.ASKED) ? 0 : EtoRules.WHO, EtoTalk.Effect.NONE);
            case CHAT -> new EtoTalk.Reply(EtoLines.fill(c, voice.pick(seed,
                s("Quick quick, {partner} gets jealous. Actually no, {partner} gets bored. Same thing."),
                s("Talk! I'll juggle. I listen better when I'm juggling.", "Go on. Make it interesting. Or tragic. Tragic's fine."),
                s("Sit! Tell me something. Anything. Tell me how you'd die if you could pick.", "Talk to me. The tent echoes. It's like having friends."),
                s("Come here and talk. I like your voice. It's the least funny thing about you and I mean that as a compliment.",
                    "Tell me everything. Start with the gossip, end with the murders."),
                s("Talk to me, sweetheart. ...No, keep going. I'm smiling because of you, not the joke I'm thinking of.",
                    "Sit with me. Closer. ...Closer. There. Now talk."),
                s("Tell me about your day, love. I'll pretend the boring parts are a comedy.", "Come talk. I saved up all my quiet for you."))),
                c.chattedToday() ? 0 : EtoRules.CHAT, EtoTalk.Effect.TALK);
            case BOOK -> errandItem(c, "Fireworks? Your hands are empty. Is that the joke? It's a good joke.",
                "One bang a day! Too many and the neighbours start shooting back.", "Fireworks! For me! I'm going to set them off at the worst "
                    + "possible moment. Thank you, sweetheart.", "Ooh. A firework. Every show needs a bang. Leave it there. Don't touch it. "
                    + "It's mine now.", EtoGesture.GIGGLE);
            case FLOWER -> flowerGiven(c, "A flower? Where? Is it invisible? I love invisible flowers.",
                "One flower a day. I'm a ringmaster, not a garden.", "An allium. It's PURPLE. Like my... oh, you noticed. You notice things. "
                    + "...I'm putting it on my mask.", "A purple flower. For the clown. ...That's very sweet. Nobody does sweet in here. "
                    + "I'll keep it.", EtoGesture.SHY);
            case GIFT -> c.meat() ? r(c.ghoul() ? "Dinner! Ooh, who was it? Don't tell me. I like a mystery meat. ...Thank you. Really."
                : "Where did a human get THAT? I have so many questions and I want none of the answers. I'll take it.", 3)
                : r("You're holding nothing. Nothing's my second favourite gift.", 0);
            case PUPPY -> c.puppy() == null ? r("A what? There's nothing there. ...Is it a ghost puppy? I want it.", 0)
                : new EtoTalk.Reply(this.puppy(c, c.puppy(), false), 8, EtoTalk.Effect.PUPPY, EtoGesture.PET);
            case LETTER -> c.letter() ? new EtoTalk.Reply(this.letter(c, false), c.giftedToday() ? 0 : 4, EtoTalk.Effect.LETTER, EtoGesture.READ)
                : r("A letter? Where? Did you write it in invisible ink? Romantic!", 0);
            case LESSON -> lesson(c, LESSONS);
            case FAVOUR -> errand(c, this.key(),
                "A favour? From the ringmaster? I don't know you well enough to get you killed yet. Come back later!",
                "That's the whole programme! Every act done. ...Was there something you wanted to ask me? You've got a face.",
                "I need nothing. I have you. That's a lot. That's like two shows.",
                "{partner} handles all my favours. And my murders. Mostly my murders.", "You're married! Go home! Go be married AT someone!");
            case ASK_OUT -> !c.mayDate() ? r(c.claimed() ? EtoLines.fill(c, "I'm {partner}'s. It's the funniest thing that's ever happened to me.")
                    : "You've got someone! I don't steal. ...Okay, I steal. Not people. Go home.", 0)
                : c.step() != EtoRules.Step.ASK ? r("Ask me what? Use your words. Or a mime. I'm good at mimes.", 0)
                : new EtoTalk.Reply("...Wait. You're serious. *For once she doesn't laugh.* ...Yes. YES. Oh, I'm going to be insufferable about this. "
                    + "Everybody! EVERYBODY! ...They're asleep. Doesn't matter. Yes.", 0, EtoTalk.Effect.DATE, EtoGesture.SHY);
            case KISS -> c.dating() ? new EtoTalk.Reply(one(seed, "A kiss! In front of the Clowns? ...Good. Let them learn something.",
                "Pucker up, sweetheart. ...No, don't actually pucker. That's terrifying."), 0, EtoTalk.Effect.KISS)
                : r("Ooh, forward! No. Not yet. Buy me a firework first.", 0);
            case HUG -> c.dating() ? new EtoTalk.Reply(this.hugged(seed), 0, EtoTalk.Effect.HUG) : r("A hug? With all my tendrils? Bold. Later.", 0);
            case SWEET -> c.dating() ? new EtoTalk.Reply(EtoLines.fill(c, one(seed, "Ooh, flattery! Go on! More! ...Too much. I'm going pink. "
                + "Stop.", "Say something sweet. I dare you. I bet I don't blush. ...I'm blushing.", "Go on, I'm listening. Make it embarrassing.")), 0,
                EtoTalk.Effect.SWEET) : r("Compliments! Save them, I'm not done being suspicious of you.", 0);
            case NIGHT -> !c.dating() ? r("Ha! No. Nice try. I'll put that in the show though.", -1) : new EtoTalk.Reply(c.night() && !c.nightUsed()
                ? one(seed, "Stay the night? In the tent? With the Clowns snoring? ...Yes. Bring earplugs.",
                    "Yes. Stay. I sleep badly. Nightmares. Very funny ones. Not with you here.")
                : one(seed, "It's daytime! Scandalous! ...Tonight.", "Not tonight, the ringmaster is exhausted. ...Stay close anyway."), 0,
                c.night() && !c.nightUsed() ? EtoTalk.Effect.NIGHT : EtoTalk.Effect.NONE);
            case PROPOSE -> new EtoTalk.Reply("", 0, EtoTalk.Effect.PROPOSE);
            case HOME -> c.stage() == Romance.Stage.MARRIED ? new EtoTalk.Reply(one(seed, "Ours! A house! With walls that aren't canvas! "
                + "I'm going to paint every one of them purple.", "Home. ...I've lived in a tent my whole life. Ours. Okay. Ours."), 2,
                EtoTalk.Effect.HOME) : new EtoTalk.Reply("Ours? We're not married! One act at a time, sweetheart.", 0, EtoTalk.Effect.NONE);
            case FOLLOW -> new EtoTalk.Reply(voice.together() ? one(seed, "Lead the way, love! The ringmaster is on tour!",
                "With you? Anywhere. Especially somewhere dangerous.") : one(seed, "A field trip! Ooh. I'll bring my legs.",
                "Okay! Lead on. If we die, I want it to be funny."), 0, EtoTalk.Effect.FOLLOW);
            case STAY -> new EtoTalk.Reply(voice.together() ? one(seed, "I'll wait right here. Don't get killed. I'll be furious.",
                "Here? Okay. I'll practise faces.") : one(seed, "Staying! Like a good little clown.", "I'll wait. I'm very good at waiting. "
                + "And terrible at it."), 0, EtoTalk.Effect.STAY);
            case DECLINE -> r("Boo! Your loss.", 0);
            case TEASE -> new EtoTalk.Reply(one(seed, "Ooh! Rude! Do it again!", "You think you can out-joke the ringmaster? ...You might. "
                + "Marry me. Kidding! ...Unless?", "Ha! Okay, that was good. I'm stealing it.", "Oh, you're MEAN. I love it. Who taught you that?"),
                1, EtoTalk.Effect.NONE, EtoGesture.LAUGH);
            case INSULT -> NpcBonds.clamp(c.regard() + EtoRules.INSULT) <= NpcBonds.ENEMY
                ? new EtoTalk.Reply("*The grin doesn't move. Everything else does.* ...That's not funny. Get out of my tent. Next time the Clowns "
                    + "won't wait for my word.", EtoRules.INSULT, EtoTalk.Effect.ANGER, EtoGesture.ANGRY)
                : new EtoTalk.Reply(voice == EtoVoice.CLAIMED ? EtoLines.fill(c, "{partner}'s going to hear that. And laugh at you. Out.")
                : one(seed, "Ha! ...No, that one actually hurt. Out.", "Booo. Terrible material. Get off my stage."), EtoRules.INSULT,
                EtoTalk.Effect.CLOSE, EtoGesture.SCOLD);
            case BACK -> new EtoTalk.Reply(EtoLines.fill(c, voice.pick(seed, s("Anything else?"), s("What else? Quick, I'm bored."),
                s("And?"), null, s("Hm? Go on, sweetheart.", "What is it, love?"))), 0, EtoTalk.Effect.MAIN);
            case LEAVE -> new EtoTalk.Reply("", 0, EtoTalk.Effect.CLOSE);
            default -> r("Hm? Say it louder. The tent eats words.", 0);
        };
    }

    /** She teaches nothing: what she knows is her legs and her tendrils, and she isn't telling. */
    static final LessonWords LESSONS = new LessonWords() {
        @Override
        public String teach(Teaching.Lesson lesson, EtoTalk.Context c) {
            return "Watch closely! ...Did you see it? No? Good. That's the trick. (" + lesson.name() + ")";
        }

        @Override
        public String refuse(Teaching.Refusal refusal, Teaching.View view, EtoTalk.Context c) {
            return one(c.seed(), "Teach you? Ha! A magician never tells. A clown definitely never tells. Mostly because we forget.",
                "Lesson one: laugh at everything. Lesson two: kick them in the head while they're laughing too. That's the whole course! "
                    + "You're welcome.", "I don't teach. I perform. Watch me in a fight sometime. Take notes. Bring a bucket.");
        }
    };

    @Override
    protected String[][] sweetSays() {
        return new String[][] {{"You're funny.", "funny"}, {"I love you.", "love"}, {"I missed you.", "missed"}, {"You're beautiful.", "pretty"},
            {"I like your mask.", "mask"}, {"Lean on me.", "lean"}, {"You don't have to perform with me.", "soft"}, {"Laugh for me.", "laugh"},
            {"I'm proud of you.", "proud"}, {"Why me?", "why"}};
    }

    @Override
    public EtoTalk.Reply sweetly(EtoTalk.Context c, String what) {
        int seed = c.seed();
        boolean married = c.married();
        EtoTalk.Reply reply = switch (what) {
            case "cling" -> new EtoTalk.Reply(one(seed, "Like this? *wraps both arms round yours* Mine. You're mine now. Legally.",
                "*hangs off your arm* Don't let go. I'll fall over. On purpose."), 2, EtoTalk.Effect.CLING, EtoGesture.NONE);
            case "hand" -> new EtoTalk.Reply(one(seed, "Hand! *takes it and swings it like a kid* Look at us. Disgusting. Keep going.",
                "...Your hand's warm. Mine's always cold. That's a ghoul thing. Or a me thing."), 1, EtoTalk.Effect.HAND, EtoGesture.NONE);
            case "funny" -> r(one(seed, "I KNOW. Thank you. Finally someone gets it.", "...You think so? Really? ...Nobody laughs. You laugh. "
                + "That's the whole reason."), 2, EtoGesture.LAUGH);
            case "love" -> married ? r(one(seed, "I love you too. Every day. Even the boring ones. Especially the boring ones.",
                "...I love you. No joke. Look, I'm not even smiling. ...Okay, I'm smiling."), 3, EtoTalk.Effect.NONE, EtoGesture.HUG)
                : r(one(seed, "...You can't just SAY that. I had a whole bit planned. It's ruined. ...I love you too. There. Happy?",
                    "*goes completely still* ...Say it again. Slower. I want to remember the exact face you made."), 3, EtoTalk.Effect.NONE,
                    EtoGesture.SHY);
            case "missed" -> r(one(seed, "I missed you more. I counted. I counted out loud. The Clowns made me stop.",
                "Good. I'd hate to be the only idiot in this relationship."), 2, EtoTalk.Effect.CLING, EtoGesture.CLING);
            case "pretty" -> r(one(seed, "Beautiful? Me? Under all the paint? ...Liar. ...Say it again.", "*poses dramatically* I KNOW. ...Thank "
                + "you. Really."), 2, EtoGesture.SHY);
            case "mask" -> r(one(seed, "You like it?! I painted it myself! It took a week! It's crying AND laughing! Like me!",
                "...It's the only one that's mine. The others I give away. ...I'll paint you one. A happy one."), 2, EtoGesture.GIGGLE);
            case "lean" -> r(one(seed, "Lean? I'll LIE on you. *does*", "...Okay. Just for a minute. Don't tell the Clowns their ringmaster gets "
                + "tired."), 2, EtoTalk.Effect.CLING, EtoGesture.CLING);
            case "soft" -> r(one(seed, "...I don't know how not to. If I stop, I don't know what's left. ...Okay. Show me. Slowly.",
                "*the grin falters, then goes soft* ...With you I keep forgetting to. It's terrifying. I like it."), 4, EtoTalk.Effect.HUG,
                EtoGesture.HUG);
            case "laugh" -> r(one(seed, "*laughs so hard she snorts, then laughs at the snort* ...Happy?", "Ha! ...Haha! ...Okay that one was real. "
                + "That one was for you."), 2, EtoGesture.LAUGH);
            case "proud" -> r(one(seed, "Of me? ...Nobody's ever said that. They say \"funny\" or \"crazy\". ...Proud. Huh. I'm keeping that one.",
                "...Stop it. I'm going to cry and ruin my paint and it's YOUR fault."), 3, EtoTalk.Effect.NONE, EtoGesture.SHY);
            case "why" -> r(one(seed, "Because you laughed. Everybody else runs. You laughed. ...And then you stayed.",
                "Because you're the only one who ever asked what was under the mask. And then didn't mind the answer."), 4, EtoTalk.Effect.HUG,
                EtoGesture.HUG);
            case "baby", "tonight" -> EtoTalk.babyAnswer(c, what, s("A baby? A tiny clown? ...Oh no. Oh no, I want that. Tonight.",
                    "A family. Me. A mother. ...That's the funniest thing I've ever heard. Yes. Tonight.",
                    "...I always thought I'd die before I got to want this. I want this. Tonight."),
                s("Another one! A whole troupe! Tonight.", "Two little clowns. The tent won't survive. Tonight!",
                    "...You're sure? Last time I was a nightmare. ...Then yes. Tonight."),
                days -> "So soon?! I'm still recovering from the last act. Ask me in " + EtoTalk.days(days) + ". The answer's yes, it's always yes.",
                s("I haven't forgotten. I've thought of nothing else. Tonight.", "Tonight. I told the Clowns to sleep outside.", "Still yes! Tonight."),
                EtoGesture.HUG, EtoGesture.SHY);
            default -> r("Hm?", 0);
        };
        return new EtoTalk.Reply(EtoLines.fill(c, reply.line()), reply.regard(), reply.effect(), reply.gesture());
    }

    @Override
    public String giftsAsked(int seed) {
        return one(seed, "Presents?! Show me show me show me.", "For me? Is it a body? It's fine if it's a body.");
    }

    @Override
    public String topicsAgain(int seed) {
        return one(seed, "What else?", "Next act!", "Go on, go on.");
    }

    @Override
    public String busy() {
        return "The ringmaster is busy. (She's fighting.)";
    }

    @Override
    public String approach(EtoTalk.Context c) {
        int seed = c.seed();
        if (c.pregnant() && seed % 2 == 0) {
            return this.pregnant(c.family().stage(), seed);
        }
        return EtoLines.fill(c, switch (c.mood()) {
            case CLINGY, LONELY -> one(seed, "Hi. ...No joke. I just wanted to stand next to you.", "You were gone ALL day. I counted the minutes. "
                + "Out loud.");
            case AFFECTIONATE -> one(seed, "Come here! No reason! I don't need a reason, I'm the ringmaster.", "I thought about you during a fight "
                + "and got kicked in the face. Worth it.");
            case SULKY, JEALOUS -> one(seed, "Who was THAT? ...I'm not jealous. I'm writing a very sad clown routine about it.",
                "I saw. It's fine. I'm going to go stare at a wall dramatically.");
            case GIDDY -> one(seed, "Guess what! Nobody died today! ...Okay, two people died, but they were awful.", "Best day ever! I found a "
                + "skull! I named it after you! ...As a compliment!");
            case SLEEPY -> one(seed, "*yawns* Sit with me. If I fall asleep, draw on my face. It's tradition.", "Wake me if something funny happens.");
            case BLOODTHIRSTY -> one(seed, "*grinning a bit too wide* I'm hungry. Not for you! Probably. Stay close.", "Give me a minute. The joke "
                + "in my head right now isn't one you'd like.");
            default -> one(seed, "Walk with me! I'll narrate. Badly.", "{name}! I have an hour and a terrible idea. Come.",
                "You look like you need a laugh. I have SO many.");
        });
    }

    @Override
    public String welcome(EtoTalk.Context c) {
        return one(c.seed(), "You're back! Big round of applause! ...Clowns? Applause? ...Fine. I'll clap.", "Home! Come here. Off stage.",
            "There you are. I kept a seat for you. Front row.");
    }

    @Override
    public String kissed(int seed, boolean married) {
        return married ? one(seed, "...Still gets me. Every time. That's not supposed to happen after the first act.", "Again! Encore!",
            "Mine. *grins against your mouth* Mine mine mine.")
            : one(seed, "...Oh. Oh no. I forgot the punchline.", "*pulls back, pink under the paint* ...That was not in the programme. ...Again.",
                "Okay. Okay. Wow. ...Don't tell the Clowns. They'll make a song.");
    }

    @Override
    public String peck(int seed) {
        return one(seed, "*a quick kiss, then a bow* Thank you, thank you!", "Gotcha! Ha! Didn't see that coming, did you?");
    }

    @Override
    public String stolen(int seed) {
        return one(seed, "*turns your face and kisses you, then cackles* Stolen! The ringmaster takes what she wants!",
            "Ha! Distraction and misdirection! The oldest trick in the circus!");
    }

    @Override
    public String kissWaiting(String name) {
        return name + " closes her eyes, puckers up dramatically, and peeks...";
    }

    @Override
    public String kissHint(int seed) {
        return one(seed, "...Ahem. The audience is waiting.", "*taps her cheek, very obviously*");
    }

    @Override
    public String babyNight(EtoTalk.Context c) {
        return one(c.seed(), "It's dark. ...You still want this? ...Me too. No jokes. Come here.", "I sent the Clowns out. All of them. ...Come here.");
    }

    @Override
    public String held(int seed) {
        return one(seed, "Don't let go! I'll float away! ...Kidding. Don't let go anyway.", "A little longer.", "Hold on. ...Tighter.");
    }

    @Override
    public String pulled(int seed) {
        return one(seed, "Get back here! I'm not done with you!", "Closer! ...Closer! There.", "Come here, you.");
    }

    @Override
    public String hanging(int seed) {
        return one(seed, "...Oh well! Next act!", "Fine! I'll hug a Clown!", "Your loss!");
    }

    @Override
    public String hugged(int seed) {
        return one(seed, "*squeezes until something cracks* ...Was that you? Ha! Sorry!", "*tendrils curl round you too* ...Oops. They like you.",
            "...Oh. That's nice. That's really nice. Don't stop.");
    }

    @Override
    public String handTaken(int seed) {
        return one(seed, "Holding hands! In public! Scandal!", "Your hand. Mine now.", "*swings it* Look how cute we are. Gross.");
    }

    @Override
    public String handLet(int seed) {
        return one(seed, "Boo.", "...Okay! Free hand! I'll use it for juggling.");
    }

    @Override
    public String partnerHit(int seed, boolean married) {
        return married ? one(seed, "Ow! ...Was that foreplay? Ask first!", "Hey! Not you. You don't get to do that.")
            : one(seed, "Ha! Ow! What was that for?", "If you want a fight, just ask! I'd love one!");
    }

    @Override
    public String serious(int seed) {
        return one(seed, "Oh, you're actually trying. Cute.", "Okay. Okay. No more jokes.", "*the grin stays. The eyes don't.* ...Last chance to laugh.");
    }

    @Override
    public String maniac(int seed) {
        return one(seed, "HAHAHA! Again! AGAIN!", "That HURT! I love it! Do it again!", "Is this the finale?! Is it?!");
    }

    @Override
    public String protect(EtoTalk.Context c) {
        return one(c.seed(), "Ah-ah. That one's mine.", "Touch them and I'll make a hat out of you.", "Behind me, sweetheart. Watch this.");
    }

    @Override
    public String won(int seed) {
        return one(seed, "And scene!", "Ta-da!", "Tough crowd. Dead crowd.");
    }

    @Override
    public String puppy(EtoTalk.Context c, String kind, boolean sorry) {
        String what = kind == null ? "that" : "a " + kind;
        return sorry ? one(c.seed(), "You brought " + what + " to say sorry? ...That's cheating. It worked. It always works.",
            "An apology with fur! ...Forgiven. Mostly because of the fur.")
            : one(c.seed(), "Is that " + what + "?! For the circus?! It's joining the act. It's the new ringmaster. I've been replaced.",
                "Oh. Oh no. It's perfect. It's staying. It lives here now.");
    }

    @Override
    public String letter(EtoTalk.Context c, boolean sorry) {
        return sorry ? one(c.seed(), "A sorry letter. ...You spelled it right and everything. Okay. Forgiven.", "...Accepted. Come here.")
            : one(c.seed(), "A letter? For me? *reads it, then reads it again with her mask off* ...I'm keeping this one under my pillow.",
                "Nobody writes to clowns. ...Thank you. I'll read it a hundred times. Don't watch me.");
    }

    @Override
    public String snub(int seed, boolean enemy) {
        return enemy ? one(seed, "Roma tilts her head and smiles at you. Behind her, every Clown in the tent stands up.",
            "Roma laughs once, very quietly, and doesn't take her eyes off you.")
            : one(seed, "Roma is busy painting a mask. She doesn't look up.", "Roma waves without turning round.");
    }

    @Override
    public String ring(EtoTalk.Context c, Romance.Refusal refusal) {
        if (refusal == null) {
            return one(c.seed(), "...A ring. You're on one knee. In MY ring. ...Oh, that's a good pun. That's the best pun. YES. YES. "
                + "*she's crying through the paint* Look what you did, it's running.",
                "...I always thought my funeral would be the biggest show I ever did. It's this. Yes. A hundred times yes.");
        }
        return switch (refusal) {
            case NOT_DATING -> "A ring?! We haven't even been on a date! Put that away before I say yes by accident.";
            case CLAIMED -> EtoLines.fill(c, "I'm {partner}'s! Put it away. ...It's a nice ring though.");
            case MARRIED_ELSEWHERE -> "You're married! Go home!";
            case TOO_SOON -> "Too soon! That's not a no. It's a \"come back later with more drama\".";
            case NOT_CLOSE -> "Not yet. I don't say yes to things I'm not sure of. Except fights. Always fights.";
            case ALREADY -> "We're married! I was there! I wore the good mask!";
        };
    }

    @Override
    public String morning(EtoTalk.Context c) {
        return one(c.seed(), "Roma is sitting cross-legged at the end of the bed, mask off, chin in her hands, watching you.\n\"Morning! You drool. "
            + "It's adorable. I've been up for an hour.\"", "Roma is asleep across you like a cat, one tendril curled round your ankle.\n\"...Mm. "
            + "Five more minutes. Ringmaster's orders.\"");
    }

    @Override
    public String conceived(EtoTalk.Context c) {
        return one(c.seed(), "Roma isn't making jokes this morning. She keeps putting her hand on her stomach and looking at it like a magic trick.\n"
            + "\"...Huh.\"", "Roma has drawn a tiny clown face on her belly in lipstick.\n\"No reason!\"");
    }

    @Override
    public String pregnant(com.hex.kagune.control.family.Pregnancy.Stage stage, int seed) {
        return switch (stage) {
            case EARLY -> one(seed, "I threw up on a Clown. He didn't laugh. Nobody laughs anymore.", "I'm craving something horrible. Don't ask.");
            case ALONG -> one(seed, "It KICKED! It kicks! It's mine! It does Kick Master!", "I tell it jokes. It kicks at the punchlines. "
                + "It has taste.");
            case LATE -> one(seed, "I'm the size of the tent. Somebody roll me.", "Not long. ...I'm scared. Isn't that funny? I'm not laughing.");
            case DUE -> one(seed, "Any day now! The grand finale!", "Stay. Please. Right here.");
        };
    }

    @Override
    public String saw(Display shown, EtoVoice voice, boolean home, int seed) {
        boolean ours = voice.together();
        return switch (shown) {
            case NONE -> "";
            case EYES -> ours ? one(seed, "Ooh, red eyes! Matching! We're matching!", "Your eyes. ...Pretty. Scary pretty. My favourite kind.")
                : one(seed, "A ghoul! Welcome, cousin! Wipe your feet.", "Nice eyes. Don't do anything I'd find funny.");
            case KAGUNE -> ours ? one(seed, "Ooh, show me show me! ...Mine's purple. Purple's better. Kidding. Yours is perfect.",
                "Look at that. Gorgeous. Put it away before the Clowns get excited.")
                : one(seed, "Kagune out in my tent? Bold! Let's see if it's funny.", "Oh, a show! I love a show!");
            case KAKUJA -> ours ? one(seed, "Hey. Hey. It's me. Come back. You're scaring the Clowns. You're scaring ME.",
                "Come back to me. Follow my voice. I'm the loud one.")
                : one(seed, "Kakuja! Everybody out! ...No, stay, this'll be great.", "Oh, now THAT'S an act.");
            case DREAD -> ours ? one(seed, "...What IS that? ...Come back. I'm not laughing. Come back.", "Not you. I won't lose you to that.")
                : one(seed, "Okay! Okay! Run! Running is the joke now!", "Clowns! Scatter! That's not a bit!");
        };
    }

    @Override
    public String dread(FinalSight sight, EtoVoice voice, boolean home, int seed) {
        boolean ours = voice.together();
        return switch (sight) {
            case WAR -> ours ? one(seed, "Take it off. That's not you. That's not funny.", "Come back before it keeps you.")
                : one(seed, "Oh, that's the end of the world. Clowns, out the back.", "I can't kick that. Nobody can kick that.");
            case PEACE -> ours ? one(seed, "...That's the king's. And it's you. ...I'm speechless. Me. Speechless. Write that down.",
                "You carry THAT and still let me call you sweetheart?") : one(seed, "Nobody move. Nobody make a joke.", "...Wow.");
            case DRAGON -> ours ? one(seed, "COME BACK. I'll drag you out myself, tendrils and all.", "Not you. Not to that.")
                : one(seed, "Clowns! RUN! That's a city-eater!", "Okay, that one's not funny. RUN.");
            case RINKAKU -> ours ? one(seed, "Easy. Easy. I know what that feels like. Come back down.", "Breathe. I'm right here.")
                : one(seed, "Rinkaku kakuja! Cousin! ...Stay back, all of you.", "Ooh. Big tendrils. Respect. Keep away.");
            case UKAKU -> ours ? one(seed, "Like a firework with your face! ...Come down, love.", "I see you in there. Come back.")
                : one(seed, "Ukaku kakuja! Duck!", "Everybody behind something heavy!");
            case NONE -> "";
        };
    }

    @Override
    public EtoGesture dreadGesture(FinalSight sight, boolean ours) {
        return switch (sight) {
            case WAR, DRAGON -> EtoGesture.FLINCH;
            case PEACE -> EtoGesture.AWE;
            case RINKAKU, UKAKU -> ours ? EtoGesture.AWE : EtoGesture.LAUGH;
            case NONE -> EtoGesture.NONE;
        };
    }

    @Override
    public String initiative(EtoInitiative.Act act, int seed) {
        return switch (act) {
            case HAND -> one(seed, "Hand! Give! ...Thank you.", "Hold my hand. I'm about to do something stupid and I want a witness.");
            case CLING -> one(seed, "*latches onto your arm* Mine.", "Stay by me. Closer. The Clowns are watching. Let them.");
            case HUG -> one(seed, "Hug! Incoming! Brace!", "Come here. Don't say anything. If you say something I'll make a joke.");
            case LEAN -> one(seed, "Lean on me! I'm very sturdy. Mostly tendrils.", "Rest. I'll keep watch. And make faces at you.");
            case HUM -> one(seed, "*hums the circus march, badly, with all the drum parts* ...Bom bom!", "*hums something slow, then stops when "
                + "you look* ...What. Clowns can hum.");
            case READ -> one(seed, "Read to me! Something with a murder in it.", "Read me something. I like your voice in the tent.");
            case DANCE -> one(seed, "Dance with me! Nobody's watching! Everybody's watching! Dance anyway!", "Come on, one dance. I lead. I always lead.");
            default -> "";
        };
    }

    @Override
    public String moment(EtoInitiative.Moment moment, boolean together, int seed) {
        return switch (moment) {
            case KILL -> together ? one(seed, "Ooh, clean! Ten out of ten!", "Encore!") : one(seed, "Ha!", "And scene.");
            case HURT -> together ? one(seed, "You're bleeding! WHO? Point. I'll kick them into next week.", "Hey hey hey. Look at me. "
                + "You're fine. I'm not laughing. You're fine.") : one(seed, "Ouch! That looks fun. Get it seen to.", "You're leaking.");
            case RAIN -> together ? one(seed, "Rain! Dance in it with me!", "Under the tent, quick! ...Or not. Let's get soaked.")
                : one(seed, "Rain! My paint!", "Ugh. Rain.");
            case DUSK -> together ? one(seed, "Showtime soon. Stay near me.", "The sun's going. The good part's coming.")
                : one(seed, "Dusk! Showtime.", "Lights down.");
            case DAWN -> together ? one(seed, "Morning. We made it. ...Hi.", "Dawn. The Clowns are asleep. It's just us.") : one(seed, "Morning!", "Dawn. Boo.");
        };
    }

    static final List<EtoTopics.Topic> TOPICS = List.of(
        new EtoTopics.Topic("clowns", c -> true, c -> "Who are the Clowns?",
            c -> EtoVoice.of(c).pick(c.seed(),
                s("My gang! {partner}'s too now, I suppose. Don't tell them."),
                s("My family! Ghouls who'd rather laugh than cry. Everyone in a mask, everyone different, nobody starts anything. "
                    + "We protect each other. That's the whole rule.", "My troupe! Hurt one, you've hurt all of us. That's the act."),
                s("They're mine. Every one of them came to me with nothing, and I gave them a face. ...Don't tell them I'm sentimental."),
                null,
                s("My family. And now you. The family's bigger. I'm terrified. It's great."),
                s("Still my family. You're the part of it I go home to.")),
            List.of(
                new EtoTopics.Follow("Can I join?", c -> r(together(c) ? "Join? You're IN, sweetheart. ...Officially is different. Officially, "
                    + "you have to earn the mask. Ask me again sometime." : "Ha! You? ...Maybe. Make me laugh first. Then we'll talk.", 1, EtoGesture.TILT)),
                new EtoTopics.Follow("And the Gas Masks?", c -> r("UGH. Those killjoys. No jokes, no faces, just breathing at people. "
                    + "We fight them on sight. It's not personal. Okay, it's personal.", 2, EtoGesture.HUFF)),
                new EtoTopics.Follow("You really protect each other?", c -> r("Every time. Nobody gets left behind. That's not a joke. "
                    + "That's the only thing I don't joke about.", 3, EtoGesture.LOOK)))),

        new EtoTopics.Topic("death", c -> c.regard() >= NpcBonds.FRIENDLY, c -> "Why all the death jokes?",
            c -> one(c.seed(), "Because death's the funniest thing there is! Everyone's so serious about it, and it happens to everyone anyway. "
                + "It's the oldest joke in the world.", "If you laugh at it first, it can't scare you. ...Mostly. It mostly works."),
            List.of(
                new EtoTopics.Follow("Does it work?", c -> r("...Most days. Not at night. At night it laughs back.", 3, EtoGesture.LOOK)),
                new EtoTopics.Follow("You're morbid.", c -> r("Morbid and adorable! Put it on my tombstone!", 2, EtoGesture.LAUGH)),
                new EtoTopics.Follow("Come here.", c -> together(c), c -> r("...Okay. No jokes for a minute. Just hold on.", 4,
                    EtoTalk.Effect.HUG, EtoGesture.HUG)))),

        new EtoTopics.Topic("fight", c -> true, c -> "How do you fight?",
            c -> one(c.seed(), "Legs! Kick Master! Everybody watches the tendrils, so I kick them in the face. Misdirection!",
                "Four tendrils and two legs. They never know which one's the joke and which one's the punchline."),
            List.of(
                new EtoTopics.Follow("Purple tendrils?", c -> r("Purple! Nobody else has purple. I asked. I asked very nicely, with my "
                    + "tendrils.", 2, EtoGesture.GIGGLE)),
                new EtoTopics.Follow("Teach me.", c -> r("Ha! A clown never tells. Watch me fight someday. Bring a bucket.", 0, EtoGesture.SHRUG)))),

        new EtoTopics.Topic("mask", c -> c.regard() >= NpcBonds.FRIEND, c -> "Why do you wear a mask?",
            c -> one(c.seed(), "Everyone wears a mask. Mine's just honest about it.", "So nobody can tell when I'm sad. ...That's a joke. "
                + "Laugh. Laugh!"),
            List.of(
                new EtoTopics.Follow("Can I see your face?", c -> r(together(c) ? "*lifts it, just for a second* ...Happy? That's the last "
                    + "free show." : "Ha! That's a second-date thing.", together(c) ? 4 : 1, EtoGesture.SHY)),
                new EtoTopics.Follow("You can be sad with me.", c -> r("...Don't say things like that. I'll believe you.", 3, EtoGesture.LOOK)))),

        new EtoTopics.Topic("serious", c -> together(c), c -> "Are you ever serious?",
            c -> one(c.seed(), "*long pause* ...With you. Only with you. It's really annoying.", "Sometimes. When it matters. You matter. "
                + "That's the problem."),
            List.of(
                new EtoTopics.Follow("It's not a problem.", c -> r("...Okay. Then it's the best problem I've ever had.", 4, EtoGesture.SHY)),
                new EtoTopics.Follow("Only for me?", c -> r("Only ever for you. Thirty Clowns and not one of them's seen it. Just you.", 4,
                    EtoGesture.SHY)))),

        new EtoTopics.Topic("baby", EtoTalk.Context::pregnant, c -> "How's our baby?",
            c -> switch (c.family().stage()) {
                case EARLY -> "Tiny! Unfunny! Making me throw up! Very much yours!";
                case ALONG -> "Kicking! Kick Master already! Gets it from me.";
                case LATE -> "Huge. I'm a tent. I'm a tent with a tent in it.";
                case DUE -> "Any day. ...Stay close. Please.";
            },
            List.of(
                new EtoTopics.Follow("Will they be a clown?", c -> r("Only if they want to be. ...They'll have a mask either way. I'm painting it now.",
                    4, EtoGesture.HUG)),
                new EtoTopics.Follow("Hold me.", c -> r("...Come here. Both of you.", 4, EtoTalk.Effect.CLING, EtoGesture.CLING)))),

        new EtoTopics.Topic("family", c -> c.married() && !c.pregnant() && c.family().children() == 0, c -> "Do you want a family?",
            c -> c.family().wantsBaby() ? "Tonight. ...If you still want to. I do. No joke."
                : "A family? Me? ...I've got a tent full of Clowns. ...With you, I'd want a real one. Little masks. Tiny ones.",
            List.of(
                new EtoTopics.Follow("Someday.", c -> r("Someday! I'll start painting the masks now. Just in case.", 2, EtoGesture.SHY)))));

    @Override
    protected List<EtoTopics.Topic> topics() {
        return TOPICS;
    }
}
