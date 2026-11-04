package com.hex.kagune.control.village;

import com.hex.kagune.control.village.HumanPersona.Temper;
import java.util.ArrayList;
import java.util.List;

/**
 * 2.1.3 (pure, tested): how a village human says its life ({@link HumanLife}) in its temper's voice: the "Talk a while." page's topics
 * (what it does, its family, what it does for fun, what scares it, what it dreams of, its odd habit, its secret, its favourite thing, what
 * it thinks of ghouls, the village's gossip, who it falls for), how its day is going, and how it takes a gift. What it says is its life's;
 * the temper puts the words around it, so two shy bakers still say different things, and a shy baker and a bold one say the same thing
 * differently.
 */
public final class HumanVoice {
    private HumanVoice() {
    }

    /** What can be talked about, the label it has on the page, and how warm it must be first (regard). */
    public enum Topic {
        WORK("What do you do here?", 0),
        FAMILY("Tell me about your family.", 0),
        HOBBY("What do you do for fun?", 0),
        QUIRK("What's that you're doing?", 0),
        GHOULS("What do you think of ghouls?", 0),
        GOSSIP("Heard any gossip?", 0),
        FAVOURITE("What's your favourite thing?", HumanPersona.WARM),
        TYPE("What's your type?", HumanPersona.WARM),
        FEAR("What scares you?", HumanPersona.WARM),
        DREAM("What do you dream of?", HumanPersona.CLOSE),
        SECRET("Tell me a secret.", 80);

        public final String label;
        public final int warmth;

        Topic(String label, int warmth) {
            this.label = label;
            this.warmth = warmth;
        }

        /** The bond flag that marks it learned (2.1.3: a human's flags past CONFESSED, PARTNER and NOT_TYPE). */
        public int flag() {
            return 128 << this.ordinal();
        }

        /** True if learning it is getting to know them (paid once: {@link #LEARNED}). */
        public boolean personal() {
            return this != GOSSIP && this != TYPE;
        }
    }

    /** What it says as the "Talk a while." page opens. */
    public static final String[] TOPICS_ASKED = {"Sure. What do you want to know?", "Talk? Alright. Ask away.", "Oh? What about?",
        "I've got time. What's on your mind?", "Go on, then. Ask me anything."};

    /** Regard the first time each personal thing is learned about them. */
    public static final int LEARNED = 2;
    /** At most this many topics on the page. */
    public static final int ROOM = 7;

    /**
     * The topics on the page for someone at {@code regard} ({@code partner}: together; {@code learned}: the bond's flags), at most
     * {@link #ROOM}: what they haven't learned yet first, then the rest by turns ({@code seed}).
     */
    public static List<Topic> topics(int regard, boolean partner, int learned, int seed) {
        List<Topic> fresh = new ArrayList<>();
        List<Topic> known = new ArrayList<>();
        for (Topic topic : Topic.values()) {
            if (regard < topic.warmth && !partner) {
                continue;
            }
            (topic.personal() && (learned & topic.flag()) == 0 ? fresh : known).add(topic);
        }
        List<Topic> out = new ArrayList<>(fresh);
        int start = known.isEmpty() ? 0 : Math.floorMod(seed, known.size());
        for (int i = 0; i < known.size() && out.size() < ROOM; i++) {
            out.add(known.get((start + i) % known.size()));
        }
        return out.size() > ROOM ? out.subList(0, ROOM) : out;
    }

    // ---------------------------------------------------------------- its voice around its words

    /** What it says before its words on {@code topic}. */
    static String before(Temper temper, Topic topic, int seed) {
        boolean deep = topic == Topic.FEAR || topic == Topic.DREAM || topic == Topic.SECRET;
        if (deep) {
            return switch (temper) {
                case CHEERFUL -> pick(seed, "*the smile slips, just for a second* ...", "Oh. A real question. Okay. ", "*quietly, for once* ");
                case SHY -> pick(seed, "*looks around first, then very quietly* ...", "*fiddles with a sleeve for a long time* ...", "*whispers* ");
                case BOLD -> pick(seed, "Straight answer, since it's you. ", "*crosses their arms, then uncrosses them* Fine. ", "I don't tell people this. ");
                case KIND -> pick(seed, "*takes your hand for a moment* ", "Since you asked so gently... ", "*smiles, a little sadly* ");
                case GRUMPY -> pick(seed, "...Why do you want to know? *sighs* Fine. ", "If you laugh, we're done. ", "*grumbles, then goes quiet* ...");
                case FLIRTY -> pick(seed, "Ooh, going deep? *the grin fades* ...Okay. Real answer. ", "*for once, doesn't joke* ", "You'd have to be special to hear this. You are. ");
                case DREAMY -> pick(seed, "*looks up at the sky for a long time* ...", "Hmm... ", "I've never said this out loud. ");
                case NERVOUS -> pick(seed, "*takes a big breath* Okay. Okay. ", "P-promise you won't tell? ...Okay. ", "*voice wobbling* ");
                case PLAYFUL -> pick(seed, "Ha, okay, serious mode. *clears throat* ", "*stops bouncing* ...Real answer? ", "Pinky swear first. *hooks your finger* Okay. ");
                case STOIC -> pick(seed, "*a long silence* ", "...I'll tell you. Once. ", "*looks you in the eye* ");
                case SARCASTIC -> pick(seed, "Oh, the deep stuff. Great. *drops the act* ...", "No jokes for this one. ", "*sighs* You would ask that. ");
                case ROMANTIC -> pick(seed, "*hand on heart* Only for you. ", "Come closer. This is for your ears only. ", "*softly* ");
            };
        }
        return switch (temper) {
            case CHEERFUL -> pick(seed, "Oh, I love this question! ", "Ha! Okay, so... ", "", "Ooh! ");
            case SHY -> pick(seed, "Um... ", "*quietly* ", "Oh. Well... ", "");
            case BOLD -> pick(seed, "", "Simple. ", "Here's the thing. ", "Ha. ");
            case KIND -> pick(seed, "", "Oh, that's sweet of you to ask. ", "Well, dear... ", "Mm. ");
            case GRUMPY -> pick(seed, "Ugh. ", "*sighs* ", "", "If you must know... ");
            case FLIRTY -> pick(seed, "Getting to know me? I like that. ", "Mm, curious. ", "", "For you? Anything. ");
            case DREAMY -> pick(seed, "Hm? Oh... ", "", "*drifts back from somewhere* ", "Let me think... ");
            case NERVOUS -> pick(seed, "Oh! Um. ", "W-well... ", "", "Okay, so, um... ");
            case PLAYFUL -> pick(seed, "Ooh, a quiz! ", "", "Guess! No? Fine. ", "Ha, okay! ");
            case STOIC -> pick(seed, "", "Hm. ", "*nods* ", "");
            case SARCASTIC -> pick(seed, "Oh, riveting question. ", "", "You really want to know? Wow. ", "Brace yourself. ");
            case ROMANTIC -> pick(seed, "Oh, a question about me! ", "", "How lovely of you to ask. ", "Mm... ");
        };
    }

    /** What it says after its words (often nothing). */
    static String after(Temper temper, Topic topic, int seed) {
        if (Math.floorMod(seed >> 3, 3) != 0) {
            return "";
        }
        return switch (temper) {
            case CHEERFUL -> pick(seed, " Anyway! Your turn!", " Isn't life funny?");
            case SHY -> pick(seed, " ...Sorry. I talked too much.", " ...Thank you for listening.");
            case BOLD -> pick(seed, " That's me.", " Any questions?");
            case KIND -> pick(seed, " But enough about me. How are you, really?", " Thank you for asking. People don't.");
            case GRUMPY -> pick(seed, " Happy now?", " Don't spread it around.");
            case FLIRTY -> pick(seed, " Your turn. Tell me something about you.", " Now you know me a little better. Want more?");
            case DREAMY -> pick(seed, " ...Where was I?", " Funny, the things that stay with you.");
            case NERVOUS -> pick(seed, " Was that too much? That was too much.", " Sorry. Sorry. I'm done.");
            case PLAYFUL -> pick(seed, " Bet you didn't see that coming!", " Your turn! Tell me something weird.");
            case STOIC -> pick(seed, " That's all.", " Enough said.");
            case SARCASTIC -> pick(seed, " Thrilling, I know.", " Try not to faint.");
            case ROMANTIC -> pick(seed, " Now you know a little more of my story.", " I feel like I could tell you anything.");
        };
    }

    /** Its answer on {@code topic}: its life's words in its temper's voice ({@code gossip}: a word about a neighbour, if there is one). */
    public static String answer(Temper temper, HumanLife.Life life, Topic topic, int seed, String gossip) {
        String said = switch (topic) {
            case WORK -> life.job().work(seed);
            case FAMILY -> life.family();
            case HOBBY -> life.hobby();
            case QUIRK -> life.quirkSays();
            case GHOULS -> life.view().say(seed);
            case GOSSIP -> gossip != null ? gossip : news(seed);
            case FAVOURITE -> life.favouriteSays();
            case FEAR -> life.fear();
            case DREAM -> life.dream();
            case SECRET -> life.secret();
            case TYPE -> "";
        };
        return before(temper, topic, seed) + said + after(temper, topic, seed >> 1);
    }

    // ---------------------------------------------------------------- how it is, the news, the neighbours

    /** "How are you?": its temper's small talk, or how its day at work went. */
    public static String chat(Temper temper, HumanLife.Life life, int seed) {
        if (Math.floorMod(seed >> 2, 5) < 2) {
            return before(temper, Topic.WORK, seed) + life.job().day(seed >> 4);
        }
        return pick(seed, CHAT[temper.ordinal()]);
    }

    /** Each temper's small talk (2.1.3: five each; in temper order). */
    static final String[][] CHAT = {
        {"I'm great! The bakery had fresh bread this morning.", "Wonderful! I've been humming all day.", "Fantastic! The sun's out and nobody's been eaten. Best kind of day!",
            "So good! I helped a kid find their dog. The dog was very grateful. So was the kid.", "Amazing! I tried a new recipe. It exploded. Still amazing!"},
        {"I'm... fine. Thank you for asking. People don't, usually.", "Okay, I think. Better now.", "*nods* Good. Quiet. I like quiet days.",
            "I'm alright. I found a nice spot by the wall to sit. Nobody bothers me there.", "Fine. Um. You? ...Sorry. I'm bad at this part."},
        {"Bored. Something should happen around here.", "Strong as ever. Want to arm wrestle?", "Restless. If I don't do something exciting soon I'll fight the rooster.",
            "Good. Won an argument with the elder. He'll be sulking for days.", "Fine. Hungry. Bored. Mostly bored."},
        {"I'm well. The neighbour's child was sick, but she's better now.", "Busy, but good. How are YOU?", "Tired, but the good kind. I spent the day helping people.",
            "I'm alright, dear. Have you eaten? You look thin.", "Better for seeing you. That's not flattery, it's true."},
        {"My back hurts and the rooster won't shut up. Next.", "Same as yesterday. Worse, probably.", "Terrible. Someone whistled near my window at dawn. Whistled.",
            "Fine. Don't make me say it twice.", "The weather's wrong, the bread's stale, and people keep asking me how I am."},
        {"Better now that you asked.", "Lonely. Not that you'd do anything about it. ...Would you?", "Oh, I'm wonderful. Especially from this angle.",
            "Bored, until about ten seconds ago. Funny, that.", "Mm, I'll be better if you stay a while."},
        {"I counted the stars last night. I lost count at the pretty ones.", "Floating. In a good way.", "I had a dream about a whale in the sky. I'm still thinking about it.",
            "The clouds this morning looked like a ship. I watched it sail all the way over the hills.", "Oh, I'm alright. The wind's in a good mood today."},
        {"Fine! Fine. I heard a noise last night. It was probably a cat.", "Okay as long as the CCG walks by.", "I'm good! I think. Is something wrong? Is it me?",
            "I checked my lock four times today. That's down from six. Progress!", "I'm okay! Mostly. There was a spider. It's gone now. I think."},
        {"I put a frog in the baker's flour sack. Best morning ever.", "Bored! Play a game with me. Any game.", "I'm great! I've been hiding from my chores since breakfast.",
            "I taught the goat a trick. It's not a good trick. It's a trick.", "Fantastic! I beat the kids at tag. They're seven. Still counts."},
        {"Well enough. The wind is from the east.", "Steady. As always.", "Well. The day's work is done.", "*shrugs* Alive. That's enough.",
            "Good. Nothing happened. That's how I like it."},
        {"Living the dream. The dream is mostly chores.", "Fantastic. The rooster and I are best friends now.", "Oh, I'm thriving. Can't you tell from my face?",
            "Great. Someone asked me to smile more. I'm still recovering.", "Never better. Which isn't saying much, but there it is."},
        {"I'm reading a love story. The heroine's an idiot. I adore her.", "Wonderful! Someone left a rose on my step. Probably the wind. Probably.",
            "Dreamy. I saw two old people holding hands at the well. I nearly cried.", "Oh, I'm well! The evening light's so golden today. Perfect for a walk with someone.",
            "Lovestruck. With the sunset. It's a one-sided relationship."}};

    /** The village's news (2.1.3: twenty-four). */
    static final String[] NEWS = {"The CCG doubled the patrols. Something must have happened.", "Old Taka swears he saw red eyes in the field last night.",
        "There's going to be a market day soon. Bring coin!", "Someone left flowers on every doorstep. Nobody knows who.",
        "A family moved out. Said the nights got too long.", "The bell rang at midnight. Nobody admits ringing it.",
        "They say a ghoul can look just like us. Just like us.", "The baker's daughter is getting married!",
        "An investigator came through with a case as big as a coffin. Didn't say a word to anyone.", "The well water tasted of iron this week. Nobody's saying why.",
        "Someone's been stealing eggs. The elder's furious. Suspiciously furious.", "There'll be a festival when the harvest's in. Masks and lanterns and dancing.",
        "A traveller said the next village emptied in one night. Everyone just... gone.", "The children found a mask in the woods. The CCG took it away.",
        "Two of the night watch quit. Nobody blames them.", "The old mill wheel stopped for an hour yesterday. Something was caught in it.",
        "Someone saw a girl with a book walking the wall at three in the morning. Smiling.", "The CCG put up new posters. The faces on them look like anybody.",
        "A dog's been following the gravedigger everywhere. He pretends to hate it. He feeds it.", "Fog's coming in from the river every night now. Thick as milk.",
        "There's a new coffee shop rumour: a cafe in the city where the waitress kicks like a horse.", "The elder's nephew proposed at the well. She said maybe.",
        "Lanterns went missing from the gate. The watch blames the wind. The wind doesn't carry lanterns.",
        "A merchant's selling 'ghoul-proof' charms. They're just rocks. People are buying them."};

    public static String news(int seed) {
        return pick(seed, NEWS);
    }

    /** A word about a neighbour, {@code other} the {@code job} (twenty templates; "{other}" and "{job}" put in). */
    static final String[] GOSSIP = {"Have you met {other}? The {job}. Sweetest person in the village. Don't tell them I said so.",
        "{other} the {job} has been humming all week. Someone's in love.", "Between you and me, {other} the {job} hasn't slept properly in days.",
        "{other}? The {job}? We grew up together. They still owe me a sweet from when we were nine.",
        "I saw {other} the {job} crying by the well yesterday. I didn't ask. Maybe you could.",
        "{other} the {job} says they've seen something in the woods. Nobody believes them. I half do.",
        "Don't buy anything from {other} the {job} on a rainy day. Grumpiest person alive when it rains.",
        "{other} the {job} is saving up for something. Won't say what. I think a ring.", "If you need anything mended, ask {other}. The {job}. Best hands in the valley.",
        "{other} the {job} and I had a fight. Over nothing. Over a fence. It's not about the fence.",
        "{other} the {job} makes the best tea in the village. Not their job. Just a gift.", "Watch out for {other} the {job}. They talk. I mean they TALK.",
        "{other} the {job} has a crush on someone. Everyone knows but them.", "I've never once seen {other} the {job} eat. Not once. Isn't that funny? ...Isn't it?",
        "{other} the {job} lost someone last winter. Be gentle with them.", "{other} the {job} once fought off a wolf with a broom. True story. Ask them.",
        "My mother wants me to marry {other} the {job}. I'd rather marry the well.", "{other} the {job} keeps a light on all night. Every night. Afraid of something.",
        "{other}, the {job}? Kindest soul here. They'd give you the shirt off their back and apologise it wasn't warmer.",
        "I think {other} the {job} is lonely. Go and say hello sometime."};

    /** Gossip about {@code other} the {@code job}. */
    public static String gossip(String other, String job, int seed) {
        return pick(seed, GOSSIP).replace("{other}", other).replace("{job}", job);
    }

    /** 2.1.3: what it's caught doing as the talk opens, now and then (its odd habit), before its hello; else "". */
    public static String caught(HumanLife.Life life, int seed) {
        return Math.floorMod(seed >> 5, 4) == 0 ? life.quirkDoing() + " ...Oh! " : "";
    }

    // ---------------------------------------------------------------- gifts

    /** How it takes a gift ({@code thing}: what it's called; {@code again}: it had one from them today already). */
    public static String gift(Temper temper, HumanLife.Gift how, String thing, boolean again, int seed) {
        String line = giftLine(temper, how, again, seed).replace("{thing}", thing);
        return line.isEmpty() ? line : Character.toUpperCase(line.charAt(0)) + line.substring(1);
    }

    private static String giftLine(Temper temper, HumanLife.Gift how, boolean again, int seed) {
        if (again && how != HumanLife.Gift.DISLIKED) {
            return switch (temper) {
                case CHEERFUL, PLAYFUL -> "Another one? You're spoiling me! Save some for tomorrow!";
                case SHY, NERVOUS -> "*holds it, bright red* ...You already gave me something today...";
                case GRUMPY, SARCASTIC -> "Twice in a day? People will talk. ...Fine. Thanks.";
                case STOIC, BOLD -> "Another? ...Thank you. Once a day is plenty.";
                default -> "You're too kind. One a day is more than enough, really.";
            };
        }
        return switch (how) {
            case LOVED -> switch (temper) {
                case CHEERFUL -> pick(seed, "{thing}! How did you know?! I love you! I mean— I LOVE it!", "*squeals* {thing}! My favourite! You're the best!");
                case SHY -> pick(seed, "*stares at it* ...{thing}. You remembered. *hugs it to their chest*", "*eyes go wide* ...How did you know? Thank you. Really.");
                case BOLD -> pick(seed, "{thing}? Now you're just showing off. ...I love it.", "You know me too well. That's dangerous. Thank you.");
                case KIND -> pick(seed, "Oh, {thing}... you listened. Nobody listens. Thank you, dear.", "*eyes shine* You remembered. That's the real gift.");
                case GRUMPY -> pick(seed, "...{thing}. You remembered. *clears throat* I'm not crying. It's dusty.", "Hmph. {thing}. ...Fine. You're my favourite. Don't tell anyone.");
                case FLIRTY -> pick(seed, "{thing}? Oh, you're trying to win me over. ...It's working.", "Mm, you know exactly what I like. What else do you know?");
                case DREAMY -> pick(seed, "{thing}... it's like you reached into my head. Thank you.", "*holds it up to the light* ...I'll keep it forever.");
                case NERVOUS -> pick(seed, "F-for me? {thing}?! I- thank you! Sorry! Thank you!", "Oh no, I'm going to cry. {thing}! Thank you!");
                case PLAYFUL -> pick(seed, "{thing}! You win! You win the whole game! What game? All of them!", "*does a little spin* {thing}! Best day ever!");
                case STOIC -> pick(seed, "*a long pause* ...{thing}. You remembered. Thank you.", "*actually smiles* ...This means a great deal.");
                case SARCASTIC -> pick(seed, "Oh, great, {thing}, my favourite— ...wait, it IS my favourite. Huh. Thanks. Really.", "Okay, you got me. That's... really nice.");
                case ROMANTIC -> pick(seed, "{thing}! *clutches it to heart* Nobody's ever... oh, you!", "This is the most romantic thing anyone's done for me this year.");
            };
            case LIKED -> switch (temper) {
                case CHEERFUL -> pick(seed, "Ooh, for me? Thank you!", "Aww, how nice! Thank you!");
                case SHY -> pick(seed, "*takes it carefully* ...Thank you.", "Oh! For me? ...Thank you.");
                case BOLD -> pick(seed, "Nice. Thanks.", "Good taste. I'll take it.");
                case KIND -> pick(seed, "Oh, that's lovely. Thank you, dear.", "You didn't have to. Thank you.");
                case GRUMPY -> pick(seed, "...Hm. It's fine. Thanks.", "What's this for? ...Fine. Thanks.");
                case FLIRTY -> pick(seed, "A gift? You're sweet on me.", "Mm, thank you. I'll think of you when I use it.");
                case DREAMY -> pick(seed, "Oh, how pretty. Thank you.", "It's lovely. Like a little present from the sky.");
                case NERVOUS -> pick(seed, "Oh! For me? Thank you! Sorry. Thank you!", "Is this— for me? Oh. Thank you!");
                case PLAYFUL -> pick(seed, "A present! Is it a trick? It's not? Yay!", "Ooh, gimme! Thanks!");
                case STOIC -> pick(seed, "Thank you.", "*nods* Kind of you.");
                case SARCASTIC -> pick(seed, "A gift. For me. How unexpected. ...Thanks.", "Oh, nice. Not bad at all.");
                case ROMANTIC -> pick(seed, "A gift! *holds it close* Thank you!", "How thoughtful. I'll treasure it.");
            };
            case FINE -> switch (temper) {
                case CHEERFUL -> pick(seed, "Oh! Um. Thanks! I'll find a use for it!", "Haha, okay! Thank you!");
                case SHY -> pick(seed, "*looks at it, puzzled* ...Thank you?", "Oh... okay. Thank you.");
                case BOLD -> pick(seed, "...What am I supposed to do with this?", "Odd gift. Thanks, I guess.");
                case KIND -> pick(seed, "Oh, how... practical. Thank you, dear.", "That's very kind. I'm sure it'll come in handy.");
                case GRUMPY -> pick(seed, "Why would I want this?", "...Is this a joke? Fine.");
                case FLIRTY -> pick(seed, "Hm. Not flowers, but I'll take it.", "Interesting choice. You're a mystery.");
                case DREAMY -> pick(seed, "Oh... I'll find something it's good for. Everything's good for something.", "How curious. Thank you.");
                case NERVOUS -> pick(seed, "Oh! Thank you! What is it for? Sorry! Thank you!", "Um. Thanks! I'll put it somewhere safe.");
                case PLAYFUL -> pick(seed, "Ha, what IS this? I love random stuff!", "Weird gift! Ten points!");
                case STOIC -> pick(seed, "...Thank you.", "*turns it over* Useful. Thanks.");
                case SARCASTIC -> pick(seed, "Wow. Just what I always wanted. Truly.", "Oh, a... thing. Thanks.");
                case ROMANTIC -> pick(seed, "Every gift means something. I'll work out what this one means!", "Thank you! It's the thought that counts.");
            };
            case DISLIKED -> switch (temper) {
                case CHEERFUL -> pick(seed, "Oh! Ew. Is this a prank? Haha... please take it back.", "Um. That's... thank you? No. No, I can't.");
                case SHY -> pick(seed, "*goes pale and holds it at arm's length*", "...Why would you give me this?");
                case BOLD -> pick(seed, "What is this? Is this an insult?", "Try that again and I'll throw it at you.");
                case KIND -> pick(seed, "Oh, dear. I don't think I want this. But thank you for thinking of me.", "That's... not very nice. Are you alright?");
                case GRUMPY -> pick(seed, "Get that away from me.", "Is this what you think of me? Charming.");
                case FLIRTY -> pick(seed, "Wow. That's a mood-killer.", "Is this how you court people? Rough.");
                case DREAMY -> pick(seed, "Oh. That's... a sad thing to give someone.", "*holds it gingerly* ...Why?");
                case NERVOUS -> pick(seed, "*shrieks* Why?! Why would you?!", "I- I don't- please take it back!");
                case PLAYFUL -> pick(seed, "Gross! You lose ten points!", "Is this a dare? I'm not eating it.");
                case STOIC -> pick(seed, "No.", "*hands it straight back*");
                case SARCASTIC -> pick(seed, "Oh wow. You shouldn't have. Really. You shouldn't have.", "Truly the gift of the year.");
                case ROMANTIC -> pick(seed, "*gasps* That's not romantic at all!", "My heart just broke a little. Why this?");
            };
        };
    }

    static String pick(int seed, String... lines) {
        return lines[Math.floorMod(seed, lines.length)];
    }
}
