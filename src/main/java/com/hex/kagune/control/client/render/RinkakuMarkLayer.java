package com.hex.kagune.control.client.render;

import com.hex.kagune.client.KaguneClientState;
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
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * 2.0.40 (the owner: "add some texture to the skin as if it's like, you know, red on the area it's emerging from. This should just like
 * be a modification of the skin, just like the eyes are"): where a Rinkaku's tails tear out of the lower back (the owner's reference: a
 * glowing opening with a torn rim, red veins branching out across the skin), drawn on the back of the torso as it is posed (a layer, as
 * the kakugan is on the head), over the skin and its second layer, while the tails are out ({@code tools/rinkaku_mark.py}'s tiles). In the
 * kagune's own colour; its heart and the veins' cores glow in the dark, breathing a little. It comes up as the kagune comes out and goes as
 * it goes back.
 */
public final class RinkakuMarkLayer extends RenderLayer<AbstractClientPlayer, EntityModel<AbstractClientPlayer>> {
    private static final ResourceLocation MARK = Objects.requireNonNull(ResourceLocation.m_214293_("hexkagune", "textures/misc/rinkaku_mark.png"));
    private static final ResourceLocation GLOW = Objects.requireNonNull(ResourceLocation.m_214293_("hexkagune", "textures/misc/rinkaku_mark_glow.png"));
    private static final RenderType LIT = RenderType.m_110473_(MARK);
    private static final RenderType SHINE = RenderType.m_110488_(GLOW);
    private static final int FULL_BRIGHT = 0xF000F0;
    /** The reference's red: the skin's opening and veins, and their glow (turned to the kagune's colour, KaguneTint). */
    static final int RED = 0xFFD0203A;
    static final int GLOWING = 0xFFFF3A48;
    /** Over the back of the torso (model pixels): across all of it, from 2 px under the shoulders to the belt, just outside its jacket. */
    static final float LEFT = -4.0F;
    static final float RIGHT = 4.0F;
    static final float TOP = 2.0F;
    static final float BOTTOM = 12.0F;
    static final float OUT = 2.32F;
    /** 2.0.46: on a man's build, how many bands down the back the tile is cut into, and how far outside the build's back it lies. */
    static final int BANDS = 20;
    static final float LIFT = 0.07F;

    public RinkakuMarkLayer(RenderLayerParent<AbstractClientPlayer, EntityModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    @Override
    public void m_6494_(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer player, float limbSwing, float limbSwingAmount,
                        float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        try {
            draw(pose, buffers, light, player, partialTick, ageInTicks);
        } catch (RuntimeException failure) {
            PlayerLayers.failed("the Rinkaku mark", failure);
        }
    }

    private void draw(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer player, float partialTick, float ageInTicks) {
        if (player.m_20145_() || !(this.m_117386_() instanceof HumanoidModel<?> model)) {
            return;
        }
        // The same view the original draws the tails from (none for Ukaku, Pale, Graft...), and how far they are out.
        KaguneClientState.View view = com.hex.kagune.control.client.UkakuClient.tendrilView(player);
        if (view == null || !view.active()) {
            return;
        }
        float out = com.hex.kagune.control.client.GraftClient.deploy(player, partialTick); // (the original's manifest timing, any path)
        if (out <= 0.01F) {
            return;
        }
        int colour = view.color() & 0xFFFFFF;
        int red = com.hex.kagune.control.KaguneTint.recolour(RED, colour);
        int glowing = com.hex.kagune.control.KaguneTint.recolour(GLOWING, colour);
        float breath = 0.82F + 0.18F * (float)Math.sin(ageInTicks * 0.12F + player.m_19879_());
        pose.m_85836_();
        model.f_102810_.m_104299_(pose);
        Matrix4f matrix = pose.m_85850_().m_252922_();
        Matrix3f normals = pose.m_85850_().m_252943_();
        // 2.0.46: on a man's broader back (BuildLayer) the mark lies on it, not under it.
        boolean built = model instanceof net.minecraft.client.model.PlayerModel<?> player2 && BuildLayer.drawn(player, player2, partialTick);
        boolean jacket = built && BuildLayer.jacket((net.minecraft.client.model.PlayerModel<?>)model);
        quad(buffers.m_6299_(LIT), matrix, normals, red, (int)(255 * Math.min(1.0F, out * 1.4F)), light, false, built, jacket);
        float k = Math.min(1.0F, out) * breath;
        int dim = 0xFF000000 | (int)(((glowing >> 16) & 0xFF) * k) << 16 | (int)(((glowing >> 8) & 0xFF) * k) << 8 | (int)((glowing & 0xFF) * k);
        quad(buffers.m_6299_(SHINE), matrix, normals, dim, 255, FULL_BRIGHT, true, built, jacket);
        pose.m_85849_();
    }

    /**
     * The tile over the back ({@code both}: sent both ways round, for the glow's type, which culls back faces). 2.0.46: {@code built}, on a
     * man's build: cut into bands down the back, each laid just outside the build's back where it stands out ({@code jacket}: its jacket
     * layer's).
     */
    private static void quad(VertexConsumer out, Matrix4f matrix, Matrix3f normals, int argb, int alpha, int light, boolean both, boolean built,
                             boolean jacket) {
        int bands = built ? BANDS : 1;
        for (int b = 0; b < bands; b++) {
            float v0 = b / (float)bands;
            float v1 = (b + 1) / (float)bands;
            float y0 = TOP + (BOTTOM - TOP) * v0;
            float y1 = TOP + (BOTTOM - TOP) * v1;
            float z0 = built ? Math.max(OUT, (float)com.hex.kagune.control.npc.ReaperBuild.back(com.hex.kagune.control.npc.ReaperBuild.PLAYER, y0, jacket) + LIFT) : OUT;
            float z1 = built ? Math.max(OUT, (float)com.hex.kagune.control.npc.ReaperBuild.back(com.hex.kagune.control.npc.ReaperBuild.PLAYER, y1, jacket) + LIFT) : OUT;
            float[][] corners = {{LEFT, y0, 0.0F, v0, z0}, {LEFT, y1, 0.0F, v1, z1}, {RIGHT, y1, 1.0F, v1, z1}, {RIGHT, y0, 1.0F, v0, z0}};
            for (int pass = 0; pass < (both ? 2 : 1); pass++) {
                for (int i = 0; i < 4; i++) {
                    float[] c = corners[pass == 0 ? i : 3 - i];
                    // The back faces +z in the model; seen from behind its left is the model's +x, so the tile's u runs the other way.
                    out.m_252986_(matrix, c[0] / 16.0F, c[1] / 16.0F, c[4] / 16.0F)
                        .m_6122_((argb >> 16) & 0xFF, (argb >> 8) & 0xFF, argb & 0xFF, alpha)
                        .m_7421_(1.0F - c[2], c[3])
                        .m_86008_(OverlayTexture.f_118083_)
                        .m_85969_(light)
                        .m_252939_(normals, 0.0F, 0.0F, 1.0F)
                        .m_5752_();
                }
            }
        }
    }
}
