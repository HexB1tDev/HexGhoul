package com.hex.kagune.control.client.render;

import com.hex.kagune.KaguneMod;
import com.hex.kagune.client.KaguneClientState;
import com.hex.kagune.control.KakujaBurst;
import com.hex.kagune.control.ayato.UkakuModel;
import com.hex.kagune.control.client.AyatoClient;
import com.hex.kagune.control.client.KakuganClient;
import com.hex.kagune.control.client.fx.KakujaFx;
import com.hex.kagune.control.kakugan.CentipedeMask;
import com.hex.kagune.control.kakugan.KakuganEyes;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.Objects;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * Kaneki's kakuja mask (the Centipede and both kakujas), on his face (3.0.0). The original drew it in the world from his
 * head's yaw, pitch and eye height, so every animation that moves the head model (a lean, a crouch, a swing) left it hanging
 * in the air beside his face. This draws the mask as a layer on the head model part, so it stays on the face through
 * everything. The original's world-space mask is switched off ({@link #emitKanekiMask}). Since 3.1.0 it is the new mask
 * ({@link CentipedeMask}: a lens over his right eye, the left eye bare, a beak, in the centipede legs' plum) rather than the
 * original's patch and beak ({@code KanekiMask}, kept for the previews' "before").
 */
public final class KanekiMaskLayer extends RenderLayer<AbstractClientPlayer, EntityModel<AbstractClientPlayer>> {
    private static final RenderType TYPE = RenderType.m_110473_(Objects.requireNonNull(KaguneMod.id("textures/misc/ukaku_white.png")));

    public KanekiMaskLayer(RenderLayerParent<AbstractClientPlayer, EntityModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    /** The original's world-space mask (trampoline target of {@code KaguneWorldRenderer.emitKanekiMask}): drawn by the layer now. */
    public static void emitKanekiMask(VertexConsumer consumer, Matrix4f pose, Matrix3f normals, AbstractClientPlayer player, KaguneClientState.View view,
                                      float partialTick, int light) {
    }

    @Override
    public void m_6494_(PoseStack pose, MultiBufferSource buffers, int packedLight, AbstractClientPlayer player, float limbSwing, float limbSwingAmount,
                        float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        PlayerLayers.ran(player);
        try {
            this.draw(pose, buffers, packedLight, player, limbSwing, limbSwingAmount, partialTick, ageInTicks, netHeadYaw, headPitch);
        } catch (RuntimeException failure) {
            PlayerLayers.failed("Kaneki mask", failure);
        }
    }

    private void draw(PoseStack pose, MultiBufferSource buffers, int packedLight, AbstractClientPlayer player, float limbSwing, float limbSwingAmount,
                        float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        if (player.m_20145_() || !player.m_6084_() || !(this.m_117386_() instanceof HumanoidModel<?> model)) {
            return;
        }
        KaguneClientState.View view = AyatoClient.tendrilView(player);
        if (!view.active() || !view.form().hasKanekiMask()) {
            return;
        }
        // 5.0.0: the mask grows over his face when the kakuja bursts out, and is gone the moment it breaks.
        double age = KakujaFx.maskAge(player, com.hex.kagune.control.client.ControlClient.worldTime(partialTick));
        if (Double.isNaN(age)) {
            return;
        }
        KakuganEyes eyes = KakuganClient.eyes(player);
        pose.m_85836_();
        PlayerLayers.upperBody(pose, player, partialTick);
        model.f_102808_.m_104299_(pose);
        Matrix4f matrix = pose.m_85850_().m_252922_();
        Matrix3f normals = pose.m_85850_().m_252943_();
        VertexConsumer consumer = buffers.m_6299_(TYPE);
        UkakuModel.Sink sink = (p, q, r, t, n, ca, cb, cc, cd) -> {
            double[][] corners = {p, q, r, t};
            int[] colours = {ca, cb, cc, cd};
            for (int corner = 0; corner < 4; corner++) {
                double[] v = corners[corner];
                int colour = colours[corner];
                consumer.m_252986_(matrix, (float)(v[0] / 16.0), (float)(v[1] / 16.0), (float)(v[2] / 16.0))
                    .m_6122_((colour >> 16) & 0xFF, (colour >> 8) & 0xFF, colour & 0xFF, colour >>> 24)
                    .m_7421_(0.5F, 0.5F)
                    .m_86008_(OverlayTexture.f_118083_)
                    .m_85969_(packedLight)
                    .m_252939_(normals, (float)n[0], (float)n[1], (float)n[2])
                    .m_5752_();
            }
        };
        CentipedeMask.build(age < KakujaBurst.MASK_TICKS ? KakujaBurst.growingMask(sink, CentipedeMask.lens(eyes), KakujaBurst.maskFront(age)) : sink, eyes);
        pose.m_85849_();
    }
}
