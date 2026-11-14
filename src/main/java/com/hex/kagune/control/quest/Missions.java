package com.hex.kagune.control.quest;

import com.hex.kagune.control.npc.JoinTrial;
import java.util.List;

/**
 * 4.1.95 (the owner, quoted exactly: "I suggest that every mission now gets into a temporary category of the quests, which are like this
 * thing that Kaneki gives you is a quest, sort of. It's a timed quest. And this should apply for every single mission in the game that is
 * not considered a quest. They are now meant to be considered one. If it's failed, it's going to show it's going to show as failed for a
 * few seconds in the timed category, as long as the cooldown is active. Because when you fail a quest, you should wait at least 1 Minecraft
 * day before going there again ... after the quest is completed, or failed, depending on what happened, all the items that the player got
 * should be removed, even items they used to craft items with ... If they're completed, you move them to the done tab, and they cannot
 * redo this mission ever again"): the game's missions as timed quests (pure). Each is one of the gangs' trials ({@link JoinTrial.Kind}), or
 * Ayato's spar for his courtship ({@link #COURT_SPAR}); it shows as a quest of the {@link Quest.Line#TIMED} line: its title, how it is
 * done, what it asks, its clock. Its state is kept in the {@link QuestLog} ({@link QuestLog.Mission}).
 */
public final class Missions {
    private Missions() {
    }

    /** A mission on (its clock running), failed (not to be asked for again until a day has passed), done (never again). */
    public static final int ON = 0;
    public static final int FAILED = 1;
    public static final int DONE = 2;
    /** How long after failing a mission it may not be asked for again: one Minecraft day (ticks). */
    public static final long COOLDOWN = 24000L;
    /** Ayato's spar for his courtship (the same fight as Aogiri's, its own mission). */
    public static final String COURT_SPAR = "COURT_SPAR";
    /** The tag kept on everything a mission gave (and on whatever is made from it): taken back when it ends. */
    public static final String ITEM_TAG = "HexMission";

    /** The mission kind of trial {@code kind} ({@code courting}: held for a courtship). */
    public static String kind(JoinTrial.Kind kind, boolean courting) {
        return courting && kind == JoinTrial.Kind.SPAR ? COURT_SPAR : kind.name();
    }

    /** The trial mission {@code kind} is, or null if none. */
    public static JoinTrial.Kind trial(String kind) {
        if (COURT_SPAR.equals(kind)) {
            return JoinTrial.Kind.SPAR;
        }
        for (JoinTrial.Kind one : JoinTrial.Kind.values()) {
            if (one.name().equals(kind)) {
                return one;
            }
        }
        return null;
    }

    /** How long mission {@code kind} may take (ticks). */
    public static long limit(String kind) {
        JoinTrial.Kind trial = trial(kind);
        return trial == null ? 6000L : JoinTrial.limit(trial);
    }

    /** The quest mission {@code kind} shows as ("mission.shift": its title, how it is done, what it asks), or null if it is no mission. */
    public static Quest quest(String kind) {
        JoinTrial.Kind trial = trial(kind);
        if (trial == null) {
            return null;
        }
        boolean court = COURT_SPAR.equals(kind);
        String title = court ? "Ayato: a spar, for him" : JoinTrial.title(trial);
        String goal = court ? "Spar with Ayato (bring him to half; never fall under a quarter)"
            : JoinTrial.goal(JoinTrial.gang(trial), JoinTrial.step(trial));
        String story = JoinTrial.how(trial) + " Timed: " + minutes(limit(kind)) + ". If it fails, it can't be asked for again for a "
            + "Minecraft day. Whatever you were given for it is taken back when it ends, and anything made from it.";
        // Its one objective is the trial itself: no event counts on it (the trial's own server says when it is won or lost).
        return new Quest("mission." + kind.toLowerCase(java.util.Locale.ROOT), Quest.Line.TIMED, null, title, story, 0, List.of(), null,
            List.of(Objective.of(goal, Signal.TIME, 1, "hexkagune.mission")), Reward.NONE);
    }

    /** Who gave mission {@code quest} ("A mission from Ken, to join Anteiku"), as its quest says where it comes from. */
    public static String from(Quest quest) {
        String kind = quest.id().startsWith("mission.") ? quest.id().substring(8).toUpperCase(java.util.Locale.ROOT) : "";
        JoinTrial.Kind trial = trial(kind);
        if (trial == null) {
            return "A mission";
        }
        if (COURT_SPAR.equals(kind)) {
            return "A mission from Ayato, for his heart";
        }
        String leader = switch (trial) {
            case SPAR, AMBUSH -> "Ayato";
            case RING -> "Roma";
            case GAS, GATE -> "Banjou";
            case SHIFT, CAFE -> "Ken";
            case HUNT -> "the General";
        };
        return "A mission from " + leader + ", to join " + com.hex.kagune.control.npc.GangPlayer.title(JoinTrial.gang(trial));
    }

    /** What doing mission {@code quest} brings, as its quest's rewards say it. */
    public static String pays(Quest quest) {
        return COURT_SPAR.equalsIgnoreCase(quest.id().substring(Math.min(quest.id().length(), 8))) ? "the next chapter of his courtship"
            : "the next step of your way in";
    }

    /** {@code ticks} as the menu says a length ("5 minutes"). */
    public static String minutes(long ticks) {
        long m = Math.max(1L, Math.round(ticks / 1200.0));
        return m + (m == 1 ? " minute" : " minutes");
    }

    /** A clock's time left, as "m:ss" ({@code ticks} left, never under 0). */
    public static String clock(long ticks) {
        long seconds = Math.max(0L, ticks) / 20L;
        return seconds / 60L + ":" + (seconds % 60L < 10L ? "0" : "") + seconds % 60L;
    }
}
