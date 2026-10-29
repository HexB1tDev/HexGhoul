package com.hex.kagune.control.client.render;

import com.hex.kagune.KaguneMod;
import com.hex.kagune.control.ukaku.SteelBlade;
import com.hex.kagune.control.client.UkakuClient;
import com.hex.kagune.control.client.ControlClient;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * The final kakuja's Steel Blade on the right arm ({@link SteelBlade}; 5.0.17, rebuilt in 5.0.19): drawn in the arm's own space,
 * so it rides every pose and swing of the arm, forming out of the plates wrapped round it as the wings close. A swing leaves a
 * pale trail behind the blade (the steel's own motion blur, from where the blade was over the last few frames), and the wearer
 * sees it on his own empty right hand in first person ({@link FirstPerson}).
 */
public final class SteelBladeLayer extends RenderLayer<AbstractClientPlayer, EntityModel<AbstractClientPlayer>> {
    private static final RenderType TYPE = RenderType.m_110473_(Objects.requireNonNull(KaguneMod.id("textures/misc/ukaku_white.png")));
    private static final int FULL_BRIGHT = 0xF000F0;
    /** How long the trail lingers behind the blade (ticks), and the tip speed (blocks a tick) it starts showing at. */
    private static final double TRAIL_TICKS = 4.5;
    private static final double TRAIL_FROM = 0.18;
    private static final int TRAIL_COLOUR = 0xDCE2EC;
    /** Where the blade was over the last few frames, per player: in the model's root space (it turns with the body). */
    private static final Map<UUID, ArrayDeque<Sample>> TRAILS = new HashMap<>();

    /** One frame's blade: when, and its cutting stretch from a third of the way out to the point (root space, blocks). */
    private record Sample(double time, float[] near, float[] tip) {
    }

    public SteelBladeLayer(RenderLayerParent<AbstractClientPlayer, EntityModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    @Override
    public void m_6494_(PoseStack pose, MultiBufferSource buffers, int packedLight, AbstractClientPlayer player, float limbSwing, float limbSwingAmount,
                        float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        try {
            double time = ControlClient.worldTime(partialTick);
            double formed = UkakuClient.blade(player, time);
            if (formed <= 0.005 || player.m_20145_() || !(this.m_117386_() instanceof HumanoidModel<?> model) || !model.f_102811_.f_104207_) {
                TRAILS.remove(player.m_20148_());
                return;
            }
            Matrix4f root = new Matrix4f(pose.m_85850_().m_252922_());
            Matrix3f rootNormals = new Matrix3f(pose.m_85850_().m_252943_());
            pose.m_85836_();
            PlayerLayers.upperBody(pose, player, partialTick);
            model.f_102811_.m_104299_(pose);
            VertexConsumer out = buffers.m_6299_(TYPE);
            boolean slim = "slim".equals(player.m_108564_());
            double[] blood = com.hex.kagune.control.client.fx.BladeBloodFx.stain(player.m_19879_(), time);
            // 6.0.4: as far out as the arm was posed with (drawn in while it rests near the ground).
            double reach = com.hex.kagune.control.client.animation.KaguneBodyAnimation.bladeReach(player);
            SteelBlade.build(formed, time / 20.0, slim, true, blood[0], blood[1], reach, sink(out, pose.m_85850_().m_252922_(),
                pose.m_85850_().m_252943_(), packedLight));
            pose.m_85849_();
            // The same arm transform on its own, from the model's root: where the blade is relative to the body this frame.
            PoseStack local = new PoseStack();
            PlayerLayers.upperBody(local, player, partialTick);
            model.f_102811_.m_104299_(local);
            trail(out, player, formed, reach, time, slim, root, rootNormals, local.m_85850_().m_252922_());
        } catch (RuntimeException failure) {
            PlayerLayers.failed("steel blade", failure);
        }
    }

    /**
     * Where the blade's corners go: straight into {@code out} through {@code matrix} (arm pixels to the render's space), each with
     * its own smooth normal (no quads built on the way: the blade is a few thousand of them a frame).
     */
    static SteelBlade.Sink sink(VertexConsumer out, Matrix4f matrix, Matrix3f normals, int light) {
        return (x, y, z, argb, nx, ny, nz) -> out.m_252986_(matrix, (float)(x / 16.0), (float)(y / 16.0), (float)(z / 16.0))
            .m_6122_((argb >> 16) & 0xFF, (argb >> 8) & 0xFF, argb & 0xFF, 255)
            .m_7421_(0.5F, 0.5F)
            .m_86008_(OverlayTexture.f_118083_)
            .m_85969_(light)
            .m_252939_(normals, (float)nx, (float)ny, (float)nz)
            .m_5752_();
    }

    /**
     * Records where the blade's cutting stretch is this frame and draws the trail it leaves: a ribbon through the last few frames'
     * blades, fading with age, and only as fast as the blade moves (a swing leaves one, a stance doesn't).
     */
    private static void trail(VertexConsumer out, AbstractClientPlayer player, double formed, double reach, double time, boolean slim,
                              Matrix4f root, Matrix3f rootNormals, Matrix4f toRoot) {
        if (SteelBlade.extend(formed) < 0.3) {
            return;
        }
        // Its cutting edge, from a third of the way along to the point.
        float[] near = point(toRoot, SteelBlade.edgePoint(0.36, formed, slim, reach));
        float[] tip = point(toRoot, SteelBlade.edgePoint(1.0, formed, slim, reach));
        ArrayDeque<Sample> samples = TRAILS.computeIfAbsent(player.m_20148_(), ignored -> new ArrayDeque<>());
        if (samples.isEmpty() || time > samples.peekFirst().time() + 1.0E-4) {
            samples.addFirst(new Sample(time, near, tip));
        }
        while (samples.size() > 64 || !samples.isEmpty() && time - samples.peekLast().time() > TRAIL_TICKS) {
            samples.removeLast();
        }
        if (samples.size() < 2) {
            return;
        }
        Iterator<Sample> it = samples.iterator();
        Sample newer = it.next();
        while (it.hasNext()) {
            Sample older = it.next();
            double dt = Math.max(1.0E-3, newer.time() - older.time());
            float[] a = newer.tip();
            float[] b = older.tip();
            double speed = Math.sqrt((a[0] - b[0]) * (a[0] - b[0]) + (a[1] - b[1]) * (a[1] - b[1]) + (a[2] - b[2]) * (a[2] - b[2])) / dt;
            float strength = (float)Math.max(0.0, Math.min(1.0, (speed - TRAIL_FROM) / 0.5));
            float fadeNew = (float)Math.max(0.0, 1.0 - (time - newer.time()) / TRAIL_TICKS);
            float fadeOld = (float)Math.max(0.0, 1.0 - (time - older.time()) / TRAIL_TICKS);
            if (strength > 0.01F && fadeNew > 0.01F) {
                int aNew = (int)(150 * strength * fadeNew * fadeNew);
                int aOld = (int)(150 * strength * fadeOld * fadeOld);
                float[][] corners = {newer.near(), newer.tip(), older.tip(), older.near()};
                int[] alphas = {aNew / 3, aNew, aOld, aOld / 3};
                for (int k = 0; k < 4; k++) {
                    out.m_252986_(root, corners[k][0], corners[k][1], corners[k][2])
                        .m_6122_((TRAIL_COLOUR >> 16) & 0xFF, (TRAIL_COLOUR >> 8) & 0xFF, TRAIL_COLOUR & 0xFF, alphas[k])
                        .m_7421_(0.5F, 0.5F)
                        .m_86008_(OverlayTexture.f_118083_)
                        .m_85969_(FULL_BRIGHT)
                        .m_252939_(rootNormals, 0.0F, 1.0F, 0.0F)
                        .m_5752_();
                }
            }
            newer = older;
        }
    }

    /** A point of the arm (pixels) through {@code matrix} (blocks). */
    private static float[] point(Matrix4f matrix, double[] pixels) {
        Vector3f v = matrix.transformPosition(new Vector3f().set((float)(pixels[0] / 16.0), (float)(pixels[1] / 16.0), (float)(pixels[2] / 16.0)));
        return new float[] {v.x, v.y, v.z};
    }

    /**
     * 6.0.0: the Steel Blade in first person, held and swung exactly as the third person sees it: in place of the empty right
     * hand, the blade is drawn from the same pose of the arm (the hold, the swings, the ground guard: {@link
     * com.hex.kagune.control.client.animation.KaguneBodyAnimation#bladePose}) through the same turns the world is drawn with
     * (the camera's pitch, the head's turn against the body, the model's own flip and scale, the shoulder), so what he sees of it
     * is where it is.
     */
    @Mod.EventBusSubscriber(modid = "hexkagune", value = Dist.CLIENT)
    public static final class FirstPerson {
        private FirstPerson() {
        }

        @SubscribeEvent
        public static void hand(RenderHandEvent event) {
            if (event.getHand() != InteractionHand.MAIN_HAND || !event.getItemStack().m_41619_()) {
                return;
            }
            try {
                net.minecraft.client.player.LocalPlayer player = net.minecraft.client.Minecraft.m_91087_().f_91074_;
                if (player == null) {
                    return;
                }
                float partial = event.getPartialTick();
                double time = ControlClient.worldTime(partial);
                double formed = UkakuClient.blade(player, time);
                if (formed <= 0.005) {
                    return;
                }
                event.setCanceled(true); // the arm is the blade's
                com.hex.kagune.control.client.animation.RigPose rig = com.hex.kagune.control.client.animation.KaguneBodyAnimation.bladePose(player, partial);
                boolean crouching = player.m_6047_();
                float[] turn = rig.transform("rightArm", com.hex.kagune.control.client.animation.RigPose.ROTATION, 0.0F, 0.0F, 0.0F);
                float[] pivot = rig.transform("rightArm", com.hex.kagune.control.client.animation.RigPose.POSITION, -5.0F, crouching ? 5.2F : 2.0F, 0.0F);
                float headYaw = player.f_19859_ + (player.m_146908_() - player.f_19859_) * partial;
                float bodyDelta = ((player.f_20883_ - player.f_20884_) % 360.0F + 540.0F) % 360.0F - 180.0F;
                float bodyYaw = player.f_20884_ + bodyDelta * partial;
                PoseStack pose = event.getPoseStack();
                pose.m_85836_();
                // As the world is drawn: the camera's pitch, then from the eye down to his feet, the body turned under the head.
                pose.m_252781_(new Quaternionf().rotateAxis((float)Math.toRadians(event.getInterpolatedPitch()), 1.0F, 0.0F, 0.0F));
                pose.m_252880_(0.0F, crouching ? -1.27F : -1.62F, 0.0F);
                pose.m_252781_(new Quaternionf().rotateAxis((float)Math.toRadians(headYaw - bodyYaw), 0.0F, 1.0F, 0.0F));
                // The player model's own space, then its right arm, as ModelPart turns it.
                pose.m_85841_(-1.0F, -1.0F, 1.0F);
                pose.m_85841_(0.9375F, 0.9375F, 0.9375F);
                pose.m_252880_(0.0F, -1.501F, 0.0F);
                pose.m_252880_(pivot[0] / 16.0F, pivot[1] / 16.0F, pivot[2] / 16.0F);
                pose.m_252781_(new Quaternionf().rotateAxis(turn[2], 0.0F, 0.0F, 1.0F).rotateAxis(turn[1], 0.0F, 1.0F, 0.0F).rotateAxis(turn[0], 1.0F, 0.0F,
                    0.0F));
                double[] blood = com.hex.kagune.control.client.fx.BladeBloodFx.stain(player.m_19879_(), time);
                // No pauldron: right by the eye, it would fill the view.
                SteelBlade.build(formed, time / 20.0, "slim".equals(player.m_108564_()), false, blood[0], blood[1], rig.bladeReach,
                    sink(event.getMultiBufferSource().m_6299_(TYPE), pose.m_85850_().m_252922_(), pose.m_85850_().m_252943_(), event.getPackedLight()));
                pose.m_85849_();
            } catch (RuntimeException failure) {
                PlayerLayers.failed("steel blade (first person)", failure);
            }
        }

        @SubscribeEvent
        public static void loggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
            TRAILS.clear();
        }
    }
}
