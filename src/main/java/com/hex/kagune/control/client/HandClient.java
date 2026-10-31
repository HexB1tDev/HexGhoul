package com.hex.kagune.control.client;

import com.hex.kagune.control.npc.EtoGesture;
import com.hex.kagune.control.npc.HandSway;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 1.3.13: every hand held in sight, alive ({@link HandSway}): per NPC holding someone's hand, a spring stepped each tick from how the one
 * holding it moved (forward along their body's facing, and how far it turned). The NPC's renderer draws her where it puts her and her
 * arm reaching; the holder's body layer turns their arm to meet hers.
 */
@Mod.EventBusSubscriber(modid = "hexkagune", value = Dist.CLIENT)
public final class HandClient {
    private static final Map<Integer, HandSway> SWAYS = new HashMap<>();
    /** Per holder (entity id): the NPC whose hand they hold. */
    private static final Map<Integer, Integer> HOLDERS = new HashMap<>();

    private HandClient() {
    }

    @SubscribeEvent
    public static void clientTick(TickEvent.ClientTickEvent event) {
        Minecraft minecraft = Minecraft.m_91087_();
        if (event.phase != TickEvent.Phase.END || minecraft.f_91073_ == null || minecraft.m_91104_()) {
            return;
        }
        HOLDERS.clear();
        for (int id : EtoClient.ids()) {
            EtoClient.View view = EtoClient.get(id);
            Entity holder = view == null || view.gesture() != EtoGesture.HAND || view.with() == 0 ? null : minecraft.f_91073_.m_6815_(view.with());
            if (!(holder instanceof LivingEntity living)) {
                SWAYS.remove(id);
                continue;
            }
            HandSway sway = SWAYS.computeIfAbsent(id, ignored -> new HandSway());
            double yaw = Math.toRadians(living.f_20883_);
            double dx = living.m_20185_() - living.f_19790_;
            double dz = living.m_20189_() - living.f_19792_;
            double forward = dx * -Math.sin(yaw) + dz * Math.cos(yaw);
            float turn = ((living.f_20883_ - living.f_20884_) % 360.0F + 540.0F) % 360.0F - 180.0F;
            sway.step(forward, turn);
            HOLDERS.put(view.with(), id);
        }
        SWAYS.keySet().removeIf(id -> minecraft.f_91073_.m_6815_(id) == null);
    }

    /** The sway of the hand NPC {@code id} holds, or null if she holds none. */
    public static HandSway of(int id) {
        return SWAYS.get(id);
    }

    /** The sway of the hand the entity {@code holder} holds (an NPC's), or null. */
    public static HandSway held(int holder) {
        Integer npc = HOLDERS.get(holder);
        return npc == null ? null : SWAYS.get(npc);
    }

    /** The sway's clock {@code partial} through this tick. */
    public static double clock(HandSway sway, float partial) {
        return sway.ticks + partial;
    }

    @SubscribeEvent
    public static void loggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        SWAYS.clear();
        HOLDERS.clear();
    }
}
