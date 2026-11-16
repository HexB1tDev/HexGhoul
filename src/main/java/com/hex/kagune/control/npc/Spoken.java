package com.hex.kagune.control.npc;

import java.util.Map;

/**
 * 4.2.4 (the owner: "There should be such a thing as one-time options, like one-time dialogue options."; "Currently, you can just spam one
 * option and it'll give you relationship. It's a bug like that."): what a player has said to one of the one-of-a-kind NPCs for good, and the
 * bookkeeping of what was paid today (pure, tested). Kept in their NPC memory ({@link NpcMemory}) under the topic's id, so a topic keeps its
 * marks however many topics come before or after it:
 *
 * <ul>
 *   <li>{@code o_<topic>}: bit {@code i} once the topic's {@code i}-th reply marked {@link EtoTopics.Follow#once} was said, bit
 *   {@link #TOPIC_BIT} once a topic marked {@link EtoTopics.Topic#once} was opened. Never forgotten (forgotten with everything else of
 *   the NPC: {@link NpcMemory#forgetAll}).</li>
 *   <li>{@code p_<topic>}: {@code (day << 9) | replies paid that day}: each reply's warmth paid once a day.</li>
 *   <li>{@code c_<choice>}: the last game day a choice of the first page (or a keyed act: a kiss half asleep, an apology) paid its
 *   warmth; once a day, whatever the script says it is worth.</li>
 * </ul>
 */
public record Spoken(Map<String, Long> once) {
    /** Nothing said yet (a preview, a test, a context made before 4.2.4). */
    public static final Spoken NONE = new Spoken(Map.of());
    /** The bit of {@code o_<topic>} for the topic itself (its replies are 0..7: a page holds eight). */
    public static final int TOPIC_BIT = 8;
    /** How many replies a topic may have (a page's eight slots). */
    public static final int MOST_FOLLOWS = 8;

    public Spoken {
        once = once == null ? Map.of() : Map.copyOf(once);
    }

    /** Its memory name for the one-time marks of {@code topic}. */
    public static String onceKey(String topic) {
        return "o_" + topic;
    }

    /** Its memory name for what of {@code topic} was paid today. */
    public static String paidKey(String topic) {
        return "p_" + topic;
    }

    /** Its memory name for the last day {@code what} (a choice's name, or a keyed act's) paid its warmth. */
    public static String choiceKey(String what) {
        StringBuilder out = new StringBuilder("c_");
        for (char c : what.toLowerCase(java.util.Locale.ROOT).toCharArray()) {
            out.append(c >= 'a' && c <= 'z' || c >= '0' && c <= '9' ? c : '_');
        }
        return out.length() > 24 ? out.substring(0, 24) : out.toString();
    }

    /** True once {@code topic}'s reply {@code follow} (a one-time one) was said. */
    public boolean said(String topic, int follow) {
        Long marks = this.once.get(onceKey(topic));
        return marks != null && follow >= 0 && follow < MOST_FOLLOWS && (marks & 1L << follow) != 0L;
    }

    /** True once {@code topic} (a one-time one) was opened. */
    public boolean opened(String topic) {
        Long marks = this.once.get(onceKey(topic));
        return marks != null && (marks & 1L << TOPIC_BIT) != 0L;
    }

    /** {@code marks} with reply {@code bit} (or {@link #TOPIC_BIT}) set. */
    public static long mark(long marks, int bit) {
        return bit < 0 || bit > TOPIC_BIT ? marks : marks | 1L << bit;
    }

    /** What of a topic was paid on {@code day}, from what is stored ({@code stored}: 0 or less for nothing). */
    public static long paid(long stored, long day) {
        return stored > 0L && stored >>> 9 == day ? stored & 0x1FFL : 0L;
    }

    /** True if reply {@code follow} of a topic is paid on {@code day} (not yet that day). */
    public static boolean pays(long stored, long day, int follow) {
        return follow >= 0 && follow < MOST_FOLLOWS && (paid(stored, day) & 1L << follow) == 0L;
    }

    /** What to store once reply {@code follow} is paid on {@code day}. */
    public static long pay(long stored, long day, int follow) {
        long mask = paid(stored, day);
        if (follow >= 0 && follow < MOST_FOLLOWS) {
            mask |= 1L << follow;
        }
        return day << 9 | mask;
    }

    /** True if a choice last paid on {@code last} pays on {@code day} (once a day). */
    public static boolean choicePays(long last, long day) {
        return last != day;
    }

    /** {@code regard} as paid: in full the first time today, never more after (what hurts always hurts). */
    public static int warmth(int regard, boolean first) {
        return first ? regard : Math.min(0, regard);
    }
}
