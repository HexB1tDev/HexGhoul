package com.hex.kagune.control;

import com.hex.kagune.KaguneMod;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.world.entity.Entity;

/**
 * 6.0.10: nothing in this mod may take a player off the server or the server down. Every way Minecraft calls into it while the
 * game runs (every event listener, the original's and ours, server and client; the kagune spike's tick, hits and saves; the
 * bleeding effect's tick; the spike's renderer) is wrapped at build time ({@code patch/KaguneGuard}) in a catch that lands here:
 * the error is logged (in full the first time from each place, then a count now and then) and the game carries on. A spike whose
 * tick or hit failed is removed, so it can't fail again every tick. Out of memory and the like are let through: nothing can carry
 * on from those.
 */
public final class Safety {
    private static final Map<String, AtomicInteger> FAILURES = new ConcurrentHashMap<>();

    private Safety() {
    }

    /** Bytecode hook: {@code failure} escaped {@code where}; logged, and the game carries on. */
    public static void caught(Throwable failure, String where) {
        if (failure instanceof VirtualMachineError && !(failure instanceof StackOverflowError)) {
            throw (VirtualMachineError)failure;
        }
        int times = FAILURES.computeIfAbsent(where, ignored -> new AtomicInteger()).incrementAndGet();
        try {
            if (times == 1) {
                KaguneMod.LOGGER.error("HexKagune caught an error in " + where + " and carried on (nothing was crashed or kicked)", failure);
            } else if (times % 1000 == 0) {
                KaguneMod.LOGGER.error("HexKagune: " + where + " has failed " + times + " times, each caught (last: " + failure + ")", failure);
            }
        } catch (RuntimeException | LinkageError ignored) {
            // Not even the log may take the game down.
        }
    }

    /** Bytecode hook: the entity whose tick or hit failed is removed, so it can't fail again every tick. */
    public static void discard(Entity entity) {
        try {
            entity.m_146870_();
        } catch (RuntimeException | LinkageError ignored) {
            // Already gone, or its world is: nothing more to do.
        }
    }

    /** How many times {@code where} has failed (for tests and diagnostics). */
    public static int failures(String where) {
        AtomicInteger count = FAILURES.get(where);
        return count == null ? 0 : count.get();
    }
}
