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
    /** getTag (1.3.25) */ public net.minecraft.nbt.CompoundTag m_41783_() { throw new UnsupportedOperationException(); }
    /** setTag (1.3.25) */ public void m_41751_(net.minecraft.nbt.CompoundTag tag) { throw new UnsupportedOperationException(); }
    /** isEmpty */ public boolean m_41619_() { throw new UnsupportedOperationException(); }
    /** getUseDuration */ public int m_41779_() { throw new UnsupportedOperationException(); }
    /** isEdible */ public boolean m_41614_() { throw new UnsupportedOperationException(); }
    /** Forge IForgeItemStack bouncer */ public net.minecraft.world.food.FoodProperties getFoodProperties(net.minecraft.world.entity.LivingEntity entity) { throw new UnsupportedOperationException(); }
    /** EMPTY (1.3.29) */ public static final ItemStack f_41583_ = null;
    /** getCount (1.3.29) */ public int m_41613_() { throw new UnsupportedOperationException(); }
    /** setCount (1.3.29) */ public void m_41764_(int count) { throw new UnsupportedOperationException(); }
    /** copy (1.3.29) */ public ItemStack m_41777_() { throw new UnsupportedOperationException(); }
    /** getMaxStackSize (1.3.29) */ public int m_41741_() { throw new UnsupportedOperationException(); }
    /** isSameItemSameTags(a, b) (1.3.29) */ public static boolean m_150942_(ItemStack a, ItemStack b) { throw new UnsupportedOperationException(); }
}
