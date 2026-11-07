package com.hex.kagune.control.server;

import com.hex.kagune.control.family.ChildMoment;
import com.hex.kagune.control.npc.ChildNpc;
import com.hex.kagune.control.npc.EtoNpc;
import com.hex.kagune.control.npc.HumanNpc;
import com.hex.kagune.control.npc.Turns;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 3.2.2: who has a player now ({@link Turns}: one at a time). Someone has them while walking up to them for something, in a hug, a kiss, on
 * their arm or by the hand (a companion, a villager partner), in a moment with them or walking up for one (a child), or waiting on their
 * answer to a card; a talk screen open has them too. Every few ticks it notes who last had each player, so the next waits {@link Turns#GAP}
 * after they let go. Whoever starts something by themselves asks {@link #busy} first; what the player asks for is never held back.
 */
@Mod.EventBusSubscriber(modid = "hexkagune")
public final class Engaged {
    private Engaged() {
    }

    /** How far around a player it looks (blocks), and how often it notes who has them (ticks). */
    static final double AROUND = 24.0;
    static final long EVERY = 5L;

    /** Who last had each player (an entity id) and on which tick. */
    private static final Map<UUID, long[]> LAST = WorldTimes.kept();

    @SubscribeEvent
    public static void levelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level) || level.m_46467_() % EVERY != 0L) {
            return;
        }
        long now = level.m_46467_();
        for (ServerPlayer player : level.m_6907_()) {
            try {
                int who = with(player, 0, now);
                if (who != 0) {
                    LAST.put(player.m_20148_(), new long[] {who, now});
                }
            } catch (RuntimeException failure) {
                com.hex.kagune.control.Safety.caught(failure, "who has a player");
            }
        }
    }

    /**
     * True if {@code me} (an entity id; 0: whoever asks) must wait before starting something with {@code player}: someone else has them
     * now, or let go of them less than {@link Turns#GAP} ago, or they are in a talk with someone else.
     */
    static boolean busy(ServerPlayer player, int me, long now) {
        return busy(player, me, now, true);
    }

    /** The same; {@code recent} false: only someone with them now counts (a homecoming at the door can't wait). */
    static boolean busy(ServerPlayer player, int me, long now, boolean recent) {
        if (talking(player) && !mine(player, me, now)) {
            return true;
        }
        long[] last = LAST.get(player.m_20148_());
        return Turns.waits(me, with(player, me, now), last == null ? 0 : (int)last[0], last == null ? -1L : now - last[1], recent);
    }

    /** True if someone other than {@code me} has {@code player} right now (not counting a talk screen, nor who had them a moment ago). */
    static boolean taken(ServerPlayer player, int me, long now) {
        return with(player, me, now) != 0;
    }

    /** True if a talk screen is open for {@code player} (with a companion, a child, a villager, a ghoul). */
    private static boolean talking(ServerPlayer player) {
        return EtoServer.talking(player) || ChildTalk.talking(player) || HumanTalk.talking(player) || GhoulNpcTalk.talking(player);
    }

    /** True if {@code me} is the one who has {@code player} (so their talk with it is no reason to wait). */
    private static boolean mine(ServerPlayer player, int me, long now) {
        if (me == 0) {
            return false;
        }
        Entity body = player.m_284548_().m_6815_(me);
        return body instanceof EtoNpc npc && has(npc, player) || body instanceof ChildNpc child && has(child, player)
            || body instanceof HumanNpc human && has(human, player, now) || SceneServer.waitingFor(player) == me;
    }

    /** Who (an entity id) other than {@code me} has {@code player} now; 0: nobody. */
    static int with(ServerPlayer player, int me, long now) {
        int card = SceneServer.waitingFor(player);
        if (card != 0 && card != me) {
            return card;
        }
        ServerLevel level = player.m_284548_();
        net.minecraft.world.phys.AABB around = player.m_20191_().m_82400_(AROUND);
        for (EtoNpc npc : level.m_6443_(EtoNpc.class, around, body -> body.m_6084_() && body.mind != null && body.m_19879_() != me)) {
            if (has(npc, player)) {
                return npc.m_19879_();
            }
        }
        for (ChildNpc child : level.m_6443_(ChildNpc.class, around, body -> body.m_6084_() && body.mind != null && body.m_19879_() != me)) {
            if (has(child, player)) {
                return child.m_19879_();
            }
        }
        for (HumanNpc human : level.m_6443_(HumanNpc.class, around, body -> body.m_6084_() && body.mind != null && body.m_19879_() != me)) {
            if (has(human, player, now)) {
                return human.m_19879_();
            }
        }
        return 0;
    }

    /** A companion has them: walking up to them, holding on to them (a hug, a kiss, an arm), by the hand, or being talked to. */
    private static boolean has(EtoNpc npc, ServerPlayer player) {
        EtoBrain.Mind mind = npc.mind;
        UUID id = player.m_20148_();
        return id.equals(mind.toward) || mind.with == player.m_19879_() || id.equals(mind.holding)
            || id.equals(mind.talking) && EtoServer.talkingWith(player, npc); // (her talk holds her a while after; only while it is open counts)
    }

    /** A child has them: in a moment with them, or walking up to them for one. Carried or following is not having them. */
    private static boolean has(ChildNpc child, ServerPlayer player) {
        ChildBrain.Mind mind = child.mind;
        UUID id = player.m_20148_();
        return mind.moment != ChildMoment.NONE && id.equals(mind.partner) || mind.pending != ChildMoment.NONE && id.equals(mind.pendingWith);
    }

    /** A villager has them: coming over to them, walking up for a kiss or a hug, in a kiss, or holding their hand. */
    private static boolean has(HumanNpc human, ServerPlayer player, long now) {
        HumanBrain.Mind mind = human.mind;
        UUID id = player.m_20148_();
        return id.equals(mind.comeTo) || mind.pending != 0 && id.equals(mind.with) && now < mind.pendingUntil
            || mind.kiss != null && id.equals(mind.with) || id.equals(mind.holding);
    }
}
