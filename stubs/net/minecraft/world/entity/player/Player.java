// Compile-only API stub (Minecraft 1.20.1 / Forge 47 / PlayerAnimator). Never packaged; see verify/api-allowlist.txt.
package net.minecraft.world.entity.player;

public abstract class Player extends net.minecraft.world.entity.LivingEntity {
    /** addItem */ public boolean m_36356_(net.minecraft.world.item.ItemStack stack) { throw new UnsupportedOperationException(); }
    /** drop(stack, includeThrowerName) */ public net.minecraft.world.entity.item.ItemEntity m_36176_(net.minecraft.world.item.ItemStack stack, boolean thrower) { throw new UnsupportedOperationException(); }
    /** getFoodData */ public net.minecraft.world.food.FoodData m_36324_() { throw new UnsupportedOperationException(); }
    /** causeFoodExhaustion */ public void m_36399_(float exhaustion) { throw new UnsupportedOperationException(); }
    /** mayBuild */ public boolean m_36326_() { throw new UnsupportedOperationException(); }
    /** isCreative */ public boolean m_7500_() { throw new UnsupportedOperationException(); }
    /** getGameProfile */ public com.mojang.authlib.GameProfile m_36316_() { throw new UnsupportedOperationException(); }
    /** attack (7.0.0) */ public void m_5706_(net.minecraft.world.entity.Entity target) { throw new UnsupportedOperationException(); }
    /** disableShield(becauseOfAxe) (7.0.2: Owl's Execution breaks through a raised shield) */ public void m_36384_(boolean axe) { throw new UnsupportedOperationException(); }
    /** displayClientMessage */ public void m_5661_(net.minecraft.network.chat.Component message, boolean actionBar) { throw new UnsupportedOperationException(); }
}
