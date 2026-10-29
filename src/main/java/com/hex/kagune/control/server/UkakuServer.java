package com.hex.kagune.control.server;

import com.hex.kagune.KaguneMod;
import com.hex.kagune.control.ComboStyle;
import com.hex.kagune.control.KagunePath;
import com.hex.kagune.control.ukaku.UkakuMutation;
import com.hex.kagune.control.ukaku.UkakuRules;
import com.hex.kagune.control.ukaku.WingForm;
import com.hex.kagune.control.network.ControlNetwork;
import com.hex.kagune.control.network.S2CUkakuState;
import com.hex.kagune.data.KaguneProfile;
import com.hex.kagune.gameplay.KaguneServerController;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Server owner of the Ukaku kit: the kagune's form (the wings by mastery, or a kakuja while K has him transformed)
 * and the transient kagune state every client needs to draw it. Runs after the original controller each tick (LOW
 * priority), like the control system.
 */
@Mod.EventBusSubscriber(modid = "hexkagune")
public final class UkakuServer {
    private static final Map<UUID, UkakuState> STATES = new HashMap<>();
    /** Profile key: his unlocked mutations (a bitmask of UkakuMutation). */
    static final String MUTATIONS_KEY = "UkakuMutations";
    private static long nextErrorLogAt;

    /** Transient per-player Ukaku state on the logical server. */
    static final class UkakuState {
        /** Transformed into a kakuja (K): lasts while the kagune is out. */
        boolean kakuja;
        /** The kakuja is the final one (K held, mastery 1000). */
        boolean finalKakuja;
        /** The Wing Vortex (Z): the wing thrown (+1 right, -1 left, 0 none yet), the tick it leaves, the tick a new one starts to grow. */
        int vortexSide;
        long vortexAt = Long.MIN_VALUE;
        long vortexRegrowAt = Long.MIN_VALUE;
        /** Next tick Z may throw a wing (UkakuVortex). */
        long nextVortexAt = Long.MIN_VALUE;
        /** The Gatling (J): firing, the tick it went on or off, until when the trigger counts as held, and its fire bookkeeping. */
        boolean gatling;
        long gatlingChangedAt = Long.MIN_VALUE;
        long gatlingHeldUntil = Long.MIN_VALUE;
        double gatlingAccum;
        int gatlingShot;
        /** Next time a "wings can't do that" reminder may show (UkakuGates). */
        long nextNoticeAt = Long.MIN_VALUE;
        /** The crystal mode (V): the wings crystallized from root to tip, the M1 and right click fire crystal spikes. */
        boolean crystalMode;
        /** Game time the crystal mode last went on or off (the clients time the crystal's climb from it). */
        long crystalChangedAt = Long.MIN_VALUE;
        boolean dirty = true;
        /**
         * The form and mutations every client was last sent (5.0.16). His mastery grows the wings (the second one at 100) and a
         * command can set it, and nothing else marks the state dirty then: until something like the crystal mode did, clients
         * kept drawing the one wing.
         */
        WingForm sentForm;
        int sentMutations = -1;
        /** 5.0.18: the Steel Blade (C in the final kakuja): the steel wings closed round his right arm, and since when. */
        boolean blade;
        long bladeChangedAt = Long.MIN_VALUE;

        /** Forget everything in flight (death, respawn): the wings come back calm. */
        void calm() {
            this.kakuja = false;
            this.finalKakuja = false;
            this.vortexSide = 0;
            this.vortexAt = Long.MIN_VALUE;
            this.vortexRegrowAt = Long.MIN_VALUE;
            this.gatling = false;
            this.gatlingChangedAt = Long.MIN_VALUE;
            this.crystalMode = false;
            this.crystalChangedAt = Long.MIN_VALUE;
            this.blade = false;
            this.bladeChangedAt = Long.MIN_VALUE;
            this.dirty = true;
        }
    }

    private UkakuServer() {
    }

    public static boolean isUkaku(ServerPlayer player) {
        return KaguneProfile.isUnlocked(player) && PathServer.path(player) == KagunePath.UKAKU;
    }

    /** 6.2.0: every running cooldown of his ended now ({@link Cooldowns}): the vortex (a wing still growing back still grows). */
    static void clearCooldowns(ServerPlayer player) {
        UkakuState state = STATES.get(player.m_20148_());
        if (state != null) {
            state.nextVortexAt = Long.MIN_VALUE;
        }
    }

    /** The state if this player has any yet (never creates one). */
    static UkakuState peek(ServerPlayer player) {
        return STATES.get(player.m_20148_());
    }

    static UkakuState state(ServerPlayer player) {
        return STATES.computeIfAbsent(player.m_20148_(), ignored -> new UkakuState());
    }

    /**
     * The kagune in use: his first or final kakuja while K has him transformed, otherwise the wings his mastery has grown (one
     * small wing, then the twin wings at 100).
     */
    public static WingForm form(ServerPlayer player) {
        int mastery = KaguneProfile.mastery(player);
        UkakuState state = STATES.get(player.m_20148_());
        if (state != null && state.kakuja && mastery >= UkakuForms.KAKUJA_MASTERY) {
            return state.finalKakuja ? WingForm.FINAL_KAKUJA.usable(mastery) : WingForm.KAKUJA;
        }
        return WingForm.TWIN.usable(mastery);
    }

    /** His unlocked mutations (a bitmask of {@link UkakuMutation}). */
    public static int mutations(ServerPlayer player) {
        return KaguneProfile.root(player).m_128451_(MUTATIONS_KEY) & UkakuMutation.all();
    }

    /** True if he has unlocked {@code mutation}. */
    public static boolean hasMutation(ServerPlayer player, UkakuMutation mutation) {
        return isUkaku(player) && mutation.in(mutations(player));
    }

    /** The Ukaku menu asks to unlock a mutation: it must not be had yet, and a milestone point must be left to spend. */
    static void unlockMutation(ServerPlayer player, int ordinal) {
        UkakuMutation mutation = UkakuMutation.byOrdinal(ordinal);
        int mastery = KaguneProfile.mastery(player);
        int mask = mutations(player);
        // 7.0.1: quests pay mutation points too; some mutations also need their quest.
        if (mutation == null || !UkakuMutation.canUnlock(mastery, mask, mutation, QuestServer.points(player))
            || !QuestServer.pass(player, com.hex.kagune.control.quest.Gates.ukaku(mutation))) {
            return;
        }
        KaguneProfile.root(player).m_128405_(MUTATIONS_KEY, mask | mutation.bit());
        player.m_5661_(Component.m_237110_("message.hexkagune.ukaku.mutation_unlocked", mutation.displayName()), true);
        KaguneServerController.playSelf(player, com.hex.kagune.sound.HexKaguneSounds.KAGUNE_RESHAPE, 0.8F, KaguneServerController.vary(player, 1.3F, 0.05F));
        state(player).dirty = true;
    }

    /** Tells him an ability is still locked: "{@code name} unlocks at mastery {@code mastery}" (see UkakuUnlock). */
    static void locked(ServerPlayer player, String name, int mastery) {
        player.m_5661_(locked(name, mastery), true);
    }

    static Component locked(String name, int mastery) {
        return Component.m_237110_("message.hexkagune.ukaku.locked", name, mastery);
    }

    /** True while K has him transformed into a kakuja (the first or the final). */
    public static boolean kakuja(ServerPlayer player) {
        UkakuState state = STATES.get(player.m_20148_());
        return state != null && state.kakuja;
    }

    /**
     * K: transforms into a kakuja ({@code finalForm}: the final one), from one kakuja into the other, or back to the wings.
     * Whatever the kagune was doing stops (a Gatling, the crystal mode); every client sees the change (and
     * plays the transformation from when it learns of it).
     */
    static void setKakuja(ServerPlayer player, boolean on, boolean finalForm) {
        UkakuState state = state(player);
        boolean fin = on && finalForm;
        if (state.kakuja == on && state.finalKakuja == fin) {
            return;
        }
        state.kakuja = on;
        state.finalKakuja = fin;
        long now = player.m_9236_().m_46467_();
        if (state.gatling) {
            state.gatling = false;
            state.gatlingChangedAt = now;
        }
        setCrystal(player, state, false, now);
        state.dirty = true;
    }

    /** The combo Ukaku fights with right now (wing form and the crystal mode). */
    public static ComboStyle comboStyle(ServerPlayer player, long now) {
        if (blade(player)) {
            return ComboStyle.UKAKU_STEEL_BLADE;
        }
        return UkakuRules.style(KaguneProfile.mastery(player), form(player), false, crystalMode(player));
    }

    /** 5.0.18: true while the Steel Blade is out (the final kakuja's steel wings closed round his right arm). */
    public static boolean blade(ServerPlayer player) {
        UkakuState state = STATES.get(player.m_20148_());
        return state != null && state.blade;
    }

    /**
     * C (the original's guard key, free on the Ukaku path since 5.0.13): in the final kakuja the steel wings close round his right
     * arm into the Steel Blade; C again opens them.
     */
    static void toggleBlade(ServerPlayer player) {
        UkakuState state = state(player);
        long now = player.m_9236_().m_46467_();
        if (!state.blade && form(player) != WingForm.FINAL_KAKUJA) {
            player.m_5661_(Component.m_237113_("Steel Blade: only in the final kakuja (hold K)"), true);
            return;
        }
        if (state.bladeChangedAt != Long.MIN_VALUE && now - state.bladeChangedAt < com.hex.kagune.control.ukaku.SteelBlade.FORM_TICKS
            && now >= state.bladeChangedAt) {
            return;
        }
        state.blade = !state.blade;
        state.bladeChangedAt = now;
        state.dirty = true;
        net.minecraft.world.phys.Vec3 arm = player.m_20182_().m_82520_(0.0, 1.2, 0.0);
        UkakuFire.sound(player.m_284548_(), arm, state.blade ? "item.trident.return" : "item.armor.equip_iron", 1.2F,
            KaguneServerController.vary(player, state.blade ? 0.6F : 0.8F, 0.05F));
        UkakuFire.sound(player.m_284548_(), arm, "block.anvil.place", 0.5F, KaguneServerController.vary(player, 1.6F, 0.05F));
    }

    /**
     * True while wing {@code side} (+1 right, -1 left) is off his back: thrown as a Wing Vortex and not yet half grown back.
     * Shots that would leave from it leave from the other wing.
     */
    public static boolean wingGone(ServerPlayer player, int side, long now) {
        UkakuState state = STATES.get(player.m_20148_());
        return state != null && state.vortexSide == side && state.vortexAt != Long.MIN_VALUE && now >= state.vortexAt
            && now < state.vortexRegrowAt + UkakuRules.REGROW_TICKS / 2;
    }

    /** True while the crystal mode is on. */
    public static boolean crystalMode(ServerPlayer player) {
        UkakuState state = STATES.get(player.m_20148_());
        return state != null && state.crystalMode;
    }

    /**
     * V (the original's Lift key, which Ukaku wings have no use for): the wings crystallize from the shoulder blades up to
     * the tips and the M1 and right click fire crystal spikes; V again thaws them back into fire. Needs
     * {@link UkakuRules#CRYSTAL_MASTERY}, and the wings out (the original only passes V on while they are).
     */
    static void toggleCrystal(ServerPlayer player) {
        UkakuState state = state(player);
        long now = player.m_9236_().m_46467_();
        int mastery = KaguneProfile.mastery(player);
        if (!state.crystalMode && !UkakuRules.crystalModeAllowed(mastery, form(player))) {
            String key = form(player) == WingForm.ONE_WING ? "message.hexkagune.ukaku.crystal_one_wing"
                : form(player).kakuja() ? "message.hexkagune.ukaku.kakuja_no_wings" : "message.hexkagune.ukaku.crystal_locked";
            player.m_5661_(Component.m_237110_(key, new Object[] {UkakuRules.CRYSTAL_MASTERY}), true);
            return;
        }
        if (state.crystalChangedAt != Long.MIN_VALUE && now - state.crystalChangedAt < UkakuRules.CRYSTAL_TOGGLE_TICKS
            && now >= state.crystalChangedAt) {
            return;
        }
        setCrystal(player, state, !state.crystalMode, now);
    }

    /** Turns the crystal mode on or off (with its sound for everyone near), and tells every client. */
    private static void setCrystal(ServerPlayer player, UkakuState state, boolean on, long now) {
        if (state.crystalMode == on) {
            return;
        }
        state.crystalMode = on;
        state.crystalChangedAt = now;
        state.dirty = true;
        UkakuCrystalCombat.modeChanged(player, on);
    }

    static S2CUkakuState snapshot(ServerPlayer player) {
        UkakuState state = state(player);
        return new S2CUkakuState(player.m_20148_(), form(player), state.vortexSide, state.vortexAt, state.vortexRegrowAt,
            state.crystalMode, state.crystalChangedAt, state.gatling, state.gatlingChangedAt, mutations(player), state.blade, state.bladeChangedAt);
    }

    static void broadcast(ServerPlayer player) {
        UkakuState state = state(player);
        state.dirty = false;
        S2CUkakuState snapshot = snapshot(player);
        state.sentForm = snapshot.form();
        state.sentMutations = snapshot.mutations();
        ControlNetwork.broadcast(player, snapshot);
    }

    /**
     * Called by {@link PathServer#setPath}: a new path starts from a clean kit and every client learns the wing form.
     * Becoming Ukaku parks the Rinkaku-only progression ({@link RinkakuStash}); leaving the Ukaku path puts it back.
     */
    static void onPathChanged(ServerPlayer player) {
        UkakuState state = STATES.remove(player.m_20148_());
        KagunePath path = PathServer.path(player);
        boolean ukaku = path == KagunePath.UKAKU;
        // 6.1.0: Pale's state goes too; her path, like Ukaku's, keeps the neutral seed and parks Rinkaku's progression (7.0.0: and
        // Graft DNA's).
        PaleServer.onPathChanged(player);
        GraftServer.onPathChanged(player);
        if (ukaku || path == KagunePath.PALE || path == KagunePath.GRAFT) {
            RinkakuStash.stash(player);
        } else {
            RinkakuStash.restore(player);
            // His spikes are only ticked while he is an Ukaku: the ones in flight vanish, the stuck ones fall out.
            UkakuCrystalCombat.forget(player);
            UkakuPyre.forget(player);
            UkakuThorns.forget(player);
        }
        if (state != null || ukaku) {
            broadcast(player);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void playerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) {
            return;
        }
        try {
            tick(player);
        } catch (RuntimeException failure) {
            long now = System.currentTimeMillis();
            if (now >= nextErrorLogAt) {
                nextErrorLogAt = now + 30000L;
                KaguneMod.LOGGER.error("HexKagune Ukaku tick failed", failure);
            }
        }
    }

    private static void tick(ServerPlayer player) {
        if (!isUkaku(player)) {
            return;
        }
        UkakuState state = state(player);
        long now = player.m_9236_().m_46467_();
        if (now % 20L == 0L) {
            // Backstop: whatever path a Rinkaku form or mutation took into this profile, it is parked within a second.
            RinkakuStash.stash(player);
        }
        if (state.kakuja && (!KaguneProfile.isActive(player) || !player.m_6084_() || KaguneProfile.mastery(player) < UkakuForms.KAKUJA_MASTERY)) {
            // The kakuja lasts while the kagune is out: it comes out as the wings again next time.
            state.kakuja = false;
            state.finalKakuja = false;
            state.dirty = true;
        }
        if (state.crystalMode && (!KaguneProfile.isActive(player) || !player.m_6084_()
            || !UkakuRules.crystalModeAllowed(KaguneProfile.mastery(player), form(player)))) {
            // The wings went back in (or changed form): they come out as fire again.
            setCrystal(player, state, false, now);
        }
        if (state.blade && (form(player) != WingForm.FINAL_KAKUJA || !KaguneProfile.isActive(player) || !player.m_6084_())) {
            // Out of the final kakuja: the wings open again.
            state.blade = false;
            state.bladeChangedAt = now;
            state.dirty = true;
        }
        // 5.0.15: held by a Rinkaku's tails in the crystal mode, thorns erupt from him and the kagune lets go.
        UkakuThorns.tick(player, state.crystalMode, now);
        if (KaguneProfile.isActive(player) && player.m_6084_()) {
            // Ukaku speed (canon C8): faster on his feet and in the air while the wings are out. Same refresh as the
            // original's form buffs (short, ambient, never overriding a stronger effect).
            int mastery = KaguneProfile.mastery(player);
            // Agility Boost: much faster and a far higher jump.
            boolean agile = hasMutation(player, UkakuMutation.AGILITY_BOOST);
            // 5.0.18: the Steel Blade: far faster and a far higher jump.
            int blade = state.blade ? com.hex.kagune.control.ukaku.SteelBlade.SPEED : 0;
            int bladeJump = state.blade ? com.hex.kagune.control.ukaku.SteelBlade.JUMP : 0;
            KaguneServerController.refreshFormEffect(player, MobEffects.f_19596_, UkakuRules.speedAmplifier(mastery) + (agile ? UkakuRules.AGILITY_SPEED : 0)
                + blade);
            KaguneServerController.refreshFormEffect(player, MobEffects.f_19603_, UkakuRules.jumpAmplifier(mastery) + (agile ? UkakuRules.AGILITY_JUMP : 0)
                + bladeJump);
        }
        UkakuVortex.tick(player, state, now);
        UkakuFire.tick(player, now);
        UkakuCrystalCombat.tick(player, now);
        UkakuPyre.tick(player, now);
        UkakuGatling.tick(player, state, now);
        if (form(player) != state.sentForm || mutations(player) != state.sentMutations) {
            // His mastery changed the wings (the twin wings at 100, or a command set it) or his mutations changed.
            state.dirty = true;
        }
        if (state.dirty) {
            broadcast(player);
        }
    }

    /** An Ukaku input from {@code C2SUkakuAction} (only Ukaku players with the wings out are heard; K is also Rinkaku's). */
    public static void onAction(ServerPlayer player, int action, int targetId) {
        if (PaleWitness.frozen(player)) {
            return; // 6.2.0: frozen in an Owl DNA player's Witness This: no kagune answers
        }
        if (action == com.hex.kagune.control.network.C2SUkakuAction.MUTATE) {
            if (isUkaku(player)) {
                unlockMutation(player, targetId);
            }
            return;
        }
        if (PaleStun.stunned(player)) {
            return; // 7.0.2: stunned, no kagune answers
        }
        if (action == com.hex.kagune.control.network.C2SUkakuAction.KAKUJA || action == com.hex.kagune.control.network.C2SUkakuAction.FINAL_KAKUJA) {
            boolean hold = action == com.hex.kagune.control.network.C2SUkakuAction.FINAL_KAKUJA;
            if (isUkaku(player)) {
                UkakuForms.kakujaKey(player, hold);
            } else if (KaguneProfile.isUnlocked(player) && PathServer.path(player) == KagunePath.RINKAKU) {
                UkakuForms.rinkakuKey(player, hold);
            } else if (PaleServer.isPale(player)) {
                // 6.2.0: her Owl (6.2.0: tap or hold, it is her only kakuja).
                PaleServer.onAction(player, com.hex.kagune.control.network.C2SPaleAction.KAKUJA, hold ? 1 : 0, 0);
            } else if (GraftServer.isGraft(player)) {
                // 7.0.0: tapped, his kakuja (in or out); held, his next base stage.
                GraftServer.kakujaKey(player, hold);
            }
            return;
        }
        if (action == com.hex.kagune.control.network.C2SUkakuAction.DASH) {
            KakujaPassive.dash(player, targetId < 0 ? -1 : 1);
            return;
        }
        if (!isUkaku(player) || !KaguneProfile.isActive(player) || !player.m_6084_()) {
            return;
        }
        if ((action == com.hex.kagune.control.network.C2SUkakuAction.ENDER || action == com.hex.kagune.control.network.C2SUkakuAction.ENDER_FORWARD)
            && blade(player)) {
            // 6.0.0: with the Steel Blade out, right click is its dash.
            KakujaPassive.dash(player, 1);
            return;
        }
        // Any ability is an attack: out of the wind.
        KakujaPassive.attacked(player);
        if (action == com.hex.kagune.control.network.C2SUkakuAction.ENDER || action == com.hex.kagune.control.network.C2SUkakuAction.ENDER_FORWARD) {
            if (UkakuGatling.firing(player)) {
                return; // both wings are busy firing
            }
            net.minecraft.world.entity.Entity hint = targetId > 0 ? player.m_284548_().m_6815_(targetId) : null;
            KaguneCombat.startEnder(player, hint, action == com.hex.kagune.control.network.C2SUkakuAction.ENDER_FORWARD);
        } else if (action == com.hex.kagune.control.network.C2SUkakuAction.GATLING) {
            UkakuGatling.trigger(player, targetId != 0);
        }
    }

    @SubscribeEvent
    public static void loggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            STATES.remove(player.m_20148_());
            if (isUkaku(player)) {
                ControlNetwork.sendTo(player, snapshot(player));
            }
        }
    }

    @SubscribeEvent
    public static void startTracking(PlayerEvent.StartTracking event) {
        if (event.getEntity() instanceof ServerPlayer viewer && event.getTarget() instanceof ServerPlayer subject && isUkaku(subject)) {
            ControlNetwork.sendTo(viewer, snapshot(subject));
        }
    }

    @SubscribeEvent
    public static void respawned(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            UkakuState state = STATES.get(player.m_20148_());
            if (state != null) {
                state.calm();
            }
        }
    }

    @SubscribeEvent
    public static void loggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        STATES.remove(event.getEntity().m_20148_());
        if (event.getEntity() instanceof ServerPlayer player) {
            UkakuFire.forget(player);
            UkakuCrystalCombat.forget(player);
            UkakuPyre.forget(player);
            UkakuVortex.forget(player);
            UkakuRush.forget(player);
        }
        UkakuThorns.loggedOut(event.getEntity().m_20148_());
    }
}
