package com.hex.kagune.control.family;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 3.2.1: a player bringing their child home (pure, tested; the world's side is {@code HomecomingServer}), the owner: "more conversations,
 * like bringing the child home ... depending on how the character is or who it is, the personality they have". A child carried in, or
 * walking in at its parent's side, after a while out, is a homecoming: either an outing the two of them are back from, or a child that went
 * off on its own and was found. Its other parent waiting at home (Eto, Touka, Ken) meets them at the door with something of their own to
 * say to the player, and the child calls out to them as it comes in ({@link #hello}); with nobody waiting, the child has its own word with
 * the player. What they say is in the scene scripts ({@code brought}, {@code back}, {@code found}).
 */
public final class Homecoming {
    private Homecoming() {
    }

    /** How long it must have been out (ticks) to count; the least time between two for the same child; how long its scenes fit after. */
    public static final long AWAY = 900L;
    public static final long AGAIN = 6000L;
    public static final long LASTS = 1200L;

    /** True if coming back in after {@code away} ticks out, {@code since} ticks after its last homecoming, is a homecoming. */
    public static boolean counts(long away, long since) {
        return away >= AWAY && since >= AGAIN;
    }

    /** True if a homecoming {@code ago} ticks back is still the moment (its scenes fit). */
    public static boolean fresh(long ago) {
        return ago >= 0L && ago < LASTS;
    }

    /** True if it was found: it went out on its own or with someone else ({@code leftWith}), not with the one who brought it home. */
    public static boolean found(UUID leftWith, UUID broughtBy) {
        return leftWith == null || !leftWith.equals(broughtBy);
    }

    // ---------------------------------------------------------------- what it calls out as it comes in

    private static final Map<String, String[]> HELLO = new HashMap<>();

    private static void put(String key, String... lines) {
        HELLO.put(key, lines);
    }

    /** Its age's words: a baby's, a toddler's, a bigger child's (a child's and a teen's). */
    static String group(ChildStage stage) {
        return switch (stage) {
            case BABY -> "baby";
            case TODDLER -> "little";
            case CHILD, TEEN, ADULT -> "big";
        };
    }

    /**
     * What the child calls out to its other parent waiting at home as it comes in ({@code call}: what it calls them), by its temper, its
     * age and whether it was {@code found} or is back from an outing; one by {@code seed}.
     */
    public static String hello(ChildTemper temper, ChildStage stage, boolean found, String call, int seed) {
        String g = group(stage);
        String[] lines = "baby".equals(g) ? HELLO.get("baby")
            : HELLO.get((temper == null ? ChildTemper.SUNNY : temper).word() + ":" + g + ":" + (found ? "found" : "outing"));
        String line = lines == null || lines.length == 0 ? "*waves*" : lines[Math.floorMod(seed, lines.length)];
        return line.replace("{call}", call == null || call.isEmpty() ? "Mama" : call);
    }

    /** Every key ("temper:age:found|outing", "baby"), for the tests. */
    static java.util.Set<String> keys() {
        return java.util.Collections.unmodifiableSet(HELLO.keySet());
    }

    static {
        put("baby", "*squeals and reaches for {call} with both arms*", "*babbles something very urgent at {call}*");
        // A toddler back from an outing with you.
        put("sunny:little:outing", "{call}! {call}! We HOME!", "*runs at {call} with both arms out* Was FUN!");
        put("shy:little:outing", "*peeks at {call} from behind your legs and waves, tiny*", "*whispers* ...Hi, {call}.");
        put("bold:little:outing", "{call}! I walked! ALL the way!", "*marches in* Back! Me!");
        put("curious:little:outing", "{call}, look! *holds up a fistful of something* Found!", "{call}! Saw a bird! Why bird fly?");
        put("mischief:little:outing", "*giggles and hides behind you* {call} can't see me!", "Shh! *tiptoes in, very loudly*");
        put("gentle:little:outing", "*toddles to {call} for a hug* Missed you.", "*holds out a squashed flower* For {call}!");
        put("serious:little:outing", "*announces to {call}* Home.", "*nods at {call}, very grave* Back.");
        // A toddler you went and found.
        put("sunny:little:found", "{call}! I got lost and FOUND!", "*waves at {call}* Lost! Now home!");
        put("shy:little:found", "*hides its face and reaches for {call}*", "*tiny voice* ...{call}...");
        put("bold:little:found", "Wasn't lost, {call}! Was exploring!", "*kicks* Not lost! NOT!");
        put("curious:little:found", "{call}! Beetle went far!", "{call}, outside is BIG.");
        put("mischief:little:found", "*giggles* Hided from {call}!", "Hehe. Found me!");
        put("gentle:little:found", "Sorry, {call}. Sorry.", "*reaches for {call}* Sorry...");
        put("serious:little:found", "*solemn* Too far. Sorry, {call}.", "*to {call}* Lost. Now found.");
        // A child or a teen back from an outing with you.
        put("sunny:big:outing", "{call}! We're back! You would NOT believe the day we had!", "{call}, we had the best time! Come next time?");
        put("shy:big:outing", "*gives {call} a small wave* ...Hi. We're back.", "*quietly, to {call}* It was nice.");
        put("bold:big:outing", "{call}! I led the way home. Didn't need any help.", "Back. I could've stayed out longer, but fine.");
        put("curious:big:outing", "{call}, I've got so many questions about what we saw!", "{call}! Did you know there's a cave past the hill?");
        put("mischief:big:outing", "{call}, nothing happened. Nothing at all. Don't ask about the river.", "*grins at {call}* We were very well behaved. Mostly.");
        put("gentle:big:outing", "{call}, I brought you something from outside.", "{call}! Were you lonely? We missed you.");
        put("serious:big:outing", "{call}. We're back. Safely. I kept track of the time.", "Report, {call}: outing complete. No injuries.");
        // A child or a teen you went and found.
        put("sunny:big:found", "{call}! Sorry! I'm fine! I'm totally fine!", "Hi, {call}... Sorry. But I found the coolest place!");
        put("shy:big:found", "*can't meet {call}'s eyes* ...Sorry.", "*quietly* ...I'm sorry, {call}.");
        put("bold:big:found", "Before you say anything, {call}: I had it handled.", "I wasn't lost, {call}. I was just far.");
        put("curious:big:found", "{call}, I know, I know. But I found something amazing.", "I lost track of time, {call}. There was so much to look at.");
        put("mischief:big:found", "{call}! Hi! You look great. Have you done something with your hair?", "*innocent* Oh, hi, {call}. Was I gone long?");
        put("gentle:big:found", "I'm so sorry, {call}. I didn't mean to scare you.", "{call}, are you okay? I'm sorry I worried you.");
        put("serious:big:found", "{call}. I made a mistake. I'm ready to talk about it.", "I'm sorry, {call}. It won't happen again.");
    }
}
