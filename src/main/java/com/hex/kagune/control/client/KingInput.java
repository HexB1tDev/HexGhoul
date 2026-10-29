package com.hex.kagune.control.client;

import com.hex.kagune.client.KaguneKeys;
import com.hex.kagune.control.eto.EyeRules;
import com.hex.kagune.control.king.KingForm;
import com.hex.kagune.control.king.WarRules;
import com.hex.kagune.control.network.C2SKingAction;
import com.hex.kagune.control.network.ControlNetwork;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
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
 * 1.0.6: War's keys. While War has the body, the kagune's ability keys are its six abilities ({@link WarRules.Ability}: Z, J, G, B, C,
 * V), taken before the original's key handling so it never sees them. 1.0.13: each is a press (none is held any more), and goes with
 * what is under the crosshair; the server decides everything. 1.0.14: and M1 is his chain of blows ({@code WarStrike}) instead of a
 * punch: at a body, or the air; on a block it still mines, unless a body is right there by the crosshair.
 */
@Mod.EventBusSubscriber(modid = "hexkagune", value = Dist.CLIENT)
public final class KingInput {
    /** The keys, in the order of {@link WarRules.Ability}. */
    private static final KeyMapping[] KEYS = {KaguneKeys.RAPID_STRIKE, KaguneKeys.SLINGSHOT, KaguneKeys.GRAB, KaguneKeys.SLAM, KaguneKeys.GUARD,
        KaguneKeys.LIFT};
    /** 1.0.14: an M1 already went this tick; the attack key was down last tick (a held click on a block never starts a fight). */
    private static boolean struck;
    private static boolean attackWasDown;
    /** 1.0.20: the heavy (RMB) is held: pressed and not yet let go. */
    private static boolean heavyHeld;

    private KingInput() {
    }

    /**
     * 1.0.14: M1 in War. Taken before everything else (the eyes' own blitz included), so nothing else strikes as well: at a living
     * body or the air it is a blow at once; on a block it mines, unless a body is right by the crosshair ({@link EyeRules#strikesOverBlock}).
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void attackInput(InputEvent.InteractionKeyMappingTriggered event) {
        if (event.isUseItem()) {
            heavyInput(event);
            return;
        }
        if (!event.isAttack()) {
            return;
        }
        Minecraft minecraft = Minecraft.m_91087_();
        LocalPlayer player = minecraft.f_91074_;
        if (player == null || minecraft.f_91080_ != null || !worn()) {
            return;
        }
        HitResult hit = minecraft.f_91077_;
        int target = 0;
        boolean fresh = !struck;
        if (hit instanceof EntityHitResult entityHit) {
            if (!(entityHit.m_82443_() instanceof LivingEntity)) {
                return; // a boat, a frame, a cart: hit as ever
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
            ControlNetwork.CHANNEL.sendToServer(new C2SKingAction(C2SKingAction.STRIKE, flags(minecraft, player), target));
        }
    }

    /** 1.0.20: what a blow is clicked with ({@code KingCombo}): the jump key held, off the ground. */
    private static int flags(Minecraft minecraft, LocalPlayer player) {
        int flags = 0;
        if (minecraft.f_91066_ != null && minecraft.f_91066_.f_92089_.m_90857_()) {
            flags |= com.hex.kagune.control.king.KingCombo.JUMP;
        }
        if (!player.m_20096_()) {
            flags |= com.hex.kagune.control.king.KingCombo.AIR;
        }
        return flags;
    }

    /**
     * 1.0.20: RMB in War or Peace with the hand empty is the heavy: pressed here (and held: the game asks again and again while it is,
     * kept quiet), let go when the key comes up ({@link #keys}). On a block within reach it does what it always does.
     */
    private static void heavyInput(InputEvent.InteractionKeyMappingTriggered event) {
        Minecraft minecraft = Minecraft.m_91087_();
        LocalPlayer player = minecraft.f_91074_;
        if (player == null || minecraft.f_91080_ != null || !worn() || !player.m_21205_().m_41619_()) {
            return;
        }
        HitResult hit = minecraft.f_91077_;
        if (hit != null && hit.m_6662_() == HitResult.Type.BLOCK) {
            return; // a door, a chest, a lever: used as ever
        }
        event.setCanceled(true);
        event.setSwingHand(false);
        if (heavyHeld) {
            return;
        }
        heavyHeld = true;
        int target = hit instanceof EntityHitResult entityHit ? entityHit.m_82443_().m_19879_() : 0;
        ControlNetwork.CHANNEL.sendToServer(new C2SKingAction(C2SKingAction.HEAVY, flags(minecraft, player) | com.hex.kagune.control.king.KingCombo.PRESS,
            target));
    }

    /** 1.0.16: a final kakuja with keys and an M1 of its own is worn: War, or Peace. */
    private static boolean worn() {
        return KingClient.wearing(KingForm.WAR) || KingClient.wearing(KingForm.PEACE);
    }

    /** The living body nearest the crosshair close enough to strike over a block, or null. */
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

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void keys(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.m_91087_();
        attackWasDown = minecraft.f_91066_ != null && minecraft.f_91066_.f_92096_.m_90857_();
        struck = false;
        LocalPlayer player = minecraft.f_91074_;
        if (heavyHeld && (minecraft.f_91066_ == null || !minecraft.f_91066_.f_92095_.m_90857_() || player == null || !worn())) {
            // 1.0.20: the heavy let go: what it is depends on how long it was held (the server knows).
            heavyHeld = false;
            if (player != null) {
                int target = minecraft.f_91077_ instanceof EntityHitResult hit ? hit.m_82443_().m_19879_() : 0;
                ControlNetwork.CHANNEL.sendToServer(new C2SKingAction(C2SKingAction.HEAVY, flags(minecraft, player), target));
            }
        }
        if (player == null || !worn()) {
            // 1.0.28: a key held as the form went (or the world): let go.
            for (WarRules.Ability ability : WarRules.Ability.values()) {
                if (HELD[ability.ordinal()]) {
                    HELD[ability.ordinal()] = false;
                    if (player != null) {
                        send(ability, WarRules.RELEASE, 0);
                    }
                }
            }
            return;
        }
        boolean free = minecraft.f_91080_ == null;
        int target = minecraft.f_91077_ instanceof EntityHitResult hit ? hit.m_82443_().m_19879_() : 0;
        wyrm(minecraft, player, free);
        for (WarRules.Ability ability : WarRules.Ability.values()) {
            KeyMapping key = KEYS[ability.ordinal()];
            boolean pressed = false;
            while (key.m_90859_()) {
                pressed = true;
            }
            if (pressed && free) {
                send(ability, player.m_6144_() ? WarRules.CROUCH_PRESS : WarRules.PRESS, target);
                HELD[ability.ordinal()] = true;
            } else if (HELD[ability.ordinal()] && (!key.m_90857_() || !free)) {
                // 1.0.28: the held moves (Black Reaper's pull, the cocoon, the Spire Wave's charge) end when the key is let go.
                HELD[ability.ordinal()] = false;
                send(ability, WarRules.RELEASE, target);
            }
        }
    }

    /**
     * 1.0.29: as War's small Dragon (Centipede Dominion), the keys he holds go to the server every tick: it is his to drive (W A S D,
     * Space to leap, sprint to rush, sneak to come out of it). 1.0.33: and M1 and RMB (its bite and slam), which the chase camera takes
     * before the game sees them ({@link #wyrmClicks}).
     */
    private static void wyrm(Minecraft minecraft, LocalPlayer player, boolean free) {
        if (!driving(player)) {
            wyrmKeys = 0;
            wyrmClicks = 0;
            return;
        }
        int keys = free ? wyrmKeys : 0;
        if (free && minecraft.f_91066_ != null) {
            if (minecraft.f_91066_.f_92089_.m_90857_()) keys |= WarRules.WYRM_JUMP;
            if (minecraft.f_91066_.f_92091_.m_90857_()) keys |= WarRules.WYRM_SPRINT;
            keys |= wyrmClicks;
        }
        wyrmClicks = 0;
        // 1.0.39: and where he looks (the server hears nothing of his turning while the chase camera has the view).
        ControlNetwork.CHANNEL.sendToServer(new C2SKingAction(C2SKingAction.WYRM, keys, WarRules.wyrmAim(player.m_146908_(), player.m_146909_())));
    }

    /** 1.0.33: the movement keys he held this tick as the small Dragon, taken before anything zeroes them. */
    private static int wyrmKeys;
    /** 1.0.33: M1 and RMB clicked as the small Dragon since the keys last went. */
    private static int wyrmClicks;

    /** 1.0.33: {@code player} is War's small Dragon now (or turning into it). */
    static boolean driving(LocalPlayer player) {
        KingClient.View view = player == null ? null : KingClient.get(player);
        return view != null && view.moveAt(ControlClient.worldTime(0.0F)) == com.hex.kagune.control.king.KingMove.WYRM;
    }

    /**
     * 1.0.33: as the small Dragon his keys drive it, not him: read here first (before the awakening's camera zeroes them, which is
     * why it never went on W), then nothing moves his own body, held where he stood (a step of it and the server put him back,
     * turning his aim back with him: the steering fought the mouse).
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void movementInput(net.minecraftforge.client.event.MovementInputUpdateEvent event) {
        if (!(event.getEntity() instanceof LocalPlayer player) || !driving(player)) {
            return;
        }
        net.minecraft.client.player.Input input = event.getInput();
        int keys = 0;
        if (input.f_108567_ > 0.1F) keys |= WarRules.WYRM_FORWARD;
        if (input.f_108567_ < -0.1F) keys |= WarRules.WYRM_BACK;
        if (input.f_108566_ > 0.1F) keys |= WarRules.WYRM_LEFT;
        if (input.f_108566_ < -0.1F) keys |= WarRules.WYRM_RIGHT;
        if (input.f_108573_) keys |= WarRules.WYRM_SNEAK;
        wyrmKeys = keys;
        input.f_108567_ = 0.0F;
        input.f_108566_ = 0.0F;
        input.f_108572_ = false;
        input.f_108573_ = false;
    }

    /**
     * 1.0.36: through a move that carries his body along its own path ({@code KingMove#carries}), his keys move nothing: walking,
     * strafing or jumping only fought the path the server carries him along (the shaking, and being thrown off it).
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void carried(net.minecraftforge.client.event.MovementInputUpdateEvent event) {
        if (!(event.getEntity() instanceof LocalPlayer player)) {
            return;
        }
        KingClient.View view = KingClient.get(player);
        if (view == null || !view.on() || !view.moveAt(ControlClient.worldTime(0.0F)).carries()) {
            return;
        }
        net.minecraft.client.player.Input input = event.getInput();
        input.f_108567_ = 0.0F;
        input.f_108566_ = 0.0F;
        input.f_108572_ = false;
    }

    /** 1.0.33: M1 ({@code bite}) and RMB ({@code slam}) clicked while the chase camera has them (the awakening hands them on). */
    public static void wyrmClicks(boolean bite, boolean slam) {
        if (bite) {
            wyrmClicks |= WarRules.WYRM_KEY_BITE;
        }
        if (slam) {
            wyrmClicks |= WarRules.WYRM_KEY_SLAM;
        }
    }

    /** 1.0.28: which ability keys are held down since their press was sent (their letting go is sent too). */
    private static final boolean[] HELD = new boolean[WarRules.Ability.values().length];

    private static void send(WarRules.Ability ability, int action, int target) {
        ControlNetwork.CHANNEL.sendToServer(new C2SKingAction(C2SKingAction.ABILITY, ability.ordinal() << 2 | action, target));
    }
}
