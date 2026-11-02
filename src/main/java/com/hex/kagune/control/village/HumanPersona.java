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

    static String pick(int seed, String... lines) {
        return lines[Math.floorMod(seed, lines.length)];
    }
}
