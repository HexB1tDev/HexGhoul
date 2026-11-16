package com.hex.kagune.control.family;

import java.util.ArrayList;
import java.util.List;

/**
 * 1.3.49: a companion parent looking after its children (pure, tested; the world's side is {@code ParentDuty}), the owner: "if the kid
 * runs off the home and gets too far away or goes out at night, the mother should see it and go pick it up. If the kid goes out on a
 * rampage in the ghoul state, the spouse should go stop them... when they're chasing something for too long... the parents scolding the
 * child, back and forward conversations in the bubbles". When it is too much is worked out here: how far a child may stray from home by
 * day and by night (more the older it is), how long and how far it may fight before it has gone too far, and how badly hurt; and what
 * they say to each other ({@link ParentingLines}: hundreds of lines, by the parent, the child's temper and its age).
 */
public final class Parenting {
    /** Why a parent goes after its child. */
    public enum Why {
        /** Strayed too far from home. */
        WANDERED,
        /** Out of the house at night. */
        NIGHT,
        /** Fighting too long, too far off, or too hurt to go on: a rampage. */
        RAMPAGE,
        /** Its kagune came out (a word from the parent, no going after it). */
        TRANSFORM,
        /** Brought home (what they say at the door). */
        HOME
    }

    /** One line of a talk: whose ({@code parent} or the child), what. */
    public record Line(boolean parent, String text) {
    }

    /** How far beyond its home's edge a child may go by day, at each stage (blocks); by night. */
    public static double strays(ChildStage stage, boolean night) {
        return switch (stage) {
            case BABY -> 0.0;
            case TODDLER -> night ? 0.0 : 4.0;
            case CHILD -> night ? 2.0 : 16.0;
            case TEEN, ADULT -> night ? 24.0 : 40.0;
        };
    }

    /**
     * Why its parent would go after it now, or null: a child {@code beyond} blocks past its home's edge (negative: inside), at night or
     * not; never one that follows a player, is carried, or is a baby (it can't go anywhere on its own); never for a short fight.
     */
    public static Why strayed(ChildStage stage, double beyond, boolean night, boolean following, boolean carried) {
        if (following || carried || stage == ChildStage.BABY) {
            return null;
        }
        if (night && beyond > strays(stage, true)) {
            return Why.NIGHT;
        }
        return beyond > strays(stage, false) ? Why.WANDERED : null;
    }

    /** How long a child (and a teen) may fight with its kagune before it is too much (ticks): thirty seconds, a minute for a teen. */
    public static long fightsFor(ChildStage stage) {
        return stage.older() ? 1200L : 600L;
    }

    /**
     * True if a fight has gone too far: going on {@code fought} ticks, carried {@code beyond} blocks past its home's edge, or its health down
     * to {@code health} of its most (it is losing). A toddler or a baby never fights.
     */
    public static boolean rampage(ChildStage stage, long fought, double beyond, float health) {
        if (!stage.learns()) {
            return false;
        }
        boolean teen = stage.older();
        return fought > fightsFor(stage) || beyond > (teen ? 36.0 : 20.0) || health < (teen ? 0.25F : 0.3F);
    }

    /** How far off a parent notices its child (blocks, in sight); how long a child out of sight and out of home goes before it is missed. */
    public static final double NOTICES = 40.0;
    public static final long MISSED_AFTER = 600L;
    /** How long between two goings-after the same child (ticks), how long before a parent gives up on reaching it. */
    public static final long AGAIN_AFTER = 1200L;
    public static final long GIVES_UP = 2400L;
    /** A fight stopped: how long the child holds back from fighting after (ticks). */
    public static final long CALMED = 1200L;
    /** How long between two words about its kagune coming out (ticks). */
    public static final long REMARKS_EVERY = 3000L;
    /** How close the parent comes before it speaks (blocks). */
    public static final double REACHES = 2.6;

    /** True if a parent {@code distance} away notices: in sight within {@link #NOTICES}, or the child has been gone {@code missing} ticks. */
    public static boolean notices(double distance, boolean sees, long missing) {
        return sees && distance <= NOTICES || missing >= MISSED_AFTER;
    }

    /** How long a line stays up before the next is said (ticks): long enough to read it. */
    public static int pause(String text) {
        // 1.3.52: a little longer (each line now comes down as the reply goes up, so this is all the time it is up to be read).
        int length = text == null ? 0 : text.length();
        return Math.max(40, Math.min(140, 32 + length * 7 / 5));
    }

    /** The stage's group its lines are for: a toddler's words, a child's, a teen's. */
    public static String group(ChildStage stage) {
        return switch (stage) {
            case BABY, TODDLER -> "little";
            case CHILD -> "child";
            case TEEN, ADULT -> "teen";
        };
    }

    /**
     * What they say to each other ({@code persona}: the parent's key, "touka", "ken", "eto"; {@code name}: the child's; {@code call}: what
     * it calls that parent, "Mama", "Dad"...; {@code seed} picks among the lines): the parent first, then back and forth. A word about its
     * kagune is short (the parent, the child, now and then the parent again); a child brought back after straying, out at night or on a
     * rampage is scolded and answers, is told why, answers again, and the parent has the last word; at the door, {@link #home} (3.2.0).
     */
    public static List<Line> talk(Why why, String persona, ChildStage stage, ChildTemper temper, String name, String call, int seed) {
        String who = persona == null ? "eto" : persona;
        String g = group(stage);
        String w = why.name().toLowerCase(java.util.Locale.ROOT);
        String t = temper == null ? "sunny" : temper.word();
        List<Line> out = new ArrayList<>();
        java.util.Random pick = new java.util.Random(seed * 0x9E3779B97F4A7C15L ^ why.ordinal());
        switch (why) {
            case TRANSFORM -> {
                add(out, true, ParentingLines.get("open", who, w, g), pick, name, call);
                add(out, false, ParentingLines.get("reply", t, w, g), pick, name, call);
                if (pick.nextBoolean()) {
                    add(out, true, ParentingLines.get("final", who, w, g), pick, name, call);
                }
            }
            case HOME -> {
                return home(null, persona, stage, temper, name, call, seed);
            }
            default -> {
                add(out, true, ParentingLines.get("open", who, w, g), pick, name, call);
                add(out, false, ParentingLines.get("reply", t, w, g), pick, name, call);
                add(out, true, ParentingLines.get("follow", who, w, g), pick, name, call);
                add(out, false, ParentingLines.get("close", t, w, g), pick, name, call);
                add(out, true, ParentingLines.get("final", who, w, g), pick, name, call);
            }
        }
        return out;
    }

    /** 3.2.0: how often the child has a last word at the door (in a hundred), after its parent's answer. */
    public static final int LAST_WORD = 55;

    /**
     * 3.2.0: the word at the door, the owner: "more conversations, like bringing the child home ... depending on how the character is or who
     * it is, the personality they have". Brought back for {@code fetched} (it strayed, it was out at night, it went on a rampage; null: not
     * known), the parent says something about that (or something any homecoming would hear); the child answers in its temper's way, about
     * that; the parent answers that kind of child (Touka to a bold one, Ken to a shy one, Eto to a mischievous one, each their own way); and
     * now and then ({@link #LAST_WORD}) the child has the last word.
     */
    public static List<Line> home(Why fetched, String persona, ChildStage stage, ChildTemper temper, String name, String call, int seed) {
        String who = persona == null ? "eto" : persona;
        String g = group(stage);
        String t = temper == null ? "sunny" : temper.word();
        String why = fetched == Why.WANDERED || fetched == Why.NIGHT || fetched == Why.RAMPAGE ? fetched.name().toLowerCase(java.util.Locale.ROOT) : null;
        List<Line> out = new ArrayList<>();
        java.util.Random pick = new java.util.Random(seed * 0x9E3779B97F4A7C15L ^ Why.HOME.ordinal());
        add(out, true, either(pick, why == null ? null : ParentingLines.get("door", who, why, g), ParentingLines.get("open", who, "home", g)), pick, name, call);
        add(out, false, either(pick, why == null ? null : ParentingLines.get("doorreply", t, why, g), ParentingLines.get("reply", t, "home", g)), pick, name, call);
        add(out, true, ParentingLines.get("answer", who, t, g), pick, name, call);
        if (pick.nextInt(100) < LAST_WORD) {
            add(out, false, ParentingLines.get("last", t, "home", g), pick, name, call);
        }
        return out;
    }

    /** 3.2.0: how often (in a hundred) a line that knows why is said rather than one any homecoming would hear. */
    public static final int KNOWS_WHY = 70;

    /** The lines that know why ({@code knowing}) {@link #KNOWS_WHY} times in a hundred, else the plain ones (either may be null). */
    private static String[] either(java.util.Random pick, String[] knowing, String[] plain) {
        if (knowing == null || knowing.length == 0) {
            return plain;
        }
        return plain == null || plain.length == 0 || pick.nextInt(100) < KNOWS_WHY ? knowing : plain;
    }

    private static void add(List<Line> out, boolean parent, String[] lines, java.util.Random pick, String name, String call) {
        if (lines == null || lines.length == 0) {
            return;
        }
        String line = lines[pick.nextInt(lines.length)];
        out.add(new Line(parent, line.replace("{name}", name == null || name.isEmpty() ? "kid" : name).replace("{call}", call == null ? "Mama" : call)));
    }
}
