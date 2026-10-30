package com.hex.kagune.control.client.render;

import com.hex.kagune.control.client.animation.RigPose;
import com.hex.kagune.control.npc.EtoNpc;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;

/**
 * 1.3.0: Eto's body: vanilla's player model on the slim arms as it walks and looks, whatever pose she is in on top ({@link #pose}, as
 * {@link GhoulModel} takes a ghoul NPC's), and her figure: the legs drawn a little wider and deeper than the torso, so her hips are wider
 * than her waist (her bust is {@link EtoRenderer}'s, drawn on the torso). The skin's second layer is carried with every part.
 */
final class EtoModel extends PlayerModel<EtoNpc> {
    /** How much wider (across) and deeper (front to back) her legs are drawn. */
    static final float HIPS = 1.25F;
    static final float THIGHS = 1.1F;
    /** 1.3.1: each leg's pivot moved out so the wider legs meet at the middle (as vanilla's do, 0.1 pixel over) rather than overlap. */
    static final float LEG_X = 2.0F * HIPS - 0.1F;

    /** The pose of the Eto being drawn now (the render thread's alone), or null. */
    static RigPose pose;

    EtoModel(ModelPart root) {
        super(root, true);
    }

    @Override
    public void m_6973_(EtoNpc npc, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        super.m_6973_(npc, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        RigPose p = pose;
        if (p != null) {
            float arms = 1.0F - Math.max(0.0F, Math.min(1.0F, p.armHold));
            float legs = 1.0F - Math.max(0.0F, Math.min(1.0F, p.legHold));
            float head = 1.0F - Math.max(0.0F, Math.min(1.0F, p.headHold));
            turn(this.f_102808_, head, p.headX, p.headY, p.headZ);
            turn(this.f_102811_, arms, p.rightArmX, p.rightArmY, p.rightArmZ);
            turn(this.f_102812_, arms, p.leftArmX, p.leftArmY, p.leftArmZ);
            turn(this.f_102813_, legs, p.rightLegX, p.rightLegY, p.rightLegZ);
            turn(this.f_102814_, legs, p.leftLegX, p.leftLegY, p.leftLegZ);
            this.f_102809_.m_104315_(this.f_102808_);
            this.f_103375_.m_104315_(this.f_102811_);
            this.f_103374_.m_104315_(this.f_102812_);
            this.f_103377_.m_104315_(this.f_102813_);
            this.f_103376_.m_104315_(this.f_102814_);
            this.f_103378_.m_104315_(this.f_102810_);
        }
        for (ModelPart leg : new ModelPart[] {this.f_102813_, this.f_102814_, this.f_103377_, this.f_103376_}) {
            leg.f_233553_ = HIPS;
            leg.f_233555_ = THIGHS;
            leg.f_104200_ = leg == this.f_102813_ || leg == this.f_103377_ ? -LEG_X : LEG_X;
        }
    }

    private static void turn(ModelPart part, float keep, float x, float y, float z) {
        part.f_104203_ = part.f_104203_ * keep + x;
        part.f_104204_ = part.f_104204_ * keep + y;
        part.f_104205_ = part.f_104205_ * keep + z;
    }
}
