package com.hex.kagune.control.client.render;

import com.hex.kagune.KaguneMod;
import com.hex.kagune.control.ukaku.KakujaModel;
import com.hex.kagune.control.client.UkakuClient;
import com.hex.kagune.control.client.KakuganClient;
import com.hex.kagune.control.kakugan.KakuganEyes;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.List;
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
 * The armor Ukaku's kakuja grows over his body ({@link KakujaModel}): the plated chest with the tentacles' sockets on the back
 * (the first kakuja's face stays bare since 3.0.0); the final kakuja's chest, and its half mask over the lower face, its top
 * edge under wherever his eyes are, fitted to the face (6.0.2). A layer of the player renderer, so it follows the torso and the
 * head through every animation. Lit by the world like the skin, the sockets' RC glowing.
 */
public final class KakujaArmorLayer extends RenderLayer<AbstractClientPlayer, EntityModel<AbstractClientPlayer>> {
    private static final RenderType TYPE = RenderType.m_110473_(Objects.requireNonNull(KaguneMod.id("textures/misc/ukaku_white.png")));
    private static final int FULL_BRIGHT = 0xF000F0;
    /**
     * With 3D Skin Layers (the second layer as voxels about 1.2 pixels deep): the chest's front and back plates pushed out
     * past them (depth scaled about the torso's middle, the front plates from 0.9 to 1.6 pixels off the body). The half mask is
     * fitted over whatever is under it instead (6.0.2, {@link KakujaModel#halfMaskShell}), never scaled.
     */
    private static final float CHEST_CLEARANCE = 1.3F;

    /** One armor quad in the part's pixels, with its normal and colours, and whether it glows. */
    private record Piece(double[][] corners, double[] normal, int[] argb) {
    }

    public KakujaArmorLayer(RenderLayerParent<AbstractClientPlayer, EntityModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    @Override
    public void m_6494_(PoseStack pose, MultiBufferSource buffers, int packedLight, AbstractClientPlayer player, float limbSwing, float limbSwingAmount,
                        float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        PlayerLayers.ran(player);
        try {
            this.draw(pose, buffers, packedLight, player, limbSwing, limbSwingAmount, partialTick, ageInTicks, netHeadYaw, headPitch);
        } catch (RuntimeException failure) {
            PlayerLayers.failed("kakuja armor", failure);
        }
    }

    private void draw(PoseStack pose, MultiBufferSource buffers, int packedLight, AbstractClientPlayer player, float limbSwing, float limbSwingAmount,
                        float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        if (player.m_20145_() || !(this.m_117386_() instanceof HumanoidModel<?> model)) {
            return;
        }
        float grow = UkakuClient.kakujaArmor(player, partialTick);
        if (grow <= 0.003F) {
            return;
        }
        // The first kakuja is bare-faced (3.0.0): no faceplate and no horns, only the chest armor. The final one wears the half mask.
        boolean finalForm = UkakuClient.finalKakuja(player);
        List<Piece> chest = new ArrayList<>();
        KakujaModel.chest(collect(chest), grow, 1.0, finalForm);
        VertexConsumer consumer = buffers.m_6299_(TYPE);
        if (finalForm) {
            KakuganEyes eyes = KakuganClient.eyes(player);
            int[] eyeCells = {eyes.column(true), eyes.row(true), eyes.column(false), eyes.row(false)};
            List<Piece> head = new ArrayList<>();
            // Fitted over the face itself when the skin's second layer leaves the lower face bare, over that layer (vanilla's or
            // 3D Skin Layers' voxels) when it covers any of it; a skin not read yet counts as covered.
            double shell = KakujaModel.halfMaskShell(eyeCells, KakuganClient.covered(player), KakuganClient.skinRead(player),
                PlayerLayers.skinLayers3d());
            KakujaModel.halfMask(collect(head), eyeCells, grow, 1.0, shell);
            pose.m_85836_();
            PlayerLayers.upperBody(pose, player, partialTick);
            model.f_102808_.m_104299_(pose);
            emit(consumer, head, packedLight, pose.m_85850_().m_252922_(), pose.m_85850_().m_252943_());
            pose.m_85849_();
        }
        pose.m_85836_();
        model.f_102810_.m_104299_(pose);
        if (PlayerLayers.skinLayers3d()) {
            pose.m_85841_(1.0F, 1.0F, CHEST_CLEARANCE);
        }
        // 5.0.19: the body part itself carries the lean now (RigPose), so the chest rides it as drawn.
        emit(consumer, chest, packedLight, pose.m_85850_().m_252922_(), pose.m_85850_().m_252943_());
        pose.m_85849_();
    }

    private static com.hex.kagune.control.ukaku.UkakuModel.Sink collect(List<Piece> out) {
        return (a, b, c, d, n, ca, cb, cc, cd) -> out.add(new Piece(new double[][] {a, b, c, d}, n, new int[] {ca, cb, cc, cd}));
    }

    private static void emit(VertexConsumer consumer, List<Piece> pieces, int light, Matrix4f matrix, Matrix3f normals) {
        for (Piece piece : pieces) {
            // The model's glow convention: a quad lit from straight above is RC light, at full brightness.
            boolean glow = piece.normal()[0] == 0.0 && piece.normal()[1] == 1.0 && piece.normal()[2] == 0.0;
            for (int corner = 0; corner < 4; corner++) {
                double[] p = piece.corners()[corner];
                int colour = piece.argb()[corner];
                consumer.m_252986_(matrix, (float)(p[0] / 16.0), (float)(p[1] / 16.0), (float)(p[2] / 16.0))
                    .m_6122_((colour >> 16) & 0xFF, (colour >> 8) & 0xFF, colour & 0xFF, colour >>> 24)
                    .m_7421_(0.5F, 0.5F)
                    .m_86008_(OverlayTexture.f_118083_)
                    .m_85969_(glow ? FULL_BRIGHT : light)
                    .m_252939_(normals, (float)piece.normal()[0], (float)piece.normal()[1], (float)piece.normal()[2])
                    .m_5752_();
            }
        }
    }
}
