// Compile-only API stub (Minecraft 1.20.1 / Forge 47 / PlayerAnimator). Never packaged; see verify/api-allowlist.txt.
package net.minecraft.world.entity;

public abstract class LivingEntity extends Entity {
    /** dropCustomDeathLoot(source, looting, recentlyHit) (7.0.2) */ protected void m_7472_(net.minecraft.world.damagesource.DamageSource source, int looting, boolean recentlyHit) { throw new UnsupportedOperationException(); }
    /** getAttribute (7.0.2) */ public net.minecraft.world.entity.ai.attributes.AttributeInstance m_21051_(net.minecraft.world.entity.ai.attributes.Attribute attribute) { throw new UnsupportedOperationException(); }
    /** getRandom (declared here, not on Entity) */ public net.minecraft.util.RandomSource m_217043_() { throw new UnsupportedOperationException(); }
    /** swinging (1.0.42) */ public boolean f_20911_;
    /** swingTime (1.0.42) */ public int f_20913_;
    /** yBodyRot */ public float f_20883_;
    /** yBodyRotO */ public float f_20884_;
    /** yHeadRot */ public float f_20885_;
    /** getArmorValue */ public int m_21230_() { throw new UnsupportedOperationException(); }
    /** yHeadRotO */ public float f_20886_;
    /** walkAnimation */ public final WalkAnimationState f_267362_ = null;
    /** isFallFlying */ public boolean m_21255_() { throw new UnsupportedOperationException(); }
    /** hasLineOfSight */ public boolean m_142582_(Entity entity) { throw new UnsupportedOperationException(); }
    /** onClimbable */ public boolean m_6147_() { throw new UnsupportedOperationException(); }
    /** isSleeping */ public boolean m_5803_() { throw new UnsupportedOperationException(); }
    /** isBlocking (7.0.2) */ public boolean m_21254_() { throw new UnsupportedOperationException(); }
    /** isUsingItem */ public boolean m_6117_() { throw new UnsupportedOperationException(); }
    /** heal */ public void m_5634_(float amount) { throw new UnsupportedOperationException(); }
    /** getHealth */ public float m_21223_() { throw new UnsupportedOperationException(); }
    /** getMaxHealth */ public float m_21233_() { throw new UnsupportedOperationException(); }
    /** pushEntities (yarn tickCramming; 1.3.15) */ protected void m_6138_() { throw new UnsupportedOperationException(); }
    /** setHealth */ public void m_21153_(float health) { throw new UnsupportedOperationException(); }
    /** hasEffect */ public boolean m_21023_(net.minecraft.world.effect.MobEffect effect) { throw new UnsupportedOperationException(); }
    /** removeEffect */ public boolean m_21195_(net.minecraft.world.effect.MobEffect effect) { throw new UnsupportedOperationException(); }
    /** getEffect */ public net.minecraft.world.effect.MobEffectInstance m_21124_(net.minecraft.world.effect.MobEffect effect) { throw new UnsupportedOperationException(); }
    /** addEffect */ public boolean m_7292_(net.minecraft.world.effect.MobEffectInstance effect) { throw new UnsupportedOperationException(); }
    /** addEffect(effect, source) */ public boolean m_147207_(net.minecraft.world.effect.MobEffectInstance effect, Entity source) { throw new UnsupportedOperationException(); }
    /** getMainHandItem */ public net.minecraft.world.item.ItemStack m_21205_() { throw new UnsupportedOperationException(); }
    /** getItemBySlot */ public net.minecraft.world.item.ItemStack m_6844_(EquipmentSlot slot) { throw new UnsupportedOperationException(); }
    /** getOffhandItem */ public net.minecraft.world.item.ItemStack m_21206_() { throw new UnsupportedOperationException(); }
    /** knockback */ public void m_147240_(double strength, double x, double z) { throw new UnsupportedOperationException(); }
    /** getAttributeValue */ public double m_21133_(net.minecraft.world.entity.ai.attributes.Attribute attribute) { throw new UnsupportedOperationException(); }
    /** getBrain (6.2.0) */ public net.minecraft.world.entity.ai.Brain<?> m_6274_() { throw new UnsupportedOperationException(); }
    /** getLastHurtByMob (6.2.0) */ public LivingEntity m_21188_() { throw new UnsupportedOperationException(); }
    /** swing(hand, sendToSwingingEntity) (7.0.0) */ public void m_21011_(net.minecraft.world.InteractionHand hand, boolean send) { throw new UnsupportedOperationException(); }
    /** setLastHurtByMob (6.2.0) */ public void m_6703_(LivingEntity entity) { throw new UnsupportedOperationException(); }
    /** isBaby (1.3.2) */ public boolean m_6162_() { throw new UnsupportedOperationException(); }
    /** setItemInHand (1.3.2) */ public void m_21008_(net.minecraft.world.InteractionHand hand, net.minecraft.world.item.ItemStack stack) { throw new UnsupportedOperationException(); }
}
