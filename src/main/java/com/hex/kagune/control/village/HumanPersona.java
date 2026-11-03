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
        NERVOUS("Nervous"),
        // 2.0.36 (the owner: "more variety in the traits"): appended (saved by ordinal); only villagers placed from now on roll them.
        PLAYFUL("Playful"),
        STOIC("Stoic"),
        SARCASTIC("Sarcastic"),
        ROMANTIC("Romantic");

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

    /** 2.0: how many names there are, and name {@code index} (a village gives each of its people its own). */
    public static int names() {
        return NAMES.length;
    }

    public static String named(int index) {
        return NAMES[Math.floorMod(index, NAMES.length)];
    }

    /** 2.0.36: the first eight tempers, the ones a villager placed before 2.0.36 has. */
    public static final int LEGACY = 8;

    /**
     * Its temper by its seed alone, one of the first {@link #LEGACY} (2.0.36: unchanged for every villager placed before, who keep theirs;
     * a villager placed since keeps the one it {@link #rolled} in its saved data).
     */
    public static Temper temper(long seed) {
        Temper[] all = Temper.values();
        return all[(int)Math.floorMod(seed * 0x9E3779B97F4A7C15L >>> 33, (long)LEGACY)];
    }

    /** 2.0.36: a new villager's temper, any of them (the new ones too), from its seed. */
    public static Temper rolled(long seed) {
        Temper[] all = Temper.values();
        return all[Math.min(all.length - 1, (int)(unit(seed, 0x7E3FL) * all.length))];
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
            case PLAYFUL, ROMANTIC -> 5;
            case STOIC, SARCASTIC -> 2;
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
                case PLAYFUL -> pick(seed, "Boo! Did I scare you? ...I missed you.", "Tag, you're it! ...Kidding. Come here.");
                case STOIC -> pick(seed, "You're back. Good.", "*nods once, and steps close*");
                case SARCASTIC -> pick(seed, "Oh look, my favourite headache is back.", "You again. Lucky me. ...No, really. Lucky me.");
                case ROMANTIC -> pick(seed, "My darling! I wrote you a letter. Three, actually.", "You're here! My heart did a little dance.");
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
            case PLAYFUL -> close ? pick(seed, "Guess who's been waiting? Me! It's me.", "There you are! I hid your favourite apple. Find it.")
                : warm ? pick(seed, "Oh good, a victim! I mean, a friend.", "Back again? Wanna race to the well?") : pick(seed, "Ooh, a new face! Are you fun?", "Hi! Wanna see a trick? ...Later, then.");
            case STOIC -> close ? pick(seed, "You came. I'm glad.", "Sit. Rest. You've earned it.")
                : warm ? pick(seed, "Hello again.", "*nods* You're well?") : pick(seed, "Hm. A traveller.", "State your business. Calmly.");
            case SARCASTIC -> close ? pick(seed, "Ah, my most tolerable person.", "If it isn't the one human I can stand.")
                : warm ? pick(seed, "Oh, joy. You.", "Back for more of my charm?") : pick(seed, "Wow, a stranger. In a village. How thrilling.", "Let me guess: lost, hungry, or both?");
            case ROMANTIC -> close ? pick(seed, "Oh, it's you! I was just thinking about you. Again.", "Every time you come by, it feels like a story starting.")
                : warm ? pick(seed, "Hello again! The flowers bloomed today. Did you see?", "Oh! You. My day just got a chapter better.") : pick(seed, "Oh, hello! You have a face from a love story.", "A stranger at the gate... how romantic.");
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
            case PLAYFUL -> pick(seed, "I put a frog in the baker's flour sack. Best morning ever.", "Bored! Play a game with me. Any game.");
            case STOIC -> pick(seed, "Well enough. The wind is from the east.", "Steady. As always.");
            case SARCASTIC -> pick(seed, "Living the dream. The dream is mostly chores.", "Fantastic. The rooster and I are best friends now.");
            case ROMANTIC -> pick(seed, "I'm reading a love story. The heroine's an idiot. I adore her.", "Wonderful! Someone left a rose on my step. Probably the wind. Probably.");
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
            case PLAYFUL -> pick(seed, "I know every rooftop in this village. Don't tell my mother.", "I'm the best at hide and seek. Nobody's found me since spring.");
            case STOIC -> pick(seed, "I chop wood and keep watch at night. Someone should.", "I lost my family to a ghoul. I don't hate. I don't forget.");
            case SARCASTIC -> pick(seed, "I run the stall nobody visits. Business is booming.", "I'm the village's ray of sunshine. Can't you tell?");
            case ROMANTIC -> pick(seed, "I believe in love at first sight. And second. And third.", "I keep a pressed flower from every festival. Hundreds of them.");
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
            case PLAYFUL -> pick(seed, "Ha! I know. Say it again, louder!", warm ? "Careful, I'll start believing you." : "Flattery gets you... a cookie. Here.");
            case STOIC -> pick(seed, "...Thank you.", warm ? "*a rare smile* You're kind." : "Noted.");
            case SARCASTIC -> pick(seed, "Wow. Did that hurt to say?", warm ? "...Okay, that one actually landed. Thanks." : "Sure. And I'm the CCG's finest.");
            case ROMANTIC -> pick(seed, "*hand over heart* You'll make me swoon!", warm ? "Say that by moonlight and I'm yours." : "Oh! Thank you. You're very sweet.");
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
            case PLAYFUL -> warm ? "Don't go! ...Fine. Race you next time!" : "Bye-bye! Catch you later. Literally.";
            case STOIC -> warm ? "Go carefully. Come back." : "Safe road.";
            case SARCASTIC -> warm ? "Try not to die. I'd be mildly upset." : "Don't let the gate hit you.";
            case ROMANTIC -> warm ? "Parting is such sweet sorrow. Come back soon!" : "Farewell! Mind the roses.";
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
        return confession(temper, regard, convincing, roll, 0.0);
    }

    /** 2.0.35: the same, {@code more} likelier for what it looks for ({@link #trusts}). */
    public static Answer confession(Temper temper, int regard, boolean convincing, double roll, double more) {
        double trust = switch (temper) {
            case KIND -> 0.6;
            case DREAMY -> 0.55;
            case FLIRTY, BOLD -> 0.5;
            case CHEERFUL -> 0.45;
            case GRUMPY -> 0.3;
            case SHY -> 0.25;
            case NERVOUS -> 0.15;
            case PLAYFUL -> 0.5;
            case STOIC -> 0.4;
            case SARCASTIC -> 0.35;
            case ROMANTIC -> 0.55;
        };
        double chance = trust + more + Math.max(0.0, regard - CLOSE) / 100.0 + (convincing ? 0.35 : 0.0);
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
                case PLAYFUL -> pick(seed, "A ghoul? Wow. Can you do the eye thing? Do the eye thing!", "So you're dangerous AND fun. Best friend ever. More than friend. You know.");
                case STOIC -> pick(seed, "...I see. You've never given me reason to fear you. I won't start now.", "A ghoul. And still here, being honest. That's enough for me.");
                case SARCASTIC -> pick(seed, "A ghoul. Of course. Of COURSE the one I like is a ghoul. ...Fine. I'm in.", "Well, that explains the diet. I'm still here, aren't I?");
                case ROMANTIC -> pick(seed, "A ghoul and a human... the most beautiful tragedy. Let's not make it one.", "Love doesn't ask what you are. Neither do I.");
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
                case PLAYFUL -> pick(seed, "Ha ha, good one. ...Wait. You're not laughing.", "Is this a prank? Tell me it's a prank.");
                case STOIC -> pick(seed, "*is silent a long time* ...Explain.", "I need more than that. Tell me everything.");
                case SARCASTIC -> pick(seed, "Oh great, a ghoul. Love that for me. ...Talk.", "Sure, and I'm a dove. ...Wait. You're serious?");
                case ROMANTIC -> pick(seed, "Like the stories... but which ending? Tell me more.", "My heart says yes. My head needs to hear more.");
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
                case PLAYFUL -> "That's not a funny game. Go away. Please.";
                case STOIC -> "No. Leave. Don't come back today.";
                case SARCASTIC -> "Wow. No. Absolutely not. Goodbye.";
                case ROMANTIC -> "Some stories aren't meant to be. I'm sorry. Please go.";
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
            case PLAYFUL -> pick(seed, "Yes! Ha! I win!", "Yes! Now you have to play every game I make up.");
            case STOIC -> pick(seed, "Yes.", "*takes your hand* ...Yes.");
            case SARCASTIC -> pick(seed, "Took you long enough. Yes, obviously.", "Fine. Yes. Don't let it go to your head.");
            case ROMANTIC -> pick(seed, "Yes! Yes, a thousand times!", "I've been dreaming of this. Yes!");
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
            case PLAYFUL -> pick(seed, "Love you more! I win.", "*pokes your cheek* Love you too, silly.");
            case STOIC -> pick(seed, "I love you. I don't say things I don't mean.", "*rests their forehead on yours* ...Yes. Me too.");
            case SARCASTIC -> pick(seed, "Ugh. ...I love you too. Happy?", "Gross. ...Me too.");
            case ROMANTIC -> pick(seed, "I love you to the moon and every star past it.", "I love you. I'll say it every day until you're sick of it.");
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
            case PLAYFUL -> pick(seed, "Ha! Got you back. ...Wait, I liked that.", "Again! That's the rule.");
            case STOIC -> pick(seed, "...Good.", "*says nothing, but holds you a moment longer*");
            case SARCASTIC -> pick(seed, "Wow, in public. Scandalous. ...Do it again.", "That was terrible. Again, for practice.");
            case ROMANTIC -> pick(seed, "*swoons* Like the last page of a novel.", "My knees went weak. Do that again.");
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
            case PLAYFUL -> pick(seed, "*lifts you off your feet* Gotcha!", "Hug attack! You lose.");
            case STOIC -> pick(seed, "*holds you, steady as a wall*", "You're safe here.");
            case SARCASTIC -> pick(seed, "Ugh, affection. ...Don't let go yet.", "This is so embarrassing. Five more seconds.");
            case ROMANTIC -> pick(seed, "I could stay like this forever.", "*sighs happily* My favourite place in the world.");
        };
    }

    /** 1.3.78: taking its partner's hand for a walk. */
    public static String walk(Temper temper, int seed) {
        return switch (temper) {
            case CHEERFUL -> pick(seed, "A walk? Yes! Hold on tight!", "*grabs your hand and swings it* Let's go!");
            case SHY -> pick(seed, "*takes your hand very carefully* ...Okay.", "Your hand's warm. ...Let's walk.");
            case BOLD -> pick(seed, "Finally. *laces their fingers with yours*", "Lead the way. Or I will.");
            case KIND -> pick(seed, "I'd love to. Not too fast, okay?", "*squeezes your hand* Show me your favourite street.");
            case GRUMPY -> pick(seed, "...Fine. Only because my feet are cold.", "*takes your hand, looking anywhere else* Walk, then.");
            case FLIRTY -> pick(seed, "Hand in hand? Everyone will talk. Good.", "Mm. I was hoping you'd ask.");
            case DREAMY -> pick(seed, "Like in the stories. Let's walk till the lamps come on.", "Let's count the chimneys.");
            case NERVOUS -> pick(seed, "O-okay! Is my hand sweaty? Sorry!", "People can see us. ...I don't mind. Let's go.");
            case PLAYFUL -> pick(seed, "Race you! ...Holding hands. Slow race.", "Let's go! I'll show you a secret path.");
            case STOIC -> pick(seed, "Lead on.", "*takes your hand without a word*");
            case SARCASTIC -> pick(seed, "Hand in hand? How original. ...Let's go.", "Fine. I'll let you be seen with me.");
            case ROMANTIC -> pick(seed, "Hand in hand, like in the books! Lead on, my love.", "A stroll at dusk? You know my weak spot.");
        };
    }

    /** 1.3.78: a word now and then, walking hand in hand. */
    public static String stroll(Temper temper, int seed) {
        return switch (temper) {
            case CHEERFUL -> pick(seed, "Look, the baker waved at us!", "This is the best day. Again.", "*hums and swings your hands*");
            case SHY -> pick(seed, "*leans their head on your shoulder for a moment*", "...I like this.", "*squeezes your hand once*");
            case BOLD -> pick(seed, "Let them stare. You're mine.", "Faster! No, slower. I want this to last.", "Nobody else gets to do this.");
            case KIND -> pick(seed, "Are you warm enough?", "I'm glad you're here. Really.", "Mind the step, love.");
            case GRUMPY -> pick(seed, "...This isn't so bad.", "If anyone asks, you dragged me.", "*doesn't let go*");
            case FLIRTY -> pick(seed, "You walk like you own the place. Cute.", "Your hand fits mine. Funny, that.", "Where are you taking me?");
            case DREAMY -> pick(seed, "The bricks are warm from the sun. Feel?", "I'm going to remember this street forever.", "Do you hear the swallows?");
            case NERVOUS -> pick(seed, "Am I walking too fast? Too slow?", "I keep smiling. I can't stop.", "Hold on, my heart's going so fast.");
            case PLAYFUL -> pick(seed, "Step on the cracks and you lose!", "*swings your hand way too high*", "Bet I can walk backwards the whole way.");
            case STOIC -> pick(seed, "*matches your step exactly*", "Quiet evening. Good.", "I like walking with you.");
            case SARCASTIC -> pick(seed, "Everyone's staring. Smile, it's free.", "Yes, this is my idea of a thrill.", "Mind the puddle. Or don't, I'd laugh.");
            case ROMANTIC -> pick(seed, "Look, the lamps are coming on just for us.", "Do you think they'll write songs about us?", "*leans into your arm, smiling*");
        };
    }

    /** 1.3.78: its partner let go of its hand. */
    public static String letGo(Temper temper, int seed) {
        return switch (temper) {
            case CHEERFUL -> pick(seed, "Aw! Again soon, okay?", "That was fun! Thank you!");
            case SHY -> pick(seed, "*holds the hand you held* ...Thank you.", "...Already? Okay. That was nice.");
            case BOLD -> pick(seed, "Done already? Next time, longer.", "Fine. But I'm keeping your warmth.");
            case KIND -> pick(seed, "Thank you for the walk. Come back safe.", "That was lovely. Rest a little, okay?");
            case GRUMPY -> pick(seed, "...Hmph. My hand's cold now.", "Whatever. ...Same time tomorrow?");
            case FLIRTY -> pick(seed, "Leaving me wanting more. Clever.", "Mm. I'll be thinking about that.");
            case DREAMY -> pick(seed, "The end of a chapter. Not the book.", "I'll dream about this walk.");
            case NERVOUS -> pick(seed, "Was that okay? It was okay. It was great!", "Oh! Okay. Thank you. I'm still shaking.");
            case PLAYFUL -> pick(seed, "Aw, game over? Rematch tomorrow!", "That was fun! Tag, you're it!");
            case STOIC -> pick(seed, "Until next time.", "*nods* Thank you.");
            case SARCASTIC -> pick(seed, "Finally, my hand is free. ...Come back soon.", "That was almost fun. Don't tell anyone.");
            case ROMANTIC -> pick(seed, "Until we meet again, my love.", "I'll keep the warmth of your hand all night.");
        };
    }

    /** 1.3.78: at the edge of the village: it never goes past (the owner: "they never leave the village perimeter"). */
    public static String edge(Temper temper, int seed) {
        return switch (temper) {
            case CHEERFUL -> pick(seed, "Not past the walls! Come back in, it's nicer here!", "Out there? No way. Back inside!");
            case SHY -> pick(seed, "*stops* I... can't go out there. I'm sorry.", "*lets go* It's not safe past the walls...");
            case BOLD -> pick(seed, "Past the walls? Not even for you.", "I stay inside. You know that.");
            case KIND -> pick(seed, "I can't go beyond the village. Come back to me soon.", "This is as far as I go. Be careful out there.");
            case GRUMPY -> pick(seed, "Out there? With the ghouls? No.", "I'm not going past the walls. Don't ask.");
            case FLIRTY -> pick(seed, "If you want me, you'll have to come back in.", "Running away from me? I'll wait here.");
            case DREAMY -> pick(seed, "The village ends here. My world does too.", "Past the walls is someone else's story.");
            case NERVOUS -> pick(seed, "N-no, not out there! Please come back in!", "I can't, I can't. The walls, please.");
            case PLAYFUL -> pick(seed, "Nope! The wall is base. Safe zone!", "Can't cross the line, those are the rules!");
            case STOIC -> pick(seed, "The wall stays between me and the dark. I stop here.", "No further. Go well.");
            case SARCASTIC -> pick(seed, "Out there? With the monsters? Pass.", "The wall and I have an understanding. I stay.");
            case ROMANTIC -> pick(seed, "Even love stops at the wall. Come back to me.", "I'll wait here, like a lantern in a window.");
        };
    }

    // ---------------------------------------------------------------- 2.0.35: who it falls for, and what it looks for

    /**
     * 2.0.35 (the owner: "romance with both males and female characters, different preferences on every male and female"): who it falls
     * for, its own for life ({@link #attraction}).
     */
    public enum Attraction {
        MEN, WOMEN, BOTH
    }

    /** 2.0.35: what it looks for in a partner, its own for life ({@link #taste}). */
    public enum Taste {
        STRONG, GENTLE, FUNNY, MYSTERIOUS, CLEVER, DARING, LOYAL, CARING
    }

    /** Bond flag: it told this player they are not its type (no asking it out again; still friends). */
    public static final int NOT_TYPE = 64;

    /** {@code seed} stirred with {@code salt} into 0..1 (each of its traits from its own bits, none moving another). */
    static double unit(long seed, long salt) {
        long s = (seed ^ salt * 0x9E3779B97F4A7C15L) * 0xBF58476D1CE4E5B9L;
        s ^= s >>> 31;
        s *= 0x94D049BB133111EBL;
        s ^= s >>> 29;
        return (s >>> 11) * 0x1.0p-53;
    }

    /** Who a {@code female} (or male) villager falls for: most the other sex (62 in 100), a fair share both (22), the rest their own. */
    public static Attraction attraction(long seed, boolean female) {
        double r = unit(seed, 0xA77AL);
        if (r < 0.62) {
            return female ? Attraction.MEN : Attraction.WOMEN;
        }
        if (r < 0.84) {
            return Attraction.BOTH;
        }
        return female ? Attraction.WOMEN : Attraction.MEN;
    }

    public static Taste taste(long seed) {
        Taste[] all = Taste.values();
        return all[Math.min(all.length - 1, (int)(unit(seed, 0x7A57EL) * all.length))];
    }

    /** True if it could fall for a player who is a woman ({@code female} true), a man (false), or never said (null: it may). */
    public static boolean attracted(Attraction attraction, Boolean female) {
        return female == null || attraction == Attraction.BOTH || (attraction == Attraction.WOMEN) == female;
    }

    /** Who it falls for, in its own words' plain part ("men", "women"). */
    static String who(Attraction attraction) {
        return attraction == Attraction.MEN ? "men" : "women";
    }

    /** Its answer to "What's your type?": who, then what it looks for, in its temper's way. */
    public static String type(Temper temper, Attraction attraction, Taste taste, int seed) {
        String opener = switch (temper) {
            case CHEERFUL -> "Ooh, fun question! ";
            case SHY -> "*fidgets with a sleeve* ...";
            case BOLD -> "Straight answer? ";
            case KIND -> "Hmm, let me think... ";
            case GRUMPY -> "Why do you care? ...Fine. ";
            case FLIRTY -> "Fishing, are we? ";
            case DREAMY -> "Oh... ";
            case NERVOUS -> "W-what? Um. ";
            case PLAYFUL -> "Hee, wanna guess? No? Okay. ";
            case STOIC -> "...Hm. ";
            case SARCASTIC -> "Oh, we're doing this now? ";
            case ROMANTIC -> "Oh, my favourite question! ";
        };
        String who = switch (attraction) {
            case MEN -> pick(seed, "Men. It's always been men for me.", "I fall for men. Every time.");
            case WOMEN -> pick(seed, "Women. It's always been women for me.", "I fall for women. Every time.");
            case BOTH -> pick(seed, "Men, women... I fall for the person, not the rest.", "Truly? Anyone. It's the heart I fall for.");
        };
        String looks = switch (taste) {
            case STRONG -> "Someone strong enough to keep the night out.";
            case GENTLE -> "Someone gentle, who really listens.";
            case FUNNY -> "Someone who can make me laugh till it hurts.";
            case MYSTERIOUS -> "Someone with a few secrets. I like a puzzle.";
            case CLEVER -> "Someone clever. A sharp mind gets me every time.";
            case DARING -> "Someone a little dangerous, if I'm honest.";
            case LOYAL -> "Someone who'd never leave. Ever.";
            case CARING -> "Someone who'd look after me when I'm sick.";
        };
        return opener + who + " " + looks;
    }

    /** Its kind no to being asked out by someone who isn't who it falls for (only ever {@link Attraction#MEN} or {@link Attraction#WOMEN}). */
    public static String notMyType(Temper temper, Attraction attraction, int seed) {
        String who = who(attraction);
        return switch (temper) {
            case CHEERFUL -> pick(seed, "Aww, I'm so flattered! But I'm only into " + who + ". Friends forever, though?",
                "You're amazing, really! I just only fall for " + who + ". Still my favourite person to talk to!");
            case SHY -> pick(seed, "*goes bright red* I... I'm sorry. I only like " + who + ". ...We can still be friends?",
                "*hides their face* You're so kind... but it's " + who + " for me. I'm sorry.");
            case BOLD -> pick(seed, "Flattered. But I'm into " + who + ", always have been. No hard feelings.",
                "Ha. Wrong door, friend. " + cap(who) + " only. Still got your back, though.");
            case KIND -> pick(seed, "Oh, sweetheart. I'm sorry. My heart's always gone to " + who + ". You still mean a lot to me.",
                "That's lovely of you. But I only fall for " + who + ". Let me be your friend instead?");
            case GRUMPY -> pick(seed, "...You're not my type. " + cap(who) + " are. Don't make it weird. ...We're still fine.",
                "Hmph. " + cap(who) + ". Only " + who + ". ...But you can still come by. If you want.");
            case FLIRTY -> pick(seed, "Mm, tempting. But I only play for " + who + ", darling. Shame.",
                "Oh, you're sweet. But it's " + who + " for me. You can still look, though.");
            case DREAMY -> pick(seed, "In another story, maybe. But my heart has only ever drifted toward " + who + ".",
                "Like the moon and the sea... close, but not that. I only love " + who + ". I'm sorry.");
            case NERVOUS -> pick(seed, "Oh! Oh no. I'm so sorry. I- I like " + who + ". Please don't be mad!",
                "I-I'm flattered! But I only like " + who + ". We're still okay? Please say we're okay.");
            case PLAYFUL -> pick(seed, "Aww, nice try! But I'm only into " + who + ". Still my favourite playmate!",
                "Ha! Wrong team, buddy. " + cap(who) + " only. But you're still fun!");
            case STOIC -> pick(seed, "I'm honoured. But I love " + who + ". That won't change. Friends, if you'll have me.",
                "No. " + cap(who) + ", only. ...But you have my respect. And my friendship.");
            case SARCASTIC -> pick(seed, "Bold. Also: I'm into " + who + ", genius. Still friends, I guess.",
                "Aw, that's cute. Unfortunately for you, it's " + who + " for me. Friends?");
            case ROMANTIC -> pick(seed, "Oh, you're darling. But my heart has only ever sung for " + who + ". Let's be the best of friends?",
                "If only! But I've only ever fallen for " + who + ". You'll find your story, I know it.");
        };
    }

    private static String cap(String word) {
        return Character.toUpperCase(word.charAt(0)) + word.substring(1);
    }

    /**
     * 2.0.35: how much more readily it takes a ghoul's confession for what it looks for: someone mysterious or a little dangerous, a good
     * deal; someone loyal or caring, a little (it sees what they risked to tell it).
     */
    public static double trusts(Taste taste) {
        return switch (taste) {
            case MYSTERIOUS, DARING -> 0.15;
            case LOYAL, CARING -> 0.05;
            default -> 0.0;
        };
    }

    static String pick(int seed, String... lines) {
        return lines[Math.floorMod(seed, lines.length)];
    }
}
