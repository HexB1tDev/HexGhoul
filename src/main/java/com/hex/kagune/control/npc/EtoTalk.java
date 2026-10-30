package com.hex.kagune.control.npc;

import java.util.ArrayList;
import java.util.List;

/**
 * 1.3.0: talking with Eto (pure, tested), on the ghoul NPCs' talk screen ({@link NpcTalk.Choice}, the new ones at its end). She talks
 * to anyone, ghoul or human, who has not made an enemy of her: chats (once a day), takes a book or a flower (one gift a day raises her
 * regard; the ones her errands ask for always count), teaches a ghoul her three lessons ({@link EtoRules.Lesson}), at her highest regard
 * sends them on her errands ({@link EtoRules.Step}), and once they are dating: kisses, hugs, and the surgery that makes them like her.
 * Every answer is a {@link Reply}: her words, what it does to her regard, what the server then does ({@link Effect}).
 */
public final class EtoTalk {
    /** What the server does after an answer. */
    public enum Effect {
        NONE, CLOSE, BOOK, FLOWER, TEACH, BEGIN, DATE, KISS, HUG, OFFER, SURGERY, ANGER
    }

    /**
     * Everything a page depends on: her regard for them and her bond's flags ({@code bond}: the step and count of her errands are read
     * from it), a ghoul or not, chatted and gifted today, a book or a flower in hand, the lesson she would teach next and whether the one
     * before it is practised (its quest done), whether she has already made them like her, a seed for her lines.
     */
    public record Context(int regard, int flags, int count, boolean ghoul, boolean chattedToday, boolean giftedToday, boolean book, boolean flower,
                          EtoRules.Lesson next, boolean beforeDone, boolean granted, int seed) {
        boolean has(int flag) {
            return (this.flags & flag) != 0;
        }

        EtoRules.Step step() {
            return EtoRules.step(new com.hex.kagune.control.quest.QuestLog.Bond(this.regard, 0L, this.flags, this.count));
        }

        boolean dating() {
            return this.has(EtoRules.DATING);
        }
    }

    /** What an answer brings: her words, the change in her regard, what happens. */
    public record Reply(String line, int regard, Effect effect) {
    }

    private EtoTalk() {
    }

    /** True if she talks to them at all (an enemy of hers is fought, not talked to). */
    public static boolean talks(int regard) {
        return regard > NpcBonds.ENEMY;
    }

    /** The answers she allows now. */
    public static List<NpcTalk.Choice> choices(Context c) {
        List<NpcTalk.Choice> out = new ArrayList<>();
        out.add(NpcTalk.Choice.WHO);
        out.add(NpcTalk.Choice.CHAT);
        if (c.book()) {
            out.add(NpcTalk.Choice.BOOK);
        }
        if (c.flower()) {
            out.add(NpcTalk.Choice.FLOWER);
        }
        if (c.ghoul() && c.next() != null) {
            out.add(NpcTalk.Choice.LESSON);
        }
        EtoRules.Step step = c.step();
        if (step != EtoRules.Step.NONE && step != EtoRules.Step.DATING || step == EtoRules.Step.NONE && c.regard() >= NpcBonds.MOST) {
            out.add(NpcTalk.Choice.FAVOUR);
        }
        if (step == EtoRules.Step.ASK) {
            out.add(NpcTalk.Choice.ASK_OUT);
        }
        if (c.dating()) {
            out.add(NpcTalk.Choice.KISS);
            out.add(NpcTalk.Choice.HUG);
            if (!c.granted()) {
                out.add(NpcTalk.Choice.SURGERY);
            }
        } else {
            out.add(NpcTalk.Choice.INSULT);
        }
        out.add(NpcTalk.Choice.LEAVE);
        return out;
    }

    /** The answers to her offer of the surgery. */
    public static List<NpcTalk.Choice> offer() {
        return List.of(NpcTalk.Choice.ACCEPT, NpcTalk.Choice.DECLINE);
    }

    /** Her first words. */
    public static String greeting(Context c) {
        NpcBonds.Band band = NpcBonds.band(c.regard());
        if (band == NpcBonds.Band.ENEMY) {
            return "You. You've got some nerve, walking into my library.";
        }
        if (band == NpcBonds.Band.COLD) {
            return "Oh. It's you. Mind the shelves. And your manners, this time.";
        }
        if (c.dating()) {
            return pick(new String[] {"There you are. I was starting to think you'd found another library.",
                "Hi, you. Sit, I saved you the good chair. It's the one without the bloodstain."}, c.seed());
        }
        if (c.regard() >= NpcBonds.MOST) {
            return "My favourite visitor! I was just thinking about you. ...Don't let it go to your head.";
        }
        if (band == NpcBonds.Band.FRIEND) {
            return "Back again? Careful, I'll start thinking you come for me and not the books.";
        }
        if (band == NpcBonds.Band.FRIENDLY) {
            return "Oh, hello again! Don't mind the mess. The books only bite a little.";
        }
        return c.ghoul() ? "Hm? You smell like one of us. Relax, it's a library, not a hunting ground. Mostly."
            : "A human, in my library, at this hour? Bold. I like bold. Don't touch the red shelf.";
    }

    /** What she says back to {@code choice}. */
    public static Reply answer(Context c, NpcTalk.Choice choice) {
        return switch (choice) {
            case WHO -> new Reply("Eto. I live here, with the books. I write a little too, under another name. Don't ask which, you'd only want an "
                + "autograph.", c.has(NpcBonds.ASKED) ? 0 : EtoRules.WHO, Effect.NONE);
            case CHAT -> c.chattedToday() ? new Reply("We've talked enough for one day. Come back tomorrow, I'll be more interesting.", 0, Effect.NONE)
                : new Reply(pick(chat(c), c.seed() + c.regard()), EtoRules.CHAT, Effect.NONE);
            case BOOK -> book(c);
            case FLOWER -> flower(c);
            case LESSON -> lesson(c);
            case FAVOUR -> favour(c);
            case ASK_OUT -> c.step() == EtoRules.Step.ASK ? new Reply("...Took you long enough. Yes. Obviously, yes.", 0, Effect.DATE)
                : new Reply("Ask me what, exactly?", 0, Effect.NONE);
            case KISS -> c.dating() ? new Reply("Mm. That'll do. For now.", 0, Effect.KISS) : new Reply("Bold. Buy me a book first.", 0, Effect.NONE);
            case HUG -> c.dating() ? new Reply("Tighter. I won't break, I promise.", 0, Effect.HUG) : new Reply("Hands where I can see them.", 0, Effect.NONE);
            case SURGERY -> surgery(c);
            case ACCEPT -> c.dating() && !c.granted() ? new Reply("Shh. Close your eyes. I'll be right here when you wake up.", 0, Effect.SURGERY)
                : new Reply("Another time.", 0, Effect.NONE);
            case DECLINE -> new Reply("Another time, then. The offer stands.", 0, Effect.NONE);
            case INSULT -> NpcBonds.clamp(c.regard() + EtoRules.INSULT) <= NpcBonds.ENEMY
                ? new Reply("Oh, you want to play rough? Fine. Let's play.", EtoRules.INSULT, Effect.ANGER)
                : new Reply("Wow. Rude. Get out of my library before I shelve you.", EtoRules.INSULT, Effect.CLOSE);
            case LEAVE -> new Reply("", 0, Effect.CLOSE);
            default -> new Reply("Hm? Say that again.", 0, Effect.NONE);
        };
    }

    private static Reply book(Context c) {
        if (!c.book()) {
            return new Reply("A book? Where?", 0, Effect.NONE);
        }
        if (c.step() == EtoRules.Step.BOOKS) {
            int now = c.count() + 1;
            int regard = c.giftedToday() ? 0 : EtoRules.GIFT;
            return now >= EtoRules.Step.BOOKS.need
                ? new Reply("Three! Oh, these are perfect. Now, something less bookish: ask me again.", regard, Effect.BOOK)
                : new Reply("One more for the pile. That's " + now + " of three.", regard, Effect.BOOK);
        }
        if (c.giftedToday()) {
            return new Reply("You're spoiling me. One a day, or I'll never finish them.", 0, Effect.NONE);
        }
        return new Reply("A book! For me? You really do know the way to a girl's heart.", EtoRules.GIFT, Effect.BOOK);
    }

    private static Reply flower(Context c) {
        if (!c.flower()) {
            return new Reply("A flower? Where?", 0, Effect.NONE);
        }
        if (c.step() == EtoRules.Step.FLOWER) {
            return new Reply("...You picked this yourself. I can tell. Thank you. Was there something you wanted to ask me?", 0, Effect.FLOWER);
        }
        if (c.giftedToday()) {
            return new Reply("I've still got the last one. Spoil me tomorrow.", 0, Effect.NONE);
        }
        return new Reply("For me? I'll press it in my favourite book.", EtoRules.GIFT, Effect.FLOWER);
    }

    private static Reply lesson(Context c) {
        EtoRules.Refusal refusal = EtoRules.refusal(c.next(), c.regard(), c.ghoul(), c.beforeDone());
        if (refusal != null) {
            return new Reply(switch (refusal) {
                case ALL_TAUGHT -> "I've taught you everything I know. The rest you learn by bleeding.";
                case HUMAN -> "Teach a human to fight like a ghoul? Cute. No.";
                case PRACTISE -> "Practise what I showed you first. Then come back and show off.";
                default -> "Lessons are for friends. We're not quite there yet. Read me something?";
            }, 0, Effect.NONE);
        }
        return new Reply(switch (c.next()) {
            case KICK -> "Watch my feet. Strike, strike, flip away, then all your weight through one kick. Go on, try it. (Her lesson is in your journal.)";
            case SOAR -> "The ground is for people who are tired. Flip, and keep going up. Kill them before you land. (Her lesson is in your journal.)";
            default -> "One punch is rude. Wind it all the way back and make them watch. Then do it again. (Her lesson is in your journal.)";
        }, 0, Effect.TEACH);
    }

    private static Reply favour(Context c) {
        return switch (c.step()) {
            case NONE -> c.regard() >= NpcBonds.MOST
                ? new Reply("Since you asked so nicely... Bring me three books I haven't read. Then I'll think of something else.", 0, Effect.BEGIN)
                : new Reply("Nothing I'd ask of you. Yet.", 0, Effect.NONE);
            case BOOKS -> new Reply("Books. Three of them, ones I haven't read. You've brought " + c.count() + ".", 0, Effect.NONE);
            case HUNT -> new Reply("The night's crawling with ugly things. Kill ten of them for me, after dark. " + c.count() + " so far.", 0, Effect.NONE);
            case FLOWER -> new Reply("Last thing. A flower. Pick it yourself, I'll know.", 0, Effect.NONE);
            case ASK -> new Reply("That's everything. ...Was there something you wanted to ask me?", 0, Effect.NONE);
            case DATING -> new Reply("You don't have to do anything for me now, silly. Just come back.", 0, Effect.NONE);
        };
    }

    private static Reply surgery(Context c) {
        if (!c.dating()) {
            return new Reply("Like me? You don't even know what I am.", 0, Effect.NONE);
        }
        if (c.granted()) {
            return new Reply("You already are, silly. Look in a mirror at night.", 0, Effect.NONE);
        }
        return c.ghoul() ? new Reply("Like me? One red eye, one of your own... It hurts. A lot. Lie down and close your eyes. Sure?", 0, Effect.OFFER)
            : new Reply("Like me? You'd stop being human, you know. For good. It hurts, and there's no going back. Still sure?", 0, Effect.OFFER);
    }

    static String[] chat(Context c) {
        if (c.dating() || c.regard() >= NpcBonds.FRIEND) {
            return new String[] {"You know what I'd write about you? Nothing. I'd keep you to myself.",
                "Stay a while. The shelves are quieter when you're here.", "Sometimes I think the whole world's a bad first draft. You're one of the good lines."};
        }
        if (c.regard() >= NpcBonds.FRIENDLY) {
            return new String[] {"A story's just a lie that tells the truth sideways. Like people.", "I like you. You don't flinch when I smile.",
                "If you ever see one of my books in a shop, turn it face out. Small kindnesses."};
        }
        return new String[] {"Do you read? Everyone who comes here pretends to.", "The roof leaks over the poetry. Fitting, honestly.",
            "I counted the books once. Then I lost count and started a novel about it."};
    }

    private static String pick(String[] lines, int seed) {
        return lines[Math.floorMod(seed, lines.length)];
    }
}
