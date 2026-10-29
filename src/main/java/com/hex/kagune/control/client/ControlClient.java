package com.hex.kagune.control.client;

import com.hex.kagune.control.network.S2CControlState;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Client store of every player's {@link ControlView}, fed only by the server. */
@Mod.EventBusSubscriber(modid = "hexkagune", value = Dist.CLIENT)
public final class ControlClient {
    private static final Map<UUID, ControlView> VIEWS = new HashMap<>();

    private ControlClient() {
    }

    public static void accept(S2CControlState message) {
        VIEWS.put(message.playerId(), ControlView.of(message));
    }

    public static ControlView get(AbstractClientPlayer player) {
        return player == null ? ControlView.NEUTRAL : VIEWS.getOrDefault(player.m_36316_().getId(), ControlView.NEUTRAL);
    }

    public static ControlView self() {
        Minecraft minecraft = Minecraft.m_91087_();
        return minecraft.f_91074_ == null ? ControlView.NEUTRAL : get(minecraft.f_91074_);
    }

    /** 1.0.27: what the world clock runs from (whose ticks it counts, in which world). */
    private static final SmoothClock CLOCK = new SmoothClock();
    private static Object clockPlayer;
    private static Object clockLevel;

    /**
     * Interpolated client world time in ticks (matches the server's game-time stamps in the packets). 1.0.27: smooth: the client's own
     * ticks, eased onto the game time ({@link SmoothClock}), not the game time itself (set again by the server once a second, which
     * hitched everything drawn by it). 1.0.28: but read with no share of a tick (as the client's ticks read it, to stamp and count by),
     * it is the game time itself, whole ticks apart, so what happens on one tick of an effect happens on exactly that one.
     */
    public static double worldTime(float partialTick) {
        Minecraft minecraft = Minecraft.m_91087_();
        net.minecraft.client.multiplayer.ClientLevel level = minecraft.f_91073_;
        if (level == null) {
            return 0.0;
        }
        net.minecraft.client.player.LocalPlayer player = minecraft.f_91074_;
        double game = level.m_46467_();
        if (player == null) {
            return game + (double)partialTick;
        }
        if (player != clockPlayer || level != clockLevel) {
            clockPlayer = player;
            clockLevel = level;
            CLOCK.reset(game, player.f_19797_);
        } else {
            CLOCK.at(game, player.f_19797_);
        }
        return partialTick == 0.0F ? game : CLOCK.time(partialTick);
    }

    @SubscribeEvent
    public static void loggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        VIEWS.clear();
        clockPlayer = null;
        clockLevel = null;
    }
}
