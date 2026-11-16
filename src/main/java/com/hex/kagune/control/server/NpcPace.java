package com.hex.kagune.control.server;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

/**
 * 4.1.100 (the lag patch, part 2): how often an NPC looks round for something to fight (a search of every living thing within its sight,
 * and a line of sight for each one it would go for): every half second ({@link #NEAR_LOOK}) while a player is within {@link #FAR} blocks of
 * it, every two seconds ({@link #FAR_LOOK}) when none is (nobody sees it then: the game draws mobs out to about 64 blocks). A fight it is in
 * goes on at its own pace: only the look for a new foe is slowed.
 */
public final class NpcPace {
    public static final double FAR = 64.0;
    public static final long NEAR_LOOK = 10L;
    public static final long FAR_LOOK = 40L;

    private NpcPace() {
    }

    /** Pure: the look is due after {@code ago} ticks (far from every player or not). */
    public static boolean due(long ago, boolean far) {
        return ago >= (far ? FAR_LOOK : NEAR_LOOK);
    }

    /** True if no player of {@code level} (spectators too: they see) is within {@link #FAR} blocks of {@code body}. */
    public static boolean far(Entity body, ServerLevel level) {
        for (ServerPlayer player : level.m_6907_()) {
            if (player.m_20280_(body) <= FAR * FAR) {
                return false;
            }
        }
        return true;
    }

    /** The look is due for {@code body}, {@code ago} ticks after its last (the players looked for only when it matters). */
    public static boolean looks(Entity body, ServerLevel level, long ago) {
        return ago >= FAR_LOOK || ago >= NEAR_LOOK && !far(body, level);
    }
}
