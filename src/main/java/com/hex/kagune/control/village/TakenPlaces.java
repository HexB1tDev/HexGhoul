package com.hex.kagune.control.village;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 4.1.14: where each one-of-a-kind place was built in one world (pure, tested; kept with the world by PlaceClaims): the first spot the world
 * makes for a place takes it, every other spot is refused from then on, and a spot given up (nothing was built there after all) lets the
 * next one take it. Safe from any thread.
 */
public final class TakenPlaces {
    /** By place id: its start chunk {x, z}. */
    private final Map<String, int[]> taken = new LinkedHashMap<>();

    /** {@code id}'s start chunk {x, z}, or null if none is taken. */
    public synchronized int[] at(String id) {
        int[] at = this.taken.get(id);
        return at == null ? null : at.clone();
    }

    /** {@code id} built at ({@code x}, {@code z}): true if that is its spot (just taken, or taken before), false if another holds it. */
    public synchronized boolean take(String id, int x, int z) {
        int[] at = this.taken.get(id);
        if (at == null) {
            this.taken.put(id, new int[] {x, z});
            return true;
        }
        return at[0] == x && at[1] == z;
    }

    /** {@code id}'s spot ({@code x}, {@code z}) given up; true if it was its spot. */
    public synchronized boolean release(String id, int x, int z) {
        int[] at = this.taken.get(id);
        if (at != null && at[0] == x && at[1] == z) {
            this.taken.remove(id);
            return true;
        }
        return false;
    }

    /** The place id whose spot is ({@code x}, {@code z}), or null. */
    public synchronized String whose(int x, int z) {
        for (Map.Entry<String, int[]> entry : this.taken.entrySet()) {
            if (entry.getValue()[0] == x && entry.getValue()[1] == z) {
                return entry.getKey();
            }
        }
        return null;
    }

    /** As saved: "id:x,z;id:x,z". */
    public synchronized String write() {
        StringBuilder out = new StringBuilder();
        for (Map.Entry<String, int[]> entry : this.taken.entrySet()) {
            if (out.length() > 0) {
                out.append(';');
            }
            out.append(entry.getKey()).append(':').append(entry.getValue()[0]).append(',').append(entry.getValue()[1]);
        }
        return out.toString();
    }

    /** Read back from {@link #write}; anything it can't read is left out (as if that place were not built yet). */
    public static TakenPlaces read(String text) {
        TakenPlaces out = new TakenPlaces();
        if (text == null) {
            return out;
        }
        for (String entry : text.split(";")) {
            int colon = entry.indexOf(':');
            if (colon <= 0) {
                continue;
            }
            String[] at = entry.substring(colon + 1).split(",");
            try {
                out.taken.put(entry.substring(0, colon), new int[] {Integer.parseInt(at[0].trim()), Integer.parseInt(at[1].trim())});
            } catch (RuntimeException unreadable) {
                // a broken entry: as if that place were not built yet
            }
        }
        return out;
    }
}
