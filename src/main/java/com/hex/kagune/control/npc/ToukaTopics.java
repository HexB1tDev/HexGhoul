package com.hex.kagune.control.npc;

import static com.hex.kagune.control.npc.CompanionScript.one;
import static com.hex.kagune.control.npc.CompanionScript.r;
import static com.hex.kagune.control.npc.CompanionScript.together;

import java.util.List;

/**
 * 4.2.7 (the owner: "So develop every single NPC even further, add more dialogue, more options, all of them"): Touka's further topics (pure,
 * tested), after her first ones ({@link ToukaScript}). Sharp-tongued, quick to kick, rather die than say she cares and then does anyway,
 * badly; what she only admits once is said once.
 */
final class ToukaTopics {
    private ToukaTopics() {
    }

    static final List<EtoTopics.Topic> MORE = List.of(
        new EtoTopics.Topic("exams", c -> !c.claimed(), c -> "How are your exams going?",
            c -> one(c.seed(), "Terrible. Chemistry. I can take a man's arm off and I can't balance an equation.",
                "Fine. ...They're not fine. Don't look at me like that. They're fine."),
            List.of(
                new EtoTopics.Follow("I'll help you study.", c -> r("...You'd do that? Fine. Tomorrow. Bring snacks. Don't make it weird.", 3, EtoGesture.HUFF)),
                new EtoTopics.Follow("You'll pass. You always do.", c -> r("I don't always. ...But thanks. Shut up. Thanks.", 2, EtoGesture.SHY)),
                new EtoTopics.Follow("Just quit school.", c -> r("Shut up. I'm not quitting. Some of us want more than this.", -3, EtoGesture.ANGRY)))),

        new EtoTopics.Topic("latte", c -> true, c -> "Can you do latte art?",
            c -> one(c.seed(), "A rabbit. Only a rabbit. Don't ask for anything else.", "I can do a rabbit. Ken can do a leaf. Mine's better."),
            List.of(
                new EtoTopics.Follow("Make me one.", c -> r("*a minute later she slides it over* ...Don't drink it right away. Look at it first. Okay. Now drink.",
                    2, EtoGesture.SERVE)),
                new EtoTopics.Follow("Ken's leaf is better.", c -> r("It is NOT. Get out. ...Drink your coffee first. THEN get out.", -1, EtoGesture.POUT)),
                new EtoTopics.Follow("That rabbit's a blob.", c -> r("It's a rabbit. It's a rabbit lying down. ...You're a blob.", -2, EtoGesture.HUFF)))),

        new EtoTopics.Topic("regulars", c -> c.regard() >= NpcBonds.FRIENDLY, c -> "Who are the regulars?",
            c -> one(c.seed(), "An old man who reads the same paper every day and never turns the page. A girl who flirts with Ken. Ken doesn't notice. "
                + "It's painful to watch.", "People who don't know what we are. It's nice. For an hour, nobody's afraid of anybody."),
            List.of(
                new EtoTopics.Follow("Sounds peaceful.", c -> r("It is. Don't ruin it. ...You can be a regular. If you tip.", 2, EtoGesture.WIPE)),
                new EtoTopics.Follow("I'll be one.", c -> r("You already are. You just didn't notice. ...Same seat every time. Idiot.", 2, EtoGesture.SHY)),
                new EtoTopics.Follow("They're all food to me.", c -> r("Then don't come in. Not here. Never here.", -4, EtoGesture.SCOLD)))),

        new EtoTopics.Topic("ayato_now", c -> c.regard() >= NpcBonds.FRIEND, c -> "Have you seen Ayato lately?",
            c -> "*She stops wiping the counter.* ...Who told you that name? ...No. I haven't. Not since he chose them.",
            List.of(
                EtoTopics.Follow.once("He misses you. He won't say it.", c -> r("...*she grips the cloth* That sounds like him. That sounds exactly like him. "
                    + "...Thanks.", 3, EtoGesture.LOOK)),
                new EtoTopics.Follow("Want me to talk to him?", c -> r("No. ...Maybe. If you see him, tell him- ...tell him to eat properly.", 2, EtoGesture.WIPE)),
                new EtoTopics.Follow("He chose them. Let him go.", c -> r("I KNOW where he is. I don't need you to tell me. Drop it.", -3, EtoGesture.ANGRY)))),

        new EtoTopics.Topic("hunger", EtoTalk.Context::ghoul, c -> "Don't you get hungry at work?",
            c -> one(c.seed(), "Every shift. The coffee smell helps. The customers smell better. I count the cups.",
                "Starving. Always. You learn to like it. You don't, actually, but you learn."),
            List.of(
                new EtoTopics.Follow("How do you hold back?", c -> r("The manager. He said: every cup you pour, you're choosing. Every cup. So I pour a lot of cups.",
                    2, EtoGesture.SIP)),
                new EtoTopics.Follow("I'll bring you something.", c -> r("...You don't have to. ...After closing. The back door. Don't let Ken see.", 2,
                    EtoGesture.SHY)),
                new EtoTopics.Follow("Just eat one.", c -> r("*her eye goes red* Say that in here again. I dare you.", -5, EtoGesture.ANGRY)))),

        new EtoTopics.Topic("kicks", c -> c.regard() >= NpcBonds.FRIENDLY, c -> "Teach me to kick like you.",
            c -> one(c.seed(), "Kicks aren't taught. You get angry enough and your leg figures it out.", "Stand like this. No. Like THIS. ...Hopeless."),
            List.of(
                new EtoTopics.Follow("I'm angry now.", c -> r("*laughs, actually laughs* No you're not. You look like a confused dog. ...Again tomorrow.", 1,
                    EtoGesture.LAUGH)),
                new EtoTopics.Follow("Show me one more time.", c -> r("*the kick stops a hair from your chin* Like that. Don't flinch. You flinched.", 2,
                    EtoGesture.KICKS)),
                new EtoTopics.Follow("You kick like a girl.", c -> r("I AM a girl. And you're on the floor. Think about that.", -3, EtoGesture.KICKS)))),

        new EtoTopics.Topic("lonely_rabbit", c -> c.regard() >= NpcBonds.FRIEND, c -> "Is it true rabbits die of loneliness?",
            c -> "*She goes still.* ...Who said that to you? It's a stupid myth. Rabbits are fine alone. ...I'm fine alone. ...Don't go anywhere, "
                + "okay? Not yet.",
            List.of(
                EtoTopics.Follow.once("I'm not going anywhere.", c -> r("...Okay. Good. *she won't look at you* Good. Shut up.", 4, EtoGesture.SHY)),
                EtoTopics.Follow.once("It's just a myth, like you said.", c -> r("...Right. Yeah. Just a myth. *she goes back to wiping the same spot*", -1,
                    EtoGesture.WIPE))),
            true),

        new EtoTopics.Topic("rabbit_mask", c -> true, c -> "What's your mask like?",
            c -> one(c.seed(), "A rabbit. Black. Under the counter. Don't touch it.", "Uta made it. It's a rabbit. It's SCARY. Stop smiling."),
            List.of(
                new EtoTopics.Follow("I bet it's terrifying.", c -> r("It is. ...Thank you. Finally, someone gets it.", 2, EtoGesture.CROSS)),
                new EtoTopics.Follow("Show me.", c -> r(together(c) ? "*she holds it up, just for a second, then shoves it back* ...Happy?" : "No.",
                    together(c) ? 2 : 0, EtoGesture.HUFF)),
                new EtoTopics.Follow("Rabbits are cute, though.", c -> r("It's NOT cute. It's a war mask. I'll kick you.", -2, EtoGesture.POUT)))),

        new EtoTopics.Topic("promise_touka", c -> together(c), c -> "Promise me something.",
            c -> one(c.seed(), "Ugh. What. ...Fine. What?", "*crosses her arms* One. Make it good."),
            List.of(
                EtoTopics.Follow.once("Let me walk you home after your shifts.", c -> r("...Every night? It's out of your way. ...Fine. Every night. Don't be late.",
                    4, EtoGesture.SHY)),
                EtoTopics.Follow.once("Pass your exams.", c -> r("Seriously? THAT'S your promise? ...Okay. For you. I'll pass them.", 3, EtoGesture.HUFF)),
                EtoTopics.Follow.once("Don't kick me too hard.", c -> r("*snorts* No promises. ...Medium hard. That's the best I can do.", 2, EtoGesture.LAUGH)))),

        new EtoTopics.Topic("first_touka", c -> together(c), c -> "When did you start liking me?",
            c -> one(c.seed(), "I didn't. ...You stayed after closing to help me mop. Nobody stays. Idiot.",
                "When you drank the bad coffee I made on purpose and said it was good. You're a terrible liar. It was nice."),
            List.of(
                new EtoTopics.Follow("I'll mop forever.", c -> r("...Don't say stupid things. *she's smiling into the counter* Idiot.", 3, EtoGesture.SHY)),
                new EtoTopics.Follow("The coffee really was good.", c -> r("Liar. ...Say it again.", 2, EtoGesture.HUFF)),
                new EtoTopics.Follow("I only came for the free coffee.", c -> r("...Wow. Okay. *she takes your cup away*", -3, EtoGesture.POUT)))),

        new EtoTopics.Topic("cute", c -> together(c), c -> "You're cute when you're angry.",
            c -> "I'm NOT cute. ...Shut up. I'm not angry either. ...I'm a little angry.",
            List.of(
                new EtoTopics.Follow("Very cute.", c -> r("*kicks your shin, very gently* ...Stop. ...Okay, don't stop.", 2, EtoGesture.BLUSH)),
                new EtoTopics.Follow("Okay, terrifying, then.", c -> r("Better. ...Still mostly cute, though? ...Never mind.", 1, EtoGesture.HUFF)),
                new EtoTopics.Follow("You're cuter when you're quiet.", c -> r("Then enjoy the quiet. I'm not talking to you for an hour.", -2, EtoGesture.POUT)))));
}
