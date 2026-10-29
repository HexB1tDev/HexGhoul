package com.hex.kagune.control.client.render;

import com.hex.kagune.KaguneMod;
import com.hex.kagune.control.client.ControlClient;
import com.hex.kagune.control.client.KingClient;
import com.hex.kagune.control.client.animation.FirstPersonArms;
import com.hex.kagune.control.king.KingForm;
import com.hex.kagune.control.king.KingMove;
import com.hex.kagune.control.king.WarModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderArmEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

/**
 * 1.0.26: a final kakuja's own arms in first person. Vanilla draws a bare arm for an empty hand, its skin and nothing of War's plates
 * or Peace's white armour; just before it does ({@link RenderArmEvent}), that arm's pieces of the form are drawn on it, from the same
 * model third person draws, where vanilla's model puts that arm (its shoulder, its little sway); holding something, vanilla draws the
 * item, no arm. 1.2.4: through War's own moves too (his arms posed as third person poses them, pulled in before the eyes, filled the
 * view), the hand swinging as each blow of his hands lands ({@link #tick}).
 */
@Mod.EventBusSubscriber(modid = "hexkagune", value = Dist.CLIENT)
public final class KingFirstPerson {
    private static final RenderType SHAPES = RenderType.m_110473_(Objects.requireNonNull(KaguneMod.id("textures/misc/ukaku_white.png")));
    private static final RenderType LIGHT = RenderType.m_110488_(Objects.requireNonNull(KaguneMod.id("textures/misc/ukaku_white.png")));
    private static final int FULL_BRIGHT = 0xF000F0;
    /** Vanilla's first-person arm: its shoulder (model pixels; a slim arm sits half a pixel lower) and its sway (radians, outward). */
    static final float SHOULDER_X = 5.0F;
    static final float SHOULDER_Y = 2.0F;
    static final float SLIM_Y = 2.5F;
    static final float SWAY = 0.1F;

    /** 1.2.4: when the move whose blow last swung this client's hand began (world ticks): one swing a blow. */
    private static long swungFor = Long.MIN_VALUE;

    private KingFirstPerson() {
    }

    /**
     * 1.2.4: in first person, War's own hand swings (vanilla's swing, on this client alone: nothing is sent, no one else sees it) as each
     * blow of his hands lands ({@link FirstPersonArms#swingAt}), once a blow.
     */
    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.m_91087_();
        LocalPlayer player = minecraft.f_91074_;
        if (player == null || minecraft.m_91104_() || !minecraft.f_91066_.m_92176_().m_90612_()) {
            return;
        }
        KingClient.View view = KingClient.get(player);
        if (view == null || !view.on() || view.form() != KingForm.WAR) {
            return;
        }
        double now = ControlClient.worldTime(0.0F);
        KingMove move = view.moveAt(now);
        long began = view.moveAt();
        if (move == KingMove.NONE || began == swungFor) {
            return;
        }
        int at = FirstPersonArms.swingAt(move, view.level());
        if (at >= 0 && now - began >= at) {
            swungFor = began;
            ((net.minecraft.world.entity.LivingEntity)player).m_21011_(InteractionHand.MAIN_HAND, false);
        }
    }

    @SubscribeEvent
    public static void arm(RenderArmEvent event) {
        AbstractClientPlayer player = event.getPlayer();
        KingClient.View view = player == null ? null : KingClient.get(player);
        if (view == null || !view.on()) {
            return;
        }
        double time = ControlClient.worldTime(net.minecraft.client.Minecraft.m_91087_().m_91296_());
        double grow = view.grow(time);
        if (grow < 0.3 || view.form() != KingForm.WAR && view.form() != KingForm.PEACE) {
            return;
        }
        try {
            boolean right = ((Enum<?>)event.getArm()).ordinal() == 1;
            boolean slim = "slim".equals(player.m_108564_());
            WarModel.Part part = right ? WarModel.Part.RIGHT_ARM : WarModel.Part.LEFT_ARM;
            List<float[]> quads = new ArrayList<>();
            com.hex.kagune.control.ayato.UkakuModel.Sink sink = (a, b, c, d, n, ca, cb, cc, cd) -> quads.add(new float[] {(float)a[0], (float)a[1],
                (float)a[2], (float)b[0], (float)b[1], (float)b[2], (float)c[0], (float)c[1], (float)c[2], (float)d[0], (float)d[1], (float)d[2], (float)n[0],
                (float)n[1], (float)n[2], ca, cb, cc, cd, n[0] == 0.0 && n[1] == 1.0 && n[2] == 0.0 ? 1.0F : 0.0F});
            if (view.form() == KingForm.WAR) {
                WarModel.build(sink, part, time, grow, 0.0, WarModel.Motion.STILL,
                    WarModel.eyeOn(com.hex.kagune.control.client.KakuganClient.eyes(player)));
            } else {
                com.hex.kagune.control.king.PeaceModel.build(sink, part, time, grow, com.hex.kagune.control.king.PeaceMotion.State.REST, false, null,
                    com.hex.kagune.control.king.PeaceModel.ARMOUR);
            }
            if (quads.isEmpty()) {
                return;
            }
            PoseStack pose = event.getPoseStack();
            pose.m_85836_();
            // Where vanilla's model puts the arm it is about to draw: at its shoulder, swayed out a little (ModelPart's own turn).
            pose.m_252880_((right ? -SHOULDER_X : SHOULDER_X) / 16.0F, (slim ? SLIM_Y : SHOULDER_Y) / 16.0F, 0.0F);
            pose.m_252781_(new Quaternionf().rotateAxis(right ? SWAY : -SWAY, 0.0F, 0.0F, 1.0F));
            Matrix4f matrix = pose.m_85850_().m_252922_();
            Matrix3f normals = pose.m_85850_().m_252943_();
            MultiBufferSource buffers = event.getMultiBufferSource();
            int light = event.getPackedLight();
            VertexConsumer shapes = buffers.m_6299_(SHAPES);
            for (float[] q : quads) {
                if (q[19] == 0.0F) {
                    for (int k = 0; k < 4; k++) {
                        vertex(shapes, matrix, normals, q, k, (int)q[15 + k], light);
                    }
                }
            }
            VertexConsumer burning = buffers.m_6299_(LIGHT);
            for (float[] q : quads) {
                if (q[19] != 0.0F) {
                    for (int k = 0; k < 4; k++) {
                        vertex(burning, matrix, normals, q, k, premultiplied((int)q[15 + k]), FULL_BRIGHT);
                    }
                }
            }
            pose.m_85849_();
        } catch (RuntimeException failure) {
            PlayerLayers.failed("final kakuja arm (first person)", failure);
        }
    }

    private static void vertex(VertexConsumer out, Matrix4f matrix, Matrix3f normals, float[] q, int corner, int colour, int light) {
        out.m_252986_(matrix, q[corner * 3] / 16.0F, q[corner * 3 + 1] / 16.0F, q[corner * 3 + 2] / 16.0F)
            .m_6122_((colour >> 16) & 0xFF, (colour >> 8) & 0xFF, colour & 0xFF, colour >>> 24)
            .m_7421_(0.5F, 0.5F)
            .m_86008_(OverlayTexture.f_118083_)
            .m_85969_(light)
            .m_252939_(normals, q[12], q[13], q[14])
            .m_5752_();
    }

    /** A glow's colour scaled by its own alpha (the glow adds: the alpha is how bright). */
    private static int premultiplied(int argb) {
        double a = ((argb >>> 24) & 0xFF) / 255.0;
        int r = (int)Math.round(((argb >> 16) & 0xFF) * a);
        int g = (int)Math.round(((argb >> 8) & 0xFF) * a);
        int b = (int)Math.round((argb & 0xFF) * a);
        return 0xFF000000 | r << 16 | g << 8 | b;
    }
}
