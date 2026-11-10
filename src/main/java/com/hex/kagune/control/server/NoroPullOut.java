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
 * 4.0.2 (the owner: "As for Noro, when you train with him, when you actually do a few quests, you tilt your head before the Kagune pops
 * out, basically. You just stand still and then you tilt your head and the Kagune pops out"; and "The crack fingers animation is only
 * Kaneki's"): once Noro has taught a Bikaku player a few of his lessons ({@link #taught}: the Worms and The Walker), bringing the
 * kagune out is his way: they stand, the head tilts slowly far over ({@code EtoPose.tiltOver}), and at {@link #OUT_AT} the kagune comes
 * out of their back, wet and quiet. Every client poses it ({@link S2CUkakuFx#TILT}).
 */
@Mod.EventBusSubscriber(modid = "hexkagune")
public final class NoroPullOut {
    /** When the kagune comes out (ticks into the tilt), and how long the tilt is drawn. */
    public static final long OUT_AT = 22L;
    public static final float LASTS = 46.0F;

    private static final Map<UUID, Long> PENDING = WorldTimes.map();

    private NoroPullOut() {
    }

    /** True once Noro has taught {@code player} a few of his lessons (the Worms, The Walker), and they are on the Bikaku path. */
    static boolean taught(ServerPlayer player) {
        com.hex.kagune.control.quest.QuestLog log = QuestServer.log(player);
        return BikakuServer.isBikaku(player) && log.open(Gate.BIKAKU_WORMS) && log.open(Gate.BIKAKU_WALKER);
    }

    static boolean pending(ServerPlayer player) {
        return PENDING.containsKey(player.m_20148_());
    }

    /** The tilt begins: every client poses it; the kagune follows at {@link #OUT_AT}. */
    static void start(ServerPlayer player) {
        PENDING.put(player.m_20148_(), player.m_9236_().m_46467_());
        UkakuFire.fx(player, S2CUkakuFx.TILT, 0, player.m_20182_(), Vec3.f_82478_, 1.0F, 0);
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
                UkakuFire.sound(level, player.m_20182_().m_82520_(0.0, 1.0, 0.0), "hexkagune:sfx.writhe", 1.0F, 0.8F);
            }
        }
    }
}
