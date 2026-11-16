package com.hex.kagune.control.family;

/**
 * 1.3.10: what a child says (pure), by how grown it is: a baby babbles and giggles, a toddler wants up and points at everything, a child asks
 * to explore and to be taught, a teen is too cool for most of it and still glad you're there. {@code parent}: what it calls the one it
 * talks to ("Mama", "Papa", or their name for a teen). A child's words only.
 */
public final class ChildLines {
    /** What it calls a parent: a little one "Mama" or "Papa", a teen too ("Mom", "Dad"). */
    public static String call(ChildStage stage, boolean mother) {
        if (stage.older()) {
            return mother ? "Mom" : "Dad";
        }
        return mother ? "Mama" : "Papa";
    }

    private ChildLines() {
    }

    /** Unprompted, near a parent now and then. */
    public static String idle(ChildStage stage, String parent, int seed) {
        return switch (stage) {
            case BABY -> pick(seed, "*babbles* Ba... ba-ba!", "*giggles*", "*reaches for you with both hands*", "Da! ...Da!", "*yawns, tiny*");
            case TODDLER -> pick(seed, parent + "! " + parent + ", look!", "Up! Up up up!", "Bird! ...Big bird!", "I runned fast! Did you see?",
                "Why is the sky? ...Why?");
            case CHILD -> pick(seed, "Can we go exploring? Please?", parent + ", teach me something cool.", "I found a weird rock. It's mine now.",
                "When I grow up I'm gonna be strong like you.", "Do you think monsters are scared of us?");
            case TEEN, ADULT -> pick(seed, "...What? I'm just standing here.", "Can I go out tonight? I'll be careful. Mostly.",
                "You're kind of embarrassing, " + parent + ". ...I'm glad you're here, though.", "I've been practising. Wanna see?",
                "I'm fine. Really. ...Okay, maybe a hug.");
        };
    }

    /** As a parent comes up to it. */
    public static String greet(ChildStage stage, String parent, int seed) {
        return switch (stage) {
            case BABY -> pick(seed, "*kicks happily*", "*squeals*", "*holds out tiny arms*");
            case TODDLER -> pick(seed, parent + "!", parent + "! You came back!", "*runs at your legs and holds on*");
            case CHILD -> pick(seed, "Hi " + parent + "!", parent + "! Guess what I did today!", "You're back! Can we play?");
            case TEEN, ADULT -> pick(seed, "Hey.", "Oh. Hey, " + parent + ".", "...Hi. Took you long enough.");
        };
    }

    public static String up(ChildStage stage, int seed) {
        return stage == ChildStage.BABY ? pick(seed, "*snuggles into you*", "*giggles and grabs your nose*", "*falls asleep almost at once*")
            : pick(seed, "Up! Yay!", "Higher! ...No, not that high!", "I can see everything from here!");
    }

    public static String down(ChildStage stage, int seed) {
        return stage == ChildStage.BABY ? pick(seed, "*fusses, then settles*", "*blinks up at you*") : pick(seed, "Again! Again!", "Okay... bye up.");
    }

    public static String play(ChildStage stage, int seed) {
        return switch (stage) {
            case BABY -> pick(seed, "*shrieks with laughter*", "*grabs your finger and won't let go*", "Peek... boo! *giggles*");
            case TODDLER -> pick(seed, "Again! Do it again!", "You can't catch me! ...You caught me!", "*spins until it falls over, laughing*");
            case CHILD -> pick(seed, "Tag, you're it!", "I win! I always win!", "Let's play hide and seek. You count. No peeking!");
            case TEEN, ADULT -> pick(seed, "Fine. One round. ...Okay, two.", "You're slow. ...That was fun.", "Don't tell anyone I still like this.");
        };
    }

    /** A lesson given (its skill now), or why not. */
    public static String lesson(ChildStage stage, int skill, int seed) {
        if (!stage.learns()) {
            return stage == ChildStage.BABY ? "*tries to eat your sleeve*" : "*punches your knee, very proudly*";
        }
        return switch (Math.min(skill, 5)) {
            case 1 -> pick(seed, "Like this? Fists up?", "Okay. Feet apart. Got it.");
            case 2 -> pick(seed, "I'm getting faster!", "Hit, step, hit. Hit, step, hit.");
            case 3 -> pick(seed, "Watch this! ...Did you see that?", "I'm not scared of zombies anymore.");
            case 4 -> pick(seed, "I can keep up with you now. Almost.", "One day I'm gonna beat you.");
            default -> pick(seed, "There's nothing left you can teach me. ...Kidding. Sort of.", "I'll protect you now. Promise.");
        };
    }

    public static String tooSoon(ChildStage stage, int seed) {
        return stage.learns() ? pick(seed, "I'm tired... Tomorrow?", "My arms hurt. Can we train tomorrow?") : pick(seed, "*yawns*", "*wanders off after a butterfly*");
    }

    public static String follow(ChildStage stage, int seed) {
        return switch (stage) {
            case BABY -> "*can't walk yet*";
            case TODDLER -> pick(seed, "Coming! Wait for me!", "*toddles after you*");
            case CHILD -> pick(seed, "Where are we going? Can I lead?", "Adventure!");
            case TEEN, ADULT -> pick(seed, "Fine. Where to?", "Sure. Lead the way.");
        };
    }

    public static String stay(ChildStage stage, int seed) {
        return switch (stage) {
            case BABY -> "*stays exactly where you put it*";
            case TODDLER -> pick(seed, "Okay... Come back soon?", "I stay. I'm good at staying.");
            case CHILD -> pick(seed, "Aww. Okay. I'll be here.", "Fine, but bring me something.");
            case TEEN, ADULT -> pick(seed, "Yeah, yeah. I'll be here.", "Cool. I'll hang out.");
        };
    }

    public static String fight(ChildStage stage, int seed) {
        return stage.older() ? pick(seed, "Back off from my family!", "Leave them alone!") : pick(seed, "Get away! I know how to fight!", "Hyaa!");
    }

    public static String scared(ChildStage stage, String parent, int seed) {
        return stage == ChildStage.BABY ? "*cries*" : pick(seed, parent + "! " + parent + ", help!", "*hides behind you*", "Make it go away!");
    }

    /** It grew into {@code stage}. */
    public static String grown(ChildStage stage, String name) {
        return switch (stage) {
            case BABY -> name + " is so small.";
            case TODDLER -> name + " took their first steps!";
            case CHILD -> name + " isn't a toddler anymore. Look how big they are.";
            case TEEN -> name + " is a teenager now. When did that happen?";
            case ADULT -> name + " is all grown up now.";
        };
    }

    /** 1.3.20: what it says as a moment with a parent begins ({@code parent}: what it calls them). */
    public static String moment(ChildMoment moment, ChildStage stage, String parent, int seed) {
        boolean baby = stage == ChildStage.BABY;
        boolean teen = stage.older();
        return switch (moment) {
            case HUG -> teen ? pick(seed, "Ugh, fine. ...*hugs you back, tight*", "...Okay. Just one. *doesn't let go*")
                : pick(seed, "*runs into your arms* " + parent + "!", "*squeezes you as hard as it can*", "Biggest hug! The BIGGEST!");
            case PAT -> baby ? pick(seed, "*coos*", "*grabs your fingers*") : teen ? pick(seed, "Hey, the hair! ...*smiles anyway*", "I'm not a kid anymore. ...Do it again.")
                : pick(seed, "*beams up at you*", "Hehe. Again!", "*leans into your hand*");
            case HIGH_FIVE -> teen ? pick(seed, "Nice.", "Up top. ...Too slow!") : pick(seed, "High five! Ow, hehe.", "Up high! Down low!");
            case TICKLE -> baby ? pick(seed, "*shrieks with laughter*", "*kicks and giggles*") : pick(seed, "No— hahaha— stop! Don't stop!", "*squirms, giggling* Not fair!");
            case PEEKABOO -> baby ? pick(seed, "*gasps* ...*giggles*", "*squeals and claps*") : pick(seed, "Again! Do peekaboo again!", "I saw you! Hehe!");
            case TOSS -> baby ? pick(seed, "*shrieks with delight*", "*giggles all the way up*") : pick(seed, "Wheee! Higher!", "Again! Again! AGAIN!");
            case SPIN -> pick(seed, "I'm flyiiing!", "Faster! Faster!", "Wheee! *arms out like wings*");
            case ARMS_UP -> pick(seed, "Up! " + parent + ", up!", "Uppy! Pleeease?", "*reaches up at you with both arms*");
            case WAVE -> teen ? pick(seed, "*nods at you*", "Hey.") : pick(seed, "Hi " + parent + "! *waves*", "*waves with its whole arm*");
            case CLAP -> baby ? pick(seed, "*claps its tiny hands*", "*bounces and claps*") : pick(seed, "Yay! *claps*", "*claps and laughs*");
            case FLOWER -> pick(seed, "I found this for you! It's the prettiest one.", parent + ", look! A flower! It's yours.", "*holds out a flower, very proud*");
            case TUCK -> baby ? pick(seed, "*yawns a tiny yawn*", "*sighs and curls up*") : teen ? pick(seed, "Night, " + parent + ". ...Thanks.", "I'm too old for this. ...Stay a bit?")
                : pick(seed, "Night night, " + parent + ".", "Will you check for monsters? ...Okay. Night.", "*yawns* Love you, " + parent + ".");
            case FEED -> baby ? pick(seed, "*munches happily*", "*grabs at the food, gurgling*") : pick(seed, "Yummy! Thank you, " + parent + "!", "*munch munch* So good!",
                "Mmm! Can I have another?");
            case NUZZLE -> baby ? pick(seed, "*coos and grabs a fistful of your hair*", "*gurgles happily*") : teen
                ? pick(seed, "Not in front of people! ...*smiles at the floor*", "Ugh. ...Love you too, " + parent + ".")
                : pick(seed, "*giggles and hugs your leg*", "Hehe! That tickles!", "Love you, " + parent + "!");
            case BOOP -> baby ? pick(seed, "*goes cross-eyed, then shrieks with laughter*", "*sneezes, surprised*")
                : pick(seed, "Hey! ...Boop! *boops you back*", "*cross-eyed* Hehehe! Again!", "My nose! You got my nose!");
            case FIST_BUMP -> teen ? pick(seed, "*bumps it* ...Pshhh. *blows it up*", "Respect.", "Okay, that was cool. Don't tell anyone.")
                : pick(seed, "Boom! *explodes its fist*", "Pow! Again! Harder!", "*bumps it with all its might*");
            case PATTY_CAKE -> pick(seed, "Patty-cake, patty-cake! Faster!", "*claps, misses, giggles* Again!", "Roll it and pat it and mark it with a... me!");
            case DANCE -> teen ? pick(seed, "Oh no. Okay. One song. ...Two.", "You're SO bad at this. *dances anyway*", "If anyone sees this, I'm moving out.")
                : pick(seed, "Dance! Dance! *stomps happily*", "Spin me! Spin me!", "*sways and giggles, holding on tight*");
            default -> "";
        };
    }

    /** 1.3.21: a ghoul child given human food, or a human child given a ghoul's: it won't. */
    public static String wontEat(ChildStage stage, boolean ghoul, int seed) {
        if (stage == ChildStage.BABY) {
            return "*turns its face away and fusses*";
        }
        return ghoul ? pick(seed, "Bleh! It tastes like dirt... I want the other kind.", "*spits it out* That's yucky! You know I can't eat that.")
            : pick(seed, "Ew! That's... meat? I don't like that kind.", "*makes a face* No thank you.");
    }

    /** 1.3.21: bedtime, asked (it goes off to bed), and when it has no bed of its own at home. */
    public static String bedtime(ChildStage stage, int seed) {
        return stage.older() ? pick(seed, "Ugh, fine. ...Night.", "It's not even late. ...Okay.") : pick(seed, "Five more minutes? ...Okay.", "*yawns* Okay, " +
            "I'm going.", "Will you tuck me in?");
    }

    /** 1.3.21: in bed, calling for its parent to come tuck it in. */
    public static String tuckMe(ChildStage stage, String parent, int seed) {
        return stage.older() ? pick(seed, "...You can say goodnight, " + parent + ". If you want.", "Night, " + parent + "? ...You coming?")
            : pick(seed, parent + "! Come tuck me in!", "I'm in bed, " + parent + "! Tuck me in?", parent + "? Are you coming to say night night?");
    }

    public static String noBed(ChildStage stage, int seed) {
        return stage.older() ? "I don't have a bed here. ...I'll crash wherever." : pick(seed, "But I don't have a bed...", "Where do I sleep?");
    }

    /** 1.3.21: walking hand in hand, and let go of. */
    public static String hand(ChildStage stage, int seed) {
        return stage.older() ? pick(seed, "In public? ...Fine. Don't tell anyone.", "*takes your hand, grumbling, and doesn't let go*")
            : pick(seed, "*grabs your hand tight*", "Hand! Let's go!", "*swings your hand happily*");
    }

    /** It is gone home (told to). */
    public static String home(ChildStage stage, int seed) {
        return stage.older() ? "Heading home. See you there." : pick(seed, "Going home! Race you!", "Okay! Home!");
    }


    static String pick(int seed, String... lines) {
        return lines[Math.floorMod(seed, lines.length)];
    }
}
