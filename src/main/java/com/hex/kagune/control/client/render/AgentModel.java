package com.hex.kagune.control.client.render;

import com.hex.kagune.control.client.animation.RigPose;
import com.hex.kagune.control.npc.AgentRules;
import com.hex.kagune.control.npc.CcgAgent;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;

/**
 * 1.3.27: a CCG investigator's body: vanilla's player model (wide or slim arms) as it walks and looks, its pose on top ({@link #pose}:
 * its run, its quinque, its flips, each part posed as a player's is, {@link RigPose#transform}), and a woman's figure on hers (Eto's,
 * the shared female body's legs, a touch less of them: {@link AgentRules#FEMALE_LEGS}; her bust is the renderer's).
 */
final class AgentModel extends PlayerModel<CcgAgent> {
    /** The pose of the investigator being drawn now (the render thread's alone), or null. */
    static RigPose pose;
    /** 2.0.45: the investigator being drawn now is the White Reaper (his arms set out round his broader chest: ReaperBuild). */
    static boolean brawny;
    /** True while this model's arms are set out for him ({@link #settle} puts them back once he is drawn). */
    private boolean spread;

    private final boolean female;

    AgentModel(ModelPart root, boolean slim, boolean female) {
        super(root, slim);
        this.female = female;
    }

    @Override
    public void m_6973_(CcgAgent agent, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        super.m_6973_(agent, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        RigPose p = pose;
        if (p != null) {
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
        this.settle();
        if (brawny) {
            // 2.0.45: his arms hang a little further out, clear of his broader chest (their sleeves with them).
            this.f_102811_.f_104200_ -= com.hex.kagune.control.npc.ReaperBuild.ARM_OUT;
            this.f_102812_.f_104200_ += com.hex.kagune.control.npc.ReaperBuild.ARM_OUT;
            this.f_103375_.m_104315_(this.f_102811_);
            this.f_103374_.m_104315_(this.f_102812_);
            this.spread = true;
        }
        if (!this.female) {
            return;
        }
        for (ModelPart leg : new ModelPart[] {this.f_102813_, this.f_102814_, this.f_103377_, this.f_103376_}) {
            leg.f_233553_ = 1.0F;
            leg.f_233555_ = 1.0F;
        }
        FemaleFigure.shapeLegs(this, AgentRules.FEMALE_LEGS);
        FemaleFigure.spreadLegs(this, AgentRules.FEMALE_LEGS);
    }

    /**
     * 2.0.45: the arms back where they were before they were set out for the White Reaper (the model is shared with others on the same
     * arms, and vanilla doesn't put an arm's place back every frame).
     */
    void settle() {
        if (!this.spread) {
            return;
        }
        this.spread = false;
        this.f_102811_.f_104200_ += com.hex.kagune.control.npc.ReaperBuild.ARM_OUT;
        this.f_102812_.f_104200_ -= com.hex.kagune.control.npc.ReaperBuild.ARM_OUT;
        this.f_103375_.m_104315_(this.f_102811_);
        this.f_103374_.m_104315_(this.f_102812_);
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
