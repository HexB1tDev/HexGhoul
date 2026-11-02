package com.hex.kagune.control.village;

/**
 * 1.3.73: who a human is (pure, tested; the owner: "Make general conversation and make humans dateable with romance and different types of
 * personalities"): a name and a temper for life, both from its own seed (its UUID), and everything it says when a player talks to it, by
 * its temper and how warm it is to them (regard, as every NPC's: {@code NpcBonds}).
 */
public final class HumanPersona {
    private HumanPersona() {
    }

    /** Its temper: how it greets, chats, talks of itself, takes a compliment. */
    public enum Temper {
        CHEERFUL("Cheerful"),
        SHY("Shy"),
        BOLD("Bold"),
        KIND("Kind"),
        GRUMPY("Grumpy"),
        FLIRTY("Flirty"),
        DREAMY("Dreamy"),
        NERVOUS("Nervous");

        public final String title;

        Temper(String title) {
            this.title = title;
        }
    }

    /** Names for the village's people (none of them anyone's from the stories). */
    static final String[] NAMES = {"Rin", "Haru", "Yuki", "Aki", "Sora", "Nao", "Mio", "Hikaru", "Ren", "Kaoru", "Makoto", "Chihiro", "Izumi",
        "Michi", "Tsubasa", "Sen", "Hinata", "Yori", "Asa", "Kiko", "Riku", "Emi", "Taka", "Noa"};

    /** How warm (regard) before it is friendly to a player, and a friend. */
    public static final int WARM = 20;
    public static final int CLOSE = 60;

    public static Temper temper(long seed) {
        Temper[] all = Temper.values();
        return all[(int)Math.floorMod(seed * 0x9E3779B97F4A7C15L >>> 33, (long)all.length)];
    }

    public static String name(long seed) {
        return NAMES[(int)Math.floorMod(seed >>> 7, (long)NAMES.length)];
    }

    /** How much warmer a compliment makes it (once a day). */
    public static int liking(Temper temper) {
        return switch (temper) {
            case FLIRTY -> 6;
            case CHEERFUL, SHY, DREAMY -> 4;
            case KIND, BOLD, NERVOUS -> 3;
            case GRUMPY -> 1;
        };
    }

    /** What it says as a player starts talking to it ({@code regard}: how warm it is to them). */
    public static String hello(Temper temper, int regard, int seed) {
        return hello(temper, regard, false, seed);
    }

    /** As above; 1.3.74: {@code partner}: they are together. */
    public static String hello(Temper temper, int regard, boolean partner, int seed) {
        if (partner) {
            return switch (temper) {
                case CHEERFUL -> pick(seed, "My love! Come here!", "There's my favourite person in the whole world!");
                case SHY -> pick(seed, "*lights up* ...Hi, you.", "*takes your hand without a word*");
                case BOLD -> pick(seed, "There you are. Missed you. Don't let it go to your head.", "Mine. Come here.");
                case KIND -> pick(seed, "Welcome home, love. Are you hurt? Hungry?", "I saved you some dinner.");
                case GRUMPY -> pick(seed, "...You're late. I was worried. Not that I'd say so.", "Hmph. Hi. ...Hi.");
                case FLIRTY -> pick(seed, "Mm, there you are. I was getting lonely.", "Come closer. Closer.");
                case DREAMY -> pick(seed, "My moon. You came back.", "I dreamt about you. Again.");
                case NERVOUS -> pick(seed, "You're safe! You're safe. Good. Hi.", "I counted the minutes. Don't laugh.");
            };
        }
        boolean close = regard >= CLOSE;
        boolean warm = regard >= WARM;
        return switch (temper) {
            case CHEERFUL -> close ? pick(seed, "There you are! My day just got better.", "You came to see me? You came to see me!")
                : warm ? pick(seed, "Hi again! Lovely to see you.", "Oh, it's you! Hello!") : pick(seed, "Hello there! New face, huh?", "Hi! Welcome to the village!");
            case SHY -> close ? pick(seed, "Oh... hi. I was hoping you'd come by.", "*smiles at the ground* ...Hi.")
                : warm ? pick(seed, "H-hello again.", "*waves, a little* Hi.") : pick(seed, "Oh! Um. Hello.", "*steps back a little* ...Can I help you?");
            case BOLD -> close ? pick(seed, "Took you long enough. Come here.", "There's my favourite trouble.")
                : warm ? pick(seed, "Back again? Good.", "You again. I don't mind.") : pick(seed, "Who are you, then?", "You're not from around here.");
            case KIND -> close ? pick(seed, "Have you eaten today? Sit with me a while.", "I always feel better when you're around.")
                : warm ? pick(seed, "Welcome back. Is everything alright?", "Good to see you safe.") : pick(seed, "Hello, traveller. Do you need anything?", "Welcome. You look tired.");
            case GRUMPY -> close ? pick(seed, "Hmph. You. ...I'm glad, alright? Don't make it weird.", "Fine. You can stay.")
                : warm ? pick(seed, "Oh. It's you. Could be worse.", "What now?") : pick(seed, "What do you want?", "I'm busy. Make it quick.");
            case FLIRTY -> close ? pick(seed, "Well, if it isn't my favourite person.", "Mm, I was just thinking about you.")
                : warm ? pick(seed, "Back for more of me already?", "Hello, handsome. Or beautiful. Both, maybe.")
                : pick(seed, "Well, hello. I'd remember a face like yours.", "Oh? And who might you be?");
            case DREAMY -> close ? pick(seed, "Oh, it's you. The clouds looked like you this morning.", "I was hoping the wind would bring you.")
                : warm ? pick(seed, "Hello again... did you see the sky today?", "Oh! You're real. Hello.") : pick(seed, "Hm? Oh. Hello. Sorry, I was somewhere else.", "You look like a story.");
            case NERVOUS -> close ? pick(seed, "Oh good, it's you. I feel safer already.", "You came! I- I mean, hello.")
                : warm ? pick(seed, "Ah, hello again. Everything's fine. Probably.", "Oh, it's you. Phew.") : pick(seed, "W-who's there? Oh. Hello.", "Please don't be a ghoul. ...You're not, are you?");
        };
    }

    /** Small talk ("How are you?"). */
    public static String chat(Temper temper, int seed) {
        return switch (temper) {
            case CHEERFUL -> pick(seed, "I'm great! The bakery had fresh bread this morning.", "Wonderful! I've been humming all day.");
            case SHY -> pick(seed, "I'm... fine. Thank you for asking. People don't, usually.", "Okay, I think. Better now.");
            case BOLD -> pick(seed, "Bored. Something should happen around here.", "Strong as ever. Want to arm wrestle?");
            case KIND -> pick(seed, "I'm well. The neighbour's child was sick, but she's better now.", "Busy, but good. How are YOU?");
            case GRUMPY -> pick(seed, "My back hurts and the rooster won't shut up. Next.", "Same as yesterday. Worse, probably.");
            case FLIRTY -> pick(seed, "Better now that you asked.", "Lonely. Not that you'd do anything about it. ...Would you?");
            case DREAMY -> pick(seed, "I counted the stars last night. I lost count at the pretty ones.", "Floating. In a good way.");
            case NERVOUS -> pick(seed, "Fine! Fine. I heard a noise last night. It was probably a cat.", "Okay as long as the CCG walks by.");
        };
    }

    /** What it tells of itself ("Tell me about yourself."). */
    public static String about(Temper temper, int seed) {
        return switch (temper) {
            case CHEERFUL -> pick(seed, "I sell flowers in the square. And smiles, for free!", "I love festivals. And people. And festivals with people.");
            case SHY -> pick(seed, "There's not much to tell. I read. I draw a little. Don't ask to see.", "I like quiet places. You're... quiet too.");
            case BOLD -> pick(seed, "I wanted to join the CCG. They said I talk back too much.", "I can lift a cart by myself. Twice.");
            case KIND -> pick(seed, "I look after the old folks on our street. Somebody has to.", "I bake for the neighbours. It makes the bad nights easier.");
            case GRUMPY -> pick(seed, "I mend shoes. Nobody thanks me. Nobody.", "Born here, will die here. Probably of boredom.");
            case FLIRTY -> pick(seed, "I like good food, good music, and people who look at me like you do.", "I'm a lot of fun. Ask anyone. Better yet, find out.");
            case DREAMY -> pick(seed, "I write poems nobody reads. You could be the first.", "I think the moon is lonely. I talk to it sometimes.");
            case NERVOUS -> pick(seed, "I keep the inn's books. Numbers don't bite.", "I triple-lock my door. Is that a lot? It's not a lot.");
        };
    }

    /** The village's news ("Any news?"). */
    public static String news(int seed) {
        return pick(seed, "The CCG doubled the patrols. Something must have happened.", "Old Taka swears he saw red eyes in the field last night.",
            "There's going to be a market day soon. Bring coin!", "Someone left flowers on every doorstep. Nobody knows who.",
            "A family moved out. Said the nights got too long.", "The bell rang at midnight. Nobody admits ringing it.",
            "They say a ghoul can look just like us. Just like us.", "The baker's daughter is getting married!");
    }

    /** How it takes a compliment ("You look lovely today."). */
    public static String compliment(Temper temper, int regard, int seed) {
        boolean warm = regard >= WARM;
        return switch (temper) {
            case CHEERFUL -> pick(seed, "Aww! You just made my whole week!", warm ? "Stop it, you'll make me cry. Happy tears!" : "Haha, thank you! You too!");
            case SHY -> pick(seed, "*goes bright red* I- thank you...", warm ? "*hides behind their hands* You can't just SAY that..." : "*stares at the floor* ...Thanks.");
            case BOLD -> pick(seed, "I know. But it's nice to hear it from you.", warm ? "Careful. Keep that up and I'll take you seriously." : "Ha! Flattery. I like it.");
            case KIND -> pick(seed, "Oh, that's so sweet of you. You look lovely too.", warm ? "You always know what to say." : "Thank you, dear.");
            case GRUMPY -> pick(seed, "...Hmph. Fine. Thank you. Don't tell anyone.", warm ? "*mutters something, but they're smiling*" : "Are you blind? ...Thanks, though.");
            case FLIRTY -> pick(seed, "Only today? *winks*", warm ? "Keep talking like that and I'll steal you away." : "Mm. Say it again, slower.");
            case DREAMY -> pick(seed, "Lovely... like a word in a poem. Thank you.", warm ? "I'm going to write that down. And keep it forever." : "Oh! Me? Thank you.");
            case NERVOUS -> pick(seed, "M-me? Are you sure? Thank you!", warm ? "*laughs too loud* Thank you! Sorry. Thank you." : "Oh! Oh. That's... nice. Thanks.");
        };
    }

    /** A word as the player goes. */
    public static String bye(Temper temper, int regard, int seed) {
        boolean warm = regard >= WARM;
        return switch (temper) {
            case CHEERFUL -> warm ? "Come back soon, okay?" : "Bye-bye!";
            case SHY -> warm ? "...Come back soon?" : "*waves, small*";
            case BOLD -> warm ? "Don't get yourself killed out there." : "See you around.";
            case KIND -> warm ? "Take care of yourself. Promise me." : "Stay safe.";
            case GRUMPY -> warm ? "...Don't be a stranger. Or do. Whatever." : "Finally.";
            case FLIRTY -> warm ? "Leaving already? I'll miss you." : "Don't be a stranger, stranger.";
            case DREAMY -> warm ? "Until the next pretty sky." : "Bye... safe travels.";
            case NERVOUS -> warm ? "Be careful! Lock your door! ...Bye!" : "B-bye!";
        };
    }

    // ---------------------------------------------------------------- 1.3.74: romance

    /** Bond flags (beside {@code NpcBonds}' own): a ghoul player has told it what they are and it took it; they are together. */
    public static final int CONFESSED = 16;
    public static final int PARTNER = 32;

    /** How it takes a ghoul's confession: yes, it needs to hear more (a second try, with more chance), or no (try another day). */
    public enum Answer { YES, DOUBT, NO }

    /**
     * 1.3.74 (the owner: "if you're a ghoul at some point before starting a relationship you have to admit to your human love interest that
     * you're a ghoul, may or may not require convincing"): how it takes it, from its temper's trust (the kind and the dreamy most, the
     * nervous least), how close they are ({@code regard} past {@link #CLOSE} helps), whether this is the player pleading their case
     * ({@code convincing}: a good deal likelier), against {@code roll} 0..1. A first try that would fail is a doubt (it wants to hear
     * more), not a no; the plea that fails is a no.
     */
    public static Answer confession(Temper temper, int regard, boolean convincing, double roll) {
        double trust = switch (temper) {
            case KIND -> 0.6;
            case DREAMY -> 0.55;
            case FLIRTY, BOLD -> 0.5;
            case CHEERFUL -> 0.45;
            case GRUMPY -> 0.3;
            case SHY -> 0.25;
            case NERVOUS -> 0.15;
        };
        double chance = trust + Math.max(0.0, regard - CLOSE) / 100.0 + (convincing ? 0.35 : 0.0);
        if (roll < chance) {
            return Answer.YES;
        }
        return convincing ? Answer.NO : Answer.DOUBT;
    }

    /** What it says to the confession, by how it takes it. */
    public static String confessed(Temper temper, Answer answer, int seed) {
        return switch (answer) {
            case YES -> switch (temper) {
                case CHEERFUL -> pick(seed, "...A ghoul. You. And you never hurt me once. Then you're still you to me!", "I'm scared. But I'm more scared of losing you.");
                case SHY -> pick(seed, "*is quiet for a long time* ...You told me. You trusted me. ...I trust you too.", "I- I'm shaking. But I'm not leaving.");
                case BOLD -> pick(seed, "Ha. I knew there was something about you. Doesn't change a thing.", "A ghoul. Good. Then nobody will dare touch me.");
                case KIND -> pick(seed, "Oh, you poor thing. Carrying that alone. You don't have to anymore.", "Thank you for telling me. That must have been so hard.");
                case GRUMPY -> pick(seed, "...Figures. The one person I like, and you're a ghoul. ...I still like you. Idiot.", "Hmph. Fine. Don't eat the neighbours.");
                case FLIRTY -> pick(seed, "Dangerous AND gorgeous? You should have led with that.", "Mm. So you could eat me. ...I've had worse offers.");
                case DREAMY -> pick(seed, "Like the stories... a monster with a gentle heart. I always hoped it was true.", "Then the moon and I both love something strange.");
                case NERVOUS -> pick(seed, "*breathes very fast* ...Okay. Okay. You've had a hundred chances to hurt me. You never did. Okay.", "I'm going to faint. ...But I'm staying.");
            };
            case DOUBT -> switch (temper) {
                case CHEERFUL -> pick(seed, "Wait, what? That's... that's not funny. Is it true?", "*the smile drops* ...You're joking. You have to be joking.");
                case SHY -> pick(seed, "*backs away a step* ...Why are you telling me this?", "I... I need to sit down.");
                case BOLD -> pick(seed, "A ghoul. Prove you won't hurt anyone. Go on.", "You'd better explain that. Right now.");
                case KIND -> pick(seed, "I... I don't know what to say. Help me understand.", "*takes your hand, then hesitates* ...Tell me more.");
                case GRUMPY -> pick(seed, "Are you out of your mind? Explain. Slowly.", "...Give me one reason not to scream for the CCG.");
                case FLIRTY -> pick(seed, "Okay, that's a lot. Even for me. Convince me.", "*laughs nervously* ...You're serious.");
                case DREAMY -> pick(seed, "A ghoul... like in the stories? The bad ones or the sad ones?", "*stares at you a long time* ...Which story are you?");
                case NERVOUS -> pick(seed, "*goes white* Y-you're... please don't... please explain.", "*glances at the street, at the CCG* ...Talk. Quickly.");
            };
            case NO -> switch (temper) {
                case CHEERFUL -> "I'm sorry. I can't. Not today. Please go.";
                case SHY -> "*shakes their head, crying* ...Please leave.";
                case BOLD -> "No. I'm not doing this. Walk away.";
                case KIND -> "I'm sorry. I need time. Please give me time.";
                case GRUMPY -> "Get out. Before I change my mind about the CCG.";
                case FLIRTY -> "That's... too much. Even for me. Not today.";
                case DREAMY -> "The sad stories end badly. I'm sorry. Not today.";
                case NERVOUS -> "*backs away, shaking* G-go away. Please.";
            };
        };
    }

    /** Its yes to being asked out (it is close to them and, if they are a ghoul, knows). */
    public static String yes(Temper temper, int seed) {
        return switch (temper) {
            case CHEERFUL -> pick(seed, "YES! I mean, yes. Yes!", "I thought you'd never ask!");
            case SHY -> pick(seed, "*covers their face* ...Yes. Yes, please.", "Me? ...Okay. Yes.");
            case BOLD -> pick(seed, "Finally. Yes. Now kiss me before I change my mind.", "About time. Yes.");
            case KIND -> pick(seed, "I'd love that more than anything.", "Yes. I'll take care of you. I promise.");
            case GRUMPY -> pick(seed, "...Fine. Yes. Don't make me regret it.", "Hmph. Took you long enough. Yes.");
            case FLIRTY -> pick(seed, "I was starting to think I'd have to ask you.", "Mm. Yes. You're mine now.");
            case DREAMY -> pick(seed, "Yes. I'll write a poem about today.", "Like a story. Yes.");
            case NERVOUS -> pick(seed, "Y-yes! Oh no, I said that too loud. Yes.", "Really? Me? Yes. Yes!");
        };
    }

    /** What its partner hears ("I love you."). */
    public static String love(Temper temper, int seed) {
        return switch (temper) {
            case CHEERFUL -> pick(seed, "I love you too! So, so much!", "*throws their arms around you* Me too!");
            case SHY -> pick(seed, "*whispers* ...I love you too.", "*hides their face in your shoulder* ...Me too.");
            case BOLD -> pick(seed, "Obviously. I love you too.", "Say it louder. ...I love you too.");
            case KIND -> pick(seed, "I love you too. Always. Whatever you are.", "And I love you. Come home safe tonight.");
            case GRUMPY -> pick(seed, "...Yeah. Me too. Don't make a thing of it.", "*grumbles* ...Love you too.");
            case FLIRTY -> pick(seed, "Prove it. *grins* ...I love you too.", "Mm, say it again. I love you too.");
            case DREAMY -> pick(seed, "You're my favourite story.", "I love you more than the moon. Don't tell the moon.");
            case NERVOUS -> pick(seed, "I love you too! Please be careful out there!", "Me too. So much it scares me. In a good way.");
        };
    }

    /** 1.3.77: what it says after a kiss with its partner. */
    public static String kissed(Temper temper, int seed) {
        return switch (temper) {
            case CHEERFUL -> pick(seed, "*giggles* Again! No, wait, people are looking. ...Again!", "Mm! You taste like sunshine.");
            case SHY -> pick(seed, "*goes red to the ears* ...Hi.", "*hides their face in their hands* ...That was nice.");
            case BOLD -> pick(seed, "That's more like it.", "Don't stop there. ...Fine, later.");
            case KIND -> pick(seed, "*smiles* I missed you today.", "You're home. That's all I wanted.");
            case GRUMPY -> pick(seed, "...In the street? Really? ...Don't stop.", "Hmph. ...One more. Then go.");
            case FLIRTY -> pick(seed, "Mm. I was waiting for that all day.", "You're getting good at that.");
            case DREAMY -> pick(seed, "*sighs* Like the end of a story.", "I think I heard music just now.");
            case NERVOUS -> pick(seed, "Oh! Oh. Okay. Wow. Hi.", "D-did anyone see? ...I don't care. Hi.");
        };
    }

    /** 1.3.77: what it says in a hug with its partner. */
    public static String hugged(Temper temper, int seed) {
        return switch (temper) {
            case CHEERFUL -> pick(seed, "*squeezes you tight* Gotcha!", "Hugs fix everything!");
            case SHY -> pick(seed, "*holds on a little longer* ...Thank you.", "*muffled* ...You're warm.");
            case BOLD -> pick(seed, "Come here. *pulls you in*", "Nobody hugs like you do.");
            case KIND -> pick(seed, "There you are. You're safe now.", "*rubs your back* Long day?");
            case GRUMPY -> pick(seed, "...Fine. A short one. ...Okay, a long one.", "*grumbles, but doesn't let go*");
            case FLIRTY -> pick(seed, "Mm, you smell nice today.", "Just a hug? ...For now.");
            case DREAMY -> pick(seed, "Let's stay like this till the stars come out.", "You feel like home.");
            case NERVOUS -> pick(seed, "*hugs back stiffly, then melts* ...Oh. That's nice.", "Is this okay? This is okay. Good.");
        };
    }

    static String pick(int seed, String... lines) {
        return lines[Math.floorMod(seed, lines.length)];
    }
}
