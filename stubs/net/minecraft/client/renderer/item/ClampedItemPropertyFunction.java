// Compile-only API stub (Minecraft 1.20.1 / Forge 47 / PlayerAnimator). Never packaged; see verify/api-allowlist.txt.
package net.minecraft.client.renderer.item;

/** 1.3.25: an item model's property, clamped to 0..1 (its unclampedCall is what a lambda is). */
@FunctionalInterface
public interface ClampedItemPropertyFunction {
    /** unclampedCall */ float m_142187_(net.minecraft.world.item.ItemStack stack, net.minecraft.client.multiplayer.ClientLevel level,
                                         net.minecraft.world.entity.LivingEntity entity, int seed);
}
