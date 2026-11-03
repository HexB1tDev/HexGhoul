package com.hex.kagune.control.family;

import com.hex.kagune.control.npc.Display;

/**
 * 2.0.5: what someone else's child says to a player who is not its parent (pure, tested), by how grown it is and its temper. The owner: "make
 * sure chat is accurate to the child's age, dont make a toddler/baby be able to say anything coherent": a baby only makes sounds and does
 * things, a toddler babbles a word or two at most; a child chatters about its mom and dad by name; a teen is wary; a grown son or daughter
 * makes polite small talk and mentions its parents. Never flirty, at any age: a grown child is still someone's son or daughter
 * ({@link ChildPage#romantic}, tested). {@code mom}, {@code dad}: its parents' names (empty: unknown).
 */
public final class StrangerLines {
    private StrangerLines() {
    }

    /** As a stranger opens a talk with it. */
    public static String hello(ChildStage stage, ChildTemper temper, int seed) {
        return switch (stage) {
            case BABY -> temper == ChildTemper.SHY ? pick(seed, "*hides its face in its tiny hands*", "*whimpers and looks away*")
                : pick(seed, "*stares at you with big round eyes*", "*grabs at your sleeve*", "*blows a spit bubble at you*");
            case TODDLER -> switch (temper) {
                case SHY, SERIOUS -> pick(seed, "*hides behind its hands*", "*sucks its thumb and stares*");
                case BOLD, SUNNY -> pick(seed, "Hi! Hi!", "*points at you* Dat!");
                case MISCHIEF -> pick(seed, "*giggles and runs off two steps*", "Bah! *giggles*");
                default -> pick(seed, "*stares* ...Hi?", "*tilts its head at you*");
            };
            case CHILD -> switch (temper) {
                case SUNNY -> pick(seed, "Hi! Are you new here?", "Hello! I like your clothes!");
                case SHY -> pick(seed, "...Hi. *looks at its shoes*", "*mumbles* ...Hello.");
                case BOLD -> pick(seed, "Who are you? Do you fight?", "Hey! You're not from around here.");
                case CURIOUS -> pick(seed, "Hello! What's your name? Where do you live?", "Hi! Are you a ghoul or a human? You can tell me.");
                case MISCHIEF -> pick(seed, "Hehe. Hi, stranger.", "*hides something behind its back* ...Hi!");
                case GENTLE -> pick(seed, "Oh, hello! Are you lost?", "Hi! Are you okay? You look tired.");
                case SERIOUS -> pick(seed, "Hello. I'm not supposed to talk to strangers. ...Just a little, then.", "Hello. Who are you here to see?");
            };
            case TEEN -> switch (temper) {
                case SUNNY -> pick(seed, "Oh, hey!", "Hey! What's up?");
                case SHY -> pick(seed, "...Hi.", "Oh. Um. Hi.");
                case BOLD -> pick(seed, "Can I help you?", "Yeah? What do you want?");
                case CURIOUS -> pick(seed, "Hey. Haven't seen you around.", "Hi. You new around here?");
                case MISCHIEF -> pick(seed, "Hey. You lost or something?", "Uh-oh. Am I in trouble? ...Kidding. Hi.");
                case GENTLE -> pick(seed, "Hi. Do you need something?", "Hello. Is everything okay?");
                case SERIOUS -> pick(seed, "Yes? Can I help you?", "Hello. Who are you?");
            };
            case ADULT -> switch (temper) {
                case SUNNY -> pick(seed, "Hello! Nice to meet you.", "Hi there! Lovely day, isn't it?");
                case SHY -> pick(seed, "Oh. Hello.", "Hello. ...Sorry, I'm not good with new people.");
                case BOLD -> pick(seed, "Hello. Can I help you?", "Hello. What brings you here?");
                case CURIOUS -> pick(seed, "Hello. I don't think we've met.", "Hi. Are you a friend of my parents?");
                case MISCHIEF -> pick(seed, "Hi. Looking for my parents? Everyone is.", "Hello! Whatever the little ones told you, it isn't true.");
                case GENTLE -> pick(seed, "Hello. Is everything all right?", "Hi. Can I get you anything?");
                case SERIOUS -> pick(seed, "Good day. Can I help you with something?", "Hello. Who are you here to see?");
            };
        };
    }

    /** "Hello there." / "What's your name?": a little about itself and its parents (a baby, a toddler: sounds and a word). */
    public static String chat(ChildStage stage, ChildTemper temper, String name, String mom, String dad, int seed) {
        String m = mom == null || mom.isEmpty() ? "my mom" : mom;
        String d = dad == null || dad.isEmpty() ? "my dad" : dad;
        return switch (stage) {
            case BABY -> pick(seed, "*babbles at you* Ba! Ba-ba!", "*reaches out and grabs your finger*", "*coos and kicks its feet*",
                "*gums its fist and stares at you*");
            case TODDLER -> temper == ChildTemper.SHY ? pick(seed, "*hides its face* ...Mama?", "*peeks out between its fingers*")
                : pick(seed, "Mama! *points somewhere*", "*holds up two fingers* Dis many!", "Doggy! ...Doggy?", "Bah! *claps*", "*babbles happily at you*");
            case CHILD -> switch (temper) {
                case SHY -> pick(seed, "...I'm " + name + ". My mom is " + m + ".", "*whispers* I'm " + name + ". ...Are you a friend of my dad's?");
                case BOLD -> pick(seed, "I'm " + name + "! My dad " + d + " is the strongest. Probably.", "I'm " + name + ". " + m + " is teaching me to fight. Wanna see? *flexes*");
                case CURIOUS -> pick(seed, "I'm " + name + "! Where are you from? Do you have a house? Is it big?",
                    "My mom is " + m + " and my dad is " + d + ". Do you know them? What are they like when I'm not there?");
                case MISCHIEF -> pick(seed, "I'm " + name + ". If anyone asks, I was never here. Hehe.", "My name's " + name + ". Don't tell " + m + " you saw me by the cookies.");
                case GENTLE -> pick(seed, "I'm " + name + ". Do you want to see a flower I found?", "I'm " + name + ". My mom " + m + " says to be nice to everyone. So hi!");
                case SERIOUS -> pick(seed, "My name is " + name + ". My parents are " + m + " and " + d + ". That's all I'm allowed to say.",
                    "I'm " + name + ". " + d + " says I have to be polite. ...How do you do.");
                default -> pick(seed, "I'm " + name + "! My mom is " + m + " and my dad is " + d + "!", "Hi, I'm " + name + "! " + m + " makes the BEST dinners.");
            };
            case TEEN -> switch (temper) {
                case SHY -> pick(seed, "...I'm " + name + ". Sorry, I'm not good at this.", "I'm " + name + ". " + m + "'s kid. ...Yeah.");
                case BOLD -> pick(seed, name + ". Remember it.", "I'm " + name + ". " + d + "'s kid. So don't try anything.");
                case SERIOUS -> pick(seed, "I'm " + name + ". My parents are " + m + " and " + d + ". You probably know them.", "I'm " + name + ". I train every day. ...What do you do?");
                default -> pick(seed, "I'm " + name + ". " + m + "'s kid. ...Yeah, that " + m + ".", "Not much going on. Just hanging around. " + d + " says I should get out more.");
            };
            case ADULT -> switch (temper) {
                case SHY -> pick(seed, "I'm " + name + ". " + m + " and " + d + " raised me. ...Sorry, that was an odd way to say it.", "I'm " + name + ". It's nice to meet you.");
                case GENTLE -> pick(seed, "I'm " + name + ". It's nice to meet a friend of the family. ...Or a neighbour?", "I'm " + name + ". If you're hungry, " + m + " always cooks too much.");
                case SERIOUS -> pick(seed, "I'm " + name + ". My parents are " + m + " and " + d + ". I help out around home.", "I'm " + name + ". " + d + " taught me everything I know about fighting.");
                default -> pick(seed, "I'm " + name + ". " + m + " and " + d + " still worry about me, honestly. Parents.",
                    "I'm " + name + ". If you're looking for " + m + ", I can pass a message on.", "Nice weather. " + d + " would say it's perfect for training.");
            };
        };
    }

    /** It waves back (a baby claps) as the talk closes. */
    public static String wave(ChildStage stage, ChildTemper temper, int seed) {
        return switch (stage) {
            case BABY -> pick(seed, "*flaps its tiny hands*", "*claps, delighted*");
            case TODDLER -> temper == ChildTemper.SHY ? "*waves, tiny, from behind its hand*" : pick(seed, "Bye-bye! *waves its whole arm*", "*waves both hands*");
            case CHILD -> temper == ChildTemper.SHY ? "*a small wave*" : pick(seed, "*waves* Hi hi!", "*waves with its whole arm*");
            case TEEN -> pick(seed, "*nods at you*", "*a little wave* Hey.");
            case ADULT -> pick(seed, "*waves back with a smile*", "*a friendly wave*");
        };
    }

    /** True if it won't high-five a stranger (a shy child hides its hands). */
    public static boolean shyOfHands(ChildStage stage, ChildTemper temper) {
        return stage == ChildStage.CHILD && temper == ChildTemper.SHY;
    }

    /** A stranger's high five: its eager word (it happens as the talk closes), or a shy one's no. */
    public static String five(ChildStage stage, ChildTemper temper, int seed) {
        if (shyOfHands(stage, temper)) {
            return pick(seed, "*hides its hands behind its back* ...Maybe later.", "*shakes its head and looks down*");
        }
        return switch (stage) {
            case BABY -> "*grabs your fingers*";
            case TODDLER -> "*slaps your hand* Hehe!";
            case CHILD -> temper == ChildTemper.BOLD ? "Up top! Hit it HARD!" : pick(seed, "Okay! Ready?", "High five! Yes!");
            case TEEN -> pick(seed, "...Fine. Up top.", "Sure. Don't leave me hanging.");
            case ADULT -> pick(seed, "Ha. Sure.", "Go on, then.");
        };
    }

    /** As a moment with a stranger begins (its own word in a bubble). */
    public static String moment(ChildMoment moment, ChildStage stage, int seed) {
        return switch (moment) {
            case WAVE -> stage == ChildStage.TODDLER ? "Bye-bye!" : stage.older() ? "*waves*" : pick(seed, "*waves* Bye!", "*waves and waves*");
            case CLAP -> "*claps its tiny hands*";
            case HIGH_FIVE -> stage.older() ? pick(seed, "Nice.", "Good one.") : pick(seed, "Yeah! *giggles*", "Ow! Hehe.");
            default -> "";
        };
    }

    /**
     * Offered food by a stranger: little ones won't take it (a toddler just shakes its head); a teen or a grown-up takes it politely
     * ({@code eats}: food it can eat) or says it can't.
     */
    public static String treat(ChildStage stage, ChildTemper temper, boolean eats, int seed) {
        return switch (stage) {
            case BABY -> "*turns its face away*";
            case TODDLER -> temper == ChildTemper.MISCHIEF ? "*reaches for it... then hides its hands* No no!" : pick(seed, "No! *shakes its head hard*", "*hides its hands* Mama no.");
            case CHILD -> temper == ChildTemper.MISCHIEF ? "*looks at it, looks at you* ...Nope. Mom would know."
                : pick(seed, "My mom says I can't take food from strangers. ...Sorry.", "No thank you. I'm not allowed.");
            case TEEN -> eats ? pick(seed, "Oh. Uh, thanks. I'll have it later.", "Free food? ...Thanks.") : pick(seed, "Thanks, but I can't eat that.", "Uh, I can't eat that. But thanks.");
            case ADULT -> eats ? pick(seed, "That's kind of you. Thank you.", "Oh, thank you. I'll save it for later.") : pick(seed, "That's kind, but I can't eat that.", "Thank you, really, but that's not something I can eat.");
        };
    }

    /**
     * "Where are your parents?": {@code near}: the nearer parent is close by ({@code mother} which one, {@code who} their name); else
     * they are away. A baby and a toddler only look or point.
     */
    public static String parents(ChildStage stage, ChildTemper temper, boolean near, boolean mother, String who, int seed) {
        String call = ChildLines.call(stage, mother);
        String parent = who == null || who.isEmpty() ? (mother ? "my mom" : "my dad") : who;
        if (near) {
            return switch (stage) {
                case BABY -> "*turns its head toward " + parent + "*";
                case TODDLER -> pick(seed, "*points* " + call + "!", call + "! *points and bounces*");
                case CHILD -> pick(seed, "*points* That's my " + (mother ? "mom" : "dad") + ", " + parent + "! Over there!", "*points* Right there! " + call + "! Someone's here!");
                case TEEN -> pick(seed, "*points over its shoulder* " + parent + "'s right there. Go say hi.", "*points* Over there. Try not to be weird.");
                case ADULT -> pick(seed, "*points* " + parent + "'s just over there.", "My " + (mother ? "mother" : "father") + "? Right over there. *points*");
            };
        }
        return switch (stage) {
            case BABY -> "*looks around for someone who isn't there*";
            case TODDLER -> temper == ChildTemper.SHY ? "*lip wobbles* ...Mama?" : pick(seed, "Gone. *points at nothing*", "Mama... bye-bye.");
            case CHILD -> pick(seed, "They're out. They always come back, though!", "Mom and Dad are busy. I'm being good till they're back.");
            case TEEN -> pick(seed, "Out. Somewhere. They don't tell me everything.", "Not here. Want me to tell them you came by?");
            case ADULT -> pick(seed, "Out, I think. I can tell them you stopped by.", "They're away right now. Can I pass on a message?");
        };
    }

    /** A stranger shows their kakugan, kagune, a kakuja or worse: its first word (a baby, a toddler: a cry, a word). */
    public static String scared(ChildStage stage, ChildTemper temper, Display shown, int seed) {
        boolean worst = shown.frightening();
        return switch (stage) {
            case BABY -> worst ? "*wails*" : shown == Display.KAGUNE ? "*bursts into tears*" : "*stares at your eyes, then whimpers*";
            case TODDLER -> worst ? "*screams* MAMAAA!" : shown == Display.KAGUNE ? "*shrieks and runs*" : pick(seed, "*backs away* ...Eyes. Red.", "*lip wobbles*");
            case CHILD -> worst ? pick(seed, "MOM! MOM, HELP!", "*screams and runs*")
                : shown == Display.KAGUNE ? temper == ChildTemper.BOLD ? "I'm not scared of you! ...I'm a little scared of you." : pick(seed, "Stay away! I'm getting my mom!", "*runs*")
                : pick(seed, "W-why are your eyes like that?", "Your eyes went red... Are you hungry?");
            case TEEN -> worst ? pick(seed, "...Run. Everyone, run!", "What ARE you?! Get back!") : shown == Display.KAGUNE ? pick(seed, "Whoa. Back off. I mean it.", "Put that away. Now.")
                : pick(seed, "...Put the eyes away. You're scaring the little ones.", "Red eyes. Great. Just what we needed.");
            case ADULT -> worst ? pick(seed, "Everyone get back! Now!", "Stay away from this house!") : shown == Display.KAGUNE ? pick(seed, "Put that away. This is a family's home.", "Easy. Nobody here wants a fight.")
                : pick(seed, "Easy. There are children around here.", "Your eyes. ...Calm down, all right?");
        };
    }

    /** Hiding from a frightening stranger (behind {@code who}, its parent's call; empty: on its own). */
    public static String hiding(ChildStage stage, String who, int seed) {
        boolean behind = who != null && !who.isEmpty();
        return switch (stage) {
            case BABY -> pick(seed, "*whimpers*", "*cries quietly*");
            case TODDLER -> behind ? pick(seed, "*clings to " + who + "'s legs*", "*peeks out* ...Bad.") : pick(seed, "*hides its face*", "*sniffles*");
            case CHILD -> behind ? pick(seed, "*peeks out from behind " + who + "*", "*whispers* " + who + ", make them go away...") : pick(seed, "*hides and peeks out*", "Go away...");
            case TEEN -> pick(seed, "*keeps its distance, watching you*", "I'm watching you.");
            case ADULT -> pick(seed, "*keeps between you and the little ones*", "Don't come any closer.");
        };
    }

    /** Which way {@code dx}, {@code dz} lies (north is -z): "north", "south-east"... */
    public static String compass(double dx, double dz) {
        String[] winds = {"north", "north-east", "east", "south-east", "south", "south-west", "west", "north-west"};
        double angle = Math.toDegrees(Math.atan2(dx, -dz));
        return winds[Math.floorMod((int)Math.round(angle / 45.0), 8)];
    }

    static String pick(int seed, String... lines) {
        return lines[Math.floorMod(seed, lines.length)];
    }
}
