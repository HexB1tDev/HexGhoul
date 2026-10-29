package com.hex.kagune.control.client;

import com.hex.kagune.control.king.TurningModel;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 1.0.11: who is turning into the Dragon on the client, and since when (the awakening tells it, {@code DragonClient}: the server
 * repeats how far in it is, so a player who comes into view halfway still sees the rest). {@link #progress} drives the body
 * ({@code WarPose.turning}) and what grows on it ({@link TurningModel}, drawn by {@code KingLayer}). While the awakening has the
 * view ({@link #hold}) the keys don't move the player.
 */
@Mod.EventBusSubscriber(modid = "hexkagune", value = Dist.CLIENT)
public final class TurningClient {
    /** Each turning player's start (world ticks). */
    private static final Map<UUID, Double> STARTED = new HashMap<>();
    /** A start this far off what the server says (ticks) is set again. */
    private static final double DRIFT = 4.0;
    private static boolean held;

    private TurningClient() {
    }

    /** {@code player} is {@code tick} ticks into turning (the server, again and again while it lasts). */
    public static void sync(UUID player, int tick) {
        double start = ControlClient.worldTime(0.0F) - tick;
        Double known = STARTED.get(player);
        if (known == null || Math.abs(known - start) > DRIFT) {
            STARTED.put(player, start);
        }
    }

    /** {@code player} has turned: the Dragon is out of him (its first frame), or it is over. */
    public static void end(UUID player) {
        STARTED.remove(player);
    }

    /** How far {@code player} has turned at {@code time} (world ticks): 0..1, or -1 when they are not turning. */
    public static double progress(AbstractClientPlayer player, double time) {
        if (player == null) {
            return -1.0;
        }
        Double start = STARTED.get(player.m_20148_());
        if (start == null) {
            return -1.0;
        }
        double p = (time - start) / TurningModel.PRELUDE_TICKS;
        if (p > 1.5 || p < -0.5) {
            STARTED.remove(player.m_20148_()); // never heard the end: let the body go
            return -1.0;
        }
        return Math.max(0.0, Math.min(1.0, p));
    }

    /** While the awakening has this player's view, their keys don't move them (the awakening says, every tick). */
    public static void hold(boolean hold) {
        held = hold;
    }

    @SubscribeEvent
    public static void movementInput(MovementInputUpdateEvent event) {
        if (held && event.getEntity() instanceof LocalPlayer) {
            Input input = event.getInput();
            input.f_108567_ = 0.0F;
            input.f_108566_ = 0.0F;
        }
    }

    @SubscribeEvent
    public static void loggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        STARTED.clear();
        held = false;
    }
}
