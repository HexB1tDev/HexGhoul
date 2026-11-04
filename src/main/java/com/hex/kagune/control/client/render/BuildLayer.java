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
 * his body layer ({@code KaguneBodyAnimation}). Not drawn through a chestplate, nor while he is unseen.
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
            ReaperBuild.chest(false, sink);
            // The jacket layer when he wears one (3D Skin Layers hides the flat one and draws its own: drawn here too, so the chest
            // isn't left in the bare torso's colours).
            if (jacket(model)) {
                ReaperBuild.chest(true, sink);
            }
            pose.m_85849_();
        } catch (RuntimeException failure) {
            PlayerLayers.failed("build", failure);
        }
    }

    /**
     * True when {@code player}'s build is drawn on {@code model} now: a man, seen, his torso shown, no chestplate, and nothing grown over
     * his torso (Ukaku's kakuja armour, Graft's, a King's form or the turning into one: each fits the torso as it always was).
     */
    static boolean drawn(AbstractClientPlayer player, PlayerModel<?> model, float partialTick) {
        if (!Genders.male(player) || player.m_20145_() || !model.f_102810_.f_104207_
            || !player.m_6844_(net.minecraft.world.entity.EquipmentSlot.CHEST).m_41619_()) {
            return false;
        }
        if (com.hex.kagune.control.client.UkakuClient.kakujaArmor(player, partialTick) > 0.003F) {
            return false;
        }
        double clock = com.hex.kagune.control.client.PaleClient.clock(partialTick);
        if (com.hex.kagune.control.client.GraftClient.isGraft(player)
            && com.hex.kagune.control.client.GraftClient.hide(com.hex.kagune.control.client.GraftClient.get(player), clock) > 0.003) {
            return false;
        }
        com.hex.kagune.control.client.KingClient.View king = com.hex.kagune.control.client.KingClient.get(player);
        double time = com.hex.kagune.control.client.ControlClient.worldTime(partialTick);
        return (king == null || king.form() == com.hex.kagune.control.king.KingForm.NONE)
            && com.hex.kagune.control.client.TurningClient.progress(player, time) < 0.0;
    }

    /** True when the build's jacket layer is drawn: he wears his skin's jacket (or 3D Skin Layers draws it, hiding the flat one). */
    static boolean jacket(PlayerModel<?> model) {
        return model.f_103378_.f_104207_ || PlayerLayers.skinLayers3d();
    }
}
