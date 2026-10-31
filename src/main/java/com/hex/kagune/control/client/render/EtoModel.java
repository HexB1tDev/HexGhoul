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
    /** 1.3.9: her figure is the one female body's ({@code FemaleBody}), shared with every woman. */
    static final float HIPS = com.hex.kagune.control.FemaleBody.HIPS;
    static final float THIGHS = com.hex.kagune.control.FemaleBody.THIGHS;
    static final float LEG_X = com.hex.kagune.control.FemaleBody.LEG_X;

    /** The pose of the Eto being drawn now (the render thread's alone), or null. */
    static RigPose pose;

    /** 1.3.14: a woman's figure (Eto, Touka) or not (Ken). */
    private final boolean female;
    /** 1.3.14: how much of the figure's curves (Touka's are a little less than Eto's). */
    private final float curves;

    EtoModel(ModelPart root) {
        this(root, true, true, 1.0F);
    }

    EtoModel(ModelPart root, boolean slim, boolean female, float curves) {
        super(root, slim);
        this.female = female;
        this.curves = curves;
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
        if (!this.female) {
            return;
        }
        // 1.3.9: the shared female figure's legs (FemaleFigure): wider, deeper, meeting at the middle.
        for (ModelPart leg : new ModelPart[] {this.f_102813_, this.f_102814_, this.f_103377_, this.f_103376_}) {
            leg.f_233553_ = 1.0F;
            leg.f_233555_ = 1.0F;
        }
        FemaleFigure.shapeLegs(this, this.curves);
        FemaleFigure.spreadLegs(this, this.curves);
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
