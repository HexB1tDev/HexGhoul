package com.hex.kagune.control.server;

import com.hex.kagune.control.family.ChildStage;
import com.hex.kagune.control.npc.ChildNpc;
import com.hex.kagune.control.npc.Display;
import com.hex.kagune.control.npc.EtoNpc;
import com.hex.kagune.control.npc.FearLines;
import com.hex.kagune.control.npc.FinalSight;
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
 * 1.3.65: the worst sights told apart ({@link FinalSight}: War, Peace, the Dragon, Rinkaku's and Ukaku's final kakujas), each with its own
 * words ({@link FearLines}); War and the Dragon kept up while worn ({@code terror}): wild ghouls keep running, their children hide. Humans
 * and the CCG look for themselves ({@link #seen}).
 */
@Mod.EventBusSubscriber(modid = "hexkagune")
public final class Sightings {
    /** One kind of NPC's way of reacting: {@code player} shows {@code shown} (more than {@code before}) near it. */
    public interface Watcher {
        void saw(ServerLevel level, ServerPlayer player, Display shown, Display before, long now);
    }

    private static final List<Watcher> WATCHERS = new ArrayList<>();
    private static final Map<UUID, Display> SHOWN = new HashMap<>();
    /** 1.3.65: which of the worst sights each player was when last looked at, and when a ghoul last shouted about them. */
    private static final Map<UUID, FinalSight> SIGHT = new HashMap<>();
    private static final Map<UUID, Long> SHOUTED = new HashMap<>();
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

    /**
     * What {@code player} shows now ({@link Display#of}). 1.3.64: dread while they are the Dragon, wear War or Peace (worn now: it was
     * {@code KingServer.awake}, the save that the Dragon once went through them, so a War/Peace-path player always read as dread and NPCs
     * reacted once, never again), or are in Rinkaku's final kakuja or Ukaku's (both read as a kakuja before).
     */
    public static Display of(ServerPlayer player) {
        return of(player, finalOf(player));
    }

    private static Display of(ServerPlayer player, FinalSight sight) {
        boolean active = KaguneProfile.isActive(player);
        boolean owl = PaleServer.peek(player) != null && PaleServer.peek(player).owl;
        boolean kakuja = owl || UkakuServer.kakuja(player) || GraftServer.kakuja(player) || active && KaguneProfile.form(player).isKakuja();
        return Display.of(sight == FinalSight.DRAGON, sight == FinalSight.WAR || sight == FinalSight.PEACE,
            sight == FinalSight.RINKAKU || sight == FinalSight.UKAKU, kakuja, active, PaleServer.eyes(player));
    }

    /** 2.0: true while {@code player} is in a kakuja of any kind (rinkaku's, ukaku's, a graft's, the Owl). */
    public static boolean kakuja(ServerPlayer player) {
        boolean owl = PaleServer.peek(player) != null && PaleServer.peek(player).owl;
        return owl || UkakuServer.kakuja(player) || GraftServer.kakuja(player) || KaguneProfile.isActive(player) && KaguneProfile.form(player).isKakuja();
    }

    /** 1.3.65: which of the worst sights {@code player} is now ({@link FinalSight}: told apart; NONE for anything less). */
    public static FinalSight finalOf(ServerPlayer player) {
        boolean rinkaku = KaguneProfile.isActive(player) && PathServer.path(player) == com.hex.kagune.control.KagunePath.RINKAKU
            && KaguneProfile.form(player) == com.hex.kagune.control.RinkakuForms.finalKakuja();
        boolean ukaku = UkakuServer.kakuja(player) && UkakuServer.form(player).finalKakuja();
        return FinalSight.of(com.hex.kagune.control.DragonLock.dragon(player), KingServer.wearing(player), rinkaku, ukaku);
    }

    /** 1.3.65: which of the worst sights {@code player} was when last looked at (twice a second; NONE if never). */
    public static FinalSight seen(ServerPlayer player) {
        return SIGHT.getOrDefault(player.m_20148_(), FinalSight.NONE);
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
        FinalSight sight = finalOf(player);
        Display shown = of(player, sight);
        Display before = SHOWN.put(player.m_20148_(), shown);
        FinalSight was = SIGHT.put(player.m_20148_(), sight);
        long now = level.m_46467_();
        // 1.3.65: one of the worst sights turned into another (the Dragon gone and War on) is seen again too.
        if (shown.rose(before == null ? Display.NONE : before) || sight.any() && sight != was) {
            for (Watcher watcher : WATCHERS) {
                try {
                    watcher.saw(level, player, shown, before == null ? Display.NONE : before, now);
                } catch (RuntimeException failure) {
                    com.hex.kagune.control.Safety.caught(failure, "an NPC seeing a player transform");
                }
            }
        }
        if (sight.kingly()) {
            try {
                dread(level, player, sight, now); // 2.1.9
            } catch (RuntimeException failure) {
                com.hex.kagune.control.Safety.caught(failure, "NPCs before Peace or War");
            }
        }
        if (sight.terror()) {
            try {
                terror(level, player, sight, now);
            } catch (RuntimeException failure) {
                com.hex.kagune.control.Safety.caught(failure, "NPCs fleeing War");
            }
        }
    }

    @SubscribeEvent
    public static void loggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        SHOWN.remove(event.getEntity().m_20148_());
        SIGHT.remove(event.getEntity().m_20148_());
        SHOUTED.remove(event.getEntity().m_20148_());
    }

    /**
     * 1.3.65: War or the Dragon, kept up twice a second while it is worn (not only as it is put on): every wild ghoul that sees it within
     * {@link FinalSight#KEEPS_RUNNING} keeps running from it (now and then one shouts), and their own children near hide from them
     * ({@code ChildBrain.dread}: behind the other parent).
     */
    private static void terror(ServerLevel level, ServerPlayer player, FinalSight sight, long now) {
        boolean shout = now - SHOUTED.getOrDefault(player.m_20148_(), Long.MIN_VALUE / 2) >= FinalSight.SHOUTS;
        for (GhoulNpc npc : level.m_6443_(GhoulNpc.class, player.m_20191_().m_82400_(FinalSight.KEEPS_RUNNING),
                ghoul -> ghoul.m_6084_() && ghoul.mind != null && ghoul.m_142582_(player))) {
            if (player.m_20148_().equals(npc.mind.owner) || sight.kingly() && npc.identity() != null
                && FinalSight.ghoul(npc.identity().tier(), com.hex.kagune.control.npc.NpcIdentity.TIERS) == FinalSight.Nerve.SHAKE) {
                continue; // 2.1.9: the strongest wild ghouls stand before War, trembling (dread)
            }
            npc.mind.scaredUntil = Math.max(npc.mind.scaredUntil, now + FinalSight.STILL_SCARED);
            npc.mind.scaredOf = player.m_19879_();
            if (shout && npc.m_20280_(player) <= 20.0 * 20.0) {
                shout = false;
                SHOUTED.put(player.m_20148_(), now);
                com.hex.kagune.control.network.ControlNetwork.sendTo(player, new com.hex.kagune.control.network.S2CNpcSay(npc.m_19879_(),
                    FearLines.fleeing(sight, npc.m_217043_().m_188503_(100))));
            }
        }
        String me = Pregnancies.player(player.m_20148_());
        for (ChildNpc npc : level.m_6443_(ChildNpc.class, player.m_20191_().m_82400_(NOTICE), child -> child.m_6084_() && child.mind != null)) {
            Children.Child child = Children.get(level, npc.childId());
            if (child != null && child.parent(me) && (npc.m_20280_(player) <= 8.0 * 8.0 || npc.m_142582_(player))) {
                ChildBrain.dread(npc, player, level, now);
            }
        }
    }

    /**
     * 2.1.9: Peace or War in sight, kept up twice a second: investigators of Rank 0 to 4 and the weaker wild ghouls run (and keep
     * running while it is in sight); Rank 5 to 7 and the strongest wild ghouls stand, trembling, their nerve giving now and then; Rank 8 and
     * above stand as they are.
     */
    private static void dread(ServerLevel level, ServerPlayer player, FinalSight sight, long now) {
        net.minecraft.world.phys.AABB around = player.m_20191_().m_82400_(FinalSight.DREAD_RANGE);
        for (com.hex.kagune.control.npc.CcgAgent agent : level.m_6443_(com.hex.kagune.control.npc.CcgAgent.class, around,
                a -> a.m_6084_() && a.mind != null && (a.m_142582_(player) || a.m_20280_(player) <= 10.0 * 10.0))) {
            switch (FinalSight.investigator(AgentBrain.rankLevel(agent))) {
                case RUN -> {
                    agent.mind.routedFrom = player.m_19879_();
                    agent.mind.routedUntil = Math.max(agent.mind.routedUntil, now + FinalSight.STILL_RUNNING);
                }
                case SHAKE -> {
                    agent.mind.afraidOf = player.m_19879_();
                    agent.mind.afraidUntil = Math.max(agent.mind.afraidUntil, now + FinalSight.STILL_SHAKING);
                }
                default -> {
                }
            }
        }
        for (GhoulNpc npc : level.m_6443_(GhoulNpc.class, around, ghoul -> ghoul.m_6084_() && ghoul.mind != null && ghoul.identity() != null
                && (ghoul.m_142582_(player) || ghoul.m_20280_(player) <= 10.0 * 10.0))) {
            if (player.m_20148_().equals(npc.mind.owner)) {
                continue;
            }
            if (FinalSight.ghoul(npc.identity().tier(), com.hex.kagune.control.npc.NpcIdentity.TIERS) == FinalSight.Nerve.RUN) {
                npc.mind.scaredUntil = Math.max(npc.mind.scaredUntil, now + FinalSight.STILL_RUNNING);
                npc.mind.scaredOf = player.m_19879_();
            } else {
                npc.mind.afraidOf = player.m_19879_();
                npc.mind.afraidUntil = Math.max(npc.mind.afraidUntil, now + FinalSight.STILL_SHAKING);
            }
        }
    }

    // ---------------------------------------------------------------- the NPCs

    private static void eto(ServerLevel level, ServerPlayer player, Display shown, Display before, long now) {
        for (EtoNpc npc : level.m_6443_(EtoNpc.class, player.m_20191_().m_82400_(NOTICE), eto -> eto.m_6084_() && eto.m_142582_(player))) {
            EtoBrain.saw(npc, player, shown, seen(player), level, now);
        }
    }

    private static void ghouls(ServerLevel level, ServerPlayer player, Display shown, Display before, long now) {
        boolean told = false;
        FinalSight sight = seen(player);
        for (GhoulNpc npc : level.m_6443_(GhoulNpc.class, player.m_20191_().m_82400_(NOTICE), ghoul -> ghoul.m_6084_() && ghoul.mind != null && ghoul.m_142582_(player))) {
            boolean ally = player.m_20148_().equals(npc.mind.owner);
            boolean shaker = sight.kingly() && npc.identity() != null
                && FinalSight.ghoul(npc.identity().tier(), com.hex.kagune.control.npc.NpcIdentity.TIERS) == FinalSight.Nerve.SHAKE; // 2.1.9
            if (!ally && shown.frightening() && !shaker) {
                // It runs from what it can't fight (unless it is already at their throat). 1.3.65: from them (not the nearest player), as
                // long as the sight is bad (War and the Dragon longest, and kept up while in sight: terror).
                if (npc.mind.prey != player.m_19879_() || shown == Display.DREAD) {
                    npc.mind.scaredUntil = Math.max(npc.mind.scaredUntil, now + (sight.any() ? sight.scares() : shown == Display.DREAD ? 240L : 160L));
                    npc.mind.scaredOf = player.m_19879_();
                }
            }
            if (!told && npc.m_20280_(player) <= 12.0 * 12.0 && (ally || shown != Display.EYES)) {
                told = true;
                // 1.3.30: in a bubble over its head, not the chat. 1.3.65: the worst sights each their own words.
                int seed = (int)(now / 10L);
                String line = sight.any() ? ally ? FearLines.ally(sight, seed) : FearLines.stranger(sight, seed)
                    : ally ? ReactionLines.ally(shown, seed) : ReactionLines.stranger(shown, seed);
                com.hex.kagune.control.network.ControlNetwork.sendTo(player, new com.hex.kagune.control.network.S2CNpcSay(npc.m_19879_(), line));
            }
        }
    }

    private static void children(ServerLevel level, ServerPlayer player, Display shown, Display before, long now) {
        String me = Pregnancies.player(player.m_20148_());
        for (ChildNpc npc : level.m_6443_(ChildNpc.class, player.m_20191_().m_82400_(NOTICE), child -> child.m_6084_())) {
            Children.Child child = Children.get(level, npc.childId());
            if (child != null && !child.parent(me) && shown != Display.NONE && (npc.m_20280_(player) <= 12.0 * 12.0 || npc.m_142582_(player))) {
                stranger(level, player, npc, child, shown, now); // 2.0.5: someone else's child
                continue;
            }
            if (child == null || !child.parent(me) || shown == Display.EYES && now % 3L != 0L) {
                continue;
            }
            ChildStage stage = child.stage(now);
            String call = com.hex.kagune.control.family.ChildLines.call(stage, ChildBrain.isMother(child, player));
            FinalSight sight = seen(player);
            int seed = npc.m_217043_().m_188503_(100);
            ChildBrain.say(player, npc, child, sight.any() ? FearLines.child(stage, call, sight, seed) : ReactionLines.child(stage, call, shown, seed));
            if (sight.terror() && npc.mind != null) {
                ChildBrain.dread(npc, player, level, now); // 1.3.65: it hides from them (behind the other parent)
            }
        }
    }

    /**
     * 2.0.5: someone else's child sees a stranger show their kakugan, kagune or worse: its words by its age and temper; a kagune frightens
     * a little one (a kakuja and worse anyone) into hiding behind a parent, or keeping off ({@code ChildBrain.dread}).
     */
    private static void stranger(ServerLevel level, ServerPlayer player, ChildNpc npc, Children.Child child, Display shown, long now) {
        ChildStage stage = child.stage(now);
        com.hex.kagune.control.family.ChildTemper temper = com.hex.kagune.control.family.ChildTemper.of(child.id, child.mother, child.father);
        ChildBrain.say(player, npc, child, com.hex.kagune.control.family.StrangerLines.scared(stage, temper, shown, npc.m_217043_().m_188503_(100)));
        boolean frightened = shown.frightening() || shown == Display.KAGUNE && !stage.older();
        if (frightened && npc.mind != null) {
            ChildBrain.dread(npc, player, level, now, shown.frightening() ? 200L : 120L);
        }
    }
}
