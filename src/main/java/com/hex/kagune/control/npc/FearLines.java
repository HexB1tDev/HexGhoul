package com.hex.kagune.control.npc;

import com.hex.kagune.control.family.ChildStage;

/**
 * 1.3.65: what everyone says at the worst sights ({@link FinalSight}), pure: wild ghouls that don't know them (War most of all: they keep
 * running, shouting, while it is in sight), the ghouls that follow them, their own children (and, hiding behind the other parent from War,
 * now and then), humans as they run screaming, and CCG investigators as they falter, then find their nerve again. The companions' own are
 * their scripts' ({@code Script.dread}).
 */
public final class FearLines {
    private FearLines() {
    }

    /** A wild ghoul that doesn't know them, at first sight. */
    public static String stranger(FinalSight sight, int seed) {
        return switch (sight) {
            case WAR -> pick(seed, "That mask... the King. The One-Eyed King. RUN!", "Don't look at it. Don't let it look at you!",
                "No. No no no. Not him. Not HIM.", "I heard the stories. I didn't believe the stories!");
            case PEACE -> pick(seed, "...It's beautiful. Why is it beautiful? Why am I shaking?", "Don't make it angry. Just... back away. Slowly.",
                "That's not a kagune. That's a cathedral.");
            case DRAGON -> pick(seed, "Wh— what ARE you?!", "That's not a ghoul. Run. RUN!", "The ground's moving... it's ALIVE!");
            case RINKAKU -> pick(seed, "All those legs— it's eating itself! Get away!", "A centipede the size of a house. I'm out!",
                "It's still growing. Why is it still growing?!");
            case UKAKU -> pick(seed, "It's on fire— it's ON FIRE and it's still coming!", "Needles! Get down! GET DOWN!",
                "Half a mask and all those flames... no thanks!");
            case NONE -> "";
        };
    }

    /** A ghoul that follows them. */
    public static String ally(FinalSight sight, int seed) {
        return switch (sight) {
            case WAR -> pick(seed, "...I'll follow you anywhere. Just... not this close.", "So that's the King. And I'm on his side. Good. Good.",
                "Remind me never to cross you. Ever.");
            case PEACE -> pick(seed, "...I didn't know a kakuja could look like that.", "I feel calm. Why do I feel calm?");
            case DRAGON -> pick(seed, "...I'm glad I'm on your side.", "Is... is anyone still in there? Boss?");
            case RINKAKU -> pick(seed, "That's a real final kakuja. Incredible.", "Just tell those legs I'm a friend, okay?");
            case UKAKU -> pick(seed, "You're burning the air. Incredible.", "Remind me to stand behind you. Way behind.");
            case NONE -> "";
        };
    }

    /** A wild ghoul still running from a terror in sight (War, the Dragon), now and then. */
    public static String fleeing(FinalSight sight, int seed) {
        return switch (sight) {
            case WAR -> pick(seed, "It's still there! Keep running!", "Don't stop! Don't look back!", "Split up! It can't chase all of us!");
            case DRAGON -> pick(seed, "It's following us!", "Faster! FASTER!", "Get underground! Anywhere!");
            default -> "";
        };
    }

    /** One of their own children at first sight, by how grown it is ({@code parent}: what it calls them). */
    public static String child(ChildStage stage, String parent, FinalSight sight, int seed) {
        return switch (stage) {
            case BABY -> switch (sight) {
                case WAR, DRAGON -> pick(seed, "*screams, hiding its face*", "*howls and howls*");
                case PEACE -> pick(seed, "*goes very quiet, staring up*", "*reaches up, then whimpers*");
                case RINKAKU, UKAKU -> pick(seed, "*cries*", "*whimpers, shaking*");
                case NONE -> "";
            };
            case TODDLER -> switch (sight) {
                case WAR -> pick(seed, "NO! Go 'way scary! Go 'WAY!", "*hides* Not " + parent + "! Not " + parent + "!");
                case PEACE -> pick(seed, parent + "... shiny...", "Ooh... " + parent + " big. " + parent + " pretty?");
                case DRAGON -> pick(seed, "*screams for " + parent + "*", parent + "! " + parent + "! Where " + parent + "?!");
                case RINKAKU -> pick(seed, "Leggies! Too many leggies!", parent + " scary! " + parent + " scary!");
                case UKAKU -> pick(seed, "Hot! " + parent + " hot! Owie!", parent + " on fire!");
                case NONE -> "";
            };
            case CHILD -> switch (sight) {
                case WAR -> pick(seed, parent + "...? That's not you. Where are you?", "Please take it off. Please, " + parent + ". I'm scared.");
                case PEACE -> pick(seed, "Whoa... " + parent + ", you look like a statue. A scary statue.", "Is that... still you? It's kind of pretty.");
                case DRAGON -> pick(seed, parent + "?! " + parent + ", where ARE you?!", "I want my " + parent + " back!");
                case RINKAKU -> pick(seed, "It's got so many legs... are they yours? All of them?", parent + ", you're HUGE. Don't step on me.");
                case UKAKU -> pick(seed, "You're on fire! Does it hurt? Tell me it doesn't hurt!", "Flames and needles... okay. Okay. That's scary.");
                case NONE -> "";
            };
            case TEEN -> switch (sight) {
                case WAR -> pick(seed, "...That's what they're all so scared of. It's you. It's been you.", "Take it off. I'm not joking. Take it off.");
                case PEACE -> pick(seed, "...Okay. That's actually beautiful. Don't let it go to your head.", "That's the calmest I've ever seen you. It's terrifying.");
                case DRAGON -> pick(seed, "Is anyone even in there? " + parent + "?!", "Come back. Just... come back as you.");
                case RINKAKU -> pick(seed, "...Okay, that's actually terrifying. Nice.", "Just... come back as you, okay?");
                case UKAKU -> pick(seed, "You're literally on fire. Cool. Also: please stop.", "That's insane. Show me how. Later. When I stop shaking.");
                case NONE -> "";
            };
        };
    }

    /**
     * Hiding from a parent in War (or the Dragon), now and then ({@code parent}: what it calls that one; {@code other}: what it calls the
     * one it hides behind, "" if there is no one to hide behind).
     */
    public static String hiding(ChildStage stage, String parent, String other, int seed) {
        boolean behind = other != null && !other.isEmpty();
        return switch (stage) {
            case BABY -> pick(seed, "*whimpers*", "*hiccups, staring*");
            case TODDLER -> behind ? pick(seed, "*clings to " + other + "'s leg*", "Make it go 'way, " + other + "...", "*peeks out, then hides again*")
                : pick(seed, "*peeks out, shaking*", "*covers its eyes* Can't see me...");
            case CHILD -> behind ? pick(seed, "Is that really " + parent + "? Is " + parent + " okay?", "I'm not scared. I'm just standing here. Behind you.",
                "...Tell me when it's " + parent + " again.")
                : pick(seed, "I'm not looking. I'm not looking.", "...Is it gone yet?");
            case TEEN -> behind ? pick(seed, "...Just tell me when it's over.", "I'm fine. I'm FINE. I'm just standing back here.",
                "Don't let them come any closer, okay?")
                : pick(seed, "...Just tell me when it's over.", "I'm staying right here. Far right here.");
        };
    }

    /** A human as it runs from them screaming ({@code sight} NONE: a ghoul with its kagune out, or one come too close). */
    public static String scream(FinalSight sight, int seed) {
        return switch (sight) {
            case WAR -> pick(seed, "AAAAAAAH!", "It's the King! The One-Eyed King! RUN!", "Help! Somebody HELP!", "*screams*",
                "Don't let it see me don't let it see me—");
            case PEACE -> pick(seed, "What— what is that?! RUN!", "*screams, and can't stop staring*", "An angel? A monster?! RUN!");
            case DRAGON -> pick(seed, "AAAAAAAAAH!", "The world's ending! It's ENDING!", "*screams until its voice breaks*");
            case RINKAKU -> pick(seed, "Legs! Legs everywhere! AAAH!", "MONSTER! MONSTER!", "*screams*");
            case UKAKU -> pick(seed, "Fire! It's on FIRE! RUN!", "MONSTER!", "*screams*");
            case NONE -> pick(seed, "G-ghoul! GHOUL!", "*gasps* Its eyes...", "Run! Call the CCG!");
        };
    }

    /** A CCG investigator at first sight of it: frozen where it stands ({@code senior}: the pair's leader, who holds better). */
    public static String falter(FinalSight sight, boolean senior, int seed) {
        return switch (sight) {
            case WAR -> senior ? pick(seed, "...That's the One-Eyed King. Steady. STEADY.", "Hold. Nobody move. Nobody breathe.")
                : pick(seed, "I-I can't... that's not in the manual...", "Sir? Sir, my hands won't stop shaking.");
            case PEACE -> senior ? pick(seed, "...Lord. Look at it.", "Don't look away from it. And don't look at it too long.")
                : pick(seed, "It's... beautiful? Why can't I move?", "Sir, I don't want to fight that.");
            case DRAGON -> senior ? pick(seed, "That's the thing from the reports. Fall back. FALL BACK!", "Get word to headquarters. Now.")
                : pick(seed, "Oh god. Oh god oh god.", "We're dead. We're all dead.");
            case RINKAKU -> senior ? pick(seed, "A final kakuja. Rinkaku. Watch the legs, it'll come from everywhere.", "That's S-rate at the very least. Steady.")
                : pick(seed, "Th-there's so many of them...", "It's eating itself and getting BIGGER?!");
            case UKAKU -> senior ? pick(seed, "Ukaku, final stage. Mind the needles. Mind the FIRE.", "Stay out of the flames. Watch my back.")
                : pick(seed, "It's burning the air... I can't breathe...", "Needles... I can't dodge that many!");
            case NONE -> "";
        };
    }

    /** Its nerve back after falling back: in again. */
    public static String rally(boolean senior, int seed) {
        return senior ? pick(seed, "On me! It bleeds like the rest of them!", "For everyone it ever ate. Forward!", "We don't run. Not from this. Not from anything.")
            : pick(seed, "...Okay. Okay. With you, sir.", "If I die, tell my mother I was brave.", "I'm not running. I'm NOT running!");
    }

    static String pick(int seed, String... lines) {
        return lines[Math.floorMod(seed, lines.length)];
    }
}
