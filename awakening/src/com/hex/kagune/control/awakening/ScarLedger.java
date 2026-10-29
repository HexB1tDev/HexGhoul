package com.hex.kagune.control.awakening;

import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;

/**
 * Bookkeeping for temporary terrain destruction, independent of Minecraft so the restore schedule can
 * be tested. Each destroyed position remembers its original state exactly once; repeated hits never
 * overwrite it.
 *
 * @param <S> the remembered block state
 */
final class ScarLedger<S> {
    static final int MAX_SCARS = 60_000;

    static final class Scar<S> {
        final long position;
        final S original;
        long restoreAt;
        int retries;

        Scar(long position, S original, long restoreAt) {
            this.position = position;
            this.original = original;
            this.restoreAt = restoreAt;
        }
    }

    private final Map<Long, Scar<S>> scars = new HashMap<>();
    private final PriorityQueue<Scar<S>> due = new PriorityQueue<>((a, b) -> Long.compare(a.restoreAt, b.restoreAt));

    boolean contains(long position) {
        return scars.containsKey(position);
    }

    int size() {
        return scars.size();
    }

    boolean full() {
        return scars.size() >= MAX_SCARS;
    }

    /** Records a destroyed position; returns false if it is already scarred or the ledger is full. */
    boolean record(long position, S original, long restoreAt) {
        if (full() || scars.containsKey(position)) return false;
        Scar<S> scar = new Scar<>(position, original, restoreAt);
        scars.put(position, scar);
        due.add(scar);
        return true;
    }

    /** Next scar whose time has come, or null. The caller must {@link #finish} or {@link #defer} it. */
    Scar<S> poll(long now) {
        while (!due.isEmpty()) {
            Scar<S> head = due.peek();
            if (scars.get(head.position) != head) {
                due.poll();
                continue;
            }
            if (head.restoreAt > now) return null;
            return due.poll();
        }
        return null;
    }

    void finish(Scar<S> scar) {
        scars.remove(scar.position, scar);
    }

    void defer(Scar<S> scar, long until, boolean countRetry) {
        scar.restoreAt = until;
        if (countRetry) scar.retries++;
        due.add(scar);
    }

    /** Pulls every pending restore forward so it completes no later than {@code latest}. */
    void hasten(long now, long latest) {
        if (scars.isEmpty()) return;
        due.clear();
        long span = Math.max(1L, latest - now);
        int index = 0;
        int count = scars.size();
        for (Scar<S> scar : scars.values()) {
            if (scar.restoreAt > latest) scar.restoreAt = now + span * index / Math.max(1, count);
            due.add(scar);
            index++;
        }
    }

    Iterable<Scar<S>> all() {
        return scars.values();
    }

    void clear() {
        scars.clear();
        due.clear();
    }
}
