package com.hex.kagune.control.family;

import java.util.ArrayList;
import java.util.List;

/**
 * 1.3.51: brothers and sisters (pure, tested; the world's side is {@code SiblingLife}), the owner: "are there interactions between brothers
 * and sisters?" Two children of the same parent near each other now and then have a moment of their own: they chat, tease, brag, comfort
 * one that is hurt, scheme against a parent, squabble (and a companion parent near breaks it up), play tag; an older one goes after a
 * toddler that wandered off and brings it home; in a fight they cover each other, and a sibling's kagune coming out gets a word. What they
 * say is {@link SiblingLines} (hundreds of lines), by the speaker's temper and age.
 */
public final class Siblings {
    /** A moment between two of them (never sent: order free). */
    public enum Kind {
        CHAT, TEASE, BRAG, COMFORT, SCHEME, SQUABBLE, TAG, MIND, REACT, TEAM
    }

    /** One line: who says it (1 the one who started, 2 the other, 0 a parent), what. */
    public record Line(int who, String text) {
    }

    /** How near two of them must be to have a moment (blocks); how long between one child's moments (ticks); a game of tag's length. */
    public static final double NEAR = 10.0;
    public static final long EVERY = 1500L;
    public static final long TAG_TICKS = 400L;
    /** How close a chaser must get to tag (blocks); how long a tagged one can't tag straight back (ticks). */
    public static final double TAG_REACH = 1.3;
    public static final long NO_TAG_BACKS = 30L;
    /** Hurt under this share of its health, a sibling near comforts it. */
    public static final float HURT = 0.6F;
    /** How long between two words about a sibling's kagune, or a "got your back" (ticks). */
    public static final long REMARKS_EVERY = 1200L;

    private Siblings() {
    }

    /**
     * Which moment a child ({@code a}, its temper {@code ta}) has with a brother or sister ({@code b}, {@code tb}) near it: an older one
     * goes after a toddler that has {@code strayed}; one that is {@code hurt} is comforted; else by their tempers (the bold and the
     * mischievous tease and squabble, the sunny brag and play, the curious and the mischievous scheme, everyone chats); toddlers chat, tease,
     * squabble and play. {@code roll} in 0..1. Null for none (a baby has no moments of its own).
     */
    public static Kind pick(ChildStage a, ChildTemper ta, ChildStage b, ChildTemper tb, boolean hurt, boolean strayed, double roll) {
        if (a == ChildStage.BABY || b == ChildStage.BABY) {
            return null;
        }
        if (strayed && b == ChildStage.TODDLER && a.learns()) {
            return Kind.MIND;
        }
        if (hurt) {
            return Kind.COMFORT;
        }
        Kind[] kinds = {Kind.CHAT, Kind.TEASE, Kind.BRAG, Kind.SCHEME, Kind.SQUABBLE, Kind.TAG};
        double[] w = {3.0, 1.5, 1.2, 1.0, 0.8, 1.6};
        for (ChildTemper t : new ChildTemper[] {ta, tb}) {
            if (t == null) {
                continue;
            }
            switch (t) {
                case BOLD -> {
                    w[1] += 0.8;
                    w[4] += 0.7;
                    w[5] += 0.5;
                }
                case MISCHIEF -> {
                    w[1] += 1.0;
                    w[3] += 1.0;
                    w[4] += 0.4;
                }
                case SUNNY -> {
                    w[2] += 0.8;
                    w[5] += 1.0;
                }
                case CURIOUS -> {
                    w[0] += 0.8;
                    w[3] += 0.8;
                }
                case GENTLE, SHY -> {
                    w[0] += 1.0;
                    w[4] *= 0.5;
                }
                case SERIOUS -> {
                    w[0] += 0.5;
                    w[4] += 0.4;
                }
            }
        }
        if (a == ChildStage.TODDLER || b == ChildStage.TODDLER) {
            w[2] *= 0.5; // a toddler doesn't brag much, or scheme
            w[3] *= 0.4;
        }
        double total = 0.0;
        for (double x : w) {
            total += x;
        }
        double at = Math.max(0.0, Math.min(0.999999, roll)) * total;
        for (int i = 0; i < kinds.length; i++) {
            at -= w[i];
            if (at < 0.0) {
                return kinds[i];
            }
        }
        return Kind.CHAT;
    }

    /** "little" for a toddler's words, "big" for an older one's. */
    public static String group(ChildStage stage) {
        return stage == ChildStage.BABY || stage == ChildStage.TODDLER ? "little" : "big";
    }

    /**
     * What they say, in turn ({@code a} started it; {@code callA}/{@code callB}: what each calls the companion parent; {@code girlA},
     * {@code girlB}: a sister or a brother): most moments the one, the other, the one again; a squabble back and forth twice; tag a shout
     * and an answer; a word about a kagune or "got your back" the one alone.
     */
    public static List<Line> talk(Kind kind, ChildTemper ta, ChildStage sa, String nameA, String callA, boolean girlA, ChildTemper tb, ChildStage sb,
                                  String nameB, String callB, boolean girlB, int seed) {
        String k = kind.name().toLowerCase(java.util.Locale.ROOT);
        java.util.Random pick = new java.util.Random(seed * 0x9E3779B97F4A7C15L ^ (kind.ordinal() * 31L));
        Speaker a = new Speaker(1, ta, sa, nameB, callA, girlB);
        Speaker b = new Speaker(2, tb, sb, nameA, callB, girlA);
        List<Line> out = new ArrayList<>();
        switch (kind) {
            case REACT, TEAM -> add(out, a, "open", k, pick);
            case TAG -> {
                add(out, a, "open", k, pick);
                add(out, b, "reply", k, pick);
            }
            case SQUABBLE -> {
                add(out, a, "open", k, pick);
                add(out, b, "reply", k, pick);
                add(out, a, "hit", k, pick);
                add(out, b, "hitback", k, pick);
            }
            default -> {
                add(out, a, "open", k, pick);
                add(out, b, "reply", k, pick);
                add(out, a, "back", k, pick);
            }
        }
        return out;
    }

    /** A game of tag's shout as the chaser catches the other ({@code tagger} says it). */
    public static Line tagged(ChildTemper tagger, ChildStage stage, String other, int seed) {
        List<Line> out = new ArrayList<>();
        add(out, new Speaker(1, tagger, stage, other, "Mama", false), "tagged", "tag", new java.util.Random(seed));
        return out.isEmpty() ? new Line(1, "Tag!") : out.get(0);
    }

    /** A companion parent breaking up their squabble ({@code persona}'s line), then each says sorry, sulking. */
    public static List<Line> breakUp(String persona, ChildTemper ta, String nameA, ChildTemper tb, String nameB, int seed) {
        java.util.Random pick = new java.util.Random(seed * 0x2545F4914F6CDD1DL);
        List<Line> out = new ArrayList<>();
        String[] lines = SiblingLines.get("break", persona == null ? "eto" : persona, "squabble", "any");
        if (lines != null) {
            out.add(new Line(0, lines[pick.nextInt(lines.length)]));
        }
        String[] sulkA = SiblingLines.get("sulk", ta == null ? "sunny" : ta.word(), "squabble", "any");
        String[] sulkB = SiblingLines.get("sulk", tb == null ? "sunny" : tb.word(), "squabble", "any");
        if (sulkA != null) {
            out.add(new Line(1, sulkA[pick.nextInt(sulkA.length)].replace("{sib}", nameB)));
        }
        if (sulkB != null) {
            out.add(new Line(2, sulkB[pick.nextInt(sulkB.length)].replace("{sib}", nameA)));
        }
        return out;
    }

    /** Who says a line, and the names they put in it (the other's, what they call the parent, whether the other is a sister). */
    private record Speaker(int who, ChildTemper temper, ChildStage stage, String other, String call, boolean otherGirl) {
    }

    private static void add(List<Line> out, Speaker s, String part, String kind, java.util.Random pick) {
        String[] lines = SiblingLines.get(part, s.temper() == null ? "sunny" : s.temper().word(), kind, group(s.stage()));
        if (lines == null || lines.length == 0) {
            return;
        }
        String line = lines[pick.nextInt(lines.length)];
        out.add(new Line(s.who(), line.replace("{sib}", s.other() == null || s.other().isEmpty() ? "you" : s.other())
            .replace("{call}", s.call() == null ? "Mom" : s.call()).replace("{sibrole}", s.otherGirl() ? "sister" : "brother")));
    }
}
