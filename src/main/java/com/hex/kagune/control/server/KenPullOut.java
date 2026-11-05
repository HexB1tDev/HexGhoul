package com.hex.kagune.control.server;

import com.hex.kagune.control.network.S2CUkakuFx;
import com.hex.kagune.control.quest.Gate;
import com.hex.kagune.data.KaguneProfile;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 2.4.8 (the owner: "after the player learns kakuja from [Ken] they gain the same pull out animation as [Ken] (crack finger and then kagune
 * explodes from the back in a very violent fashion"): once Ken has taught them a kakuja (any of his lessons' gates open), bringing the kagune
 * out is his way: the right arm out, a finger cracked ({@link #CRACK_AT}: {@code finger_crack}, a real bone's crack), then at {@link #OUT_AT} the kagune tears out of
 * their back: blood thrown out behind them, a ripping crunch, and whatever stands close shoved away.
 */
@Mod.EventBusSubscriber(modid = "hexkagune")
public final class KenPullOut {
    private KenPullOut() {
    }

    static final long CRACK_AT = 14L;
    static final long OUT_AT = 20L;

    private static final Map<UUID, Long> PENDING = new HashMap<>();

    /** True once Ken has taught {@code player} a kakuja (one of his lessons' gates open). */
    static boolean taught(ServerPlayer player) {
        com.hex.kagune.control.quest.QuestLog log = QuestServer.log(player);
        return log.open(Gate.RINKAKU_CENTIPEDE) || log.open(Gate.RINKAKU_FULL_KAKUJA) || log.open(Gate.RINKAKU_DRAGON) || log.open(Gate.UKAKU_FINAL_KAKUJA);
    }

    /** True while {@code player} is cracking a finger, the kagune about to come. */
    static boolean pending(ServerPlayer player) {
        return PENDING.containsKey(player.m_20148_());
    }

    /** The crack begins: every client poses it; the kagune follows at {@link #OUT_AT}. */
    static void start(ServerPlayer player) {
        long now = player.m_9236_().m_46467_();
        PENDING.put(player.m_20148_(), now);
        UkakuFire.fx(player, S2CUkakuFx.CRACK, 0, player.m_20182_(), Vec3.f_82478_, 1.0F, 0);
    }

    @SubscribeEvent
    public static void playerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player) || !(player.m_9236_() instanceof ServerLevel level)) {
            return;
        }
        Long start = PENDING.get(player.m_20148_());
        if (start == null) {
            return;
        }
        long age = level.m_46467_() - start;
        if (!player.m_6084_() || age > OUT_AT + 20L) {
            PENDING.remove(player.m_20148_());
            return;
        }
        double yaw = Math.toRadians(player.f_20883_);
        Vec3 forward = new Vec3(-Math.sin(yaw), 0.0, Math.cos(yaw));
        if (age == CRACK_AT) {
            Vec3 hand = player.m_20182_().m_82520_(0.0, 1.35, 0.0).m_82549_(forward.m_82490_(0.6));
            UkakuFire.sound(level, hand, "hexkagune:finger_crack", 1.0F, 1.0F);
        }
        if (age >= OUT_AT) {
            PENDING.remove(player.m_20148_());
            if (!KaguneProfile.isActive(player)) {
                com.hex.kagune.gameplay.KaguneServerController.toggleLegacy(player);
                burst(player, level, forward);
            }
        }
    }

    /** The kagune tears out of their back: blood thrown out behind and up, a ripping crunch, everything close shoved away. */
    static void burst(ServerPlayer player, ServerLevel level, Vec3 forward) {
        Vec3 back = player.m_20182_().m_82520_(0.0, 1.05, 0.0).m_82546_(forward.m_82490_(0.3));
        Vec3 side = new Vec3(forward.f_82481_, 0.0, -forward.f_82479_);
        // Two fans of blood out of the back: one low and wide, one thrown high, as the kagune rips through.
        for (int i = 0; i < 8; i++) {
            double spread = (i % 4 - 1.5) * 0.55;
            Vec3 way = forward.m_82490_(-1.0).m_82549_(side.m_82490_(spread)).m_82520_(0.0, i < 4 ? 0.35 : 1.1, 0.0).m_82541_();
            UkakuFire.fx(player, S2CUkakuFx.BLOOD, player.m_19879_(), back, way, i < 4 ? 2.8F : 2.2F, 1);
        }
        UkakuFire.sound(level, back, "hexkagune:kagune_burst", 1.5F, 0.9F);
        UkakuFire.sound(level, back, "hexkagune:manifest_rip", 1.4F, 0.75F);
        for (LivingEntity near : level.m_6443_(LivingEntity.class, player.m_20191_().m_82400_(2.5), e -> e != player && e.m_6084_())) {
            Vec3 away = near.m_20182_().m_82546_(player.m_20182_());
            double flat = Math.sqrt(away.f_82479_ * away.f_82479_ + away.f_82481_ * away.f_82481_);
            if (flat < 1.0E-3) {
                continue;
            }
            near.m_20256_(near.m_20184_().m_82520_(away.f_82479_ / flat * 0.8, 0.35, away.f_82481_ / flat * 0.8));
            near.f_19864_ = true;
        }
    }
}
