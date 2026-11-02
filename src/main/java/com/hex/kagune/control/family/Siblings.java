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
    /**
     * 1.3.54: how far a runner in tag heads off at a time (blocks), and how far inside its home's edge it keeps (a toddler at play never
     * wanders off enough for a parent to come after it).
     */
    public static final double RUN = 5.0;
    public static final double YARD = 2.5;
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

    /** 1.3.54: how far from its home's middle a game of tag may go (blocks): its edge less {@link #YARD}, never under 3. */
    public static double yard(double radius) {
        return Math.max(3.0, radius - YARD);
    }

    /**
     * 1.3.54: where a runner in tag at ({@code x}, {@code z}) heads: {@link #RUN} blocks away from the one who is it ({@code cx},
     * {@code cz}), turned by {@code turn} radians; kept in its home (middle {@code hx}, {@code hz}, {@code radius}: {@link #yard}). A run
     * that would leave it turns along the edge (the turn that keeps it farthest from the chaser), else heads back in.
     */
    public static double[] runTo(double x, double z, double cx, double cz, double turn, double hx, double hz, double radius) {
        double ax = x - cx;
        double az = z - cz;
        double length = Math.sqrt(ax * ax + az * az);
        if (length < 1.0E-4) {
            ax = 1.0;
            az = 0.0;
        } else {
            ax /= length;
            az /= length;
        }
        double limit = yard(radius);
        double[] best = null;
        double bestFar = -1.0;
        for (double extra : new double[] {0.0, 0.5, -0.5, 1.0, -1.0, 1.5, -1.5, 2.0, -2.0, 2.6, -2.6}) {
            double a = turn + extra;
            double rx = ax * Math.cos(a) - az * Math.sin(a);
            double rz = ax * Math.sin(a) + az * Math.cos(a);
            double tx = x + rx * RUN;
            double tz = z + rz * RUN;
            if (Math.hypot(tx - hx, tz - hz) > limit) {
                continue;
            }
            double far = Math.hypot(tx - cx, tz - cz);
            if (extra == 0.0) {
                return new double[] {tx, tz}; // the way it meant to go is fine
            }
            if (far > bestFar) {
                bestFar = far;
                best = new double[] {tx, tz};
            }
        }
        if (best != null) {
            return best;
        }
        // Nowhere in reach is inside: back in, toward the middle, as far as it may go.
        double ix = hx - x;
        double iz = hz - z;
        double in = Math.sqrt(ix * ix + iz * iz);
        if (in < 1.0E-4) {
            return new double[] {x, z};
        }
        double step = Math.min(RUN, in);
        return new double[] {x + ix / in * step, z + iz / in * step};
    }

    /**
     * 1.3.55: what an older one says when it was going after a toddler ({@code name}) that wandered and sees a parent ({@code call}: what it
     * calls them) already on the way: it stands down, in its own temper ({@code t}) and age ({@code stage}: a child's words, or a teen's).
     */
    public static String standDown(ChildTemper t, ChildStage stage, String name, String call, int seed) {
        String key = (t == null ? "any" : t.word()) + ":" + (stage == ChildStage.TEEN ? "teen" : "child");
        java.util.List<String> lines = STAND_DOWN.getOrDefault(key, STAND_DOWN.get("any:" + (stage == ChildStage.TEEN ? "teen" : "child")));
        String line = lines.get(Math.floorMod(seed, lines.size()));
        return line.replace("{name}", name == null || name.isBlank() ? "you" : name).replace("{call}", call == null || call.isBlank() ? "Mom" : call);
    }

    /** 1.3.55: every stand-down line, by "temper:group" (for the tests). */
    public static java.util.Map<String, java.util.List<String>> standDownLines() {
        return STAND_DOWN;
    }

    private static final java.util.Map<String, java.util.List<String>> STAND_DOWN = java.util.Map.ofEntries(
        java.util.Map.entry("any:child", java.util.List.of("{call}'s got you. ...You're so in trouble, {name}.", "Oh good, {call}'s here.")),
        java.util.Map.entry("any:teen", java.util.List.of("{call}'s got it. Good luck, {name}.", "Never mind. {call}'s on it.")),
        java.util.Map.entry("sunny:child", java.util.List.of("Oh good, {call}'s got you! Bye, {name}!", "{call}'s coming! You're saved, {name}! ...Kind of.",
            "Phew. {call}'s on it.")),
        java.util.Map.entry("sunny:teen", java.util.List.of("Oh, {call}'s on it. Later, {name}!", "Saved by {call}. Lucky you, {name}.")),
        java.util.Map.entry("shy:child", java.util.List.of("...{call}'s got you. Okay.", "*stops, and lets {call} go to {name}*", "...oh. Good.")),
        java.util.Map.entry("shy:teen", java.util.List.of("...{call}'s got it.", "*steps back as {call} goes after {name}*")),
        java.util.Map.entry("bold:child", java.util.List.of("{call}'s got you. ...You're SO in trouble, {name}.", "Ha! Busted, {name}.",
            "Fine, {call} can deal with you.")),
        java.util.Map.entry("bold:teen", java.util.List.of("{call}'s got you. Enjoy the lecture, {name}.", "You're so dead, {name}.")),
        java.util.Map.entry("curious:child", java.util.List.of("Oh, {call} got there first. I wonder how mad {call} is.", "{call}'s going? Huh. Okay.")),
        java.util.Map.entry("curious:teen", java.util.List.of("{call} beat me to it.", "Huh. {call}'s fast.")),
        java.util.Map.entry("mischief:child", java.util.List.of("Ooh, {call}'s got you. Hehe. Good luck, {name}~", "Busted! Hehe.",
            "Not my problem anymore~")),
        java.util.Map.entry("mischief:teen", java.util.List.of("Ohh, {name}. You're in for it now~", "And that's my cue to leave.")),
        java.util.Map.entry("gentle:child", java.util.List.of("{call}'s got you, {name}. It's okay.", "Be good for {call}, okay?",
            "Okay... {call} will bring you home.")),
        java.util.Map.entry("gentle:teen", java.util.List.of("Go with {call}, {name}. It's okay.", "{call}'s here. You're safe, {name}.")),
        java.util.Map.entry("serious:child", java.util.List.of("{call} has it handled. Good.", "{call}'s got you. We'll talk about this later, {name}.",
            "Right. Not my job, then.")),
        java.util.Map.entry("serious:teen", java.util.List.of("{call} has it. Fine.", "Good. {call} will handle it.")));
}
