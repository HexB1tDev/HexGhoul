package com.hex.kagune.control.client;

import com.hex.kagune.control.KagunePath;
import com.hex.kagune.control.quest.Gate;
import com.hex.kagune.control.quest.QuestLog;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** 7.0.1: the player at the keys' quest log as the server last sent it ({@code S2CQuests}), for the journal and the menus' locks. */
@Mod.EventBusSubscriber(modid = "hexkagune", value = Dist.CLIENT)
public final class QuestClient {
    private static QuestLog log = new QuestLog();
    private static boolean known;

    private QuestClient() {
    }

    public static void accept(String saved) {
        log = QuestLog.load(saved);
        known = true;
    }

    public static QuestLog log() {
        return log;
    }

    /**
     * True if {@code gate} is open for the player at the keys (or is another path's, or nothing is known yet: the server decides
     * anyway, so a menu never shows a lock that isn't there).
     */
    public static boolean open(Gate gate) {
        if (gate == null || !known) {
            return true;
        }
        Minecraft minecraft = Minecraft.m_91087_();
        KagunePath path = minecraft.f_91074_ == null ? KagunePath.NONE : PathClient.get(minecraft.f_91074_);
        return !gate.on(path) || log.open(gate);
    }

    /** Mutation points earned from quests. */
    public static int points() {
        return log.points();
    }

    /** The Quests key opens the journal (only with no other screen up). */
    @SubscribeEvent
    public static void clientTick(net.minecraftforge.event.TickEvent.ClientTickEvent event) {
        if (event.phase != net.minecraftforge.event.TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.m_91087_();
        boolean pressed = false;
        while (ControlKeys.QUESTS.m_90859_()) {
            pressed = true;
        }
        if (pressed && minecraft.f_91080_ == null && minecraft.f_91074_ != null) {
            minecraft.m_91152_(new QuestScreen());
        }
    }

    @SubscribeEvent
    public static void loggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        log = new QuestLog();
        known = false;
    }
}
