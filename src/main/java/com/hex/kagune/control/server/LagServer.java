package com.hex.kagune.control.server;

import com.hex.kagune.KaguneMod;
import com.hex.kagune.control.LagMeter;
import com.hex.kagune.control.network.ControlNetwork;
import com.hex.kagune.control.network.S2CLag;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;

/**
 * 4.1.99: {@code /kagune lag} (anyone may run it): the server's side of the mod's profiler ({@link LagMeter}). For {@link #SECONDS}
 * seconds every timed place of the mod is measured on the server's thread (and its world generation workers), and the asker's own game
 * measures its side at the same time ({@link S2CLag}); then each prints its ten costliest places in the asker's chat and writes them to
 * its log. {@code /kagune lag log}: the same every {@link #LOG_SECONDS} seconds to the logs, until it is run again.
 */
@Mod.EventBusSubscriber(modid = "hexkagune")
public final class LagServer {
    /** How long {@code /kagune lag} measures. */
    public static final int SECONDS = 30;
    /** And each window of {@code /kagune lag log}. */
    public static final int LOG_SECONDS = 60;

    private static final Set<UUID> ASKERS = new LinkedHashSet<>();
    private static int seconds;
    private static boolean logging;
    private static long tickBegan;

    private LagServer() {
    }

    /** {@code /kagune lag}: measures (or joins the measurement under way); what to say back. */
    public static String measure(ServerPlayer asker) {
        if (LagMeter.open(LagMeter.Side.SERVER)) {
            if (asker != null) {
                ASKERS.add(asker.m_20148_());
                ControlNetwork.sendTo(asker, new S2CLag(Math.max(1, seconds - (int)LagMeter.seconds(LagMeter.Side.SERVER)), false));
            }
            return "Already measuring: " + Math.max(0, seconds - (int)LagMeter.seconds(LagMeter.Side.SERVER))
                + " s left. The results will come to you too.";
        }
        open(SECONDS);
        if (asker != null) {
            ASKERS.add(asker.m_20148_());
            ControlNetwork.sendTo(asker, new S2CLag(SECONDS, false));
        }
        return "Measuring the mod's lag for " + SECONDS + " s, on the server and in your game. Keep playing where it lags (a fight, the CCG"
            + " tower, the hideout, the hotel); the ten costliest parts come to your chat and the log.";
    }

    /** {@code /kagune lag log}: turns the minute by minute log on or off; what to say back. */
    public static String log(ServerPlayer asker) {
        logging = !logging;
        if (asker != null) {
            ControlNetwork.sendTo(asker, new S2CLag(logging ? LOG_SECONDS : 0, true));
        }
        if (logging) {
            if (!LagMeter.open(LagMeter.Side.SERVER)) {
                open(LOG_SECONDS);
            }
            return "Lag log on: every " + LOG_SECONDS + " s the mod's costliest parts go to logs/latest.log (the server's and your game's),"
                + " until /kagune lag log again.";
        }
        return "Lag log off.";
    }

    private static void open(int length) {
        seconds = length;
        LagMeter.open(LagMeter.Side.SERVER, Thread.currentThread());
    }

    /** The tick's start, as early as the mod can see it. */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void tickBegins(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.START && LagMeter.measuring()) {
            tickBegan = System.nanoTime();
        }
    }

    /** Its end, as late as the mod can see it: the tick counted; and once the window is over, the report. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void tickEnds(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.START || !LagMeter.open(LagMeter.Side.SERVER)) {
            return;
        }
        long now = System.nanoTime();
        if (tickBegan != 0L) {
            LagMeter.tick(LagMeter.Side.SERVER, now - tickBegan);
        }
        tickBegan = 0L;
        if (LagMeter.seconds(LagMeter.Side.SERVER) < seconds) {
            return;
        }
        LagMeter.Report report = LagMeter.close(LagMeter.Side.SERVER);
        if (report != null) {
            List<String> lines = new ArrayList<>(LagMeter.lines(report));
            lines.add(loaded());
            tell(lines);
        }
        if (logging) {
            open(LOG_SECONDS);
        }
    }

    /** 4.1.100: how many of the mod's NPCs and players the worlds hold now (NpcIndex), for the report's reader. */
    private static String loaded() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        int npcs = 0;
        int players = 0;
        if (server != null) {
            for (net.minecraft.server.level.ServerLevel level : server.m_129785_()) {
                npcs += NpcIndex.count(level);
                players += level.m_6907_().size();
            }
        }
        return "§7 Loaded now: §f" + npcs + "§7 of the mod's NPCs, §f" + players + "§7 players";
    }

    private static void tell(List<String> lines) {
        for (String line : lines) {
            KaguneMod.LOGGER.info("HexKagune lag: " + LagMeter.plain(line));
        }
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        List<UUID> askers = new ArrayList<>(ASKERS);
        ASKERS.clear();
        if (server == null) {
            return;
        }
        for (UUID id : askers) {
            ServerPlayer player = server.m_6846_().m_11259_(id);
            if (player != null) {
                for (String line : lines) {
                    player.m_5661_(Component.m_237113_(line), false);
                }
            }
        }
    }

    /** A world that stopped takes its measurement with it (single player goes on to the next world in the same game). */
    @SubscribeEvent
    public static void stopped(ServerStoppedEvent event) {
        LagMeter.close(LagMeter.Side.SERVER);
        ASKERS.clear();
        logging = false;
        tickBegan = 0L;
    }
}
