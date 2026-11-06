package com.hex.kagune.control.server;

import com.hex.kagune.control.npc.EtoRules;
import com.hex.kagune.control.npc.NpcBonds;
import com.hex.kagune.control.npc.NpcMemory;
import com.hex.kagune.control.npc.Persona;
import com.hex.kagune.control.npc.Romance;
import com.hex.kagune.control.npc.RomanceCard;
import com.hex.kagune.control.npc.UniqueNpc;
import com.hex.kagune.control.quest.QuestLog;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * 1.3.2: love between players and the one-of-a-kind NPCs, as any of them shares it ({@link Romance}). Love shown to one of them (a date,
 * a kiss, a hug, a ring, a night) is felt by every other one the player is together with: each is jealous a day. Jealousy never removes
 * a relationship or drains affection. (Every NPC made later calls {@link #romanced} for its own love; Eto is the first.)
 */
public final class RomanceServer {
    /** How long a partner stays jealous (game ticks: a day). */
    static final long JEALOUS_FOR = 24000L;

    private RomanceServer() {
    }

    /**
     * 1.3.29: the journal's Romance tab: how {@code player} stands with each companion (met, stage, regard, errands, days together, the home,
     * children, a child on the way), sent with their quest log.
     */
    public static List<RomanceCard> cards(ServerPlayer player) {
        ServerLevel level = player.m_284548_();
        long day = Math.floorDiv(level.m_46468_(), 24000L);
        boolean woman = com.hex.kagune.control.Genders.female(player);
        NpcMemory memory = NpcMemoryServer.of(player);
        List<RomanceCard> out = new ArrayList<>();
        for (Persona persona : Persona.ALL) {
            String key = persona.key();
            QuestLog.Bond bond = EtoServer.courtBond(player, key);
            UniqueNpc unique = UniqueNpcs.get(level, key);
            Romance.Stage stage = unique.stageOf(player.m_20148_());
            String claimedBy = unique.claimedFrom(player.m_20148_()) ? unique.partnerName : null;
            long since = memory.get(key, stage == Romance.Stage.MARRIED ? NpcMemory.WED : NpcMemory.SINCE, -1L);
            com.hex.kagune.control.family.FamilyView family = EtoServer.family(level, player, memory, persona);
            boolean home = stage == Romance.Stage.MARRIED && Households.of(level, player.m_20148_()) != null;
            out.add(new RomanceCard(key, bond.has(NpcBonds.MET), stage, bond.regard(), persona.likes(woman), claimedBy, EtoRules.step(bond),
                EtoRules.count(bond), stage.together() ? Romance.days(since, day) : 0L, family.children(), home, family.carrying() >= 0.0,
                com.hex.kagune.control.npc.Courtship.closed(bond, com.hex.kagune.control.npc.Courtship.CHAPTERS)));
        }
        return out;
    }

    /** {@code player} was sweet on the NPC {@code key}: the others they are together with take it badly. */
    public static void romanced(ServerPlayer player, String key) {
        ServerLevel level = player.m_284548_();
        long now = level.m_46467_();
        for (Map.Entry<String, Romance.Stage> other : UniqueNpcs.partnersOf(level, player, key).entrySet()) {
            int loss = Romance.jealousy(other.getValue(), false);
            if (loss != 0) {
                QuestLog.Bond bond = QuestServer.bond(player, other.getKey());
                QuestServer.bond(player, other.getKey(), bond.regard(bond.regard() + loss));
            }
            NpcMemoryServer.set(player, other.getKey(), NpcMemory.JEALOUS, now + JEALOUS_FOR);
        }
    }
}
