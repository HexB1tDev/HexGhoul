// Compile-only API stub (Minecraft 1.20.1 / Forge 47 / PlayerAnimator). Never packaged; see verify/api-allowlist.txt.
package net.minecraft.client.model;

public class PlayerModel<T extends net.minecraft.world.entity.LivingEntity> extends HumanoidModel<T> {
    public PlayerModel(net.minecraft.client.model.geom.ModelPart root, boolean slim) {
    }

    /** rightSleeve: the skin's outer layer of the right arm */ public net.minecraft.client.model.geom.ModelPart f_103375_;
    /** leftSleeve */ public net.minecraft.client.model.geom.ModelPart f_103374_;
    /** leftPants (1.0.41) */ public net.minecraft.client.model.geom.ModelPart f_103376_;
    /** rightPants */ public net.minecraft.client.model.geom.ModelPart f_103377_;
    /** jacket */ public net.minecraft.client.model.geom.ModelPart f_103378_;
    /** setupAnim (1.0.41: a ghoul NPC's flip posed on top) */ public void m_6973_(T entity, float limbSwing, float limbSwingAmount, float ageInTicks,
        float netHeadYaw, float headPitch) { throw new UnsupportedOperationException(); }
}
