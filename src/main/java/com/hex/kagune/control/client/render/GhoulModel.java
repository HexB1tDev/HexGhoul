package com.hex.kagune.control.client.render;

import com.hex.kagune.control.client.animation.RigPose;
import com.hex.kagune.control.npc.GhoulNpc;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;

/**
 * 1.0.41: a ghoul NPC's body: vanilla's player model (wide or slim arms) as it walks and looks, with whatever pose it is in on top
 * ({@link #pose}: its flip, as a player's is posed, {@code PaleMotions.flip}; 1.3.36 an arm blade's guard, cuts, wrap and sprint), each
 * part posed as a player's is (RigPose.transform), the limbs' own swing dropped as far as the pose holds them, and the skin's second
 * layer carried with them. Elbows and knees do not bend on vanilla's model: the limbs swing whole. The whole body's turn (the flip
 * itself, a lunge, a drop) is the renderer's.
 */
final class GhoulModel extends PlayerModel<GhoulNpc> {
    /** The pose of the NPC being drawn now (set by the renderer for the one it draws; the render thread's alone), or null. */
    static RigPose pose;

    GhoulModel(ModelPart root, boolean slim) {
        super(root, slim);
    }

    @Override
    public void m_6973_(GhoulNpc npc, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        super.m_6973_(npc, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        RigPose p = pose;
        if (p == null) {
            return;
        }
        // 1.3.36: each part moved and turned as a player's is (RigPose.transform: the chest's twist and the lean carried, held arms in
        // the chest's frame), as the investigators' are: the arm blade's cuts turn the chest through them.
        apply("head", this.f_102808_, p);
        apply("torso", this.f_102810_, p);
        apply("rightArm", this.f_102811_, p);
        apply("leftArm", this.f_102812_, p);
        apply("rightLeg", this.f_102813_, p);
        apply("leftLeg", this.f_102814_, p);
        // The second layer follows (vanilla copies it before the pose was put on).
        this.f_102809_.m_104315_(this.f_102808_);
        this.f_103375_.m_104315_(this.f_102811_);
        this.f_103374_.m_104315_(this.f_102812_);
        this.f_103377_.m_104315_(this.f_102813_);
        this.f_103376_.m_104315_(this.f_102814_);
        this.f_103378_.m_104315_(this.f_102810_);
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
