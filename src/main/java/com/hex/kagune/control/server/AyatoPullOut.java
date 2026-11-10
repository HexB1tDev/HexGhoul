package com.hex.kagune.control.server;

import com.hex.kagune.control.network.S2CUkakuFx;
import com.hex.kagune.control.quest.Gate;
import com.hex.kagune.data.KaguneProfile;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 4.1.0 (the owner: "after training with Ayato (Ukaku kakuja quest) the player gains a new pull out kagune animation sort of like Ken gives
 * the player. the player lifts both hands god like and then they're unleashed."): once Ayato has taught an Ukaku player his First Kakuja
 * ({@link #taught}), bringing the kagune out is his way: they rise a little, both arms lifted out wide, palms open upward, chin up
 * ({@code AyatoPullOutClient}), and at {@link #OUT_AT} the wings are unleashed. Every client poses it ({@link S2CUkakuFx#UNLEASH}).
 */
@Mod.EventBusSubscriber(modid = "hexkagune")
public final class AyatoPullOut {
    /** When the wings come out (ticks into the lift): the arms all the way out, held a breath. */
    public static final long OUT_AT = 20L;

    private static final Map<UUID, Long> PENDING = WorldTimes.map();

    private AyatoPullOut() {
    }

    /** True once Ayato has taught {@code player} the Ukaku's First Kakuja (its gate open), and they are on the Ukaku path. */
    static boolean taught(ServerPlayer player) {
        return PathServer.path(player) == com.hex.kagune.control.KagunePath.UKAKU && QuestServer.log(player).open(Gate.UKAKU_KAKUJA);
    }

    static boolean pending(ServerPlayer player) {
        return PENDING.containsKey(player.m_20148_());
    }

    /** The lift begins: every client poses it; the wings follow at {@link #OUT_AT}. */
    static void start(ServerPlayer player) {
        PENDING.put(player.m_20148_(), player.m_9236_().m_46467_());
        UkakuFire.fx(player, S2CUkakuFx.UNLEASH, 0, player.m_20182_(), Vec3.f_82478_, 1.0F, 0);
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
        if (!player.m_6084_() || age < 0L || age > OUT_AT + 20L) {
            PENDING.remove(player.m_20148_());
            return;
        }
        if (age >= OUT_AT) {
            PENDING.remove(player.m_20148_());
            if (!KaguneProfile.isActive(player)) {
                com.hex.kagune.gameplay.KaguneServerController.toggleLegacy(player);
                Vec3 back = player.m_20182_().m_82520_(0.0, 1.2, 0.0);
                UkakuFire.sound(level, back, "hexkagune:manifest_ukaku", 1.4F, 1.0F);
                UkakuFire.sound(level, back, "hexkagune:sfx.air_burst", 1.0F, 0.9F);
            }
        }
    }
}
