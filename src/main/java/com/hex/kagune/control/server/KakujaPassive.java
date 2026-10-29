package com.hex.kagune.control.server;

import com.hex.kagune.control.KagunePath;
import com.hex.kagune.control.network.S2CAyatoFx;
import com.hex.kagune.data.KaguneProfile;
import com.hex.kagune.gameplay.KaguneForm;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.ArrowLooseEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 6.0.0: the kakuja's passive and dash, for Ayato's Steel Blade and Kaneki's Full Kakuja.
 *
 * <p><b>Vanish.</b> Breaking into a sprint, he vanishes in a gust of wind: for five seconds nobody sees him at all (his body,
 * armour, what he holds, his kagune, his name, his shadow: the invisibility here, and the clients skip drawing him) and mobs
 * lose him. It ends early the moment he attacks by any means (a swing, an ability, a shot, any damage he deals) or the form goes
 * (the blade opened, out of the kakuja, the kagune put away, death). Then thirty seconds before it can happen again. While
 * Kaneki is vanished his centipede legs don't hunt ({@link CentipedeHunt}).
 *
 * <p><b>Dash.</b> Right click dashes: forward (W, or no direction), backward (S). Ayato's blade dashes far; Kaneki's kakuja,
 * heavier, far less.
 *
 * <p>6.0.5, true invisibility: vanished, nothing of him is seen, by anyone, himself included in third person ({@code PlayerLayers});
 * no dirt kicked up as he sprints ({@code VanishClient}), no dust where he lands, no tendrils catching his fall
 * ({@link AyatoGates#tryStartFallArrest}, the original's landing burst), no wind or sound when he dashes.
 */
@Mod.EventBusSubscriber(modid = "hexkagune")
public final class KakujaPassive {
    /** How long he stays vanished, and how long until he can vanish again once he is back (ticks). */
    public static final int VANISH_TICKS = 100;
    public static final int COOLDOWN_TICKS = 600;

    /** Whose passive: the dash's speed (blocks a tick), how long it pushes (ticks) and its cooldown (ticks). */
    enum Kind {
        BLADE(1.7, 6, 22),
        KANEKI(0.95, 5, 40);

        final double dashSpeed;
        final int dashTicks;
        final int dashCooldown;

        Kind(double dashSpeed, int dashTicks, int dashCooldown) {
            this.dashSpeed = dashSpeed;
            this.dashTicks = dashTicks;
            this.dashCooldown = dashCooldown;
        }
    }

    private static final class State {
        long vanishUntil = Long.MIN_VALUE;
        long readyAt = Long.MIN_VALUE;
        boolean wasSprinting = true;
        boolean ourInvisibility;
        long dashUntil = Long.MIN_VALUE;
        long dashReadyAt = Long.MIN_VALUE;
        Vec3 heading = Vec3.f_82478_;
        double dashSpeed;
    }

    private static final Map<UUID, State> STATES = new HashMap<>();

    private KakujaPassive() {
    }

    /** Whose passive he has right now: Ayato with the Steel Blade out, Kaneki in his Full Kakuja; null otherwise. */
    static Kind kind(ServerPlayer player) {
        if (!player.m_6084_() || !KaguneProfile.isActive(player)) {
            return null;
        }
        if (AyatoServer.isAyato(player)) {
            return AyatoServer.blade(player) ? Kind.BLADE : null;
        }
        if (PathServer.path(player) == KagunePath.KANEKI && KaguneProfile.form(player) == KaguneForm.FULL_KAKUJA) {
            return Kind.KANEKI;
        }
        return null;
    }

    /** True while he is vanished. */
    public static boolean vanished(ServerPlayer player) {
        State state = STATES.get(player.m_20148_());
        return state != null && state.vanishUntil != Long.MIN_VALUE && player.m_9236_().m_46467_() < state.vanishUntil;
    }

    /** He attacked (any way at all): out of the wind. */
    public static void attacked(ServerPlayer player) {
        State state = STATES.get(player.m_20148_());
        if (state != null && state.vanishUntil != Long.MIN_VALUE) {
            reveal(player, state, player.m_9236_().m_46467_());
        }
    }

    /**
     * Right click with the passive's form: a dash forward ({@code direction} 1) or back (-1). True when it was the passive's (the
     * click is spent, even on cooldown).
     */
    public static boolean dash(ServerPlayer player, int direction) {
        Kind kind = kind(player);
        if (kind == null) {
            return false;
        }
        State state = STATES.computeIfAbsent(player.m_20148_(), ignored -> new State());
        long now = player.m_9236_().m_46467_();
        if (now < state.dashReadyAt) {
            return true;
        }
        Vec3 look = player.m_20154_();
        double length = Math.sqrt(look.f_82479_ * look.f_82479_ + look.f_82481_ * look.f_82481_);
        if (length < 1.0E-4) {
            return true;
        }
        double sign = direction < 0 ? -1.0 : 1.0;
        state.heading = new Vec3(look.f_82479_ / length * sign, 0.0, look.f_82481_ / length * sign);
        state.dashSpeed = kind.dashSpeed * (direction < 0 ? 0.85 : 1.0);
        state.dashUntil = now + kind.dashTicks;
        state.dashReadyAt = now + Cooldowns.of(player, kind.dashCooldown);
        push(player, state);
        if (!vanished(player)) {
            // 6.0.5: vanished, the dash gives nothing away: no rush of wind, no sound.
            AyatoFire.fx(player, S2CAyatoFx.GUST, 0, player.m_20182_(), state.heading.m_82490_(state.dashSpeed), kind == Kind.BLADE ? 1.0F : 0.6F, 0);
            AyatoFire.sound(player.m_284548_(), player.m_20182_(), "item.trident.riptide_1", kind == Kind.BLADE ? 0.9F : 0.6F,
                kind == Kind.BLADE ? 1.3F : 1.0F);
        }
        return true;
    }

    /** 6.2.0: the vanish and the dash ready again now ({@link Cooldowns}). */
    static void clearCooldowns(ServerPlayer player) {
        State state = STATES.get(player.m_20148_());
        if (state != null) {
            state.readyAt = Long.MIN_VALUE;
            state.dashReadyAt = Long.MIN_VALUE;
        }
    }

    private static void push(ServerPlayer player, State state) {
        Vec3 motion = player.m_20184_();
        player.m_20256_(new Vec3(state.heading.f_82479_ * state.dashSpeed, Math.min(motion.f_82480_, player.m_20096_() ? 0.1 : 0.04),
            state.heading.f_82481_ * state.dashSpeed));
        player.f_19864_ = true;
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void playerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) {
            return;
        }
        Kind kind = kind(player);
        State state = STATES.get(player.m_20148_());
        if (state == null) {
            if (kind == null) {
                return;
            }
            state = new State();
            state.wasSprinting = player.m_20142_();
            STATES.put(player.m_20148_(), state);
        }
        long now = player.m_9236_().m_46467_();
        boolean sprinting = player.m_20142_();
        if (state.vanishUntil != Long.MIN_VALUE) {
            if (kind == null || now >= state.vanishUntil) {
                reveal(player, state, now);
            } else {
                if (!player.m_21023_(MobEffects.f_19609_)) {
                    // Something took the invisibility (milk): he is still in the wind.
                    player.m_7292_(new MobEffectInstance(MobEffects.f_19609_, (int)(state.vanishUntil - now) + 5, 0, false, false, true));
                }
                // 6.0.5: no puff of dust where he lands (vanilla throws one after a fall of more than three blocks). The manifested
                // kagune takes no fall damage anyway (the original's ghoul durability), so nothing else changes.
                player.f_19789_ = 0.0F;
            }
        } else if (kind != null && sprinting && !state.wasSprinting && now >= state.readyAt) {
            vanish(player, state, now);
        }
        state.wasSprinting = sprinting;
        if (state.dashUntil != Long.MIN_VALUE) {
            if (now >= state.dashUntil || !player.m_6084_()) {
                state.dashUntil = Long.MIN_VALUE;
            } else {
                push(player, state);
            }
        }
        if (kind == null && state.vanishUntil == Long.MIN_VALUE && state.dashUntil == Long.MIN_VALUE && now >= state.readyAt) {
            STATES.remove(player.m_20148_());
        }
    }

    private static void vanish(ServerPlayer player, State state, long now) {
        state.vanishUntil = now + VANISH_TICKS;
        state.ourInvisibility = !player.m_21023_(MobEffects.f_19609_);
        player.m_7292_(new MobEffectInstance(MobEffects.f_19609_, VANISH_TICKS + 5, 0, false, false, true));
        // Mobs lose him.
        for (Mob mob : player.m_284548_().m_6443_(Mob.class, player.m_20191_().m_82400_(48.0), mob -> mob.m_5448_() == player)) {
            mob.m_6710_(null);
        }
        Vec3 at = player.m_20182_();
        AyatoFire.fx(player, S2CAyatoFx.VANISH, 0, at, Vec3.f_82478_, 1.0F, 0);
        AyatoFire.sound(player.m_284548_(), at, "entity.illusioner.mirror_move", 1.0F, 0.9F);
        AyatoFire.sound(player.m_284548_(), at, "entity.ender_dragon.flap", 0.6F, 1.5F);
        player.m_5661_(Component.m_237115_("message.hexkagune.vanish"), true);
    }

    private static void reveal(ServerPlayer player, State state, long now) {
        state.vanishUntil = Long.MIN_VALUE;
        state.readyAt = now + Cooldowns.of(player, COOLDOWN_TICKS);
        if (state.ourInvisibility) {
            player.m_21195_(MobEffects.f_19609_);
        }
        state.ourInvisibility = false;
        Vec3 at = player.m_20182_();
        AyatoFire.fx(player, S2CAyatoFx.REVEAL, 0, at, Vec3.f_82478_, 0.6F, 0);
        AyatoFire.sound(player.m_284548_(), at, "entity.illusioner.mirror_move", 0.7F, 1.4F);
    }

    // ------------------------------------------------------------------ what ends it, and the mobs

    @SubscribeEvent
    public static void hurt(LivingHurtEvent event) {
        Entity source = event.getSource().m_7639_();
        if (source instanceof ServerPlayer player) {
            attacked(player);
        }
    }

    @SubscribeEvent
    public static void attackEntity(AttackEntityEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            attacked(player);
        }
    }

    @SubscribeEvent
    public static void arrowLoose(ArrowLooseEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            attacked(player);
        }
    }

    @SubscribeEvent
    public static void changeTarget(LivingChangeTargetEvent event) {
        if (event.getNewTarget() instanceof ServerPlayer player && vanished(player)) {
            event.setNewTarget(null);
        }
    }

    @SubscribeEvent
    public static void loggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        STATES.remove(event.getEntity().m_20148_());
    }
}
