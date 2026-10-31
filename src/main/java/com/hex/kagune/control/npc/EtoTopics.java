package com.hex.kagune.control.npc;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * 1.3.2: the things that can be talked about with Eto (pure, tested): each a topic the player brings up, her answer, and what they
 * can say back ({@link Follow}), each with her reply, what it does to her regard, and what she does. Who they are to her
 * ({@link EtoVoice}), her mood and the hour change all of it; some topics only come up at night, or once they are together, or for a
 * human, or a ghoul.
 */
public final class EtoTopics {
    /** Something said back on a topic: its words and her reply. */
    public record Follow(String prompt, Predicate<EtoTalk.Context> when, Function<EtoTalk.Context, EtoTalk.Reply> reply) {
        Follow(String prompt, Function<EtoTalk.Context, EtoTalk.Reply> reply) {
            this(prompt, c -> true, reply);
        }
    }

    /** A topic: when it can come up, how the player brings it up, her answer, what can be said back. */
    public record Topic(String id, Predicate<EtoTalk.Context> when, Function<EtoTalk.Context, String> prompt, Function<EtoTalk.Context, String> answer,
                        List<Follow> follows) {
        /** The replies open to {@code c}. */
        public List<Follow> open(EtoTalk.Context c) {
            List<Follow> out = new ArrayList<>();
            for (Follow follow : this.follows) {
                if (follow.when().test(c)) {
                    out.add(follow);
                }
            }
            return out;
        }
    }

    private EtoTopics() {
    }

    private static EtoTalk.Reply r(String line, int regard) {
        return new EtoTalk.Reply(line, regard, EtoTalk.Effect.NONE, EtoGesture.NONE);
    }

    private static EtoTalk.Reply r(String line, int regard, EtoGesture gesture) {
        return new EtoTalk.Reply(line, regard, EtoTalk.Effect.NONE, gesture);
    }

    private static EtoTalk.Reply r(String line, int regard, EtoTalk.Effect effect, EtoGesture gesture) {
        return new EtoTalk.Reply(line, regard, effect, gesture);
    }

    private static String[] s(String... lines) {
        return lines;
    }

    private static boolean together(EtoTalk.Context c) {
        return EtoVoice.of(c).together();
    }

    /** Every topic, in the order they are offered. */
    public static final List<Topic> ALL = List.of(
        new Topic("reading", c -> true, c -> "What are you reading?",
            c -> EtoVoice.of(c).pick(c.seed(),
                s("Something you wouldn't like. It has no pictures and a lot of teeth."),
                s("A cookbook. For humans. I'm reading it the way you'd read a horror novel.",
                    "Poetry. Somebody bled very prettily onto these pages, and I respect that.",
                    "My own book, under another name. I'm checking whether I still hate chapter nine. I do."),
                s("A detective novel. The narrator did it. I guessed on page two. I always do. It's a curse.",
                    "A book about a girl who lives in a library. It's terribly written. I'd know."),
                null,
                s("A romance. Don't make that face. I'm taking notes on what not to do to you.",
                    "Nothing. I've been staring at the same page for an hour thinking about you. Happy?"),
                s("Our story. I keep adding pages. You keep making them good.",
                    "A book on gardening. I want to grow something with you. Probably not plants.")),
            List.of(
                new Follow("Read some to me?", c -> switch (EtoVoice.of(c)) {
                    case CLAIMED -> r("No.", 0);
                    case PARTNER, WIFE -> r("Lie down. Head here. ...'And the lamp went out, and the dark had teeth—' Stop smiling, you're ruining the mood.", 3,
                        EtoGesture.READ);
                    default -> r("...'And the lamp went out, and the dark had teeth.' That's the happy part.", 2, EtoGesture.READ);
                }),
                new Follow("Recommend me something.", c -> r("Anything with a monster who thinks it's the hero. Or a hero who finds out it's the monster. "
                    + "Same book, different cover.", 1)),
                new Follow("Isn't that boring?", c -> together(c)
                    ? r("Say that again and I'm reading it to you. Out loud. All of it.", 0, EtoGesture.POUT)
                    : r("Boring? You're boring. Books are a way to live a thousand lives without anyone finding the bodies.", -2, EtoGesture.POUT)))),

        new Topic("writing", c -> true, c -> "What do you write?",
            c -> EtoVoice.of(c).pick(c.seed(),
                s("Novels. Under a name you'd recognise. That's all you get."),
                s("Novels. Under a pen name you'd recognise, and a face you wouldn't. People love my books. They'd hate me. It's a nice arrangement."),
                null,
                s("Stories about people who don't fit anywhere. You'd make a good one. Bad ending, though."),
                s("Lately? You. In the margins of everything. It's ruining my career."),
                s("The truth, dressed up as lies. And lately, us, dressed up as nothing at all.")),
            List.of(
                new Follow("Can I read your next one?", c -> switch (EtoVoice.of(c)) {
                    case CLAIMED, WARY -> r("When it's published, like everyone else. Pay for it, too.", 0);
                    case FRIENDLY, CLOSE -> r("Maybe. If you promise to cry at the right parts.", 1);
                    default -> r("You'll be the first. You're also in it. Chapter twelve. Don't skip ahead.", 2, EtoGesture.GIGGLE);
                }),
                new Follow("Write about me.", c -> switch (EtoVoice.of(c)) {
                    case CLAIMED -> r("I write about {partner}. You're not interesting enough to be a footnote.", -1);
                    case PARTNER -> r("I already am. Every page. You just don't know which character you are yet.", 2, EtoGesture.HAIR);
                    case WIFE -> r("I have. Three hundred pages. You die in none of them. That's a first for me.", 3, EtoGesture.HAIR);
                    default -> r("I only write about people who'd make good tragedies. ...Give it time.", 1);
                }),
                new Follow("Why the secret name?", c -> r("Because a story should be judged by its teeth, not the mouth it came out of.", 1)))),

        new Topic("eye", c -> true, c -> "Your eye... why only one?",
            c -> c.granted() && !c.claimed() ? (together(c) ? "You'd know. You've got one too, now. Matching. I still can't believe you let me."
                : "You'd know. You have one too, now. How does it feel, being a little bit wrong?")
                : EtoVoice.of(c).pick(c.seed(),
                s("Because I was made, not born. That's the whole story you're getting."),
                s("It's a long story with a short ending: I was made, not born. Half of me is what I was given. The other half is what I took. "
                    + "The eye is just the half that shows."),
                null, null,
                s("Because the world made me half of something. You make me feel like a whole thing. ...That was disgusting. Forget I said it."),
                s("It's the part of me that's honest. It's looking at you right now. It likes what it sees.")),
            List.of(
                new Follow("Does it hurt?", c -> r("Only when I think about it. So, constantly. Ahaha— no, it's fine. Mostly.", 1)),
                new Follow("It's beautiful.", c -> switch (EtoVoice.of(c)) {
                    case CLAIMED -> r("Save it for someone who isn't taken.", 0);
                    case PARTNER, WIFE -> r("Stop. ...No, don't stop.", 4, EtoGesture.HAIR);
                    default -> r("...Say that again when it's looking at you in the dark.", 3, EtoGesture.HAIR);
                }),
                new Follow("Is that why you're alone here?", c -> together(c)
                    ? r("I'm not alone. I have four thousand books, a leaky roof, and you.", 2)
                    : r("I'm not alone. I have four thousand books and a leaky roof. And now, apparently, you.", 1)))),

        new Topic("library", c -> true, c -> "Why live in a library?",
            c -> together(c) ? "Because it was quiet, and I was hiding. Now I'm not hiding. It's still quiet. I like it better."
                : "Because nobody looks for a monster between the shelves. They look in alleys. I like being where no one thinks to look.",
            List.of(
                new Follow("Do people come here?", c -> together(c) ? r("Only you. I scare the rest off. On purpose.", 1, EtoGesture.GIGGLE)
                    : r("Sometimes. They leave. ...Sometimes.", 0)),
                new Follow("Can I stay a while?", c -> switch (EtoVoice.of(c)) {
                    case CLAIMED -> r("Quietly. And not near me.", 0);
                    case PARTNER, WIFE -> r("You don't have to ask. You never have to ask.", 3, EtoTalk.Effect.CLING, EtoGesture.CLING);
                    default -> r("Mind the shelves. And the red one— don't touch the red one.", 2);
                }),
                new Follow("What's on the red shelf?", c -> together(c) ? r("My diaries. The early ones. You are never, ever reading them.", 1, EtoGesture.POUT)
                    : r("Diaries. Not mine. Their owners don't need them anymore.", 0)))),

        new Topic("herself", c -> true, c -> "Tell me about yourself.",
            c -> EtoVoice.of(c).pick(c.seed(),
                s("I'm busy and I'm taken. That's about all that's relevant to you."),
                s("No. ...Fine. I like rain on glass, old paper, and the sound people make when they realise the twist was obvious all along."),
                null,
                s("I'm a girl who reads too much and eats people. Pretty ordinary, really."),
                s("You know most of it. The rest you'll find out slowly, and you'll love me anyway. That's the deal.")),
            List.of(
                new Follow("What do you hate?", c -> r("Cages. Shelves that are too high. People who fold the corners of pages. I can smell it, you know.", 1)),
                new Follow("Do you have family?", c -> switch (EtoVoice.of(c)) {
                    case PARTNER -> r("I have you. That's a family, isn't it? It's small. It's enough.", 3, EtoGesture.CLING);
                    case WIFE -> r("Yes. I married into one. It's you. It's a very small family. ...For now.", 3, EtoGesture.HAIR);
                    default -> r("Had. Have. Depends on the night. ...Next question.", 0);
                }),
                new Follow("What scares you?", c -> together(c)
                    ? r("Being forgotten. Being caged. Running out of things to read. ...And losing you. That one's new. I don't like it.", 2)
                    : r("Being forgotten. Being caged. Running out of things to read. In that order.", 1)))),

        new Topic("human", c -> !c.ghoul(), c -> "What's it like, being a ghoul?",
            c -> "Imagine everyone you meet is also lunch, and you have to be polite about it.",
            List.of(
                new Follow("Would you eat me?", c -> switch (EtoVoice.of(c)) {
                    case CLAIMED -> r("You're not my type. Or my diet.", 0);
                    case PARTNER -> r("Only a little. Somewhere nobody would see.", 1, EtoGesture.GIGGLE);
                    case WIFE -> r("I'd never. ...A nibble. Once. On our anniversary.", 2, EtoGesture.GIGGLE);
                    default -> r("Not today. You're more interesting walking around.", 0);
                }),
                new Follow("Do you hate humans?", c -> r("Hate? No. I hate rules. Humans just happen to have written most of them.", 1)),
                new Follow("Could I become one?", c -> r("Could you? Yes. Should you? Ask me to teach you, and I'll tell you the price.", 0)))),

        new Topic("ghoul", EtoTalk.Context::ghoul, c -> "How do you stay hidden?",
            c -> "Smile, keep a nice coat, never eat where you sleep. And own a library. Nobody suspects librarians.",
            List.of(
                new Follow("Do you ever feel guilty?", c -> r("Every meal. Then I'm hungry again. Guilt's a luxury for the full.", 1)),
                new Follow("Hunt with me sometime?", c -> switch (EtoVoice.of(c)) {
                    case CLAIMED -> r("I hunt with {partner}. Find your own.", 0);
                    case PARTNER, WIFE -> r("A date with blood in it? You romantic. Yes.", 3, EtoGesture.GIGGLE);
                    default -> r("With you? You'd scare off everything with all that crunching.", 0);
                }),
                new Follow("Teach me to be like you.", c -> r("Ask me for a lesson. Properly. I'm very particular about lessons.", 0)))),

        new Topic("day", c -> true, c -> "Let me tell you about my day.",
            c -> EtoVoice.of(c).pick(c.seed(),
                s("Is it about lessons? No? Then the short version."),
                s("Fine. Make it interesting."),
                null,
                s("Go on. Leave out the boring bits. ...So, all of it, probably."),
                s("Go on. Start from the moment you woke up and missed me."),
                s("Tell me everything. Especially the parts where you thought about me.")),
            List.of(
                new Follow("I fought something huge.", c -> together(c) ? r("And you came back to me in one piece. Good. Do that every time.", 2, EtoGesture.HUG)
                    : r("And came back with all your limbs? Show-off.", 1)),
                new Follow("It was boring, honestly.", c -> together(c) ? r("Then I'll make the rest of it interesting.", 1, EtoGesture.GIGGLE)
                    : r("Most days are. That's what books are for.", 0)),
                new Follow("I thought about you all day.", c -> switch (EtoVoice.of(c)) {
                    case CLAIMED -> r("That's {partner}'s job. Think about something else.", -3, EtoGesture.POUT);
                    case PARTNER -> r("...Good. Keep doing that.", 3, EtoGesture.HAIR);
                    case WIFE -> r("Only all day? Amateur.", 3, EtoGesture.GIGGLE);
                    default -> r("...Huh. Careful.", 2);
                }))),

        new Topic("feeling", c -> true, c -> "How are you feeling?",
            c -> switch (c.mood()) {
                case SULKY -> "Annoyed. At a specific person. Who is standing in front of me.";
                case JEALOUS -> "Fine. Totally fine. Ask me who I'm jealous of. I dare you.";
                case LONELY -> "Better, now. It was a quiet few days. Too quiet. I talked to a moth.";
                case CLINGY -> "Like I want to hold onto something and never let go. Coincidentally, you're right there.";
                case SLEEPY -> "Tired. The sun's coming. The sun always ruins everything.";
                case BLOODTHIRSTY -> "Hungry. It's a ghoul thing. Don't stand downwind.";
                case AFFECTIONATE -> "Warm. Restless. ...You could do something about that.";
                case GIDDY -> "Embarrassingly happy. It's disgusting. I love it.";
                case BORED -> "Bored out of my skull. Entertain me or get eaten. Kidding. Mostly.";
                case CLAIMED -> "Taken. Next question.";
                case WARY -> "Wary. Of you, mostly. Nothing personal.";
                default -> "Good. Quiet. The roof's not leaking. You're here. It's a good day.";
            },
            List.of(
                new Follow("I'm sorry.", c -> c.mood() == EtoMood.SULKY,
                    c -> r("...Say it like you mean it. ...Okay. That was close enough.", 5, EtoTalk.Effect.FORGIVE, EtoGesture.NONE)),
                new Follow("There's no one else.", c -> c.mood() == EtoMood.JEALOUS,
                    c -> r("...Promise? ...Okay. I believe you. Don't make me stop.", 5, EtoTalk.Effect.FORGIVE, EtoGesture.CLING)),
                new Follow("I'm here now.", c -> c.mood() == EtoMood.LONELY,
                    c -> r("You are. Stay a while, then. Make up for it.", 4, EtoTalk.Effect.CLING, EtoGesture.CLING)),
                new Follow("Then come to bed.", c -> c.mood() == EtoMood.AFFECTIONATE && together(c),
                    c -> r(EtoLines.nightAsk(c), 2, c.night() && !c.nightUsed() ? EtoTalk.Effect.NIGHT : EtoTalk.Effect.NONE, EtoGesture.NONE)),
                new Follow("Want to go hunting?", c -> c.mood() == EtoMood.BLOODTHIRSTY && !c.claimed(),
                    c -> r("With you? Yes. Bring a coat, it gets messy.", 2, EtoGesture.LICK)),
                new Follow("Good. I like you happy.", c -> c.mood() == EtoMood.HAPPY || c.mood() == EtoMood.GIDDY,
                    c -> r("Then keep coming back. It's a simple system.", 1, EtoGesture.HUM)),
                new Follow("What can I do?", c -> together(c) ? r("Stay. Be you. Annoy me lovingly.", 2, EtoGesture.CLING)
                    : r("Leave me a good book on your way out.", 0)))),

        new Topic("night", EtoTalk.Context::night, c -> "Can't sleep either?",
            c -> together(c) ? "Not without you, apparently. It's very inconvenient."
                : "I don't really sleep. I wait for the sun to give up. It always does.",
            List.of(
                new Follow("Want to watch the stars?", c -> c.claimed() ? r("Watch them with someone else.", 0)
                    : together(c) ? r("Lie down with me under the hole in the roof. Shut up and look up.", 3, EtoGesture.LEAN)
                    : r("There's a hole in the roof right over the poetry section. Best seat in the house.", 2)),
                new Follow("What do you do all night?", c -> r("Read. Write. Hunt. Wait for interesting people to wander in. Tonight I got one out of four.", 1)),
                new Follow("Come to bed, then.", EtoTopics::together,
                    c -> r(EtoLines.nightAsk(c), 2, !c.nightUsed() ? EtoTalk.Effect.NIGHT : EtoTalk.Effect.NONE, EtoGesture.NONE)))),

        new Topic("us", EtoTopics::together, c -> c.married() ? "Are you happy, married to me?" : "What are we, exactly?",
            c -> c.married() ? EtoLines.one(c.seed(), "Happy? I married you on purpose. I do very few things on purpose.",
                "Every morning I wake up and remember I'm married to you, and every morning it's a good morning.")
                : "We're a story I haven't decided the ending of. ...I'm leaning towards a happy one. Don't make me regret it.",
            List.of(
                new Follow("I love you.", c -> c.married() ? r("I know. I love you more. Don't argue, I've read more books.", 4, EtoGesture.HUG)
                    : r("...You can't just— ...I love you too. Idiot.", 5, EtoGesture.HAIR)),
                new Follow("Where is this going?", c -> !c.married(), c -> r("Somewhere with a ring, if you're smart about it.", 0, EtoGesture.GIGGLE)),
                new Follow("Are you seeing anyone else?", c -> !c.married(), c -> r("Who else would put up with me? Who else would I put up with?", 1)),
                new Follow("Tell me about our wedding.", EtoTalk.Context::married, c -> r("You were shaking. I pretended not to notice. I was shaking too.", 3,
                    EtoGesture.HAIR)),
                new Follow("Do you want a family someday?", c -> c.married() && !c.family().wantsBaby() && !c.pregnant(),
                    c -> r("...With you? Ask me again some night. Maybe I'll say yes.", 2, EtoGesture.HAIR)),
                // 1.3.10: asked for, the next night it comes.
                new Follow("Let's have a baby.", c -> c.married() && !c.family().wantsBaby() && !c.pregnant(),
                    c -> new EtoTalk.Reply(EtoLines.one(c.seed(), "...Yes. Tonight, then. Don't make me wait.",
                        "A baby. Ours. ...I've thought about it more than I'd admit. Yes. Come to me tonight.",
                        "You'd be a good father. Annoyingly good. ...Yes. Tonight."), 6, EtoTalk.Effect.BABY, EtoGesture.HAIR)),
                new Follow("About tonight...", c -> c.married() && c.family().wantsBaby() && !c.pregnant(),
                    c -> r(EtoLines.one(c.seed(), "I haven't forgotten. I've been thinking about nothing else. Wait for the dark.",
                        "Tonight. I said tonight. Don't look at me like that in front of the books."), 2, EtoGesture.GIGGLE)))),

        // 1.3.10: carrying their child.
        new Topic("baby", EtoTalk.Context::pregnant, c -> "How's the baby?",
            c -> switch (c.family().stage()) {
                case EARLY -> EtoLines.one(c.seed(), "Sick in the mornings. Starving at night. Don't make that face, you did this.",
                    "Fine. Strange. Like there's a secret in me that I haven't read yet.");
                case ALONG -> EtoLines.one(c.seed(), "Heavier. It moves when I read out loud. It likes the sad parts. Like its mother.",
                    "It kicks when you're near. Traitor. It already likes you better.");
                case LATE -> EtoLines.one(c.seed(), "Like I swallowed a moon. Sit with me. Don't talk. Just sit.",
                    "Tired. Huge. Happy. Don't tell anyone I said happy.");
                case DUE -> EtoLines.one(c.seed(), "Any day now. Any hour. Stay close. ...Please.", "Soon. I can feel it. Don't you dare wander off.");
            },
            List.of(
                new Follow("Can I feel the baby?", c -> c.family().stage() != com.hex.kagune.control.family.Pregnancy.Stage.EARLY,
                    c -> r(EtoLines.one(c.seed(), "Here. Your hand. ...There. Did you feel that? That's ours.",
                        "Gently. ...It's turning over. It knows your voice."), 5, EtoTalk.Effect.HUG, EtoGesture.HUG)),
                new Follow("Do you need anything?", c -> r(EtoLines.one(c.seed(), "Something sour. And something sweet. And you, not wandering off.",
                    "Pickles. Honey. A blanket. A foot rub. In that order.", "Quiet. A book. You, reading it to me."), 3, EtoGesture.HAIR)),
                new Follow("Boy or girl, do you think?", c -> r(EtoLines.one(c.seed(), "I'll tell you when it tells me. It's very secretive. Gets that from me.",
                    "A girl. Or a boy. I've decided not to have an opinion. ...A girl.", "Whatever it is, it's going to bite."), 2, EtoGesture.GIGGLE)),
                new Follow("Are you scared?", c -> r(EtoLines.one(c.seed(), "Of course. Of everything. ...Not of you, though. Never of you.",
                    "Terrified. Ask me again and I'll bite you."), 4, EtoTalk.Effect.CLING, EtoGesture.CLING)))),

        new Topic("past", c -> !c.claimed() && c.regard() >= NpcBonds.FRIENDLY, c -> "Where did you come from?",
            c -> together(c) ? "From somewhere dark. You don't need the map. You're the part after."
                : "A cage. A very kind one. Then I ate the lock.",
            List.of(
                new Follow("That sounds awful.", c -> r("It was. It made me. I'm not grateful. I'm just... here.", 1)),
                new Follow("You're safe now.", c -> together(c) ? r("...Am I? With you, I think I am. Weird feeling.", 4, EtoTalk.Effect.CLING, EtoGesture.CLING)
                    : r("Nobody's safe. But thank you.", 2)))),

        new Topic("rumours", c -> true, c -> "Heard any rumours?",
            c -> EtoLines.one(c.seed(), "They say there's a cafe in the city where ghouls drink coffee and pretend to be people. Cute. Delusional.",
                "Someone's been hunting ghouls with suitcases. Doves. If you ever see a white coat, run. Then come and tell me where it went.",
                "There's an owl in the stories ghouls whisper to each other. ...I wonder who that could be."),
            List.of(
                new Follow("Tell me about the cafe.", c -> r("Coffee and quiet and people trying very hard to be good. I went once. The coffee was wonderful. "
                    + "The people were better than me. I didn't go back.", 1)),
                new Follow("Who's the owl?", c -> r("Nobody you'd want to meet. ...Except you already have. Ahaha.", 0, EtoGesture.GIGGLE)),
                new Follow("Doves?", c -> r("Investigators. They hunt us with weapons made of us. Poetic. I'd write about it if it weren't so rude.", 0)))));

    /** The topics open to {@code c}. */
    public static List<Topic> open(EtoTalk.Context c) {
        List<Topic> out = new ArrayList<>();
        for (Topic topic : ALL) {
            if (topic.when().test(c)) {
                out.add(topic);
            }
        }
        return out;
    }

    /** 1.3.10: whether {@code topic} is always offered first (the baby she carries, their children). */
    public static boolean pinned(Topic topic) {
        return topic.id().equals("baby") || topic.id().equals("children");
    }

    public static Topic get(String id) {
        for (Topic topic : ALL) {
            if (topic.id().equals(id)) {
                return topic;
            }
        }
        return null;
    }

    public static int index(Topic topic) {
        return ALL.indexOf(topic);
    }
}
