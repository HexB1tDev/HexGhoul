package com.hex.kagune.control.client;

import com.hex.kagune.client.KaguneClientState;
import com.hex.kagune.control.KagunePath;
import com.hex.kagune.control.RinkakuBlock;
import com.hex.kagune.control.RinkakuForms;
import com.hex.kagune.control.network.C2SUkakuAction;
import com.hex.kagune.control.network.ControlNetwork;
import com.hex.kagune.control.network.S2CRinkakuBlock;
import com.hex.kagune.gameplay.KaguneForm;
import com.hex.kagune.gameplay.TendrilMode;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 2.0.42: Rinkaku's block as the client has it ({@link RinkakuBlock}): the right click held (vanilla's right click comes first, as for
 * Graft's dash: a door, a chest, a block placed or an item used go through untouched; the block takes only the off hand's turn with
 * nothing in it; crouching it is the normal use), sent to the server and shown at once on his own screen; and every Rinkaku's block as it
 * is drawn: the tails swung into it ({@link #apply}, from {@code StrikePaths.clearOwner}) and the body's guard
 * ({@link #amount}, {@link #jolt}, {@code KaguneBodyAnimation}).
 */
@Mod.EventBusSubscriber(modid = "hexkagune", value = Dist.CLIENT)
public final class RinkakuBlockClient {
    /** One player's block as drawn: up or not, when it went up and came down, its last blow (and whether perfect), when it broke. */
    private static final class Guard {
        boolean up;
        double raisedAt = Double.NaN;
        double loweredAt = Double.NaN;
        double hitAt = Double.NaN;
        boolean perfect;
        double brokenAt = Double.NaN;
    }

    private static final Map<UUID, Guard> GUARDS = new HashMap<>();
    /** The right click being held for the block, ticks until the server is told again, and (after an attack) held but spent. */
    private static boolean holding;
    private static boolean spent;
    private static int resend;
    /** His own block's cooldown, as last heard (client time): no press is shown before it ends. */
    private static double coolUntil;

    private RinkakuBlockClient() {
    }

    public static void accept(S2CRinkakuBlock message) {
        double now = ControlClient.worldTime(0.0F);
        LocalPlayer self = Minecraft.m_91087_().f_91074_;
        boolean own = self != null && self.m_36316_().getId().equals(message.playerId());
        Guard guard = GUARDS.computeIfAbsent(message.playerId(), id -> new Guard());
        switch (message.event()) {
            case S2CRinkakuBlock.UP -> raise(guard, now);
            case S2CRinkakuBlock.DOWN -> lower(guard, now);
            case S2CRinkakuBlock.HIT -> {
                guard.hitAt = now;
                guard.perfect = false;
            }
            case S2CRinkakuBlock.PERFECT -> {
                guard.hitAt = now;
                guard.perfect = true;
            }
            case S2CRinkakuBlock.BREAK -> {
                guard.brokenAt = now;
                lower(guard, now);
                if (own) {
                    coolUntil = now + message.ticks();
                    holding = false;
                    spent = true;
                }
            }
            case S2CRinkakuBlock.REFUSED -> {
                lower(guard, now);
                if (own) {
                    coolUntil = now + message.ticks();
                }
            }
            default -> {
            }
        }
    }

    private static void raise(Guard guard, double now) {
        if (guard.up) {
            return; // already shown (his own press, shown at once)
        }
        guard.up = true;
        guard.raisedAt = now;
        guard.loweredAt = Double.NaN;
        guard.brokenAt = Double.NaN;
    }

    private static void lower(Guard guard, double now) {
        if (!guard.up) {
            return;
        }
        guard.up = false;
        guard.loweredAt = now;
    }

    // ---------------------------------------------------------------- his own right click

    /** True when {@code player} (this client's) can block now: a Rinkaku, the tails out and free, not the kakuja's forms. */
    private static boolean ready(LocalPlayer player) {
        if (PathClient.get(player) != KagunePath.RINKAKU) {
            return false;
        }
        KaguneClientState.View view = KaguneClientState.self();
        KaguneForm form = view.form();
        return view.unlocked() && view.active() && form != KaguneForm.FULL_KAKUJA && !RinkakuForms.autonomous(form);
    }

    @SubscribeEvent
    public static void useInput(InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isUseItem()) {
            return;
        }
        Minecraft minecraft = Minecraft.m_91087_();
        LocalPlayer player = minecraft.f_91074_;
        if (player == null || minecraft.f_91080_ != null || player.m_6144_() || !ready(player)) {
            return;
        }
        if (event.getHand() != InteractionHand.OFF_HAND || !player.m_21206_().m_41619_()) {
            return;
        }
        event.setCanceled(true);
        event.setSwingHand(false);
        if (holding || spent) {
            return;
        }
        holding = true;
        resend = RinkakuBlock.RESEND_TICKS;
        ControlNetwork.CHANNEL.sendToServer(new C2SUkakuAction(C2SUkakuAction.BLOCK, 2));
        double now = ControlClient.worldTime(0.0F);
        if (now >= coolUntil && KaguneClientState.self().mode() == TendrilMode.IDLE) {
            raise(GUARDS.computeIfAbsent(player.m_36316_().getId(), id -> new Guard()), now);
        }
    }

    @SubscribeEvent
    public static void clientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.m_91087_();
        LocalPlayer player = minecraft.f_91074_;
        boolean down = minecraft.f_91066_ != null && minecraft.f_91066_.f_92095_.m_90857_();
        if (!down) {
            spent = false;
        }
        if (!holding) {
            return;
        }
        boolean attack = minecraft.f_91066_ != null && minecraft.f_91066_.f_92096_.m_90857_();
        if (player == null || !down || attack || minecraft.f_91080_ != null || !ready(player)) {
            // Let go; or an attack (the block comes down for it: his free shot), held on but spent until it is let go.
            holding = false;
            spent = down;
            ControlNetwork.CHANNEL.sendToServer(new C2SUkakuAction(C2SUkakuAction.BLOCK, 0));
            if (player != null) {
                Guard guard = GUARDS.get(player.m_36316_().getId());
                if (guard != null) {
                    lower(guard, ControlClient.worldTime(0.0F));
                }
            }
            return;
        }
        if (--resend <= 0) {
            resend = RinkakuBlock.RESEND_TICKS;
            ControlNetwork.CHANNEL.sendToServer(new C2SUkakuAction(C2SUkakuAction.BLOCK, 1));
        }
    }

    // ---------------------------------------------------------------- as drawn

    /** How far {@code player}'s block is up at {@code time} (0..1; 0 if it never was, or long since came down). */
    public static float amount(AbstractClientPlayer player, double time) {
        Guard guard = GUARDS.get(player.m_36316_().getId());
        if (guard == null || Double.isNaN(guard.raisedAt)) {
            return 0.0F;
        }
        return RinkakuBlock.amount(time - guard.raisedAt, guard.up ? Double.NaN : time - guard.loweredAt);
    }

    /**
     * The body's jolt at {@code time}: how far a blow pressed the guard back ({@code -}) or a perfect block drove it out ({@code +}), and
     * how far a broken guard has thrown him (0..1): {jolt, thrown}.
     */
    public static float[] jolt(AbstractClientPlayer player, double time) {
        Guard guard = GUARDS.get(player.m_36316_().getId());
        if (guard == null) {
            return new float[2];
        }
        double recoil = Double.isNaN(guard.hitAt) ? 0.0 : RinkakuBlock.recoil(time - guard.hitAt, guard.perfect);
        double[] thrown = Double.isNaN(guard.brokenAt) ? new double[2] : RinkakuBlock.thrown(time - guard.brokenAt);
        return new float[] {(float)recoil, (float)(thrown[0] * thrown[1])};
    }

    /**
     * {@code path} (world, from the tail's root) of {@code player}'s tail {@code limb} at {@code time}, moved into the block as far as it
     * is up (or thrown back by a broken one); null when the block has nothing to do with it. {@code feet} is where the player stands and
     * {@code yaw} the body's facing (radians).
     */
    public static double[][] apply(double[][] path, int limb, AbstractClientPlayer player, KaguneClientState.View view, double[] feet,
                                   double yaw, double time) {
        if (GUARDS.isEmpty() || path.length < 3) {
            return null;
        }
        Guard guard = GUARDS.get(player.m_36316_().getId());
        if (guard == null || Double.isNaN(guard.raisedAt)) {
            return null;
        }
        if (view.mode() != TendrilMode.IDLE && guard.up) {
            return null; // mid-move: the tails are the move's (the server lets the block down)
        }
        double up = RinkakuBlock.amount(time - guard.raisedAt, guard.up ? Double.NaN : time - guard.loweredAt);
        double[] thrown = Double.isNaN(guard.brokenAt) ? new double[2] : RinkakuBlock.thrown(time - guard.brokenAt);
        double hold = Math.max(up, thrown[1]);
        if (hold <= 0.001) {
            if (!guard.up && time - guard.loweredAt > RinkakuBlock.THROW_TICKS + RinkakuBlock.THROWN_TICKS + 2.0) {
                GUARDS.remove(player.m_36316_().getId());
            }
            return null;
        }
        // Its place among the tails (centipede legs are no tails).
        KaguneForm form = view.form();
        int tails = 0;
        int index = -1;
        for (int i = 0; i < form.limbCount(); i++) {
            if (!form.limb(i).segmented()) {
                if (i == limb) {
                    index = tails;
                }
                tails++;
            }
        }
        if (index < 0) {
            return null;
        }
        double fanSide = form.limb(limb).fanSide();
        double height = player.m_20206_();
        Minecraft minecraft = Minecraft.m_91087_();
        boolean own = player == minecraft.f_91074_ && minecraft.f_91066_.m_92176_().m_90612_();
        double[][] pose = RinkakuBlock.pose(index, tails, fanSide, path.length, height, own);
        if (thrown[0] > 0.0) {
            pose = RinkakuBlock.blend(pose, RinkakuBlock.flung(index, fanSide, path.length, height), thrown[0]);
        }
        double recoil = Double.isNaN(guard.hitAt) ? 0.0 : RinkakuBlock.recoil(time - guard.hitAt, guard.perfect);
        if (recoil != 0.0) {
            pose = RinkakuBlock.push(pose, new double[] {0.0, 0.0, 1.0}, recoil);
        }
        // The owner's frame (x right, y up, z forward, from the feet) into the world.
        double fx = -Math.sin(yaw);
        double fz = Math.cos(yaw);
        double rx = -Math.cos(yaw);
        double rz = -Math.sin(yaw);
        double[][] world = new double[pose.length][];
        for (int i = 0; i < pose.length; i++) {
            double[] q = pose[i];
            world[i] = new double[] {feet[0] + q[0] * rx + q[2] * fx, feet[1] + q[1], feet[2] + q[0] * rz + q[2] * fz};
        }
        return RinkakuBlock.blend(path, world, hold);
    }

    @SubscribeEvent
    public static void loggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        GUARDS.clear();
        holding = false;
        spent = false;
        coolUntil = 0.0;
    }
}
