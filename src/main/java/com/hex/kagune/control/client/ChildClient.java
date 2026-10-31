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
    public record View(String name, Genome genome, ChildStage stage, int carrier, boolean turned, int ride,
                       com.hex.kagune.control.family.ChildMoment moment, long momentAt, double momentBegan, int partner) {
        /** 1.3.20: how far into its moment it is (client clock ticks), or -1 when it has none. */
        public double momentAge(double clock) {
            return this.moment == com.hex.kagune.control.family.ChildMoment.NONE ? -1.0 : clock - this.momentBegan;
        }

        /** 1.3.20: true while its moment runs at {@code clock}. */
        public boolean inMoment(double clock) {
            return this.moment.running(this.momentAge(clock));
        }
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
        Minecraft minecraft = Minecraft.m_91087_();
        // 1.3.20: its moment on this client's clock (the same moment sent again keeps when it began here).
        View before = VIEWS.get(message.id());
        com.hex.kagune.control.family.ChildMoment moment = com.hex.kagune.control.family.ChildMoment.byOrdinal(message.moment());
        double began;
        if (before != null && before.momentAt() == message.momentAt() && before.moment() == moment) {
            began = before.momentBegan();
        } else {
            long world = minecraft.f_91073_ == null ? message.momentAt() : minecraft.f_91073_.m_46467_();
            began = PaleClient.clock(0.0F) - Math.max(0L, Math.min(world - message.momentAt(), 200L));
        }
        VIEWS.put(message.id(), new View(message.name(), genome, stage, message.carrier(), message.turned(), message.ride(), moment, message.momentAt(),
            began, message.partner()));
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

    /** True if the player {@code id} carries a child now in their arms (1.3.20: not on their shoulders, nor held out spinning). */
    public static boolean carrying(int id) {
        for (View view : VIEWS.values()) {
            if (view.carrier() == id && view.ride() == com.hex.kagune.control.family.ChildRules.RIDE_ARMS) {
                return true;
            }
        }
        return false;
    }

    /** 1.3.20: the child the body {@code id} holds (any way), or null. */
    public static View held(int id) {
        for (View view : VIEWS.values()) {
            if (view.carrier() == id) {
                return view;
            }
        }
        return null;
    }

    /** 1.3.20: the child having a moment with the body {@code id} now (one it takes part in), or null. */
    public static View partnered(int id, double clock) {
        for (View view : VIEWS.values()) {
            if (view.partner() == id && view.inMoment(clock) && !view.moment().solo) {
                return view;
            }
        }
        return null;
    }

    /** 1.3.20: true if the body {@code id} holds a child, or has a moment with one: its body is posed for it. */
    public static boolean busy(int id) {
        return held(id) != null || partnered(id, PaleClient.clock(0.0F)) != null;
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
