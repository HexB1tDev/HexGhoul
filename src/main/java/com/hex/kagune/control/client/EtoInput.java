package com.hex.kagune.control.client;

import com.hex.kagune.client.KaguneClientState;
import com.hex.kagune.client.KaguneKeys;
import com.hex.kagune.control.KagunePath;
import com.hex.kagune.control.eto.EtoKey;
import com.hex.kagune.control.eto.EtoRules;
import com.hex.kagune.control.eto.EyeRules;
import com.hex.kagune.control.network.C2SEtoAction;
import com.hex.kagune.control.network.ControlNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Eto's keys (6.1.0). Her M1 is the original's own (the swipe trampoline sends it to her Speed Blitz). Her right click, with her
 * base form out, is a flip the way she is moving: W (or nothing) forward, S back, A and D to the sides. Vanilla's right click
 * comes first, as for Ayato's dash: Forge asks once per hand, main hand first, and only asks the off hand when the main hand did
 * nothing, so doors, chests, levers, trades, blocks placed and items used all go through untouched; the flip takes only the off
 * hand's turn with nothing in it. Crouching, it is always the normal use. One click, one flip.
 */
@Mod.EventBusSubscriber(modid = "hexkagune", value = Dist.CLIENT)
public final class EtoInput {
    /** False after a flip is sent until the button is let go. */
    private static boolean armed = true;

    private EtoInput() {
    }

    @SubscribeEvent
    public static void useInput(InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isUseItem()) {
            return;
        }
        Minecraft minecraft = Minecraft.m_91087_();
        LocalPlayer player = minecraft.f_91074_;
        // 7.0.1: and every other ghoul's with only the eyes open (the shared eye-mode fight).
        if (player == null || minecraft.f_91080_ != null || !EtoClient.fighter(player) || player.m_6144_()) {
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
        EtoRules.Flip flip = input == null ? EtoRules.Flip.FORWARD : EtoRules.Flip.from(input.f_108567_, input.f_108566_);
        ControlNetwork.CHANNEL.sendToServer(new C2SEtoAction(C2SEtoAction.FLIP, flip.ordinal(), 0));
    }

    /** 7.0.1: M1 sent to the fight this tick (a press on a block fires twice in one tick: the click and the hold). */
    private static boolean struck;
    /** 7.0.1: M1 was down at the end of the last tick (so a hold on a block next to a fight strikes only once). */
    private static boolean attackWasDown;

    /**
     * 7.0.1, the shared eye-mode fight: with only the eyes open M1 is her Speed Blitz (the original sends an M1 only with the kagune
     * out). On a body (a living one; a boat, a frame or a cart is still hit as ever) or the air it strikes at once; on a block it
     * still mines, unless a body is right there by the crosshair ({@link EyeRules#strikesOverBlock}), when it strikes at that and
     * a hold never starts digging.
     */
    @SubscribeEvent
    public static void attackInput(InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isAttack()) {
            return;
        }
        Minecraft minecraft = Minecraft.m_91087_();
        LocalPlayer player = minecraft.f_91074_;
        if (player == null || minecraft.f_91080_ != null || !EtoClient.eyes(player)) {
            return;
        }
        HitResult hit = minecraft.f_91077_;
        int target = 0;
        boolean fresh = !struck;
        if (hit instanceof EntityHitResult entityHit) {
            if (!(entityHit.m_82443_() instanceof LivingEntity)) {
                return;
            }
            target = entityHit.m_82443_().m_19879_();
        } else if (hit != null && hit.m_6662_() == HitResult.Type.BLOCK) {
            LivingEntity near = aimed(player);
            if (near == null) {
                return; // nothing to fight: dig
            }
            target = near.m_19879_();
            fresh &= !attackWasDown;
        }
        event.setCanceled(true);
        event.setSwingHand(false);
        if (fresh) {
            struck = true;
            ControlNetwork.CHANNEL.sendToServer(new C2SEtoAction(C2SEtoAction.STRIKE, 0, target));
        }
    }

    /** The living body nearest the crosshair close enough to strike over a block ({@link EyeRules#strikesOverBlock}), or null. */
    private static LivingEntity aimed(LocalPlayer player) {
        Vec3 eye = player.m_146892_();
        Vec3 look = player.m_20154_();
        LivingEntity best = null;
        double bestDistance = Double.MAX_VALUE;
        for (LivingEntity body : player.m_9236_().m_6443_(LivingEntity.class, player.m_20191_().m_82400_(EyeRules.AIM_RANGE),
            entity -> entity != player && entity.m_6084_() && !entity.m_5833_())) {
            Vec3 to = body.m_20182_().m_82520_(0.0, body.m_20206_() * 0.5, 0.0).m_82546_(eye);
            double distance = to.m_82553_();
            if (distance < 1.0E-4 || !EyeRules.strikesOverBlock(distance - body.m_20205_() * 0.5, to.m_82490_(1.0 / distance).m_82526_(look))) {
                continue;
            }
            if (distance < bestDistance) {
                bestDistance = distance;
                best = body;
            }
        }
        return best;
    }

    /** Z (the original's Rapid Strike key): Big Punch, charged from the moment it goes down. */
    private static final EtoKey PUNCH = new EtoKey(true);
    /** J (Slingshot's key): the flesh swipes on a tap, grabs on a hold. */
    private static final EtoKey FLESH = new EtoKey(false);
    /** V (Lift's key): the speed kick on a tap, the flesh hammer on a hold. */
    private static final EtoKey KICK = new EtoKey(false);
    /** 6.2.0, N (Throw's key): the Mutated Arm, shot out on a tap, swelling on a hold. */
    private static final EtoKey ARM = new EtoKey(false);

    /**
     * Z, J and V on her path with her base form out: taken before the original's key handling (so it never sends its own Rapid
     * Strike, Slingshot or Lift) and timed into taps and holds. 7.0.1: with only the eyes open (any other path), Z and V alone
     * (the Big Punch and the Speed Kick), taken before the path's own keys (HIGHEST); the rest are the path's.
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void keys(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.m_91087_();
        LocalPlayer player = minecraft.f_91074_;
        KaguneClientState.View view = KaguneClientState.self();
        if (player != null && EtoClient.eyes(player)) {
            boolean open = minecraft.f_91080_ == null;
            boolean z = consume(KaguneKeys.RAPID_STRIKE);
            send(PUNCH.tick(z, open && KaguneKeys.RAPID_STRIKE.m_90857_()), C2SEtoAction.PUNCH, -1, C2SEtoAction.PUNCH);
            if (consume(KaguneKeys.LIFT) && open) {
                ControlNetwork.CHANNEL.sendToServer(new C2SEtoAction(C2SEtoAction.KICK, 1, 0));
            }
            return;
        }
        if (player == null || PathClient.get(player) != KagunePath.ETO || !view.unlocked() || !view.active()) {
            if (player != null) {
                // Let anything still held go, so a hold never outlives the form.
                send(PUNCH.tick(false, false), C2SEtoAction.PUNCH, -1, C2SEtoAction.PUNCH);
                send(FLESH.tick(false, false), -1, C2SEtoAction.GRAB, -1);
                send(KICK.tick(false, false), -1, C2SEtoAction.HAMMER, -1);
                arm(ARM.tick(false, false));
            }
            return;
        }
        boolean open = minecraft.f_91080_ == null;
        boolean z = consume(KaguneKeys.RAPID_STRIKE);
        boolean j = consume(KaguneKeys.SLINGSHOT);
        boolean v = consume(KaguneKeys.LIFT);
        send(PUNCH.tick(z, open && KaguneKeys.RAPID_STRIKE.m_90857_()), C2SEtoAction.PUNCH, -1, C2SEtoAction.PUNCH);
        send(FLESH.tick(j, open && KaguneKeys.SLINGSHOT.m_90857_()), C2SEtoAction.SWIPE, C2SEtoAction.GRAB, -1);
        send(KICK.tick(v, open && KaguneKeys.LIFT.m_90857_()), C2SEtoAction.KICK, C2SEtoAction.HAMMER, -1);
        // 6.2.0: B (the original's Slam key) is Watch This, on a press.
        if (consume(KaguneKeys.SLAM) && open) {
            ControlNetwork.CHANNEL.sendToServer(new C2SEtoAction(C2SEtoAction.WATCH, 0, 0));
        }
        boolean n = consume(KaguneKeys.THROW);
        arm(ARM.tick(n, open && KaguneKeys.THROW.m_90857_()));
    }

    /** The Mutated Arm's key: a tap; or a hold (2 as it starts, 1 still down, 0 let go: only its start shoots the arm out). */
    private static void arm(int what) {
        if (what == EtoKey.NONE) {
            return;
        }
        int action = what == EtoKey.TAP ? C2SEtoAction.ARM_TAP : C2SEtoAction.ARM;
        int arg = what == EtoKey.HOLD ? 2 : what == EtoKey.LET_GO ? 0 : 1;
        ControlNetwork.CHANNEL.sendToServer(new C2SEtoAction(action, arg, 0));
    }

    private static boolean consume(net.minecraft.client.KeyMapping key) {
        boolean pressed = false;
        while (key.m_90859_()) {
            pressed = true;
        }
        return pressed;
    }

    /** Tells the server: {@code tap} on a tap, {@code hold} on a hold (1 down, 0 let go), {@code once} for a hold-at-once key. */
    private static void send(int what, int tap, int hold, int once) {
        int action = what == EtoKey.TAP ? tap : once >= 0 ? once : hold;
        if (what == EtoKey.NONE || action < 0) {
            return;
        }
        int arg = what == EtoKey.LET_GO ? 0 : 1;
        ControlNetwork.CHANNEL.sendToServer(new C2SEtoAction(action, arg, 0));
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
        attackWasDown = minecraft.f_91066_ != null && minecraft.f_91066_.f_92096_.m_90857_();
        struck = false;
    }
}
