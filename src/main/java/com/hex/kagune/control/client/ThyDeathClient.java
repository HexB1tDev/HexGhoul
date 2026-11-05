package com.hex.kagune.control.client;

import com.hex.kagune.control.ThyDeath;
import com.hex.kagune.control.network.C2SUkakuAction;
import com.hex.kagune.control.network.ControlNetwork;
import com.hex.kagune.control.network.S2CThyDeath;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 2.4.5: Thy Death on the client: Z held is told to the server (pressed, a ping every few ticks, let go), and each tail's lunge it is told
 * of ({@link S2CThyDeath}) is drawn over the tail's own motion ({@link #apply}, from {@code StrikePaths}).
 */
@Mod.EventBusSubscriber(modid = "hexkagune", value = Dist.CLIENT)
public final class ThyDeathClient {
    private ThyDeathClient() {
    }

    private record Strike(int target, long start) {
    }

    /** Each lunge on by player entity id and tail: {@code player << 4 | limb}. */
    private static final Map<Long, Strike> STRIKES = new HashMap<>();
    private static boolean held;
    private static int pinged;

    public static void accept(S2CThyDeath message) {
        if (message.target() == 0) {
            STRIKES.keySet().removeIf(key -> (key >> 4) == message.player());
            return;
        }
        STRIKES.put(key(message.player(), message.limb()), new Strike(message.target(), message.start()));
        if (STRIKES.size() > 256) {
            STRIKES.clear();
        }
    }

    private static long key(int player, int limb) {
        return (long)player << 4 | (limb & 15);
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.m_91087_();
        if (minecraft.f_91074_ == null || minecraft.f_91080_ != null) {
            if (held) {
                held = false;
                ControlNetwork.CHANNEL.sendToServer(new C2SUkakuAction(C2SUkakuAction.THY_DEATH, 0));
            }
            return;
        }
        boolean down = com.hex.kagune.client.KaguneKeys.RAPID_STRIKE.m_90857_();
        if (down && !held) {
            held = true;
            pinged = 0;
            ControlNetwork.CHANNEL.sendToServer(new C2SUkakuAction(C2SUkakuAction.THY_DEATH, 2));
        } else if (down && ++pinged % 4 == 0) {
            ControlNetwork.CHANNEL.sendToServer(new C2SUkakuAction(C2SUkakuAction.THY_DEATH, 1));
        } else if (!down && held) {
            held = false;
            ControlNetwork.CHANNEL.sendToServer(new C2SUkakuAction(C2SUkakuAction.THY_DEATH, 0));
        }
    }

    /**
     * Tail {@code limb} of {@code owner}'s {@code path} lunging at its foe now, or null when it isn't ({@code fanSide} its layout's,
     * {@code yaw} its owner's body yaw in radians, {@code time} the world's ticks).
     */
    public static double[][] apply(double[][] path, int limb, Entity owner, double fanSide, double yaw, double time, float partial) {
        Strike strike = STRIKES.get(key(owner.m_19879_(), limb));
        if (strike == null || path == null || path.length < 2 || owner.m_9236_() == null) {
            return null;
        }
        double age = time - strike.start();
        if (age <= 0.0 || age >= ThyDeath.STRIKE) {
            return null;
        }
        Entity foe = owner.m_9236_().m_6815_(strike.target());
        if (foe == null) {
            return null;
        }
        double[] target = {foe.f_19790_ + (foe.m_20185_() - foe.f_19790_) * partial, foe.f_19791_ + (foe.m_20186_() - foe.f_19791_) * partial
            + foe.m_20206_() * 0.55, foe.f_19792_ + (foe.m_20189_() - foe.f_19792_) * partial};
        double side = com.hex.kagune.control.RinkakuRoot.side(limb, fanSide);
        return ThyDeath.strike(path, target, age, side, -Math.cos(yaw), -Math.sin(yaw), 0.9);
    }
}
