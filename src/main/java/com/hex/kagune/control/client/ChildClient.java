package com.hex.kagune.control.client;

import com.hex.kagune.control.family.ChildStage;
import com.hex.kagune.control.family.Genome;
import com.hex.kagune.control.network.S2CChild;
import com.hex.kagune.control.npc.ChildNpc;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** 1.3.10: every child this client can see (fed only by the server, {@code S2CChild}): its name, looks, stage, who carries it, its kakugan. */
@Mod.EventBusSubscriber(modid = "hexkagune", value = Dist.CLIENT)
public final class ChildClient {
    public record View(String name, Genome genome, ChildStage stage, int carrier, boolean turned) {
    }

    private static final Map<Integer, View> VIEWS = new HashMap<>();

    private ChildClient() {
    }

    public static void accept(S2CChild message) {
        Genome genome = Genome.load(message.genome());
        if (genome == null) {
            return;
        }
        ChildStage stage = ChildStage.byOrdinal(message.stage());
        VIEWS.put(message.id(), new View(message.name(), genome, stage, message.carrier(), message.turned()));
        Minecraft minecraft = Minecraft.m_91087_();
        if (minecraft.f_91073_ != null && minecraft.f_91073_.m_6815_(message.id()) instanceof ChildNpc child) {
            // 1.3.15: carried, it neither pushes nor is pushed on this side either (the local player's own pushing is the client's).
            child.carriedBy = message.carrier();
            if (child.stage != stage) {
                child.stage = stage;
                child.m_6210_(); // its box, its stage's
            }
        }
    }

    public static View get(int id) {
        return VIEWS.get(id);
    }

    /** True if the player {@code id} carries a child now (their arms hold it). */
    public static boolean carrying(int id) {
        for (View view : VIEWS.values()) {
            if (view.carrier() == id) {
                return true;
            }
        }
        return false;
    }

    public static void prune(net.minecraft.client.multiplayer.ClientLevel level) {
        VIEWS.keySet().removeIf(id -> level.m_6815_(id) == null);
    }

    @SubscribeEvent
    public static void loggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        VIEWS.clear();
        com.hex.kagune.control.client.render.ChildRenderer.forget();
    }
}
