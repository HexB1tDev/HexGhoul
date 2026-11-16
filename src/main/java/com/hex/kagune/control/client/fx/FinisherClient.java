package com.hex.kagune.control.client.fx;

import com.hex.kagune.KaguneMod;
import com.hex.kagune.control.client.ControlClient;
import com.hex.kagune.control.client.animation.Carcass;
import com.hex.kagune.control.client.animation.KaguneBodyAnimation;
import com.hex.kagune.control.client.animation.RigPose;
import com.hex.kagune.control.client.render.BloodStains;
import com.hex.kagune.control.network.S2CUkakuFx;
import com.hex.kagune.control.ukaku.Finisher;
import com.hex.kagune.control.ukaku.Ragdoll;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

/**
 * 4.2.9: every client's side of the Steel Blade's finisher ({@code BladeFinisher}). Through the stun, a thin line of light draws itself round
 * the body's chest and arms from the middle of its front both ways at once ({@link Carcass#wound}; round the box of anything not built like a
 * player, {@link Finisher#line}). When it meets itself the body is no longer drawn (no death animation ever plays on it) and falls apart as a
 * ragdoll instead ({@link Carcass#cut}, {@link Ragdoll}: on the blocks as the game's collision has them), its own skin on each piece, what is
 * inside on each cut, blood thrown out of every cut and pooling where they lie; a Rinkaku's or a Bikaku's kagune shatters. The pieces lie there
 * {@link #LIFE} ticks (forty seconds), then fade away over {@link #FADE}.
 */
@Mod.EventBusSubscriber(modid = "hexkagune", value = Dist.CLIENT)
public final class FinisherClient {
    /** How long the pieces lie before they start to fade, and how long the fade takes (ticks). */
    public static final double LIFE = 800.0;
    public static final double FADE = 100.0;
    /** The most bodies lying cut up at once (the oldest go first). */
    static final int MOST = 12;
    /** How far off they are still simulated and drawn (blocks). */
    static final double FAR = 64.0;
    private static final RenderType WHITE = RenderType.m_110473_(Objects.requireNonNull(KaguneMod.id("textures/misc/ukaku_white.png")));
    private static final int FULL_BRIGHT = 0xF000F0;

    /** A body under the finisher: when its line began, how long it takes (ticks), the light and (a mob) the pose it was last drawn in. */
    private static final class Stunned {
        final double from;
        final int ticks;
        int light = FULL_BRIGHT;
        RigPose pose;

        Stunned(double from, int ticks) {
            this.from = from;
            this.ticks = Math.max(1, ticks);
        }
    }

    /** A body lying in pieces: the pieces, their bodies, its skin, the light it was in, when it was cut, which pieces have pooled. */
    private static final class Corpse {
        final List<Carcass.Piece> pieces;
        final List<Ragdoll.Body> bodies;
        final RenderType skin;
        final int light;
        final double born;
        final boolean[] pooled;

        Corpse(List<Carcass.Piece> pieces, List<Ragdoll.Body> bodies, ResourceLocation skin, int light, double born) {
            this.pieces = pieces;
            this.bodies = bodies;
            this.skin = RenderType.m_110473_(skin);
            this.light = light;
            this.born = born;
            this.pooled = new boolean[pieces.size()];
        }
    }

    private static final Map<Integer, Stunned> STUNNED = new HashMap<>();
    /** Bodies cut apart, not drawn any more, until when (world ticks). */
    private static final Map<Integer, Double> HIDDEN = new HashMap<>();
    private static final List<Corpse> CORPSES = new ArrayList<>();

    private FinisherClient() {
    }

    /** The server's word ({@code S2CUkakuFx.FINISH} or {@code SPLIT}). */
    static void accept(ClientLevel level, S2CUkakuFx message, Vec3 at) {
        double now = ControlClient.worldTime(0.0F);
        if (message.kind() == S2CUkakuFx.FINISH) {
            if (STUNNED.size() > 64) {
                STUNNED.clear();
            }
            STUNNED.put(message.id(), new Stunned(now, message.extra()));
            return;
        }
        Stunned stunned = STUNNED.remove(message.id());
        if (!(level.m_6815_(message.id()) instanceof LivingEntity body)) {
            return;
        }
        int light = stunned == null ? FULL_BRIGHT : stunned.light;
        Vec3 middle = body.m_20182_().m_82520_(0.0, Carcass.height(), 0.0);
        bleed(level, body, middle);
        if (message.extra() == 1 && body instanceof AbstractClientPlayer player) {
            KakujaFx.breakApart(player); // a Rinkaku's or a Bikaku's kagune shatters
        }
        Corpse corpse = corpse(body, light, now, message.vy(), stunned == null ? null : stunned.pose);
        if (corpse == null) {
            return; // not built like a player: it bleeds and dies as it always did
        }
        HIDDEN.put(message.id(), now + LIFE + FADE);
        if (CORPSES.size() >= MOST) {
            CORPSES.remove(0);
        }
        CORPSES.add(corpse);
    }

    /** {@code body} cut apart: its pieces (null if it is not built like a player). */
    static Corpse corpse(LivingEntity body, int light, double now, float yawDegrees, RigPose drawn) {
        Carcass.Layout layout;
        ResourceLocation skin;
        RigPose pose = drawn;
        double scale = 1.0;
        Finisher.Inside inside = Finisher.Inside.BODY;
        if (body instanceof AbstractClientPlayer player) {
            layout = "slim".equals(player.m_108564_()) ? Carcass.Layout.PLAYER_SLIM : Carcass.Layout.PLAYER_WIDE;
            skin = player.m_108560_();
            pose = KaguneBodyAnimation.body(player, now);
        } else {
            EntityRenderer<? super LivingEntity> renderer = Minecraft.m_91087_().m_91290_().m_114382_(body);
            if (!(renderer instanceof LivingEntityRenderer<?, ?> living) || !(living.m_7200_() instanceof HumanoidModel<?> model)) {
                return null;
            }
            skin = renderer.m_5478_(body);
            String path = skin.m_135815_();
            layout = model instanceof net.minecraft.client.model.PlayerModel<?> ? Carcass.Layout.PLAYER_WIDE
                : path.contains("skeleton") || path.contains("stray") ? Carcass.Layout.THIN : Carcass.Layout.HUMANOID;
            // The mod's own people (ghouls, the CCG, townsfolk) have insides; every other mob is meat.
            boolean person = body instanceof com.hex.kagune.control.npc.GhoulNpc || body instanceof com.hex.kagune.control.npc.CcgAgent
                || body instanceof com.hex.kagune.control.npc.HumanNpc;
            inside = person ? Finisher.Inside.BODY : Finisher.Inside.MEAT;
            // A mob is drawn at its model's own size (a player at the player renderer's 0.9375): its pieces match it.
            scale = 1.0 / com.hex.kagune.control.client.animation.BodyFrame.SCALE;
        }
        if (pose == null) {
            pose = new RigPose();
        }
        List<Carcass.Piece> pieces = Carcass.cut(pose, layout, scale, inside);
        double yaw = Math.toRadians(yawDegrees);
        double[] turn = Ragdoll.about(new double[] {0.0, 1.0, 0.0}, -yaw);
        double[] feet = {body.m_20185_(), body.m_20186_(), body.m_20189_()};
        List<Ragdoll.Body> bodies = new ArrayList<>();
        for (Carcass.Piece piece : pieces) {
            Ragdoll.Body b = new Ragdoll.Body(piece.boxes.toArray(new double[0][]), feet, turn);
            double[][] launch = Carcass.launch(piece.kind);
            double[] v = Ragdoll.rotate(turn, launch[0]);
            double[] w = Ragdoll.rotate(turn, launch[1]);
            System.arraycopy(v, 0, b.v, 0, 3);
            System.arraycopy(w, 0, b.w, 0, 3);
            bodies.add(b);
        }
        return new Corpse(pieces, bodies, skin, light, now);
    }

    /** The cut: blood thrown out of it every way, a pool spreading under it. */
    static void bleed(ClientLevel level, LivingEntity body, Vec3 middle) {
        if (!RcFx.near(middle, FAR)) {
            return;
        }
        for (int i = 0; i < 8; i++) {
            double a = Math.PI * 2.0 * i / 8.0 + Math.random() * 0.4;
            UkakuBlood.spurt(level, middle, new Vec3(Math.cos(a), 0.35 + Math.random() * 0.5, Math.sin(a)), 1.6F, body);
        }
        for (int i = 0; i < 40; i++) {
            RcFx.at(RcFx.MOTE, true, 18 + (int)(Math.random() * 12), middle).velocity(new Vec3((Math.random() - 0.5) * 0.5, 0.05 + Math.random() * 0.3,
                (Math.random() - 0.5) * 0.5)).colors(RcFx.CRIMSON, 0x00200004).size(0.14F, 0.05F).fall(0.04F).spawn();
        }
    }

    // ------------------------------------------------------------------ the line, and hiding the body once it is cut

    /** The line round a body under the finisher, drawn as it stands (its last light kept for its pieces). */
    @SubscribeEvent
    @SuppressWarnings("rawtypes")
    public static void drawn(RenderLivingEvent.Post event) {
        if (STUNNED.isEmpty()) {
            return;
        }
        LivingEntity body = event.getEntity();
        Stunned stunned = STUNNED.get(body.m_19879_());
        if (stunned == null) {
            return;
        }
        stunned.light = event.getPackedLight();
        double now = ControlClient.worldTime(event.getPartialTick());
        double amount = Finisher.round((now - stunned.from) * Finisher.STUN / stunned.ticks);
        if (amount <= 0.0) {
            return;
        }
        PoseStack pose = event.getPoseStack();
        pose.m_85836_();
        float yaw = body.f_20884_ + (((body.f_20883_ - body.f_20884_) % 360.0F + 540.0F) % 360.0F - 180.0F) * event.getPartialTick();
        pose.m_252781_(new Quaternionf().rotateAxis((float)Math.toRadians(-yaw), 0.0F, 1.0F, 0.0F));
        Matrix4f matrix = pose.m_85850_().m_252922_();
        Matrix3f normals = pose.m_85850_().m_252943_();
        VertexConsumer out = event.getMultiBufferSource().m_6299_(WHITE);
        com.hex.kagune.control.ukaku.UkakuModel.Sink sink = (a, b, c, d, n, ca, cb, cc, cd) -> {
            for (double[] p : new double[][] {a, b, c, d}) {
                vertex(out, matrix, normals, p, Carcass.GLOW, FULL_BRIGHT, 0.5F, 0.5F, n);
            }
        };
        if (body instanceof AbstractClientPlayer player) {
            RigPose rig = KaguneBodyAnimation.body(player, now);
            // Past the second layer as it is drawn: flat, or 3D Skin Layers' voxels.
            double layer = com.hex.kagune.control.client.render.PlayerLayers.skinLayers3d() ? Carcass.VOXEL_LAYER : Carcass.FLAT_LAYER;
            Carcass.wound(rig == null ? new RigPose() : rig, "slim".equals(player.m_108564_()) ? Carcass.Layout.PLAYER_SLIM : Carcass.Layout.PLAYER_WIDE,
                1.0, amount, true, layer, sink);
        } else if (event.getRenderer().m_7200_() instanceof HumanoidModel<?> model) {
            // A mob: the pose its own model was just set in (a zombie's arms held out), so the line and then its pieces are where it is.
            stunned.pose = posed(model);
            Carcass.wound(stunned.pose, Carcass.Layout.HUMANOID, 1.0 / com.hex.kagune.control.client.animation.BodyFrame.SCALE, amount, true,
                Carcass.FLAT_LAYER, sink);
        } else {
            double height = Math.min(Carcass.height(), body.m_20206_() * 0.6);
            double half = body.m_20205_() * 0.5 + 0.02;
            Finisher.line(sink, amount, half, half, height, Carcass.LINE / 16.0, Carcass.GLOW);
        }
        pose.m_85849_();
    }

    /** The pose a humanoid model was just drawn in, as a body pose held whole (its arms', legs' and head's own turns). */
    static RigPose posed(HumanoidModel<?> model) {
        RigPose p = new RigPose();
        p.armHold = 1.0F;
        p.legHold = 1.0F;
        p.headHold = 1.0F;
        p.rightArmX = model.f_102811_.f_104203_;
        p.rightArmY = model.f_102811_.f_104204_;
        p.rightArmZ = model.f_102811_.f_104205_;
        p.leftArmX = model.f_102812_.f_104203_;
        p.leftArmY = model.f_102812_.f_104204_;
        p.leftArmZ = model.f_102812_.f_104205_;
        p.rightLegX = model.f_102813_.f_104203_;
        p.rightLegY = model.f_102813_.f_104204_;
        p.rightLegZ = model.f_102813_.f_104205_;
        p.leftLegX = model.f_102814_.f_104203_;
        p.leftLegY = model.f_102814_.f_104204_;
        p.leftLegZ = model.f_102814_.f_104205_;
        p.headX = model.f_102808_.f_104203_;
        p.headY = model.f_102808_.f_104204_;
        p.headZ = model.f_102808_.f_104205_;
        return p;
    }

    /** A body cut apart is not drawn: its pieces are (no death animation ever plays on it). */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    @SuppressWarnings("rawtypes")
    public static void hideLiving(RenderLivingEvent.Pre event) {
        if (!HIDDEN.isEmpty() && HIDDEN.containsKey(event.getEntity().m_19879_())) {
            event.setCanceled(true);
        }
    }

    /** The same for a player (before its own layers are touched). */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void hidePlayer(RenderPlayerEvent.Pre event) {
        if (!HIDDEN.isEmpty() && HIDDEN.containsKey(event.getEntity().m_19879_())) {
            event.setCanceled(true);
        }
    }

    // ------------------------------------------------------------------ the pieces

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.m_91087_();
        ClientLevel level = minecraft.f_91073_;
        if (level == null || minecraft.f_91074_ == null) {
            STUNNED.clear();
            HIDDEN.clear();
            CORPSES.clear();
            return;
        }
        double now = ControlClient.worldTime(0.0F);
        // A body that lives again (respawned under the same id) is drawn again.
        HIDDEN.entrySet().removeIf(e -> now > e.getValue() || level.m_6815_(e.getKey()) instanceof LivingEntity living && living.m_6084_()
            && living.m_21223_() > 0.0F && now > e.getValue() - LIFE - FADE + 40.0);
        STUNNED.entrySet().removeIf(e -> now - e.getValue().from > e.getValue().ticks + 100.0);
        Ragdoll.Terrain terrain = (a, b) -> {
            BlockHitResult hit = level.m_45547_(new ClipContext(new Vec3(a[0], a[1], a[2]), new Vec3(b[0], b[1], b[2]), ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, minecraft.f_91074_));
            if (hit == null || hit.m_6662_() != HitResult.Type.BLOCK) {
                return null;
            }
            Vec3 at = hit.m_82450_();
            return new double[] {at.f_82479_, at.f_82480_, at.f_82481_};
        };
        Vec3 eye = minecraft.f_91074_.m_20182_();
        for (Iterator<Corpse> it = CORPSES.iterator(); it.hasNext(); ) {
            Corpse corpse = it.next();
            double age = now - corpse.born;
            if (age > LIFE + FADE || age < -40.0) {
                it.remove();
                continue;
            }
            Ragdoll.Body first = corpse.bodies.get(0);
            if (far(eye, first.pos)) {
                continue;
            }
            boolean awake = false;
            for (Ragdoll.Body b : corpse.bodies) {
                awake |= !b.asleep;
            }
            if (awake) {
                Ragdoll.tick(corpse.bodies, terrain);
            }
            for (int i = 0; i < corpse.pieces.size(); i++) {
                Carcass.Piece piece = corpse.pieces.get(i);
                Ragdoll.Body b = corpse.bodies.get(i);
                double[] cut = cutAt(piece, b);
                if (cut == null) {
                    continue;
                }
                Vec3 at = new Vec3(cut[0], cut[1], cut[2]);
                // Each cut keeps bleeding as it falls, and where it comes to rest the blood pools.
                if (age < 30.0 && Math.random() < 0.6) {
                    RcFx.at(RcFx.MOTE, true, 16, at).velocity(new Vec3((Math.random() - 0.5) * 0.08, 0.02, (Math.random() - 0.5) * 0.08))
                        .colors(RcFx.CRIMSON, 0x00200004).size(0.1F, 0.04F).fall(0.04F).spawn();
                }
                if (b.asleep && !corpse.pooled[i]) {
                    corpse.pooled[i] = true;
                    boolean big = piece.kind == Carcass.Kind.TOP || piece.kind == Carcass.Kind.BOTTOM;
                    BloodStains.pool(level, new Vec3(b.pos[0], Math.floor(b.pos[1] - 0.05) + groundIn(level, b.pos), b.pos[2]), big ? 0.9 : 0.4,
                        (int)(LIFE + FADE), big ? 60 : 30);
                }
            }
        }
    }

    /** The height of the ground's top within the block under {@code p} (0..1; 0 if it can't be found). */
    static double groundIn(ClientLevel level, double[] p) {
        BlockHitResult hit = level.m_45547_(new ClipContext(new Vec3(p[0], p[1] + 0.2, p[2]), new Vec3(p[0], p[1] - 1.5, p[2]), ClipContext.Block.COLLIDER,
            ClipContext.Fluid.NONE, Minecraft.m_91087_().f_91074_));
        if (hit == null || hit.m_6662_() != HitResult.Type.BLOCK) {
            return 0.0;
        }
        double y = hit.m_82450_().f_82480_;
        return y - Math.floor(p[1] - 0.05);
    }

    /** Where a piece's cuts are now (the middle of what is inside it, the world), or null if it has none. */
    static double[] cutAt(Carcass.Piece piece, Ragdoll.Body body) {
        if (piece.caps.isEmpty()) {
            return null;
        }
        double[] sum = new double[3];
        int step = Math.max(1, piece.caps.size() / 24);
        int counted = 0;
        for (int i = 0; i < piece.caps.size(); i += step) {
            double[] p = piece.caps.get(i).p()[0];
            for (int k = 0; k < 3; k++) {
                sum[k] += p[k];
            }
            counted++;
        }
        for (int k = 0; k < 3; k++) {
            sum[k] = sum[k] / counted - body.offset[k];
        }
        return body.toWorld(sum);
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES || CORPSES.isEmpty()) {
            return;
        }
        Minecraft minecraft = Minecraft.m_91087_();
        Vec3 camera = minecraft.f_91063_.m_109153_().m_90583_();
        double now = ControlClient.worldTime(event.getPartialTick());
        PoseStack stack = event.getPoseStack();
        Matrix4f matrix = stack.m_85850_().m_252922_();
        Matrix3f normals = stack.m_85850_().m_252943_();
        MultiBufferSource.BufferSource buffers = minecraft.m_91269_().m_110104_();
        for (Corpse corpse : CORPSES) {
            Ragdoll.Body first = corpse.bodies.get(0);
            if (far(camera, first.pos)) {
                continue;
            }
            double age = now - corpse.born;
            int alpha = (int)Math.round(255.0 * Math.max(0.0, Math.min(1.0, 1.0 - (age - LIFE) / FADE)));
            if (alpha <= 3) {
                continue;
            }
            VertexConsumer skin = buffers.m_6299_(corpse.skin);
            for (int i = 0; i < corpse.pieces.size(); i++) {
                Carcass.Piece piece = corpse.pieces.get(i);
                Ragdoll.Body body = corpse.bodies.get(i);
                for (Carcass.Face face : piece.faces) {
                    double[] n = Ragdoll.rotate(body.q, face.n());
                    for (int k = 0; k < 4; k++) {
                        double[] p = place(body, face.p()[k], camera);
                        vertex(skin, matrix, normals, p, alpha << 24 | 0xFFFFFF, corpse.light, (float)face.uv()[k][0], (float)face.uv()[k][1], n);
                    }
                }
            }
            buffers.m_109912_(corpse.skin);
            VertexConsumer caps = buffers.m_6299_(WHITE);
            for (int i = 0; i < corpse.pieces.size(); i++) {
                Carcass.Piece piece = corpse.pieces.get(i);
                Ragdoll.Body body = corpse.bodies.get(i);
                for (Carcass.Cap cap : piece.caps) {
                    double[] n = Ragdoll.rotate(body.q, cap.n());
                    for (int k = 0; k < 4; k++) {
                        vertex(caps, matrix, normals, place(body, cap.p()[k], camera), alpha << 24 | cap.argb()[k] & 0xFFFFFF, corpse.light, 0.5F, 0.5F, n);
                    }
                }
            }
            buffers.m_109912_(WHITE);
        }
    }

    static boolean far(Vec3 from, double[] p) {
        double dx = p[0] - from.f_82479_;
        double dy = p[1] - from.f_82480_;
        double dz = p[2] - from.f_82481_;
        return dx * dx + dy * dy + dz * dz > FAR * FAR;
    }

    /** A point of a piece (the carcass's space, about its origin) where its body has it now, from the camera. */
    static double[] place(Ragdoll.Body body, double[] p, Vec3 camera) {
        double[] w = body.toWorld(new double[] {p[0] - body.offset[0], p[1] - body.offset[1], p[2] - body.offset[2]});
        return new double[] {w[0] - camera.f_82479_, w[1] - camera.f_82480_, w[2] - camera.f_82481_};
    }

    private static void vertex(VertexConsumer out, Matrix4f matrix, Matrix3f normals, double[] p, int argb, int light, float u, float v, double[] n) {
        out.m_252986_(matrix, (float)p[0], (float)p[1], (float)p[2])
            .m_6122_((argb >> 16) & 0xFF, (argb >> 8) & 0xFF, argb & 0xFF, argb >>> 24)
            .m_7421_(u, v)
            .m_86008_(OverlayTexture.f_118083_)
            .m_85969_(light)
            .m_252939_(normals, (float)n[0], (float)n[1], (float)n[2])
            .m_5752_();
    }
}
