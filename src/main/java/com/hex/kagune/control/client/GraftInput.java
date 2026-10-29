package com.hex.kagune.control.client;

import com.hex.kagune.client.KaguneClientState;
import com.hex.kagune.client.KaguneKeys;
import com.hex.kagune.control.KagunePath;
import com.hex.kagune.control.pale.PaleRules;
import com.hex.kagune.control.graft.GraftMove;
import com.hex.kagune.control.network.C2SGraftAction;
import com.hex.kagune.control.network.ControlNetwork;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 7.0.0: Graft DNA's keys. His M1 is the original's own (the swipe trampoline sends it to his string, or, with a blade in hand, to
 * the blade and the kagune). The original's other kagune keys are his moves, whatever his stage puts on them (Z, J, V, B, N, H, G,
 * C: {@link GraftMove#of}), taken before the original's key handling so it never sends its own grab, slam or guard. K (tapped: the
 * kakuja; held: the next base stage) goes through {@code UkakuInput}'s timing like every path's.
 *
 * <p>His right click, with the kagune out, is the dash the way he is moving, as Pale's flips were: vanilla's right click comes first
 * (doors, chests, blocks placed and items used all go through untouched); the dash takes only the off hand's turn with nothing in
 * it. Crouching, it is always the normal use. One click, one dash.
 */
@Mod.EventBusSubscriber(modid = "hexkagune", value = Dist.CLIENT)
public final class GraftInput {
    /** False after a dash is sent until the button is let go. */
    private static boolean armed = true;

    private GraftInput() {
    }

    @SubscribeEvent
    public static void useInput(InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isUseItem()) {
            return;
        }
        Minecraft minecraft = Minecraft.m_91087_();
        LocalPlayer player = minecraft.f_91074_;
        if (player == null || minecraft.f_91080_ != null || PathClient.get(player) != KagunePath.GRAFT) {
            return;
        }
        KaguneClientState.View view = KaguneClientState.self();
        if (!view.unlocked() || !view.active() || player.m_6144_()) {
            return;
        }
        if (event.getHand() != InteractionHand.OFF_HAND || !player.m_21206_().m_41619_()) {
            return;
        }
        event.setCanceled(true);
        event.setSwingHand(false);
        if (!armed) {
            return;
        }
        armed = false;
        Input input = player.f_108618_;
        PaleRules.Flip way = input == null ? PaleRules.Flip.FORWARD : PaleRules.Flip.from(input.f_108567_, input.f_108566_);
        int arg = switch (way) {
            case BACK -> 1;
            case LEFT -> 2;
            case RIGHT -> 3;
            default -> 0;
        };
        ControlNetwork.CHANNEL.sendToServer(new C2SGraftAction(C2SGraftAction.STEP, arg, 0));
    }

    /** The original's keys and the move slot each is on his path. */
    private static final Object[][] KEYS = {{KaguneKeys.RAPID_STRIKE, GraftMove.Slot.Z}, {KaguneKeys.SLINGSHOT, GraftMove.Slot.J},
        {KaguneKeys.LIFT, GraftMove.Slot.V}, {KaguneKeys.SLAM, GraftMove.Slot.B}, {KaguneKeys.THROW, GraftMove.Slot.N},
        {KaguneKeys.CRUSH, GraftMove.Slot.H}, {KaguneKeys.GRAB, GraftMove.Slot.G}, {KaguneKeys.GUARD, GraftMove.Slot.C}};

    /** His move keys, each press one move (the server picks the stage's move on it), aimed at what is under the crosshair. */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void keys(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.m_91087_();
        LocalPlayer player = minecraft.f_91074_;
        if (player == null || PathClient.get(player) != KagunePath.GRAFT || !KaguneClientState.self().unlocked()) {
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
                ControlNetwork.CHANNEL.sendToServer(new C2SGraftAction(C2SGraftAction.KEY, ((GraftMove.Slot)entry[1]).ordinal(), target));
            }
        }
    }

    /** Takes a mutation from his menu (the server checks the points). */
    public static void unlockMutation(int ordinal) {
        ControlNetwork.CHANNEL.sendToServer(new C2SGraftAction(C2SGraftAction.MUTATION, ordinal, 0));
    }

    /** Picks a base stage from his menu. */
    public static void pickStage(int ordinal) {
        ControlNetwork.CHANNEL.sendToServer(new C2SGraftAction(C2SGraftAction.STAGE, ordinal, 0));
    }

    @SubscribeEvent
    public static void clientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.m_91087_();
        if (minecraft.f_91066_ == null || !minecraft.f_91066_.f_92095_.m_90857_()) {
            armed = true;
        }
    }
}
