package com.hex.kagune.control.client;

import com.hex.kagune.client.KaguneClientState;
import com.hex.kagune.client.KaguneKeys;
import com.hex.kagune.control.KagunePath;
import com.hex.kagune.control.RinkakuForms;
import com.hex.kagune.control.ukaku.UkakuRules;
import com.hex.kagune.control.ukaku.KakujaKey;
import com.hex.kagune.control.ukaku.WingForm;
import com.hex.kagune.control.network.C2SUkakuAction;
import com.hex.kagune.control.network.ControlNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Ukaku's right click: with the wings out it is his M2, the ender of the M1 chain (the server picks which from the shots
 * fired so far, from the fire's enders or, in the crystal mode, the crystal's). It stays the normal use when that is clearly what is meant: crouching, holding something with a use (food,
 * a bow, a shield), or pointing at a block (a door, a chest) outside a fight. One click, one ender: holding the button
 * does not repeat it. It also holds the Gatling's trigger: J (the original's Slingshot key) held down fires it, and times K
 * (the original's tendril-cycle key) on both paths: a tap is the first kakuja, holding it the final kakuja.
 */
@Mod.EventBusSubscriber(modid = "hexkagune", value = Dist.CLIENT)
public final class UkakuInput {
    /** False after an ender is sent until the button is let go. */
    private static boolean armed = true;
    /** The Gatling's trigger (J, the Slingshot key) as last told to the server, and ticks until it is told again. */
    private static boolean gatlingHeld;
    private static int gatlingResend;
    /** While J is held the server is reminded this often (it stops the Gatling without word for a moment). */
    private static final int GATLING_RESEND_TICKS = 4;
    /** K being timed. */
    private static final KakujaKey KAKUJA_KEY = new KakujaKey();

    private UkakuInput() {
    }

    @SubscribeEvent
    public static void useInput(InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isUseItem()) {
            return;
        }
        Minecraft minecraft = Minecraft.m_91087_();
        LocalPlayer player = minecraft.f_91074_;
        if (player == null || minecraft.f_91080_ != null) {
            return;
        }
        KagunePath path = PathClient.get(player);
        KaguneClientState.View view = KaguneClientState.self();
        if (path != KagunePath.UKAKU && path != KagunePath.RINKAKU || !view.unlocked() || !view.active() || player.m_6144_()) {
            return;
        }
        // 6.0.0: the kakuja's dash: Ukaku's Steel Blade, Rinkaku's Full Kakuja. W (or nothing) forward, S back.
        boolean dashes = path == KagunePath.UKAKU ? UkakuClient.blade(player, ControlClient.worldTime(0.0F)) >= 1.0
            : view.form() == com.hex.kagune.gameplay.KaguneForm.FULL_KAKUJA;
        if (path == KagunePath.RINKAKU && !dashes) {
            return;
        }
        if (dashes) {
            // Vanilla's right click comes first. Forge asks once per hand, main hand first, and only asks for the off hand when
            // the main hand did nothing: so a door, a chest, a lever, a trade, a mount, a block placed or an item used all go
            // through untouched, and the dash takes only what was left, the off hand's turn with nothing in it.
            if (event.getHand() != InteractionHand.OFF_HAND || !player.m_21206_().m_41619_()) {
                return;
            }
            event.setCanceled(true);
            event.setSwingHand(false);
            if (!armed) {
                return;
            }
            armed = false;
            boolean back = minecraft.f_91066_.f_92087_.m_90857_() && !minecraft.f_91066_.f_92085_.m_90857_();
            ControlNetwork.CHANNEL.sendToServer(new C2SUkakuAction(C2SUkakuAction.DASH, back ? -1 : 1));
            return;
        }
        ItemStack held = player.m_21205_();
        if (!held.m_41619_() && held.m_41779_() > 0) {
            return;
        }
        HitResult hit = minecraft.f_91077_;
        if (hit != null && hit.m_6662_() == HitResult.Type.BLOCK && !fighting()) {
            return;
        }
        event.setCanceled(true);
        event.setSwingHand(false);
        if (!armed) {
            return;
        }
        armed = false;
        int target = hit instanceof EntityHitResult entityHit ? entityHit.m_82443_().m_19879_() : 0;
        // W held with it: Fire Dash (a mutation) dashes forward instead of hopping back; the server decides.
        boolean forward = minecraft.f_91066_.f_92085_.m_90857_();
        ControlNetwork.CHANNEL.sendToServer(new C2SUkakuAction(forward ? C2SUkakuAction.ENDER_FORWARD : C2SUkakuAction.ENDER, target));
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
        gatling(minecraft);
    }

    /**
     * K on both paths: takes the presses before the original's key handling (so it never sends its own K) and tells the server
     * a tap or a hold ({@link KakujaKey}): the first kakuja or the final one, Ukaku's or Rinkaku's ({@code RinkakuForms}).
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void kakujaKey(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.m_91087_();
        LocalPlayer player = minecraft.f_91074_;
        KaguneClientState.View view = KaguneClientState.self();
        KagunePath path = player == null ? KagunePath.NONE : PathClient.get(player);
        if (path == KagunePath.NONE || !view.unlocked()) {
            KAKUJA_KEY.reset();
            return;
        }
        boolean pressed = false;
        while (KaguneKeys.CYCLE_TENDRILS.m_90859_()) {
            pressed = true;
        }
        boolean down = minecraft.f_91080_ == null && KaguneKeys.CYCLE_TENDRILS.m_90857_();
        // 7.0.0: Graft DNA's K hold picks his next base stage, at any mastery.
        // 1.0.10: Rinkaku's K held is his final kakuja (War or Peace) once he has chosen it, whatever his mastery.
        boolean finalUnlocked = path == KagunePath.GRAFT || (path == KagunePath.UKAKU ? WingForm.FINAL_KAKUJA.unlocked(view.mastery())
            : RinkakuForms.finalKakuja().isUnlocked(view.mastery()) || KingClient.chosen().built());
        int sent = KAKUJA_KEY.tick(pressed, down, finalUnlocked);
        if (sent != KakujaKey.NONE) {
            ControlNetwork.CHANNEL.sendToServer(new C2SUkakuAction(sent == KakujaKey.HOLD ? C2SUkakuAction.FINAL_KAKUJA : C2SUkakuAction.KAKUJA, 0));
        }
    }

    /** The Gatling is held, not pressed: J down tells the server every few ticks, J up tells it once. */
    private static void gatling(Minecraft minecraft) {
        LocalPlayer player = minecraft.f_91074_;
        KaguneClientState.View view = KaguneClientState.self();
        boolean down = player != null && minecraft.f_91080_ == null && PathClient.get(player) == KagunePath.UKAKU && view.unlocked() && view.active()
            && KaguneKeys.SLINGSHOT.m_90857_();
        if (down) {
            if (!gatlingHeld || --gatlingResend <= 0) {
                ControlNetwork.CHANNEL.sendToServer(new C2SUkakuAction(C2SUkakuAction.GATLING, 1));
                gatlingResend = GATLING_RESEND_TICKS;
            }
            gatlingHeld = true;
        } else if (gatlingHeld) {
            gatlingHeld = false;
            if (player != null) {
                ControlNetwork.CHANNEL.sendToServer(new C2SUkakuAction(C2SUkakuAction.GATLING, 0));
            }
        }
    }

    /** True while an M1 chain is going (a block under the crosshair is then just where the fight is, not a door). */
    private static boolean fighting() {
        ControlView control = ControlClient.self();
        if (!control.comboStyle().ukakuShots() || !control.hasCombo()) {
            return false;
        }
        double since = ControlClient.worldTime(0.0F) - (control.comboStarted() + control.comboDuration());
        return since <= UkakuRules.ENDER_WINDOW_TICKS;
    }
}
