package com.hex.kagune.control.npc;

import static com.hex.kagune.control.npc.CompanionScript.one;
import static com.hex.kagune.control.npc.CompanionScript.r;
import static com.hex.kagune.control.npc.CompanionScript.together;

import java.util.List;

/**
 * 4.2.7 (the owner: "So develop every single NPC even further, add more dialogue, more options, all of them"): Eto's further topics (pure,
 * tested), after her first ones ({@link EtoTopics}). The novelist in the Library: playful, a little cruel, always one page ahead of you; what
 * she only admits once is said once.
 */
final class EtoMoreTopics {
    private EtoMoreTopics() {
    }

    static final List<EtoTopics.Topic> MORE = List.of(
        new EtoTopics.Topic("favourite_book", c -> true, c -> "What's your favourite book?",
            c -> one(c.seed(), "My own, obviously. ...No. 'The Black Goat's Egg'. ...I wrote that one too. Ahaha.",
                "The one nobody's written yet. I keep starting it. Everybody in it dies on page one. I'm working on that."),
            List.of(
                new EtoTopics.Follow("I loved it.", c -> r("Did you? Which chapter made you cry? ...Wrong answer. Chapter nine. Read it again.", 2, EtoGesture.GIGGLE)),
                new EtoTopics.Follow("Recommend me something.", c -> r("Something with a bad ending. Good endings are lies people tell children.", 1,
                    EtoGesture.READ)),
                new EtoTopics.Follow("Never heard of it.", c -> r("...Never heard of it. *her smile doesn't move at all* How refreshing.", -2, EtoGesture.POUT)))),

        new EtoTopics.Topic("owl", c -> c.regard() >= NpcBonds.FRIEND, c -> "Are you the owl ghouls whisper about?",
            c -> "*She closes one eye and smiles with the other.* ...What do you think?",
            List.of(
                EtoTopics.Follow.once("I think you are.", c -> r("Clever. Clever's a dangerous thing to be around me. Keep it anyway.", 3, EtoGesture.HAIR)),
                EtoTopics.Follow.once("I don't care what you are.", c -> r("*for a second she looks surprised, really surprised* ...Ahaha. Nobody's ever "
                    + "said that and meant it. You meant it.", 4, EtoGesture.SHY)),
                EtoTopics.Follow.once("I'll tell the doves.", c -> r("Ahaha... no. No, you won't. *it isn't a question*", -6, EtoGesture.ANGRY))),
            true),

        new EtoTopics.Topic("fans", c -> true, c -> "Do you get fan mail?",
            c -> one(c.seed(), "Sacks of it. I answer all of it. In invisible ink. They think I'm shy.", "Mostly from people who'd like me to stop "
                + "killing their favourite characters. I keep a list. Of the letters. ...And the characters."),
            List.of(
                new EtoTopics.Follow("I'd write to you.", c -> r("Would you? Write something terrible. I like terrible. It's honest.", 2, EtoGesture.WRITE)),
                new EtoTopics.Follow("Do you read them all?", c -> r("Every one. Some of them are better than my books. Don't tell my editor.", 1,
                    EtoGesture.READ)),
                new EtoTopics.Follow("Your fans sound weird.", c -> r("My fans are lovely. They have excellent taste in tragedy. ...You're not one, are you.",
                    -1, EtoGesture.TILT)))),

        new EtoTopics.Topic("endings", c -> c.regard() >= NpcBonds.FRIENDLY, c -> "Why do your stories end so badly?",
            c -> one(c.seed(), "Because they do. Every story ends with somebody dead. I'm just honest about the order.",
                "A happy ending is a story that stopped too early. I don't stop early."),
            List.of(
                new EtoTopics.Follow("Write a happy one. For me.", c -> r("For you? ...I'll think about it. It would be a very short book.", 2, EtoGesture.GIGGLE)),
                new EtoTopics.Follow("I like sad endings.", c -> r("Of course you do. You've got that look. The one that's lost something.", 1, EtoGesture.LOOK)),
                new EtoTopics.Follow("That's just depressing.", c -> r("So is life. I just charge for it.", -2, EtoGesture.SHRUG)))),

        new EtoTopics.Topic("bandages", c -> c.regard() >= NpcBonds.FRIENDLY, c -> "Why the bandages?",
            c -> one(c.seed(), "So nobody sees what's underneath. Isn't that why anybody covers anything?", "Fashion. ...Ahaha. No. But it's a good "
                + "answer, isn't it? Use it, if anyone asks you about yours."),
            List.of(
                new EtoTopics.Follow("Can I see?", c -> r(together(c) ? "*she unwinds one, slowly, watching your face the whole time* ...Still here? Good."
                    : "Ahaha. No. Ask me again when you're braver.", together(c) ? 3 : -1, EtoGesture.HAIR)),
                new EtoTopics.Follow("They suit you.", c -> r("They suit everybody. That's the trick.", 1, EtoGesture.GIGGLE)),
                new EtoTopics.Follow("You look like a mummy.", c -> r("And you look like dinner. We all have our burdens.", -2, EtoGesture.LICK)))),

        new EtoTopics.Topic("aogiri_eto", c -> c.regard() >= NpcBonds.FRIEND, c -> "What are the masked ones to you?",
            c -> one(c.seed(), "A toy box. Full of very angry toys. I wind them up and watch which way they walk.",
                "A family, of a kind. The kind that would eat each other if I left the room. So I don't leave the room."),
            List.of(
                new EtoTopics.Follow("You're dangerous.", c -> r("Ahaha. Finally. Someone noticed. *pats your cheek* Good.", 1, EtoGesture.PET)),
                new EtoTopics.Follow("Let me help you.", c -> r("Help me? With what, exactly? ...Oh, you don't know. That's sweet. Stay sweet.", 2,
                    EtoGesture.TILT)),
                new EtoTopics.Follow("That's cruel.", c -> r("It is. And you're still sitting here. What does that make you?", -2, EtoGesture.SHRUG)))),

        new EtoTopics.Topic("editor", c -> true, c -> "Who's your editor?",
            c -> one(c.seed(), "A poor woman who thinks I'm a sweet shut-in with deadline trouble. I'm two of those three.",
                "She sends me fruit when I'm late. I'm always late. I have so much fruit."),
            List.of(
                new EtoTopics.Follow("She'd faint if she knew.", c -> r("She would! Right onto the manuscript. ...I'd use it. Ahaha.", 2, EtoGesture.GIGGLE)),
                new EtoTopics.Follow("Meet your deadlines.", c -> r("Deadlines are for people who'll be alive next week. ...Fine. Fine. Nag.", -1, EtoGesture.POUT)))),

        new EtoTopics.Topic("lonely", c -> c.regard() >= NpcBonds.FRIEND && c.night(), c -> "Do you ever get lonely?",
            c -> "*She's quiet for a bit, which isn't like her.* ...I live in a building full of other people's words. What do you think?",
            List.of(
                EtoTopics.Follow.once("Not anymore.", c -> r("...*she doesn't make a joke* No. Not anymore. Don't make me take that back.", 4, EtoGesture.SHY)),
                new EtoTopics.Follow("Everyone's lonely.", c -> r("Hm. Yes. That's the first line of half my books.", 0, EtoGesture.LOOK)),
                new EtoTopics.Follow("You chose this.", c -> r("Did I? *very lightly* How would you know what I chose?", -2, EtoGesture.TILT)))),

        new EtoTopics.Topic("like_me", c -> together(c), c -> "What do you like about me?",
            c -> one(c.seed(), "You'd make a terrible character. Too kind. No twist. ...I like that. I like that a great deal.",
                "You come back. Nobody comes back to the Library twice. They come back to the books. You come back to me."),
            List.of(
                new EtoTopics.Follow("That's sweet.", c -> r("Don't tell anyone I said it. I have a reputation.", 3, EtoGesture.SHY)),
                new EtoTopics.Follow("I could have a twist.", c -> r("Ooh. Do you? *leans in* ...Tell me later. Make me wait.", 1, EtoGesture.HAIR)),
                new EtoTopics.Follow("I don't believe you.", c -> r("...Well. That's your problem, isn't it.", -2, EtoGesture.POUT)))),

        new EtoTopics.Topic("promise_eto", c -> together(c), c -> "Promise me something.",
            c -> one(c.seed(), "A promise? From me? ...Ahaha. Careful. I keep the ones I make.", "*puts down her pen* All right. One. Choose well."),
            List.of(
                EtoTopics.Follow.once("Don't put me in a tragedy.", c -> r("...Too late. But I'll write you a sequel. One where you live.", 4, EtoGesture.HUG)),
                EtoTopics.Follow.once("Always tell me the truth.", c -> r("The truth is a lot, with me. ...All right. You asked. I'll tell you everything. "
                    + "Slowly.", 4, EtoGesture.LOOK)),
                EtoTopics.Follow.once("Never leave without telling me.", c -> r("Never? ...Never. A note on the pillow, at the very least. In blood, if "
                    + "I'm in a hurry. Ahaha. Ink. Ink.", 3, EtoGesture.GIGGLE)))),

        new EtoTopics.Topic("knew", c -> together(c), c -> "When did you know?",
            c -> one(c.seed(), "When you came back to the Library without being dared to. Everybody's dared. You just came.",
                "When I wrote your name in a margin and didn't cross it out."),
            List.of(
                new EtoTopics.Follow("I'd come back a hundred times.", c -> r("Count them. I will. I already am.", 3, EtoGesture.SHY)),
                new EtoTopics.Follow("Someone did dare me.", c -> r("...Ahaha. Hah. Funny. *she writes something down and doesn't show you*", -3,
                    EtoGesture.WRITE)))));
}
