package com.hex.kagune.control.server;

import com.hex.kagune.control.npc.NpcMemory;
import com.hex.kagune.control.npc.Romance;
import com.hex.kagune.control.quest.QuestLog;
import java.util.Map;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * 1.3.2: love between players and the one-of-a-kind NPCs, as any of them shares it ({@link Romance}). Love shown to one of them (a date,
 * a kiss, a hug, a ring, a night) is felt by every other one the player is together with: each is jealous a day, and thinks less of
 * them, more if they are married. (Every NPC made later calls {@link #romanced} for its own love; Eto is the first.)
 */
public final class RomanceServer {
    /** How long a partner stays jealous (game ticks: a day). */
    static final long JEALOUS_FOR = 24000L;

    private RomanceServer() {
    }

    /** {@code player} was sweet on the NPC {@code key}: the others they are together with take it badly. */
    public static void romanced(ServerPlayer player, String key) {
        ServerLevel level = player.m_284548_();
        long now = level.m_46467_();
        for (Map.Entry<String, Romance.Stage> other : UniqueNpcs.partnersOf(level, player, key).entrySet()) {
            int loss = Romance.jealousy(other.getValue(), false);
            QuestLog.Bond bond = QuestServer.bond(player, other.getKey());
            QuestServer.bond(player, other.getKey(), bond.regard(bond.regard() + loss));
            NpcMemoryServer.set(player, other.getKey(), NpcMemory.JEALOUS, now + JEALOUS_FOR);
        }
    }
}
