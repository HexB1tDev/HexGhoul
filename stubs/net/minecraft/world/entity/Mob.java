// Compile-only API stub (Minecraft 1.20.1 / Forge 47 / PlayerAnimator). Never packaged; see verify/api-allowlist.txt.
package net.minecraft.world.entity;

public abstract class Mob extends LivingEntity {
    /** goalSelector */ protected final net.minecraft.world.entity.ai.goal.GoalSelector f_21345_ = null;
    /** registerGoals */ protected void m_8099_() { throw new UnsupportedOperationException(); }
    /** customServerAiStep */ protected void m_8024_() { throw new UnsupportedOperationException(); }
    /** getNavigation */ public net.minecraft.world.entity.ai.navigation.PathNavigation m_21573_() { throw new UnsupportedOperationException(); }
    /** getLookControl */ public net.minecraft.world.entity.ai.control.LookControl m_21563_() { throw new UnsupportedOperationException(); }
    /** getJumpControl */ public net.minecraft.world.entity.ai.control.JumpControl m_21569_() { throw new UnsupportedOperationException(); }
    /** setPersistenceRequired */ public void m_21530_() { throw new UnsupportedOperationException(); }
    /** isPersistenceRequired */ public boolean m_21532_() { throw new UnsupportedOperationException(); }
    /** createMobAttributes */ public static net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder m_21552_() { throw new UnsupportedOperationException(); }
    /** getTarget */ public LivingEntity m_5448_() { throw new UnsupportedOperationException(); }
    /** restrictTo(center, radius) (1.3.0) */ public void m_21446_(net.minecraft.core.BlockPos center, int radius) { throw new UnsupportedOperationException(); }
    /** setTarget */ public void m_6710_(LivingEntity target) { throw new UnsupportedOperationException(); }
    /** getLeashHolder (1.3.2) */ public net.minecraft.world.entity.Entity m_21524_() { throw new UnsupportedOperationException(); }
    /** isLeashed (1.3.2) */ public boolean m_21523_() { throw new UnsupportedOperationException(); }
    /** dropLeash(broadcast, dropItem) (1.3.2) */ public void m_21455_(boolean broadcast, boolean dropItem) { throw new UnsupportedOperationException(); }
    /** setLeftHanded (1.3.3) */ public void m_21559_(boolean left) { throw new UnsupportedOperationException(); }
}
