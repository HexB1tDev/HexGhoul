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
            // 1.3.4: each part posed exactly as a player's is under the same pose (RigPose.transform: PlayerAnimator's own conversion):
            // the head, the chest's lean, side and twist (the arms' pivots carried round with it), the arms and legs with their holds.
            apply("head", this.f_102808_, p);
            apply("torso", this.f_102810_, p);
            apply("rightArm", this.f_102811_, p);
            apply("leftArm", this.f_102812_, p);
            apply("rightLeg", this.f_102813_, p);
            apply("leftLeg", this.f_102814_, p);
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

    /** {@code part} (PlayerAnimator's name {@code name}) moved and turned as the pose has it, from where vanilla put it. */
    private static void apply(String name, ModelPart part, RigPose p) {
        float[] at = p.transform(name, RigPose.POSITION, part.f_104200_, part.f_104201_, part.f_104202_);
        float[] turn = p.transform(name, RigPose.ROTATION, part.f_104203_, part.f_104204_, part.f_104205_);
        part.f_104200_ = at[0];
        part.f_104201_ = at[1];
        part.f_104202_ = at[2];
        part.f_104203_ = turn[0];
        part.f_104204_ = turn[1];
        part.f_104205_ = turn[2];
    }
}
