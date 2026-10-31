package com.hex.kagune.control.client.render;

import com.hex.kagune.control.ControlEntities;
import com.hex.kagune.control.client.EtoClient;
import com.hex.kagune.control.client.PaleClient;
import com.hex.kagune.control.client.animation.BodyFrame;
import com.hex.kagune.control.client.animation.EtoFight;
import com.hex.kagune.control.client.animation.EtoPose;
import com.hex.kagune.control.client.animation.RigPose;
import com.hex.kagune.control.npc.EtoGesture;
import com.hex.kagune.control.npc.EtoHand;
import com.hex.kagune.control.npc.EtoNpc;
import com.hex.kagune.control.npc.EtoRules;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.Objects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

/**
 * 1.3.0: Eto as vanilla draws a player, on the slim arms in her own skin ({@code tools/eto_textures.py}), her figure ({@link EtoModel}:
 * wider hips, and a bust drawn on her torso here), posed as she is ({@link EtoPose}: her gestures, her moves, her flips). Over the skin:
 * her kakugan (the right eye turned, veins and all) fading in at night and in a fight with its iris glowing, her eyes shut "- -" when she
 * blinks or leans in for a kiss, her cheeks pink when she blushes, and the anger mark over her head when she was left hanging.
 */
public final class EtoRenderer extends HumanoidMobRenderer<EtoNpc, PlayerModel<EtoNpc>> {
    static final ResourceLocation SKIN = texture("eto");
    static final ResourceLocation KAKUGAN = texture("eto_kakugan");
    static final ResourceLocation GLOW = texture("eto_kakugan_glow");
    static final ResourceLocation CLOSED = texture("eto_closed");
    static final ResourceLocation BLUSH = texture("eto_blush");
    static final ResourceLocation ANGRY = texture("eto_angry");

    private static ResourceLocation texture(String name) {
        return Objects.requireNonNull(ResourceLocation.m_214293_("hexkagune", "textures/entity/eto/" + name + ".png"));
    }

    public EtoRenderer(EntityRendererProvider.Context context) {
        super(context, new EtoModel(context.m_174023_(ModelLayers.f_171166_)), 0.5F * EtoRules.SIZE);
        this.m_115326_(new Face(this));
    }

    /** Her pose this frame: her gesture, with her fight's move (1.3.4: a Pale player's, by its own animator) over it (null: none). */
    static RigPose pose(EtoNpc npc, EtoClient.View view, double clock, float partial) {
        if (view == null) {
            return null;
        }
        return EtoFight.pose(npc, view.fight(), EtoPose.gesture(view.gesture(), view.gestureAge(clock)), clock, partial);
    }

    @Override
    public void m_7392_(EtoNpc npc, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        EtoClient.View view = EtoClient.get(npc.m_19879_());
        double now = PaleClient.clock(partial);
        if (com.hex.kagune.control.client.OwlClient.creature(npc, now)) {
            // 1.3.8: the Owl's creature is drawn in her place (PaleFleshRenderer), carried over from her body as posed this frame: posed,
            // never drawn.
            RigPose posed = pose(npc, view, now, partial);
            DRAWN.put(npc.m_19879_(), posed == null ? new RigPose() : new RigPose().add(posed, 1.0F));
            DRAWN_AT.put(npc.m_19879_(), now);
            return;
        }
        boolean glued = glue(npc, view, partial, pose);
        try {
            this.draw(npc, yaw, partial, pose, buffers, light, view);
        } finally {
            if (glued) {
                pose.m_85849_();
            }
        }
    }

    /**
     * 1.3.4: hand in hand, she is drawn glued at the side of the one whose hand she holds (where EtoHand puts her, as they are drawn
     * this frame), facing the way they face: they walk as one, whatever lag her own position has. True if she was moved (a pose pushed).
     */
    static boolean glue(EtoNpc npc, EtoClient.View view, float partial, PoseStack pose) {
        if (view == null || view.gesture() != EtoGesture.HAND || view.with() == 0
            || !(npc.m_9236_().m_6815_(view.with()) instanceof net.minecraft.world.entity.LivingEntity holder)) {
            return false;
        }
        float turn = ((holder.f_20883_ - holder.f_20884_) % 360.0F + 540.0F) % 360.0F - 180.0F;
        float bodyYaw = holder.f_20884_ + turn * partial;
        double[] off = EtoHand.offset(bodyYaw, com.hex.kagune.control.Genders.female(holder));
        double hx = holder.f_19790_ + (holder.m_20185_() - holder.f_19790_) * partial;
        double hy = holder.f_19791_ + (holder.m_20186_() - holder.f_19791_) * partial;
        double hz = holder.f_19792_ + (holder.m_20189_() - holder.f_19792_) * partial;
        double ex = npc.f_19790_ + (npc.m_20185_() - npc.f_19790_) * partial;
        double ey = npc.f_19791_ + (npc.m_20186_() - npc.f_19791_) * partial;
        double ez = npc.f_19792_ + (npc.m_20189_() - npc.f_19792_) * partial;
        pose.m_85836_();
        pose.m_85837_(hx + off[0] - ex, hy - ey, hz + off[1] - ez);
        npc.f_20883_ = bodyYaw;
        npc.f_20884_ = bodyYaw;
        return true;
    }

    /**
     * 1.3.6: what draws the Owl DNA kit on a body ({@link PaleFleshRenderer}'s flesh, tentacles...): who, her move, her body as it was
     * drawn this frame (null: not drawn, the move alone poses it), and her size against a player's.
     */
    interface Kit {
        boolean draw(net.minecraft.world.entity.LivingEntity who, PaleClient.PaleView view, RigPose drawn, double scale);
    }

    /** 1.3.6: each Eto's body as it was last drawn, and on what clock. */
    private static final java.util.Map<Integer, RigPose> DRAWN = new java.util.HashMap<>();
    private static final java.util.Map<Integer, Double> DRAWN_AT = new java.util.HashMap<>();

    /** 1.3.6: her body as drawn on {@code clock} (this frame), or null if it wasn't. */
    static RigPose drawn(int id, double clock) {
        Double at = DRAWN_AT.get(id);
        return at != null && Math.abs(at - clock) < 1.0E-3 ? DRAWN.get(id) : null;
    }

    /** 1.3.6: {@code kit} run for every Eto in sight within {@code range} of this client's player; true if anything was drawn. */
    static boolean kit(Kit kit, net.minecraft.client.multiplayer.ClientLevel level, net.minecraft.client.Minecraft minecraft, double clock, double range) {
        DRAWN.keySet().removeIf(id -> level.m_6815_(id) == null);
        DRAWN_AT.keySet().removeIf(id -> level.m_6815_(id) == null);
        boolean drew = false;
        for (int id : EtoClient.ids()) {
            EtoClient.View view = EtoClient.get(id);
            if (view == null || !(level.m_6815_(id) instanceof EtoNpc npc) || !npc.m_6084_() || npc.m_20145_()
                || npc.m_20280_(minecraft.f_91074_) > range * range) {
                continue;
            }
            drew |= kit.draw(npc, view.fight(), drawn(id, clock), EtoRules.SIZE);
        }
        return drew;
    }

    private void draw(EtoNpc npc, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light, EtoClient.View view) {
        double clock = PaleClient.clock(partial);
        RigPose posed = pose(npc, view, clock, partial);
        DRAWN.put(npc.m_19879_(), posed == null ? null : new RigPose().add(posed, 1.0F));
        DRAWN_AT.put(npc.m_19879_(), clock);
        EtoModel.pose = posed;
        boolean turned = posed != null && (Math.abs(posed.flip) > 1.0E-3F || Math.abs(posed.roll) > 1.0E-3F || Math.abs(posed.spin) > 1.0E-3F
            || Math.abs(posed.drop) > 1.0E-3F || Math.abs(posed.lunge) > 1.0E-3F);
        // 1.3.1: a little smaller than a player (her box is too, EtoRules.SIZE).
        pose.m_85836_();
        pose.m_85841_(EtoRules.SIZE, EtoRules.SIZE, EtoRules.SIZE);
        try {
            if (turned) {
                float delta = ((npc.f_20883_ - npc.f_20884_) % 360.0F + 540.0F) % 360.0F - 180.0F;
                double body = Math.toRadians(npc.f_20884_ + delta * partial);
                float rightX = (float)-Math.cos(body);
                float rightZ = (float)-Math.sin(body);
                pose.m_85836_();
                // 1.3.4: a lunge carries the whole of her forward (model pixels), as a Pale player's.
                pose.m_85837_(-Math.sin(body) * posed.lunge / 16.0, 0.0, Math.cos(body) * posed.lunge / 16.0);
                pose.m_85837_(0.0, -posed.drop / 16.0 + BodyFrame.TURN_PIVOT, 0.0);
                pose.m_252781_(new Quaternionf().rotateAxis(-posed.spin, 0.0F, 1.0F, 0.0F));
                pose.m_252781_(new Quaternionf().rotateAxis(posed.flip, rightX, 0.0F, rightZ));
                pose.m_252781_(new Quaternionf().rotateAxis(-posed.roll, rightZ, 0.0F, -rightX));
                pose.m_85837_(0.0, -BodyFrame.TURN_PIVOT, 0.0);
            }
            super.m_7392_(npc, yaw, partial, pose, buffers, light);
        } finally {
            if (turned) {
                pose.m_85849_();
            }
            pose.m_85849_();
            EtoModel.pose = null;
        }
        if (view != null && view.gesture() == EtoGesture.ANGRY && !npc.m_20145_()) {
            angry(npc, view.gestureAge(clock), pose, buffers);
        }
        // 1.3.2: taken, she says so over her head: her name, and under it whose she is.
        if (view != null && !view.tag().isEmpty() && !npc.m_20145_()) {
            this.m_7649_(npc, Component.m_237113_("\u00a7d\u2764 \u00a7f" + view.tag()), pose, buffers, light);
            pose.m_85836_();
            pose.m_85837_(0.0, 0.26, 0.0);
            this.m_7649_(npc, Component.m_237113_("\u00a7d" + EtoRules.NAME), pose, buffers, light);
            pose.m_85849_();
        }
    }

    @Override
    public ResourceLocation m_5478_(EtoNpc npc) {
        return SKIN;
    }

    /** The anger mark over her head, to her right, popping in and throbbing, turned to the camera. */
    private static void angry(EtoNpc npc, double age, PoseStack pose, MultiBufferSource buffers) {
        int ticks = EtoGesture.ANGRY.ticks;
        if (age >= ticks) {
            return;
        }
        float pop = age < 4.0 ? (float)(age / 4.0) * 1.25F : age < 7.0 ? 1.25F - 0.25F * (float)((age - 4.0) / 3.0) : 1.0F;
        float fade = age > ticks - 8 ? (float)((ticks - age) / 8.0) : 1.0F;
        float throb = 1.0F + 0.08F * (float)Math.sin(age * 0.9);
        float size = 0.36F * pop * throb;
        Vec3 camera = Minecraft.m_91087_().f_91063_.m_109153_().m_90583_();
        double dx = camera.f_82479_ - npc.m_20185_();
        double dz = camera.f_82481_ - npc.m_20189_();
        float toward = (float)Math.atan2(dx, dz);
        pose.m_85836_();
        pose.m_85837_(0.0, npc.m_20206_() + 0.32, 0.0);
        pose.m_252781_(new Quaternionf().rotateAxis(toward, 0.0F, 1.0F, 0.0F));
        pose.m_85837_(-0.28, 0.0, 0.0);
        Matrix4f matrix = pose.m_85850_().m_252922_();
        Matrix3f normals = pose.m_85850_().m_252943_();
        VertexConsumer consumer = buffers.m_6299_(RenderType.m_110473_(ANGRY));
        int alpha = Math.round(255 * Math.max(0.0F, Math.min(1.0F, fade)));
        float[][] corners = {{-size, -size, 0.0F, 1.0F}, {size, -size, 1.0F, 1.0F}, {size, size, 1.0F, 0.0F}, {-size, size, 0.0F, 0.0F}};
        for (float[] c : corners) {
            consumer.m_252986_(matrix, c[0], c[1], 0.0F).m_6122_(255, 255, 255, alpha).m_7421_(c[2], c[3]).m_86008_(OverlayTexture.f_118083_)
                .m_85969_(0xF000F0).m_252939_(normals, 0.0F, 0.0F, 1.0F).m_5752_();
        }
        pose.m_85849_();
    }

    /**
     * Her face and figure over the skin: the bust on her torso, the kakugan (the eye open by its fade; its iris glowing), her eyes shut
     * (a blink, or a gesture that shuts them), her blush.
     */
    static final class Face extends RenderLayer<EtoNpc, PlayerModel<EtoNpc>> {
        Face(RenderLayerParent<EtoNpc, PlayerModel<EtoNpc>> parent) {
            super(parent);
        }

        @Override
        public void m_6494_(PoseStack pose, MultiBufferSource buffers, int light, EtoNpc npc, float limbSwing, float limbSwingAmount, float partialTick,
                            float ageInTicks, float netHeadYaw, float headPitch) {
            if (npc.m_20145_()) {
                return;
            }
            try {
                PlayerModel<EtoNpc> model = this.m_117386_();
                // 1.3.1: her bust bounces as she jumps and lands (her height as drawn: a hop's lift too).
                RigPose posed = EtoModel.pose;
                double y = npc.f_19791_ + (npc.m_20186_() - npc.f_19791_) * partialTick - (posed == null ? 0.0 : posed.drop / 16.0);
                float bounce = EtoClient.jiggle(npc.m_19879_(), y, PaleClient.clock(partialTick));
                bust(model, pose, buffers.m_6299_(RenderType.m_110473_(SKIN)), light, bounce);
                EtoClient.View view = EtoClient.get(npc.m_19879_());
                double clock = PaleClient.clock(partialTick);
                EtoGesture gesture = view == null ? EtoGesture.NONE : view.gesture();
                boolean shut = gesture.eyesClosed || EtoRules.blink(npc.m_19879_() * 31 + 7, ageInTicks);
                float eye = EtoClient.eye(npc.m_19879_(), view, npc.m_9236_().m_46468_(), clock);
                if (!shut && eye > 0.003F) {
                    model.m_7695_(pose, buffers.m_6299_(RenderType.m_110473_(KAKUGAN)), light, OverlayTexture.f_118083_, 1.0F, 1.0F, 1.0F, eye);
                    model.m_7695_(pose, buffers.m_6299_(RenderType.m_110488_(GLOW)), 0xF000F0, OverlayTexture.f_118083_, eye, eye, eye, 1.0F);
                }
                if (shut) {
                    model.m_7695_(pose, buffers.m_6299_(RenderType.m_110473_(CLOSED)), light, OverlayTexture.f_118083_, 1.0F, 1.0F, 1.0F, 1.0F);
                }
                if (gesture.blush) {
                    float blush = (float)Math.min(1.0, (view == null ? 0.0 : view.gestureAge(clock)) / 8.0);
                    model.m_7695_(pose, buffers.m_6299_(RenderType.m_110473_(BLUSH)), light, OverlayTexture.f_118083_, 1.0F, 1.0F, 1.0F, blush);
                }
            } catch (RuntimeException failure) {
                PlayerLayers.failed("Eto's face", failure);
            }
        }
    }

    /** Her bust (1.3.9: the shared female body's, {@link FemaleFigure#bust}), on her torso, its tip {@code bounce} pixels up off its rest. */
    static void bust(PlayerModel<EtoNpc> model, PoseStack pose, VertexConsumer consumer, int light, float bounce) {
        FemaleFigure.bust(model.f_102810_, pose, consumer, light, bounce, false);
    }

    @Mod.EventBusSubscriber(modid = "hexkagune", value = Dist.CLIENT)
    public static final class Frame {
        private Frame() {
        }

        @SubscribeEvent
        public static void after(RenderLevelStageEvent event) {
            if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
                return;
            }
            Minecraft minecraft = Minecraft.m_91087_();
            if (minecraft.f_91073_ != null && minecraft.f_91073_.m_46467_() % 100L == 50L) {
                EtoClient.prune(minecraft.f_91073_);
                EtoFight.prune(id -> minecraft.f_91073_.m_6815_(id) == null);
            }
            EtoClient.pull();
        }
    }

    @Mod.EventBusSubscriber(modid = "hexkagune", bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class Registration {
        private Registration() {
        }

        @SubscribeEvent
        public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerEntityRenderer(ControlEntities.ETO.get(), EtoRenderer::new);
        }
    }
}
