package com.hex.kagune.control.family;

import java.util.HashMap;
import java.util.Map;

/**
 * 3.4.2 (pure, tested; the owner's list after 3.2.1: "kids greeting you after days away"): a parent back after days away is greeted by
 * each child of theirs by its temper and its age, and the longer it was (a week or more), the more it has to say. When it last saw them is
 * kept with the player ({@link #key}: their NPC memory), so it outlasts the child's own mind, which is gone while it is unloaded with them
 * far off. The world's side is {@code ChildBrain} (and the hug it runs in for, as before).
 */
public final class KidGreeting {
    private KidGreeting() {
    }

    /** Days away (game days: 24000 ticks) before it counts; a long while; how often it notes they are near (ticks). */
    public static final long DAYS = 2L;
    public static final long LONG = 7L;
    public static final long STAMP = 1200L;
    public static final long DAY = 24000L;

    /** Where a child's last sight of a player is kept in that player's NPC memory ({@code NpcMemory}: lower-case words, 24 at most). */
    public static String key(String childId) {
        String id = childId == null ? "" : childId.toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9_]", "");
        return ("kid_" + id).length() > 24 ? ("kid_" + id).substring(0, 24) : "kid_" + id;
    }

    /** Whole days between {@code seen} and {@code now} (0 if never seen). */
    public static long days(long seen, long now) {
        return seen == Long.MIN_VALUE || now <= seen ? 0L : (now - seen) / DAY;
    }

    /** True if {@code days} away is long enough to be greeted for. */
    public static boolean greets(long days) {
        return days >= DAYS;
    }

    /** What it says, back from {@code days} away ({@code call}: what it calls them), by its temper and age; one by {@code seed}. */
    public static String line(ChildTemper temper, ChildStage stage, long days, String call, int seed) {
        String who = stage == ChildStage.BABY ? "baby" : stage == ChildStage.TODDLER ? "little" : (temper == null ? ChildTemper.SUNNY : temper).word();
        String[] lines = LINES.get(who + ":" + (days >= LONG ? "long" : "days"));
        String text = lines == null || lines.length == 0 ? "*runs to {call}*" : lines[Math.floorMod(seed, lines.length)];
        return text.replace("{call}", call == null || call.isEmpty() ? "Mama" : call).replace("{days}", words(days));
    }

    /** "two", "three"... "twenty", then the number. */
    static String words(long days) {
        String[] small = {"zero", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten", "eleven", "twelve", "thirteen",
            "fourteen", "fifteen", "sixteen", "seventeen", "eighteen", "nineteen", "twenty"};
        return days >= 0 && days < small.length ? small[(int)days] : Long.toString(days);
    }

    /** Every key ("temper|little|baby:days|long"), for the tests. */
    static java.util.Set<String> keys() {
        return java.util.Collections.unmodifiableSet(LINES.keySet());
    }

    private static final Map<String, String[]> LINES = new HashMap<>();

    private static void put(String key, String... lines) {
        LINES.put(key, lines);
    }

    static {
        put("baby:days", "*stares at {call}, then bursts into the biggest smile and reaches with both arms*", "*squeals and kicks its legs at the sight of {call}*");
        put("baby:long", "*looks at {call} a long, careful moment, as if checking, then shrieks and reaches*",
            "*goes very still, then grabs a fistful of {call}'s sleeve and won't let go*");
        put("little:days", "{call}! {call}! You BACK!", "*runs at {call} as fast as little legs go* Missed you!", "{call} gone LONG time!");
        put("little:long", "*stops dead, then runs* {call}!! You gone SO long! Where you go?!", "*hugs {call}'s legs and starts crying* ...Don't go again.",
            "{call}! I growed! Look! I growed while you gone!");
        put("sunny:days", "{call}! You're back! I've got SO much to tell you, it's been forever!",
            "{call}! Finally! I drew you like six pictures while you were gone!");
        put("sunny:long", "{call}!! *crashes into you* You were gone for AGES! I counted! Then I lost count! Then I started again!",
            "You're HOME! I knew you'd come back! I told everyone you would! Every day!");
        put("shy:days", "*peeks round the corner, then runs over and holds on without a word*", "*quietly* ...You're back. Good.");
        put("shy:long", "*hangs back, not sure, then suddenly hugs you so hard it hurts* ...I thought maybe you forgot us.",
            "*won't let go of your sleeve* ...Please don't go that long again.");
        put("bold:days", "There you are! Took you long enough, {call}. I kept everything running while you were gone.",
            "{call}! I fought off a spider while you were away. A BIG one. You missed it.");
        put("bold:long", "Where WERE you? You can't just leave for that long! ...I wasn't worried. I'm just saying.",
            "*stomps over* That was {days} days, {call}. That's a LOT of days. ...Don't do that. Okay? Just don't.");
        put("curious:days", "{call}! Where did you go? What did you see? Did you bring anything back?",
            "You're back! Okay, first question: where were you. Second question: can I come next time.");
        put("curious:long", "{call}! You were gone {days} days! What happened? Was it far? Did you see the sea? Tell me everything!",
            "*runs at you with a notebook* I wrote down all my questions while you were gone. There are forty-one.");
        put("mischief:days", "Oh, hi, {call}. You're back. Nothing happened while you were gone. Definitely nothing. Don't check the chest.",
            "*appears from nowhere* Boo! Ha! Missed me? I missed you. A normal amount.");
        put("mischief:long", "{call}! *grins too wide* You were gone so long I had to get up to SO much trouble. You'll find out.",
            "*hugs you, then steps back looking innocent* ...Don't look under your pillow. It's a surprise. A bad one.");
        put("gentle:days", "{call}, you're home! *hugs you* Are you hungry? Are you tired? Sit down, I'll get you something.",
            "I'm so glad you're back, {call}. I kept your pillow fluffed.");
        put("gentle:long", "*runs into your arms and holds on* I missed you every single day, {call}. I kept a candle lit every night.",
            "{call}... *eyes wet* You're okay. You're really okay. I was so worried.");
        put("serious:days", "Welcome back, {call}. You were away {days} days. Everything here is in order.",
            "{call}. Good. You're back. I kept a list of what happened. It's short.");
        put("serious:long", "You were gone {days} days, {call}. I marked each one. *a pause, and then a hug, stiff and fierce* ...Welcome home.",
            "You were gone a very long time. I had questions. I've decided they can wait. *hugs you*");
    }
}
