package com.hex.kagune.control;

import java.util.UUID;
import java.util.function.Predicate;
import net.minecraft.server.level.ServerPlayer;

/**
 * 1.0.4: while the Dragon (Kakuja V2, {@code control.awakening}) has a player's body, nothing the player presses may act. The
 * original kagune actions ({@code KagunePatch.patchDragonLock}) and every action packet of the expansion ask {@link #locked}
 * first. The Dragon installs its test when it loads, so the expansion never needs to know the Dragon's classes.
 */
public final class DragonLock {
    private static volatile Predicate<UUID> test = id -> false;

    private DragonLock() {
    }

    /** Called once by the Dragon at common setup. */
    public static void install(Predicate<UUID> locked) {
        test = locked == null ? id -> false : locked;
    }

    /**
     * True while {@code player} is the Dragon, or (1.0.5) wears a final kakuja ({@code KingServer}: it has his body, the kagune stays
     * in): the kagune's keys, attacks and abilities and every action packet of the expansion do nothing. The final kakuja's own
     * requests ask {@link #dragon} only.
     */
    public static boolean locked(ServerPlayer player) {
        return player != null && (test.test(player.m_20148_()) || com.hex.kagune.control.server.KingServer.locks(player.m_20148_()));
    }

    /** True while {@code player} is the Dragon. */
    public static boolean dragon(ServerPlayer player) {
        return player != null && test.test(player.m_20148_());
    }
}
