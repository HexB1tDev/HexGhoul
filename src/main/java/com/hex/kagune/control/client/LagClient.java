package com.hex.kagune.control.client;

import com.hex.kagune.KaguneMod;
import com.hex.kagune.control.LagMeter;
import com.hex.kagune.control.network.S2CLag;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 4.1.99: the client's side of {@code /kagune lag} ({@link LagMeter}): told by the server ({@link S2CLag}), it measures the mod's
 * calls on the render thread (every client tick, render stage, renderer and layer of the mod) and the frames, then prints the ten
 * costliest in the chat and writes them to this game's log; or, the log on, the same every window to the log only.
 */
@Mod.EventBusSubscriber(modid = "hexkagune", value = Dist.CLIENT)
public final class LagClient {
    private static int seconds;
    private static int logSeconds;
    private static boolean toChat;
    private static long lastFrame;

    private LagClient() {
    }

    public static void accept(S2CLag message) {
        if (message.log()) {
            logSeconds = Math.max(0, message.seconds());
            if (logSeconds > 0 && !LagMeter.open(LagMeter.Side.CLIENT)) {
                open(logSeconds, false);
            }
            return;
        }
        if (LagMeter.open(LagMeter.Side.CLIENT)) {
            toChat = true; // the window under way (the log's) ends in the chat too
            return;
        }
        open(Math.max(1, message.seconds()), true);
    }

    private static void open(int length, boolean chat) {
        seconds = length;
        toChat = chat;
        lastFrame = 0L;
        LagMeter.open(LagMeter.Side.CLIENT, Thread.currentThread());
    }

    /** Each frame's length, start to start (the first one of a window only marks the clock). */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void frame(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.START || !LagMeter.open(LagMeter.Side.CLIENT)) {
            return;
        }
        long now = System.nanoTime();
        if (lastFrame != 0L) {
            LagMeter.tick(LagMeter.Side.CLIENT, now - lastFrame);
        }
        lastFrame = now;
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) {
            return;
        }
        LagMeter.knowClient(Thread.currentThread());
        if (!LagMeter.open(LagMeter.Side.CLIENT) || LagMeter.seconds(LagMeter.Side.CLIENT) < seconds) {
            return;
        }
        LagMeter.Report report = LagMeter.close(LagMeter.Side.CLIENT);
        if (report != null) {
            List<String> lines = LagMeter.lines(report);
            for (String line : lines) {
                KaguneMod.LOGGER.info("HexKagune lag: " + LagMeter.plain(line));
            }
            Minecraft minecraft = Minecraft.m_91087_();
            net.minecraft.world.entity.player.Player player = minecraft.f_91074_; // Player's own method (the verified one)
            if (toChat && player != null) {
                for (String line : lines) {
                    player.m_5661_(Component.m_237113_(line), false);
                }
            }
        }
        toChat = false;
        if (logSeconds > 0) {
            open(logSeconds, false);
        }
    }
}
