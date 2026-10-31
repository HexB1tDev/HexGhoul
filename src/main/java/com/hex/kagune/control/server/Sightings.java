package com.hex.kagune.control.server;

import com.hex.kagune.control.family.ChildStage;
import com.hex.kagune.control.npc.ChildNpc;
import com.hex.kagune.control.npc.Display;
import com.hex.kagune.control.npc.EtoNpc;
import com.hex.kagune.control.npc.GhoulNpc;
import com.hex.kagune.control.npc.ReactionLines;
import com.hex.kagune.data.KaguneProfile;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 1.3.11: what each player shows of what they are ({@link Display}: their eyes, their kagune, a kakuja, a final kakuja or the Dragon),
 * looked at twice a second; when it rises, every NPC near who sees them reacts, each its own way ({@link Watcher}): Eto by who they are
 * to her (amused, annoyed in her Library, impressed, worried for her partner), ghoul NPCs that don't know them afraid of a kakuja and
 * worse (they run), their followers impressed, their own children amazed or frightened. Every NPC to come adds its own watcher.
 */
@Mod.EventBusSubscriber(modid = "hexkagune")
public final class Sightings {
    /** One kind of NPC's way of reacting: {@code player} shows {@code shown} (more than {@code before}) near it. */
    public interface Watcher {
        void saw(ServerLevel level, ServerPlayer player, Display shown, Display before, long now);
    }

    private static final List<Watcher> WATCHERS = new ArrayList<>();
    private static final Map<UUID, Display> SHOWN = new HashMap<>();
    /** How far an NPC notices (blocks). */
    static final double NOTICE = 16.0;

    static {
        WATCHERS.add(Sightings::eto);
        WATCHERS.add(Sightings::ghouls);
        WATCHERS.add(Sightings::children);
    }

    private Sightings() {
    }

    public static void watch(Watcher watcher) {
        WATCHERS.add(watcher);
    }

    /** What {@code player} shows now. */
    public static Display of(ServerPlayer player) {
        if (com.hex.kagune.control.DragonLock.dragon(player) || KingServer.awake(player)) {
            return Display.DREAD;
        }
        boolean owl = PaleServer.peek(player) != null && PaleServer.peek(player).owl;
        if (owl || UkakuServer.kakuja(player) || GraftServer.kakuja(player) || KaguneProfile.isActive(player) && KaguneProfile.form(player).isKakuja()) {
            return Display.KAKUJA;
        }
        if (KaguneProfile.isActive(player)) {
            return Display.KAGUNE;
        }
        return PaleServer.eyes(player) ? Display.EYES : Display.NONE;
    }

    /** Last shown (NONE if never looked at). */
    public static Display last(ServerPlayer player) {
        return SHOWN.getOrDefault(player.m_20148_(), Display.NONE);
    }

    @SubscribeEvent
    public static void playerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player) || !(player.m_9236_() instanceof ServerLevel level)
            || (level.m_46467_() + player.m_19879_()) % 10L != 0L || !player.m_6084_()) {
            return;
        }
        Display shown = of(player);
        Display before = SHOWN.put(player.m_20148_(), shown);
        if (shown.rose(before == null ? Display.NONE : before)) {
            long now = level.m_46467_();
            for (Watcher watcher : WATCHERS) {
                try {
                    watcher.saw(level, player, shown, before == null ? Display.NONE : before, now);
                } catch (RuntimeException failure) {
                    com.hex.kagune.control.Safety.caught(failure, "an NPC seeing a player transform");
                }
            }
        }
    }

    @SubscribeEvent
    public static void loggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        SHOWN.remove(event.getEntity().m_20148_());
    }

    // ---------------------------------------------------------------- the NPCs

    private static void eto(ServerLevel level, ServerPlayer player, Display shown, Display before, long now) {
        for (EtoNpc npc : level.m_6443_(EtoNpc.class, player.m_20191_().m_82400_(NOTICE), eto -> eto.m_6084_() && eto.m_142582_(player))) {
            EtoBrain.saw(npc, player, shown, level, now);
        }
    }

    private static void ghouls(ServerLevel level, ServerPlayer player, Display shown, Display before, long now) {
        boolean told = false;
        for (GhoulNpc npc : level.m_6443_(GhoulNpc.class, player.m_20191_().m_82400_(NOTICE), ghoul -> ghoul.m_6084_() && ghoul.mind != null && ghoul.m_142582_(player))) {
            boolean ally = player.m_20148_().equals(npc.mind.owner);
            if (!ally && shown.frightening()) {
                // It runs from what it can't fight (unless it is already at their throat).
                if (npc.mind.prey != player.m_19879_() || shown == Display.DREAD) {
                    npc.mind.scaredUntil = now + (shown == Display.DREAD ? 240L : 160L);
                }
            }
            if (!told && npc.m_20280_(player) <= 12.0 * 12.0 && (ally || shown != Display.EYES)) {
                told = true;
                String name = npc.identity() == null ? "Ghoul" : npc.identity().displayName();
                player.m_5661_(Component.m_237113_("§7" + name + ": " + (ally ? ReactionLines.ally(shown, (int)(now / 10L))
                    : ReactionLines.stranger(shown, (int)(now / 10L)))), false);
            }
        }
    }

    private static void children(ServerLevel level, ServerPlayer player, Display shown, Display before, long now) {
        String me = Pregnancies.player(player.m_20148_());
        for (ChildNpc npc : level.m_6443_(ChildNpc.class, player.m_20191_().m_82400_(NOTICE), child -> child.m_6084_())) {
            Children.Child child = Children.get(level, npc.childId());
            if (child == null || !child.parent(me) || shown == Display.EYES && now % 3L != 0L) {
                continue;
            }
            ChildStage stage = child.stage(now);
            ChildBrain.say(player, child, ReactionLines.child(stage, com.hex.kagune.control.family.ChildLines.call(stage, ChildBrain.isMother(child, player)), shown,
                npc.m_217043_().m_188503_(100)));
        }
    }
}
