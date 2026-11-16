package com.hex.kagune.control.server;

import com.hex.kagune.control.network.S2CUkakuFx;
import com.hex.kagune.control.ukaku.SteelFlash;
import com.hex.kagune.control.ukaku.UkakuRules;
import com.hex.kagune.control.ukaku.UkakuSounds;
import com.hex.kagune.data.KaguneProfile;
import com.hex.kagune.gameplay.KaguneServerController;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 4.2.9 (the owner: "Blade Arm Control + right click (does a high speed dash, only leaving behind after images)"): the Steel Blade's flash
 * ({@link SteelFlash}). Thrown along his look, flat, as the kakuja's dash is pushed ({@link KakujaPassive}), so walls still stop him; for the
 * throw he is not seen at all (the vanish's own invisibility: with the blade out nothing of him is drawn, {@code PlayerLayers.vanished}) and
 * nothing touches him; every client lays his afterimages along the way ({@code BladeFlashClient}). Every body near the line he crossed is
 * cut once, as a Steel Blade cut (its blood, its wound), counted as the blade's.
 */
@Mod.EventBusSubscriber(modid = "hexkagune")
public final class BladeFlash {
    private static final class State {
        long until = Long.MIN_VALUE;
        long readyAt = Long.MIN_VALUE;
        Vec3 heading = Vec3.f_82478_;
        Vec3 last = Vec3.f_82478_;
        final Set<Integer> cut = new HashSet<>();
    }

    private static final Map<UUID, State> STATES = new HashMap<>();

    private BladeFlash() {
    }

    /** Ctrl + right click with the Steel Blade out. True when it was the blade's (the click is spent, even on cooldown). */
    static boolean flash(ServerPlayer player) {
        if (KakujaPassive.kind(player) != KakujaPassive.Kind.BLADE) {
            return false;
        }
        State state = STATES.computeIfAbsent(player.m_20148_(), ignored -> new State());
        long now = player.m_9236_().m_46467_();
        if (now < state.readyAt || now < state.until + SteelFlash.SWEEP_AFTER) {
            return true;
        }
        Vec3 look = player.m_20154_();
        double length = Math.sqrt(look.f_82479_ * look.f_82479_ + look.f_82481_ * look.f_82481_);
        if (length < 1.0E-4) {
            return true;
        }
        state.heading = new Vec3(look.f_82479_ / length, 0.0, look.f_82481_ / length);
        state.until = now + SteelFlash.TICKS;
        state.readyAt = now + SteelFlash.TICKS + Cooldowns.of(player, SteelFlash.COOLDOWN);
        state.last = player.m_20182_();
        state.cut.clear();
        KakujaPassive.attacked(player); // a move of the blade's: out of the wind
        // Not seen while it lasts: the vanish's own invisibility (the clients draw nothing of a player so hidden with the blade out).
        player.m_7292_(new MobEffectInstance(MobEffects.f_19609_, SteelFlash.TICKS + 3, 0, false, false, false));
        push(player, state);
        Vec3 at = player.m_20182_();
        UkakuFire.fx(player, S2CUkakuFx.FLASH, SteelFlash.TICKS, at, state.heading.m_82490_(SteelFlash.SPEED), 1.0F, 0);
        // 4.1.91's rule: every Ukaku sound the game's own.
        UkakuFire.sound(player.m_284548_(), at, UkakuSounds.SHADOW_STEP, 1.0F, 1.6F);
        UkakuFire.sound(player.m_284548_(), at, UkakuSounds.VORTEX_THROW, 0.8F, 1.7F);
        return true;
    }

    /** True while {@code player} is mid-flash (nothing touches him). */
    public static boolean flashing(ServerPlayer player) {
        State state = STATES.get(player.m_20148_());
        return state != null && player.m_9236_().m_46467_() < state.until;
    }

    /** The flash ready again now ({@link Cooldowns}). */
    static void clearCooldown(ServerPlayer player) {
        State state = STATES.get(player.m_20148_());
        if (state != null) {
            state.readyAt = Long.MIN_VALUE;
        }
    }

    private static void push(ServerPlayer player, State state) {
        Vec3 motion = player.m_20184_();
        player.m_20256_(new Vec3(state.heading.f_82479_ * SteelFlash.SPEED, Math.min(motion.f_82480_, player.m_20096_() ? 0.06 : 0.0),
            state.heading.f_82481_ * SteelFlash.SPEED));
        player.f_19864_ = true;
        player.f_19789_ = 0.0F;
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void playerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) {
            return;
        }
        State state = STATES.get(player.m_20148_());
        if (state == null) {
            return;
        }
        long now = player.m_9236_().m_46467_();
        if (state.until == Long.MIN_VALUE || now >= state.until + SteelFlash.SWEEP_AFTER || !player.m_6084_()) {
            state.until = Long.MIN_VALUE;
            if (now >= state.readyAt) {
                STATES.remove(player.m_20148_());
            }
            return;
        }
        if (now < state.until) {
            push(player, state);
        }
        Vec3 at = player.m_20182_();
        sweep(player, state, state.last, at);
        state.last = at;
    }

    /** Cuts every body near the line from {@code from} to {@code to} (his feet) not cut yet this flash. */
    private static void sweep(ServerPlayer player, State state, Vec3 from, Vec3 to) {
        if (state.cut.size() >= SteelFlash.MOST) {
            return;
        }
        double[] a = {from.f_82479_, from.f_82480_, from.f_82481_};
        double[] b = {to.f_82479_, to.f_82480_, to.f_82481_};
        AABB area = new AABB(Math.min(a[0], b[0]), Math.min(a[1], b[1]), Math.min(a[2], b[2]), Math.max(a[0], b[0]), Math.max(a[1], b[1]) + 2.0,
            Math.max(a[2], b[2])).m_82400_(SteelFlash.RADIUS + 1.0);
        List<LivingEntity> near = new ArrayList<>(player.m_284548_().m_6443_(LivingEntity.class, area,
            entity -> entity != player && !state.cut.contains(entity.m_19879_()) && KaguneServerController.validSwipeTarget(player, entity)));
        float damage = UkakuRules.meleeDamage(KaguneProfile.mastery(player)) * SteelFlash.DAMAGE;
        int landed = 0;
        for (LivingEntity target : near) {
            if (state.cut.size() >= SteelFlash.MOST) {
                break;
            }
            Vec3 centre = KaguneServerController.center(target);
            if (!SteelFlash.crossed(a, b, new double[] {centre.f_82479_, centre.f_82480_, centre.f_82481_}, target.m_20205_() * 0.5,
                target.m_20206_() * 0.5)) {
                continue;
            }
            state.cut.add(target.m_19879_());
            WoundHits.expect(target, com.hex.kagune.control.wound.WoundBook.Blow.SLASH, centre.m_82546_(state.heading.m_82490_(0.3)), state.heading,
                (player.m_217043_().m_188500_() - 0.5) * 0.6);
            if (!target.m_6469_(player.m_269291_().m_269075_(player), damage)) {
                continue;
            }
            landed++;
            BladeBlood.cut(player, target, com.hex.kagune.control.StrikePattern.STEEL_SWEEP, state.heading, damage);
            UkakuFire.sound(player.m_284548_(), centre, UkakuSounds.WING_CUT, 1.0F, 1.3F);
        }
        if (landed > 0) {
            KaguneProfile.addMastery(player, "Swipe", 1);
            ControlServer.onKaguneHit(player, landed);
        }
    }

    /** Mid-flash nothing touches him: he is not there to be hit. */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void attacked(LivingAttackEvent event) {
        if (!STATES.isEmpty() && event.getEntity() instanceof ServerPlayer player && flashing(player)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void loggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        STATES.remove(event.getEntity().m_20148_());
    }
}
