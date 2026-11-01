package com.hex.kagune.control.npc;

import java.util.ArrayList;
import java.util.List;

/**
 * 1.3.30: what an NPC says, shown in a bubble over its head instead of the chat (the owner: the chat was spammed nonstop) (pure, tested):
 * its words wrapped to a few short lines, how long they stay up (longer for more words), what an action ("*waves*") looks like.
 */
public final class Speech {
    /** Characters on a line of the bubble, at most; lines in it, at most (the rest cut with an ellipsis). */
    public static final int WIDTH = 34;
    public static final int LINES = 4;
    /** How long a bubble stays up: at least, per character, at most (ticks). */
    public static final int LEAST = 60;
    public static final int MOST = 220;

    private Speech() {
    }

    /** {@code text} broken into the bubble's lines at spaces (a word too long for a line is cut), never more than {@link #LINES}. */
    public static List<String> wrap(String text) {
        List<String> out = new ArrayList<>();
        if (text == null) {
            return out;
        }
        String rest = text.strip().replaceAll("\\s+", " ");
        while (!rest.isEmpty()) {
            if (out.size() == LINES - 1 && rest.length() > WIDTH) {
                out.add(rest.substring(0, Math.max(0, WIDTH - 3)).strip() + "...");
                return out;
            }
            if (rest.length() <= WIDTH) {
                out.add(rest);
                break;
            }
            int cut = rest.lastIndexOf(' ', WIDTH);
            if (cut <= 0) {
                cut = WIDTH;
            }
            out.add(rest.substring(0, cut).strip());
            rest = rest.substring(cut).strip();
        }
        return out;
    }

    /** How long {@code text} stays up (ticks): long enough to read. */
    public static int ticks(String text) {
        int length = text == null ? 0 : text.length();
        return Math.max(LEAST, Math.min(MOST, 40 + length * 2));
    }

    /** One line of the bubble with its look: an action (in asterisks) grey and slanted, words white. Formatting codes stripped first. */
    public static String styled(String line, boolean action) {
        String bare = line.replaceAll("§.", "");
        return (action ? "§7§o" : "§f") + bare;
    }

    /** True if {@code text} is all an action ("*waves*"). */
    public static boolean action(String text) {
        String t = text == null ? "" : text.strip();
        return t.length() > 1 && t.startsWith("*") && t.endsWith("*") && t.indexOf('*', 1) == t.length() - 1;
    }
}
