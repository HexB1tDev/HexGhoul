package com.hex.kagune.control.npc;

import static com.hex.kagune.control.npc.CompanionScript.one;
import static com.hex.kagune.control.npc.CompanionScript.r;
import static com.hex.kagune.control.npc.CompanionScript.together;

import java.util.List;

/**
 * 4.2.6 (the owner: "So develop every single NPC even further, add more dialogue, more options, all of them"): the General's further topics
 * (pure, tested), after his first ones ({@link GeneralScript}). Clipped with a stranger, dry with a friend, red to the ears with the woman he
 * loves; what he has never told anyone is told once.
 */
final class GeneralTopics {
    private GeneralTopics() {
    }

    static final List<EtoTopics.Topic> MORE = List.of(
        new EtoTopics.Topic("reports", c -> true, c -> "What are you working on?",
            c -> one(c.seed(), "Incident reports. Fourteen of them. Twelve say the same thing in different handwriting.",
                "Budget. The bureau wants more doves and fewer bullets. I want both. Neither of us will get it.",
                "A letter to a widow. ...It doesn't matter how many you write. It doesn't get easier. Sit, if you're staying."),
            List.of(
                new EtoTopics.Follow("Can I help?", c -> r("...You can read the twelve and tell me which one is lying. Here.", 2, EtoGesture.READ)),
                new EtoTopics.Follow("You should rest.", c -> r(together(c) ? "...After this one. Then you can make me. You're the only one who can."
                    : "I'll rest when the ward does.", together(c) ? 3 : 1, EtoGesture.CHIN)),
                new EtoTopics.Follow("Sounds boring.", c -> r("It is. It's also how people come home. Close the door on your way out.", -2, EtoGesture.HUFF)))),

        new EtoTopics.Topic("partners", c -> c.regard() >= NpcBonds.FRIEND, c -> "You said you outlived three partners.",
            c -> "*He sets the pen down.* ...Hayashi, my first year. A Rinkaku in a stairwell; I was a floor too slow. Mori, twelve years in, laughed "
                + "at everything, even at the end. Takeda. ...Takeda was last spring. I still set out two cups some mornings.",
            List.of(
                EtoTopics.Follow.once("They'd be proud of you.", c -> r("...You never met them. ...But Mori would have said that. Thank you.", 4,
                    EtoGesture.LOOK)),
                EtoTopics.Follow.once("Do you blame yourself?", c -> r("Every day. It's the job. Someone has to remember them correctly.", 3,
                    EtoGesture.CROSS)),
                new EtoTopics.Follow("Partners can be replaced.", c -> r("*very quietly* Get out of my office.", -6, EtoGesture.ANGRY))),
            true),

        new EtoTopics.Topic("arima", c -> c.regard() >= NpcBonds.FRIENDLY, c -> "What do you make of Arima?",
            c -> one(c.seed(), "The finest investigator this bureau has ever had, and the loneliest man in it. Both are my fault, a little.",
                "Nobody trained Arima. People stood near him while he got better. I was one of them."),
            List.of(
                new EtoTopics.Follow("He scares me.", c -> r("Good. He scares me too. That's how you know he's on our side.", 1, EtoGesture.CHIN)),
                new EtoTopics.Follow("You're better than him.", c -> r("Flattery. ...Noted. Continue, if you like.", 2, EtoGesture.SHY)),
                new EtoTopics.Follow("He'd make a better general.", c -> r("He'd make a dead general inside a week. He can't sit through a meeting. "
                    + "...Was that meant to sting? It did.", -3, EtoGesture.SCOLD)))),

        new EtoTopics.Topic("rookie", c -> true, c -> "Any advice for a rookie?",
            c -> one(c.seed(), "Check your case twice. Never go into a building alone. Write to your mother. In that order.",
                "Eat breakfast. Know your exits. If something smiles at you in an alley, it isn't happy to see you."),
            List.of(
                new EtoTopics.Follow("Thank you, sir.", c -> r("Don't thank me. Survive. That's how you thank me.", 2, EtoGesture.BOW)),
                EtoTopics.Follow.once("Did you write to yours?", c -> r("Every Sunday, until she died. ...Write to yours. Today.", 3, EtoGesture.LOOK)),
                new EtoTopics.Follow("I don't need advice.", c -> r("Everyone I buried said that. Dismissed.", -2, EtoGesture.HUFF)))),

        new EtoTopics.Topic("tower", c -> true, c -> "What's it like, running the tower?",
            c -> one(c.seed(), "The lift breaks every Tuesday. The coffee is a crime. Forty-eight of the best people I know work in it. I'd keep all three.",
                "Like holding a door shut with your back while you do your taxes."),
            List.of(
                new EtoTopics.Follow("The coffee really is awful.", c -> r("It is. It has been for thirty years. It is the one thing this bureau agrees on.",
                    1, EtoGesture.SIP)),
                new EtoTopics.Follow("It's a fortress.", c -> r("It's meant to be. Everyone inside it goes home. That's the whole design.", 2, EtoGesture.CROSS)),
                new EtoTopics.Follow("I could walk right in.", c -> r("*he looks at you over his glasses* Try it.", -3, EtoGesture.SCOLD)))),

        new EtoTopics.Topic("spared", c -> c.regard() >= NpcBonds.FRIEND, c -> "Have you ever let a ghoul go?",
            c -> "*He's quiet for a long time.* ...Once. A child, maybe eight, in a flat in the twentieth ward. Her parents were already gone. She "
                + "held up her hands like I was the monster. ...I wrote the room up as empty. I have never told a soul that.",
            List.of(
                EtoTopics.Follow.once("That was the right thing.", c -> r("...I've wondered for twenty years. Hearing someone say it helps. More than it "
                    + "should.", 4, EtoGesture.LOOK)),
                EtoTopics.Follow.once("Your secret's safe.", c -> r("I know. I wouldn't have said it otherwise.", 3, EtoGesture.CHIN)),
                new EtoTopics.Follow("You broke the rules.", c -> r("I did. I'd do it again. Don't make me regret telling you.", -2, EtoGesture.CROSS))),
            true),

        new EtoTopics.Topic("legs", c -> c.regard() >= NpcBonds.FRIENDLY, c -> "Who taught you to fight with your legs?",
            c -> one(c.seed(), "A drill sergeant who'd lost both arms to a Bikaku. He said: legs are what you have left when everything else is gone. "
                + "He wasn't wrong.", "Nobody. My first quinque broke on my first night. I kicked my way home. It became a habit."),
            List.of(
                new EtoTopics.Follow("Show me.", c -> r("*the heel stops a finger's width from your ear; he never seemed to move* ...Like that. Again "
                    + "tomorrow, if you want.", 2, EtoGesture.KICKS)),
                new EtoTopics.Follow("That's incredible.", c -> r("It's practice. Thirty years of it. ...Thank you.", 2, EtoGesture.SHY)),
                new EtoTopics.Follow("Legs are for running.", c -> r("Then run. I'll catch up. I always do.", -2, EtoGesture.HUFF)))),

        new EtoTopics.Topic("retire", c -> c.regard() >= NpcBonds.FRIEND, c -> "Will you ever retire?",
            c -> one(c.seed(), "When the ghouls retire. I've sent them the paperwork. They haven't replied.",
                "They'll carry me out of the office. Feet first. I've told them which door."),
            List.of(
                new EtoTopics.Follow("Retire with me.", c -> together(c), c -> r("*he goes red to the collar* ...Don't. Don't tempt an old man. ...A house "
                    + "by the sea? ...Don't.", 4, EtoGesture.BLUSH)),
                new EtoTopics.Follow("The bureau still needs you.", c -> r("It does. Somebody has to sign things nobody else will.", 2, EtoGesture.CHIN)),
                new EtoTopics.Follow("You're getting too old for this.", c -> r("And yet I'm still faster than you. Think on that.", -3, EtoGesture.SCOLD)))),

        new EtoTopics.Topic("trust", c -> c.ghoul() && !together(c) && c.regard() >= NpcBonds.FRIENDLY,
            c -> "What would it take for you to trust a ghoul?",
            c -> one(c.seed(), "Time. Proof. ...You, coming back. Again and again, with nothing on your hands.",
                "I've spent thirty years being right about ghouls. It's hard to want to be wrong. ...You make it easier."),
            List.of(
                new EtoTopics.Follow("Then I'll keep coming back.", c -> r("...See that you do. I'll be here. Watching. Hoping.", 3, EtoGesture.LOOK)),
                EtoTopics.Follow.once("I trust you.", c -> r("*he looks at you a long moment* ...That's a heavy thing to hand a man like me. I'll be careful "
                    + "with it.", 3, EtoGesture.CHIN)),
                new EtoTopics.Follow("You'll never trust me.", c -> r("Not with that attitude.", -2, EtoGesture.HUFF)))),

        new EtoTopics.Topic("dance", c -> together(c), c -> "Do you dance?",
            c -> one(c.seed(), "...Badly. At the bureau's gala I stepped on the Chairman. Twice. He still mentions it.",
                "I was taught. In a gym. By a woman who threatened to report me. I passed."),
            List.of(
                new EtoTopics.Follow("Dance with me.", c -> r("Here? Now? ...Lock the door. *he's stiff for three steps, and then he isn't*", 3, EtoGesture.DANCE)),
                new EtoTopics.Follow("I bet you're terrible.", c -> r("Correct, and you'll never find out. ...Hm.", -1, EtoGesture.HUFF)))),

        new EtoTopics.Topic("promise", c -> together(c), c -> "Promise me something.",
            c -> one(c.seed(), "*straightens his coat* I don't promise lightly. Go on.", "A general's word is binding. ...Be careful what you ask for."),
            List.of(
                EtoTopics.Follow.once("Come home every night.", c -> r("Every night I can. ...And on the nights I can't, I'll send word. I swear it.", 4,
                    EtoGesture.HUG)),
                EtoTopics.Follow.once("Never lie to me.", c -> r("I never have. I won't start. Even when the truth is a report I'd rather burn.", 4,
                    EtoGesture.CHIN)),
                EtoTopics.Follow.once("Let me fight beside you.", c -> r(c.ghoul() ? "A ghoul at a general's side. ...They'll talk. Let them. There's "
                    + "nobody I'd sooner have there." : "...Yes. God help the ghoul who sees the two of us coming.", 3, EtoGesture.CROSS)))),

        new EtoTopics.Topic("first", c -> together(c), c -> "When did you fall for me?",
            c -> one(c.seed(), "When you argued with me in my own office and were right. Nobody argues with me. Nobody is right.",
                "When I caught myself checking the incident reports for your name. Not because I wanted to find it. Because I didn't."),
            List.of(
                new EtoTopics.Follow("I'll keep arguing.", c -> r("I'm counting on it. ...Don't tell the bureau.", 3, EtoGesture.SHY)),
                new EtoTopics.Follow("You checked the reports for me?", c -> r("Every morning. ...That was not meant to come out. *he picks up a report "
                    + "and reads it upside down*", 2, EtoGesture.BLUSH)),
                new EtoTopics.Follow("I haven't fallen for you.", c -> r("...I see. *very still* Then I've been a fool. I'm told it happens.", -4,
                    EtoGesture.FLINCH)))),

        new EtoTopics.Topic("glasses", c -> c.regard() >= NpcBonds.FRIENDLY, c -> "Do you need those glasses?",
            c -> one(c.seed(), "Only to read. And to look at people over the top of them. That one's the important part.",
                "I've had them since I was forty. The bureau's optometrist says I'm stubborn. I've had him reassigned."),
            List.of(
                new EtoTopics.Follow("They make you look distinguished.", c -> r("...Do they. Hm. *he does not take them off for the rest of the day*", 2,
                    EtoGesture.SHY)),
                new EtoTopics.Follow("Do the look on me.", c -> r("*he lowers his chin and looks at you over the frames, very slowly* ...Satisfied?", 1,
                    EtoGesture.CHIN)),
                new EtoTopics.Follow("They make you look old.", c -> r("I am old. I'm also armed. Was there anything else?", -2, EtoGesture.SCOLD)))));
}
