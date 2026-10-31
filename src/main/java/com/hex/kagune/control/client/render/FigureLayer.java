package com.hex.kagune.control.client.render;

import com.hex.kagune.control.Genders;
import com.hex.kagune.control.client.PaleClient;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;

/**
 * 1.3.9: a female player's bust ({@link FemaleFigure#bust}): on her torso as it is posed (the upper body's bend carried, as every layer
 * on it must), painted with her own skin's torso front and its jacket layer over it, bouncing as she moves. Not drawn through a
 * chestplate, nor while she is unseen.
 */
public final class FigureLayer extends RenderLayer<AbstractClientPlayer, EntityModel<AbstractClientPlayer>> {
    public FigureLayer(RenderLayerParent<AbstractClientPlayer, EntityModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    @Override
    public void m_6494_(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer player, float limbSwing, float limbSwingAmount,
                        float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        PlayerLayers.ran(player);
        try {
            if (!Genders.female(player) || player.m_20145_() || !(this.m_117386_() instanceof PlayerModel<?> model)
                || !player.m_6844_(net.minecraft.world.entity.EquipmentSlot.CHEST).m_41619_()) {
                return;
            }
            double y = player.f_19791_ + (player.m_20186_() - player.f_19791_) * partialTick;
            float bounce = FemaleFigure.jiggle(player.m_19879_(), y, PaleClient.clock(partialTick));
            pose.m_85836_();
            PlayerLayers.upperBody(pose, player, partialTick);
            RenderType skin = RenderType.m_110473_(player.m_108560_());
            FemaleFigure.bust(model.f_102810_, pose, buffers.m_6299_(skin), light, bounce, false);
            // 1.3.10: carrying a child, her belly.
            float swell = com.hex.kagune.control.client.PregnancyClient.belly(player, partialTick);
            FemaleFigure.belly(model.f_102810_, pose, buffers.m_6299_(skin), light, swell, false);
            if (model.f_103378_.f_104207_) {
                FemaleFigure.bust(model.f_102810_, pose, buffers.m_6299_(skin), light, bounce, true);
                FemaleFigure.belly(model.f_102810_, pose, buffers.m_6299_(skin), light, swell, true);
            }
            pose.m_85849_();
        } catch (RuntimeException failure) {
            PlayerLayers.failed("figure", failure);
        }
    }
}
