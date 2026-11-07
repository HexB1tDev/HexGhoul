package com.hex.kagune.control.client;

import com.hex.kagune.control.ControlItems;
import com.hex.kagune.control.client.animation.QuinqueGrip;
import com.hex.kagune.control.client.animation.QuinquePose;
import com.hex.kagune.control.client.animation.RigPose;
import com.hex.kagune.control.client.animation.WeaponPose;
import com.hex.kagune.control.network.C2SArmAction;
import com.hex.kagune.control.network.ControlNetwork;
import com.hex.kagune.control.network.S2CArmSwing;
import com.hex.kagune.control.npc.ArmRules;
import com.hex.kagune.control.quinque.Arm;
import com.hex.kagune.control.quinque.Quinque;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 3.0.6: a player's kagune-made weapon ({@link Arm}) on the client: M1 asks for the next blow of its string, right click its first move and
 * Shift + right click its second ({@link C2SArmAction}; a chest or a furnace under the crosshair is still used, a block is
 * still mined); every player's blows and moves as the server tells them ({@link S2CArmSwing}); their body posed with it as an investigator's
 * is ({@link WeaponPose}: its stance, each blow, each move, both hands on a two-handed one).
 */
@Mod.EventBusSubscriber(modid = "hexkagune", value = Dist.CLIENT)
public final class WeaponClient {
    /** A body's weapon as the server last told it: what it swung ({@code ArmRules} codes, -1 none), when, whom its whip binds. */
    public record View(int code, long at, int bound) {
    }

    private static final Map<Integer, View> VIEWS = new HashMap<>();
    /** The last right click asked for (game time): one held down repeats every four ticks, asked for once. */
    private static long lastUse = Long.MIN_VALUE;

    private WeaponClient() {
    }

    public static void accept(S2CArmSwing message) {
        if (VIEWS.size() > 256) {
            VIEWS.clear();
            ANCHORS.clear();
        }
        VIEWS.put(message.player(), new View(message.code(), message.at(), message.bound()));
        // 3.1.8: a Tendril Swing's latch, from where its swinger was as it bit (they are hauled away from there).
        double[] off = ArmRules.ability(message.code()) == Arm.Ability.SWING ? com.hex.kagune.control.quinque.WhipPath.anchor(message.bound()) : null;
        net.minecraft.world.entity.Entity who = off == null || Minecraft.m_91087_().f_91073_ == null ? null
            : Minecraft.m_91087_().f_91073_.m_6815_(message.player());
        if (who != null) {
            ANCHORS.put(message.player(), new double[] {who.m_20185_() + off[0], who.m_20186_() + off[1], who.m_20189_() + off[2]});
        } else {
            ANCHORS.remove(message.player());
        }
    }

    private static final Map<Integer, double[]> ANCHORS = new HashMap<>();

    /** 3.1.8: where {@code id}'s Tendril Swing bit (world), or null. */
    public static double[] anchor(int id) {
        return ANCHORS.get(id);
    }

    /** {@code id}'s weapon as last told, or null. */
    public static View view(int id) {
        return VIEWS.get(id);
    }

    /** Ticks since {@code view}'s swing began (-1 with none). */
    public static double age(View view, float partial) {
        if (view == null || view.code() < 0) {
            return -1.0;
        }
        return ControlClient.worldTime(partial) - view.at();
    }

    /** How far out of its case the weapon in {@code stack} is (0 .. 1): it grows out of the hand as it is torn out. */
    public static float shown(ItemStack stack, float partial) {
        CompoundTag tag = stack.m_41783_();
        if (tag == null || !tag.m_128441_(Quinque.AT)) {
            return 1.0F;
        }
        double age = ControlClient.worldTime(partial) - tag.m_128454_(Quinque.AT);
        return age < 0.0 || age > 200.0 ? 1.0F : ArmRules.shown(true, age);
    }

    // ---------------------------------------------------------------- the body

    /**
     * {@code body}'s pose with {@code arm} out in its hand ({@code held}): tearing out of its case, a blow of its string, one of its moves, or
     * its stance (its feet left to the walk while it moves and swings nothing); blended out of the last over a few ticks; both hands on a
     * two-handed one (but the javelin's throw).
     */
    public static RigPose pose(LivingEntity body, ItemStack held, Arm arm, float partial) {
        float shown = shown(held, partial);
        View view = view(body.m_19879_());
        double age = age(view, partial);
        int code = view == null ? -1 : view.code();
        int step = ArmRules.step(code);
        Arm.Ability ability = ArmRules.ability(code);
        Quinque base = WeaponPose.base(arm);
        boolean oneHanded = !arm.twoHanded;
        boolean acting = false;
        RigPose p;
        if (shown < 1.0F) {
            p = QuinquePose.unfolding(base, shown);
        } else if (step >= 0 && age >= 0.0 && age < arm.strike(step).total()) {
            p = WeaponPose.strike(arm, step, age);
            acting = true;
        } else if (ability != null && arm.slot(ability) >= 0 && age >= 0.0 && age < ability.total) {
            p = WeaponPose.ability(arm, ability, age);
            oneHanded |= WeaponPose.oneHanded(ability, age);
            acting = true;
        } else {
            p = WeaponPose.stance(arm);
        }
        if (p == null) {
            return null;
        }
        double speed = Math.hypot(body.m_20185_() - body.f_19790_, body.m_20189_() - body.f_19792_);
        if (!acting && speed > 0.02) {
            // Walking or running with it ready: the feet are the walk's, the body upright over them.
            p.legHold = 0.0F;
            p.rightLegX = 0.0F;
            p.leftLegX = 0.0F;
            p.rightLegZ = 0.0F;
            p.leftLegZ = 0.0F;
            p.drop = 0.0F;
            p.lunge = 0.0F;
        }
        p = AgentClient.flow(body.m_19879_(), view == null ? Integer.MIN_VALUE : (int)view.at(), code, p, ControlClient.worldTime(partial));
        if (!oneHanded && QuinquePose.gripped(shown)) {
            boolean slim = body instanceof net.minecraft.client.player.AbstractClientPlayer player && "slim".equals(player.m_108564_());
            QuinqueGrip.reach(p, base, slim);
        }
        return p;
    }

    // ---------------------------------------------------------------- the keys

    /**
     * M1 with it out: at a living body or the air, the next blow of its string (no punch: the server swings it); on a block it mines; 3.1.8
     * Sneak + M1 its signature move. Right click: its first move (Shift: its second), at what is under the crosshair; a chest or a furnace
     * is still used.
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void input(InputEvent.InteractionKeyMappingTriggered event) {
        Minecraft minecraft = Minecraft.m_91087_();
        LocalPlayer player = minecraft.f_91074_;
        if (player == null || minecraft.f_91080_ != null || event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        Arm arm = ControlItems.armWeapon(player.m_21205_());
        if (arm == null) {
            return;
        }
        HitResult hit = minecraft.f_91077_;
        int target = hit instanceof EntityHitResult entityHit && entityHit.m_82443_() instanceof LivingEntity ? entityHit.m_82443_().m_19879_() : 0;
        if (event.isAttack()) {
            if (hit != null && hit.m_6662_() == HitResult.Type.BLOCK) {
                return; // dig
            }
            if (hit instanceof EntityHitResult entityHit && !(entityHit.m_82443_() instanceof LivingEntity)) {
                return; // a boat, a frame, a cart: hit as ever
            }
            event.setCanceled(true);
            event.setSwingHand(false);
            // 3.1.8: Sneak + M1 is its signature move (the whip's Tendril Swing, the hammer's Fault Line, the greatsword's Bone Wall).
            boolean signature = player.m_6047_() && arm.signature() != null;
            ControlNetwork.CHANNEL.sendToServer(new C2SArmAction(signature ? C2SArmAction.THIRD : C2SArmAction.STRIKE, target));
        } else if (event.isUseItem()) {
            if (hit instanceof BlockHitResult block && hit.m_6662_() == HitResult.Type.BLOCK && usable(player, block)) {
                return;
            }
            event.setCanceled(true);
            event.setSwingHand(false);
            long now = player.m_9236_().m_46467_();
            boolean held = lastUse != Long.MIN_VALUE && now - lastUse >= 0L && now - lastUse <= 6L;
            lastUse = now;
            if (!held) {
                ControlNetwork.CHANNEL.sendToServer(new C2SArmAction(player.m_6047_() ? C2SArmAction.SECOND : C2SArmAction.FIRST, target));
            }
        }
    }

    /** True for a block a right click is meant for: one with a menu or a store (a chest, a furnace, a barrel). */
    private static boolean usable(LocalPlayer player, BlockHitResult hit) {
        return player.m_9236_().m_7702_(hit.m_82425_()) != null;
    }
}
