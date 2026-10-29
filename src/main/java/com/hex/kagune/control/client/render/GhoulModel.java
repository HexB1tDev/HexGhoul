package com.hex.kagune.control.client.render;

import com.hex.kagune.control.client.animation.RigPose;
import com.hex.kagune.control.npc.GhoulNpc;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;

/**
 * 1.0.41: a ghoul NPC's body: vanilla's player model (wide or slim arms) as it walks and looks, with whatever pose it is in on top
 * ({@link #pose}: its flip, as a player's is posed, {@code EtoMotions.flip}): the head, arms and legs turned by the pose's offsets,
 * the limbs' own swing dropped as far as the pose holds them, and the skin's second layer carried with them. Elbows and knees do not
 * bend on vanilla's model: the limbs swing whole. The whole body's turn (the flip itself) is the renderer's.
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
        float arms = 1.0F - Math.max(0.0F, Math.min(1.0F, p.armHold));
        float legs = 1.0F - Math.max(0.0F, Math.min(1.0F, p.legHold));
        float head = 1.0F - Math.max(0.0F, Math.min(1.0F, p.headHold));
        turn(this.f_102808_, head, p.headX, p.headY, p.headZ);
        turn(this.f_102811_, arms, p.rightArmX, p.rightArmY, p.rightArmZ);
        turn(this.f_102812_, arms, p.leftArmX, p.leftArmY, p.leftArmZ);
        turn(this.f_102813_, legs, p.rightLegX, p.rightLegY, p.rightLegZ);
        turn(this.f_102814_, legs, p.leftLegX, p.leftLegY, p.leftLegZ);
        // The second layer follows (vanilla copies it before the pose was put on).
        this.f_102809_.m_104315_(this.f_102808_);
        this.f_103375_.m_104315_(this.f_102811_);
        this.f_103374_.m_104315_(this.f_102812_);
        this.f_103377_.m_104315_(this.f_102813_);
        this.f_103376_.m_104315_(this.f_102814_);
        this.f_103378_.m_104315_(this.f_102810_);
    }

    /** {@code part}'s own turn kept {@code keep} of the way, the pose's offsets on top. */
    private static void turn(ModelPart part, float keep, float x, float y, float z) {
        part.f_104203_ = part.f_104203_ * keep + x;
        part.f_104204_ = part.f_104204_ * keep + y;
        part.f_104205_ = part.f_104205_ * keep + z;
    }
}
