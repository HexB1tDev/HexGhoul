package com.hex.kagune.control.server;

import com.hex.kagune.control.KagunePath;
import com.hex.kagune.control.network.S2CUkakuFx;
import com.hex.kagune.control.ukaku.UkakuSounds;
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
 * 4.3.0 (the owner, quoted exactly: "Ukaku Mastered animation should be a evil laugh sequence: Put hand on face (head tilted down), start
 * laughing, hand back down a little on the side (right hand) the hand should be a little to the right off the body, crack finger with sound,
 * ukaku emerges."): a mastered Ukaku (mastery {@link #MASTERED}) brings the wings out laughing. Every client poses it and writes the laugh over
 * the head ({@code LaughPullOutClient}, {@code LaughPose}); here, its sounds (the game's own: a low cackle as each "HA" rises, a sharp snap at
 * the crack) and, at {@link #OUT_AT}, the wings tearing out (blood thrown out of the back, a wing's beat).
 */
@Mod.EventBusSubscriber(modid = "hexkagune")
public final class LaughPullOut {
    /** The mastery it takes. */
    public static final int MASTERED = 1000;
    /** When the cackles come (ticks: with the first, the third and the fifth "HA"), the crack, and the wings (LaughPose's own timing). */
    static final long[] CACKLES = {10L, 18L, 26L};
    public static final long CRACK_AT = 48L;
    public static final long OUT_AT = 52L;

    private static final Map<UUID, Long> PENDING = WorldTimes.map();

    private LaughPullOut() {
    }

    /** True for an Ukaku player at full mastery: the laugh is their pull-out (before Ayato's lift and Ken's crack). */
    static boolean taught(ServerPlayer player) {
        return PathServer.path(player) == KagunePath.UKAKU && KaguneProfile.mastery(player) >= MASTERED;
    }

    static boolean pending(ServerPlayer player) {
        return PENDING.containsKey(player.m_20148_());
    }

    /** The laugh begins: every client poses it and writes it; the wings follow at {@link #OUT_AT}. */
    static void start(ServerPlayer player) {
        PENDING.put(player.m_20148_(), player.m_9236_().m_46467_());
        UkakuFire.fx(player, S2CUkakuFx.LAUGH, 0, player.m_20182_(), Vec3.f_82478_, 1.0F, 0);
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
        Vec3 head = player.m_20182_().m_82520_(0.0, player.m_20206_() * 0.9, 0.0);
        for (int i = 0; i < CACKLES.length; i++) {
            if (age == CACKLES[i]) {
                UkakuFire.sound(level, head, UkakuSounds.LAUGH, 1.0F, 0.72F - 0.05F * i);
            }
        }
        if (age == CRACK_AT) {
            UkakuFire.sound(level, player.m_20182_().m_82520_(0.0, 1.0, 0.0), UkakuSounds.FINGER_CRACK, 1.0F, 1.6F);
        }
        if (age >= OUT_AT) {
            PENDING.remove(player.m_20148_());
            if (!KaguneProfile.isActive(player)) {
                com.hex.kagune.gameplay.KaguneServerController.toggleLegacy(player);
                burst(player, level);
            }
        }
    }

    /** The wings tear out of the back: blood thrown out behind and up, a heavy beat, the fire catching. */
    static void burst(ServerPlayer player, ServerLevel level) {
        double yaw = Math.toRadians(player.f_20883_);
        Vec3 forward = new Vec3(-Math.sin(yaw), 0.0, Math.cos(yaw));
        Vec3 back = player.m_20182_().m_82520_(0.0, 1.2, 0.0).m_82546_(forward.m_82490_(0.3));
        Vec3 side = new Vec3(forward.f_82481_, 0.0, -forward.f_82479_);
        for (int i = 0; i < 6; i++) {
            double spread = (i % 3 - 1.0) * 0.7;
            Vec3 way = forward.m_82490_(-1.0).m_82549_(side.m_82490_(spread)).m_82520_(0.0, i < 3 ? 0.4 : 1.0, 0.0).m_82541_();
            UkakuFire.fx(player, S2CUkakuFx.BLOOD, player.m_19879_(), back, way, 2.2F, 1);
        }
        UkakuFire.sound(level, back, UkakuSounds.WING_FLAP, 1.3F, 0.75F);
        UkakuFire.sound(level, back, UkakuSounds.MANIFEST, 1.0F, 0.8F);
    }
}
