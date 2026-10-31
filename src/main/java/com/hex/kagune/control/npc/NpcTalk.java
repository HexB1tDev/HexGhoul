package com.hex.kagune.control.npc;

import java.util.ArrayList;
import java.util.List;

/**
 * 7.0.2: talking with a ghoul NPC (pure, tested). A conversation is pages: what the ghoul says and what the player can answer
 * ({@link Choice}). What it says depends on its temper (each has its own voice), how it stands with the player ({@link NpcBonds}),
 * whether the player is a ghoul and the side they took; what can be answered on what the player holds, the favours on, and whether
 * the ghoul walks with them. Every answer is a {@link Reply}: what the ghoul says back, what it does to its regard, and what the
 * server then does ({@link Effect}). Ferals never talk; a ghoul that would go for the player on sight never gets as far as talking.
 */
public final class NpcTalk {
    /** What the player can say. */
    public enum Choice {
        WHO("Who are you?"),
        CHAT("Talk a while."),
        GIFT("Here. Eat. (give the Ghoul Meat in your hand)"),
        FAVOUR("Need anything done?"),
        ACCEPT("I'll do it."),
        DECLINE("Not now."),
        INVITE("Walk with me."),
        FOLLOW("Follow me."),
        STAY("Wait here."),
        PART("Go your own way."),
        INSULT("Get out of my sight."),
        LEAVE("Leave."),
        // 1.3.0: Eto's (new ones go at the end: the talk is sent by ordinal).
        BOOK("I brought you a book. (give the book in your hand)"),
        FLOWER("I picked this for you. (give the flower in your hand)"),
        LESSON("Teach me something."),
        ASK_OUT("Go out with me."),
        KISS("Kiss her."),
        HUG("Hug her."),
        SURGERY("Make me like you."),
        // 1.3.2: the one-of-a-kind NPCs' (Eto's first). SAY_1..SAY_8 carry the words the page gives them (a topic, a reply).
        SAY_1("..."),
        SAY_2("..."),
        SAY_3("..."),
        SAY_4("..."),
        SAY_5("..."),
        SAY_6("..."),
        SAY_7("..."),
        SAY_8("..."),
        SWEET("Be sweet to her."),
        PUPPY("I brought you a little someone. (the baby animal on your lead)"),
        PROPOSE("Marry me. (offer the ring)"),
        NIGHT("Come to bed."),
        TRIAL("I'm ready. Make me one-eyed."),
        TEASE("Tease her."),
        LETTER("I wrote you something. (give the written book)"),
        BACK("Back.");

        /** The reply slots, in order. */
        public static final Choice[] SAYS = {SAY_1, SAY_2, SAY_3, SAY_4, SAY_5, SAY_6, SAY_7, SAY_8};

        /** The slot this is (0..7), or -1. */
        public int say() {
            return this.ordinal() >= SAY_1.ordinal() && this.ordinal() <= SAY_8.ordinal() ? this.ordinal() - SAY_1.ordinal() : -1;
        }

        private final String text;

        Choice(String text) {
            this.text = text;
        }

        public String text() {
            return this.text;
        }

        public static Choice byOrdinal(int ordinal) {
            Choice[] all = values();
            return ordinal >= 0 && ordinal < all.length ? all[ordinal] : LEAVE;
        }
    }

    /** What the server does after an answer. */
    public enum Effect {
        NONE, OFFER, START_FAVOUR, GIFT, ALLY, FOLLOW, STAY, PART, ANGER, FLEE, CLOSE
    }

    /**
     * Everything a page depends on: who the ghoul is and how it regards the player, what the player is, whether it walks with them
     * (and follows), whether they chatted today and it told them who it is, Ghoul Meat in hand, its favour on for them, room for
     * another favour, what it would ask (null: nothing), room for another ally.
     */
    public record Context(NpcIdentity who, int regard, boolean ghoul, String side, boolean ally, boolean following, boolean chattedToday,
                          boolean asked, boolean meatInHand, boolean favourOn, boolean favourRoom, String asking, boolean allyRoom) {
    }

    /** A page: what it says, and the answers. */
    public record Page(String line, List<Choice> choices) {
    }

    /** What an answer brings: its words, the change in its regard, what happens. */
    public record Reply(String line, int regard, Effect effect) {
    }

    private NpcTalk() {
    }

    /** True if the ghoul talks at all to a player it takes to so ({@code attitude}). */
    public static boolean talks(NpcTemper temper, NpcTemper.Attitude attitude) {
        return temper != NpcTemper.FERAL && attitude != NpcTemper.Attitude.HOSTILE;
    }

    /** The first page: its greeting, and the answers. */
    public static Page open(Context c) {
        return new Page(greeting(c), choices(c));
    }

    /** The answers it allows now. */
    public static List<Choice> choices(Context c) {
        List<Choice> out = new ArrayList<>();
        NpcTemper temper = c.who().temper();
        out.add(Choice.WHO);
        out.add(Choice.CHAT);
        if (c.meatInHand() && temper != NpcTemper.ANTEIKU) {
            out.add(Choice.GIFT);
        }
        if (c.asking() != null || c.favourOn()) {
            out.add(Choice.FAVOUR);
        }
        if (c.ally()) {
            out.add(c.following() ? Choice.STAY : Choice.FOLLOW);
            out.add(Choice.PART);
        } else {
            if (c.regard() >= NpcBonds.FRIENDLY && temper != NpcTemper.GOURMET) {
                out.add(Choice.INVITE);
            }
            out.add(Choice.INSULT);
        }
        out.add(Choice.LEAVE);
        return out;
    }

    /** The answers to its offer of a favour. */
    public static List<Choice> offer() {
        return List.of(Choice.ACCEPT, Choice.DECLINE);
    }

    /** What it says back to {@code choice}. */
    public static Reply answer(Context c, Choice choice) {
        NpcIdentity who = c.who();
        NpcTemper temper = who.temper();
        String given = who.given();
        return switch (choice) {
            case WHO -> new Reply(who(temper, given), c.asked() ? 0 : NpcBonds.WHO, Effect.NONE);
            case CHAT -> c.chattedToday() ? new Reply("We've talked enough for one night.", 0, Effect.NONE)
                : new Reply(pick(chat(temper), who.seed() + c.regard()), NpcBonds.CHAT, Effect.NONE);
            case GIFT -> c.meatInHand() && temper != NpcTemper.ANTEIKU
                ? new Reply(thanks(temper, c.ghoul()), temper == NpcTemper.GOURMET ? NpcBonds.GIFT_GOURMET : NpcBonds.GIFT, Effect.GIFT)
                : new Reply("Keep it.", 0, Effect.NONE);
            case FAVOUR -> c.favourOn() ? new Reply("You're still on what I asked you last time.", 0, Effect.NONE)
                : c.asking() == null ? new Reply("Nothing I'd ask of you.", 0, Effect.NONE)
                : !c.favourRoom() ? new Reply("You've got enough on your plate already.", 0, Effect.NONE)
                : new Reply(ask(temper) + " " + c.asking(), 0, Effect.OFFER);
            case ACCEPT -> c.asking() != null && c.favourRoom() && !c.favourOn()
                ? new Reply(temper == NpcTemper.PACK ? "Don't keep me waiting." : "Good. I'll owe you.", NpcBonds.ACCEPT, Effect.START_FAVOUR)
                : new Reply("Another time.", 0, Effect.NONE);
            case DECLINE -> new Reply("Another time, then.", 0, Effect.NONE);
            case INVITE -> invite(c);
            case FOLLOW -> c.ally() ? new Reply(temper == NpcTemper.SCAVENGER ? "Right behind you. Always behind you." : "Lead on.", 0, Effect.FOLLOW)
                : new Reply("Follow you where?", 0, Effect.NONE);
            case STAY -> c.ally() ? new Reply("I'll be here.", 0, Effect.STAY) : new Reply("I go where I please.", 0, Effect.NONE);
            case PART -> c.ally() ? new Reply("...Suit yourself. Look after yourself out there.", 0, Effect.PART) : new Reply("We were never together.", 0,
                Effect.NONE);
            case INSULT -> insult(c);
            case LEAVE -> new Reply("", 0, Effect.CLOSE);
            // 1.3.0: Eto's alone (EtoTalk); never offered by a ghoul NPC.
            case BOOK, FLOWER, LESSON, ASK_OUT, KISS, HUG, SURGERY, SAY_1, SAY_2, SAY_3, SAY_4, SAY_5, SAY_6, SAY_7, SAY_8, SWEET, PUPPY, PROPOSE,
                NIGHT, TRIAL, TEASE, LETTER, BACK -> new Reply("", 0, Effect.CLOSE);
        };
    }

    private static Reply invite(Context c) {
        NpcTemper temper = c.who().temper();
        if (c.ally()) {
            return new Reply("I'm already with you.", 0, Effect.NONE);
        }
        if (temper == NpcTemper.GOURMET) {
            return new Reply("I dine alone.", 0, Effect.NONE);
        }
        if (!NpcBonds.joins(temper, c.side())) {
            return new Reply(temper == NpcTemper.PACK ? "Run with the Pack first. Then we'll talk." : "Not with the company you keep.", 0, Effect.NONE);
        }
        if (c.regard() < NpcBonds.FRIEND) {
            return new Reply("We're not there yet. Give it time.", 0, Effect.NONE);
        }
        if (!c.allyRoom()) {
            return new Reply("You've already got enough people trailing after you.", 0, Effect.NONE);
        }
        return new Reply(switch (temper) {
            case ANTEIKU -> "All right. I'll watch your back. Don't make me regret it.";
            case PACK -> "Finally. Point me at something and watch it break.";
            case SCAVENGER -> "You... you mean it? All right. I'll stay close.";
            default -> "Lead the way.";
        }, 0, Effect.ALLY);
    }

    private static Reply insult(Context c) {
        NpcTemper temper = c.who().temper();
        int after = NpcBonds.clamp(c.regard() + NpcBonds.INSULT);
        return switch (temper) {
            case PACK -> new Reply("Say that again. I dare you.", NpcBonds.INSULT, Effect.ANGER);
            case SCAVENGER -> new Reply("I'm going, I'm going!", NpcBonds.INSULT, Effect.FLEE);
            case ANTEIKU -> new Reply("...All right. I hope whatever's eating at you lets go of you.", NpcBonds.INSULT, Effect.CLOSE);
            case GOURMET -> new Reply("How rude. And you smelled so ordinary, too.", NpcBonds.INSULT, Effect.CLOSE);
            default -> after <= NpcBonds.ENEMY ? new Reply("You want a fight? You've got one.", NpcBonds.INSULT, Effect.ANGER)
                : new Reply("Fine. Don't come looking for me.", NpcBonds.INSULT, Effect.CLOSE);
        };
    }

    // ---------------------------------------------------------------- its voice

    static String greeting(Context c) {
        NpcIdentity who = c.who();
        NpcTemper temper = who.temper();
        int seed = who.seed();
        boolean pack = "pack".equals(c.side());
        if (c.ally()) {
            return switch (temper) {
                case ANTEIKU -> "Still here. Where are we going?";
                case PACK -> "Who are we hunting?";
                case SCAVENGER -> "I'm still here. Right behind you.";
                default -> "Still with you. Where to?";
            };
        }
        NpcBonds.Band band = NpcBonds.band(c.regard());
        if (band == NpcBonds.Band.COLD || band == NpcBonds.Band.ENEMY) {
            return switch (temper) {
                case ANTEIKU -> "You've hurt people. I can see it on you. What do you want?";
                case PACK -> "You've got a nerve, showing your face here.";
                case SCAVENGER -> "You. Keep your distance.";
                case GOURMET -> "You again. Your manners haven't improved.";
                default -> "I remember you. Say what you came to say.";
            };
        }
        if (band == NpcBonds.Band.FRIEND) {
            return switch (temper) {
                case ANTEIKU -> "You've been good to people out here. It doesn't go unnoticed.";
                case PACK -> "You've earned your place. Nobody touches you while I'm around.";
                case SCAVENGER -> "You're the only one who doesn't look at me like I'm a meal.";
                case GOURMET -> "If only every human had your taste in company.";
                default -> "Good to see you. I kept watch while you were gone.";
            };
        }
        if (band == NpcBonds.Band.FRIENDLY) {
            return switch (temper) {
                case ANTEIKU -> "Come, sit. There's always a cup of coffee for a friend.";
                case PACK -> pack ? "One of ours. Good. The Pack eats tonight." : "You again. You're starting to grow on me.";
                case SCAVENGER -> "Oh, it's you. Find anything good out there?";
                case GOURMET -> "You again. You're far more interesting than you smell.";
                default -> "You again. The night's quieter with company.";
            };
        }
        if (!c.ghoul()) {
            return switch (temper) {
                case ANTEIKU -> "You shouldn't be out this late. It isn't safe for people like you. Stay where it's lit.";
                case GOURMET -> pick(new String[] {"A human, out at night... how delightfully rare. Run along, before I remember I'm hungry.",
                    "Mm. You smell ordinary. Lucky you."}, seed);
                case SCAVENGER -> "You shouldn't be talking to me. Really. Walk away.";
                default -> "A human, out here, this late. You've no idea what I am, have you? Go home.";
            };
        }
        return switch (temper) {
            case ANTEIKU -> pack ? "You run with the Pack. Say your piece and go."
                : pick(new String[] {"Easy. Nobody needs to get hurt tonight. Are you managing to eat?",
                    "If you're hungry, there are better ways than hunting people. Ask, and I'll tell you."}, seed);
            case PACK -> pack ? "One of ours. Good. The Pack eats tonight."
                : pick(new String[] {"Alone out here? The weak end up as someone's dinner. The Pack doesn't.",
                    "You've got teeth. Question is whether you'll use them for us."}, seed);
            case SCAVENGER -> pick(new String[] {"D-don't come closer with those eyes. I only eat what's already dead, I swear it.",
                "Easy, easy. There's nothing on me worth taking."}, seed);
            default -> pick(new String[] {"You smell like one of us. Keep walking, or sit a while. Either's fine.",
                "Another one out under the moon. Don't make it a fight and it won't be one."}, seed);
        };
    }

    static String who(NpcTemper temper, String given) {
        return switch (temper) {
            case ANTEIKU -> given + ". In the daytime I help out at a little café. Ghouls and humans can live side by side, if we're careful.";
            case PACK -> given + ". The Pack takes what it wants. Stand with us or stand aside.";
            case SCAVENGER -> given + ". Nobody, really. I eat what the others leave. It keeps me alive and out of their way.";
            case GOURMET -> given + ". A connoisseur. I eat only the finest... and the finest are always ghouls.";
            default -> given + ". I go where the hunting's quiet. Stay in one place too long and someone comes looking.";
        };
    }

    static String[] chat(NpcTemper temper) {
        return switch (temper) {
            case ANTEIKU -> new String[] {"The trick to good coffee is patience. Same as the trick to not killing people.",
                "Some nights I just walk, and listen to the city breathing.", "A human dropped her keys yesterday. I gave them back. She smiled at me."};
            case PACK -> new String[] {"The doves are getting bolder. Soon they'll learn.", "Fear's useful. Teach them to fear you and they leave you alone.",
                "Anteiku talks about peace. Peace is what the strong let the weak have."};
            case SCAVENGER -> new String[] {"There's a cave west of here with bones in it. Old ones. Not mine.",
                "The Pack came through two nights ago. I hid in a hole for a whole day.", "Do you ever miss the taste of bread? I do."};
            case GOURMET -> new String[] {"Most hunters have no patience. A meal should be anticipated.",
                "There's a ghoul near here whose scent is... exquisite. I can almost taste it."};
            default -> new String[] {"Rain's coming. You can smell it on the wind.",
                "Ever notice how quiet it gets right before dawn? That's when they're done hunting.", "I had a name for the moon once. Forgot it."};
        };
    }

    static String thanks(NpcTemper temper, boolean ghoul) {
        return switch (temper) {
            case SCAVENGER -> "For me? You're... you're really giving me this?";
            case PACK -> "Now that's a gift. The Pack remembers who feeds it.";
            case GOURMET -> ghoul ? "From a ghoul's own hand. How thoughtful." : "Where would a human get this? ...I won't ask. Thank you.";
            default -> "...Thanks. I won't forget it.";
        };
    }

    static String ask(NpcTemper temper) {
        return switch (temper) {
            case ANTEIKU -> "There is, if you're willing.";
            case PACK -> "Prove you're worth something.";
            case SCAVENGER -> "M-maybe. If you're offering.";
            case GOURMET -> "Perhaps you could do me a small service.";
            default -> "There is something.";
        };
    }

    private static String pick(String[] lines, int seed) {
        return lines[Math.floorMod(seed, lines.length)];
    }
}
