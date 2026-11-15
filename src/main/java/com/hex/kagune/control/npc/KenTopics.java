package com.hex.kagune.control.npc;

import static com.hex.kagune.control.npc.CompanionScript.one;
import static com.hex.kagune.control.npc.CompanionScript.r;
import static com.hex.kagune.control.npc.CompanionScript.together;

import java.util.List;

/**
 * 4.2.7 (the owner: "So develop every single NPC even further, add more dialogue, more options, all of them"): Ken's further topics (pure,
 * tested), after his first ones ({@link KenScript}). Polite, gentle, a little sad; calm in a way that frightens people when he talks about pain;
 * what he only tells once is told once.
 */
final class KenTopics {
    private KenTopics() {
    }

    static final List<EtoTopics.Topic> MORE = List.of(
        new EtoTopics.Topic("author", c -> true, c -> "Who's your favourite author?",
            c -> one(c.seed(), "There's one novelist I've read everything by. Twice. Her endings hurt, in a way that feels honest.",
                "A novelist nobody's ever seen. No photographs, no signings. I like that. The words are all there is of her."),
            List.of(
                new EtoTopics.Follow("I've read her too.", c -> r("Really? Which one? ...We have to talk about the ending. I've been waiting for someone to talk "
                    + "about the ending.", 2, EtoGesture.READ)),
                new EtoTopics.Follow("What if you met her?", c -> r("I'd say something stupid. Probably about her commas. ...I'd like to, though.", 1, EtoGesture.LOOK)),
                new EtoTopics.Follow("Sounds pretentious.", c -> r("...Oh. That's alright. Not everyone likes her. *he closes the book*", -2, EtoGesture.FLINCH)))),

        new EtoTopics.Topic("eyepatch", c -> c.regard() >= NpcBonds.FRIENDLY, c -> "Why the eyepatch?",
            c -> one(c.seed(), "It covers the eye that isn't mine. Or that is, now. It's complicated. People stare less.",
                "So the customers don't see the red. ...So I don't have to see them see it."),
            List.of(
                new EtoTopics.Follow("You don't have to hide it from me.", c -> r("...Thank you. I'll try to remember that. It's an old habit.", 3, EtoGesture.LOOK)),
                new EtoTopics.Follow("It looks good on you.", c -> r("Does it? I never thought about it like that. ...Thank you.", 1, EtoGesture.SHY)),
                new EtoTopics.Follow("It's creepy.", c -> r("...Yes. I know. That's why I wear it. *a finger cracks under his thumb*", -2, EtoGesture.CRACK)))),

        new EtoTopics.Topic("hair", c -> c.regard() >= NpcBonds.FRIEND, c -> "What happened to your hair?",
            c -> "*His thumb finds a finger.* ...Ten days. I'll only say that much. It went white on the seventh. I remember the moment. I was "
                + "counting.",
            List.of(
                EtoTopics.Follow.once("You survived.", c -> r("I did. Some of me. ...The parts that came out are mine. I've decided that.", 3, EtoGesture.LOOK)),
                EtoTopics.Follow.once("I'm here now.", c -> r("*he stops cracking his fingers* ...Yes. You are. That helps. I didn't expect it to help.", 4,
                    EtoGesture.SHY)),
                new EtoTopics.Follow("You should dye it.", c -> r("I've thought about it. It doesn't feel right to hide it. ...Never mind.", -3, EtoGesture.CRACK))),
            true),

        new EtoTopics.Topic("mask_ken", c -> true, c -> "Who made your mask?",
            c -> one(c.seed(), "Uta, in the fourth ward. He makes masks for ghouls. Mine has a smile full of teeth. I don't smile like that.",
                "A friend who makes masks. He said mine should show what I'd rather hide. It's mostly teeth."),
            List.of(
                new EtoTopics.Follow("Can I get one?", c -> r("I'll introduce you. Don't let him measure your face for too long. He gets ideas.", 1, EtoGesture.LOOK)),
                new EtoTopics.Follow("It suits the fighter in you.", c -> r("...I'd rather it didn't. But maybe you're right.", 1, EtoGesture.CRACK)),
                new EtoTopics.Follow("It's hideous.", c -> r("Ah. Well. That was rather the point. *he smiles politely and doesn't mean it*", -2, EtoGesture.SHRUG)))),

        new EtoTopics.Topic("anteiku_ken", c -> true, c -> "Do you like working at Anteiku?",
            c -> one(c.seed(), "Very much. The manager taught me to brew. Touka taught me to be shouted at. Both have been useful.",
                "It's the first place I felt I could stop pretending. Well. Pretend less. Coffee helps."),
            List.of(
                new EtoTopics.Follow("You make the best coffee in the city.", c -> r("*goes a little pink* ...That's very kind. I'll make you another. On the house.",
                    3, EtoGesture.SERVE)),
                new EtoTopics.Follow("Touka's scary.", c -> r("She is. She's also the kindest person here. Don't tell her I said that.", 1, EtoGesture.LAUGH)),
                new EtoTopics.Follow("I prefer tea.", c -> r("Oh. That's... fine. That's quite fine. *he looks very slightly heartbroken*", -1, EtoGesture.SHRUG)))),

        new EtoTopics.Topic("pain", c -> c.regard() >= NpcBonds.FRIEND, c -> "Does it still hurt?",
            c -> one(c.seed(), "Everything heals. That's the problem. Nothing stays, not even the hurt. You'd think that would be a mercy.",
                "Not the body. The body forgets. ...I don't."),
            List.of(
                EtoTopics.Follow.once("Then I'll remember it with you.", c -> r("*very quiet* ...Nobody's ever offered to carry any of it. Thank you. I mean it.",
                    4, EtoGesture.LOOK)),
                new EtoTopics.Follow("At least you heal.", c -> r("Yes. At least that. *he says it kindly, but he doesn't agree*", 0, EtoGesture.CRACK)),
                new EtoTopics.Follow("Get over it.", c -> r("...I'll try. *he smiles, and it's the most frightening thing in the room*", -5, EtoGesture.CRACK)))),

        new EtoTopics.Topic("polite", c -> c.regard() >= NpcBonds.FRIENDLY, c -> "Why are you always so polite?",
            c -> one(c.seed(), "My mother. She worked herself to death being kind. I think I'm still trying to make it worth it.",
                "Because the other way is very easy, for me. Easier than you'd like. Manners are a fence."),
            List.of(
                new EtoTopics.Follow("It is worth it.", c -> r("...Thank you. I hope so. I hope she'd think so.", 3, EtoGesture.BOW)),
                new EtoTopics.Follow("Kindness gets people killed.", c -> r("It does. I'd still rather be kind and dead than the other thing.", -2, EtoGesture.LOOK)))),

        new EtoTopics.Topic("dark_ken", EtoTalk.Context::night, c -> "Can't sleep?",
            c -> one(c.seed(), "Not without a light on. ...Sorry. That's embarrassing to admit.", "I read until the words stop making sense. Then a little longer."),
            List.of(
                new EtoTopics.Follow("I'll stay until you fall asleep.", c -> together(c), c -> r("...Would you? I'd like that. I'd like that very much.", 3,
                    EtoGesture.SHY)),
                new EtoTopics.Follow("Me neither. Read to me?", c -> r("Of course. *he picks a book, and his voice goes soft* ...Chapter one.", 2, EtoGesture.READ)),
                new EtoTopics.Follow("Grow up.", c -> r("...I'll turn it off, then. *he doesn't*", -3, EtoGesture.FLINCH)))),

        new EtoTopics.Topic("promise_ken", c -> together(c), c -> "Promise me something.",
            c -> one(c.seed(), "Of course. Anything I can keep. I don't make the other kind.", "*he sets the book down, marking the page* I'm listening."),
            List.of(
                EtoTopics.Follow.once("Don't carry everything alone.", c -> r("...That's the hardest one you could have asked. I'll try. For you, I'll try.", 4,
                    EtoGesture.HUG)),
                EtoTopics.Follow.once("Come back to me. Every time.", c -> r("Every time. Whatever's left of me. I promise.", 4, EtoGesture.LOOK)),
                EtoTopics.Follow.once("Read to me every night.", c -> r("Every night. I'll start with something with a happy ending. ...I'll have to find one.",
                    3, EtoGesture.READ)))),

        new EtoTopics.Topic("knew_ken", c -> together(c), c -> "When did you know?",
            c -> one(c.seed(), "When you laughed at something I said. I'm not funny. You laughed anyway.",
                "When I saved you the last of the good beans and didn't even think about it."),
            List.of(
                new EtoTopics.Follow("You are funny.", c -> r("I'm really not. ...But thank you for thinking so. Keep thinking so.", 3, EtoGesture.SHY)),
                new EtoTopics.Follow("I was laughing at you.", c -> r("...Ah. *he laughs too, a beat late* Of course. Of course you were.", -3, EtoGesture.FLINCH)))));
}
