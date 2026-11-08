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
import net.minecraft.world.phys.EntityHitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 3.5.0: the Bikaku path's keys. Its M1 is the original's own (the swipe trampoline sends it to the worm's bites). The original's other
 * kagune keys are the worm's moves ({@link BikakuMove#of}: G Devouring Maw, J Predatory Drag, and those to come), taken before the
 * original's key handling so it never sends its own grab, grapple, slam or guard. K goes through {@code UkakuInput}'s timing like every
 * path's.
 */
@Mod.EventBusSubscriber(modid = "hexkagune", value = Dist.CLIENT)
public final class BikakuInput {
    private BikakuInput() {
    }

    /** The original's keys and the move slot each is on this path. */
    private static final Object[][] KEYS = {{KaguneKeys.RAPID_STRIKE, BikakuMove.Slot.Z}, {KaguneKeys.SLINGSHOT, BikakuMove.Slot.J},
        {KaguneKeys.LIFT, BikakuMove.Slot.V}, {KaguneKeys.SLAM, BikakuMove.Slot.B}, {KaguneKeys.THROW, BikakuMove.Slot.N},
        {KaguneKeys.CRUSH, BikakuMove.Slot.H}, {KaguneKeys.GRAB, BikakuMove.Slot.G}, {KaguneKeys.GUARD, BikakuMove.Slot.C}};

    /** Its move keys, each press one move (the server picks the move on it), aimed at what is under the crosshair. */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void keys(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.m_91087_();
        LocalPlayer player = minecraft.f_91074_;
        if (player == null || PathClient.get(player) != KagunePath.BIKAKU || !KaguneClientState.self().unlocked()) {
            return;
        }
        boolean open = minecraft.f_91080_ == null;
        boolean active = KaguneClientState.self().active();
        int target = minecraft.f_91077_ instanceof EntityHitResult hit ? hit.m_82443_().m_19879_() : 0;
        for (Object[] entry : KEYS) {
            KeyMapping key = (KeyMapping)entry[0];
            boolean pressed = false;
            while (key.m_90859_()) {
                pressed = true;
            }
            if (pressed && open && active) {
                ControlNetwork.CHANNEL.sendToServer(new C2SBikakuAction(C2SBikakuAction.KEY, ((BikakuMove.Slot)entry[1]).ordinal(), target));
            }
        }
    }
}
