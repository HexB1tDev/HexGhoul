package com.hex.kagune.control.client.render;

import com.hex.kagune.control.Genders;
import com.hex.kagune.control.npc.ReaperBuild;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * 2.0.46 (the owner: "whenever the player model picks male, I want you to give the same model as [the White Reaper] in terms of the
 * muscle"): a man's build, the White Reaper's ({@link ReaperBuild}): his torso's upper part drawn again over it, broader across the
 * shoulders and chest and standing out in front of his belly, tapering into his waist, painted with his own skin's torso and its jacket
 * layer over it, on his torso as it is posed (the upper body's bend carried, as every layer on it must). His arms are set out round it by
 * his body layer ({@code KaguneBodyAnimation}). Not drawn through a chestplate, nor while he is unseen. 2.2.5: slimmer than the Reaper's
 * ({@link ReaperBuild#PLAYER}; the owner: the top chest "a little too large ... for the player").
 */
public final class BuildLayer extends RenderLayer<AbstractClientPlayer, EntityModel<AbstractClientPlayer>> {
    public BuildLayer(RenderLayerParent<AbstractClientPlayer, EntityModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    @Override
    public void m_6494_(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer player, float limbSwing, float limbSwingAmount,
                        float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        try {
            if (!(this.m_117386_() instanceof PlayerModel<?> model) || !drawn(player, model, partialTick)) {
                return;
            }
            pose.m_85836_();
            PlayerLayers.upperBody(pose, player, partialTick);
            model.f_102810_.m_104299_(pose);
            Matrix4f m = pose.m_85850_().m_252922_();
            Matrix3f n = pose.m_85850_().m_252943_();
            VertexConsumer out = buffers.m_6299_(RenderType.m_110473_(player.m_108560_()));
            ReaperBuild.Sink sink = (corners, uv, normal) -> {
                for (int k = 0; k < 4; k++) {
                    double[] c = corners[k];
                    out.m_252986_(m, (float)(c[0] / 16.0), (float)(c[1] / 16.0), (float)(c[2] / 16.0)).m_6122_(255, 255, 255, 255)
                        .m_7421_((float)(uv[2 * k] / 64.0), (float)(uv[2 * k + 1] / 64.0)).m_86008_(OverlayTexture.f_118083_).m_85969_(light)
                        .m_252939_(n, (float)normal[0], (float)normal[1], (float)normal[2]).m_5752_();
                }
            };
            ReaperBuild.chest(ReaperBuild.PLAYER, false, sink); // 2.2.5: a player's, slimmer than the Reaper's
            // The jacket layer when he wears one (3D Skin Layers hides the flat one and draws its own: drawn here too, so the chest
            // isn't left in the bare torso's colours).
            if (jacket(model)) {
                ReaperBuild.chest(ReaperBuild.PLAYER, true, sink);
            }
            pose.m_85849_();
        } catch (RuntimeException failure) {
            PlayerLayers.failed("build", failure);
        }
    }

    /**
     * True when {@code player}'s build is drawn on {@code model} now: a man, seen, his torso shown, no chestplate. 2.3.3 (the owner: "Whenever
     * you transform into any form, your chest ... vanishes ... The chest should adapt to the form and vice versa"): in every form too
     * (Ukaku's kakuja, Graft's hide, War, Peace, the Dragon's turning); the forms' torso armour is widened over it instead
     * ({@link #fit}).
     */
    static boolean drawn(AbstractClientPlayer player, PlayerModel<?> model, float partialTick) {
        return built(player, model);
    }

    /** 2.3.3: true when {@code player}'s build shows on {@code model} (a man, seen, his torso shown, no chestplate). */
    public static boolean built(AbstractClientPlayer player, net.minecraft.client.model.HumanoidModel<?> model) {
        return Genders.male(player) && !player.m_20145_() && model.f_102810_.f_104207_
            && player.m_6844_(net.minecraft.world.entity.EquipmentSlot.CHEST).m_41619_();
    }

    /**
     * 2.3.3: {@code pose}, already on the torso ({@code translateAndRotate}), widened and deepened about its middle so armour fitted to
     * the bare torso sits over a man's chest ({@code ReaperBuild.PLAYER_FIT_X}, {@code _Z}); nothing for anyone else.
     */
    public static void fit(PoseStack pose, AbstractClientPlayer player, net.minecraft.client.model.HumanoidModel<?> model) {
        if (built(player, model)) {
            pose.m_85841_(ReaperBuild.PLAYER_FIT_X, 1.0F, ReaperBuild.PLAYER_FIT_Z);
        }
    }

    /** True when the build's jacket layer is drawn: he wears his skin's jacket (or 3D Skin Layers draws it, hiding the flat one). */
    static boolean jacket(PlayerModel<?> model) {
        return model.f_103378_.f_104207_ || PlayerLayers.skinLayers3d();
    }
}
