// Compile-only API stub (Minecraft 1.20.1 / Forge 47 / PlayerAnimator). Never packaged; see verify/api-allowlist.txt.
package net.minecraft.world.item;

public final class ItemStack {
    /** shrink(count) (7.0.2) */ public void m_41774_(int count) { throw new UnsupportedOperationException(); }
    ItemStack() {
    }

    public ItemStack(net.minecraft.world.level.ItemLike item, int count) {
    }

    /** is(TagKey) */ public boolean m_204117_(net.minecraft.tags.TagKey<Item> tag) { throw new UnsupportedOperationException(); }
    /** getItem */ public Item m_41720_() { throw new UnsupportedOperationException(); }
    /** isEmpty */ public boolean m_41619_() { throw new UnsupportedOperationException(); }
    /** getUseDuration */ public int m_41779_() { throw new UnsupportedOperationException(); }
    /** isEdible */ public boolean m_41614_() { throw new UnsupportedOperationException(); }
    /** Forge IForgeItemStack bouncer */ public net.minecraft.world.food.FoodProperties getFoodProperties(net.minecraft.world.entity.LivingEntity entity) { throw new UnsupportedOperationException(); }
}
