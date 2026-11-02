package com.hex.kagune.control.family;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 1.3.53: a talk that holds together (pure, tested; the world's side is {@code Conversation}), the owner: "prevent NPCs from walking away
 * during conversations". Everyone in a talk stays for it: the one who starts it walks up first ({@link #GATHER}), a parent breaking up a
 * squabble comes over (each line waits for its speaker to be near: {@link #SPEAKS}, at most {@link #WAITS}); they stand, and turn to whoever
 * speaks ({@link #faces}), the speaker to whom it answers or who answers it. It ends when its last line has had its time, or at once when
 * someone goes ({@link End}): gone, walked off past {@link #LEAVES}, hurt, a fight, asleep, or a player wanting one of them. Walked off on,
 * whoever stays may say so ({@link #cutShort}).
 */
public final class Talks {
    /** How close the one who comes up stands to the one it talks to; how near a speaker must be to say its line; further: it left. */
    public static final double GATHER = 2.2;
    public static final double SPEAKS = 4.0;
    public static final double LEAVES = 8.0;
    /** The most a line waits for its speaker to come near (then said from where it is, if it is still about); to come together at all. */
    public static final long WAITS = 240L;
    public static final long GATHERS = 300L;
    /** How far a body turns toward the speaker a tick (degrees): a turn of the shoulders, not a snap. */
    public static final float TURN = 24.0F;

    /** Why a talk ended (never sent: order free). */
    public enum End {
        /** Its last line said and read. */
        DONE,
        /** One of them walked (or was carried) off. */
        LEFT,
        /** One of them gone from the world (dead, unloaded). */
        GONE,
        /** One of them hurt. */
        HURT,
        /** A fight. */
        FIGHT,
        /** One of them fell asleep. */
        ASLEEP,
        /** A player wants one of them (a talk of their own, a hand, picked up). */
        INTERRUPTED
    }

    private Talks() {
    }

    /**
     * Who {@code self} faces now (an entity id): whoever is speaking ({@code speaker}, the last line's; 0 none yet), else, speaking itself,
     * whoever answers it ({@code next}, the next line's speaker) or it answered ({@code previous}); before the first line, the first speaker
     * ({@code next}); failing all of those, {@code anchor} (the one they gather round), or {@code other} (anyone else in it). Never itself.
     */
    public static int faces(int self, int speaker, int previous, int next, int anchor, int other) {
        for (int who : speaker != 0 && speaker != self ? new int[] {speaker} : new int[] {next, previous, anchor, other}) {
            if (who != 0 && who != self) {
                return who;
            }
        }
        return other != self ? other : 0;
    }

    /** A body's yaw {@code from} turned toward {@code to} (degrees) by at most {@code most}, the short way round. */
    public static float turn(float from, float to, float most) {
        float delta = ((to - from) % 360.0F + 540.0F) % 360.0F - 180.0F;
        return from + Math.max(-most, Math.min(most, delta));
    }

    /** True if a talk ended this way leaves room for a word about it (only walking off; a fight, a hurt, sleep, a player: nothing). */
    public static boolean remarked(End why) {
        return why == End.LEFT;
    }

    /**
     * What one who stays says when the other walked off mid-talk: a companion parent ({@code who} its persona key) or a child ({@code who}
     * its temper's word), by age ({@code stage}; a parent's line for a child, toddler or teen alike), {@code name} the one who left,
     * {@code call} what a child calls its companion parent. Null if it says nothing (a toddler sometimes just watches them go).
     */
    public static String cutShort(String who, ChildStage stage, String name, String call, int seed) {
        String key = who == null ? "" : who.toLowerCase(Locale.ROOT);
        List<String> lines = PARENT.get(key);
        if (lines == null) {
            String group = stage == ChildStage.TODDLER ? "little" : stage == ChildStage.TEEN ? "teen" : "child";
            lines = CHILD.get(key + ":" + group);
            if (lines == null) {
                lines = CHILD.get("any:" + group);
            }
        }
        if (lines == null || lines.isEmpty()) {
            return null;
        }
        String line = lines.get(Math.floorMod(seed, lines.size()));
        return line.isEmpty() ? null : line.replace("{name}", name == null || name.isBlank() ? "you" : name)
            .replace("{call}", call == null || call.isBlank() ? "Mom" : call);
    }

    /** Every key's lines (for the tests). */
    public static Map<String, List<String>> lines() {
        java.util.Map<String, List<String>> all = new java.util.TreeMap<>(CHILD);
        all.putAll(PARENT);
        return all;
    }

    private static final Map<String, List<String>> PARENT = Map.of(
        "touka", List.of("Hey. We're not done, {name}.", "...Fine. We'll finish this at home.", "Don't you walk away from me.",
            "Oi. I was talking to you."),
        "ken", List.of("...We'll talk later, okay?", "{name}? ...Okay. Later, then.", "Hey, wait- *sighs* Later.",
            "I wasn't finished, but... alright."),
        "eto", List.of("Running off mid-lecture? How very like you.", "Oh, we'll finish this, {name}. Later. Count on it.",
            "Hmm. Chapter unfinished. I'll write the ending myself.", "Fufu. Off you go, then."));

    private static final Map<String, List<String>> CHILD = Map.ofEntries(
        Map.entry("any:little", List.of("Where you going?", "Nooo, come back!", "{name}? ...{name}!", "", "*watches {name} go, lip wobbling*")),
        Map.entry("any:child", List.of("Hey! I wasn't done!", "...Okay, bye then.", "Wait, come back!", "Fine. Be that way.")),
        Map.entry("any:teen", List.of("Wow. Okay.", "We're finishing this later.", "...Cool. Bye, I guess.", "Rude.")),
        Map.entry("sunny:child", List.of("Aw, come back! It was just getting good!", "Bye, {name}! Talk later!", "Hey! Wait for me!")),
        Map.entry("sunny:teen", List.of("Later, {name}!", "Aw, and I had the best part left.")),
        Map.entry("shy:child", List.of("...oh. Okay.", "*waves a little, too late*", "...bye.")),
        Map.entry("shy:teen", List.of("...okay. Never mind.", "*shrugs, a little hurt*")),
        Map.entry("bold:child", List.of("Hey! Get back here, we're not done!", "Running away? Ha! Knew it!", "Coward!")),
        Map.entry("bold:teen", List.of("Yeah, walk away. Real mature.", "This isn't over, {name}.")),
        Map.entry("curious:child", List.of("Wait, where are you going? Why?", "Hey, I had more questions!")),
        Map.entry("curious:teen", List.of("...Where are they off to?", "Hm. I'll ask later.")),
        Map.entry("mischief:child", List.of("Hehe. I'm telling {call} you ran.", "Fine! More cake for me.", "Bet you can't catch me later!")),
        Map.entry("mischief:teen", List.of("Ha. Saved by whatever that was.", "I'll remember this, {name}~")),
        Map.entry("gentle:child", List.of("Oh... be careful, okay?", "Okay! Bye, {name}! Be safe!")),
        Map.entry("gentle:teen", List.of("Go on, it's fine. Later.", "Take care, {name}.")),
        Map.entry("serious:child", List.of("We'll finish this later. I'm keeping score.", "...Unbelievable.")),
        Map.entry("serious:teen", List.of("We'll pick this up later.", "Noted.")));
}
