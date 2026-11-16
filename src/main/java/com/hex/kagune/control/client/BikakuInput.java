package com.hex.kagune.control.client;

import com.hex.kagune.client.KaguneClientState;
import com.hex.kagune.client.KaguneKeys;
import com.hex.kagune.control.KagunePath;
import com.hex.kagune.control.bikaku.BikakuMove;
import com.hex.kagune.control.network.C2SBikakuAction;
import com.hex.kagune.control.network.ControlNetwork;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 3.5.0: the Bikaku path's keys. The original's other kagune keys are the worm's moves ({@link BikakuMove#of}: G Devouring Maw, J
 * Predatory Drag, and the rest), taken before the original's key handling so it never sends its own grab, grapple, slam or guard. K goes
 * through {@code UkakuInput}'s timing like every path's. 3.5.8 (the owner: "it should be a held move that the longer you hold it for, the
 * longer it goes for"): M1 and the move keys are held and let go (the server times the hold and throws it that far); M1 is taken here
 * out of the kakuja (in it, the original's swipe as before); the reach so far shows on the action bar while it is held (the server shows it); what it goes after
 * is the body nearest the line of the look within the farthest reach (the crosshair finds bodies only close by).
 */
@Mod.EventBusSubscriber(modid = "hexkagune", value = Dist.CLIENT)
public final class BikakuInput {
    private BikakuInput() {
    }

    /** The original's keys and the move slot each is on this path. */
    private static final Object[][] KEYS = {{KaguneKeys.RAPID_STRIKE, BikakuMove.Slot.Z}, {KaguneKeys.SLINGSHOT, BikakuMove.Slot.J},
        {KaguneKeys.LIFT, BikakuMove.Slot.V}, {KaguneKeys.SLAM, BikakuMove.Slot.B}, {KaguneKeys.THROW, BikakuMove.Slot.N},
        {KaguneKeys.CRUSH, BikakuMove.Slot.H}, {KaguneKeys.GRAB, BikakuMove.Slot.G}, {KaguneKeys.GUARD, BikakuMove.Slot.C}};

    /** 3.5.8: which move keys are held (by slot ordinal) and for how many ticks; M1 held and for how long. */
    private static final boolean[] HELD = new boolean[BikakuMove.Slot.values().length];
    private static final int[] HELD_FOR = new int[BikakuMove.Slot.values().length];
    private static boolean attackHeld;
    private static int attackHeldFor;

    /** 3.5.8: how close to the line of the look (the cosine of the angle off it) a body is picked, and how far out. */
    static final double AIM_CONE = 0.985;
    static final double AIM_FAR = com.hex.kagune.control.bikaku.BikakuRules.HELD_REACH + 1.0;

    /** True when the worm takes the keys: Bikaku, the kagune out, no screen. */
    private static boolean worm(Minecraft minecraft, LocalPlayer player) {
        return player != null && PathClient.get(player) == KagunePath.BIKAKU && KaguneClientState.self().unlocked() && KaguneClientState.self().active()
            && minecraft.f_91080_ == null;
    }

    /**
     * 3.5.8: M1 out of the kakuja: taken from the original's swipe and held (the worm winds up), unless it is on a block in reach with no body
     * to go after (it digs) or on something that isn't alive (a boat, a frame: hit as ever).
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void attackInput(InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isAttack()) {
            return;
        }
        Minecraft minecraft = Minecraft.m_91087_();
        LocalPlayer player = minecraft.f_91074_;
        if (!worm(minecraft, player) || QuinqueClient.armed(player) || BikakuClient.get(player).kakuja()) {
            return;
        }
        HitResult hit = minecraft.f_91077_;
        LivingEntity aimed = aimed(player);
        if (hit instanceof EntityHitResult entityHit && !(entityHit.m_82443_() instanceof LivingEntity)) {
            return;
        }
        if (hit != null && hit.m_6662_() == HitResult.Type.BLOCK && aimed == null) {
            return;
        }
        event.setCanceled(true);
        event.setSwingHand(false);
        if (!attackHeld) {
            attackHeld = true;
            attackHeldFor = 0;
            ControlNetwork.CHANNEL.sendToServer(new C2SBikakuAction(C2SBikakuAction.ATTACK, 0, target(minecraft, aimed)));
        }
    }

    /** Its move keys: each press winds a thrown move up (or makes a move at once), each letting go throws it; M1 let go likewise. */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void keys(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.m_91087_();
        LocalPlayer player = minecraft.f_91074_;
        if (player == null || PathClient.get(player) != KagunePath.BIKAKU || !KaguneClientState.self().unlocked()) {
            letAllGo(minecraft, null);
            return;
        }
        boolean takes = worm(minecraft, player);
        LivingEntity aimed = null;
        for (Object[] entry : KEYS) {
            KeyMapping key = (KeyMapping)entry[0];
            BikakuMove.Slot slot = (BikakuMove.Slot)entry[1];
            boolean pressed = false;
            while (key.m_90859_()) {
                pressed = true;
            }
            int s = slot.ordinal();
            if (pressed && takes && !HELD[s]) {
                aimed = aimed == null ? aimed(player) : aimed;
                ControlNetwork.CHANNEL.sendToServer(new C2SBikakuAction(C2SBikakuAction.KEY, s, target(minecraft, aimed)));
                HELD[s] = true;
                HELD_FOR[s] = 0;
            } else if (HELD[s] && (!key.m_90857_() || !takes)) {
                aimed = aimed == null ? aimed(player) : aimed;
                ControlNetwork.CHANNEL.sendToServer(new C2SBikakuAction(C2SBikakuAction.KEY_UP, s, target(minecraft, aimed)));
                HELD[s] = false;
            } else if (HELD[s]) {
                HELD_FOR[s]++;
            }
        }
        if (attackHeld) {
            boolean down = minecraft.f_91066_ != null && minecraft.f_91066_.f_92096_.m_90857_();
            if (!down || !takes) {
                aimed = aimed == null ? aimed(player) : aimed;
                ControlNetwork.CHANNEL.sendToServer(new C2SBikakuAction(C2SBikakuAction.ATTACK_UP, 0, target(minecraft, aimed)));
                attackHeld = false;
            } else {
                attackHeldFor++;
            }
        }
    }

    /** Lets every held key go (the path or the kagune gone while one was held), telling the server. */
    private static void letAllGo(Minecraft minecraft, LivingEntity aimed) {
        for (int s = 0; s < HELD.length; s++) {
            if (HELD[s]) {
                HELD[s] = false;
                if (minecraft.f_91074_ != null) {
                    ControlNetwork.CHANNEL.sendToServer(new C2SBikakuAction(C2SBikakuAction.KEY_UP, s, 0));
                }
            }
        }
        if (attackHeld) {
            attackHeld = false;
            if (minecraft.f_91074_ != null) {
                ControlNetwork.CHANNEL.sendToServer(new C2SBikakuAction(C2SBikakuAction.ATTACK_UP, 0, 0));
            }
        }
    }

    /** What it goes after: the body under the crosshair, else the one nearest the line of the look, else none (0). */
    private static int target(Minecraft minecraft, LivingEntity aimed) {
        if (minecraft.f_91077_ instanceof EntityHitResult hit && hit.m_82443_() instanceof LivingEntity) {
            return hit.m_82443_().m_19879_();
        }
        return aimed == null ? 0 : aimed.m_19879_();
    }

    /** The living body nearest the line of the look within {@link #AIM_FAR}, inside {@link #AIM_CONE} of it (or null). */
    static LivingEntity aimed(LocalPlayer player) {
        Vec3 eye = player.m_146892_();
        Vec3 look = player.m_20154_();
        LivingEntity best = null;
        double bestOff = Double.MAX_VALUE;
        for (LivingEntity body : player.m_9236_().m_6443_(LivingEntity.class, player.m_20191_().m_82400_(AIM_FAR),
            entity -> entity != player && entity.m_6084_() && !entity.m_5833_())) {
            Vec3 to = body.m_20182_().m_82520_(0.0, body.m_20206_() * 0.5, 0.0).m_82546_(eye);
            double distance = to.m_82553_();
            if (distance < 1.0E-4 || distance > AIM_FAR) {
                continue;
            }
            double facing = to.m_82490_(1.0 / distance).m_82526_(look);
            double allowance = Math.min(0.05, (body.m_20205_() * 0.5 + 0.4) / distance);
            if (facing < AIM_CONE - allowance) {
                continue;
            }
            double off = (1.0 - facing) * distance;
            if (off < bestOff) {
                bestOff = off;
                best = body;
            }
        }
        return best;
    }
}
