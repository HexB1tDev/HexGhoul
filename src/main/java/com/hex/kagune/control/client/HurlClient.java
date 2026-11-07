package com.hex.kagune.control.client;

import com.hex.kagune.client.KaguneClientState;
import com.hex.kagune.control.KagunePath;
import com.hex.kagune.control.RinkakuForms;
import com.hex.kagune.control.RinkakuHurl;
import com.hex.kagune.control.RinkakuRoot;
import com.hex.kagune.control.ThyDeath;
import com.hex.kagune.control.network.C2SUkakuAction;
import com.hex.kagune.control.network.ControlNetwork;
import com.hex.kagune.control.network.S2CHurl;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * 3.1.6: Rinkaku's Rubble Hurl on the client ({@link RinkakuHurl}): N held with no tail holding anyone is told to the server (as the
 * Impale's H: pressed, pinged, let go; with someone held N stays the throw, the server's to say), and every hurl it is told of
 * ({@link S2CHurl}) is drawn: the chunk as the torn block's own model, a big cube with smaller lumps stuck round it
 * ({@link RinkakuHurl#pieces}), eased between the server's ticks, turning slowly in the tails' grip and tumbling in flight; and the tails
 * gripping it reach to it and whip after it as it is thrown ({@link #apply}, from {@code StrikePaths}).
 */
@Mod.EventBusSubscriber(modid = "hexkagune", value = Dist.CLIENT)
public final class HurlClient {
    private HurlClient() {
    }

    /** One piece as drawn: where it was and where it is going (eased over a tick), how big, and when it was last heard of. */
    private static final class Piece {
        double[] from;
        double[] to;
        double heardAt;
        double size;
    }

    /** One player's hurl as drawn. */
    private static final class Hurl {
        int limbA = -1;
        int limbB = -1;
        String item = "";
        ItemStack stack;
        double startedAt;
        double thrownAt = Double.NaN;
        double heardAt;
        final Map<Integer, Piece> pieces = new HashMap<>();
        /** Where the first piece was as it was thrown (the tails whip after it from here). */
        double[] thrownFrom;
    }

    private static final Map<Integer, Hurl> HURLS = new HashMap<>();
    /** Ticks it is drawn on without hearing from the server before it is dropped (the server tells every tick). */
    private static final double STALE = 12.0;
    private static boolean held;
    private static boolean sent;
    private static int pinged;

    public static void accept(S2CHurl message) {
        double now = ControlClient.worldTime(0.0F);
        if (message.phase() == S2CHurl.GONE) {
            HURLS.remove(message.player());
            return;
        }
        Hurl hurl = HURLS.get(message.player());
        if (hurl == null) {
            hurl = new Hurl();
            hurl.startedAt = now;
            HURLS.put(message.player(), hurl);
            if (HURLS.size() > 64) {
                HURLS.clear();
                HURLS.put(message.player(), hurl);
            }
        }
        hurl.limbA = message.limbA();
        hurl.limbB = message.limbB();
        hurl.heardAt = now;
        if (!message.item().equals(hurl.item)) {
            hurl.item = message.item();
            hurl.stack = null;
        }
        double[] at = {message.x(), message.y(), message.z()};
        if (message.phase() == S2CHurl.BURST) {
            hurl.pieces.remove(message.piece());
            return;
        }
        if (message.phase() == S2CHurl.FLY && Double.isNaN(hurl.thrownAt)) {
            hurl.thrownAt = now;
            Piece first = hurl.pieces.get(0);
            hurl.thrownFrom = first == null ? at.clone() : shown(first, now);
        }
        Piece piece = hurl.pieces.get(message.piece());
        if (piece == null) {
            piece = new Piece();
            piece.from = at.clone();
            piece.to = at;
            hurl.pieces.put(message.piece(), piece);
        } else {
            piece.from = shown(piece, now);
            piece.to = at;
        }
        piece.heardAt = now;
        piece.size = message.size();
    }

    /** Where {@code piece} is drawn at {@code time}: eased from where it was to where it was last told over a tick. */
    private static double[] shown(Piece piece, double time) {
        double f = Math.max(0.0, Math.min(1.0, time - piece.heardAt));
        return new double[] {piece.from[0] + (piece.to[0] - piece.from[0]) * f, piece.from[1] + (piece.to[1] - piece.from[1]) * f,
            piece.from[2] + (piece.to[2] - piece.from[2]) * f};
    }

    // ---------------------------------------------------------------- his own key

    /** True when this client's player could hurl: a Rinkaku with the tails out, not the first kakuja (whether N is the throw is the server's to say). */
    private static boolean ready(LocalPlayer player) {
        if (PathClient.get(player) != KagunePath.RINKAKU) {
            return false;
        }
        KaguneClientState.View view = KaguneClientState.self();
        return view.unlocked() && view.active() && !RinkakuForms.autonomous(view.form());
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
                if (sent && minecraft.f_91074_ != null) {
                    ControlNetwork.CHANNEL.sendToServer(new C2SUkakuAction(C2SUkakuAction.HURL, 0));
                }
                sent = false;
            }
            if (minecraft.f_91073_ == null) {
                HURLS.clear();
            }
            return;
        }
        boolean down = com.hex.kagune.client.KaguneKeys.THROW.m_90857_();
        if (down && !held) {
            held = true;
            pinged = 0;
            // The original's own key handler sends the throw as well; with someone held the server throws them and ignores this.
            sent = ready(minecraft.f_91074_);
            if (sent) {
                ControlNetwork.CHANNEL.sendToServer(new C2SUkakuAction(C2SUkakuAction.HURL, 2));
            }
        } else if (down && sent && ++pinged % 4 == 0) {
            ControlNetwork.CHANNEL.sendToServer(new C2SUkakuAction(C2SUkakuAction.HURL, 1));
        } else if (!down && held) {
            held = false;
            if (sent) {
                ControlNetwork.CHANNEL.sendToServer(new C2SUkakuAction(C2SUkakuAction.HURL, 0));
            }
            sent = false;
        }
    }

    // ---------------------------------------------------------------- the chunk

    /** The torn block's own item to draw it with (cobblestone if it can't be found). */
    private static ItemStack stack(Hurl hurl) {
        if (hurl.stack == null) {
            String id = hurl.item == null || hurl.item.isEmpty() ? "minecraft:cobblestone" : hurl.item;
            int colon = id.indexOf(':');
            ResourceLocation key = ResourceLocation.m_214293_(colon < 0 ? "minecraft" : id.substring(0, colon), id.substring(colon + 1));
            Item item = key == null ? null : ForgeRegistries.ITEMS.getValue(key);
            ItemStack stack = item == null ? ItemStack.f_41583_ : new ItemStack(item, 1);
            if (stack.m_41619_()) {
                Item cobble = ForgeRegistries.ITEMS.getValue(ResourceLocation.m_214293_("minecraft", "cobblestone"));
                stack = cobble == null ? ItemStack.f_41583_ : new ItemStack(cobble, 1);
            }
            hurl.stack = stack;
        }
        return hurl.stack;
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES || HURLS.isEmpty()) {
            return;
        }
        Minecraft minecraft = Minecraft.m_91087_();
        ClientLevel level = minecraft.f_91073_;
        if (level == null) {
            return;
        }
        try {
            double time = ControlClient.worldTime(event.getPartialTick());
            Vec3 camera = minecraft.f_91063_.m_109153_().m_90583_();
            PoseStack pose = event.getPoseStack();
            MultiBufferSource.BufferSource buffers = minecraft.m_91269_().m_110104_();
            HURLS.values().removeIf(hurl -> time - hurl.heardAt > STALE);
            for (Map.Entry<Integer, Hurl> entry : HURLS.entrySet()) {
                Hurl hurl = entry.getValue();
                ItemStack stack = stack(hurl);
                if (stack.m_41619_()) {
                    continue;
                }
                for (Map.Entry<Integer, Piece> each : hurl.pieces.entrySet()) {
                    Piece piece = each.getValue();
                    double[] at = shown(piece, time);
                    int seed = entry.getKey() * 31 + each.getKey() * 7919;
                    int light = LevelRenderer.m_109541_(level, BlockPos.m_274561_(at[0], at[1], at[2]));
                    // In the grip it turns slowly and sways; thrown, it tumbles end over end.
                    boolean flying = !Double.isNaN(hurl.thrownAt);
                    double turn = flying ? (time - hurl.thrownAt) * 21.0 : Math.sin((time - hurl.startedAt) * 0.13) * 9.0;
                    double yaw = (seed % 360) + (flying ? 0.0 : (time - hurl.startedAt) * 1.5);
                    pose.m_85836_();
                    pose.m_85837_(at[0] - camera.f_82479_, at[1] - camera.f_82480_, at[2] - camera.f_82481_);
                    pose.m_252781_(new org.joml.Quaternionf().rotateAxis((float)Math.toRadians(yaw), 0, 1, 0)
                        .rotateAxis((float)Math.toRadians(turn), 1, 0, 0).rotateAxis((float)Math.toRadians(turn * 0.37), 0, 0, 1));
                    for (double[] lump : RinkakuHurl.pieces(seed, RinkakuHurl.lumps(piece.size))) {
                        pose.m_85836_();
                        pose.m_85837_(lump[0] * piece.size, lump[1] * piece.size, lump[2] * piece.size);
                        pose.m_252781_(new org.joml.Quaternionf().rotateAxis((float)Math.toRadians(lump[4]), 0, 1, 0)
                            .rotateAxis((float)Math.toRadians(lump[4] * 0.5), 1, 0, 0));
                        float scale = (float)(lump[3] * piece.size);
                        pose.m_85841_(scale, scale, scale);
                        minecraft.m_91291_().m_269128_(stack, net.minecraft.world.item.ItemDisplayContext.NONE, light, OverlayTexture.f_118083_, pose,
                            buffers, level, seed);
                        pose.m_85849_();
                    }
                    pose.m_85849_();
                }
            }
        } catch (RuntimeException failure) {
            com.hex.kagune.control.Safety.caught(failure, "the Rubble Hurl's chunk");
            HURLS.clear();
        }
    }

    // ---------------------------------------------------------------- the tails

    /** How far from his shoulder the tails follow a thrown piece as they whip after it (blocks). */
    private static final double WHIP_REACH = 4.5;

    /**
     * Tail {@code limb} of {@code owner}'s {@code path} (world, root first) gripping its chunk or whipping after it as it is thrown, or null
     * when it isn't: {@code fanSide} its layout's, {@code feet} his feet, {@code yaw} his body yaw in radians, {@code time} the world's
     * ticks. 3.1.7 (the owner: "the tails are going through the body"): it goes round him ({@link RinkakuHurl#grip}: back out of him, up
     * behind its own shoulder, then on to the chunk), never straight through him from his back.
     */
    public static double[][] apply(double[][] path, int limb, Entity owner, double fanSide, double[] feet, double yaw, double time) {
        if (HURLS.isEmpty() || path == null || path.length < 2 || owner == null) {
            return null;
        }
        Hurl hurl = HURLS.get(owner.m_19879_());
        if (hurl == null || limb != hurl.limbA && limb != hurl.limbB) {
            return null;
        }
        double side = RinkakuRoot.side(limb, fanSide);
        double rightX = -Math.cos(yaw);
        double rightZ = -Math.sin(yaw);
        double age;
        double[] target;
        double size;
        if (Double.isNaN(hurl.thrownAt)) {
            Piece piece = hurl.pieces.get(0);
            if (piece == null) {
                return null;
            }
            target = shown(piece, time);
            size = piece.size;
            // Up to their full reach twice as fast as a lunge (they are in the ground before it rises), and held there (never let back).
            age = Math.min(ThyDeath.HIT_AT, (time - hurl.startedAt) * 2.0);
        } else {
            age = ThyDeath.HIT_AT + (time - hurl.thrownAt);
            if (age >= ThyDeath.STRIKE) {
                return null;
            }
            Piece piece = hurl.pieces.get(0);
            double[] after = piece != null ? shown(piece, time) : hurl.thrownFrom;
            if (after == null) {
                return null;
            }
            // Whipping after it: toward it, but never further than they reach.
            double[] root = path[0];
            double dx = after[0] - root[0];
            double dy = after[1] - root[1];
            double dz = after[2] - root[2];
            double d = Math.sqrt(dx * dx + dy * dy + dz * dz);
            double k = d > WHIP_REACH ? WHIP_REACH / d : 1.0;
            target = new double[] {root[0] + dx * k, root[1] + dy * k, root[2] + dz * k};
            size = piece != null ? piece.size : 1.0;
        }
        if (!(age > 0.0)) {
            return null;
        }
        // Each tail grips its own side of it, reaching round him to it: in by a lunge's reach, then (thrown) back out the same way.
        double s = side >= 0.0 ? 1.0 : -1.0;
        double[] grip = {target[0] + rightX * s * size * 0.3, target[1], target[2] + rightZ * s * size * 0.3};
        Minecraft minecraft = Minecraft.m_91087_();
        boolean own = owner == minecraft.f_91074_ && minecraft.f_91066_.m_92176_().m_90612_();
        double[][] round = RinkakuHurl.grip(path[0], feet, owner.m_20206_(), side, rightX, rightZ, -Math.sin(yaw), Math.cos(yaw), grip,
            path.length, own);
        return com.hex.kagune.control.RinkakuImpale.blend(path, round, ThyDeath.reach(age));
    }
}
