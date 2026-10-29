package com.hex.kagune.control.server;

import com.hex.kagune.KaguneMod;
import com.hex.kagune.control.ControlConfig;
import com.hex.kagune.control.ControlMath;
import com.hex.kagune.control.GhoulReserve;
import com.hex.kagune.control.network.ControlNetwork;
import com.hex.kagune.control.network.S2CControlState;
import com.hex.kagune.data.KaguneProfile;
import com.hex.kagune.gameplay.KaguneRuntime;
import com.hex.kagune.gameplay.MutationNode;
import com.hex.kagune.gameplay.TendrilMode;
import com.hex.kagune.network.KaguneNetwork;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Server hub for kagune control: computes control/hunger/frenzy, drives instinct urges and the combo
 * input buffer, drains food while manifested, awards Restraint practice and syncs everything to clients.
 * Runs at LOW priority so it always sees the original controller's work for the same tick.
 */
@Mod.EventBusSubscriber(modid = "hexkagune")
public final class ControlServer {
    /** ActionMastery key for practiced restraint (persisted in the existing profile NBT, old saves read 0). */
    public static final String RESTRAINT = "Restraint";
    private static final Map<UUID, ServerControlState> STATES = new HashMap<>();
    private static long nextErrorLogAt;

    private ControlServer() {
    }

    static ServerControlState state(ServerPlayer player) {
        return STATES.computeIfAbsent(player.m_20148_(), ignored -> new ServerControlState());
    }

    static ServerControlState peek(ServerPlayer player) {
        return STATES.get(player.m_20148_());
    }

    /** Current learned control 0..1 (cached for a second; recomputed immediately after progress changes). */
    public static float control(ServerPlayer player) {
        ServerControlState state = state(player);
        refreshControl(player, state, player.m_9236_().m_46467_());
        return state.control;
    }

    public static float hunger(ServerPlayer player) {
        return GhoulReserveServer.hunger(player);
    }

    public static float frenzy(ServerPlayer player) {
        ServerControlState state = peek(player);
        return state == null ? 0.0F : state.frenzy;
    }

    private static void refreshControl(ServerPlayer player, ServerControlState state, long now) {
        if (now >= state.controlRefreshAt || now < state.controlRefreshAt - 40L) {
            state.control = ControlMath.control(
                KaguneProfile.mastery(player),
                KaguneProfile.actionMastery(player, RESTRAINT),
                KaguneProfile.hasMutation(player, MutationNode.REACH),
                KaguneProfile.hasMutation(player, MutationNode.MULTI_CONTROL));
            state.controlRefreshAt = now + 20L;
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void playerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.player instanceof ServerPlayer player) {
            try {
                RinkakuSlingshot.tick(player);
                tick(player);
            } catch (RuntimeException failure) {
                long now = System.currentTimeMillis();
                if (now >= nextErrorLogAt) {
                    nextErrorLogAt = now + 30000L;
                    KaguneMod.LOGGER.error("HexKagune control tick failed for {}", player.m_36316_().getName(), failure);
                }
            }
        }
    }

    private static void tick(ServerPlayer player) {
        if (!KaguneProfile.isUnlocked(player)) {
            ServerControlState stale = peek(player);
            if (stale != null && (stale.frenzy > 0.0F || stale.urge.severity() > 0)) {
                stale.frenzy = 0.0F;
                stale.clearUrge();
                broadcast(player, stale);
            }
            return;
        }
        long now = player.m_9236_().m_46467_();
        ServerControlState state = state(player);
        refreshControl(player, state, now);
        state.hunger = GhoulReserveServer.hunger(player);
        boolean active = KaguneProfile.isActive(player) && player.m_6084_();
        KaguneRuntime.State runtime = active ? KaguneRuntime.peek(player.m_20148_()) : null;
        if (!KaguneProfile.isActive(player) && now % 10L == 0L && !UkakuServer.isUkaku(player)) {
            // A Rinkaku kakuja K brought out ends when the kagune goes back in.
            UkakuForms.kagunePutAway(player);
        }

        if (runtime != null && runtime.mode == TendrilMode.MANIFEST && runtime.modeStarted != state.lastManifestStart) {
            state.lastManifestStart = runtime.modeStarted;
            state.manifestSettled = false;
            state.frenzy = ControlMath.clamp01(state.frenzy + ControlMath.manifestFrenzy(state.control));
            state.dirty = true;
        } else if (runtime != null && !state.manifestSettled && runtime.mode != TendrilMode.MANIFEST) {
            state.manifestSettled = true;
            if (runtime.mode != TendrilMode.DISMISS && state.control < 0.85F && now >= state.nextEnduranceAwardAt) {
                // Enduring a full, painful manifestation is practice in its own right.
                state.nextEnduranceAwardAt = now + 1200L;
                awardRestraint(player, state, 1);
            }
        }

        float floor = active ? ControlMath.frenzyFloor(state.control, state.hunger) : 0.0F;
        if (state.frenzy > floor) {
            // Bloodlust settles back down; mastered ghouls calm faster.
            state.frenzy = Math.max(floor, state.frenzy - ControlMath.frenzyDecay(state.control, active));
        } else {
            // Starvation slowly gnaws the host back up to its floor.
            state.frenzy = Math.min(floor, state.frenzy + 0.002F);
        }
        state.frenzy = ControlMath.clamp01(state.frenzy);

        // 7.0.0: with the shared bar the kagune out already costs RC, which is the reserve: no second sip.
        if (active && GhoulReserveServer.applies(player) && false) {
            float drain = ControlConfig.manifestHungerDrain();
            if (drain > 0.0F) {
                float exhaustion = ControlMath.manifestExhaustion(KaguneProfile.manifestedLimbs(player), state.control) * drain;
                GhoulReserveServer.set(player, GhoulReserve.drain(GhoulReserveServer.get(player), exhaustion));
            }
        }

        if (runtime != null) {
            KaguneCombat.serverTick(player, runtime, now);
            RinkakuAutonomy.tick(player, runtime, now);
            KaguneInstinct.tick(player, runtime, state, now);
        } else if (state.urge.severity() > 0) {
            KaguneInstinct.end(player, state, now, false);
        }

        sync(player, state, now, active);
    }

    private static void sync(ServerPlayer player, ServerControlState state, long now, boolean active) {
        boolean drifted = Math.abs(state.control - state.syncedControl) > 0.02F
            || Math.abs(state.hunger - state.syncedHunger) > 0.03F
            || Math.abs(state.frenzy - state.syncedFrenzy) > 0.03F;
        if (state.dirty || drifted && now >= state.nextTrackerSyncAt) {
            broadcast(player, state);
            state.nextTrackerSyncAt = now + 20L;
            state.nextSelfSyncAt = now + 10L;
        } else if (active && now >= state.nextSelfSyncAt) {
            ControlNetwork.sendTo(player, snapshot(player, state));
            state.nextSelfSyncAt = now + 10L;
        }
    }

    static void broadcast(ServerPlayer player, ServerControlState state) {
        state.dirty = false;
        state.syncedControl = state.control;
        state.syncedHunger = state.hunger;
        state.syncedFrenzy = state.frenzy;
        ControlNetwork.broadcast(player, snapshot(player, state));
    }

    /** Immediately pushes the latest state (used when a combo stage or urge starts). */
    static void broadcastNow(ServerPlayer player) {
        if (KaguneProfile.isUnlocked(player)) {
            broadcast(player, state(player));
        }
    }

    static S2CControlState snapshot(ServerPlayer player, ServerControlState state) {
        KaguneCombat.Combo combo = KaguneCombat.peek(player);
        boolean showCombo = combo != null && combo.stage >= 0;
        return new S2CControlState(
            player.m_20148_(),
            state.control,
            state.hunger,
            state.frenzy,
            state.urge.ordinal(),
            state.urgeStarted,
            state.urgeDuration,
            state.urgeTargetId,
            state.urgeStrength,
            state.urgeRestrained,
            showCombo ? combo.style.ordinal() : 0,
            showCombo ? combo.stage : -1,
            showCombo ? combo.startedAt : 0L,
            showCombo ? combo.duration : 0,
            showCombo ? combo.limbMask : 0,
            showCombo ? combo.deviation : 0.0F,
            showCombo && combo.involuntary);
    }

    static void awardRestraint(ServerPlayer player, ServerControlState state, int amount) {
        if (amount <= 0) {
            return;
        }
        KaguneProfile.addMastery(player, RESTRAINT, amount);
        state.controlRefreshAt = Long.MIN_VALUE;
        refreshControl(player, state, player.m_9236_().m_46467_());
        state.dirty = true;
        KaguneNetwork.broadcast(player);
    }

    static void addFrenzy(ServerPlayer player, float amount) {
        if (amount <= 0.0F || !KaguneProfile.isUnlocked(player)) {
            return;
        }
        ServerControlState state = state(player);
        float before = state.frenzy;
        state.frenzy = ControlMath.clamp01(state.frenzy + amount);
        if (state.frenzy - before > 0.08F) {
            state.dirty = true;
        }
    }

    static void calm(ServerPlayer player, float amount) {
        ServerControlState state = peek(player);
        if (state != null && amount > 0.0F) {
            state.frenzy = Math.max(0.0F, state.frenzy - amount);
            state.dirty = true;
        }
    }

    /** Called by the combo system after an authoritative strike damaged {@code targets} entities. */
    static void onKaguneHit(ServerPlayer player, int targets) {
        addFrenzy(player, ControlMath.hitFrenzy(control(player), targets));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void hostHurt(LivingHurtEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && KaguneProfile.isActive(player) && event.getAmount() > 0.0F) {
            addFrenzy(player, ControlMath.hurtFrenzy(control(player), event.getAmount()));
        }
    }

    @SubscribeEvent
    public static void hostKilled(LivingDeathEvent event) {
        if (event.getSource().m_7639_() instanceof ServerPlayer player && player != event.getEntity() && KaguneProfile.isActive(player)) {
            addFrenzy(player, ControlMath.killFrenzy(control(player)));
        }
    }

    @SubscribeEvent
    public static void loggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ServerControlState state = state(player);
            state.clearUrge();
            state.urgeHintsShown = 0;
            state.dirty = true;
        }
    }

    @SubscribeEvent
    public static void loggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            STATES.remove(player.m_20148_());
            KaguneCombat.forget(player);
        }
    }

    @SubscribeEvent
    public static void respawned(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ServerControlState state = state(player);
            state.frenzy = 0.0F;
            state.clearUrge();
            state.dirty = true;
            KaguneCombat.forget(player);
        }
    }

    @SubscribeEvent
    public static void changedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ServerControlState state = state(player);
            state.clearUrge();
            state.dirty = true;
            KaguneCombat.forget(player);
        }
    }

    @SubscribeEvent
    public static void startTracking(PlayerEvent.StartTracking event) {
        if (event.getEntity() instanceof ServerPlayer viewer && event.getTarget() instanceof ServerPlayer subject
            && KaguneProfile.isUnlocked(subject)) {
            ControlNetwork.sendTo(viewer, snapshot(subject, state(subject)));
        }
    }

    @SubscribeEvent
    public static void commands(RegisterCommandsEvent event) {
        ControlCommands.register(event.getDispatcher());
    }

    static void tell(ServerPlayer player, String translationKey) {
        player.m_5661_(Component.m_237115_(translationKey), true);
    }
}
