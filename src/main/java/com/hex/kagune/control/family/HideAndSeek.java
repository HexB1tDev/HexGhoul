package com.hex.kagune.control.family;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 1.3.58: hide and seek between brothers and sisters (pure, tested; the world's side is {@code SiblingLife}), the owner liked the idea. At
 * home by day, instead of tag now and then: the one who starts it counts with its eyes covered, turned away; the other runs off and hides
 * somewhere in the yard the seeker can't see from where it counts ({@link #bestSpot}), crouched down small, giggling now and then (which
 * gives it away: {@link #HINT_EVERY}); the seeker looks about the yard and finds it ({@link #FINDS}, in sight), or gives up
 * ({@link #SEEKS}) and the hider pops out. What they say ({@link #line}) by part, age and temper.
 */
public final class HideAndSeek {
    /** How often, when two of them would play tag, they play hide and seek instead. */
    public static final double INSTEAD_OF_TAG = 0.5;
    /** Ticks the seeker counts; looks before it gives up; between the hider's giggles (each a chance); a giggle heard for this long. */
    public static final long COUNT = 100L;
    public static final long SEEKS = 900L;
    public static final long HINT_EVERY = 160L;
    public static final long HEARD = 80L;
    /** How near the seeker must come, seeing it, to find it (blocks); how near and far from the seeker it looks for a spot. */
    public static final double FINDS = 3.0;
    public static final double NEAREST = 4.0;
    public static final double FARTHEST = 11.0;
    /** Spots it weighs up; ticks it may take getting to one (else it hides where it is); how long they stand about once it's over. */
    public static final int SPOTS = 24;
    public static final long REACHES = 160L;
    public static final long AFTER = 80L;
    /** The longest a game lasts, all told. */
    public static final long LONGEST = COUNT + SEEKS + AFTER + 200L;

    /** A line's part in the game (never sent: order free). */
    public enum Part {
        /** The one who starts it: "Hide and seek! I'll count!" */
        PROPOSE,
        /** The other, running off. */
        AGREE,
        /** The seeker, counting, eyes covered. */
        COUNT,
        /** "...ten! Ready or not, here I come!" */
        READY,
        /** The seeker, looking about. */
        SEEKING,
        /** The hider, trying not to giggle (a thought). */
        HINT,
        /** The seeker, finding it. */
        FOUND,
        /** The hider, found. */
        CAUGHT,
        /** The seeker, giving up. */
        GIVE_UP,
        /** The hider, popping out the winner. */
        WIN
    }

    private HideAndSeek() {
    }

    /** What {@code temper}'s child of {@code stage} says for {@code part}; its own temper's line if it has one, else its age's. */
    public static String line(Part part, ChildTemper temper, ChildStage stage, int seed) {
        String group = stage == ChildStage.BABY || stage == ChildStage.TODDLER ? "little" : "big";
        String p = part.name().toLowerCase(Locale.ROOT);
        List<String> own = temper == null ? null : LINES.get(p + ":" + temper.word() + ":" + group);
        List<String> any = LINES.get(p + ":any:" + group);
        List<String> from = own != null && (any == null || Math.floorMod(seed, 3) != 0) ? own : any;
        return from.get(Math.floorMod(seed >> 2, from.size()));
    }

    /** Every key's lines ("part:temper|any:little|big"), for the tests. */
    public static Map<String, List<String>> lines() {
        return LINES;
    }

    /**
     * The best of the spots weighed up (their distances from the seeker, whether the seeker can see them from where it counts): one it
     * can't see first, then the farthest within {@link #NEAREST}..{@link #FARTHEST} (one too near or too far only if nothing else will
     * do); -1 for none.
     */
    public static int bestSpot(double[] distance, boolean[] hidden) {
        int best = -1;
        double bestScore = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < distance.length; i++) {
            if (Double.isNaN(distance[i])) {
                continue;
            }
            boolean band = distance[i] >= NEAREST && distance[i] <= FARTHEST;
            double score = (hidden[i] ? 1000.0 : 0.0) + (band ? 100.0 : 0.0) + Math.min(distance[i], FARTHEST);
            if (score > bestScore) {
                bestScore = score;
                best = i;
            }
        }
        return best;
    }

    /**
     * Spot {@code i} of {@link #SPOTS} to weigh up: spread round the yard (middle {@code hx}, {@code hz}, {@code radius}: within
     * {@link Siblings#yard}), each at its own angle and a share of the way out, turned by {@code seed}.
     */
    public static double[] spot(int i, double hx, double hz, double radius, int seed) {
        double limit = Siblings.yard(radius);
        double golden = Math.PI * (3.0 - Math.sqrt(5.0));
        double angle = i * golden + Math.floorMod(seed, 360) * Math.PI / 180.0;
        double out = limit * Math.sqrt((i + 0.5) / SPOTS);
        return new double[] {hx + Math.cos(angle) * out, hz + Math.sin(angle) * out};
    }

    /** True if the seeker heads for where it last heard a giggle ({@code sinceHint} ticks ago). */
    public static boolean heard(long sinceHint) {
        return sinceHint >= 0L && sinceHint <= HEARD;
    }

    private static final Map<String, List<String>> LINES = Map.ofEntries(
        Map.entry("propose:any:little", List.of("Hide and seek! I count!", "Hiiide and seek! You hide!", "Play hide and seek with me! I count!")),
        Map.entry("propose:any:big", List.of("Hide and seek? I'll count. Go hide.", "Let's play hide and seek! I'm counting!",
            "Bet you can't hide from me. I'll count.")),
        Map.entry("propose:sunny:big", List.of("HIDE AND SEEK! I'll count! Go, go, go!")),
        Map.entry("propose:shy:big", List.of("...um. Hide and seek? I can count...")),
        Map.entry("propose:bold:big", List.of("Hide and seek. I'll find you in ten seconds flat.")),
        Map.entry("propose:curious:big", List.of("Hide and seek! I want to see where you think is a good spot.")),
        Map.entry("propose:mischief:big", List.of("Hide and seek! ...I definitely won't peek.")),
        Map.entry("propose:gentle:big", List.of("Want to play hide and seek? I'll count, you hide!")),
        Map.entry("propose:serious:big", List.of("Hide and seek. Standard rules. I count to ten.")),
        Map.entry("agree:any:little", List.of("Okay! No peeking!", "Yay! *runs off giggling*", "*squeals and runs*")),
        Map.entry("agree:any:big", List.of("You're on. No peeking.", "Fine. Count slow.", "Okay! Eyes closed!")),
        Map.entry("agree:mischief:big", List.of("Hehe. You'll never find me.")),
        Map.entry("agree:shy:big", List.of("...okay. *scurries off*")),
        Map.entry("agree:bold:big", List.of("Ha! Good luck.")),
        Map.entry("agree:mischief:little", List.of("Hehe! *runs off*")),
        Map.entry("count:any:little", List.of("One... two... uh... five...", "One, two, free, four...", "*eyes covered* One... two...")),
        Map.entry("count:any:big", List.of("One... two... three... four...", "*eyes covered* One... two... three...", "One Mississippi... two Mississippi...")),
        Map.entry("count:serious:big", List.of("One. Two. Three. Four. Five...")),
        Map.entry("count:mischief:big", List.of("One... two... *peeks* ...three...")),
        Map.entry("ready:any:little", List.of("...ten! Here I come!", "...TEN! Ready or not!")),
        Map.entry("ready:any:big", List.of("...nine... ten! Ready or not, here I come!", "...ten! Coming, ready or not!")),
        Map.entry("ready:bold:big", List.of("...TEN! You can't hide forever!")),
        Map.entry("seeking:any:little", List.of("Where are youuu?", "Are you here? ...No.", "*looks under a leaf*")),
        Map.entry("seeking:any:big", List.of("Hmm... where are you~?", "I know you're here somewhere...", "Are you behind the... no.")),
        Map.entry("seeking:curious:big", List.of("Hm. Statistically, you're behind something.")),
        Map.entry("seeking:bold:big", List.of("Come out, come out, wherever you are!")),
        Map.entry("seeking:shy:big", List.of("...are you there?")),
        Map.entry("hint:any:little", List.of("*giggle*", "*peeks out, then ducks*", "*tries to be very quiet*")),
        Map.entry("hint:any:big", List.of("*giggles*", "*tries very hard not to giggle*", "*shh...*", "*hides a little lower*")),
        Map.entry("found:any:little", List.of("Found you! Found you!", "There you are!", "Gotcha!")),
        Map.entry("found:any:big", List.of("Found you!", "Gotcha!", "Ha! There you are!")),
        Map.entry("found:bold:big", List.of("Found you. Told you, ten seconds.")),
        Map.entry("found:serious:big", List.of("Found. That took longer than it should have.")),
        Map.entry("caught:any:little", List.of("Aww!", "Again! Again!", "*giggles* You found me!")),
        Map.entry("caught:any:big", List.of("Aww! No fair!", "How did you find me?!", "...That was a good spot, too.")),
        Map.entry("caught:mischief:big", List.of("You peeked! I KNOW you peeked!")),
        Map.entry("caught:shy:big", List.of("...you found me.")),
        Map.entry("caught:bold:big", List.of("Lucky guess.")),
        Map.entry("give_up:any:little", List.of("I give up! Where are youuu?", "Come out! Pleeease?")),
        Map.entry("give_up:any:big", List.of("Okay, I give up! Where are you?", "Fine! You win! Come out!")),
        Map.entry("give_up:bold:big", List.of("...Okay. You win. THIS time.")),
        Map.entry("win:any:little", List.of("Here I am! I win!", "*pops out* Boo!")),
        Map.entry("win:any:big", List.of("Here! Ha! I win!", "*pops out* Boo! I win!")),
        Map.entry("win:mischief:big", List.of("I was right behind you the WHOLE time.")),
        Map.entry("win:serious:big", List.of("Here. I win. Obviously.")));
}
