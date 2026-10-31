package com.hex.kagune.control.family;

/**
 * 1.3.10: what a child says (pure), by how grown it is: a baby babbles and giggles, a toddler wants up and points at everything, a child asks
 * to explore and to be taught, a teen is too cool for most of it and still glad you're there. {@code parent}: what it calls the one it
 * talks to ("Mama", "Papa", or their name for a teen). A child's words only.
 */
public final class ChildLines {
    /** What it calls a parent: a little one "Mama" or "Papa", a teen too ("Mom", "Dad"). */
    public static String call(ChildStage stage, boolean mother) {
        if (stage == ChildStage.TEEN) {
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
            case TEEN -> pick(seed, "...What? I'm just standing here.", "Can I go out tonight? I'll be careful. Mostly.",
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
            case TEEN -> pick(seed, "Hey.", "Oh. Hey, " + parent + ".", "...Hi. Took you long enough.");
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
            case TEEN -> pick(seed, "Fine. One round. ...Okay, two.", "You're slow. ...That was fun.", "Don't tell anyone I still like this.");
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
            case TEEN -> pick(seed, "Fine. Where to?", "Sure. Lead the way.");
        };
    }

    public static String stay(ChildStage stage, int seed) {
        return switch (stage) {
            case BABY -> "*stays exactly where you put it*";
            case TODDLER -> pick(seed, "Okay... Come back soon?", "I stay. I'm good at staying.");
            case CHILD -> pick(seed, "Aww. Okay. I'll be here.", "Fine, but bring me something.");
            case TEEN -> pick(seed, "Yeah, yeah. I'll be here.", "Cool. I'll hang out.");
        };
    }

    public static String fight(ChildStage stage, int seed) {
        return stage == ChildStage.TEEN ? pick(seed, "Back off from my family!", "Leave them alone!") : pick(seed, "Get away! I know how to fight!", "Hyaa!");
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
        };
    }

    static String pick(int seed, String... lines) {
        return lines[Math.floorMod(seed, lines.length)];
    }
}
