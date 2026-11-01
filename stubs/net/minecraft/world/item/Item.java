// Compile-only API stub (Minecraft 1.20.1 / Forge 47 / PlayerAnimator). Never packaged; see verify/api-allowlist.txt.
package net.minecraft.world.item;

public class Item implements net.minecraft.world.level.ItemLike {
    Item() {
    }

    public Item(Properties properties) {
    }

    /** getUseAnimation (1.3.40) */ public UseAnim m_6164_(ItemStack stack) { throw new UnsupportedOperationException(); }
    /** getUseDuration (1.3.40) */ public int m_8105_(ItemStack stack) { throw new UnsupportedOperationException(); }
    public static class Properties {
        public Properties() {
        }

        /** food */ public Properties m_41489_(net.minecraft.world.food.FoodProperties food) { throw new UnsupportedOperationException(); }
        /** stacksTo (1.3.2) */ public Properties m_41487_(int count) { throw new UnsupportedOperationException(); }
        /** craftRemainder (1.3.40) */ public Properties m_41495_(Item remainder) { throw new UnsupportedOperationException(); }
    }
}
